package com.wakeup.esmoglogger.ui.mapview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object SharedMapData {
    private val _command = MutableLiveData<String>()
    val command: LiveData<String> get() = _command

    fun sendCommand(command: String) {
        _command.value = command
    }
}