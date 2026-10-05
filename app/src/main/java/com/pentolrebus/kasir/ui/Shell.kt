package com.pentolrebus.kasir.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pentolrebus.kasir.R
import com.pentolrebus.kasir.domain.*
import com.pentolrebus.kasir.util.BluetoothPrinter
import com.pentolrebus.kasir.util.Diagnostics

private enum class Pay { NONE, CASH, QRIS, SUCCESS }

/** Main authenticated shell: 4-tab bottom navigation shared by Kasir and Owner, plus a page stack for detail/form screens. */
@Composable
fun MainShell(vm: PosViewModel, session: Session, themeMode: Int, onThemeMode: (Int) -> Unit, diagnostics: Diagnostics) {
    val ctx = LocalContext.current
    val printer = remember(ctx) { BluetoothPrinter(ctx) }
    val shift by vm.shift.collectAsState()
    val cart by vm.cart.collectAsState()
    val outlets by vm.outlets.collectAsState()
    val activeOutlet by vm.activeOutlet.collectAsState()
    val message by vm.message.collectAsState()

    var tab by remember { mutableStateOf(0) }
    var stack by remember { mutableStateOf(listOf<Pair<String, String?>>()) }
    var pay by remember { mutableStateOf(Pay.NONE) }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var discount by remember { mutableStateOf(0L) }
    var showStart by remember { mutableStateOf(false) }
    var showClose by remember { mutableStateOf(false) }
    var showBlocked by remember { mutableStateOf(false) }

    LaunchedEffect(message) { message?.let { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show(); vm.consumeMessage() } }
    val nav = remember { Nav(open = { r, a -> stack = stack + (r to a) }, back = { stack = stack.dropLast(1) }) }
    val outletName = outlets.firstOrNull { it.id == activeOutlet }?.name ?: "Outlet"
    val subtotal = cart.sumOf { it.product.price * it.quantity }
    val payTotal = (subtotal - discount.coerceIn(0, subtotal)).coerceAtLeast(0)

    BackHandler(enabled = stack.isNotEmpty() || (tab == 1 && pay != Pay.NONE && pay != Pay.SUCCESS)) {
        if (stack.isNotEmpty()) stack = stack.dropLast(1) else pay = Pay.NONE
    }
    val tryLogout = { if (shift != null) showBlocked = true else vm.logout() }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (stack.isNotEmpty()) {
                val (route, arg) = stack.last()
                ManageRouter(route, arg, vm, session, nav, printer, themeMode, onThemeMode) { showClose = true }
            } else when (tab) {
                0 -> Column(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(outletName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                            Text(if (session.role == Role.OWNER) "Owner · ${session.username}" else "${session.username} · Kasir", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        ShiftChip(shift, onStart = { showStart = true }, onOpen = { nav.open("Shift:detail", "active") })
                    }
                    Spacer(Modifier.height(6.dp))
                    KasirScreen(vm, session.role, nav, onCheckout = { tab = 1 }, onStartShift = { showStart = true })
                }
                1 -> when (pay) {
                    Pay.NONE -> CheckoutScreen(vm, discount, { discount = it }, method, { method = it }) {
                        if (shift == null) Toast.makeText(ctx, "Mulai shift terlebih dahulu", Toast.LENGTH_SHORT).show()
                        else pay = if (method == PaymentMethod.CASH) Pay.CASH else Pay.QRIS
                    }
                    Pay.CASH -> CashPayScreen(payTotal, { pay = Pay.NONE }) { received -> vm.checkout(PaymentMethod.CASH, null, discount, received); discount = 0; pay = Pay.SUCCESS }
                    Pay.QRIS -> QrisPayScreen(payTotal, activeOutlet ?: session.outletId.orEmpty(), { pay = Pay.NONE }) { vm.checkout(PaymentMethod.QRIS, null, discount); discount = 0; pay = Pay.SUCCESS }
                    Pay.SUCCESS -> SuccessScreen(vm, printer, onNew = { pay = Pay.NONE; tab = 0 }) { r, a -> nav.open(r, a) }
                }
                2 -> ReportsScreen(vm, session, nav)
                else -> SettingsScreen(vm, session, nav, onLogout = tryLogout)
            }
        }
        if (stack.isEmpty() && pay != Pay.SUCCESS) Surface(color = MaterialTheme.colorScheme.surface) {
            Row(Modifier.fillMaxWidth().navigationBarsPadding()) {
                val labels = listOf("Kasir", "Checkout", "Laporan", "Pengaturan")
                val icons = listOf(R.drawable.ic_cart, R.drawable.ic_grid, R.drawable.ic_report, R.drawable.ic_more)
                labels.forEachIndexed { i, l ->
                    val selected = tab == i
                    val c = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    Column(Modifier.weight(1f).clickable { tab = i; if (i != 1) pay = Pay.NONE }.padding(top = 8.dp, bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        BadgedBox(badge = { if (i == 1 && cart.isNotEmpty()) Badge { Text("${cart.sumOf { it.quantity }}") } }) { Icon(painterResource(icons[i]), l, tint = c, modifier = Modifier.size(24.dp)) }
                        Text(l, color = c, style = MaterialTheme.typography.labelSmall, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
        }
    }

    if (showStart) MoneyDialog("Mulai Shift", "Kas awal (hitung uang di laci)", { showStart = false }) { vm.startShift(it); showStart = false }
    if (showClose) CloseShiftDialogFull(shift, { showClose = false }) { vm.closeShift(it); showClose = false; stack = emptyList() }
    if (showBlocked) AlertDialog(onDismissRequest = { showBlocked = false },
        icon = { Text("!", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary) },
        title = { Text("Shift masih aktif") }, text = { Text("Anda harus menutup shift terlebih dahulu sebelum keluar.") },
        confirmButton = { Button(onClick = { showBlocked = false; showClose = true }) { Text("TUTUP SHIFT") } },
        dismissButton = { TextButton(onClick = { showBlocked = false }) { Text("KEMBALI") } })
}

@Composable
private fun ShiftChip(shift: Shift?, onStart: () -> Unit, onOpen: () -> Unit) {
    Surface(Modifier.clickable { if (shift == null) onStart() else onOpen() }, shape = RoundedCornerShape(18.dp), color = if (shift == null) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(9.dp), shape = CircleShape, color = if (shift == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary) {}
            Spacer(Modifier.width(8.dp))
            Column {
                Text(if (shift == null) "Belum ada Shift" else "Shift Aktif", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = if (shift == null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary)
                Text(if (shift == null) "ketuk untuk mulai" else "${fmtTime(shift.startAt)} · ketuk", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MoneyDialog(title: String, label: String, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    var v by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { KField(label, v, { v = it }, number = true) },
        confirmButton = { Button(onClick = { onConfirm(v.toLongOrNull() ?: 0) }, enabled = v.isNotEmpty()) { Text("MULAI SHIFT") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("BATAL") } })
}
