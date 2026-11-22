package uz.ozkey.keyboard

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File

/**
 * O'rnatilgan ÖZKEY APK faylini ulashish uchun kichik, read-only ContentProvider.
 * Tashqi ilovalar faqat FLAG_GRANT_READ_URI_PERMISSION bilan berilgan URI orqali o'qiydi.
 */
class ApkShareProvider : ContentProvider() {

    private fun apkFile(): File {
        val c = context ?: throw IllegalStateException("Context unavailable")
        return File(c.applicationInfo.sourceDir)
    }

    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String = "application/vnd.android.package-archive"

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw SecurityException("Read-only")
        return ParcelFileDescriptor.open(apkFile(), ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val f = apkFile()
        val cols = projection ?: arrayOf("_display_name", "_size")
        val c = MatrixCursor(cols, 1)
        c.addRow(cols.map {
            when (it) {
                "_display_name" -> f.name
                "_size" -> f.length()
                else -> null
            }
        }.toTypedArray())
        return c
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw UnsupportedOperationException("Read-only")

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = throw UnsupportedOperationException("Read-only")

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = throw UnsupportedOperationException("Read-only")
}
