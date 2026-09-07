package org.lanenode

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import java.net.NetworkInterface

class LaneService : android.app.Service() {

    companion object {
        const val CH = "lane"
        @Volatile var running = false
        @Volatile var server: ProxyServer? = null
        @Volatile var binder: CellularBinder? = null
        @Volatile var lastError: String? = null

        fun lanAddress(): String {
            return try {
                NetworkInterface.getNetworkInterfaces().toList()
                    .filter { it.isUp && !it.isLoopback }
                    .flatMap { it.inetAddresses.toList() }
                    .firstOrNull {
                        val ha = it.hostAddress ?: return@firstOrNull false
                        !it.isLoopbackAddress &&
                            ha.contains('.') &&
                            (ha.startsWith("192.168") || ha.startsWith("10."))
                    }
                    ?.hostAddress ?: "unknown"
            } catch (e: Exception) { "unknown" }
        }
    }

    private var wake: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (running) return START_STICKY
        val cfg = Config.load(this)

        createChannel()
        val n = notification("starting…")
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1, n)
        }

        wake = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "lanenode:cpu").apply { acquire() }
        wifiLock = (getSystemService(Context.WIFI_SERVICE) as WifiManager)
            .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "lanenode:wifi")
            .apply { acquire() }

        Thread {
            val b = CellularBinder(this)
            binder = b
            if (!b.start()) {
                lastError = "cellular did not come up — is mobile data enabled?"
                stopSelf()
                return@Thread
            }
            val s = ProxyServer(cfg, b)
            server = s
            try {
                s.start()
                running = true
                update("listening ${lanAddress()}:${cfg.listenPort}")
            } catch (e: Exception) {
                lastError = e.message
                stopSelf()
            }
        }.start()

        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        server?.stop(); server = null
        binder?.stop(); binder = null
        runCatching { wake?.release() }
        runCatching { wifiLock?.release() }
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CH, "Lane node", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun notification(text: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CH)
            .setContentTitle("Lane node active")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun update(text: String) {
        getSystemService(NotificationManager::class.java).notify(1, notification(text))
    }
}
