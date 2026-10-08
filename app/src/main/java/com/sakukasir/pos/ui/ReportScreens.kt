package com.sakukasir.pos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sakukasir.pos.domain.*
import com.sakukasir.pos.util.ReportExport
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val dayFmt get() = SimpleDateFormat("yyyy-MM-dd", Locale.US)
private fun todayStr() = dayFmt.format(Date())
private fun shift(days: Int): String = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, days) }.let { dayFmt.format(it.time) }
private fun net(t: Transaction) = t.total - t.refundAmount

private data class Range(val from: String, val to: String)
internal data class Growth(val dir: String, val pct: Double)

private fun monthRange(offset: Int): Range {
    val cal = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); add(Calendar.MONTH, offset) }
    val from = dayFmt.format(cal.time)
    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
    return Range(from, dayFmt.format(cal.time))
}

private fun periodRange(p: String, from: String, to: String): Range = when (p) {
    "today" -> Range(todayStr(), todayStr())
    "week" -> Range(shift(-6), todayStr())
    "month" -> monthRange(0)
    "lastmonth" -> monthRange(-1)
    "custom" -> Range(from.ifBlank { todayStr() }, to.ifBlank { todayStr() })
    else -> Range("0000-01-01", "9999-12-31")
}

private fun prevRange(p: String, from: String, to: String): Range = when (p) {
    "today" -> Range(shift(-1), shift(-1))
    "week" -> Range(shift(-13), shift(-7))
    "month" -> monthRange(-1)
    "lastmonth" -> monthRange(-2)
    "custom" -> {
        val f = runCatching { dayFmt.parse(from)!!.time }.getOrDefault(0L); val t = runCatching { dayFmt.parse(to)!!.time }.getOrDefault(0L)
        val len = ((t - f) / 86400000L).toInt() + 1
        val c1 = Calendar.getInstance().apply { time = Date(f); add(Calendar.DAY_OF_YEAR, -len) }
        val c2 = Calendar.getInstance().apply { time = Date(f); add(Calendar.DAY_OF_YEAR, -1) }
        Range(dayFmt.format(c1.time), dayFmt.format(c2.time))
    }
    else -> Range("0000-00-00", "0000-00-00")
}

private fun periodLabel(p: String) = when (p) { "today" -> "Hari ini"; "week" -> "7 hari"; "month" -> "Bulan ini"; "lastmonth" -> "Bulan lalu"; "custom" -> "Custom"; else -> "Semua" }

private fun growth(cur: Long, prev: Long): Growth {
    if (prev == 0L) return if (cur > 0) Growth("up", 100.0) else Growth("flat", 0.0)
    val pct = Math.abs(cur - prev).toDouble() / prev * 100
    return Growth(if (cur > prev) "up" else if (cur < prev) "down" else "flat", pct)
}

@Composable
private fun GrowthLine(g: Growth, suffix: String, invert: Boolean = false) {
    val c = Sk.c
    val good = if (invert) g.dir == "down" else g.dir == "up"
    val bad = if (invert) g.dir == "up" else g.dir == "down"
    val col = if (good) c.cash else if (bad) c.alert else c.textMuted
    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (g.dir == "up") SkIcon(SkIcons.TrendUp, 14.dp, col) else if (g.dir == "down") SkIcon(SkIcons.TrendDown, 14.dp, col)
        Txt("${g.pct.toInt()}% $suffix", 12, FontWeight.SemiBold, col)
    }
}

@Composable
private fun HeroExtra(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Sk.c.border))
        Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun ExtraRow(l: String, v: String, vc: Color = Sk.c.text) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Txt(l, 12, color = Sk.c.textMuted); Txt(v, 12, FontWeight.SemiBold, vc) }
}

/* ============================== Laporan Kasir (dashboard) ============================== */
@Composable
fun DashboardScreen(vm: PosViewModel) {
    val c = Sk.c
    val tx by vm.transactions.collectAsState()
    val shift by vm.shift.collectAsState()
    val user = vm.state.collectAsState().value.user!!
    var detail by remember { mutableStateOf<Transaction?>(null) }
    val mine = tx.filter { it.cashierId == user.username }
    val todayTx = mine.filter { it.date == todayStr() }.ifEmpty { mine }
    val active = todayTx.filter { it.status != TransactionStatus.VOID }
    val sales = active.sumOf { net(it) }
    val cash = active.filter { it.method == PaymentMethod.CASH }.sumOf { net(it) }
    val qris = active.filter { it.method == PaymentMethod.QRIS }.sumOf { net(it) }
    val voidAmt = todayTx.filter { it.status == TransactionStatus.VOID }.sumOf { it.total }
    val refundAmt = todayTx.sumOf { it.refundAmount }
    val ySales = mine.filter { it.date == shift(-1) && it.status != TransactionStatus.VOID }.sumOf { net(it) }
    val g = growth(sales, ySales)
    Screen {
        PageHead("Laporan", "Ringkasan & riwayat hari ini")
        SkCard(Modifier.fillMaxWidth().padding(bottom = 16.dp), padding = 20) {
            Txt("Ringkasan Penjualan · Hari ini", 13, color = c.textMuted)
            Txt(rupiah(sales), 32, FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp), lineHeight = 1.1f, spacing = -.64f)
            Txt("${active.size} transaksi", 13, FontWeight.Medium, c.cash)
            GrowthLine(g, "dari kemarin")
            HeroExtra { ExtraRow("Kemarin", rupiah(ySales)) }
        }
        KpiGrid(listOf(Triple("Cash", rupiah(cash), null), Triple("QRIS", rupiah(qris), null), Triple("Void", rupiah(voidAmt), c.alert), Triple("Refund", rupiah(refundAmt), c.warn)))
        shift?.let { sh ->
            val st = todayTx.filter { it.timestamp >= sh.startAt && it.status == TransactionStatus.COMPLETED }
            val sc = st.filter { it.method == PaymentMethod.CASH }.sumOf { it.total }; val sq = st.filter { it.method == PaymentMethod.QRIS }.sumOf { it.total }
            SkCard(Modifier.fillMaxWidth().padding(bottom = 16.dp), padding = 20) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                    Box(Modifier.size(8.dp).background(c.cash, CircleShape)); Txt("SHIFT AKTIF", 12, FontWeight.SemiBold, c.cash, spacing = .48f)
                }
                Txt("Mulai ${SimpleDateFormat("HH:mm", Locale.US).format(Date(sh.startAt))}", 12, color = c.textMuted)
                Column(Modifier.padding(top = 12.dp)) {
                    KeyRow("Transaksi shift ini", st.size.toString()); KeyRow("Cash", rupiah(sc)); KeyRow("QRIS", rupiah(sq)); KeyRow("Total", rupiah(sc + sq), c.primary, divider = false, big = true)
                }
            }
        }
        SectionTitle("Riwayat Transaksi Hari Ini (${todayTx.size})")
        if (todayTx.isEmpty()) EmptyState("Belum ada transaksi hari ini", "Transaksi yang kamu buat hari ini akan muncul di sini.")
        else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { todayTx.forEach { t -> TxItem(t) { detail = t } } }
    }
    detail?.let { cur -> TransactionDetail(vm, tx.find { it.id == cur.id } ?: cur) { detail = null } }
}

/* ============================== Laporan Owner ============================== */
@Composable
fun ReportScreen(vm: PosViewModel) {
    val c = Sk.c
    val txAll by vm.transactions.collectAsState()
    val exAll by vm.expenses.collectAsState()
    val workers by vm.workers.collectAsState()
    val s by vm.state.collectAsState()
    val toast = LocalToast.current
    var tab by remember { mutableIntStateOf(0) }
    var filter by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<Transaction?>(null) }
    val range = periodRange(s.reportPeriod, s.reportFrom, s.reportTo)
    val prev = prevRange(s.reportPeriod, s.reportFrom, s.reportTo)
    val userList = txAll.filter { it.date >= range.from && it.date <= range.to && (s.reportUser == "all" || it.cashierId == s.reportUser) }
    val active = userList.filter { it.status != TransactionStatus.VOID }
    val sales = active.sumOf { net(it) }
    val cash = active.filter { it.method == PaymentMethod.CASH }.sumOf { net(it) }
    val qris = active.filter { it.method == PaymentMethod.QRIS }.sumOf { net(it) }
    val discount = userList.sumOf { it.discount }; val tax = userList.sumOf { it.tax }
    val voidAmt = userList.filter { it.status == TransactionStatus.VOID }.sumOf { it.total }
    val refundAmt = userList.sumOf { it.refundAmount }
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val expIn = exAll.filter { val d = fmt.format(Date(it.date)); d >= range.from && d <= range.to }
    val expense = expIn.sumOf { it.amount }
    val netProfit = sales - expense
    val prevActive = txAll.filter { it.date >= prev.from && it.date <= prev.to && (s.reportUser == "all" || it.cashierId == s.reportUser) && it.status != TransactionStatus.VOID }
    val prevSales = prevActive.sumOf { net(it) }
    val prevExpense = exAll.filter { val d = fmt.format(Date(it.date)); d >= prev.from && d <= prev.to }.sumOf { it.amount }
    val periodText = if (s.reportPeriod == "custom") "${s.reportFrom} → ${s.reportTo}" else periodLabel(s.reportPeriod)
    val userText = if (s.reportUser == "all") "Semua kasir" else (workers.find { it.username == s.reportUser }?.name ?: s.reportUser)

    Screen {
        PageHead("Laporan", "$periodText · $userText") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn("Filter", { filter = true }, kind = 1, icon = SkIcons.Filter)
                Btn("Export", { ReportExport.exportExpenses(exAll); toast("Export XLSX diproses backend", "success") }, kind = 1)
            }
        }
        SegTabs(listOf("Transaksi", "Pengeluaran", "Ringkasan"), tab, { tab = it }, listOf(SkIcons.Receipt, SkIcons.Wallet, SkIcons.Chart))
        when (tab) {
            0 -> if (userList.isEmpty()) EmptyState("Belum ada transaksi", "Coba ubah filter periode atau kasir.")
                 else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { userList.forEach { t -> TxItem(t) { detail = t } } }
            1 -> ExpensesBody(vm, expIn, periodText, growth(expense, prevExpense))
            else -> {
                SkCard(Modifier.fillMaxWidth().padding(bottom = 16.dp), padding = 20) {
                    Txt("Ringkasan Penjualan · $periodText", 13, color = c.textMuted)
                    Txt(rupiah(sales), 32, FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp), lineHeight = 1.1f, spacing = -.64f)
                    Txt("${active.size} transaksi", 13, FontWeight.Medium, c.cash)
                    HeroExtra {
                        GrowthLine(growth(sales, prevSales), "dari periode lalu")
                        ExtraRow("Periode lalu", rupiah(prevSales)); ExtraRow("Transaksi lalu", prevActive.size.toString())
                    }
                }
                KpiGrid(listOf(
                    Triple("Cash", rupiah(cash), null), Triple("QRIS", rupiah(qris), null), Triple("Void", rupiah(voidAmt), c.alert), Triple("Refund", rupiah(refundAmt), c.warn),
                    Triple("Pengeluaran", rupiah(expense), c.alert), Triple("Laba bersih", rupiah(netProfit), c.cash)
                ))
                // 7 hari terakhir
                val days = (6 downTo 0).map { off ->
                    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -off) }
                    val ds = dayFmt.format(cal.time)
                    Triple(listOf("M", "S", "S", "R", "K", "J", "S")[cal.get(Calendar.DAY_OF_WEEK) - 1], txAll.filter { it.date == ds && it.status != TransactionStatus.VOID }.sumOf { net(it) }, off == 0)
                }
                val max = maxOf(days.maxOf { it.second }, 1L).toFloat()
                SkCard(Modifier.fillMaxWidth().padding(bottom = 16.dp), padding = 16) {
                    Txt("7 hari terakhir", 13, color = c.textMuted, modifier = Modifier.padding(bottom = 12.dp))
                    Row(Modifier.fillMaxWidth().height(100.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                        days.forEach { (label, total, today) ->
                            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom)) {
                                Box(Modifier.fillMaxWidth().height(maxOf(4f, 78f * maxOf(6f, total / max * 100f) / 100f).dp).clip(RXs).background(if (today) c.primary else c.surfaceAlt))
                                Txt(label, 11, color = c.textFaint, lineHeight = 1.2f)
                            }
                        }
                    }
                }
                SkCard(Modifier.fillMaxWidth().padding(bottom = 16.dp), padding = 16) {
                    Txt("Metode pembayaran", 13, color = c.textMuted, modifier = Modifier.padding(bottom = 4.dp))
                    MethodBlock("Cash", cash, sales, c.cash, false); MethodBlock("QRIS", qris, sales, c.qris, true)
                }
                SectionTitle("Ringkasan keuangan", true)
                Column(Modifier.fillMaxWidth().padding(bottom = 16.dp).skCard().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    KeyRow("Gross sales", rupiah(sales + discount)); KeyRow("Diskon", "- " + rupiah(discount), c.alert); KeyRow("Pajak", "- " + rupiah(tax), c.alert)
                    if (voidAmt > 0) KeyRow("Void", "- " + rupiah(voidAmt), c.alert)
                    if (refundAmt > 0) KeyRow("Refund", "- " + rupiah(refundAmt), c.warn)
                    KeyRow("Net sales", rupiah(sales)); KeyRow("Pengeluaran", "- " + rupiah(expense), c.alert)
                    KeyRow("Net profit", rupiah(netProfit), c.primary, divider = false, big = true)
                }
                SectionTitle("Rekap void/refund per kasir")
                val recap = userList.groupBy { it.cashier }
                if (recap.isEmpty()) EmptyState("Belum ada data") else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recap.forEach { (name, rows) ->
                        val vc = rows.count { it.status == TransactionStatus.VOID }; val rc = rows.count { it.status == TransactionStatus.REFUNDED || it.status == TransactionStatus.PARTIAL_REFUND }
                        val amt = rows.filter { it.status == TransactionStatus.VOID }.sumOf { it.total } + rows.sumOf { it.refundAmount }
                        Row(Modifier.fillMaxWidth().skCard().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Txt(name, 13, FontWeight.Medium); Txt("Void $vc× · Refund $rc×", 11, color = c.textMuted, modifier = Modifier.padding(top = 2.dp)) }
                            Txt(if (amt > 0) "-" + rupiah(amt) else rupiah(0), 13, FontWeight.Bold, c.alert)
                        }
                    }
                }
                SectionTitle("Penjualan per outlet"); NameAmountRows(active.groupBy { it.outlet }.map { it.key to it.value.sumOf { t -> net(t) } })
                SectionTitle("Penjualan per kasir"); NameAmountRows(active.groupBy { it.cashier }.map { it.key to it.value.sumOf { t -> net(t) } })
            }
        }
    }
    if (filter) FilterSheet(vm, workers) { filter = false }
    detail?.let { cur -> TransactionDetail(vm, txAll.find { it.id == cur.id } ?: cur) { detail = null } }
}

@Composable
private fun MethodBlock(label: String, amount: Long, total: Long, color: Color, divider: Boolean) {
    val c = Sk.c
    if (divider) Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))
    Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = if (divider) 0.dp else 12.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) { Txt(label, 13, FontWeight.Medium); Txt(rupiah(amount), 13, FontWeight.SemiBold) }
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RXs).background(c.surfaceAlt)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(if (total <= 0L) 0f else (amount.toFloat() / total).coerceIn(0f, 1f)).clip(RXs).background(color))
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

@Composable
private fun FilterSheet(vm: PosViewModel, workers: List<Worker>, onDismiss: () -> Unit) {
    val s = vm.state.collectAsState().value
    var period by remember { mutableStateOf(s.reportPeriod) }
    var user by remember { mutableStateOf(s.reportUser) }
    var from by remember { mutableStateOf(s.reportFrom.ifBlank { todayStr() }) }
    var to by remember { mutableStateOf(s.reportTo.ifBlank { todayStr() }) }
    SkSheet(onDismiss) {
        Txt("Filter Laporan", 17, FontWeight.SemiBold, lineHeight = 1.3f)
        Spacer(Modifier.height(12.dp))
        SectionTitle("Periode", true)
        ChipRow { listOf("all" to "Semua", "today" to "Hari ini", "week" to "7 hari", "month" to "Bulan ini", "lastmonth" to "Bulan lalu", "custom" to "Custom").forEach { (id, l) -> Chip(l, period == id) { period = id } } }
        if (period == "custom") Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Field("Dari", from, { from = it }, Modifier.weight(1f), placeholder = "yyyy-MM-dd"); Field("Sampai", to, { to = it }, Modifier.weight(1f), placeholder = "yyyy-MM-dd")
        }
        SectionTitle("Kasir")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            (listOf("all" to "Semua kasir") + workers.map { it.username to it.name }).forEach { (id, l) ->
                ListRow(onClick = { user = id }) { Txt(l, 14, FontWeight.Medium, modifier = Modifier.weight(1f)); if (user == id) Txt("Aktif", 14, FontWeight.SemiBold, Sk.c.primary) }
            }
        }
        FormActions("Reset", { period = "all"; user = "all"; from = todayStr(); to = todayStr() }, "Terapkan", { vm.setReportFilter(period, user, from, to); onDismiss() }, top = 16)
    }
}

/* ============================== Pengeluaran ============================== */
private val EXP_CATS = listOf("Bahan", "Operasional", "Gaji", "Lainnya")

@Composable
fun ExpensesPage(vm: PosViewModel, onHome: () -> Unit) {
    val c = Sk.c
    val ex by vm.expenses.collectAsState()
    val user = vm.state.collectAsState().value.user!!
    Screen {
        PageHead("Pengeluaran", "${ex.size} catatan") { BackHome(onHome) }
        ExpensesBody(vm, ex, "", null, editable = user.role == Role.OWNER, compactCats = listOf("Bahan", "Operasional"))
    }
}

@Composable
internal fun ExpensesBody(vm: PosViewModel, ex: List<Expense>, periodText: String, g: Growth?, editable: Boolean = true, compactCats: List<String>? = null) {
    val c = Sk.c
    var form by remember { mutableStateOf<Expense?>(null) }
    var creating by remember { mutableStateOf(false) }
    var del by remember { mutableStateOf<Expense?>(null) }
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    val total = ex.sumOf { it.amount }
    SkCard(Modifier.fillMaxWidth().padding(bottom = 16.dp), padding = 20) {
        Txt(if (periodText.isEmpty()) "Total pengeluaran" else "Total Pengeluaran · $periodText", 13, color = c.textMuted)
        Txt(rupiah(total), 32, FontWeight.Bold, c.alert, Modifier.padding(top = 4.dp, bottom = 6.dp), lineHeight = 1.1f, spacing = -.64f)
        Txt("${ex.size} catatan", 13, FontWeight.Medium, c.cash)
        if (g != null) HeroExtra {
            GrowthLine(g, "dari periode lalu", invert = true)
            EXP_CATS.forEach { cat -> ExtraRow(cat, rupiah(ex.filter { it.category == cat }.sumOf { it.amount })) }
        }
    }
    if (compactCats != null) KpiCompact(compactCats.map { cat -> Triple(cat, rupiah(ex.filter { it.category == cat }.sumOf { it.amount }), null) })
    Row(Modifier.fillMaxWidth().padding(top = if (g != null) 0.dp else 0.dp, bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column { Txt("Daftar pengeluaran", 14, FontWeight.SemiBold); Txt("${ex.size} catatan" + if (periodText.isEmpty()) "" else " di periode ini", 12, color = c.textMuted) }
        Box(Modifier.heightIn(min = 36.dp).clip(RSm).background(c.primary).clickable { creating = true }.padding(horizontal = 14.dp, vertical = 8.dp), contentAlignment = Alignment.Center) { Txt("+ Tambah", 13, FontWeight.SemiBold, Color.White) }
    }
    if (ex.isEmpty()) EmptyState("Belum ada pengeluaran di periode ini")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ex.forEach { e ->
            ListRow {
                Column(Modifier.weight(1f)) {
                    Txt(e.note.ifBlank { e.category }, 14, FontWeight.Medium)
                    Txt("${e.category} · ${fmt.format(Date(e.date))} · ${e.by}", 12, color = c.textMuted, modifier = Modifier.padding(top = 2.dp))
                }
                Txt("-" + rupiah(e.amount), 14, FontWeight.Bold, c.alert)
                if (editable) {
                    IconBtn(SkIcons.Edit, { form = e }, size = 16, modifier = Modifier.size(32.dp))
                    IconBtn(SkIcons.Trash, { del = e }, size = 16, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
    if (creating || form != null) ExpenseSheet(vm, form) { creating = false; form = null }
    del?.let { d -> ConfirmModal("Hapus pengeluaran?", "\"${d.note.ifBlank { d.category }}\" akan dihapus.", "Hapus", true, true, { del = null }) { vm.deleteExpense(d.id); del = null } }
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
