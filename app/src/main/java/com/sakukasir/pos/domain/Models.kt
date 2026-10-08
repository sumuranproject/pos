package com.sakukasir.pos.domain

enum class Role { OWNER, CASHIER }
enum class Permission(val id: String, val label: String) {
    POS("pos", "POS"), DASHBOARD("dashboard", "Dashboard"), TRANSACTIONS("transactions", "Transaksi"),
    SHIFT("shift", "Shift"), EXPENSE("expense", "Pengeluaran"), VOID("void", "Void Transaksi"),
    REFUND("refund", "Refund Transaksi"), PRINTER("printer", "Printer"), SYNC("sync", "Sinkronisasi"),
    THEME("theme", "Tema"), PROFILE("profile", "Profil")
}
enum class PaymentMethod { CASH, QRIS }
enum class TransactionStatus { COMPLETED, VOID, REFUNDED, PARTIAL_REFUND }
enum class SyncStatus { PENDING_SYNC, SYNCED, SYNC_ERROR }
enum class RefundMethod { CASH, QRIS }

data class User(
    val username: String,
    val displayName: String,
    val role: Role,
    val outlet: String,
    val permissions: Set<Permission> = Permission.entries.toSet()
) {
    fun can(permission: Permission) = role == Role.OWNER || permissions.contains(permission)
}

data class Outlet(val id: Int, val name: String, val address: String, val active: Boolean = true, val phone: String = "")
data class Worker(
    val id: Int, val name: String, val username: String, val outlet: String,
    val active: Boolean = true, val whatsapp: String = "",
    val permissions: Set<Permission> = Permission.entries.toSet()
)
data class ShiftSummary(val id: String, val start: String, val end: String, val cash: Long, val qris: Long, val tx: Int, val variance: Long)
data class Category(val id: Int, val name: String, val active: Boolean = true)
data class Product(
    val id: Int, val name: String, val price: Long, val unit: String,
    val category: String, val stock: Int, val lowStock: Int, val trackStock: Boolean = true,
    val active: Boolean = true
)
data class CartItem(val productId: Int, val name: String, val price: Long, val qty: Int, val unit: String)
data class RefundItem(val productId: Int, val name: String, val qty: Int, val amount: Long)

data class QrisProof(
    val localPath: String,
    val capturedAt: Long,
    val deviceModel: String,
    val fileSize: Long,
    val expiredAt: Long,
    val url: String? = null,
    val expired: Boolean = false
)

data class Transaction(
    val id: String,
    val timestamp: Long,
    val date: String,
    val time: String,
    val items: List<CartItem>,
    val total: Long,
    val method: PaymentMethod,
    val cashier: String,
    val cashierId: String,
    val outlet: String,
    val discount: Long = 0,
    val tax: Long = 0,
    val taxPct: Int = 0,
    val received: Long = 0,
    val change: Long = 0,
    val status: TransactionStatus = TransactionStatus.COMPLETED,
    val syncStatus: SyncStatus = SyncStatus.PENDING_SYNC,
    val qrisProof: QrisProof? = null,
    val refundAmount: Long = 0,
    val refundMethod: RefundMethod? = null,
    val refundReason: String? = null,
    val refundedAt: Long? = null,
    val refundedBy: String? = null,
    val refundedItems: List<RefundItem> = emptyList()
)

data class Shift(
    val id: String,
    val startAt: Long,
    val endAt: Long? = null,
    val openingCash: Long,
    val closingCash: Long? = null,
    val expectedCash: Long = openingCash,
    val variance: Long? = null,
    val cashSales: Long = 0,
    val qrisSales: Long = 0,
    val transactionCount: Int = 0,
    val cashierId: String
)

data class Expense(
    val id: String,
    val amount: Long,
    val category: String,
    val note: String,
    val date: Long,
    val by: String
)

data class AuditEntry(
    val id: String,
    val type: String,
    val timestamp: Long,
    val trxId: String,
    val amount: Long,
    val isFull: Boolean,
    val by: String,
    val byName: String,
    val reason: String,
    val device: String
)

data class AppNotification(val id: Long, val type: String, val title: String, val body: String, val time: Long, val read: Boolean = false)

data class SecuritySettings(
    val voidWindowMinutes: Int = 5,
    val voidLimitCashier: Long = 50_000,
    val refundLimitCashier: Long = 50_000,
    val ownerPin: String = "1234",
    val alertVoidPerDay: Int = 5
)

data class ReceiptSettings(
    val bizName: String = "Toko Berkah", val showOutlet: Boolean = true, val showTrxNumber: Boolean = true,
    val showCashier: Boolean = true, val showMethod: Boolean = true, val showChange: Boolean = true,
    val footer: String = "Terima kasih sudah berbelanja"
)

data class AppSettings(
    val qrisEnabled: Boolean = true,
    val qrisOutlet: String = "Toko Berkah",
    val qrisRetentionDays: Int = 35,
    val printerName: String = "SK-Printer-58mm",
    val printerConnected: Boolean = false,
    val security: SecuritySettings = SecuritySettings(),
    val receipt: ReceiptSettings = ReceiptSettings(),
    val notifStockLow: Boolean = true,
    val notifStockOut: Boolean = true,
    val notifSystem: Boolean = true
)

data class Totals(val subtotal: Long, val discount: Long, val taxable: Long, val tax: Long, val total: Long, val taxPct: Int)
