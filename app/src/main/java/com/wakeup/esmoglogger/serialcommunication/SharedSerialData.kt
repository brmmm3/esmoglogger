package com.wakeup.esmoglogger.serialcommunication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.wakeup.esmoglogger.data.ESmog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset

object SharedSerialData {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _command = MutableLiveData<Boolean>()
    val command: LiveData<Boolean> get() = _command

    private val _esmog = MutableSharedFlow<ESmog>(
        replay = 0, // No replay for new subscribers
        extraBufferCapacity = 1000, // Large buffer to handle high-frequency data
        onBufferOverflow = BufferOverflow.SUSPEND // Suspend emitter if buffer is full
    )
    val esmog = _esmog.asSharedFlow()

    fun start() {
        _command.value = true
    }

    fun stop() {
        _command.value = false
    }

    fun addESmog(level: Float, frequency: Int) {
        scope.launch {
            _esmog.emit(ESmog(LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli(), level, frequency))
        }
    }
}