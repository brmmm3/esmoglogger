package com.wakeup.esmoglogger.ui.chartview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.wakeup.esmoglogger.data.ESmog

object SharedChartData {
    private val _data = MutableLiveData<ESmog>()
    val data: LiveData<ESmog> get() = _data

    fun add(esmog: ESmog) {
        _data.value = esmog
    }
}