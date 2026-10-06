package com.pentolrebus.kasir.ui

import android.Manifest
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pentolrebus.kasir.R
import com.pentolrebus.kasir.domain.*
import com.pentolrebus.kasir.util.BluetoothPrinter
import com.pentolrebus.kasir.util.QrisProof
import com.pentolrebus.kasir.util.QrisStore

@Composable
fun KasirScreen(vm: PosViewModel, role: Role, nav: Nav, onCheckout: () -> Unit, onStartShift: () -> Unit) {
    val products by vm.products.collectAsState()
    val categories by vm.categories.collectAsState()
    val cart by vm.cart.collectAsState()
    val shift by vm.shift.collectAsState()
    var query by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf<String?>(null) }
    var listMode by remember { mutableStateOf(false) }

    if (shift == null) {
        Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            KEmpty("Belum ada Shift", "Mulai shift untuk menerima transaksi.\nProduk terkunci sampai shift dimulai.", R.drawable.ic_stopwatch, "MULAI SHIFT", onStartShift)
        }
        return
    }
    val shown = products.filter { it.active && (query.isBlank() || it.name.contains(query, true)) && (cat == null || it.categoryId == cat) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(21.dp), color = MaterialTheme.colorScheme.surface) {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_search), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(19.dp))
                androidx.compose.foundation.text.BasicTextField(value = query, onValueChange = { query = it }, singleLine = true, modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 14.dp), textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface), decorationBox = { inner -> if (query.isEmpty()) Text("Cari produk…", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium); inner() })
                if (query.isNotEmpty()) IconButton(onClick = { query = "" }, modifier = Modifier.size(32.dp)) { Icon(painterResource(R.drawable.ic_close), "Hapus", modifier = Modifier.size(18.dp)) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KChips(listOf("Semua") + categories.filter { it.active }.map { it.name }, if (cat == null) "Semua" else categories.firstOrNull { it.id == cat }?.name.orEmpty()) { selected -> cat = if (selected == "Semua") null else categories.firstOrNull { it.name == selected }?.id }
            }
            TextButton(onClick = { listMode = !listMode }) { Text(if (listMode) "Grid" else "Daftar") }
        }
        if (products.isEmpty()) {
            KEmpty(if (role == Role.OWNER) "Belum ada produk" else "Produk belum tersedia",
                if (role == Role.OWNER) "Tambahkan produk dulu agar bisa dijual." else "Minta Owner menambahkan produk.", R.drawable.ic_product,
                if (role == Role.OWNER) "TAMBAH PRODUK" else null, if (role == Role.OWNER) ({ nav.open("Produk:form", null) }) else null)
        } else if (listMode) {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown, key = { it.id }) { p ->
                    val q = cart.firstOrNull { it.product.id == p.id }?.quantity ?: 0
                    val out = p.stockEnabled && p.stock <= 0
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            ProductThumb(36)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.name, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(rp(p.price) + stockText(p), style = MaterialTheme.typography.labelMedium, color = if (p.stockEnabled && p.stock <= p.lowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (q > 0) { RoundBtn(R.drawable.ic_remove) { vm.remove(p) }; Text("$q", Modifier.padding(horizontal = 10.dp), fontWeight = FontWeight.Bold) }
                            RoundBtn(R.drawable.ic_add, enabled = !out) { vm.add(p) }
                        }
                    }
                }
            }
        } else {
            LazyVerticalGrid(GridCells.Fixed(2), Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                gridItems(shown, key = { it.id }) { p ->
                    val q = cart.firstOrNull { it.product.id == p.id }?.quantity ?: 0
                    val out = p.stockEnabled && p.stock <= 0
                    Surface(Modifier.fillMaxWidth().heightIn(min = 106.dp).clickable(enabled = !out) { vm.add(p) }, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
                        Column(Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) { ProductThumb(52); Spacer(Modifier.weight(1f)); if (q > 0) Badge { Text("$q") } }
                            Spacer(Modifier.height(8.dp))
                            Text(p.name, fontWeight = FontWeight.Bold, maxLines = 1)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(rp(p.price), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (p.stockEnabled) Text(if (out) "Habis" else "Stok ${p.stock}", style = MaterialTheme.typography.labelSmall, color = if (p.stock <= p.lowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                RoundBtn(R.drawable.ic_add, enabled = !out) { vm.add(p) }
                            }
                        }
                    }
                }
            }
        }
        val count = cart.sumOf { it.quantity }
        val total = cart.sumOf { it.product.price * it.quantity }
        Surface(Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable(enabled = count > 0) { onCheckout() }, shape = RoundedCornerShape(16.dp), color = if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                val fg = if (count > 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                Column(Modifier.weight(1f)) { Text("$count item", color = fg, style = MaterialTheme.typography.labelSmall); Text(rp(total), color = fg, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium) }
                Text("Checkout ›", color = fg, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun stockText(p: Product) = if (p.stockEnabled) " · Stok ${p.stock}" else ""

@Composable
private fun ProductThumb(size: Int) {
    Surface(Modifier.size(size.dp), shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Box(contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.ic_product), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size((size * 0.55f).dp)) }
    }
}

@Composable
private fun RoundBtn(icon: Int, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(Modifier.size(30.dp).clickable(enabled = enabled, onClick = onClick), shape = CircleShape, color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant) {
        Box(contentAlignment = Alignment.Center) { Icon(painterResource(icon), null, tint = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) }
    }
}

// ------------------------------------------------------------------ Checkout
@Composable
fun CheckoutScreen(vm: PosViewModel, discount: Long, onDiscount: (Long) -> Unit, taxPercent: Long, onTaxPercent: (Long) -> Unit, method: PaymentMethod, onMethod: (PaymentMethod) -> Unit, onNext: () -> Unit) {
    val cart by vm.cart.collectAsState()
    val subtotal = cart.sumOf { it.product.price * it.quantity }
    val disc = discount.coerceIn(0, subtotal)
    val taxPct = taxPercent.coerceIn(0, 100)
    val tax = kotlin.math.round((subtotal - disc) * taxPct / 100.0).toLong()
    val total = subtotal - disc + tax
    KPage("Checkout", null, actions = { TextButton(onClick = { vm.clearCart(); onDiscount(0); onTaxPercent(0) }, enabled = cart.isNotEmpty()) { Text("Kosongkan", color = MaterialTheme.colorScheme.error) } }) {
        if (cart.isEmpty()) { KEmpty("Keranjang kosong", "Tambahkan produk dari layar Kasir.", R.drawable.ic_cart); return@KPage }
        KCard {
            cart.forEach { c ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(c.product.name, fontWeight = FontWeight.Bold); Text("${c.quantity} × ${rp(c.product.price)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    RoundBtn(R.drawable.ic_remove) { vm.remove(c.product) }
                    Text("${c.quantity}", Modifier.padding(horizontal = 8.dp), fontWeight = FontWeight.Bold)
                    RoundBtn(R.drawable.ic_add, enabled = !(c.product.stockEnabled && c.quantity >= c.product.stock)) { vm.add(c.product) }
                    Text(rp(c.product.price * c.quantity), Modifier.width(92.dp), textAlign = TextAlign.End, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        KCard {
            KRow("Subtotal", rp(subtotal))
            KField("Diskon (Rp)", if (discount == 0L) "" else discount.toString(), { onDiscount(it.toLongOrNull() ?: 0) }, number = true, placeholder = "0")
            Spacer(Modifier.height(6.dp))
            KField("Pajak (%)", if (taxPct == 0L) "" else taxPct.toString(), { onTaxPercent((it.toLongOrNull() ?: 0).coerceIn(0, 100)) }, number = true, placeholder = "0")
            Spacer(Modifier.height(6.dp))
            KRow("Pajak ${taxPct}%", rp(tax))
            KRow("Total", rp(total), bold = true, valueColor = MaterialTheme.colorScheme.primary)
        }
        KLabel("Metode pembayaran")
        KSegmented(listOf("CASH", "QRIS"), if (method == PaymentMethod.CASH) "CASH" else "QRIS") { onMethod(if (it == "CASH") PaymentMethod.CASH else PaymentMethod.QRIS) }
        KPrimary("LANJUT BAYAR", enabled = total >= 0, onClick = onNext)
    }
}

@Composable
fun CashPayScreen(total: Long, onBack: () -> Unit, onDone: (Long) -> Unit) {
    var received by remember { mutableStateOf(total) }
    val change = received - total
    val quick = listOf(total, ((total / 50000) + 1) * 50000, ((total / 100000) + 1) * 100000).distinct()
    KPage("Pembayaran Cash", Nav({ _, _ -> }, onBack)) {
        KCard { Text("Total tagihan", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(rp(total), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold) }
        KField("Jumlah diterima (Rp)", if (received == 0L) "" else received.toString(), { received = it.toLongOrNull() ?: 0 }, number = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            quick.forEachIndexed { i, q -> Surface(Modifier.height(42.dp).clickable { received = q }, shape = RoundedCornerShape(20.dp), color = if (received == q) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface) { Box(Modifier.padding(horizontal = 13.dp), contentAlignment = Alignment.Center) { Text(if (i == 0) "Uang pas" else rp(q), color = if (received == q) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold) } } }
        }
        KCard(color = MaterialTheme.colorScheme.primaryContainer) {
            Text("Kembalian", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (change >= 0) rp(change) else "Kurang ${rp(-change)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = if (change >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        }
        KPrimary("SELESAIKAN TRANSAKSI", enabled = change >= 0 && total > 0) { onDone(received) }
    }
}

@Composable
fun QrisPayScreen(total: Long, outletId: String, onBack: () -> Unit, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val qr = remember(outletId) { QrisStore.load(ctx, outletId) }
    KPage("Pembayaran QRIS", Nav({ _, _ -> }, onBack)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (qr != null) {
                Surface(shape = RoundedCornerShape(16.dp), color = androidx.compose.ui.graphics.Color.White) { Image(qr.asImageBitmap(), "QRIS", Modifier.size(240.dp).padding(8.dp)) }
            } else {
                KCard { Text("QRIS belum diatur", fontWeight = FontWeight.Bold); Text("Owner mengunggah gambar QRIS di Pengaturan › QRIS. Pembayaran tetap bisa dikonfirmasi manual jika pelanggan menunjukkan bukti.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Spacer(Modifier.height(12.dp))
            Text(rp(total), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text("Scan lewat e-wallet / m-banking", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            AssistChip(onClick = {}, label = { Text("● Menunggu pembayaran") })
        }
        KPrimary("PEMBAYARAN DITERIMA", onClick = onDone)
    }
}

@Composable
fun SuccessScreen(vm: PosViewModel, printer: BluetoothPrinter, onNew: () -> Unit, onOpen: (String, String?) -> Unit) {
    val last by vm.lastTransaction.collectAsState()
    val t = last
    val ctx = LocalContext.current
    var print by remember { mutableStateOf(false) }
    var proofMsg by remember { mutableStateOf<String?>(null) }
    val cam = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp: Bitmap? ->
        if (bmp != null && t != null) {
            val path = QrisProof.save(ctx, bmp, t.transactionId)
            if (path != null) { vm.attachProof(t.transactionId, path); proofMsg = "Bukti tersimpan di $path" } else proofMsg = "Gagal menyimpan foto bukti"
        }
    }
    KPage("", null) {
        Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(Modifier.size(84.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) { Box(contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.ic_check), null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(44.dp)) } }
            Spacer(Modifier.height(12.dp))
            Text("Transaksi Berhasil", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text(rp(t?.total ?: 0), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        }
        if (t != null) {
            KCard {
                KRow("No. transaksi", shortId(t.transactionId))
                KRow("Metode", if (t.paymentMethod == PaymentMethod.QRIS) "QRIS" else "Cash")
                if (t.discount > 0) KRow("Diskon", "- " + rp(t.discount))
                if (t.tax > 0) KRow("Pajak ${t.taxPercent}%", rp(t.tax))
                if (t.paymentMethod == PaymentMethod.CASH) { KRow("Diterima", rp(t.cashReceived)); KRow("Kembalian", rp((t.cashReceived - t.total).coerceAtLeast(0))) }
                if (t.paymentMethod == PaymentMethod.QRIS) KRow("Bukti QRIS", if (t.qrisProofPath != null) "Terlampir" else "Belum ada")
                KRow("Sinkronisasi", syncLabel(t.syncStatus))
            }
            KPrimary("CETAK STRUK") { print = true }
            if (t.paymentMethod == PaymentMethod.QRIS) KSecondary(if (t.qrisProofPath != null) "AMBIL ULANG FOTO BUKTI" else "FOTO BUKTI QRIS (opsional)") { cam.launch(null) }
            proofMsg?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        KSecondary("TRANSAKSI BARU", onClick = onNew)
    }
    if (print && t != null) PrintDialog(printer, t) { print = false }
}

@Composable
fun PrintDialog(printer: BluetoothPrinter, t: Transaction, onDismiss: () -> Unit) {
    var devices by remember { mutableStateOf(printer.pairedDevices()) }
    var msg by remember { mutableStateOf<String?>(null) }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) devices = printer.pairedDevices() else msg = "Izin Bluetooth ditolak" }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Cetak struk") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!printer.hasConnectPermission()) { Text("Izin Bluetooth diperlukan untuk mencari printer."); TextButton(onClick = { perm.launch(Manifest.permission.BLUETOOTH_CONNECT) }) { Text("BERI IZIN") } }
                else if (devices.isEmpty()) Text("Belum ada printer yang dipasangkan. Pasangkan printer di pengaturan Bluetooth Android.")
                devices.forEach { d -> TextButton(onClick = { msg = printer.print(t, d.address).fold({ "Struk dikirim ke ${d.name}" }, { "Gagal: ${it.message}" }) }, modifier = Modifier.fillMaxWidth()) { Text("${d.name} · ${d.address}") } }
                msg?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("TUTUP") } })
}
