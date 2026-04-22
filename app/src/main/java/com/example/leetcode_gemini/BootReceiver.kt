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
        // We only care about the boot completed action
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {

            // Re-register the periodic work that was lost on reboot
            val periodicRequest = PeriodicWorkRequestBuilder<UpdateWorker>(15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "lc_periodic",
                ExistingPeriodicWorkPolicy.KEEP, // KEEP prevents resetting the timer if it's already running
                periodicRequest
            )
        }
    }
}