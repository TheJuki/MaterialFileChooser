package br.tiagohm.materialfilechooser.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import br.tiagohm.materialfilechooser.MaterialFileChooser
import br.tiagohm.materialfilechooser.Sorter
import com.thejuki.kformmaster.helper.*
import com.thejuki.kformmaster.model.FormTextViewElement
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var formBuilder: FormBuildHelper

    private var fileChooserModel = FileChooserModel()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupForm()
    }

    private enum class Tag {
        Files,
        Image
    }

    private fun setupForm() {
        formBuilder = form(this, recyclerView) {
            textArea {
                title = "Initial Folder"
                value = fileChooserModel.initialFolder.absolutePath
                valueObservers.add({ newValue, _ ->
                    val folder = File(newValue.orEmpty())
                    fileChooserModel.initialFolder = folder
                })
            }
            checkBox<Boolean> {
                title = "Allow Multiple Files"
                value = fileChooserModel.allowMultipleFiles
                checkedValue = true
                unCheckedValue = false
                valueObservers.add({ newValue, _ ->
                    fileChooserModel.allowMultipleFiles = newValue ?: false
                })
            }
            checkBox<Boolean> {
                title = "Allow Browsing"
                value = fileChooserModel.allowBrowsing
                checkedValue = true
                unCheckedValue = false
                valueObservers.add({ newValue, _ ->
                    fileChooserModel.allowBrowsing = newValue ?: false
                })
            }
            checkBox<Boolean> {
                title = "Allow Create Folder"
                value = fileChooserModel.allowCreateFolder
                checkedValue = true
                unCheckedValue = false
                valueObservers.add({ newValue, _ ->
                    fileChooserModel.allowCreateFolder = newValue ?: false
                })
            }
            checkBox<Boolean> {
                title = "Allow Selecting Folders"
                value = fileChooserModel.allowSelectFolder
                checkedValue = true
                unCheckedValue = false
                valueObservers.add({ newValue, _ ->
                    fileChooserModel.allowSelectFolder = newValue ?: false
                })
            }
            number {
                title = "Min Selected Files"
                numbersOnly = true
                value = fileChooserModel.minSelectedFiles.toString()
                valueObservers.add({ newValue, _ ->
                    fileChooserModel.minSelectedFiles = newValue?.toIntOrNull() ?: 0
                })
            }
            number {
                title = "Max Selected Files"
                numbersOnly = true
                value = fileChooserModel.maxSelectedFiles.toString()
                valueObservers.add({ newValue, _ ->
                    fileChooserModel.maxSelectedFiles = newValue?.toInt() ?: 0
                })
            }
            checkBox<Boolean> {
                title = "Show Hidden Files"
                value = fileChooserModel.showHiddenFiles
                checkedValue = true
                unCheckedValue = false
                valueObservers.add({ newValue, _ ->
                    fileChooserModel.showHiddenFiles = newValue ?: false
                })
            }
            checkBox<Boolean> {
                title = "Show Folders First"
                value = fileChooserModel.showFoldersFirst
                checkedValue = true
                unCheckedValue = false
                valueObservers.add({ newValue, _ ->
                    fileChooserModel.showFoldersFirst = newValue ?: false
                })
            }
            checkBox<Boolean> {
                title = "Show Files"
                value = fileChooserModel.showFiles
                checkedValue = true
                unCheckedValue = false
                valueObservers.add({ newValue, _ ->
                    fileChooserModel.showFiles = newValue ?: false
                })
            }
            checkBox<Boolean> {
                title = "Show Folders"
                value = fileChooserModel.showFolders
                checkedValue = true
                unCheckedValue = false
                valueObservers.add({ newValue, _ ->
                    fileChooserModel.showFolders = newValue ?: false
                })
            }
            checkBox<Boolean> {
                title = "Allow Browsing"
                value = fileChooserModel.allowBrowsing
                checkedValue = true
                unCheckedValue = false
                valueObservers.add({ newValue, _ ->
                    fileChooserModel.allowBrowsing = newValue ?: false
                })
            }
            checkBox<Boolean> {
                title = "Restore folder"
                value = fileChooserModel.restoreFolder
                checkedValue = true
                unCheckedValue = false
                valueObservers.add({ newValue, _ ->
                    fileChooserModel.restoreFolder = newValue ?: false
                })
            }
            button {
                value = getString(R.string.open_file_chooser)
                valueObservers.add({ _, _ ->
                    if (ContextCompat.checkSelfPermission(this@MainActivity,
                                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this@MainActivity,
                                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                                1234)
                    } else {
                        showMaterialFileChooser()
                    }

                })
            }
            textView(Tag.Files.ordinal) {
                title = "Files"
                maxLines = 10
                value = "Selected file names will show up here"
            }
        }

        //formBuilder.registerCustomViewBinder(CustomViewBinder(this, formBuilder).viewBinder)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1234 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showMaterialFileChooser()
                }
                return
            }
        }
    }
    
    private fun showMaterialFileChooser() {
        MaterialFileChooser(this@MainActivity,
                allowBrowsing = fileChooserModel.allowBrowsing,
                allowCreateFolder = fileChooserModel.allowCreateFolder,
                allowMultipleFiles = fileChooserModel.allowMultipleFiles,
                allowSelectFolder = fileChooserModel.allowSelectFolder,
                minSelectedFiles = fileChooserModel.minSelectedFiles,
                maxSelectedFiles = fileChooserModel.maxSelectedFiles,
                showFiles = fileChooserModel.showFiles,
                showFoldersFirst = fileChooserModel.showFoldersFirst,
                showFolders = fileChooserModel.showFolders,
                showHiddenFiles = fileChooserModel.showHiddenFiles,
                initialFolder = fileChooserModel.initialFolder, //Environment.getExternalStoragePublicDirectory("DIRECTORY_DCIM"),
                restoreFolder = fileChooserModel.restoreFolder)
                .title("Choose a file")
                .sorter(Sorter.ByNameInDescendingOrder)
                .onSelectedFilesListener {
                    fileChooserModel.files.clear()
                    fileChooserModel.files.addAll(it)
                    formBuilder.getFormElement<FormTextViewElement>(Tag.Files.ordinal).value = fileChooserModel.files.toString()
                }
                .show()
    }
}