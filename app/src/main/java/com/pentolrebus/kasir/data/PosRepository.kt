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
 suspend fun deleteProduct(product:Product):Result<Unit>
 suspend fun loadCategories(outletId:String):List<Category>
 suspend fun saveCategory(category:Category):Result<Unit>
 suspend fun deleteCategory(category:Category):Result<Unit>
 suspend fun loadOutlets(ownerUid:String):List<Outlet>
 suspend fun saveOutlet(outlet:Outlet):Result<Unit>
 suspend fun deleteOutlet(outlet:Outlet):Result<Unit>
 suspend fun loadWorkers(outletId:String):List<Worker>
 suspend fun saveWorker(worker:Worker):Result<Unit>
 suspend fun deleteWorker(worker:Worker):Result<Unit>
 suspend fun loadBusiness(ownerUid:String):Business?
 suspend fun saveBusiness(business:Business):Result<Unit>
 suspend fun loadExpenses(outletId:String):List<Expense>
 suspend fun saveExpense(expense:Expense):Result<Unit>
 suspend fun deleteExpense(expense:Expense):Result<Unit>
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
 private suspend inline fun <reified T> readList(path:String):List<T> = runCatching { timed{root.child(path).get().await()}.children.mapNotNull{it.getValue(T::class.java)} }.getOrDefault(emptyList())
 private suspend inline fun <reified T> readOne(path:String):T? = runCatching { timed{root.child(path).get().await()}.getValue(T::class.java) }.getOrNull()
 private suspend fun write(path:String,value:Any):Result<Unit> = runCatching{timed{root.child(path).setValue(value).await()};Unit}
 private suspend fun remove(path:String):Result<Unit> = runCatching{timed{root.child(path).removeValue().await()};Unit}
 override suspend fun registerOwner(email:String,password:String,username:String,pin:CharArray,businessName:String?,outletName:String?,whatsapp:String?):Result<Session> = runCatching { val result=timed{auth.createUserWithEmailAndPassword(email,password).await()}; val uid=result.user?.uid?:error("Firebase UID tidak tersedia"); val businessId=businessName?.takeIf{it.isNotBlank()}?.let{UUID.randomUUID().toString()}; val outletId=outletName?.takeIf{it.isNotBlank()}?.let{UUID.randomUUID().toString()}; root.child("users/$uid").setValue(mapOf("uid" to uid,"ownerUid" to uid,"username" to username,"role" to "OWNER","displayName" to username,"businessId" to businessId,"outletId" to outletId,"whatsapp" to whatsapp)).await(); if(businessId!=null) saveBusiness(Business(businessId,uid,businessName!!,whatsapp)); if(outletId!=null) saveOutlet(Outlet(outletId,uid,businessId,outletName!!)); secure.saveCredential(username,uid,pin); secure.saveSession(uid,username,"OWNER",businessId,outletId); Session(uid,username,Role.OWNER,businessId,outletId) }
 override suspend fun emailLogin(email:String,password:String)=runCatching{val r=timed{auth.signInWithEmailAndPassword(email,password).await()};val uid=r.user?.uid?:error("UID missing");val snap=timed{root.child("users/$uid").get().await()};val username=snap.child("username").getValue(String::class.java)?:email;val role=Role.valueOf(snap.child("role").getValue(String::class.java)?:("OWNER"));val b=snap.child("businessId").getValue(String::class.java);val o=snap.child("outletId").getValue(String::class.java);secure.saveSession(uid,username,role.name,b,o);Session(uid,username,role,b,o)}
 override suspend fun localPinLogin(username:String,pin:CharArray)=runCatching{val uid=secure.verify(username,pin)?:error("Username/PIN perangkat tidak valid");val cached=secure.session();if(cached?.get("uid")==uid&&cached["username"]==username)return@runCatching Session(uid,username,Role.valueOf(cached["role"]?:"CASHIER"),cached["businessId"],cached["outletId"]);val snap=timed{root.child("users/$uid").get().await()};val role=Role.valueOf(snap.child("role").getValue(String::class.java)?:"CASHIER");val b=snap.child("businessId").getValue(String::class.java);val o=snap.child("outletId").getValue(String::class.java);secure.saveSession(uid,username,role.name,b,o);Session(uid,username,role,b,o)}
 override suspend fun loadProducts(outletId:String)=readList("outlets/$outletId/products")
 override suspend fun saveProduct(product:Product):Result<Unit> = write("outlets/${product.outletId}/products/${product.id}",product)
 override suspend fun deleteProduct(product:Product)=remove("outlets/${product.outletId}/products/${product.id}")
 override suspend fun loadCategories(outletId:String)=readList("outlets/$outletId/categories")
 override suspend fun saveCategory(category:Category)=write("outlets/${category.outletId}/categories/${category.id}",category)
 override suspend fun deleteCategory(category:Category)=remove("outlets/${category.outletId}/categories/${category.id}")
 override suspend fun loadOutlets(ownerUid:String)=readList<Outlet>("outlets").filter{it.ownerUid==ownerUid}
 override suspend fun saveOutlet(outlet:Outlet)=write("outlets/${outlet.id}",outlet)
 override suspend fun deleteOutlet(outlet:Outlet)=remove("outlets/${outlet.id}")
 override suspend fun loadWorkers(outletId:String)=readList("outlets/$outletId/workers")
 override suspend fun saveWorker(worker:Worker)=write("outlets/${worker.outletId}/workers/${worker.id}",worker)
 override suspend fun deleteWorker(worker:Worker)=remove("outlets/${worker.outletId}/workers/${worker.id}")
 override suspend fun loadBusiness(ownerUid:String):Business?=readList<Business>("businesses").firstOrNull{it.ownerUid==ownerUid}
 override suspend fun saveBusiness(business:Business)=write("businesses/${business.id}",business)
 override suspend fun loadExpenses(outletId:String)=readList("outlets/$outletId/expenses")
 override suspend fun saveExpense(expense:Expense)=write("outlets/${expense.outletId}/expenses/${expense.id}",expense)
 override suspend fun deleteExpense(expense:Expense)=remove("outlets/${expense.outletId}/expenses/${expense.id}")
 override suspend fun startShift(session:Session,openingCash:Long)=runCatching{val s=Shift(ownerUid=session.uid,businessId=session.businessId.orEmpty(),outletId=session.outletId.orEmpty(),cashierUid=session.uid,startAt=System.currentTimeMillis(),openingCash=openingCash);write("outlets/${s.outletId}/shifts/${s.id}",s).getOrThrow();s}
 override suspend fun closeShift(shift:Shift,closingCash:Long)=runCatching{val s=shift.copy(closedAt=System.currentTimeMillis(),closingCash=closingCash,syncStatus=SyncStatus.SYNCED);write("outlets/${s.outletId}/shifts/${s.id}",s).getOrThrow();s}
 override suspend fun saveShift(shift:Shift)=write("outlets/${shift.outletId}/shifts/${shift.id}",shift)
 override suspend fun saveTransaction(t:Transaction):Result<Unit> = write("outlets/${t.outletId}/transactions/${t.transactionId}",t)
 override suspend fun loadTransactions(session:Session)=readList<Transaction>("outlets/${session.outletId}/transactions").filter{session.role==Role.OWNER||it.cashierUid==session.uid}
 override suspend fun loadActiveShift(session:Session):Shift?=readList<Shift>("outlets/${session.outletId}/shifts").filter{it.cashierUid==session.uid&&it.closedAt==null}.maxByOrNull{it.startAt}
 override fun logout(){auth.signOut();secure.clearSession()}
}
