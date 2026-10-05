package com.pentolrebus.kasir.data

import android.util.Base64
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.pentolrebus.kasir.domain.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.UUID

interface PosRepository {
 suspend fun registerOwner(email:String,password:String,username:String,displayName:String,businessName:String?,outletName:String?,whatsapp:String?):Result<Session>
 suspend fun usernameLogin(username:String,password:String):Result<Session>
 suspend fun sendPasswordReset(username:String):Result<Unit>
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
 suspend fun saveWorker(w:Worker,password:String?):Result<Unit>
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
 override suspend fun registerOwner(email:String,password:String,username:String,displayName:String,businessName:String?,outletName:String?,whatsapp:String?):Result<Session> = runCatching {
  val clean=username.trim().lowercase().replace(" ",""); if(clean.length<3) error("Username minimal 3 karakter")
  val existing=timed{root.child("loginIndex").child(clean).get().await()}; if(existing.exists()) error("Username sudah dipakai")
  val result=timed{auth.createUserWithEmailAndPassword(email.trim(),password).await()}; val uid=result.user?.uid ?: error("Firebase UID tidak tersedia")
  val businessId=businessName?.takeIf{it.isNotBlank()}?.let{UUID.randomUUID().toString()}; val outletId=outletName?.takeIf{it.isNotBlank()}?.let{UUID.randomUUID().toString()}
  val profile=mapOf("uid" to uid,"ownerUid" to uid,"username" to clean,"role" to "OWNER","displayName" to displayName.trim().ifBlank{clean},"businessId" to businessId,"outletId" to outletId,"whatsapp" to whatsapp,"email" to email.trim())
  timed{root.child("users").child(uid).setValue(profile).await()}
  timed{root.child("loginIndex").child(clean).setValue(mapOf("authEmail" to email.trim(),"uid" to uid,"role" to "OWNER","active" to true,"recoveryEmail" to email.trim())).await()}
  if(businessId!=null) timed{root.child("businesses").child(businessId).setValue(mapOf("id" to businessId,"ownerUid" to uid,"name" to businessName)).await()}
  if(outletId!=null) timed{root.child("outlets").child(outletId).setValue(mapOf("id" to outletId,"ownerUid" to uid,"businessId" to businessId,"name" to outletName)).await()}
  secure.saveSession(uid,clean,"OWNER",businessId,outletId); Session(uid,clean,Role.OWNER,businessId,outletId)
 }.also{if(it.isFailure)log("ERROR","OWNER_REGISTRATION_ERROR stage=registration")}
 override suspend fun usernameLogin(username:String,password:String)=runCatching{
  val clean=username.trim().lowercase().replace(" ",""); if(clean.isBlank()) error("Username wajib diisi"); if(password.isBlank()) error("Password wajib diisi")
  val idx=timed{root.child("loginIndex").child(clean).get().await()}; if(!idx.exists()) error("Username atau password salah")
  if(idx.child("active").getValue(Boolean::class.java)==false) error("Akun dinonaktifkan")
  val email=idx.child("authEmail").getValue(String::class.java)?:error("Data akun tidak lengkap")
  val r=timed{auth.signInWithEmailAndPassword(email,password).await()}; val uid=r.user?.uid?:error("UID missing")
  val snap=timed{root.child("users").child(uid).get().await()}; val stored=snap.child("username").getValue(String::class.java)?:clean
  val role=Role.valueOf(snap.child("role").getValue(String::class.java)?:(idx.child("role").getValue(String::class.java)?:("CASHIER")))
  val b=snap.child("businessId").getValue(String::class.java); val o=snap.child("outletId").getValue(String::class.java); val owner=snap.child("ownerUid").getValue(String::class.java)
  secure.saveSession(uid,stored,role.name,b,o); Session(uid,stored,role,b,o,owner)
 }
 override suspend fun sendPasswordReset(username:String)=runCatching{
  val clean=username.trim().lowercase().replace(" ",""); val idx=timed{root.child("loginIndex").child(clean).get().await()}; if(!idx.exists()) error("Username tidak ditemukan")
  val role=idx.child("role").getValue(String::class.java)?:("CASHIER"); if(role!="OWNER") error("Untuk akun Kasir, reset password dilakukan oleh Owner")
  val email=idx.child("recoveryEmail").getValue(String::class.java)?:error("Email pemulihan belum tersedia")
  timed{auth.sendPasswordResetEmail(email).await()}; Unit
 }
 override suspend fun loadProducts(outletId:String)=runCatching{val s=timed{root.child("outlets/$outletId/products").get().await()};s.children.mapNotNull{it.getValue(Product::class.java)}}.getOrDefault(emptyList())

 override suspend fun saveProduct(product: Product): Result<Unit> = runCatching {
  timed { root.child("outlets/${product.outletId}/products/${product.id}").setValue(product).await() }
  Unit
}
 override suspend fun startShift(session:Session,openingCash:Long)=runCatching{val s=Shift(ownerUid=session.uid,businessId=session.businessId.orEmpty(),outletId=session.outletId.orEmpty(),cashierUid=session.uid,startAt=System.currentTimeMillis(),openingCash=openingCash);timed{root.child("outlets/${s.outletId}/shifts/${s.id}").setValue(s).await()};s}
 override suspend fun closeShift(shift:Shift,closingCash:Long)=runCatching{val s=shift.copy(closedAt=System.currentTimeMillis(),closingCash=closingCash,syncStatus=SyncStatus.SYNCED);timed{root.child("outlets/${s.outletId}/shifts/${s.id}").setValue(s).await()};s}
 override suspend fun saveShift(shift:Shift):Result<Unit> = runCatching { timed { root.child("outlets/${shift.outletId}/shifts/${shift.id}").setValue(shift).await() }; Unit }.onFailure { log("RTDB", "saveShift failed path=outlets/${shift.outletId}/shifts/${shift.id} error=${it.javaClass.simpleName}: ${it.message}") }
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
 override suspend fun saveWorker(w:Worker,password:String?)=runCatching{
  val clean=w.username.trim().lowercase().replace(" ",""); if(clean.length<3) error("Username minimal 3 karakter")
  val idx=timed{root.child("loginIndex").child(clean).get().await()}
  val existing=if(idx.exists()) idx.child("uid").getValue(String::class.java) else null
  if(existing!=null && existing!=w.authUid) error("Username sudah dipakai")
  var authUid=w.authUid
  if(authUid==null){
    val defaultApp=FirebaseApp.getInstance(); val name="worker-${UUID.randomUUID()}"; val secondary=FirebaseApp.initializeApp(defaultApp.applicationContext,defaultApp.options,name) ?: error("Firebase App sekunder gagal dibuat")
    try {
      val secondaryAuth=FirebaseAuth.getInstance(secondary); val authEmail=authEmailForUsername(clean)
      val created=timed{secondaryAuth.createUserWithEmailAndPassword(authEmail,password?.takeIf{it.isNotBlank()}?:error("Password wajib diisi")).await()}; authUid=created.user?.uid?:error("UID Kasir tidak tersedia")
    } finally { secondary.delete() }
  }
  val q=w.copy(id=w.id,username=clean,authUid=authUid)
  timed{root.child("outlets/${q.outletId}/workers/${q.id}").setValue(q).await()}
  val profile=mapOf("uid" to authUid,"ownerUid" to q.ownerUid,"username" to clean,"role" to "CASHIER","displayName" to q.displayName,"businessId" to q.businessId,"outletId" to q.outletId,"whatsapp" to q.whatsapp,"active" to q.active)
  timed{root.child("users").child(authUid!!).setValue(profile).await()}
  timed{root.child("loginIndex").child(clean).setValue(mapOf("authEmail" to authEmailForUsername(clean),"uid" to authUid,"role" to "CASHIER","active" to q.active,"ownerUid" to q.ownerUid,"outletId" to q.outletId)).await()}
  Unit
 }.onFailure { log("RTDB", "saveWorker failed username=${w.username} outlet=${w.outletId} error=${it.javaClass.simpleName}: ${it.message}") }
 override suspend fun loadBusiness(session:Session)=runCatching{session.businessId?.let{timed{root.child("businesses/$it").get().await()}.getValue(Business::class.java)}}.getOrNull()
 override suspend fun saveBusiness(b:Business)=runCatching{timed{root.child("businesses/${b.id}").updateChildren(mapOf("id" to b.id,"ownerUid" to b.ownerUid,"name" to b.name,"type" to b.type,"phone" to b.phone)).await()};Unit}
 override suspend fun loadOutletTransactions(outletId:String)=runCatching{timed{root.child("outlets/$outletId/transactions").get().await()}.children.mapNotNull{it.getValue(Transaction::class.java)}}.getOrDefault(emptyList())
 override suspend fun loadOutletShifts(outletId:String)=runCatching{timed{root.child("outlets/$outletId/shifts").get().await()}.children.mapNotNull{it.getValue(Shift::class.java)}}.getOrDefault(emptyList())
 override suspend fun loadExpenses(outletId:String)=runCatching{timed{root.child("outlets/$outletId/expenses").get().await()}.children.mapNotNull{it.getValue(Expense::class.java)}}.getOrDefault(emptyList())
 override suspend fun saveExpense(e:Expense)=runCatching{timed{root.child("outlets/${e.outletId}/expenses/${e.id}").setValue(e).await()};Unit}
 override suspend fun deleteExpense(outletId:String,id:String)=runCatching{timed{root.child("outlets/$outletId/expenses/$id").removeValue().await()};Unit}
 override suspend fun updateProfile(session:Session,displayName:String,whatsapp:String?)=runCatching{timed{root.child("users/${session.uid}").updateChildren(mapOf("displayName" to displayName,"whatsapp" to whatsapp)).await()};Unit}
 private fun authEmailForUsername(username:String):String {
  val bytes=username.toByteArray(Charsets.UTF_8)
  val encoded=Base64.encodeToString(bytes,Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING).replace("-","_").replace("_","x")
  return "u-$encoded@login.sakukasir.invalid"
 }

 override fun logout(){auth.signOut();secure.clearSession()}
}
