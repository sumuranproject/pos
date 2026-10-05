package com.pentolrebus.kasir.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

object ReportExport {
    /** Writes a CSV into Downloads (MediaStore on API 29+, app-specific folder before). Returns a display path or null. */
    fun saveCsv(context: Context, baseName: String, rows: List<List<String>>): String? = runCatching {
        val name = "$baseName-${System.currentTimeMillis()}.csv"
        val text = rows.joinToString("\n") { r -> r.joinToString(",") { "\"" + it.replace("\"", "\"\"") + "\"" } }
        if (Build.VERSION.SDK_INT >= 29) {
            val v = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name); put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Kasir")
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v) ?: error("insert gagal")
            context.contentResolver.openOutputStream(uri)!!.use { it.write(text.toByteArray()) }
            "Download/Kasir/$name"
        } else {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Kasir").apply { mkdirs() }
            File(dir, name).also { it.writeText(text) }.absolutePath
        }
    }.getOrNull()
}
