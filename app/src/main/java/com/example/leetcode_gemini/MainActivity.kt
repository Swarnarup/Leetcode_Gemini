package com.example.leetcode_gemini

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.asFlow
import androidx.work.*
import java.util.concurrent.TimeUnit
import androidx.core.content.edit


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var username by remember { mutableStateOf("") }

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

            Column(Modifier.padding(20.dp)) {
                TextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("LeetCode Username") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val prefs = getSharedPreferences("leetcode_prefs", Context.MODE_PRIVATE)
                        prefs.edit { putString("username", username) }

                        val workManager = WorkManager.getInstance(this@MainActivity)

                        // 1. THE IMMEDIATE WORK (Runs right now)
                        val immediateWork = OneTimeWorkRequestBuilder<UpdateWorker>()
                            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                            .build()

                        // 2. THE PERIODIC WORK (Runs every hour)
                        val periodicWork = PeriodicWorkRequestBuilder<UpdateWorker>(1, TimeUnit.MINUTES)
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
            }
        }
    }
}

//@Composable
//fun Greeting(name: String, modifier: Modifier = Modifier) {
//    Text(
//        text = "Hello $name!",
//        modifier = modifier
//    )
//}
//
//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    Leetcode_GeminiTheme {
//        Greeting("Android")
//    }
//}