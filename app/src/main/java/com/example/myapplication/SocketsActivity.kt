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
                sendZmqMessage(ip)
            } else {
                tvLog.append("Пожалуйста, введите IP адрес!\n")
            }
        }
    }

    private fun sendZmqMessage(ip: String) {
        tvLog.append("Подключение к $ip...\n")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                ZContext().use { context ->
                    val socket = context.createSocket(SocketType.REQ)
                    socket.receiveTimeOut = 3000 // Тайм-аут 3 секунды

                    socket.connect("tcp://$ip:5555")

                    val message = "Hello from Android!"
                    socket.send(message.toByteArray(Charsets.UTF_8), 0)

                    val reply = socket.recvStr(0)

                    withContext(Dispatchers.Main) {
                        if (reply != null) {
                            tvLog.append("Сервер: $reply\n")
                        } else {
                            tvLog.append("Ошибка: Сервер не ответил (тайм-аут).\n")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    tvLog.append("Ошибка: ${e.message}\n")
                }
            }
        }
    }
}