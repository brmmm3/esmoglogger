package com.wakeup.esmoglogger.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CopyOnWriteArrayList

class DataSeries {
    var startTime: LocalDateTime = LocalDateTime.now()
    // Data series name
    var name = ""
    var lvlFrqData: CopyOnWriteArrayList<Pair<Float, Pair<Float, Int>>> = CopyOnWriteArrayList()
    var gpsLocations: CopyOnWriteArrayList<Pair<Double, Double>> = CopyOnWriteArrayList()
    // Data series notes
    var seriesNotes = ""
    // Filename. If empty dataseries is not saved
    var filename = ""

    fun clear() {
        lvlFrqData = CopyOnWriteArrayList()
    }

    fun start() {
        lvlFrqData = CopyOnWriteArrayList()
        startTime = LocalDateTime.now()
    }

    fun addLvlFrq(dt: Float, data: Pair<Float, Int>) {
        lvlFrqData.add(Pair(dt, data))
    }

    fun addGpsLocation(data: Pair<Double, Double>) {
        gpsLocations.add(data)
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
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        jsonObject.put("start", startTime.format(formatter))
        if (!seriesNotes.isEmpty()) {
            jsonObject.put("notes", seriesNotes)
        }
        if (!lvlFrqData.isEmpty()) {
            jsonObject.put("lvlfrq",
                JSONArray().apply {
                    lvlFrqData.forEach { pair ->
                    put(JSONArray().apply {
                        put(pair.first)
                        put(pair.second.first)
                        put(pair.second.second)
                    })
                }
            })
        }
        if (!gpsLocations.isEmpty()) {
            jsonObject.put("gps",
                JSONArray().apply {
                    gpsLocations.forEach { pair ->
                        put(JSONArray().apply {
                            put(pair.first)
                            put(pair.second)
                        })
                    }
                })
        }
        return jsonObject
    }
}
