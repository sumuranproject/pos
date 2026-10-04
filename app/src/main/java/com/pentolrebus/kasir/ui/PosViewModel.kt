package com.pentolrebus.kasir.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pentolrebus.kasir.data.OfflineStore
import com.pentolrebus.kasir.data.PosRepository
import com.pentolrebus.kasir.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface AuthState { data object LoggedOut:AuthState; data object Loading:AuthState; data class LoggedIn(val session:Session):AuthState; data class Error(val message:String):AuthState }

class PosViewModel(private val repo:PosRepository, private val offline:OfflineStore) : ViewModel() {
 private val _auth=MutableStateFlow<AuthState>(AuthState.LoggedOut); val auth:StateFlow<AuthState> = _auth
 private val _products=MutableStateFlow<List<Product>>(emptyList()); val products:StateFlow<List<Product>> = _products
 private val _cart=MutableStateFlow<List<CartItem>>(emptyList()); val cart:StateFlow<List<CartItem>> = _cart
 private val _shift=MutableStateFlow<Shift?>(null); val shift:StateFlow<Shift?> = _shift
 private val _transactions=MutableStateFlow<List<Transaction>>(emptyList()); val transactions:StateFlow<List<Transaction>> = _transactions
 private val _lastTransaction=MutableStateFlow<Transaction?>(null); val lastTransaction:StateFlow<Transaction?> = _lastTransaction
 private val _syncing=MutableStateFlow(false); val syncing:StateFlow<Boolean> = _syncing

 fun loginLocal(username:String,pin:String,expectedRole:Role?=null){_auth.value=AuthState.Loading;viewModelScope.launch{repo.localPinLogin(username,pin.toCharArray()).onSuccess{acceptSession(it,expectedRole)}.onFailure{_auth.value=AuthState.Error(it.message?:"Login gagal")}}}
 fun loginEmail(email:String,password:String,expectedRole:Role?=null){_auth.value=AuthState.Loading;viewModelScope.launch{repo.emailLogin(email,password).onSuccess{acceptSession(it,expectedRole)}.onFailure{_auth.value=AuthState.Error(it.message?:"Login gagal")}}}
 fun register(email:String,password:String,username:String,pin:String,business:String?,outlet:String?,whatsapp:String?){_auth.value=AuthState.Loading;viewModelScope.launch{repo.registerOwner(email,password,username,pin.toCharArray(),business,outlet,whatsapp).onSuccess(::loginSuccess).onFailure{_auth.value=AuthState.Error(it.message?:"Registrasi gagal")}}}
 private fun acceptSession(s:Session,e:Role?){if(e!=null&&s.role!=e){_auth.value=AuthState.Error("Akun ini bukan akun ${if(e==Role.OWNER)"Owner" else "Kasir"}.");return};loginSuccess(s)}
 private fun loginSuccess(s:Session){_auth.value=AuthState.LoggedIn(s);viewModelScope.launch{_products.value=repo.loadProducts(s.outletId.orEmpty());mergeLocalAndRemote(s);syncPending(s)}}

 private suspend fun mergeLocalAndRemote(s:Session){
  val localTx=offline.transactions().filter{it.outletId==s.outletId&&it.cashierUid==s.uid||it.outletId==s.outletId&&s.role==Role.OWNER}
  val remote=repo.loadTransactions(s); _transactions.value=(remote+localTx).associateBy{it.transactionId}.values.sortedByDescending{it.createdAt}
  val localActive=offline.shifts().filter{it.outletId==s.outletId&&it.cashierUid==s.uid&&it.closedAt==null}.maxByOrNull{it.startAt}
  _shift.value=localActive ?: repo.loadActiveShift(s)
 }

 fun syncPending(s:Session?=(_auth.value as? AuthState.LoggedIn)?.session){val session=s?:return;viewModelScope.launch{_syncing.value=true;try{
   offline.shifts().filter{it.outletId==session.outletId&&it.syncStatus!=SyncStatus.SYNCED}.forEach{shift->repo.saveShift(shift).onSuccess{offline.updateShiftStatus(shift.id,SyncStatus.SYNCED)}.onFailure{offline.updateShiftStatus(shift.id,SyncStatus.SYNC_ERROR)}}
   offline.transactions().filter{it.outletId==session.outletId&&it.syncStatus!=SyncStatus.SYNCED}.forEach{t->repo.saveTransaction(t).onSuccess{offline.updateTransactionStatus(t.transactionId,SyncStatus.SYNCED)}.onFailure{offline.updateTransactionStatus(t.transactionId,SyncStatus.SYNC_ERROR)}}
   mergeLocalAndRemote(session)
 }finally{_syncing.value=false}}}

 fun add(p:Product){val u=_cart.value.toMutableList();val i=u.indexOfFirst{it.product.id==p.id};if(i>=0)u[i]=u[i].copy(quantity=u[i].quantity+1)else u.add(CartItem(p,1));_cart.value=u}
 fun remove(p:Product){_cart.value=_cart.value.mapNotNull{when{it.product.id!=p.id->it;it.quantity>1->it.copy(quantity=it.quantity-1);else->null}}}
 fun clearCart(){_cart.value=emptyList()}
 fun startShift(openingCash:Long){val s=(_auth.value as? AuthState.LoggedIn)?.session?:return;viewModelScope.launch{val shift=Shift(ownerUid=s.uid,businessId=s.businessId.orEmpty(),outletId=s.outletId.orEmpty(),cashierUid=s.uid,startAt=System.currentTimeMillis(),openingCash=openingCash,syncStatus=SyncStatus.PENDING_SYNC);offline.saveShift(shift);_shift.value=shift;repo.saveShift(shift).onSuccess{offline.updateShiftStatus(shift.id,SyncStatus.SYNCED);_shift.value=shift.copy(syncStatus=SyncStatus.SYNCED)} }}
 fun checkout(method:PaymentMethod,qrisPath:String?=null){val s=(_auth.value as? AuthState.LoggedIn)?.session?:return;val sh=_shift.value?:return;val items=_cart.value.map{TransactionItem(it.product.id,it.product.name,it.product.price,it.quantity,it.product.price*it.quantity)};if(items.isEmpty())return;val total=items.sumOf{it.subtotal};val t=Transaction(ownerUid=s.uid,businessId=s.businessId.orEmpty(),outletId=s.outletId.orEmpty(),shiftId=sh.id,cashierUid=s.uid,items=items,subtotal=total,total=total,paymentMethod=method,qrisProofPath=qrisPath,syncStatus=SyncStatus.PENDING_SYNC);offline.saveTransaction(t);_lastTransaction.value=t;_transactions.value=(_transactions.value.filterNot{it.transactionId==t.transactionId}+t).sortedByDescending{it.createdAt};clearCart();viewModelScope.launch{repo.saveTransaction(t).onSuccess{offline.updateTransactionStatus(t.transactionId,SyncStatus.SYNCED);_lastTransaction.value=t.copy(syncStatus=SyncStatus.SYNCED);_transactions.value=offline.transactions().filter{it.outletId==s.outletId}.sortedByDescending{it.createdAt}}.onFailure{ /* remain PENDING_SYNC for later retry */ }}}
 fun closeShift(closingCash:Long){val s=(_auth.value as? AuthState.LoggedIn)?.session?:return;val sh=_shift.value?:return;viewModelScope.launch{val closed=sh.copy(closedAt=System.currentTimeMillis(),closingCash=closingCash,syncStatus=SyncStatus.PENDING_SYNC);offline.saveShift(closed);_shift.value=null;repo.saveShift(closed).onSuccess{offline.updateShiftStatus(sh.id,SyncStatus.SYNCED)}}}
 fun logout(){if(_shift.value!=null)return;repo.logout();_auth.value=AuthState.LoggedOut;clearCart();_lastTransaction.value=null}
}
