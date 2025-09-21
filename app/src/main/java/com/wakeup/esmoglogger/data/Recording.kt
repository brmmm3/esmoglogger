package com.wakeup.esmoglogger.data

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.Float

data class ESmog(var time: Long,
                 val level: Float,
                 val frequency: Int)

data class GpsLocation(var time: Long,
                       val latitude: Double,
                       val longitude: Double,
                       val altitude: Double)

data class ESmogAndLocation(var time: Long,
                            var level: Float,
                            val frequency: Int,
                            var latitude: Double,
                            var longitude: Double,
                            var altitude: Double)

class Recording {
    var startTime: LocalDateTime = LocalDateTime.now()
    var startTimeMillis: Long = 0
    var endTime: LocalDateTime = LocalDateTime.now()
    var endTimeMillis: Long = 0
    // Data series name
    var name = ""
    // Data Format version
    var version: Int = 1
    // App version
    var appId = "com.wakeup.esmoglogger"  // Should be: BuildConfig.APPLICATION_ID
    // App version
    var appVersion = "0.1.0"  // Should be: BuildConfig.VERSION_NAME
    // Device name
    var device = "ED88TPlus5G2"
    var hasGps = false
    var data: CopyOnWriteArrayList<ESmogAndLocation> = CopyOnWriteArrayList()
    var compressedHex: String? = null
    var count: Int = 0
    // Data series notes
    var seriesNotes = ""
    // Filename. If empty dataseries is not saved
    var fileName = ""
    var fileSize: Long = 0
    // User who provided this recording
    var userName = ""

    fun isSaved(): Boolean {
        return fileName != ""
    }

    fun setSaved(fileName: String, fileSize: Long) {
        this.fileName = fileName
        this.fileSize = fileSize
        compressedHex = null
    }

    fun isLoaded(): Boolean {
        return compressedHex == null && !data.isEmpty() && !fileName.isEmpty()
    }

    fun load(): Boolean {
        if (compressedHex == null) {
            return false
        }
        data = CopyOnWriteArrayList(decompressData(version, compressedHex!!))
        if (endTime == startTime) {
            // Calculate end time from number of data values. Assume 2 values per second
            endTime = endTime.plusSeconds(data.size.toLong() / 2)
        }
        compressedHex = null
        return true
    }

    fun unload() {
        compressData()
        data.clear()
    }

    fun clear() {
        data = CopyOnWriteArrayList()
    }

    fun start() {
        data = CopyOnWriteArrayList()
        startTime = LocalDateTime.now()
        startTimeMillis = startTime.toInstant(ZoneOffset.UTC).toEpochMilli()
        hasGps = false
    }

    fun add(value: ESmogAndLocation) {
        data.add(value)
        if (!hasGps && (value.latitude != 0.0 || value.longitude != 0.0 || value.altitude != 0.0)) {
            hasGps = true
        }
    }

    fun stop(name: String) {
        this.name = name
        endTime = LocalDateTime.now()
        endTimeMillis = endTime.toInstant(ZoneOffset.UTC).toEpochMilli()
    }

    fun setNotes(notes: String) {
        this.seriesNotes = notes
    }

    fun toJson(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("name", name)
        jsonObject.put("version", version)
        jsonObject.put("appId", appId)
        jsonObject.put("appVersion", appVersion)
        jsonObject.put("device", device)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        jsonObject.put("start", startTime.format(formatter))
        jsonObject.put("end", endTime.format(formatter))
        jsonObject.put("count", data.size)
        jsonObject.put("has_gps", hasGps)
        if (!seriesNotes.isEmpty()) {
            jsonObject.put("notes", seriesNotes)
        }
        compressData()
        if (compressedHex != null) {
            Log.d("DataSeries", "Compressed size: ${compressedHex!!.length} bytes")
            jsonObject.put("data",compressedHex)
        } else {
            Log.e("DataSeries", "Failed to compress JSON")
        }
        return jsonObject
    }

    companion object {
        fun fromJson(jsonObject: JSONObject, fileName: String, fileSize: Long): Recording {
            val recording = Recording()
            recording.setSaved(fileName, fileSize)
            recording.name = jsonObject.optString("name")
            recording.version = jsonObject.getInt("version")
            recording.appId = jsonObject.optString("appId")
            recording.appVersion = jsonObject.optString("appVersion")
            recording.device = jsonObject.optString("device")
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            recording.startTime = LocalDateTime.parse(jsonObject.get("start") as CharSequence?, formatter)
            if (jsonObject.has("end")) {
                recording.endTime = LocalDateTime.parse(jsonObject.get("end") as CharSequence?, formatter)
            } else {
                // Real end time will be written later
                recording.endTime = recording.startTime
            }
            recording.seriesNotes = jsonObject.optString("notes")
            recording.hasGps = jsonObject.getBoolean("has_gps")
            recording.compressedHex = jsonObject.optString("data")
            recording.count = jsonObject.getInt("count")
            return recording
        }

        fun decompressData(version: Int, compressedHex: String): ArrayList<ESmogAndLocation> {
            val jsonArray = JsonCompressor.decompressJson(compressedHex) ?: JSONArray()
            val data: ArrayList<ESmogAndLocation> = ArrayList()
            var curTime = 0L
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONArray(i) ?: continue
                val dt = if (version == 0) {
                    (jsonObject.getDouble(0) * 1000.0).toLong()
                } else {
                    jsonObject.getLong(0)
                }
                curTime += dt
                data.add(ESmogAndLocation(
                    curTime,
                    jsonObject.getDouble(1).toFloat(),
                    jsonObject.getInt(2),
                    jsonObject.getDouble(3),
                    jsonObject.getDouble(4),
                    jsonObject.getDouble(5)
                ))
            }
            return data
        }
    }

    fun compressData() {
        count = data.size
        val jsonData = JSONArray().apply {
            var lastTime = 0L
            data.forEach { value ->
                val dt = value.time - lastTime
                lastTime = value.time
                put(JSONArray().apply {
                    put(dt)
                    put(value.level)
                    put(value.frequency)
                    put(value.latitude)
                    put(value.longitude)
                    put(value.altitude)
                })
            }
        }
        compressedHex = JsonCompressor.compressJson(jsonData)
    }
}
