package com.pentolrebus.kasir.data

import com.pentolrebus.kasir.domain.*

class LocalPosRepository(private val secure:SecureLocalStore, private val log:(String,String)->Unit):PosRepository {
 private val products=mutableListOf(Product("p1","demo-owner","demo-outlet","c1","Pentol Rebus",12000,"porsi",true,48,5,null,true),Product("p2","demo-owner","demo-outlet","c1","Pentol Pedas",12000,"porsi",true,3,5,null,true),Product("p3","demo-owner","demo-outlet","c2","Es Teh Fresh Brew",5000,"gelas",true,120,10,null,true),Product("p4","demo-owner","demo-outlet","c2","Es Teh Lemon",7000,"gelas",true,64,10,null,true))
 private val categories=mutableListOf(Category("c1","demo-owner","demo-outlet","Pentol"),Category("c2","demo-owner","demo-outlet","Es Teh"),Category("c3","demo-owner","demo-outlet","Tambahan"))
 private val outlets=mutableListOf(Outlet("demo-outlet","demo-owner","demo-business","Outlet Alun-Alun","Jl. Alun-Alun Jember"))
 private val workers=mutableListOf(Worker("w1","demo-owner","demo-outlet","dewi","Dewi",Role.CASHIER,null,true))
 private var business=Business("demo-business","demo-owner","Pentol Rebus x Es Teh Fresh Brew")
 private val expenses=mutableListOf<Expense>()
 private val tx=mutableListOf<Transaction>(); private var active:Shift?=null
 override suspend fun registerOwner(email:String,password:String,username:String,pin:CharArray,businessName:String?,outletName:String?,whatsapp:String?)=Result.success(Session("demo-owner",username,Role.OWNER,"demo-business","demo-outlet")).also{secure.saveCredential(username,"demo-owner",pin);secure.saveSession("demo-owner",username,"OWNER","demo-business","demo-outlet")}
 override suspend fun emailLogin(email:String,password:String)=Result.success(Session("demo-owner","owner",Role.OWNER,"demo-business","demo-outlet"))
 override suspend fun localPinLogin(username:String,pin:CharArray)=runCatching{if(secure.verify(username,pin)==null)error("Username/PIN perangkat tidak valid");Session("demo-owner",username,Role.OWNER,"demo-business","demo-outlet")}
 override suspend fun loadProducts(outletId:String)=products.filter{it.outletId==outletId}
 override suspend fun saveProduct(product:Product):Result<Unit> =Result.success(Unit).also{products.removeAll{it.id==product.id};products.add(product)}
 override suspend fun deleteProduct(product:Product)=Result.success(Unit).also{products.removeAll{it.id==product.id}}
 override suspend fun loadCategories(outletId:String)=categories.filter{it.outletId==outletId}
 override suspend fun saveCategory(category:Category)=Result.success(Unit).also{categories.removeAll{it.id==category.id};categories.add(category)}
 override suspend fun deleteCategory(category:Category)=Result.success(Unit).also{categories.removeAll{it.id==category.id}}
 override suspend fun loadOutlets(ownerUid:String)=outlets.filter{it.ownerUid==ownerUid}
 override suspend fun saveOutlet(outlet:Outlet)=Result.success(Unit).also{outlets.removeAll{it.id==outlet.id};outlets.add(outlet)}
 override suspend fun deleteOutlet(outlet:Outlet)=Result.success(Unit).also{outlets.removeAll{it.id==outlet.id}}
 override suspend fun loadWorkers(outletId:String)=workers.filter{it.outletId==outletId}
 override suspend fun saveWorker(worker:Worker)=Result.success(Unit).also{workers.removeAll{it.id==worker.id};workers.add(worker)}
 override suspend fun deleteWorker(worker:Worker)=Result.success(Unit).also{workers.removeAll{it.id==worker.id}}
 override suspend fun loadBusiness(ownerUid:String)=business.takeIf{it.ownerUid==ownerUid}
 override suspend fun saveBusiness(business:Business)=Result.success(Unit).also{this.business=business}
 override suspend fun loadExpenses(outletId:String)=expenses.filter{it.outletId==outletId}.sortedByDescending{it.createdAt}
 override suspend fun saveExpense(expense:Expense)=Result.success(Unit).also{expenses.removeAll{it.id==expense.id};expenses.add(expense)}
 override suspend fun deleteExpense(expense:Expense)=Result.success(Unit).also{expenses.removeAll{it.id==expense.id}}
 override suspend fun startShift(session:Session,openingCash:Long)=Result.success(Shift(ownerUid=session.uid,businessId=session.businessId.orEmpty(),outletId=session.outletId.orEmpty(),cashierUid=session.uid,startAt=System.currentTimeMillis(),openingCash=openingCash)).also{active=it.getOrNull()}
 override suspend fun saveShift(shift:Shift)=Result.success(Unit).also{active=if(shift.closedAt==null)shift else null}
 override suspend fun closeShift(shift:Shift,closingCash:Long)=Result.success(shift.copy(closedAt=System.currentTimeMillis(),closingCash=closingCash)).also{active=null}
 override suspend fun saveTransaction(t:Transaction):Result<Unit> =Result.success(Unit).also{tx.removeAll{it.transactionId==t.transactionId};tx.add(t)}
 override suspend fun loadTransactions(session:Session)=tx.filter{session.role==Role.OWNER||it.cashierUid==session.uid}
 override suspend fun loadActiveShift(session:Session)=active
 override fun logout(){secure.clearSession()}
}
