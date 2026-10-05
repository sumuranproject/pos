package com.pentolrebus.kasir.util

import android.content.Context
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

enum class LogTag { APP, AUTH, REGISTRATION, LOGIN, RTDB, FCM, SYNC, PRINTER, QRIS, ERROR }

class Diagnostics(private val context: Context) {
    private val q = ConcurrentLinkedQueue<String>()
    private val file = File(context.filesDir, "kasir-diagnostics.log")

    fun log(tag: LogTag, message: String) {
        val line = "${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())} [$tag] $message"
        q.add(line)
        runCatching {
            file.appendText(line + "\n")
        }
    }

    fun recordUncaughtException(t: Throwable) {
        val sw = java.io.StringWriter()
        t.printStackTrace(java.io.PrintWriter(sw))
        log(LogTag.ERROR, "UNCAUGHT_EXCEPTION\n${sw}")
    }

    fun export(): File {
        val f = File(context.cacheDir, "kasir-diagnostic-${System.currentTimeMillis()}.txt")
        val persisted = runCatching { if (file.exists()) file.readText() else "" }.getOrDefault("")
        val current = q.joinToString("\n")
        f.writeText((persisted + (if (persisted.isNotBlank() && current.isNotBlank()) "\n" else "") + current).trimEnd() + "\n")
        return f
    }

    fun exportToDownloads(): String? {
        val name = "kasir-diagnostic-${System.currentTimeMillis()}.txt"
        val v = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Kasir")
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v) ?: return null
        val persisted = runCatching { if (file.exists()) file.readText() else "" }.getOrDefault("")
        val current = q.joinToString("\n")
        val content = (persisted + (if (persisted.isNotBlank() && current.isNotBlank()) "\n" else "") + current).trimEnd() + "\n"
        return runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) } ?: return@runCatching null
            name
        }.getOrNull()
    }
}
