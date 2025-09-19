package com.wakeup.esmoglogger

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import androidx.core.graphics.toColorInt
import com.google.android.material.button.MaterialButton
import org.osmdroid.util.GeoPoint
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

const val PREFS_KEY: String = "ESmogLogger"
const val PREFS_DARKMODE: String = "DarkMode"

data class FileInfo(val name: String, val size: Long, val hasGps: Boolean, val count: Int)

val levelLimits = arrayListOf(
    0.18,
    5.8
)

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

fun calcDistance(p1: GeoPoint, p2: GeoPoint): Double {
    val R = 6371000.0
    val dLatitde: Double = abs(p1.latitude.minus(p2.latitude))
    val dLongitude: Double = abs(p1.longitude.minus(p2.longitude))
    val a = sin(dLatitde / 360.0 * PI).pow(2.0) + cos(p1.latitude / 180.0 * PI) * cos(p2.latitude / 180.0 * PI) * sin(dLongitude / 360.0 * PI).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    val d = R * c
    val dAltitude = p1.altitude - p2.altitude
    return sqrt(d * d + dAltitude * dAltitude)
}

class FixedSizeArray<T>(private val size: Int) {
    private val array = arrayOfNulls<Pair<Float, Double>>(size)
    private var head = 0 // Points to the next insertion position

    fun push(value: Pair<Float, Double>) {
        array[head] = value
        head = (head + 1) % size
    }

    fun getArray(): List<T> {
        // Return array in logical order (oldest to newest)
        return (0 until size).map { array[(head + it) % size] as T }
    }

    fun getDistanceSum(): Float {
        var sum = 0.0f // Use Double to handle all numeric types
        for (i in 0 until size) {
            val value = array[(head + i) % size]
            when (value) {
                is Pair<*, *> -> sum += value.first
            }
        }
        return sum
    }

    fun getTimeSum(): Double {
        var sum = 0.0 // Use Double to handle all numeric types
        for (i in 0 until size) {
            val value = array[(head + i) % size]
            when (value) {
                is Pair<*, *> -> sum += value.second
            }
        }
        return sum
    }
}
