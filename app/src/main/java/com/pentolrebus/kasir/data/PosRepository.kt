package com.pentolrebus.kasir.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.pentolrebus.kasir.domain.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.UUID

interface PosRepository {
 suspend fun registerOwner(email:String,password:String,username:String,pin:CharArray,businessName:String?,outletName:String?,whatsapp:String?):Result<Session>
 suspend fun emailLogin(email:String,password:String):Result<Session>
 suspend fun sendPasswordReset(email:String):Result<Unit>
 suspend fun localPinLogin(username:String,pin:CharArray):Result<Session>
 suspend fun loadProducts(outletId:String):List<Product>
 suspend fun saveProduct(product:Product):Result<Unit>
 suspend fun startShift(session:Session,openingCash:Long):Result<Shift>
 suspend fun closeShift(shift:Shift,closingCash:Long):Result<Shift>
 suspend fun saveShift(shift:Shift):Result<Unit>
 suspend fun saveTransaction(t:Transaction):Result<Unit>
 suspend fun loadTransactions(session:Session):List<Transaction>
 suspend fun loadActiveShift(session:Session):Shift?
 suspend fun deleteProduct(outletId:String,id:String):Result<Unit>
 suspend fun adjustStock(outletId:String,productId:String,delta:Long):Result<Unit>
 suspend fun loadCategories(outletId:String):List<Category>
 suspend fun saveCategory(c:Category):Result<Unit>
 suspend fun deleteCategory(outletId:String,id:String):Result<Unit>
 suspend fun loadOutlets(session:Session):List<Outlet>
 suspend fun saveOutlet(o:Outlet):Result<Unit>
 suspend fun loadWorkers(session:Session):List<Worker>
 suspend fun saveWorker(w:Worker,pin:CharArray?):Result<Unit>
 suspend fun loadBusiness(session:Session):Business?
 suspend fun saveBusiness(b:Business):Result<Unit>
 suspend fun loadOutletTransactions(outletId:String):List<Transaction>
 suspend fun loadOutletShifts(outletId:String):List<Shift>
 suspend fun loadExpenses(outletId:String):List<Expense>
 suspend fun saveExpense(e:Expense):Result<Unit>
 suspend fun deleteExpense(outletId:String,id:String):Result<Unit>
 suspend fun updateProfile(session:Session,displayName:String,whatsapp:String?):Result<Unit>
 fun logout()
}

class FirebasePosRepository(private val auth:FirebaseAuth=FirebaseAuth.getInstance(), private val root:DatabaseReference=FirebaseDatabase.getInstance().reference, private val secure:SecureLocalStore, private val log:(String,String)->Unit):PosRepository {
 private suspend fun <T> timed(block:suspend()->T)=withTimeout(15_000){block()}
 override suspend fun registerOwner(email:String,password:String,username:String,pin:CharArray,businessName:String?,outletName:String?,whatsapp:String?):Result<Session> = runCatching { log("REGISTRATION","OWNER_REGISTRATION_STARTED"); log("REGISTRATION","AUTH_CREATE_STARTED"); val result=timed{auth.createUserWithEmailAndPassword(email,password).await()}; val uid=result.user?.uid ?: error("Firebase UID tidak tersedia"); log("REGISTRATION","AUTH_CREATE_SUCCESS"); val businessId=businessName?.takeIf{it.isNotBlank()}?.let{UUID.randomUUID().toString()}; val outletId=outletName?.takeIf{it.isNotBlank()}?.let{UUID.randomUUID().toString()}; log("RTDB","RTDB_BOOTSTRAP_STARTED"); val profile=mapOf("uid" to uid,"ownerUid" to uid,"username" to username,"role" to "OWNER","displayName" to username,"businessId" to businessId,"outletId" to outletId,"whatsapp" to whatsapp); timed{root.child("users").child(uid).setValue(profile).await()}; log("REGISTRATION","PROFILE_CREATED"); if(businessId!=null){timed{root.child("businesses").child(businessId).setValue(mapOf("id" to businessId,"ownerUid" to uid,"name" to businessName)).await()};log("REGISTRATION","BUSINESS_CREATED")}; if(outletId!=null){timed{root.child("outlets").child(outletId).setValue(mapOf("id" to outletId,"ownerUid" to uid,"businessId" to businessId,"name" to outletName)).await()};log("REGISTRATION","OUTLET_CREATED")}; secure.saveCredential(username,uid,pin); secure.saveSession(uid,username,"OWNER",businessId,outletId);log("REGISTRATION","SESSION_CREATED");log("REGISTRATION","REGISTRATION_SUCCESS");Session(uid,username,Role.OWNER,businessId,outletId) }.also{if(it.isFailure)log("ERROR","OWNER_REGISTRATION_ERROR stage=registration")}
 override suspend fun emailLogin(email:String,password:String)=runCatching{val r=timed{auth.signInWithEmailAndPassword(email,password).await()};val uid=r.user?.uid?:error("UID missing");val snap=timed{root.child("users").child(uid).get().await()};val username=snap.child("username").getValue(String::class.java)?:email;val role=Role.valueOf(snap.child("role").getValue(String::class.java)?:"OWNER");val b=snap.child("businessId").getValue(String::class.java);val o=snap.child("outletId").getValue(String::class.java);secure.saveSession(uid,username,role.name,b,o);Session(uid,username,role,b,o)}
 override suspend fun sendPasswordReset(email:String)=runCatching{timed{auth.sendPasswordResetEmail(email).await()};Unit}
 override suspend fun localPinLogin(username:String,pin:CharArray)=runCatching{
  val uid=secure.verify(username,pin)?:error("Username/PIN perangkat tidak valid")
  secure.workerSession(username)?.let{w->return@runCatching Session(w[0],username,Role.CASHIER,w[2].ifBlank{null},w[3].ifBlank{null},w[1])}
  val cached=secure.session()
  if (cached?.get("uid")==uid && cached["username"]==username) {
   val role=Role.valueOf(cached["role"]?:"CASHIER")
   return@runCatching Session(uid,username,role,cached["businessId"],cached["outletId"])
  }
  val snap=timed{root.child("users").child(uid).get().await()}
  val role=Role.valueOf(snap.child("role").getValue(String::class.java)?:"CASHIER")
  val b=snap.child("businessId").getValue(String::class.java); val o=snap.child("outletId").getValue(String::class.java)
  secure.saveSession(uid,username,role.name,b,o); Session(uid,username,role,b,o)
}
 override suspend fun loadProducts(outletId:String)=runCatching{val s=timed{root.child("outlets/$outletId/products").get().await()};s.children.mapNotNull{it.getValue(Product::class.java)}}.getOrDefault(emptyList())
 override suspend fun saveProduct(product: Product): Result<Unit> = runCatching {
  timed { root.child("outlets/${product.outletId}/products/${product.id}").setValue(product).await() }
  Unit
}
 override suspend fun startShift(session:Session,openingCash:Long)=runCatching{val s=Shift(ownerUid=session.uid,businessId=session.businessId.orEmpty(),outletId=session.outletId.orEmpty(),cashierUid=session.uid,startAt=System.currentTimeMillis(),openingCash=openingCash);timed{root.child("outlets/${s.outletId}/shifts/${s.id}").setValue(s).await()};s}
 override suspend fun closeShift(shift:Shift,closingCash:Long)=runCatching{val s=shift.copy(closedAt=System.currentTimeMillis(),closingCash=closingCash,syncStatus=SyncStatus.SYNCED);timed{root.child("outlets/${s.outletId}/shifts/${s.id}").setValue(s).await()};s}
 override suspend fun saveShift(shift:Shift):Result<Unit> = runCatching { timed { root.child("outlets/${shift.outletId}/shifts/${shift.id}").setValue(shift).await() }; Unit }
 override suspend fun saveTransaction(t: Transaction): Result<Unit> = runCatching {
  timed { root.child("outlets/${t.outletId}/transactions/${t.transactionId}").setValue(t).await() }
  Unit
}
 override suspend fun loadTransactions(session:Session)=runCatching{val s=timed{root.child("outlets/${session.outletId}/transactions").get().await()};s.children.mapNotNull{it.getValue(Transaction::class.java)}}.getOrDefault(emptyList())
 override suspend fun loadActiveShift(session:Session):Shift?=runCatching{val s=timed{root.child("outlets/${session.outletId}/shifts").get().await()};s.children.mapNotNull{it.getValue(Shift::class.java)}.firstOrNull{it.cashierUid==session.uid&&it.closedAt==null}}.getOrNull()
 override suspend fun deleteProduct(outletId:String,id:String)=runCatching{timed{root.child("outlets/$outletId/products/$id").removeValue().await()};Unit}
 override suspend fun adjustStock(outletId:String,productId:String,delta:Long)=runCatching{timed{root.child("outlets/$outletId/products/$productId/stock").setValue(com.google.firebase.database.ServerValue.increment(delta)).await()};Unit}
 override suspend fun loadCategories(outletId:String)=runCatching{val s=timed{root.child("outlets/$outletId/categories").get().await()};s.children.mapNotNull{it.getValue(Category::class.java)}}.getOrDefault(emptyList())
 override suspend fun saveCategory(c:Category)=runCatching{timed{root.child("outlets/${c.outletId}/categories/${c.id}").setValue(c).await()};Unit}
 override suspend fun deleteCategory(outletId:String,id:String)=runCatching{timed{root.child("outlets/$outletId/categories/$id").removeValue().await()};Unit}
 override suspend fun loadOutlets(session:Session)=runCatching{
  if(session.role!=Role.OWNER) return@runCatching listOfNotNull(session.outletId?.let{timed{root.child("outlets/$it").get().await()}.getValue(Outlet::class.java)})
  val s=timed{root.child("outlets").orderByChild("ownerUid").equalTo(session.uid).get().await()}
  val list=s.children.mapNotNull{it.getValue(Outlet::class.java)}
  if(list.isEmpty()&&session.outletId!=null) listOfNotNull(timed{root.child("outlets/${session.outletId}").get().await()}.getValue(Outlet::class.java)) else list
 }.getOrDefault(emptyList())
 override suspend fun saveOutlet(o:Outlet)=runCatching{timed{root.child("outlets/${o.id}").updateChildren(mapOf("id" to o.id,"ownerUid" to o.ownerUid,"businessId" to o.businessId,"name" to o.name,"address" to o.address,"active" to o.active)).await()};Unit}
 override suspend fun loadWorkers(session:Session)=runCatching{
  val outlets=loadOutlets(session)
  outlets.flatMap{o->timed{root.child("outlets/${o.id}/workers").get().await()}.children.mapNotNull{it.getValue(Worker::class.java)}}
 }.getOrDefault(emptyList())
 override suspend fun saveWorker(w:Worker,pin:CharArray?)=runCatching{
  timed{root.child("outlets/${w.outletId}/workers/${w.id}").setValue(w).await()}
  if(pin!=null&&pin.isNotEmpty()) secure.saveCredential(w.username,w.id,pin)
  secure.saveWorkerSession(w.username,w.id,w.ownerUid,w.businessId,w.outletId)
  if(!w.active) secure.removeWorker(w.username)
  Unit
 }
 override suspend fun loadBusiness(session:Session)=runCatching{session.businessId?.let{timed{root.child("businesses/$it").get().await()}.getValue(Business::class.java)}}.getOrNull()
 override suspend fun saveBusiness(b:Business)=runCatching{timed{root.child("businesses/${b.id}").updateChildren(mapOf("id" to b.id,"ownerUid" to b.ownerUid,"name" to b.name,"type" to b.type,"phone" to b.phone)).await()};Unit}
 override suspend fun loadOutletTransactions(outletId:String)=runCatching{timed{root.child("outlets/$outletId/transactions").get().await()}.children.mapNotNull{it.getValue(Transaction::class.java)}}.getOrDefault(emptyList())
 override suspend fun loadOutletShifts(outletId:String)=runCatching{timed{root.child("outlets/$outletId/shifts").get().await()}.children.mapNotNull{it.getValue(Shift::class.java)}}.getOrDefault(emptyList())
 override suspend fun loadExpenses(outletId:String)=runCatching{timed{root.child("outlets/$outletId/expenses").get().await()}.children.mapNotNull{it.getValue(Expense::class.java)}}.getOrDefault(emptyList())
 override suspend fun saveExpense(e:Expense)=runCatching{timed{root.child("outlets/${e.outletId}/expenses/${e.id}").setValue(e).await()};Unit}
 override suspend fun deleteExpense(outletId:String,id:String)=runCatching{timed{root.child("outlets/$outletId/expenses/$id").removeValue().await()};Unit}
 override suspend fun updateProfile(session:Session,displayName:String,whatsapp:String?)=runCatching{timed{root.child("users/${session.uid}").updateChildren(mapOf("displayName" to displayName,"whatsapp" to whatsapp)).await()};Unit}
 override fun logout(){auth.signOut();secure.clearSession()}
}
