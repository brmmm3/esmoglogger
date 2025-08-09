package com.wakeup.esmoglogger.ui.chartview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object SharedChartData {
    private val _data = MutableLiveData<Pair<Float, Pair<Float, Int>>>()
    val data: LiveData<Pair<Float, Pair<Float, Int>>> get() = _data

    fun addData(data: Pair<Float, Pair<Float, Int>>) {
        _data.value = data
    }
}