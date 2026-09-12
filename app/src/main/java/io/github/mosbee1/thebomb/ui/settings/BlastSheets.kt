package io.github.mosbee1.thebomb.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.mosbee1.thebomb.R
import io.github.mosbee1.thebomb.data.model.DurationUnit
import io.github.mosbee1.thebomb.ui.common.PillButton
import io.github.mosbee1.thebomb.ui.common.SegmentedRow
import io.github.mosbee1.thebomb.ui.common.rememberDurationLabel

/** Blast time editor: TimePicker plus quick-time segments, big Save pill. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlastTimeSheet(
    currentMinutes: Int,
    onSave: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val timeState = rememberTimePickerState(
        initialHour = currentMinutes / 60,
        initialMinute = currentMinutes % 60,
        is24Hour = true,
    )
    val quick = listOf(
        "22:00" to 22 * 60,
        "23:30" to 23 * 60 + 30,
        "00:00" to 0,
        "06:00" to 6 * 60,
    )
    var quickIndex by remember {
        mutableIntStateOf(quick.indexOfFirst { it.second == currentMinutes })
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.blast_sheet_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.blast_sheet_sub),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
            TimePicker(state = timeState)
            Spacer(Modifier.size(8.dp))
            SegmentedRow(
                options = quick.map { it.first },
                selectedIndex = quickIndex,
                onSelect = { index ->
                    quickIndex = index
                    timeState.hour = quick[index].second / 60
                    timeState.minute = quick[index].second % 60
                },
            )
            Spacer(Modifier.size(20.dp))
            PillButton(text = stringResource(R.string.blast_save), onClick = {
                onSave(timeState.hour * 60 + timeState.minute)
            })
        }
    }
}

/** Fuse preset editor: Min/Hrs/Days/Wks segments + slider + Save pill. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetSheet(
    slot: Int,
    initialAmount: Int,
    initialUnit: DurationUnit,
    onSave: (Int, DurationUnit) -> Unit,
    onDismiss: () -> Unit,
) {
    var unit by remember { mutableStateOf(initialUnit) }
    var amount by remember { mutableIntStateOf(initialAmount) }
    val range = unit.sliderRange
    val safeAmount = amount.coerceIn(range.first, range.last)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.preset_sheet_title, slot),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                stringResource(R.string.preset_sheet_sub),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
            SegmentedRow(
                options = listOf("Min", "Hrs", "Days", "Wks"),
                selectedIndex = DurationUnit.entries.indexOf(unit),
                onSelect = { index ->
                    unit = DurationUnit.entries[index]
                    amount = 1
                },
            )
            Spacer(Modifier.size(20.dp))
            Slider(
                value = safeAmount.toFloat(),
                onValueChange = { raw -> amount = raw.toInt().coerceIn(range.first, range.last) },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                steps = (range.last - range.first) - 1,
            )
            Text(
                stringResource(R.string.label_deletes_in, rememberDurationLabel(safeAmount, unit)),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(Modifier.size(20.dp))
            PillButton(text = stringResource(R.string.preset_save), onClick = { onSave(safeAmount, unit) })
        }
    }
}
