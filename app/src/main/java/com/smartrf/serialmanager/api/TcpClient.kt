package com.smartrf.serialmanager.api

import android.util.Log
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

object TcpClient{
    private var socket: Socket? = null
    private var output: DataOutputStream? = null
    private var input: DataInputStream? = null

    fun connect(serverIp: String, serverPort: Int): Boolean {
        return try {
            val serverAddr = InetAddress.getByName(serverIp)
            val timeout = 5000
            socket = Socket().apply {
                connect(InetSocketAddress(serverAddr, serverPort), timeout)
            }
            output = DataOutputStream(socket!!.getOutputStream())
            input = DataInputStream(socket!!.getInputStream())
            Log.d("TcpClient", "Connected to server at $serverIp:$serverPort")
            true
        } catch (e: Exception) {
            Log.e("TcpClient", "Failed to connect to server: ${e.message}", e)
            false
        }
    }

    fun sendData(data: ByteArray) {
        if (output == null) {
            Log.e("TcpClient", "Output stream is null. Unable to send data.")
            return
        }
        try {
            output!!.write(data)
            output!!.flush()
            Log.d("TcpClient", "Sent data: ${data.joinToString(" ") { "%02X".format(it) }}")
        } catch (e: IOException) {
            Log.e("TcpClient", "IOException while sending data: ${e.message}")
        } catch (e: Exception) {
            Log.e("TcpClient", "Failed to send data: ${e.message}")
            e.printStackTrace()
        }
    }

    fun readData(timeout: Int): String? {
        return try {
            socket?.soTimeout = timeout

            val response = ByteArray(1024 * 1024)
            val bytesRead = input?.read(response)
            if (bytesRead != null && bytesRead > 0) {
                val responseString = response.copyOf(bytesRead).toString(Charsets.UTF_8)
                Log.d("TcpClient", "Received data: $responseString")
                Log.d("TcpClient", "Size of received data: $bytesRead bytes")
                responseString
            } else {
                Log.d("TcpClient", "No data received or connection closed")
                null
            }
        } catch (e: SocketTimeoutException) {
            Log.e("TcpClient", "Read timed out")
            null
        } catch (e: IOException) {
            Log.e("TcpClient", "IOException when reading data", e)
            null
        } catch (e: Exception) {
            Log.e("TcpClient", "Unexpected exception", e)
            null
        }
    }

    fun disconnect(): Boolean{
        try {
            output?.close()
            input?.close()
            socket?.close()
            Log.d("TcpClient", "Disconnected from server")
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("TcpClient", "Failed to disconnect: ${e.message}")
            return false
        }
    }
}