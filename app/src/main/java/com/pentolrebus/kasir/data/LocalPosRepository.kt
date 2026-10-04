package com.pentolrebus.kasir.data

import com.pentolrebus.kasir.domain.*
import java.util.UUID

class LocalPosRepository(private val secure:SecureLocalStore, private val log:(String,String)->Unit):PosRepository {
 private val products=mutableListOf(
  Product("p1","demo-owner","demo-outlet","c1","Pentol Rebus",12000,"porsi",false,0,true),
  Product("p2","demo-owner","demo-outlet","c1","Pentol Pedas",12000,"porsi",true,3,true),
  Product("p3","demo-owner","demo-outlet","c2","Es Teh Fresh Brew",5000,"gelas",true,120,true),
  Product("p4","demo-owner","demo-outlet","c2","Es Teh Lemon",7000,"gelas",true,64,true)
 )
 private val tx=mutableListOf<Transaction>(); private var active:Shift?=null
 override suspend fun registerOwner(email:String,password:String,username:String,pin:CharArray,businessName:String?,outletName:String?,whatsapp:String?):Result<Session>{val s=Session("demo-owner",username,Role.OWNER,"demo-business","demo-outlet");secure.saveCredential(username,s.uid,pin);secure.saveSession(s.uid,username,s.role.name,s.businessId,s.outletId);log("REGISTRATION","REGISTRATION_SUCCESS local development state");return Result.success(s)}
 override suspend fun emailLogin(email:String,password:String)=Result.success(Session("demo-owner","owner",Role.OWNER,"demo-business","demo-outlet"))
 override suspend fun localPinLogin(username:String,pin:CharArray)=runCatching{if(secure.verify(username,pin)==null) error("Username/PIN perangkat tidak valid");Session("demo-owner",username,Role.OWNER,"demo-business","demo-outlet")}
 override suspend fun loadProducts(outletId:String)=products.toList()
 override suspend fun saveProduct(product:Product):Result<Unit> = runCatching {
  products.removeAll { it.id == product.id }
  products.add(product)
  Unit
}
 override suspend fun startShift(session:Session,openingCash:Long)=runCatching{Shift(ownerUid=session.uid,businessId=session.businessId.orEmpty(),outletId=session.outletId.orEmpty(),cashierUid=session.uid,startAt=System.currentTimeMillis(),openingCash=openingCash,syncStatus=SyncStatus.PENDING_SYNC).also{active=it}}
 override suspend fun saveShift(shift:Shift):Result<Unit> = runCatching { active = if (shift.closedAt == null) shift else null; Unit }
 override suspend fun closeShift(shift:Shift,closingCash:Long)=runCatching{shift.copy(closedAt=System.currentTimeMillis(),closingCash=closingCash,syncStatus=SyncStatus.PENDING_SYNC).also{active=null}}
 override suspend fun saveTransaction(t:Transaction):Result<Unit> = runCatching {
  tx.add(t)
  Unit
}
 override suspend fun loadTransactions(session:Session)=tx.toList()
 override suspend fun loadActiveShift(session:Session)=active
 override fun logout(){secure.clearSession()}
}
