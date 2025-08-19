package com.wakeup.esmoglogger

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import com.google.android.material.button.MaterialButton

data class FileInfo(val name: String, val size: Long)

fun buttonSetEnabled(button: MaterialButton?, enabled: Boolean) {
    button?.isEnabled = enabled
    if (enabled) {
        button?.icon?.clearColorFilter()
    } else {
        button?.icon?.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP)
    }
}

fun isEmulator(): Boolean {
    return (Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.MODEL.contains("google_sdk") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for x86") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic") ||
            Build.PRODUCT == "sdk_gphone64_arm64" ||
            Build.HARDWARE.contains("goldfish") ||
            Build.HARDWARE.contains("ranchu"))
}
