package com.sakukasir.pos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sakukasir.pos.domain.*
import com.sakukasir.pos.util.ReportExport

@Composable
fun DashboardScreen(vm: PosViewModel) {
    val tx by vm.transactions.collectAsState()
    val ex by vm.expenses.collectAsState()
    val user = vm.state.collectAsState().value.user!!
    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
    val todayTx = tx.filter { it.date == today }.ifEmpty { tx }
    val active = todayTx.filter { it.status != TransactionStatus.VOID }
    val voided = todayTx.filter { it.status == TransactionStatus.VOID }
    val refunded = todayTx.filter { it.status == TransactionStatus.REFUNDED || it.status == TransactionStatus.PARTIAL_REFUND }
    val totalSales = active.sumOf { it.total - it.refundAmount }
    val cash = active.filter { it.method == PaymentMethod.CASH }.sumOf { it.total - it.refundAmount }
    val qris = active.filter { it.method == PaymentMethod.QRIS }.sumOf { it.total - it.refundAmount }
    val expense = ex.filter { it.date >= System.currentTimeMillis() - 24 * 60 * 60 * 1000L }.sumOf { it.amount }
    val voidAmount = voided.sumOf { it.total }
    val refundAmount = refunded.sumOf { it.refundAmount }
    val net = totalSales - expense

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 22.dp)) {
        Text("Halo, ${user.displayName.substringBefore(' ')}", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
        Text(if (user.role == Role.OWNER) "Ringkasan hari ini" else "Ringkasan shift kamu", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(28.dp))

        SkKpiHeroExact("Penjualan hari ini", rupiah(totalSales), "${active.size} transaksi")
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            SkKpiExact("Cash", rupiah(cash), Modifier.weight(1f))
            SkKpiExact("QRIS", rupiah(qris), Modifier.weight(1f))
        }
        if (user.role == Role.OWNER) {
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                SkKpiExact("Void", rupiah(voidAmount), Modifier.weight(1f), Color(0xFFCF3038))
                SkKpiExact("Refund", rupiah(refundAmount), Modifier.weight(1f), Color(0xFFD17B00))
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                SkKpiExact("Pengeluaran", rupiah(expense), Modifier.weight(1f))
                SkKpiExact("Laba bersih", rupiah(net), Modifier.weight(1f))
            }

            Spacer(Modifier.height(28.dp))
            Text("REKAP VOID/REFUND PER KASIR", style = MaterialTheme.typography.labelLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            val recap = todayTx.groupBy { it.cashier }.mapValues { (_, rows) ->
                val vc = rows.count { it.status == TransactionStatus.VOID }
                val rc = rows.count { it.status == TransactionStatus.REFUNDED || it.status == TransactionStatus.PARTIAL_REFUND }
                val amount = rows.filter { it.status == TransactionStatus.VOID }.sumOf { it.total } + rows.sumOf { it.refundAmount }
                Triple(vc, rc, amount)
            }
            recap.forEach { (name, r) ->
                SkRecapCard(name, r.first, r.second, r.third)
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(8.dp))
            SkChartCard()
            Spacer(Modifier.height(18.dp))
            SkPaymentCard(cash, qris, totalSales)
        } else {
            vm.shift.collectAsState().value?.let { sh ->
                Spacer(Modifier.height(18.dp))
                SkShiftSummaryCard(sh, active.filter { it.cashierId == user.username })
            }
        }

        Spacer(Modifier.height(28.dp))
        Text("TRANSAKSI TERBARU", style = MaterialTheme.typography.labelLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        todayTx.take(if (user.role == Role.OWNER) 4 else 5).forEach { txItem ->
            SkDashboardTransaction(txItem)
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SkKpiHeroExact(label: String, value: String, delta: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(horizontal = 36.dp, vertical = 28.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
            Spacer(Modifier.height(6.dp))
            Text(delta, color = Color(0xFF279B5B), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun SkKpiExact(label: String, value: String, modifier: Modifier, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(value, color = valueColor, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
        }
    }
}

@Composable
private fun SkRecapCard(name: String, voidCount: Int, refundCount: Int, amount: Long) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(name, style = MaterialTheme.typography.titleMedium); Text("Void ${voidCount}× · Refund ${refundCount}×", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text(rupiah(amount), color = if (amount > 0) Color(0xFFCF3038) else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
        }
    }
}

@Composable
private fun SkChartCard() {
    val values = listOf(58, 82, 68, 78, 80, 78, 8)
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(28.dp)) {
            Text("7 hari terakhir", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth().height(130.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                values.forEachIndexed { i, v ->
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.fillMaxWidth().height((v * 1.05f).dp).clip(RoundedCornerShape(8.dp)).background(if (i == 6) Color(0xFF0F5132) else MaterialTheme.colorScheme.surfaceVariant))
                        Spacer(Modifier.height(8.dp)); Text(listOf("S","S","R","K","J","S","M")[i], color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SkPaymentCard(cash: Long, qris: Long, total: Long) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(28.dp)) {
            Text("Metode pembayaran", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(24.dp))
            SkPaymentRow("Cash", cash, total, Color(0xFF21A65B)); Spacer(Modifier.height(18.dp)); SkPaymentRow("QRIS", qris, total, Color(0xFF7C3AED))
        }
    }
}

@Composable
private fun SkPaymentRow(label: String, amount: Long, total: Long, color: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(78.dp), style = MaterialTheme.typography.titleMedium)
        Box(Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(if (total == 0L) 0f else (amount.toFloat() / total).coerceIn(0f, 1f)).background(color, RoundedCornerShape(8.dp)))
        }
        Spacer(Modifier.width(18.dp)); Text(rupiah(amount), style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
    }
}

@Composable
private fun SkDashboardTransaction(tx: Transaction) {
    val count = tx.items.sumOf { it.qty }
    val synced = tx.syncStatus == SyncStatus.SYNCED
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("#${tx.id.takeLast(4)} · ${tx.time}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.width(8.dp)); SkBadge(if (synced) "✓ SYNCED" else "⌛ PENDING", if (synced) Color(0xFF219653) else Color(0xFFD17B00))
                }
                Spacer(Modifier.height(5.dp)); Text("$count item · ${if (tx.method == PaymentMethod.CASH) "Cash" else "QRIS"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(rupiah(tx.total), style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
        }
    }
}

@Composable
private fun SkShiftSummaryCard(shift: Shift, tx: List<Transaction>) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(22.dp)) {
            Text("Shift aktif", color = Color(0xFF279B5B), style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold))
            Text("Mulai ${java.text.SimpleDateFormat("HH:mm").format(java.util.Date(shift.startAt))}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp)); Text("${tx.size} transaksi · ${rupiah(tx.sumOf { it.total })}")
        }
    }
}

@Composable fun ReportScreen(vm:PosViewModel){
    val tx by vm.transactions.collectAsState();val ex by vm.expenses.collectAsState();var tab by remember{mutableIntStateOf(0)}
    val tabs=listOf("Ringkasan","Outlet","Kasir","Harian","Bulanan","Keuangan","Pengeluaran")
    Column(Modifier.fillMaxSize().padding(16.dp)){ScrollableTabRow(selectedTabIndex=tab){tabs.forEachIndexed{i,t->Tab(i==tab,{tab=i},text={Text(t)})}};Spacer(Modifier.height(12.dp))
        when(tab){
            0->{val sales=tx.filter{it.status==TransactionStatus.COMPLETED}.sumOf{it.total};SkKpiHero("Total penjualan",rupiah(sales));SkKpiCompact(listOf("Cash" to rupiah(tx.filter{it.method==PaymentMethod.CASH}.sumOf{it.total}),"QRIS" to rupiah(tx.filter{it.method==PaymentMethod.QRIS}.sumOf{it.total})))}
            1->SimpleBreakdown("Outlet",tx.groupBy{it.outlet}.map{it.key to rupiah(it.value.filter{t->t.status==TransactionStatus.COMPLETED}.sumOf{t->t.total})})
            2->SimpleBreakdown("Kasir",tx.groupBy{it.cashier}.map{it.key to rupiah(it.value.filter{t->t.status==TransactionStatus.COMPLETED}.sumOf{t->t.total})})
            3->SimpleBreakdown("Harian",tx.groupBy{it.date}.map{it.key to rupiah(it.value.filter{t->t.status==TransactionStatus.COMPLETED}.sumOf{t->t.total})})
            4->SimpleBreakdown("Bulanan",tx.groupBy{it.date.take(7)}.map{it.key to rupiah(it.value.filter{t->t.status==TransactionStatus.COMPLETED}.sumOf{t->t.total})})
            5->{val gross=tx.filter{it.status==TransactionStatus.COMPLETED}.sumOf{it.total};val disc=tx.sumOf{it.discount};val tax=tx.sumOf{it.tax};val void=tx.filter{it.status==TransactionStatus.VOID}.sumOf{it.total};val refund=tx.sumOf{it.refundAmount};val net=gross-disc-tax-void-refund;val profit=net-ex.sumOf{it.amount};SimpleBreakdown("Keuangan",listOf("Gross sales" to rupiah(gross),"- Diskon" to rupiah(disc),"- Pajak" to rupiah(tax),"- Void" to rupiah(void),"- Refund" to rupiah(refund),"Net Sales" to rupiah(net),"- Pengeluaran" to rupiah(ex.sumOf{it.amount}),"Net Profit" to rupiah(profit)))}
            6->{SimpleBreakdown("Pengeluaran",ex.groupBy{it.category}.map{it.key to rupiah(it.value.sumOf{e->e.amount})});Spacer(Modifier.height(10.dp));SkButton("Export XLSX",{ReportExport.exportExpenses(ex)})}
        }
    }
}
@Composable private fun SimpleBreakdown(title:String,rows:List<Pair<String,String>>){Text(title,style=MaterialTheme.typography.titleMedium);rows.forEach{(a,b)->ListItem(headlineContent={Text(a)},trailingContent={Text(b)})}}
