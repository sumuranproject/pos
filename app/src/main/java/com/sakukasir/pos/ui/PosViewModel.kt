package com.sakukasir.pos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakukasir.pos.data.PosRepository
import com.sakukasir.pos.domain.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class PosUiState(
    val user: User? = null,
    val cart: List<CartItem> = emptyList(),
    val discount: Long = 0,
    val taxPct: Int = 0,
    val selectedCategory: String = "Semua",
    val search: String = "",
    val darkTheme: Boolean = false,
    val selectedOutlet: String = "Toko Berkah",
    val notice: String? = null,
    val simulateOffline: Boolean = false,
    val reportPeriod: String = "all",
    val reportUser: String = "all",
    val reportFrom: String = "",
    val reportTo: String = ""
)

class PosViewModel(private val repo: PosRepository): ViewModel() {
    private val _state=MutableStateFlow(PosUiState())
    val state=_state.asStateFlow()
    val products=repo.products
    val categories=repo.categories
    val transactions=repo.transactions
    val expenses=repo.expenses
    val audit=repo.audit
    val notifications=repo.notifications
    val settings=repo.settings
    val shift=repo.activeShift
    val syncQueue=repo.syncQueue
    val outlets=repo.outlets
    val workers=repo.workers
    val shiftHistory=repo.shiftHistory

    fun login(username:String,password:String) {
        val user=when(username.lowercase()) {
            "owner" -> User("owner","Budi Santoso",Role.OWNER,"Toko Berkah")
            "kasir" -> User("kasir","Andi Wijaya",Role.CASHIER,"Toko Berkah",Permission.entries.toSet())
            "kasir2" -> User("kasir2","Siti Aminah",Role.CASHIER,"Cabang Pasar",setOf(Permission.POS,Permission.TRANSACTIONS,Permission.SHIFT,Permission.PRINTER,Permission.SYNC,Permission.THEME,Permission.PROFILE))
            "kasir3" -> User("kasir3","Rudi Hartono",Role.CASHIER,"Toko Berkah",setOf(Permission.POS,Permission.PRINTER,Permission.THEME,Permission.PROFILE))
            else -> null
        }
        if(user==null || (username.lowercase()!="owner" && password!="kasir123")) {
            _state.update{it.copy(notice="Username atau password salah.")}; return
        }
        _state.update{it.copy(user=user,selectedOutlet=user.outlet,notice=null)}
    }
    fun logout(){_state.value=PosUiState()}
    fun setSearch(v:String)=_state.update{it.copy(search=v)}
    fun setCategory(v:String)=_state.update{it.copy(selectedCategory=v)}
    fun setDiscount(v:Long)=_state.update{it.copy(discount=v)}
    fun setTax(v:Int)=_state.update{it.copy(taxPct=v.coerceIn(0,100))}
    fun setReportFilter(period:String,user:String,from:String,to:String)=_state.update{it.copy(reportPeriod=period,reportUser=user,reportFrom=from,reportTo=to)}
    fun toggleOffline(){_state.update{it.copy(simulateOffline=!it.simulateOffline)}}
    fun toggleTheme(){_state.update{it.copy(darkTheme=!it.darkTheme)}}
    fun selectOutlet(name:String){_state.update{it.copy(selectedOutlet=name)}}
    fun addToCart(p:Product){_state.update{s-> val found=s.cart.find{it.productId==p.id};s.copy(cart=if(found==null)s.cart+CartItem(p.id,p.name,p.price,1,p.unit) else s.cart.map{if(it.productId==p.id)it.copy(qty=it.qty+1)else it})}}
    fun changeQty(id:Int,delta:Int){_state.update{s->s.copy(cart=s.cart.mapNotNull{if(it.productId!=id)it else {val q=it.qty+delta;if(q<=0)null else it.copy(qty=q)}})}}
    fun clearCart(){_state.update{it.copy(cart=emptyList(),discount=0,taxPct=0)}}
    fun nextTransactionId(): String {
        val date = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val prefix = "TRX-$date-"
        val next = (repo.transactions.value.mapNotNull {
            if (it.id.startsWith(prefix)) it.id.removePrefix(prefix).toIntOrNull() else null
        }.maxOrNull() ?: 0) + 1
        return prefix + next.toString().padStart(4, '0')
    }

    fun totals():Totals {val s=_state.value;val sub=s.cart.sumOf{it.price*it.qty};val d=s.discount.coerceIn(0,sub);val after=sub-d;val tax=after*s.taxPct/100;return Totals(sub,d,after,tax,after+tax,s.taxPct)}
    fun checkout(method:PaymentMethod,received:Long,qrisProof:QrisProof?,transactionId:String?=null,onDone:(Transaction)->Unit) {
        val s=_state.value;val t=totals();if(s.cart.isEmpty())return
        if(method==PaymentMethod.CASH && received<t.total){_state.update{it.copy(notice="Uang diterima kurang dari total.")};return}
        if(method==PaymentMethod.QRIS && qrisProof==null){_state.update{it.copy(notice="Foto bukti QRIS wajib.")};return}
        if(method==PaymentMethod.QRIS && qrisProof!=null) {
            val nowCheck=System.currentTimeMillis()
            if(qrisProof.capturedAt > nowCheck || nowCheck - qrisProof.capturedAt > 5*60_000L || nowCheck >= qrisProof.expiredAt || qrisProof.localPath.isBlank() || !java.io.File(qrisProof.localPath).isFile) {
                _state.update{it.copy(notice="Bukti QRIS tidak valid atau sudah kedaluwarsa. Ambil foto ulang.")};return
            }
        }
        val now=System.currentTimeMillis();val date=SimpleDateFormat("yyyyMMdd",Locale.US).format(Date(now))
        val id=transactionId?.takeIf { it.startsWith("TRX-$date-") } ?: nextTransactionId()
        val tx=Transaction(id,now,SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date(now)),SimpleDateFormat("HH:mm",Locale.US).format(Date(now)),s.cart,t.total,method,s.user!!.displayName,s.user.username,s.selectedOutlet,t.discount,t.tax,t.taxPct,received,(received-t.total).coerceAtLeast(0),qrisProof=qrisProof)
        viewModelScope.launch {repo.addTransaction(tx);repo.addNotification(AppNotification(now,"transaction","Transaksi baru","${s.user.displayName} · ${rupiah(tx.total)}",now));clearCart();onDone(tx)}
    }
    fun voidTransaction(tx:Transaction,reason:String,pin:String?=null,onDone:(Boolean,String)->Unit) {
        val s=_state.value; if(s.user==null){onDone(false,"Belum login");return}
        if(tx.status!=TransactionStatus.COMPLETED){onDone(false,"Transaksi tidak eligible.");return}
        val owner=s.user.role==Role.OWNER
        if(!owner && tx.cashierId!=s.user.username){onDone(false,"Hanya transaksi sendiri.");return}
        if(!owner && System.currentTimeMillis()-tx.timestamp>settings.value.security.voidWindowMinutes*60_000L){onDone(false,"Window void kasir hanya ${settings.value.security.voidWindowMinutes} menit. Hubungi owner.");return}
        if(!owner && tx.total>settings.value.security.voidLimitCashier && pin!=settings.value.security.ownerPin){onDone(false,"Void di atas limit membutuhkan PIN owner.");return}
        val updated=tx.copy(status=TransactionStatus.VOID,syncStatus=SyncStatus.PENDING_SYNC)
        viewModelScope.launch {
            repo.updateTransaction(updated)
            repo.addAudit(AuditEntry("AUD-${System.currentTimeMillis()}", "VOID",System.currentTimeMillis(),tx.id,tx.total,true,s.user.username,s.user.displayName,reason,"Android"))
            repo.addNotification(AppNotification(System.currentTimeMillis(),"void","Void transaksi","${s.user.displayName} void ${tx.id} · ${rupiah(tx.total)}",System.currentTimeMillis()))
            onDone(true,"Transaksi berhasil di-void.")
        }
    }
    fun refundTransaction(tx:Transaction,items:List<RefundItem>,method:RefundMethod,reason:String,pin:String?=null,onDone:(Boolean,String)->Unit) {
        val s=_state.value; val owner=s.user?.role==Role.OWNER
        if(s.user==null){onDone(false,"Belum login");return}
        if(items.isEmpty()){onDone(false,"Pilih item refund.");return}
        if(tx.status==TransactionStatus.VOID || tx.status==TransactionStatus.REFUNDED){onDone(false,"Transaksi tidak eligible.");return}
        if(!owner && tx.cashierId!=s.user.username){onDone(false,"Hanya transaksi sendiri.");return}
        val alreadyRefunded=tx.refundedItems.groupBy{it.productId}.mapValues{(_,rows)->rows.sumOf{it.qty}}
        val originalById=tx.items.associateBy{it.productId}
        if(items.any { it.qty<=0 || it.amount<=0L || it.qty > (originalById[it.productId]?.qty ?: 0) - (alreadyRefunded[it.productId] ?: 0) }){onDone(false,"Jumlah refund melebihi item yang tersedia.");return}
        val amount=items.sumOf{it.amount}
        val remaining=tx.total-tx.refundAmount
        if(amount<=0L || amount>remaining){onDone(false,"Nominal refund melebihi sisa transaksi.");return}
        if(!owner && amount>settings.value.security.refundLimitCashier && pin!=settings.value.security.ownerPin){onDone(false,"Refund di atas limit membutuhkan PIN owner.");return}
        val full=amount>=remaining
        val updated=tx.copy(status=if(full)TransactionStatus.REFUNDED else TransactionStatus.PARTIAL_REFUND,refundAmount=tx.refundAmount+amount,refundMethod=method,refundReason=reason,refundedAt=System.currentTimeMillis(),refundedBy=s.user?.displayName,refundedItems=tx.refundedItems+items)
        viewModelScope.launch {
            repo.updateTransaction(updated)
            repo.addAudit(AuditEntry("AUD-${System.currentTimeMillis()}","REFUND",System.currentTimeMillis(),tx.id,amount,full,s.user!!.username,s.user.displayName,reason,"Android"))
            repo.addNotification(AppNotification(System.currentTimeMillis(),"refund","Refund transaksi","${s.user.displayName} refund ${tx.id} · ${rupiah(amount)}",System.currentTimeMillis()))
            onDone(true,"Refund berhasil.")
        }
    }
    fun startShift(opening:Long){val s=_state.value; if(s.user==null)return; viewModelScope.launch{repo.setShift(Shift("SH-${System.currentTimeMillis().toString().takeLast(4)}",System.currentTimeMillis(),openingCash=opening,cashierId=s.user.username))}}
    fun closeShift(closing:Long){
        val sh=shift.value?:return
        val tx=transactions.value.filter{it.cashierId==_state.value.user?.username&&it.timestamp>=sh.startAt&&it.status==TransactionStatus.COMPLETED}
        val cash=tx.filter{it.method==PaymentMethod.CASH}.sumOf{it.total};val qris=tx.filter{it.method==PaymentMethod.QRIS}.sumOf{it.total}
        val expected=sh.openingCash+cash
        val fmt=SimpleDateFormat("HH:mm",Locale.US)
        viewModelScope.launch{
            repo.addShiftHistory(ShiftSummary(sh.id,fmt.format(Date(sh.startAt)),fmt.format(Date()),cash,qris,tx.size,closing-expected))
            repo.setShift(null)
        }
    }
    fun expectedCash():Long{val sh=shift.value?:return 0L;val cash=transactions.value.filter{it.cashierId==_state.value.user?.username&&it.timestamp>=sh.startAt&&it.status==TransactionStatus.COMPLETED&&it.method==PaymentMethod.CASH}.sumOf{it.total};return sh.openingCash+cash}
    fun addExpense(amount:Long,cat:String,note:String){val s=_state.value;viewModelScope.launch{repo.addExpense(Expense("EXP-${System.currentTimeMillis()}",amount,cat,note,System.currentTimeMillis(),s.user?.displayName?:"-"))}}
    fun deleteExpense(id:String)=viewModelScope.launch{repo.deleteExpense(id)}
    fun sync()=viewModelScope.launch{repo.sync()}
    fun markNotificationsRead()=viewModelScope.launch{repo.markNotificationsRead()}
    fun deleteProduct(id:Int)=viewModelScope.launch{repo.deleteProduct(id)}
    fun upsertCategory(c:Category)=viewModelScope.launch{repo.upsertCategory(c)}
    fun deleteCategory(id:Int)=viewModelScope.launch{repo.deleteCategory(id)}
    fun upsertOutlet(o:Outlet)=viewModelScope.launch{repo.upsertOutlet(o)}
    fun deleteOutlet(id:Int)=viewModelScope.launch{repo.deleteOutlet(id)}
    fun upsertWorker(w:Worker)=viewModelScope.launch{repo.upsertWorker(w)}
    fun updateExpense(e:Expense)=viewModelScope.launch{repo.updateExpense(e)}
    fun addProduct(p:Product)=viewModelScope.launch{repo.addProduct(p)}
    fun updateProduct(p:Product)=viewModelScope.launch{repo.updateProduct(p)}
    fun updateSettings(settings:AppSettings)=viewModelScope.launch{repo.updateSettings(settings)}
    fun setNotice(v:String?)=_state.update{it.copy(notice=v)}
}
