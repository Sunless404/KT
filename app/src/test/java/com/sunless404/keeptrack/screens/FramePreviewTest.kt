package com.sunless404.keeptrack.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.github.takahirom.roborazzi.captureRoboImage
import com.sunless404.keeptrack.ui.cosmetics.FramedAvatar
import com.sunless404.keeptrack.ui.cosmetics.Frames
import com.sunless404.keeptrack.ui.theme.KeepTrackTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Graba los 4 bocetos de marco de cada personaje en varios instantes, para hacer GIFs. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h460dp-night-xxhdpi")
class FramePreviewTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    @Config(qualifiers = "w460dp-h330dp-night-xxhdpi")
    fun marcos() {
        var idx by androidx.compose.runtime.mutableIntStateOf(0)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            KeepTrackTheme {
                val set = Frames.all[idx]
                Column(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(set.first().collection, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        set.forEachIndexed { i, f ->
                            Column(Modifier.width(220.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                FramedAvatar(null, "K", 120.dp, f)
                                Text(
                                    "${"ABCD"[i]} · ${f.name}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                            }
                        }
                    }
                }
            }
        }
        Frames.all.forEachIndexed { i, set ->
            idx = i
            rule.mainClock.advanceTimeByFrame()
            for (f in 0 until 30) {
                rule.mainClock.advanceTimeBy(133)
                rule.onRoot().captureRoboImage("build/marcos/${set.first().id.substringBefore('_')}-$f.png")
            }
        }
    }

    @Test
    @Config(qualifiers = "w460dp-h420dp-night-xxhdpi")
    fun ilustrado() {
        var idx by androidx.compose.runtime.mutableIntStateOf(0)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            KeepTrackTheme {
                val f = com.sunless404.keeptrack.ui.cosmetics.ImageFrames.all[idx]
                Column(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(f.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        FramedAvatar(null, "K", 130.dp, f)
                        FramedAvatar(null, "K", 56.dp, f)
                    }
                }
            }
        }
        com.sunless404.keeptrack.ui.cosmetics.ImageFrames.all.forEachIndexed { i, f ->
            idx = i
            rule.mainClock.advanceTimeByFrame()
            for (k in 0 until 40) {
                rule.mainClock.advanceTimeBy(100)
                rule.onRoot().captureRoboImage("build/marcos/${f.id}-$k.png")
            }
        }
    }

    @Test
    fun llamas() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            KeepTrackTheme {
                Column(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Llama de racha", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(8.dp))
                    listOf(listOf(0, 1, 3, 7), listOf(14, 30, 60, 100)).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            row.forEach { d ->
                                Column(Modifier.width(80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    com.sunless404.keeptrack.ui.stats.StreakFlame(d, 72.dp)
                                    Text(if (d == 1) "1 día" else "$d días", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
        for (f in 0 until 30) {
            rule.mainClock.advanceTimeBy(100)
            rule.onRoot().captureRoboImage("build/marcos/llamas-$f.png")
        }
    }
}
