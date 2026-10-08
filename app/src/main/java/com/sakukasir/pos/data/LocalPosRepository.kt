package com.sakukasir.pos.data

import com.sakukasir.pos.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class LocalPosRepository(
    private val store: OfflineStore,
    private val queue: OfflineSyncQueue
) : PosRepository {
    private val _transactions = MutableStateFlow(seedTransactions())
    private val _expenses = MutableStateFlow(seedExpenses())
    private val _audit = MutableStateFlow<List<AuditEntry>>(emptyList())
    private val _notifications = MutableStateFlow(seedNotifications())
    private val _products = MutableStateFlow(seedProducts())
    private val _categories = MutableStateFlow(seedCategories())
    private val _outlets = MutableStateFlow(seedOutlets())
    private val _workers = MutableStateFlow(seedWorkers())
    private val _shiftHistory = MutableStateFlow(listOf(ShiftSummary("SH-041","08:15","16:30",850000,420000,24,0)))
    private val _settings = MutableStateFlow(AppSettings())
    private val _shift = MutableStateFlow<Shift?>(null)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val persistenceReady = CompletableDeferred<Boolean>()

    init {
        ioScope.launch {
            store.transactions.collectLatest { rows ->
                if (!persistenceReady.isCompleted) persistenceReady.complete(rows.isNotEmpty())
                if (rows.isNotEmpty()) _transactions.value = rows.map(::toTransaction)
            }
        }
    }

    override val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()
    override val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()
    override val audit: StateFlow<List<AuditEntry>> = _audit.asStateFlow()
    override val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()
    override val products: StateFlow<List<Product>> = _products.asStateFlow()
    override val categories: StateFlow<List<Category>> = _categories.asStateFlow()
    override val outlets: StateFlow<List<Outlet>> = _outlets.asStateFlow()
    override val workers: StateFlow<List<Worker>> = _workers.asStateFlow()
    override val shiftHistory: StateFlow<List<ShiftSummary>> = _shiftHistory.asStateFlow()
    override val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    override val activeShift: StateFlow<Shift?> = _shift.asStateFlow()
    override val syncQueue = queue.items

    override suspend fun addTransaction(tx: Transaction) {
        val hasPersistedRows = persistenceReady.await()
        if (!hasPersistedRows) {
            _transactions.value.forEach { store.save(toEntity(it)) }
        }
        _transactions.value = listOf(tx) + _transactions.value
        store.save(toEntity(tx))
        queue.enqueue(SyncQueueItem(tx.id, "transaction", tx.id))
    }

    override suspend fun updateTransaction(tx: Transaction) {
        persistenceReady.await()
        _transactions.value = _transactions.value.map { if (it.id == tx.id) tx else it }
        store.save(toEntity(tx))
        queue.enqueue(SyncQueueItem(tx.id, "transaction_update", tx.id))
    }

    override suspend fun addExpense(expense: Expense) {
        _expenses.value = listOf(expense) + _expenses.value
        queue.enqueue(SyncQueueItem(expense.id, "expense", expense.id))
    }

    override suspend fun deleteExpense(id: String) {
        _expenses.value = _expenses.value.filterNot { it.id == id }
        queue.enqueue(SyncQueueItem(id, "expense_delete", id))
    }

    override suspend fun addAudit(entry: AuditEntry) { _audit.value = listOf(entry) + _audit.value }
    override suspend fun addNotification(item: AppNotification) { _notifications.value = listOf(item) + _notifications.value }
    override suspend fun markNotificationsRead() { _notifications.value = _notifications.value.map { it.copy(read = true) } }

    override suspend fun addProduct(product: Product) {
        _products.value = _products.value + product
    }

    override suspend fun updateProduct(product: Product) {
        _products.value = _products.value.map { if (it.id == product.id) product else it }
    }

    override suspend fun deleteProduct(id: Int) { _products.value = _products.value.filterNot { it.id == id } }
    override suspend fun upsertCategory(category: Category) {
        _categories.value = if (_categories.value.any { it.id == category.id }) _categories.value.map { if (it.id == category.id) category else it } else _categories.value + category
    }
    override suspend fun deleteCategory(id: Int) { _categories.value = _categories.value.filterNot { it.id == id } }
    override suspend fun upsertOutlet(outlet: Outlet) {
        _outlets.value = if (_outlets.value.any { it.id == outlet.id }) _outlets.value.map { if (it.id == outlet.id) outlet else it } else _outlets.value + outlet
    }
    override suspend fun deleteOutlet(id: Int) { _outlets.value = _outlets.value.filterNot { it.id == id } }
    override suspend fun upsertWorker(worker: Worker) {
        _workers.value = if (_workers.value.any { it.id == worker.id }) _workers.value.map { if (it.id == worker.id) worker else it } else _workers.value + worker
    }
    override suspend fun updateExpense(expense: Expense) { _expenses.value = _expenses.value.map { if (it.id == expense.id) expense else it } }
    override suspend fun addShiftHistory(item: ShiftSummary) { _shiftHistory.value = listOf(item) + _shiftHistory.value }
    override suspend fun updateSettings(settings: AppSettings) { _settings.value = settings }
    override suspend fun setShift(shift: Shift?) { _shift.value = shift }

    override suspend fun sync() {
        kotlinx.coroutines.delay(250)
        queue.items.value.forEach { queue.markSynced(it.id) }
        _transactions.value = _transactions.value.map { it.copy(syncStatus = SyncStatus.SYNCED) }
    }

    private fun toEntity(tx: Transaction): TransactionEntity = TransactionEntity(
        id = tx.id, timestamp = tx.timestamp, date = tx.date, time = tx.time,
        itemsJson = tx.items.joinToString("|") { "${it.productId}:${it.qty}:${it.price}:${it.name.replace("|", " ")}:${it.unit.replace("|", " ")}" },
        total = tx.total, method = tx.method.name, cashier = tx.cashier, cashierId = tx.cashierId, outlet = tx.outlet,
        discount = tx.discount, tax = tx.tax, taxPct = tx.taxPct, received = tx.received, change = tx.change,
        status = tx.status.name, syncStatus = tx.syncStatus.name, qrisProofJson = tx.qrisProof?.let {
            JSONObject().apply { put("localPath", it.localPath); put("capturedAt", it.capturedAt); put("deviceModel", it.deviceModel); put("fileSize", it.fileSize); put("expiredAt", it.expiredAt); put("url", it.url) }.toString()
        },
        refundAmount = tx.refundAmount, refundMethod = tx.refundMethod?.name, refundReason = tx.refundReason,
        refundedAt = tx.refundedAt, refundedBy = tx.refundedBy,
        refundedItemsJson = JSONArray().apply { tx.refundedItems.forEach { item -> put(JSONObject().apply { put("productId", item.productId); put("name", item.name); put("qty", item.qty); put("amount", item.amount) }) } }.toString()
    )

    private fun toTransaction(e: TransactionEntity): Transaction {
        val items = e.itemsJson.split("|").filter { it.isNotBlank() }.mapNotNull { raw ->
            val p = raw.split(":", limit = 5)
            if (p.size < 5) null else CartItem(p[0].toIntOrNull() ?: return@mapNotNull null, p[3], p[2].toLongOrNull() ?: return@mapNotNull null, p[1].toIntOrNull() ?: return@mapNotNull null, p[4])
        }
        val proof = e.qrisProofJson?.let { raw -> runCatching {
            val o=JSONObject(raw); QrisProof(o.getString("localPath"),o.getLong("capturedAt"),o.getString("deviceModel"),o.getLong("fileSize"),o.getLong("expiredAt"),o.optString("url").takeIf{it.isNotBlank()})
        }.getOrNull() }
        val refunded = runCatching {
            val a=JSONArray(e.refundedItemsJson); buildList { for(i in 0 until a.length()){val o=a.getJSONObject(i);add(RefundItem(o.getInt("productId"),o.getString("name"),o.getInt("qty"),o.getLong("amount"))) } }
        }.getOrDefault(emptyList())
        return Transaction(e.id,e.timestamp,e.date,e.time,items,e.total,PaymentMethod.valueOf(e.method),e.cashier,e.cashierId,e.outlet,e.discount,e.tax,e.taxPct,e.received,e.change,TransactionStatus.valueOf(e.status),runCatching{SyncStatus.valueOf(e.syncStatus)}.getOrDefault(SyncStatus.PENDING_SYNC),proof,e.refundAmount,e.refundMethod?.let{runCatching{RefundMethod.valueOf(it)}.getOrNull()},e.refundReason,e.refundedAt,e.refundedBy,refunded)
    }

    companion object {
        private fun date(ts: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(ts))
        private fun time(ts: Long): String = SimpleDateFormat("HH:mm", Locale.US).format(Date(ts))
        private fun seedProducts() = listOf(
            Product(1,"Kopi Susu Gula Aren",18000,"cup","Minuman",24,5),
            Product(2,"Teh Manis",8000,"gelas","Minuman",3,5),
            Product(3,"Roti Bakar Coklat",15000,"porsi","Makanan",12,3),
            Product(4,"Nasi Goreng Spesial",25000,"porsi","Makanan",8,3),
            Product(5,"Air Mineral 600ml",5000,"botol","Minuman",0,5),
            Product(6,"Keripik Singkong",12000,"pack","Snack",40,10),
            Product(7,"Es Krim Vanilla",10000,"cup","Snack",15,5),
            Product(8,"Mie Instan Goreng",12000,"porsi","Makanan",2,5)
        )
        private fun seedCategories() = listOf(Category(1,"Makanan"),Category(2,"Minuman"),Category(3,"Snack"),Category(4,"Lainnya"))
        private fun seedOutlets() = listOf(Outlet(1,"Toko Berkah","Jl. Merdeka 12",true,"0812-1111-2222"),Outlet(2,"Cabang Pasar","Pasar Baru Blok C",true,"0812-3333-4444"))
        private fun seedExpenses(): List<Expense> {
            fun at(dayOffset: Int, h: Int, m: Int): Long = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, dayOffset); set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0)
            }.timeInMillis
            return listOf(
                Expense("EXP-001",150000,"Bahan","Beli kopi 2kg",at(0,8,0),"Budi"),
                Expense("EXP-002",50000,"Operasional","Token listrik",at(0,9,30),"Budi"),
                Expense("EXP-003",120000,"Gaji","Kasbon Andi",at(-1,17,0),"Budi")
            )
        }
        private fun seedWorkers() = listOf(
            Worker(1,"Andi Wijaya","kasir","Toko Berkah",true,"0812-5555-6666"),
            Worker(2,"Siti Aminah","kasir2","Cabang Pasar",true,"0812-7777-8888",setOf(Permission.POS,Permission.TRANSACTIONS,Permission.SHIFT,Permission.PRINTER,Permission.SYNC,Permission.THEME,Permission.PROFILE)),
            Worker(3,"Rudi Hartono","kasir3","Toko Berkah",false,"0812-9999-0000",setOf(Permission.POS,Permission.PRINTER,Permission.THEME,Permission.PROFILE))
        )
        private fun seedNotifications() = listOf(
            AppNotification(1,"stock_low","Stok menipis","Teh Manis · sisa 3",System.currentTimeMillis()-1800000),
            AppNotification(2,"stock_low","Stok menipis","Mie Instan · sisa 2",System.currentTimeMillis()-2700000),
            AppNotification(3,"system","Sinkronisasi berhasil","3 transaksi tersinkron",System.currentTimeMillis()-3600000,true)
        )
        private fun seedTransactions(): List<Transaction> {
            val now = System.currentTimeMillis()
            return listOf(
                Transaction("TRX-20261006-0042",now-3600000,date(now-3600000),"14:32",listOf(CartItem(1,"Kopi Susu",18000,2,"cup")),38000,PaymentMethod.CASH,"Andi Wijaya","kasir","Toko Berkah",received=50000,change=12000,syncStatus=SyncStatus.SYNCED),
                Transaction("TRX-20261006-0041",now-5000000,date(now-5000000),"14:05",listOf(CartItem(2,"Teh Manis",8000,1,"gelas")),18000,PaymentMethod.QRIS,"Andi Wijaya","kasir","Toko Berkah",syncStatus=SyncStatus.SYNCED),
                Transaction("TRX-20261006-0040",now-7000000,date(now-7000000),"13:48",listOf(CartItem(4,"Nasi Goreng",25000,3,"porsi")),97000,PaymentMethod.CASH,"Andi Wijaya","kasir","Toko Berkah",discount=2000,received=100000,change=3000,syncStatus=SyncStatus.PENDING_SYNC),
                Transaction("TRX-20261006-0039",now-9000000,date(now-9000000),"13:12",listOf(CartItem(3,"Roti Bakar",15000,2,"porsi")),33000,PaymentMethod.CASH,"Andi Wijaya","kasir","Toko Berkah",received=50000,change=17000,syncStatus=SyncStatus.SYNCED),
                Transaction("TRX-20261006-0038",now-11000000,date(now-11000000),"12:55",listOf(CartItem(7,"Es Krim",10000,4,"cup")),62000,PaymentMethod.QRIS,"Andi Wijaya","kasir","Toko Berkah",syncStatus=SyncStatus.SYNC_ERROR)
            )
        }
    }
}
