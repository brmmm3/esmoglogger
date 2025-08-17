package com.wakeup.esmoglogger.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CopyOnWriteArrayList

data class ESmog(val time: Float, val level: Float, val frequency: Int) {}

data class GpsLocation(val time: Float, val latitude: Double, val longitude: Double, val altitude: Double)

class DataSeries {
    var startTime: LocalDateTime = LocalDateTime.now()
    // Data series name
    var name = ""
    // App version
    var version: Int = 0
    // Device name
    var device = ""
    var esmogData: CopyOnWriteArrayList<ESmog> = CopyOnWriteArrayList()
    var gpsLocations: CopyOnWriteArrayList<GpsLocation> = CopyOnWriteArrayList()
    // Data series notes
    var seriesNotes = ""
    // Filename. If empty dataseries is not saved
    var filename = ""

    fun clear() {
        esmogData = CopyOnWriteArrayList()
    }

    fun start() {
        esmogData = CopyOnWriteArrayList()
        startTime = LocalDateTime.now()
    }

    fun addESmog(lvlFrq: ESmog) {
        esmogData.add(lvlFrq)
    }

    fun addGpsLocation(location: GpsLocation) {
        gpsLocations.add(location)
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
        if (!esmogData.isEmpty()) {
            jsonObject.put("lvlfrq",
                JSONArray().apply {
                    esmogData.forEach { lvlFrq ->
                    put(JSONArray().apply {
                        put(lvlFrq.time)
                        put(lvlFrq.level)
                        put(lvlFrq.frequency)
                    })
                }
            })
        }
        if (!gpsLocations.isEmpty()) {
            jsonObject.put("location",
                JSONArray().apply {
                    gpsLocations.forEach { location ->
                        put(JSONArray().apply {
                            put(location.time)
                            put(location.latitude)
                            put(location.longitude)
                            put(location.altitude)
                        })
                    }
                })
        }
        return jsonObject
    }
}
