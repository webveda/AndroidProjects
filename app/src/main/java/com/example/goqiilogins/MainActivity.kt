package com.example.goqiilogins

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.os.Handler
import android.os.Looper

import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val targetPackage = "com.betaout.GOQii"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val clearButton: Button = findViewById(R.id.btnClear)
        val loginButton: Button = findViewById(R.id.btnLogin)

        clearButton.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$targetPackage")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }

        loginButton.setOnClickListener {
            val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
            launchIntent?.let { startActivity(it) }

            // Delay automation start by 2 seconds
            Handler(Looper.getMainLooper()).postDelayed({
                MyAccessibilityServiceController.shouldClickSignIn = true
            }, 4000)
        }
    }
}

/** Global flag holder to talk to AccessibilityService */
object MyAccessibilityServiceController {
    var shouldClickSignIn = false
    var shouldClickBtnLogin = false
}