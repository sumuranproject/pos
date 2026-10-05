package com.pentolrebus.kasir.data

import android.content.Context
import com.pentolrebus.kasir.domain.*
import org.json.JSONArray
import org.json.JSONObject

/** Durable local-first store for transactions/shifts. No secrets are stored here. */
class OfflineStore(context: Context) {
    private val txFile = context.getFileStreamPath("offline-transactions.json")
    private val shiftFile = context.getFileStreamPath("offline-shifts.json")

    @Synchronized fun saveTransaction(t: Transaction) {
        val all = transactions().toMutableList()
        val i = all.indexOfFirst { it.transactionId == t.transactionId }
        if (i >= 0) all[i] = t else all.add(t)
        writeTransactions(all)
    }

    @Synchronized fun updateTransactionStatus(id: String, status: SyncStatus) {
        val all = transactions().map { if (it.transactionId == id) it.copy(syncStatus = status) else it }
        writeTransactions(all)
    }

    @Synchronized fun transactions(): List<Transaction> {
        if (!txFile.exists()) return emptyList()
        return runCatching {
            val a = JSONArray(txFile.readText())
            (0 until a.length()).mapNotNull { parseTransaction(a.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    @Synchronized fun saveShift(s: Shift) {
        val all = shifts().toMutableList()
        val i = all.indexOfFirst { it.id == s.id }
        if (i >= 0) all[i] = s else all.add(s)
        writeShifts(all)
    }

    @Synchronized fun updateShiftStatus(id: String, status: SyncStatus) {
        val all = shifts().map { if (it.id == id) it.copy(syncStatus = status) else it }
        writeShifts(all)
    }

    @Synchronized fun shifts(): List<Shift> {
        if (!shiftFile.exists()) return emptyList()
        return runCatching {
            val a = JSONArray(shiftFile.readText())
            (0 until a.length()).mapNotNull { parseShift(a.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    private val expFile = context.getFileStreamPath("offline-expenses.json")

    @Synchronized fun saveExpense(e: Expense) {
        val all = expenses().toMutableList()
        val i = all.indexOfFirst { it.id == e.id }
        if (i >= 0) all[i] = e else all.add(e)
        writeExpenses(all)
    }

    @Synchronized fun deleteExpense(id: String) { writeExpenses(expenses().filterNot { it.id == id }) }

    @Synchronized fun updateExpenseStatus(id: String, status: SyncStatus) {
        writeExpenses(expenses().map { if (it.id == id) it.copy(syncStatus = status) else it })
    }

    @Synchronized fun expenses(): List<Expense> {
        if (!expFile.exists()) return emptyList()
        return runCatching {
            val a = JSONArray(expFile.readText())
            (0 until a.length()).map { val o = a.getJSONObject(it)
                Expense(id = o.optString("id"), ownerUid = o.optString("ownerUid"), businessId = o.optString("businessId"), outletId = o.optString("outletId"),
                    amount = o.optLong("amount"), category = o.optString("category"), note = o.optString("note"), createdAt = o.optLong("createdAt"),
                    createdBy = o.optString("createdBy"), createdByName = o.optString("createdByName"),
                    syncStatus = SyncStatus.valueOf(o.optString("syncStatus", SyncStatus.PENDING_SYNC.name)))
            }
        }.getOrDefault(emptyList())
    }

    private fun writeExpenses(list: List<Expense>) {
        val a = JSONArray()
        list.forEach { e -> a.put(JSONObject().apply {
            put("id", e.id); put("ownerUid", e.ownerUid); put("businessId", e.businessId); put("outletId", e.outletId)
            put("amount", e.amount); put("category", e.category); put("note", e.note); put("createdAt", e.createdAt)
            put("createdBy", e.createdBy); put("createdByName", e.createdByName); put("syncStatus", e.syncStatus.name) }) }
        expFile.writeText(a.toString())
    }

    private fun writeTransactions(list: List<Transaction>) {
        val a = JSONArray()
        list.forEach { a.put(transactionJson(it)) }
        txFile.writeText(a.toString())
    }

    private fun writeShifts(list: List<Shift>) {
        val a = JSONArray()
        list.forEach { a.put(shiftJson(it)) }
        shiftFile.writeText(a.toString())
    }

    private fun transactionJson(t: Transaction) = JSONObject().apply {
        put("transactionId", t.transactionId); put("ownerUid", t.ownerUid); put("businessId", t.businessId)
        put("outletId", t.outletId); put("shiftId", t.shiftId); put("cashierUid", t.cashierUid)
        put("subtotal", t.subtotal); put("total", t.total); put("discount", t.discount); put("tax", t.tax); put("taxPercent", t.taxPercent); put("cashReceived", t.cashReceived); put("paymentMethod", t.paymentMethod.name)
        put("paymentStatus", t.paymentStatus); put("createdAt", t.createdAt)
        put("qrisProofPath", t.qrisProofPath ?: JSONObject.NULL); put("syncStatus", t.syncStatus.name)
        val items = JSONArray(); t.items.forEach { item ->
            items.put(JSONObject().apply {
                put("productId", item.productId); put("name", item.name); put("price", item.price)
                put("quantity", item.quantity); put("subtotal", item.subtotal)
            })
        }; put("items", items)
    }

    private fun parseTransaction(o: JSONObject): Transaction? = runCatching {
        val itemsJson = o.optJSONArray("items") ?: JSONArray()
        val items = (0 until itemsJson.length()).map { i ->
            val x = itemsJson.getJSONObject(i)
            TransactionItem(x.optString("productId"), x.optString("name"), x.optLong("price"), x.optInt("quantity"), x.optLong("subtotal"))
        }
        Transaction(
            transactionId = o.optString("transactionId"), ownerUid = o.optString("ownerUid"), businessId = o.optString("businessId"),
            outletId = o.optString("outletId"), shiftId = o.optString("shiftId"), cashierUid = o.optString("cashierUid"),
            items = items, subtotal = o.optLong("subtotal"), total = o.optLong("total"), discount = o.optLong("discount"), tax = o.optLong("tax"), taxPercent = o.optLong("taxPercent"), cashReceived = o.optLong("cashReceived"),
            paymentMethod = PaymentMethod.valueOf(o.optString("paymentMethod", PaymentMethod.CASH.name)),
            paymentStatus = o.optString("paymentStatus", "PAID"), createdAt = o.optLong("createdAt"),
            qrisProofPath = if (o.isNull("qrisProofPath")) null else o.optString("qrisProofPath"),
            syncStatus = SyncStatus.valueOf(o.optString("syncStatus", SyncStatus.PENDING_SYNC.name))
        )
    }.getOrNull()

    private fun shiftJson(s: Shift) = JSONObject().apply {
        put("id", s.id); put("ownerUid", s.ownerUid); put("businessId", s.businessId); put("outletId", s.outletId)
        put("cashierUid", s.cashierUid); put("startAt", s.startAt); put("openingCash", s.openingCash)
        put("transactionCount", s.transactionCount); put("cashTotal", s.cashTotal); put("qrisTotal", s.qrisTotal)
        put("closedAt", s.closedAt ?: JSONObject.NULL); put("closingCash", s.closingCash ?: JSONObject.NULL); put("syncStatus", s.syncStatus.name)
    }

    private fun parseShift(o: JSONObject): Shift? = runCatching {
        Shift(
            id = o.optString("id"), ownerUid = o.optString("ownerUid"), businessId = o.optString("businessId"), outletId = o.optString("outletId"),
            cashierUid = o.optString("cashierUid"), startAt = o.optLong("startAt"), openingCash = o.optLong("openingCash"),
            transactionCount = o.optInt("transactionCount"), cashTotal = o.optLong("cashTotal"), qrisTotal = o.optLong("qrisTotal"),
            closedAt = if (o.isNull("closedAt")) null else o.optLong("closedAt"),
            closingCash = if (o.isNull("closingCash")) null else o.optLong("closingCash"),
            syncStatus = SyncStatus.valueOf(o.optString("syncStatus", SyncStatus.PENDING_SYNC.name))
        )
    }.getOrNull()
}
