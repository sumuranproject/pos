package com.sakukasir.pos.ui

import android.content.Context
import com.sakukasir.pos.domain.ReceiptSettings
import com.sakukasir.pos.domain.Transaction
import com.sakukasir.pos.util.BluetoothPrinter

/** One printer connection shared by the printer page, success screen and transaction detail. */
object PrinterHolder {
    @Volatile private var instance: BluetoothPrinter? = null
    fun get(context: Context): BluetoothPrinter = instance ?: synchronized(this) {
        instance ?: BluetoothPrinter(context.applicationContext).also { instance = it }
    }
}

private fun pad(l: String, r: String, w: Int = 32): String { val sp = (w - l.length - r.length).coerceAtLeast(1); return l + " ".repeat(sp) + r }

fun receiptLines(tx: Transaction, r: ReceiptSettings): List<String> = buildList {
    val line = "-".repeat(32)
    add(r.bizName); add(line)
    if (r.showTrxNumber) add(tx.id)
    add("${tx.date} · ${tx.time}")
    if (r.showOutlet) add(tx.outlet)
    if (r.showCashier) add("Kasir: ${tx.cashier}")
    add(line)
    tx.items.forEach { add(pad("${it.name.take(14)}  ${it.qty}x ${it.price / 1000}rb", rupiah(it.price * it.qty).removePrefix("Rp "))) }
    add(line)
    add(pad("Subtotal", rupiah(tx.items.sumOf { it.price * it.qty }).removePrefix("Rp ")))
    if (tx.discount > 0) add(pad("Diskon", "-" + rupiah(tx.discount).removePrefix("Rp ")))
    if (tx.tax > 0) add(pad("Pajak ${tx.taxPct}%", rupiah(tx.tax).removePrefix("Rp ")))
    add(pad("TOTAL", rupiah(tx.total).removePrefix("Rp ")))
    if (r.showMethod) add(pad(if (tx.method.name == "CASH") "Cash" else "QRIS", rupiah(if (tx.received > 0) tx.received else tx.total).removePrefix("Rp ")))
    if (r.showChange && tx.method.name == "CASH") add(pad("Kembali", rupiah(tx.change).removePrefix("Rp ")))
    add(line)
    if (r.footer.isNotBlank()) add(r.footer)
}
