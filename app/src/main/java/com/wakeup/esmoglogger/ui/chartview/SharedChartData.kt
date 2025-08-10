package com.wakeup.esmoglogger.ui.chartview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object SharedChartData {
    private val _data = MutableLiveData<Pair<Float, Pair<Float, Int>>>()
    val data: LiveData<Pair<Float, Pair<Float, Int>>> get() = _data

    private val _command = MutableLiveData<String>()
    val command: LiveData<String> get() = _command

    private val _view = MutableLiveData<String>()
    val view: LiveData<String> get() = _view

    fun add(data: Pair<Float, Pair<Float, Int>>) {
        _data.value = data
    }

    fun sendCommand(command: String) {
        _command.value = command
    }

    fun setView(view: String) {
        _view.value = view
    }
}