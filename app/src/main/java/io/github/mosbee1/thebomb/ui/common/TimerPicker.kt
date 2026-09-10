package io.github.mosbee1.thebomb.ui.common

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mosbee1.thebomb.R
import io.github.mosbee1.thebomb.data.model.DurationUnit
import io.github.mosbee1.thebomb.data.model.TimerPreset

/**
 * The full timer picker in one reusable panel: four quick fuse presets
 * (each an independently configured (amount, unit) pair), plus a custom
 * control - unit chips + amount slider with a live "Detonates in X" label -
 * and a Confirm button showing the resolved duration ("Confirm: 3 Hours").
 * Used identically inside the popup, the home action sheet, and elsewhere.
 */
sealed interface TimerSelection {
    data class Preset(val slot: Int) : TimerSelection
    data class Custom(val amount: Int, val unit: DurationUnit) : TimerSelection

    /** Resolved duration, or null when nothing is selected. */
    fun durationMillis(presets: List<TimerPreset>): Long? = when (this) {
        is Preset -> presets.firstOrNull { it.slot == slot }?.durationMillis
        is Custom -> unit.toMillis(amount)
    }
}

fun durationLabel(context: Context, amount: Int, unit: DurationUnit): String {
    val plural = when (unit) {
        DurationUnit.MINUTES -> R.plurals.duration_minutes
        DurationUnit.HOURS -> R.plurals.duration_hours
        DurationUnit.DAYS -> R.plurals.duration_days
        DurationUnit.WEEKS -> R.plurals.duration_weeks
    }
    return context.resources.getQuantityString(plural, amount, amount)
}

fun shortUnitLabel(amount: Int, unit: DurationUnit): String = when (unit) {
    DurationUnit.MINUTES -> "${amount}m"
    DurationUnit.HOURS -> "${amount}h"
    DurationUnit.DAYS -> "${amount}d"
    DurationUnit.WEEKS -> "${amount}w"
}

@Composable
fun rememberDurationLabel(amount: Int, unit: DurationUnit): String {
    val context = LocalContext.current
    return remember(context, amount, unit) { durationLabel(context, amount, unit) }
}

@Composable
fun TimerPickerPanel(
    presets: List<TimerPreset>,
    initialSelection: TimerSelection?,
    onConfirm: (TimerSelection) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf(initialSelection) }
    var customUnit by remember { mutableStateOf(DurationUnit.HOURS) }
    var customAmount by remember { mutableIntStateOf(1) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.timer_presets_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { preset ->
                val isSlotSelected = selected is TimerSelection.Preset &&
                    (selected as TimerSelection.Preset).slot == preset.slot
                FilterChip(
                    selected = isSlotSelected,
                    onClick = { selected = TimerSelection.Preset(preset.slot) },
                    label = { Text(shortUnitLabel(preset.amount, preset.unit)) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.timer_custom_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DurationUnit.entries.forEach { unit ->
                FilterChip(
                    selected = customUnit == unit,
                    onClick = {
                        customUnit = unit
                        customAmount = 1
                        selected = TimerSelection.Custom(customAmount, unit)
                    },
                    label = {
                        Text(
                            when (unit) {
                                DurationUnit.MINUTES -> stringResource(R.string.unit_minutes_short)
                                DurationUnit.HOURS -> stringResource(R.string.unit_hours_short)
                                DurationUnit.DAYS -> stringResource(R.string.unit_days_short)
                                DurationUnit.WEEKS -> stringResource(R.string.unit_weeks_short)
                            }
                        )
                    },
                )
            }
        }

        val range = customUnit.sliderRange
        Slider(
            value = customAmount.toFloat(),
            onValueChange = { raw ->
                customAmount = raw.toInt().coerceIn(range.first, range.last)
                selected = TimerSelection.Custom(customAmount, customUnit)
            },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first) - 1,
        )
        Text(
            stringResource(
                R.string.label_deletes_in,
                rememberDurationLabel(customAmount, customUnit),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(Modifier.height(16.dp))
        val confirmText = when (val sel = selected) {
            is TimerSelection.Preset -> {
                val preset = presets.firstOrNull { it.slot == sel.slot } ?: presets.first()
                stringResource(
                    R.string.timer_confirm,
                    rememberDurationLabel(preset.amount, preset.unit),
                )
            }
            is TimerSelection.Custom ->
                stringResource(
                    R.string.timer_confirm,
                    rememberDurationLabel(sel.amount, sel.unit),
                )
            null -> stringResource(R.string.timer_confirm, stringResource(R.string.timer_none))
        }
        Button(
            onClick = { selected?.let(onConfirm) },
            enabled = selected != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(confirmText)
        }
        Spacer(Modifier.height(4.dp))
    }
}
