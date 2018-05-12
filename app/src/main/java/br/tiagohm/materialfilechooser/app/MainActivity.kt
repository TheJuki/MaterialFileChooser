package br.tiagohm.materialfilechooser.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import br.tiagohm.materialfilechooser.MaterialFileChooser
import br.tiagohm.materialfilechooser.Sorter

class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    1234)
        } else {
            showMaterialFileChooser()
        }
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
        MaterialFileChooser(this,
                allowBrowsing = true,
                allowCreateFolder = true,
                allowMultipleFiles = true,
                allowSelectFolder = true,
                minSelectedFiles = 1,
                maxSelectedFiles = 3,
                showFiles = true,
                showFoldersFirst = true,
                showFolders = true,
                showHiddenFiles = false,
                initialFolder = Environment.getRootDirectory(),
                restoreFolder = false)
                .title("Selecione um arquivo")
                .sorter(Sorter.ByNewestModification)
                .onSelectedFilesListener {
                    Toast.makeText(this, it.toString(), Toast.LENGTH_SHORT).show()
                }
                .show()
    }
}