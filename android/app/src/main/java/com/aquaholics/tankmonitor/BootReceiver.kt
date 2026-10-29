package com.aquaholics.tankmonitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// Restarts the alert listener after the phone reboots.
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent?) {
        if (i?.action != Intent.ACTION_BOOT_COMPLETED) return
        val topic = c.getSharedPreferences("aqua", Context.MODE_PRIVATE).getString("topic", "").orEmpty()
        if (topic.isNotEmpty()) try { AlertService.start(c) } catch (_: Exception) { }
    }
}
