package com.example.leetcode_gemini

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.appwidget.*
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import java.io.File

class LeetCodeWidget : GlanceAppWidget() {
    companion object {
        private val SMALL_SQUARE = DpSize(100.dp, 100.dp)
        private val HORIZONTAL_RECT = DpSize(250.dp, 100.dp)
        private val BIG_SQUARE = DpSize(250.dp, 250.dp)
    }

    override val sizeMode = SizeMode.Responsive(
        setOf(SMALL_SQUARE, HORIZONTAL_RECT, BIG_SQUARE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("leetcode_prefs", Context.MODE_PRIVATE)
        val name = prefs.getString("real_name", "User") ?: "User.."
        val daily = prefs.getString("daily_title", "Loading...") ?: "daily.."
        val streak = prefs.getInt("streak", 0)
        val dailyDone = prefs.getBoolean("daily_solved", false)
        val done = if (dailyDone) "✔" else "❌"

        val avatarFile = File(context.filesDir, "avatar.png")
        val avatarBitmap = if (avatarFile.exists()) {
            BitmapFactory.decodeFile(avatarFile.absolutePath)
        } else {
            null
        }

        provideContent {
            GlanceTheme {
                val size = LocalSize.current
                if (size.width < 120.dp) {
                    // Small Widget
                    Row(
                        GlanceModifier.fillMaxSize()
                            .background(GlanceTheme.colors.background)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
//                        Text(
//                            "🔥",
//                            style = TextStyle(
//                                color = GlanceTheme.colors.onBackground,
//                                fontSize = 18.sp
//                            )
//                        )
                        Image(
                            provider = if (dailyDone) {
                                ImageProvider(R.drawable.fire_non_animated)
                            } else {
                                ImageProvider(R.drawable.fire_non_animated_dim)
                            },
                            contentDescription = "Status",
                        )
                        Text(text = done,
                            style = TextStyle(
                                fontSize = 24.sp,
                                color = GlanceTheme.colors.onBackground
                            )
                        )
                        Text(text = "$streak",
                            style = TextStyle(
                                fontSize = 24.sp,
                                color = GlanceTheme.colors.onBackground
                            )
                        )

                    }
                } else {
                    // Medium Widget
                    Row(
                        GlanceModifier.fillMaxSize()
                            .background(GlanceTheme.colors.background)
                            .padding(12.dp)
                    ) {
                        Column(GlanceModifier.defaultWeight()) {
                            if (avatarBitmap != null) {
                                Image(
                                    provider = ImageProvider(avatarBitmap),
                                    contentDescription = "Avatar",
                                    modifier = GlanceModifier.size(40.dp).cornerRadius(20.dp)
                                )
                            } else {
                                Box(
                                    modifier = GlanceModifier.size(40.dp)
                                        .background(Color.Gray)
                                        .cornerRadius(20.dp)
                                ) {}
                            }
                            Text(
                                text = name,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onBackground,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = daily,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 18.sp
                                )
                            )
                        }
                        Image(
                            provider = if (dailyDone) {
                                ImageProvider(R.drawable.fire_non_animated)
                            } else {
                                ImageProvider(R.drawable.fire_non_animated_dim)
                            },
                            contentDescription = "Status",
                        )
                        Text(text = done,
                            style = TextStyle(
                                fontSize = 24.sp,
                                color = GlanceTheme.colors.onBackground
                            )
                        )
                        Text(text = "$streak",
                            style = TextStyle(
                                fontSize = 24.sp,
                                color = GlanceTheme.colors.onBackground
                            )
                        )
                    }
                }
            }
        }
    }
}

class LeetCodeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LeetCodeWidget()
}
