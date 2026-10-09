package com.sakukasir.pos.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakukasir.pos.domain.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
private fun Page(title: String, sub: String?, onHome: () -> Unit, fab: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Screen(bottomSpace = if (fab != null) 96.dp else 24.dp, overlay = {
        if (fab != null) SkFab(fab, Modifier.align(Alignment.BottomEnd).padding(20.dp))
    }) {
        PageHead(title, sub) { BackHome(onHome) }
        content()
    }
}

@Composable
private fun SearchBox(value: String, onChange: (String) -> Unit) {
    val c = Sk.c
    Row(
        Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RSm).background(c.card).border(1.dp, c.borderStrong, RSm).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SkIcon(SkIcons.Search, 18.dp, c.textFaint)
        BasicTextField(value, onChange, singleLine = true, cursorBrush = SolidColor(c.primary), textStyle = TextStyle(fontFamily = Inter, fontSize = 15.sp, color = c.text), modifier = Modifier.weight(1f),
            decorationBox = { inner -> Box { if (value.isEmpty()) Txt("Cari produk…", 15, color = c.textFaint); inner() } })
    }
}


/* ============================== Kelola ============================== */
@Composable
fun ManageScreen(vm: PosViewModel, owner: Boolean, user: User, go: (String) -> Unit, onLogout: () -> Unit) {
    val c = Sk.c
    val products by vm.products.collectAsState()
    val cats by vm.categories.collectAsState()
    val outlets by vm.outlets.collectAsState()
    val workers by vm.workers.collectAsState()
    val settings by vm.settings.collectAsState()
    val queue by vm.syncQueue.collectAsState()
    val audit by vm.audit.collectAsState()
    val dark = vm.state.collectAsState().value.darkTheme
    val low = products.count { it.trackStock && it.stock > 0 && it.stock <= it.lowStock }
    val out = products.count { it.trackStock && it.stock == 0 }

    val ownerGroups = listOf(
        "Katalog" to listOf(
            ManageItem("Produk", "Kelola daftar produk", "products", products.size.toString()),
            ManageItem("Kategori", "Kelompok produk", "categories", cats.size.toString()),
            ManageItem("Stok", "Pantau & sesuaikan stok", "inventory", if (low + out > 0) "${low + out} low" else "")
        ),
        "Bisnis" to listOf(
            ManageItem("Outlet", "Cabang & lokasi toko", "outlets", outlets.size.toString()),
            ManageItem("Kasir", "Akun & hak akses", "workers", workers.size.toString()),
            ManageItem("Shift", "Riwayat shift karyawan", "manage-shift", "")
        ),
        "Pengaturan" to listOf(
            ManageItem("QRIS", "Metode pembayaran QR", "qris", if (settings.qrisEnabled) "Aktif" else "Off"),
            ManageItem("Printer", "Printer Bluetooth struk", "printer", if (settings.printerConnected) "On" else "Off"),
            ManageItem("Struk", "Format & konten struk", "receipt", ""),
            ManageItem("Notifikasi", "Preferensi notifikasi", "notif-settings", ""),
            ManageItem("Sinkronisasi", "Status & queue", "sync-settings", queue.size.takeIf { it > 0 }?.toString() ?: ""),
            ManageItem("Keamanan", "Aturan void & refund", "security", ""),
            ManageItem("Audit Log", "Riwayat aktivitas", "audit", audit.size.takeIf { it > 0 }?.toString() ?: ""),
            ManageItem("Tema", if (dark) "Mode gelap" else "Mode terang", "theme", ""),
            ManageItem("Profil", "Info akun kamu", "profile", ""),
            ManageItem("Tentang", "Info aplikasi", "about", "")
        )
    )
    val cashierItems = buildList {
        if (user.can(Permission.EXPENSE)) add(ManageItem("Pengeluaran", "Catat pengeluaran harian", "expenses", ""))
        if (user.can(Permission.PRINTER)) add(ManageItem("Printer", "Printer Bluetooth struk", "printer", ""))
        if (user.can(Permission.SYNC)) add(ManageItem("Sinkronisasi", "Status & queue", "sync-settings", queue.size.takeIf { it > 0 }?.toString() ?: ""))
        if (user.can(Permission.THEME)) add(ManageItem("Tema", if (dark) "Mode gelap" else "Mode terang", "theme", ""))
        if (user.can(Permission.PROFILE)) add(ManageItem("Profil", "Info akun kamu", "profile", ""))
        add(ManageItem("Tentang", "Info aplikasi", "about", ""))
    }

    Screen(bottomSpace = 24.dp) {
        Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Txt("Kelola", 24, FontWeight.Bold, lineHeight = 1.2f)
                Txt(if (owner) "Menu owner" else "Menu kasir", 14, color = c.textMuted, modifier = Modifier.padding(top = 4.dp))
            }
            Btn("Keluar", onLogout, kind = 2)
        }
        if (owner) {
            ownerGroups.forEachIndexed { index, (title, items) ->
                SectionTitle(title, first = index == 0)
                items.forEach { item -> ManageRow(item, c) { go(item.route) } }
            }
        } else {
            if (cashierItems.isEmpty()) EmptyState("Tidak ada akses", "Hubungi owner untuk mengaktifkan fitur.")
            else cashierItems.forEach { ManageRow(it, c) { go(it.route) } }
        }
    }
}

private data class ManageItem(val title: String, val sub: String, val route: String, val meta: String)

@Composable
private fun ManageRow(item: ManageItem, c: SkColors, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RMd).background(c.card).border(1.dp, c.border, RMd)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Txt(item.title, 14, FontWeight.SemiBold)
            Txt(item.sub, 12, color = c.textMuted, modifier = Modifier.padding(top = 2.dp))
        }
        if (item.meta.isNotBlank()) {
            Txt(item.meta, 11, FontWeight.SemiBold, c.textMuted, modifier = Modifier.padding(horizontal = 8.dp))
        }
        Txt("›", 24, FontWeight.Normal, c.textFaint)
    }
}

/* ============================== Produk ============================== */
@Composable
fun ProductsPage(vm: PosViewModel, onHome: () -> Unit) {
    val c = Sk.c
    val products by vm.products.collectAsState()
    var q by remember { mutableStateOf("") }
    var edit by remember { mutableStateOf<Product?>(null) }
    var creating by remember { mutableStateOf(false) }
    var del by remember { mutableStateOf<Product?>(null) }
    Page("Produk", "${products.size} produk", onHome, fab = { creating = true }) {
        SearchBox(q) { q = it }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            products.filter { it.name.contains(q, true) }.forEach { p ->
                ListRow(onClick = { edit = p }) {
                    Column(Modifier.weight(1f)) {
                        Txt(p.name, 14, FontWeight.Medium, maxLines = 1)
                        Txt("${p.category} · /${p.unit}" + if (p.trackStock) " · stok ${p.stock}" else "", 12, color = c.textMuted, modifier = Modifier.padding(top = 2.dp), maxLines = 1)
                    }
                    Txt(rupiah(p.price), 14, FontWeight.Bold, spacing = -.14f)
                    IconBtn(SkIcons.Edit, { edit = p }, size = 16, modifier = Modifier.size(32.dp))
                    IconBtn(SkIcons.Trash, { del = p }, size = 16, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
    if (creating || edit != null) ProductSheet(vm, edit) { creating = false; edit = null }
    del?.let { d -> ConfirmModal("Hapus produk?", "${d.name} akan dihapus dari katalog.", "Hapus", true, true, { del = null }) { vm.deleteProduct(d.id); del = null } }
}

@Composable
private fun ProductSheet(vm: PosViewModel, p: Product?, onDismiss: () -> Unit) {
    val cats by vm.categories.collectAsState()
    val toast = LocalToast.current
    var name by remember { mutableStateOf(p?.name ?: "") }
    var cat by remember { mutableStateOf(p?.category ?: cats.firstOrNull()?.name ?: "Lainnya") }
    var unit by remember { mutableStateOf(p?.unit ?: "porsi") }
    var price by remember { mutableLongStateOf(p?.price ?: 0L) }
    var track by remember { mutableStateOf(p?.trackStock ?: true) }
    var stock by remember { mutableStateOf((p?.stock ?: 0).toString()) }
    var low by remember { mutableStateOf((p?.lowStock ?: 5).toString()) }
    var active by remember { mutableStateOf(p?.active ?: true) }
    SkSheet(onDismiss) {
        SheetTitle(if (p == null) "Produk baru" else "Edit produk")
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Field("Nama produk", name, { name = it })
            SelectField("Kategori", cat, cats.map { it.name }, { cat = it })
            Field("Unit", unit, { unit = it })
            RupiahField("Harga jual (Rp)", price, { price = it })
            SwitchRow("Stock tracking", "Pantau stok & notifikasi low stock", track, { track = it })
            if (track) {
                Field("Stok saat ini", stock, { stock = digits(it) }, keyboard = KeyboardType.Number)
                Field("Batas low stock", low, { low = digits(it) }, keyboard = KeyboardType.Number)
            }
            SwitchRow("Aktif", "Nonaktif tidak muncul di POS", active, { active = it })
        }
        FormActions("Batal", onDismiss, "Simpan", {
            if (name.isBlank() || price <= 0) toast("Nama dan harga wajib diisi", "error")
            else {
                val np = Product(p?.id ?: ((vm.products.value.maxOfOrNull { it.id } ?: 0) + 1), name.trim(), price, unit.trim(), cat, stock.toIntOrNull() ?: 0, low.toIntOrNull() ?: 5, track, active)
                if (p == null) vm.addProduct(np) else vm.updateProduct(np)
                onDismiss()
            }
        }, top = 16)
    }
}

/* ============================== Kategori ============================== */
@Composable
fun CategoriesPage(vm: PosViewModel, onHome: () -> Unit) {
    val c = Sk.c
    val cats by vm.categories.collectAsState()
    val products by vm.products.collectAsState()
    var edit by remember { mutableStateOf<Category?>(null) }
    var creating by remember { mutableStateOf(false) }
    var del by remember { mutableStateOf<Category?>(null) }
    val toast = LocalToast.current
    Page("Kategori", "${cats.size} kategori", onHome, fab = { creating = true }) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            cats.forEach { cat ->
                ListRow {
                    Column(Modifier.weight(1f)) {
                        Txt(cat.name, 14, FontWeight.Medium)
                        Txt("${products.count { it.category == cat.name }} produk", 12, color = c.textMuted, modifier = Modifier.padding(top = 2.dp))
                    }
                    IconBtn(SkIcons.Edit, { edit = cat }, size = 16, modifier = Modifier.size(32.dp))
                    IconBtn(SkIcons.Trash, { del = cat }, size = 16, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
    if (creating) PromptModal("Kategori baru", "Nama", "", onDismiss = { creating = false }) { v ->
        if (v.isNotBlank()) vm.upsertCategory(Category((cats.maxOfOrNull { it.id } ?: 0) + 1, v.trim())); creating = false
    }
    edit?.let { e -> PromptModal("Edit kategori", "Nama", e.name, onDismiss = { edit = null }) { v ->
        if (v.isNotBlank()) { vm.upsertCategory(e.copy(name = v.trim())); products.filter { it.category == e.name }.forEach { vm.updateProduct(it.copy(category = v.trim())) } }
        edit = null
    } }
    del?.let { d -> ConfirmModal("Hapus kategori?", if (products.any { it.category == d.name }) "Masih ada produk di kategori ${d.name}." else "${d.name} akan dihapus.", "Hapus", true, true, { del = null }) {
        if (products.any { it.category == d.name }) toast("Pindahkan produk dulu", "error") else vm.deleteCategory(d.id); del = null
    } }
}

/* ============================== Stok ============================== */
@Composable
fun InventoryPage(vm: PosViewModel, onHome: () -> Unit) {
    val c = Sk.c
    val products by vm.products.collectAsState()
    val tracked = products.filter { it.trackStock }
    var adj by remember { mutableStateOf<Product?>(null) }
    Page("Stok", "${tracked.size} produk dilacak", onHome) {
        KpiCompact(listOf(
            Triple("Low stock", tracked.count { it.stock in 1..it.lowStock }.toString(), c.warn),
            Triple("Out of stock", tracked.count { it.stock == 0 }.toString(), c.alert)
        ))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            tracked.forEach { p ->
                ListRow {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Txt(p.name, 14, FontWeight.Medium, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                            if (p.stock == 0) Tag("Habis", c.alertSoft, c.alert) else if (p.stock <= p.lowStock) Tag("Low", c.warnSoft, c.warn)
                        }
                        Txt("${p.category} · low threshold ${p.lowStock}", 12, color = c.textMuted, modifier = Modifier.padding(top = 2.dp))
                    }
                    Txt("${p.stock} ${p.unit}", 14, FontWeight.Bold)
                    IconBtn(SkIcons.Plus, { adj = p }, size = 16, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
    adj?.let { p ->
        var plus by remember { mutableStateOf(false) }
        var qty by remember { mutableStateOf("1") }
        val toast = LocalToast.current
        SkSheet({ adj = null }) {
            SheetTitle(p.name, "Stok saat ini: ${p.stock} ${p.unit}")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn("+ Tambah", { plus = true }, Modifier.fillMaxWidth(), kind = if (plus) 0 else 1)
                Btn("− Kurang", { plus = false }, Modifier.fillMaxWidth(), kind = if (!plus) 0 else 1)
                Field("Jumlah", qty, { qty = digits(it) }, keyboard = KeyboardType.Number)
            }
            FormActions("Batal", { adj = null }, "Simpan", {
                val n = qty.toIntOrNull() ?: 0
                if (n <= 0) toast("Jumlah tidak valid", "error")
                else if (!plus && n > p.stock) toast("Stok tidak cukup", "error")
                else { vm.updateProduct(p.copy(stock = if (plus) p.stock + n else p.stock - n)); adj = null }
            }, top = 16)
        }
    }
}

/* ============================== Outlet ============================== */
@Composable
fun OutletsPage(vm: PosViewModel, onHome: () -> Unit) {
    val c = Sk.c
    val outlets by vm.outlets.collectAsState()
    var edit by remember { mutableStateOf<Outlet?>(null) }
    var creating by remember { mutableStateOf(false) }
    var del by remember { mutableStateOf<Outlet?>(null) }
    Page("Outlet", "${outlets.size} outlet", onHome, fab = { creating = true }) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            outlets.forEach { o ->
                ListRow {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Txt(o.name, 14, FontWeight.Medium); TagActive(o.active) }
                        Txt(o.address, 12, color = c.textMuted, modifier = Modifier.padding(top = 2.dp))
                    }
                    IconBtn(SkIcons.Edit, { edit = o }, size = 16, modifier = Modifier.size(32.dp))
                    IconBtn(SkIcons.Trash, { del = o }, size = 16, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
    if (creating || edit != null) {
        val o = edit
        var name by remember { mutableStateOf(o?.name ?: "") }
        var addr by remember { mutableStateOf(o?.address ?: "") }
        var phone by remember { mutableStateOf(o?.phone ?: "") }
        var active by remember { mutableStateOf(o?.active ?: true) }
        var branchEnabled by remember { mutableStateOf(o?.branchEnabled ?: false) }
        var branchName by remember { mutableStateOf(o?.branchName ?: "") }
        val close = { creating = false; edit = null }
        SkSheet(close) {
            SheetTitle(if (o == null) "Outlet baru" else "Edit outlet")
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Field("Nama outlet", name, { name = it }); Field("Alamat", addr, { addr = it })
                Field("Nomor kontak", phone, { phone = it }, keyboard = KeyboardType.Phone)
                SwitchRow("Aktif", null, active, { active = it })
                SwitchRow("Gunakan cabang", "Aktifkan jika bisnis memiliki cabang", branchEnabled, { branchEnabled = it })
                if (branchEnabled) Field("Nama cabang", branchName, { branchName = it }, placeholder = "Contoh: Cabang 1")
            }
            FormActions("Batal", close, "Simpan", {
                if (name.isNotBlank() && (!branchEnabled || branchName.isNotBlank())) { vm.upsertOutlet(Outlet(o?.id ?: ((outlets.maxOfOrNull { it.id } ?: 0) + 1), name.trim(), addr.trim(), active, phone.trim(), branchEnabled, branchName.trim())); close() }
            }, top = 16)
        }
    }
    del?.let { d -> ConfirmModal("Hapus outlet?", "${d.name} akan dihapus.", "Hapus", true, true, { del = null }) { vm.deleteOutlet(d.id); del = null } }
}

/* ============================== Kasir ============================== */
private val PERM_DESC = mapOf(
    Permission.POS to ("POS" to "Buat transaksi penjualan"), Permission.DASHBOARD to ("Dashboard" to "Ringkasan penjualan kasir"),
    Permission.TRANSACTIONS to ("Transaksi" to "Lihat riwayat transaksi sendiri"), Permission.SHIFT to ("Shift" to "Mulai & tutup shift"),
    Permission.EXPENSE to ("Pengeluaran" to "Catat & kelola pengeluaran"), Permission.VOID to ("Void Transaksi" to "Batalkan transaksi sendiri"),
    Permission.REFUND to ("Refund Transaksi" to "Refund transaksi sendiri"), Permission.PRINTER to ("Printer" to "Setup & cetak struk Bluetooth"),
    Permission.SYNC to ("Sinkronisasi" to "Status & manual sync"), Permission.THEME to ("Ganti Tema" to "Terang / gelap"),
    Permission.PROFILE to ("Profil" to "Lihat info akun sendiri")
)

@Composable
fun WorkersPage(vm: PosViewModel, onHome: () -> Unit) {
    val c = Sk.c
    val workers by vm.workers.collectAsState()
    val outlets by vm.outlets.collectAsState()
    val appSettings by vm.settings.collectAsState()
    var edit by remember { mutableStateOf<Worker?>(null) }
    var creating by remember { mutableStateOf(false) }
    Page("Kasir", "${workers.size} pekerja", onHome, fab = { creating = true }) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            workers.forEach { w ->
                ListRow {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Txt(w.name, 14, FontWeight.Medium); TagActive(w.active) }
                        Txt("@${w.username} · ${w.outlet} · ${w.permissions.size} fitur", 12, color = c.textMuted, modifier = Modifier.padding(top = 2.dp))
                    }
                    IconBtn(SkIcons.Edit, { edit = w }, size = 16, modifier = Modifier.size(32.dp))
                    IconBtn(SkIcons.CheckCircle, { vm.upsertWorker(w.copy(active = !w.active)) }, size = 16, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
    if (creating || edit != null) {
        val w = edit
        var name by remember { mutableStateOf(w?.name ?: "") }
        var user by remember { mutableStateOf(w?.username ?: "") }
        var pass by remember { mutableStateOf("") }
        var outlet by remember { mutableStateOf(w?.outlet ?: outlets.firstOrNull()?.name ?: "") }
        var wa by remember { mutableStateOf(w?.whatsapp ?: "") }
        var active by remember { mutableStateOf(w?.active ?: true) }
        val perms = remember { mutableStateListOf<Permission>().apply { addAll(w?.permissions ?: Permission.entries) } }
        val close = { creating = false; edit = null }
        SkSheet(close) {
            SheetTitle(if (w == null) "Kasir baru" else "Edit kasir")
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Field("Nama tampilan", name, { name = it })
                Field("Username", user, { user = it }, enabled = w == null)
                Field(if (w == null) "Password" else "Password baru (kosongkan jika tidak diubah)", pass, { pass = it }, password = true, placeholder = if (w == null) "" else "(tidak diubah)")
                SelectField("Cabang", outlet, outlets.map { o -> if (o.branchEnabled) "${appSettings.receipt.bizName} - ${o.branchName}" else appSettings.receipt.bizName }, { outlet = it })
                Field("Nomor WhatsApp (opsional)", wa, { wa = it }, keyboard = KeyboardType.Phone)
                SwitchRow("Akun aktif", null, active, { active = it })
            }
            SectionTitle("Akses fitur")
            Txt("Pilih fitur yang boleh dipakai kasir ini.", 12, color = c.textMuted, modifier = Modifier.padding(bottom = 4.dp))
            Permission.entries.forEachIndexed { i, p ->
                val (label, desc) = PERM_DESC[p] ?: (p.label to "")
                SwitchRow(label, desc, p in perms, { if (it) perms.add(p) else perms.remove(p) }, divider = i < Permission.entries.size - 1, pad = 12)
            }
            FormActions("Batal", close, "Simpan", {
                if (name.isNotBlank() && user.isNotBlank()) {
                    vm.upsertWorker(Worker(w?.id ?: ((workers.maxOfOrNull { it.id } ?: 0) + 1), name.trim(), user.trim(), outlet, active, wa.trim(), perms.toSet())); close()
                }
            }, top = 16)
        }
    }
}

/* ============================== Printer ============================== */
@Composable
fun PrinterScreen(vm: PosViewModel, onHome: () -> Unit) {
    val c = Sk.c
    val context = LocalContext.current
    val printer = remember { PrinterHolder.get(context) }
    val status by printer.status.collectAsState()
    val settings by vm.settings.collectAsState()
    val toast = LocalToast.current
    var devices by remember { mutableStateOf(emptyList<android.bluetooth.BluetoothDevice>()) }
    val scan = { devices = printer.pairedDevices().toList().sortedBy { it.name ?: it.address }; if (devices.isEmpty()) toast("Tidak ada printer yang dipasangkan", "error") }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { scan() }
    val connected = status == "Connected"
    Page("Printer", "Bluetooth thermal", onHome) {
        Column(Modifier.fillMaxWidth().skCard().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Txt(if (connected) settings.printerName else "Belum terhubung", 14, FontWeight.Medium)
                    Txt(if (connected) "Siap mencetak struk" else "Tap tombol untuk cari printer", 12, color = c.textMuted)
                }
                SyncDot(if (connected) "synced" else "error")
            }
            if (connected) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Btn("Test print", { if (printer.testPrint()) toast("Test print terkirim", "") else toast("Gagal mencetak", "error") }, Modifier.weight(1f), kind = 1)
                Btn("Putuskan", { printer.disconnect(); vm.updateSettings(settings.copy(printerConnected = false)); toast("Printer diputus", "") }, Modifier.weight(1f), kind = 2)
            } else Btn("Cari printer", {
                if (Build.VERSION.SDK_INT >= 31 && androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED)
                    permission.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN))
                else scan()
            }, Modifier.fillMaxWidth())
        }
        if (!connected && devices.isNotEmpty()) {
            SectionTitle("Perangkat ditemukan")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                devices.forEach { d ->
                    ListRow(onClick = {
                        if (printer.connect(d)) { vm.updateSettings(settings.copy(printerConnected = true, printerName = d.name ?: d.address)); toast("${d.name ?: d.address} terhubung", "success") }
                        else toast("Gagal terhubung ke ${d.name ?: d.address}", "error")
                    }) {
                        Column(Modifier.weight(1f)) { Txt(d.name ?: "Perangkat tanpa nama", 14, FontWeight.Medium); Txt(d.address, 12, color = c.textMuted) }
                        Txt("Hubungkan", 13, FontWeight.SemiBold, c.primary)
                    }
                }
            }
        }
    }
}

/* ============================== Struk ============================== */
@Composable
fun ReceiptSettingsPage(vm: PosViewModel, onHome: () -> Unit) {
    val c = Sk.c
    val set by vm.settings.collectAsState()
    val outlets by vm.outlets.collectAsState()
    val appState by vm.state.collectAsState()
    val currentOutlet = outlets.firstOrNull { it.name == appState.selectedOutlet } ?: outlets.firstOrNull()
    var r by remember(set.receipt) { mutableStateOf(set.receipt) }
    val toast = LocalToast.current
    Page("Struk", "Format & konten", onHome) {
        Column(Modifier.fillMaxWidth().padding(bottom = 16.dp).skCard().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Field("Nama bisnis (header)", r.bizName, { r = r.copy(bizName = it) })
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Txt("Alamat outlet", 13, color = c.textMuted)
                Txt(currentOutlet?.address?.takeIf { it.isNotBlank() } ?: "Alamat belum diisi di menu Outlet", 15, color = c.text, modifier = Modifier.padding(top = 4.dp))
            }
            SwitchRow("Tampilkan nomor transaksi", null, r.showTrxNumber, { r = r.copy(showTrxNumber = it) })
            SwitchRow("Tampilkan kasir", null, r.showCashier, { r = r.copy(showCashier = it) })
            SwitchRow("Tampilkan metode bayar", null, r.showMethod, { r = r.copy(showMethod = it) })
            SwitchRow("Tampilkan kembalian", null, r.showChange, { r = r.copy(showChange = it) })
            Field("Footer (max 120 karakter)", r.footer, { r = r.copy(footer = it.take(120)) })
        }
        SectionTitle("Preview struk", true)
        val line = "─".repeat(24)
        val preview = buildString {
            appendLine(r.bizName); appendLine(line)
            if (r.showTrxNumber) appendLine("TRX-20261006-0042")
            appendLine("06 Okt 2026 · 14:32")
            currentOutlet?.address?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
            if (r.showCashier) appendLine("Kasir: Andi")
            appendLine(line); appendLine("Kopi Susu   2× 18.000"); appendLine("            36.000"); appendLine(line)
            appendLine("Subtotal        36.000")
            if (r.showMethod) appendLine("Cash            50.000")
            if (r.showChange) appendLine("Kembali         14.000")
            appendLine(line); append(r.footer)
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.widthIn(max = 280.dp).fillMaxWidth().clip(RSm).background(if (c.dark) Color(0xFFF5F5F5) else Color.White).border(1.dp, Color(0xFFE0E0E0), RSm).padding(16.dp)) {
                Txt(preview, 11, color = Color.Black, mono = true, lineHeight = 1.55f)
            }
        }
        Btn("Simpan", { vm.updateSettings(set.copy(receipt = r)); toast("Pengaturan struk disimpan", "success") }, Modifier.fillMaxWidth().padding(top = 16.dp))
    }
}

/* ============================== Notifikasi / Sinkronisasi ============================== */
@Composable
fun NotifSettingsPage(vm: PosViewModel, onHome: () -> Unit) {
    val set by vm.settings.collectAsState()
    Page("Notifikasi", "Preferensi", onHome) {
        Column(Modifier.fillMaxWidth().skCard().padding(horizontal = 16.dp, vertical = 4.dp)) {
            SwitchRow("Stok menipis", null, set.notifStockLow, { vm.updateSettings(set.copy(notifStockLow = it)) }, divider = true, pad = 12)
            SwitchRow("Stok habis", null, set.notifStockOut, { vm.updateSettings(set.copy(notifStockOut = it)) }, divider = true, pad = 12)
            SwitchRow("Sistem", null, set.notifSystem, { vm.updateSettings(set.copy(notifSystem = it)) }, pad = 12)
        }
    }
}

@Composable
fun SyncSettingsPage(vm: PosViewModel, onHome: () -> Unit) {
    val queue by vm.syncQueue.collectAsState()
    val s by vm.state.collectAsState()
    val online = rememberOnlineState() && !s.simulateOffline
    val toast = LocalToast.current
    Page("Sinkronisasi", "Status & queue", onHome) {
        Column(Modifier.fillMaxWidth().padding(bottom = 16.dp).skCard().padding(horizontal = 16.dp, vertical = 4.dp)) {
            KeyRow("Status", if (online) "Online" else "Offline")
            KeyRow("Pending", queue.size.toString(), divider = false)
        }
        Btn("Sync sekarang", { if (online) { vm.sync(); toast("Sinkronisasi berjalan…", "success") } else toast("Tidak ada koneksi", "error") }, Modifier.fillMaxWidth())
    }
}

/* ============================== Keamanan / Audit ============================== */
@Composable
fun SecurityScreen(vm: PosViewModel, onHome: () -> Unit) {
    val c = Sk.c
    val set by vm.settings.collectAsState()
    val sec = set.security
    var win by remember(sec) { mutableStateOf(sec.voidWindowMinutes.toString()) }
    var vl by remember(sec) { mutableLongStateOf(sec.voidLimitCashier) }
    var rl by remember(sec) { mutableLongStateOf(sec.refundLimitCashier) }
    var pin by remember(sec) { mutableStateOf(sec.ownerPin) }
    var alert by remember(sec) { mutableStateOf(sec.alertVoidPerDay.toString()) }
    val toast = LocalToast.current
    Page("Keamanan", "Aturan void & refund", onHome) {
        Box(Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RSm).background(c.surfaceAlt).padding(12.dp, 12.dp)) {
            Txt("Aturan ini membatasi kasir untuk void/refund supaya tidak bisa menghilangkan uang dari kas. Void/refund yang melebihi limit akan butuh PIN owner.", 12, color = c.textMuted, lineHeight = 1.55f)
        }
        Column(Modifier.fillMaxWidth().skCard().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Field("Window void untuk kasir (menit)", win, { win = digits(it) }, keyboard = KeyboardType.Number, hint = "Kasir hanya bisa void dalam X menit pertama setelah transaksi.")
            RupiahField("Limit void kasir (Rp)", vl, { vl = it }); RupiahField("Limit refund kasir (Rp)", rl, { rl = it })
            Field("PIN Owner (4 digit)", pin, { pin = digits(it).take(4) }, keyboard = KeyboardType.NumberPassword)
            Field("Alert kalau kasir void ≥ X kali/hari", alert, { alert = digits(it) }, keyboard = KeyboardType.Number)
            Btn("Simpan", {
                if (pin.length != 4) toast("PIN harus 4 digit", "error")
                else { vm.updateSettings(set.copy(security = sec.copy(voidWindowMinutes = win.toIntOrNull() ?: 5, voidLimitCashier = vl, refundLimitCashier = rl, ownerPin = pin, alertVoidPerDay = alert.toIntOrNull() ?: 5))); toast("Pengaturan keamanan disimpan", "success") }
            }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun AuditScreen(vm: PosViewModel, onHome: () -> Unit) {
    val c = Sk.c
    val a by vm.audit.collectAsState()
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    Page("Audit Log", "${a.size} catatan", onHome) {
        if (a.isEmpty()) EmptyState("Belum ada catatan", "Void/refund yang dilakukan akan tercatat di sini.")
        else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            a.forEach { e ->
                Column(Modifier.fillMaxWidth().skCard().padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        if (e.type == "VOID") Tag("Void", c.alertSoft, c.alert, bold = true) else Tag("Refund", c.warnSoft, c.warn, bold = true)
                        Txt(fmt.format(Date(e.timestamp)), 11, color = c.textFaint, mono = true)
                    }
                    Txt("${e.trxId} · ${rupiah(e.amount)}", 13)
                    Txt("${e.byName} · ${e.reason}", 12, color = c.textMuted)
                }
            }
        }
    }
}

/* ============================== Tema / Profil / Tentang ============================== */
@Composable
fun ThemeScreen(vm: PosViewModel, onHome: () -> Unit) {
    val c = Sk.c
    val dark = vm.state.collectAsState().value.darkTheme
    Page("Tema", null, onHome) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(false to ("Terang" to "Cocok untuk ruangan terang"), true to ("Gelap" to "Hemat mata di malam hari")).forEach { (isDark, t) ->
                val bg = if (isDark) Color(0xFF17171A) else Color.White
                val fg = if (isDark) Color(0xFFF5F5F0) else Color(0xFF1A1A1A)
                Row(
                    Modifier.fillMaxWidth().clip(RMd).background(bg).border(1.dp, if (dark == isDark) c.primary else c.border, RMd)
                        .clickable { if (dark != isDark) vm.toggleTheme() }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) { Txt(t.first, 14, FontWeight.Medium, fg); Txt(t.second, 12, color = if (isDark) Color(0xFFA0A09A) else Color(0xFF6B6B63), modifier = Modifier.padding(top = 2.dp)) }
                    if (dark == isDark) Txt("Aktif", 14, FontWeight.SemiBold, if (isDark) Color(0xFF22A06B) else Color(0xFF0F5132))
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(vm: PosViewModel, onHome: () -> Unit, onLogout: () -> Unit) {
    val u = vm.state.collectAsState().value.user!!
    Page("Profil", null, onHome) {
        Column(Modifier.fillMaxWidth().padding(bottom = 12.dp).skCard().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Field("Nama", u.displayName, {}, enabled = false); Field("Username", u.username, {}, enabled = false)
            Field("Role", if (u.role == Role.OWNER) "Owner" else "Cashier", {}, enabled = false)
            if (u.role == Role.CASHIER) Field("Outlet", u.outlet, {}, enabled = false)
        }
        Btn("Keluar", onLogout, Modifier.fillMaxWidth(), kind = 2)
    }
}

@Composable
fun AboutScreen(onHome: () -> Unit) {
    val c = Sk.c
    Page("Tentang", null, onHome) {
        Column(Modifier.fillMaxWidth().skCard().padding(20.dp)) {
            Box(Modifier.size(56.dp).clip(RMd).background(c.primary), contentAlignment = Alignment.Center) { Txt("SK", 20, FontWeight.Bold, Color.White) }
            Txt("SakuKasir", 17, FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
            Txt("Versi prototipe · 2026", 12, color = c.textMuted, modifier = Modifier.padding(top = 4.dp))
            Txt("Point of Sale untuk usaha kecil dan menengah.", 12, color = c.textMuted, modifier = Modifier.padding(top = 12.dp))
        }
    }
}
