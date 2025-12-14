package com.example.myapplication

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import org.json.JSONObject
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class LocatorActivity : LocationListener, AppCompatActivity() {

    private val LOG_TAG: String = "LOCATOR_ACTIVITY"

    companion object {
        private const val PERMISSION_REQUEST_ACCESS_LOCATION = 100
    }

    private lateinit var locationManager: LocationManager
    private lateinit var tvLatitude: TextView
    private lateinit var tvLongitude: TextView
    private lateinit var tvAltitude: TextView
    private lateinit var tvTime: TextView
    private lateinit var btnGetLocation: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_locator)

        locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        tvLatitude = findViewById(R.id.tvLatitude)
        tvLongitude = findViewById(R.id.tvLongitude)
        tvAltitude = findViewById(R.id.tvAltitude)
        tvTime = findViewById(R.id.tvTime)
        btnGetLocation = findViewById(R.id.btnGetLocation)
    }

    override fun onResume() {
        super.onResume()
        btnGetLocation.setOnClickListener {
            updateCurrentLocation()
        }
        updateCurrentLocation()
    }

    override fun onPause() {
        super.onPause()
        locationManager.removeUpdates(this)
    }

    private fun updateCurrentLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions()
                    return
                }

                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    100000L,
                    5f,
                    this
                )

                Toast.makeText(this, "Ожидание GPS сигнала...", Toast.LENGTH_SHORT).show()
                logEventJson("Запрос обновления местоположения")
            } else {
                Toast.makeText(applicationContext, "Включите GPS в настройках", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
                logEventJson("GPS выключен, запрос включения")
            }
        } else {
            Log.w(LOG_TAG, "location permission is not allowed")
            tvLatitude.text = "Разрешение не предоставлено"
            tvLongitude.text = "Разрешение не предоставлено"
            tvAltitude.text = "Разрешение не предоставлено"
            tvTime.text = "Разрешение не предоставлено"
            requestPermissions()
            logEventJson("Разрешение на местоположение не предоставлено, запрос разрешений")
        }
    }

    private fun requestPermissions() {
        Log.w(LOG_TAG, "requestPermissions()")
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_REQUEST_ACCESS_LOCATION
        )
    }

    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_ACCESS_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(applicationContext, "Разрешение предоставлено", Toast.LENGTH_SHORT).show()
                logEventJson("Разрешение на местоположение предоставлено")
                updateCurrentLocation()
            } else {
                Toast.makeText(applicationContext, "Разрешение отклонено", Toast.LENGTH_SHORT).show()
                logEventJson("Разрешение на местоположение отклонено")
            }
        }
    }

    private fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun logEventJson(message: String) {
        val jsonObject = JSONObject()
        jsonObject.put("timestamp", System.currentTimeMillis())
        jsonObject.put("event", message)
        val filename = "locator_log.json"

        try {
            val resolver = contentResolver
            val values = ContentValues()
            values.put(MediaStore.Downloads.DISPLAY_NAME, filename)
            values.put(MediaStore.Downloads.MIME_TYPE, "application/json")
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                val outputStream = resolver.openOutputStream(uri, "wa")
                outputStream?.write((jsonObject.toString() + "\n").toByteArray())
                outputStream?.close()
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Ошибка записи JSON лога: ${e.message}")
        }
    }

    override fun onLocationChanged(location: Location) {
        Log.i(LOG_TAG, "Location changed: ${location.latitude}, ${location.longitude}")

        val latitude = location.latitude
        val longitude = location.longitude
        val altitude = location.altitude
        val time = location.time

        tvLatitude.text = "Широта: $latitude"
        tvLongitude.text = "Долгота: $longitude"
        tvAltitude.text = "Высота: $altitude м"
        tvTime.text = "Время: $time мс"

        logEventJson("Location changed: lat=$latitude, lon=$longitude, alt=$altitude, time=$time")
    }

    override fun onProviderEnabled(provider: String) {
        Log.i(LOG_TAG, "Provider enabled: $provider")
        Toast.makeText(this, "GPS включен", Toast.LENGTH_SHORT).show()
        logEventJson("Provider enabled: $provider")
    }

    override fun onProviderDisabled(provider: String) {
        Log.i(LOG_TAG, "Provider disabled: $provider")
        Toast.makeText(this, "GPS выключен", Toast.LENGTH_SHORT).show()
        logEventJson("Provider disabled: $provider")
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        Log.i(LOG_TAG, "Provider status changed: $provider, status: $status")
        if (provider != null) {
            logEventJson("Provider status changed: $provider, status: $status")
        }
    }
}
