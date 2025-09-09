package com.example.goqiilogins

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class MainActivity : AppCompatActivity() {

    // Change this to the target app's package name
    private val targetPackage = "com.betaout.GOQii"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val clearButton: Button = findViewById(R.id.btnClear)
        val loginButton: Button = findViewById(R.id.btnLogin)

        // Test

        // Opens App Info screen
        clearButton.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:com.betaout.GOQii") // replace with target app’s package
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }

        // Launches target app
        loginButton.setOnClickListener {
            val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
            launchIntent?.let { startActivity(it) }
        }
    }
}
