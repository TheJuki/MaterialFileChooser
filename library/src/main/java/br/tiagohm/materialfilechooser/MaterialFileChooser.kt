package br.tiagohm.materialfilechooser

import android.content.Context
import android.os.Environment
import android.support.annotation.StringRes
import android.support.design.widget.FloatingActionButton
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.View
import android.widget.*
import br.tiagohm.breadcrumbview.BreadCrumbItem
import br.tiagohm.breadcrumbview.BreadCrumbView
import br.tiagohm.easyadapter.EasyAdapter
import br.tiagohm.materialfilechooser.filters.Filter
import com.afollestad.materialdialogs.MaterialDialog
import java.io.File
import java.io.FileFilter
import java.util.*
import java.util.concurrent.ConcurrentHashMap

open class MaterialFileChooser(val context: Context,
                               val allowMultipleFiles: Boolean = false,
                               val allowCreateFolder: Boolean = false,
                               var initialFolder: File = Environment.getExternalStorageDirectory(),
                               val allowSelectFolder: Boolean = false,
                               val minSelectedFiles: Int = 0,
                               val maxSelectedFiles: Int = Int.MAX_VALUE,
                               val showHiddenFiles: Boolean = false,
                               val showFoldersFirst: Boolean = true,
                               val showFiles: Boolean = true,
                               val showFolders: Boolean = true,
                               val allowBrowsing: Boolean = true,
                               val restoreFolder: Boolean = true) {

    private val typedValue = TypedValue()
    private val theme = context.theme
    private val listOfFilesAndFoldersAdapter = EasyAdapter()
    private val folders = LinkedList<File>()
    private val fileSelectionStatus = ConcurrentHashMap<File, Boolean>()
    private val filters = ArrayList<Filter>()
    private val chooserTextWatcher = ChooserTextWatcher()
    private val chooserFileFilter = ChooserFileFilter()
    private val backgroundColorInt: Int by lazy {
        theme.resolveAttribute(R.attr.mfc_theme_background, typedValue, true)
        typedValue.data
    }
    private val foregroundColorInt: Int by lazy {
        theme.resolveAttribute(R.attr.mfc_theme_foreground, typedValue, true)
        typedValue.data
    }
    private val titleColorInt: Int by lazy {
        theme.resolveAttribute(R.attr.mfc_theme_title, typedValue, true)
        typedValue.data
    }
    private val cancelButtonColorInt: Int by lazy {
        theme.resolveAttribute(R.attr.mfc_theme_cancel_button, typedValue, true)
        typedValue.data
    }
    private val okButtonColorInt: Int by lazy {
        theme.resolveAttribute(R.attr.mfc_theme_ok_button, typedValue, true)
        typedValue.data
    }

    private var title: CharSequence? = ""
    private val filesSelected: MutableSet<File> = Collections.newSetFromMap(ConcurrentHashMap<File, Boolean>())
    private var filePreviouslySelectedCb: CheckBox? = null
    private var fileSelected: File? = null
    private lateinit var currentFolder: File
    private var orderSorter: Sorter = Sorter.ByNameInAscendingOrder
    private lateinit var dialogWindow: DialogBuilder
    private var textSearch = ""
    private var currentFiles: List<File> = Collections.emptyList()
    private var selectedFilesTotalSize = 0L
    private var onSelectedFilesListener: (files: List<File>) -> Unit = {}
    
    init {
        // Default home folder
        setHomeFolder(ChooserSharedPreference.getPreviouslySelectedDirectory(context, restoreFolder, initialFolder))
    }

    /** Sets the title of the [dialogWindow] */
    fun title(title: CharSequence?): MaterialFileChooser {
        this.title = title
        return this
    }

    /** Sets the title of the [dialogWindow] using a string resource */
    fun title(@StringRes resId: Int): MaterialFileChooser {
        return title(context.getText(resId))
    }

    /** Sets the sort method of listing files and folders. */
    fun sorter(sorter: Sorter): MaterialFileChooser {
        orderSorter = sorter
        return this
    }

    /** Sets the selected files listener which is called when the dialog is onPositive dismissed */
    fun onSelectedFilesListener(listener: (files: List<File>) -> Unit): MaterialFileChooser {
        onSelectedFilesListener = listener
        return this
    }

    private fun setHomeFolder(folder: File): MaterialFileChooser {
        initialFolder = folder
        currentFolder = folder
        // Clear folders
        folders.clear()
        // Insert current folder
        folders.addFirst(currentFolder)
        // Current folder is not selected
        fileSelectionStatus[currentFolder] = false
        return this
    }

    private fun displayBreadCrumbView(file: File) {
        var mfile = file
        // Clear crumbs
        dialogWindow.mBreadCrumbDirectory.itens.clear()
        // Checks if it is not a folder
        if (!mfile.isFolder) {
            mfile = mfile.parentFile
        }
        // Gets the parent folder
        val parent = mfile.parentFile
        // There is no parent folder
        if (parent == null) {
            val item = RootFileBreadCrumbItem(mfile)
            dialogWindow.mBreadCrumbDirectory.addItem(item)
        }
        // There is a parent folder
        else {
            displayBreadCrumbView(parent)
            val item = FileBreadCrumbItem(mfile)
            dialogWindow.mBreadCrumbDirectory.addItem(item)
        }
    }

    private fun selectFile(buttonView: CompoundButton?, file: File, isSelected: Boolean) {
        // Checkbox is selected
        if (isSelected) {
            // It is not multi-selectable and has a selected file
            if (!allowMultipleFiles && filePreviouslySelectedCb != null) {
                val cb = filePreviouslySelectedCb!!
                filePreviouslySelectedCb = null
                // Two files are in the same folder
                if (buttonView !== cb && file.parent == fileSelected?.parent) {
                    // Uncheck what is selected
                    cb.isChecked = false
                } else {
                    // Removes what is selected
                    filesSelected.remove(fileSelected)
                    fileSelected = null
                }
            }
            // Add the file
            if (!filesSelected.contains(file)) {
                selectedFilesTotalSize += if (file.isFolder) 0 else file.length()
            }
            filesSelected.add(file)
        } else {
            // Remove the file
            if (filesSelected.contains(file)) {
                selectedFilesTotalSize -= if (file.isFolder) 0 else file.length()
            }
            filesSelected.remove(file)
            fileSelected = null
        }
        // Mark the file that was selected
        filePreviouslySelectedCb = buttonView as CheckBox?
        fileSelected = file
        // Updates the number of selected folders according to the plurality
        dialogWindow.displayNumberOfSelectedItems(selectedFilesTotalSize)
    }
    
    /**
     * Go to a specific folder.
     */
    fun goTo(file: File) {
        // Navigate only if it is a folder
        if (allowBrowsing && file.isFolder) {
            currentFolder = file
            folders.addFirst(currentFolder)
            // You have not yet navigated this folder. Selecting everything is disabled.
            if (allowMultipleFiles && !fileSelectionStatus.containsKey(file)) {
                fileSelectionStatus[file] = false
            }
            loadCurrentFolder()
            // Sets the state of the select all checkBox.
            dialogWindow.mSelectAllCheckBox.isChecked = allowMultipleFiles && fileSelectionStatus[file]!!
        }
    }
    
    /**
     * Go to the initial folder
     */
    fun goToStart() {
        goTo(initialFolder)
    }
    
    private fun backTo(file: File): Boolean {
        // Can browse and is a folder.
        return if (allowBrowsing && file.isFolder) {
            currentFolder = file
            loadCurrentFolder()
            // Sets the state of the select all checkBox.
            dialogWindow.mSelectAllCheckBox.isChecked = allowMultipleFiles && fileSelectionStatus[file] ?: false
            true
        } else {
            false
        }
    }
    
    /**
     * Go to the previous folder.
     */
    fun back(): Boolean {
        // If you can navigate and there is folder to navigate.
        return if (allowBrowsing && folders.size > 1) {
            // Remove the current folder
            folders.removeFirst()
            // Return to the previous folder
            backTo(folders.first)
        } else {
            false
        }
    }
    
    private fun compareFile(a: File, b: File): Int {
        // Sort by folders first
        return if (showFoldersFirst) {
            when {
                a.isDirectory == b.isDirectory -> orderSorter.compare(a, b)
                a.isDirectory -> -1
                else -> 1
            }
        } else {
            when {
                a.isFile == b.isFile -> orderSorter.compare(a, b)
                a.isFile -> -1
                else -> 1
            }
        }
    }
    
    private fun scanFiles(file: File): List<File> {
        // Get the list of files
        val fileList = file.listFiles(chooserFileFilter)?.toMutableList() ?: Collections.emptyList()
        // Set the number of files displayed
        dialogWindow.mNumberOfFiles.text = fileList.size.toString()
        // Order file list
        fileList.sortWith(Comparator { a, b -> compareFile(a, b) })
        return fileList
    }

    private fun displayRecyclerView(file: File) {
        currentFiles = scanFiles(file)
        listOfFilesAndFoldersAdapter.setData(currentFiles)
    }

    private fun loadCurrentFolder() {
        displayBreadCrumbView(currentFolder)
        displayRecyclerView(currentFolder)
        dialogWindow.mSizeTotal.text = currentFolder.sizeAsString
    }

    /** Display the [dialogWindow] */
    fun show() {
        DialogBuilder()
        dialogWindow.show()
    }
    
    private inner class DialogBuilder : MaterialDialog.Builder(context) {

        /** Views in the [dialogWindow] */
        val mTitle: TextView by lazy { customView.findViewById<TextView>(R.id.title) }
        val mBreadCrumbDirectory: BreadCrumbView<File> by lazy { customView.findViewById<BreadCrumbView<File>>(R.id.directoryPath) }
        val mListOfFiles: RecyclerView by lazy { customView.findViewById<RecyclerView>(R.id.filesRecylerView) }
        val mSizeTotal: TextView by lazy { customView.findViewById<TextView>(R.id.totalSize) }
        val mNumberOfFiles: TextView by lazy { customView.findViewById<TextView>(R.id.numberOfItems) }
        val mBackButton: ImageView by lazy { customView.findViewById<ImageView>(R.id.backButton) }
        val mHomeButton: ImageView by lazy { customView.findViewById<ImageView>(R.id.goHomeButton) }
        val mNumberOfSelectedItems: TextView by lazy { customView.findViewById<TextView>(R.id.numberOfSelectedItems) }
        val mSearchButton: ImageView by lazy { customView.findViewById<ImageView>(R.id.searchButton) }
        val mSearchField: EditText by lazy { customView.findViewById<EditText>(R.id.searchField) }
        val mSearchBoxFrame: View by lazy { customView.findViewById<View>(R.id.searchBoxFrame) }
        val mSwipeRefreshLayout: SwipeRefreshLayout by lazy { customView.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout) }
        val mSelectAllCheckBox: CheckBox by lazy { customView.findViewById<CheckBox>(R.id.selectAllCheckBox) }
        val mCreateFolderFAB: FloatingActionButton by lazy { customView.findViewById<FloatingActionButton>(R.id.createFolderFAB) }
        
        init {
            dialogWindow = this
            // Set layout
            customView(R.layout.dialog_file_chooser, false)
            // Set the title
            if (this@MaterialFileChooser.title.isNullOrEmpty()) mTitle.visibility = View.GONE else mTitle.text = this@MaterialFileChooser.title
            // Buttons
            positiveText(android.R.string.ok)
            negativeText(android.R.string.cancel)
            // Theme
            backgroundColor(backgroundColorInt)
            positiveColor(foregroundColorInt)
            negativeColor(foregroundColorInt)
            mSwipeRefreshLayout.setColorSchemeColors(foregroundColorInt)
            // Behavior
            cancelable(false)
            canceledOnTouchOutside(false)
            autoDismiss(false)
            // Configure the RecyclerView.
            mListOfFiles.layoutManager = LinearLayoutManager(context)
            listOfFilesAndFoldersAdapter.map<File>(R.layout.file_item) { file, injector ->
                // File or folder icon
                injector.image(R.id.fileIcon,
                        if (file.isFolder) R.drawable.folder else getIconByExtension(file))
                // Protected icon
                injector.image(R.id.fileProtection,
                        if (file.isProtected) R.drawable.padlock else 0)
                // Folder with selected items icon
                injector.image(R.id.folderWithSelectedItems,
                        if (file.isFolder && containsSelectedFiles(file)) R.drawable.asterisk else 0)
                // Set opacity if the file is hidden
                injector.using<View>(R.id.fileIcon) { alpha = if (file.isHidden) 0.4f else 1f }
                // Set file name
                injector.text(R.id.fileName, file.name)
                // Set size of the file
                if (file.isFile) {
                    // Size in bytes
                    injector.text(R.id.fileSize, file.sizeAsString)
                }
                // Size of items
                else {
                    // Number of items
                    val itemCount = file.count(chooserFileFilter)
                    // Format item count with plurality
                    val stringRes = if (itemCount > 1) R.string.number_of_items_plural else R.string.number_of_items_singular
                    injector.text(R.id.fileSize, context.getString(stringRes, itemCount))
                }
                // Set last modified date
                injector.text(R.id.lastModificationDate, file.lastModified)
                // Allow selection of multiple files
                injector.show(R.id.selectFileCheckBox, allowSelectFolder || file.isFile)
                // Events
                injector.click<View>(EasyAdapter.ROOT_VIEW) {
                    // Go to clicked folder
                    if (file.isFolder) {
                        goTo(file)
                    } else {
                        injector.using<CheckBox>(R.id.selectFileCheckBox) {
                            selectFile(this, file, isChecked)
                        }
                    }
                }
                // Select/un-select file
                injector.using<CheckBox>(R.id.selectFileCheckBox) {
                    tag = file
                    // Check checkbox if this is from a selected file.
                    setOnCheckedChangeListener(null)
                    isChecked = filesSelected.contains(file)
                    setOnCheckedChangeListener({ buttonView, isChecked ->
                        // Selects or deselects the file.
                        selectFile(buttonView, file, isChecked)
                    })
                }
            }
            listOfFilesAndFoldersAdapter.mapEmpty(R.layout.dialog_empty_folder)
            // Update folder
            mSwipeRefreshLayout.setOnRefreshListener {
                loadCurrentFolder()
                mSwipeRefreshLayout.isRefreshing = false
            }
            // Select/Un-select all
            mSelectAllCheckBox.setOnCheckedChangeListener { _, checked ->
                // Select or un-select the current file
                fileSelectionStatus[currentFolder] = checked
                currentFiles.forEach {
                    if (allowSelectFolder || !it.isFolder) {
                        selectFile(null, it, checked)
                    }
                }
                // Reload the displayed items
                loadCurrentFolder()
            }
            // Crumbs
            mBreadCrumbDirectory.setBreadCrumbListener(object : BreadCrumbView.BreadCrumbListener<File> {
                override fun onItemClicked(breadCrumbView: BreadCrumbView<File>, breadCrumbItem: BreadCrumbItem<File>, i: Int) {
                    goTo(breadCrumbItem.selectedItem)
                }
                
                override fun onItemValueChanged(breadCrumbView: BreadCrumbView<File>, breadCrumbItem: BreadCrumbItem<File>, i: Int, file: File, t1: File): Boolean {
                    return false
                }
            })
            // Go back a folder
            mBackButton.setOnClickListener { back() }
            // Go home
            mHomeButton.setOnClickListener { goToStart() }
            // Search
            mSearchButton.setOnClickListener {
                if (mSearchBoxFrame.visibility == View.VISIBLE) {
                    mSearchBoxFrame.visibility = View.GONE
                } else {
                    mSearchBoxFrame.visibility = View.VISIBLE
                }
            }
            // Search events
            mSearchField.removeTextChangedListener(chooserTextWatcher)
            mSearchField.addTextChangedListener(chooserTextWatcher)
            listOfFilesAndFoldersAdapter.attachTo(mListOfFiles)
            // Create folder
            mCreateFolderFAB.setOnClickListener {
                MaterialDialog.Builder(context)
                        .title(R.string.create_folder_title)
                        .titleColor(titleColorInt)
                        .inputRangeRes(1, -1, R.color.criar_pasta_input_out_range)
                        .inputType(InputType.TYPE_CLASS_TEXT)
                        .negativeText(android.R.string.cancel)
                        .negativeColor(cancelButtonColorInt)
                        .positiveColor(okButtonColorInt)
                        .backgroundColor(backgroundColorInt)
                        .input(R.string.create_folder_name_hint, 0, false, { _, input ->
                            val novaPasta = File(currentFolder, input.toString())
                            try {
                                if (!novaPasta.mkdir()) {
                                    Toast.makeText(context, "error", Toast.LENGTH_SHORT).show()
                                } else {
                                    loadCurrentFolder()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "error: " + e.message, Toast.LENGTH_SHORT).show()
                            }
                        }).show()
            }
            onPositive { dialog, _ ->
                // User has selected the minimum amount of files.
                if (filesSelected.size in minSelectedFiles..maxSelectedFiles) {
                    onSelectedFilesListener(filesSelected.toList())
                    // Close dialog window
                    dialog.dismiss()
                }
                
            }
            onNegative { dialog, _ ->
                dialog.dismiss()
            }
            dismissListener {
                // Saves the current folder if it is allowed to restore folders.
                if (restoreFolder) {
                    ChooserSharedPreference.setPreviouslySelectedDirectory(context, currentFolder)
                }
            }
            // Allows for multiple selection
            mSelectAllCheckBox.visibility = if (allowMultipleFiles) View.VISIBLE else View.GONE
            // Allow creating folders
            dialogWindow.mCreateFolderFAB.visibility = if (allowCreateFolder) View.VISIBLE else View.GONE
            // Start
            displayNumberOfSelectedItems(0)
            loadCurrentFolder()
        }

        fun displayNumberOfSelectedItems(size: Long) {
            // Updates the number of selected folders according to the plurality.
            if (filesSelected.size > 1) {
                dialogWindow.mNumberOfSelectedItems.text =
                        context.getString(R.string.number_of_selected_items_plural, filesSelected.size, size.toSizeString())
            } else {
                dialogWindow.mNumberOfSelectedItems.text =
                        context.getString(R.string.number_of_selected_items_singular, filesSelected.size, size.toSizeString())
            }
        }

        private fun containsSelectedFiles(parent: File): Boolean {
            for (file in filesSelected) {
                if (file.absolutePath != parent.absolutePath &&
                        file.absolutePath.startsWith(parent.absolutePath)) {
                    return true
                }
            }
            return false
        }
        
        private fun getIconByExtension(file: File): Int {
            return when (file.extension) {
                "mp4" -> R.drawable.video
                "c", "cpp", "cs", "js", "h", "java", "kt", "php", "xml" -> R.drawable.code
                "avi" -> R.drawable.avi
                "doc" -> R.drawable.doc
                "flv" -> R.drawable.flv
                "jpg", "jpeg" -> R.drawable.jpg
                "json" -> R.drawable.json
                "mov" -> R.drawable.mov
                "mp3" -> R.drawable.mp3
                "pdf" -> R.drawable.pdf
                "txt" -> R.drawable.txt
                else -> R.drawable.file
            }
        }
    }
    
    private object ChooserSharedPreference {
        
        private const val NAME = "materialfilechooser_prefs"
        private const val PREVIOUS_SELECTED_FOLDER = "prev_selected_folder"

        fun getPreviouslySelectedDirectory(context: Context, restoreFolder: Boolean, initialFolder: File): File {
            if (!restoreFolder) return initialFolder
            val path = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                    .getString(PREVIOUS_SELECTED_FOLDER, null)
            return if (path != null) File(path) else initialFolder
        }

        fun setPreviouslySelectedDirectory(context: Context, file: File) {
            context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(PREVIOUS_SELECTED_FOLDER, file.absolutePath)
                    .apply()
        }
    }

    // Item for the root folder.
    private class RootFileBreadCrumbItem(file: File) : FileBreadCrumbItem(file) {
        
        override fun getText() = "/"
    }

    // Item for a folder.
    private open class FileBreadCrumbItem(file: File) : BreadCrumbItem<File>() {
        
        init {
            itens = listOf(file)
        }
        
        override fun getText(): String? = selectedItem.name
    }

    // File filtering
    private inner class ChooserFileFilter : FileFilter {
        
        override fun accept(f: File): Boolean {
            val showHidden = showHiddenFiles || !f.isHidden
            return (textSearch.isEmpty() || f.name.contains(textSearch, true)) &&
                    // It is a hidden file and can be displayed.
                    showHidden &&
                    // View files and / or folders.
                    (showFiles && f.isFile || showFolders && f.isFolder) &&
                    // Filter
                    filter(f)
        }
        
        private fun filter(f: File): Boolean {
            // No filters
            if (filters.size == 0) return true
            // Filter
            for (filter in filters) {
                if (filter.accept(f)) {
                    return true
                }
            }
            return false
        }
    }
    
    private inner class ChooserTextWatcher : TextWatcher {
        
        override fun afterTextChanged(s: Editable) {
            textSearch = s.toString().toLowerCase()
            loadCurrentFolder()
        }
        
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        }
        
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        }
    }
}