package com.pentolrebus.kasir.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.pentolrebus.kasir.R
import com.pentolrebus.kasir.domain.*
import com.pentolrebus.kasir.util.BluetoothPrinter

private enum class Pay { NONE, CASH, QRIS, SUCCESS }

/** Main authenticated shell: 4-tab bottom navigation shared by Kasir and Owner, plus a page stack for detail/form screens. */
@Composable
fun MainShell(vm: PosViewModel, session: Session, themeMode: Int, onThemeMode: (Int) -> Unit) {
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
    var taxPercent by remember { mutableStateOf(0L) }
    var showStart by remember { mutableStateOf(false) }
    var showClose by remember { mutableStateOf(false) }
    var showBlocked by remember { mutableStateOf(false) }

    LaunchedEffect(message) { message?.let { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show(); vm.consumeMessage() } }
    val nav = remember { Nav(open = { r, a -> stack = stack + (r to a) }, back = { stack = stack.dropLast(1) }) }
    val outletName = outlets.firstOrNull { it.id == activeOutlet }?.name ?: "Outlet"
    val subtotal = cart.sumOf { it.product.price * it.quantity }
    val discountValue = discount.coerceIn(0, subtotal)
    val taxValue = kotlin.math.round((subtotal - discountValue) * taxPercent.coerceIn(0, 100) / 100.0).toLong()
    val payTotal = (subtotal - discountValue + taxValue).coerceAtLeast(0)
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

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
                    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 17.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(outletName, fontSize = 17.6.sp, lineHeight = 21.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                            Text(if (session.role == Role.OWNER) "Owner · ${session.username}" else "${session.username} · Kasir", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        ShiftChip(shift, onStart = { showStart = true }, onOpen = { nav.open("Shift:detail", "active") })
                    }
                    Spacer(Modifier.height(6.dp))
                    KasirScreen(vm, session.role, nav, onCheckout = { tab = 1 }, onStartShift = { showStart = true })
                }
                1 -> when (pay) {
                    Pay.NONE -> CheckoutScreen(vm, discount, { discount = it }, taxPercent, { taxPercent = it }, method, { method = it }) {
                        if (shift == null) Toast.makeText(ctx, "Mulai shift terlebih dahulu", Toast.LENGTH_SHORT).show()
                        else pay = if (method == PaymentMethod.CASH) Pay.CASH else Pay.QRIS
                    }
                    Pay.CASH -> CashPayScreen(payTotal, { pay = Pay.NONE }) { received -> vm.checkout(PaymentMethod.CASH, null, discount, taxPercent, received); discount = 0; taxPercent = 0; pay = Pay.SUCCESS }
                    Pay.QRIS -> QrisPayScreen(payTotal, activeOutlet ?: session.outletId.orEmpty(), { pay = Pay.NONE }) { vm.checkout(PaymentMethod.QRIS, null, discount, taxPercent); discount = 0; taxPercent = 0; pay = Pay.SUCCESS }
                    Pay.SUCCESS -> SuccessScreen(vm, printer, onNew = { pay = Pay.NONE; tab = 0 }) { r, a -> nav.open(r, a) }
                }
                2 -> ReportsScreen(vm, session, nav)
                else -> SettingsScreen(vm, session, nav, themeMode = themeMode, onLogout = tryLogout)
            }
        }
        if (pay != Pay.SUCCESS && !imeVisible) Surface(color = MaterialTheme.colorScheme.surface) {
            Row(Modifier.fillMaxWidth().navigationBarsPadding()) {
                val labels = listOf("Kasir", "Checkout", "Laporan", "Pengaturan")
                val icons = listOf(R.drawable.ic_grid, R.drawable.ic_cart, R.drawable.ic_report, R.drawable.ic_more)
                labels.forEachIndexed { i, l ->
                    val selected = tab == i
                    val c = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    Column(Modifier.weight(1f).clickable { tab = i; stack = emptyList(); if (i != 1) pay = Pay.NONE }.padding(top = 6.dp, bottom = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(shape = RoundedCornerShape(18.dp), color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent) {
                            Box(Modifier.padding(horizontal = 14.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
                                BadgedBox(badge = { if (i == 1 && cart.isNotEmpty()) Badge { Text("${cart.sumOf { it.quantity }}") } }) { Icon(painterResource(icons[i]), l, tint = c, modifier = Modifier.size(26.dp)) }
                            }
                        }
                        Text(l, color = c, fontSize = 17.6.sp, lineHeight = 21.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
        }
    }

    if (showStart) StartShiftDialog(session, outlets, activeOutlet, { showStart = false }) { outletId, cash -> vm.startShift(cash, outletId); showStart = false }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartShiftDialog(session: Session, outlets: List<Outlet>, activeOutlet: String?, onDismiss: () -> Unit, onConfirm: (String?, Long) -> Unit) {
    var cash by remember { mutableStateOf("") }
    var outletId by remember { mutableStateOf(activeOutlet ?: outlets.firstOrNull()?.id.orEmpty()) }
    var menu by remember { mutableStateOf(false) }
    val owner = session.role == Role.OWNER
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().imePadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Mulai Shift", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            if (owner) {
                Box {
                    KField("Outlet", outlets.firstOrNull { it.id == outletId }?.name ?: "Pilih outlet", {}, enabled = false)
                    Surface(Modifier.matchParentSize().clickable { menu = true }, color = androidx.compose.ui.graphics.Color.Transparent) {}
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, modifier = Modifier.fillMaxWidth(.88f)) {
                        outlets.forEach { o -> DropdownMenuItem(text = { Text(o.name) }, onClick = { outletId = o.id; menu = false }) }
                    }
                }
            } else {
                KCard(color = MaterialTheme.colorScheme.primaryContainer) {
                    Text("Outlet", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(outlets.firstOrNull { it.id == session.outletId }?.name ?: "Outlet kasir", fontWeight = FontWeight.Bold)
                    Text("Outlet mengikuti akun kasir", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            KField("Kas Awal", cash, { cash = it }, number = true, placeholder = "mis. 200.000")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KSecondary("BATAL", modifier = Modifier.weight(1f), onClick = onDismiss)
                val amount = cash.toLongOrNull()
                KPrimary("MULAI SHIFT", enabled = amount != null && amount >= 0L && (!owner || outletId.isNotBlank()), modifier = Modifier.weight(1f)) {
                    amount?.let { onConfirm(if (owner) outletId else session.outletId, it) }
                }
            }
        }
    }
}
