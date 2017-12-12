package br.tiagohm.materialfilechooser;


import java.io.File;
import java.io.FileFilter;

public abstract class Filter implements FileFilter {

    private final Filter filter;

    public Filter(Filter filter) {
        this.filter = filter;
    }

    public Filter() {
        this.filter = null;
    }

    @Override
    public boolean accept(File file) {
        return filter == null || filter.accept(file);
    }
}
