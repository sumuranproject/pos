package com.sakukasir.pos.ui

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

@Composable
fun QrisScreen(vm: PosViewModel, onHome: () -> Unit) {
    val c = Sk.c
    val set by vm.settings.collectAsState()
    val toast = LocalToast.current
    var days by remember(set.qrisRetentionDays) { mutableStateOf(set.qrisRetentionDays.toString()) }
    Screen {
        PageHead("QRIS", "Pengaturan pembayaran QR") { BackHome(onHome) }
        Column(Modifier.fillMaxWidth().padding(bottom = 16.dp).skCard().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.fillMaxWidth().height(80.dp).clip(RSm).border(1.dp, c.border, RSm), contentAlignment = Alignment.Center) { Txt("Belum ada gambar QRIS", 13, color = c.textMuted) }
            SwitchRow("Aktifkan QRIS", "Muncul sebagai metode bayar di POS", set.qrisEnabled, { vm.updateSettings(set.copy(qrisEnabled = it)) }, pad = 0)
            Btn("Upload QR", { toast("Upload gambar QR belum tersedia di build ini", "error") }, Modifier.fillMaxWidth(), kind = 1)
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
