package com.sakukasir.pos.util

import android.content.Context
import android.graphics.*
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

object QrisProof {
    suspend fun processCapture(context: Context, source: File, cashier: String, outlet: String, trxId: String, capturedAt: Long): com.sakukasir.pos.domain.QrisProof {
        val bitmap=BitmapFactory.decodeFile(source.absolutePath) ?: error("Foto kamera tidak dapat dibaca")
        val maxSide=1080
        val scale=min(1f,maxSide.toFloat()/maxOf(bitmap.width,bitmap.height))
        val resized=if(scale<1f)Bitmap.createScaledBitmap(bitmap,(bitmap.width*scale).toInt(),(bitmap.height*scale).toInt(),true) else bitmap
        val canvas=Canvas(resized);val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.WHITE;textSize=30f;setShadowLayer(4f,1f,1f,Color.BLACK)}
        val stamp=java.text.SimpleDateFormat("dd MMM yyyy · HH:mm:ss",java.util.Locale("id","ID")).format(java.util.Date(capturedAt))
        val lines=listOf(trxId,stamp,outlet,cashier)
        lines.forEachIndexed{i,t->canvas.drawText(t,24f,resized.height-120f+i*32f,paint)}
        val dir=File(context.filesDir,"qris_proofs").apply{mkdirs()};val out=File(dir,"$trxId-$capturedAt.webp")
        FileOutputStream(out).use{resized.compress(if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP,75,it)}
        if(resized!==bitmap)resized.recycle();bitmap.recycle();source.delete()
        val size=out.length()
        return com.sakukasir.pos.domain.QrisProof(out.absolutePath,capturedAt,Build.MANUFACTURER+" "+Build.MODEL,size,capturedAt+35L*24*60*60*1000)
    }
}
