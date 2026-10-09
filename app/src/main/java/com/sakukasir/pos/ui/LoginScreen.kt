package com.sakukasir.pos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** .field-icon: label 14/500 text colour, input-wrap min-height 52, radius 12, icon 18 faint. */
@Composable
private fun IconField(
    label: String, value: String, onChange: (String) -> Unit, icon: ImageVector?, placeholder: String,
    password: Boolean = false, keyboard: KeyboardType = KeyboardType.Text, readOnly: Boolean = false, iconTint: Color = Sk.c.textFaint
) {
    val c = Sk.c
    var show by remember { mutableStateOf(false) }
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Txt(label, 14, FontWeight.Medium)
        Row(
            Modifier.fillMaxWidth().height(52.dp).clip(RMd).background(c.card)
                .border(if (focused) 1.5.dp else 1.dp, if (focused) c.primary else c.borderStrong, RMd).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) { SkIcon(icon, 18.dp, iconTint); Spacer(Modifier.width(10.dp)) }
            BasicTextField(
                value, onChange, singleLine = true, readOnly = readOnly, interactionSource = src, cursorBrush = SolidColor(c.primary),
                textStyle = TextStyle(fontFamily = Inter, fontSize = 15.sp, color = if (readOnly) c.textMuted else c.text),
                keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboard),
                visualTransformation = if (password && !show) PasswordVisualTransformation() else VisualTransformation.None,
                modifier = Modifier.weight(1f).padding(vertical = 14.dp),
                decorationBox = { inner -> Box { if (value.isEmpty()) Txt(placeholder, 15, color = c.textFaint); inner() } }
            )
            if (password) Box(Modifier.padding(start = 6.dp).clip(RoundedCornerShape6dp).clickable { show = !show }.padding(4.dp)) {
                SkIcon(if (show) SkIcons.EyeOff else SkIcons.Eye, 18.dp, c.textFaint)
            }
        }
    }
}
private val RoundedCornerShape6dp = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)

@Composable
private fun BackCircle(onClick: () -> Unit) {
    Box(Modifier.size(40.dp).clip(CircleShape).background(Sk.c.surfaceAlt).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        SkIcon(SkIcons.ArrowLeft, 20.dp, Sk.c.text)
    }
}

@Composable
private fun BigButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RMd).background(Sk.c.primary).clickable(onClick = onClick).padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) { Txt(text, 15, FontWeight.SemiBold, Color.White) }
}

@Composable
private fun ErrorBox(msg: String?) {
    if (msg != null) Box(Modifier.fillMaxWidth().clip(RSm).background(Sk.c.alertSoft).padding(12.dp, 10.dp)) { Txt(msg, 13, FontWeight.Medium, Sk.c.alert) }
}

@Composable
private fun AuthFooter(prefix: String, link: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.Center) {
        Txt("$prefix ", 14, color = Sk.c.textMuted)
        Txt(link, 14, FontWeight.SemiBold, Sk.c.primary, Modifier.clickable(onClick = onClick))
    }
}

@Composable
fun LoginScreen(vm: PosViewModel) {
    val c = Sk.c
    var mode by remember { mutableStateOf("login") } // login | register | setup | forgot
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pw1 by remember { mutableStateOf("") }
    var pw2 by remember { mutableStateOf("") }
    var wa by remember { mutableStateOf("") }
    var bizName by remember { mutableStateOf("") }
    var bizAddr by remember { mutableStateOf("") }
    var bizPhone by remember { mutableStateOf("") }
    var bizType by remember { mutableStateOf("") }
    var forgot by remember { mutableStateOf("") }
    var toast by remember { mutableStateOf<Pair<String, String>?>(null) }
    val notice = vm.state.collectAsState().value.notice
    LaunchedEffect(toast) { if (toast != null) { delay(2500); toast = null } }
    LaunchedEffect(mode) { err = null }

    Box(Modifier.fillMaxSize().background(c.surface).statusBarsPadding()) {
        Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
            Column(
                Modifier.widthIn(max = 460.dp).fillMaxWidth().align(Alignment.CenterHorizontally).padding(start = 24.dp, end = 24.dp, top = if (mode == "login") 80.dp else 24.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (mode != "login") BackCircle { mode = when (mode) { "setup" -> "register"; else -> "login" } }
                when (mode) {
                    "login", "register" -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        BrandMark(72, 16)
                        Spacer(Modifier.height(20.dp))
                        Txt(if (mode == "login") "Selamat Datang" else "Buat Akun SakuKasir", 26, FontWeight.Bold, align = TextAlign.Center, spacing = -.52f, lineHeight = 1.25f)
                        Spacer(Modifier.height(8.dp))
                        Txt(
                            if (mode == "login") "Masuk untuk mengelola bisnis Anda" else "Daftarkan bisnis Anda untuk menyimpan data di cloud dan tetap bisa digunakan secara offline.",
                            14, color = c.textMuted, align = TextAlign.Center, modifier = Modifier.widthIn(max = 340.dp), lineHeight = 1.5f
                        )
                    }
                    "setup" -> Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Txt("Setup Bisnis", 28, FontWeight.Bold, spacing = -.56f, lineHeight = 1.25f)
                        Txt("Lengkapi informasi bisnis Anda", 14, color = c.textMuted, modifier = Modifier.padding(top = 8.dp))
                    }
                    else -> Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Txt("Lupa Password?", 28, FontWeight.Bold, spacing = -.56f, lineHeight = 1.25f)
                        Txt("Masukkan email yang terdaftar. Kami akan mengirimkan token untuk mereset password Anda.", 14, color = c.textMuted, modifier = Modifier.padding(top = 8.dp), lineHeight = 1.5f)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    when (mode) {
                        "login" -> {
                            IconField("Email / Username", user, { user = it }, SkIcons.Mail, "nama@email.com")
                            IconField("Password", pass, { pass = it }, SkIcons.Lock, "Masukkan password", password = true)
                            Row(Modifier.fillMaxWidth().offset(y = (-6).dp), horizontalArrangement = Arrangement.End) { BtnLink("Lupa Password?", { mode = "forgot" }) }
                            ErrorBox(err ?: notice)
                            BigButton("Masuk") { if (user.isBlank() || pass.isBlank()) err = "Email dan password wajib diisi." else { err = null; vm.login(user, pass) } }
                        }
                        "register" -> {
                            IconField("Nama Lengkap / Bisnis *", name, { name = it }, SkIcons.User, "Nama Anda atau Bisnis")
                            IconField("Email *", email, { email = it }, SkIcons.Mail, "contoh@email.com", keyboard = KeyboardType.Email)
                            IconField("Password *", pw1, { pw1 = it }, SkIcons.Lock, "Minimal 6 karakter", password = true)
                            IconField("Konfirmasi Password *", pw2, { pw2 = it }, SkIcons.Lock, "Ulangi password", password = true)
                            IconField("WhatsApp (Opsional)", wa, { wa = it }, SkIcons.Chat, "08123456789", keyboard = KeyboardType.Phone, iconTint = Color(0xFF25D366))
                            ErrorBox(err)
                            BigButton("Daftar") {
                                err = when {
                                    name.isBlank() || email.isBlank() || pw1.isBlank() || pw2.isBlank() -> "Semua field bertanda * wajib diisi."
                                    !Regex("^\\S+@\\S+\\.\\S+$").matches(email.trim()) -> "Format email tidak valid."
                                    pw1.length < 6 -> "Password minimal 6 karakter."
                                    pw1 != pw2 -> "Konfirmasi password tidak cocok."
                                    else -> null
                                }
                                if (err == null) { bizName = name; mode = "setup" }
                            }
                        }
                        "setup" -> {
                            IconField("Nama Bisnis *", bizName, { bizName = it }, null, "Nama bisnis")
                            IconField("Alamat Bisnis *", bizAddr, { bizAddr = it }, null, "Alamat lengkap")
                            IconField("No Telepon Bisnis *", bizPhone, { bizPhone = it }, null, "Nomor telepon", keyboard = KeyboardType.Phone)
                            IconField("Email Bisnis", email.trim().lowercase(), {}, null, "", readOnly = true)
                            var open by remember { mutableStateOf(false) }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Txt("Jenis Bisnis *", 14, FontWeight.Medium)
                                Box {
                                    Row(
                                        Modifier.fillMaxWidth().height(52.dp).clip(RMd).background(c.card).border(1.dp, c.borderStrong, RMd)
                                            .clickable { open = true }.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        SkIcon(SkIcons.Building, 18.dp, c.textFaint); Spacer(Modifier.width(10.dp))
                                        Txt(bizType.ifEmpty { "Pilih jenis bisnis" }, 15, color = if (bizType.isEmpty()) c.textFaint else c.text, modifier = Modifier.weight(1f))
                                        SkIcon(SkIcons.ChevronDown, 16.dp, c.textMuted)
                                    }
                                    DropdownMenu(open, { open = false }) {
                                        listOf("Kafe / Coffee Shop", "Restoran", "Warung / Kelontong", "Retail", "Jasa", "Lainnya").forEach { o ->
                                            DropdownMenuItem(text = { Txt(o, 14) }, onClick = { bizType = o; open = false })
                                        }
                                    }
                                }
                            }
                            BigButton("Selesai Setup") {
                                if (bizName.isBlank() || bizAddr.isBlank() || bizPhone.isBlank() || bizType.isBlank()) toast = "Lengkapi semua field wajib." to "error"
                                else { toast = "Akun berhasil dibuat. Silakan login." to "success"; user = email.trim(); pass = ""; mode = "login" }
                            }
                        }
                        else -> {
                            IconField("Email", forgot, { forgot = it }, SkIcons.Mail, "nama@email.com", keyboard = KeyboardType.Email)
                            BigButton("Kirim Instruksi") { if (forgot.isNotBlank()) { toast = "Link reset dikirim ke ${forgot.trim()}" to "success"; mode = "login" } }
                        }
                    }
                }
                if (mode == "login") AuthFooter("Belum punya akun?", "Daftar Sekarang") { mode = "register" }
                if (mode == "register") AuthFooter("Sudah punya akun?", "Masuk disini") { mode = "login" }
            }
        }
        ToastView(toast, 16.dp)
    }
}
