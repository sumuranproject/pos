package com.pentolrebus.kasir.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** Local persistence for user-supplied UI assets such as the outlet QRIS image. */
class UiAssetStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("saku_kasir_ui_assets", Context.MODE_PRIVATE)

    fun importImage(uri: Uri, prefix: String): String? = runCatching {
        val dir = File(context.filesDir, "ui-assets").apply { mkdirs() }
        val file = File(dir, "${prefix}_${UUID.randomUUID()}.img")
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "File gambar tidak dapat dibaca" }
            FileOutputStream(file).use { output -> input.copyTo(output) }
        }
        file.absolutePath
    }.getOrNull()

    fun setQrImage(path: String?) {
        val old = prefs.getString(KEY_QR, null)
        if (old != null && old != path) File(old).delete()
        prefs.edit().putString(KEY_QR, path).apply()
    }

    fun qrImage(): String? = prefs.getString(KEY_QR, null)?.takeIf { File(it).exists() }

    fun clearQrImage() {
        qrImage()?.let { File(it).delete() }
        prefs.edit().remove(KEY_QR).apply()
    }

    fun qrisEnabled(): Boolean = prefs.getBoolean(KEY_QRIS, false)
    fun setQrisEnabled(enabled: Boolean) { prefs.edit().putBoolean(KEY_QRIS, enabled).apply() }

    fun printerAddress(): String? = prefs.getString(KEY_PRINTER_ADDRESS, null)
    fun printerName(): String? = prefs.getString(KEY_PRINTER_NAME, null)
    fun setPrinter(address: String, name: String) {
        prefs.edit().putString(KEY_PRINTER_ADDRESS, address).putString(KEY_PRINTER_NAME, name).apply()
    }
    fun clearPrinter() { prefs.edit().remove(KEY_PRINTER_ADDRESS).remove(KEY_PRINTER_NAME).apply() }

    companion object {
        private const val KEY_QR = "qris_image"
        private const val KEY_QRIS = "qris_enabled"
        private const val KEY_PRINTER_ADDRESS = "printer_address"
        private const val KEY_PRINTER_NAME = "printer_name"
    }
}
