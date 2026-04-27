package com.example.leetcode_gemini

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.appwidget.*
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
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

class LeetCodeWidget : GlanceAppWidget() {
    companion object {
        private val SMALL_SQUARE = DpSize(100.dp, 100.dp)
        private val HORIZONTAL_RECT = DpSize(250.dp, 100.dp)
        private val BIG_SQUARE = DpSize(250.dp, 250.dp)
    }

    override val sizeMode = SizeMode.Responsive(
        setOf(SMALL_SQUARE, HORIZONTAL_RECT, BIG_SQUARE)
    )

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
        val flameIcon = if (dailyDone) R.drawable.new_fire else R.drawable.fire_non_animated_dim
        val flameDesc = if (dailyDone) "Daily completed" else "Daily pending"

        provideContent {
            val size = LocalSize.current
            when {
                size.width < 80.dp -> ExtraSmallLayout(bgDrawable, avatarBitmap, flameIcon, flameDesc)
                size.width < 120.dp -> SmallLayout(bgDrawable, avatarBitmap, flameIcon, flameDesc)
                size.height < 200.dp -> MediumLayout(bgDrawable, avatarBitmap, name, flameIcon, flameDesc, streak, dailyDone)
                else -> LargeLayout(bgDrawable, avatarBitmap, name, daily, flameIcon, flameDesc, streak, dailyDone)
            }
        }
    }
}
@Composable
private fun ExtraSmallLayout(
    bgDrawable: Int,
    avatarBitmap: android.graphics.Bitmap?,
    flameIcon: Int,
    flameDesc: String
) {
    Box(
        modifier = GlanceModifier.fillMaxSize()
            .padding(horizontal = 2.dp)
            .background(ImageProvider(bgDrawable)),
        contentAlignment = Alignment.Center
    ) {
        Row(
//            modifier = GlanceModifier.padding(1.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (avatarBitmap != null) {
                Image(
                    provider = ImageProvider(avatarBitmap),
                    contentDescription = "Avatar",
                    modifier = GlanceModifier.size(40.dp).cornerRadius(24.dp)
                )
            } else {
                Box(
                    modifier = GlanceModifier.size(48.dp)
                        .background(Color(0xFF9E9E9E))
                        .cornerRadius(24.dp)
                ) {}
            }
            Spacer(GlanceModifier.width(2.dp))
            Image(
                provider = ImageProvider(flameIcon),
                contentDescription = flameDesc,
                modifier = GlanceModifier.size(60.dp)
            )
        }
    }
}
@Composable
private fun SmallLayout(
    bgDrawable: Int,
    avatarBitmap: android.graphics.Bitmap?,
    flameIcon: Int,
    flameDesc: String
) {
    Row(
        modifier = GlanceModifier.fillMaxSize()
        .padding(horizontal = 5.dp)
        .background(ImageProvider(bgDrawable)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = GlanceModifier.defaultWeight().padding(6.dp),
            contentAlignment = Alignment.Center) {
            if (avatarBitmap != null) {
                Image(
                    provider = ImageProvider(avatarBitmap),
                    contentDescription = "Avatar",
//                    modifier = GlanceModifier.size(60.dp).cornerRadius(30.dp)
                    modifier = GlanceModifier.fillMaxSize().cornerRadius((minOf(LocalSize.current.height, LocalSize.current.height))/2)
                )
            } else {
                Box(
                    modifier = GlanceModifier.fillMaxSize()
                        .background(Color(0xFF9E9E9E))
                        .cornerRadius(minOf(LocalSize.current.height, LocalSize.current.height)/2)
                ) {}
            }
        }
        Box(modifier = GlanceModifier.defaultWeight().padding(4.dp),
            contentAlignment = Alignment.Center)
        {
            Image(
                provider = ImageProvider(flameIcon),
                contentDescription = flameDesc,
//                modifier = GlanceModifier.size(80.dp)
                modifier = GlanceModifier.fillMaxSize()
            )
        }
    }
}
@Composable
private fun MediumLayout(
    bgDrawable: Int,
    avatarBitmap: android.graphics.Bitmap?,
    name: String,
    flameIcon: Int,
    flameDesc: String,
    streak: Int,
    dailyDone: Boolean
) {
    Row(
        modifier = GlanceModifier.fillMaxSize()
            .background(ImageProvider(bgDrawable)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = GlanceModifier.defaultWeight()
        ) {
            if (avatarBitmap != null) {
                Image(
                    provider = ImageProvider(avatarBitmap),
                    contentDescription = "Avatar",
                    modifier = GlanceModifier.size(48.dp).cornerRadius(24.dp)
                )
            } else {
                Box(
                    modifier = GlanceModifier.size(48.dp)
                        .background(Color(0xFF9E9E9E))
                        .cornerRadius(24.dp)
                ) {}
            }
            Spacer(GlanceModifier.height(3.dp))
            Text(
                text = name,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                ),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight()
            )
        }
        Spacer(GlanceModifier.defaultWeight())
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                provider = ImageProvider(flameIcon),
                contentDescription = flameDesc,
                modifier = GlanceModifier.size(130.dp)
            )
            Spacer(GlanceModifier.height(1.dp))
            Text(
                text = "$streak",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (dailyDone) streakActive else streakInactive
                )
            )
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
        Text(
            text = "$streak",
            style = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (dailyDone) streakActive else streakInactive
            )
        )
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
