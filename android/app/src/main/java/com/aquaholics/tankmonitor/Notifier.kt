package com.aquaholics.tankmonitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object Notifier {
    const val ALERTS = "alerts"
    const val STATUS = "status"

    fun channels(c: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val m = c.getSystemService(NotificationManager::class.java)
        m.createNotificationChannel(NotificationChannel(ALERTS, "Tank alerts", NotificationManager.IMPORTANCE_HIGH))
        m.createNotificationChannel(NotificationChannel(STATUS, "Alert listener", NotificationManager.IMPORTANCE_MIN))
    }

    private fun open(c: Context) = PendingIntent.getActivity(
        c, 0, Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    @Suppress("DEPRECATION")
    private fun builder(c: Context, channel: String): Notification.Builder =
        if (Build.VERSION.SDK_INT >= 26) Notification.Builder(c, channel) else Notification.Builder(c)

    fun status(c: Context): Notification =
        builder(c, STATUS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Tank Monitor is watching for alerts")
            .setContentIntent(open(c))
            .setOngoing(true)
            .build()

    fun alert(c: Context, title: String, text: String) {
        val n = builder(c, ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(open(c))
            .setAutoCancel(true)
            .build()
        try {
            c.getSystemService(NotificationManager::class.java)
                .notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), n)
        } catch (_: SecurityException) { /* notification permission not granted */ }
    }
}
