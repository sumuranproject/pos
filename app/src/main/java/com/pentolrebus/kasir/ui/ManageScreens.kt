package com.pentolrebus.kasir.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pentolrebus.kasir.R
import com.pentolrebus.kasir.domain.*
import com.pentolrebus.kasir.util.BluetoothPrinter
import com.pentolrebus.kasir.util.QrisStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val UNITS = listOf("porsi", "gelas", "pcs", "botol", "bungkus")

// ------------------------------------------------------------------ Router
@Composable
fun ManageRouter(route: String, arg: String?, vm: PosViewModel, session: Session, nav: Nav, printer: BluetoothPrinter, themeMode: Int, onThemeMode: (Int) -> Unit, onCloseShift: () -> Unit) {
    when (route) {
        "Produk" -> ProductListScreen(vm, nav)
        "Produk:detail" -> ProductDetailScreen(vm, arg, nav)
        "Produk:form" -> ProductFormScreen(vm, arg, nav)
        "Kategori" -> CategoryListScreen(vm, nav)
        "Kategori:form" -> CategoryFormScreen(vm, arg, nav)
        "Stok" -> StockListScreen(vm, nav)
        "Stok:detail" -> StockDetailScreen(vm, arg, nav)
        "Outlet" -> OutletListScreen(vm, session, nav)
        "Outlet:detail" -> OutletDetailScreen(vm, session, arg, nav)
        "Outlet:form" -> OutletFormScreen(vm, arg, nav)
        "Pekerja" -> WorkerListScreen(vm, nav)
        "Pekerja:detail" -> WorkerDetailScreen(vm, arg, nav)
        "Pekerja:form" -> WorkerFormScreen(vm, arg, nav)
        "Owner", "Profil" -> ProfileScreen(vm, session, nav)
        "Owner:form" -> ProfileFormScreen(vm, session, nav)
        "Bisnis" -> BusinessScreen(vm, nav)
        "Bisnis:form" -> BusinessFormScreen(vm, nav)
        "QRIS" -> QrisSettingsScreen(vm, session, nav)
        "Printer" -> PrinterScreen(printer, nav)
        "Dropbox" -> DropboxScreen(nav)
        "Notifikasi" -> NotificationsScreen(vm, nav)
        "Edit Struk" -> ReceiptSettingsScreen(vm, printer, nav)
        "Tema" -> ThemeScreen(themeMode, onThemeMode, nav)
        "Sinkron" -> SyncScreen(vm, nav)
        "Tentang" -> AboutScreen(nav)
        "Riwayat Transaksi" -> TransactionHistoryScreen(vm, session, nav)
        "Transaksi:detail" -> TransactionDetailScreen(vm, arg, printer, nav)
        "Bukti:QRIS" -> QrisProofScreen(vm, arg, nav)
        "Riwayat Shift" -> ShiftHistoryScreen(vm, session, nav)
        "Shift:detail" -> ShiftDetailScreen(vm, arg, nav, onCloseShift)
        "Pengeluaran" -> ExpenseListScreen(vm, nav)
        "Pengeluaran:form" -> ExpenseFormScreen(vm, arg, nav)
        "Pengeluaran:detail" -> ExpenseDetailScreen(vm, arg, nav)
        "Keuangan" -> FinancialReportScreen(vm, nav)
        else -> KPage(route, nav) { KEmpty("Halaman tidak tersedia", route, R.drawable.ic_more) }
    }
}

// ------------------------------------------------------------------ Pengaturan
@Composable
fun SettingsScreen(vm: PosViewModel, session: Session, nav: Nav, themeMode: Int, onLogout: () -> Unit) {
    val business by vm.business.collectAsState()
    val outlets by vm.outlets.collectAsState()
    val activeOutlet by vm.activeOutlet.collectAsState()
    val syncing by vm.syncing.collectAsState()
    val ctx = LocalContext.current
    val printer = remember(ctx) { BluetoothPrinter(ctx) }
    val qrisReady = remember(activeOutlet) { QrisStore.has(ctx, activeOutlet.orEmpty()) }
    val printerReady = remember { runCatching { printer.pairedDevices().isNotEmpty() }.getOrDefault(false) }
    val owner = session.role == Role.OWNER
    val display = session.username.ifBlank { if (owner) "Owner" else "Kasir" }
    KPage("Pengaturan", null) {
        if (owner) {
            Text("Owner · Bisnis ${business?.name ?: "—"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            KListGroup {
                KListItem("Produk", icon = R.drawable.ic_product, onClick = { nav.open("Produk", null) })
                KListItem("Kategori", icon = R.drawable.ic_category, onClick = { nav.open("Kategori", null) })
                KListItem("Stok", icon = R.drawable.ic_stock, onClick = { nav.open("Stok", null) })
                KListItem("Outlet", icon = R.drawable.ic_store, onClick = { nav.open("Outlet", null) })
                KListItem("Kasir / Pekerja", icon = R.drawable.ic_people, onClick = { nav.open("Pekerja", null) })
                KListItem("Owner", icon = R.drawable.ic_owner, onClick = { nav.open("Owner", null) })
                KListItem("Bisnis", icon = R.drawable.ic_business, divider = false, onClick = { nav.open("Bisnis", null) })
            }
            KLabel("PEMBAYARAN & PERANGKAT")
            KListGroup {
                KListItem("QRIS", trailing = if (qrisReady) "Aktif" else "Belum diatur", icon = R.drawable.ic_qris, onClick = { nav.open("QRIS", null) })
                KListItem("Printer", trailing = if (printerReady) "Siap" else "Belum dipasang", icon = R.drawable.ic_printer, onClick = { nav.open("Printer", null) })
                KListItem("Dropbox", icon = R.drawable.ic_cloud, onClick = { nav.open("Dropbox", null) })
                KListItem("Edit Struk", icon = R.drawable.ic_edit_struk, divider = false, onClick = { nav.open("Edit Struk", null) })
            }
        } else {
            KCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(42.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Box(contentAlignment = Alignment.Center) { Text(display.take(1).uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold) }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(display, fontWeight = FontWeight.ExtraBold)
                        Text("Kasir · ${outlets.firstOrNull()?.name ?: "Outlet"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            KListGroup {
                KListItem("Profil", icon = R.drawable.ic_person, onClick = { nav.open("Profil", null) })
                KListItem("Outlet", trailing = outlets.firstOrNull()?.name ?: "Outlet", icon = R.drawable.ic_store, onClick = null)
                KListItem("Printer Bluetooth", trailing = if (printerReady) "Siap" else "Terputus", icon = R.drawable.ic_printer, onClick = { nav.open("Printer", null) })
            }
        }
        KLabel("APLIKASI")
        KListGroup {
            KListItem("Tema", trailing = when (themeMode) { 1 -> "Terang"; 2 -> "Gelap"; else -> "Sistem" }, icon = R.drawable.ic_theme, onClick = { nav.open("Tema", null) })
            KListItem("Sinkronisasi", trailing = if (syncing) "Menyinkronkan…" else "Tersinkron", icon = R.drawable.ic_sync, onClick = { nav.open("Sinkron", null) })
            KListItem("Notifikasi", icon = R.drawable.ic_notification, onClick = { nav.open("Notifikasi", null) })
            KListItem("Tentang aplikasi", icon = R.drawable.ic_info, divider = false, onClick = { nav.open("Tentang", null) })
        }
        KSecondary("KELUAR", danger = true, onClick = onLogout)
    }
}

// ------------------------------------------------------------------ Produk
@Composable
fun ProductListScreen(vm: PosViewModel, nav: Nav) {
    val products by vm.products.collectAsState()
    val cats by vm.categories.collectAsState()
    var q by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf<String?>(null) }
    KPage("Produk", nav, fab = { nav.open("Produk:form", null) }) {
        KSearchField(q, { q = it }, "Cari produk…")
        KChips(listOf("Semua") + cats.map { it.name }, cats.firstOrNull { it.id == cat }?.name ?: "Semua") { n -> cat = cats.firstOrNull { it.name == n }?.id }
        val list = products.filter { (q.isBlank() || it.name.contains(q, true)) && (cat == null || it.categoryId == cat) }
        if (products.isEmpty()) KEmpty("Belum ada produk", "Tambahkan produk pertama Anda.", R.drawable.ic_product)
        if (list.isNotEmpty()) KListGroup { list.forEachIndexed { i, p ->
            val low = p.stockEnabled && p.stock <= p.lowStock
            KListItem(p.name + if (!p.active) " (nonaktif)" else "", rp(p.price) + " / " + p.unit, if (p.stockEnabled) "Stok ${p.stock}" else "", if (low) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, R.drawable.ic_product, onClick = { nav.open("Produk:detail", p.id) }, divider = i != list.lastIndex)
        } }
    }
}

@Composable
fun ProductDetailScreen(vm: PosViewModel, id: String?, nav: Nav) {
    val products by vm.products.collectAsState(); val cats by vm.categories.collectAsState()
    val p = products.firstOrNull { it.id == id }
    var del by remember { mutableStateOf(false) }
    KPage("Detail Produk", nav) {
        if (p == null) { KEmpty("Produk tidak ditemukan", "", R.drawable.ic_product); return@KPage }
        KCard { Text(p.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold); Text(rp(p.price) + " / " + p.unit) }
        KCard { KRow("Kategori", cats.firstOrNull { it.id == p.categoryId }?.name ?: "—"); KRow("Status", if (p.active) "Aktif" else "Nonaktif"); KRow("Lacak stok", if (p.stockEnabled) "ON" else "OFF"); if (p.stockEnabled) { KRow("Jumlah", "${p.stock} ${p.unit}"); KRow("Batas menipis", "${p.lowStock}") } }
        KPrimary("UBAH PRODUK") { nav.open("Produk:form", p.id) }
        KSecondary(if (p.active) "NONAKTIFKAN" else "AKTIFKAN", danger = p.active) { vm.saveProduct(p.copy(active = !p.active)) }
        KSecondary("HAPUS PRODUK", danger = true) { del = true }
    }
    if (del && p != null) KConfirm("Hapus produk?", "${p.name} akan dihapus permanen. Riwayat transaksi tidak berubah.", "HAPUS", true, { del = false }) { vm.deleteProduct(p.id); del = false; nav.back() }
}

@Composable
fun ProductFormScreen(vm: PosViewModel, id: String?, nav: Nav) {
    val products by vm.products.collectAsState(); val cats by vm.categories.collectAsState()
    val old = products.firstOrNull { it.id == id }
    var step by remember { mutableStateOf(1) }
    var name by remember { mutableStateOf(old?.name ?: "") }
    var catId by remember { mutableStateOf(old?.categoryId ?: "") }
    var price by remember { mutableStateOf(old?.price?.toString() ?: "") }
    var unit by remember { mutableStateOf(old?.unit ?: UNITS.first()) }
    var active by remember { mutableStateOf(old?.active ?: true) }
    var track by remember { mutableStateOf(old?.stockEnabled ?: false) }
    var stock by remember { mutableStateOf(old?.stock?.toString() ?: "0") }
    var low by remember { mutableStateOf(old?.lowStock?.toString() ?: "5") }
    KPage(if (old == null) "Tambah Produk" else "Ubah Produk", nav, subtitle = if (step == 1) "Langkah 1 dari 2 · Info" else "Langkah 2 dari 2 · Stok dan status") {
        if (step == 1) {
            KField("Nama produk", name, { name = it })
            KLabel("Kategori")
            if (cats.isEmpty()) Text("Belum ada kategori. Buat di Pengaturan › Kategori.", style = MaterialTheme.typography.bodySmall)
            KChips(listOf("Tanpa kategori") + cats.map { it.name }, cats.firstOrNull { it.id == catId }?.name ?: "Tanpa kategori") { n -> catId = cats.firstOrNull { it.name == n }?.id ?: "" }
            KField("Harga (Rp)", price, { price = it }, number = true)
            KLabel("Satuan"); KChips(UNITS, unit) { unit = it }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KSecondary("BATAL", modifier = Modifier.weight(1f), onClick = nav.back)
                KPrimary("LANJUT", enabled = name.isNotBlank() && (price.toLongOrNull() ?: 0) > 0, modifier = Modifier.weight(1f)) { step = 2 }
            }
        } else {
            KSwitchRow("Produk aktif", active) { active = it }
            KSwitchRow("Lacak stok", track) { track = it }
            if (track) { KField("Jumlah stok", stock, { stock = it }, number = true); KField("Batas stok menipis", low, { low = it }, number = true); Text("Peringatan muncul bila stok ≤ batas.", style = MaterialTheme.typography.labelSmall) }
            else Text("Stok tidak dilacak. Produk selalu bisa dijual tanpa jumlah stok.", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KSecondary("KEMBALI", modifier = Modifier.weight(1f)) { step = 1 }
                KPrimary("SIMPAN", modifier = Modifier.weight(1f)) {
                    vm.saveProduct((old ?: Product()).copy(name = name.trim(), categoryId = catId, price = price.toLong(), unit = unit, active = active, stockEnabled = track, stock = if (track) stock.toLongOrNull() ?: 0 else 0, lowStock = low.toLongOrNull() ?: 5))
                    nav.back()
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Kategori
@Composable
fun CategoryListScreen(vm: PosViewModel, nav: Nav) {
    val cats by vm.categories.collectAsState(); val products by vm.products.collectAsState()
    KPage("Kategori", nav, fab = { nav.open("Kategori:form", null) }) {
        if (cats.isEmpty()) KEmpty("Belum ada kategori", "Buat kategori sesuai produk Anda.", R.drawable.ic_category)
        if (cats.isNotEmpty()) KListGroup { cats.forEachIndexed { i, c -> KListItem(c.name, "${products.count { it.categoryId == c.id }} produk", icon = R.drawable.ic_category, onClick = { nav.open("Kategori:form", c.id) }, divider = i != cats.lastIndex) } }
    }
}

@Composable
fun CategoryFormScreen(vm: PosViewModel, id: String?, nav: Nav) {
    val cats by vm.categories.collectAsState(); val products by vm.products.collectAsState()
    val old = cats.firstOrNull { it.id == id }
    var name by remember { mutableStateOf(old?.name ?: "") }
    var del by remember { mutableStateOf(false) }
    KPage(if (old == null) "Tambah Kategori" else "Ubah Kategori", nav) {
        KField("Nama kategori", name, { name = it }); Text("Dipakai sebagai filter di layar Kasir.", style = MaterialTheme.typography.labelSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KSecondary("BATAL", modifier = Modifier.weight(1f), onClick = nav.back)
            KPrimary("SIMPAN", enabled = name.isNotBlank(), modifier = Modifier.weight(1f)) { vm.saveCategory(name, old?.id); nav.back() }
        }
        if (old != null) KSecondary("HAPUS KATEGORI", danger = true) { del = true }
    }
    if (del && old != null) KConfirm("Hapus kategori?", "${products.count { it.categoryId == old.id }} produk akan menjadi tanpa kategori.", "HAPUS", true, { del = false }) {
        products.filter { it.categoryId == old.id }.forEach { vm.saveProduct(it.copy(categoryId = "")) }
        vm.deleteCategory(old.id); del = false; nav.back()
    }
}

// ------------------------------------------------------------------ Stok
@Composable
fun StockListScreen(vm: PosViewModel, nav: Nav) {
    val products by vm.products.collectAsState()
    KPage("Stok", nav) {
        if (products.isEmpty()) KEmpty("Belum ada produk", "", R.drawable.ic_stock)
        if (products.isNotEmpty()) KListGroup { products.forEachIndexed { i, p ->
            val low = p.stockEnabled && p.stock <= p.lowStock
            KListItem(p.name, if (!p.stockEnabled) "Stok OFF · tidak dilacak" else if (low) "Stok ON · menipis" else "Stok ON", if (p.stockEnabled) "${p.stock}" else "–", if (low) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface, R.drawable.ic_stock, onClick = { nav.open("Stok:detail", p.id) }, divider = i != products.lastIndex)
        } }
    }
}

@Composable
fun StockDetailScreen(vm: PosViewModel, id: String?, nav: Nav) {
    val products by vm.products.collectAsState()
    val p = products.firstOrNull { it.id == id }
    var mode by remember { mutableStateOf("Tambah") }
    var amount by remember { mutableStateOf("") }
    KPage("Detail Stok", nav) {
        if (p == null) { KEmpty("Produk tidak ditemukan", "", R.drawable.ic_stock); return@KPage }
        if (!p.stockEnabled) { KEmpty("Stok tidak dilacak", "Aktifkan 'Lacak stok' di form produk untuk mengatur jumlah.", R.drawable.ic_stock, "UBAH PRODUK") { nav.open("Produk:form", p.id) }; return@KPage }
        val delta: Long = (amount.toLongOrNull() ?: 0L) * if (mode == "Tambah") 1L else -1L
        KCard(color = MaterialTheme.colorScheme.primaryContainer) { Text(p.name, fontWeight = FontWeight.ExtraBold); Text("${p.stock} ${p.unit}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = if (p.stock <= p.lowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface); if (p.stock <= p.lowStock) Text("Menipis · batas ${p.lowStock}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium) }
        KChips(listOf("Tambah", "Kurangi"), mode) { mode = it }
        KField("Jumlah penyesuaian", amount, { amount = it }, number = true)
        Text("Stok setelah penyesuaian: ${(p.stock + delta).coerceAtLeast(0)} ${p.unit}", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KSecondary("BATAL", modifier = Modifier.weight(1f), onClick = nav.back)
            KPrimary("SIMPAN", enabled = delta != 0L && p.stock + delta >= 0L, modifier = Modifier.weight(1f)) { vm.adjustStock(p.id, delta); nav.back() }
        }
    }
}

// ------------------------------------------------------------------ Outlet
@Composable
fun OutletListScreen(vm: PosViewModel, session: Session, nav: Nav) {
    val outlets by vm.outlets.collectAsState(); val workers by vm.workers.collectAsState(); val active by vm.activeOutlet.collectAsState(); val shifts by vm.shifts.collectAsState()
    KPage("Outlet", nav, subtitle = "Owner › Bisnis › Outlet", fab = { nav.open("Outlet:form", null) }) {
        if (outlets.isEmpty()) KEmpty("Belum ada outlet", "Tambahkan outlet pertama.", R.drawable.ic_store)
        if (outlets.isNotEmpty()) KListGroup { outlets.forEachIndexed { i, o -> KListItem(o.name + if (o.id == active) " · aktif" else "", (o.address ?: "Alamat belum diisi") + "\n${workers.count { it.outletId == o.id }} kasir · ${shifts.count { it.outletId == o.id && it.closedAt == null }} shift aktif", icon = R.drawable.ic_store, onClick = { nav.open("Outlet:detail", o.id) }, divider = i != outlets.lastIndex) } }
    }
}

@Composable
fun OutletDetailScreen(vm: PosViewModel, session: Session, id: String?, nav: Nav) {
    val outlets by vm.outlets.collectAsState(); val workers by vm.workers.collectAsState(); val active by vm.activeOutlet.collectAsState()
    val o = outlets.firstOrNull { it.id == id }
    KPage(o?.name ?: "Outlet", nav, subtitle = "Owner › Bisnis › Outlet") {
        if (o == null) { KEmpty("Outlet tidak ditemukan", "", R.drawable.ic_store); return@KPage }
        KCard { KRow("Alamat", o.address ?: "—"); KRow("Status", if (o.active) "Aktif" else "Nonaktif") }
        KLabel("Kasir terkait")
        val ws = workers.filter { it.outletId == o.id }
        if (ws.isEmpty()) Text("Belum ada kasir di outlet ini.", style = MaterialTheme.typography.bodySmall)
        ws.forEach { w -> KItem(w.displayName.ifBlank { w.username }, if (w.active) "Aktif" else "Nonaktif") { nav.open("Pekerja:detail", w.id) } }
        if (o.id != active) KPrimary("JADIKAN OUTLET AKTIF") { vm.setActiveOutlet(o.id) } else Text("● Outlet ini sedang aktif untuk produk, stok, dan transaksi.", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
        KSecondary("UBAH OUTLET") { nav.open("Outlet:form", o.id) }
    }
}

@Composable
fun OutletFormScreen(vm: PosViewModel, id: String?, nav: Nav) {
    val outlets by vm.outlets.collectAsState()
    val old = outlets.firstOrNull { it.id == id }
    var name by remember { mutableStateOf(old?.name ?: "") }
    var addr by remember { mutableStateOf(old?.address ?: "") }
    var act by remember { mutableStateOf(old?.active ?: true) }
    KPage(if (old == null) "Tambah Outlet" else "Ubah Outlet", nav) {
        KField("Nama outlet", name, { name = it }); KField("Alamat", addr, { addr = it }); KSwitchRow("Outlet aktif", act) { act = it }
        Text("Kasir ditambahkan di menu Kasir / Pekerja.", style = MaterialTheme.typography.labelSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KSecondary("BATAL", modifier = Modifier.weight(1f), onClick = nav.back)
            KPrimary("SIMPAN", enabled = name.isNotBlank(), modifier = Modifier.weight(1f)) { vm.saveOutlet(name, addr, act, old?.id); nav.back() }
        }
    }
}

// ------------------------------------------------------------------ Pekerja
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerListScreen(vm: PosViewModel, nav: Nav) {
    val workers by vm.workers.collectAsState(); val outlets by vm.outlets.collectAsState()
    var sheet by remember { mutableStateOf(false) }
    KPage("Kasir / Pekerja", nav, subtitle = "Owner › Bisnis › Outlet › Pekerja", fab = { sheet = true }) {
        if (workers.isEmpty()) KEmpty("Belum ada kasir", "Owner membuat akun kasir; pekerja tidak mendaftar sendiri.", R.drawable.ic_people)
        if (workers.isNotEmpty()) KListGroup { workers.forEachIndexed { i, w -> KListItem(w.displayName.ifBlank { w.username }, (outlets.firstOrNull { it.id == w.outletId }?.name ?: "—") + " · Kasir", if (w.active) "Aktif" else "Nonaktif", if (w.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, R.drawable.ic_people, onClick = { nav.open("Pekerja:detail", w.id) }, divider = i != workers.lastIndex) } }
    }
    if (sheet) ModalBottomSheet(onDismissRequest = { sheet = false }) {
        Column(Modifier.fillMaxWidth().imePadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Tambah Pekerja", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            WorkerFormBody(vm, null) { sheet = false }
        }
    }
}

@Composable
private fun WorkerFormBody(vm: PosViewModel, old: Worker?, onDone: () -> Unit) {
    val outlets by vm.outlets.collectAsState(); val workers by vm.workers.collectAsState(); val activeOutlet by vm.activeOutlet.collectAsState()
    var name by remember { mutableStateOf(old?.displayName ?: "") }
    var user by remember { mutableStateOf(old?.username ?: "") }
    var password by remember { mutableStateOf("") }
    var outletId by remember { mutableStateOf(old?.outletId ?: activeOutlet ?: outlets.firstOrNull()?.id.orEmpty()) }
    var wa by remember { mutableStateOf(old?.whatsapp ?: "") }
    var act by remember { mutableStateOf(old?.active ?: true) }
    LaunchedEffect(activeOutlet, outlets) { if (outletId.isBlank()) outletId = activeOutlet ?: outlets.firstOrNull()?.id.orEmpty() }
    val u = user.trim().lowercase().replace(" ", "")
    val dup = workers.any { it.username == u && it.id != old?.id }
    val err = when { u.isBlank() -> null; dup -> "Username sudah dipakai"; old == null && password.isNotEmpty() && password.length < 8 -> "Password minimal 8 karakter"; else -> null }
    KField("Nama", name, { name = it }); KField("Username", user, { user = it })
    KSecretField(if (old == null) "Password" else "Password (hanya saat membuat akun)", password, onValue = { password = it }, numeric = false)
    KField("WhatsApp (opsional)", wa, { wa = it })
    KLabel("Outlet"); KChips(outlets.map { it.name }, outlets.firstOrNull { it.id == outletId }?.name ?: "") { n -> outletId = outlets.firstOrNull { it.name == n }?.id ?: outletId }
    if (old != null) KSwitchRow("Akun aktif", act) { act = it }
    err?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium) }
    val passwordOk = if (old == null) password.length >= 8 else password.isEmpty() || password.length >= 8
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        KSecondary("BATAL", modifier = Modifier.weight(1f), onClick = onDone)
        KPrimary("SIMPAN", enabled = name.isNotBlank() && u.isNotBlank() && !dup && passwordOk && outletId.isNotBlank(), modifier = Modifier.weight(1f)) {
            vm.saveWorker((old ?: Worker()).copy(displayName = name.trim(), username = u, outletId = outletId, whatsapp = wa.trim().ifBlank { null }, active = act), password); onDone()
        }
    }
}

@Composable
fun WorkerFormScreen(vm: PosViewModel, id: String?, nav: Nav) {
    val workers by vm.workers.collectAsState()
    val w = workers.firstOrNull { it.id == id }
    KPage(if (w == null) "Tambah Pekerja" else "Ubah Pekerja", nav) { if (id != null && w == null) KEmpty("Pekerja tidak ditemukan", "", R.drawable.ic_people) else WorkerFormBody(vm, w) { nav.back() } }
}

@Composable
fun WorkerDetailScreen(vm: PosViewModel, id: String?, nav: Nav) {
    val workers by vm.workers.collectAsState(); val outlets by vm.outlets.collectAsState()
    val w = workers.firstOrNull { it.id == id }
    KPage("Detail Pekerja", nav) {
        if (w == null) { KEmpty("Pekerja tidak ditemukan", "", R.drawable.ic_people); return@KPage }
        KCard { Text(w.displayName.ifBlank { w.username }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold); Text("Kasir · " + (outlets.firstOrNull { it.id == w.outletId }?.name ?: "—")) }
        KCard { KRow("Username", w.username); KRow("Password", "••••••••"); KRow("WhatsApp", w.whatsapp ?: "—"); KRow("Status", if (w.active) "Aktif" else "Nonaktif") }
        KPrimary("UBAH PEKERJA") { nav.open("Pekerja:form", w.id) }
        KSecondary(if (w.active) "NONAKTIFKAN" else "AKTIFKAN", danger = w.active) { vm.saveWorker(w.copy(active = !w.active), "") }
    }
}

// ------------------------------------------------------------------ Profil / Bisnis
@Composable
fun ProfileScreen(vm: PosViewModel, session: Session, nav: Nav) {
    val business by vm.business.collectAsState(); val outlets by vm.outlets.collectAsState()
    KPage(if (session.role == Role.OWNER) "Profil Owner" else "Profil", nav) {
        KCard { Text(session.username, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold); Text(if (session.role == Role.OWNER) "Owner" else "Kasir") }
        KCard { KRow("Username", session.username); KRow("Bisnis", business?.name ?: "—"); KRow("Outlet", if (session.role == Role.OWNER) "${outlets.size} outlet" else outlets.firstOrNull()?.name ?: "—") }
        if (session.role == Role.OWNER) KPrimary("UBAH PROFIL") { nav.open("Owner:form", null) } else Text("Data akun kasir dikelola oleh Owner.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ProfileFormScreen(vm: PosViewModel, session: Session, nav: Nav) {
    var name by remember { mutableStateOf(session.username) }
    var wa by remember { mutableStateOf("") }
    KPage("Ubah Profil Owner", nav) {
        KField("Nama", name, { name = it }); KField("WhatsApp (opsional)", wa, { wa = it })
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KSecondary("BATAL", modifier = Modifier.weight(1f), onClick = nav.back)
            KPrimary("SIMPAN", enabled = name.isNotBlank(), modifier = Modifier.weight(1f)) { vm.saveProfile(name, wa); nav.back() }
        }
    }
}

@Composable
fun BusinessScreen(vm: PosViewModel, nav: Nav) {
    val b by vm.business.collectAsState(); val outlets by vm.outlets.collectAsState(); val workers by vm.workers.collectAsState()
    KPage("Bisnis", nav, subtitle = "Owner › Bisnis") {
        KCard { KRow("Nama bisnis", b?.name ?: "—"); KRow("Jenis usaha", b?.type ?: "—"); KRow("Telepon", b?.phone ?: "—") }
        KLabel("Outlet milik bisnis")
        outlets.forEach { o -> KItem(o.name, "${workers.count { it.outletId == o.id }} kasir") { nav.open("Outlet:detail", o.id) } }
        KPrimary("UBAH BISNIS") { nav.open("Bisnis:form", null) }
    }
}

@Composable
fun BusinessFormScreen(vm: PosViewModel, nav: Nav) {
    val b by vm.business.collectAsState()
    var name by remember { mutableStateOf(b?.name ?: "") }
    var type by remember { mutableStateOf(b?.type ?: "") }
    var phone by remember { mutableStateOf(b?.phone ?: "") }
    KPage("Ubah Bisnis", nav) {
        KField("Nama bisnis", name, { name = it }); KField("Jenis usaha", type, { type = it }); KField("Telepon (opsional)", phone, { phone = it })
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KSecondary("BATAL", modifier = Modifier.weight(1f), onClick = nav.back)
            KPrimary("SIMPAN", enabled = name.isNotBlank(), modifier = Modifier.weight(1f)) { vm.saveBusiness(name, type, phone); nav.back() }
        }
    }
}

// ------------------------------------------------------------------ QRIS / Printer / Tema / Sinkron / Tentang
@Composable
fun QrisSettingsScreen(vm: PosViewModel, session: Session, nav: Nav) {
    val ctx = LocalContext.current
    val outlet by vm.activeOutlet.collectAsState()
    val oid = outlet ?: session.outletId.orEmpty()
    var version by remember { mutableStateOf(0) }
    val bmp = remember(oid, version) { QrisStore.load(ctx, oid) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null && QrisStore.importFrom(ctx, oid, uri)) version++ }
    KPage("QRIS", nav) {
        if (bmp != null) { Surface(shape = RoundedCornerShape(16.dp), color = androidx.compose.ui.graphics.Color.White) { Image(bmp.asImageBitmap(), "QRIS", Modifier.fillMaxWidth().heightIn(max = 320.dp).padding(8.dp)) }; Text("● QRIS aktif", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
        else KEmpty("QRIS belum diatur", "Unggah gambar QRIS milik outlet agar tampil di layar pembayaran.", R.drawable.ic_qris)
        KPrimary(if (bmp != null) "GANTI QR" else "UNGGAH QRIS") { pick.launch("image/*") }
        if (bmp != null) KSecondary("HAPUS QR", danger = true) { QrisStore.remove(ctx, oid); version++ }
        Text("Gambar QRIS disimpan di perangkat ini saja. Pada perangkat kasir lain, unggah gambar yang sama.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PrinterScreen(printer: BluetoothPrinter, nav: Nav) {
    var devices by remember { mutableStateOf(printer.pairedDevices()) }
    var msg by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<String?>(null) }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) devices = printer.pairedDevices() else msg = "Izin Bluetooth ditolak" }
    KPage("Printer", nav) {
        if (!printer.hasConnectPermission()) { KEmpty("Izin Bluetooth diperlukan", "Beri izin agar printer yang dipasangkan dapat ditemukan.", R.drawable.ic_printer, "BERI IZIN") { perm.launch(Manifest.permission.BLUETOOTH_CONNECT) } }
        else {
            KLabel("Perangkat yang dipasangkan")
            if (devices.isEmpty()) Text("Belum ada printer. Pasangkan printer di pengaturan Bluetooth Android lalu kembali ke sini.", style = MaterialTheme.typography.bodySmall)
            devices.forEach { d -> KItem(d.name, d.address, if (selected == d.address) "Dipilih" else "", MaterialTheme.colorScheme.primary, R.drawable.ic_printer) { selected = d.address } }
            KPrimary("TEST PRINT", enabled = selected != null) {
                val t = Transaction(items = listOf(TransactionItem("test", "TEST PRINT", 0, 1, 0)), subtotal = 0, total = 0)
                msg = printer.print(t, selected!!).fold({ "Test print terkirim" }, { "Gagal terhubung: ${it.message}" })
            }
            KSecondary("MUAT ULANG") { devices = printer.pairedDevices() }
        }
        msg?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
fun ThemeScreen(mode: Int, onMode: (Int) -> Unit, nav: Nav) {
    KPage("Tema", nav) {
        KListGroup {
            listOf("Sistem", "Terang", "Gelap").forEachIndexed { i, label ->
                Row(Modifier.fillMaxWidth().kRoundedClickable(RoundedCornerShape(12.dp)) { onMode(i) }.padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    RadioButton(selected = mode == i, onClick = { onMode(i) })
                }
                if (i != 2) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
fun SyncScreen(vm: PosViewModel, nav: Nav) {
    val tx by vm.transactions.collectAsState(); val ex by vm.expenses.collectAsState(); val shifts by vm.shifts.collectAsState(); val syncing by vm.syncing.collectAsState()
    val all = tx.map { it.syncStatus } + ex.map { it.syncStatus } + shifts.map { it.syncStatus }
    val ok = all.count { it == SyncStatus.SYNCED }; val pend = all.count { it == SyncStatus.PENDING_SYNC }; val err = all.count { it == SyncStatus.SYNC_ERROR }
    KPage("Sinkronisasi", nav) {
        KCard(color = MaterialTheme.colorScheme.primaryContainer) { Text(if (syncing) "Menyinkronkan…" else if (err + pend == 0) "● Tersinkron" else "● Ada data menunggu", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KStat("Berhasil", "$ok", Modifier.weight(1f)); KStat("Pending", "$pend", Modifier.weight(1f)); KStat("Gagal", "$err", Modifier.weight(1f), MaterialTheme.colorScheme.error) }
        val bad = tx.filter { it.syncStatus != SyncStatus.SYNCED }
        if (bad.isNotEmpty()) KLabel("Perlu perhatian")
        bad.forEach { KItem("Transaksi ${shortId(it.transactionId)}", syncLabel(it.syncStatus), rp(it.total)) }
        ex.filter { it.syncStatus != SyncStatus.SYNCED }.forEach { KItem("Pengeluaran ${it.note.ifBlank { it.category }}", syncLabel(it.syncStatus), rp(it.amount)) }
        KPrimary(if (syncing) "MENYINKRONKAN…" else "SINKRONISASI ULANG", enabled = !syncing) { vm.syncPending() }
    }
}

@Composable
fun AboutScreen(nav: Nav) {
    val ctx = LocalContext.current
    val ver = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "-" }
    KPage("Tentang", nav) {
        Column(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(Modifier.size(64.dp), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Box(contentAlignment = Alignment.Center) { Image(painterResource(R.drawable.app_icon), "Saku Kasir", Modifier.size(48.dp)) }
            }
            Text("Saku Kasir", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 10.dp))
            Text("Versi $ver", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        KCard {
            KRow("Bisnis", "Pentol Rebus x Es Teh")
            KRow("Platform", "Android")
            KRow("Bantuan", "Hubungi Owner")
        }
    }
}

// ------------------------------------------------------------------ V37 feature port: Dropbox / Notifikasi / Edit Struk
@Composable
fun DropboxScreen(nav: Nav) {
    var showConnectInfo by rememberSaveable { mutableStateOf(false) }
    KPage("Dropbox", nav) {
        KCard(color = MaterialTheme.colorScheme.primaryContainer) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Surface(Modifier.size(64.dp), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primary) {
                    Box(contentAlignment = androidx.compose.ui.Alignment.Center) { Icon(painterResource(R.drawable.ic_cloud), "Dropbox", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp)) }
                }
                Text("Hubungkan Dropbox", fontSize = MaterialTheme.typography.titleLarge.fontSize, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 12.dp))
                Text("Simpan foto produk, QRIS, dan logo bisnis langsung ke Dropbox Anda.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, fontSize = MaterialTheme.typography.bodySmall.fontSize, modifier = Modifier.padding(top = 6.dp))
            }
        }
        KLabel("PENYIMPANAN")
        StorageInfoRow("Foto Produk", "Disimpan di Dropbox")
        StorageInfoRow("QRIS", "Disimpan di Dropbox")
        StorageInfoRow("Logo Bisnis", "Disimpan di Dropbox")
        KCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_cloud), "Dropbox", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Belum terhubung", fontWeight = FontWeight.Bold)
                    Text("Hubungkan akun Dropbox milik bisnis ini", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        KPrimary("HUBUNGKAN DROPBOX") { showConnectInfo = true }
        Text("Setiap bisnis dapat menggunakan akun Dropbox miliknya sendiri. Saku Kasir hanya menggunakan App Folder untuk penyimpanan aplikasi.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 9.dp))
    }
    if (showConnectInfo) {
        AlertDialog(onDismissRequest = { showConnectInfo = false }, title = { Text("Hubungkan Dropbox") }, text = { Text("Saku Kasir akan membuka halaman Dropbox untuk login dan memberikan izin melalui OAuth. Integrasi OAuth belum diaktifkan pada tahap ini, jadi belum ada akun yang dianggap terhubung.") }, confirmButton = { TextButton(onClick = { showConnectInfo = false }) { Text("MENGERTI") } })
    }
}

@Composable
private fun StorageInfoRow(title: String, subtitle: String) {
    KCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_cloud), title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall) }
        }
    }
}

@Composable
fun NotificationsScreen(vm: PosViewModel, nav: Nav) {
    val products by vm.products.collectAsState()
    val low = products.filter { it.stockEnabled && it.stock <= it.lowStock && it.active }
    KPage("Notifikasi", nav, subtitle = "Peringatan stok menipis") {
        if (low.isEmpty()) KEmpty("Tidak ada peringatan", "Semua stok yang dilacak masih di atas batas minimum.", R.drawable.ic_notification)
        else low.forEach { p ->
            KCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_notification), "Peringatan stok", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text(p.name, fontWeight = FontWeight.Bold); Text("Stok ${p.stock} · batas ${p.lowStock}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
    }
}

@Composable
fun ReceiptSettingsScreen(vm: PosViewModel, printer: BluetoothPrinter, nav: Nav) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("receipt_settings", android.content.Context.MODE_PRIVATE) }
    var showBiz by rememberSaveable { mutableStateOf(prefs.getBoolean("showBiz", true)) }
    var showOutlet by rememberSaveable { mutableStateOf(prefs.getBoolean("showOutlet", true)) }
    var showTxId by rememberSaveable { mutableStateOf(prefs.getBoolean("showTxId", true)) }
    var showCashier by rememberSaveable { mutableStateOf(prefs.getBoolean("showCashier", true)) }
    var showPayment by rememberSaveable { mutableStateOf(prefs.getBoolean("showPayment", true)) }
    var showChange by rememberSaveable { mutableStateOf(prefs.getBoolean("showChange", true)) }
    var footer by rememberSaveable { mutableStateOf(prefs.getString("footer", "Terima kasih!") ?: "Terima kasih!") }
    var preview by remember { mutableStateOf(false) }
    var print by remember { mutableStateOf(false) }
    val transactions by vm.transactions.collectAsState()
    val business by vm.business.collectAsState()
    val outlets by vm.outlets.collectAsState()
    fun save() {
        prefs.edit()
            .putBoolean("showBiz", showBiz).putBoolean("showOutlet", showOutlet).putBoolean("showTxId", showTxId)
            .putBoolean("showCashier", showCashier).putBoolean("showPayment", showPayment).putBoolean("showChange", showChange)
            .putString("footer", footer).apply()
    }
    KPage("Edit Struk", nav, subtitle = "Atur informasi yang tampil pada struk cetak.") {
        KCard {
            listOf(
                "Nama bisnis" to showBiz,
                "Outlet" to showOutlet,
                "Nomor transaksi" to showTxId,
                "Nama kasir" to showCashier,
                "Metode & pembayaran" to showPayment,
                "Kembalian" to showChange
            ).forEachIndexed { i, (label, value) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    KToggle(value) { checked -> when (label) {
                        "Nama bisnis" -> showBiz = checked
                        "Outlet" -> showOutlet = checked
                        "Nomor transaksi" -> showTxId = checked
                        "Nama kasir" -> showCashier = checked
                        "Metode & pembayaran" -> showPayment = checked
                        "Kembalian" -> showChange = checked
                    }}
                }
                if (i != 5) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        KCard {
            KLabel("PESAN BAWAH STRUK")
            KField("Pesan", footer, { footer = it.take(120) }, placeholder = "Terima kasih!")
            Text("Maksimal 120 karakter.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        KPrimary("PRATINJAU STRUK", enabled = transactions.isNotEmpty()) { if (transactions.isNotEmpty()) preview = true }
        KPrimary("SIMPAN") { save(); nav.back() }
    }
    val tx = transactions.firstOrNull()
    if (preview && tx != null) {
        ReceiptPreviewDialog(
            transaction = tx,
            businessName = business?.name,
            outletName = outlets.firstOrNull { it.id == tx.outletId }?.name,
            showBiz = showBiz,
            showOutlet = showOutlet,
            showTxId = showTxId,
            showCashier = showCashier,
            showPayment = showPayment,
            showChange = showChange,
            footer = footer,
            onPrint = { preview = false; print = true },
            onDismiss = { preview = false }
        )
    }
    if (print && tx != null) PrintDialog(printer, tx) { print = false }
}

@Composable
private fun ReceiptPreviewDialog(
    transaction: Transaction,
    businessName: String?,
    outletName: String?,
    showBiz: Boolean,
    showOutlet: Boolean,
    showTxId: Boolean,
    showCashier: Boolean,
    showPayment: Boolean,
    showChange: Boolean,
    footer: String,
    onPrint: () -> Unit,
    onDismiss: () -> Unit
) {
    val date = remember(transaction.createdAt) { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(Date(transaction.createdAt)) }
    val content = @Composable {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            if (showBiz) Text(businessName?.ifBlank { "Bisnis" } ?: "Bisnis", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.ExtraBold)
            if (showOutlet) Text(outletName?.ifBlank { "Outlet" } ?: "Outlet", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (showTxId) shortId(transaction.transactionId) else "Tanggal", style = MaterialTheme.typography.labelSmall)
                Text(date, style = MaterialTheme.typography.labelSmall)
            }
            if (showCashier) KRow("Kasir", transaction.cashierUid.ifBlank { "—" })
            HorizontalDivider()
            transaction.items.forEach { item ->
                Text(item.name, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${item.quantity} × ${rp(item.price)}", style = MaterialTheme.typography.labelSmall)
                    Text(rp(item.subtotal), style = MaterialTheme.typography.labelSmall)
                }
            }
            HorizontalDivider()
            KRow("Subtotal", rp(transaction.subtotal))
            if (transaction.discount > 0) KRow("Diskon", "- ${rp(transaction.discount)}")
            if (transaction.tax > 0) KRow("Pajak ${transaction.taxPercent}%", rp(transaction.tax))
            KRow("TOTAL", rp(transaction.total), bold = true, valueColor = MaterialTheme.colorScheme.primary)
            if (showPayment) {
                HorizontalDivider()
                KRow("Pembayaran", if (transaction.paymentMethod == PaymentMethod.QRIS) "QRIS" else "Cash")
                if (transaction.paymentMethod == PaymentMethod.CASH && showChange) {
                    KRow("Diterima", rp(transaction.cashReceived))
                    KRow("Kembali", rp((transaction.cashReceived - transaction.total).coerceAtLeast(0)))
                }
            }
            if (footer.isNotBlank()) {
                HorizontalDivider()
                Text(footer, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pratinjau Struk") },
        text = { Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) { content() } } },
        confirmButton = { TextButton(onClick = onPrint) { Text("CETAK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("TUTUP") } }
    )
}
