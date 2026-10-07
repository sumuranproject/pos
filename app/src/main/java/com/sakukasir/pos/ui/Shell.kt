package com.sakukasir.pos.ui

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
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

    val routes = if (owner) listOf("dashboard", "pos", "reports") else buildList {
        if (user.can(Permission.DASHBOARD)) add("dashboard")
        if (user.can(Permission.POS)) add("pos")
        if (user.can(Permission.TRANSACTIONS)) add("transactions")
        if (user.can(Permission.SHIFT)) add("shift")
    }.take(4)
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
    var lastTab by remember { mutableStateOf(start) }
    LaunchedEffect(route) { if (route != null && route in routes) lastTab = route }
    val toastFn: (String, String) -> Unit = { m, t -> toast = m to t }
    LaunchedEffect(toast) { if (toast != null) { delay(2500); toast = null } }

    val goTab: (String) -> Unit = { r ->
        nav.navigate(r) { popUpTo(nav.graph.findStartDestination().id) { saveState = false }; launchSingleTop = true }
    }
    val goPage: (String) -> Unit = { r -> nav.navigate(r) { launchSingleTop = true } }
    val goHome: () -> Unit = { goTab(if (owner) "dashboard" else if ("pos" in routes) "pos" else routes.first()) }

    CompositionLocalProvider(LocalToast provides toastFn) {
        Box(Modifier.fillMaxSize().background(c.surface)) {
            Column(Modifier.fillMaxSize()) {
                /* ---- header (56dp, card bg, bottom border) ---- */
                Row(
                    Modifier.fillMaxWidth().height(56.dp).background(c.card).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(32.dp).clip(RSm).background(c.primary).clickable { sheet = "menu" }, contentAlignment = Alignment.Center) {
                            Txt("SK", 13, FontWeight.Bold, Color.White, spacing = -.26f)
                        }
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
                        IconBtn(if (s.darkTheme) SkIcons.Sun else SkIcons.Moon, { if (user.can(Permission.THEME)) vm.toggleTheme() else toastFn("Tidak ada akses tema", "error") })
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
                        enterTransition = { EnterTransition.None }, exitTransition = { ExitTransition.None },
                        popEnterTransition = { EnterTransition.None }, popExitTransition = { ExitTransition.None }
                    ) {
                        composable("dashboard") { DashboardScreen(vm) }
                        composable("pos") { PosScreen(vm) }
                        composable("reports") { ReportScreen(vm) }
                        composable("transactions") { TransactionScreen(vm) }
                        composable("shift") { ShiftScreen(vm) }
                        composable("products") { ProductsPage(vm, goHome) }
                        composable("categories") { CategoriesPage(vm, goHome) }
                        composable("inventory") { InventoryPage(vm, goHome) }
                        composable("outlets") { OutletsPage(vm, goHome) }
                        composable("workers") { WorkersPage(vm, goHome) }
                        composable("qris") { QrisScreen(vm, goHome) }
                        composable("printer") { PrinterScreen(vm, goHome) }
                        composable("receipt") { ReceiptSettingsPage(vm, goHome) }
                        composable("notif-settings") { NotifSettingsPage(vm, goHome) }
                        composable("sync-settings") { SyncSettingsPage(vm, goHome) }
                        composable("security") { SecurityScreen(vm, goHome) }
                        composable("audit") { AuditScreen(vm, goHome) }
                        composable("theme") { ThemeScreen(vm, goHome) }
                        composable("profile") { ProfileScreen(vm, goHome) { confirmLogout = true } }
                        composable("about") { AboutScreen(goHome) }
                        composable("expenses") { ExpensesPage(vm, goHome) }
                    }
                }

                /* ---- bottom nav (64dp) ---- */
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))
                Row(Modifier.fillMaxWidth().height(64.dp).background(c.card).navigationBarsPadding()) {
                    routes.forEach { r ->
                        val icon: ImageVector = when (r) { "dashboard" -> SkIcons.Dashboard; "pos" -> SkIcons.Pos; "reports" -> SkIcons.Chart; "transactions" -> SkIcons.Receipt; else -> SkIcons.Clock }
                        val label = when (r) { "dashboard" -> "Dashboard"; "pos" -> "POS"; "reports" -> "Laporan"; "transactions" -> "Transaksi"; else -> "Shift" }
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
            "menu" -> MenuSheet(vm, owner, user, { sheet = null }, goPage) { confirmLogout = true; sheet = null }
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

/* ---------------- menu sheet ---------------- */
private data class MenuItem(
    val label: String,
    val route: String,
    val count: Int? = null,
    val badge: String? = null,
    val badgeKind: String = "muted"
)

@Composable
private fun MenuSheet(
    vm: PosViewModel,
    owner: Boolean,
    user: User,
    close: () -> Unit,
    go: (String) -> Unit,
    logout: () -> Unit
) {
    val c = Sk.c
    val products by vm.products.collectAsState()
    val cats by vm.categories.collectAsState()
    val outlets by vm.outlets.collectAsState()
    val workers by vm.workers.collectAsState()
    val settings by vm.settings.collectAsState()
    val queue by vm.syncQueue.collectAsState()
    val audit by vm.audit.collectAsState()
    val dark = vm.state.collectAsState().value.darkTheme
    val open = remember { mutableStateMapOf("katalog" to true, "bisnis" to true, "pengaturan" to true) }

    SkSheet(close) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(32.dp).clip(RSm).background(c.primary), contentAlignment = Alignment.Center) { Txt("SK", 13, FontWeight.Bold, Color.White, spacing = -.26f) }
            Column(Modifier.weight(1f)) {
                Txt(if (owner) "Kelola" else "Akun Saya", 15, FontWeight.SemiBold)
                Txt(if (owner) "Menu owner" else "Pengaturan kasir", 12, color = c.textMuted)
            }
        }
        Spacer(Modifier.height(12.dp))

        if (owner) {
            val low = products.count { it.trackStock && it.stock > 0 && it.stock <= it.lowStock }
            val out = products.count { it.trackStock && it.stock == 0 }
            val groups = listOf(
                Triple("katalog", "Katalog", listOf(
                    MenuItem("Produk", "products", count = products.size),
                    MenuItem("Kategori", "categories", count = cats.size),
                    MenuItem("Stok", "inventory", badge = if (low + out > 0) "${low + out} low" else null, badgeKind = "warn")
                )),
                Triple("bisnis", "Bisnis", listOf(
                    MenuItem("Outlet", "outlets", count = outlets.size),
                    MenuItem("Kasir", "workers", count = workers.size)
                )),
                Triple("pengaturan", "Pengaturan", listOf(
                    MenuItem("QRIS", "qris", badge = if (settings.qrisEnabled) "Aktif" else "Off", badgeKind = if (settings.qrisEnabled) "cash" else "muted"),
                    MenuItem("Printer", "printer", badge = if (settings.printerConnected) "On" else "Off", badgeKind = if (settings.printerConnected) "cash" else "muted"),
                    MenuItem("Struk", "receipt"),
                    MenuItem("Notifikasi", "notif-settings"),
                    MenuItem("Sinkronisasi", "sync-settings", count = queue.size.takeIf { it > 0 }),
                    MenuItem("Keamanan", "security"),
                    MenuItem("Audit Log", "audit", count = audit.size.takeIf { it > 0 }),
                    MenuItem("Tema", "theme", badge = if (dark) "Gelap" else "Terang"),
                    MenuItem("Profil", "profile"),
                    MenuItem("Tentang", "about")
                ))
            )
            groups.forEach { (id, title, items) ->
                val isOpen = open[id] == true
                Row(Modifier.fillMaxWidth().clip(RSm).clickable { open[id] = !isOpen }.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Txt(title.uppercase(), 11, FontWeight.SemiBold, c.textMuted, spacing = .66f, modifier = Modifier.weight(1f))
                    SkIcon(SkIcons.ChevronDown, 14.dp, c.textFaint, if (isOpen) Modifier.graphicsLayer(rotationZ = 180f) else Modifier)
                }
                if (isOpen) items.forEach { item -> MenuRow(item) { close(); go(item.route) } }
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))
            }
        } else {
            val items = buildList {
                if (user.can(Permission.PROFILE)) add(MenuItem("Profil", "profile"))
                if (user.can(Permission.PRINTER)) add(MenuItem("Printer", "printer"))
                if (user.can(Permission.SYNC)) add(MenuItem("Sinkronisasi", "sync-settings", count = queue.size.takeIf { it > 0 }))
                if (user.can(Permission.EXPENSE)) add(MenuItem("Pengeluaran", "expenses"))
                if (user.can(Permission.THEME)) add(MenuItem("Tema", "theme", badge = if (dark) "Gelap" else "Terang"))
                add(MenuItem("Tentang", "about"))
            }
            items.forEach { MenuRow(it) { close(); go(it.route) } }
        }

        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))
        Row(
            Modifier.fillMaxWidth().clip(RSm).background(c.alertSoft).clickable(onClick = logout).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SkIcon(SkIcons.Logout, 18.dp, c.alert)
            Txt("Keluar", 14, FontWeight.SemiBold, c.alert)
        }
    }
}
}

@Composable
private fun MenuRow(item: MenuItem, onClick: () -> Unit) {
    val c = Sk.c
    Row(
        Modifier.fillMaxWidth().clip(RSm).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Txt(item.label, 14, FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1)
        if (item.badge != null) {
            val (bg, fg) = when (item.badgeKind) {
                "warn" -> c.warnSoft to c.warn
                "cash" -> c.cashSoft to c.cash
                else -> c.surfaceAlt to c.textMuted
            }
            Tag(item.badge, bg, fg)
        }
        if (item.count != null && item.count != 0) {
            Box(Modifier.clip(RPill).background(c.surfaceAlt).padding(horizontal = 8.dp, vertical = 2.dp)) { Txt(item.count.toString(), 12, FontWeight.SemiBold, c.textMuted) }
        }
    }
}
