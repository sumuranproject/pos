package com.sakukasir.pos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
private fun Brand(title: String, sub: String) {
    val c = Sk.c
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(56.dp).clip(RMd).background(c.primary), contentAlignment = Alignment.Center) { Txt("SK", 20, FontWeight.Bold, Color.White, spacing = -.4f) }
        Spacer(Modifier.height(12.dp))
        Txt(title, 24, FontWeight.SemiBold, lineHeight = 1.3f, align = TextAlign.Center)
        Txt(sub, 14, color = c.textMuted, modifier = Modifier.padding(top = 4.dp), align = TextAlign.Center)
    }
}

@Composable
fun LoginScreen(vm: PosViewModel) {
    val c = Sk.c
    var mode by remember { mutableStateOf("login") } // login | register | forgot
    var step by remember { mutableIntStateOf(1) }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var display by remember { mutableStateOf("") }
    var biz by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val notice = vm.state.collectAsState().value.notice
    Box(Modifier.fillMaxSize().background(c.surface).statusBarsPadding().imePadding(), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
            when (mode) {
                "register" -> Brand("Daftar Owner", "Tahap $step dari 2")
                "forgot" -> Brand("Lupa password", "Link reset akan dikirim ke email kamu")
                else -> Brand("SakuKasir", "Point of Sale untuk usaha kamu")
            }
            Column(Modifier.widthIn(max = 400.dp).fillMaxWidth().skCard().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (mode) {
                    "login" -> {
                        Field("Username", user, { user = it }, placeholder = "owner / kasir")
                        Field("Password", pass, { pass = it }, password = true)
                        (notice ?: message)?.let { msg -> Box(Modifier.fillMaxWidth().clip(RSm).background(c.alertSoft).padding(12.dp, 10.dp)) { Txt(msg, 13, FontWeight.Medium, c.alert) } }
                        Btn("Masuk", { vm.login(user, pass) }, Modifier.fillMaxWidth())
                        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            BtnLink("Daftar sebagai Owner", { mode = "register"; step = 1; message = null })
                            BtnLink("Lupa password?", { mode = "forgot"; message = null })
                        }
                    }
                    "register" -> {
                        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(if (step == 1) 24.dp else 8.dp, 8.dp).clip(RPill).background(if (step == 1) c.primary else c.primarySoft))
                            Box(Modifier.size(if (step == 2) 24.dp else 8.dp, 8.dp).clip(RPill).background(if (step == 2) c.primary else c.borderStrong))
                        }
                        if (step == 1) {
                            Field("Username", user, { user = it }); Field("Email", email, { email = it }, keyboard = KeyboardType.Email)
                            Field("Password", pass, { pass = it }, password = true); Field("Nama tampilan", display, { display = it })
                            message?.let { msg -> Box(Modifier.fillMaxWidth().clip(RSm).background(c.alertSoft).padding(12.dp, 10.dp)) { Txt(msg, 13, FontWeight.Medium, c.alert) } }
                            Btn("Lanjut", {
                                message = when {
                                    user.isBlank() || email.isBlank() || pass.isBlank() || display.isBlank() -> "Lengkapi semua field"
                                    pass.length < 6 -> "Password minimal 6 karakter"
                                    else -> { step = 2; null }
                                }
                            }, Modifier.fillMaxWidth())
                        } else {
                            Field("Nama bisnis", biz, { biz = it })
                            SelectField("Tipe bisnis", type.ifEmpty { "Pilih tipe" }, listOf("Kafe / Coffee Shop", "Restoran", "Warung / Kelontong", "Retail", "Jasa", "Lainnya"), { type = it })
                            Field("Nomor kontak (opsional)", phone, { phone = it }, keyboard = KeyboardType.Phone)
                            message?.let { msg -> Box(Modifier.fillMaxWidth().clip(RSm).background(c.alertSoft).padding(12.dp, 10.dp)) { Txt(msg, 13, FontWeight.Medium, c.alert) } }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Btn("Kembali", { step = 1; message = null }, kind = 1)
                                Btn("Daftar", {
                                    if (biz.isBlank() || type.isBlank() || type == "Pilih tipe") message = "Lengkapi data bisnis"
                                    else { mode = "login"; message = "Registrasi berhasil · silakan login" }
                                }, Modifier.weight(1f))
                            }

                        }
                    }
                    else -> {
                        Field("Email terdaftar", email, { email = it }, keyboard = KeyboardType.Email)
                        message?.let { msg -> Box(Modifier.fillMaxWidth().clip(RSm).background(c.alertSoft).padding(12.dp, 10.dp)) { Txt(msg, 13, FontWeight.Medium, c.alert) } }
                        Btn("Kirim link reset", {
                            message = if (email.isBlank()) "Email wajib diisi" else { mode = "login"; "Link reset dikirim ke email kamu" }
                        }, Modifier.fillMaxWidth())
                    }
                }
            }
            if (mode != "login") BtnLink("Kembali ke login", { mode = "login"; message = null })
        }
    }
}
