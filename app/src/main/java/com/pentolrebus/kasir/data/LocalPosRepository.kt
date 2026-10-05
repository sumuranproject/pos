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
 override suspend fun registerOwner(email:String,password:String,username:String,displayName:String,businessName:String?,outletName:String?,whatsapp:String?):Result<Session>{val s=Session("demo-owner",username,Role.OWNER,"demo-business","demo-outlet");secure.saveSession(s.uid,username,s.role.name,s.businessId,s.outletId);log("REGISTRATION","REGISTRATION_SUCCESS local development state");return Result.success(s)}
 override suspend fun usernameLogin(username:String,password:String)=Result.success(Session("demo-owner",username.trim(),Role.OWNER,"demo-business","demo-outlet"))
 override suspend fun sendPasswordReset(username:String):Result<Unit> = Result.success(Unit)
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
 private val cats=mutableListOf<Category>(); private val outs=mutableListOf(Outlet("demo-outlet","demo-owner","demo-business","Outlet Demo",null)); private val wks=mutableListOf<Worker>(); private val exps=mutableListOf<Expense>(); private var biz:Business?=Business("demo-business","demo-owner","Demo")
 override suspend fun deleteProduct(outletId:String,id:String)=runCatching{products.removeAll{it.id==id};Unit}
 override suspend fun adjustStock(outletId:String,productId:String,delta:Long)=runCatching{val i=products.indexOfFirst{it.id==productId};if(i>=0)products[i]=products[i].copy(stock=products[i].stock+delta);Unit}
 override suspend fun loadCategories(outletId:String)=cats.toList()
 override suspend fun saveCategory(c:Category)=runCatching{cats.removeAll{it.id==c.id};cats.add(c);Unit}
 override suspend fun deleteCategory(outletId:String,id:String)=runCatching{cats.removeAll{it.id==id};Unit}
 override suspend fun loadOutlets(session:Session)=outs.toList()
 override suspend fun saveOutlet(o:Outlet)=runCatching{outs.removeAll{it.id==o.id};outs.add(o);Unit}
 override suspend fun loadWorkers(session:Session)=wks.toList()
 override suspend fun saveWorker(w:Worker,password:String?)=runCatching{wks.removeAll{it.id==w.id};wks.add(w);Unit}
 override suspend fun loadBusiness(session:Session)=biz
 override suspend fun saveBusiness(b:Business)=runCatching{biz=b;Unit}
 override suspend fun loadOutletTransactions(outletId:String)=tx.filter{it.outletId==outletId}
 override suspend fun loadOutletShifts(outletId:String)=listOfNotNull(active)
 override suspend fun loadExpenses(outletId:String)=exps.filter{it.outletId==outletId}
 override suspend fun saveExpense(e:Expense)=runCatching{exps.removeAll{it.id==e.id};exps.add(e);Unit}
 override suspend fun deleteExpense(outletId:String,id:String)=runCatching{exps.removeAll{it.id==id};Unit}
 override suspend fun updateProfile(session:Session,displayName:String,whatsapp:String?)=Result.success(Unit)
 override fun logout(){secure.clearSession()}
}
