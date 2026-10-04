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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyRow
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
import com.pentolrebus.kasir.domain.Category
import com.pentolrebus.kasir.domain.Business
import com.pentolrebus.kasir.domain.Outlet
import com.pentolrebus.kasir.domain.Worker
import com.pentolrebus.kasir.domain.Expense
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShell(
    vm: PosViewModel,
    session: Session,
    dark: Boolean,
    onTheme: () -> Unit,
    diagnostics: Diagnostics
) {
    var screen by remember { mutableStateOf("kasir") }
    var page by remember { mutableStateOf<String?>(null) }
    var showStartShift by remember { mutableStateOf(false) }
    var showCloseShift by remember { mutableStateOf(false) }
    var showPrinterPicker by remember { mutableStateOf(false) }
    var showTransaction by remember { mutableStateOf<com.pentolrebus.kasir.domain.Transaction?>(null) }
    val products by vm.products.collectAsState()
    val categories by vm.categories.collectAsState()
    val outlets by vm.outlets.collectAsState()
    val workers by vm.workers.collectAsState()
    val expenses by vm.expenses.collectAsState()
    val business by vm.business.collectAsState()
    val cart by vm.cart.collectAsState()
    val shift by vm.shift.collectAsState()
    val transactions by vm.transactions.collectAsState()
    val lastTransaction by vm.lastTransaction.collectAsState()
    val syncing by vm.syncing.collectAsState()
    val context = LocalContext.current
    val printer = remember(context) { BluetoothPrinter(context) }
    val bluetoothPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) showPrinterPicker = true }
    val openPrinter = { if (Build.VERSION.SDK_INT >= 31 && !printer.hasConnectPermission()) bluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT) else showPrinterPicker = true }
    val logout = { if (shift == null) vm.logout() else showCloseShift = true }
    val displayProducts = products

    Column(Modifier.fillMaxSize()) {
        if (page == null) {
            TopAppBar(
                title = {
                    Column {
                        Text(if (screen == "kasir") (session.outletId ?: "Outlet Alun-Alun") else screenTitle(screen), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (session.role == Role.OWNER) "Owner · ${session.username}" else "${session.username} · Kasir", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onTheme) { Icon(painterResource(if (dark) com.pentolrebus.kasir.R.drawable.ic_theme else com.pentolrebus.kasir.R.drawable.ic_theme), if (dark) "Light mode" else "Dark mode") }
                    IconButton(onClick = { diagnostics.exportToDownloads() }) { Icon(Icons.Default.Download, "Download log") }
                }
            )
            if (screen == "kasir") ShiftHeader(shift, onStart = { showStartShift = true }, onClose = { showCloseShift = true })
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (page != null) {
                ManagementPage(
                    page = page!!,
                    session = session,
                    products = displayProducts,
                    categories = categories,
                    outlets = outlets,
                    workers = workers,
                    expenses = expenses,
                    business = business,
                    transactions = transactions,
                    shift = shift,
                    syncing = syncing,
                    vm = vm,
                    onBack = { page = null },
                    onOpen = { page = it },
                    onSync = { vm.syncPending() },
                    onPrinter = openPrinter,
                    onLogout = logout,
                    onTheme = onTheme
                )
            } else when (screen) {
                "kasir" -> PosScreenFull(vm, displayProducts, cart, shift) { screen = "checkout" }
                "checkout" -> CheckoutScreenFull(vm, cart, onDone = { screen = "success" })
                "success" -> TransactionSuccessScreen(lastTransaction, printer) { screen = "kasir" }
                "laporan" -> ReportsFull(
                    session = session, transactions = transactions, shift = shift,
                    onDetail = { showTransaction = it }, onOpen = { page = it },
                    onSync = { vm.syncPending() }, syncing = syncing
                )
                "pengaturan" -> OwnerSettingsPage(
                    session = session, onOpen = { page = it }, onSync = { vm.syncPending() },
                    syncing = syncing, onPrinter = openPrinter, onTheme = onTheme, onLogout = logout
                )
                else -> Unit
            }
        }
        if (page == null) BottomNavModern(screen) { screen = it }
    }
    if (showStartShift) MoneyDialog("Mulai Shift", "Modal awal", { showStartShift = false }) { vm.startShift(it); showStartShift = false }
    if (showCloseShift) CloseShiftDialog(shift, { showCloseShift = false }) { vm.closeShift(it); showCloseShift = false }
    if (showPrinterPicker) PrinterPickerDialog(printer) { showPrinterPicker = false }
    showTransaction?.let { TransactionDetailDialog(it, printer) { showTransaction = null } }
}

private fun screenTitle(screen: String) = when(screen) { "checkout" -> "Checkout"; "laporan" -> "Laporan"; else -> "Pengaturan" }

@Composable
private fun ShiftHeader(shift: com.pentolrebus.kasir.domain.Shift?, onStart: () -> Unit, onClose: () -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(18.dp), color = if (shift == null) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(10.dp).height(10.dp).border(2.dp, if (shift == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary, RoundedCornerShape(50)))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(if (shift == null) "Belum ada Shift" else "Shift Aktif", fontWeight = FontWeight.SemiBold)
                Text(if (shift == null) "Mulai shift untuk menerima transaksi" else "Shift berjalan · ketuk untuk menutup", style = MaterialTheme.typography.labelSmall)
            }
            if (shift == null) Button(onClick = onStart, contentPadding = ButtonDefaults.ContentPadding) { Text("MULAI SHIFT") }
            else OutlinedButton(onClick = onClose, contentPadding = ButtonDefaults.ContentPadding) { Text("TUTUP") }
        }
    }
}

@Composable
private fun BottomNavModern(screen: String, onNavigate: (String) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        val items = listOf("kasir" to "Kasir", "checkout" to "Checkout", "laporan" to "Laporan", "pengaturan" to "Pengaturan")
        val icons = listOf(R.drawable.ic_cart, R.drawable.ic_grid, R.drawable.ic_report, R.drawable.ic_more)
        items.forEachIndexed { i, (route, label) ->
            val selected = screen == route
            NavigationBarItemCompat(label, painterResource(icons[i]), selected) { onNavigate(route) }
        }
    }
}

@Composable
private fun RowScope.NavigationBarItemCompat(label: String, icon: Painter, selected: Boolean, onClick: () -> Unit) {
    val c = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.weight(1f).clickable(onClick = onClick).padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, label, tint = c, modifier = Modifier.height(22.dp))
        Text(label, color = c, style = MaterialTheme.typography.labelSmall, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun PosScreenFull(vm: PosViewModel, products: List<Product>, cart: List<com.pentolrebus.kasir.domain.CartItem>, shift: com.pentolrebus.kasir.domain.Shift?, onCheckout: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Semua") }
    val categories = listOf("Semua", "Pentol", "Es Teh", "Tambahan")
    val filtered = products.filter { it.active && (query.isBlank() || it.name.contains(query, true)) && (category == "Semua" || it.name.contains(category.replace("Es Teh", "Es Teh"), true) || category == "Tambahan" && it.name.contains("Sambal", true)) }
    val shown = if (filtered.isEmpty() && query.isBlank()) products else filtered
    if (shift == null) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(24.dp)); Icon(Icons.Default.Schedule, null, modifier = Modifier.height(54.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp)); Text("Belum ada Shift", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Mulai shift untuk menerima transaksi", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp)); Text("Produk terkunci sampai shift dimulai", style = MaterialTheme.typography.labelMedium)
        }
        return
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Cari produk…") }, leadingIcon = { Icon(Icons.Default.Search, null) })
        LazyRow(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(categories) { c -> FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) }) } }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Produk", fontWeight = FontWeight.Bold); Text("${cart.sumOf { it.quantity }} item", style = MaterialTheme.typography.labelMedium) }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp)) {
            items(shown) { ProductCardFull(it) { vm.add(it) } }
        }
        val total = cart.sumOf { it.product.price * it.quantity }
        Surface(Modifier.fillMaxWidth().padding(vertical = 8.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primary) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("${cart.sumOf { it.quantity }} item", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelSmall); Text(money(total), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) }
                Button(onClick = onCheckout, enabled = cart.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimary, contentColor = MaterialTheme.colorScheme.primary)) { Text("CHECKOUT") }
            }
        }
    }
}

@Composable
private fun ProductCardFull(product: Product, onAdd: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.width(54.dp).height(54.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) { Box(contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.ic_product), null, tint = MaterialTheme.colorScheme.primary) } }
            Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(product.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(money(product.price), color = MaterialTheme.colorScheme.onSurfaceVariant); if (product.stockEnabled) Text("Stok ${product.stock}", style = MaterialTheme.typography.labelSmall, color = if (product.stock <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) }
            IconButton(onClick = onAdd) { Icon(Icons.Default.Add, "Tambah") }
        }
    }
}

@Composable
private fun CheckoutScreenFull(vm: PosViewModel, cart: List<com.pentolrebus.kasir.domain.CartItem>, onDone: () -> Unit) {
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var discount by remember { mutableStateOf("0") }
    var received by remember { mutableStateOf("") }
    var qrisProofPath by remember { mutableStateOf<String?>(null) }
    val proofPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> qrisProofPath = uri?.toString() }
    val subtotal = cart.sumOf { it.product.price * it.quantity }
    val disc = discount.toLongOrNull() ?: 0
    val total = (subtotal - disc).coerceAtLeast(0)
    val cash = received.toLongOrNull() ?: 0
    val change = (cash - total).coerceAtLeast(0)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Checkout", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            TextButton(onClick = { vm.clearCart() }, enabled = cart.isNotEmpty()) { Text("Kosongkan") }
        }
        if (cart.isEmpty()) EmptyState("Keranjang kosong", "Tambahkan produk dari layar Kasir.", R.drawable.ic_cart)
        else {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    cart.forEach { item ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) { Text(item.product.name, fontWeight = FontWeight.SemiBold); Text("${item.quantity} × ${money(item.product.price)}", style = MaterialTheme.typography.labelSmall) }
                            Text(money(item.product.price * item.quantity), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(discount, { discount = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label={Text("Diskon")}, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number), singleLine=true)
            SummaryRow("Subtotal", money(subtotal)); SummaryRow("Diskon", money(disc)); SummaryRow("Total", money(total), true)
            Spacer(Modifier.height(12.dp)); Text("Metode pembayaran", fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { FilterChip(selected = method == PaymentMethod.CASH, onClick = { method = PaymentMethod.CASH }, label = { Text("CASH") }); FilterChip(selected = method == PaymentMethod.QRIS, onClick = { method = PaymentMethod.QRIS }, label = { Text("QRIS") }) }
            if (method == PaymentMethod.CASH) {
                Spacer(Modifier.height(8.dp)); OutlinedTextField(received, { received = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label={Text("Uang diterima")}, prefix={Text("Rp ")}, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number), singleLine=true)
                SummaryRow("Kembalian", money(change), true)
            } else { Spacer(Modifier.height(8.dp)); QrisPreview(); OutlinedButton(onClick = { proofPicker.launch("image/*") }, Modifier.fillMaxWidth()) { Icon(Icons.Default.PhotoCamera, null); Spacer(Modifier.width(6.dp)); Text(if (qrisProofPath == null) "LAMPIRKAN BUKTI QRIS" else "BUKTI QRIS TERPILIH") } }
            Spacer(Modifier.height(14.dp))
            Button(onClick = { vm.checkout(method, if(method==PaymentMethod.CASH) cash else 0, qrisProofPath, disc); onDone() }, Modifier.fillMaxWidth(), enabled = cart.isNotEmpty() && (method == PaymentMethod.QRIS || cash >= total)) { Text("LANJUT BAYAR") }
        }
    }
}

@Composable private fun SummaryRow(label: String, value: String, total: Boolean = false) { Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, fontWeight = if(total) FontWeight.Bold else FontWeight.Normal); Text(value, fontWeight = if(total) FontWeight.Bold else FontWeight.Medium, color = if(total) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) } }
@Composable private fun QrisPreview() { Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(painterResource(R.drawable.ic_qris), null, modifier = Modifier.height(34.dp), tint = MaterialTheme.colorScheme.primary); Text("QRIS", fontWeight = FontWeight.Bold); Text("Bukti pembayaran dapat dilampirkan setelah transaksi.", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center) } } }

@Composable
private fun ReportsFull(session: Session, transactions: List<com.pentolrebus.kasir.domain.Transaction>, shift: com.pentolrebus.kasir.domain.Shift?, onDetail: (com.pentolrebus.kasir.domain.Transaction) -> Unit, onOpen: (String) -> Unit, onSync: () -> Unit, syncing: Boolean) {
    var tab by remember { mutableStateOf("Ringkasan") }
    var query by remember { mutableStateOf("") }
    val total = transactions.sumOf { it.total }
    val cash = transactions.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.total }
    val qris = transactions.filter { it.paymentMethod == PaymentMethod.QRIS }.sumOf { it.total }
    val filtered = transactions.filter { query.isBlank() || it.transactionId.contains(query, true) || it.items.any { item -> item.name.contains(query, true) } }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(if (session.role == Role.OWNER) "Ringkasan Bisnis" else "Laporan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(if (session.role == Role.OWNER) "Owner · semua outlet" else "Kasir · ${session.outletId ?: "Outlet"}", style = MaterialTheme.typography.labelMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 12.dp)) {
            items(listOf("Ringkasan", "Outlet", "Kasir", "Harian", "Bulanan")) { label -> FilterChip(selected = tab == label, onClick = { tab = label }, label = { Text(label) }) }
        }
        when (tab) {
            "Ringkasan" -> {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primary) { Column(Modifier.padding(16.dp)) { Text("Penjualan hari ini", color = MaterialTheme.colorScheme.onPrimary); Text(money(total), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) } }
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatCardCompact("Cash", money(cash), Modifier.weight(1f)); StatCardCompact("QRIS", money(qris), Modifier.weight(1f)) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatCardCompact("Transaksi", transactions.size.toString(), Modifier.weight(1f)); StatCardCompact("Shift", if (shift == null) "0" else "1", Modifier.weight(1f)) }
            }
            "Outlet" -> { SummaryRow("Outlet aktif", session.outletId ?: "-"); SummaryRow("Penjualan", money(total)); SummaryRow("Transaksi", transactions.size.toString()) }
            "Kasir" -> { val grouped = transactions.groupBy { it.cashierUid }; grouped.forEach { (uid, list) -> SummaryRow(uid.takeLast(8), "${list.size} transaksi · ${money(list.sumOf { it.total })}") } }
            "Harian" -> { SummaryRow("Hari ini", money(total)); SummaryRow("Cash", money(cash)); SummaryRow("QRIS", money(qris)) }
            "Bulanan" -> { SummaryRow("Bulan berjalan", money(total)); SummaryRow("Transaksi", transactions.size.toString()); SummaryRow("Rata-rata", money(if (transactions.isEmpty()) 0 else total / transactions.size)) }
        }
        Spacer(Modifier.height(12.dp)); Text("Riwayat transaksi", fontWeight = FontWeight.Bold)
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(vertical = 8.dp), singleLine = true, placeholder = { Text("Cari ID / produk…") }, leadingIcon = { Icon(Icons.Default.Search, null) })
        if (filtered.isEmpty()) EmptyState("Belum ada transaksi", "Transaksi yang selesai akan muncul di sini.", R.drawable.ic_report) else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { items(filtered.take(30)) { t -> TransactionRow(t) { onDetail(t) } } }
        if (session.role == Role.OWNER) { Text("Laporan owner", fontWeight = FontWeight.Bold); listOf("Laporan Owner — Kasir", "Laporan Owner — Harian", "Laporan Owner — Bulanan", "Laporan Keuangan — Hari ini", "Laporan Keuangan — Bulan ini", "Pengeluaran — daftar").forEach { SettingsRowPainter(it, "Buka laporan", pageIcon(it), { onOpen(it) }) } }
        OutlinedButton(onClick = onSync, Modifier.fillMaxWidth()) { Icon(painterResource(R.drawable.ic_sync), null); Spacer(Modifier.width(8.dp)); Text(if (syncing) "Menyinkronkan…" else "Sinkronkan transaksi") }
    }
}

@Composable private fun StatCardCompact(title:String,value:String,modifier:Modifier=Modifier){ OutlinedCard(modifier, shape=RoundedCornerShape(14.dp)){Column(Modifier.padding(12.dp)){Text(title,style=MaterialTheme.typography.labelSmall);Text(value,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)}}}
@Composable
private fun TransactionRow(
    t: com.pentolrebus.kasir.domain.Transaction,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("#${t.transactionId.takeLast(6)}", fontWeight = FontWeight.SemiBold)
                Text(
                    "${t.paymentMethod} · ${t.items.sumOf { item -> item.quantity }} item",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Text(money(t.total), fontWeight = FontWeight.Bold)
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable

private fun ManagementPage(page:String, vm:PosViewModel, session:Session, products:List<Product>, categories:List<com.pentolrebus.kasir.domain.Category>, outlets:List<com.pentolrebus.kasir.domain.Outlet>, workers:List<com.pentolrebus.kasir.domain.Worker>, expenses:List<com.pentolrebus.kasir.domain.Expense>, business:com.pentolrebus.kasir.domain.Business?, transactions:List<com.pentolrebus.kasir.domain.Transaction>, shift:com.pentolrebus.kasir.domain.Shift?, syncing:Boolean, onBack:()->Unit, onOpen:(String)->Unit, onSync:()->Unit, onPrinter:()->Unit, onLogout:()->Unit, onTheme:()->Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,"Kembali")};Column{Text(page,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text(pageSubtitle(page),style=MaterialTheme.typography.labelSmall)}}
        when(page){
            "Pengaturan","Pengaturan Owner","Pengaturan Kasir" -> OwnerSettingsPage(session,onOpen,onSync,syncing,onPrinter,onTheme,onLogout)
            "Produk","Management Produk","Detail Produk","Form Produk (1/2 Info)","Form Produk (2/2 Stok ON)","Form Produk (2/2 Stok OFF)" -> ProductManagementReal(vm,session,products,categories,page)
            "Kategori","Kategori — daftar","Kategori — tambah / ubah","Kategori — kosong" -> CategoryManagementReal(vm,session,categories,products,page)
            "Stok","Detail Stok" -> StockReal(vm,products,categories,page)
            "Outlet","Management Outlet","Detail Outlet","Form Outlet","Outlet Kampus" -> OutletReal(vm,session,outlets,page)
            "Kasir / Pekerja","Management Kasir/Pekerja","Kasir / Pekerja — daftar","Detail Pekerja","Ubah Pekerja" -> WorkerReal(vm,session,workers,outlets,page)
            "Owner","Profil","Profil Owner" -> ProfileReal(session,business)
            "Shift","Detail Shift" -> ShiftDetailReal(shift)
            "Detail Transaksi" -> TransactionDetailReal(transactions.firstOrNull())
            "Bisnis","Ubah Bisnis" -> BusinessReal(vm,session,business,outlets,page)
            "QRIS","Bukti QRIS — foto tersedia","Bukti QRIS — belum ada foto" -> QrisReal(page)
            "Printer","Printer Bluetooth" -> PrinterPage(onPrinter)
            "Tema" -> ThemePage(onTheme)
            "Sinkronisasi" -> SyncPage(syncing,onSync)
            "Tentang aplikasi" -> AboutPage()
            "Riwayat Transaksi" -> TransactionHistoryReal(transactions)
            "Riwayat Shift" -> ShiftHistoryPage(shift)
            "Dashboard Owner","Laporan Owner — Kasir","Laporan Owner — Harian","Laporan Owner — Bulanan","Laporan Keuangan — Hari ini","Laporan Keuangan — Bulan ini" -> OwnerReportReal(page,transactions,expenses,workers,outlets)
            "Pengeluaran — daftar","Pengeluaran — tambah / ubah","Detail Pengeluaran" -> ExpenseReal(vm,session,expenses,page)
            "Pilih Peran","Daftar Owner","Login Username/PIN","Login Email/Password" -> AuthPreviewPage(page)
            else -> GenericMockPage(page)
        }
    }
}

private fun pageSubtitle(page:String)=when(page){"Produk","Kategori","Stok"->"Kelola data operasional";"QRIS","Printer"->"Pembayaran & perangkat";"Tema","Sinkronisasi"->"Preferensi aplikasi";else->"SakuKasir"}
private fun pageIcon(page:String)=when{page.contains("Produk")->R.drawable.ic_product;page.contains("Kategori")->R.drawable.ic_category;page.contains("Stok")->R.drawable.ic_stock;page.contains("Outlet")->R.drawable.ic_store;page.contains("Pekerja")||page.contains("Kasir")->R.drawable.ic_people;page.contains("QRIS")->R.drawable.ic_qris;page.contains("Printer")->R.drawable.ic_printer;page.contains("Sinkron")->R.drawable.ic_sync;page.contains("Bisnis")->R.drawable.ic_business;page.contains("Owner")||page.contains("Profil")->R.drawable.ic_owner;else->R.drawable.ic_more}

private fun settingsSubtitle(page: String): String = when (page) {
    "Produk" -> "Kelola daftar produk dan harga"
    "Kategori" -> "Kelola kategori produk"
    "Stok" -> "Atur stok dan batas minimum"
    "Outlet" -> "Kelola outlet bisnis"
    "Kasir / Pekerja" -> "Kelola akun dan akses kasir"
    "Owner" -> "Profil pemilik usaha"
    "Bisnis" -> "Informasi dan identitas bisnis"
    "Profil" -> "Profil akun kasir"
    "Shift" -> "Shift aktif dan riwayat shift"
    else -> "Kelola pengaturan"
}

@Composable private fun OwnerSettingsPage(session:Session,onOpen:(String)->Unit,onSync:()->Unit,syncing:Boolean,onPrinter:()->Unit,onTheme:()->Unit,onLogout:()->Unit){LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Text(if(session.role==Role.OWNER)"Owner · Bisnis" else session.username,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};item{SectionLabel("OPERASIONAL")};if(session.role==Role.OWNER){listOf("Produk","Kategori","Stok","Outlet","Kasir / Pekerja","Owner","Bisnis").forEach{p->item{SettingsRowPainter(p,settingsSubtitle(p),pageIcon(p)){onOpen(p)}}}}else listOf("Profil","Shift").forEach{p->item{SettingsRowPainter(p,settingsSubtitle(p),pageIcon(p)){onOpen(p)}}};item{SectionLabel("PEMBAYARAN & PERANGKAT")};item{SettingsRowPainter("QRIS","Atur pembayaran QRIS",R.drawable.ic_qris){onOpen("QRIS")}};item{SettingsRowPainter("Printer","Bluetooth",R.drawable.ic_printer,onPrinter)};item{SectionLabel("APLIKASI")};item{SettingsRowPainter("Tema","Terang / Gelap",R.drawable.ic_theme,onTheme)};item{SettingsRowPainter("Sinkronisasi",if(syncing)"Menyinkronkan…" else "Tersinkron",R.drawable.ic_sync,onSync)};item{SettingsRowPainter("Tentang aplikasi","Versi 0.1",R.drawable.ic_info){onOpen("Tentang aplikasi")}};item{Spacer(Modifier.height(4.dp));Button(onClick=onLogout,Modifier.fillMaxWidth()){Text("KELUAR")}}}}
@Composable private fun SectionLabel(t:String){Text(t,style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)}
@Composable private fun SettingsRowPainter(title:String,subtitle:String,icon:Int,onClick:()->Unit){Surface(onClick=onClick,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface,tonalElevation=1.dp){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){Surface(Modifier.width(38.dp).height(38.dp),shape=RoundedCornerShape(11.dp),color=MaterialTheme.colorScheme.primaryContainer){Box(contentAlignment=Alignment.Center){Icon(painterResource(icon),null,tint=MaterialTheme.colorScheme.primary)}};Spacer(Modifier.width(11.dp));Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.SemiBold);Text(subtitle,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};Icon(Icons.Default.ChevronRight,null,tint=MaterialTheme.colorScheme.onSurfaceVariant)}}}

@Composable private fun ProductManagementReal(vm:PosViewModel,session:Session,products:List<Product>,categories:List<Category>,page:String){var q by remember{mutableStateOf("")};var cat by remember{mutableStateOf<String?>(null)};var editing by remember{mutableStateOf<Product?>(null)};val outlet=session.outletId.orEmpty();val shown=products.filter{it.active&&(q.isBlank()||it.name.contains(q,true))&&(cat==null||it.categoryId==cat)};Column(Modifier.fillMaxSize().padding(16.dp)){OutlinedTextField(q,{q=it},Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("Cari produk…")},leadingIcon={Icon(Icons.Default.Search,null)});LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.padding(vertical=10.dp)){item{FilterChip(selected=cat==null,onClick={cat=null},label={Text("Semua")})};items(categories){c->FilterChip(selected=cat==c.id,onClick={cat=c.id},label={Text(c.name)})}};if(shown.isEmpty())EmptyState("Belum ada produk","Buat produk untuk mulai berjualan.",R.drawable.ic_product)else LazyVerticalGrid(columns=GridCells.Fixed(2),modifier=Modifier.weight(1f),contentPadding=PaddingValues(bottom=80.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){gridItems(shown){p->ProductGridCard(p){editing=p}}};FloatingActionButton(onClick={editing=Product(outletId=outlet,ownerUid=session.uid)},modifier=Modifier.align(Alignment.End)){Icon(Icons.Default.Add,"Tambah produk")}};editing?.let{ProductEditor(it,categories,onDismiss={editing=null}){vm.saveProduct(it);editing=null}}}
@Composable private fun ProductGridCard(p:Product,onClick:()->Unit){OutlinedCard(onClick=onClick,shape=RoundedCornerShape(14.dp)){Column{Surface(Modifier.fillMaxWidth().height(100.dp),color=MaterialTheme.colorScheme.primaryContainer){Box(contentAlignment=Alignment.Center){Icon(painterResource(R.drawable.ic_product),null,Modifier.height(38.dp),tint=MaterialTheme.colorScheme.primary)}};Column(Modifier.padding(10.dp)){Text(p.name,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(money(p.price),color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold);Text(if(p.stockEnabled)"Stok ${p.stock}" else "Stok OFF",style=MaterialTheme.typography.labelSmall,color=if(p.stockEnabled&&p.stock<=p.lowStockThreshold)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)}}}}
@Composable
private fun ProductEditor(initial: Product, categories: List<Category>, onDismiss: () -> Unit, onSave: (Product) -> Unit) {
    var step by remember { mutableStateOf(1) }
    var name by remember { mutableStateOf(initial.name) }
    var price by remember { mutableStateOf(if (initial.price == 0L) "" else initial.price.toString()) }
    var unit by remember { mutableStateOf(initial.unit) }
    var category by remember { mutableStateOf(initial.categoryId) }
    var stockOn by remember { mutableStateOf(initial.stockEnabled) }
    var stock by remember { mutableStateOf(initial.stock.toString()) }
    var low by remember { mutableStateOf(initial.lowStockThreshold.toString()) }
    var imagePath by remember { mutableStateOf(initial.imagePath.orEmpty()) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> imagePath = uri?.toString().orEmpty() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (step == 1) "Tambah / Ubah Produk · Info" else "Tambah / Ubah Produk · Stok") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (step == 1) {
                    OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nama produk") })
                    OutlinedTextField(price, { price = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Harga") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(unit, { unit = it }, Modifier.fillMaxWidth(), label = { Text("Satuan") })
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { c -> FilterChip(selected = category == c.id, onClick = { category = c.id }, label = { Text(c.name) }) }
                    }
                    Button(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Photo, null); Spacer(Modifier.width(6.dp)); Text(if (imagePath.isBlank()) "PILIH FOTO PRODUK" else "FOTO TERPILIH") }
                    if (imagePath.isNotBlank()) Text(imagePath.substringAfterLast("/").takeLast(32), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Kelola stok", Modifier.weight(1f)); Switch(stockOn, { stockOn = it }) }
                    if (stockOn) {
                        OutlinedTextField(stock, { stock = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Stok awal") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(low, { low = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Batas stok menipis") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    } else {
                        Text("Stok OFF · produk tetap dapat dijual.")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (step == 1) {
                    step = 2
                } else {
                    onSave(initial.copy(name = name, price = price.toLongOrNull() ?: 0, unit = unit, categoryId = category, stockEnabled = stockOn, stock = stock.toLongOrNull() ?: 0, lowStockThreshold = low.toLongOrNull() ?: 5, imagePath = imagePath.ifBlank { initial.imagePath.orEmpty() }))
                }
            }) { Text(if (step == 1) "LANJUT" else "SIMPAN") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("BATAL") } }
    )
}

@Composable private fun CategoryManagementReal(vm:PosViewModel,session:Session,categories:List<Category>,products:List<Product>,page:String){var editing by remember{mutableStateOf<Category?>(null)};Column(Modifier.fillMaxSize().padding(16.dp)){if(categories.isEmpty())EmptyState("Belum ada kategori","Buat kategori sesuai produk Anda.",R.drawable.ic_category)else LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){items(categories){c->SettingsRowPainter(c.name,"${products.count{it.categoryId==c.id}} produk",R.drawable.ic_category){editing=c}}};Button(onClick={editing=Category(ownerUid=session.uid,outletId=session.outletId.orEmpty())},Modifier.fillMaxWidth()){Icon(Icons.Default.Add,null);Spacer(Modifier.width(6.dp));Text("BUAT KATEGORI")}};editing?.let{CategoryEditor(it,onDismiss={editing=null}){vm.saveCategory(it);editing=null}}}
@Composable private fun CategoryEditor(initial:Category,onDismiss:()->Unit,onSave:(Category)->Unit){var name by remember{mutableStateOf(initial.name)};AlertDialog(onDismissRequest=onDismiss,title={Text(if(initial.name.isBlank())"Tambah Kategori" else "Ubah Kategori")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(name,{name=it},Modifier.fillMaxWidth(),label={Text("Nama kategori")});Text("Dipakai sebagai filter di layar Kasir.",style=MaterialTheme.typography.labelSmall)}},confirmButton={Button(onClick={if(name.isNotBlank())onSave(initial.copy(name=name.trim()))}){Text("SIMPAN")}},dismissButton={TextButton(onClick=onDismiss){Text("BATAL")}})}

@Composable private fun StockReal(vm:PosViewModel,products:List<Product>,categories:List<Category>,page:String){var selected by remember{mutableStateOf<Product?>(null)};Column(Modifier.fillMaxSize().padding(16.dp)){Text("Stok",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text("Atur jumlah dan batas stok setiap produk.",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(10.dp));LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(products){p->SettingsRowPainter(p.name,if(p.stockEnabled)"${p.stock} ${p.unit} · minimum ${p.lowStockThreshold}" else "Stock OFF",R.drawable.ic_stock){selected=p}}}};selected?.let{p->StockEditor(p,onDismiss={selected=null}){vm.saveProduct(it);selected=null}}}
@Composable
private fun StockEditor(p: Product, onDismiss: () -> Unit, onSave: (Product) -> Unit) {
    var enabled by remember { mutableStateOf(p.stockEnabled) }
    var qty by remember { mutableStateOf(p.stock.toString()) }
    var low by remember { mutableStateOf(p.lowStockThreshold.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detail Stok") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(p.name, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Stock ON", Modifier.weight(1f)); Switch(enabled, { enabled = it }) }
                if (enabled) {
                    OutlinedTextField(qty, { qty = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Jumlah") })
                    OutlinedTextField(low, { low = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Batas menipis") })
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(p.copy(stockEnabled = enabled, stock = qty.toLongOrNull() ?: 0, lowStockThreshold = low.toLongOrNull() ?: 5, imagePath = p.imagePath)) }) { Text("SIMPAN") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("BATAL") } }
    )
}

@Composable
private fun OutletReal(vm: PosViewModel, session: Session, outlets: List<Outlet>, page: String) {
    var editing by remember { mutableStateOf<Outlet?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(outlets) { o -> SettingsRowPainter(o.name, o.address ?: "Outlet aktif", R.drawable.ic_store) { editing = o } }
        }
        Button(onClick = { editing = Outlet(ownerUid = session.uid, businessId = session.businessId) }, Modifier.fillMaxWidth()) { Text("TAMBAH OUTLET") }
    }
    editing?.let { o -> OutletEditor(o, onDismiss = { editing = null }) { vm.saveOutlet(it); editing = null } }
}

@Composable
private fun OutletEditor(initial: Outlet, onDismiss: () -> Unit, onSave: (Outlet) -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var address by remember { mutableStateOf(initial.address.orEmpty()) }
    var phone by remember { mutableStateOf(initial.phone.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.name.isBlank()) "Tambah Outlet" else "Ubah Outlet") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nama outlet") })
            OutlinedTextField(address, { address = it }, Modifier.fillMaxWidth(), label = { Text("Alamat") })
            OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("Telepon") })
        } },
        confirmButton = { Button(onClick = { if (name.isNotBlank()) onSave(initial.copy(name = name, address = address, phone = phone)) }) { Text("SIMPAN") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("BATAL") } }
    )
}

@Composable
private fun WorkerReal(vm: PosViewModel, session: Session, workers: List<Worker>, outlets: List<Outlet>, page: String) {
    var editing by remember { mutableStateOf<Worker?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(workers) { w ->
                val outletName = outlets.firstOrNull { it.id == w.outletId }?.name ?: "Outlet"
                SettingsRowPainter(w.displayName, "${w.role.name} · $outletName · ${if (w.active) "Aktif" else "Nonaktif"}", R.drawable.ic_people) { editing = w }
            }
        }
        Button(onClick = { editing = Worker(ownerUid = session.uid, outletId = session.outletId.orEmpty()) }, Modifier.fillMaxWidth()) { Text("TAMBAH PEKERJA") }
    }
    editing?.let { w -> WorkerEditor(w, onDismiss = { editing = null }) { vm.saveWorker(it); editing = null } }
}

@Composable
private fun WorkerEditor(initial: Worker, onDismiss: () -> Unit, onSave: (Worker) -> Unit) {
    var name by remember { mutableStateOf(initial.displayName) }
    var username by remember { mutableStateOf(initial.username) }
    var phone by remember { mutableStateOf(initial.phone.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.displayName.isBlank()) "Tambah Pekerja" else "Ubah Pekerja") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nama") })
            OutlinedTextField(username, { username = it }, Modifier.fillMaxWidth(), label = { Text("Username") })
            OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("WhatsApp (opsional)") })
            Text("Role: Kasir", style = MaterialTheme.typography.labelMedium)
        } },
        confirmButton = { Button(onClick = { if (name.isNotBlank() && username.isNotBlank()) onSave(initial.copy(displayName = name, username = username, phone = phone)) }) { Text("SIMPAN") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("BATAL") } }
    )
}

@Composable
private fun ProfileReal(session: Session, business: Business?) {
    DetailCard(if (session.role == Role.OWNER) "Profil Owner" else "Profil Kasir", listOf("Username" to session.username, "Role" to session.role.name, "Bisnis" to (business?.name ?: "-"), "Business ID" to (session.businessId ?: "-"), "Outlet ID" to (session.outletId ?: "-")), null, null)
}

@Composable
private fun BusinessReal(vm: PosViewModel, session: Session, business: Business?, outlets: List<Outlet>, page: String) {
    var editing by remember { mutableStateOf(page.contains("Ubah")) }
    var name by remember { mutableStateOf(business?.name ?: "") }
    var whatsapp by remember { mutableStateOf(business?.whatsapp.orEmpty()) }
    var address by remember { mutableStateOf(business?.address.orEmpty()) }
    if (!editing) {
        DetailCard("Bisnis", listOf("Nama" to (business?.name ?: "-"), "Business ID" to (business?.id ?: "-"), "Outlet" to outlets.size.toString()), { editing = true }, "Ubah Bisnis")
        return
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nama bisnis") })
        OutlinedTextField(whatsapp, { whatsapp = it }, Modifier.fillMaxWidth(), label = { Text("WhatsApp") })
        OutlinedTextField(address, { address = it }, Modifier.fillMaxWidth(), label = { Text("Alamat") })
        Button(onClick = { vm.saveBusiness((business ?: Business(ownerUid = session.uid)).copy(name = name, whatsapp = whatsapp, address = address)); editing = false }, Modifier.fillMaxWidth()) { Text("SIMPAN") }
    }
}

@Composable private fun QrisReal(page:String){if(page.contains("Bukti")){EmptyState(if(page.contains("belum"))"Bukti QRIS belum tersedia" else "Bukti QRIS tersedia",if(page.contains("belum"))"Transaksi QRIS belum memiliki foto bukti." else "Foto bukti tersimpan lokal di perangkat.",R.drawable.ic_qris);return};Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Surface(Modifier.fillMaxWidth().height(230.dp),shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surfaceVariant){Box(contentAlignment=Alignment.Center){Icon(painterResource(R.drawable.ic_qris),null,Modifier.height(70.dp),tint=MaterialTheme.colorScheme.primary)}};SummaryRow("Status","Aktif");SummaryRow("Outlet","Outlet Alun-Alun");Button({},Modifier.fillMaxWidth()){Text("GANTI QRIS")};Text("Bukti pembayaran disimpan lokal di Pictures/Kasir/QRIS.",style=MaterialTheme.typography.labelSmall)}}
@Composable
private fun ShiftDetailReal(shift: com.pentolrebus.kasir.domain.Shift?) {
    if (shift == null) { EmptyState("Belum ada shift", "Mulai shift untuk melihat detailnya.", R.drawable.ic_report); return }
    DetailCard("Detail Shift", listOf("Shift ID" to shift.id.takeLast(8), "Modal awal" to money(shift.openingCash), "Cash" to money(shift.cashTotal), "QRIS" to money(shift.qrisTotal), "Transaksi" to shift.transactionCount.toString(), "Status" to if (shift.closedAt == null) "Aktif" else "Ditutup"), null, null)
}

@Composable
private fun TransactionDetailReal(transaction: com.pentolrebus.kasir.domain.Transaction?) {
    if (transaction == null) { EmptyState("Belum ada transaksi", "Detail transaksi akan muncul setelah pembayaran.", R.drawable.ic_report); return }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Detail Transaksi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("#${transaction.transactionId}", style = MaterialTheme.typography.labelSmall)
        transaction.items.forEach { item -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("${item.quantity} × ${item.name}"); Text(money(item.subtotal), fontWeight = FontWeight.SemiBold) } }
        HorizontalDivider()
        SummaryRow("Subtotal", money(transaction.subtotal))
        SummaryRow("Diskon", money(transaction.discount))
        SummaryRow("Total", money(transaction.total), true)
        SummaryRow("Metode", transaction.paymentMethod.name)
        if (transaction.paymentMethod == PaymentMethod.CASH) { SummaryRow("Uang diterima", money(transaction.cashReceived)); SummaryRow("Kembalian", money(transaction.change)) }
        SummaryRow("Status sync", transaction.syncStatus.name)
    }
}

@Composable private fun PrinterPage(onPrinter: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Printer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        SettingsRowPainter("Printer Bluetooth", "Kelola perangkat printer", R.drawable.ic_printer, onPrinter)
        Text("Hubungkan printer Bluetooth untuk mencetak dan mencetak ulang struk.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun ThemePage(onTheme: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Tema", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        SettingsRowPainter("Mode tampilan", "Terang / Gelap", R.drawable.ic_theme, onTheme)
    }
}

@Composable private fun SyncPage(syncing: Boolean, onSync: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Sinkronisasi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(if (syncing) "Sinkronisasi sedang berjalan…" else "Data siap disinkronkan.", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onSync, Modifier.fillMaxWidth()) { Text(if (syncing) "MENYINKRONKAN…" else "SINKRONKAN SEKARANG") }
    }
}

@Composable private fun AboutPage() {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Tentang aplikasi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("SakuKasir", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("Versi 0.1", style = MaterialTheme.typography.bodySmall)
        Text("Aplikasi kasir untuk pengelolaan penjualan, shift, transaksi, printer, dan sinkronisasi.", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable private fun ShiftHistoryPage(shift: com.pentolrebus.kasir.domain.Shift?) {
    if (shift == null) EmptyState("Belum ada riwayat shift", "Riwayat shift akan muncul setelah shift dibuat.", R.drawable.ic_report)
    else DetailCard("Riwayat Shift", listOf("Shift ID" to shift.id, "Mulai" to shift.startAt.toString(), "Modal awal" to money(shift.openingCash), "Transaksi" to shift.transactionCount.toString(), "Status" to shift.syncStatus.name), null, null)
}

@Composable private fun TransactionHistoryReal(transactions:List<com.pentolrebus.kasir.domain.Transaction>){var q by remember{mutableStateOf("")};var method by remember{mutableStateOf<PaymentMethod?>(null)};Column(Modifier.fillMaxSize().padding(16.dp)){OutlinedTextField(q,{q=it},Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("Cari ID / produk…")},leadingIcon={Icon(Icons.Default.Search,null)});LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.padding(vertical=8.dp)){item{FilterChip(method==null,{method=null},{Text("Semua")})};item{FilterChip(method==PaymentMethod.CASH,{method=PaymentMethod.CASH},{Text("Cash")})};item{FilterChip(method==PaymentMethod.QRIS,{method=PaymentMethod.QRIS},{Text("QRIS")})}};val list=transactions.filter{(q.isBlank()||it.transactionId.contains(q,true)||it.items.any{item->item.name.contains(q,true)})&&(method==null||it.paymentMethod==method)};if(list.isEmpty())EmptyState("Belum ada transaksi","Coba ubah filter atau lakukan transaksi.",R.drawable.ic_report)else LazyColumn(Modifier.weight(1f)){items(list){TransactionRow(it){}}}}}
@Composable private fun OwnerReportReal(page:String,transactions:List<com.pentolrebus.kasir.domain.Transaction>,expenses:List<com.pentolrebus.kasir.domain.Expense>,workers:List<com.pentolrebus.kasir.domain.Worker>,outlets:List<com.pentolrebus.kasir.domain.Outlet>){val total=transactions.sumOf{it.total};val expense=expenses.sumOf{it.amount};Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){Text(page,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Spacer(Modifier.height(10.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){StatCardCompact("Penjualan",money(total),Modifier.weight(1f));StatCardCompact("Transaksi",transactions.size.toString(),Modifier.weight(1f))};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){StatCardCompact("Cash",money(transactions.filter{it.paymentMethod==PaymentMethod.CASH}.sumOf{it.total}),Modifier.weight(1f));StatCardCompact("QRIS",money(transactions.filter{it.paymentMethod==PaymentMethod.QRIS}.sumOf{it.total}),Modifier.weight(1f))};Spacer(Modifier.height(12.dp));when{page.contains("Kasir")->workers.forEach{w->SummaryRow(w.displayName,transactions.filter{it.cashierUid==w.id}.size.toString()+" transaksi")};page.contains("Harian")->SummaryRow("Hari ini",money(total));page.contains("Bulanan")->SummaryRow("Bulan berjalan",money(total));else->{SummaryRow("Pengeluaran",money(expense));SummaryRow("Laba bersih",money(total-expense))}}}}
@Composable private fun ExpenseReal(vm:PosViewModel,session:Session,expenses:List<com.pentolrebus.kasir.domain.Expense>,page:String){var editing by remember{mutableStateOf<com.pentolrebus.kasir.domain.Expense?>(if(page.contains("tambah"))com.pentolrebus.kasir.domain.Expense(ownerUid=session.uid,outletId=session.outletId.orEmpty())else null)};Column(Modifier.fillMaxSize().padding(16.dp)){if(expenses.isEmpty())EmptyState("Belum ada pengeluaran","Catat pengeluaran operasional Anda.",R.drawable.ic_report)else LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){items(expenses){e->SettingsRowPainter(e.title,"${money(e.amount)} · ${e.category}",R.drawable.ic_report){editing=e}}};Button(onClick={editing=com.pentolrebus.kasir.domain.Expense(ownerUid=session.uid,outletId=session.outletId.orEmpty())},Modifier.fillMaxWidth()){Text("TAMBAH PENGELUARAN")}};editing?.let{ExpenseEditor(it,onDismiss={editing=null}){vm.saveExpense(it);editing=null}}}
@Composable private fun ExpenseEditor(initial:Expense,onDismiss:()->Unit,onSave:(Expense)->Unit){var title by remember{mutableStateOf(initial.title)};var amount by remember{mutableStateOf(if(initial.amount==0L)"" else initial.amount.toString())};var category by remember{mutableStateOf(initial.category)};var note by remember{mutableStateOf(initial.note)};AlertDialog(onDismissRequest=onDismiss,title={Text(if(initial.title.isBlank())"Tambah Pengeluaran" else "Ubah Pengeluaran")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(title,{title=it},Modifier.fillMaxWidth(),label={Text("Nama pengeluaran")});OutlinedTextField(amount,{amount=it.filter(Char::isDigit)},Modifier.fillMaxWidth(),label={Text("Nominal")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number));OutlinedTextField(category,{category=it},Modifier.fillMaxWidth(),label={Text("Kategori")});OutlinedTextField(note,{note=it},Modifier.fillMaxWidth(),label={Text("Catatan")})}},confirmButton={Button({if(title.isNotBlank())onSave(initial.copy(title=title,amount=amount.toLongOrNull()?:0,category=category,note=note))}){Text("SIMPAN")}},dismissButton={TextButton(onClick=onDismiss){Text("BATAL")}})}

@Composable private fun DetailCard(title:String,rows:List<Pair<String,String>>,onOpen:(()->Unit)?=null,next:String?=null){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)){Column(Modifier.padding(16.dp)){Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);Spacer(Modifier.height(8.dp));rows.forEach{(a,b)->SummaryRow(a,b)}}};if(onOpen!=null&&next!=null){Spacer(Modifier.height(12.dp));Button(onClick=onOpen,Modifier.fillMaxWidth()){Text(if(next.contains("Form")||next.contains("Ubah"))"UBAH" else next)}}}}
@Composable private fun AuthPreviewPage(page:String){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text(page,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);when{page.contains("Pilih")->{SettingsRowPainter("Owner","Kelola bisnis dan outlet",R.drawable.ic_owner){};SettingsRowPainter("Kasir","Penjualan dan shift",R.drawable.ic_people){}};page.contains("Daftar")->{listOf("Email","Password","Konfirmasi password","Username","PIN","Konfirmasi PIN","Bisnis","Outlet pertama").forEach{OutlinedTextField("",{},Modifier.fillMaxWidth(),label={Text(it)},singleLine=true)};Button({},Modifier.fillMaxWidth()){Text("DAFTAR")}};page.contains("Username")->{OutlinedTextField("",{},Modifier.fillMaxWidth(),label={Text("Username")});OutlinedTextField("",{},Modifier.fillMaxWidth(),label={Text("PIN")});Button({},Modifier.fillMaxWidth()){Text("MASUK")}};else->{OutlinedTextField("",{},Modifier.fillMaxWidth(),label={Text("Email")});OutlinedTextField("",{},Modifier.fillMaxWidth(),label={Text("Password")});Button({},Modifier.fillMaxWidth()){Text("MASUK")}}}}}
@Composable private fun GenericMockPage(page:String){EmptyState(page,"State ini belum memiliki jalur interaksi khusus.",pageIcon(page))}
@Composable private fun EmptyState(title:String,subtitle:String,icon:Int){Column(Modifier.fillMaxSize().padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Icon(painterResource(icon),null,Modifier.height(56.dp),tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.height(14.dp));Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center);Text(subtitle,style=MaterialTheme.typography.bodySmall,textAlign=TextAlign.Center,color=MaterialTheme.colorScheme.onSurfaceVariant)}}

@Composable private fun MoneyDialog(title:String,label:String,onDismiss:()->Unit,onConfirm:(Long)->Unit){var value by remember{mutableStateOf("")};AlertDialog(onDismissRequest=onDismiss,title={Text(title)},text={OutlinedTextField(value,{value=it.filter(Char::isDigit)},label={Text(label)},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))},confirmButton={Button({onConfirm(value.toLongOrNull()?:0)}){Text("SIMPAN")}},dismissButton={TextButton(onClick=onDismiss){Text("BATAL")}})}
@Composable private fun CloseShiftDialog(shift:com.pentolrebus.kasir.domain.Shift?,onDismiss:()->Unit,onConfirm:(Long)->Unit){var value by remember{mutableStateOf("")};AlertDialog(onDismissRequest=onDismiss,title={Text("Tutup Shift")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Cash ${money(shift?.cashTotal?:0)} · QRIS ${money(shift?.qrisTotal?:0)}");OutlinedTextField(value,{value=it.filter(Char::isDigit)},label={Text("Kas akhir")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))}},confirmButton={Button({onConfirm(value.toLongOrNull()?:0)}){Text("TUTUP SHIFT")}},dismissButton={TextButton(onClick=onDismiss){Text("BATAL")}})}
@Composable private fun PrinterPickerDialog(printer:BluetoothPrinter,onDismiss:()->Unit){val devices=remember{printer.pairedDevices()};AlertDialog(onDismissRequest=onDismiss,title={Text("Printer")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.primaryContainer){Column(Modifier.padding(14.dp)){Text("● Terhubung",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold);Text("RPP02N · Bluetooth",style=MaterialTheme.typography.labelSmall)}};Text("Perangkat ditemukan",fontWeight=FontWeight.SemiBold);if(devices.isEmpty())Text("Pasangkan printer melalui Bluetooth Android.",style=MaterialTheme.typography.bodySmall);devices.forEach{d->SettingsRowPainter(d.name,"${d.address} · ${if(d.name.contains("RPP",true))"Terhubung" else "Tersedia"}",R.drawable.ic_printer){}}}},confirmButton={Button(onClick=onDismiss){Text("TEST PRINT")}},dismissButton={TextButton(onClick=onDismiss){Text("SELESAI")}})}

@Composable
private fun TransactionSuccessScreen(transaction: com.pentolrebus.kasir.domain.Transaction?, printer: BluetoothPrinter, onDone: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(28.dp))
        Surface(Modifier.width(76.dp).height(76.dp), shape=RoundedCornerShape(50), color=MaterialTheme.colorScheme.primaryContainer) { Box(contentAlignment=Alignment.Center) { Text("✓", style=MaterialTheme.typography.headlineMedium, color=MaterialTheme.colorScheme.primary, fontWeight=FontWeight.Bold) } }
        Spacer(Modifier.height(16.dp)); Text("Transaksi berhasil", style=MaterialTheme.typography.headlineSmall, fontWeight=FontWeight.Bold); Text("Pembayaran berhasil disimpan", color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp)); Card(Modifier.fillMaxWidth(), shape=RoundedCornerShape(16.dp)) { Column(Modifier.padding(16.dp)) { Text("${transaction?.paymentMethod ?: PaymentMethod.CASH}", color=MaterialTheme.colorScheme.primary, fontWeight=FontWeight.Bold); Text(money(transaction?.total ?: 0), style=MaterialTheme.typography.headlineMedium, fontWeight=FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text("ID transaksi", style=MaterialTheme.typography.labelSmall); Text(transaction?.transactionId ?: "-", style=MaterialTheme.typography.bodySmall); Text("Status", style=MaterialTheme.typography.labelSmall); Text(transaction?.syncStatus?.name ?: "PENDING_SYNC", color=MaterialTheme.colorScheme.primary) } }
        Spacer(Modifier.height(16.dp)); Button(onClick=onDone, Modifier.fillMaxWidth()) { Text("KEMBALI KE KASIR") }
    }
}

@Composable
private fun TransactionDetailDialog(transaction: com.pentolrebus.kasir.domain.Transaction, printer: BluetoothPrinter, onDismiss: () -> Unit) {
    val devices = remember { printer.pairedDevices() }
    var selected by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest=onDismiss,title={Text("Detail Transaksi")},text={Column(verticalArrangement=Arrangement.spacedBy(7.dp)){Text("Transaksi #${transaction.transactionId.takeLast(6)}",fontWeight=FontWeight.Bold);Text("● ${transaction.paymentStatus} · ${transaction.paymentMethod}",color=MaterialTheme.colorScheme.primary);transaction.items.forEach{Text("${it.quantity} × ${it.name}    ${money(it.subtotal)}")};HorizontalDivider();SummaryRow("Subtotal",money(transaction.subtotal));SummaryRow("Total",money(transaction.total),true);Text("Sync: ${transaction.syncStatus}",style=MaterialTheme.typography.labelSmall);if(selected){HorizontalDivider();Text("Printer",fontWeight=FontWeight.SemiBold);if(devices.isEmpty())Text("Belum ada printer Bluetooth yang dipasangkan.",style=MaterialTheme.typography.bodySmall);devices.forEach{d->TextButton(onClick={printer.print(transaction,d.address)},modifier=Modifier.fillMaxWidth()){Text(d.name)}}}}},confirmButton={Button(onClick={selected=false;selected=true}){Icon(painterResource(R.drawable.ic_printer),null);Spacer(Modifier.width(6.dp));Text("CETAK ULANG")}},dismissButton={TextButton(onClick=onDismiss){Text("TUTUP")}})
}
