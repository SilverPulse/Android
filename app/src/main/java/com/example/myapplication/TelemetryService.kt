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
    private var zContext: ZContext? = null
    private var zSocket: org.zeromq.ZMQ.Socket? = null
    private var wakeLock: android.os.PowerManager.WakeLock? = null

    private val socketLock = Any()

    override fun onCreate() {
        super.onCreate()
        zContext = ZContext()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        statsManager = getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serverIp = intent?.getStringExtra("IP") ?: ""

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Мониторинг активен")
            .setContentText("Данные уходят на $serverIp")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(1, notification)
            }
        } catch (e: Exception) {
            Log.e("SERVICE", "Критическая ошибка старта: ${e.message}")
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            if (wakeLock == null) {
                wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "MyApp::TelemetryWakeLock")
            }
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire()
            }
        } catch (e: Exception) {
            Log.e("SERVICE", "WakeLock error: ${e.message}")
        }

        startGpsUpdates()

        return START_STICKY
    }

    private fun startGpsUpdates() {
        try {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 2f, this)
                Log.d("SERVICE", "GPS обновления запущены успешно")
            } else {
                Log.e("SERVICE", "Нет прав ACCESS_FINE_LOCATION для запуска GPS!")
            }
        } catch (e: SecurityException) {
            Log.e("SERVICE", "SecurityException: права на локацию не полные: ${e.message}")
        } catch (e: Exception) {
            Log.e("SERVICE", "Ошибка при запуске GPS: ${e.message}")
        }
    }



    override fun onLocationChanged(location: Location) {
        val fullPackage = JSONObject().apply {
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

        sendToServer(fullPackage)
    }

    private fun sendToServer(json: JSONObject) {
        if (serverIp.isEmpty()) return

        Thread {
            synchronized(socketLock) {
                try {
                    if (zSocket == null) {
                        zSocket = zContext?.createSocket(SocketType.REQ)
                        zSocket?.linger = 0
                        zSocket?.receiveTimeOut = 3000
                        zSocket?.connect("tcp://$serverIp:5555")
                    }

                    val sent = zSocket?.send(json.toString()) ?: false
                    if (sent) {
                        val response = zSocket?.recvStr()
                        if (response == null) resetSocket()
                    }
                } catch (e: Exception) {
                    Log.e("ZMQ", "Ошибка связи: ${e.message}")
                    resetSocket()
                }
            }
        }.start()
    }

    private fun resetSocket() {
        try {
            zSocket?.close()
        } finally {
            zSocket = null
        }
    }

    private fun getCellInfoJson(): JSONObject {
        val root = JSONObject()
        try {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return root
            }

            val infoList = telephonyManager.allCellInfo ?: return root

            for (info in infoList) {
                if (info is CellInfoLte) {
                    val id = info.cellIdentity
                    val ss = info.cellSignalStrength
                    root.put("lte", JSONObject().apply {
                        put("pci", id.pci)
                        put("earfcn", id.earfcn)
                        put("ci", id.ci)
                        put("tac", id.tac)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) put("mcc", id.mccString) else put("mcc", id.mcc)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) put("mnc", id.mncString) else put("mnc", id.mnc)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) put("band", id.bands.joinToString(","))

                        put("rsrp", ss.rsrp)
                        put("rsrq", ss.rsrq)
                        put("rssi", ss.rssi)
                        put("rssnr", ss.rssnr)
                        put("cqi", ss.cqi)
                        put("asu_level", ss.asuLevel)
                        put("timing_advance", ss.timingAdvance)
                    })
                }
                else if (info is CellInfoGsm) {
                    val id = info.cellIdentity
                    val ss = info.cellSignalStrength
                    root.put("gsm", JSONObject().apply {
                        put("ci", id.cid)
                        put("bsic", id.bsic)
                        put("arfcn", id.arfcn)
                        put("lac", id.lac)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) put("mcc", id.mccString) else put("mcc", id.mcc)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) put("mnc", id.mncString) else put("mnc", id.mnc)

                        put("dbm", ss.dbm)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) put("rssi", ss.rssi)
                        put("timing_advance", ss.timingAdvance)
                    })
                }
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && info is android.telephony.CellInfoNr) {
                    val id = info.cellIdentity as android.telephony.CellIdentityNr
                    val ss = info.cellSignalStrength as android.telephony.CellSignalStrengthNr
                    root.put("nr", JSONObject().apply {
                        put("nci", id.nci)
                        put("pci", id.pci)
                        put("nrarfcn", id.nrarfcn)
                        put("tac", id.tac)
                        put("mcc", id.mccString)
                        put("mnc", id.mncString)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) put("band", id.bands.joinToString(","))

                        put("ss_rsrp", ss.csiRsrp)
                        put("ss_rsrq", ss.csiRsrq)
                        put("ss_sinr", ss.csiSinr)
                    })
                }
            }
        } catch (e: SecurityException) {
            Log.e("TELEMETRY", "Нет прав для получения данных о сети")
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


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(CHANNEL_ID, "Telemetry", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        locationManager.removeUpdates(this)
        resetSocket()
        zContext?.destroy()
        if (wakeLock?.isHeld == true) wakeLock?.release()
    }
}