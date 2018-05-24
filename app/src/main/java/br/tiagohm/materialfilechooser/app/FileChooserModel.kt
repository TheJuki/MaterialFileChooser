package br.tiagohm.materialfilechooser.app

import android.os.Environment
import java.io.File

class FileChooserModel(var allowMultipleFiles: Boolean = false,
                       var allowCreateFolder: Boolean = false,
                       var initialFolder: File = Environment.getExternalStorageDirectory(),
                       var allowSelectFolder: Boolean = false,
                       var minSelectedFiles: Int = 0,
                       var maxSelectedFiles: Int = 10,
                       var showHiddenFiles: Boolean = false,
                       var showFoldersFirst: Boolean = true,
                       var showFiles: Boolean = true,
                       var showFolders: Boolean = true,
                       var allowBrowsing: Boolean = true,
                       var restoreFolder: Boolean = true,
                       var files: MutableList<File> = mutableListOf()
)