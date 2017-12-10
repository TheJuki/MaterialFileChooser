package br.tiagohm.materialfilechooser.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.File;
import java.util.List;

import br.tiagohm.materialfilechooser.MaterialFileChooser;

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
            new MaterialFileChooser(this, "Selecione um arquivo").onFileChooserListener(this).show();
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1234: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    new MaterialFileChooser(this, "Selecione um arquivo").show();
                }
                return;
            }
        }
    }

    @Override
    public void onItemSelected(List<File> files) {
        Log.d("TAG", files.toString());
    }
}
