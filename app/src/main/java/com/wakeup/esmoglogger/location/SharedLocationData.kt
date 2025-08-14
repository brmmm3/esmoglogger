package com.wakeup.esmoglogger.location

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object SharedLocationData {
    private val _location = MutableLiveData<Location>()
    val location: LiveData<Location> get() = _location

    fun sendLocation(location: Location) {
        _location.value = location
    }
}