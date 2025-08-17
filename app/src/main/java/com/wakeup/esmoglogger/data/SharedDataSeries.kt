package com.wakeup.esmoglogger.data

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.wakeup.esmoglogger.FileInfo
import java.time.Duration
import java.time.LocalDateTime
import kotlin.collections.MutableList

object  SharedDataSeries {
    val dataSeriesList: MutableList<FileInfo> = mutableListOf()
    var dataSeries = DataSeries()

    private val _esmog = MutableLiveData<ESmog>()
    val esmog: LiveData<ESmog> get() = _esmog

    fun clear() {
        dataSeries.clear()
    }

    fun start() {
        dataSeries.clear()
        dataSeries.start()
    }

    fun stop(name: String) {
        dataSeries.stop(name)
    }

    fun addESmog(level: Float, frequency: Int) {
        val dt = Duration.between(dataSeries.startTime, LocalDateTime.now()).toMillis().toFloat() / 1000f
        val esmog = ESmog(dt, level, frequency)
        dataSeries.addESmog(esmog)
        _esmog.postValue(esmog)
    }

    fun addGpsLocation(location: Location) {
        val dt = Duration.between(dataSeries.startTime, LocalDateTime.now()).toMillis().toFloat() / 1000f
        val gpsLocation = GpsLocation(dt, location.latitude, location.longitude, location.altitude)
        dataSeries.addGpsLocation(gpsLocation)
        //_gps_location.value = gpsLocation
    }

    fun setNotes(notes: String) {
        dataSeries.setNotes(notes)
    }

    fun saved(fileInfo: FileInfo) {
        dataSeries.filename = fileInfo.name
        dataSeriesList.add(fileInfo)
    }
}
