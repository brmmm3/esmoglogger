package com.wakeup.esmoglogger.serialcommunication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object SharedSerialData {
    private val _command = MutableLiveData<String>()
    val command: LiveData<String> get() = _command

    fun sendCommand(command: String) {
        _command.value = command
    }
}