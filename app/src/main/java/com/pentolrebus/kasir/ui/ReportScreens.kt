package com.pentolrebus.kasir.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pentolrebus.kasir.R
import com.pentolrebus.kasir.domain.*
import com.pentolrebus.kasir.util.BluetoothPrinter
import com.pentolrebus.kasir.util.QrisProof
import com.pentolrebus.kasir.util.ReportExport
import java.util.Calendar

private const val DAY = 86_400_000L

@Composable
fun rememberDatePicker(onPick: (Long) -> Unit): () -> Unit {
    val ctx = LocalContext.current
    return {
        val c = Calendar.getInstance()
        android.app.DatePickerDialog(ctx, { _, y, m, d ->
            onPick(Calendar.getInstance().apply { set(y, m, d, 0, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis)
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }
}

private fun inRange(ms: Long, from: Long, to: Long) = ms >= from && ms < to
private fun List<Transaction>.sales() = sumOf { it.total }
private fun List<Transaction>.cash() = filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.total }
private fun List<Transaction>.qris() = filter { it.paymentMethod == PaymentMethod.QRIS }.sumOf { it.total }

@Composable
private fun DayFilter(day: Long, onDay: (Long) -> Unit) {
    val today = startOfDay(System.currentTimeMillis())
    val pick = rememberDatePicker(onDay)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Hari ini" to (day == today), "Kemarin" to (day == today - DAY), (if (day != today && day != today - DAY) fmtDate(day) else "Pilih tanggal") to (day != today && day != today - DAY)).forEach { (label, selected) ->
            Surface(Modifier.height(38.dp).kRoundedClickable(RoundedCornerShape(20.dp)) { when(label) { "Hari ini" -> onDay(today); "Kemarin" -> onDay(today - DAY); else -> pick() } }, shape = RoundedCornerShape(20.dp), color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface) { Box(Modifier.padding(horizontal = 15.dp), contentAlignment = Alignment.Center) { Text(label, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold) } }
        }
    }
}

private fun nameOf(uid: String, workers: List<Worker>, session: Session) =
    if (uid == session.uid) session.username else workers.firstOrNull { it.id == uid }?.displayName?.ifBlank { null } ?: workers.firstOrNull { it.id == uid }?.username ?: "Kasir"

// ------------------------------------------------------------------ Laporan (tab)
@Composable
fun ReportsScreen(vm: PosViewModel, session: Session, nav: Nav) {
    val tx by vm.transactions.collectAsState()
    val ex by vm.expenses.collectAsState()
    val shifts by vm.shifts.collectAsState()
    val outlets by vm.outlets.collectAsState()
    val workers by vm.workers.collectAsState()
    val shift by vm.shift.collectAsState()
    val owner = session.role == Role.OWNER
    val tabs = if (owner) listOf("Ringkasan", "Outlet", "Kasir", "Harian", "Bulanan") else listOf("Hari ini", "Riwayat shift")
    var tab by remember { mutableStateOf(0) }
    var day by remember { mutableStateOf(startOfDay(System.currentTimeMillis())) }
    val today = startOfDay(System.currentTimeMillis())
    val myTx = if (owner) tx else tx.filter { it.cashierUid == session.uid }
    KPage(if (owner) "Ringkasan Bisnis" else "Laporan", null, subtitle = if (owner) "Owner · semua outlet" else "Kasir · ${outlets.firstOrNull()?.name ?: session.username}") {
        KChips(tabs, tabs[tab]) { t -> val i = tabs.indexOf(t); if (!owner && i == 1) nav.open("Riwayat Shift", null) else tab = i }
        if (!owner) {
            val td = myTx.filter { inRange(it.createdAt, today, today + DAY) }
            KHero("Penjualan hari ini", rp(td.sales()))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KStat("Transaksi", "${td.size}", Modifier.weight(1f)); KStat("Shift", if (shift == null) "Tidak aktif" else "Aktif", Modifier.weight(1f)) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KStat("Cash", rp(td.cash()), Modifier.weight(1f)); KStat("QRIS", rp(td.qris()), Modifier.weight(1f)) }
            if (shift != null) KItem("Detail shift aktif", "Mulai ${fmtTime(shift!!.startAt)} · ${shift!!.transactionCount} transaksi", onClick = { nav.open("Shift:detail", "active") })
            KLabel("Riwayat transaksi")
            if (td.isEmpty()) KEmpty("Belum ada transaksi", "Transaksi yang selesai akan muncul di sini.", R.drawable.ic_report)
            td.take(5).forEach { TxItem(it, nav) }
            if (td.isNotEmpty()) KSecondary("LIHAT SEMUA RIWAYAT") { nav.open("Riwayat Transaksi", null) }
            return@KPage
        }
        when (tab) {
            0 -> {
                val td = tx.filter { inRange(it.createdAt, today, today + DAY) }
                val exp = ex.filter { inRange(it.createdAt, today, today + DAY) }.sumOf { it.amount }
                KHero("Penjualan hari ini", rp(td.sales()))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KStat("Pengeluaran", rp(exp), Modifier.weight(1f).clickableCompat { nav.open("Pengeluaran", null) }, MaterialTheme.colorScheme.error)
                    KStat("Laba bersih", rp(td.sales() - exp), Modifier.weight(1f).clickableCompat { nav.open("Keuangan", null) }, MaterialTheme.colorScheme.primary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KStat("Transaksi", "${td.size}", Modifier.weight(1f)); KStat("Shift aktif", "${shifts.count { it.closedAt == null }}", Modifier.weight(1f)) }
                KCard {
                    Text("Cash ${rp(td.cash())} · QRIS ${rp(td.qris())}", fontWeight = FontWeight.SemiBold)
                    val paymentTotal = (td.cash() + td.qris()).coerceAtLeast(0)
                    val cashProgress = if (paymentTotal > 0L) td.cash().toFloat() / paymentTotal.toFloat() else 0f
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { cashProgress.coerceIn(0f, 1f) }, Modifier.fillMaxWidth().height(8.dp))
                }
                KLabel("Per outlet")
                outlets.forEach { o ->
                    val ot = td.filter { it.outletId == o.id }
                    KCard(Modifier.kRoundedClickable(RoundedCornerShape(16.dp)) { nav.open("Outlet:detail", o.id) }) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(o.name, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                                Text("${ot.size} transaksi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(rp(ot.sales()), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
                Surface(
                    Modifier.fillMaxWidth().kRoundedClickable(RoundedCornerShape(20.dp)) { nav.open("Keuangan", null) },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Laporan Keuangan", fontSize = 17.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold)
                            Text("Ringkasan pendapatan, pengeluaran, dan laba", fontSize = 14.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(painterResource(R.drawable.ic_chevron), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                    }
                }
            }
            1 -> {
                val td = tx.filter { inRange(it.createdAt, today, today + DAY) }
                outlets.forEach { o ->
                    val ot = td.filter { it.outletId == o.id }; val oe = ex.filter { it.outletId == o.id && inRange(it.createdAt, today, today + DAY) }.sumOf { it.amount }
                    KCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(o.name, fontWeight = FontWeight.ExtraBold); Text(rp(ot.sales()), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary) }
                        Text("Cash ${rp(ot.cash())} · QRIS ${rp(ot.qris())}", style = MaterialTheme.typography.bodySmall)
                        Text("Pengeluaran ${rp(oe)} · Laba ${rp(ot.sales() - oe)}", style = MaterialTheme.typography.bodySmall)
                        HorizontalDivider(Modifier.padding(vertical = 6.dp))
                        Text("Per kasir", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        ot.groupBy { it.cashierUid }.forEach { (uid, l) -> Text("${nameOf(uid, workers, session)} ${rp(l.sales())}", style = MaterialTheme.typography.bodySmall) }
                    }
                }
                if (outlets.isEmpty()) KEmpty("Belum ada outlet", "Tambahkan outlet di Pengaturan.", R.drawable.ic_store)
            }
            2 -> {
                val td = tx.filter { inRange(it.createdAt, today, today + DAY) }
                if (td.isEmpty()) KEmpty("Belum ada penjualan hari ini", "Performa kasir tampil setelah ada transaksi.", R.drawable.ic_people)
                td.groupBy { it.cashierUid }.forEach { (uid, l) ->
                    KItem(nameOf(uid, workers, session), "${l.size} transaksi" + if (shifts.any { it.cashierUid == uid && it.closedAt == null }) " · shift aktif" else "", rp(l.sales()), onClick = { if (workers.any { it.id == uid }) nav.open("Pekerja:detail", uid) })
                }
            }
            3 -> {
                DayFilter(day) { day = it }
                val td = tx.filter { inRange(it.createdAt, day, day + DAY) }
                val exp = ex.filter { inRange(it.createdAt, day, day + DAY) }.sumOf { it.amount }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KStat("Penjualan", rp(td.sales()), Modifier.weight(1f)); KStat("Transaksi", "${td.size}", Modifier.weight(1f)) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KStat("Cash", rp(td.cash()), Modifier.weight(1f)); KStat("QRIS", rp(td.qris()), Modifier.weight(1f)) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KStat("Pengeluaran", rp(exp), Modifier.weight(1f).clickableCompat { nav.open("Pengeluaran", null) }, MaterialTheme.colorScheme.error)
                    KStat("Laba bersih", rp(td.sales() - exp), Modifier.weight(1f).clickableCompat { nav.open("Keuangan", null) }, MaterialTheme.colorScheme.primary)
                }
            }
            else -> {
                val m0 = startOfMonth(today)
                val cal = Calendar.getInstance().apply { timeInMillis = m0; add(Calendar.MONTH, -1) }
                val p0 = cal.timeInMillis
                val cur = tx.filter { it.createdAt >= m0 }; val prev = tx.filter { inRange(it.createdAt, p0, m0) }
                val exp = ex.filter { it.createdAt >= m0 }.sumOf { it.amount }
                Text(fmtMonth(m0), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                KHero("Penjualan bulan ini", rp(cur.sales()))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KStat("Transaksi", "${cur.size}", Modifier.weight(1f)); KStat("Cash", rp(cur.cash()), Modifier.weight(1f)) }
                KCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("QRIS ${rp(cur.qris())}", fontWeight = FontWeight.SemiBold)
                        val pct = if (prev.sales() > 0) ((cur.sales() - prev.sales()) * 100 / prev.sales()) else null
                        Text(if (pct == null) "Belum ada data bulan lalu" else (if (pct >= 0) "▲ " else "▼ ") + Math.abs(pct) + "% vs bulan lalu", color = if ((pct ?: 0) >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KStat("Pengeluaran", rp(exp), Modifier.weight(1f).clickableCompat { nav.open("Pengeluaran", null) }, MaterialTheme.colorScheme.error)
                    KStat("Laba bersih", rp(cur.sales() - exp), Modifier.weight(1f).clickableCompat { nav.open("Keuangan", null) }, MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

private fun Modifier.clickableCompat(onClick: () -> Unit): Modifier = this.kRoundedClickable(RoundedCornerShape(12.dp), onClick = onClick)

@Composable
private fun TxItem(t: Transaction, nav: Nav) {
    KItem("${shortId(t.transactionId)} · ${fmtTime(t.createdAt)}", (if (t.paymentMethod == PaymentMethod.QRIS) "QRIS" else "Cash") + " · " + syncLabel(t.syncStatus), rp(t.total), onClick = { nav.open("Transaksi:detail", t.transactionId) })
}

// ------------------------------------------------------------------ Riwayat & detail
@Composable
fun TransactionHistoryScreen(vm: PosViewModel, session: Session, nav: Nav) {
    val tx by vm.transactions.collectAsState()
    var day by remember { mutableStateOf(startOfDay(System.currentTimeMillis())) }
    val list = (if (session.role == Role.OWNER) tx else tx.filter { it.cashierUid == session.uid }).filter { inRange(it.createdAt, day, day + DAY) }
    KPage("Riwayat Transaksi", nav) {
        DayFilter(day) { day = it }
        if (list.isEmpty()) KEmpty("Belum ada transaksi", "Tidak ada transaksi pada tanggal ini.", R.drawable.ic_report)
        list.forEach { TxItem(it, nav) }
    }
}

@Composable
fun TransactionDetailScreen(vm: PosViewModel, id: String?, printer: BluetoothPrinter, nav: Nav) {
    val tx by vm.transactions.collectAsState()
    val workers by vm.workers.collectAsState()
    val t = tx.firstOrNull { it.transactionId == id }
    var print by remember { mutableStateOf(false) }
    KPage(if (t != null) "Transaksi ${shortId(t.transactionId)}" else "Detail Transaksi", nav) {
        if (t == null) { KEmpty("Transaksi tidak ditemukan", "Data mungkin belum tersinkron.", R.drawable.ic_report); return@KPage }
        AssistChip(onClick = {}, label = { Text("● Lunas · " + if (t.paymentMethod == PaymentMethod.QRIS) "QRIS" else "Cash") })
        KCard {
            KRow("Waktu", fmtDateTime(t.createdAt))
            KRow("Kasir", workers.firstOrNull { it.id == t.cashierUid }?.displayName ?: "—")
            KRow("Sinkronisasi", syncLabel(t.syncStatus))
        }
        KCard { t.items.forEach { KRow("${it.quantity} × ${it.name}", rp(it.subtotal)) } }
        KCard {
            KRow("Subtotal", rp(t.subtotal)); KRow("Diskon", rp(t.discount)); KRow("Pajak ${t.taxPercent}%", rp(t.tax)); KRow("Total", rp(t.total), true)
            if (t.paymentMethod == PaymentMethod.CASH && t.cashReceived > 0) { KRow("Diterima", rp(t.cashReceived)); KRow("Kembalian", rp((t.cashReceived - t.total).coerceAtLeast(0))) }
        }
        if (t.paymentMethod == PaymentMethod.QRIS) KSecondary("LIHAT BUKTI QRIS") { nav.open("Bukti:QRIS", t.transactionId) }
        KPrimary("CETAK ULANG STRUK") { print = true }
    }
    if (print && t != null) PrintDialog(printer, t) { print = false }
}

@Composable
fun QrisProofScreen(vm: PosViewModel, id: String?, nav: Nav) {
    val tx by vm.transactions.collectAsState()
    val t = tx.firstOrNull { it.transactionId == id }
    val ctx = LocalContext.current
    val bmp = remember(t?.qrisProofPath) { t?.qrisProofPath?.let { QrisProof.load(ctx, it) } }
    KPage("Bukti QRIS · ${t?.let { shortId(it.transactionId) } ?: ""}", nav) {
        if (bmp != null) {
            Image(bmp.asImageBitmap(), "Bukti QRIS", Modifier.fillMaxWidth().heightIn(max = 420.dp))
            Text("● Foto tersedia", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        } else {
            KEmpty(if (t?.qrisProofPath != null) "Foto tidak dapat dimuat" else "Belum ada foto", "Bukti bersifat opsional dan hanya pendukung. Foto bukti diambil setelah transaksi QRIS selesai, di layar Transaksi Berhasil.", R.drawable.ic_qris)
        }
        t?.qrisProofPath?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

// ------------------------------------------------------------------ Shift
@Composable
fun ShiftHistoryScreen(vm: PosViewModel, session: Session, nav: Nav) {
    val shifts by vm.shifts.collectAsState()
    val list = if (session.role == Role.OWNER) shifts else shifts.filter { it.cashierUid == session.uid }
    KPage("Riwayat Shift", nav) {
        if (list.isEmpty()) KEmpty("Belum ada shift", "Shift yang dimulai akan tampil di sini.", R.drawable.ic_report)
        list.forEach { s -> KItem(fmtDate(s.startAt) + " · " + fmtTime(s.startAt) + " – " + (s.closedAt?.let { fmtTime(it) } ?: "aktif"), "${s.transactionCount} transaksi · " + syncLabel(s.syncStatus), rp(s.cashTotal + s.qrisTotal), onClick = { nav.open("Shift:detail", s.id) }) }
    }
}

@Composable
fun ShiftDetailScreen(vm: PosViewModel, id: String?, nav: Nav, onClose: () -> Unit) {
    val active by vm.shift.collectAsState()
    val shifts by vm.shifts.collectAsState()
    val s = if (id == "active") active else shifts.firstOrNull { it.id == id } ?: active
    KPage("Detail Shift", nav) {
        if (s == null) { KEmpty("Shift tidak ditemukan", "", R.drawable.ic_report); return@KPage }
        AssistChip(onClick = {}, label = { Text(if (s.closedAt == null) "● Shift Aktif · ${fmtTime(s.startAt)}" else "Shift selesai") })
        KCard {
            KRow("Mulai", fmtDateTime(s.startAt)); s.closedAt?.let { KRow("Selesai", fmtDateTime(it)) }
            KRow("Kas awal", rp(s.openingCash)); KRow("Transaksi", "${s.transactionCount}")
            KRow("Penjualan Cash", rp(s.cashTotal)); KRow("Penjualan QRIS", rp(s.qrisTotal)); KRow("Total penjualan", rp(s.cashTotal + s.qrisTotal), true)
            KRow("Kas seharusnya", rp(s.openingCash + s.cashTotal))
            s.closingCash?.let { KRow("Kas akhir", rp(it)); KRow("Selisih", rp(it - (s.openingCash + s.cashTotal)), valueColor = if (it == s.openingCash + s.cashTotal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
        }
        if (s.closedAt == null && s.id == active?.id) KPrimary("TUTUP SHIFT", onClick = onClose)
    }
}

@Composable
fun CloseShiftDialogFull(shift: Shift?, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    var v by remember { mutableStateOf("") }
    val expected = (shift?.openingCash ?: 0) + (shift?.cashTotal ?: 0)
    val closing = v.toLongOrNull()
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Tutup Shift") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (shift != null) {
                    KRow("Waktu mulai", fmtTime(shift.startAt)); KRow("Kas awal", rp(shift.openingCash)); KRow("Penjualan Cash", rp(shift.cashTotal))
                    KRow("Penjualan QRIS", rp(shift.qrisTotal)); KRow("Jumlah transaksi", "${shift.transactionCount}"); KRow("Kas seharusnya", rp(expected), true)
                }
                KField("Kas akhir (hitung uang di laci)", v, { v = it }, number = true)
                if (closing != null) KRow("Selisih", rp(closing - expected), valueColor = if (closing == expected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { Button(onClick = { onConfirm(closing ?: 0) }, enabled = closing != null) { Text("TUTUP SHIFT") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("BATAL") } })
}

// ------------------------------------------------------------------ Pengeluaran
@Composable
fun ExpenseListScreen(vm: PosViewModel, nav: Nav) {
    val ex by vm.expenses.collectAsState()
    var period by remember { mutableStateOf("Hari ini") }
    val today = startOfDay(System.currentTimeMillis())
    val from = if (period == "Hari ini") today else startOfMonth(today)
    val list = ex.filter { it.createdAt >= from }
    KPage("Pengeluaran", nav, fab = { nav.open("Pengeluaran:form", null) }) {
        KChips(listOf("Hari ini", "Bulan ini"), period) { period = it }
        KCard(color = MaterialTheme.colorScheme.primaryContainer) { Text("Total pengeluaran · $period", style = MaterialTheme.typography.labelMedium); Text(rp(list.sumOf { it.amount }), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.error) }
        if (list.isEmpty()) KEmpty("Belum ada pengeluaran", "Catat biaya bahan atau operasional.", R.drawable.ic_report)
        list.forEach { e -> KItem(e.note.ifBlank { e.category }, "${e.category} · ${fmtDateTime(e.createdAt)}" + if (e.syncStatus != SyncStatus.SYNCED) " · ${syncLabel(e.syncStatus)}" else "", rp(e.amount), MaterialTheme.colorScheme.error, onClick = { nav.open("Pengeluaran:detail", e.id) }) }
        if (list.isNotEmpty()) {
            KLabel("Per kategori")
            val total = list.sumOf { it.amount }.coerceAtLeast(1)
            KCard { list.groupBy { it.category }.forEach { (c, l) -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(c); Text(rp(l.sumOf { it.amount }), fontWeight = FontWeight.SemiBold) }; LinearProgressIndicator(progress = { l.sumOf { it.amount }.toFloat() / total }, Modifier.fillMaxWidth().padding(bottom = 8.dp)) } }
        }
    }
}

@Composable
fun ExpenseFormScreen(vm: PosViewModel, id: String?, nav: Nav) {
    val ex by vm.expenses.collectAsState()
    val old = ex.firstOrNull { it.id == id }
    var amount by remember { mutableStateOf(old?.amount?.toString() ?: "") }
    var cat by remember { mutableStateOf(old?.category ?: EXPENSE_CATEGORIES.first()) }
    var note by remember { mutableStateOf(old?.note ?: "") }
    var confirm by remember { mutableStateOf(false) }
    KPage(if (old == null) "Tambah Pengeluaran" else "Ubah Pengeluaran", nav) {
        KField("Nominal (Rp)", amount, { amount = it }, number = true)
        KLabel("Kategori"); KChips(EXPENSE_CATEGORIES, cat) { cat = it }
        KField("Catatan", note, { note = it })
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KSecondary("BATAL", modifier = Modifier.weight(1f), onClick = nav.back)
            KPrimary("SIMPAN", enabled = (amount.toLongOrNull() ?: 0) > 0, modifier = Modifier.weight(1f)) { vm.saveExpense(amount.toLong(), cat, note, old?.id); nav.back() }
        }
        if (old != null) KSecondary("HAPUS PENGELUARAN", danger = true) { confirm = true }
    }
    if (confirm) KConfirm("Hapus pengeluaran?", "Data ini akan dihapus.", "HAPUS", true, { confirm = false }) { vm.deleteExpense(old!!.id); confirm = false; nav.back() }
}

@Composable
fun ExpenseDetailScreen(vm: PosViewModel, id: String?, nav: Nav) {
    val ex by vm.expenses.collectAsState()
    val e = ex.firstOrNull { it.id == id }
    var confirm by remember { mutableStateOf(false) }
    KPage("Detail Pengeluaran", nav) {
        if (e == null) { KEmpty("Tidak ditemukan", "", R.drawable.ic_report); return@KPage }
        KCard(color = MaterialTheme.colorScheme.primaryContainer) { Text("Nominal", style = MaterialTheme.typography.labelMedium); Text(rp(e.amount), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.error) }
        KCard { KRow("Kategori", e.category); KRow("Tanggal", fmtDateTime(e.createdAt)); KRow("Dicatat oleh", e.createdByName.ifBlank { "—" }); KRow("Catatan", e.note.ifBlank { "—" }); KRow("Sinkronisasi", syncLabel(e.syncStatus)) }
        KPrimary("UBAH PENGELUARAN") { nav.open("Pengeluaran:form", e.id) }
        KSecondary("HAPUS", danger = true) { confirm = true }
    }
    if (confirm && e != null) KConfirm("Hapus pengeluaran?", "Data ini akan dihapus.", "HAPUS", true, { confirm = false }) { vm.deleteExpense(e.id); confirm = false; nav.back() }
}

// ------------------------------------------------------------------ Laporan keuangan
@Composable
fun FinancialReportScreen(vm: PosViewModel, nav: Nav) {
    val tx by vm.transactions.collectAsState()
    val ex by vm.expenses.collectAsState()
    val ctx = LocalContext.current
    var period by remember { mutableStateOf("Hari ini") }
    var msg by remember { mutableStateOf<String?>(null) }
    val today = startOfDay(System.currentTimeMillis())
    val from = if (period == "Hari ini") today else startOfMonth(today)
    val to = if (period == "Hari ini") today + DAY else startOfMonth(today + 32L * DAY)
    val t = tx.filter { inRange(it.createdAt, from, to) }
    val e = ex.filter { inRange(it.createdAt, from, to) }
    val gross = t.sumOf { it.subtotal }
    val disc = t.sumOf { it.discount }
    val tax = t.sumOf { it.tax }
    val net = t.sales()
    val exp = e.sumOf { it.amount }
    val cash = t.cash()
    val qris = t.qris()
    val categories = e.groupBy { it.category }.entries.sortedBy { it.key }.joinToString(" · ") { it.key }

    KPage("Laporan Keuangan", nav, subtitle = if (period == "Hari ini") fmtDate(today) else fmtMonth(today)) {
        KChips(listOf("Hari ini", "Bulan ini"), period) { period = it; msg = null }

        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                FinancialRow("Penjualan kotor", rp(gross))
                FinancialRow("Diskon", rp(disc))
                FinancialRow("Pajak", rp(tax))
                FinancialRow("Penjualan bersih", rp(net))
                FinancialRow("Pengeluaran", "- " + rp(exp))
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                FinancialRow("Laba bersih", rp(net - exp), emphasized = true, valueColor = Color(0xFF3DD68C))
            }
        }

        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                Text("Pembayaran masuk", fontSize = 17.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FinancialRow("Cash", rp(cash))
                FinancialRow("QRIS", rp(qris))
            }
        }

        Surface(
            Modifier.fillMaxWidth().kRoundedClickable(RoundedCornerShape(20.dp)) { nav.open("Pengeluaran", null) },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Pengeluaran per kategori", fontSize = 17.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold)
                    Text(categories.ifBlank { "Belum ada" }, fontSize = 14.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
                Icon(painterResource(R.drawable.ic_chevron), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp).padding(start = 4.dp))
            }
        }

        KPrimary("UNDUH LAPORAN") {
            msg = ReportExport.saveXlsx(ctx, "Laporan_Keuangan", period, gross, disc, tax, net, exp, net - exp, cash, qris, t)
                ?.let { "Tersimpan: $it" } ?: "Gagal menyimpan laporan"
        }
        msg?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun FinancialRow(label: String, value: String, emphasized: Boolean = false, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = if (emphasized) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = if (emphasized) 18.sp else 17.sp, lineHeight = if (emphasized) 22.sp else 21.sp, fontWeight = if (emphasized) FontWeight.ExtraBold else FontWeight.Normal)
        Text(value, color = valueColor, fontSize = if (emphasized) 20.sp else 16.sp, lineHeight = if (emphasized) 24.sp else 20.sp, fontWeight = if (emphasized) FontWeight.ExtraBold else FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End, modifier = Modifier.padding(start = 12.dp))
    }
}
