package com.example.myapplication

import android.app.*
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.TrafficStats
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.telephony.*
import android.util.Log
import androidx.core.app.NotificationCompat
import org.json.JSONArray
import org.json.JSONObject
import org.zeromq.SocketType
import org.zeromq.ZContext
import kotlin.math.pow
import kotlin.math.sqrt

class TelemetryService : Service(), LocationListener {
    private lateinit var locationManager: LocationManager
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var statsManager: NetworkStatsManager

    private val CHANNEL_ID = "TelemetryServiceChannel"
    private var serverIp: String = ""

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        statsManager = getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serverIp = intent?.getStringExtra("IP") ?: ""

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Мониторинг сети")
            .setContentText("Сбор данных для анализа (LTE/5G/Трафик)")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        startForeground(1, notification)

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 2f, this)
        } catch (e: SecurityException) {
            Log.e("SERVICE", "Нет прав на локацию")
        }

        return START_STICKY
    }

    override fun onLocationChanged(location: Location) {
        val telemetry = JSONObject().apply {
            put("type", "telemetry_update")
            put("timestamp", System.currentTimeMillis())

            put("location", JSONObject().apply {
                put("lat", location.latitude)
                put("lon", location.longitude)
                put("alt", location.altitude)
                put("accuracy", location.accuracy.toDouble())
                put("time", location.time)
            })

            put("telephony", getCellInfoJson())

            put("network_stats", getTrafficStatsJson())
        }

        sendToServer(telemetry)
    }

    private fun getCellInfoJson(): JSONObject {
        val root = JSONObject()
        try {
            val infoList = telephonyManager.allCellInfo ?: return root
            for (info in infoList) {
                when (info) {
                    is CellInfoLte -> {
                        val id = info.cellIdentity
                        val ss = info.cellSignalStrength
                        root.put("lte", JSONObject().apply {
                            put("pci", id.pci)
                            put("earfcn", id.earfcn)
                            put("tac", id.tac)
                            put("mcc", id.mccString)
                            put("mnc", id.mncString)
                            put("rsrp", ss.rsrp)
                            put("rsrq", ss.rsrq)
                            put("rssi", if (Build.VERSION.SDK_INT >= 29) ss.rssi else -1)
                            put("ta", ss.timingAdvance)
                        })
                    }
                    is CellInfoNr -> { // 5G
                        val id = info.cellIdentity as CellIdentityNr
                        val ss = info.cellSignalStrength as CellSignalStrengthNr
                        root.put("nr", JSONObject().apply {
                            put("pci", id.pci)
                            put("tac", id.tac)
                            put("nci", id.nci)
                            put("ssRsrp", ss.ssRsrp)
                            put("ssRsrq", ss.ssRsrq)
                            put("ssSinr", ss.ssSinr)
                        })
                    }
                    is CellInfoGsm -> {
                        val id = info.cellIdentity
                        val ss = info.cellSignalStrength
                        root.put("gsm", JSONObject().apply {
                            put("lac", id.lac)
                            put("cid", id.cid)
                            put("dbm", ss.dbm)
                        })
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("TEL", "Ошибка доступа к CellInfo")
        }
        return root
    }

    private fun getTrafficStatsJson(): JSONObject {
        val result = JSONObject()
        val appTrafficList = mutableListOf<Pair<String, Long>>()

        val totalBytes = TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes()
        result.put("total_bytes_device", totalBytes)

        val pm = packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (app in packages) {
            val received = TrafficStats.getUidRxBytes(app.uid)
            val sent = TrafficStats.getUidTxBytes(app.uid)
            val total = received + sent
            if (total > 0) {
                appTrafficList.add(app.packageName to total)
            }
        }

        if (appTrafficList.isNotEmpty()) {
            val values = appTrafficList.map { it.second.toDouble() }
            val avg = values.average()
            val stdDev = sqrt(values.map { (it - avg).pow(2.0) }.average())
            val threshold = avg + 2 * stdDev

            val topApps = JSONArray()
            appTrafficList.filter { it.second > threshold }.forEach {
                topApps.put(JSONObject().apply {
                    put("package", it.first)
                    put("bytes", it.second)
                })
            }
            result.put("top_apps_2sigma", topApps)
        }
        return result
    }

    private fun sendToServer(json: JSONObject) {
        if (serverIp.isEmpty()) return
        Thread {
            try {
                ZContext().use { context ->
                    val socket = context.createSocket(SocketType.REQ)
                    socket.linger = 0
                    socket.connect("tcp://$serverIp:5555")
                    socket.send(json.toString())
                    socket.recvStr()
                }
            } catch (e: Exception) {
                Log.e("ZMQ", "Ошибка отправки: ${e.message}")
            }
        }.start()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Telemetry Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        locationManager.removeUpdates(this)
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
}