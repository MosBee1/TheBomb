package io.github.mosbee1.thebomb.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.mosbee1.thebomb.util.DurationFormatter
import kotlinx.coroutines.delay

/** A ticking "now" clock shared by every countdown chip and banner (30s tick). */
@Composable
fun rememberNowTicker(periodMillis: Long = 30_000L): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(periodMillis) {
        while (true) {
            now = System.currentTimeMillis()
            delay(periodMillis)
        }
    }
    return now
}

/** Live "Detonates in 2h 5m" text for a fuse that fires at [targetMillis]. */
@Composable
fun rememberCountdownText(targetMillis: Long): String {
    val now = rememberNowTicker()
    return remember(targetMillis, now) {
        DurationFormatter.remainingFor(targetMillis, now)
    }
}
