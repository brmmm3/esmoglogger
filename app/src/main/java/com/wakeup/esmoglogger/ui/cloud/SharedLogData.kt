package com.wakeup.esmoglogger.ui.cloud

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SharedLogData {
    private val _data = MutableLiveData<List<LogEntry>>(emptyList())
    val data: LiveData<List<LogEntry>> get() = _data

    fun addLog(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val currentLogs = _data.value ?: emptyList()
        _data.postValue(currentLogs + LogEntry(timestamp, message))
    }
}