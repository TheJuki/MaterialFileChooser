package br.tiagohm.materialfilechooser.filters;


import java.io.File;
import java.util.Arrays;
import java.util.List;

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

    private static String getExtension(final String filename) {
        final int index = indexOfExtension(filename);
        return index == -1 ? "" : filename.substring(index + 1);
    }

    private static int indexOfExtension(final String filename) {
        final int extensionPos = filename.lastIndexOf('.');
        final int lastSeparator = indexOfLastSeparator(filename);
        return lastSeparator > extensionPos ? -1 : extensionPos;
    }

    private static int indexOfLastSeparator(final String filename) {
        return filename.lastIndexOf('/');
    }

    @Override
    public boolean accept(File file) {
        if (file.isDirectory()) {
            return super.accept(file);
        }

        final String ext = getExtension(file.getName()).toLowerCase();
        for (String extension : extensions) {
            if (ext.equals(extension)) {
                return super.accept(file);
            }
        }

        return false;
    }
}
