package com.sakukasir.pos.ui

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.sakukasir.pos.domain.*
import com.sakukasir.pos.domain.QrisProof as QrisProofModel
import com.sakukasir.pos.util.QrisProof as QrisProofProcessor
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun PosScreen(vm:PosViewModel) {
    val state by vm.state.collectAsState();val products by vm.products.collectAsState();val cats by vm.categories.collectAsState()
    var cartOpen by remember{mutableStateOf(false)};var checkout by remember{mutableStateOf(false)}
    val filtered=products.filter{it.active && (state.selectedCategory=="Semua"||it.category==state.selectedCategory)&&it.name.contains(state.search,true)}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=16.dp, vertical=18.dp)) {
        OutlinedTextField(state.search,vm::setSearch,leadingIcon={Icon(Icons.Default.Search,null)},placeholder={Text("Cari produk…")},singleLine=true,modifier=Modifier.fillMaxWidth().height(58.dp),shape=RoundedCornerShape(16.dp),colors=OutlinedTextFieldDefaults.colors(unfocusedContainerColor=MaterialTheme.colorScheme.surface,focusedContainerColor=MaterialTheme.colorScheme.surface))
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical=14.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){SkChip("Semua",state.selectedCategory=="Semua"){vm.setCategory("Semua")};cats.forEach{c->SkChip(c.name,state.selectedCategory==c.name){vm.setCategory(c.name)}}}
        if(filtered.isEmpty()) SkEmptyState("Produk tidak ditemukan","Coba kata kunci atau kategori lain.")
        else LazyVerticalGrid(columns=GridCells.Fixed(2),modifier=Modifier.heightIn(min=200.dp,max=700.dp),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalArrangement=Arrangement.spacedBy(14.dp),userScrollEnabled=false){items(filtered){p->SkProductCard(p.name,p.price,p.unit,p.stock,p.lowStock){vm.addToCart(p)}}}
        Spacer(Modifier.height(12.dp))
        SkCartBar(state.cart.sumOf{it.qty},rupiah(vm.totals().total)){cartOpen=true}
    }
    if(cartOpen) CartSheet(vm,onCheckout={cartOpen=false;checkout=true},onDismiss={cartOpen=false})
    if(checkout) CheckoutSheet(vm,onDismiss={checkout=false})
}

@Composable private fun CartSheet(vm:PosViewModel,onCheckout:()->Unit,onDismiss:()->Unit) {
    val s by vm.state.collectAsState();val t=vm.totals()
    SkSheet(true,onDismiss){Column(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=10.dp)){
        Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center){Box(Modifier.size(width=72.dp,height=5.dp).background(MaterialTheme.colorScheme.outline.copy(alpha=.45f),RoundedCornerShape(10.dp)))}
        Spacer(Modifier.height(18.dp));Text("Cart · ${s.cart.sumOf{it.qty}} item",style=MaterialTheme.typography.titleLarge.copy(fontWeight=FontWeight.SemiBold));Text("Tap +/- untuk ubah jumlah",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodyLarge);Spacer(Modifier.height(18.dp))
        s.cart.forEach{item->Row(Modifier.fillMaxWidth().padding(vertical=9.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(item.name,style=MaterialTheme.typography.titleMedium);Text("${rupiah(item.price)} × ${item.qty}",color=MaterialTheme.colorScheme.onSurfaceVariant)};Column(horizontalAlignment=Alignment.End){Text(rupiah(item.price*item.qty),style=MaterialTheme.typography.titleMedium.copy(fontWeight=FontWeight.Bold));SkQtyControl(item.qty,{vm.changeQty(item.productId,-1)},{vm.changeQty(item.productId,1)})}}}
        HorizontalDivider(Modifier.padding(vertical=8.dp));Row(Modifier.fillMaxWidth().padding(vertical=8.dp),horizontalArrangement=Arrangement.SpaceBetween){Text("Subtotal",style=MaterialTheme.typography.titleMedium);Text(rupiah(t.subtotal),style=MaterialTheme.typography.titleMedium)}
        Spacer(Modifier.height(8.dp));SkButton("Kosongkan cart",{vm.clearCart()},modifier=Modifier.fillMaxWidth(),block=true);Spacer(Modifier.height(10.dp));SkButton("Bayar · ${rupiah(t.total)}",onCheckout,modifier=Modifier.fillMaxWidth(),block=true)
    }}
}

@Composable private fun CheckoutSheet(vm:PosViewModel,onDismiss:()->Unit) {
    val s by vm.state.collectAsState();val t=vm.totals();var method by remember{mutableStateOf(PaymentMethod.CASH)};var received by remember{mutableLongStateOf(t.total)};var proof by remember{mutableStateOf<QrisProofModel?>(null)};var captureStartedAt by remember{mutableLongStateOf(0L)};var cameraFile by remember{mutableStateOf<File?>(null)};val context=LocalContext.current;val trxId=remember{vm.nextTransactionId()};val retentionDays=vm.settings.collectAsState().value.qrisRetentionDays
    val scope=rememberCoroutineScope()
    val takePicture=rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()){ok->
        if(ok&&cameraFile!=null){
            scope.launch{
                val capturedAt = captureStartedAt.takeIf { it > 0L } ?: System.currentTimeMillis()
                proof=QrisProofProcessor.processCapture(context,cameraFile!!,s.user!!.displayName,s.selectedOutlet,trxId,capturedAt,retentionDays)
            }
        }
    }
    val requestCamera=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->
        if(granted){
            val file=cameraFile ?: File(context.cacheDir,"qris_capture").apply{mkdirs()}
                .let{dir->File(dir,"capture_${System.currentTimeMillis()}.jpg")}
            cameraFile=file
            captureStartedAt=System.currentTimeMillis()
            takePicture.launch(FileProvider.getUriForFile(context,context.packageName+".fileprovider",file))
        }
    }
    SkSheet(true,onDismiss){Column(Modifier.fillMaxWidth().padding(16.dp)){Text("Pembayaran",style=MaterialTheme.typography.titleLarge);Text("Total ${rupiah(t.total)}",style=MaterialTheme.typography.headlineSmall);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){SkChip("Cash",method==PaymentMethod.CASH){method=PaymentMethod.CASH};SkChip("QRIS",method==PaymentMethod.QRIS){method=PaymentMethod.QRIS}}
        if(method==PaymentMethod.CASH){SkRupiahField(received,{received=it},"Uang diterima");Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(t.total,50000,100000).distinct().forEach{v->AssistChip(onClick={received=v},label={Text(if(v==t.total)"Pas" else rupiah(v).removePrefix("Rp "))})}};Text("Kembalian ${rupiah((received-t.total).coerceAtLeast(0))}")}else{
            SkCard(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.QrCode2,null,Modifier.size(120.dp));Text("Scan QRIS outlet");Text("Bukti pembayaran wajib dari kamera",color=MaterialTheme.colorScheme.onSurfaceVariant);if(proof!=null)SkBadge("Bukti siap",Color(0xFF16A34A));SkButton("Ambil Foto Bukti",{
                if(context.checkSelfPermission(android.Manifest.permission.CAMERA)==android.content.pm.PackageManager.PERMISSION_GRANTED){
                    val file=cameraFile ?: File(context.cacheDir,"qris_capture").apply{mkdirs()}
                        .let{dir->File(dir,"capture_${System.currentTimeMillis()}.jpg")}
                    cameraFile=file
                    takePicture.launch(FileProvider.getUriForFile(context,context.packageName+".fileprovider",file))
                }else requestCamera.launch(android.Manifest.permission.CAMERA)
            })}}
        }
        Spacer(Modifier.height(10.dp));SkButton("Bayar",onClick={vm.checkout(method,received,proof,trxId){onDismiss()}},enabled=method==PaymentMethod.CASH&&received>=t.total || method==PaymentMethod.QRIS&&proof!=null,block=true)
    }}
}

@Composable
fun TransactionScreen(vm:PosViewModel) {
    val tx by vm.transactions.collectAsState();val user=vm.state.collectAsState().value.user!!
    var selected by remember{mutableStateOf<Transaction?>(null)}
    val visible=tx.filter{user.role==Role.OWNER||it.cashierId==user.username}
    Column(Modifier.fillMaxSize().padding(16.dp)){Text("Transaksi",style=MaterialTheme.typography.headlineSmall);Text("${visible.size} transaksi",color=MaterialTheme.colorScheme.onSurfaceVariant);LazyColumn{items(visible.size){SkTransactionItem(visible[it]){selected=visible[it]}}}}
    selected?.let{TransactionDetail(vm,it){selected=null}}
}
@Composable private fun TransactionDetail(vm:PosViewModel,tx:Transaction,onDismiss:()->Unit){
    val user=vm.state.collectAsState().value.user!!;var voidReason by remember{mutableStateOf("Salah input")};var refundReason by remember{mutableStateOf("Barang rusak")}
    var showVoid by remember{mutableStateOf(false)};var showRefund by remember{mutableStateOf(false)}
    SkSheet(true,onDismiss){Column(Modifier.fillMaxWidth().padding(16.dp)){Text(tx.id,style=trxMonoStyle(FontWeight.Medium, 18.sp));Text(rupiah(tx.total),style=MaterialTheme.typography.headlineSmall);Text("${tx.cashier} · ${tx.outlet}");Text("Status: ${tx.status}");tx.items.forEach{Text("${it.name} × ${it.qty} · ${rupiah(it.price*it.qty)}")};if(tx.qrisProof!=null){val expired=System.currentTimeMillis()>=tx.qrisProof.expiredAt;Text(if(expired)"Bukti QRIS kadaluarsa (retensi ${((tx.qrisProof.expiredAt-tx.qrisProof.capturedAt)/(24L*60*60*1000))} hari)" else "Bukti QRIS tersedia")};if(tx.status==TransactionStatus.COMPLETED){if(user.can(Permission.VOID))SkButton("Void",{showVoid=true},block=true);if(user.can(Permission.REFUND))SkButton("Refund",{showRefund=true},block=true)}}}
    if(showVoid){SkModal(true,{showVoid=false},"Alasan Void"){Text("Pilih alasan");Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf("Salah input","Customer batal","Double entry","Lainnya").forEach{SkChip(it,voidReason==it){voidReason=it}}};SkButton("Konfirmasi",onClick={vm.voidTransaction(tx,voidReason){_,_->showVoid=false;onDismiss()}})}}
    if(showRefund){SkModal(true,{showRefund=false},"Refund"){Text("Refund seluruh transaksi demo");Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf("Barang rusak","Salah pesan","Komplain","Lainnya").forEach{SkChip(it,refundReason==it){refundReason=it}}};SkButton("Konfirmasi",onClick={vm.refundTransaction(tx,tx.items.map{RefundItem(it.productId,it.name,it.qty,it.price*it.qty)},RefundMethod.CASH,refundReason){_,_->showRefund=false;onDismiss()}})}}
}
