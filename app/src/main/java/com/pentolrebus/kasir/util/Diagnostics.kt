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
    fun log(tag:LogTag, message:String) { q.add("${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())} [$tag] $message") }
    fun export(): File { val f=File(context.cacheDir,"kasir-diagnostic-${System.currentTimeMillis()}.txt"); f.writeText(q.joinToString("\n")); return f }
    fun exportToDownloads(): String? { val name="kasir-diagnostic-${System.currentTimeMillis()}.txt"; val v=ContentValues().apply{put(MediaStore.Downloads.DISPLAY_NAME,name);put(MediaStore.Downloads.MIME_TYPE,"text/plain");put(MediaStore.Downloads.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/Kasir")}; val uri=context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v)?:return null; context.contentResolver.openOutputStream(uri)?.use{it.write(q.joinToString("\n").toByteArray())}; return name }
}
