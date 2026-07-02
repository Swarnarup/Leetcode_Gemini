package com.example.leetcode_gemini

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.asFlow
import androidx.work.*
import java.util.concurrent.TimeUnit
import java.util.Calendar


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("leetcode_prefs", Context.MODE_PRIVATE)
        setContent {
            var username by remember { mutableStateOf("") }
            var samplingGapText by remember { mutableStateOf("") }
            var storedUsername by remember { mutableStateOf(prefs.getString("username", null)) }
            var storedGapHours by remember { mutableFloatStateOf(prefs.getFloat("sampling_gap_hours", 1f)) }

            var reminderEnabled by remember { mutableStateOf(prefs.getBoolean("reminder_enabled", false)) }
            var reminderHour by remember { mutableIntStateOf(prefs.getInt("reminder_hour", 23)) }
            var reminderMinute by remember { mutableIntStateOf(prefs.getInt("reminder_minute", 0)) }
            var showDialog by remember { mutableStateOf(false) }

            var hasNotificationPermission by remember {
                mutableStateOf(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    } else {
                        true
                    }
                )
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                hasNotificationPermission = isGranted
                if (isGranted) {
                    enableAndScheduleReminder(prefs, reminderHour, reminderMinute)
                    reminderEnabled = true
                }
            }

            val workInfo by WorkManager.getInstance(this@MainActivity)
                .getWorkInfosForUniqueWorkLiveData("lc_sync")
                .asFlow()
                .collectAsState(initial = emptyList())

            val currentWork = workInfo.firstOrNull()

            val workInfo2 by WorkManager.getInstance(this@MainActivity)
                .getWorkInfosForUniqueWorkLiveData("lc_immediate_sync")
                .asFlow()
                .collectAsState(initial = emptyList())

            val immediateWork = workInfo2.firstOrNull()

// This variable will update the UI automatically
            val statusText = when (immediateWork?.state) {
                WorkInfo.State.RUNNING -> "🔄 Syncing with LeetCode..."
                WorkInfo.State.SUCCEEDED -> immediateWork.outputData.getString("json_result") ?: "✅ Sync Complete"
                WorkInfo.State.FAILED -> immediateWork.outputData.getString("json_result") ?: "❌ Sync Failed"
                WorkInfo.State.ENQUEUED -> "⏳ Scheduled/Waiting..."
                else -> "Registry Empty"
            }
            val scrSTATE = rememberScrollState()
            Column(Modifier.padding(20.dp).scrollable(scrSTATE, orientation=androidx.compose.foundation.gestures.Orientation.Vertical)) {
                TextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("LeetCode Username") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (storedUsername != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Current: $storedUsername",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(16.dp))

                TextField(
                    value = samplingGapText,
                    onValueChange = { samplingGapText = it },
                    label = { Text("Sampling Gap (hours)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Current: ${"%.1f".format(storedGapHours)} hr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (username.isNotBlank()) {
                            prefs.edit { putString("username", username) }
                            storedUsername = username
                        }

                        val gapHours = samplingGapText.toFloatOrNull()
                        if (gapHours != null && gapHours > 0f) {
                            prefs.edit { putFloat("sampling_gap_hours", gapHours) }
                            storedGapHours = gapHours
                        }

                        val intervalMinutes = (storedGapHours * 60).toLong().coerceAtLeast(15)

                        val workManager = WorkManager.getInstance(this@MainActivity)

                        // 1. THE IMMEDIATE WORK (Runs right now)
                        val immediateWork = OneTimeWorkRequestBuilder<UpdateWorker>()
                            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                            .build()

                        // 2. THE PERIODIC WORK (Runs every hour)
                        val periodicWork = PeriodicWorkRequestBuilder<UpdateWorker>(
                            intervalMinutes,
                            TimeUnit.MINUTES)
                            .build()

                        // Execute both
                        workManager.enqueueUniqueWork(
                            "lc_immediate_sync",
                            ExistingWorkPolicy.REPLACE, // REPLACE is key to resetting the "Scheduled" status
                            immediateWork
                        )
                        workManager.enqueueUniquePeriodicWork(
                            "lc_sync",
                            ExistingPeriodicWorkPolicy.KEEP,
                            periodicWork
                        )
                    }
                ) {
                    Text("Sync Data Now")
                }

                Spacer(Modifier.height(20.dp))

                // This is the line that shows the result
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyLarge,
                )

                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Daily Reminder",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enable reminder notification")
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !hasNotificationPermission
                                ) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    enableAndScheduleReminder(prefs, reminderHour, reminderMinute)
                                    reminderEnabled = true
                                }
                            } else {
                                prefs.edit { putBoolean("reminder_enabled", false) }
                                ReminderAlarmScheduler.cancel(this@MainActivity)
                                reminderEnabled = false
                            }
                        }
                    )
                }

//                Spacer(Modifier.height(8.dp))
//
//                Row(modifier = Modifier.fillMaxWidth()) {
//                    TextField(
//                        value = reminderHourText,
//                        onValueChange = { reminderHourText = it },
//                        label = { Text("Hour (0-23)") },
//                        modifier = Modifier.weight(1f)
//                    )
//                    Spacer(Modifier.width(8.dp))
//                    TextField(
//                        value = reminderMinuteText,
//                        onValueChange = { reminderMinuteText = it },
//                        label = { Text("Minute (0-59)") },
//                        modifier = Modifier.weight(1f)
//                    )
//                }

                Spacer(Modifier.height(12.dp))
                Column() {
                    Text(
                        text = "Current: %02d:%02d".format(
                            prefs.getInt("reminder_hour", 23),
                            prefs.getInt("reminder_minute", 0)
                        ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = reminderEnabled,
                        onClick = {
//                        val hour = reminderHourText.toIntOrNull()?.coerceIn(0, 23)
//                        val minute = reminderMinuteText.toIntOrNull()?.coerceIn(0, 59)
//                        if (hour != null && minute != null) {
//                            prefs.edit {
//                                putInt("reminder_hour", hour)
//                                putInt("reminder_minute", minute)
//                            }
//                            reminderHourText = hour.toString()
//                            reminderMinuteText = minute.toString().padStart(2, '0')
//                            ReminderAlarmScheduler.schedule(this@MainActivity, hour, minute)
//                        }
                            showDialog = true;
                        }
                    ) {
                        Text("Update Reminder Time")
                    }
                }

                if (showDialog) {
                    AlarmTimePicker(
                        onDismiss = {
                            showDialog = false;
                        },
                        onTimeSelected = { hour, minute ->
                            prefs.edit {
                                putInt("reminder_hour", hour)
                                putInt("reminder_minute", minute)
                            }
                            reminderHour = hour
                            reminderMinute = minute
                            ReminderAlarmScheduler.schedule(this@MainActivity, hour, minute)
                            showDialog = false;
                        }
                    )
                }
            }
        }
    }


    private fun createNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(ReminderReceiver.CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                ReminderReceiver.CHANNEL_ID,
                "Daily Challenge Reminder",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminds you to solve the daily LeetCode challenge"
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun enableAndScheduleReminder(
        prefs: android.content.SharedPreferences,
        hour: Int,
        minute: Int
    ) {
        prefs.edit {
            putBoolean("reminder_enabled", true)
            putInt("reminder_hour", hour)
            putInt("reminder_minute", minute)
        }
        ReminderAlarmScheduler.schedule(this, hour, minute)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmTimePicker(
    onDismiss: () -> Unit,
    onTimeSelected: (hour: Int, minute: Int) -> Unit
) {
    // Get current time to set as default
    val calendar = Calendar.getInstance()
    val timePickerState = rememberTimePickerState (
        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar.get(Calendar.MINUTE),
        is24Hour = true // Set to true for 24-hour format
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        confirmButton = {
            TextButton(onClick = {
                onTimeSelected(timePickerState.hour, timePickerState.minute)
                onDismiss()
            }) {
                Text("OK")
            }
        },
        text = {
            // This displays the beautiful Material 3 Clock Dial
            TimePicker(state = timePickerState)
        }
    )
}
