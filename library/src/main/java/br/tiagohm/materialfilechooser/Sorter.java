package br.tiagohm.materialfilechooser;


import java.io.File;
import java.util.Comparator;

public abstract class Sorter implements Comparator<File> {

    public static final Sorter SORT_BY_NAME_ASC = new Sorter() {
        @Override
        public int compare(File a, File b) {
            return a.getName().compareToIgnoreCase(b.getName());
        }
    };

    public static final Sorter SORT_BY_NAME_DESC = new Sorter() {
        @Override
        public int compare(File a, File b) {
            return -a.getName().compareToIgnoreCase(b.getName());
        }
    };

    public static final Sorter SORT_BY_NEWEST_MODIFICATION = new Sorter() {
        @Override
        public int compare(File a, File b) {
            return -Long.compare(a.lastModified(), b.lastModified());
        }
    };

    public static final Sorter SORT_BY_LATEST_MODIFICATION = new Sorter() {
        @Override
        public int compare(File a, File b) {
            return Long.compare(a.lastModified(), b.lastModified());
        }
    };

    public static final Sorter SORT_BY_SIZE_ASC = new Sorter() {
        @Override
        public int compare(File a, File b) {
            return Long.compare(a.length(), b.length());
        }
    };

    public static final Sorter SORT_BY_SIZE_DESC = new Sorter() {
        @Override
        public int compare(File a, File b) {
            return -Long.compare(a.length(), b.length());
        }
    };

    public static final Sorter SORT_BY_TYPE = new Sorter() {
        @Override
        public int compare(File a, File b) {
            return FileHelper.getExtension(a.getName())
                    .compareToIgnoreCase(FileHelper.getExtension(b.getName()));
        }
    };
}
