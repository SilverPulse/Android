package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.json.JSONObject

class SocketsActivity : AppCompatActivity() {
    private lateinit var tvLog: TextView
    private lateinit var etServerIp: EditText
    private lateinit var btnSend: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sockets)

        tvLog = findViewById(R.id.tvLog)
        etServerIp = findViewById(R.id.etServerIp)
        btnSend = findViewById(R.id.btnSend)

        btnSend.setOnClickListener {
            val ip = etServerIp.text.toString()
            if (ip.isNotEmpty()) {
                sendTestMessage(ip)
            } else {
                tvLog.append("Введите IP!\n")
            }
        }
    }

    private fun sendTestMessage(ip: String) {
        tvLog.append("Тестовая отправка на $ip...\n")
        val testJson = JSONObject().apply {
            put("timestamp", System.currentTimeMillis())
            put("type", "system_event")
            put("message", "Тестовая проверка связи с телефона")
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                ZContext().use { context ->
                    val socket = context.createSocket(SocketType.REQ)
                    socket.receiveTimeOut = 3000
                    socket.connect("tcp://$ip:5555")
                    socket.send(testJson.toString().toByteArray(Charsets.UTF_8), 0)
                    val reply = socket.recvStr(0)

                    withContext(Dispatchers.Main) {
                        tvLog.append(if (reply != null) "Сервер: $reply\n" else "Тайм-аут\n")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvLog.append("Ошибка: ${e.message}\n")
                }
            }
        }
    }
}