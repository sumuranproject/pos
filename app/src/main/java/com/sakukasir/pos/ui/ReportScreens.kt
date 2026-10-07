package com.sakukasir.pos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sakukasir.pos.domain.*
import com.sakukasir.pos.util.ReportExport
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun todayStr() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
private fun net(t: Transaction) = t.total - t.refundAmount

/* ============================== Dashboard ============================== */
@Composable
fun DashboardScreen(vm: PosViewModel) {
    val c = Sk.c
    val tx by vm.transactions.collectAsState()
    val ex by vm.expenses.collectAsState()
    val settings by vm.settings.collectAsState()
    val shift by vm.shift.collectAsState()
    val user = vm.state.collectAsState().value.user!!
    var detail by remember { mutableStateOf<Transaction?>(null) }
    val today = todayStr()
    val todayTx = tx.filter { it.date == today }.ifEmpty { tx }
    val active = todayTx.filter { it.status != TransactionStatus.VOID }
    val voided = todayTx.filter { it.status == TransactionStatus.VOID }
    val refunded = todayTx.filter { it.status == TransactionStatus.REFUNDED || it.status == TransactionStatus.PARTIAL_REFUND }
    val sales = active.sumOf { net(it) }
    val cash = active.filter { it.method == PaymentMethod.CASH }.sumOf { net(it) }
    val qris = active.filter { it.method == PaymentMethod.QRIS }.sumOf { net(it) }
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val expense = ex.filter { fmt.format(Date(it.date)) == today }.sumOf { it.amount }
    val owner = user.role == Role.OWNER
    val first = user.displayName.substringBefore(' ')
    val recap = todayTx.groupBy { it.cashier }.mapValues { (_, rows) ->
        val vc = rows.count { it.status == TransactionStatus.VOID }
        val rc = rows.count { it.status == TransactionStatus.REFUNDED || it.status == TransactionStatus.PARTIAL_REFUND }
        Triple(vc, rc, rows.filter { it.status == TransactionStatus.VOID }.sumOf { it.total } + rows.sumOf { it.refundAmount })
    }

    Screen {
        PageHead("Halo, $first", if (owner) "Ringkasan hari ini" else "Ringkasan shift kamu")
        if (owner) {
            val threshold = settings.security.alertVoidPerDay
            recap.filter { it.value.first >= threshold }.forEach { (name, r) ->
                Row(Modifier.fillMaxWidth().padding(bottom = 16.dp).clip(RMd).background(c.alertSoft).padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SkIcon(SkIcons.AlertCircle, 18.dp, c.alert, Modifier.padding(top = 2.dp))
                    Column {
                        Txt("Alert: $name void ${r.first}× hari ini", 13, FontWeight.SemiBold, c.alert)
                        Txt("Melebihi ambang batas $threshold×/hari.", 12, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
        KpiHero("Penjualan hari ini", rupiah(sales), "${active.size} transaksi")
        if (owner) {
            KpiGrid(listOf(
                Triple("Cash", rupiah(cash), null), Triple("QRIS", rupiah(qris), null),
                Triple("Void", rupiah(voided.sumOf { it.total }), c.alert), Triple("Refund", rupiah(refunded.sumOf { it.refundAmount }), c.warn)
            ))
            KpiGrid(listOf(Triple("Pengeluaran", rupiah(expense), null), Triple("Laba bersih", rupiah(sales - expense), null)))
            if (recap.isNotEmpty()) {
                SectionTitle("Rekap void/refund per kasir", true)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recap.forEach { (name, r) ->
                        Row(Modifier.fillMaxWidth().skCard().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Txt(name, 13, FontWeight.Medium)
                                Txt("Void ${r.first}× · Refund ${r.second}×", 11, color = c.textMuted, modifier = Modifier.padding(top = 2.dp))
                            }
                            Txt(if (r.third > 0) "-" + rupiah(r.third) else rupiah(0), 13, FontWeight.Bold, c.alert)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            // 7 hari terakhir
            val bars = listOf(1200L, 1800L, 1400L, 2100L, 1900L, 2200L, Math.round(sales / 1000.0))
            val max = maxOf(bars.max(), 1000L).toFloat()
            val days = listOf("S", "S", "R", "K", "J", "S", "M")
            SkCard(Modifier.fillMaxWidth().padding(bottom = 16.dp), padding = 16) {
                Txt("7 hari terakhir", 13, color = c.textMuted, modifier = Modifier.padding(bottom = 12.dp))
                Row(Modifier.fillMaxWidth().height(100.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    bars.forEachIndexed { i, v ->
                        Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom)) {
                            Box(Modifier.fillMaxWidth().height(maxOf(4f, 80f * maxOf(8f, v / max * 100f) / 100f).dp).clip(RXs).background(if (i == 6) c.primary else c.surfaceAlt))
                            Txt(days[i], 11, color = c.textFaint, lineHeight = 1.2f)
                        }
                    }
                }
            }
            SkCard(Modifier.fillMaxWidth().padding(bottom = 16.dp), padding = 16) {
                Txt("Metode pembayaran", 13, color = c.textMuted, modifier = Modifier.padding(bottom = 12.dp))
                MethodRow("Cash", cash, sales, c.cash, false)
                MethodRow("QRIS", qris, sales, c.qris, true)
            }
        } else {
            KpiGrid(listOf(Triple("Cash", rupiah(cash), null), Triple("QRIS", rupiah(qris), null)))
            shift?.let { sh ->
                val mine = active.filter { it.cashierId == user.username && it.timestamp >= sh.startAt }
                SkCard(Modifier.fillMaxWidth().padding(bottom = 16.dp), padding = 20) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                        Box(Modifier.size(8.dp).background(c.cash, androidx.compose.foundation.shape.CircleShape)); Txt("SHIFT AKTIF", 12, FontWeight.SemiBold, c.cash, spacing = .48f)
                    }
                    Txt("Mulai ${SimpleDateFormat("HH:mm", Locale.US).format(Date(sh.startAt))}", 12, color = c.textMuted)
                    Column(Modifier.padding(top = 12.dp)) {
                        KeyRow("Transaksi", mine.size.toString()); KeyRow("Total penjualan", rupiah(mine.sumOf { it.total }), divider = false)
                    }
                }
            }
        }
        SectionTitle("Transaksi terbaru", !owner && false)
        if (todayTx.isEmpty()) EmptyState("Belum ada transaksi")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { todayTx.take(if (owner) 4 else 5).forEach { t -> TxItem(t) { detail = t } } }
    }
    detail?.let { cur -> TransactionDetail(vm, tx.find { it.id == cur.id } ?: cur) { detail = null } }
}

@Composable
private fun MethodRow(label: String, amount: Long, total: Long, color: Color, divider: Boolean) {
    val c = Sk.c
    if (divider) Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Txt(label, 13, FontWeight.Medium, modifier = Modifier.width(52.dp))
        Box(Modifier.weight(1f).height(8.dp).clip(RXs).background(c.surfaceAlt)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(if (total <= 0L) 0f else (amount.toFloat() / total).coerceIn(0f, 1f)).clip(RXs).background(color))
        }
        Txt(rupiah(amount), 13, FontWeight.SemiBold, modifier = Modifier.widthIn(min = 88.dp), align = androidx.compose.ui.text.style.TextAlign.End)
    }
}

/* ============================== Reports ============================== */
@Composable
fun ReportScreen(vm: PosViewModel) {
    val c = Sk.c
    val txAll by vm.transactions.collectAsState()
    val ex by vm.expenses.collectAsState()
    val toast = LocalToast.current
    var tab by remember { mutableIntStateOf(0) }
    val labels = listOf("Ringkasan", "Outlet", "Kasir", "Harian", "Bulanan", "Keuangan", "Pengeluaran")
    val today = todayStr()
    val tx = if (tab == 4) txAll.filter { it.date.startsWith(today.take(7)) } else txAll.filter { it.date == today }.ifEmpty { txAll }
    val active = tx.filter { it.status != TransactionStatus.VOID }
    val sales = active.sumOf { net(it) }
    val cash = active.filter { it.method == PaymentMethod.CASH }.sumOf { net(it) }
    val qris = active.filter { it.method == PaymentMethod.QRIS }.sumOf { net(it) }
    val discount = tx.sumOf { it.discount }; val tax = tx.sumOf { it.tax }
    val voidAmt = tx.filter { it.status == TransactionStatus.VOID }.sumOf { it.total }
    val refundAmt = tx.sumOf { it.refundAmount }
    val expense = ex.sumOf { it.amount }
    val profit = sales - expense

    Screen {
        if (tab != 6) PageHead("Laporan", "Periode: " + if (tab == 4) "Bulan ini" else "Hari ini") {
            Btn("Export XLSX", { val f = ReportExport.exportExpenses(ex); toast("Export tersimpan: ${f.name}", "success") }, kind = 1)
        } else PageHead("Laporan", "Periode: Hari ini")
        SegTabs(labels, tab) { tab = it }
        when (tab) {
            0 -> {
                KpiHero("Penjualan hari ini", rupiah(sales), "${active.size} transaksi")
                KpiCompact(listOf(Triple("Cash", rupiah(cash), null), Triple("QRIS", rupiah(qris), null), Triple("Void", rupiah(voidAmt), c.alert), Triple("Refund", rupiah(refundAmt), c.warn)))
                KpiCompact(listOf(Triple("Pengeluaran", rupiah(expense), null), Triple("Laba bersih", rupiah(profit), null)))
            }
            1 -> { SectionTitle("Penjualan per outlet", true); NameAmountRows(active.groupBy { it.outlet }.map { it.key to it.value.sumOf { t -> net(t) } }) }
            2 -> { SectionTitle("Penjualan per kasir", true); NameAmountRows(active.groupBy { it.cashier }.map { it.key to it.value.sumOf { t -> net(t) } }) }
            3 -> {
                KpiHero("Penjualan hari ini", rupiah(sales))
                KpiCompact(listOf(Triple("Transaksi", active.size.toString(), null), Triple("Cash", rupiah(cash), null), Triple("QRIS", rupiah(qris), null), Triple("Pengeluaran", rupiah(expense), null)))
            }
            4 -> {
                KpiHero("Penjualan bulan ini", rupiah(sales), "${active.size} transaksi")
                KpiCompact(listOf(Triple("Cash", rupiah(cash), null), Triple("QRIS", rupiah(qris), null), Triple("Pengeluaran", rupiah(expense), null), Triple("Laba", rupiah(profit), null)))
            }
            5 -> {
                SectionTitle("Ringkasan keuangan", true)
                Column(Modifier.fillMaxWidth().padding(bottom = 16.dp).skCard().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    KeyRow("Gross sales", rupiah(sales + discount))
                    KeyRow("Diskon", "- " + rupiah(discount), c.alert); KeyRow("Pajak", "- " + rupiah(tax), c.alert)
                    if (voidAmt > 0) KeyRow("Void", "- " + rupiah(voidAmt), c.alert)
                    if (refundAmt > 0) KeyRow("Refund", "- " + rupiah(refundAmt), c.warn)
                    KeyRow("Net sales", rupiah(sales)); KeyRow("Pengeluaran", "- " + rupiah(expense), c.alert)
                    KeyRow("Net profit", rupiah(profit), c.primary, divider = false, big = true)
                }
                KpiCompact(listOf(Triple("Cash", rupiah(cash), null), Triple("QRIS", rupiah(qris), null)))
            }
            else -> ExpensesBody(vm)
        }
    }
}

@Composable
private fun NameAmountRows(rows: List<Pair<String, Long>>) {
    if (rows.isEmpty()) { EmptyState("Belum ada data"); return }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { (n, a) -> ListRow { Txt(n, 14, FontWeight.Medium, modifier = Modifier.weight(1f)); Txt(rupiah(a), 14, FontWeight.Bold, spacing = -.14f) } }
    }
}

/* ============================== Expenses ============================== */
private val EXP_CATS = listOf("Bahan", "Operasional", "Gaji", "Lainnya")

@Composable
fun ExpensesPage(vm: PosViewModel, onHome: () -> Unit) {
    val ex by vm.expenses.collectAsState()
    Screen {
        Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Txt("Pengeluaran", 20, FontWeight.SemiBold, lineHeight = 1.3f); Txt("${ex.size} catatan", 13, color = Sk.c.textMuted) }
            Btn("← Kembali", onHome, kind = 1)
        }
        ExpensesBody(vm, showTitle = false)
    }
}

@Composable
fun ExpensesBody(vm: PosViewModel, showTitle: Boolean = true) {
    val c = Sk.c
    val ex by vm.expenses.collectAsState()
    var form by remember { mutableStateOf<Expense?>(null) }
    var creating by remember { mutableStateOf(false) }
    var del by remember { mutableStateOf<Expense?>(null) }
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column { if (showTitle) { Txt("Pengeluaran", 15, FontWeight.SemiBold); Txt("${ex.size} catatan", 12, color = c.textMuted) } else Txt("${ex.size} catatan", 12, color = c.textMuted) }
        Btn("+ Tambah", { creating = true })
    }
    SkCard(Modifier.fillMaxWidth().padding(bottom = 16.dp), padding = 16) {
        Txt("Total pengeluaran", 13, color = c.textMuted)
        Txt(rupiah(ex.sumOf { it.amount }), 32, FontWeight.Bold, c.alert, Modifier.padding(top = 4.dp), spacing = -.64f, lineHeight = 1.2f)
    }
    KpiCompact(EXP_CATS.map { cat -> Triple(cat, rupiah(ex.filter { it.category == cat }.sumOf { it.amount }), null) })
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ex.forEach { e ->
            ListRow {
                Column(Modifier.weight(1f)) {
                    Txt(e.note.ifBlank { e.category }, 14, FontWeight.Medium)
                    Txt("${e.category} · ${fmt.format(Date(e.date))} · ${e.by}", 12, color = c.textMuted, modifier = Modifier.padding(top = 2.dp))
                }
                Txt("-" + rupiah(e.amount), 14, FontWeight.Bold, c.alert)
                IconBtn(SkIcons.Edit, { form = e }, size = 16, modifier = Modifier.size(32.dp))
                IconBtn(SkIcons.Trash, { del = e }, size = 16, modifier = Modifier.size(32.dp))
            }
        }
    }
    if (creating || form != null) ExpenseSheet(vm, form, { creating = false; form = null })
    del?.let { d -> ConfirmModal("Hapus pengeluaran?", "${d.note.ifBlank { d.category }} · ${rupiah(d.amount)} akan dihapus.", "Hapus", true, true, { del = null }) { vm.deleteExpense(d.id); del = null } }
}

@Composable
private fun ExpenseSheet(vm: PosViewModel, edit: Expense?, onDismiss: () -> Unit) {
    var amount by remember { mutableLongStateOf(edit?.amount ?: 0L) }
    var cat by remember { mutableStateOf(edit?.category ?: "Bahan") }
    var note by remember { mutableStateOf(edit?.note ?: "") }
    val toast = LocalToast.current
    SkSheet(onDismiss) {
        SheetTitle(if (edit == null) "Pengeluaran baru" else "Ubah pengeluaran")
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            RupiahField("Nominal (Rp)", amount, { amount = it })
            SelectField("Kategori", cat, EXP_CATS, { cat = it })
            Field("Catatan", note, { note = it }, placeholder = "Deskripsi singkat")
        }
        FormActions("Batal", onDismiss, if (edit == null) "Tambah" else "Simpan", {
            if (amount <= 0) toast("Nominal wajib diisi", "error")
            else { if (edit == null) vm.addExpense(amount, cat, note) else vm.updateExpense(edit.copy(amount = amount, category = cat, note = note)); onDismiss() }
        }, top = 16)
    }
}
