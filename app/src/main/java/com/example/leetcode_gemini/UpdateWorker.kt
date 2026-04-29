package com.example.leetcode_gemini

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.gson.Gson
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.glance.appwidget.state.updateAppWidgetState
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult

class UpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("leetcode_prefs", Context.MODE_PRIVATE)
        val username = prefs.getString("username", null) ?: return Result.failure()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://leetcode.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(LeetCodeApiService::class.java)

        val queryStr = """
            query getWidgetData(${"$"}username: String!) {
              activeDailyCodingChallengeQuestion {
                  question {
                    title
                    titleSlug
                  }
              }
              matchedUser(username: ${"$"}username) {
                profile { userAvatar realName }
                submitStats { acSubmissionNum { difficulty count } }
              }
              recentAcSubmissionList(username: ${'$'}username, limit: 5) {
                titleSlug
              }
              streakCounter(username: ${'$'}username) {
                streakCount
                daysSkipped
                currentDayCompleted
              }
            }
        """.trimIndent()

        return try {
            val response = api.getData(GraphQLQuery(queryStr, mapOf("username" to username)))
            val user = response.data.matchedUser
            val jsonResponse = Gson().toJson(response)

            prefs.edit().apply {
                putString("real_name", user?.profile?.realName ?: "Unknown")
                putString("avatar_url", user?.profile?.userAvatar)
                putString("daily_title", response.data.activeDailyCodingChallengeQuestion?.question?.title ?: "No Challenge")
                putString("raw_json", jsonResponse)
                // Simplified "streak" as total solved for this example
                putInt("totalSubmission", user?.submitStats?.acSubmissionNum?.firstOrNull { it.difficulty == "All" }?.count ?: 0)
                putInt("streak",response.data.streakCounter?.streakCount ?: 0)
//                val dailySlug = response.data.activeDailyCodingChallengeQuestion?.question?.titleSlug
//                val isSolved = response.data.recentAcSubmissionList.any { it.titleSlug == dailySlug }
//
//                if (isSolved) {
//                    putBoolean("daily_solved", true)
//                } else {
//                    putBoolean("daily_solved", false)
//                }
                putBoolean("daily_solved", response.data.streakCounter?.currentDayCompleted ?: false)
                apply()
            }
            val avatarUrl = user?.profile?.userAvatar ?: ""
            if (avatarUrl.isNotEmpty()) {
                val loader = ImageLoader(applicationContext)
                val request = ImageRequest.Builder(applicationContext)
                    .data(avatarUrl)
                    .allowHardware(false)
                    .build()

                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as BitmapDrawable).bitmap

                    // Save the bitmap directly to a file
                    applicationContext.openFileOutput("avatar.png", Context.MODE_PRIVATE).use { fos ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    }
                }
            }

            LeetCodeWidget().updateAll(applicationContext)
            Result.success(workDataOf("json_result" to jsonResponse))
        } catch (e: Exception) {
            // FAILURE: Return the error to the UI instead of silent retrying
            Result.failure(workDataOf("json_result" to "Error: ${e.localizedMessage}"))
        }
    }
}
