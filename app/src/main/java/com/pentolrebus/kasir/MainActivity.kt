package com.pentolrebus.kasir

import android.Manifest
import android.content.pm.PackageManager
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pentolrebus.kasir.data.RepositoryProvider
import com.pentolrebus.kasir.data.OfflineStore
import com.pentolrebus.kasir.domain.PaymentMethod
import com.pentolrebus.kasir.domain.Product
import com.pentolrebus.kasir.domain.Role
import com.pentolrebus.kasir.domain.Session
import com.pentolrebus.kasir.ui.AuthState
import com.pentolrebus.kasir.ui.KasirTheme
import com.pentolrebus.kasir.ui.KSegmented
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
        val baseDensity = LocalDensity.current
        // Match the HTML/screenshot design scale globally, not typography alone.
        // The device's actual density remains untouched; only the app's Compose
        // design coordinate space is scaled proportionally by 10%.
        androidx.compose.runtime.CompositionLocalProvider(
            LocalDensity provides Density(
                density = baseDensity.density * 1.10f,
                fontScale = baseDensity.fontScale
            )
        ) {
        val view = LocalView.current
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val barColor = if (dark) Color(0xFF0A0E16) else Color(0xFFE8EEF7)
            window.statusBarColor = barColor.toArgb()
            window.navigationBarColor = barColor.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
        Surface(Modifier.fillMaxSize(), color = if (dark) Color(0xFF0A0E16) else Color(0xFFE8EEF7)) {
            BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val wide = maxWidth >= 520.dp
                Surface(
                    Modifier.fillMaxSize().widthIn(max = 992.dp),
                    shape = if (wide) RoundedCornerShape(28.dp) else RoundedCornerShape(0.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = if (wide) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
                ) {
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
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
        Surface(Modifier.fillMaxWidth().heightIn(min = 55.dp), shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.surface) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_lock), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                BasicTextField(value, onValue, Modifier.weight(1f).padding(horizontal = 9.dp, vertical = 14.dp), singleLine = true, textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold), cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(), decorationBox = { inner -> if (value.isEmpty()) Text("Masukkan password", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge); inner() })
                IconButton(onClick = { visible = !visible }, modifier = Modifier.size(44.dp)) { Icon(painterResource(if (visible) R.drawable.ic_eye else R.drawable.ic_eye_off), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp)) }
            }
        }
    }
}

@Composable
private fun PinField(label: String, value: String, onValue: (String) -> Unit) = AuthPinField(label, value, onValue)

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
    val message by vm.message.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(message) { message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); vm.consumeMessage() } }

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
                onForgotPassword = { vm.resetPassword(email) },
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
    Column(Modifier.fillMaxSize().padding(horizontal = 17.6.dp, vertical = 15.4.dp)) {
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.8.dp, bottom = 61.6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(Modifier.size(55.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(painterResource(R.drawable.app_icon), "Logo Saku Kasir", Modifier.size(50.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Saku Kasir", fontSize = 17.5.sp, lineHeight = 21.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Point of Sale", fontSize = 12.2.sp, lineHeight = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("Selamat datang", fontSize = 27.5.sp, lineHeight = 33.sp, fontWeight = FontWeight.ExtraBold)
            Text("Masuk untuk mengelola bisnis Anda", fontSize = 13.2.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))
            Text("MASUK SEBAGAI", fontSize = 11.2.sp, lineHeight = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            RoleCard("Owner", "Kelola outlet, produk, laporan", R.drawable.ic_owner, onOwner)
            RoleCard("Kasir", "Transaksi dan shift", R.drawable.ic_people, onCashier)
        }
        Column(Modifier.fillMaxWidth().padding(top = 20.dp)) {
            Text("Butuh bantuan? Hubungi Owner Anda", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Saku Kasir · Versi 0.1", Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RoleCard(title: String, subtitle: String, icon: Int, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick), shape = RoundedCornerShape(17.6.dp), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(15.8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(57.2.dp), shape = RoundedCornerShape(15.8.dp), color = MaterialTheme.colorScheme.primaryContainer) { Box(contentAlignment = Alignment.Center) { Icon(painterResource(icon), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.6.dp)) } }
            Spacer(Modifier.width(15.4.dp))
            Column(Modifier.weight(1f)) { Text(title, fontSize = 16.5.sp, lineHeight = 20.sp, fontWeight = FontWeight.ExtraBold); Text(subtitle, fontSize = 12.sp, lineHeight = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Icon(painterResource(R.drawable.ic_chevron), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun LoginRoleScreen(
    role: Role, register: Boolean, email: String, password: String, confirmPassword: String, username: String, pin: String, confirmPin: String,
    business: String, outlet: String, whatsapp: String, error: String?, validation: String?, onBack: () -> Unit, onRegisterToggle: () -> Unit,
    onEmail: (String) -> Unit, onPassword: (String) -> Unit, onConfirmPassword: (String) -> Unit, onUsername: (String) -> Unit,
    onPin: (String) -> Unit, onConfirmPin: (String) -> Unit, onBusiness: (String) -> Unit, onOutlet: (String) -> Unit, onWhatsapp: (String) -> Unit,
    onValidation: (String?) -> Unit, onLoginLocal: () -> Unit, onLoginEmail: () -> Unit, onForgotPassword: () -> Unit, onRegister: () -> Unit
) {
    val isOwner = role == Role.OWNER
    var emailMode by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 17.6.dp, vertical = 13.2.dp)) {
        Surface(
            Modifier.size(48.4.dp).clickable { onBack() },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_back), "Kembali", modifier = Modifier.size(24.6.dp), tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Image(painterResource(R.drawable.app_icon), "Logo Saku Kasir", Modifier.size(58.dp).padding(top = 8.8.dp))
            Text("Selamat datang", fontSize = 27.5.sp, lineHeight = 33.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 8.8.dp))
            Text("Masuk untuk mengelola bisnis Anda", fontSize = 14.2.sp, lineHeight = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Text("● ${if (isOwner) "Owner" else "Kasir"}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
            }
        }
        if (isOwner && !register) {
            Spacer(Modifier.height(18.dp))
            KSegmented(listOf("PIN Cepat", "Email & Password"), if (emailMode) "Email & Password" else "PIN Cepat") { emailMode = it == "Email & Password" }
            Spacer(Modifier.height(10.dp))
            if (!emailMode) {
                AuthField("Username", "Masukkan username", username, onUsername, R.drawable.ic_person)
                Spacer(Modifier.height(9.dp)); AuthPinField("PIN", pin, onPin)
                error?.let { Spacer(Modifier.height(7.dp)); AlertBox(it) }
                Spacer(Modifier.height(11.dp)); AuthPrimaryButton("Masuk", onLoginLocal)
            } else {
                AuthField("Email", "Masukkan email", email, onEmail, R.drawable.ic_mail, KeyboardType.Email)
                Spacer(Modifier.height(9.dp)); PasswordField("Password", password, onPassword)
                TextButton(
                    onClick = onForgotPassword,
                    enabled = true,
                    modifier = Modifier.padding(top = 2.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
                ) { Text("Lupa Password?", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
                error?.let { Spacer(Modifier.height(7.dp)); AlertBox(it) }
                Spacer(Modifier.height(11.dp)); AuthPrimaryButton("Masuk", onLoginEmail)
            }
            TextButton(onClick = onRegisterToggle, modifier = Modifier.padding(top = 2.dp), contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)) { Text("Daftar sebagai Owner", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
        } else if (isOwner) {
            error?.let { AlertBox(it) }; validation?.let { AlertBox(it) }
            Spacer(Modifier.height(18.dp));
            AuthField("Email", "Masukkan email", email, onEmail, R.drawable.ic_mail, KeyboardType.Email)
            Spacer(Modifier.height(9.dp)); PasswordField("Password", password, onPassword)
            Spacer(Modifier.height(9.dp)); PasswordField("Confirm Password", confirmPassword, onConfirmPassword)
            Spacer(Modifier.height(9.dp)); AuthField("Username", "Masukkan username", username, onUsername, R.drawable.ic_person)
            Spacer(Modifier.height(9.dp)); AuthPinField("PIN", pin, onPin)
            Spacer(Modifier.height(9.dp)); AuthPinField("Confirm PIN", confirmPin, onConfirmPin)
            Spacer(Modifier.height(9.dp)); AuthField("Nama Bisnis (opsional)", "Masukkan nama bisnis", business, onBusiness, null)
            Spacer(Modifier.height(9.dp)); AuthField("Nama Outlet (opsional)", "Masukkan nama outlet", outlet, onOutlet, null)
            Spacer(Modifier.height(9.dp)); AuthField("WhatsApp (opsional)", "Masukkan WhatsApp", whatsapp, onWhatsapp, null)
            Spacer(Modifier.height(12.dp)); AuthPrimaryButton("Daftar Owner", onRegister)
            TextButton(onClick = onRegisterToggle, modifier = Modifier.fillMaxWidth()) { Text("Sudah punya akun? Login Owner", color = MaterialTheme.colorScheme.primary) }
        } else {
            error?.let { AlertBox(it) }
            Spacer(Modifier.height(18.dp)); AuthField("Username", "Masukkan username", username, onUsername, R.drawable.ic_person)
            Spacer(Modifier.height(9.dp)); AuthPinField("PIN", pin, onPin)
            Spacer(Modifier.height(12.dp)); AuthPrimaryButton("Masuk", onLoginLocal)
            Text("Akun kasir dibuat oleh Owner.\nLupa PIN? Tanyakan Owner Anda.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 14.dp))
            Text("Contoh: dewi / 1234", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AuthField(label: String, placeholder: String, value: String, onValue: (String) -> Unit, icon: Int?, keyboardType: KeyboardType = KeyboardType.Text) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
        Surface(Modifier.fillMaxWidth().heightIn(min = 55.dp), shape = RoundedCornerShape(13.2.dp), color = MaterialTheme.colorScheme.surface) {
            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) { Icon(painterResource(icon), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(11.dp)) }
                androidx.compose.foundation.text.BasicTextField(value, onValue, Modifier.weight(1f).padding(vertical = 14.dp), singleLine = true, textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold), cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface), keyboardOptions = KeyboardOptions(keyboardType = keyboardType), decorationBox = { inner -> if (value.isEmpty()) Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium); inner() })
            }
        }
    }
}

@Composable
private fun AuthPinField(label: String, value: String, onValue: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
        Surface(Modifier.fillMaxWidth().heightIn(min = 55.dp), shape = RoundedCornerShape(13.2.dp), color = MaterialTheme.colorScheme.surface) {
            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_dialpad), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(11.dp))
                androidx.compose.foundation.text.BasicTextField(value, { onValue(it.filter(Char::isDigit).take(6)) }, Modifier.weight(1f).padding(vertical = 14.dp), singleLine = true, textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold), cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(), decorationBox = { inner -> if (value.isEmpty()) Text("Masukkan PIN", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium); inner() })
                IconButton(onClick = { visible = !visible }, modifier = Modifier.size(44.dp)) { Icon(painterResource(if (visible) R.drawable.ic_eye else R.drawable.ic_eye_off), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp)) }
            }
        }
    }
}

@Composable
private fun AuthPrimaryButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 52.8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary), shape = RoundedCornerShape(15.4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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


