package com.juragan38.pos.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.OutputStream
import java.util.UUID

object BluetoothPrinter {

    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    fun pairedPrinters(): List<BluetoothDevice> {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
            if (!adapter.isEnabled) return emptyList()
            adapter.bondedDevices?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    fun print(device: BluetoothDevice, text: String): Boolean {
        var socket: BluetoothSocket? = null
        var output: OutputStream? = null
        return try {
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            output = socket.outputStream
            output.write(text.toByteArray())
            output.flush()
            true
        } catch (e: Exception) {
            false
        } finally {
            try {
                output?.close()
                socket?.close()
            } catch (_: Exception) {
            }
        }
    }
}
