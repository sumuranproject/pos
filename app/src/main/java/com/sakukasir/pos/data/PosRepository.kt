package com.sakukasir.pos.data

import com.sakukasir.pos.domain.*
import kotlinx.coroutines.flow.StateFlow

interface PosRepository {
    val transactions: StateFlow<List<Transaction>>
    val expenses: StateFlow<List<Expense>>
    val audit: StateFlow<List<AuditEntry>>
    val notifications: StateFlow<List<AppNotification>>
    val products: StateFlow<List<Product>>
    val categories: StateFlow<List<Category>>
    val outlets: StateFlow<List<Outlet>>
    val workers: StateFlow<List<Worker>>
    val shiftHistory: StateFlow<List<ShiftSummary>>
    val settings: StateFlow<AppSettings>
    val activeShift: StateFlow<Shift?>
    val syncQueue: StateFlow<List<SyncQueueItem>>
    suspend fun addTransaction(tx: Transaction)
    suspend fun updateTransaction(tx: Transaction)
    suspend fun addExpense(expense: Expense)
    suspend fun deleteExpense(id: String)
    suspend fun addAudit(entry: AuditEntry)
    suspend fun addNotification(item: AppNotification)
    suspend fun markNotificationsRead()
    suspend fun addProduct(product: Product)
    suspend fun updateProduct(product: Product)
    suspend fun deleteProduct(id: Int)
    suspend fun upsertCategory(category: Category)
    suspend fun deleteCategory(id: Int)
    suspend fun upsertOutlet(outlet: Outlet)
    suspend fun deleteOutlet(id: Int)
    suspend fun upsertWorker(worker: Worker)
    suspend fun updateExpense(expense: Expense)
    suspend fun addShiftHistory(item: ShiftSummary)
    suspend fun updateSettings(settings: AppSettings)
    suspend fun setShift(shift: Shift?)
    suspend fun sync()
}
