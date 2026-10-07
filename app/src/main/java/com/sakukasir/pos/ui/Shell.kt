package com.sakukasir.pos.ui

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.sakukasir.pos.domain.*

@Composable
fun rememberOnlineState(): Boolean {
    val context = LocalContext.current
    val cm = remember { context.getSystemService(ConnectivityManager::class.java) }
    fun current(): Boolean = cm.activeNetwork?.let { n ->
        cm.getNetworkCapabilities(n)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    } ?: false
    var online by remember { mutableStateOf(current()) }
    DisposableEffect(cm) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { online = true }
            override fun onLost(network: Network) { online = current() }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                online = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        }
        cm.registerDefaultNetworkCallback(callback)
        onDispose { cm.unregisterNetworkCallback(callback) }
    }
    return online
}

@Composable
fun AppShell(vm: PosViewModel, onLogout: () -> Unit) {
    val s by vm.state.collectAsState()
    val user = s.user ?: return
    val nav = rememberNavController()
    var drawer by remember { mutableStateOf(false) }
    val online = rememberOnlineState()
    val routes = if (user.role == Role.OWNER) listOf("dashboard", "pos", "reports") else buildList {
        if (user.can(Permission.DASHBOARD)) add("dashboard")
        if (user.can(Permission.POS)) add("pos")
        if (user.can(Permission.TRANSACTIONS)) add("transactions")
        if (user.can(Permission.SHIFT)) add("shift")
    }.take(4)
    if (routes.isEmpty()) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Lock, null, Modifier.size(40.dp)); Text("Tidak ada akses fitur. Hubungi owner.", modifier = Modifier.padding(16.dp)); TextButton(onClick = onLogout) { Text("Keluar") }
        }
        return
    }
    LaunchedEffect(Unit) { nav.navigate(routes.first()) { popUpTo(0) } }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(36.dp).clickable { drawer = true },
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(10.dp)
                            ) { Box(contentAlignment = Alignment.Center) { Text("SK", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleSmall) } }
                            Spacer(Modifier.width(10.dp))
                            SkOutletChip(s.selectedOutlet, user.role == Role.OWNER) { if (user.role == Role.OWNER) drawer = true }
                        }
                    },
                    navigationIcon = {},
                    actions = {
                        SkSyncDot(online = online)
                        IconButton(onClick = {}) { Icon(Icons.Default.Print, contentDescription = "Printer") }
                        Box {
                            IconButton(onClick = { vm.markNotificationsRead() }) { Icon(Icons.Default.NotificationsNone, contentDescription = "Notifikasi") }
                            if (vm.notifications.collectAsState().value.any { !it.read }) {
                                Box(Modifier.size(7.dp).background(MaterialTheme.colorScheme.error, RoundedCornerShape(50)).align(Alignment.TopEnd).offset((-7).dp, 9.dp))
                            }
                        }
                        IconButton(onClick = { if (user.can(Permission.THEME)) vm.toggleTheme() }) {
                            Icon(if (s.darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = "Tema")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
                if (!online) {
                    Surface(color = Color(0xFFFFF2C7), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            SkSyncDot(online = false)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Mode offline · ${vm.syncQueue.collectAsState().value.size} transaksi menunggu", style = MaterialTheme.typography.bodyLarge)
                            }
                            TextButton(onClick = { vm.sync() }, enabled = false) { Text("Sync sekarang", color = Color(0xFFD97706)) }
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                routes.forEach { route ->
                    val selected = nav.currentBackStackEntryAsState().value?.destination?.route == route
                    val icon = when (route) {
                        "dashboard" -> Icons.Default.GridView
                        "pos" -> Icons.Default.GridOn
                        "reports" -> Icons.Default.ShowChart
                        "transactions" -> Icons.Default.ReceiptLong
                        else -> Icons.Default.Schedule
                    }
                    val label = when (route) { "reports" -> "Laporan"; "transactions" -> "Transaksi"; "shift" -> "Shift"; else -> route.replaceFirstChar { it.uppercase() } }
                    NavigationBarItem(selected, { nav.navigate(route) { launchSingleTop = true } }, icon = { Icon(icon, null) }, label = { Text(label) })
                }
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = routes.first(), Modifier.padding(pad).fillMaxSize()) {
            composable("dashboard") { DashboardScreen(vm) }
            composable("pos") { PosScreen(vm) }
            composable("reports") { ReportScreen(vm) }
            composable("transactions") { TransactionScreen(vm) }
            composable("shift") { ShiftScreen(vm) }
            composable("manage") { ManageScreen(vm) }
            composable("expenses") { ExpenseScreen(vm) }
            composable("printer") { PrinterScreen(vm) }
            composable("profile") { ProfileScreen(vm) }
            composable("audit") { AuditScreen(vm) }
            composable("security") { SecurityScreen(vm) }
            composable("notifications") { NotificationScreen(vm) }
            composable("qris") { QrisScreen(vm) }
            composable("theme") { ThemeScreen(vm) }
        }
    }

    if (drawer) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = .32f)).clickable { drawer = false }) {
            Surface(
                Modifier.fillMaxHeight().width(328.dp).clickable(enabled = false) {},
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                if (user.role == Role.OWNER) OwnerDrawer(vm, nav, onClose = { drawer = false }) { drawer = false; onLogout() } else CashierDrawer(vm, nav, onClose = { drawer = false }) { drawer = false; onLogout() }
            }
        }
    }
}

@Composable
private fun DrawerHeader(title: String, subtitle: String, onClose: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(42.dp), color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(10.dp)) {
                Box(contentAlignment = Alignment.Center) { Text("SK", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleMedium) }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleLarge); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, null) }
        }
        HorizontalDivider()
    }
}

@Composable
private fun OwnerDrawer(vm: PosViewModel, nav: NavHostController, onClose: () -> Unit, onLogout: () -> Unit) {
    var katalog by remember { mutableStateOf(true) }
    var bisnis by remember { mutableStateOf(true) }
    var settings by remember { mutableStateOf(true) }
    val products by vm.products.collectAsState(); val cats by vm.categories.collectAsState(); val settingsState by vm.settings.collectAsState()
    val go: (String) -> Unit = { route -> nav.navigate(route); onClose() }
    Column(Modifier.fillMaxSize()) {
        DrawerHeader("Kelola", "Menu owner", onClose)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            AccordionHeader("KATALOG", katalog) { katalog = !katalog }
            if (katalog) {
                DrawerTextItem("Produk", products.size.toString()) { go("manage") }
                DrawerTextItem("Kategori", cats.size.toString()) { go("manage") }
                val low = products.count { it.trackStock && it.stock in 1..it.lowStock }
                val out = products.count { it.trackStock && it.stock == 0 }
                DrawerTextItem("Stok", if (low + out > 0) "${low + out} LOW" else null, warn = low + out > 0) { go("manage") }
            }
            AccordionHeader("BISNIS", bisnis) { bisnis = !bisnis }
            if (bisnis) {
                DrawerTextItem("Outlet", "2") { go("manage") }
                DrawerTextItem("Kasir", "3") { go("manage") }
            }
            AccordionHeader("PENGATURAN", settings) { settings = !settings }
            if (settings) {
                DrawerTextItem("QRIS", if (settingsState.qrisEnabled) "AKTIF" else "OFF", success = settingsState.qrisEnabled) { go("qris") }
                DrawerTextItem("Printer", if (settingsState.printerConnected) "AKTIF" else "OFF", success = false) { go("printer") }
                DrawerTextItem("Struk") { go("profile") }
                DrawerTextItem("Notifikasi") { go("notifications") }
                DrawerTextItem("Sinkronisasi") { vm.sync(); onClose() }
                DrawerTextItem("Keamanan") { go("security") }
                DrawerTextItem("Audit Log") { go("audit") }
                DrawerTextItem("Tema", if (vm.state.collectAsState().value.darkTheme) "GELAP" else "TERANG") { go("theme") }
                DrawerTextItem("Profil") { go("profile") }
                DrawerTextItem("Tentang") { go("profile") }
            }
        }
        HorizontalDivider()
        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth().padding(20.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFE1E1), contentColor = Color(0xFFCC2E35)), shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Default.Logout, null); Spacer(Modifier.width(8.dp)); Text("Keluar")
        }
    }
}

@Composable
private fun CashierDrawer(vm: PosViewModel, nav: NavHostController, onClose: () -> Unit, onLogout: () -> Unit) {
    val user = vm.state.collectAsState().value.user!!
    val go: (String) -> Unit = { route -> nav.navigate(route); onClose() }
    Column(Modifier.fillMaxSize()) {
        DrawerHeader("Akun Saya", "Pengaturan kasir", onClose)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            DrawerTextItem("Profil") { go("profile") }
            if (user.can(Permission.PRINTER)) DrawerTextItem("Printer") { go("printer") }
            if (user.can(Permission.SYNC)) DrawerTextItem("Sinkronisasi") { vm.sync(); onClose() }
            if (user.can(Permission.EXPENSE)) DrawerTextItem("Pengeluaran") { go("expenses") }
            if (user.can(Permission.THEME)) DrawerTextItem("Tema", if (vm.state.collectAsState().value.darkTheme) "GELAP" else "TERANG") { go("theme") }
            DrawerTextItem("Tentang") { go("profile") }
        }
        HorizontalDivider()
        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth().padding(20.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFE1E1), contentColor = Color(0xFFCC2E35)), shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Default.Logout, null); Spacer(Modifier.width(8.dp)); Text("Keluar")
        }
    }
}

@Composable private fun AccordionHeader(title: String, expanded: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun DrawerTextItem(text: String, badge: String? = null, success: Boolean = false, warn: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 20.dp, end = 20.dp, top = 13.dp, bottom = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (badge != null) {
            val c = when { success -> Color(0xFF219653); warn -> Color(0xFFD97706); else -> MaterialTheme.colorScheme.onSurfaceVariant }
            val bg = when { success -> Color(0xFFDDF7E7); warn -> Color(0xFFFFF0C2); else -> MaterialTheme.colorScheme.surfaceVariant }
            Surface(color = bg, shape = RoundedCornerShape(7.dp)) { Text(badge, color = c, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)) }
        }
    }
}
