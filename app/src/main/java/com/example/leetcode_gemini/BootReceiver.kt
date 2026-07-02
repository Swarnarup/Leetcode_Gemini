package com.example.leetcode_gemini

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON") {
            val prefs = context.getSharedPreferences("leetcode_prefs", Context.MODE_PRIVATE)
            val gapHours = prefs.getFloat("sampling_gap_hours", 1f)
            val intervalMinutes = (gapHours * 60).toLong().coerceAtLeast(15)

            val periodicRequest = PeriodicWorkRequestBuilder<UpdateWorker>(intervalMinutes, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "lc_periodic",
                ExistingPeriodicWorkPolicy.KEEP, // KEEP prevents resetting the timer if it's already running
                periodicRequest
            )
        }

        ReminderAlarmScheduler.scheduleIfEnabled(context)
    }
}