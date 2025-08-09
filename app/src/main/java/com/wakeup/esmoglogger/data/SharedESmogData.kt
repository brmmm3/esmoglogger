package com.wakeup.esmoglogger.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlin.collections.MutableList

object SharedESmogData {
    private val _data = MutableLiveData<Pair<Float, Int>>()
    val data: LiveData<Pair<Float, Int>> get() = _data

    private val dataSeriesHistory: MutableList<DataSeries> = mutableListOf()
    private var dataSeries = DataSeries()

    fun clear() {
        dataSeries.clear()
    }

    fun clearHistory() {
        dataSeriesHistory.clear()
    }

    fun start() {
        dataSeries.clear()
        dataSeries.start()
    }

    fun add(data: Pair<Float, Int>) {
        dataSeries.add(data)
        _data.postValue(data)
    }

    fun stop(name: String) {
        dataSeries.stop(name)
        dataSeriesHistory.add(dataSeries)
        dataSeries = DataSeries()
    }

    fun setNotes(notes: String) {
        dataSeries.setNotes(notes)
    }
}