package com.pentolrebus.kasir.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

/** Owner-supplied QRIS image, kept on this device only (no Firebase Storage). */
object QrisStore {
    private fun file(c: Context, outletId: String) = File(c.filesDir, "qris_${outletId.ifBlank { "default" }}.png")
    fun has(c: Context, outletId: String) = file(c, outletId).exists()
    fun load(c: Context, outletId: String): Bitmap? = runCatching { file(c, outletId).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.absolutePath) } }.getOrNull()
    fun importFrom(c: Context, outletId: String, uri: Uri): Boolean = runCatching {
        c.contentResolver.openInputStream(uri)!!.use { i -> file(c, outletId).outputStream().use { o -> i.copyTo(o) } }
        true
    }.getOrDefault(false)
    fun remove(c: Context, outletId: String) { file(c, outletId).delete() }
}
