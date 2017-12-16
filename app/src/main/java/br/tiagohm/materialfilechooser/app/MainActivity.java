package br.tiagohm.materialfilechooser.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.List;

import br.tiagohm.materialfilechooser.MaterialFileChooser;
import br.tiagohm.materialfilechooser.Sorter;

public class MainActivity extends AppCompatActivity implements MaterialFileChooser.OnFileChooserListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    1234);
        } else {
            showMaterialFileChooser();
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1234: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showMaterialFileChooser();
                }
                return;
            }
        }
    }

    @Override
    public void onItemSelected(List<File> files) {
        Log.d("TAG", files.toString());
    }

    @Override
    public void onCancelled() {
        Toast.makeText(this, "A janela foi cancelada", Toast.LENGTH_SHORT).show();
    }

    private void showMaterialFileChooser() {
        new MaterialFileChooser(this, "Selecione um arquivo")
                .allowSelectFolder(false)
                .allowBrowsing(true)
                .allowMultipleFiles(true)
                .allowCreateFolder(false)
                .showHiddenFiles(true)
                .showFoldersFirst(true)
                .showFolders(true)
                .showFiles(true)
                //.initialFolder(Environment.getExternalStorageDirectory())
                .onFileChooserListener(this)
                //OR Logic
                //.filter(new ExtensionFilter("jpg"))
                //.filter(new ExtensionFilter("png"))
                //AND Logic
                //.filter(new ExtensionFilter(new RegexFilter(".*WA.*"), "jpg"))
                .sorter(Sorter.SORT_BY_NEWEST_MODIFICATION)
                .show();
    }
}
