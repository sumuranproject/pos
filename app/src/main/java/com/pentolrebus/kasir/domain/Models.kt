package com.pentolrebus.kasir.domain

import java.util.UUID

enum class Role { OWNER, CASHIER }
enum class PaymentMethod { CASH, QRIS }
enum class SyncStatus { PENDING_SYNC, SYNCED, SYNC_ERROR }

data class Session(val uid: String, val username: String, val role: Role, val businessId: String?, val outletId: String?)
data class UserProfile(val uid:String, val ownerUid:String, val username:String, val role:Role, val displayName:String?, val businessId:String?, val outletId:String?)
data class Business(val id:String=UUID.randomUUID().toString(), val ownerUid:String="", val name:String="", val whatsapp:String?=null, val address:String?=null, val active:Boolean=true)
data class Outlet(val id:String=UUID.randomUUID().toString(), val ownerUid:String="", val businessId:String?=null, val name:String="", val address:String?=null, val phone:String?=null, val active:Boolean=true)
data class Category(val id:String=UUID.randomUUID().toString(), val ownerUid:String="", val outletId:String="", val name:String="", val active:Boolean=true)
data class Product(val id:String=UUID.randomUUID().toString(), val ownerUid:String="", val outletId:String="", val categoryId:String="", val name:String="", val price:Long=0, val unit:String="pcs", val stockEnabled:Boolean=false, val stock:Long=0, val lowStockThreshold:Long=5, val imagePath:String?=null, val active:Boolean=true)
data class Worker(val id:String=UUID.randomUUID().toString(), val ownerUid:String="", val outletId:String="", val username:String="", val displayName:String="", val role:Role=Role.CASHIER, val phone:String?=null, val active:Boolean=true)
data class Expense(val id:String=UUID.randomUUID().toString(), val ownerUid:String="", val outletId:String="", val title:String="", val category:String="Operasional", val amount:Long=0, val note:String="", val createdAt:Long=System.currentTimeMillis(), val syncStatus:SyncStatus=SyncStatus.PENDING_SYNC)
data class CartItem(val product:Product, val quantity:Int)
data class Shift(val id:String=UUID.randomUUID().toString(), val ownerUid:String="", val businessId:String="", val outletId:String="", val cashierUid:String="", val startAt:Long=0, val openingCash:Long=0, val transactionCount:Int=0, val cashTotal:Long=0, val qrisTotal:Long=0, val closedAt:Long?=null, val closingCash:Long?=null, val syncStatus:SyncStatus=SyncStatus.PENDING_SYNC)
data class TransactionItem(val productId:String, val name:String, val price:Long, val quantity:Int, val subtotal:Long)
data class Transaction(val transactionId:String=UUID.randomUUID().toString(), val ownerUid:String="", val businessId:String="", val outletId:String="", val shiftId:String="", val cashierUid:String="", val items:List<TransactionItem> = emptyList(), val subtotal:Long=0, val total:Long=0, val paymentMethod:PaymentMethod=PaymentMethod.CASH, val paymentStatus:String="PAID", val createdAt:Long=System.currentTimeMillis(), val qrisProofPath:String?=null, val cashReceived:Long=0, val change:Long=0, val discount:Long=0, val syncStatus:SyncStatus=SyncStatus.PENDING_SYNC)
