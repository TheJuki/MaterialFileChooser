package br.tiagohm.materialfilechooser.filters;


import java.io.File;
import java.util.Arrays;
import java.util.List;

import br.tiagohm.materialfilechooser.FileHelper;
import br.tiagohm.materialfilechooser.Filter;

public class ExtensionFilter extends Filter {

    private final List<String> extensions;

    public ExtensionFilter(Filter filter, String... extensions) {
        super(filter);
        this.extensions = Arrays.asList(extensions);
    }

    public ExtensionFilter(Filter filter, List<String> extensions) {
        super(filter);
        this.extensions = extensions;
    }

    public ExtensionFilter(String... extensions) {
        this.extensions = Arrays.asList(extensions);
    }

    public ExtensionFilter(List<String> extensions) {
        this.extensions = extensions;
    }

    @Override
    public boolean accept(File file) {
        if (file.isDirectory()) {
            return super.accept(file);
        }

        final String ext = FileHelper.getExtension(file.getName()).toLowerCase();
        for (String extension : extensions) {
            if (ext.equals(extension)) {
                return super.accept(file);
            }
        }

        return false;
    }
}
