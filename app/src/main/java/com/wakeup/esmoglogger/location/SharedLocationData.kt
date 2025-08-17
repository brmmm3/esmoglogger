package com.wakeup.esmoglogger.location

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object SharedLocationData {
    private val _command = MutableLiveData<String>()
    val command: LiveData<String> get() = _command

    private val _location = MutableLiveData<Location>()
    val location: LiveData<Location> get() = _location

    fun start() {
        _command.value = "start"
    }

    fun stop() {
        _command.value = "stop"
    }

    fun sendLocation(location: Location) {
        _location.value = location
    }
}