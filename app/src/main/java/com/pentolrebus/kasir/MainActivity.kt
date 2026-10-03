package com.pentolrebus.kasir

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pentolrebus.kasir.data.RepositoryProvider
import com.pentolrebus.kasir.domain.*
import com.pentolrebus.kasir.ui.*
import com.pentolrebus.kasir.util.Diagnostics
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private fun money(value: Long): String = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    .apply { maximumFractionDigits = 0 }
    .format(value).replace("Rp", "Rp ")

private val Blue get() = Color(0xFF2563EB)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val diagnostics = Diagnostics(this)
        val repository = RepositoryProvider.create(this, diagnostics)
        setContent {
            KasirTheme { val vm: PosViewModel = viewModel(factory = PosViewModelFactory(repository)); KasirApp(vm, diagnostics) }
        }
    }
}

@Composable private fun KasirApp(vm: PosViewModel, diagnostics: Diagnostics) {
    var dark by remember { mutableStateOf(false) }
    KasirTheme(dark = dark) {
        Surface(Modifier.fillMaxSize()) {
            when (val state = vm.auth.collectAsState().value) {
                AuthState.LoggedOut -> AuthScreen(vm)
                AuthState.Loading -> LoadingScreen()
                is AuthState.Error -> AuthScreen(vm, state.message)
                is AuthState.LoggedIn -> MainShell(vm, state.session, dark, { dark = !dark }, diagnostics)
            }
        }
    }
}

@Composable private fun LoadingScreen() = Box(Modifier.fillMaxSize(), Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(); Spacer(Modifier.height(12.dp)); Text("Memproses…") }
}

@Composable private fun PasswordField(label: String, value: String, onValue: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(value, onValue, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = { IconButton({ visible = !visible }) { Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } })
}

@Composable private fun PinField(label: String, value: String, onValue: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(value, onValue, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        trailingIcon = { IconButton({ visible = !visible }) { Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } })
}

@Composable private fun AuthScreen(vm: PosViewModel, error: String? = null) {
    var role by remember { mutableStateOf<Role?>(null) }; var register by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var confirmPassword by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }; var pin by remember { mutableStateOf("") }; var confirmPin by remember { mutableStateOf("") }
    var business by remember { mutableStateOf("") }; var outlet by remember { mutableStateOf("") }; var whatsapp by remember { mutableStateOf("") }
    var validation by remember { mutableStateOf<String?>(null) }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (role == null) RoleSelectionScreen({ role = Role.OWNER }, { role = Role.CASHIER })
        else LoginRoleScreen(role!!, register, email, password, confirmPassword, username, pin, confirmPin, business, outlet, whatsapp, error, validation,
            { role = null; register = false; validation = null }, { register = !register; validation = null },
            { email = it }, { password = it }, { confirmPassword = it }, { username = it }, { pin = it }, { confirmPin = it }, { business = it }, { outlet = it }, { whatsapp = it }, { validation = it },
            { vm.loginLocal(username, pin, role!!) }, { vm.loginEmail(email, password, role!!) }, {
                validation = validateRegistration(email,password,confirmPassword,username,pin,confirmPin,business,outlet)
                if (validation == null) vm.register(email,password,username,pin,business,outlet,whatsapp.takeIf(String::isNotBlank))
            })
    }
}

@Composable private fun RoleSelectionScreen(onOwner: () -> Unit, onCashier: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Spacer(Modifier.height(18.dp)); Image(painterResource(com.pentolrebus.kasir.R.drawable.sakukasir_logo), "SakuKasir", Modifier.size(76.dp))
        Text("SakuKasir", fontSize = 28.sp, fontWeight = FontWeight.Bold); Text("Pilih peran Anda untuk masuk", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp)); RoleCard("OWNER","Kelola bisnis & sistem",Icons.Default.Store,onOwner); RoleCard("KASIR","Transaksi & POS",Icons.Default.PointOfSale,onCashier)
    }
}

@Composable private fun RoleCard(title:String, subtitle:String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick:()->Unit) {
    Surface(onClick=onClick, modifier=Modifier.fillMaxWidth().widthIn(max=480.dp).height(88.dp), shape=RoundedCornerShape(16.dp), color=MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxSize().padding(horizontal=16.dp), verticalAlignment=Alignment.CenterVertically) {
            Surface(Modifier.size(48.dp), CircleShape, MaterialTheme.colorScheme.primaryContainer) { Box(Alignment.Center){ Icon(icon,null,tint=Blue) } }
            Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.Bold);Text(subtitle,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            Icon(Icons.Default.ChevronRight,null,tint=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun LoginRoleScreen(role:Role, register:Boolean, email:String,password:String,confirmPassword:String,username:String,pin:String,confirmPin:String,business:String,outlet:String,whatsapp:String,error:String?,validation:String?,onBack:()->Unit,onRegisterToggle:()->Unit,onEmail:(String)->Unit,onPassword:(String)->Unit,onConfirmPassword:(String)->Unit,onUsername:(String)->Unit,onPin:(String)->Unit,onConfirmPin:(String)->Unit,onBusiness:(String)->Unit,onOutlet:(String)->Unit,onWhatsapp:(String)->Unit,onValidation:(String?)->Unit,onLoginLocal:()->Unit,onLoginEmail:()->Unit,onRegister:()->Unit) {
    val owner=role==Role.OWNER; var emailMode by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=20.dp,vertical=18.dp), horizontalAlignment=Alignment.CenterHorizontally) {
        Image(painterResource(com.pentolrebus.kasir.R.drawable.sakukasir_logo),"SakuKasir",Modifier.size(62.dp)); Spacer(Modifier.height(6.dp)); Text("SakuKasir",fontSize=23.sp,fontWeight=FontWeight.Bold)
        Text(if(owner)"Selamat datang kembali, Owner" else "Selamat datang kembali, Kasir",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=13.sp); Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().widthIn(max=520.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onBack){Icon(Icons.Default.ArrowBack,"Kembali")};Text(if(register)"Daftar Owner" else "Login ${if(owner)"Owner" else "Kasir"}",fontSize=20.sp,fontWeight=FontWeight.Bold)}
        Spacer(Modifier.height(10.dp))
        if(owner&&!register){
            Row(Modifier.fillMaxWidth().widthIn(max=520.dp).height(48.dp)){AuthTab("PIN Cepat",!emailMode,Modifier.weight(1f)){emailMode=false};AuthTab("Email & Password",emailMode,Modifier.weight(1f)){emailMode=true}}
            Spacer(Modifier.height(12.dp)); if(!emailMode){AuthField("Username","Masukkan username",username,onUsername);Spacer(Modifier.height(8.dp));PinField("PIN",pin,onPin);Spacer(Modifier.height(14.dp));PrimaryButton("MASUK",onLoginLocal)}
            else{AuthField("Email","Masukkan email",email,onEmail,KeyboardType.Email);Spacer(Modifier.height(8.dp));PasswordField("Password",password,onPassword);Spacer(Modifier.height(14.dp));PrimaryButton("MASUK",onLoginEmail)}
            TextButton(onRegisterToggle){Text("Daftar sebagai Owner",color=Blue)}
        } else if(owner){
            error?.let{AlertBox(it)}; validation?.let{AlertBox(it)}
            AuthField("Email","Masukkan email",email,onEmail,KeyboardType.Email);Spacer(Modifier.height(8.dp));PasswordField("Password",password,onPassword);Spacer(Modifier.height(8.dp));PasswordField("Confirm Password",confirmPassword,onConfirmPassword);Spacer(Modifier.height(8.dp));AuthField("Username","Masukkan username",username,onUsername);Spacer(Modifier.height(8.dp));PinField("PIN",pin,onPin);Spacer(Modifier.height(8.dp));PinField("Confirm PIN",confirmPin,onConfirmPin);Spacer(Modifier.height(8.dp));AuthField("Nama Bisnis","Masukkan nama bisnis",business,onBusiness);Spacer(Modifier.height(8.dp));AuthField("Nama Cabang Pertama","Masukkan nama outlet",outlet,onOutlet);Spacer(Modifier.height(8.dp));AuthField("WhatsApp (opsional)","Nomor WhatsApp",whatsapp,onWhatsapp,KeyboardType.Phone);Spacer(Modifier.height(14.dp));PrimaryButton("DAFTAR OWNER",onRegister)
        } else {
            error?.let{AlertBox(it)};AuthField("Username","Masukkan username",username,onUsername);Spacer(Modifier.height(8.dp));PinField("PIN",pin,onPin);Spacer(Modifier.height(14.dp));PrimaryButton("MASUK",onLoginLocal)
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable private fun AuthTab(label:String,active:Boolean,modifier:Modifier,onClick:()->Unit)=Surface(onClick=onClick,modifier=modifier.fillMaxHeight().padding(2.dp),shape=RoundedCornerShape(14.dp),color=if(active)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant){Box(Alignment.Center){Text(label,fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=if(active)Color.White else MaterialTheme.colorScheme.onSurfaceVariant)}}
@Composable private fun AuthField(label:String,placeholder:String,value:String,onValue:(String)->Unit,type:KeyboardType=KeyboardType.Text){Column(Modifier.fillMaxWidth().widthIn(max=520.dp)){Text(label,fontSize=12.sp,fontWeight=FontWeight.Medium,color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(4.dp));OutlinedTextField(value,onValue,Modifier.fillMaxWidth(),placeholder={Text(placeholder,fontSize=13.sp)},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=type))}}
@Composable private fun PrimaryButton(label:String,onClick:()->Unit)=Button(onClick=onClick,modifier=Modifier.fillMaxWidth().widthIn(max=520.dp).height(48.dp),shape=RoundedCornerShape(12.dp)){Text(label,fontWeight=FontWeight.Bold,fontSize=13.sp)}
@Composable private fun AlertBox(message:String)=Surface(Modifier.fillMaxWidth().padding(bottom=8.dp),shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.errorContainer){Text(message,Modifier.padding(12.dp),fontSize=12.sp,color=MaterialTheme.colorScheme.onErrorContainer)}
private fun validateRegistration(email:String,password:String,confirmPassword:String,username:String,pin:String,confirmPin:String,business:String,outlet:String):String?=when{!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()->"Email tidak valid";password.length<8->"Password minimal 8 karakter";password!=confirmPassword->"Confirm Password tidak cocok";username.length<3->"Username minimal 3 karakter";!pin.matches(Regex("\\d{4,6}"))->"PIN harus 4–6 angka";pin!=confirmPin->"Confirm PIN tidak cocok";business.isBlank()->"Nama bisnis wajib diisi";outlet.isBlank()->"Nama cabang pertama wajib diisi";else->null}

@Composable private fun MainShell(vm:PosViewModel,session:Session,dark:Boolean,onTheme:()->Unit,diagnostics:Diagnostics){
    var screen by remember{mutableStateOf(if(session.role==Role.OWNER)"reports" else "pos")}; var reportPage by remember{mutableStateOf("Ringkasan")}; var settingsPage by remember{mutableStateOf<String?>(null)}; var showStart by remember{mutableStateOf(false)};var showClose by remember{mutableStateOf(false)}
    val products=vm.products.collectAsState().value; val cart=vm.cart.collectAsState().value; val shift=vm.shift.collectAsState().value; val transactions=vm.transactions.collectAsState().value
    val title=when(screen){"pos"->"Kasir";"checkout"->"Checkout";"reports"->"Laporan";"settings"->"Pengaturan";else->"SakuKasir"}
    Column(Modifier.fillMaxSize()){
        AppTopBar(title,session,dark,onTheme,{diagnostics.exportToDownloads()},{if(shift==null)vm.logout()else showClose=true})
        Box(Modifier.weight(1f).fillMaxWidth()){
            when(screen){
                "pos"->PosPage(vm,products,cart,shift,{showStart=true},{screen="checkout"})
                "checkout"->CheckoutPage(vm,cart){screen="pos"}
                "reports"->ReportsPage(session.role,transactions,shift,reportPage,{reportPage=it})
                "settings"->SettingsPage(session,settingsPage,{settingsPage=it},{settingsPage=null},onTheme,{diagnostics.exportToDownloads()},{if(shift==null)vm.logout()else showClose=true})
            }
        }
        BottomBar(screen,cart.sumOf{it.quantity},{screen="pos"},{if(cart.isNotEmpty())screen="checkout"},{screen="reports"},{screen="settings"})
    }
    if(showStart)MoneyDialog("Mulai Shift","Kas awal",{showStart=false}){vm.startShift(it);showStart=false}
    if(showClose)MoneyDialog("Tutup Shift","Kas akhir",{showClose=false}){vm.closeShift(it);showClose=false}
}

@Composable private fun AppTopBar(title:String,session:Session,dark:Boolean,onTheme:()->Unit,onDownload:()->Unit,onLogout:()->Unit){
    Surface(color=MaterialTheme.colorScheme.background){Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(title,fontSize=20.sp,fontWeight=FontWeight.Bold);Text("${session.username} · ${session.role.name.lowercase().replaceFirstChar{it.uppercase()}}",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};IconButton(onTheme){Icon(if(dark)Icons.Default.LightMode else Icons.Default.DarkMode,"Tema")};IconButton(onDownload){Icon(Icons.Default.Download,"Download log")};IconButton(onLogout){Icon(Icons.Default.Logout,"Keluar")}}}
}

@Composable private fun BottomBar(screen:String,badge:Int,onKasir:()->Unit,onCheckout:()->Unit,onReport:()->Unit,onSettings:()->Unit){Surface(color=MaterialTheme.colorScheme.surface,tonalElevation=2.dp){Row(Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=7.dp),horizontalArrangement=Arrangement.SpaceEvenly){BottomItem("Kasir",Icons.Default.PointOfSale,screen=="pos",onKasir);BottomItem("Checkout",Icons.Default.ShoppingCart,screen=="checkout",onCheckout,badge);BottomItem("Laporan",Icons.Default.ReceiptLong,screen=="reports",onReport);BottomItem("Pengaturan",Icons.Default.Settings,screen=="settings",onSettings)}}}
@Composable private fun BottomItem(label:String,icon:androidx.compose.ui.graphics.vector.ImageVector,selected:Boolean,onClick:()->Unit,badge:Int=0){Surface(onClick=onClick,modifier=Modifier.weight(1f).height(56.dp).padding(horizontal=3.dp),shape=RoundedCornerShape(14.dp),color=if(selected)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Box{Icon(icon,label,tint=if(selected)Blue else MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.size(21.dp));if(badge>0)Badge(Modifier.align(Alignment.TopEnd)){Text(badge.coerceAtMost(99).toString(),fontSize=8.sp)}};Text(label,fontSize=10.sp,fontWeight=if(selected)FontWeight.Bold else FontWeight.Medium,maxLines=1,color=if(selected)Blue else MaterialTheme.colorScheme.onSurfaceVariant)}}}

@Composable private fun PosPage(vm:PosViewModel,products:List<Product>,cart:List<CartItem>,shift:Shift?,onStart:()->Unit,onCheckout:()->Unit){
    val total=cart.sumOf{it.product.price*it.quantity}; var category by remember{mutableStateOf("Semua")}; var search by remember{mutableStateOf("")}
    val visible=products.filter{it.active&&it.name.contains(search,true)}
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp,vertical=8.dp)){
        ShiftHeader(shift,onStart);Spacer(Modifier.height(10.dp));
        OutlinedTextField(search,{search=it},Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("Cari produk…")},leadingIcon={Icon(Icons.Default.Search,null)},shape=RoundedCornerShape(12.dp))
        Spacer(Modifier.height(8.dp));LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){item{Chip("Semua",category=="Semua"){category="Semua"}};item{Chip("Produk",category=="Produk"){category="Produk"}};item{Chip("Favorit",category=="Favorit"){category="Favorit"}}}
        Spacer(Modifier.height(10.dp));
        BoxWithConstraints(Modifier.weight(1f)){val cols=if(maxWidth>=700.dp)4 else if(maxWidth>=430.dp)3 else 2;LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(visible.chunked(cols)){row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){row.forEach{p->ProductTile(p,{if(shift!=null)vm.add(p)},Modifier.weight(1f));repeat(cols-row.size){Spacer(Modifier.weight(1f))}}}}}}
        Surface(Modifier.fillMaxWidth().padding(top=8.dp),shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.primaryContainer){Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Keranjang · ${cart.sumOf{it.quantity}} item",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(money(total),fontSize=19.sp,fontWeight=FontWeight.Bold)};Button(onCheckout,enabled=shift!=null&&cart.isNotEmpty(),shape=RoundedCornerShape(12.dp)){Text("CHECKOUT")}}}
    }
}

@Composable private fun ShiftHeader(shift:Shift?,onStart:()->Unit){Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),color=if(shift==null)MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer){Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){Surface(Modifier.size(38.dp),CircleShape,color=if(shift==null)MaterialTheme.colorScheme.outline.copy(alpha=.18f) else MaterialTheme.colorScheme.primary){Box(Alignment.Center){Icon(if(shift==null)Icons.Default.Schedule else Icons.Default.Check,null,tint=if(shift==null)MaterialTheme.colorScheme.onSurfaceVariant else Color.White,modifier=Modifier.size(20.dp))}};Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(if(shift==null)"Belum ada Shift" else "Shift Aktif",fontWeight=FontWeight.Bold);Text(if(shift==null)"Mulai shift sebelum transaksi" else "Shift dimulai · ${time(shift.startAt)}",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};if(shift==null)Button(onStart,shape=RoundedCornerShape(10.dp),contentPadding=PaddingValues(horizontal=12.dp)){Text("MULAI",fontSize=11.sp)}}}}

@Composable private fun ProductTile(p:Product,onAdd:()->Unit,modifier:Modifier){Surface(modifier,shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.padding(8.dp)){Box(Modifier.fillMaxWidth().aspectRatio(1.35f).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primaryContainer),Alignment.Center){Icon(Icons.Default.Image,null,tint=Blue,modifier=Modifier.size(28.dp))};Spacer(Modifier.height(7.dp));Text(p.name,fontSize=12.sp,fontWeight=FontWeight.SemiBold,maxLines=2);Text(money(p.price),fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);if(p.stockEnabled)Text("Stok ${p.stock}",fontSize=10.sp,color=if(p.stock<=3)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){IconButton(onAdd,modifier=Modifier.size(32.dp)){Icon(Icons.Default.AddCircle,"Tambah",tint=Blue)}}}}}
@Composable private fun Chip(label:String,selected:Boolean,onClick:()->Unit)=Surface(onClick=onClick,shape=RoundedCornerShape(18.dp),color=if(selected)Blue else MaterialTheme.colorScheme.surfaceVariant){Text(label,Modifier.padding(horizontal=14.dp,vertical=7.dp),fontSize=11.sp,fontWeight=FontWeight.SemiBold,color=if(selected)Color.White else MaterialTheme.colorScheme.onSurfaceVariant)}

@Composable private fun CheckoutPage(vm:PosViewModel,cart:List<CartItem>,onDone:()->Unit){var method by remember{mutableStateOf(PaymentMethod.CASH)};val total=cart.sumOf{it.product.price*it.quantity};Column(Modifier.fillMaxSize().padding(16.dp)){Text("Checkout",fontSize=20.sp,fontWeight=FontWeight.Bold);Text("Periksa pesanan sebelum pembayaran",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(12.dp));LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)){items(cart){item->OrderRow(item)}};Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surfaceVariant){Column(Modifier.padding(14.dp)){Text("Metode pembayaran",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(7.dp));Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){Chip("Cash",method==PaymentMethod.CASH){method=PaymentMethod.CASH};Chip("QRIS",method==PaymentMethod.QRIS){method=PaymentMethod.QRIS}};Spacer(Modifier.height(12.dp));Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("Total",fontWeight=FontWeight.SemiBold);Spacer(Modifier.weight(1f));Text(money(total),fontSize=20.sp,fontWeight=FontWeight.Bold)}}};Spacer(Modifier.height(10.dp));Button({vm.checkout(method);onDone()},Modifier.fillMaxWidth().height(48.dp),enabled=cart.isNotEmpty(),shape=RoundedCornerShape(12.dp)){Icon(Icons.Default.Payment,null);Spacer(Modifier.width(7.dp));Text("BAYAR")}}}
@Composable private fun OrderRow(item:CartItem){Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surfaceVariant){Row(Modifier.padding(11.dp),verticalAlignment=Alignment.CenterVertically){Surface(Modifier.size(38.dp),RoundedCornerShape(9.dp),MaterialTheme.colorScheme.primaryContainer){Box(Alignment.Center){Icon(Icons.Default.Image,null,tint=Blue)}};Spacer(Modifier.width(9.dp));Column(Modifier.weight(1f)){Text(item.product.name,fontWeight=FontWeight.SemiBold,fontSize=12.sp);Text("${item.quantity} × ${money(item.product.price)}",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Text(money(item.product.price*item.quantity),fontWeight=FontWeight.Bold,fontSize=12.sp)}}}

@Composable private fun ReportsPage(role:Role,transactions:List<Transaction>,shift:Shift?,page:String,onPage:(String)->Unit){val total=transactions.sumOf{it.total};val cash=transactions.filter{it.paymentMethod==PaymentMethod.CASH}.sumOf{it.total};val qris=transactions.filter{it.paymentMethod==PaymentMethod.QRIS}.sumOf{it.total};Column(Modifier.fillMaxSize().padding(horizontal=16.dp,vertical=8.dp)){if(role==Role.OWNER){LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("Ringkasan","Outlet","Kasir","Harian","Bulanan").forEach{item->item{Chip(item,page==item){onPage(item)}}}};Spacer(Modifier.height(10.dp))};LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){item{if(role==Role.OWNER)Text(if(page=="Ringkasan")"Ringkasan Bisnis" else "Laporan $page",fontSize=20.sp,fontWeight=FontWeight.Bold)else Text("Laporan Hari Ini",fontSize=20.sp,fontWeight=FontWeight.Bold);Text(if(role==Role.OWNER)"Semua hasil penjualan & operasional bisnis" else "Transaksi dan shift outlet ini",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};item{SummaryHero("Penjualan",money(total))};item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ReportStat("Transaksi",transactions.size.toString(),Modifier.weight(1f));ReportStat("Shift",if(shift==null)"0" else "1",Modifier.weight(1f))}};item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ReportStat("Cash",money(cash),Modifier.weight(1f));ReportStat("QRIS",money(qris),Modifier.weight(1f))}};item{SectionTitle("Aktivitas Terbaru")};items(transactions.take(8)){tx->TransactionRow(tx)}}}}
@Composable private fun SummaryHero(label:String,value:String)=Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),color=Blue){Column(Modifier.padding(16.dp)){Text(label,fontSize=11.sp,color=Color.White.copy(alpha=.85f));Text(value,fontSize=25.sp,fontWeight=FontWeight.Bold,color=Color.White)}}
@Composable private fun ReportStat(label:String,value:String,modifier:Modifier)=Surface(modifier,shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surfaceVariant){Column(Modifier.padding(13.dp)){Text(label,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(value,fontSize=16.sp,fontWeight=FontWeight.Bold,maxLines=1)}}
@Composable private fun SectionTitle(s:String)=Text(s,fontSize=14.sp,fontWeight=FontWeight.Bold)
@Composable private fun TransactionRow(tx:Transaction)=Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surfaceVariant){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("#${tx.transactionId.take(6)}",fontSize=12.sp,fontWeight=FontWeight.SemiBold);Text("${tx.paymentMethod} · ${time(tx.createdAt)}",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Text(money(tx.total),fontWeight=FontWeight.Bold,fontSize=12.sp)}}

@Composable private fun SettingsPage(session:Session,page:String?,onOpen:(String)->Unit,onBack:()->Unit,onTheme:()->Unit,onLog:()->Unit,onLogout:()->Unit){if(page!=null){SettingsDetail(page,onBack,session,onTheme,onLog)}else{LazyColumn(Modifier.fillMaxSize().padding(horizontal=16.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){item{Text("Pengaturan",fontSize=20.sp,fontWeight=FontWeight.Bold);Text(if(session.role==Role.OWNER)"Owner · bisnis & outlet" else "Kasir · outlet ${session.outletId?:""}",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};item{SectionTitle("OPERASIONAL")};if(session.role==Role.OWNER){listOf("Produk" to "Kelola produk dan foto","Kategori" to "Buat kategori sendiri","Stok" to "Aktif / nonaktif per produk","Outlet" to "Kelola cabang bisnis","Kasir / Pekerja" to "Akun pekerja outlet","Owner" to "Profil pemilik","Bisnis" to "Informasi bisnis").forEach{item{SettingsRow(item.first,item.second){onOpen(item.first)}}}}else{listOf("Profil" to session.username,"Shift" to "Status shift & penutupan","Printer" to "Printer Bluetooth").forEach{item{SettingsRow(item.first,item.second){onOpen(item.first)}}}};item{SectionTitle("PEMBAYARAN & PERANGKAT")};item{SettingsRow("QRIS","Pembayaran QRIS"){onOpen("QRIS")}};item{SettingsRow("Printer","Bluetooth"){onOpen("Printer")}};item{SectionTitle("APLIKASI")};item{SettingsRow("Tema","Terang / Gelap",onTheme)};item{SettingsRow("Sinkronisasi","Status koneksi")};item{OutlinedButton(onLog,Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp)){Icon(Icons.Default.Download,null);Spacer(Modifier.width(7.dp));Text("Download Log")}};item{OutlinedButton(onLogout,Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp)){Text("KELUAR")}}}}}

@Composable private fun SettingsRow(title:String,value:String,onClick:()->Unit)=Surface(onClick=onClick,Modifier.fillMaxWidth(),shape=RoundedCornerShape(13.dp),color=MaterialTheme.colorScheme.surfaceVariant){Row(Modifier.padding(horizontal=13.dp,vertical=11.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.SemiBold,fontSize=13.sp);Text(value,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Icon(Icons.Default.ChevronRight,null,tint=MaterialTheme.colorScheme.onSurfaceVariant)}}
@Composable private fun SettingsDetail(page:String,onBack:()->Unit,session:Session,onTheme:()->Unit,onLog:()->Unit){Column(Modifier.fillMaxSize().padding(16.dp)){Row(verticalAlignment=Alignment.CenterVertically){IconButton(onBack){Icon(Icons.Default.ArrowBack,"Kembali")};Text(page,fontSize=20.sp,fontWeight=FontWeight.Bold)};Spacer(Modifier.height(10.dp));when(page){"Produk"->ProductManagement();"Kategori"->CategoryManagement();"Stok"->StockManagement();"Outlet"->SimpleManagement("Outlet","Tambah outlet",listOf("Outlet utama"));"Kasir / Pekerja"->SimpleManagement("Kasir / Pekerja","Tambah pekerja",listOf("Belum ada data"));"Owner"->SimpleManagement("Owner","Edit profil",listOf(session.username));"Bisnis"->SimpleManagement("Bisnis","Edit bisnis",listOf(session.businessId?:"Belum diatur"));"QRIS"->SimpleManagement("QRIS","Atur QRIS",listOf("Belum dikonfigurasi"));"Printer"->SimpleManagement("Printer","Hubungkan perangkat",listOf("Belum terhubung"));"Shift"->SimpleManagement("Shift","Kelola shift",listOf("Status shift tersedia di Kasir"));"Profil"->SimpleManagement("Profil","Edit profil",listOf(session.username));else->SimpleManagement(page,"Tambah",listOf("Belum ada data"))}}}
@Composable private fun ProductManagement(){var search by remember{mutableStateOf("")};Column{OutlinedTextField(search,{search=it},Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("Cari produk…")},leadingIcon={Icon(Icons.Default.Search,null)},shape=RoundedCornerShape(12.dp));Spacer(Modifier.height(10.dp));Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.primaryContainer){Text("+ Tambah Produk",Modifier.padding(14.dp),fontWeight=FontWeight.Bold,color=Blue)};Spacer(Modifier.height(8.dp));Text("Produk akan tampil dengan foto, kategori, harga, status aktif, dan status stok.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
@Composable private fun CategoryManagement(){LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){item{Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.primaryContainer){Text("+ Buat Kategori",Modifier.padding(14.dp),fontWeight=FontWeight.Bold,color=Blue)}};item{Text("Kategori dibuat sendiri oleh Owner. Tidak ada daftar kategori yang dipaksakan aplikasi.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
@Composable private fun StockManagement(){Column{Text("Stok per produk",fontWeight=FontWeight.Bold);Spacer(Modifier.height(7.dp));Text("Aktif: jumlah dan batas stok digunakan. Nonaktif: produk tetap dapat dijual tanpa perhitungan stok.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(10.dp));SettingsRow("Stok Aktif","ON / OFF per produk"){};SettingsRow("Peringatan stok","Batas stok menipis"){} }}
@Composable private fun SimpleManagement(title:String,action:String,items:List<String>){Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.primaryContainer){Text("+ $action",Modifier.padding(14.dp),fontWeight=FontWeight.Bold,color=Blue)};items.forEach{SettingsRow(it,"Data contoh / status",{})}}}

@Composable private fun MoneyDialog(title:String,label:String,onDismiss:()->Unit,onConfirm:(Long)->Unit){var value by remember{mutableStateOf("")};AlertDialog(onDismissRequest=onDismiss,title={Text(title)},text={OutlinedTextField(value,{value=it.filter(Char::isDigit)},Modifier.fillMaxWidth(),label={Text(label)},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))},confirmButton={Button({onConfirm(value.toLongOrNull()?:0)}){Text("SIMPAN")}},dismissButton={TextButton(onDismiss){Text("BATAL")}})}
private fun time(ms:Long):String=if(ms<=0)"--:--"else SimpleDateFormat("HH:mm",Locale("id","ID")).format(Date(ms))
