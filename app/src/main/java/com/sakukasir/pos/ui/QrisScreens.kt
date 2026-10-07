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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun QrisScreen(vm: PosViewModel, onHome: () -> Unit) {
    val c = Sk.c
    val set by vm.settings.collectAsState()
    val toast = LocalToast.current
    val context = LocalContext.current
    val pickQr = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            vm.updateSettings(set.copy(qrisImageUri = uri.toString()))
            toast("QRIS berhasil diunggah", "success")
        }
    }
    var days by remember(set.qrisRetentionDays) { mutableStateOf(set.qrisRetentionDays.toString()) }
    Screen {
        PageHead("QRIS", "Pengaturan pembayaran QR") { BackHome(onHome) }
        Column(Modifier.fillMaxWidth().padding(bottom = 16.dp).skCard().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val qrBitmap = remember(set.qrisImageUri) {
                set.qrisImageUri?.let { uri -> runCatching { context.contentResolver.openInputStream(Uri.parse(uri))?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull() }
            }
            Box(Modifier.fillMaxWidth().height(180.dp).clip(RSm).border(1.dp, c.borderStrong, RSm), contentAlignment = Alignment.Center) {
                if (qrBitmap != null) Image(qrBitmap, "QRIS ${set.qrisOutlet}", Modifier.fillMaxSize().padding(12.dp))
                else Txt("Belum ada gambar QRIS", 13, color = c.textMuted)
            }
            SwitchRow("Aktifkan QRIS", "Muncul sebagai metode bayar di POS", set.qrisEnabled, { vm.updateSettings(set.copy(qrisEnabled = it)) }, pad = 0)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Btn(if (set.qrisImageUri == null) "Upload QR" else "Ganti QR", { pickQr.launch(arrayOf("image/*")) }, Modifier.weight(1f), kind = 1)
                if (set.qrisImageUri != null) Btn("Hapus", { vm.updateSettings(set.copy(qrisImageUri = null)); toast("QRIS dihapus", "success") }, Modifier.weight(1f), kind = 2)
            }
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
