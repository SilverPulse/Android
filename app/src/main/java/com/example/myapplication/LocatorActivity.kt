package com.example.myapplication

import android.Manifest
import android.app.ActivityManager
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
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.util.Log

class LocatorActivity : AppCompatActivity(), LocationListener {
    private val PREFS_NAME = "MySettings"
    private val IP_KEY = "server_ip"

    private lateinit var etServerIp: EditText
    private lateinit var btnMainAction: Button
    private lateinit var tvLatitude: TextView
    private lateinit var tvLongitude: TextView
    private lateinit var tvAltitude: TextView
    private lateinit var tvTime: TextView
    private lateinit var locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_locator)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        etServerIp = findViewById(R.id.etServerIpLocator)
        btnMainAction = findViewById(R.id.btnMainAction)
        tvLatitude = findViewById(R.id.tvLatitude)
        tvLongitude = findViewById(R.id.tvLongitude)
        tvAltitude = findViewById(R.id.tvAltitude)
        tvTime = findViewById(R.id.tvTime)

        etServerIp.setText(getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(IP_KEY, ""))

        updateButtonState()

        btnMainAction.setOnClickListener {
            if (isServiceRunning(TelemetryService::class.java)) {
                stopMonitoring()
            } else {
                startMonitoring()
            }
        }
    }

    private fun startMonitoring() {
        val ip = etServerIp.text.toString()
        if (ip.isEmpty()) {
            Toast.makeText(this, "Введите IP сервера!", Toast.LENGTH_SHORT).show()
            return
        }
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putString(IP_KEY, ip).apply()

        if (checkAndRequestPermissions()) {
            try {
                val intent = Intent(this, TelemetryService::class.java).apply { putExtra("IP", ip) }
                ContextCompat.startForegroundService(this, intent)

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 2f, this)
                }
                updateButtonState()
            } catch (e: Exception) {
                Log.e("CRASH", "Ошибка при старте: ${e.message}")
                Toast.makeText(this, "Ошибка запуска: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopMonitoring() {
        stopService(Intent(this, TelemetryService::class.java))
        locationManager.removeUpdates(this)
        updateButtonState()
        Toast.makeText(this, "Отправка остановлена", Toast.LENGTH_SHORT).show()
    }

    private fun updateButtonState() {
        if (isServiceRunning(TelemetryService::class.java)) {
            btnMainAction.text = "ОСТАНОВИТЬ ОТПРАВКУ"
            btnMainAction.setBackgroundColor(android.graphics.Color.parseColor("#F44336"))
        } else {
            btnMainAction.text = "ОТПРАВИТЬ ДАННЫЕ"
            btnMainAction.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        val basicPermissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            basicPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingBasic = basicPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingBasic.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingBasic.toTypedArray(), 101)
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(this, "В открывшемся окне выберите 'Разрешить в любом режиме'", Toast.LENGTH_LONG).show()

                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), 102)
                return false
            }
        }

        if (!hasUsageStatsPermission()) {
            Toast.makeText(this, "Дайте доступ к статистике использования", Toast.LENGTH_SHORT).show()
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            return false
        }

        return true
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

    override fun onLocationChanged(location: Location) {
        tvLatitude.text = "Широта: ${location.latitude}"
        tvLongitude.text = "Долгота: ${location.longitude}"
        tvAltitude.text = "Высота: ${location.altitude} м"
        tvTime.text = "Время: ${location.time} мс"
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) return true
        }
        return false
    }

    override fun onProviderEnabled(p: String) {}
    override fun onProviderDisabled(p: String) {}
}