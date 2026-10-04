package com.pentolrebus.kasir

import android.Manifest
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.asImageBitmap
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
import com.pentolrebus.kasir.util.UiAssetStore
import com.pentolrebus.kasir.util.Diagnostics
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    var themeMode by remember { mutableStateOf(0) } // 0 system, 1 light, 2 dark
    val dark = when (themeMode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val colors = if (dark) darkColorScheme(
        primary = Color(0xFF6EA8FF),
        onPrimary = Color.White,
        background = Color(0xFF111722),
        surface = Color(0xFF1B2332),
        surfaceVariant = Color(0xFF1B2C4A),
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
                is AuthState.LoggedIn -> MainShellNew(vm, a.session, dark, { themeMode = if (dark) 1 else 2 }, { themeMode = it }, diagnostics)
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
            RoleChoiceScreen("Owner", "Kelola toko & sistem", com.pentolrebus.kasir.R.drawable.ic_owner) { role = Role.OWNER }
            Spacer(Modifier.height(12.dp))
            RoleChoiceScreen("Kasir", "Transaksi & POS", com.pentolrebus.kasir.R.drawable.ic_printer) { role = Role.CASHIER }
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
        Box(modifier = Modifier, contentAlignment = Alignment.Center) { Image(painterResource(R.drawable.app_icon), "Saku Kasir", Modifier.size(size * .72f)) }
    }
}

@Composable private fun RoleChoiceScreen(title: String, subtitle: String, icon: Int, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(BlueLight), contentAlignment = Alignment.Center) { IconRes(icon, Modifier.size(28.dp)) }
        Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold); Text(subtitle, color = Muted, fontSize = 12.sp) }; Text("›", color = Muted, fontSize = 25.sp)
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
private fun MainShellNew(vm: PosViewModel, session: Session, dark: Boolean, onTheme: () -> Unit, onThemeMode: (Int) -> Unit, diagnostics: Diagnostics) {
    FaithfulSakuShell(vm, session, dark, onTheme, onThemeMode)
}

// -----------------------------------------------------------------------------
// V35 screen-faithful native UI layer.
// The HTML mockup is used as the visual/navigation contract; Firebase/repository
// code remains untouched.
// -----------------------------------------------------------------------------

private enum class UiPage {
    HOME, CHECKOUT, CASH, QRIS, DONE,
    REPORTS, REPORT_HISTORY, REPORT_FINANCE, EXPENSES, EXPENSE_DETAIL, TRANSACTION_DETAIL, SHIFT_DETAIL, CLOSE_SHIFT, CLOSED,
    SETTINGS, PROFILE, PRODUCTS, PRODUCT_FORM, PRODUCT_DETAIL, CATEGORIES, STOCK, OUTLETS, OUTLET_DETAIL,
    WORKERS, WORKER_DETAIL, BUSINESS, OWNER, OUTLET_INFO, QRIS_SETTINGS, PRINTER, DROPBOX, RECEIPT, THEME, SYNC, NOTIFICATIONS, ABOUT
}

@Composable
private fun FaithfulSakuShell(vm: PosViewModel, session: Session, dark: Boolean, onTheme: () -> Unit, onThemeMode: (Int) -> Unit) {
    var page by remember { mutableStateOf(UiPage.HOME) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedOutlet by remember { mutableStateOf<Outlet?>(null) }
    var selectedWorker by remember { mutableStateOf<Worker?>(null) }
    var selectedTx by remember { mutableStateOf<Transaction?>(null) }
    var selectedShift by remember { mutableStateOf<Shift?>(null) }
    var selectedExpense by remember { mutableStateOf<Expense?>(null) }
    val assetStore = remember { UiAssetStore(androidx.compose.ui.platform.LocalContext.current) }
    var qrisActive by remember { mutableStateOf(assetStore.qrisEnabled()) }
    var qrImage by remember { mutableStateOf(assetStore.qrImage()) }
    var printerName by remember { mutableStateOf(assetStore.printerName()) }
    var printerAddress by remember { mutableStateOf(assetStore.printerAddress()) }
    var receiptLogo by rememberSaveable { mutableStateOf(true) }
    var receiptId by rememberSaveable { mutableStateOf(true) }
    var receiptDate by rememberSaveable { mutableStateOf(true) }
    var receiptCashier by rememberSaveable { mutableStateOf(true) }
    var receiptPrice by rememberSaveable { mutableStateOf(true) }
    var receiptSubtotal by rememberSaveable { mutableStateOf(true) }
    var receiptDiscount by rememberSaveable { mutableStateOf(true) }
    var receiptTax by rememberSaveable { mutableStateOf(true) }
    var receiptPayment by rememberSaveable { mutableStateOf(true) }
    var receiptChange by rememberSaveable { mutableStateOf(true) }
    var receiptTitle by rememberSaveable { mutableStateOf("") }
    var receiptAddress by rememberSaveable { mutableStateOf("") }
    var receiptPhone by rememberSaveable { mutableStateOf("") }
    var receiptFooter by rememberSaveable { mutableStateOf("Terima kasih!") }
    var cashValue by rememberSaveable { mutableStateOf("") }
    var discount by rememberSaveable { mutableStateOf(0L) }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var proofUri by rememberSaveable { mutableStateOf<String?>(null) }
    var productSearch by rememberSaveable { mutableStateOf("") }
    var productCategory by rememberSaveable { mutableStateOf(0) }
    var reportMode by rememberSaveable { mutableStateOf(0) }
    var expenseFilter by rememberSaveable { mutableStateOf(0) }
    var showProductForm by remember { mutableStateOf(false) }
    var showCategoryForm by remember { mutableStateOf(false) }
    var showOutletForm by remember { mutableStateOf(false) }
    var showWorkerForm by remember { mutableStateOf(false) }
    var showExpenseForm by remember { mutableStateOf(false) }
    var showTxDialog by remember { mutableStateOf(false) }
    var showPrinterDialog by remember { mutableStateOf(false) }
    val products by vm.products.collectAsState()
    val categories by vm.categories.collectAsState()
    val outlets by vm.outlets.collectAsState()
    val workers by vm.workers.collectAsState()
    val expenses by vm.expenses.collectAsState()
    val business by vm.business.collectAsState()
    val cart by vm.cart.collectAsState()
    val shift by vm.shift.collectAsState()
    val txs by vm.transactions.collectAsState()
    val lastTx by vm.lastTransaction.collectAsState()
    val syncing by vm.syncing.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val printer = remember(context) { BluetoothPrinter(context) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) showPrinterDialog = true }
    val openPrinter = {
        if (Build.VERSION.SDK_INT >= 31 && !printer.hasConnectPermission()) permission.launch(Manifest.permission.BLUETOOTH_CONNECT)
        else showPrinterDialog = true
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { proofUri = assetStore.importImage(it, "qris-proof") }
    }
    val pickQrImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { val path = assetStore.importImage(it, "qris") ; if (path != null) { assetStore.setQrImage(path); qrImage = path } }
    }
    val currentTab = when (page) {
        UiPage.REPORTS, UiPage.REPORT_HISTORY, UiPage.REPORT_FINANCE, UiPage.EXPENSES, UiPage.EXPENSE_DETAIL, UiPage.TRANSACTION_DETAIL, UiPage.SHIFT_DETAIL -> "laporan"
        UiPage.SETTINGS, UiPage.PROFILE, UiPage.PRODUCTS, UiPage.PRODUCT_FORM, UiPage.PRODUCT_DETAIL, UiPage.CATEGORIES,
        UiPage.STOCK, UiPage.OUTLETS, UiPage.OUTLET_DETAIL, UiPage.WORKERS, UiPage.WORKER_DETAIL, UiPage.BUSINESS,
        UiPage.OWNER, UiPage.OUTLET_INFO, UiPage.QRIS_SETTINGS, UiPage.PRINTER, UiPage.DROPBOX, UiPage.RECEIPT, UiPage.THEME, UiPage.SYNC, UiPage.NOTIFICATIONS, UiPage.ABOUT -> "pengaturan"
        UiPage.CHECKOUT, UiPage.CASH, UiPage.QRIS, UiPage.DONE, UiPage.CLOSE_SHIFT, UiPage.CLOSED -> "checkout"
        else -> "kasir"
    }
    val goRoot: (String) -> Unit = { tab ->
        page = when (tab) { "laporan" -> UiPage.REPORTS; "pengaturan" -> UiPage.SETTINGS; "checkout" -> UiPage.CHECKOUT; else -> UiPage.HOME }
    }
    val back = {
        page = when (page) {
            UiPage.CASH, UiPage.QRIS -> UiPage.CHECKOUT
            UiPage.CHECKOUT, UiPage.DONE, UiPage.CLOSED -> UiPage.HOME
            UiPage.PRODUCT_FORM, UiPage.PRODUCT_DETAIL, UiPage.CATEGORIES, UiPage.STOCK, UiPage.OUTLETS, UiPage.WORKERS,
            UiPage.BUSINESS, UiPage.OWNER, UiPage.QRIS_SETTINGS, UiPage.PRINTER, UiPage.DROPBOX, UiPage.RECEIPT, UiPage.THEME, UiPage.SYNC,
            UiPage.ABOUT, UiPage.PROFILE, UiPage.OUTLET_INFO, UiPage.NOTIFICATIONS -> UiPage.SETTINGS
            UiPage.OUTLET_DETAIL -> UiPage.OUTLETS
            UiPage.WORKER_DETAIL -> UiPage.WORKERS
            UiPage.EXPENSES, UiPage.REPORT_FINANCE, UiPage.REPORT_HISTORY, UiPage.SHIFT_DETAIL -> UiPage.REPORTS
            UiPage.EXPENSE_DETAIL -> UiPage.EXPENSES
            UiPage.TRANSACTION_DETAIL -> UiPage.REPORTS
            UiPage.CLOSE_SHIFT -> UiPage.SHIFT_DETAIL
            else -> UiPage.SETTINGS
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (page) {
                        UiPage.HOME -> HomeScreen(vm, session, products, categories, outlets, cart, shift, productSearch, { productSearch = it }, productCategory, { productCategory = it }, { vm.add(it) }, { page = UiPage.CHECKOUT }, { page = UiPage.CLOSE_SHIFT }, { page = UiPage.SHIFT_DETAIL }, { page = UiPage.CLOSE_SHIFT })
                        UiPage.CHECKOUT -> CheckoutScreen(cart, discount, paymentMethod, { paymentMethod = it }, { discount = it }, { page = UiPage.CASH }, { page = UiPage.QRIS }, { page = UiPage.HOME }, { page = UiPage.CASH })
                        UiPage.CASH -> CashScreen(cart, discount, cashValue, { cashValue = it }, { page = UiPage.CHECKOUT }, { val total = checkoutTotal(cart, discount); val paid = cashValue.filter(Char::isDigit).toLongOrNull() ?: 0; if (paid >= total) { vm.checkout(PaymentMethod.CASH, paid, discount = discount); cashValue = ""; discount = 0; page = UiPage.DONE } })
                        UiPage.QRIS -> QrisScreen(cart, discount, qrisActive, qrImage, proofUri, { pickImage.launch("image/*") }, { page = UiPage.CHECKOUT }, { vm.checkout(PaymentMethod.QRIS, qrisPath = proofUri, discount = discount); proofUri = null; discount = 0; page = UiPage.DONE })
                        UiPage.DONE -> DoneScreen(lastTx, printer, { page = UiPage.HOME }, { if (printerAddress != null && lastTx != null) printer.print(lastTx!!, printerAddress!!) })
                        UiPage.REPORTS -> ReportsScreen(session, txs, shift, reportMode, { reportMode = it }, { selectedTx = it; page = UiPage.TRANSACTION_DETAIL }, { selectedShift = it; page = UiPage.SHIFT_DETAIL }, { page = UiPage.REPORT_HISTORY }, { page = UiPage.REPORT_FINANCE }, { page = UiPage.EXPENSES })
                        UiPage.REPORT_HISTORY -> HistoryScreen(session, txs, { page = UiPage.REPORTS; reportMode = 0 }, { selectedTx = it; page = UiPage.TRANSACTION_DETAIL })
                        UiPage.REPORT_FINANCE -> FinanceScreen(txs, expenses, expenseFilter, { expenseFilter = it }, { page = UiPage.EXPENSES }, { page = UiPage.REPORTS })
                        UiPage.EXPENSES -> ExpenseScreen(expenses, expenseFilter, { expenseFilter = it }, { selectedExpense = it; page = UiPage.EXPENSE_DETAIL }, { selectedExpense = null; showExpenseForm = true }, { page = UiPage.REPORTS })
                        UiPage.EXPENSE_DETAIL -> selectedExpense?.let { ExpenseDetailScreen(it, session, { selectedExpense = it; showExpenseForm = true }, { vm.deleteExpense(it); selectedExpense = null; page = UiPage.EXPENSES }, { page = UiPage.EXPENSES }) } ?: EmptyScreen("Pengeluaran tidak ditemukan")
                        UiPage.TRANSACTION_DETAIL -> selectedTx?.let { TxDetailScreen(it, printer, { page = UiPage.REPORTS }, { showTxDialog = true }) } ?: EmptyScreen("Transaksi tidak ditemukan")
                        UiPage.SHIFT_DETAIL -> selectedShift?.let { ShiftDetailScreen(it, txs, session, { page = UiPage.REPORTS }, { page = UiPage.CLOSE_SHIFT }) } ?: EmptyScreen("Shift tidak ditemukan")
                        UiPage.CLOSE_SHIFT -> CloseShiftScreen(shift, txs, { page = UiPage.SHIFT_DETAIL }, { value -> vm.closeShift(value); page = UiPage.CLOSED })
                        UiPage.CLOSED -> ClosedScreen(shift, txs, session, { page = UiPage.REPORTS }, { page = UiPage.HOME })
                        UiPage.SETTINGS -> SettingsScreen(session, business, syncing, { page = it }, { vm.syncPending() }, openPrinter, onTheme, { if (shift == null) vm.logout() else page = UiPage.CLOSE_SHIFT })
                        UiPage.PROFILE -> ProfileScreen(session, business, { page = UiPage.SETTINGS })
                        UiPage.PRODUCTS -> ProductsScreen(products, categories, productSearch, { productSearch = it }, productCategory, { productCategory = it }, { selectedProduct = it; page = UiPage.PRODUCT_DETAIL }, { selectedProduct = null; showProductForm = true }, { selectedProduct = it; showProductForm = true }, { page = UiPage.SETTINGS })
                        UiPage.PRODUCT_FORM -> ProductFormScreen(selectedProduct, categories, session, { vm.saveProduct(it); selectedProduct = null; page = UiPage.PRODUCTS }, { page = UiPage.PRODUCTS })
                        UiPage.PRODUCT_DETAIL -> selectedProduct?.let { ProductDetailScreen(it, categories, { selectedProduct = it; page = UiPage.PRODUCT_FORM }, { vm.deleteProduct(it); selectedProduct = null; page = UiPage.PRODUCTS }, { page = UiPage.PRODUCTS }) } ?: EmptyScreen("Produk tidak ditemukan")
                        UiPage.CATEGORIES -> CategoriesScreen(categories, products, { selectedCategory = it }, { showCategoryForm = true }, { vm.deleteCategory(it) }, { page = UiPage.SETTINGS })
                        UiPage.STOCK -> StockScreen(products, { selectedProduct = it; page = UiPage.PRODUCT_FORM }, { page = UiPage.SETTINGS })
                        UiPage.OUTLETS -> OutletsScreen(outlets, workers, shift, { selectedOutlet = it; page = UiPage.OUTLET_DETAIL }, { selectedOutlet = null; showOutletForm = true }, { selectedOutlet = it; showOutletForm = true }, { page = UiPage.SETTINGS })
                        UiPage.OUTLET_DETAIL -> selectedOutlet?.let { OutletDetailScreen(it, workers, txs, shift, { page = UiPage.OUTLETS }, { selectedOutlet = it; showOutletForm = true }, { vm.deleteOutlet(it); selectedOutlet = null; page = UiPage.OUTLETS }) } ?: EmptyScreen("Outlet tidak ditemukan")
                        UiPage.WORKERS -> WorkersScreen(workers, outlets, { selectedWorker = it; page = UiPage.WORKER_DETAIL }, { selectedWorker = null; showWorkerForm = true }, { selectedWorker = it; showWorkerForm = true }, { page = UiPage.SETTINGS })
                        UiPage.WORKER_DETAIL -> selectedWorker?.let { WorkerDetailScreen(it, outlets, txs, { page = UiPage.WORKERS }, { selectedWorker = it; showWorkerForm = true }, { vm.deleteWorker(it); selectedWorker = null; page = UiPage.WORKERS }) } ?: EmptyScreen("Pekerja tidak ditemukan")
                        UiPage.BUSINESS -> BusinessScreen(business, outlets, session, { b -> vm.saveBusiness(b) }, { page = UiPage.SETTINGS })
                        UiPage.OWNER -> OwnerScreen(session, business, { page = UiPage.SETTINGS })
                        UiPage.QRIS_SETTINGS -> QrisSettingsScreen(qrisActive, { qrisActive = it; assetStore.setQrisEnabled(it) }, qrImage, pickQrImage::launch, { assetStore.clearQrImage(); qrImage = null }, { page = UiPage.SETTINGS })
                        UiPage.DROPBOX -> DropboxScreen({ page = UiPage.SETTINGS })
                        UiPage.PRINTER -> PrinterScreen(printer, printerName, printerAddress, { address -> printer.connect(address) }, { info -> printerName = info.name; printerAddress = info.address; assetStore.setPrinter(info.address, info.name) }, { printer.disconnect(); assetStore.clearPrinter(); printerName = null; printerAddress = null }, { address -> printer.testPrint(address) }, { page = UiPage.SETTINGS })
                        UiPage.RECEIPT -> ReceiptScreen(receiptTitle, { receiptTitle = it }, receiptAddress, { receiptAddress = it }, receiptPhone, { receiptPhone = it }, receiptFooter, { receiptFooter = it }, listOf(
                            "Logo" to receiptLogo, "Nomor transaksi" to receiptId, "Tanggal & waktu" to receiptDate, "Kasir" to receiptCashier, "Harga satuan" to receiptPrice,
                            "Subtotal" to receiptSubtotal, "Diskon" to receiptDiscount, "Pajak" to receiptTax, "Metode & uang dibayar" to receiptPayment, "Kembalian" to receiptChange
                        ), { label, value -> when(label) { "Logo" -> receiptLogo = value; "Nomor transaksi" -> receiptId = value; "Tanggal & waktu" -> receiptDate = value; "Kasir" -> receiptCashier = value; "Harga satuan" -> receiptPrice = value; "Subtotal" -> receiptSubtotal = value; "Diskon" -> receiptDiscount = value; "Pajak" -> receiptTax = value; "Metode & uang dibayar" -> receiptPayment = value; "Kembalian" -> receiptChange = value } }, { receiptLogo = true; receiptId = true; receiptDate = true; receiptCashier = true; receiptPrice = true; receiptSubtotal = true; receiptDiscount = true; receiptTax = true; receiptPayment = true; receiptChange = true; receiptTitle = ""; receiptAddress = ""; receiptPhone = ""; receiptFooter = "Terima kasih!" }, { page = UiPage.SETTINGS })
                        UiPage.THEME -> ThemeScreen(dark, onTheme, onThemeMode, { page = UiPage.SETTINGS })
                        UiPage.SYNC -> SyncScreen(txs, expenses, shift, syncing, { vm.syncPending() }, { page = UiPage.SETTINGS })
                        UiPage.NOTIFICATIONS -> NotificationsScreen(products, { page = UiPage.SETTINGS })
                        UiPage.OUTLET_INFO -> OutletInfoScreen(session, outlets, { page = UiPage.SETTINGS })
                        UiPage.ABOUT -> AboutScreen(business, { page = UiPage.SETTINGS })
                    }
                }
                if (page == UiPage.HOME || page == UiPage.CHECKOUT || page == UiPage.REPORTS || page == UiPage.SETTINGS) {
                    BottomScreen(currentTab, cart.sumOf { it.quantity }, goRoot)
                }
            }
        }
    }

    if (showProductForm) {
        ProductFormScreen(selectedProduct, categories, session, { vm.saveProduct(it); showProductForm = false; selectedProduct = null; page = UiPage.PRODUCTS }, { showProductForm = false })
    }
    if (showCategoryForm) {
        SimpleTextDialog("Tambah Kategori", "Nama kategori", "Pentol / Es Teh", { name -> if (name.isNotBlank()) vm.saveCategory(Category(ownerUid = session.uid, outletId = session.outletId.orEmpty(), name = name)); showCategoryForm = false }) { showCategoryForm = false }
    }
    if (showOutletForm) {
        OutletFormDialog(selectedOutlet, session, { vm.saveOutlet(it); showOutletForm = false; selectedOutlet = null }) { showOutletForm = false }
    }
    if (showWorkerForm) {
        WorkerFormDialog(selectedWorker, session, outlets, { vm.saveWorker(it); showWorkerForm = false; selectedWorker = null }) { showWorkerForm = false }
    }
    if (showExpenseForm) {
        ExpenseFormDialog(session, selectedExpense, { vm.saveExpense(it); showExpenseForm = false; selectedExpense = null; page = UiPage.EXPENSES }) { showExpenseForm = false; selectedExpense = null }
    }
    if (showTxDialog && selectedTx != null) {
        TxDialogNew(selectedTx!!, printer) { showTxDialog = false }
    }
    if (showPrinterDialog) {
        PrinterDialogNew(printer, printerAddress, { d -> printer.connect(d.address).onSuccess { info -> printerAddress = info.address; printerName = info.name; assetStore.setPrinter(info.address, info.name) } }, { showPrinterDialog = false })
    }
}

private fun checkoutTotal(cart: List<CartItem>, discount: Long): Long = (cart.sumOf { it.product.price * it.quantity } - discount).coerceAtLeast(0)

@Composable private fun ScreenHeader(title: String, subtitle: String? = null, back: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
        if (back == null) Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        else Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = back, contentPadding = PaddingValues(0.dp)) { Text("‹", fontSize = 34.sp, lineHeight = 30.sp) }
            Spacer(Modifier.width(4.dp)); Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
        subtitle?.let { Text(it, color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp)) }
    }
}

@Composable private fun IconRes(@androidx.annotation.DrawableRes id: Int, modifier: Modifier = Modifier.size(24.dp)) {
    Image(painterResource(id), contentDescription = null, modifier = modifier)
}

@Composable private fun LogoScreen(size: androidx.compose.ui.unit.Dp = 44.dp) {
    Image(painterResource(com.pentolrebus.kasir.R.drawable.sakukasir_logo), contentDescription = "Saku Kasir", modifier = Modifier.size(size).clip(RoundedCornerShape(10.dp)))
}

@Composable private fun HomeScreen(vm: PosViewModel, session: Session, products: List<Product>, categories: List<Category>, outlets: List<Outlet>, cart: List<CartItem>, shift: Shift?, query: String, onQuery: (String) -> Unit, catIndex: Int, onCat: (Int) -> Unit, onAdd: (Product) -> Unit, onCheckout: () -> Unit, onClose: () -> Unit, onShiftDetail: () -> Unit, onStart: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LogoScreen(); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) {
                Text(outlets.find { it.id == session.outletId }?.name ?: "Saku Kasir", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                Text("${session.username} · ${if (session.role == Role.OWNER) "Owner" else "Kasir"}", color = Muted, fontSize = 14.sp)
            }
            IconRes(com.pentolrebus.kasir.R.drawable.ic_theme, Modifier.size(30.dp))
        }
        Spacer(Modifier.height(14.dp))
        Card(Modifier.fillMaxWidth().clickable { if (shift != null) onShiftDetail() else onStart() }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = if (shift == null) MaterialTheme.colorScheme.surface else BlueLight)) {
            Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(if (shift == null) Muted else Green))
                Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) {
                    Text(if (shift == null) "Belum ada Shift" else "Shift Aktif", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(if (shift == null) "Mulai shift untuk menerima transaksi" else "Mulai ${dateTime(shift.startAt)}", color = Muted, fontSize = 13.sp)
                }
                Text(if (shift == null) "MULAI" else "DETAIL", color = Blue, fontWeight = FontWeight.Bold)
            }
        }
        if (shift == null) {
            EmptyScreen("Belum ada Shift", "Mulai shift untuk menerima transaksi", onStart, "MULAI SHIFT")
        } else {
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(query, onQuery, Modifier.weight(1f), placeholder = { Text("Cari produk…") }, singleLine = true, shape = RoundedCornerShape(20.dp))
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) { IconRes(com.pentolrebus.kasir.R.drawable.ic_grid) }
            }
            val names = listOf("Semua") + categories.map { it.name }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 10.dp)) {
                items(names) { name -> FilterChip(selected = names.indexOf(name) == catIndex, onClick = { onCat(names.indexOf(name)) }, label = { Text(name) }) }
            }
            val selectedCat = categories.getOrNull(catIndex - 1)?.id
            val filtered = products.filter { it.active && it.name.contains(query, true) && (selectedCat == null || it.categoryId == selectedCat) }
            if (filtered.isEmpty()) EmptyScreen("Belum ada produk", "Tambahkan produk dari Pengaturan.", null, null)
            else LazyVerticalGrid(GridCells.Fixed(2), Modifier.heightIn(max = 520.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), userScrollEnabled = false) {
                gridItems(filtered) { p -> ProductTileScreen(p) { onAdd(p) } }
            }
            if (cart.isNotEmpty()) {
                Spacer(Modifier.height(10.dp)); Button(onClick = onCheckout, Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)) { Text("${cart.sumOf { it.quantity }} item · ${money(cart.sumOf { it.product.price * it.quantity })}", Modifier.weight(1f)); Text("CHECKOUT ›") }
            }
        }
    }
}

@Composable private fun ProductTileScreen(p: Product, onAdd: () -> Unit) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).clickable(onClick = onAdd).padding(11.dp)) {
        Column { Box(Modifier.size(52.dp).clip(RoundedCornerShape(13.dp)).background(BlueLight), contentAlignment = Alignment.Center) { IconRes(com.pentolrebus.kasir.R.drawable.ic_product, Modifier.size(27.dp)) }; Text(p.name, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp), maxLines = 2); Text(money(p.price), color = Blue, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text(if (p.stockEnabled) "Stok ${p.stock}" else p.unit, color = Muted, fontSize = 11.sp) }
        Box(Modifier.align(Alignment.BottomEnd).size(30.dp).clip(CircleShape).background(Blue), contentAlignment = Alignment.Center) { Text("+", color = Color.White, fontSize = 21.sp) }
    }
}

@Composable private fun CheckoutScreen(cart: List<CartItem>, discount: Long, method: PaymentMethod, onMethod: (PaymentMethod) -> Unit, onDiscount: (Long) -> Unit, onCash: () -> Unit, onQris: () -> Unit, onBack: () -> Unit, onClear: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        ScreenHeader("Checkout", back = onBack)
        if (cart.isEmpty()) { EmptyScreen("Keranjang kosong", "Pilih produk di tab Kasir.", onBack, "PILIH PRODUK"); return }
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp)) { cart.forEach { i -> Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(i.product.name, fontWeight = FontWeight.Bold); Text("${i.quantity} × ${money(i.product.price)}", color = Muted, fontSize = 12.sp) }; Text(money(i.product.price * i.quantity), fontWeight = FontWeight.Bold) } }; HorizontalDivider(); SummaryRow("Subtotal", money(cart.sumOf { it.product.price * it.quantity })); SummaryRow("Diskon", money(discount)); SummaryRow("Total", money(checkoutTotal(cart, discount)), true) } }
        Spacer(Modifier.height(10.dp)); Segmented(listOf("Cash", "QRIS"), if (method == PaymentMethod.CASH) 0 else 1) { onMethod(if (it == 0) PaymentMethod.CASH else PaymentMethod.QRIS) }
        Button(onClick = if (method == PaymentMethod.CASH) onCash else onQris, Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)) { Text(if (method == PaymentMethod.CASH) "LANJUT KE CASH" else "LANJUT KE QRIS") }
        OutlinedButton(onClick = { onDiscount(0) }, Modifier.fillMaxWidth()) { Text("Hapus diskon") }
        OutlinedButton(onClick = onClear, Modifier.fillMaxWidth()) { Text("KOSONGKAN KERANJANG") }
    }
}

@Composable private fun CashScreen(cart: List<CartItem>, discount: Long, cash: String, onCash: (String) -> Unit, onBack: () -> Unit, onFinish: () -> Unit) {
    val total = checkoutTotal(cart, discount); val paid = cash.filter(Char::isDigit).toLongOrNull() ?: 0; val change = paid - total
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        ScreenHeader("Pembayaran Cash", back = onBack)
        StatCard("Total tagihan", money(total), Blue, Modifier.fillMaxWidth())
        OutlinedTextField(cash, { onCash(it.filter(Char::isDigit)) }, Modifier.fillMaxWidth(), label = { Text("Uang diterima") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(14.dp))
        Spacer(Modifier.height(10.dp)); LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(listOf(total, 50000L, 100000L, 200000L).distinct().filter { it >= total }) { v -> AssistChip(onClick = { onCash(v.toString()) }, label = { Text(money(v)) }) } }
        Card(Modifier.fillMaxWidth().padding(top = 12.dp), RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = BlueLight)) { Column(Modifier.padding(14.dp)) { Text("Kembalian", color = Muted); Text(if (change < 0) "Kurang ${money(-change)}" else money(change), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = if (change < 0) Red else Green) } }
        Button(onClick = onFinish, enabled = paid >= total, Modifier.fillMaxWidth().padding(top = 10.dp).height(50.dp), shape = RoundedCornerShape(14.dp)) { Text("SELESAIKAN TRANSAKSI") }
    }
}

@Composable private fun QrisScreen(cart: List<CartItem>, discount: Long, active: Boolean, qrImage: String?, proof: String?, onProof: () -> Unit, onBack: () -> Unit, onFinish: () -> Unit) {
    val total = checkoutTotal(cart, discount)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        ScreenHeader("Pembayaran QRIS", back = onBack)
        if (qrImage != null) StoredImage(qrImage, Modifier.size(210.dp).clip(RoundedCornerShape(16.dp)).background(Color.White).align(Alignment.CenterHorizontally).padding(14.dp))
        else Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) { IconRes(com.pentolrebus.kasir.R.drawable.ic_qris, Modifier.size(54.dp)); Text("QRIS belum dikonfigurasi", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)); Text("Owner harus mengunggah QRIS outlet dari Pengaturan → QRIS.", color = Muted, textAlign = TextAlign.Center, fontSize = 12.sp) } }
        Text(money(total), Modifier.fillMaxWidth().padding(top = 10.dp), textAlign = TextAlign.Center, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text("Scan lewat e-wallet / m-banking", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Muted)
        Text(if (active && qrImage != null) "● Menunggu pembayaran" else "● QRIS belum siap", Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = TextAlign.Center, color = if (active && qrImage != null) Green else Muted, fontWeight = FontWeight.Bold)
        Button(onClick = onFinish, enabled = active && qrImage != null, Modifier.fillMaxWidth().padding(top = 14.dp).height(50.dp), shape = RoundedCornerShape(14.dp)) { Text("PEMBAYARAN DITERIMA") }
        OutlinedButton(onClick = onProof, Modifier.fillMaxWidth()) { Text(if (proof == null) "AMBIL FOTO BUKTI (opsional)" else "FOTO TERLAMPIR · GANTI") }
    }
}

@Composable private fun DoneScreen(t: Transaction?, printer: BluetoothPrinter, onNew: () -> Unit, onPrint: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(26.dp)); Box(Modifier.size(84.dp).clip(CircleShape).background(Green), contentAlignment = Alignment.Center) { Text("✓", color = Color.White, fontSize = 45.sp, fontWeight = FontWeight.Bold) }
        Text("Transaksi Berhasil", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 14.dp)); Text(t?.let { money(it.total) } ?: "", color = Blue, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
        t?.let { Card(Modifier.fillMaxWidth().padding(top = 16.dp), RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp)) { SummaryRow("No. transaksi", it.transactionId.takeLast(8)); SummaryRow("Metode · Bukti", it.paymentMethod.name + if (it.qrisProofPath != null) " · Terlampir" else ""); if (it.paymentMethod == PaymentMethod.CASH) SummaryRow("Kembalian", money(it.change)) } } }
        Button(onClick = onPrint, Modifier.fillMaxWidth().padding(top = 14.dp), shape = RoundedCornerShape(14.dp)) { Text("CETAK STRUK") }
        if (t?.paymentMethod == PaymentMethod.QRIS && t.qrisProofPath != null) { Text("Bukti QRIS", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)); StoredImage(t.qrisProofPath, Modifier.fillMaxWidth().heightIn(max = 220.dp).clip(RoundedCornerShape(12.dp))) }
        OutlinedButton(onClick = onNew, Modifier.fillMaxWidth()) { Text("TRANSAKSI BARU") }
    }
}

@Composable private fun ReportsScreen(session: Session, txs: List<Transaction>, shift: Shift?, mode: Int, onMode: (Int) -> Unit, onTx: (Transaction) -> Unit, onShift: (Shift) -> Unit, onHistory: () -> Unit, onFinance: () -> Unit, onExpense: () -> Unit) {
    val mine = txs.filter { session.role == Role.OWNER || it.cashierUid == session.uid }; val today = mine.filter { dateKey(it.createdAt) == dateKey(System.currentTimeMillis()) }; val tabs = if (session.role == Role.OWNER) listOf("Ringkasan", "Outlet", "Kasir", "Harian", "Bulanan") else listOf("Hari ini", "Riwayat shift"); val safeMode = mode.coerceIn(0, tabs.lastIndex)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp)) {
        ScreenHeader("Laporan", "${session.username} · ${if (session.role == Role.OWNER) "Owner" else "Kasir"}"); Segmented(tabs, safeMode) { onMode(it) }
        if (session.role == Role.CASHIER) {
            if (safeMode == 0) { val cash = today.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.total }; val qr = today.filter { it.paymentMethod == PaymentMethod.QRIS }.sumOf { it.total }; Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) { StatCard("Penjualan", money(cash + qr), Blue, Modifier.weight(1f)); StatCard("Transaksi", today.size.toString(), Green, Modifier.weight(1f)) }; Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) { StatCard("Cash", money(cash), TextMain, Modifier.weight(1f)); StatCard("QRIS", money(qr), Blue, Modifier.weight(1f)) }; Text("Riwayat transaksi", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp, bottom = 7.dp)); today.take(8).forEach { TxRowScreen(it, onTx) }; TextButton(onClick = onHistory) { Text("Lihat semua ›") } }
            else { val shifts = if (shift != null) listOf(shift) else emptyList(); if (shifts.isEmpty()) Text("Belum ada shift.", color = Muted, modifier = Modifier.padding(30.dp)) else shifts.forEach { ShiftRowScreen(it, onShift) } }
        } else {
            when (safeMode) {
                0 -> { val cash = today.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.total }; val qr = today.filter { it.paymentMethod == PaymentMethod.QRIS }.sumOf { it.total }; Text("Ringkasan Bisnis", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 10.dp)); Text("Hari ini · semua outlet", color = Muted); Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) { StatCard("Penjualan", money(cash + qr), Blue, Modifier.weight(1f)); StatCard("Transaksi", today.size.toString(), Green, Modifier.weight(1f)) }; Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) { StatCard("Cash", money(cash), TextMain, Modifier.weight(1f)); StatCard("QRIS", money(qr), Blue, Modifier.weight(1f)) }; Text("Riwayat transaksi", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp)); today.take(8).forEach { TxRowScreen(it, onTx) }; TextButton(onClick = onHistory) { Text("Lihat semua ›") }; Button(onClick = onFinance, Modifier.fillMaxWidth()) { Text("LIHAT LAPORAN KEUANGAN") }; OutlinedButton(onClick = onExpense, Modifier.fillMaxWidth()) { Text("PENGELUARAN") } }
                1 -> { mine.groupBy { it.outletId }.forEach { (outlet, list) -> Card(Modifier.fillMaxWidth().padding(top = 8.dp), RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp)) { Text(outlet.takeLast(8), fontWeight = FontWeight.Bold); Text("${list.size} transaksi · ${money(list.sumOf { it.total })}", color = Muted) } } } }
                2 -> { mine.groupBy { it.cashierUid }.forEach { (uid, list) -> Card(Modifier.fillMaxWidth().padding(top = 8.dp), RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp)) { Text(uid.takeLast(8), fontWeight = FontWeight.Bold); Text("${list.size} transaksi · ${money(list.sumOf { it.total })}", color = Muted) } } } }
                3 -> { val cash = today.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.total }; val qr = today.filter { it.paymentMethod == PaymentMethod.QRIS }.sumOf { it.total }; Text("Harian", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 10.dp)); StatCard("Penjualan", money(cash + qr), Blue, Modifier.fillMaxWidth()); StatCard("Transaksi", today.size.toString(), Green, Modifier.fillMaxWidth().padding(top = 8.dp)) }
                4 -> { val now = java.time.LocalDate.now(); val start = now.withDayOfMonth(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(); val end = now.plusMonths(1).withDayOfMonth(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(); val month = mine.filter { it.createdAt in start until end }; Text("Bulanan", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 10.dp)); StatCard("Penjualan", money(month.sumOf { it.total }), Blue, Modifier.fillMaxWidth()); StatCard("Transaksi", month.size.toString(), Green, Modifier.fillMaxWidth().padding(top = 8.dp)) }
            }
        }
    }
}

@Composable private fun HistoryScreen(session: Session, txs: List<Transaction>, back: () -> Unit, onTx: (Transaction) -> Unit) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) { ScreenHeader("Riwayat transaksi", back = back); txs.filter { session.role == Role.OWNER || it.cashierUid == session.uid }.forEach { TxRowScreen(it, onTx) } } }

@Composable private fun FinanceScreen(txs: List<Transaction>, expenses: List<Expense>, filter: Int, onFilter: (Int) -> Unit, onExpense: () -> Unit, back: () -> Unit) {
    val zone = java.time.ZoneId.systemDefault(); val today = java.time.LocalDate.now(); val start = if (filter == 0) today.atStartOfDay(zone).toInstant().toEpochMilli() else today.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli(); val end = if (filter == 0) today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() else today.plusMonths(1).withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val periodTx = txs.filter { it.createdAt in start until end }; val periodExp = expenses.filter { it.createdAt in start until end }; val gross = periodTx.sumOf { it.subtotal }; val disc = periodTx.sumOf { it.discount }; val net = periodTx.sumOf { it.total }; val exp = periodExp.sumOf { it.amount }; val cash = periodTx.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.total }; val qr = periodTx.filter { it.paymentMethod == PaymentMethod.QRIS }.sumOf { it.total }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) { ScreenHeader("Laporan Keuangan", back = back); Segmented(listOf("Hari ini", "Bulan ini"), filter, onFilter); Card(Modifier.fillMaxWidth().padding(top = 10.dp), RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp)) { SummaryRow("Penjualan kotor", money(gross)); SummaryRow("Diskon", money(-disc)); SummaryRow("Penjualan bersih", money(net), true); SummaryRow("Pengeluaran", money(-exp)); HorizontalDivider(); SummaryRow("Laba bersih", money(net - exp), true) } }; Card(Modifier.fillMaxWidth().padding(top = 10.dp), RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp)) { Text("Pembayaran masuk", fontWeight = FontWeight.Bold); SummaryRow("Cash", money(cash)); SummaryRow("QRIS", money(qr)) } }; OutlinedButton(onClick = onExpense, Modifier.fillMaxWidth()) { Text("PENGELUARAN") } }
}

@Composable private fun ExpenseScreen(expenses: List<Expense>, filter: Int, onFilter: (Int) -> Unit, onOpen: (Expense) -> Unit, onAdd: () -> Unit, back: () -> Unit) {
    val now = System.currentTimeMillis()
    val start = if (filter == 0) java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() else java.time.LocalDate.now().withDayOfMonth(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    val end = if (filter == 0) start + 86_400_000L else java.time.LocalDate.now().plusMonths(1).withDayOfMonth(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    val visible = expenses.filter { it.createdAt in start until end }.sortedByDescending { it.createdAt }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        ScreenHeader("Pengeluaran", back = back); Segmented(listOf("Hari ini", "Bulan ini"), filter, onFilter)
        Card(Modifier.fillMaxWidth().padding(top = 10.dp), RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = BlueLight)) { Column(Modifier.padding(14.dp)) { Text("Total pengeluaran", color = Muted); Text(money(visible.sumOf { it.amount }), color = Red, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold) } }
        visible.forEach { e -> Card(Modifier.fillMaxWidth().padding(top = 8.dp).clickable { onOpen(e) }, RoundedCornerShape(15.dp)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(e.title.ifBlank { e.category }, fontWeight = FontWeight.Bold); Text("${e.category} · ${dateTime(e.createdAt)}", color = Muted, fontSize = 11.sp); if (e.note.isNotBlank()) Text(e.note, color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }; Text(money(e.amount), color = Red, fontWeight = FontWeight.Bold) } } }
        if (visible.isEmpty()) Text("Belum ada pengeluaran pada periode ini.", color = Muted, modifier = Modifier.padding(24.dp).fillMaxWidth(), textAlign = TextAlign.Center)
        Button(onClick = onAdd, Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("TAMBAH PENGELUARAN") }
    }
}

@Composable private fun ExpenseDetailScreen(expense: Expense, session: Session, edit: () -> Unit, delete: () -> Unit, back: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) { ScreenHeader("Detail Pengeluaran", back = back); Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = BlueLight)) { Column(Modifier.padding(14.dp)) { Text("Nominal", color = Muted); Text(money(expense.amount), color = Red, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold) } }; Card(Modifier.fillMaxWidth().padding(top = 10.dp), RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp)) { SummaryRow("Kategori", expense.category); SummaryRow("Tanggal", dateTime(expense.createdAt)); SummaryRow("Outlet", session.outletId ?: "-"); SummaryRow("Dicatat oleh", session.username); SummaryRow("Catatan", expense.note.ifBlank { "-" }) } }; Button(onClick = edit, Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("UBAH PENGELUARAN") }; OutlinedButton(onClick = delete, Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)) { Text("HAPUS") } }
}

@Composable private fun TxDetailScreen(t: Transaction, printer: BluetoothPrinter, back: () -> Unit, showDialog: () -> Unit) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) { ScreenHeader("Transaksi ${t.transactionId.takeLast(8)}", "● Lunas · ${t.paymentMethod.name}", back); Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp)) { SummaryRow("Waktu", dateTime(t.createdAt)); SummaryRow("Subtotal", money(t.subtotal)); SummaryRow("Diskon", money(t.discount)); SummaryRow("Total", money(t.total), true); t.items.forEach { SummaryRow("${it.name} × ${it.quantity}", money(it.subtotal)) } }; }; Button(onClick = showDialog, Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("CETAK STRUK") } } }

@Composable private fun ShiftDetailScreen(s: Shift, txs: List<Transaction>, session: Session, back: () -> Unit, close: () -> Unit) { val list = txs.filter { it.shiftId == s.id }; Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) { ScreenHeader("Detail Shift", back = back); Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp)) { SummaryRow("Status", if (s.closedAt == null) "Shift Aktif" else "Shift selesai"); SummaryRow("Mulai", dateTime(s.startAt)); SummaryRow("Kas awal", money(s.openingCash)); SummaryRow("Transaksi", list.size.toString()); SummaryRow("Penjualan Cash", money(list.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.total })); SummaryRow("Penjualan QRIS", money(list.filter { it.paymentMethod == PaymentMethod.QRIS }.sumOf { it.total })); SummaryRow("Total penjualan", money(list.sumOf { it.total }), true); s.closingCash?.let { SummaryRow("Kas akhir (fisik)", money(it)); SummaryRow("Selisih", money(it - (s.openingCash + list.filter { x -> x.paymentMethod == PaymentMethod.CASH }.sumOf { x -> x.total })), true) } } }; if (s.closedAt == null) Button(onClick = close, Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("TUTUP SHIFT") } } }

@Composable private fun CloseShiftScreen(s: Shift?, txs: List<Transaction>, back: () -> Unit, close: (Long) -> Unit) { if (s == null) { EmptyScreen("Tidak ada shift aktif", "", back, "KEMBALI"); return }; val list = txs.filter { it.shiftId == s.id }; val expected = s.openingCash + list.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.total }; var cash by rememberSaveable { mutableStateOf("") }; val v = cash.toLongOrNull(); Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) { ScreenHeader("Tutup Shift", back = back); Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp)) { SummaryRow("Waktu mulai", dateTime(s.startAt)); SummaryRow("Kas awal", money(s.openingCash)); SummaryRow("Penjualan Cash", money(list.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.total })); SummaryRow("Penjualan QRIS", money(list.filter { it.paymentMethod == PaymentMethod.QRIS }.sumOf { it.total })); SummaryRow("Total penjualan", money(list.sumOf { it.total }), true); SummaryRow("Uang seharusnya di laci", money(expected), true) } }; OutlinedTextField(cash, { cash = it.filter(Char::isDigit) }, Modifier.fillMaxWidth().padding(top = 10.dp), label = { Text("Kas akhir (hitung uang di laci)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(14.dp)); v?.let { Text(if (it == expected) "Cocok. Tidak ada selisih." else if (it > expected) "Lebih ${money(it - expected)}" else "Kurang ${money(expected - it)}", color = if (it == expected) Green else Red, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp)) }; Button(onClick = { if (v != null) close(v) }, enabled = v != null, Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("TUTUP SHIFT") } } }

@Composable private fun ClosedScreen(s: Shift?, txs: List<Transaction>, session: Session, done: () -> Unit, logout: () -> Unit) { val list = txs.filter { it.shiftId == s?.id }; Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) { Spacer(Modifier.height(25.dp)); Box(Modifier.size(84.dp).clip(CircleShape).background(Green), contentAlignment = Alignment.Center) { Text("✓", color = Color.White, fontSize = 45.sp) }; Text("Shift berhasil ditutup", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 14.dp)); Card(Modifier.fillMaxWidth().padding(top = 14.dp), RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp)) { SummaryRow("Total penjualan", money(list.sumOf { it.total }), true); SummaryRow("Transaksi", list.size.toString()); SummaryRow("Kas akhir", money(s?.closingCash ?: 0)) } }; Button(onClick = if (session.role == Role.OWNER) done else logout, Modifier.fillMaxWidth().padding(top = 14.dp)) { Text(if (session.role == Role.OWNER) "SELESAI" else "KELUAR") } } }

@Composable private fun SettingsScreen(session: Session, business: Business?, syncing: Boolean, onOpen: (UiPage) -> Unit, onSync: () -> Unit, onPrinter: () -> Unit, onTheme: () -> Unit, onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        ScreenHeader("Pengaturan", "${session.username} · ${if (session.role == Role.OWNER) "Owner" else "Kasir"}")
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(58.dp).clip(CircleShape).background(BlueLight), contentAlignment = Alignment.Center) { Text(session.username.take(1).uppercase(), color = Blue, fontSize = 22.sp) }; Spacer(Modifier.width(12.dp)); Column { Text(session.username, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text(if (session.role == Role.OWNER) "Owner" else "Kasir", color = Muted) } } }
        if (session.role == Role.OWNER) {
            SettingsSectionScreen("Operasional", listOf("Produk" to R.drawable.ic_product, "Kategori" to R.drawable.ic_category, "Stok" to R.drawable.ic_stock, "Outlet" to R.drawable.ic_store, "Kasir / Pekerja" to R.drawable.ic_worker, "Owner" to R.drawable.ic_owner, "Bisnis" to R.drawable.ic_business), { name -> onOpen(if (name == "Outlet") UiPage.OUTLETS else settingPage(name)) })
            SettingsSectionScreen("Pembayaran & perangkat", listOf("QRIS" to R.drawable.ic_qris, "Printer" to R.drawable.ic_printer), onOpen, printer = onPrinter)
            SettingsSectionScreen("Penyimpanan", listOf("Dropbox" to R.drawable.ic_cloud), onOpen)
            SettingsSectionScreen("Struk & aplikasi", listOf("Edit Struk" to R.drawable.ic_printer, "Tema" to R.drawable.ic_theme, "Sinkronisasi" to R.drawable.ic_sync, "Lainnya" to R.drawable.ic_info), onOpen)
        } else {
            SettingsSectionScreen("Akun & perangkat", listOf("Profil" to R.drawable.ic_person, "Outlet" to R.drawable.ic_store, "Printer Bluetooth" to R.drawable.ic_printer, "Tema" to R.drawable.ic_theme, "Sinkronisasi" to R.drawable.ic_sync, "Notifikasi" to R.drawable.ic_notification, "Tentang aplikasi" to R.drawable.ic_info), onOpen, printer = onPrinter)
        }
        Button(onClick = onSync, Modifier.fillMaxWidth().padding(top = 10.dp)) { Text(if (syncing) "MENYINKRONKAN…" else "SINKRONISASI SEKARANG") }
        OutlinedButton(onClick = onLogout, Modifier.fillMaxWidth().padding(top = 8.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)) { Text("KELUAR") }
    }
}

@Composable private fun SettingsSectionScreen(title: String, rows: List<Pair<String, Int>>, onOpen: (UiPage) -> Unit, printer: (() -> Unit)? = null) {
    Text(title, color = Muted, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 7.dp)); Card(Modifier.fillMaxWidth(), RoundedCornerShape(17.dp)) { Column { rows.forEachIndexed { i, row -> Row(Modifier.fillMaxWidth().clickable { if (row.first == "Printer" || row.first == "Printer Bluetooth") printer?.invoke() ?: onOpen(settingPage(row.first)) else onOpen(settingPage(row.first)) }.padding(horizontal = 15.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) { IconRes(row.second, Modifier.size(24.dp)); Spacer(Modifier.width(12.dp)); Text(row.first, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 15.sp); Text("›", color = Muted, fontSize = 25.sp) }; if (i < rows.lastIndex) HorizontalDivider(Modifier.padding(start = 52.dp)) } } }
}
private fun settingPage(name: String) = when (name) { "Profil" -> UiPage.PROFILE; "Produk" -> UiPage.PRODUCTS; "Kategori" -> UiPage.CATEGORIES; "Stok" -> UiPage.STOCK; "Outlet" -> UiPage.OUTLET_INFO; "Kasir / Pekerja" -> UiPage.WORKERS; "Bisnis" -> UiPage.BUSINESS; "Owner" -> UiPage.OWNER; "QRIS" -> UiPage.QRIS_SETTINGS; "Printer", "Printer Bluetooth" -> UiPage.PRINTER; "Edit Struk" -> UiPage.RECEIPT; "Tema" -> UiPage.THEME; "Sinkronisasi" -> UiPage.SYNC; "Notifikasi" -> UiPage.NOTIFICATIONS; "Dropbox" -> UiPage.DROPBOX; else -> UiPage.ABOUT }

@Composable private fun ProfileScreen(session: Session, business: Business?, back: () -> Unit) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) { ScreenHeader("Profil", back = back); Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp)) { SummaryRow("Username", session.username); SummaryRow("Role", if (session.role == Role.OWNER) "Owner" else "Kasir"); SummaryRow("Bisnis", business?.name ?: "–") } } } }
@Composable private fun ProductDetailScreen(p: Product, categories: List<Category>, edit: () -> Unit, delete: () -> Unit, back: () -> Unit) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) { ScreenHeader("Detail Produk", back = back); Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp)) { Column(Modifier.padding(16.dp)) { Text(p.name, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold); SummaryRow("Harga", money(p.price)); SummaryRow("Satuan", p.unit); SummaryRow("Kategori", categories.find { it.id == p.categoryId }?.name ?: "Tanpa kategori"); SummaryRow("Stok", if (p.stockEnabled) p.stock.toString() else "Tidak dilacak"); SummaryRow("Status", if (p.active) "Aktif" else "Nonaktif") } }; Button(onClick = edit, Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("UBAH PRODUK") }; OutlinedButton(onClick = delete, Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)) { Text("HAPUS") } } }

@Composable private fun ProductsScreen(products: List<Product>, categories: List<Category>, query: String, onQuery: (String) -> Unit, cat: Int, onCat: (Int) -> Unit, onOpen: (Product) -> Unit, onAdd: () -> Unit, onEdit: (Product) -> Unit, back: () -> Unit) { Column(Modifier.fillMaxSize()) { ScreenHeader("Produk", back = back); Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) { OutlinedTextField(query, onQuery, Modifier.fillMaxWidth(), placeholder = { Text("Cari produk…") }, singleLine = true, shape = RoundedCornerShape(20.dp)); val names = listOf("Semua") + categories.map { it.name }; LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 10.dp)) { items(names) { n -> FilterChip(names.indexOf(n) == cat, { onCat(names.indexOf(n)) }, { Text(n) }) } }; val c = categories.getOrNull(cat - 1)?.id; products.filter { it.name.contains(query, true) && (c == null || it.categoryId == c) }.forEach { p -> Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).clickable { onOpen(p) }.padding(11.dp).padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) { IconRes(com.pentolrebus.kasir.R.drawable.ic_product, Modifier.size(44.dp)); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(p.name, fontWeight = FontWeight.Bold); Text("${money(p.price)} / ${p.unit} · ${categories.find { it.id == p.categoryId }?.name ?: "Tanpa kategori"}", color = Muted, fontSize = 11.sp) }; TextButton(onClick = { onEdit(p) }) { Text("Ubah") } } }; Spacer(Modifier.height(80.dp)) }; FloatingActionButton(onClick = onAdd, Modifier.padding(18.dp).align(Alignment.End)) { Text("+") } } }

@Composable private fun ProductFormScreen(initial: Product?, categories: List<Category>, session: Session, save: (Product) -> Unit, back: () -> Unit) { var name by remember(initial) { mutableStateOf(initial?.name ?: "") }; var price by remember(initial) { mutableStateOf(initial?.price?.toString() ?: "") }; var unit by remember(initial) { mutableStateOf(initial?.unit ?: "porsi") }; var category by remember(initial) { mutableStateOf(initial?.categoryId ?: categories.firstOrNull()?.id.orEmpty()) }; var active by remember(initial) { mutableStateOf(initial?.active ?: true) }; var track by remember(initial) { mutableStateOf(initial?.stockEnabled ?: true) }; var stock by remember(initial) { mutableStateOf(initial?.stock?.toString() ?: "") }; var min by remember(initial) { mutableStateOf(initial?.lowStockThreshold?.toString() ?: "5") }; Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) { ScreenHeader(if (initial == null) "Tambah Produk" else "Ubah Produk", "Langkah 1 dari 2 · Info", back); Field("Nama produk", name, { name = it }, "Contoh: Pentol Rebus"); Spacer(Modifier.height(7.dp)); Text("Kategori", color = Muted, fontSize = 12.sp); LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(vertical = 6.dp)) { items(categories) { c -> FilterChip(category == c.id, { category = c.id }, { Text(c.name) }) } }; Field("Harga", price, { price = it.filter(Char::isDigit) }, "12000", keyboard = KeyboardType.Number); Field("Satuan", unit, { unit = it }, "porsi"); Text("Stok dan status", fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 12.dp)); SwitchRowScreen("Produk aktif", active) { active = it }; SwitchRowScreen("Lacak stok", track) { track = it }; if (track) { Field("Stok", stock, { stock = it.filter(Char::isDigit) }, "0", keyboard = KeyboardType.Number); Field("Batas stok menipis", min, { min = it.filter(Char::isDigit) }, "5", keyboard = KeyboardType.Number) }; Button(onClick = { val pp = (initial ?: Product()).copy(ownerUid = initial?.ownerUid ?: session.uid, outletId = initial?.outletId ?: session.outletId.orEmpty(), name = name, price = price.toLongOrNull() ?: 0, unit = unit, categoryId = category, active = active, stockEnabled = track, stock = stock.toLongOrNull() ?: 0, lowStockThreshold = min.toLongOrNull() ?: 5); save(pp) }, Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("SIMPAN") } }
}

@Composable private fun CategoriesScreen(categories: List<Category>, products: List<Product>, selected: (Category) -> Unit, add: () -> Unit, delete: (Category) -> Unit, back: () -> Unit) { Column(Modifier.fillMaxSize()) { ScreenHeader("Kategori", back = back); LazyColumn(contentPadding = PaddingValues(16.dp)) { items(categories) { c -> Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(15.dp)).background(MaterialTheme.colorScheme.surface).clickable { selected(c) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { IconRes(com.pentolrebus.kasir.R.drawable.ic_category); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(c.name, fontWeight = FontWeight.Bold); Text("${products.count { it.categoryId == c.id }} produk", color = Muted) }; TextButton(onClick = { delete(c) }) { Text("Hapus", color = Red) } } }; item { if (categories.isEmpty()) Text("Belum ada kategori.", color = Muted); Button(onClick = add, Modifier.fillMaxWidth()) { Text("TAMBAH KATEGORI") } } } } }
@Composable private fun StockScreen(products: List<Product>, edit: (Product) -> Unit, back: () -> Unit) { Column(Modifier.fillMaxSize()) { ScreenHeader("Stok", back = back); LazyColumn(contentPadding = PaddingValues(16.dp)) { items(products) { p -> Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(15.dp)).background(MaterialTheme.colorScheme.surface).clickable { edit(p) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { IconRes(com.pentolrebus.kasir.R.drawable.ic_stock); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(p.name, fontWeight = FontWeight.Bold); Text(if (p.stockEnabled) "Stok dilacak · ${p.stock} ${p.unit}" else "Stok tidak dilacak", color = Muted) }; Text(if (p.stockEnabled && p.stock <= p.lowStockThreshold) "Menipis" else "OK", color = if (p.stockEnabled && p.stock <= p.lowStockThreshold) Red else Green, fontWeight = FontWeight.Bold) } } } } }

@Composable private fun OutletsScreen(outlets: List<Outlet>, workers: List<Worker>, shift: Shift?, open: (Outlet) -> Unit, add: () -> Unit, edit: (Outlet) -> Unit, back: () -> Unit) { Column(Modifier.fillMaxSize()) { ScreenHeader("Outlet", back = back); LazyColumn(contentPadding = PaddingValues(16.dp)) { items(outlets) { o -> Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(15.dp)).background(MaterialTheme.colorScheme.surface).clickable { open(o) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { IconRes(com.pentolrebus.kasir.R.drawable.ic_store); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(o.name, fontWeight = FontWeight.Bold); Text("${workers.count { it.outletId == o.id }} kasir", color = Muted) }; Text(if (shift?.outletId == o.id && shift.closedAt == null) "Shift aktif" else "›", color = if (shift?.outletId == o.id) Green else Muted); TextButton(onClick = { edit(o) }) { Text("Ubah") } } }; item { Button(onClick = add, Modifier.fillMaxWidth()) { Text("TAMBAH OUTLET") } } } } }
@Composable private fun OutletDetailScreen(o: Outlet, workers: List<Worker>, txs: List<Transaction>, shift: Shift?, back: () -> Unit, edit: () -> Unit, delete: () -> Unit) { val list = txs.filter { it.outletId == o.id }; Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) { ScreenHeader("Detail Outlet", back = back); Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp)) { SummaryRow("Nama outlet", o.name); SummaryRow("Alamat", o.address ?: "-"); SummaryRow("Telepon", o.phone ?: "-"); SummaryRow("Kasir", workers.count { it.outletId == o.id }.toString()); SummaryRow("Transaksi", list.size.toString()); SummaryRow("Penjualan", money(list.sumOf { it.total })) } }; Button(onClick = edit, Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("UBAH OUTLET") }; OutlinedButton(onClick = delete, Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)) { Text("HAPUS") } } }

@Composable private fun WorkersScreen(workers: List<Worker>, outlets: List<Outlet>, open: (Worker) -> Unit, add: () -> Unit, edit: (Worker) -> Unit, back: () -> Unit) { Column(Modifier.fillMaxSize()) { ScreenHeader("Kasir / Pekerja", "Owner › Bisnis › Outlet › Pekerja", back); LazyColumn(contentPadding = PaddingValues(16.dp)) { items(workers) { w -> Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(15.dp)).background(MaterialTheme.colorScheme.surface).clickable { open(w) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { IconRes(com.pentolrebus.kasir.R.drawable.ic_people); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(w.displayName.ifBlank { w.username }, fontWeight = FontWeight.Bold); Text("${outlets.find { it.id == w.outletId }?.name ?: "-"} · Kasir", color = Muted) }; Text(if (w.active) "Aktif" else "Nonaktif", color = if (w.active) Green else Muted); TextButton(onClick = { edit(w) }) { Text("Ubah") } } }; item { Button(onClick = add, Modifier.fillMaxWidth()) { Text("TAMBAH PEKERJA") } } } } }
@Composable private fun WorkerDetailScreen(w: Worker, outlets: List<Outlet>, txs: List<Transaction>, back: () -> Unit, edit: () -> Unit, delete: () -> Unit) { val list = txs.filter { it.cashierUid == w.id }; Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) { ScreenHeader("Detail Pekerja", back = back); Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp)) { SummaryRow("Nama", w.displayName.ifBlank { w.username }); SummaryRow("Username", w.username); SummaryRow("Outlet", outlets.find { it.id == w.outletId }?.name ?: "-"); SummaryRow("Status", if (w.active) "Aktif" else "Nonaktif"); SummaryRow("Transaksi", list.size.toString()) } }; Button(onClick = edit, Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("UBAH PEKERJA") }; OutlinedButton(onClick = delete, Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)) { Text("HAPUS") } } }

@Composable private fun BusinessScreen(b: Business?, outlets: List<Outlet>, session: Session, save: (Business) -> Unit, back: () -> Unit) { var name by remember(b) { mutableStateOf(b?.name ?: "") }; var wa by remember(b) { mutableStateOf(b?.whatsapp ?: "") }; var address by remember(b) { mutableStateOf(b?.address ?: "") }; Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) { ScreenHeader("Bisnis", back = back); Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp)) { SummaryRow("Outlet", outlets.size.toString()); SummaryRow("Status", if (b?.active == true) "Aktif" else "Belum diatur") } }; Field("Nama bisnis", name, { name = it }, "Nama bisnis"); Field("WhatsApp", wa, { wa = it }, "08xxxxxxxxxx", keyboard = KeyboardType.Phone); Field("Alamat", address, { address = it }, "Alamat bisnis"); Button(onClick = { save((b ?: Business(ownerUid = session.uid)).copy(ownerUid = b?.ownerUid ?: session.uid, name = name, whatsapp = wa, address = address)) }, Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("SIMPAN") } } }
@Composable private fun OwnerScreen(session: Session, business: Business?, back: () -> Unit) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) { ScreenHeader("Profil Owner", back = back); Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp)) { SummaryRow("Username", session.username); SummaryRow("Role", "Owner"); SummaryRow("Bisnis", business?.name ?: "-"); SummaryRow("UID", session.uid.take(12) + "…") } } } }

@Composable private fun StoredImage(path: String, modifier: Modifier = Modifier) {
    val bitmap = remember(path) { BitmapFactory.decodeFile(path) }
    if (bitmap != null) Image(bitmap.asImageBitmap(), contentDescription = "Gambar tersimpan", modifier = modifier)
    else Box(modifier, contentAlignment = Alignment.Center) { Text("Gambar tidak tersedia", color = Muted) }
}

@Composable private fun QrisSettingsScreen(active: Boolean, onActive: (Boolean) -> Unit, image: String?, pick: () -> Unit, clear: () -> Unit, back: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        ScreenHeader("QRIS", back = back)
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp)) { SwitchRowScreen("QRIS aktif", active, onActive) }
        Spacer(Modifier.height(14.dp))
        if (image != null) StoredImage(image, Modifier.size(220.dp).clip(RoundedCornerShape(16.dp)).background(Color.White).align(Alignment.CenterHorizontally).padding(12.dp))
        else Card(Modifier.size(220.dp).align(Alignment.CenterHorizontally), RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { IconRes(com.pentolrebus.kasir.R.drawable.ic_qris, Modifier.size(60.dp)); Text("Belum ada QRIS", fontWeight = FontWeight.Bold); Text("Unggah QRIS outlet Anda", color = Muted, fontSize = 12.sp) } }
        Text(if (image != null) "QRIS outlet" else "Belum ada gambar QRIS", Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center, color = Muted)
        Button(onClick = pick, Modifier.fillMaxWidth().padding(top = 12.dp)) { Text(if (image == null) "UNGGAH QRIS" else "GANTI QRIS") }
        if (image != null) OutlinedButton(onClick = clear, Modifier.fillMaxWidth()) { Text("HAPUS QRIS") }
    }
}

@Composable private fun PrinterScreen(printer: BluetoothPrinter, selectedName: String?, selectedAddress: String?, connect: (String) -> Result<BluetoothPrinter.DeviceInfo>, onConnected: (BluetoothPrinter.DeviceInfo) -> Unit, disconnect: () -> Unit, test: (String) -> Result<Unit>, back: () -> Unit) {
    val scope = rememberCoroutineScope()
    val devices = remember { printer.pairedDevices() }
    var message by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        ScreenHeader("Printer", "Printer Bluetooth yang sudah dipasangkan di Android.", back)
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (selectedAddress != null && printer.isConnected(selectedAddress)) BlueLight else MaterialTheme.colorScheme.surfaceVariant)) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(10.dp).clip(CircleShape).background(if (selectedAddress != null && printer.isConnected(selectedAddress)) Green else Muted)); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(if (selectedAddress != null && printer.isConnected(selectedAddress)) "Terhubung" else "Tidak terhubung", fontWeight = FontWeight.Bold); Text(selectedName ?: "Belum memilih printer", color = Muted, fontSize = 12.sp) } }
        }
        Text("Perangkat dipasangkan", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        if (devices.isEmpty()) Text("Tidak ada printer Bluetooth yang sudah dipasangkan. Pasangkan printer dari Pengaturan Android terlebih dahulu.", color = Muted)
        devices.forEach { d ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(15.dp)).background(MaterialTheme.colorScheme.surface).padding(14.dp), verticalAlignment = Alignment.CenterVertically) { IconRes(com.pentolrebus.kasir.R.drawable.ic_printer); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(d.name, fontWeight = FontWeight.Bold); Text(d.address, color = Muted, fontSize = 10.sp) }; if (selectedAddress == d.address && printer.isConnected(d.address)) TextButton(onClick = disconnect) { Text("PUTUSKAN", color = Red) } else TextButton(onClick = { scope.launch(Dispatchers.IO) { val result = connect(d.address); withContext(kotlinx.coroutines.Dispatchers.Main) { message = result.fold({ onConnected(it); "Terhubung ke ${it.name}" }, { "Gagal terhubung: ${it.message ?: "error"}" }) } } }) { Text("HUBUNGKAN") } }
        }
        if (selectedAddress != null && printer.isConnected(selectedAddress)) Button(onClick = { val address = selectedAddress; if (address != null) scope.launch(Dispatchers.IO) { val result = test(address); withContext(kotlinx.coroutines.Dispatchers.Main) { message = result.fold({ "Test print berhasil." }, { "Test print gagal: ${it.message ?: "error"}" }) } } }, Modifier.fillMaxWidth().padding(top = 4.dp)) { Text("TEST PRINT") }
        message?.let { Text(it, color = if (it.startsWith("Test print berhasil")) Green else Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
    }
}

@Composable
private fun DropboxScreen(back: () -> Unit) {
    var showConnectInfo by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        ScreenHeader("Dropbox", back = back)
        Spacer(Modifier.height(4.dp))
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = BlueLight)) {
            Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                    IconRes(R.drawable.ic_cloud, Modifier.size(34.dp))
                }
                Text("Hubungkan Dropbox", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 12.dp))
                Text("Simpan foto produk, QRIS, dan logo bisnis langsung ke Dropbox Anda.", color = Muted, textAlign = TextAlign.Center, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
        Text("PENYIMPANAN", color = Muted, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 7.dp))
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(17.dp)) {
            Column(Modifier.padding(15.dp)) {
                StorageInfoRow("Foto Produk", "Disimpan di Dropbox")
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                StorageInfoRow("QRIS", "Disimpan di Dropbox")
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                StorageInfoRow("Logo Bisnis", "Disimpan di Dropbox")
            }
        }
        Text("AKUN", color = Muted, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 7.dp))
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(17.dp)) {
            Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { IconRes(R.drawable.ic_cloud, Modifier.size(23.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Belum terhubung", fontWeight = FontWeight.Bold)
                    Text("Hubungkan akun Dropbox milik bisnis ini", color = Muted, fontSize = 12.sp)
                }
            }
        }
        Button(onClick = { showConnectInfo = true }, Modifier.fillMaxWidth().padding(top = 14.dp)) { Text("HUBUNGKAN DROPBOX") }
        Text("Satu akun bisnis dapat menggunakan Dropbox miliknya sendiri. Saku Kasir tidak meminta akses ke file pribadi di luar App Folder.", color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 9.dp))
    }
    if (showConnectInfo) {
        AlertDialog(
            onDismissRequest = { showConnectInfo = false },
            icon = { IconRes(R.drawable.ic_cloud, Modifier.size(32.dp)) },
            title = { Text("Hubungkan Dropbox") },
            text = { Text("Saku Kasir akan membuka halaman Dropbox untuk login dan memberikan izin melalui OAuth. Integrasi OAuth belum diaktifkan pada tahap UI ini.") },
            confirmButton = { TextButton(onClick = { showConnectInfo = false }) { Text("MENGERTI") } },
            dismissButton = { TextButton(onClick = { showConnectInfo = false }) { Text("BATAL") } }
        )
    }
}

@Composable private fun StorageInfoRow(title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconRes(R.drawable.ic_cloud, Modifier.size(23.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, color = Muted, fontSize = 12.sp) }
        Icon(Icons.Default.ChevronRight, null, tint = Muted)
    }
}

@Composable private fun ReceiptScreen(title: String, onTitle: (String) -> Unit, address: String, onAddress: (String) -> Unit, phone: String, onPhone: (String) -> Unit, footer: String, onFooter: (String) -> Unit, toggles: List<Pair<String, Boolean>>, onToggle: (String, Boolean) -> Unit, reset: () -> Unit, back: () -> Unit) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) { ScreenHeader("Edit Struk", "Atur tampilan dan informasi yang dicetak pada struk transaksi.", back); Text("IDENTITAS STRUK", color = Muted, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)); Field("Judul struk", title, onTitle, "Kosong = nama bisnis"); Field("Alamat", address, onAddress, "Kosong = alamat outlet"); Field("Telepon", phone, onPhone, "Nomor telepon"); Field("Pesan bawah struk", footer, onFooter, "Terima kasih!"); Text("ELEMEN YANG DITAMPILKAN", color = Muted, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = 14.dp)); toggles.forEach { SwitchRowScreen(it.first, it.second) { onToggle(it.first, it.second) } }; Text("PRATINJAU", color = Muted, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = 14.dp)); Card(Modifier.fillMaxWidth(), RoundedCornerShape(10.dp)) { Column(Modifier.padding(12.dp)) { Text(title.ifBlank { "Saku Kasir" }, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold); Text("Pentol Rebus × Es Teh Fresh Brew", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontSize = 11.sp); HorizontalDivider(Modifier.padding(vertical = 7.dp)); Text("Pentol Rebus     2 × 12.000", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 11.sp); HorizontalDivider(Modifier.padding(vertical = 7.dp)); Text("TOTAL            Rp 24.000", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp); Text(footer, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontSize = 10.sp, color = Muted) } }; Button(onClick = back, Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("SIMPAN") }; OutlinedButton(onClick = reset, Modifier.fillMaxWidth()) { Text("KEMBALIKAN DEFAULT") } } }

@Composable private fun OutletInfoScreen(session: Session, outlets: List<Outlet>, back: () -> Unit) {
    val outlet = outlets.firstOrNull { it.id == session.outletId }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) { ScreenHeader("Outlet", back = back); Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp)) { SummaryRow("Nama outlet", outlet?.name ?: "-"); SummaryRow("Alamat", outlet?.address ?: "-"); SummaryRow("Telepon", outlet?.phone ?: "-"); Text("Outlet diatur oleh Owner.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) } } }
}

@Composable private fun NotificationsScreen(products: List<Product>, back: () -> Unit) {
    val low = products.filter { it.active && it.stockEnabled && it.stock <= it.lowStockThreshold }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) { ScreenHeader("Notifikasi", "Peringatan stok menipis", back); if (low.isEmpty()) EmptyScreen("Tidak ada peringatan", "Semua stok yang dilacak masih di atas batas minimum.") else low.forEach { p -> Card(Modifier.fillMaxWidth().padding(bottom = 8.dp), RoundedCornerShape(15.dp)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { IconRes(R.drawable.ic_notification); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(p.name, fontWeight = FontWeight.Bold); Text("Stok ${p.stock} · batas ${p.lowStockThreshold}", color = Red, fontSize = 12.sp) } } } } }
}

@Composable private fun ThemeScreen(dark: Boolean, toggleDark: () -> Unit, setMode: (Int) -> Unit, back: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) { ScreenHeader("Tema", back = back); ThemeChoice("Terang", !dark, { setMode(1) }); ThemeChoice("Gelap", dark, { setMode(2) }); ThemeChoice("Sistem", false, { setMode(0) }) }
}
@Composable private fun ThemeChoice(label: String, selected: Boolean, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick).padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f), fontWeight = FontWeight.Bold); Box(Modifier.size(20.dp).clip(CircleShape).border(2.dp, if (selected) Blue else Muted, CircleShape).padding(4.dp).background(if (selected) Blue else Color.Transparent, CircleShape)) } }
@Composable private fun SyncScreen(txs: List<Transaction>, expenses: List<Expense>, shift: Shift?, syncing: Boolean, sync: () -> Unit, back: () -> Unit) {
    val all = buildList { addAll(txs.map { it.syncStatus }); addAll(expenses.map { it.syncStatus }); shift?.let { add(it.syncStatus) } }; val ok = all.count { it == SyncStatus.SYNCED }; val pending = all.count { it == SyncStatus.PENDING_SYNC }; val failed = all.count { it == SyncStatus.SYNC_ERROR }; val latest = txs.maxByOrNull { it.createdAt }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) { ScreenHeader("Sinkronisasi", back = back); Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (failed > 0) MaterialTheme.colorScheme.errorContainer else BlueLight)) { Column(Modifier.padding(14.dp)) { Text(if (failed > 0) "● Perlu perhatian" else if (pending > 0) "● Menunggu sinkronisasi" else "● Tersinkron", color = if (failed > 0) Red else if (pending > 0) Blue else Green, fontWeight = FontWeight.Bold); Text("Terakhir: ${latest?.let { dateTime(it.createdAt) } ?: "belum ada transaksi"}", color = Muted, fontSize = 12.sp) } }; Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 10.dp)) { StatCard("Berhasil", ok.toString(), Green, Modifier.weight(1f)); StatCard("Pending", pending.toString(), Blue, Modifier.weight(1f)); StatCard("Gagal", failed.toString(), Red, Modifier.weight(1f)) }; Text("Perlu perhatian", fontWeight = FontWeight.Bold); Text(if (failed == 0) "Tidak ada data yang gagal." else "$failed data gagal disinkronkan. Coba sinkronisasi ulang.", color = Muted, modifier = Modifier.padding(top = 4.dp)); Button(onClick = sync, Modifier.fillMaxWidth().padding(top = 10.dp)) { Text(if (syncing) "SINKRONISASI…" else "SINKRONISASI ULANG") } }
}

@Composable private fun AboutScreen(business: Business?, back: () -> Unit) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) { ScreenHeader("Tentang", back = back); LogoScreen(80.dp); Text("Saku Kasir", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 10.dp)); Text("Versi 0.1", color = Muted); Card(Modifier.fillMaxWidth().padding(top = 18.dp), RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp)) { SummaryRow("Bisnis", business?.name ?: "-"); SummaryRow("Platform", "Android · native Jetpack Compose"); SummaryRow("Bantuan", "Hubungi Owner") } } } }

@Composable private fun SwitchRowScreen(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold); Switch(checked, onChecked) } }
@Composable private fun EmptyScreen(title: String, subtitle: String = "", action: (() -> Unit)? = null, actionLabel: String? = null) { Column(Modifier.fillMaxWidth().padding(vertical = 38.dp), horizontalAlignment = Alignment.CenterHorizontally) { IconRes(com.pentolrebus.kasir.R.drawable.ic_theme, Modifier.size(58.dp)); Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 10.dp)); if (subtitle.isNotBlank()) Text(subtitle, color = Muted, textAlign = TextAlign.Center); if (action != null && actionLabel != null) Button(onClick = action, modifier = Modifier.padding(top = 14.dp)) { Text(actionLabel) } } }
@Composable private fun BottomScreen(tab: String, count: Int, navigate: (String) -> Unit) { NavigationBar(containerColor = MaterialTheme.colorScheme.surface) { listOf("kasir" to com.pentolrebus.kasir.R.drawable.ic_grid, "checkout" to com.pentolrebus.kasir.R.drawable.ic_cart, "laporan" to com.pentolrebus.kasir.R.drawable.ic_report, "pengaturan" to com.pentolrebus.kasir.R.drawable.ic_more).forEach { (id, icon) -> NavigationBarItem(selected = tab == id, onClick = { navigate(id) }, icon = { Box { IconRes(icon); if (id == "checkout" && count > 0) Box(Modifier.align(Alignment.TopEnd).size(16.dp).clip(CircleShape).background(Red), contentAlignment = Alignment.Center) { Text(count.toString(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold) } } }, label = { Text(when(id) { "kasir" -> "Kasir"; "checkout" -> "Checkout"; "laporan" -> "Laporan"; else -> "Pengaturan" }) }) } } }

@Composable private fun SimpleTextDialog(title: String, label: String, placeholder: String, save: (String) -> Unit, cancel: () -> Unit) { var value by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = cancel, title = { Text(title) }, text = { Field(label, value, { value = it }, placeholder) }, confirmButton = { TextButton(onClick = { save(value) }) { Text("SIMPAN") } }, dismissButton = { TextButton(onClick = cancel) { Text("BATAL") } }) }
@Composable private fun OutletFormDialog(initial: Outlet?, session: Session, save: (Outlet) -> Unit, cancel: () -> Unit) { var name by remember(initial) { mutableStateOf(initial?.name ?: "") }; var address by remember(initial) { mutableStateOf(initial?.address ?: "") }; var phone by remember(initial) { mutableStateOf(initial?.phone ?: "") }; AlertDialog(onDismissRequest = cancel, title = { Text(if (initial == null) "Tambah Outlet" else "Ubah Outlet") }, text = { Column { Field("Nama outlet", name, { name = it }, "Outlet utama"); Field("Alamat", address, { address = it }, "Alamat"); Field("Telepon", phone, { phone = it }, "08xxxxxxxxxx", keyboard = KeyboardType.Phone) } }, confirmButton = { TextButton(onClick = { save((initial ?: Outlet(ownerUid = session.uid)).copy(name = name, address = address, phone = phone)) }) { Text("SIMPAN") } }, dismissButton = { TextButton(onClick = cancel) { Text("BATAL") } }) }
@Composable private fun WorkerFormDialog(initial: Worker?, session: Session, outlets: List<Outlet>, save: (Worker) -> Unit, cancel: () -> Unit) { var name by remember(initial) { mutableStateOf(initial?.displayName ?: "") }; var username by remember(initial) { mutableStateOf(initial?.username ?: "") }; var pin by remember { mutableStateOf("") }; var outlet by remember(initial, outlets) { mutableStateOf(initial?.outletId ?: outlets.firstOrNull()?.id.orEmpty()) }; AlertDialog(onDismissRequest = cancel, title = { Text(if (initial == null) "Tambah Pekerja" else "Ubah Pekerja") }, text = { Column { Field("Nama", name, { name = it }, "Nama kasir"); Field("Username", username, { username = it }, "dewi"); PinInput("PIN", pin) { pin = it }; Text("Outlet", color = Muted, fontSize = 12.sp); LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(outlets) { o -> FilterChip(outlet == o.id, { outlet = o.id }, { Text(o.name) }) } } } }, confirmButton = { TextButton(onClick = { save((initial ?: Worker(ownerUid = session.uid, outletId = outlet, username = username, displayName = name)).copy(username = username, displayName = name, outletId = outlet)) }) { Text("SIMPAN") } }, dismissButton = { TextButton(onClick = cancel) { Text("BATAL") } }) }
@Composable private fun ExpenseFormDialog(session: Session, initial: Expense?, save: (Expense) -> Unit, cancel: () -> Unit) {
    var title by remember(initial) { mutableStateOf(initial?.title ?: "") }
    var amount by remember(initial) { mutableStateOf(initial?.amount?.toString() ?: "") }
    var cat by remember(initial) { mutableStateOf(initial?.category ?: "Operasional") }
    var note by remember(initial) { mutableStateOf(initial?.note ?: "") }
    AlertDialog(
        onDismissRequest = cancel,
        title = { Text(if (initial == null) "Tambah Pengeluaran" else "Ubah Pengeluaran") },
        text = { Column {
            Field("Judul", title, { title = it }, "Beli bahan")
            Field("Jumlah", amount, { amount = it.filter(Char::isDigit) }, "50000", keyboard = KeyboardType.Number)
            Field("Kategori", cat, { cat = it }, "Operasional")
            Field("Catatan", note, { note = it }, "Catatan opsional")
        } },
        confirmButton = { TextButton(onClick = {
            val value = amount.toLongOrNull() ?: 0
            if (title.isNotBlank() && value > 0) save((initial ?: Expense(ownerUid = session.uid, outletId = session.outletId.orEmpty())).copy(title = title, category = cat, amount = value, note = note))
        }) { Text("SIMPAN") } },
        dismissButton = { TextButton(onClick = cancel) { Text("BATAL") } }
    )
}
