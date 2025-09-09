package com.example.fwdamazoncardotp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsMessage
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

class SMSReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SMS_RECEIVED") return

        val bundle: Bundle? = intent.extras
        val pdus = bundle?.get("pdus") as? Array<*>

        pdus?.forEach { pdu ->
            val sms = SmsMessage.createFromPdu(pdu as ByteArray)
            val messageBody = sms.messageBody ?: return
            val sender = sms.originatingAddress ?: ""

            // ✅ Filter only Amazon OTP messages for card ending XXXX
            if (sender.contains("AMAZON", ignoreCase = true) &&
                messageBody.contains("1007") &&
                messageBody.contains("OTP", ignoreCase = true)) {

                val work = OneTimeWorkRequestBuilder<SMSForwardWorker>()
                    .setInputData(workDataOf("message" to messageBody))
                    .build()

                WorkManager.getInstance(context).enqueue(work)
            }
        }
    }
}
