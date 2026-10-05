package com.pentolrebus.kasir.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/** Stores QRIS payment proof photos under Pictures/Kasir/QRIS/ (supporting evidence only). */
object QrisProof {
    private const val REL = "Pictures/Kasir/QRIS"

    fun save(context: Context, bmp: Bitmap, transactionId: String): String? = runCatching {
        val name = "QRIS_${transactionId.takeLast(8)}_${System.currentTimeMillis()}.jpg"
        if (Build.VERSION.SDK_INT >= 29) {
            val v = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, REL)
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v) ?: error("MediaStore insert gagal")
            context.contentResolver.openOutputStream(uri)!!.use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            "$REL/$name"
        } else {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Kasir/QRIS").apply { mkdirs() }
            val f = File(dir, name)
            f.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            f.absolutePath
        }
    }.getOrNull()

    fun load(context: Context, path: String): Bitmap? = runCatching {
        val name = path.substringAfterLast('/')
        if (Build.VERSION.SDK_INT >= 29) {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Images.Media._ID),
                "${MediaStore.Images.Media.DISPLAY_NAME}=?", arrayOf(name), null
            )?.use { c ->
                if (!c.moveToFirst()) return@runCatching null
                val uri = android.content.ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, c.getLong(0))
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }
        } else BitmapFactory.decodeFile(path)
    }.getOrNull()
}
