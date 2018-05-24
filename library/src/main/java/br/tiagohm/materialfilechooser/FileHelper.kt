package br.tiagohm.materialfilechooser

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import org.ocpsoft.prettytime.PrettyTime
import java.io.File
import java.io.FileFilter
import java.util.*


private object TimeFormatter : PrettyTime()

val File.isFolder: Boolean get() = this.isDirectory

val File.size: Long
    get() {
        return if (this.isFolder) {
            var sizeInBytes = 0L
            // Scroll through all files and folders.
            this.listFiles()?.forEach {
                // Only add the the file size
                sizeInBytes += if (it.isFile) it.length() else 0
            }
            sizeInBytes
        } else {
            this.length()
        }
    }

internal fun Long.toSizeString(): String {
    return when {
        this >= 1024 * 1024 * 1024 -> String.format("%.1f", this / (1024 * 1024 * 1024).toFloat()) + " GB"
        this >= 1024 * 1024 -> String.format("%.1f", this / (1024 * 1024).toFloat()) + " MB"
        this >= 1024 -> String.format("%.1f", this / 1024f) + " KB"
        else -> this.toString() + " B"
    }
}

val File.sizeAsString: String get() = this.size.toSizeString()

val File.lastModified: String get() = TimeFormatter.format(Date(this.lastModified()))

val File.count: Int get() = if (this.isFolder) this.listFiles()?.size ?: 0 else 0

fun File.count(filter: FileFilter): Int = if (this.isFolder) this.listFiles(filter)?.size
        ?: 0 else 0

val File.isProtected: Boolean get() = !this.canRead() || !this.canWrite()

fun File.isImage(context: Context): Boolean = this.mimeType(context).startsWith("image")

fun File.mimeType(context: Context): String {
    val uri = Uri.fromFile(this)
    return if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
        val cr = context.contentResolver
        cr.getType(uri)
    } else {
        val fileExtension: String? = MimeTypeMap.getFileExtensionFromUrl(uri
                .toString())
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                fileExtension?.toLowerCase()) ?: "Unknown"
    }
}