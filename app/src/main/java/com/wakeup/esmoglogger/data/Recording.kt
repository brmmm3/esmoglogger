package com.wakeup.esmoglogger.data

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.Float

data class ESmog(val time: Float,
                 val level: Float,
                 val frequency: Int)

data class GpsLocation(val time: Float,
                       val latitude: Double,
                       val longitude: Double,
                       val altitude: Double)

data class ESmogAndLocation(val time: Float,
                            var level: Float,
                            val frequency: Int,
                            var latitude: Double,
                            var longitude: Double,
                            var altitude: Double)

class Recording {
    var startTime: LocalDateTime = LocalDateTime.now()
    var endTime: LocalDateTime = LocalDateTime.now()
    // Data series name
    var name = ""
    // App version
    var version: Int = 0
    // Device name
    var device = ""
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
        data = CopyOnWriteArrayList(decompressData(compressedHex!!))
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
    }

    fun setNotes(notes: String) {
        this.seriesNotes = notes
    }

    fun toJson(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("name", name)
        jsonObject.put("version", version)
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

        fun decompressData(compressedHex: String): ArrayList<ESmogAndLocation> {
            val jsonArray = JsonCompressor.decompressJson(compressedHex) ?: JSONArray()
            val data: ArrayList<ESmogAndLocation> = ArrayList()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONArray(i) ?: continue
                data.add(ESmogAndLocation(
                    jsonObject.getDouble(0).toFloat(),
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
            data.forEach { value ->
                put(JSONArray().apply {
                    put(value.time)
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
