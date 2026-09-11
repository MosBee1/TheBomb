package io.github.mosbee1.thebomb.ui.settings

import android.app.Application
import android.os.Build
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.mosbee1.thebomb.BuildConfig
import io.github.mosbee1.thebomb.R
import io.github.mosbee1.thebomb.TheBombApp
import io.github.mosbee1.thebomb.data.model.AppSettings
import io.github.mosbee1.thebomb.data.model.DurationUnit
import io.github.mosbee1.thebomb.data.model.TimerPreset
import io.github.mosbee1.thebomb.ui.MainViewModel
import io.github.mosbee1.thebomb.ui.common.SectionHeader
import io.github.mosbee1.thebomb.ui.common.rememberDurationLabel
import io.github.mosbee1.thebomb.ui.common.shortUnitLabel
import io.github.mosbee1.thebomb.work.BackgroundWork
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as TheBombApp).container
    private val appContext = application.applicationContext

    val settings: StateFlow<AppSettings> = container.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun setAutoArchive(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setAutoArchiveEnabled(enabled) }
    }

    fun setPopup(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setPopupEnabled(enabled) }
    }

    fun setReminder(enabled: Boolean) {
        viewModelScope.launch {
            container.settingsRepository.setCleanupReminderEnabled(enabled)
            // Re-assert so the reminder job is armed/disarmed immediately.
            BackgroundWork.rescheduleCleanupFromSettings(appContext)
        }
    }

    fun setCleanupTime(minutes: Int) {
        viewModelScope.launch {
            container.settingsRepository.setCleanupTimeMinutes(minutes)
            BackgroundWork.rescheduleCleanupFromSettings(appContext)
        }
    }

    fun savePreset(slot: Int, amount: Int, unit: DurationUnit) {
        viewModelScope.launch { container.settingsRepository.setPreset(slot, amount, unit) }
    }
}

/**
 * Settings: master switch, behavior toggles (auto-stage, popup), the daily
 * blast time + pre-blast reminder, the four independently configurable fuse
 * presets (unit chips + slider), and the about section.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(mainViewModel: MainViewModel) {
    val settingsViewModel: SettingsViewModel = viewModel()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val mainSettings by mainViewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    var showTimePicker by remember { mutableStateOf(false) }
    var editingSlot by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PermissionDashboard(mainViewModel)
        SectionHeader(stringResource(R.string.settings_section_janitor))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier
                    .clickable { mainViewModel.setJanitorEnabled(!mainSettings.janitorEnabled) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.settings_janitor),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        stringResource(R.string.settings_janitor_desc),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = mainSettings.janitorEnabled,
                    onCheckedChange = { mainViewModel.setJanitorEnabled(it) },
                )
            }
        }

        SectionHeader(stringResource(R.string.settings_section_behavior))
        SwitchRow(
            title = stringResource(R.string.settings_auto_archive),
            subtitle = stringResource(R.string.settings_auto_archive_desc),
            checked = settings.autoArchiveEnabled,
            onCheckedChange = settingsViewModel::setAutoArchive,
        )
        SwitchRow(
            title = stringResource(R.string.settings_popup),
            subtitle = stringResource(R.string.settings_popup_desc),
            checked = settings.popupEnabled,
            onCheckedChange = settingsViewModel::setPopup,
        )

        SectionHeader(stringResource(R.string.settings_section_cleanup))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier
                    .clickable { showTimePicker = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.settings_cleanup_time),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        formatCleanupTime(settings.cleanupTimeMinutes),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    stringResource(R.string.action_edit),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        SwitchRow(
            title = stringResource(R.string.settings_reminder),
            subtitle = stringResource(R.string.settings_reminder_desc),
            checked = settings.cleanupReminderEnabled,
            onCheckedChange = settingsViewModel::setReminder,
        )

        SectionHeader(stringResource(R.string.settings_section_presets))
        settings.presets.forEach { preset ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier
                        .clickable { editingSlot = preset.slot }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.settings_preset_label, preset.slot),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        rememberDurationLabel(preset.amount, preset.unit),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.action_edit),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        SupportSection()
        SectionHeader(stringResource(R.string.settings_section_about))
        LinkRow(label = stringResource(R.string.about_privacy), onClick = {
            uriHandler.openUri("https://github.com/MosBee1/TheBomb/blob/main/PRIVACY.md")
        })
        LinkRow(label = stringResource(R.string.about_license_title), onClick = {
            uriHandler.openUri("https://github.com/MosBee1/TheBomb/blob/main/LICENSE")
        })
        Text(
            "The Bomb v" + BuildConfig.VERSION_NAME + " · " +
                stringResource(R.string.settings_mit),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = settings.cleanupTimeMinutes / 60,
            initialMinute = settings.cleanupTimeMinutes % 60,
            is24Hour = DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.time_picker_title)) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    settingsViewModel.setCleanupTime(timeState.hour * 60 + timeState.minute)
                    showTimePicker = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    val slot = editingSlot
    if (slot != null) {
        val preset = settings.presets.first { it.slot == slot }
        PresetEditorDialog(
            slot = slot,
            initial = preset,
            onSave = { amount, unit -> settingsViewModel.savePreset(slot, amount, unit) },
            onDismiss = { editingSlot = null },
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.OpenInNew, contentDescription = null)
        }
    }
}

/**
 * Fuse preset editor: unit chips + amount slider within that unit's legal
 * range, with a live "Detonates in X" preview — the same control shape as
 * the custom picker in the popup.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetEditorDialog(
    slot: Int,
    initial: TimerPreset,
    onSave: (amount: Int, unit: DurationUnit) -> Unit,
    onDismiss: () -> Unit,
) {
    var unit by remember { mutableStateOf(initial.unit) }
    var amount by remember { mutableIntStateOf(initial.amount) }
    val range = unit.sliderRange
    val safeAmount = amount.coerceIn(range.first, range.last)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.preset_editor_title, slot)) },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DurationUnit.entries.forEach { candidate ->
                        FilterChip(
                            selected = unit == candidate,
                            onClick = {
                                unit = candidate
                                amount = 1
                            },
                            label = {
                                Text(
                                    when (candidate) {
                                        DurationUnit.MINUTES ->
                                            stringResource(R.string.unit_minutes_short)
                                        DurationUnit.HOURS ->
                                            stringResource(R.string.unit_hours_short)
                                        DurationUnit.DAYS ->
                                            stringResource(R.string.unit_days_short)
                                        DurationUnit.WEEKS ->
                                            stringResource(R.string.unit_weeks_short)
                                    }
                                )
                            },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Slider(
                    value = safeAmount.toFloat(),
                    onValueChange = { raw ->
                        amount = raw.toInt().coerceIn(range.first, range.last)
                    },
                    valueRange = range.first.toFloat()..range.last.toFloat(),
                    steps = (range.last - range.first) - 1,
                )
                Text(
                    stringResource(
                        R.string.label_deletes_in,
                        rememberDurationLabel(safeAmount, unit),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    shortUnitLabel(safeAmount, unit),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(safeAmount, unit)
                onDismiss()
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun formatCleanupTime(minutes: Int): String {
    return remember(minutes) {
        val time = LocalTime.of(minutes / 60, minutes % 60)
        time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    }
}
