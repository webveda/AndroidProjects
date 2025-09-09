package com.example.fwdamazoncardotp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.widget.Toast
import java.util.Locale
import java.util.regex.Pattern

class SMSReceiverOld : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "android.provider.Telephony.SMS_RECEIVED") {
            val bundle = intent.extras
            val pdus = bundle?.get("pdus") as? Array<*>
            if (pdus != null) {
                for (pdu in pdus) {
                    // Handling SMS depending on the SDK version
                    val smsMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        SmsMessage.createFromPdu(pdu as ByteArray, bundle.getString("format"))
                    } else {
                        SmsMessage.createFromPdu(pdu as ByteArray)
                    }

                    val messageBody = smsMessage.messageBody
                    val sender = smsMessage.originatingAddress

                    // Convert message body to lower case for case-insensitive comparison
                    val lowerCaseMessageBody = messageBody.lowercase(Locale.ROOT)

                    // Check for strings "at AMAZON" and "XX1007"
                    if (lowerCaseMessageBody.contains("at amazon") &&
                        lowerCaseMessageBody.contains("xx1007")) {

                        val (code, amount) = extractCodeAndAmount(messageBody)
                        val codemsg = "One Time Code for Amazon Payment of $amount is $code"
                        if (code != null) {
                            forwardMessage(context, sender, codemsg)
                        }
                    }
                }
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
        val smsManager = SmsManager.getDefault()
        smsManager.sendTextMessage(forwardNumber, null, "Message from $sender: $codemsg", null, null)

        Toast.makeText(context, "OTP message forwarded: $codemsg", Toast.LENGTH_SHORT).show()
    }
}
