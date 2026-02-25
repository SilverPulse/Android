package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import org.json.JSONObject
import org.zeromq.SocketType
import org.zeromq.ZContext

class LocatorActivity : LocationListener, AppCompatActivity() {
    private val PREFS_NAME = "MySettings"
    private val IP_KEY = "server_ip"
    private lateinit var locationManager: LocationManager
    private lateinit var tvLatitude: TextView
    private lateinit var tvLongitude: TextView
    private lateinit var tvAltitude: TextView
    private lateinit var tvTime: TextView
    private lateinit var etServerIp: EditText
    private lateinit var cbAutoSend: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_locator)

        tvLatitude = findViewById(R.id.tvLatitude)
        tvLongitude = findViewById(R.id.tvLongitude)
        tvAltitude = findViewById(R.id.tvAltitude)
        tvTime = findViewById(R.id.tvTime)
        etServerIp = findViewById(R.id.etServerIpLocator)
        cbAutoSend = findViewById(R.id.cbAutoSend)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        etServerIp.setText(getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(IP_KEY, ""))

        findViewById<Button>(R.id.btnGetLocation).setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 2f, this)
                Toast.makeText(this, "Поиск спутников...", Toast.LENGTH_SHORT).show()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            }
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

        if (cbAutoSend.isChecked) sendToServer(root)
    }

    private fun sendToServer(json: JSONObject) {
        val ip = etServerIp.text.toString()
        if (ip.isEmpty()) return
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putString(IP_KEY, ip).apply()

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

    override fun onProviderEnabled(p: String) {}
    override fun onProviderDisabled(p: String) {}
    override fun onPause() { super.onPause(); locationManager.removeUpdates(this) }
}