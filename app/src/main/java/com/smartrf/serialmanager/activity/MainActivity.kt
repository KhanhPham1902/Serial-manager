package com.smartrf.serialmanager.activity

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.smartrf.serialmanager.R
import com.smartrf.serialmanager.api.TcpClient
import com.smartrf.serialmanager.databinding.ActivityMainBinding
import com.smartrf.serialmanager.model.ConnectionListener
import com.smartrf.serialmanager.model.TcpResponse
import com.smartrf.serialmanager.model.parseJson
import com.smartrf.serialmanager.utility.Constants
import com.smartrf.serialmanager.utility.FunctionSupport
import com.smartrf.serialmanager.utility.NetworkUtility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    companion object{
        const val TAG = "MainActivity"
    }

    private var job: Job? = null

    private lateinit var binding: ActivityMainBinding
    private var selectedTextView: TextView? = null
    private lateinit var HOST: String
    private var PORT: Int = 9009

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get defaut gateway
        val networkUtils = NetworkUtility(this)
        val gateway = networkUtils.getGatewayIPAddress()
        Log.d(TAG, "Default gateway: $gateway")

        init()

        setListener()
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)

        menu?.clear() // Xóa menu cũ nếu có
        selectedTextView = v as? TextView // Lưu lại TextView đang mở menu

        when (v?.id) {
            R.id.edtJson1, R.id.edtJson4 -> menuInflater.inflate(R.menu.menu_3_item, menu)
            R.id.edtJson2, R.id.edtJson5 -> menuInflater.inflate(R.menu.menu_6_item, menu)
            R.id.edtJson3, R.id.edtJson6 -> menuInflater.inflate(R.menu.menu_10_item, menu)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        selectedTextView?.let { textView ->
            when (item.itemId) {
                R.id.item1 -> textView.text = "1"
                R.id.item2 -> textView.text = "2"
                R.id.item3 -> textView.text = "3"
                R.id.item4 -> textView.text = "4"
                R.id.item5 -> textView.text = "5"
                R.id.item6 -> textView.text = "6"
                R.id.item7 -> textView.text = "7"
                R.id.item8 -> textView.text = "8"
                R.id.item9 -> textView.text = "9"
                R.id.item10 -> textView.text = "10"
                else -> return false
            }
            return true
        }
        return false
    }

    private fun init(){
        // Đăng ký context menu cho từng TextView
        registerForContextMenu(binding.edtJson1)
        registerForContextMenu(binding.edtJson2)
        registerForContextMenu(binding.edtJson3)
        registerForContextMenu(binding.edtJson4)
        registerForContextMenu(binding.edtJson5)
        registerForContextMenu(binding.edtJson6)

        // Mở context menu khi nhấn vào TextView
        binding.edtJson1.setOnClickListener { it.showContextMenu() }
        binding.edtJson2.setOnClickListener { it.showContextMenu() }
        binding.edtJson3.setOnClickListener { it.showContextMenu() }
        binding.edtJson4.setOnClickListener { it.showContextMenu() }
        binding.edtJson5.setOnClickListener { it.showContextMenu() }
        binding.edtJson6.setOnClickListener { it.showContextMenu() }
    }

    private fun setListener(){
        binding.btnStart.setOnClickListener {
            FunctionSupport.showSetConnectionDialog(this, object : ConnectionListener {
                override fun onConnect(host: String, port: Int) {
                    handleData(host, port)
                }
            })
        }

        binding.btnStop.setOnClickListener {
            stopConnection()
        }
    }

    // Ket noi va nhan du lieu
    private fun handleData(host: String, port: Int) {
        job = lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                loadingUI(true)
                binding.txtStatusConnect.visibility = View.GONE
            }

            if (TcpClient.connect(host, port)) {
                // Luu thong tin ket noi
                val sharedPreferences = getSharedPreferences(Constants.SHF_NAME, Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putString(Constants.HOST, host)
                editor.putInt(Constants.PORT, port)
                editor.apply()

                withContext(Dispatchers.Main) {
                    binding.txtConnection.text = "$host:$port"
                    loadingUI(false)
                    binding.txtStatusConnect.visibility = View.VISIBLE
                    binding.txtStatusConnect.text = "Đã kết nối đến server"
                    binding.txtStatusConnect.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.green))
                }

                var retryCount = 0
                val maxRetries = 5  // Số lần thử kết nối lại tối đa

                while (isActive) {
                    val response = TcpClient.readData(5000)
                    Log.d(TAG, "response: $response")

                    if (response != null) {
                        if(response.trim() == "SERVER STOP"){
                            withContext(Dispatchers.Main) {
                                resetUI()
                                loadingUI(false)
                                binding.txtStatusConnect.visibility = View.VISIBLE
                                binding.txtStatusConnect.text = "Server đã ngưng kết nối"
                                binding.txtStatusConnect.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.red))
                            }
                        }else {
                            retryCount = 0 // Reset bộ đếm khi nhận được dữ liệu
                            val tcpResponse = parseJson(response)
                            withContext(Dispatchers.Main) {
                                if (tcpResponse != null) {
                                    updateUI(tcpResponse)
                                    loadingUI(false)
                                    binding.txtStatusConnect.visibility = View.VISIBLE
                                    binding.txtStatusConnect.text = "Đang lấy dữ liệu từ server..."
                                    binding.txtStatusConnect.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.light_blue))
                                }
                            }
                        }
                    } else {
                        Log.e(TAG, "Không nhận được dữ liệu, thử lại lần ${retryCount + 1}")

                        retryCount++
                        if (retryCount >= maxRetries) {
                            withContext(Dispatchers.Main) {
                                resetUI()
                                loadingUI(false)
                                binding.txtStatusConnect.visibility = View.VISIBLE
                                binding.txtStatusConnect.text = "Không nhận được dữ liệu\nHãy kết nối lại"
                                binding.txtStatusConnect.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.red))
                            }
                            break  // Dừng vòng lặp nếu vượt quá số lần thử
                        }

                        delay(1000)

                        withContext(Dispatchers.Main) {
                            binding.txtStatusConnect.text = "Đang thử kết nối lại (${retryCount}/$maxRetries)..."
                            binding.txtStatusConnect.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.orange))
                        }

                        if (!TcpClient.connect(host, port)) {
                            Log.e(TAG, "Kết nối lại thất bại")
                            withContext(Dispatchers.Main) {
                                binding.txtStatusConnect.text = "Mất kết nối! Thử lại sau."
                                binding.txtStatusConnect.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.red))
                            }
                            break
                        } else {
                            Log.d(TAG, "Kết nối lại thành công")
                            withContext(Dispatchers.Main) {
                                binding.txtStatusConnect.text = "Kết nối lại thành công"
                                binding.txtStatusConnect.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.light_blue))
                            }
                        }
                    }
                    delay(500)
                }
            } else {
                withContext(Dispatchers.Main) {
                    resetUI()
                    loadingUI(false)
                    binding.txtStatusConnect.visibility = View.VISIBLE
                    binding.txtStatusConnect.text = "Kết nối thất bại!"
                    binding.txtStatusConnect.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.red))
                }
            }
        }
    }

    // Ngắt kết nối
    private fun stopConnection() {
        job?.cancel() // Hủy coroutine đang chạy
        if (TcpClient.disconnect()) { // Gọi hàm disconnect()
            Log.d(TAG, "Đã ngắt kết nối thành công")
            lifecycleScope.launch(Dispatchers.Main) {
                resetUI()
                binding.txtStatusConnect.text = "Đã ngắt kết nối đến server"
                binding.txtStatusConnect.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.orange))
            }
        } else {
            Log.e(TAG, "Không thể ngắt kết nối đến server")
            lifecycleScope.launch(Dispatchers.Main) {
                resetUI()
                binding.txtStatusConnect.text = "Không thể ngắt kết nối đến server"
                binding.txtStatusConnect.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.red))
            }
        }
    }

    // Cập nhật giao diện với dữ liệu từ server
    private fun updateUI(response: TcpResponse) {
        binding.txtJson1.text = response.M15.toString()
        binding.txtJson2.text = response.H4.toString()
        binding.txtJson3.text = response.PNOW.toString()
        binding.txtJson4.text = response.D15.toString()
        binding.txtJson5.text = response.D4.toString()
        binding.txtJson6.text = response.L15A.toString()
        binding.txtJson7.text = response.L15B.toString()
        binding.txtJson8.text = response.L15C.toString()
        binding.txtJson9.text = response.L4A.toString()
        binding.edtJson1.text = response.L4B.toString()
        binding.edtJson2.text = response.L4C.toString()
        binding.edtJson3.text = response.ACC.toString()
        binding.edtJson4.text = response.EP.toString()
        binding.edtJson5.text = response.PO.toString()
    }

    // Giao dien loading
    private fun loadingUI(isLoading: Boolean){
        if(isLoading){
            binding.pbCheckConnection.visibility = View.VISIBLE
            binding.layoutConnect.visibility = View.GONE
        }else{
            binding.pbCheckConnection.visibility = View.GONE
            binding.layoutConnect.visibility = View.VISIBLE
        }
    }

    // Xóa dữ liệu giao diện
    private fun resetUI(){
        binding.txtJson1.text = ""
        binding.txtJson2.text = ""
        binding.txtJson3.text = ""
        binding.txtJson4.text = ""
        binding.txtJson5.text = ""
        binding.txtJson6.text = ""
        binding.txtJson7.text = ""
        binding.txtJson8.text = ""
        binding.txtJson9.text = ""
        binding.edtJson1.text = ""
        binding.edtJson2.text = ""
        binding.edtJson3.text = ""
        binding.edtJson4.text = ""
        binding.edtJson5.text = ""
        binding.edtJson6.text = ""
    }
}

/*
    "M15": 1234,
    "H4": 2345,
    "PNOW": 12345678,
    "D15": 3456,
    "D4": 4567,
    "L15A": 1,
    "L15B": 2,
    "L15C": 3,
    "L4A": 4,
    "L4B": 5,
    "L4C": 6,
    "ACC": 10000,
    "EP": 500000,
    "PO": 10
 */