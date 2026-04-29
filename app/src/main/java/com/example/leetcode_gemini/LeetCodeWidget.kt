package com.example.leetcode_gemini

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.io.File

// Text colors — day/night resolved via resource qualifiers (values/ vs values-night/)
@SuppressLint("RestrictedApi")
private val textPrimary = ColorProvider(R.color.widget_text_primary)
@SuppressLint("RestrictedApi")
private val textSecondary = ColorProvider(R.color.widget_text_secondary)
@SuppressLint("RestrictedApi")
private val textLabel = ColorProvider(R.color.widget_text_label)
@SuppressLint("RestrictedApi")
private val streakActive = ColorProvider(R.color.widget_streak_active)
@SuppressLint("RestrictedApi")
private val streakInactive = ColorProvider(R.color.widget_streak_inactive)

class RefreshActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // 1. Manually trigger a one-time sync immediately
        val workRequest = OneTimeWorkRequestBuilder<UpdateWorker>().build()
        WorkManager.getInstance(context).enqueue(workRequest)

        // Note: The widget will update itself once the UpdateWorker
        // calls LeetCodeWidget().updateAll(context) at the end of its task.
    }
}

class LeetCodeWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("leetcode_prefs", Context.MODE_PRIVATE)
        val name = prefs.getString("real_name", "User") ?: "User"
        val daily = prefs.getString("daily_title", "Loading...") ?: "Loading..."
        val streak = prefs.getInt("streak", 0)
        val dailyDone = prefs.getBoolean("daily_solved", false)

        val avatarFile = File(context.filesDir, "avatar.png")
        val avatarBitmap = if (avatarFile.exists()) {
            BitmapFactory.decodeFile(avatarFile.absolutePath)
        } else {
            null
        }

        val bgDrawable = if (dailyDone) R.drawable.widget_bg_completed else R.drawable.widget_bg_pending
        val flameIcon = if (dailyDone) R.drawable.fire3 else R.drawable.fire_non_animated_dim
        val flameDesc = if (dailyDone) "Daily completed" else "Daily pending"

        provideContent {
            val size = LocalSize.current
            Box(modifier = GlanceModifier.fillMaxSize()
                .clickable(actionRunCallback<RefreshActionCallback>())
            ) {
                if (size.width >= 250.dp && size.height >= 250.dp) {
                    LargeLayout(bgDrawable, avatarBitmap, name, daily, flameIcon, flameDesc, streak, dailyDone)
                } else {
                    CompactLayout(size, bgDrawable, avatarBitmap, name, daily, flameIcon, flameDesc, streak, dailyDone)
                }
                Box(
                    modifier = GlanceModifier.fillMaxSize().padding(bottom = 2.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = "Tap to refresh",
                        style = TextStyle(
                            fontSize = 8.sp,
                            color = textLabel
                        )
                    )
                }
            }
        }
    }
}
@Composable
private fun CompactLayout(
    size: DpSize,
    bgDrawable: Int,
    avatarBitmap: android.graphics.Bitmap?,
    name: String,
    daily: String,
    flameIcon: Int,
    flameDesc: String,
    streak: Int,
    dailyDone: Boolean
) {
    val wide = size.width >= 200.dp
    val tall = size.height >= 130.dp
    val iconSize = if (tall) 48.dp else 40.dp
    val iconRadius = iconSize / 2
    val pad = if (wide) 12.dp else 8.dp

    Row(
        modifier = GlanceModifier.fillMaxSize()
            .background(ImageProvider(bgDrawable))
            .padding(pad),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = if (!wide) GlanceModifier.defaultWeight() else GlanceModifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (avatarBitmap != null) {
                Image(
                    provider = ImageProvider(avatarBitmap),
                    contentDescription = "Avatar",
                    modifier = GlanceModifier.size(iconSize).cornerRadius(iconRadius)
                )
            } else {
                Box(
                    modifier = GlanceModifier.size(iconSize)
                        .background(Color(0xFF9E9E9E))
                        .cornerRadius(iconRadius)
                ) {}
            }
            if (tall) {
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = name,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    ),
                    maxLines = 1
                )
            } else {
                Spacer(GlanceModifier.height(0.dp))
            }
        }

        if (wide) {
            Spacer(GlanceModifier.width(10.dp))
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.Start
            ) {
                if (!tall) {
                    Text(
                        text = name,
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        ),
                        maxLines = 1
                    )
                } else {
                    Spacer(GlanceModifier.height(0.dp))
                }
                Text(
                    text = daily,
                    style = TextStyle(
                        fontSize = if (tall) 16.sp else 11.sp,
                        fontWeight = if (tall) FontWeight.Medium else FontWeight.Normal,
                        color = textSecondary
                    ),
                    maxLines = if (tall) 2 else 1
                )
            }
            Spacer(GlanceModifier.width(10.dp))
        } else {
            Spacer(GlanceModifier.width(8.dp))
        }

        Column(
            modifier = if (!wide) GlanceModifier.defaultWeight() else GlanceModifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                provider = ImageProvider(flameIcon),
                contentDescription = flameDesc,
                modifier = GlanceModifier.size(iconSize+10.dp)
            )
            if (tall) {
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = "$streak",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (dailyDone) streakActive else streakInactive
                    )
                )
            } else {
                Spacer(GlanceModifier.height(0.dp))
            }
        }
    }
}

@Composable
private fun LargeLayout(
    bgDrawable: Int,
    avatarBitmap: android.graphics.Bitmap?,
    name: String,
    daily: String,
    flameIcon: Int,
    flameDesc: String,
    streak: Int,
    dailyDone: Boolean
) {
    Column(
        modifier = GlanceModifier.fillMaxSize()
            .background(ImageProvider(bgDrawable))
            .padding(16.dp)
    ) {
        // Top: Avatar + Name
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (avatarBitmap != null) {
                Image(
                    provider = ImageProvider(avatarBitmap),
                    contentDescription = "Avatar",
                    modifier = GlanceModifier.size(56.dp).cornerRadius(28.dp)
                )
            } else {
                Box(
                    modifier = GlanceModifier.size(56.dp)
                        .background(Color(0xFF9E9E9E))
                        .cornerRadius(28.dp)
                ) {}
            }
            Spacer(GlanceModifier.width(12.dp))
            Text(
                text = name,
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                ),
                maxLines = 2
            )
        }

        Spacer(GlanceModifier.height(12.dp))

        // Daily challenge
        Text(
            text = "Today's Challenge",
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = textLabel
            )
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = daily,
            style = TextStyle(
                fontSize = 14.sp,
                color = textSecondary
            ),
            maxLines = 2
        )

        Spacer(GlanceModifier.height(8.dp))
        Row() {
            Text(
                text = "$streak",
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (dailyDone) streakActive else streakInactive
                )
            )
            Text(
                text = "  current streak",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (dailyDone) streakActive else streakInactive
                )
            )
        }
        Image(
            provider = ImageProvider(flameIcon),
            contentDescription = flameDesc,
            modifier = GlanceModifier.size(300.dp)
        )

        // Bottom: Flame + Streak centered
//        Column(
//            modifier = GlanceModifier.fillMaxWidth(),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Text(
//                text = "$streak",
//                style = TextStyle(
//                    fontSize = 20.sp,
//                    fontWeight = FontWeight.Bold,
//                    color = if (dailyDone) streakActive else streakInactive
//                )
//            )
//            Image(
//                provider = ImageProvider(flameIcon),
//                contentDescription = flameDesc,
//                modifier = GlanceModifier.size(300.dp)
//            )
//        }
    }
}

class LeetCodeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LeetCodeWidget()
}
