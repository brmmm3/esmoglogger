package com.wakeup.esmoglogger

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import androidx.core.graphics.toColorInt
import com.google.android.material.button.MaterialButton

const val PREFS_KEY: String = "ESmogLogger"
const val PREFS_DARKMODE: String = "DarkMode"

data class FileInfo(val name: String, val size: Long, val hasGps: Boolean, val count: Int)

val levelColors = arrayListOf(
    Pair(0.0, Color.GRAY),
    Pair(0.06, "#007F00".toColorInt()),
    Pair(0.18, Color.GREEN),
    Pair(0.28, "#7FFF00".toColorInt()),
    Pair(1.0, "#AFFF00".toColorInt()),
    Pair(2.0, "#BFFF00".toColorInt()),
    Pair(3.0, "#CFFF00".toColorInt()),
    Pair(4.0, "#DFFF00".toColorInt()),
    Pair(5.8, Color.YELLOW),
    Pair(8.0, "#FF7F00".toColorInt()),
    Pair(9.0, "#FF5F00".toColorInt()),
    Pair(10.0, "#FF3F00".toColorInt()),
    Pair(15.0, Color.RED),
    Pair(180.0, Color.RED))

fun getLevelColor(level: Float): Int {
    for (levelColor in levelColors) {
        if (level <= levelColor.first) {
            return levelColor.second
        }
    }
    return Color.RED
}

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
