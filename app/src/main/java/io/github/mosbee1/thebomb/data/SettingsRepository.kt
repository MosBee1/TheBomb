package io.github.mosbee1.thebomb.data

import io.github.mosbee1.thebomb.data.local.AppSettingsDao
import io.github.mosbee1.thebomb.data.local.AppSettingsEntity
import io.github.mosbee1.thebomb.data.model.AppSettings
import io.github.mosbee1.thebomb.data.model.DurationUnit
import io.github.mosbee1.thebomb.data.model.TimerPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val dao: AppSettingsDao) {

    val settings: Flow<AppSettings> =
        dao.observe().map { entity -> (entity ?: AppSettingsEntity()).toDomain() }

    suspend fun initialize() {
        dao.insertDefaults(AppSettingsEntity())
    }

    suspend fun current(): AppSettings =
        (dao.get() ?: AppSettingsEntity()).toDomain()

    suspend fun setJanitorEnabled(enabled: Boolean) = dao.setJanitorEnabled(enabled)

    suspend fun setAutoArchiveEnabled(enabled: Boolean) = dao.setAutoArchiveEnabled(enabled)

    suspend fun setPopupEnabled(enabled: Boolean) = dao.setPopupEnabled(enabled)

    suspend fun setCleanupReminderEnabled(enabled: Boolean) = dao.setCleanupReminderEnabled(enabled)

    suspend fun setCleanupTimeMinutes(minutes: Int) =
        dao.setCleanupTimeMinutes(minutes.coerceIn(0, 24 * 60 - 1))

    suspend fun setLastCleanupHandledAt(millis: Long) = dao.setLastCleanupHandledAt(millis)

    suspend fun completeOnboarding() = dao.setOnboardingCompleted()

    suspend fun setPreset(slot: Int, amount: Int, unit: DurationUnit) {
        val safeAmount = amount.coerceIn(unit.sliderRange.first, unit.sliderRange.last)
        when (slot) {
            1 -> dao.setPreset1(safeAmount, unit)
            2 -> dao.setPreset2(safeAmount, unit)
            3 -> dao.setPreset3(safeAmount, unit)
            4 -> dao.setPreset4(safeAmount, unit)
            else -> throw IllegalArgumentException("Invalid preset slot: $slot")
        }
    }
}

private fun sanitizeAmount(amount: Int, unit: DurationUnit): Int =
    amount.coerceIn(unit.sliderRange.first, unit.sliderRange.last)

private fun AppSettingsEntity.toDomain(): AppSettings = AppSettings(
    janitorEnabled = janitorEnabled,
    autoArchiveEnabled = autoArchiveEnabled,
    popupEnabled = popupEnabled,
    cleanupReminderEnabled = cleanupReminderEnabled,
    cleanupTimeMinutes = cleanupTimeMinutes.coerceIn(0, 24 * 60 - 1),
    presets = listOf(
        TimerPreset(1, sanitizeAmount(preset1Amount, preset1Unit), preset1Unit),
        TimerPreset(2, sanitizeAmount(preset2Amount, preset2Unit), preset2Unit),
        TimerPreset(3, sanitizeAmount(preset3Amount, preset3Unit), preset3Unit),
        TimerPreset(4, sanitizeAmount(preset4Amount, preset4Unit), preset4Unit),
    ),
    lastCleanupHandledAt = lastCleanupHandledAt,
    onboardingCompleted = onboardingCompleted,
)
