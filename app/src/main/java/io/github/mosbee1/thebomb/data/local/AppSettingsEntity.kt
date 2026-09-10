package io.github.mosbee1.thebomb.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.mosbee1.thebomb.data.model.DurationUnit

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = SETTINGS_ROW_ID,
    val janitorEnabled: Boolean = false,
    val autoArchiveEnabled: Boolean = false,
    val popupEnabled: Boolean = true,
    val cleanupReminderEnabled: Boolean = false,
    val cleanupTimeMinutes: Int = 23 * 60 + 30,
    val preset1Amount: Int = 10,
    val preset1Unit: DurationUnit = DurationUnit.MINUTES,
    val preset2Amount: Int = 1,
    val preset2Unit: DurationUnit = DurationUnit.HOURS,
    val preset3Amount: Int = 1,
    val preset3Unit: DurationUnit = DurationUnit.DAYS,
    val preset4Amount: Int = 1,
    val preset4Unit: DurationUnit = DurationUnit.WEEKS,
    val lastCleanupHandledAt: Long = 0L,
    val onboardingCompleted: Boolean = false,
) {
    companion object {
        const val SETTINGS_ROW_ID = 1
    }
}
