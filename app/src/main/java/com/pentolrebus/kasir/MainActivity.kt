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
import androidx.compose.ui.unit.sp
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
    val prefs = androidx.compose.ui.platform.LocalContext.current.getSharedPreferences("ui", android.content.Context.MODE_PRIVATE)
    var themeMode by remember { mutableStateOf(prefs.getInt("theme", 0)) }
    val dark = when (themeMode) { 1 -> false; 2 -> true; else -> androidx.compose.foundation.isSystemInDarkTheme() }
    KasirTheme(dark = dark) {
        Surface(Modifier.fillMaxSize()) {
            val auth by vm.auth.collectAsState()
            when (val state = auth) {
                AuthState.LoggedOut -> AuthScreen(vm)
                AuthState.Loading -> LoadingScreen("Memproses…")
                is AuthState.Error -> AuthScreen(vm, state.message)
                is AuthState.LoggedIn -> com.pentolrebus.kasir.ui.MainShell(
                    vm = vm,
                    session = state.session,
                    themeMode = themeMode,
                    onThemeMode = { themeMode = it; prefs.edit().putInt("theme", it).apply() },
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
private fun RoleSelectionScreen(onOwner: () -> Unit, onCashier: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 38.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(42.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) { Box(contentAlignment = Alignment.Center) { Image(painterResource(R.drawable.app_icon), "Logo Saku Kasir", Modifier.size(32.dp)) } }
            Spacer(Modifier.width(10.dp))
            Column { Text("Saku Kasir", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold); Text("Point of Sale", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Text("Selamat datang", style = MaterialTheme.typography.headlineLarge)
        Text("Masuk untuk mengelola bisnis Anda", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 20.dp))
        Text("MASUK SEBAGAI", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
        RoleCard("Owner", "Kelola outlet, produk, laporan", Icons.Default.Store, onOwner)
        RoleCard("Kasir", "Transaksi dan shift", Icons.Default.ShoppingCart, onCashier)
        Text("Butuh bantuan? Hubungi Owner Anda", Modifier.fillMaxWidth().padding(top = 30.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Saku Kasir · Versi 0.1", Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RoleCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(48.dp), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) } }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.bodyMedium); Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text("›", fontSize = 25.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                KSegmented(listOf("PIN Cepat", "Email & Password"), if (emailMode) "Email & Password" else "PIN Cepat") { emailMode = it == "Email & Password" }
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
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (active) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
        onClick = onClick ?: {}
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = if (active) MaterialTheme.colorScheme.onPrimary else textColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
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
        OutlinedTextField(value = value, onValueChange = onValue, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), placeholder = { Text(placeholder, color = muted) }, leadingIcon = icon?.let { { Icon(it, null, tint = muted) } }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = keyboardType), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = background, unfocusedContainerColor = background, focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = border, focusedTextColor = textColor, unfocusedTextColor = textColor, cursorColor = MaterialTheme.colorScheme.primary))
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


