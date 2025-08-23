package com.wakeup.esmoglogger.data

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.Float

data class ESmog(val time: Float,
                 val level: Float, val frequency: Int)

data class GpsLocation(val time: Float,
                       val latitude: Double, val longitude: Double, val altitude: Double)

data class ESmogAndLocation(val time: Float,
                            val level: Float, val frequency: Int,
                            var latitude: Double, var longitude: Double, var altitude: Double)

class DataSeries {
    var startTime: LocalDateTime = LocalDateTime.now()
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
    var filename = ""

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

    fun add(time: Float, level: Float, frequency: Int,
            latitude: Double, longitude: Double, altitude: Double) {
        data.add(ESmogAndLocation(time, level, frequency, latitude, longitude, altitude))
        if (!hasGps && (latitude != 0.0 || longitude != 0.0 || altitude != 0.0)) {
            hasGps = true
        }
    }

    fun stop(name: String) {
        this.name = name
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
        fun fromJson(jsonObject: JSONObject, decompress: Boolean): DataSeries {
            val dataSeries = DataSeries()
            dataSeries.name = jsonObject.optString("name")
            dataSeries.version = jsonObject.getInt("version")
            dataSeries.device = jsonObject.optString("device")
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            dataSeries.startTime = LocalDateTime.parse(jsonObject.get("start") as CharSequence?, formatter)
            dataSeries.seriesNotes = jsonObject.optString("notes")
            dataSeries.hasGps = jsonObject.getBoolean("has_gps")
            dataSeries.compressedHex = jsonObject.optString("data")
            dataSeries.count = jsonObject.getInt("count")
            if (decompress && dataSeries.compressedHex != null) {
                dataSeries.data = CopyOnWriteArrayList(decompressData(dataSeries.compressedHex!!))
            }
            return dataSeries
        }

        fun decompressData(compressedHex: String): ArrayList<ESmogAndLocation> {
            val jsonArray = JsonCompressor.decompressJson(compressedHex) ?: JSONArray()
            val data: ArrayList<ESmogAndLocation> = ArrayList()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONArray(i) ?: continue
                data.add(ESmogAndLocation(
                    jsonObject.get(0) as Float,
                    jsonObject.get(1) as Float,
                    jsonObject.get(2) as Int,
                    jsonObject.get(3) as Double,
                    jsonObject.get(4) as Double,
                    jsonObject.get(5) as Double
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
