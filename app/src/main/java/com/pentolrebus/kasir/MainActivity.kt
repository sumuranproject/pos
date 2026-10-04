package com.pentolrebus.kasir

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pentolrebus.kasir.data.OfflineStore
import com.pentolrebus.kasir.data.RepositoryProvider
import com.pentolrebus.kasir.domain.*
import com.pentolrebus.kasir.ui.AuthState
import com.pentolrebus.kasir.ui.PosViewModel
import com.pentolrebus.kasir.ui.PosViewModelFactory
import com.pentolrebus.kasir.util.BluetoothPrinter
import com.pentolrebus.kasir.util.Diagnostics
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Blue = Color(0xFF1F6FEB)
private val BlueLight = Color(0xFFE6F0FF)
private val Page = Color(0xFFE8EEF7)
private val Bg = Color(0xFFF4F7FC)
private val TextMain = Color(0xFF1C1B22)
private val Muted = Color(0xFF66708A)
private val Border = Color(0xFFDFE6F2)
private val Green = Color(0xFF1E9E5A)
private val Red = Color(0xFFD64545)

private fun money(value: Long): String = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    .apply { maximumFractionDigits = 0 }
    .format(value).replace("Rp", "Rp ")

private fun dateTime(ms: Long): String = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale("id", "ID")).format(Date(ms))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val diagnostics = Diagnostics(this)
        val repository = RepositoryProvider.create(this, diagnostics)
        val offlineStore = OfflineStore(this)
        setContent {
            val vm: PosViewModel = viewModel(factory = PosViewModelFactory(repository, offlineStore))
            SakuKasirApp(vm, diagnostics)
        }
    }
}

@Composable
private fun SakuKasirApp(vm: PosViewModel, diagnostics: Diagnostics) {
    var dark by remember { mutableStateOf(false) }
    val colors = if (dark) darkColorScheme(
        primary = Color(0xFF6EA8FF),
        onPrimary = Color.White,
        background = Color(0xFF111722),
        surface = Color(0xFF17202D),
        surfaceVariant = Color(0xFF202B3A),
        onSurface = Color(0xFFF4F7FC),
        onSurfaceVariant = Color(0xFFB6C1D3),
        outline = Color(0xFF354154),
        error = Color(0xFFF06A6A)
    ) else lightColorScheme(
        primary = Blue,
        onPrimary = Color.White,
        background = Bg,
        surface = Color.White,
        surfaceVariant = Color(0xFFF0F4FA),
        onSurface = TextMain,
        onSurfaceVariant = Muted,
        outline = Border,
        error = Red
    )
    MaterialTheme(colorScheme = colors, typography = Typography()) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val auth by vm.auth.collectAsState()
            when (val a = auth) {
                AuthState.LoggedOut -> AuthFlow(vm)
                AuthState.Loading -> LoadingView()
                is AuthState.Error -> AuthFlow(vm, a.message)
                is AuthState.LoggedIn -> MainShellNew(vm, a.session, dark, { dark = !dark }, diagnostics)
            }
        }
    }
}

@Composable private fun LoadingView() {
    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(14.dp))
        Text("Memproses…", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AuthFlow(vm: PosViewModel, error: String? = null) {
    var role by remember { mutableStateOf<Role?>(null) }
    var register by remember { mutableStateOf(false) }
    var emailMode by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var business by remember { mutableStateOf("") }
    var outlet by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var validation by remember { mutableStateOf<String?>(null) }

    if (role == null) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            LogoBox(88.dp)
            Spacer(Modifier.height(14.dp))
            Text("Saku Kasir", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Text("Point of Sale", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))
            RoleChoice("Owner", "Kelola toko & sistem", Icons.Default.Store) { role = Role.OWNER }
            Spacer(Modifier.height(12.dp))
            RoleChoice("Kasir", "Transaksi & POS", Icons.Default.PointOfSale) { role = Role.CASHIER }
        }
        return
    }

    val owner = role == Role.OWNER
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp)) {
        BackRow(if (register) "Daftar sebagai Owner" else "Login ${if (owner) "Owner" else "Kasir"}") {
            if (register) register = false else role = null
            validation = null
        }
        Spacer(Modifier.height(12.dp))
        LogoBox(68.dp)
        Spacer(Modifier.height(10.dp))
        Text(if (register) "Buat akun Owner" else "Selamat datang kembali", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text(if (register) "Lengkapi data bisnis Anda." else "Masuk untuk melanjutkan ke Saku Kasir.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        if (error != null) ErrorCard(error)
        if (validation != null) ErrorCard(validation!!)

        if (owner && !register) {
            Segmented(listOf("Username & PIN", "Email & Password"), if (emailMode) 1 else 0) { emailMode = it == 1 }
            Spacer(Modifier.height(16.dp))
            if (emailMode) {
                Field("Email", email, { email = it }, "nama@email.com", Icons.Default.Email, KeyboardType.Email)
                Spacer(Modifier.height(10.dp))
                PasswordInput("Password", password) { password = it }
                Spacer(Modifier.height(16.dp))
                PrimaryButton("Masuk") { vm.loginEmail(email, password, Role.OWNER) }
            } else {
                Field("Username", username, { username = it }, "Masukkan username", Icons.Default.Person)
                Spacer(Modifier.height(10.dp))
                PinInput("PIN", pin) { pin = it }
                Spacer(Modifier.height(16.dp))
                PrimaryButton("Masuk") { vm.loginLocal(username, pin, Role.OWNER) }
                TextButton(onClick = { register = true }, modifier = Modifier.fillMaxWidth()) { Text("Daftar sebagai Owner") }
            }
        } else if (owner) {
            Field("Email", email, { email = it }, "nama@email.com", Icons.Default.Email, KeyboardType.Email)
            Spacer(Modifier.height(10.dp)); PasswordInput("Password", password) { password = it }
            Spacer(Modifier.height(10.dp)); PasswordInput("Konfirmasi Password", confirmPassword) { confirmPassword = it }
            Spacer(Modifier.height(10.dp)); Field("Username", username, { username = it }, "username", Icons.Default.Person)
            Spacer(Modifier.height(10.dp)); PinInput("PIN", pin) { pin = it }
            Spacer(Modifier.height(10.dp)); PinInput("Konfirmasi PIN", confirmPin) { confirmPin = it }
            Spacer(Modifier.height(10.dp)); Field("Nama bisnis", business, { business = it }, "Opsional", Icons.Default.Business)
            Spacer(Modifier.height(10.dp)); Field("Nama outlet", outlet, { outlet = it }, "Opsional", Icons.Default.Store)
            Spacer(Modifier.height(10.dp)); Field("WhatsApp", whatsapp, { whatsapp = it }, "Opsional", Icons.Default.Phone, KeyboardType.Phone)
            Spacer(Modifier.height(16.dp))
            PrimaryButton("Daftar Owner") {
                validation = when {
                    !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Email tidak valid"
                    password.length < 8 -> "Password minimal 8 karakter"
                    password != confirmPassword -> "Konfirmasi password tidak cocok"
                    username.length < 3 -> "Username minimal 3 karakter"
                    !pin.matches(Regex("\\d{4,6}")) -> "PIN harus 4–6 angka"
                    pin != confirmPin -> "Konfirmasi PIN tidak cocok"
                    else -> null
                }
                if (validation == null) vm.register(email, password, username, pin, business.ifBlank { null }, outlet.ifBlank { null }, whatsapp.ifBlank { null })
            }
            TextButton(onClick = { register = false }, modifier = Modifier.fillMaxWidth()) { Text("Sudah punya akun? Login Owner") }
        } else {
            Field("Username", username, { username = it }, "Masukkan username", Icons.Default.Person)
            Spacer(Modifier.height(10.dp)); PinInput("PIN", pin) { pin = it }
            Spacer(Modifier.height(16.dp)); PrimaryButton("Masuk") { vm.loginLocal(username, pin, Role.CASHIER) }
            Spacer(Modifier.height(8.dp))
            Text("Akun kasir dibuat oleh Owner.\nLupa PIN? Tanyakan Owner Anda.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable private fun LogoBox(size: androidx.compose.ui.unit.Dp) {
    Surface(Modifier.size(size), RoundedCornerShape(size / 4), color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        Box(Alignment.Center) { Image(painterResource(R.drawable.app_icon), "Saku Kasir", Modifier.size(size * .72f)) }
    }
}

@Composable private fun RoleChoice(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(28.dp), tint = Blue); Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }; Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun BackRow(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { IconButton(onBack) { Icon(Icons.Default.ArrowBack, "Kembali") }; Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
}

@Composable private fun Field(label: String, value: String, onValue: (String) -> Unit, placeholder: String, icon: ImageVector? = null, keyboard: KeyboardType = KeyboardType.Text) {
    Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(5.dp))
    OutlinedTextField(value, onValue, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text(placeholder) }, leadingIcon = icon?.let { { Icon(it, null) } }, keyboardOptions = KeyboardOptions(keyboardType = keyboard), shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Blue, unfocusedBorderColor = MaterialTheme.colorScheme.outline))
}

@Composable private fun PasswordInput(label: String, value: String, onValue: (String) -> Unit) { var show by remember { mutableStateOf(false) }; OutlinedTextField(value, onValue, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true, visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton({ show = !show }) { Icon(if (show) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } }, shape = RoundedCornerShape(14.dp)) }
@Composable private fun PinInput(label: String, value: String, onValue: (String) -> Unit) { var show by remember { mutableStateOf(false) }; OutlinedTextField(value, { onValue(it.filter(Char::isDigit).take(6)) }, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true, visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), trailingIcon = { IconButton({ show = !show }) { Icon(if (show) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } }, shape = RoundedCornerShape(14.dp)) }
@Composable private fun PrimaryButton(label: String, onClick: () -> Unit) { Button(onClick, Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Text(label, fontWeight = FontWeight.Bold) } }
@Composable private fun ErrorCard(message: String) { Card(Modifier.fillMaxWidth().padding(bottom = 10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(12.dp)) { Text(message, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp) } }

@Composable private fun Segmented(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) { Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(4.dp)) { labels.forEachIndexed { i, l -> Box(Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(if (selected == i) MaterialTheme.colorScheme.surface else Color.Transparent).clickable { onSelect(i) }.padding(vertical = 10.dp), Alignment.Center) { Text(l, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) } } } }

@Composable
private fun MainShellNew(vm: PosViewModel, session: Session, dark: Boolean, onTheme: () -> Unit, diagnostics: Diagnostics) {
    var tab by remember { mutableStateOf("kasir") }
    var page by remember { mutableStateOf<String?>(null) }
    var showStart by remember { mutableStateOf(false) }
    var showClose by remember { mutableStateOf(false) }
    var showPrinter by remember { mutableStateOf(false) }
    var selectedTx by remember { mutableStateOf<Transaction?>(null) }
    val products by vm.products.collectAsState(); val categories by vm.categories.collectAsState(); val outlets by vm.outlets.collectAsState(); val workers by vm.workers.collectAsState(); val expenses by vm.expenses.collectAsState(); val business by vm.business.collectAsState(); val cart by vm.cart.collectAsState(); val shift by vm.shift.collectAsState(); val transactions by vm.transactions.collectAsState(); val last by vm.lastTransaction.collectAsState(); val syncing by vm.syncing.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current; val printer = remember(context) { BluetoothPrinter(context) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) showPrinter = true }
    val openPrinter = { if (Build.VERSION.SDK_INT >= 31 && !printer.hasConnectPermission()) permission.launch(Manifest.permission.BLUETOOTH_CONNECT) else showPrinter = true }
    val logout = { if (shift == null) vm.logout() else showClose = true }

    Column(Modifier.fillMaxSize()) {
        if (page == null && tab != "checkout" && tab != "success") AppHeader(session, tab, dark, onTheme)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (page != null) {
                ManagementNew(page!!, vm, session, products, categories, outlets, workers, expenses, business, transactions, shift, syncing, { page = null }, { page = it }, { vm.syncPending() }, openPrinter)
            } else when (tab) {
                "kasir" -> PosHome(vm, session, products, categories, cart, shift, { showStart = true }, { showClose = true }, { tab = "checkout" })
                "checkout" -> CheckoutNew(vm, cart) { tab = "success" }
                "success" -> SuccessNew(last, printer) { tab = "kasir" }
                "laporan" -> ReportsNew(session, transactions, shift) { selectedTx = it }
                "pengaturan" -> SettingsNew(session, business, syncing, { page = it }, { vm.syncPending() }, openPrinter, onTheme, logout)
            }
        }
        if (page == null && tab != "checkout" && tab != "success") BottomBar(tab) { tab = it }
    }
    if (showStart) MoneyDialogNew("Mulai Shift", "Kas awal") { vm.startShift(it); showStart = false }
    if (showClose) CloseDialogNew(shift) { vm.closeShift(it); showClose = false }
    if (showPrinter) PrinterDialogNew(printer) { showPrinter = false }
    selectedTx?.let { TxDialogNew(it, printer) { selectedTx = null } }
}

@Composable private fun AppHeader(session: Session, tab: String, dark: Boolean, onTheme: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        LogoBox(42.dp); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(if (tab == "kasir") "Saku Kasir" else tabTitle(tab), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp); Text("${session.username} · ${if (session.role == Role.OWNER) "Owner" else "Kasir"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        IconButton(onTheme) { Icon(if (dark) Icons.Default.LightMode else Icons.Default.DarkMode, "Tema") }
    }
}
private fun tabTitle(t: String) = when(t) { "laporan" -> "Laporan"; "pengaturan" -> "Pengaturan"; else -> "Saku Kasir" }

@Composable private fun ShiftStrip(shift: Shift?, onStart: () -> Unit, onClose: () -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp), RoundedCornerShape(15.dp), color = if (shift == null) MaterialTheme.colorScheme.surface else BlueLight) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(9.dp).clip(CircleShape).background(if (shift == null) Muted else Green)); Spacer(Modifier.width(8.dp)); Column(Modifier.weight(1f)) { Text(if (shift == null) "Belum ada Shift" else "Shift Aktif", fontWeight = FontWeight.Bold, fontSize = 13.sp); Text(if (shift == null) "Mulai shift untuk menerima transaksi" else "Shift berjalan · ${dateTime(shift.startAt)}", fontSize = 10.sp, color = Muted) }; if (shift == null) TextButton(onStart) { Text("MULAI") } else TextButton(onClose) { Text("TUTUP", color = Red) } }
    }
}

@Composable private fun PosHome(vm: PosViewModel, session: Session, products: List<Product>, categories: List<Category>, cart: List<CartItem>, shift: Shift?, onStart: () -> Unit, onClose: () -> Unit, onCheckout: () -> Unit) {
    ShiftStrip(shift, onStart, onClose)
    if (shift == null) { EmptyState(Icons.Default.Schedule, "Belum ada Shift", "Mulai shift untuk menerima transaksi", "MULAI SHIFT", onStart); return }
    var query by remember { mutableStateOf("") }; var cat by remember { mutableStateOf("Semua") }
    val names = listOf("Semua") + categories.map { it.name }.ifEmpty { listOf("Pentol", "Es Teh", "Tambahan") }
    val filtered = products.filter { it.active && it.name.contains(query, true) && (cat == "Semua" || categories.find { c -> c.name == cat }?.id == it.categoryId || it.name.contains(cat, true)) }
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 14.dp), placeholder = { Text("Cari produk…") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, shape = RoundedCornerShape(13.dp))
        LazyRow(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) { items(names) { FilterChip(selected = cat == it, onClick = { cat = it }, label = { Text(it, fontSize = 11.sp) }) } }
        if (filtered.isEmpty()) EmptyState(Icons.Default.Inventory2, "Belum ada produk", "Tambahkan produk dari Pengaturan.", null, {}) else LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(1f).padding(horizontal = 14.dp), contentPadding = PaddingValues(bottom = 110.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { gridItems(filtered) { p -> ProductTile(p) { vm.add(p) } } }
        if (cart.isNotEmpty()) CartBar(cart) { onCheckout() }
    }
}

@Composable private fun ProductTile(p: Product, onAdd: () -> Unit) { Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(12.dp)) { Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(BlueLight), Alignment.Center) { Text(p.name.take(1).uppercase(), color = Blue, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) }; Spacer(Modifier.height(9.dp)); Text(p.name, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 14.sp); Text(money(p.price), color = Blue, fontWeight = FontWeight.Bold, fontSize = 13.sp); Spacer(Modifier.height(7.dp)); Button(onAdd, Modifier.fillMaxWidth().height(36.dp), shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(0.dp)) { Icon(Icons.Default.Add, null, Modifier.size(17.dp)); Spacer(Modifier.width(4.dp)); Text("Tambah", fontSize = 12.sp) } } } }
@Composable private fun CartBar(cart: List<CartItem>, onClick: () -> Unit) { val total = cart.sumOf { it.product.price * it.quantity }; Surface(Modifier.fillMaxWidth().padding(12.dp), RoundedCornerShape(17.dp), color = Blue, shadowElevation = 5.dp) { Row(Modifier.clickable(onClick = onClick).padding(horizontal = 15.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(.18f)), Alignment.Center) { Text(cart.sumOf { it.quantity }.toString(), color = Color.White, fontWeight = FontWeight.Bold) }; Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text("Keranjang", color = Color.White.copy(.8f), fontSize = 11.sp); Text(money(total), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) }; Text("CHECKOUT", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp); Spacer(Modifier.width(5.dp)); Icon(Icons.Default.ChevronRight, null, tint = Color.White) } } }

@Composable private fun CheckoutNew(vm: PosViewModel, cart: List<CartItem>, onDone: () -> Unit) {
    var method by remember { mutableStateOf(PaymentMethod.CASH) }; var cash by remember { mutableStateOf("") }; var discount by remember { mutableStateOf(0L) }
    val subtotal = cart.sumOf { it.product.price * it.quantity }; val total = (subtotal - discount).coerceAtLeast(0); val received = cash.toLongOrNull() ?: 0; val change = (received - total).coerceAtLeast(0)
    Column(Modifier.fillMaxSize()) {
        BackRow("Checkout") { vm.clearCart(); onDone() }
        LazyColumn(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            items(cart) { item -> Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(item.product.name, fontWeight = FontWeight.SemiBold); Text("${item.quantity} × ${money(item.product.price)}", fontSize = 11.sp, color = Muted) }; Text(money(item.product.price * item.quantity), fontWeight = FontWeight.Bold) } }
            item { HorizontalDivider(Modifier.padding(vertical = 8.dp)); SummaryRow("Subtotal", money(subtotal)); SummaryRow("Diskon", money(discount)); SummaryRow("Total", money(total), true); Spacer(Modifier.height(12.dp)); Segmented(listOf("Cash", "QRIS"), if (method == PaymentMethod.CASH) 0 else 1) { method = if (it == 0) PaymentMethod.CASH else PaymentMethod.QRIS }; Spacer(Modifier.height(12.dp)); if (method == PaymentMethod.CASH) { Field("Uang diterima", cash, { cash = it.filter(Char::isDigit) }, "0", Icons.Default.Payments, KeyboardType.Number); Spacer(Modifier.height(8.dp)); Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { listOf(total, 50000L, 100000L).distinct().filter { it > 0 }.take(3).forEach { OutlinedButton({ cash = it.toString() }, Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp)) { Text(money(it).replace("Rp ", ""), fontSize = 11.sp) } } }; Spacer(Modifier.height(8.dp)); SummaryRow("Kembalian", money(change)) } else { QrPayment(total) } }
        }
        PrimaryButton(if (method == PaymentMethod.CASH) "BAYAR ${money(total)}" else "KONFIRMASI QRIS") { if (method == PaymentMethod.CASH) { if (received >= total) { vm.checkout(method, received, null, discount); onDone() } } else { vm.checkout(method, 0, null, discount); onDone() } }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable private fun QrPayment(total: Long) { Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.QrCode2, null, Modifier.size(130.dp), tint = TextMain); Text(money(total), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold); Text("Scan lewat e-wallet / m-banking", color = Muted, fontSize = 12.sp); Spacer(Modifier.height(7.dp)); Text("● Menunggu pembayaran", color = Green, fontWeight = FontWeight.Bold, fontSize = 12.sp) } } }
@Composable private fun SummaryRow(label: String, value: String, strong: Boolean = false) { Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) { Text(label, Modifier.weight(1f), fontWeight = if (strong) FontWeight.Bold else FontWeight.Normal, color = if (strong) MaterialTheme.colorScheme.onSurface else Muted); Text(value, fontWeight = if (strong) FontWeight.ExtraBold else FontWeight.SemiBold, fontSize = if (strong) 17.sp else 13.sp) } }

@Composable private fun SuccessNew(t: Transaction?, printer: BluetoothPrinter, onDone: () -> Unit) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) { Spacer(Modifier.height(35.dp)); Box(Modifier.size(74.dp).clip(CircleShape).background(Green), Alignment.Center) { Icon(Icons.Default.Check, null, tint = Color.White, Modifier.size(45.dp)) }; Spacer(Modifier.height(15.dp)); Text("Transaksi Berhasil", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold); Text(t?.let { money(it.total) } ?: "", color = Blue, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold); Spacer(Modifier.height(18.dp)); t?.let { Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp)) { Column(Modifier.padding(15.dp)) { SummaryRow("Metode", it.paymentMethod.name); SummaryRow("Waktu", dateTime(it.createdAt)); SummaryRow("Item", it.items.sumOf { x -> x.quantity }.toString()) } } }; Spacer(Modifier.height(14.dp)); PrimaryButton("SELESAI") { onDone() }; Spacer(Modifier.height(8.dp)); OutlinedButton({ t?.let { val d = printer.pairedDevices().firstOrNull(); if (d != null) printer.print(it, d.address) } }, Modifier.fillMaxWidth()) { Icon(Icons.Default.Print, null); Spacer(Modifier.width(7.dp)); Text("CETAK STRUK") } } }

@Composable private fun ReportsNew(session: Session, tx: List<Transaction>, shift: Shift?, onDetail: (Transaction) -> Unit) { val mine = tx.filter { session.role == Role.OWNER || it.cashierUid == session.uid }; val today = mine.filter { dateKey(it.createdAt) == dateKey(System.currentTimeMillis()) }; val cash = today.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.total }; val qr = today.filter { it.paymentMethod == PaymentMethod.QRIS }.sumOf { it.total }; Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) { Text("Ringkasan", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold); Text("Hari ini", color = Muted, fontSize = 12.sp); Spacer(Modifier.height(10.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatCard("Penjualan", money(cash + qr), Blue, Modifier.weight(1f)); StatCard("Transaksi", today.size.toString(), Green, Modifier.weight(1f)) }; Spacer(Modifier.height(8.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatCard("Cash", money(cash), TextMain, Modifier.weight(1f)); StatCard("QRIS", money(qr), Blue, Modifier.weight(1f)) }; Spacer(Modifier.height(14.dp)); Text("Riwayat transaksi", fontWeight = FontWeight.Bold); LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 6.dp)) { items(mine) { t -> Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onDetail(t) }, RoundedCornerShape(13.dp)) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(t.items.firstOrNull()?.name ?: "Transaksi", fontWeight = FontWeight.Bold); Text("${dateTime(t.createdAt)} · ${t.paymentMethod.name}", fontSize = 11.sp, color = Muted) }; Text(money(t.total), color = Blue, fontWeight = FontWeight.ExtraBold) } } } } } }
@Composable private fun StatCard(label: String, value: String, color: Color, modifier: Modifier) { Card(modifier, RoundedCornerShape(15.dp)) { Column(Modifier.padding(13.dp)) { Text(label, color = Muted, fontSize = 11.sp); Spacer(Modifier.height(4.dp)); Text(value, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) } } }
private fun dateKey(ms: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(ms))

@Composable private fun SettingsNew(session: Session, business: Business?, syncing: Boolean, onOpen: (String) -> Unit, onSync: () -> Unit, onPrinter: () -> Unit, onTheme: () -> Unit, onLogout: () -> Unit) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) { Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(44.dp).clip(CircleShape).background(BlueLight), Alignment.Center) { Text(session.username.take(1).uppercase(), color = Blue, fontWeight = FontWeight.Bold) }; Spacer(Modifier.width(10.dp)); Column { Text(session.username, fontWeight = FontWeight.Bold); Text(if (session.role == Role.OWNER) "Owner" else "Kasir", color = Muted, fontSize = 12.sp) } } }; Spacer(Modifier.height(14.dp)); SettingsGroup("Operasional", listOf("Produk" to Icons.Default.Inventory2, "Kategori" to Icons.Default.Category, "Stok" to Icons.Default.Warehouse, "Outlet" to Icons.Default.Store, "Kasir / Pekerja" to Icons.Default.People)) { onOpen(it) }; if (session.role == Role.OWNER) SettingsGroup("Bisnis & akun", listOf("Bisnis" to Icons.Default.Business, "Owner" to Icons.Default.Person)) { onOpen(it) }; SettingsGroup("Pembayaran & perangkat", listOf("QRIS" to Icons.Default.QrCode2, "Printer" to Icons.Default.Print)) { if (it == "Printer") onPrinter() else onOpen(it) }; SettingsGroup("Preferensi", listOf("Tema" to Icons.Default.DarkMode, "Sinkronisasi" to Icons.Default.Sync)) { if (it == "Tema") onTheme() else onSync() }; Spacer(Modifier.height(8.dp)); OutlinedButton(onSync, Modifier.fillMaxWidth()) { Icon(Icons.Default.Sync, null); Spacer(Modifier.width(6.dp)); Text(if (syncing) "MENYINKRONKAN…" else "SINKRONISASI SEKARANG") }; Spacer(Modifier.height(8.dp)); OutlinedButton(onLogout, Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)) { Icon(Icons.Default.Logout, null); Spacer(Modifier.width(6.dp)); Text("KELUAR") } } }

@Composable private fun SettingsGroup(title: String, items: List<Pair<String, ImageVector>>, onClick: (String) -> Unit) { Text(title, Modifier.padding(start = 3.dp, top = 10.dp, bottom = 6.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Muted); Card(Modifier.fillMaxWidth(), RoundedCornerShape(15.dp)) { Column { items.forEachIndexed { i, pair -> Row(Modifier.fillMaxWidth().clickable { onClick(pair.first) }.padding(horizontal = 13.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) { Icon(pair.second, null, tint = Blue, Modifier.size(21.dp)); Spacer(Modifier.width(11.dp)); Text(pair.first, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp); Icon(Icons.Default.ChevronRight, null, tint = Muted) }; if (i != items.lastIndex) HorizontalDivider(Modifier.padding(start = 45.dp)) } } } }

@Composable private fun BottomBar(tab: String, onNavigate: (String) -> Unit) { NavigationBar(containerColor = MaterialTheme.colorScheme.surface) { listOf("kasir" to Icons.Default.PointOfSale, "laporan" to Icons.Default.Assessment, "pengaturan" to Icons.Default.MoreHoriz).forEach { (id, icon) -> NavigationBarItem(selected = tab == id, onClick = { onNavigate(id) }, icon = { Icon(icon, null) }, label = { Text(when(id) { "kasir" -> "Kasir"; "laporan" -> "Laporan"; else -> "Pengaturan" }) }) } } }

@Composable private fun ManagementNew(page: String, vm: PosViewModel, session: Session, products: List<Product>, categories: List<Category>, outlets: List<Outlet>, workers: List<Worker>, expenses: List<Expense>, business: Business?, transactions: List<Transaction>, shift: Shift?, syncing: Boolean, onBack: () -> Unit, onOpen: (String) -> Unit, onSync: () -> Unit, onPrinter: () -> Unit) {
    Column(Modifier.fillMaxSize()) { BackRow(page) { onBack() }; Box(Modifier.weight(1f)) { when (page) {
        "Produk" -> ProductManagement(products, categories, session, vm)
        "Kategori" -> CategoryManagement(categories, products, session, vm)
        "Stok" -> StockManagement(products, vm)
        "Outlet" -> OutletManagement(outlets, workers, shift, vm)
        "Kasir / Pekerja" -> WorkerManagement(workers, outlets, vm)
        "Bisnis" -> BusinessManagement(business, outlets, vm)
        "Owner" -> ProfileManagement(session, business)
        "QRIS" -> QrisSettings()
        "Tema" -> ThemeSettings()
        "Sinkronisasi" -> SyncSettings(transactions, syncing, onSync)
        "Printer" -> PrinterSettings(onPrinter)
        else -> EmptyState(Icons.Default.MoreHoriz, "Belum tersedia", "Halaman ini siap dihubungkan ke data berikutnya.", null, {})
    } } } }

@Composable private fun ProductManagement(products: List<Product>, categories: List<Category>, session: Session, vm: PosViewModel) { var edit by remember { mutableStateOf<Product?>(null) }; var adding by remember { mutableStateOf(false) }; Column(Modifier.fillMaxSize()) { if (products.isEmpty()) EmptyState(Icons.Default.Inventory2, "Belum ada produk", "Buat produk pertama Anda.", null, {}) else LazyColumn(contentPadding = PaddingValues(horizontal = 14.dp, bottom = 90.dp)) { items(products) { p -> ManagementRow(p.name, categories.find { it.id == p.categoryId }?.name ?: "Tanpa kategori", money(p.price), p.active) { edit = p } } }; FloatingActionButton(onClick = { adding = true }, Modifier.padding(16.dp).align(Alignment.End)) { Icon(Icons.Default.Add, null) } }; if (adding) ProductEditDialog(null, categories, session) { vm.saveProduct(it); adding = false } ; edit?.let { p -> ProductEditDialog(p, categories, session) { vm.saveProduct(it); edit = null } } }

@Composable private fun ProductEditDialog(initial: Product?, categories: List<Category>, session: Session, onSave: (Product) -> Unit) { var name by remember(initial) { mutableStateOf(initial?.name ?: "") }; var price by remember(initial) { mutableStateOf(initial?.price?.toString() ?: "") }; var unit by remember(initial) { mutableStateOf(initial?.unit ?: "porsi") }; var category by remember(initial) { mutableStateOf(initial?.categoryId ?: categories.firstOrNull()?.id.orEmpty()) }; AlertDialog(onDismissRequest = { }, title = { Text(if (initial == null) "Tambah Produk" else "Ubah Produk") }, text = { Column { Field("Nama produk", name, { name = it }, "Contoh: Pentol Rebus"); Spacer(Modifier.height(8.dp)); Field("Harga", price, { price = it.filter(Char::isDigit) }, "12000", keyboard = KeyboardType.Number); Spacer(Modifier.height(8.dp)); Field("Satuan", unit, { unit = it }, "porsi"); Spacer(Modifier.height(8.dp)); Text("Kategori", fontSize = 12.sp, color = Muted); LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(categories) { c -> FilterChip(category == c.id, { category = c.id }, { Text(c.name) }) } } } }, confirmButton = { TextButton({ if (name.isNotBlank() && price.toLongOrNull() != null) onSave((initial ?: Product()).copy(name = name, price = price.toLong(), unit = unit, categoryId = category, ownerUid = initial?.ownerUid ?: session.uid, outletId = initial?.outletId ?: session.outletId.orEmpty())) }) { Text("SIMPAN") } }, dismissButton = { TextButton({}) { Text("BATAL") } }) }

@Composable private fun CategoryManagement(categories: List<Category>, products: List<Product>, session: Session, vm: PosViewModel) { var name by remember { mutableStateOf("") }; Column(Modifier.fillMaxSize().padding(14.dp)) { categories.forEach { c -> ManagementRow(c.name, "${products.count { it.categoryId == c.id }} produk", "", true) {} }; Spacer(Modifier.height(12.dp)); Field("Nama kategori baru", name, { name = it }, "Pentol / Es Teh"); Spacer(Modifier.height(8.dp)); PrimaryButton("TAMBAH KATEGORI") { if (name.isNotBlank()) { vm.saveCategory(Category(ownerUid = session.uid, outletId = session.outletId.orEmpty(), name = name)); name = "" } } } }

@Composable private fun StockManagement(products: List<Product>, vm: PosViewModel) { Column(Modifier.fillMaxSize()) { if (products.isEmpty()) EmptyState(Icons.Default.Warehouse, "Belum ada produk", "Stok mengikuti produk yang sudah dibuat.", null, {}) else LazyColumn(contentPadding = PaddingValues(14.dp)) { items(products) { p -> ManagementRow(p.name, if (p.stockEnabled) "Stok dilacak" else "Stok tidak dilacak", if (p.stockEnabled) "${p.stock} ${p.unit}" else "–", p.stockEnabled) { vm.saveProduct(p.copy(stock = (p.stock + 1).coerceAtLeast(0))) } } } } }

@Composable private fun OutletManagement(outlets: List<Outlet>, workers: List<Worker>, shift: Shift?, vm: PosViewModel) { Column(Modifier.fillMaxSize().padding(14.dp)) { outlets.forEach { o -> ManagementRow(o.name, "${workers.count { it.outletId == o.id }} kasir", if (shift?.outletId == o.id) "Shift aktif" else "", o.active) {} }; Text("Tambah outlet dikelola lewat form berikut.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp)) } }
@Composable private fun WorkerManagement(workers: List<Worker>, outlets: List<Outlet>, vm: PosViewModel) { Column(Modifier.fillMaxSize().padding(14.dp)) { workers.forEach { w -> ManagementRow(w.displayName.ifBlank { w.username }, "${outlets.find { it.id == w.outletId }?.name ?: "-"} · Kasir", if (w.active) "Aktif" else "Nonaktif", w.active) {} } } }
@Composable private fun BusinessManagement(business: Business?, outlets: List<Outlet>, vm: PosViewModel) { Column(Modifier.fillMaxSize().padding(14.dp)) { Card(Modifier.fillMaxWidth(), RoundedCornerShape(15.dp)) { Column(Modifier.padding(15.dp)) { SummaryRow("Nama bisnis", business?.name ?: "–"); SummaryRow("WhatsApp", business?.whatsapp ?: "–"); SummaryRow("Outlet", outlets.size.toString()) } }; Spacer(Modifier.height(10.dp)); Text("Data bisnis tetap tersimpan melalui repository Firebase yang sudah ada.", color = Muted, fontSize = 12.sp) } }
@Composable private fun ProfileManagement(session: Session, business: Business?) { Column(Modifier.fillMaxSize().padding(14.dp)) { Card(Modifier.fillMaxWidth(), RoundedCornerShape(15.dp)) { Column(Modifier.padding(15.dp)) { SummaryRow("Username", session.username); SummaryRow("Role", "Owner"); SummaryRow("Bisnis", business?.name ?: "–") } } } }
@Composable private fun QrisSettings() { Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) { Card(Modifier.fillMaxWidth(), RoundedCornerShape(15.dp)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Text("QRIS aktif", Modifier.weight(1f), fontWeight = FontWeight.Bold); Switch(true, {}) } }; Spacer(Modifier.height(15.dp)); Icon(Icons.Default.QrCode2, null, Modifier.size(190.dp), tint = TextMain); Text("QR contoh · hubungkan sumber QRIS saat backend QRIS tersedia", color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center) } }
@Composable private fun ThemeSettings() { Column(Modifier.fillMaxSize().padding(14.dp)) { Text("Tema", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold); Spacer(Modifier.height(8.dp)); Text("Gunakan tombol tema di header untuk berpindah Light / Dark.", color = Muted) } }
@Composable private fun SyncSettings(tx: List<Transaction>, syncing: Boolean, onSync: () -> Unit) { Column(Modifier.fillMaxSize().padding(14.dp)) { Card(Modifier.fillMaxWidth(), RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = BlueLight)) { Column(Modifier.padding(15.dp)) { Text("● Tersinkron", color = Green, fontWeight = FontWeight.Bold); Text("${tx.size} transaksi tersedia", color = Muted, fontSize = 12.sp) } }; Spacer(Modifier.height(10.dp)); PrimaryButton(if (syncing) "MENYINKRONKAN…" else "SINKRONISASI ULANG", onSync) } }
@Composable private fun PrinterSettings(onPrinter: () -> Unit) { Column(Modifier.fillMaxSize().padding(14.dp)) { Text("Printer", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold); Text("Hubungkan printer Bluetooth untuk mencetak struk.", color = Muted); Spacer(Modifier.height(12.dp)); PrimaryButton("PILIH PRINTER", onPrinter) } }

@Composable private fun ManagementRow(title: String, subtitle: String, trailing: String, active: Boolean, onClick: () -> Unit) { Card(Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable(onClick = onClick), RoundedCornerShape(14.dp)) { Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(if (active) BlueLight else MaterialTheme.colorScheme.surfaceVariant), Alignment.Center) { Text(title.take(1).uppercase(), color = if (active) Blue else Muted, fontWeight = FontWeight.Bold) }; Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text(subtitle, color = Muted, fontSize = 11.sp) }; if (trailing.isNotBlank()) Text(trailing, color = if (active) Green else Muted, fontWeight = FontWeight.SemiBold, fontSize = 11.sp); Icon(Icons.Default.ChevronRight, null, tint = Muted) } } }

@Composable private fun EmptyState(icon: ImageVector, title: String, subtitle: String, button: String?, onClick: () -> Unit) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(icon, null, Modifier.size(54.dp), tint = Blue); Spacer(Modifier.height(12.dp)); Text(title, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center); Text(subtitle, color = Muted, textAlign = TextAlign.Center, fontSize = 13.sp); if (button != null) { Spacer(Modifier.height(16.dp)); Button(onClick, shape = RoundedCornerShape(12.dp)) { Text(button) } } } }

@Composable private fun MoneyDialogNew(title: String, label: String, onSave: (Long) -> Unit) { var value by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = {}, title = { Text(title) }, text = { Field(label, value, { value = it.filter(Char::isDigit) }, "200000", keyboard = KeyboardType.Number) }, confirmButton = { TextButton({ value.toLongOrNull()?.let(onSave) }) { Text("SIMPAN") } }, dismissButton = { TextButton({}) { Text("BATAL") } }) }
@Composable private fun CloseDialogNew(shift: Shift?, onSave: (Long) -> Unit) { var value by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = {}, title = { Text("Tutup Shift") }, text = { Column { Text("Kas awal: ${money(shift?.openingCash ?: 0)}", color = Muted); Spacer(Modifier.height(8.dp)); Field("Kas akhir", value, { value = it.filter(Char::isDigit) }, "0", keyboard = KeyboardType.Number) } }, confirmButton = { TextButton({ value.toLongOrNull()?.let(onSave) }) { Text("TUTUP SHIFT") } }, dismissButton = { TextButton({}) { Text("BATAL") } }) }
@Composable private fun PrinterDialogNew(printer: BluetoothPrinter, onDismiss: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text("Printer Bluetooth") }, text = { Text("Gunakan pengelola printer yang sudah ada di V32. Integrasi Firebase tidak disentuh.") }, confirmButton = { TextButton(onDismiss) { Text("SELESAI") } }) }
@Composable private fun TxDialogNew(t: Transaction, printer: BluetoothPrinter, onDismiss: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text("Transaksi ${t.transactionId.takeLast(6)}") }, text = { Column { SummaryRow("Waktu", dateTime(t.createdAt)); SummaryRow("Metode", t.paymentMethod.name); SummaryRow("Total", money(t.total), true); t.items.forEach { SummaryRow("${it.name} × ${it.quantity}", money(it.subtotal)) } } }, confirmButton = { TextButton({ val d = printer.pairedDevices().firstOrNull(); if (d != null) printer.print(t, d.address); onDismiss() }) { Text("CETAK") } }, dismissButton = { TextButton(onDismiss) { Text("TUTUP") } }) }
