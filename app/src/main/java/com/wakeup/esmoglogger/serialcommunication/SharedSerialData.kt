package com.wakeup.esmoglogger.serialcommunication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object SharedSerialData {
    private val _command = MutableLiveData<String>()
    val command: LiveData<String> get() = _command

    fun start() {
        _command.value = "start"
    }

    fun stop() {
        _command.value = "stop"
    }
}