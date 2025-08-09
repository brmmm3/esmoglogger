package com.wakeup.esmoglogger.serialcommunication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object SharedSerialData {
    private val _data = MutableLiveData<Pair<Float, Int>>()
    val data: LiveData<Pair<Float, Int>> get() = _data

    fun sendData(data: Pair<Float, Int>) {
        _data.postValue(data)
    }
}