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
 private val _categories=MutableStateFlow<List<Category>>(emptyList()); val categories:StateFlow<List<Category>> = _categories
 private val _outlets=MutableStateFlow<List<Outlet>>(emptyList()); val outlets:StateFlow<List<Outlet>> = _outlets
 private val _workers=MutableStateFlow<List<Worker>>(emptyList()); val workers:StateFlow<List<Worker>> = _workers
 private val _business=MutableStateFlow<Business?>(null); val business:StateFlow<Business?> = _business
 private val _expenses=MutableStateFlow<List<Expense>>(emptyList()); val expenses:StateFlow<List<Expense>> = _expenses
 private val _shifts=MutableStateFlow<List<Shift>>(emptyList()); val shifts:StateFlow<List<Shift>> = _shifts
 private val _activeOutlet=MutableStateFlow<String?>(null); val activeOutlet:StateFlow<String?> = _activeOutlet
 private val _message=MutableStateFlow<String?>(null); val message:StateFlow<String?> = _message
 fun consumeMessage(){_message.value=null}
 private fun session():Session?=(_auth.value as? AuthState.LoggedIn)?.session
 private fun ownerOf(s:Session)=s.ownerUid?:s.uid
 private fun outletOf(s:Session)=_activeOutlet.value?:s.outletId.orEmpty()

 fun loginLocal(username:String,pin:String,expectedRole:Role?=null){_auth.value=AuthState.Loading;viewModelScope.launch{repo.localPinLogin(username,pin.toCharArray()).onSuccess{acceptSession(it,expectedRole)}.onFailure{_auth.value=AuthState.Error(it.message?:"Login gagal")}}}
 fun loginEmail(email:String,password:String,expectedRole:Role?=null){_auth.value=AuthState.Loading;viewModelScope.launch{repo.emailLogin(email,password).onSuccess{acceptSession(it,expectedRole)}.onFailure{_auth.value=AuthState.Error(it.message?:"Login gagal")}}}
 fun register(email:String,password:String,username:String,pin:String,business:String?,outlet:String?,whatsapp:String?){_auth.value=AuthState.Loading;viewModelScope.launch{repo.registerOwner(email,password,username,pin.toCharArray(),business,outlet,whatsapp).onSuccess(::loginSuccess).onFailure{_auth.value=AuthState.Error(it.message?:"Registrasi gagal")}}}
 private fun acceptSession(s:Session,e:Role?){if(e!=null&&s.role!=e){_auth.value=AuthState.Error("Akun ini bukan akun ${if(e==Role.OWNER)"Owner" else "Kasir"}.");return};loginSuccess(s)}
 private fun loginSuccess(s:Session){_auth.value=AuthState.LoggedIn(s);_activeOutlet.value=s.outletId;viewModelScope.launch{loadMaster(s);syncPending(s)}}

 private suspend fun loadMaster(s:Session){
  val outlets=repo.loadOutlets(s);_outlets.value=outlets
  val oid=_activeOutlet.value?:s.outletId?:outlets.firstOrNull()?.id
  _activeOutlet.value=oid
  if(oid!=null){_products.value=repo.loadProducts(oid);_categories.value=repo.loadCategories(oid)}
  _business.value=repo.loadBusiness(s)
  if(s.role==Role.OWNER)_workers.value=repo.loadWorkers(s)
  mergeLocalAndRemote(s)
 }

 private suspend fun mergeLocalAndRemote(s:Session){
  val ids=if(s.role==Role.OWNER)_outlets.value.map{it.id}.ifEmpty{listOfNotNull(s.outletId)} else listOfNotNull(s.outletId)
  val remoteTx=ids.flatMap{repo.loadOutletTransactions(it)}
  val localTx=offline.transactions().filter{it.outletId in ids&&(s.role==Role.OWNER||it.cashierUid==s.uid)}
  _transactions.value=(remoteTx+localTx).associateBy{it.transactionId}.values.sortedByDescending{it.createdAt}
  val remoteSh=ids.flatMap{repo.loadOutletShifts(it)}
  val localSh=offline.shifts().filter{it.outletId in ids}
  _shifts.value=(remoteSh+localSh).associateBy{it.id}.values.sortedByDescending{it.startAt}
  val remoteEx=ids.flatMap{repo.loadExpenses(it)}
  val localEx=offline.expenses().filter{it.outletId in ids}
  _expenses.value=(remoteEx+localEx).associateBy{it.id}.values.sortedByDescending{it.createdAt}
  val localActive=offline.shifts().filter{it.outletId==outletOf(s)&&it.cashierUid==s.uid&&it.closedAt==null}.maxByOrNull{it.startAt}
  _shift.value=localActive ?: repo.loadActiveShift(s)
 }

 fun syncPending(s:Session?=(_auth.value as? AuthState.LoggedIn)?.session){val session=s?:return;viewModelScope.launch{_syncing.value=true;try{
   offline.shifts().filter{(it.outletId==session.outletId||it.outletId==_activeOutlet.value)&&it.syncStatus!=SyncStatus.SYNCED}.forEach{shift->repo.saveShift(shift).onSuccess{offline.updateShiftStatus(shift.id,SyncStatus.SYNCED)}.onFailure{offline.updateShiftStatus(shift.id,SyncStatus.SYNC_ERROR)}}
   offline.transactions().filter{(it.outletId==session.outletId||it.outletId==_activeOutlet.value)&&it.syncStatus!=SyncStatus.SYNCED}.forEach{t->repo.saveTransaction(t).onSuccess{offline.updateTransactionStatus(t.transactionId,SyncStatus.SYNCED)}.onFailure{offline.updateTransactionStatus(t.transactionId,SyncStatus.SYNC_ERROR)}}
   offline.expenses().filter{it.syncStatus!=SyncStatus.SYNCED}.forEach{e->repo.saveExpense(e.copy(syncStatus=SyncStatus.SYNCED)).onSuccess{offline.updateExpenseStatus(e.id,SyncStatus.SYNCED)}.onFailure{offline.updateExpenseStatus(e.id,SyncStatus.SYNC_ERROR)}}
   mergeLocalAndRemote(session)
 }finally{_syncing.value=false}}}

 fun add(p:Product){val u=_cart.value.toMutableList();val i=u.indexOfFirst{it.product.id==p.id};if(i>=0)u[i]=u[i].copy(quantity=u[i].quantity+1)else u.add(CartItem(p,1));_cart.value=u}
 fun remove(p:Product){_cart.value=_cart.value.mapNotNull{when{it.product.id!=p.id->it;it.quantity>1->it.copy(quantity=it.quantity-1);else->null}}}
 fun clearCart(){_cart.value=emptyList()}
 fun startShift(openingCash:Long){val s=session()?:return;viewModelScope.launch{val shift=Shift(ownerUid=ownerOf(s),businessId=s.businessId.orEmpty(),outletId=outletOf(s),cashierUid=s.uid,startAt=System.currentTimeMillis(),openingCash=openingCash,syncStatus=SyncStatus.PENDING_SYNC);offline.saveShift(shift);_shift.value=shift;repo.saveShift(shift).onSuccess{offline.updateShiftStatus(shift.id,SyncStatus.SYNCED);_shift.value=shift.copy(syncStatus=SyncStatus.SYNCED)} }}

 fun checkout(method:PaymentMethod,qrisPath:String?=null,discount:Long=0,cashReceived:Long=0){
  val s=session()?:return;val sh=_shift.value?:return;val cart=_cart.value
  val items=cart.map{TransactionItem(it.product.id,it.product.name,it.product.price,it.quantity,it.product.price*it.quantity)};if(items.isEmpty())return
  val subtotal=items.sumOf{it.subtotal};val disc=discount.coerceIn(0,subtotal);val total=subtotal-disc
  val oid=outletOf(s)
  val t=Transaction(ownerUid=ownerOf(s),businessId=s.businessId.orEmpty(),outletId=oid,shiftId=sh.id,cashierUid=s.uid,items=items,subtotal=subtotal,discount=disc,total=total,cashReceived=if(method==PaymentMethod.CASH)cashReceived else 0,paymentMethod=method,qrisProofPath=qrisPath,syncStatus=SyncStatus.PENDING_SYNC)
  offline.saveTransaction(t);_lastTransaction.value=t
  _transactions.value=(_transactions.value.filterNot{it.transactionId==t.transactionId}+t).sortedByDescending{it.createdAt}
  val sh2=sh.copy(transactionCount=sh.transactionCount+1,cashTotal=sh.cashTotal+if(method==PaymentMethod.CASH)total else 0,qrisTotal=sh.qrisTotal+if(method==PaymentMethod.QRIS)total else 0,syncStatus=SyncStatus.PENDING_SYNC)
  offline.saveShift(sh2);_shift.value=sh2
  _products.value=_products.value.map{p->val q=cart.firstOrNull{it.product.id==p.id}?.quantity;if(q!=null&&p.stockEnabled)p.copy(stock=(p.stock-q).coerceAtLeast(0)) else p}
  clearCart()
  viewModelScope.launch{
   repo.saveTransaction(t).onSuccess{offline.updateTransactionStatus(t.transactionId,SyncStatus.SYNCED);_lastTransaction.value=t.copy(syncStatus=SyncStatus.SYNCED);_transactions.value=_transactions.value.map{if(it.transactionId==t.transactionId)it.copy(syncStatus=SyncStatus.SYNCED) else it}}
   repo.saveShift(sh2).onSuccess{offline.updateShiftStatus(sh2.id,SyncStatus.SYNCED)}
   cart.filter{it.product.stockEnabled}.forEach{repo.adjustStock(oid,it.product.id,-it.quantity.toLong())}
  }
 }

 fun attachProof(transactionId:String,path:String){
  val t=offline.transactions().firstOrNull{it.transactionId==transactionId}?:_transactions.value.firstOrNull{it.transactionId==transactionId}?:return
  val u=t.copy(qrisProofPath=path,syncStatus=SyncStatus.PENDING_SYNC);offline.saveTransaction(u)
  _transactions.value=_transactions.value.map{if(it.transactionId==transactionId)u else it}
  if(_lastTransaction.value?.transactionId==transactionId)_lastTransaction.value=u
  viewModelScope.launch{repo.saveTransaction(u).onSuccess{offline.updateTransactionStatus(transactionId,SyncStatus.SYNCED)}}
 }

 fun closeShift(closingCash:Long){val s=session()?:return;val sh=_shift.value?:return;viewModelScope.launch{val closed=sh.copy(closedAt=System.currentTimeMillis(),closingCash=closingCash,syncStatus=SyncStatus.PENDING_SYNC);offline.saveShift(closed);_shift.value=null;_shifts.value=(_shifts.value.filterNot{it.id==closed.id}+closed).sortedByDescending{it.startAt};repo.saveShift(closed).onSuccess{offline.updateShiftStatus(sh.id,SyncStatus.SYNCED)}}}

 // ---- Master data (Owner) ----
 private fun mutate(ok:String,block:suspend()->Result<Unit>){val s=session()?:return;viewModelScope.launch{block().onSuccess{_message.value=ok}.onFailure{_message.value="Gagal menyimpan: "+(it.message?:"periksa koneksi")};loadMaster(s)}}
 fun setActiveOutlet(id:String){val s=session()?:return;_activeOutlet.value=id;viewModelScope.launch{loadMaster(s)}}
 fun saveCategory(name:String,id:String?){val s=session()?:return;val old=_categories.value.firstOrNull{it.id==id};val c=(old?:Category()).copy(ownerUid=ownerOf(s),outletId=outletOf(s),name=name.trim());mutate("Kategori disimpan"){repo.saveCategory(c)}}
 fun deleteCategory(id:String){val s=session()?:return;mutate("Kategori dihapus"){repo.deleteCategory(outletOf(s),id)}}
 fun saveProduct(p:Product){val s=session()?:return;val q=p.copy(ownerUid=ownerOf(s),outletId=outletOf(s));mutate("Produk disimpan"){repo.saveProduct(q)}}
 fun deleteProduct(id:String){val s=session()?:return;mutate("Produk dihapus"){repo.deleteProduct(outletOf(s),id)}}
 fun adjustStock(productId:String,delta:Long){val s=session()?:return;mutate("Stok diperbarui"){repo.adjustStock(outletOf(s),productId,delta)}}
 fun saveOutlet(name:String,address:String,active:Boolean,id:String?){val s=session()?:return;val o=Outlet(id=id?:java.util.UUID.randomUUID().toString(),ownerUid=ownerOf(s),businessId=s.businessId,name=name.trim(),address=address.trim().ifBlank{null},active=active);mutate("Outlet disimpan"){repo.saveOutlet(o)}}
 fun saveWorker(w:Worker,pin:String){val s=session()?:return;val q=w.copy(ownerUid=ownerOf(s),businessId=s.businessId.orEmpty(),outletId=w.outletId.ifBlank{outletOf(s)});mutate("Pekerja disimpan"){repo.saveWorker(q,pin.toCharArray().takeIf{it.isNotEmpty()})}}
 fun saveBusiness(name:String,type:String,phone:String){
  val s=session()?:return
  val b=Business(id=s.businessId?:java.util.UUID.randomUUID().toString(),ownerUid=ownerOf(s),name=name.trim(),type=type.trim().ifBlank{null},phone=phone.trim().ifBlank{null})
  if(s.businessId==null && s.role==Role.OWNER){ _auth.value=AuthState.LoggedIn(s.copy(businessId=b.id)) }
  _business.value=b
  mutate("Bisnis disimpan"){repo.saveBusiness(b)}
}
 fun saveProfile(displayName:String,whatsapp:String){val s=session()?:return;mutate("Profil disimpan"){repo.updateProfile(s,displayName.trim(),whatsapp.trim().ifBlank{null})}}

 // ---- Expenses (offline-first) ----
 fun saveExpense(amount:Long,category:String,note:String,id:String?){
  val s=session()?:return;val old=_expenses.value.firstOrNull{it.id==id}
  val e=(old?:Expense(ownerUid=ownerOf(s),businessId=s.businessId.orEmpty(),outletId=outletOf(s),createdBy=s.uid,createdByName=s.username)).copy(amount=amount,category=category,note=note.trim(),syncStatus=SyncStatus.PENDING_SYNC)
  offline.saveExpense(e);_expenses.value=(_expenses.value.filterNot{it.id==e.id}+e).sortedByDescending{it.createdAt};_message.value="Pengeluaran disimpan"
  viewModelScope.launch{repo.saveExpense(e.copy(syncStatus=SyncStatus.SYNCED)).onSuccess{offline.updateExpenseStatus(e.id,SyncStatus.SYNCED);_expenses.value=_expenses.value.map{if(it.id==e.id)it.copy(syncStatus=SyncStatus.SYNCED) else it}}.onFailure{offline.updateExpenseStatus(e.id,SyncStatus.SYNC_ERROR)}}
 }
 fun deleteExpense(id:String){val e=_expenses.value.firstOrNull{it.id==id}?:return;offline.deleteExpense(id);_expenses.value=_expenses.value.filterNot{it.id==id};_message.value="Pengeluaran dihapus";viewModelScope.launch{repo.deleteExpense(e.outletId,id)}}

 fun logout(){if(_shift.value!=null)return;repo.logout();_auth.value=AuthState.LoggedOut;clearCart();_lastTransaction.value=null;_activeOutlet.value=null;_products.value=emptyList();_categories.value=emptyList();_transactions.value=emptyList();_expenses.value=emptyList();_shifts.value=emptyList();_outlets.value=emptyList();_workers.value=emptyList();_business.value=null}
}
