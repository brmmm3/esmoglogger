package com.wakeup.esmoglogger.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CopyOnWriteArrayList

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
        if (!seriesNotes.isEmpty()) {
            jsonObject.put("notes", seriesNotes)
        }
        if (!data.isEmpty()) {
            jsonObject.put("data",
                JSONArray().apply {
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
                })
        }
        return jsonObject
    }
}
