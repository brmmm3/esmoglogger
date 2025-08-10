package com.wakeup.esmoglogger.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlin.collections.MutableList

object  SharedESmogData {
    private val _data = MutableLiveData<Pair<Float, Int>>()
    val data: LiveData<Pair<Float, Int>> get() = _data

    val dataSeriesHistory: MutableList<DataSeries> = mutableListOf()
    var dataSeries = DataSeries()

    private val _gps_recording = MutableLiveData<Boolean>()
    val gps_recording: LiveData<Boolean> get() = _gps_recording

    private val _gps_location = MutableLiveData<Pair<Double, Double>>()
    val gps_location: LiveData<Pair<Double, Double>> get() = _gps_location

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

    fun addLvlFrq(data: Pair<Float, Int>) {
        dataSeries.addLvlFrq(data)
        _data.postValue(data)
    }

    fun stop(name: String) {
        dataSeries.stop(name)
    }

    fun setGpsRecording(isChecked: Boolean) {
        _gps_recording.value = isChecked
    }

    fun addGpsLocation(latitude: Double, longitude: Double) {
        dataSeries.addGpsLocation(Pair(latitude, longitude))
        _gps_location.value = Pair(latitude, longitude)
    }

    fun setNotes(notes: String) {
        dataSeries.setNotes(notes)
    }

    fun saved(fileName: String) {
        dataSeries.filename = fileName
        dataSeriesHistory.add(dataSeries)
        dataSeries = DataSeries()
    }
}
