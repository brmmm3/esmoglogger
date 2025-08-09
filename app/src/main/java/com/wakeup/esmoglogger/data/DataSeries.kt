package com.wakeup.esmoglogger.data

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CopyOnWriteArrayList

class DataSeries {
    var startTime = ""
    // Data series name
    var seriesName = ""
    var dataSeries: CopyOnWriteArrayList<Pair<Float, Int>> = CopyOnWriteArrayList()
    // Data series notes
    var seriesNotes = ""

    fun clear() {
        dataSeries = CopyOnWriteArrayList()
    }

    fun start() {
        dataSeries = CopyOnWriteArrayList()
        val currentDateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        startTime = currentDateTime.format(formatter)
    }

    fun add(data: Pair<Float, Int>) {
        dataSeries.add(data)
    }

    fun stop(name: String) {
        this.seriesName = name
    }

    fun setNotes(notes: String) {
        this.seriesNotes = notes
    }
}
