package com.sakukasir.pos.util

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class BluetoothPrinter(private val context: Context) {
    private val _status = MutableStateFlow("Disconnected")
    val status: StateFlow<String> = _status.asStateFlow()
    private var socket: BluetoothSocket? = null
    private val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private fun hasBluetoothPermission(): Boolean =
        android.os.Build.VERSION.SDK_INT < 31 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    fun pairedDevices(): Set<BluetoothDevice> {
        if (!hasBluetoothPermission()) {
            _status.value = "Permission required"
            return emptySet()
        }
        return runCatching { BluetoothAdapter.getDefaultAdapter()?.bondedDevices ?: emptySet() }
            .getOrElse { _status.value = "Error"; emptySet() }
    }

    fun connect(device: BluetoothDevice): Boolean {
        if (!hasBluetoothPermission()) {
            _status.value = "Permission required"
            return false
        }
        return runCatching {
            disconnect()
            socket = device.createRfcommSocketToServiceRecord(uuid)
            socket!!.connect()
            _status.value = "Connected"
            true
        }.getOrElse {
            runCatching { socket?.close() }
            socket = null
            _status.value = "Error"
            false
        }
    }

    fun disconnect() {
        runCatching { socket?.close() }
        socket = null
        _status.value = "Disconnected"
    }

    fun testPrint(): Boolean = write("SakuKasir\nTEST PRINT\n\n")
    fun printReceipt(lines: List<String>): Boolean = write(lines.joinToString("\n") + "\n\n")

    private fun write(text: String): Boolean {
        val current = socket ?: run {
            _status.value = "Disconnected"
            return false
        }
        return runCatching {
            current.outputStream.write(text.toByteArray(Charsets.UTF_8))
            current.outputStream.flush()
            true
        }.getOrElse {
            _status.value = "Error"
            false
        }
    }
}
