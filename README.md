# Aquaholics Tank Monitor

- `firmware/aquaholics_esp32/` : ESP32 sketch (Arduino IDE). Open the .ino file.
- `android/` : Android app (WebView dashboard + push alerts)
- `web/` : standalone dashboard page

## Before you upload the firmware
Edit the settings at the top of `aquaholics_esp32.ino`. Never commit your real Wi-Fi password,
Gmail App Password, or alert topic to a public repository.

## Get the APK
Actions tab -> "Build Android APK" -> latest run -> Artifacts -> TankMonitor-apk.
