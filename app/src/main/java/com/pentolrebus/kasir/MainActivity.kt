package com.pentolrebus.kasir

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pentolrebus.kasir.data.RepositoryProvider
import com.pentolrebus.kasir.data.OfflineStore
import com.pentolrebus.kasir.domain.PaymentMethod
import com.pentolrebus.kasir.domain.Product
import com.pentolrebus.kasir.domain.Role
import com.pentolrebus.kasir.domain.Session
import com.pentolrebus.kasir.ui.AuthState
import com.pentolrebus.kasir.ui.KasirTheme
import com.pentolrebus.kasir.ui.PosViewModel
import com.pentolrebus.kasir.ui.PosViewModelFactory
import com.pentolrebus.kasir.util.Diagnostics
import com.pentolrebus.kasir.util.BluetoothPrinter
import java.text.NumberFormat
import java.util.Locale

private fun money(value: Long): String = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    .apply { maximumFractionDigits = 0 }
    .format(value)
    .replace("Rp", "Rp ")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val diagnostics = Diagnostics(this)
        val repository = RepositoryProvider.create(this, diagnostics)
        val offlineStore = OfflineStore(this)
        setContent {
            KasirTheme {
                val vm: PosViewModel = viewModel(factory = PosViewModelFactory(repository, offlineStore))
                KasirApp(vm, diagnostics)
            }
        }
    }
}

@Composable
private fun KasirApp(vm: PosViewModel, diagnostics: Diagnostics) {
    var dark by remember { mutableStateOf(false) }
    KasirTheme(dark = dark) {
        Surface(Modifier.fillMaxSize()) {
            val auth by vm.auth.collectAsState()
            when (val state = auth) {
                AuthState.LoggedOut -> AuthScreen(vm)
                AuthState.Loading -> LoadingScreen("Memproses…")
                is AuthState.Error -> AuthScreen(vm, state.message)
                is AuthState.LoggedIn -> MainShell(
                    vm = vm,
                    session = state.session,
                    dark = dark,
                    onTheme = { dark = !dark },
                    diagnostics = diagnostics
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.material3.CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(message)
        }
    }
}

@Composable
private fun PasswordField(label: String, value: String, onValue: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Sembunyikan $label" else "Tampilkan $label"
                )
            }
        }
    )
}

@Composable
private fun PinField(label: String, value: String, onValue: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Sembunyikan $label" else "Tampilkan $label"
                )
            }
        }
    )
}

@Composable
private fun AuthScreen(vm: PosViewModel, error: String? = null) {
    var selectedRole by remember { mutableStateOf<Role?>(null) }
    var register by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var business by remember { mutableStateOf("") }
    var outlet by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var validation by remember { mutableStateOf<String?>(null) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (selectedRole == null) {
            RoleSelectionScreen(
                onOwner = { selectedRole = Role.OWNER },
                onCashier = { selectedRole = Role.CASHIER }
            )
        } else {
            LoginRoleScreen(
                role = selectedRole!!,
                register = register,
                email = email,
                password = password,
                confirmPassword = confirmPassword,
                username = username,
                pin = pin,
                confirmPin = confirmPin,
                business = business,
                outlet = outlet,
                whatsapp = whatsapp,
                error = error,
                validation = validation,
                onBack = {
                    selectedRole = null
                    register = false
                    validation = null
                },
                onRegisterToggle = {
                    register = !register
                    validation = null
                },
                onEmail = { email = it },
                onPassword = { password = it },
                onConfirmPassword = { confirmPassword = it },
                onUsername = { username = it },
                onPin = { pin = it },
                onConfirmPin = { confirmPin = it },
                onBusiness = { business = it },
                onOutlet = { outlet = it },
                onWhatsapp = { whatsapp = it },
                onValidation = { validation = it },
                onLoginLocal = { vm.loginLocal(username, pin, selectedRole!!) },
                onLoginEmail = { vm.loginEmail(email, password, selectedRole!!) },
                onRegister = {
                    validation = validateRegistration(email, password, confirmPassword, username, pin, confirmPin)
                    if (validation == null) {
                        vm.register(
                            email,
                            password,
                            username,
                            pin,
                            business.takeIf(String::isNotBlank),
                            outlet.takeIf(String::isNotBlank),
                            whatsapp.takeIf(String::isNotBlank)
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun RoleSelectionScreen(
    onOwner: () -> Unit,
    onCashier: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        Surface(
            modifier = Modifier.width(112.dp).height(112.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(R.drawable.app_icon),
                    contentDescription = "Logo SakuKasir",
                    modifier = Modifier.width(76.dp).height(76.dp)
                )
            }
        }
        Text("SakuKasir", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(
            "Pilih peran Anda untuk masuk",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
        )
        Spacer(Modifier.height(6.dp))
        RoleCard(
            title = "OWNER",
            subtitle = "Kelola toko & sistem",
            icon = Icons.Default.Store,
            onClick = onOwner
        )
        RoleCard(
            title = "KASIR",
            subtitle = "Transaksi & POS",
            icon = Icons.Default.ShoppingCart,
            onClick = onCashier
        )
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun RoleCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(120.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.width(68.dp).height(68.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.width(34.dp).height(34.dp))
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Text("›", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
        }
    }
}

@Composable
private fun LoginRoleScreen(
    role: Role,
    register: Boolean,
    email: String,
    password: String,
    confirmPassword: String,
    username: String,
    pin: String,
    confirmPin: String,
    business: String,
    outlet: String,
    whatsapp: String,
    error: String?,
    validation: String?,
    onBack: () -> Unit,
    onRegisterToggle: () -> Unit,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onConfirmPassword: (String) -> Unit,
    onUsername: (String) -> Unit,
    onPin: (String) -> Unit,
    onConfirmPin: (String) -> Unit,
    onBusiness: (String) -> Unit,
    onOutlet: (String) -> Unit,
    onWhatsapp: (String) -> Unit,
    onValidation: (String?) -> Unit,
    onLoginLocal: () -> Unit,
    onLoginEmail: () -> Unit,
    onRegister: () -> Unit
) {
    val isOwner = role == Role.OWNER
    var emailMode by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme
    val bg = colorScheme.background
    val fieldBg = colorScheme.surface
    val fieldBorder = colorScheme.outline
    val text = colorScheme.onBackground
    val muted = colorScheme.onSurfaceVariant
    val accent = colorScheme.primary

    Surface(Modifier.fillMaxSize(), color = bg) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(2.dp))
            Surface(
                modifier = Modifier.width(96.dp).height(96.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.app_icon),
                        contentDescription = "Logo SakuKasir",
                        modifier = Modifier.width(62.dp).height(62.dp)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("SakuKasir", color = text, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                if (isOwner) "Selamat datang kembali, Owner" else "Selamat datang kembali, Kasir",
                color = muted,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = muted, modifier = Modifier.width(26.dp).height(26.dp))
                }
                Text(
                    if (register && isOwner) "Daftar Owner" else "Login ${if (isOwner) "Owner" else "Kasir"}",
                    color = text,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(14.dp))

            if (isOwner && !register) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .padding(1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AuthTab("PIN Cepat", active = !emailMode, modifier = Modifier.weight(1f), textColor = text, onClick = { emailMode = false })
                    AuthTab("Email & Password", active = emailMode, modifier = Modifier.weight(1f), textColor = if (emailMode) text else muted, onClick = { emailMode = true })
                }
                Spacer(Modifier.height(16.dp))
                if (!emailMode) {
                    AuthField(
                        label = "Username",
                        placeholder = "Masukkan username",
                        value = username,
                        onValue = onUsername,
                        icon = Icons.Default.Person,
                        textColor = text,
                        muted = muted,
                        background = fieldBg,
                        border = fieldBorder
                    )
                    Spacer(Modifier.height(10.dp))
                    AuthPinField(
                        label = "PIN",
                        value = pin,
                        onValue = onPin,
                        textColor = text,
                        muted = muted,
                        background = fieldBg,
                        border = fieldBorder
                    )
                    Spacer(Modifier.height(16.dp))
                    AuthPrimaryButton("Masuk", accent, onLoginLocal)
                } else {
                    AuthField("Email", "Masukkan email", email, onEmail, null, text, muted, fieldBg, fieldBorder, KeyboardType.Email)
                    Spacer(Modifier.height(16.dp))
                    PasswordField("Password", password, onPassword)
                    Spacer(Modifier.height(16.dp))
                    AuthPrimaryButton("Masuk", accent, onLoginEmail)
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onRegisterToggle) {
                    Text("Daftar sebagai Owner", color = accent, style = MaterialTheme.typography.titleMedium)
                }
            } else if (isOwner && register) {
                if (error != null) AlertBox(error)
                if (validation != null) AlertBox(validation)
                AuthField("Email", "Masukkan email", email, onEmail, null, text, muted, fieldBg, fieldBorder, KeyboardType.Email)
                Spacer(Modifier.height(10.dp))
                PasswordField("Password", password, onPassword)
                Spacer(Modifier.height(10.dp))
                PasswordField("Confirm Password", confirmPassword, onConfirmPassword)
                Spacer(Modifier.height(10.dp))
                AuthField("Username", "Masukkan username", username, onUsername, Icons.Default.Person, text, muted, fieldBg, fieldBorder)
                Spacer(Modifier.height(10.dp))
                PinField("PIN", pin, onPin)
                Spacer(Modifier.height(10.dp))
                PinField("Confirm PIN", confirmPin, onConfirmPin)
                Spacer(Modifier.height(10.dp))
                AuthField("Nama Bisnis (opsional)", "Masukkan nama bisnis", business, onBusiness, null, text, muted, fieldBg, fieldBorder)
                Spacer(Modifier.height(10.dp))
                AuthField("Nama Outlet (opsional)", "Masukkan nama outlet", outlet, onOutlet, null, text, muted, fieldBg, fieldBorder)
                Spacer(Modifier.height(10.dp))
                AuthField("WhatsApp (opsional)", "Masukkan WhatsApp", whatsapp, onWhatsapp, null, text, muted, fieldBg, fieldBorder)
                Spacer(Modifier.height(22.dp))
                AuthPrimaryButton("Daftar Owner", accent, onRegister)
                TextButton(onClick = onRegisterToggle) { Text("Sudah punya akun? Login Owner", color = accent) }
            } else {
                if (error != null) AlertBox(error)
                if (validation != null) AlertBox(validation)
                AuthField("Username", "Masukkan username", username, onUsername, Icons.Default.Person, text, muted, fieldBg, fieldBorder)
                Spacer(Modifier.height(16.dp))
                AuthPinField("PIN", pin, onPin, text, muted, fieldBg, fieldBorder)
                Spacer(Modifier.height(22.dp))
                AuthPrimaryButton("Masuk", accent, onLoginLocal)
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun AuthTab(
    label: String,
    active: Boolean,
    modifier: Modifier,
    textColor: androidx.compose.ui.graphics.Color,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick ?: {}
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = textColor, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AuthField(
    label: String,
    placeholder: String,
    value: String,
    onValue: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    textColor: androidx.compose.ui.graphics.Color,
    muted: androidx.compose.ui.graphics.Color,
    background: androidx.compose.ui.graphics.Color,
    border: androidx.compose.ui.graphics.Color,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, color = muted, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValue,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = muted) },
            leadingIcon = icon?.let { { Icon(it, null, tint = muted) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedContainerColor = background,
                unfocusedContainerColor = background,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = border,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun AuthPinField(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    textColor: androidx.compose.ui.graphics.Color,
    muted: androidx.compose.ui.graphics.Color,
    background: androidx.compose.ui.graphics.Color,
    border: androidx.compose.ui.graphics.Color
) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, color = muted, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        var visible by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = value,
            onValueChange = onValue,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Masukkan PIN", color = muted) },
            leadingIcon = { Icon(Icons.Default.Dialpad, null, tint = muted) },
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = muted)
                }
            },
            singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedContainerColor = background,
                unfocusedContainerColor = background,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = border,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun AuthPrimaryButton(label: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = color, contentColor = androidx.compose.ui.graphics.Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

private fun validateRegistration(email: String, password: String, confirmPassword: String, username: String, pin: String, confirmPin: String): String? {
    return when {
        !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Email tidak valid"
        password.length < 8 -> "Password minimal 8 karakter"
        password != confirmPassword -> "Confirm Password tidak cocok"
        username.length < 3 -> "Username minimal 3 karakter"
        !pin.matches(Regex("\\d{4,6}")) -> "PIN harus 4–6 angka"
        pin != confirmPin -> "Confirm PIN tidak cocok"
        else -> null
    }
}

@Composable
private fun AlertBox(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Text(message, Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
private fun MainShell(
    vm: PosViewModel,
    session: Session,
    dark: Boolean,
    onTheme: () -> Unit,
    diagnostics: Diagnostics
) {
    var screen by remember { mutableStateOf("kasir") }
    var settingsPage by remember { mutableStateOf<String?>(null) }
    var showStartShift by remember { mutableStateOf(false) }
    var showCloseShift by remember { mutableStateOf(false) }
    var showPrinterPicker by remember { mutableStateOf(false) }

    val products by vm.products.collectAsState()
    val cart by vm.cart.collectAsState()
    val shift by vm.shift.collectAsState()
    val transactions by vm.transactions.collectAsState()
    val lastTransaction by vm.lastTransaction.collectAsState()
    val syncing by vm.syncing.collectAsState()
    val logout = { if (shift == null) vm.logout() else showCloseShift = true }
    val context = LocalContext.current
    val printer = remember(context) { BluetoothPrinter(context) }
    val bluetoothPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showPrinterPicker = true
    }

    Column(Modifier.fillMaxSize()) {
        AppHeader(session, dark, onTheme, { diagnostics.exportToDownloads() }, logout)
        if (shift == null && screen == "kasir") {
            Surface(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Shift belum dimulai", fontWeight = FontWeight.SemiBold)
                        Text("Mulai shift sebelum menerima transaksi", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(onClick = { showStartShift = true }) { Text("MULAI") }
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (screen) {
                "kasir" -> PosScreen(vm, products, cart, shift) { screen = "checkout" }
                "checkout" -> CheckoutScreen(vm, cart) { screen = "success" }
                "success" -> TransactionSuccessScreen(lastTransaction, printer) { screen = "kasir" }
                "laporan" -> ReportsScreen(transactions, shift, printer, { if (Build.VERSION.SDK_INT >= 31 && !printer.hasConnectPermission()) {
                        bluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    } else showPrinterPicker = true
                }, { vm.syncPending() }, syncing)
                "pengaturan" -> SettingsScreen(session, diagnostics, onTheme, logout, settingsPage, { settingsPage = it }, { settingsPage = null }, printer, {
                    if (Build.VERSION.SDK_INT >= 31 && !printer.hasConnectPermission()) bluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT) else showPrinterPicker = true
                }, { vm.syncPending() }, syncing)
            }
        }
        NavigationBar {
            BottomNavItem("Kasir", Icons.Default.ShoppingCart, screen == "kasir") { settingsPage = null; screen = "kasir" }
            BottomNavItem("Checkout", Icons.Default.Payment, screen == "checkout") { settingsPage = null; screen = "checkout" }
            BottomNavItem("Laporan", Icons.Default.Assessment, screen == "laporan") { settingsPage = null; screen = "laporan" }
            BottomNavItem("Pengaturan", Icons.Default.Settings, screen == "pengaturan") { screen = "pengaturan" }
        }
    }
    if (showStartShift) MoneyDialog("Mulai Shift", "Modal awal", { showStartShift = false }) { vm.startShift(it); showStartShift = false }
    if (showCloseShift) MoneyDialog("Tutup Shift", "Kas akhir", { showCloseShift = false }) { vm.closeShift(it); showCloseShift = false }
    if (showPrinterPicker) PrinterPickerDialog(printer, { showPrinterPicker = false })
}

@Composable
private fun BottomNavItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    val contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = contentColor)
        Text(
            text = label,
            maxLines = 1,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun AppHeader(session: Session, dark: Boolean, onTheme: () -> Unit, onDownload: () -> Unit, onLogout: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Store, contentDescription = null)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(session.username, fontWeight = FontWeight.Bold)
            Text(session.role.name, style = MaterialTheme.typography.labelMedium)
        }
        IconButton(onClick = onTheme) { Icon(Icons.Default.Brightness4, contentDescription = if (dark) "Light mode" else "Dark mode") }
        IconButton(onClick = onDownload) { Icon(Icons.Default.Download, contentDescription = "Download log") }
        TextButton(onClick = onLogout) { Text("Keluar") }
    }
}

@Composable
private fun NavButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) Button(onClick = onClick) { Text(label) } else OutlinedButton(onClick = onClick) { Text(label) }
}

@Composable
private fun OwnerDashboard(session: Session, products: List<Product>, transactions: List<com.pentolrebus.kasir.domain.Transaction>, shift: com.pentolrebus.kasir.domain.Shift?) {
    val total = transactions.sumOf { it.total }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Dashboard Owner", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item { StatCard("Penjualan", money(total)) }
        item { StatCard("Transaksi", transactions.size.toString()) }
        item { StatCard("Produk aktif", products.count { it.active }.toString()) }
        item { StatCard("Shift", if (shift == null) "Belum aktif" else "Aktif") }
        item { Text("Outlet: ${session.outletId ?: "Belum diatur"}") }
    }
}

@Composable
private fun CashierDashboard(session: Session) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Kasir", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Outlet: ${session.outletId ?: "Belum diatur"}")
    }
}

@Composable
private fun StatCard(title: String, value: String) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PosScreen(vm: PosViewModel, products: List<Product>, cart: List<com.pentolrebus.kasir.domain.CartItem>, shift: com.pentolrebus.kasir.domain.Shift?, onCheckout: () -> Unit) {
    val cartTotal = cart.sumOf { it.product.price * it.quantity }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ShoppingCart, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("POS", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("Keranjang: ${cart.sumOf { it.quantity }}")
        }
        if (shift == null) {
            Spacer(Modifier.height(10.dp))
            AlertBox("Shift belum dimulai. Mulai shift sebelum menerima transaksi.")
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(products.filter { it.active }) { product ->
                ProductRow(product, onAdd = { if (shift != null) vm.add(product) })
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Total", style = MaterialTheme.typography.labelLarge)
                    Text(money(cartTotal), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                Button(onClick = onCheckout, enabled = shift != null && cart.isNotEmpty()) { Text("CHECKOUT") }
            }
        }
    }
}

@Composable
private fun ProductRow(product: Product, onAdd: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.SemiBold)
                Text(money(product.price))
                if (product.stockEnabled) Text("Stok ${product.stock}", style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onAdd) { Icon(Icons.Default.Add, contentDescription = "Tambah") }
        }
    }
}

@Composable
private fun CheckoutScreen(vm: PosViewModel, cart: List<com.pentolrebus.kasir.domain.CartItem>, onDone: () -> Unit) {
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var cashPaid by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val total = cart.sumOf { it.product.price * it.quantity }
    val paid = cashPaid.toLongOrNull() ?: 0L
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Checkout", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Total ${money(total)}", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (method == PaymentMethod.CASH) Button(onClick = { method = PaymentMethod.CASH }) { Text("CASH") } else OutlinedButton(onClick = { method = PaymentMethod.CASH }) { Text("CASH") }
            if (method == PaymentMethod.QRIS) Button(onClick = { method = PaymentMethod.QRIS }) { Text("QRIS") } else OutlinedButton(onClick = { method = PaymentMethod.QRIS }) { Text("QRIS") }
        }
        if (method == PaymentMethod.CASH) {
            OutlinedTextField(value=cashPaid,onValueChange={cashPaid=it.filter(Char::isDigit)},label={Text("Uang diterima")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth())
            if (paid >= total && total > 0) Text("Kembalian ${money(paid-total)}", fontWeight=FontWeight.SemiBold)
        } else {
            Text("Konfirmasi pembayaran QRIS setelah pelanggan menyelesaikan pembayaran.")
        }
        error?.let { Text(it, color=MaterialTheme.colorScheme.error) }
        Button(onClick={
            if (cart.isEmpty()) { error="Keranjang kosong" }
            else if (method==PaymentMethod.CASH && paid < total) { error="Uang diterima belum cukup" }
            else { error=null; vm.checkout(method); onDone() }
        },modifier=Modifier.fillMaxWidth(),enabled=cart.isNotEmpty()){Icon(Icons.Default.Payment,contentDescription=null);Spacer(Modifier.width(8.dp));Text("BAYAR") }
    }
}

@Composable
private fun TransactionSuccessScreen(transaction: com.pentolrebus.kasir.domain.Transaction?, printer: BluetoothPrinter, onDone: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.CheckCircle, contentDescription=null, modifier=Modifier.width(72.dp).height(72.dp), tint=MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(14.dp))
        Text("Transaksi Berhasil", style=MaterialTheme.typography.headlineSmall, fontWeight=FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (transaction != null) {
            Text(transaction.transactionId, style=MaterialTheme.typography.titleMedium, fontWeight=FontWeight.SemiBold)
            Text(if (transaction.syncStatus == com.pentolrebus.kasir.domain.SyncStatus.SYNCED) "Tersinkron" else "Tersimpan offline • PENDING_SYNC", color=MaterialTheme.colorScheme.primary)
            Text("Total ${money(transaction.total)}", style=MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick=onDone, modifier=Modifier.fillMaxWidth()) { Text("SELESAI") }
        OutlinedButton(onClick={
            if (transaction != null) {
                val devices=printer.pairedDevices()
                if (devices.isNotEmpty()) printer.print(transaction,devices.first().address)
            }
        }, modifier=Modifier.fillMaxWidth(), enabled=transaction!=null) { Icon(Icons.Default.Print,null); Spacer(Modifier.width(8.dp)); Text("CETAK ULANG") }
    }
}

@Composable
private fun TransactionScreen(transactions: List<com.pentolrebus.kasir.domain.Transaction>) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Riwayat Transaksi", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        items(transactions) { transaction ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(money(transaction.total), fontWeight = FontWeight.Bold)
                    Text("${transaction.paymentMethod} • ${transaction.paymentStatus}")
                }
            }
        }
    }
}

@Composable
private fun ProductScreen(products: List<Product>) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Produk", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        items(products) { ProductRow(it, onAdd = {}) }
    }
}

@Composable
private fun ReportsScreen(
    transactions: List<com.pentolrebus.kasir.domain.Transaction>,
    shift: com.pentolrebus.kasir.domain.Shift?,
    printer: BluetoothPrinter,
    onOpenPrinter: () -> Unit,
    onSync: () -> Unit,
    syncing: Boolean
) {
    var selected by remember { mutableStateOf<com.pentolrebus.kasir.domain.Transaction?>(null) }
    val cash = transactions.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.total }
    val qris = transactions.filter { it.paymentMethod == PaymentMethod.QRIS }.sumOf { it.total }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Laporan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Riwayat transaksi & cetak ulang struk", style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onOpenPrinter) { Icon(Icons.Default.Print, "Printer") }
            }
        }
        item { StatCard("Total", money(transactions.sumOf { it.total })) }
        item { StatCard("Cash", money(cash)) }
        item { StatCard("QRIS", money(qris)) }
        item { StatCard("Transaksi", transactions.size.toString()) }
        item { StatCard("Shift", if (shift == null) "Tidak aktif" else "Aktif") }
        item { Text("Riwayat Transaksi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(transactions.sortedByDescending { it.createdAt }) { transaction ->
            OutlinedCard(onClick = { selected = transaction }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(money(transaction.total), fontWeight = FontWeight.Bold)
                        Text("${transaction.paymentMethod} • ${transaction.paymentStatus}", style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.ChevronRight, "Detail")
                }
            }
        }
    }
    selected?.let { tx ->
        TransactionDetailDialog(tx, printer, { selected = null })
    }
}

@Composable
private fun TransactionDetailDialog(
    transaction: com.pentolrebus.kasir.domain.Transaction,
    printer: BluetoothPrinter,
    onDismiss: () -> Unit
) {
    val devices = remember { printer.pairedDevices() }
    var showDevicePicker by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detail Transaksi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("ID: ${transaction.transactionId}", style = MaterialTheme.typography.labelSmall)
                transaction.items.forEach { Text("${it.quantity} x ${it.name} — ${money(it.subtotal)}") }
                HorizontalDivider()
                Text("Total: ${money(transaction.total)}", fontWeight = FontWeight.Bold)
                Text("Pembayaran: ${transaction.paymentMethod}")
                message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            }
        },
        confirmButton = {
            Button(onClick = { showDevicePicker = true }, enabled = devices.isNotEmpty()) {
                Icon(Icons.Default.Print, null); Spacer(Modifier.width(6.dp)); Text("CETAK ULANG")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("TUTUP") } }
    )
    if (showDevicePicker) {
        AlertDialog(
            onDismissRequest = { showDevicePicker = false },
            title = { Text("Pilih Printer") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (devices.isEmpty()) Text("Belum ada printer Bluetooth yang dipasangkan.")
                devices.forEach { device ->
                    TextButton(onClick = {
                        showDevicePicker = false
                        printer.print(transaction, device.address)
                            .onSuccess { message = "Struk berhasil dikirim ke ${device.name}." }
                            .onFailure { message = "Gagal mencetak: ${it.message ?: "error"}" }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth()) { Text(device.name); Text(device.address, style = MaterialTheme.typography.labelSmall) }
                    }
                }
            } },
            confirmButton = { TextButton(onClick = { showDevicePicker = false }) { Text("BATAL") } }
        )
    }
}

@Composable
private fun PrinterPickerDialog(printer: BluetoothPrinter, onDismiss: () -> Unit) {
    val devices = remember { printer.pairedDevices() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Printer Bluetooth") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Pasangkan printer dari Pengaturan Bluetooth Android terlebih dahulu.", style = MaterialTheme.typography.bodySmall)
                if (devices.isEmpty()) Text("Belum ada printer yang dipasangkan.")
                devices.forEach { device ->
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) { Text(device.name, fontWeight = FontWeight.SemiBold); Text(device.address, style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("SELESAI") } }
    )
}

@Composable
private fun SettingsScreen(
    session: Session,
    diagnostics: Diagnostics,
    onTheme: () -> Unit,
    onLogout: () -> Unit,
    detail: String?,
    onOpenDetail: (String) -> Unit,
    onBackDetail: () -> Unit,
    printer: BluetoothPrinter,
    onOpenPrinter: () -> Unit,
    onSync: () -> Unit,
    syncing: Boolean
) {
    if (detail != null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackDetail) { Icon(Icons.Default.ArrowBack, "Kembali") }
                Text(detail, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            SettingsDetail(detail, session)
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Pengaturan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(if (session.role == Role.OWNER) "Kelola bisnis, outlet, produk, dan perangkat" else "Kelola akun, shift, pembayaran, dan perangkat", style = MaterialTheme.typography.bodySmall) }
        item { Text("OPERASIONAL", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
        if (session.role == Role.OWNER) {
            item { SettingsRow("Produk", "Kelola produk dan foto", Icons.Default.Inventory2) { onOpenDetail("Produk") } }
            item { SettingsRow("Kategori", "Buat kategori sendiri", Icons.Default.Category) { onOpenDetail("Kategori") } }
            item { SettingsRow("Stok", "Aktif / nonaktif per produk", Icons.Default.Warehouse) { onOpenDetail("Stok") } }
            item { SettingsRow("Outlet", "Kelola cabang bisnis", Icons.Default.Store) { onOpenDetail("Outlet") } }
            item { SettingsRow("Kasir / Pekerja", "Kelola akun pekerja outlet", Icons.Default.People) { onOpenDetail("Kasir / Pekerja") } }
            item { SettingsRow("Owner", "Profil pemilik", Icons.Default.Person) { onOpenDetail("Owner") } }
            item { SettingsRow("Bisnis", "Informasi bisnis", Icons.Default.Business) { onOpenDetail("Bisnis") } }
        } else {
            item { SettingsRow("Profil", session.username, Icons.Default.Person) { onOpenDetail("Profil") } }
            item { SettingsRow("Shift", "Status dan penutupan shift", Icons.Default.Schedule) { onOpenDetail("Shift") } }
        }
        item { Text("PEMBAYARAN & PERANGKAT", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
        item { SettingsRow("QRIS", "Atur pembayaran QRIS", Icons.Default.QrCode2) { onOpenDetail("QRIS") } }
        item { SettingsRow("Printer", "Printer Bluetooth", Icons.Default.Print, onOpenPrinter) }
        item { Text("APLIKASI", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
        item { SettingsRow("Tema", "Terang / Gelap", Icons.Default.Brightness4, onTheme) }
        item { SettingsRow("Sinkronisasi", if (syncing) "Menyinkronkan transaksi offline…" else "Kirim data PENDING_SYNC tanpa duplikasi", Icons.Default.Sync, onSync) }
        item { OutlinedButton({ diagnostics.exportToDownloads() }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(7.dp)); Text("Download Log") } }
        item { OutlinedButton(onLogout, Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Icon(Icons.Default.Logout, null); Spacer(Modifier.width(7.dp)); Text("Keluar") } }
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsDetail(page: String, session: Session) {
    val description = when (page) {
        "Produk" -> "Kelola nama, kategori, harga, foto, status aktif, dan status stok produk."
        "Kategori" -> "Kategori dibuat sendiri oleh Owner dan digunakan sebagai filter di Kasir."
        "Stok" -> "Stok dapat diaktifkan atau dinonaktifkan per produk."
        "Outlet" -> "Kelola outlet/cabang yang berada di bawah Business."
        "Kasir / Pekerja" -> "Kelola pekerja dan akses kasir pada outlet."
        "QRIS" -> "Atur informasi pembayaran QRIS untuk outlet."
        "Printer" -> "Pasangkan dan uji printer Bluetooth."
        "Shift" -> "Mulai dan tutup shift melalui halaman Kasir."
        "Bisnis" -> "Business ID: ${session.businessId ?: "Belum diatur"}"
        else -> "Pengaturan ${page.lowercase()} untuk SakuKasir."
    }
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(page, fontWeight = FontWeight.Bold); Text(description, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun MoneyDialog(title: String, label: String, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it.filter(Char::isDigit) },
                label = { Text(label) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = { Button(onClick = { onConfirm(value.toLongOrNull() ?: 0L) }) { Text("SIMPAN") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("BATAL") } }
    )
}
