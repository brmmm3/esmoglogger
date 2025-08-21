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
                            val latitude: Double, val longitude: Double, val altitude: Double)

class DataSeries {
    var startTime: LocalDateTime = LocalDateTime.now()
    // Data series name
    var name = ""
    // App version
    var version: Int = 0
    // Device name
    var device = ""
    var data: CopyOnWriteArrayList<ESmogAndLocation> = CopyOnWriteArrayList()
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
    }

    fun add(value: ESmogAndLocation) {
        data.add(value)
    }

    fun add(time: Float, level: Float, frequency: Int,
            latitude: Double, longitude: Double, altitude: Double) {
        data.add(ESmogAndLocation(time, level, frequency, latitude, longitude, altitude))
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
        if (!seriesNotes.isEmpty()) {
            jsonObject.put("notes", seriesNotes)
        }
        if (!data.isEmpty()) {
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
            val compressedData = JsonCompressor.compressJson(jsonData)
            if (compressedData != null) {
                Log.d("DataSeries", "Compressed size: ${compressedData.size} bytes")
                jsonObject.put("data",compressedData.toString())
            } else {
                Log.e("DataSeries", "Failed to compress JSON")
            }
        }
        return jsonObject
    }

    fun fromJson(jsonObject: JSONObject) {
        name = jsonObject.optString("name")
        version = jsonObject.getInt("version")
        device = jsonObject.optString("device")
        startTime = jsonObject.get("start") as LocalDateTime
        seriesNotes = jsonObject.optString("notes")
        try {
            val jsonArray = JsonCompressor.decompressJson(jsonObject.optString("data").encodeToByteArray()) ?: JSONArray()
            val newData: CopyOnWriteArrayList<ESmogAndLocation> = CopyOnWriteArrayList()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONArray(i) ?: continue
                newData.add(ESmogAndLocation(
                    jsonObject.get(0) as Float,
                    jsonObject.get(1) as Float,
                    jsonObject.get(2) as Int,
                    jsonObject.get(3) as Double,
                    jsonObject.get(4) as Double,
                    jsonObject.get(5) as Double
                ))
            }
            data = newData
        } catch (e: Exception) {
            Log.e("DataSeries", "Failed to parse JSONArray: ${e.message}", e)
            null
        }
    }
}
