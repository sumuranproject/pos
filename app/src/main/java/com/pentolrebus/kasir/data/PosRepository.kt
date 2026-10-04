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
 suspend fun localPinLogin(username:String,pin:CharArray):Result<Session>
 suspend fun loadProducts(outletId:String):List<Product>
 suspend fun saveProduct(product:Product):Result<Unit>
 suspend fun startShift(session:Session,openingCash:Long):Result<Shift>
 suspend fun closeShift(shift:Shift,closingCash:Long):Result<Shift>
 suspend fun saveShift(shift:Shift):Result<Unit>
 suspend fun saveTransaction(t:Transaction):Result<Unit>
 suspend fun loadTransactions(session:Session):List<Transaction>
 suspend fun loadActiveShift(session:Session):Shift?
 fun logout()
}

class FirebasePosRepository(private val auth:FirebaseAuth=FirebaseAuth.getInstance(), private val root:DatabaseReference=FirebaseDatabase.getInstance().reference, private val secure:SecureLocalStore, private val log:(String,String)->Unit):PosRepository {
 private suspend fun <T> timed(block:suspend()->T)=withTimeout(15_000){block()}
 override suspend fun registerOwner(email:String,password:String,username:String,pin:CharArray,businessName:String?,outletName:String?,whatsapp:String?):Result<Session> = runCatching { log("REGISTRATION","OWNER_REGISTRATION_STARTED"); log("REGISTRATION","AUTH_CREATE_STARTED"); val result=timed{auth.createUserWithEmailAndPassword(email,password).await()}; val uid=result.user?.uid ?: error("Firebase UID tidak tersedia"); log("REGISTRATION","AUTH_CREATE_SUCCESS"); val businessId=businessName?.takeIf{it.isNotBlank()}?.let{UUID.randomUUID().toString()}; val outletId=outletName?.takeIf{it.isNotBlank()}?.let{UUID.randomUUID().toString()}; log("RTDB","RTDB_BOOTSTRAP_STARTED"); val profile=mapOf("uid" to uid,"ownerUid" to uid,"username" to username,"role" to "OWNER","displayName" to username,"businessId" to businessId,"outletId" to outletId,"whatsapp" to whatsapp); timed{root.child("users").child(uid).setValue(profile).await()}; log("REGISTRATION","PROFILE_CREATED"); if(businessId!=null){timed{root.child("businesses").child(businessId).setValue(mapOf("id" to businessId,"ownerUid" to uid,"name" to businessName)).await()};log("REGISTRATION","BUSINESS_CREATED")}; if(outletId!=null){timed{root.child("outlets").child(outletId).setValue(mapOf("id" to outletId,"ownerUid" to uid,"businessId" to businessId,"name" to outletName)).await()};log("REGISTRATION","OUTLET_CREATED")}; secure.saveCredential(username,uid,pin); secure.saveSession(uid,username,"OWNER",businessId,outletId);log("REGISTRATION","SESSION_CREATED");log("REGISTRATION","REGISTRATION_SUCCESS");Session(uid,username,Role.OWNER,businessId,outletId) }.also{if(it.isFailure)log("ERROR","OWNER_REGISTRATION_ERROR stage=registration")}
 override suspend fun emailLogin(email:String,password:String)=runCatching{val r=timed{auth.signInWithEmailAndPassword(email,password).await()};val uid=r.user?.uid?:error("UID missing");val snap=timed{root.child("users").child(uid).get().await()};val username=snap.child("username").getValue(String::class.java)?:email;val role=Role.valueOf(snap.child("role").getValue(String::class.java)?:"OWNER");val b=snap.child("businessId").getValue(String::class.java);val o=snap.child("outletId").getValue(String::class.java);secure.saveSession(uid,username,role.name,b,o);Session(uid,username,role,b,o)}
 override suspend fun localPinLogin(username:String,pin:CharArray)=runCatching{
  val uid=secure.verify(username,pin)?:error("Username/PIN perangkat tidak valid")
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
 override override fun logout(){auth.signOut();secure.clearSession()}
}
