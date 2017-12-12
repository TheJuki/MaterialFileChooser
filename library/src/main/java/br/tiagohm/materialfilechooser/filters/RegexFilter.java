package br.tiagohm.materialfilechooser.filters;


import java.io.File;
import java.util.regex.Pattern;

import br.tiagohm.materialfilechooser.Filter;

public class RegexFilter extends Filter {

    private final Pattern pattern;
    private final boolean applyToFolder;

    public RegexFilter(Filter filter, String regex, boolean applyToFolder) {
        super(filter);
        pattern = Pattern.compile(regex);
        this.applyToFolder = applyToFolder;
    }

    public RegexFilter(String regex, boolean applyToFolder) {
        this(null, regex, applyToFolder);
    }

    public RegexFilter(String regex) {
        this(regex, false);
    }

    @Override
    public boolean accept(File file) {
        if (!applyToFolder && file.isDirectory()) {
            return true;
        }

        return super.accept(file) && pattern.matcher(file.getName()).matches();
    }
}
