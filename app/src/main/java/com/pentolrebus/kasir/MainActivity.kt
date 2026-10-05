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
private fun AuthScreen(vm: PosViewModel, error: String? = null) {
    var register by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var business by remember { mutableStateOf("") }
    var outlet by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var validation by remember { mutableStateOf<String?>(null) }
    val message by vm.message.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(message) { message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); vm.consumeMessage() } }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(horizontal = 17.6.dp, vertical = 13.2.dp)) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                Image(painterResource(R.drawable.app_icon), "Logo Saku Kasir", Modifier.size(76.dp))
                Text(if (register) "Daftar sebagai Owner" else "Selamat datang", fontSize = 27.5.sp, lineHeight = 33.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 10.dp))
                Text(if (register) "Buat akun Owner untuk mengelola bisnis Anda" else "Masuk untuk mengelola bisnis Anda", fontSize = 14.2.sp, lineHeight = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 18.dp))

                error?.let { AlertBox(it); Spacer(Modifier.height(9.dp)) }
                validation?.let { AlertBox(it); Spacer(Modifier.height(9.dp)) }

                AuthField("Username", "Masukkan username", username, { username = it }, R.drawable.ic_person)
                Spacer(Modifier.height(9.dp))

                if (register) {
                    AuthField("Email", "Masukkan email", email, { email = it }, R.drawable.ic_mail, KeyboardType.Email)
                    Spacer(Modifier.height(9.dp))
                    PasswordField("Password", password, { password = it })
                    Spacer(Modifier.height(9.dp))
                    PasswordField("Konfirmasi Password", confirmPassword, { confirmPassword = it })
                    Spacer(Modifier.height(9.dp))
                    AuthField("Nama Owner", "Masukkan nama", ownerName, { ownerName = it }, null)
                    Spacer(Modifier.height(9.dp))
                    AuthField("Nama Bisnis (opsional)", "Masukkan nama bisnis", business, { business = it }, null)
                    Spacer(Modifier.height(9.dp))
                    AuthField("Nama Outlet (opsional)", "Masukkan nama outlet", outlet, { outlet = it }, null)
                    Spacer(Modifier.height(9.dp))
                    AuthField("WhatsApp (opsional)", "Masukkan WhatsApp", whatsapp, { whatsapp = it }, null, KeyboardType.Phone)
                    Spacer(Modifier.height(12.dp))
                    AuthPrimaryButton("Daftar Owner") {
                        validation = validateRegistration(email, password, confirmPassword, username)
                        if (validation == null) vm.register(email, password, username, ownerName, business.takeIf(String::isNotBlank), outlet.takeIf(String::isNotBlank), whatsapp.takeIf(String::isNotBlank))
                    }
                    TextButton(onClick = { register = false; validation = null }, modifier = Modifier.fillMaxWidth()) {
                        Text("Sudah punya akun? Masuk", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                } else {
                    PasswordField("Password", password, { password = it })
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { vm.resetPassword(username) }, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)) {
                            Text("Lupa Password?", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    AuthPrimaryButton("Masuk") { vm.login(username, password) }
                    TextButton(onClick = { register = true; validation = null }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Text("Daftar sebagai Owner", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(
                if (register) "Gunakan email aktif untuk pemulihan password Owner." else "Akun Kasir dibuat oleh Owner. Gunakan Username dan Password yang diberikan Owner.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp)
            )
        }
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
@Composable
private fun AuthPrimaryButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 52.8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary), shape = RoundedCornerShape(15.4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

private fun validateRegistration(email: String, password: String, confirmPassword: String, username: String): String? {
    val clean = username.trim().lowercase().replace(" ", "")
    return when {
        clean.length < 3 -> "Username minimal 3 karakter"
        !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Email tidak valid"
        password.length < 8 -> "Password minimal 8 karakter"
        password != confirmPassword -> "Konfirmasi Password tidak cocok"
        else -> null
    }
}

@Composable
private fun AlertBox(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Text(message, Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer)
    }
}


