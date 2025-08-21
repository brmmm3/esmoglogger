package com.wakeup.esmoglogger

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakeup.esmoglogger.data.DataSeries
import com.wakeup.esmoglogger.data.ESmog
import com.wakeup.esmoglogger.data.ESmogAndLocation
import com.wakeup.esmoglogger.data.GpsLocation
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime

class SharedViewModel: ViewModel() {
    // Enable/Disable GPS
    private val _gps = MutableLiveData<Boolean>()
    val gps: LiveData<Boolean> get() = _gps
    // Enable/Disable ESmog (and GPS) recording
    private val _recording = MutableLiveData<Boolean>()
    val recording: LiveData<Boolean> get() = _recording
    // Location and ESmog queue
    private val _locationAndESmogQueue = MutableSharedFlow<ESmogAndLocation>(
        replay = 0, // No replay for new subscribers
        extraBufferCapacity = 1000, // Large buffer to handle high-frequency data
        onBufferOverflow = BufferOverflow.SUSPEND // Suspend emitter if buffer is full
    )
    val locationAndESmogQueue = _locationAndESmogQueue.asSharedFlow()
    // MAP view
    var mapViewZoom = 18.0
    // Latest values
    private val _esmog = MutableLiveData<ESmog>()
    val esmog: LiveData<ESmog> get() = _esmog
    private val _location = MutableLiveData<GpsLocation>()
    val location: LiveData<GpsLocation> get() = _location
    // DataSeries
    val dataSeriesList: MutableList<FileInfo> = mutableListOf()
    var dataSeries = DataSeries()

    fun startGps() {
        _gps.value = true
    }

    fun stopGps() {
        _gps.value = false
    }

    fun startRecording() {
        dataSeries.clear()
        dataSeries.start()
        _recording.value = true
    }

    fun stopRecording(name: String) {
        dataSeries.stop(name)
        _recording.value = false
    }

    fun clear() {
        mapViewZoom = 18.0
        dataSeries.clear()
    }

    fun enqueueLocationAndESmog(value: ESmogAndLocation) {
        viewModelScope.launch {
            _locationAndESmogQueue.emit(value)
        }
    }

    fun addESmog(level: Float, frequency: Int) {
        val dt = Duration.between(dataSeries.startTime, LocalDateTime.now()).toMillis().toFloat() / 1000f
        val gpsLocation: GpsLocation = location.value ?: GpsLocation(0f, 0.0, 0.0, 0.0)
        val value = ESmogAndLocation(dt, level, frequency, gpsLocation.latitude, gpsLocation.longitude, gpsLocation.altitude)
        _esmog.postValue(ESmog(dt, level, frequency))
        viewModelScope.launch {
            _locationAndESmogQueue.emit(value)
        }
        if (recording.value == true) {
            dataSeries.add(value)
        }
    }

    fun addLocation(location: Location) {
        val dt = Duration.between(dataSeries.startTime, LocalDateTime.now()).toMillis().toFloat() / 1000f
        val esmog: ESmog = if (recording.value == true) {
            esmog.value ?: ESmog(0f, 0f, 0)
        } else {
            ESmog(0f, 0f, 0)
        }
        val value = ESmogAndLocation(dt, esmog.level, esmog.frequency, location.latitude, location.longitude, location.altitude)
        _location.postValue(GpsLocation(dt, location.latitude, location.longitude, location.altitude))
        viewModelScope.launch {
            _locationAndESmogQueue.emit(value)
        }
        if (recording.value == true) {
            dataSeries.add(value)
        }
    }

    fun setNotes(notes: String) {
        dataSeries.setNotes(notes)
    }

    fun saved(fileInfo: FileInfo) {
        dataSeries.filename = fileInfo.name
        dataSeriesList.add(fileInfo)
    }
}