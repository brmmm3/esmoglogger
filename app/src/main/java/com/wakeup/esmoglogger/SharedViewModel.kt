package com.wakeup.esmoglogger

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakeup.esmoglogger.data.Recording
import com.wakeup.esmoglogger.data.ESmog
import com.wakeup.esmoglogger.data.ESmogAndLocation
import com.wakeup.esmoglogger.data.GpsLocation
import com.wakeup.esmoglogger.ui.mapview.MapViewData
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.max

class SharedViewModel: ViewModel() {
    // Enable/Disable GPS
    private val _gps = MutableLiveData<Boolean>()
    val gps: LiveData<Boolean> get() = _gps
    // Enable/Disable ESmog (and GPS) recording
    private val _recording = MutableLiveData<Boolean>()
    val recording: LiveData<Boolean> get() = _recording
    // Dataseries has been saved to file
    private val _saved = MutableLiveData<FileInfo>()
    val saved: LiveData<FileInfo> get() = _saved
    // Location and ESmog queue
    private val _mapViewDataQueue = MutableSharedFlow<MapViewData>(
        replay = 0, // No replay for new subscribers
        extraBufferCapacity = 1000, // Large buffer to handle high-frequency data
        onBufferOverflow = BufferOverflow.SUSPEND // Suspend emitter if buffer is full
    )
    val mapViewDataQueue = _mapViewDataQueue.asSharedFlow()
    // MAP view
    var mapViewZoom = 18.0
    // Latest values
    private val _esmogQueue = MutableSharedFlow<ESmog>(
        replay = 0, // No replay for new subscribers
        extraBufferCapacity = 1000, // Large buffer to handle high-frequency data
        onBufferOverflow = BufferOverflow.SUSPEND // Suspend emitter if buffer is full
    )
    val esmogQueue = _esmogQueue.asSharedFlow()
    private val _esmog = MutableLiveData<ESmog>()
    val esmog: LiveData<ESmog> get() = _esmog
    private var lastESmogPos = 0
    private val _location = MutableLiveData<GpsLocation>()
    val location: LiveData<GpsLocation> get() = _location
    // DataSeries
    val recordings: MutableList<Recording> = mutableListOf()
    var dataSeries = Recording()

    fun startGps() {
        _gps.value = true
    }

    fun stopGps() {
        _gps.value = false
    }

    fun startRecording() {
        clear()
        dataSeries.start()
        _recording.value = true
    }

    fun stopRecording(name: String) {
        dataSeries.stop(name)
        _recording.value = false
    }

    fun clear() {
        mapViewZoom = 18.0
        lastESmogPos = 0
        dataSeries = Recording()
    }

    fun enqueueESmog(value: ESmog) {
        viewModelScope.launch {
            _esmogQueue.emit(value)
        }
    }

    fun enqueueLocationAndESmog(value: ESmogAndLocation) {
        viewModelScope.launch {
            _mapViewDataQueue.emit(MapViewData(value, false))
        }
    }

    fun addESmog(level: Float, frequency: Int) {
        val dt = Duration.between(dataSeries.startTime, LocalDateTime.now()).toMillis().toFloat() / 1000f
        val gpsLocation: GpsLocation = location.value ?: GpsLocation(0f, 0.0, 0.0, 0.0)
        val value = ESmog(dt, level, frequency)
        _esmog.postValue(value)
        viewModelScope.launch {
            _esmogQueue.emit(value)
        }
        if (recording.value == true) {
            dataSeries.add(ESmogAndLocation(dt, level, frequency, gpsLocation.latitude, gpsLocation.longitude, gpsLocation.altitude))
        }
    }

    fun addLocation(location: Location) {
        val dt = Duration.between(dataSeries.startTime, LocalDateTime.now()).toMillis().toFloat() / 1000f
        _location.postValue(GpsLocation(dt, location.latitude, location.longitude, location.altitude))
        viewModelScope.launch {
            var cnt = dataSeries.data.size - lastESmogPos
            if (cnt > 0) {
                println("GPS ${dataSeries.data.size} $cnt (${location.latitude} ${location.longitude} ${location.altitude})")
                val refValue = dataSeries.data[lastESmogPos]!!.copy()
                _mapViewDataQueue.emit(MapViewData(refValue, false))
                val dLatitude = (location.latitude - refValue.latitude) / cnt
                val dLongitude = (location.longitude - refValue.longitude) / cnt
                val dAltitude = (location.altitude - refValue.altitude) / cnt
                val moving = dLatitude != 0.0 || dLongitude != 0.0 || dAltitude != 0.0
                println("# $lastESmogPos: $refValue")
                var maxLevel = refValue.level
                var pos = 0
                while (cnt > 2) {
                    pos++
                    cnt--
                    lastESmogPos++
                    val value = dataSeries.data[lastESmogPos]!!
                    value.latitude = refValue.latitude + dLatitude * pos
                    value.longitude = refValue.longitude + dLongitude * pos
                    value.altitude = refValue.altitude + dAltitude * pos
                    dataSeries.data[lastESmogPos] = value
                    if (moving) {
                        println("*$pos $cnt $lastESmogPos: $value")
                        _mapViewDataQueue.emit(MapViewData(value, false))
                    } else {
                        maxLevel = max(value.level, maxLevel)
                    }
                }
                lastESmogPos++
                if (lastESmogPos < dataSeries.data.size) {
                    var value = dataSeries.data[lastESmogPos]!!
                    value.latitude = location.latitude
                    value.longitude = location.longitude
                    value.altitude = location.altitude
                    dataSeries.data[lastESmogPos] = value
                    println("= $lastESmogPos: $value")
                    if (!moving) {
                        value = value.copy()
                        value.level = max(value.level, maxLevel)
                    }
                    _mapViewDataQueue.emit(MapViewData(value, false))
                }
            } else {
                val esmog: ESmog = if (recording.value == true) {
                    esmog.value ?: ESmog(0f, 0f, 0)
                } else {
                    ESmog(0f, 0f, 0)
                }
                val value = ESmogAndLocation(dt, esmog.level, esmog.frequency, location.latitude, location.longitude, location.altitude)
                _mapViewDataQueue.emit(MapViewData(value, false))
            }
        }
    }

    fun setNotes(notes: String) {
        dataSeries.setNotes(notes)
    }

    fun saved(fileInfo: FileInfo) {
        dataSeries.fileName = fileInfo.name
        dataSeries.fileSize = fileInfo.size
        recordings.add(dataSeries)
        _saved.postValue(fileInfo)
    }
}