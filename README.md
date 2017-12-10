# MaterialFileChooser

[![](https://jitpack.io/v/tiagohm/MaterialFileChooser.svg)](https://jitpack.io/#tiagohm/MaterialFileChooser)

![](https://raw.githubusercontent.com/tiagohm/MaterialFileChooser/master/1.png)

## Dependencies

Adicione ao seu projeto:
```gradle
	allprojects {
		repositories {
			...
			maven { url "https://jitpack.io" }
		}
	}
```
```gradle
	dependencies {
	        compile 'com.github.tiagohm:MaterialFileChooser:VERSION'
	}
```

## How to use
```java
new MaterialFileChooser(Context, "Title")
    .allowSelectFolder(false)
    .allowMultipleFiles(false)
    .allowCreateFolder(false)
    .showHiddenFiles(false)
    .showFoldersFirst(true)
    .showFolders(true)
    .showFiles(true)
    .initialFolder(Environment.getExternalStorageDirectory())
    .onFileChooserListener(new MaterialFileChooser.OnFileChooserListener() {
                        @Override
                        public void onItemSelected(List<File> files) {
                            //
                        }
                    })
    .show();
```

## Customization
```xml
<!-- Base application theme. -->
<style name="AppTheme" parent="Theme.AppCompat.Light.DarkActionBar">
    <!-- Customize your theme here. -->
    
    <item name="mfc_theme_background_color">@color/white</item>
    <item name="mfc_theme_foreground_color">@color/green</item>
	<!-- Search hint text, file size, file modification date, etc -->
    <item name="mfc_theme_information_color">@color/grey</item>
</style>
```

## License
```
The MIT License (MIT)

Copyright (c) 2017 Tiago Melo

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
