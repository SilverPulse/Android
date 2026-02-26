package com.example.myapplication

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.zeromq.SocketType
import org.zeromq.ZContext

class LocatorActivity : AppCompatActivity(), LocationListener {
    private val PREFS_NAME = "MySettings"
    private val IP_KEY = "server_ip"

    private lateinit var locationManager: LocationManager
    private lateinit var etServerIp: EditText
    private lateinit var cbAutoSend: CheckBox
    private lateinit var tvLatitude: TextView
    private lateinit var tvLongitude: TextView
    private lateinit var tvAltitude: TextView
    private lateinit var tvTime: TextView
    private lateinit var btnGetLocation: Button
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_locator)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        etServerIp = findViewById(R.id.etServerIpLocator)
        cbAutoSend = findViewById(R.id.cbAutoSend)
        tvLatitude = findViewById(R.id.tvLatitude)
        tvLongitude = findViewById(R.id.tvLongitude)
        tvAltitude = findViewById(R.id.tvAltitude)
        tvTime = findViewById(R.id.tvTime)
        btnGetLocation = findViewById(R.id.btnGetLocation)
        btnStart = findViewById(R.id.btnStartService)
        btnStop = findViewById(R.id.btnStopService)

        etServerIp.setText(getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(IP_KEY, ""))

        btnGetLocation.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 2f, this)
                Toast.makeText(this, "Поиск спутников...", Toast.LENGTH_SHORT).show()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            }
        }

        btnStart.setOnClickListener {
            val ip = etServerIp.text.toString()
            if (ip.isEmpty()) {
                Toast.makeText(this, "Введите IP сервера !", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putString(IP_KEY, ip).apply()
            checkPermissionsAndStartService(ip)
        }

        btnStop.setOnClickListener {
            stopService(Intent(this, TelemetryService::class.java))
            Toast.makeText(this, "Фоновый мониторинг остановлен", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onLocationChanged(location: Location) {
        tvLatitude.text = "Широта: ${location.latitude}"
        tvLongitude.text = "Долгота: ${location.longitude}"
        tvAltitude.text = "Высота: ${location.altitude} м"
        tvTime.text = "Время: ${location.time} мс"

        val root = JSONObject().apply {
            put("timestamp", System.currentTimeMillis())
            put("type", "location_update")
            put("data", JSONObject().apply {
                put("lat", location.latitude)
                put("lon", location.longitude)
                put("alt", location.altitude)
                put("time", location.time)
            })
        }

        if (cbAutoSend.isChecked) sendToServerOld(root)
    }

    private fun sendToServerOld(json: JSONObject) {
        val ip = etServerIp.text.toString()
        if (ip.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                ZContext().use { context ->
                    val socket = context.createSocket(SocketType.REQ)
                    socket.linger = 0
                    socket.receiveTimeOut = 2000
                    socket.connect("tcp://$ip:5555")
                    socket.send(json.toString().toByteArray(Charsets.UTF_8), 0)
                    socket.recvStr(0)
                }
            } catch (e: Exception) {
                Log.e("LOCATOR", "Ошибка: ${e.message}")
            }
        }
    }

    private fun checkPermissionsAndStartService(ip: String) {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = permissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 100)
            return
        }

        if (!hasUsageStatsPermission()) {
            Toast.makeText(this, "Дайте разрешение на доступ к истории использования трафика", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            return
        }

        val serviceIntent = Intent(this, TelemetryService::class.java).apply { putExtra("IP", ip) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        Toast.makeText(this, "Сбор телеметрии запущен в фоне!", Toast.LENGTH_SHORT).show()
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        } else {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun onProviderEnabled(p: String) {}
    override fun onProviderDisabled(p: String) {}

    override fun onPause() {
        super.onPause()
        locationManager.removeUpdates(this)
    }
}