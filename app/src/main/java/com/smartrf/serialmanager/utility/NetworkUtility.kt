package com.smartrf.serialmanager.utility

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections

class NetworkUtility(private val context: Context) {

    // Lay default gateway
    fun getGatewayIPAddress(): String {
        // Lấy WifiManager từ context
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        // Lấy DHCPInfo chứa thông tin về kết nối Wi-Fi hiện tại
        val dhcpInfo = wifiManager.dhcpInfo

        // Lấy IP address của default gateway
        val gatewayIP = Formatter.formatIpAddress(dhcpInfo.gateway)

        return gatewayIP
    }

    // Lấy IP address của thiết bị hiện tại
    fun getDeviceIpAddress(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (networkInterface in interfaces) {
                val addresses = Collections.list(networkInterface.inetAddresses)
                for (address in addresses) {
                    if (!address.isLoopbackAddress && address is InetAddress) {
                        val ip = address.hostAddress
                        if (ip != null) {
                            if (ip.contains(":")) {
                                continue // Bỏ qua IPv6
                            }
                        }
                        return ip
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}