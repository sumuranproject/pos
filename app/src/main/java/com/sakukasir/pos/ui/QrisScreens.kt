package com.sakukasir.pos.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign

@Composable
fun QrisScreen(vm: PosViewModel, onHome: () -> Unit) {
    val c = Sk.c
    val set by vm.settings.collectAsState()
    val toast = LocalToast.current
    val context = LocalContext.current
    var qrUri by remember { mutableStateOf(QrisImageStore.get(context)) }
    val pickQr = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            QrisImageStore.set(context, uri.toString())
            qrUri = uri.toString()
            toast("Gambar QRIS disimpan", "success")
        }
    }
    var days by remember(set.qrisRetentionDays) { mutableStateOf(set.qrisRetentionDays.toString()) }
    Screen {
        PageHead("QRIS", "Pengaturan pembayaran QR") { BackHome(onHome) }
        Column(Modifier.fillMaxWidth().padding(bottom = 16.dp).skCard().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.fillMaxWidth().height(180.dp).clip(RSm).background(c.surfaceAlt).border(1.dp, c.border, RSm), contentAlignment = Alignment.Center) {
                val bitmap = remember(qrUri) { qrUri?.let { uri -> runCatching { context.contentResolver.openInputStream(Uri.parse(uri))?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull() } }
                if (bitmap != null) Image(bitmap, "QRIS toko", Modifier.fillMaxSize().padding(8.dp), contentScale = ContentScale.Fit)
                else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Txt("Belum ada gambar QRIS", 13, color = c.textMuted)
                    Txt("Unggah gambar QR toko", 11, color = c.textMuted, modifier = Modifier.padding(top = 4.dp))
                }
            }
            SwitchRow("Aktifkan QRIS", "Muncul sebagai metode bayar di POS", set.qrisEnabled, { vm.updateSettings(set.copy(qrisEnabled = it)) }, pad = 0)
            Btn(if (qrUri == null) "Upload QR" else "Ganti gambar QR", { pickQr.launch(arrayOf("image/*")) }, Modifier.fillMaxWidth(), kind = 1)
            if (qrUri != null) Btn("Hapus gambar QR", { QrisImageStore.set(context, null); qrUri = null; toast("Gambar QRIS dihapus", "success") }, Modifier.fillMaxWidth(), kind = 2)
        }
        Column(Modifier.fillMaxWidth().skCard().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.fillMaxWidth().clip(RSm).background(c.surfaceAlt).padding(12.dp)) {
                Txt("Retensi bukti QRIS", 12, FontWeight.SemiBold, c.textMuted)
                Txt("Bukti QRIS otomatis dihapus setelah ${set.qrisRetentionDays} hari. Data transaksi tetap tersimpan.", 12, color = c.textMuted, lineHeight = 1.55f, modifier = Modifier.padding(top = 2.dp))
            }
            Field("Retensi (hari)", days, { days = digits(it) }, keyboard = KeyboardType.Number)
            Btn("Simpan retensi", {
                val d = days.toIntOrNull() ?: 0
                if (d < 1) toast("Retensi minimal 1 hari", "error") else { vm.updateSettings(set.copy(qrisRetentionDays = d)); toast("Retensi disimpan", "success") }
            }, Modifier.fillMaxWidth())
        }
    }
}


/** Stores the merchant QR image URI locally; no cloud upload is implied. */
internal object QrisImageStore {
    private const val PREFS = "sakukasir_qris_image"
    private const val KEY_URI = "merchant_qr_uri"
    fun get(context: android.content.Context): String? =
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE).getString(KEY_URI, null)
    fun set(context: android.content.Context, uri: String?) {
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE).edit().apply {
            if (uri.isNullOrBlank()) remove(KEY_URI) else putString(KEY_URI, uri)
        }.apply()
    }
}

@Composable
internal fun QrisCodePreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val uri = QrisImageStore.get(context)
    val bitmap = remember(uri) {
        uri?.let { raw -> runCatching {
            context.contentResolver.openInputStream(Uri.parse(raw))?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
        }.getOrNull() }
    }
    if (bitmap != null) {
        Image(bitmap, "QR pembayaran toko", modifier, contentScale = ContentScale.Fit)
    } else {
        Box(modifier, contentAlignment = Alignment.Center) {
            Txt("QRIS belum diatur", 12, color = Sk.c.textMuted, align = TextAlign.Center)
        }
    }
}
