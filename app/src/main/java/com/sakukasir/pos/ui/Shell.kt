package com.sakukasir.pos.ui

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.sakukasir.pos.domain.*
import kotlinx.coroutines.delay

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

/** .app-main: padding 16, scrollable. [overlay] is drawn above the content (cart bar / FAB). */
@Composable
fun Screen(bottomSpace: Dp = 24.dp, overlay: (@Composable BoxScope.() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = bottomSpace), content = content)
        if (overlay != null) overlay()
    }
}

@Composable
fun SyncDot(status: String, size: Int = 10) { // synced | pending | error
    val c = Sk.c
    val (fg, soft) = when (status) { "pending" -> c.warn to c.warnSoft; "error" -> c.alert to c.alertSoft; else -> c.cash to c.cashSoft }
    Box(Modifier.size((size + 6).dp).background(soft, CircleShape), contentAlignment = Alignment.Center) {
        Box(Modifier.size(size.dp).background(fg, CircleShape))
    }
}

@Composable
fun AppShell(vm: PosViewModel, onLogout: () -> Unit) {
    val s by vm.state.collectAsState()
    val user = s.user ?: return
    val c = Sk.c
    val nav = rememberNavController()
    var sheet by remember { mutableStateOf<String?>(null) } // outlet | notif | sync
    var confirmLogout by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<Pair<String, String>?>(null) }
    val systemOnline = rememberOnlineState()
    val online = systemOnline && !s.simulateOffline
    val queue by vm.syncQueue.collectAsState()
    val notifs by vm.notifications.collectAsState()
    val outlets by vm.outlets.collectAsState()
    val activeShift by vm.shift.collectAsState()
    val owner = user.role == Role.OWNER

    val routes = listOf("pos", "checkout", "reports")
    if (routes.isEmpty()) {
        Column(Modifier.fillMaxSize().background(c.surface), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            SkIcon(SkIcons.Lock, 40.dp, c.textFaint)
            Txt("Tidak ada akses fitur. Hubungi owner.", 13, color = c.textMuted, modifier = Modifier.padding(16.dp))
            BtnLink("Keluar", onLogout)
        }
        return
    }
    val start = if (!owner && "pos" in routes) "pos" else routes.first()
    val backEntry by nav.currentBackStackEntryAsState()
    val route = backEntry?.destination?.route
    val inManage = route == "manage" || route?.startsWith("m/") == true
    var lastTab by remember { mutableStateOf(start) }
    LaunchedEffect(route) { if (route != null && route in routes) lastTab = route }
    val toastFn: (String, String) -> Unit = { m, t -> toast = m to t }
    LaunchedEffect(toast) { if (toast != null) { delay(2500); toast = null } }

    val goTab: (String) -> Unit = { r ->
        nav.navigate(r) { popUpTo(nav.graph.findStartDestination().id) { saveState = false }; launchSingleTop = true }
    }
    val goPage: (String) -> Unit = { r -> nav.navigate(r) { launchSingleTop = true } }
    val goManage: () -> Unit = {
        if (route == "manage") goTab(lastTab)
        else if (!nav.popBackStack("manage", false)) nav.navigate("manage") { launchSingleTop = true }
    }
    val backToManage: () -> Unit = {
        if (!nav.popBackStack("manage", false)) nav.navigate("manage") { launchSingleTop = true }
    }

    CompositionLocalProvider(LocalToast provides toastFn) {
        Box(Modifier.fillMaxSize().background(c.surface)) {
            Column(Modifier.fillMaxSize()) {
                /* ---- header (56dp, card bg, bottom border) ---- */
                Row(
                    Modifier.fillMaxWidth().height(56.dp).background(c.card).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BrandMark(32, 8)
                        val chipMod = Modifier.widthIn(max = 180.dp).clip(RPill).background(c.surfaceAlt).border(1.dp, c.border, RPill)
                        Row(
                            (if (owner) chipMod.clickable { sheet = "outlet" } else chipMod).padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Txt(if (owner) s.selectedOutlet else (user.outlet.ifBlank { s.selectedOutlet }), 13, FontWeight.Medium, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                            if (owner) SkIcon(SkIcons.ChevronDown, 14.dp, c.text)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Box(Modifier.size(40.dp).clip(RSm).clickable { sheet = "sync" }, contentAlignment = Alignment.Center) {
                            SyncDot(if (!online || queue.isNotEmpty()) "pending" else "synced")
                        }
                        IconBtn(SkIcons.Printer, { if (owner || user.can(Permission.PRINTER)) goPage("printer") else toastFn("Tidak ada akses printer", "error") })
                        IconBtn(SkIcons.Bell, { sheet = "notif"; vm.markNotificationsRead() }, badge = notifs.any { !it.read })
                        IconBtn(SkIcons.Lines, { if (route == "manage") goTab(lastTab) else goManage() })
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))
                if (!online) {
                    Row(
                        Modifier.fillMaxWidth().background(c.warnSoft).padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SyncDot("pending", 8)
                        Txt(if (queue.isEmpty()) "Mode offline" else "Mode offline · ${queue.size} transaksi menunggu", 13, FontWeight.Medium, modifier = Modifier.weight(1f))
                        BtnLink("Sync sekarang", { toastFn("Tidak ada koneksi", "error") }, color = c.warn)
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))
                }

                /* ---- content ---- */
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    NavHost(
                        nav, startDestination = start, modifier = Modifier.fillMaxSize(),
                        enterTransition = { if (initialState.destination.route == "manage" || targetState.destination.route == "manage") slideInHorizontally(tween(250)) { it } else EnterTransition.None },
                        exitTransition = { if (initialState.destination.route == "manage" || targetState.destination.route == "manage") slideOutHorizontally(tween(200)) { -it } else ExitTransition.None },
                        popEnterTransition = { if (initialState.destination.route == "manage" || targetState.destination.route == "manage") slideInHorizontally(tween(200)) { -it } else EnterTransition.None },
                        popExitTransition = { if (initialState.destination.route == "manage" || targetState.destination.route == "manage") slideOutHorizontally(tween(200)) { it } else ExitTransition.None }
                    ) {
                        composable("pos") { PosScreen(vm) }
                        composable("checkout") { CheckoutScreen(vm) { goTab("pos") } }
                        composable("reports") { ReportScreen(vm) }
                        composable("transactions") { TransactionScreen(vm) }
                        composable("shift") { ShiftScreen(vm) }
                        composable("manage") { ManageScreen(vm, owner, user, goPage) }
                        composable("manage-shift") { ShiftScreen(vm, backToManage) }
                        composable("products") { ProductsPage(vm, backToManage) }
                        composable("categories") { CategoriesPage(vm, backToManage) }
                        composable("inventory") { InventoryPage(vm, backToManage) }
                        composable("outlets") { OutletsPage(vm, backToManage) }
                        composable("workers") { WorkersPage(vm, backToManage) }
                        composable("qris") { QrisScreen(vm, backToManage) }
                        composable("printer") { PrinterScreen(vm, backToManage) }
                        composable("receipt") { ReceiptSettingsPage(vm, backToManage) }
                        composable("notif-settings") { NotifSettingsPage(vm, backToManage) }
                        composable("sync-settings") { SyncSettingsPage(vm, backToManage) }
                        composable("security") { SecurityScreen(vm, backToManage) }
                        composable("audit") { AuditScreen(vm, backToManage) }
                        composable("theme") { ThemeScreen(vm, backToManage) }
                        composable("profile") { ProfileScreen(vm, backToManage) { confirmLogout = true } }
                        composable("about") { AboutScreen(backToManage) }
                        composable("expenses") { ExpensesPage(vm, backToManage) }
                    }
                }

                /* ---- bottom nav (64dp, hidden on Kelola) ---- */
                if (!inManage) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))
                    Row(Modifier.fillMaxWidth().height(64.dp).background(c.card).navigationBarsPadding()) {
                        routes.forEach { r ->
                            val icon: ImageVector = when (r) { "pos" -> SkIcons.Pos; "checkout" -> SkIcons.Bag; else -> SkIcons.Chart }
                            val label = when (r) { "pos" -> "Kasir"; "checkout" -> "Checkout"; else -> "Laporan" }
                            val on = lastTab == r
                            Column(
                                Modifier.weight(1f).fillMaxHeight().clickable { goTab(r) }.padding(horizontal = 4.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)
                            ) {
                                SkIcon(icon, 22.dp, if (on) c.primary else c.textMuted)
                                Txt(label, 11, FontWeight.Medium, if (on) c.primary else c.textMuted, lineHeight = 1.2f)
                            }
                        }
                    }
                }
            }

            /* ---- toast ---- */
            toast?.let { (msg, type) ->
                Box(Modifier.fillMaxWidth().padding(top = 68.dp), contentAlignment = Alignment.TopCenter) {
                    Box(
                        Modifier.padding(horizontal = 16.dp).widthIn(max = 400.dp).fillMaxWidth().shadow(8.dp, RSm).clip(RSm)
                            .background(when (type) { "success" -> c.cash; "error" -> c.alert; else -> c.text })
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) { Txt(msg, 13, FontWeight.Medium, if (type == "") c.surface else Color.White) }
                }
            }

        /* ---- sheets / dialogs ---- */
        when (sheet) {
            "outlet" -> SkSheet({ sheet = null }) {
                Txt("Pilih outlet", 17, FontWeight.SemiBold, lineHeight = 1.3f)
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    outlets.filter { it.active }.forEach { o ->
                        ListRow(onClick = { vm.selectOutlet(o.name); sheet = null; toastFn("Outlet diganti: ${o.name}", "") }) {
                            Column(Modifier.weight(1f)) {
                                Txt(o.name, 14, FontWeight.Medium)
                                Txt(o.address, 12, color = c.textMuted, modifier = Modifier.padding(top = 2.dp))
                            }
                            if (s.selectedOutlet == o.name) Txt("Aktif", 14, FontWeight.SemiBold, c.primary)
                        }
                    }
                }
            }
            "notif" -> SkSheet({ sheet = null }) {
                Txt("Notifikasi", 17, FontWeight.SemiBold, lineHeight = 1.3f)
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (notifs.isEmpty()) EmptyState("Belum ada notifikasi")
                    notifs.forEach { n -> NotifCard(n) }
                }
            }
            "sync" -> SkSheet({ sheet = null }) {
                Txt("Sinkronisasi", 17, FontWeight.SemiBold, lineHeight = 1.3f)
                Txt("Status: " + if (online) "Online" else "Offline", 12, color = c.textMuted, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
                KeyRow("Pending", queue.size.toString(), divider = false)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Btn("Sync sekarang", { if (online) { vm.sync(); sheet = null; toastFn("Sinkronisasi berjalan…", "success") } else toastFn("Tidak ada koneksi", "error") }, Modifier.fillMaxWidth())
                    Btn(if (s.simulateOffline) "Kembali online" else "Simulasikan offline", { vm.toggleOffline(); sheet = null }, Modifier.fillMaxWidth(), kind = 1)
                }
            }
        }
        if (confirmLogout) {
            if (activeShift != null) ConfirmModal("Shift masih aktif", "Tutup shift dulu sebelum keluar. Kalau tetap keluar, shift akan tercatat sebagai anomali.", "Tetap keluar", true, true, { confirmLogout = false }) { confirmLogout = false; onLogout() }
            else ConfirmModal("Keluar dari SakuKasir?", "Kamu perlu login lagi untuk masuk.", "Keluar", false, true, { confirmLogout = false }) { confirmLogout = false; onLogout() }
        }
    }
}
}

@Composable
private fun NotifCard(n: AppNotification) {
    val c = Sk.c
    val (label, col) = when (n.type) {
        "stock_low" -> "⚠ Stok menipis" to c.warn
        "stock_out" -> "⚠ Stok habis" to c.alert
        "void" -> "⚠ Void" to c.alert
        "refund" -> "↩ Refund" to c.warn
        "transaction" -> "✓ Transaksi" to c.cash
        else -> "↓ Sistem" to c.textMuted
    }
    val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date(n.time))
    SkCard(Modifier.fillMaxWidth(), padding = 12) {
        Txt(label.uppercase(), 10, FontWeight.SemiBold, col, mono = true, spacing = .3f)
        Txt(n.title, 14, FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
        Txt("${n.body} · $time", 12, color = c.textMuted, modifier = Modifier.padding(top = 2.dp))
    }
}
