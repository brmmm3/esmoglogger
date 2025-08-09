package com.wakeup.esmoglogger.ui.chartview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object SharedChartData {
    private val _data = MutableLiveData<Pair<Float, Float>>()
    val data: LiveData<Pair<Float, Float>> get() = _data

    fun addData(data: Pair<Float, Float>) {
        _data.value = data
    }
}