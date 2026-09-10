package io.github.mosbee1.thebomb.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.mosbee1.thebomb.data.model.DurationUnit
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingsDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaults(entity: AppSettingsEntity)

    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observe(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun get(): AppSettingsEntity?

    @Query("UPDATE app_settings SET janitorEnabled = :enabled WHERE id = 1")
    suspend fun setJanitorEnabled(enabled: Boolean)

    @Query("UPDATE app_settings SET autoArchiveEnabled = :enabled WHERE id = 1")
    suspend fun setAutoArchiveEnabled(enabled: Boolean)

    @Query("UPDATE app_settings SET popupEnabled = :enabled WHERE id = 1")
    suspend fun setPopupEnabled(enabled: Boolean)

    @Query("UPDATE app_settings SET cleanupReminderEnabled = :enabled WHERE id = 1")
    suspend fun setCleanupReminderEnabled(enabled: Boolean)

    @Query("UPDATE app_settings SET cleanupTimeMinutes = :minutes WHERE id = 1")
    suspend fun setCleanupTimeMinutes(minutes: Int)

    @Query("UPDATE app_settings SET lastCleanupHandledAt = :millis WHERE id = 1")
    suspend fun setLastCleanupHandledAt(millis: Long)

    @Query("UPDATE app_settings SET onboardingCompleted = 1 WHERE id = 1")
    suspend fun setOnboardingCompleted()

    @Query("UPDATE app_settings SET preset1Amount = :amount, preset1Unit = :unit WHERE id = 1")
    suspend fun setPreset1(amount: Int, unit: DurationUnit)

    @Query("UPDATE app_settings SET preset2Amount = :amount, preset2Unit = :unit WHERE id = 1")
    suspend fun setPreset2(amount: Int, unit: DurationUnit)

    @Query("UPDATE app_settings SET preset3Amount = :amount, preset3Unit = :unit WHERE id = 1")
    suspend fun setPreset3(amount: Int, unit: DurationUnit)

    @Query("UPDATE app_settings SET preset4Amount = :amount, preset4Unit = :unit WHERE id = 1")
    suspend fun setPreset4(amount: Int, unit: DurationUnit)
}
