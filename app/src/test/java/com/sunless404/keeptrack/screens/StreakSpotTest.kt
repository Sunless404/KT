package com.sunless404.keeptrack.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.sunless404.keeptrack.domain.Habit
import com.sunless404.keeptrack.domain.Streak
import com.sunless404.keeptrack.ui.stats.FlameSpot
import com.sunless404.keeptrack.ui.stats.StreakCard
import com.sunless404.keeptrack.ui.theme.KeepTrackTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Bocetos: la tarjeta de racha con la llama en distintos sitios. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h400dp-night-xxhdpi")
class StreakSpotTest {
    @get:Rule
    val rule = createComposeRule()

    private val streaks = listOf(
        Streak(Habit.ESTUDIO, 16, 21), Streak(Habit.GIMNASIO, 4, 9),
        Streak(Habit.LECTURA, 0, 5), Streak(Habit.SUENO, 103, 103),
    )
    private val names = mapOf(
        FlameSpot.ACTUAL to "Actual", FlameSpot.ESQUINA to "1 · Esquina, pequeña",
        FlameSpot.ESQUINA_GRANDE to "2 · Esquina, mediana", FlameSpot.JUNTO to "3 · Junto al número",
        FlameSpot.DETRAS to "4 · Detrás de «días»",
    )

    @Test
    fun bocetos() {
        var idx by mutableIntStateOf(0)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            KeepTrackTheme {
                val spot = FlameSpot.entries[idx]
                Column(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(names.getValue(spot), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    streaks.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            pair.forEach { StreakCard(it, true, 0, null, Modifier.weight(1f), spot) }
                        }
                    }
                }
            }
        }
        FlameSpot.entries.forEachIndexed { i, spot ->
            idx = i
            rule.mainClock.advanceTimeBy(1500)
            for (f in 0 until 20) {
                rule.mainClock.advanceTimeBy(100)
                rule.onRoot().captureRoboImage("build/marcos/racha-${spot.name.lowercase()}-$f.png")
            }
        }
    }
}
