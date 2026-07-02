package com.example.leetcode_gemini

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "leetcode_daily_reminder"
        const val NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("leetcode_prefs", Context.MODE_PRIVATE)
        val dailySolved = prefs.getBoolean("daily_solved", false)

        if (!dailySolved) {
            ensureNotificationChannel(context)
            showNotification(context, prefs)
        }

        ReminderAlarmScheduler.scheduleIfEnabled(context)
    }

    private fun ensureNotificationChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily Challenge Reminder",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminds you to solve the daily LeetCode challenge"
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(context: Context, prefs: android.content.SharedPreferences) {
        val dailyTitle = prefs.getString("daily_title", "today's challenge") ?: "today's challenge"
        val problemLink = prefs.getString("daily_title", "https://leetcode.com") ?: "https://leetcode.com"
        val tapIntent = Intent(Intent.ACTION_VIEW, problemLink.toUri()).apply {
            // Flags to ensure smooth browser launching behavior
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("LeetCode Daily Challenge")
            .setContentText("You haven't solved \"$dailyTitle\" yet!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("You haven't solved \"$dailyTitle\" yet! Open LeetCode and keep your streak alive.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }
}
