package br.tiagohm.materialfilechooser;


import org.ocpsoft.prettytime.PrettyTime;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class FileHelper {

    private static final PrettyTime PRETTY_TIME = new PrettyTime();

    public static boolean isFile(File file) {
        return file.isFile();
    }

    public static boolean isFolder(File file) {
        return file.isDirectory();
    }

    public static boolean isHidden(File file) {
        return file.isHidden();
    }

    public static boolean exists(File file) {
        return file.exists();
    }

    public static long sizeInBytes(File file, boolean recursive) {
        if (isFile(file)) {
            return file.length();
        } else {
            long sizeInBytes = 0;
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : file.listFiles()) {
                    if (f.isFile()) {
                        sizeInBytes += f.length();
                    } else if (recursive) {
                        sizeInBytes += sizeInBytes(f, recursive);
                    }
                }
            }
            return sizeInBytes;
        }
    }

    public static String sizeToString(long size) {
        if (size >= 1024 * 1024 * 1024) {
            return String.format("%.1f", size / (float) (1024 * 1024 * 1024)) + " GB";
        } else if (size >= 1024 * 1024) {
            return String.format("%.1f", size / (float) (1024 * 1024)) + " MB";
        } else if (size >= 1024) {
            return String.format("%.1f", size / 1024f) + " KB";
        } else {
            return size + " B";
        }
    }

    public static String sizeAsString(File file, boolean recursive) {
        final long size = sizeInBytes(file, recursive);
        return sizeToString(size);
    }

    public static String lastModified(File file) {
        return PRETTY_TIME.format(new Date(file.lastModified()));
    }

    public static int itemCount(File file) {
        if (isFolder(file)) {
            File[] files = file.listFiles();
            return files != null ? files.length : 0;
        } else {
            return 0;
        }
    }

    public static int itemCount(File file, FileFilter fileFilter) {
        if (isFolder(file)) {
            File[] files = file.listFiles(fileFilter);
            return files != null ? files.length : 0;
        } else {
            return 0;
        }
    }

    public static List<File> listFiles(File file, FileFilter fileFilter) {
        //Obtém a lista de arquivos.
        final File[] files = file.listFiles(fileFilter);
        //Não foi possível obter a lista de arquivos.
        if (files == null) {
            return Collections.emptyList();
        } else {
            //Seta a quantidade de itens exibidos.
            final List<File> fileList = Arrays.asList(files);
            return fileList;
        }
    }

    public static String getExtension(final String filename) {
        final int index = indexOfExtension(filename);
        return index == -1 ? "" : filename.substring(index + 1);
    }

    public static int indexOfExtension(final String filename) {
        final int extensionPos = filename.lastIndexOf('.');
        final int lastSeparator = indexOfLastSeparator(filename);
        return lastSeparator > extensionPos ? -1 : extensionPos;
    }

    public static int indexOfLastSeparator(final String filename) {
        return filename.lastIndexOf('/');
    }


    public static boolean isProtected(File file) {
        return !file.canRead() || !file.canWrite();
    }
}
