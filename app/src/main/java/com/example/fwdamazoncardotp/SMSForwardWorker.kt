package com.example.fwdamazoncardotp

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class SMSForwardWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val messageBody = inputData.getString("message") ?: return Result.failure()

        return try {
            val forwarder = SMSForwarder(applicationContext)
            forwarder.sendSMS(messageBody)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}
