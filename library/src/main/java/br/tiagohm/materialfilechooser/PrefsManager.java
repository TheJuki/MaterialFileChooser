package br.tiagohm.materialfilechooser;


import android.content.Context;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

class PrefsManager implements Closeable {

    private static final String PREFS_NAME = "materialfilechooser_prefs";
    private static final String PREVIOUS_SELECTED_FOLDER = "prev_selected_folder";
    private Context context;

    public PrefsManager(Context context) {
        this.context = context;
    }

    @Override
    public void close() throws IOException {
        context = null;
    }

    public File getPreviouslySelectedDiretory() {
        String path = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(PREVIOUS_SELECTED_FOLDER, null);
        return path == null ? null : new File(path);
    }

    public void setPreviouslySelectedDiretory(File file) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(PREVIOUS_SELECTED_FOLDER, file.getAbsolutePath())
                .apply();
    }
}
