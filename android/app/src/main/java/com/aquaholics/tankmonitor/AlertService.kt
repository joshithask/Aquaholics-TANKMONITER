package com.aquaholics.tankmonitor

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// Keeps a live connection to ntfy.sh for your secret topic and shows each message as a notification.
// The ESP32 posts to the same topic when the water status changes.
class AlertService : Service() {
    @Volatile private var running = false
    private var worker: Thread? = null
    @Volatile private var conn: HttpURLConnection? = null

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, flags: Int, startId: Int): Int {
        Notifier.channels(this)
        val n = Notifier.status(this)
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
        if (!running) {
            running = true
            worker = Thread { loop() }.also { it.start() }
        }
        return START_STICKY
    }

    private fun loop() {
        val prefs = getSharedPreferences("aqua", Context.MODE_PRIVATE)
        var backoff = 2000L
        while (running) {
            val topic = prefs.getString("topic", "")?.trim().orEmpty()
            if (topic.isEmpty()) { stopSelf(); return }
            try {
                val since = prefs.getString("since", null) ?: (System.currentTimeMillis() / 1000).toString()
                val c = URL("https://ntfy.sh/$topic/json?since=$since").openConnection() as HttpURLConnection
                conn = c
                c.connectTimeout = 15000
                c.readTimeout = 120000          // ntfy sends a keep-alive about every 45 s
                c.inputStream.bufferedReader().useLines { lines ->
                    backoff = 2000L
                    for (line in lines) {
                        if (!running) break
                        handle(line, prefs)
                    }
                }
            } catch (_: Exception) {
            } finally {
                conn?.disconnect()
            }
            if (!running) break
            try { Thread.sleep(backoff) } catch (_: InterruptedException) { break }
            backoff = minOf(backoff * 2, 60000L)
        }
    }

    private fun handle(line: String, prefs: android.content.SharedPreferences) {
        try {
            val j = JSONObject(line)
            if (j.optString("event") != "message") return
            val id = j.optString("id")
            if (id.isNotEmpty()) prefs.edit().putString("since", id).apply()
            Notifier.alert(this, j.optString("title", "Tank Monitor"), j.optString("message"))
        } catch (_: Exception) { }
    }

    override fun onDestroy() {
        running = false
        Thread { try { conn?.disconnect() } catch (_: Exception) { } }.start()
        worker?.interrupt()
        super.onDestroy()
    }

    companion object {
        fun start(c: Context) {
            val i = Intent(c, AlertService::class.java)
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i) else c.startService(i)
        }
        fun stop(c: Context) { c.stopService(Intent(c, AlertService::class.java)) }
    }
}
