package com.sakukasir.pos.util

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class BluetoothPrinter(private val context: Context) {
    private val _status=MutableStateFlow("Disconnected")
    val status:StateFlow<String> = _status.asStateFlow()
    private var socket:BluetoothSocket?=null
    private val uuid=UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun pairedDevices():Set<BluetoothDevice> = BluetoothAdapter.getDefaultAdapter()?.bondedDevices ?: emptySet()
    fun connect(device:BluetoothDevice):Boolean = runCatching {
        socket=device.createRfcommSocketToServiceRecord(uuid);socket!!.connect();_status.value="Connected";true
    }.getOrElse{_status.value="Error";false}
    fun disconnect(){runCatching{socket?.close()};socket=null;_status.value="Disconnected"}
    fun testPrint():Boolean = write("SakuKasir\nTEST PRINT\n\n")
    fun printReceipt(lines:List<String>):Boolean=write(lines.joinToString("\n")+"\n\n")
    private fun write(text:String):Boolean=runCatching{socket?.outputStream?.write(text.toByteArray(Charsets.UTF_8));true}.getOrElse{false}
}
