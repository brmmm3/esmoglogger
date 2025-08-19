package com.wakeup.esmoglogger.ui.chartview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class ChartViewModel(private val state: SavedStateHandle) : ViewModel() {
    companion object {
        private const val KEY_COUNTER = "counter"
    }

    var counter: Int
        get() = state.get(KEY_COUNTER) ?: 0
        set(value) = state.set(KEY_COUNTER, value)
}