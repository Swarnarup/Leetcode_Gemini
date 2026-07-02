package com.example.leetcode_gemini

import retrofit2.http.Body
import retrofit2.http.POST

// API Models
data class LeetCodeResponse(val data: DataContent)
data class DataContent(
    val activeDailyCodingChallengeQuestion: DailyQuestion?,
    val matchedUser: UserProfile?,
    val recentAcSubmissionList: List<SlugData>,
    val streakCounter: StreakData?
)
data class DailyQuestion(val link: String, val question: QuestionDetails)
data class QuestionDetails(val title: String, val titleSlug: String)
data class UserProfile(val profile: ProfileDetails, val submitStats: Stats)
data class ProfileDetails(val userAvatar: String, val realName: String)
data class Stats(val acSubmissionNum: List<Submission>)
data class Submission(val difficulty: String, val count: Int)
data class SlugData(val titleSlug: String)
data class StreakData(val streakCount: Int, val daysSkipped: Int, val currentDayCompleted: Boolean)
// GraphQL Query structure
data class GraphQLQuery(val query: String, val variables: Map<String, String>)

interface LeetCodeApiService {
    @POST("graphql")
    suspend fun getData(@Body body: GraphQLQuery): LeetCodeResponse
}