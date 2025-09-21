package com.wakeup.esmoglogger

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ApiForegroundService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, "api_channel")
            .setContentTitle("API Service")
            .setContentText("Fetching data from server")
            .setSmallIcon(R.drawable.cloud_24p) // Replace with your icon
            .build()

        startForeground(1, notification)

        // Start periodic API calls
        scope.launch {
            while (true) {
                try {
                    val apiService = ApiClient.create(applicationContext)
                    val user = apiService.getUser("123")
                    // Process result (e.g., save to database)
                    Log.d("ForegroundService", "User: $user")
                } catch (e: Exception) {
                    Log.e("ForegroundService", "Error: ${e.message}")
                }
                delay(60000) // Poll every 60 seconds
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "api_channel",
                "API Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
