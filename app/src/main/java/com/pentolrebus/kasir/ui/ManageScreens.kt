package com.pentolrebus.kasir.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
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
        "Edit Struk" -> ReceiptSettingsScreen(nav)
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
fun SettingsScreen(vm: PosViewModel, session: Session, nav: Nav, onLogout: () -> Unit) {
    val business by vm.business.collectAsState()
    val outlets by vm.outlets.collectAsState()
    val active by vm.activeOutlet.collectAsState()
    val owner = session.role == Role.OWNER
    KPage("Pengaturan", null, subtitle = if (owner) "Owner · ${business?.name ?: "Bisnis"}" else "${session.username} · ${outlets.firstOrNull()?.name ?: "Outlet"}") {
        if (owner && outlets.size > 1) {
            KLabel("OUTLET AKTIF"); KChips(outlets.map { it.name }, outlets.firstOrNull { it.id == active }?.name ?: "") { n -> outlets.firstOrNull { it.name == n }?.let { vm.setActiveOutlet(it.id) } }
        }
        KLabel(if (owner) "OPERASIONAL" else "AKUN")
        if (owner) {
            KItem("Produk", "Kelola produk dan harga", icon = R.drawable.ic_product) { nav.open("Produk", null) }
            KItem("Kategori", "Kategori untuk filter Kasir", icon = R.drawable.ic_category) { nav.open("Kategori", null) }
            KItem("Stok", "Jumlah dan batas menipis", icon = R.drawable.ic_stock) { nav.open("Stok", null) }
            KItem("Outlet", "Kelola cabang bisnis", icon = R.drawable.ic_store) { nav.open("Outlet", null) }
            KItem("Kasir / Pekerja", "Akun kasir dibuat oleh Owner", icon = R.drawable.ic_people) { nav.open("Pekerja", null) }
            KItem("Owner", "Profil pemilik", icon = R.drawable.ic_owner) { nav.open("Owner", null) }
            KItem("Bisnis", "Informasi bisnis", icon = R.drawable.ic_business) { nav.open("Bisnis", null) }
            KItem("Pengeluaran", "Catat biaya operasional", icon = R.drawable.ic_report) { nav.open("Pengeluaran", null) }
            KLabel("PEMBAYARAN & PERANGKAT")
            KItem("QRIS", "Gambar QRIS outlet", icon = R.drawable.ic_qris) { nav.open("QRIS", null) }
            KItem("Printer", "Bluetooth", icon = R.drawable.ic_printer) { nav.open("Printer", null) }
            KLabel("PENYIMPANAN")
            KItem("Dropbox", "Penyimpanan foto produk, QRIS, dan logo bisnis", icon = R.drawable.ic_cloud) { nav.open("Dropbox", null) }
            KLabel("STRUK & APLIKASI")
            KItem("Edit Struk", "Atur informasi dan elemen struk", icon = R.drawable.ic_printer) { nav.open("Edit Struk", null) }
        } else {
            KItem("Profil", "Data akun kasir", icon = R.drawable.ic_person) { nav.open("Profil", null) }
            KItem("Outlet", outlets.firstOrNull()?.name ?: "Outlet", icon = R.drawable.ic_store) { }
            KItem("Pengeluaran", "Catat biaya operasional", icon = R.drawable.ic_report) { nav.open("Pengeluaran", null) }
            KItem("Printer Bluetooth", "Pasangkan dan tes cetak", icon = R.drawable.ic_printer) { nav.open("Printer", null) }
        }
        KLabel("APLIKASI")
        KItem("Tema", "Terang / Gelap / Sistem", icon = R.drawable.ic_theme) { nav.open("Tema", null) }
        KItem("Sinkronisasi", "Status data offline", icon = R.drawable.ic_sync) { nav.open("Sinkron", null) }
        KItem("Notifikasi", "Peringatan stok menipis", icon = R.drawable.ic_notification) { nav.open("Notifikasi", null) }
        KItem("Tentang aplikasi", "Versi dan bantuan", icon = R.drawable.ic_info) { nav.open("Tentang", null) }
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
    KPage("Produk", nav) {
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) { Row(Modifier.padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)); BasicTextField(q, { q = it }, Modifier.weight(1f).padding(horizontal = 9.dp, vertical = 13.dp), singleLine = true, textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface), decorationBox = { inner -> if (q.isEmpty()) Text("Cari produk…", color = MaterialTheme.colorScheme.onSurfaceVariant); inner() }) }}
        KChips(listOf("Semua") + cats.map { it.name }, cats.firstOrNull { it.id == cat }?.name ?: "Semua") { n -> cat = cats.firstOrNull { it.name == n }?.id }
        val list = products.filter { (q.isBlank() || it.name.contains(q, true)) && (cat == null || it.categoryId == cat) }
        if (products.isEmpty()) KEmpty("Belum ada produk", "Tambahkan produk pertama Anda.", R.drawable.ic_product)
        list.forEach { p ->
            val low = p.stockEnabled && p.stock <= p.lowStock
            KItem(p.name + if (!p.active) " (nonaktif)" else "", rp(p.price) + " / " + p.unit, if (p.stockEnabled) "Stok ${p.stock}" else "", if (low) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, R.drawable.ic_product) { nav.open("Produk:detail", p.id) }
        }
        KPrimary("+ TAMBAH PRODUK") { nav.open("Produk:form", null) }
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
    KPage("Kategori", nav) {
        if (cats.isEmpty()) KEmpty("Belum ada kategori", "Buat kategori sesuai produk Anda.", R.drawable.ic_category)
        cats.forEach { c -> KItem(c.name, "${products.count { it.categoryId == c.id }} produk", icon = R.drawable.ic_category) { nav.open("Kategori:form", c.id) } }
        KPrimary("+ TAMBAH KATEGORI") { nav.open("Kategori:form", null) }
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
        products.forEach { p ->
            val low = p.stockEnabled && p.stock <= p.lowStock
            KItem(p.name, if (!p.stockEnabled) "Stok OFF · tidak dilacak" else if (low) "Stok ON · menipis" else "Stok ON", if (p.stockEnabled) "${p.stock}" else "–", if (low) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface, R.drawable.ic_stock) { nav.open("Stok:detail", p.id) }
        }
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
        val delta = (amount.toLongOrNull() ?: 0) * if (mode == "Tambah") 1 else -1
        KCard(color = MaterialTheme.colorScheme.primaryContainer) { Text(p.name, fontWeight = FontWeight.ExtraBold); Text("${p.stock} ${p.unit}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = if (p.stock <= p.lowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface); if (p.stock <= p.lowStock) Text("Menipis · batas ${p.lowStock}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium) }
        KChips(listOf("Tambah", "Kurangi"), mode) { mode = it }
        KField("Jumlah penyesuaian", amount, { amount = it }, number = true)
        Text("Stok setelah penyesuaian: ${(p.stock + delta).coerceAtLeast(0)} ${p.unit}", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KSecondary("BATAL", modifier = Modifier.weight(1f), onClick = nav.back)
            KPrimary("SIMPAN", enabled = delta != 0L && p.stock + delta >= 0, modifier = Modifier.weight(1f)) { vm.adjustStock(p.id, delta); nav.back() }
        }
    }
}

// ------------------------------------------------------------------ Outlet
@Composable
fun OutletListScreen(vm: PosViewModel, session: Session, nav: Nav) {
    val outlets by vm.outlets.collectAsState(); val workers by vm.workers.collectAsState(); val active by vm.activeOutlet.collectAsState(); val shifts by vm.shifts.collectAsState()
    KPage("Outlet", nav, subtitle = "Owner › Bisnis › Outlet") {
        if (outlets.isEmpty()) KEmpty("Belum ada outlet", "Tambahkan outlet pertama.", R.drawable.ic_store)
        outlets.forEach { o -> KItem(o.name + if (o.id == active) " · aktif" else "", (o.address ?: "Alamat belum diisi") + "\n${workers.count { it.outletId == o.id }} kasir · ${shifts.count { it.outletId == o.id && it.closedAt == null }} shift aktif", icon = R.drawable.ic_store) { nav.open("Outlet:detail", o.id) } }
        KPrimary("+ TAMBAH OUTLET") { nav.open("Outlet:form", null) }
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
    KPage("Kasir / Pekerja", nav, subtitle = "Owner › Bisnis › Outlet › Pekerja") {
        if (workers.isEmpty()) KEmpty("Belum ada kasir", "Owner membuat akun kasir; pekerja tidak mendaftar sendiri.", R.drawable.ic_people)
        workers.forEach { w -> KItem(w.displayName.ifBlank { w.username }, (outlets.firstOrNull { it.id == w.outletId }?.name ?: "—") + " · Kasir", if (w.active) "Aktif" else "Nonaktif", if (w.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, R.drawable.ic_people) { nav.open("Pekerja:detail", w.id) } }
        KPrimary("+ TAMBAH PEKERJA") { sheet = true }
    }
    if (sheet) ModalBottomSheet(onDismissRequest = { sheet = false }) {
        Column(Modifier.padding(16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
    var pin by remember { mutableStateOf("") }
    var outletId by remember { mutableStateOf(old?.outletId ?: activeOutlet ?: "") }
    var wa by remember { mutableStateOf(old?.whatsapp ?: "") }
    var act by remember { mutableStateOf(old?.active ?: true) }
    val u = user.trim().lowercase().replace(" ", "")
    val dup = workers.any { it.username == u && it.id != old?.id }
    val err = when { u.isBlank() -> null; dup -> "Username sudah dipakai"; old == null && pin.isNotEmpty() && pin.length < 4 -> "PIN 4–6 angka"; else -> null }
    KField("Nama", name, { name = it }); KField("Username", user, { user = it })
    KSecretField(if (old == null) "PIN (4–6 angka)" else "PIN baru (kosongkan jika tidak diganti)", pin) { pin = it }
    KField("WhatsApp (opsional)", wa, { wa = it })
    KLabel("Outlet"); KChips(outlets.map { it.name }, outlets.firstOrNull { it.id == outletId }?.name ?: "") { n -> outletId = outlets.firstOrNull { it.name == n }?.id ?: outletId }
    if (old != null) KSwitchRow("Akun aktif", act) { act = it }
    err?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium) }
    val pinOk = if (old == null) pin.length in 4..6 else pin.isEmpty() || pin.length in 4..6
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        KSecondary("BATAL", modifier = Modifier.weight(1f), onClick = onDone)
        KPrimary("SIMPAN", enabled = name.isNotBlank() && u.isNotBlank() && !dup && pinOk && outletId.isNotBlank(), modifier = Modifier.weight(1f)) {
            vm.saveWorker((old ?: Worker()).copy(displayName = name.trim(), username = u, outletId = outletId, whatsapp = wa.trim().ifBlank { null }, active = act), pin); onDone()
        }
    }
}

@Composable
fun WorkerFormScreen(vm: PosViewModel, id: String?, nav: Nav) {
    val workers by vm.workers.collectAsState()
    val w = workers.firstOrNull { it.id == id }
    KPage("Ubah Pekerja", nav) { if (w == null) KEmpty("Pekerja tidak ditemukan", "", R.drawable.ic_people) else WorkerFormBody(vm, w) { nav.back() } }
}

@Composable
fun WorkerDetailScreen(vm: PosViewModel, id: String?, nav: Nav) {
    val workers by vm.workers.collectAsState(); val outlets by vm.outlets.collectAsState()
    val w = workers.firstOrNull { it.id == id }
    KPage("Detail Pekerja", nav) {
        if (w == null) { KEmpty("Pekerja tidak ditemukan", "", R.drawable.ic_people); return@KPage }
        KCard { Text(w.displayName.ifBlank { w.username }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold); Text("Kasir · " + (outlets.firstOrNull { it.id == w.outletId }?.name ?: "—")) }
        KCard { KRow("Username", w.username); KRow("PIN", "••••"); KRow("WhatsApp", w.whatsapp ?: "—"); KRow("Status", if (w.active) "Aktif" else "Nonaktif") }
        KPrimary("UBAH PEKERJA") { nav.open("Pekerja:form", w.id) }
        KSecondary(if (w.active) "NONAKTIFKAN" else "AKTIFKAN", danger = w.active) { vm.saveWorker(w.copy(active = !w.active), "") }
        Text("Catatan: PIN kasir disimpan terenkripsi di perangkat tempat akun dibuat/diubah.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        listOf("Sistem", "Terang", "Gelap").forEachIndexed { i, label ->
            KCard { Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { Text(label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold); RadioButton(selected = mode == i, onClick = { onMode(i) }) } }
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
        KEmpty("Saku Kasir", "Versi $ver", R.drawable.ic_info)
        KCard { KRow("Bisnis", "Pentol Rebus x Es Teh"); KRow("Platform", "Android"); KRow("Bantuan", "Hubungi Owner") }
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
                    Text("Hubungkan akun Dropbox milik bisnis ini", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
        KPrimary("HUBUNGKAN DROPBOX") { showConnectInfo = true }
        Text("Setiap bisnis dapat menggunakan akun Dropbox miliknya sendiri. Saku Kasir hanya menggunakan App Folder untuk penyimpanan aplikasi.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 9.dp))
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
            Column { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
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
                    Column(Modifier.weight(1f)) { Text(p.name, fontWeight = FontWeight.Bold); Text("Stok ${p.stock} · batas ${p.lowStock}", color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                }
            }
        }
    }
}

@Composable
fun ReceiptSettingsScreen(nav: Nav) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("receipt_settings", android.content.Context.MODE_PRIVATE) }
    var title by rememberSaveable { mutableStateOf(prefs.getString("title", "") ?: "") }
    var address by rememberSaveable { mutableStateOf(prefs.getString("address", "") ?: "") }
    var phone by rememberSaveable { mutableStateOf(prefs.getString("phone", "") ?: "") }
    var footer by rememberSaveable { mutableStateOf(prefs.getString("footer", "Terima kasih!") ?: "Terima kasih!") }
    var showLogo by rememberSaveable { mutableStateOf(prefs.getBoolean("logo", true)) }
    var showId by rememberSaveable { mutableStateOf(prefs.getBoolean("id", true)) }
    var showDate by rememberSaveable { mutableStateOf(prefs.getBoolean("date", true)) }
    var showCashier by rememberSaveable { mutableStateOf(prefs.getBoolean("cashier", true)) }
    var showPrice by rememberSaveable { mutableStateOf(prefs.getBoolean("price", true)) }
    var showSubtotal by rememberSaveable { mutableStateOf(prefs.getBoolean("subtotal", true)) }
    var showDiscount by rememberSaveable { mutableStateOf(prefs.getBoolean("discount", true)) }
    var showTax by rememberSaveable { mutableStateOf(prefs.getBoolean("tax", true)) }
    var showPayment by rememberSaveable { mutableStateOf(prefs.getBoolean("payment", true)) }
    var showChange by rememberSaveable { mutableStateOf(prefs.getBoolean("change", true)) }
    fun save() { prefs.edit().putString("title", title).putString("address", address).putString("phone", phone).putString("footer", footer).putBoolean("logo", showLogo).putBoolean("id", showId).putBoolean("date", showDate).putBoolean("cashier", showCashier).putBoolean("price", showPrice).putBoolean("subtotal", showSubtotal).putBoolean("discount", showDiscount).putBoolean("tax", showTax).putBoolean("payment", showPayment).putBoolean("change", showChange).apply() }
    KPage("Edit Struk", nav, subtitle = "Atur tampilan dan informasi yang dicetak pada struk transaksi.") {
        KLabel("IDENTITAS STRUK")
        KField("Judul struk", title, { title = it }, placeholder = "Kosong = nama bisnis")
        KField("Alamat", address, { address = it }, placeholder = "Kosong = alamat outlet")
        KField("Telepon", phone, { phone = it }, placeholder = "Nomor telepon")
        KField("Pesan bawah struk", footer, { footer = it }, placeholder = "Terima kasih!")
        KLabel("ELEMEN YANG DITAMPILKAN")
        listOf("Logo" to showLogo, "Nomor transaksi" to showId, "Tanggal & waktu" to showDate, "Kasir" to showCashier, "Harga satuan" to showPrice, "Subtotal" to showSubtotal, "Diskon" to showDiscount, "Pajak" to showTax, "Metode & uang dibayar" to showPayment, "Kembalian" to showChange).forEach { (label, value) ->
            KCard { Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { Text(label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold); Switch(checked = value, onCheckedChange = { v -> when(label) { "Logo" -> showLogo=v; "Nomor transaksi" -> showId=v; "Tanggal & waktu" -> showDate=v; "Kasir" -> showCashier=v; "Harga satuan" -> showPrice=v; "Subtotal" -> showSubtotal=v; "Diskon" -> showDiscount=v; "Pajak" -> showTax=v; "Metode & uang dibayar" -> showPayment=v; "Kembalian" -> showChange=v } }) } }
        }
        KLabel("PRATINJAU")
        KCard { Column(Modifier.fillMaxWidth().padding(4.dp)) { Text(title.ifBlank { "Saku Kasir" }, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold); Text("Pentol Rebus × Es Teh Fresh Brew", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontSize = 11.sp); HorizontalDivider(Modifier.padding(vertical = 7.dp)); Text("Pentol Rebus     2 × 12.000", fontSize = 11.sp); HorizontalDivider(Modifier.padding(vertical = 7.dp)); Text("TOTAL            Rp 24.000", fontWeight = FontWeight.Bold, fontSize = 11.sp); Text(footer, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        KPrimary("SIMPAN") { save(); nav.back() }
        KSecondary("KEMBALIKAN DEFAULT") { showLogo=true; showId=true; showDate=true; showCashier=true; showPrice=true; showSubtotal=true; showDiscount=true; showTax=true; showPayment=true; showChange=true; title=""; address=""; phone=""; footer="Terima kasih!"; save() }
    }
}
