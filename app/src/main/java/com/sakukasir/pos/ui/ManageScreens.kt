package com.sakukasir.pos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sakukasir.pos.domain.*

@Composable fun ManageScreen(vm:PosViewModel){
    val products by vm.products.collectAsState();val cats by vm.categories.collectAsState();var tab by remember{mutableIntStateOf(0)};var add by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxSize().padding(16.dp)){ScrollableTabRow(tab){listOf("Produk","Kategori","Stok","Outlet","Kasir").forEachIndexed{i,t->Tab(i==tab,{tab=i},text={Text(t)})}}
        when(tab){
            0->Column{Row(Modifier.fillMaxWidth().padding(vertical=10.dp),horizontalArrangement=Arrangement.End){SkButton("+ Tambah produk",{add=true})};LazyColumn{items(products){p->ListItem(headlineContent={Text(p.name)},supportingContent={Text("${rupiah(p.price)} · ${p.unit} · ${p.category}")},trailingContent={SkStockBadge(p.stock,p.lowStock)})}}}
            1->cats.forEach{ListItem(headlineContent={Text(it.name)},trailingContent={Switch(it.active,{})})}
            2->products.filter{it.trackStock}.forEach{p->ListItem(headlineContent={Text(p.name)},supportingContent={Text("Batas rendah ${p.lowStock}")},trailingContent={Text(p.stock.toString())})}
            3->ListItem(headlineContent={Text("Toko Berkah")},supportingContent={Text("Jl. Merdeka 12")})
            4->Column{Text("Kasir demo",style=MaterialTheme.typography.titleMedium);listOf("Andi Wijaya","Siti Aminah","Rudi Hartono").forEach{ListItem(headlineContent={Text(it)},supportingContent={Text("CASHIER")})}}
        }
    }
    if(add) AddProductDialog(vm){add=false}
} 

@Composable private fun AddProductDialog(vm:PosViewModel,onDismiss:()->Unit){
    var name by remember{mutableStateOf("")};var price by remember{mutableLongStateOf(0)};var unit by remember{mutableStateOf("porsi")};var category by remember{mutableStateOf("Makanan")};var stock by remember{mutableIntStateOf(0)};var low by remember{mutableIntStateOf(5)}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Produk baru")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        SkTextField(name,{name=it},"Nama produk");SkRupiahField(price,{price=it},"Harga");SkTextField(unit,{unit=it},"Satuan");SkTextField(category,{category=it},"Kategori");SkTextField(stock.toString(),{stock=it.toIntOrNull()?:0},"Stok");SkTextField(low.toString(),{low=it.toIntOrNull()?:5},"Batas stok rendah")
    }},confirmButton={TextButton(enabled=name.isNotBlank()&&price>0,onClick={vm.addProduct(Product((vm.products.value.maxOfOrNull{it.id}?:0)+1,name,price,unit,category,stock,low));onDismiss()}){Text("Tambah")}},dismissButton={TextButton(onClick=onDismiss){Text("Batal")}})
}
@Composable fun ExpenseScreen(vm:PosViewModel){
    val ex by vm.expenses.collectAsState();var amount by remember{mutableLongStateOf(0)};var cat by remember{mutableStateOf("Bahan")};var note by remember{mutableStateOf("")}
    Column(Modifier.fillMaxSize().padding(16.dp)){Text("Pengeluaran",style=MaterialTheme.typography.headlineSmall);SkKpiHero("Total",rupiah(ex.sumOf{it.amount}));SkRupiahField(amount,{amount=it},"Jumlah");Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("Bahan","Operasional","Gaji","Lainnya").forEach{SkChip(it,cat==it){cat=it}}};SkTextField(note,{note=it},"Catatan");SkButton("Tambah",{if(amount>0){vm.addExpense(amount,cat,note);amount=0;note=""}},block=true);ex.forEach{ListItem(headlineContent={Text(it.category)},supportingContent={Text(it.note)},trailingContent={Column{Text(rupiah(it.amount));TextButton({vm.deleteExpense(it.id)}){Text("Hapus")}}})}}
}
@Composable fun ShiftScreen(vm:PosViewModel){
    val shift by vm.shift.collectAsState();var opening by remember{mutableLongStateOf(0)};var closing by remember{mutableLongStateOf(0)}
    Column(Modifier.fillMaxSize().padding(16.dp)){Text("Shift",style=MaterialTheme.typography.headlineSmall);if(shift==null){SkRupiahField(opening,{opening=it},"Kas awal");SkButton("Mulai shift",{vm.startShift(opening)},block=true)}else{SkKpiCompact(listOf("Kas awal" to rupiah(shift!!.openingCash),"Transaksi" to shift!!.transactionCount.toString()));Text("Shift aktif sejak ${java.text.SimpleDateFormat("HH:mm").format(java.util.Date(shift!!.startAt))}");SkRupiahField(closing,{closing=it},"Uang fisik");SkButton("Tutup shift",{vm.closeShift(closing)},block=true)}}
}
@Composable fun ProfileScreen(vm:PosViewModel){val u=vm.state.collectAsState().value.user!!;Column(Modifier.fillMaxSize().padding(16.dp)){Text("Profil",style=MaterialTheme.typography.headlineSmall);SkTextField(u.displayName,{},"Nama");SkTextField(u.username,{},"Username");SkTextField(if(u.role==Role.OWNER)"Owner" else "Kasir",{},"Role");SkTextField(u.outlet,{},"Outlet")}}
@Composable fun AuditScreen(vm:PosViewModel){val a by vm.audit.collectAsState();Column(Modifier.fillMaxSize().padding(16.dp)){Text("Audit Log",style=MaterialTheme.typography.headlineSmall);if(a.isEmpty())SkEmptyState("Belum ada audit","Void dan refund akan muncul di sini.") else LazyColumn{items(a){SkAuditRow(it)}}}}
@Composable fun NotificationScreen(vm:PosViewModel){val n by vm.notifications.collectAsState();Column(Modifier.fillMaxSize().padding(16.dp)){Text("Notifikasi",style=MaterialTheme.typography.headlineSmall);n.forEach{ListItem(headlineContent={Text(it.title)},supportingContent={Text(it.body)},trailingContent={if(!it.read)SkBadge("Baru")})}}}
@Composable fun ThemeScreen(vm:PosViewModel){val dark=vm.state.collectAsState().value.darkTheme;Column(Modifier.fillMaxSize().padding(16.dp)){Text("Tema",style=MaterialTheme.typography.headlineSmall);ListItem(headlineContent={Text("Dark mode")},trailingContent={Switch(dark,{vm.toggleTheme()})})}}
@Composable fun SecurityScreen(vm:PosViewModel){val set by vm.settings.collectAsState();var win by remember(set){mutableIntStateOf(set.security.voidWindowMinutes)};var limit by remember(set){mutableLongStateOf(set.security.voidLimitCashier)};Column(Modifier.fillMaxSize().padding(16.dp)){Text("Keamanan",style=MaterialTheme.typography.headlineSmall);SkTextField(win.toString(),{win=it.toIntOrNull()?:5},"Window void kasir (menit)");SkRupiahField(limit,{limit=it},"Limit void kasir");SkButton("Simpan",{vm.updateSettings(set.copy(security=set.security.copy(voidWindowMinutes=win,voidLimitCashier=limit)))},block=true)}}

@Composable fun PrinterScreen(vm:PosViewModel){
    Column(Modifier.fillMaxSize().padding(16.dp)){Text("Printer Bluetooth",style=MaterialTheme.typography.headlineSmall);Text("Format struk 58mm · Bluetooth Classic SPP / BLE");Spacer(Modifier.height(12.dp));SkCard(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text("SK-Printer-58mm");Text("Status dikelola oleh BluetoothPrinter");SkButton("Scan perangkat",{}) ;SkButton("Test print",{})}}}
}
