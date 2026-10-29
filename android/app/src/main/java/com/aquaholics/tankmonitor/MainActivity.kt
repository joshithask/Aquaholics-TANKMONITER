package com.aquaholics.tankmonitor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

// Shows the bundled Aquaholics dashboard (app/src/main/assets/index.html).
// In the app: Setup tab -> type http://<esp32-ip>/reading -> Save. The address is remembered.
// Setup tab -> Phone alerts: pick a secret name, turn alerts on, and use the same name in the ESP32 code.
class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView

    private inner class Bridge {
        private val prefs get() = getSharedPreferences("aqua", Context.MODE_PRIVATE)

        @JavascriptInterface fun getTopic(): String = prefs.getString("topic", "") ?: ""

        @JavascriptInterface fun setTopic(t: String) {
            val topic = t.trim()
            prefs.edit().putString("topic", topic).remove("since").apply()
            runOnUiThread {
                AlertService.stop(this@MainActivity)
                if (topic.isNotEmpty()) { askNotificationPermission(); AlertService.start(this@MainActivity) }
            }
        }

        @JavascriptInterface fun testNotify() {
            runOnUiThread {
                askNotificationPermission()
                Notifier.channels(this@MainActivity)
                Notifier.alert(this@MainActivity, "Tank Monitor test", "Alerts are working on this phone.")
            }
        }

        @JavascriptInterface fun batterySettings() {
            runOnUiThread {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true              // remembers address and calibration
            allowFileAccess = true
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = true   // bundled page may call the ESP32 over http
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        web.addJavascriptInterface(Bridge(), "AquaholicsApp")
        web.webViewClient = WebViewClient()
        setContentView(web)
        if (savedInstanceState == null) web.loadUrl("file:///android_asset/index.html")
        else web.restoreState(savedInstanceState)

        // Make sure the alert listener is running whenever the app is opened.
        if (!(getSharedPreferences("aqua", Context.MODE_PRIVATE).getString("topic", "")).isNullOrEmpty()) {
            askNotificationPermission()
            try { AlertService.start(this) } catch (_: Exception) { }
        }
    }

    override fun onResume() { super.onResume(); web.onResume() }
    override fun onPause() { web.onPause(); super.onPause() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); web.saveState(outState) }
}
