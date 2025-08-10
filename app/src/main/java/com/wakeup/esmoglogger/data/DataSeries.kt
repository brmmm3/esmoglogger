package com.wakeup.esmoglogger.data

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CopyOnWriteArrayList

class DataSeries {
    var startTime = ""
    // Data series name
    var name = ""
    var lvlFrqData: CopyOnWriteArrayList<Pair<Float, Int>> = CopyOnWriteArrayList()
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
        val currentDateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        startTime = currentDateTime.format(formatter)
    }

    fun addLvlFrq(data: Pair<Float, Int>) {
        lvlFrqData.add(data)
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
}
