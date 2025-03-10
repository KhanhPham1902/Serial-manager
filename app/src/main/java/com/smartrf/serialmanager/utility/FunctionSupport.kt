package com.smartrf.serialmanager.utility

import android.app.Dialog
import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.smartrf.serialmanager.R
import com.smartrf.serialmanager.api.TcpClient
import com.smartrf.serialmanager.model.ConnectionListener
import java.net.NetworkInterface
import java.net.InetAddress
import java.util.Collections

object FunctionSupport {

    private val TAG: String = "FunctionSupport"

    fun showSetConnectionDialog(context: Context, listener: ConnectionListener){
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.bottom_sheet_layout)
        dialog.setCanceledOnTouchOutside(true)

        val edtHost = dialog.findViewById<EditText>(R.id.edtHost)
        val edtPort = dialog.findViewById<EditText>(R.id.edtPort)
        val btnConnect = dialog.findViewById<TextView>(R.id.btnConnect)
        val pbLoading = dialog.findViewById<ProgressBar>(R.id.pbLoadingConnect)

        val hostSaved = context.getSharedPreferences(Constants.SHF_NAME, MODE_PRIVATE).getString(Constants.HOST, "").toString()
        val portSaved = context.getSharedPreferences(Constants.SHF_NAME, MODE_PRIVATE).getInt(Constants.PORT, 0)

        if(hostSaved.isNotEmpty() && portSaved != 0){
            edtHost.setText(hostSaved)
            edtPort.setText(portSaved.toString())
        }

        // Dong dialog
        btnConnect.setOnClickListener {
            pbLoading.visibility = View.VISIBLE
            btnConnect.visibility = View.GONE

            val host = edtHost.text.toString()
            val port = edtPort.text.toString().toIntOrNull()
            Log.d(TAG, "Host: $host, Port: $port")

            if(host.isNotEmpty() && port!=null){
                val validIPv4 = isValidIPv4(host)
                if(!validIPv4){
                    pbLoading.visibility = View.GONE
                    btnConnect.visibility = View.VISIBLE
                    Toast.makeText(context, "Địa chỉ IP không hợp lệ", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                listener.onConnect(host, port) // Gọi callback để truyền dữ liệu về Activity
            }else{
                pbLoading.visibility = View.GONE
                btnConnect.visibility = View.VISIBLE
                Toast.makeText(context, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.window?.setGravity(Gravity.BOTTOM)
    }

    private fun isValidIPv4(ip: String): Boolean {
        val ipv4Pattern = Regex(
            "^((25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\\.){3}" +
                    "(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])$"
        )
        return ipv4Pattern.matches(ip)
    }

}