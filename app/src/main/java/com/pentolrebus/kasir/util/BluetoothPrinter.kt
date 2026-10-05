package com.pentolrebus.kasir.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.pentolrebus.kasir.domain.Transaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** Minimal ESC/POS printer support for paired Bluetooth thermal printers. */
class BluetoothPrinter(private val context: Context) {
    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    data class DeviceInfo(val name: String, val address: String)

    fun hasConnectPermission(): Boolean =
        android.os.Build.VERSION.SDK_INT < 31 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun pairedDevices(): List<DeviceInfo> {
        if (!hasConnectPermission()) return emptyList()
        val manager = context.getSystemService(BluetoothManager::class.java)
        val adapter: BluetoothAdapter = manager?.adapter ?: return emptyList()
        return adapter.bondedDevices.orEmpty()
            .sortedBy { it.name ?: it.address }
            .map { DeviceInfo(it.name?.takeIf(String::isNotBlank) ?: "Printer", it.address) }
    }

    @SuppressLint("MissingPermission")
    fun print(transaction: Transaction, address: String): Result<Unit> = runCatching {
        if (!hasConnectPermission()) error("Izin Bluetooth diperlukan")
        val manager = context.getSystemService(BluetoothManager::class.java) ?: error("Bluetooth tidak tersedia")
        val adapter = manager.adapter ?: error("Bluetooth tidak tersedia")
        val device: BluetoothDevice = adapter.getRemoteDevice(address)
        val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
        try {
            adapter.cancelDiscovery()
            socket.connect()
            socket.outputStream.use { output ->
                output.write(buildReceipt(transaction))
                output.flush()
            }
        } finally {
            runCatching { socket.close() }
        }
        Unit
    }

    private fun buildReceipt(t: Transaction): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        fun b(vararg bytes: Int) { out.write(bytes.map { (it and 0xFF).toByte() }.toByteArray()) }
        fun text(value: String) { out.write(value.toByteArray(Charsets.UTF_8)) }
        fun line(value: String = "") { text(value); text("\n") }
        val nf = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
        fun rp(v: Long) = nf.format(v).replace("Rp", "Rp ")

        b(0x1B, 0x40)
        b(0x1B, 0x61, 0x01)
        b(0x1B, 0x45, 0x01)
        line("SAKUKASIR")
        b(0x1B, 0x45, 0x00)
        line("Pentol Rebus x Es Teh Fresh Brew")
        line(SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(Date(t.createdAt)))
        line("--------------------------------")
        b(0x1B, 0x61, 0x00)
        t.items.forEach { item ->
            line(item.name.take(28))
            line("${item.quantity} x ${rp(item.price)} = ${rp(item.subtotal)}")
        }
        line("--------------------------------")
        line("Subtotal : ${rp(t.subtotal)}")
        line("TOTAL    : ${rp(t.total)}")
        line("Bayar    : ${t.paymentMethod}")
        line("Status   : ${t.paymentStatus}")
        line("ID       : ${t.transactionId.take(18)}")
        b(0x1B, 0x61, 0x01)
        line("Terima kasih")
        line()
        line()
        b(0x1D, 0x56, 0x00)
        return out.toByteArray()
    }
}
