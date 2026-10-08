package com.sakukasir.pos.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.compose.foundation.text.KeyboardOptions
import com.sakukasir.pos.domain.*
import com.sakukasir.pos.domain.QrisProof as QrisProofModel
import com.sakukasir.pos.util.QrisProof as QrisProofProcessor
import kotlinx.coroutines.launch
import java.io.File

/* ============================== POS ============================== */
@Composable
fun PosScreen(vm: PosViewModel) {
    val c = Sk.c
    val state by vm.state.collectAsState()
    val products by vm.products.collectAsState()
    val cats by vm.categories.collectAsState()
    val shift by vm.shift.collectAsState()
    val user = state.user!!
    val toast = LocalToast.current
    var startShift by remember { mutableStateOf(false) }
    var cartOpen by remember { mutableStateOf(false) }
    var checkout by remember { mutableStateOf(false) }
    var success by remember { mutableStateOf<Transaction?>(null) }
    var detail by remember { mutableStateOf<Transaction?>(null) }
    val filtered = products.filter { it.active && (state.selectedCategory == "Semua" || it.category == state.selectedCategory) && it.name.contains(state.search, true) }
    val count = state.cart.sumOf { it.qty }

    if (user.role == Role.CASHIER && shift == null) {
        Screen {
            PageHead("Kasir", "Mulai shift dulu untuk transaksi")
            Column(Modifier.fillMaxWidth().padding(top = 60.dp, start = 24.dp, end = 24.dp, bottom = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SkIcon(SkIcons.Clock, 64.dp, c.textFaint)
                Txt("Shift belum dimulai", 15, FontWeight.SemiBold, align = TextAlign.Center)
                Txt("Anda harus mulai shift dan mengisi kas awal sebelum bisa membuat transaksi.", 13, color = c.textMuted, align = TextAlign.Center, modifier = Modifier.widthIn(max = 280.dp), lineHeight = 1.5f)
                Btn("Mulai Shift Sekarang", { startShift = true }, Modifier.padding(top = 8.dp))
            }
        }
        if (startShift) PromptModal("Mulai shift", "Kas awal (Rp)", "200000", "Simpan", { startShift = false }) { v -> startShift = false; vm.startShift(digits(v).toLongOrNull() ?: 0L); toast("Shift dimulai", "success") }
        return
    }

    if (success != null) {
        SuccessScreen(vm, success!!, onNew = { success = null }, onDetail = { detail = success; success = null })
        return
    }
    Screen(bottomSpace = if (count > 0) 92.dp else 24.dp, overlay = {
        if (count > 0) Row(
            Modifier.align(Alignment.BottomCenter).padding(horizontal = 12.dp, vertical = 12.dp).fillMaxWidth().shadow(8.dp, RMd).clip(RMd)
                .background(c.primary).clickable { cartOpen = true }.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Txt("$count item", 12, color = Color.White.copy(alpha = .85f), lineHeight = 1.3f)
                Txt(rupiah(vm.totals().total), 17, FontWeight.Bold, Color.White, lineHeight = 1.3f)
            }
            Box(Modifier.clip(RSm).background(Color.White.copy(alpha = .18f)).padding(horizontal = 14.dp, vertical = 8.dp)) { Txt("Lihat Cart", 14, FontWeight.SemiBold, Color.White) }
        }
    }) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RSm).background(c.card).border(1.dp, c.borderStrong, RSm).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SkIcon(SkIcons.Search, 18.dp, c.textFaint)
            BasicTextField(
                state.search, vm::setSearch, singleLine = true, cursorBrush = SolidColor(c.primary),
                textStyle = TextStyle(fontFamily = Inter, fontSize = 15.sp, color = c.text), modifier = Modifier.weight(1f),
                decorationBox = { inner -> Box { if (state.search.isEmpty()) Txt("Cari produk…", 15, color = c.textFaint); inner() } }
            )
        }
        ChipRow {
            Chip("Semua", state.selectedCategory == "Semua") { vm.setCategory("Semua") }
            cats.filter { it.active }.forEach { cat -> Chip(cat.name, state.selectedCategory == cat.name) { vm.setCategory(cat.name) } }
        }
        if (filtered.isEmpty()) EmptyState("Produk tidak ditemukan", "Coba kata kunci lain, atau tambahkan produk baru via menu Kelola.", SkIcons.Search)
        else Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            filtered.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { p ->
                        ProductCard(p, Modifier.weight(1f).fillMaxHeight()) {
                            val inCart = state.cart.find { it.productId == p.id }?.qty ?: 0
                            if (p.trackStock && inCart >= p.stock) toast("Stok tidak cukup", "error")
                            else { vm.addToCart(p); toast("${p.name} ditambahkan", "") }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
    if (cartOpen) CartSheet(vm, onCheckout = { cartOpen = false; checkout = true }, onDismiss = { cartOpen = false })
    if (checkout) CheckoutSheet(vm, onDismiss = { checkout = false }, onDone = { checkout = false; success = it })
    detail?.let { TransactionDetail(vm, it) { detail = null } }
}

@Composable
private fun ProductCard(p: Product, modifier: Modifier, onClick: () -> Unit) {
    val c = Sk.c
    val out = p.trackStock && p.stock == 0
    Box(
        modifier.heightIn(min = 96.dp).alpha(if (out) .45f else 1f).clip(RMd).background(c.card).border(1.dp, c.border, RMd)
            .clickable(enabled = !out, onClick = onClick).padding(12.dp)
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Txt(p.name, 14, FontWeight.SemiBold, maxLines = 2, lineHeight = 1.3f)
            Txt("/ ${p.unit}", 11, color = c.textFaint)
            Spacer(Modifier.weight(1f))
            Txt(rupiah(p.price), 14, FontWeight.Bold, c.primary, spacing = -.14f)
        }
        if (p.trackStock) {
            if (p.stock == 0) Tag("Habis", c.alertSoft, c.alert, Modifier.align(Alignment.TopEnd).offset(4.dp, (-4).dp))
            else if (p.stock <= p.lowStock) Tag("Sisa ${p.stock}", c.warnSoft, c.warn, Modifier.align(Alignment.TopEnd).offset(4.dp, (-4).dp))
        }
    }
}

@Composable
fun QtyControl(qty: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    val c = Sk.c
    Row(Modifier.clip(RSm).background(c.surfaceAlt).padding(2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape6).clickable(onClick = onMinus), contentAlignment = Alignment.Center) { Txt("−", 18, FontWeight.SemiBold) }
        Txt(qty.toString(), 14, FontWeight.SemiBold, modifier = Modifier.widthIn(min = 32.dp), align = TextAlign.Center)
        Box(Modifier.size(34.dp).clip(RoundedCornerShape6).clickable(onClick = onPlus), contentAlignment = Alignment.Center) { Txt("+", 18, FontWeight.SemiBold) }
    }
}
private val RoundedCornerShape6 = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)

@Composable
private fun CartSheet(vm: PosViewModel, onCheckout: () -> Unit, onDismiss: () -> Unit) {
    val s by vm.state.collectAsState()
    val t = vm.totals()
    val products by vm.products.collectAsState()
    val toast = LocalToast.current
    var confirmClear by remember { mutableStateOf(false) }
    LaunchedEffect(s.cart.isEmpty()) { if (s.cart.isEmpty()) onDismiss() }
    SkSheet(onDismiss) {
        Txt("Cart · ${s.cart.sumOf { it.qty }} item", 17, FontWeight.SemiBold, lineHeight = 1.3f)
        Txt("Tap +/- untuk ubah jumlah", 12, color = Sk.c.textMuted, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
        s.cart.forEachIndexed { i, item ->
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Txt(item.name, 14, FontWeight.Medium)
                    Txt("${rupiah(item.price)} × ${item.qty}", 12, color = Sk.c.textMuted, modifier = Modifier.padding(top = 2.dp))
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Txt(rupiah(item.price * item.qty), 14, FontWeight.Bold)
                    QtyControl(item.qty, { vm.changeQty(item.productId, -1) }, {
                        val p = products.find { it.id == item.productId }
                        if (p != null && p.trackStock && item.qty >= p.stock) toast("Stok tidak cukup", "error") else vm.changeQty(item.productId, 1)
                    })
                }
            }
            if (i < s.cart.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Sk.c.border))
        }
        Spacer(Modifier.height(12.dp))
        SummaryRow("Subtotal", rupiah(t.subtotal))
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn("Kosongkan cart", { confirmClear = true }, Modifier.fillMaxWidth(), kind = 1)
            Btn("Bayar · ${rupiah(t.subtotal)}", onCheckout, Modifier.fillMaxWidth())
        }
    }
    if (confirmClear) ConfirmModal("Kosongkan cart?", "Semua item akan dihapus.", "Kosongkan", true, true, { confirmClear = false }) { confirmClear = false; vm.clearCart() }
}

/* ============================== Checkout ============================== */
@Composable
fun CheckoutScreen(vm: PosViewModel, onPos: () -> Unit) {
    val c = Sk.c
    val s by vm.state.collectAsState()
    val products by vm.products.collectAsState()
    val toast = LocalToast.current
    var pay by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var success by remember { mutableStateOf<Transaction?>(null) }
    var detail by remember { mutableStateOf<Transaction?>(null) }
    val count = s.cart.sumOf { it.qty }
    val subtotal = vm.totals().subtotal
    if (success != null) {
        SuccessScreen(vm, success!!, onNew = { success = null; onPos() }, onDetail = { detail = success; success = null })
        return
    }
    Screen(bottomSpace = if (count > 0) 92.dp else 24.dp, overlay = {
        if (count > 0) Row(
            Modifier.align(Alignment.BottomCenter).padding(horizontal = 12.dp, vertical = 12.dp).fillMaxWidth().shadow(8.dp, RMd).clip(RMd)
                .background(c.primary).clickable { pay = true }.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Txt("$count item", 12, color = Color.White.copy(alpha = .85f), lineHeight = 1.3f)
                Txt(rupiah(subtotal), 17, FontWeight.Bold, Color.White, lineHeight = 1.3f)
            }
            Box(Modifier.clip(RSm).background(Color.White.copy(alpha = .18f)).padding(horizontal = 14.dp, vertical = 8.dp)) { Txt("Bayar sekarang", 14, FontWeight.SemiBold, Color.White) }
        }
    }) {
        PageHead("Checkout", if (count > 0) "$count item siap dibayar" else "Belum ada item di cart") {
            if (count > 0) Btn("Kosongkan", { confirmClear = true }, kind = 1)
        }
        if (count == 0) EmptyState("Cart masih kosong", "Buka tab Kasir untuk memilih produk.", SkIcons.Bag, "Buka Kasir", onPos)
        else Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            s.cart.forEach { item ->
                Column(Modifier.fillMaxWidth().skCard().padding(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Txt(item.name, 15, FontWeight.SemiBold, lineHeight = 1.3f)
                            Txt("${rupiah(item.price)} × ${item.qty}", 12, color = c.textMuted, modifier = Modifier.padding(top = 3.dp))
                        }
                        QtyControl(item.qty, { vm.changeQty(item.productId, -1) }, {
                            val p = products.find { it.id == item.productId }
                            if (p != null && p.trackStock && item.qty >= p.stock) toast("Stok tidak cukup", "error") else vm.changeQty(item.productId, 1)
                        })
                    }
                    Box(Modifier.fillMaxWidth().drawBehind {
                        drawLine(c.border, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(size.width, 0f), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f)))
                    }.padding(top = 10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Txt("SUBTOTAL", 12, FontWeight.SemiBold, c.textMuted, spacing = .48f)
                            Txt(rupiah(item.price * item.qty), 16, FontWeight.Bold, c.primary, spacing = -.16f)
                        }
                    }
                }
            }
        }
    }
    if (pay) CheckoutSheet(vm, onDismiss = { pay = false }, onDone = { pay = false; success = it })
    if (confirmClear) ConfirmModal("Kosongkan cart?", "Semua item akan dihapus.", "Kosongkan", true, true, { confirmClear = false }) { confirmClear = false; vm.clearCart() }
    detail?.let { TransactionDetail(vm, it) { detail = null } }
}


private fun CheckoutSheet(vm: PosViewModel, onDismiss: () -> Unit, onDone: (Transaction) -> Unit) {
    val c = Sk.c
    val s by vm.state.collectAsState()
    val t = vm.totals()
    val toast = LocalToast.current
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var received by remember { mutableLongStateOf(0L) }
    var proof by remember { mutableStateOf<QrisProofModel?>(null) }
    var captureStartedAt by remember { mutableLongStateOf(0L) }
    var cameraFile by remember { mutableStateOf<File?>(null) }
    val context = LocalContext.current
    val trxId = remember { vm.nextTransactionId() }
    val retentionDays = vm.settings.collectAsState().value.qrisRetentionDays
    val scope = rememberCoroutineScope()
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok && cameraFile != null) scope.launch {
            val at = captureStartedAt.takeIf { it > 0L } ?: System.currentTimeMillis()
            proof = QrisProofProcessor.processCapture(context, cameraFile!!, s.user!!.displayName, s.selectedOutlet, trxId, at, retentionDays)
            toast("Bukti QRIS tersimpan", "success")
        }
    }
    fun launchCamera() {
        val file = File(File(context.cacheDir, "qris_capture").apply { mkdirs() }, "capture_${System.currentTimeMillis()}.jpg")
        cameraFile = file
        captureStartedAt = System.currentTimeMillis()
        takePicture.launch(FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file))
    }
    val requestCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) launchCamera() }
    val canPay = if (method == PaymentMethod.CASH) received >= t.total else proof != null
    val change = received - t.total

    SkSheet(onDismiss) {
        Txt("Pembayaran", 17, FontWeight.SemiBold, lineHeight = 1.3f)
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Txt("Total", 12, color = c.textMuted)
            Txt(rupiah(t.total), 32, FontWeight.Bold, spacing = -.64f, lineHeight = 1.2f)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.border)); Spacer(Modifier.height(16.dp))
        SectionTitle("Diskon & Pajak", true)
        Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RupiahField("Diskon (Rp)", s.discount, { vm.setDiscount(it) }, Modifier.weight(1f))
            Field("Pajak (%)", if (s.taxPct == 0) "0" else s.taxPct.toString(), { vm.setTax(digits(it).toIntOrNull() ?: 0) }, Modifier.weight(1f), keyboard = KeyboardType.Number)
        }
        SectionTitle("Metode Pembayaran", true)
        Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Btn("Cash", { method = PaymentMethod.CASH }, Modifier.weight(1f), kind = if (method == PaymentMethod.CASH) 0 else 1)
            Btn("QRIS", { method = PaymentMethod.QRIS }, Modifier.weight(1f), kind = if (method == PaymentMethod.QRIS) 0 else 1)
        }
        if (method == PaymentMethod.CASH) {
            RupiahField("Uang diterima", received, { received = it })
            Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn("Pas", { received = t.total }, kind = 1); Btn("50rb", { received = 50000 }, kind = 1); Btn("100rb", { received = 100000 }, kind = 1)
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Txt("Kembalian", 16); Txt(if (change >= 0) rupiah(change) else "—", 16, FontWeight.Bold, if (change >= 0) c.cash else c.textMuted)
            }
        } else {
            val dash = c.borderStrong
            Column(
                Modifier.fillMaxWidth().padding(bottom = 16.dp).drawBehind {
                    drawRoundRect(dash, cornerRadius = CornerRadius(12.dp.toPx()), style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
                }.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val p = proof
                if (p != null) {
                    val bmp = remember(p.localPath) { runCatching { BitmapFactory.decodeFile(p.localPath, BitmapFactory.Options().apply { inSampleSize = 4 })?.asImageBitmap() }.getOrNull() }
                    if (bmp != null) Image(bmp, "Bukti QRIS", Modifier.widthIn(max = 260.dp).fillMaxWidth().clip(RSm).border(1.dp, c.border, RSm), contentScale = ContentScale.FillWidth)
                    Row(Modifier.widthIn(max = 260.dp).fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Txt("${p.fileSize / 1024} KB", 11, color = c.textMuted)
                        Txt(java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date(p.capturedAt)), 11, color = c.textMuted)
                    }
                    Btn("Foto ulang", { proof = null }, Modifier.padding(top = 12.dp).fillMaxWidth(), kind = 1)
                } else {
                    SkIcon(SkIcons.Camera, 32.dp, c.textFaint)
                    Txt("Belum ada bukti QRIS", 14, FontWeight.Medium, modifier = Modifier.padding(top = 8.dp))
                    Txt("Bukti WAJIB dari kamera, tidak bisa dari galeri", 12, color = c.textMuted)
                    Btn("Ambil Foto Bukti", {
                        if (context.checkSelfPermission(android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) launchCamera()
                        else requestCamera.launch(android.Manifest.permission.CAMERA)
                    }, Modifier.padding(top = 12.dp), icon = SkIcons.Camera)
                }
            }
        }
        SummaryRow("Subtotal", rupiah(t.subtotal))
        SummaryRow("Diskon", if (t.discount > 0) "-" + rupiah(t.discount) else "", c.alert)
        SummaryRow("Pajak ${s.taxPct}%", if (t.tax > 0) rupiah(t.tax) else "")
        SummaryRow("Total", rupiah(t.total), total = true)
        Spacer(Modifier.height(16.dp))
        Btn(if (method == PaymentMethod.QRIS && proof == null) "Ambil bukti QRIS dulu" else "Bayar " + rupiah(t.total), {
            vm.checkout(method, received, proof, trxId) { onDone(it) }
        }, Modifier.fillMaxWidth(), enabled = canPay)
    }
}

@Composable
private fun SuccessScreen(vm: PosViewModel, tx: Transaction, onNew: () -> Unit, onDetail: () -> Unit) {
    val c = Sk.c
    val context = LocalContext.current
    val settings by vm.settings.collectAsState()
    val toast = LocalToast.current
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
        Box(Modifier.size(72.dp).background(c.cashSoft, CircleShape), contentAlignment = Alignment.Center) { SkIcon(SkIcons.Check, 36.dp, c.cash) }
        Spacer(Modifier.height(8.dp))
        Txt("Transaksi berhasil", 22, FontWeight.SemiBold)
        Txt(tx.id, 13, color = c.textMuted, mono = true)
        Txt(rupiah(tx.total), 28, FontWeight.Bold, spacing = -.56f, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
        Row(Modifier.padding(bottom = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            if (tx.method == PaymentMethod.CASH) Tag("Cash", c.cashSoft, c.cash, pill = true) else Tag("QRIS", c.qrisSoft, c.qris, pill = true)
            if (tx.method == PaymentMethod.CASH) Txt(" · Kembalian ${rupiah(tx.change)}", 14, color = c.textMuted)
        }
        Column(Modifier.widthIn(max = 320.dp).fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Btn(if (settings.printerConnected) "Cetak struk" else "Hubungkan printer", {
                if (settings.printerConnected) { if (PrinterHolder.get(context).printReceipt(receiptLines(tx, settings.receipt))) toast("Struk sedang dicetak…", "") else toast("Gagal mencetak", "error") }
                else toast("Printer belum terhubung · buka menu Printer", "error")
            }, Modifier.fillMaxWidth())
            Btn("Transaksi baru", onNew, Modifier.fillMaxWidth(), kind = 1)
            BtnLink("Lihat detail", onDetail)
        }
    }
}

/* ============================== Transactions ============================== */
@Composable
fun SyncBadge(s: SyncStatus) {
    val c = Sk.c
    when (s) {
        SyncStatus.SYNCED -> Tag("✓ synced", c.cashSoft, c.cash)
        SyncStatus.PENDING_SYNC -> Tag("⏳ pending", c.warnSoft, c.warn)
        SyncStatus.SYNC_ERROR -> Tag("⚠ error", c.alertSoft, c.alert)
    }
}

@Composable
fun TxItem(tx: Transaction, onClick: () -> Unit) {
    val c = Sk.c
    val inactive = tx.status == TransactionStatus.VOID || tx.status == TransactionStatus.REFUNDED
    ListRow(onClick = onClick) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Txt("#${tx.id.takeLast(4)} · ${tx.time}", 12, color = c.textMuted, mono = true)
                SyncBadge(tx.syncStatus)
                when (tx.status) {
                    TransactionStatus.VOID -> Tag("⚠ void", c.alertSoft, c.alert, bold = true)
                    TransactionStatus.REFUNDED -> Tag("↩ refund", c.warnSoft, c.warn, bold = true)
                    TransactionStatus.PARTIAL_REFUND -> Tag("↩ partial", c.warnSoft, c.warn, bold = true)
                    else -> {}
                }
            }
            Txt("${tx.items.sumOf { it.qty }} item · ${if (tx.method == PaymentMethod.CASH) "Cash" else "QRIS"}", 12, color = c.textMuted)
            if (tx.status == TransactionStatus.PARTIAL_REFUND) Txt("Refund sebagian · ${rupiah(tx.refundAmount)}", 11, color = c.warn)
        }
        Txt(rupiah(tx.total), 15, FontWeight.Bold, if (inactive) c.textFaint else c.text, strike = inactive, spacing = -.15f)
    }
}

@Composable
fun TransactionScreen(vm: PosViewModel) {
    val tx by vm.transactions.collectAsState()
    val user = vm.state.collectAsState().value.user!!
    var selected by remember { mutableStateOf<Transaction?>(null) }
    var filter by remember { mutableStateOf("Hari ini") }
    val owner = user.role == Role.OWNER
    val base = tx.filter { owner || it.cashierId == user.username }
    val visible = when (filter) { "Cash" -> base.filter { it.method == PaymentMethod.CASH }; "QRIS" -> base.filter { it.method == PaymentMethod.QRIS }; else -> base }
    Screen {
        PageHead("Transaksi", "${visible.size} transaksi")
        ChipRow {
            (listOf("Hari ini", "Cash", "QRIS") + if (owner) listOf("Semua outlet") else emptyList()).forEach { Chip(it, filter == it) { filter = it } }
        }
        if (visible.isEmpty()) EmptyState("Belum ada transaksi", "Transaksi hari ini akan muncul di sini.", SkIcons.Lines)
        else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { visible.forEach { t -> TxItem(t) { selected = t } } }
    }
    selected?.let { cur -> TransactionDetail(vm, tx.find { it.id == cur.id } ?: cur) { selected = null } }
}

@Composable
fun TransactionDetail(vm: PosViewModel, tx: Transaction, onDismiss: () -> Unit) {
    val c = Sk.c
    val user = vm.state.collectAsState().value.user!!
    val settings by vm.settings.collectAsState()
    val context = LocalContext.current
    val toast = LocalToast.current
    var showVoid by remember { mutableStateOf(false) }
    var showRefund by remember { mutableStateOf(false) }
    val owner = user.role == Role.OWNER
    val mine = tx.cashierId == user.username
    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
    val canVoid = tx.status == TransactionStatus.COMPLETED && tx.date == today && (owner || (mine && user.can(Permission.VOID)))
    val canRefund = (tx.status == TransactionStatus.COMPLETED || tx.status == TransactionStatus.PARTIAL_REFUND) && (owner || (mine && user.can(Permission.REFUND)))
    SkSheet(onDismiss) {
        Txt(tx.id, 17, FontWeight.SemiBold, lineHeight = 1.3f)
        Txt("${tx.date} · ${tx.time} · ${tx.outlet}", 12, color = c.textMuted, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
        when (tx.status) {
            TransactionStatus.VOID -> StatusBanner("VOID", "oleh ${tx.cashier}", false, SkIcons.Block)
            TransactionStatus.REFUNDED -> StatusBanner("REFUND PENUH", "${rupiah(tx.refundAmount)} · ${tx.refundMethod?.name ?: ""} · ${tx.refundReason ?: ""}", true, SkIcons.Undo)
            TransactionStatus.PARTIAL_REFUND -> StatusBanner("REFUND SEBAGIAN", "${rupiah(tx.refundAmount)} · ${tx.refundMethod?.name ?: ""} · ${tx.refundReason ?: ""}", true, SkIcons.Undo)
            else -> {}
        }
        KeyRow("Kasir", tx.cashier)
        KeyRow("Metode", if (tx.method == PaymentMethod.CASH) "Cash" else "QRIS")
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Txt("Status sync", 13, color = c.textMuted); SyncBadge(tx.syncStatus) }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))
        KeyRow("Umur", "${(System.currentTimeMillis() - tx.timestamp) / 60000} menit lalu", divider = false)
        if (tx.method == PaymentMethod.QRIS) {
            val p = tx.qrisProof
            Column(Modifier.fillMaxWidth().padding(top = 12.dp).clip(RSm).background(c.surfaceAlt).padding(12.dp)) {
                if (p == null) Txt("Tidak ada bukti QRIS", 13, color = c.textMuted, align = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(12.dp))
                else if (System.currentTimeMillis() > p.expiredAt) Txt("⚠ Bukti QRIS kadaluarsa\nRetensi ${settings.qrisRetentionDays} hari", 13, color = c.textMuted, align = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(12.dp))
                else {
                    Txt("Bukti QRIS · " + java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date(p.capturedAt)), 12, color = c.textMuted, modifier = Modifier.padding(bottom = 6.dp))
                    val bmp = remember(p.localPath) { runCatching { BitmapFactory.decodeFile(p.localPath, BitmapFactory.Options().apply { inSampleSize = 4 })?.asImageBitmap() }.getOrNull() }
                    if (bmp != null) Image(bmp, "Bukti QRIS", Modifier.fillMaxWidth().clip(RSm), contentScale = ContentScale.FillWidth)
                }
            }
        }
        SectionTitle("Item")
        tx.items.forEach {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) { Txt(it.name, 14, FontWeight.Medium); Txt("${rupiah(it.price)} × ${it.qty}", 12, color = c.textMuted, modifier = Modifier.padding(top = 2.dp)) }
                Txt(rupiah(it.price * it.qty), 14, FontWeight.Bold)
            }
        }
        SummaryRow("Subtotal", rupiah(tx.items.sumOf { it.price * it.qty }))
        if (tx.discount > 0) SummaryRow("Diskon", "-" + rupiah(tx.discount), c.alert)
        if (tx.tax > 0) SummaryRow("Pajak ${tx.taxPct}%", rupiah(tx.tax))
        SummaryRow("Total", rupiah(tx.total), total = true)
        if (tx.method == PaymentMethod.CASH) { SummaryRow("Diterima", rupiah(tx.received)); SummaryRow("Kembalian", rupiah(tx.change)) }
        Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn("Cetak ulang struk", {
                if (settings.printerConnected) { if (PrinterHolder.get(context).printReceipt(receiptLines(tx, settings.receipt))) toast("Mencetak ulang…", "") else toast("Gagal mencetak", "error") }
                else toast("Printer belum terhubung", "error")
            }, Modifier.fillMaxWidth())
            if (canVoid || canRefund) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canVoid) Btn("Void", { showVoid = true }, Modifier.weight(1f), kind = 2)
                if (canRefund) Btn("Refund", { showRefund = true }, Modifier.weight(1f), kind = 1)
            }
        }
    }
    if (showVoid) VoidSheet(vm, tx, { showVoid = false }) { showVoid = false; onDismiss() }
    if (showRefund) RefundSheet(vm, tx, { showRefund = false }) { showRefund = false; onDismiss() }
}

@Composable
private fun ReasonGrid(reasons: List<String>, selected: String, onSelect: (String) -> Unit) {
    val c = Sk.c
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        reasons.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { r ->
                    val on = r == selected
                    Box(
                        Modifier.weight(1f).clip(RSm).background(if (on) c.primarySoft else c.card).border(1.dp, if (on) c.primary else c.borderStrong, RSm)
                            .clickable { onSelect(r) }.padding(horizontal = 12.dp, vertical = 10.dp)
                    ) { Txt(r, 13, FontWeight.Medium, if (on) c.primary else c.text) }
                }
            }
        }
    }
}

@Composable
private fun VoidSheet(vm: PosViewModel, tx: Transaction, onDismiss: () -> Unit, onDone: () -> Unit) {
    val c = Sk.c
    val user = vm.state.collectAsState().value.user!!
    val sec = vm.settings.collectAsState().value.security
    val toast = LocalToast.current
    val owner = user.role == Role.OWNER
    val reasons = listOf("Salah input", "Customer batal", "Double entry", "Lainnya")
    var reason by remember { mutableStateOf(reasons[0]) }
    var note by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf(false) }
    val exceeds = !owner && tx.total > sec.voidLimitCashier
    fun run(p: String?) {
        val finalReason = if (reason == "Lainnya") note.trim() else reason
        vm.voidTransaction(tx, finalReason, p) { ok, msg -> toast(msg, if (ok) "success" else "error"); if (ok) onDone() }
    }
    SkSheet(onDismiss) {
        Txt("Void transaksi", 17, FontWeight.SemiBold, lineHeight = 1.3f)
        Txt("${tx.id} · ${rupiah(tx.total)} · ${if (tx.method == PaymentMethod.CASH) "Cash" else "QRIS"}", 12, color = c.textMuted, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
        StatusBanner(null, "Void tidak menghapus transaksi dari riwayat. Transaksi akan ditandai VOID dan tidak dihitung di penjualan.", false, SkIcons.AlertCircle)
        if (exceeds) StatusBanner(null, "Nominal transaksi (${rupiah(tx.total)}) melebihi limit kasir (${rupiah(sec.voidLimitCashier)}). Butuh PIN owner.", true, SkIcons.AlertCircle)
        SectionTitle("Alasan void", true)
        ReasonGrid(reasons, reason) { reason = it }
        if (reason == "Lainnya") Field("Keterangan", note, { note = it }, Modifier.padding(top = 12.dp), placeholder = "Tulis alasan…")
        FormActions("Batal", onDismiss, "Void transaksi", {
            if (reason == "Lainnya" && note.isBlank()) toast("Keterangan wajib diisi", "error") else if (exceeds) pin = true else run(null)
        }, rightKind = 2, top = 20)
    }
    if (pin) PinModal("PIN Owner", "Nominal ${rupiah(tx.total)} melebihi limit. Masukkan PIN owner.", { pin = false }) { pin = false; run(it) }
}

@Composable
private fun RefundSheet(vm: PosViewModel, tx: Transaction, onDismiss: () -> Unit, onDone: () -> Unit) {
    val c = Sk.c
    val user = vm.state.collectAsState().value.user!!
    val sec = vm.settings.collectAsState().value.security
    val toast = LocalToast.current
    val owner = user.role == Role.OWNER
    val reasons = listOf("Barang rusak", "Salah pesan", "Komplain", "Lainnya")
    val already = tx.refundedItems.groupBy { it.productId }.mapValues { (_, r) -> r.sumOf { it.qty } }
    val refundable = tx.items.filter { it.qty - (already[it.productId] ?: 0) > 0 }
    val checked = remember { mutableStateListOf<Int>().apply { addAll(refundable.map { it.productId }) } }
    var method by remember { mutableStateOf(if (tx.method == PaymentMethod.CASH) RefundMethod.CASH else RefundMethod.QRIS) }
    var reason by remember { mutableStateOf(reasons[0]) }
    var note by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf(false) }
    fun maxQty(i: CartItem) = i.qty - (already[i.productId] ?: 0)
    val total = refundable.filter { it.productId in checked }.sumOf { it.price * maxQty(it) }
    val full = refundable.all { it.productId in checked }
    val exceeds = !owner && total > sec.refundLimitCashier
    fun run(p: String?) {
        val items = refundable.filter { it.productId in checked }.map { RefundItem(it.productId, it.name, maxQty(it), it.price * maxQty(it)) }
        val finalReason = if (reason == "Lainnya") note.trim() else reason
        vm.refundTransaction(tx, items, method, finalReason, p) { ok, msg -> toast(msg, if (ok) "success" else "error"); if (ok) onDone() }
    }
    SkSheet(onDismiss) {
        Txt("Refund transaksi", 17, FontWeight.SemiBold, lineHeight = 1.3f)
        Txt("${tx.id} · Total asli ${rupiah(tx.total)}", 12, color = c.textMuted, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
        SectionTitle("Pilih item yang di-refund", true)
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            refundable.forEach { it0 ->
                val on = it0.productId in checked
                Row(
                    Modifier.fillMaxWidth().clip(RSm).background(if (on) c.primarySoft else Color.Transparent)
                        .clickable { if (on) checked.remove(it0.productId) else checked.add(it0.productId) }.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(Modifier.size(20.dp).clip(RXs).background(if (on) c.primary else Color.Transparent).border(2.dp, if (on) c.primary else c.borderStrong, RXs), contentAlignment = Alignment.Center) {
                        if (on) SkIcon(SkIcons.Check, 14.dp, Color.White)
                    }
                    Column(Modifier.weight(1f)) { Txt(it0.name, 14, FontWeight.Medium); Txt("${rupiah(it0.price)} × ${maxQty(it0)} (max ${maxQty(it0)})", 12, color = c.textMuted) }
                    Txt(rupiah(it0.price * maxQty(it0)), 14, FontWeight.SemiBold)
                }
            }
        }
        SummaryRow("Total refund", rupiah(total), c.warn, total = true)
        if (exceeds) StatusBanner(null, "Nominal refund (${rupiah(total)}) melebihi limit kasir (${rupiah(sec.refundLimitCashier)}). Butuh PIN owner.", true, SkIcons.AlertCircle, Modifier.padding(top = 12.dp))
        SectionTitle("Metode refund")
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Btn("Cash", { method = RefundMethod.CASH }, Modifier.weight(1f), kind = if (method == RefundMethod.CASH) 0 else 1)
            Btn("QRIS", { method = RefundMethod.QRIS }, Modifier.weight(1f), kind = if (method == RefundMethod.QRIS) 0 else 1)
        }
        SectionTitle("Alasan refund", true)
        ReasonGrid(reasons, reason) { reason = it }
        if (reason == "Lainnya") Field("Keterangan", note, { note = it }, Modifier.padding(top = 12.dp), placeholder = "Tulis alasan…")
        FormActions("Batal", onDismiss, (if (full) "Refund penuh" else "Refund sebagian") + " · " + rupiah(total), {
            if (reason == "Lainnya" && note.isBlank()) toast("Keterangan wajib diisi", "error") else if (exceeds) pin = true else run(null)
        }, rightEnabled = total > 0, top = 20)
    }
    if (pin) PinModal("PIN Owner", "Nominal refund ${rupiah(total)} melebihi limit. Masukkan PIN owner.", { pin = false }) { pin = false; run(it) }
}

/* ============================== Shift ============================== */
@Composable
fun ShiftScreen(vm: PosViewModel, onHome: (() -> Unit)? = null) {
    val c = Sk.c
    val shift by vm.shift.collectAsState()
    val history by vm.shiftHistory.collectAsState()
    val tx by vm.transactions.collectAsState()
    val user = vm.state.collectAsState().value.user!!
    var start by remember { mutableStateOf(false) }
    var close by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    val toast = LocalToast.current
    Screen {
        if (onHome != null) PageHead("Shift", "Riwayat & status") { onHome() } else PageHead("Shift", "Riwayat & status")
        val sh = shift
        if (sh != null) {
            val mine = tx.filter { it.cashierId == user.username && it.timestamp >= sh.startAt && it.status == TransactionStatus.COMPLETED }
            val cash = mine.filter { it.method == PaymentMethod.CASH }.sumOf { it.total }
            val qris = mine.filter { it.method == PaymentMethod.QRIS }.sumOf { it.total }
            SkCard(Modifier.fillMaxWidth().padding(bottom = 16.dp), padding = 20) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                    Box(Modifier.size(8.dp).background(c.cash, CircleShape)); Txt("SHIFT AKTIF", 12, FontWeight.SemiBold, c.cash, spacing = .48f)
                }
                Txt("Mulai ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date(sh.startAt))} · ${user.displayName}", 12, color = c.textMuted)
                Column(Modifier.padding(vertical = 12.dp)) {
                    KeyRow("Kas awal", rupiah(sh.openingCash)); KeyRow("Transaksi", mine.size.toString()); KeyRow("Cash", rupiah(cash)); KeyRow("QRIS", rupiah(qris))
                    KeyRow("Total penjualan", rupiah(cash + qris)); KeyRow("Expected cash", rupiah(sh.openingCash + cash), c.primary, divider = false, big = true)
                }
                Btn("Tutup shift", { close = true }, Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        } else EmptyState("Belum ada shift aktif", "Mulai shift untuk mencatat kas awal dan transaksi.", SkIcons.Clock, "Mulai shift") { start = true }
        SectionTitle("Riwayat shift")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            history.forEach { h ->
                ListRow {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Txt("${h.id} · ${h.start}–${h.end}", 12, color = c.textMuted, mono = true)
                        Txt("${h.tx} transaksi · Variance ${rupiah(h.variance)}", 12, color = c.textMuted)
                    }
                    Txt(rupiah(h.cash + h.qris), 15, FontWeight.Bold)
                }
            }
        }
    }
    if (start) PromptModal("Mulai shift", "Kas awal (Rp)", "200000", "Simpan", { start = false }) { v -> start = false; vm.startShift(digits(v).toLongOrNull() ?: 0L); toast("Shift dimulai", "success") }
    if (close) {
        val expected = vm.expectedCash()
        var physical by remember { mutableLongStateOf(expected) }
        SkSheet({ close = false }) {
            Txt("Tutup shift", 17, FontWeight.SemiBold, lineHeight = 1.3f)
            Txt("Hitung uang fisik, lalu masukkan jumlahnya.", 12, color = c.textMuted, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
            SummaryRow("Expected cash", rupiah(expected))
            RupiahField("Uang fisik (Rp)", physical, { physical = it }, Modifier.padding(vertical = 12.dp))
            Btn("Tutup shift", {
                val v = physical - expected
                result = if (v == 0L) "Pas, tidak ada selisih." else if (v > 0) "Lebih ${rupiah(v)}." else "Kurang ${rupiah(-v)}."
                vm.closeShift(physical); close = false
            }, Modifier.fillMaxWidth())
        }
    }
    result?.let { ConfirmModal("Shift ditutup", it, "OK", false, false, { result = null }) { result = null } }
}
