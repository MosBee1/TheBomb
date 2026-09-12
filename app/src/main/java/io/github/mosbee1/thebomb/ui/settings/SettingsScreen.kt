package io.github.mosbee1.thebomb.ui.settings

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Rocket
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.mosbee1.thebomb.BuildConfig
import io.github.mosbee1.thebomb.R
import io.github.mosbee1.thebomb.TheBombApp
import io.github.mosbee1.thebomb.data.model.AppSettings
import io.github.mosbee1.thebomb.ui.MainViewModel
import io.github.mosbee1.thebomb.ui.common.SectionTitle
import io.github.mosbee1.thebomb.ui.common.SettingsRowCard
import io.github.mosbee1.thebomb.ui.common.rememberAdaptiveHorizontalPadding
import io.github.mosbee1.thebomb.ui.common.rememberDurationLabel
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
            BackgroundWork.rescheduleCleanupFromSettings(appContext)
        }
    }

    fun setCleanupTime(minutes: Int) {
        viewModelScope.launch {
            container.settingsRepository.setCleanupTimeMinutes(minutes)
            BackgroundWork.rescheduleCleanupFromSettings(appContext)
        }
    }

    fun savePreset(slot: Int, amount: Int, unit: io.github.mosbee1.thebomb.data.model.DurationUnit) {
        viewModelScope.launch { container.settingsRepository.setPreset(slot, amount, unit) }
    }
}

@Composable
fun SettingsScreen(mainViewModel: MainViewModel) {
    val settingsViewModel: SettingsViewModel = viewModel()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val mainSettings by mainViewModel.settings.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val hPad = rememberAdaptiveHorizontalPadding()

    var showPermissions by remember { mutableStateOf(false) }
    var showBlastTime by remember { mutableStateOf(false) }
    var showChangelog by remember { mutableStateOf(false) }
    var editingSlot by remember { mutableIntStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = hPad, end = hPad, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            stringResource(R.string.tab_settings),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 6.dp),
        )

        SupportCarousel()
        Spacer(Modifier.height(10.dp))

        SectionTitle(stringResource(R.string.section_general))
        SettingsRowCard(
            icon = Icons.Rounded.Security,
            title = stringResource(R.string.perm_row_all),
            subtitle = stringResource(R.string.perm_sub_all),
            onClick = { showPermissions = true },
        )
        SettingsRowCard(
            icon = Icons.Rounded.Bolt,
            title = stringResource(R.string.settings_janitor),
            subtitle = stringResource(R.string.settings_janitor_desc),
            onClick = { mainViewModel.setJanitorEnabled(!mainSettings.janitorEnabled) },
        ) {
            Switch(
                checked = mainSettings.janitorEnabled,
                onCheckedChange = { mainViewModel.setJanitorEnabled(it) },
            )
        }
        SettingsRowCard(
            icon = Icons.Rounded.Archive,
            title = stringResource(R.string.settings_auto_archive),
            subtitle = stringResource(R.string.settings_auto_archive_desc),
            onClick = { settingsViewModel.setAutoArchive(!settings.autoArchiveEnabled) },
        ) {
            Switch(
                checked = settings.autoArchiveEnabled,
                onCheckedChange = { settingsViewModel.setAutoArchive(it) },
            )
        }
        SettingsRowCard(
            icon = Icons.Rounded.PictureInPictureAlt,
            title = stringResource(R.string.settings_popup),
            subtitle = stringResource(R.string.settings_popup_desc),
            onClick = { settingsViewModel.setPopup(!settings.popupEnabled) },
        ) {
            Switch(
                checked = settings.popupEnabled,
                onCheckedChange = { settingsViewModel.setPopup(it) },
            )
        }

        SectionTitle(stringResource(R.string.settings_section_cleanup))
        SettingsRowCard(
            icon = Icons.Rounded.Schedule,
            title = stringResource(R.string.settings_cleanup_time),
            subtitle = formatCleanupTime(settings.cleanupTimeMinutes),
            onClick = { showBlastTime = true },
        )
        SettingsRowCard(
            icon = Icons.Rounded.Alarm,
            title = stringResource(R.string.settings_reminder),
            subtitle = stringResource(R.string.settings_reminder_desc),
            onClick = { settingsViewModel.setReminder(!settings.cleanupReminderEnabled) },
        ) {
            Switch(
                checked = settings.cleanupReminderEnabled,
                onCheckedChange = { settingsViewModel.setReminder(it) },
            )
        }

        SectionTitle(stringResource(R.string.settings_section_presets))
        settings.presets.forEach { preset ->
            SettingsRowCard(
                icon = Icons.Rounded.Timer,
                title = stringResource(R.string.settings_preset_label, preset.slot),
                subtitle = rememberDurationLabel(preset.amount, preset.unit),
                onClick = { editingSlot = preset.slot },
            )
        }

        SectionTitle(stringResource(R.string.settings_section_about))
        SettingsRowCard(
            icon = Icons.Rounded.Rocket,
            title = stringResource(R.string.app_name),
            subtitle = stringResource(R.string.app_card_sub_fmt, BuildConfig.VERSION_NAME),
        )
        SettingsRowCard(
            icon = Icons.Rounded.History,
            title = stringResource(R.string.about_row_changelog),
            subtitle = stringResource(R.string.about_sub_changelog),
            onClick = { showChangelog = true },
        )
        SettingsRowCard(
            icon = Icons.Rounded.PrivacyTip,
            title = stringResource(R.string.about_privacy),
            subtitle = stringResource(R.string.about_sub_privacy),
            onClick = {
                runCatching {
                    uriHandler.openUri("https://github.com/MosBee1/TheBomb/blob/main/PRIVACY.md")
                }
            },
        )
        SettingsRowCard(
            icon = Icons.Rounded.Description,
            title = stringResource(R.string.about_license_title),
            subtitle = stringResource(R.string.about_sub_license),
            onClick = {
                runCatching {
                    uriHandler.openUri("https://github.com/MosBee1/TheBomb/blob/main/LICENSE")
                }
            },
        )
        SettingsRowCard(
            icon = Icons.Rounded.Code,
            title = stringResource(R.string.about_row_repo),
            subtitle = stringResource(R.string.about_sub_repo),
            onClick = { runCatching { uriHandler.openUri("https://github.com/MosBee1/TheBomb") } },
        )
    }

    if (showPermissions) {
        PermissionsSheet(mainViewModel) { showPermissions = false }
    }
    if (showBlastTime) {
        BlastTimeSheet(
            currentMinutes = settings.cleanupTimeMinutes,
            onSave = { minutes ->
                settingsViewModel.setCleanupTime(minutes)
                showBlastTime = false
            },
            onDismiss = { showBlastTime = false },
        )
    }
    if (editingSlot != -1) {
        val slot = editingSlot
        val preset = settings.presets.first { it.slot == slot }
        PresetSheet(
            slot = slot,
            initialAmount = preset.amount,
            initialUnit = preset.unit,
            onSave = { amount, unit ->
                settingsViewModel.savePreset(slot, amount, unit)
                editingSlot = -1
            },
            onDismiss = { editingSlot = -1 },
        )
    }
    if (showChangelog) {
        ChangelogSheet(onDismiss = { showChangelog = false })
    }
}

@Composable
private fun formatCleanupTime(minutes: Int): String {
    return remember(minutes) {
        val time = LocalTime.of(minutes / 60, minutes % 60)
        time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    }
}
