package com.example.fwdamazoncardotp

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class SMSForwardService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Show notification when service is started
        val notificationHelper = NotificationHelper(this)
        val notification = notificationHelper.getNotification().build()

        // This ensures the service runs in the foreground
        startForeground(1, notification)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
