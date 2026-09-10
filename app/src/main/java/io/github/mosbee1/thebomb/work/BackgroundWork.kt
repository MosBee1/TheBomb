package io.github.mosbee1.thebomb.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import io.github.mosbee1.thebomb.data.SettingsRepository
import io.github.mosbee1.thebomb.data.local.TheBombDatabase
import io.github.mosbee1.thebomb.data.model.AppSettings
import io.github.mosbee1.thebomb.util.CleanupSchedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object BackgroundWork {

    private const val PERIODIC_SWEEP = "periodic_sweep"
    private const val CLEANUP = "daily_cleanup"
    private const val REMINDER = "cleanup_reminder"
    private const val REMINDER_LEAD_MINUTES = 30L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private fun timerName(uri: String) = "timer|$uri"

    private fun workManager(context: Context): WorkManager =
        WorkManager.getInstance(context)

    /** Idempotently ensures all recurring work exists. Workers no-op when disarmed. */
    fun ensureScheduled(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            val wm = workManager(appContext)
            wm.enqueueUniquePeriodicWork(
                PERIODIC_SWEEP,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<TimerSweepWorker>(15, TimeUnit.MINUTES)
                    .build(),
            )
            val settings = settingsOf(appContext)
            val now = System.currentTimeMillis()
            wm.enqueueUniqueWork(
                CLEANUP,
                ExistingWorkPolicy.KEEP,
                cleanupRequest(now, settings.cleanupTimeMinutes),
            )
            reminderRequest(now, settings)?.let { request ->
                wm.enqueueUniqueWork(REMINDER, ExistingWorkPolicy.KEEP, request)
            }
        }
    }

    /** Called by Settings when the blast time or reminder toggle changes. */
    fun rescheduleCleanupFromSettings(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            val wm = workManager(appContext)
            val settings = settingsOf(appContext)
            val now = System.currentTimeMillis()
            wm.enqueueUniqueWork(
                CLEANUP,
                ExistingWorkPolicy.REPLACE,
                cleanupRequest(now, settings.cleanupTimeMinutes),
            )
            reminderRequest(now, settings)?.let { request ->
                wm.enqueueUniqueWork(REMINDER, ExistingWorkPolicy.REPLACE, request)
            }
        }
    }

    /** Arms (or re-arms) the punctual one-time sweep for one screenshot. */
    fun scheduleTimerSweep(context: Context, uri: String, fireAtMillis: Long, nowMillis: Long) {
        val delay = (fireAtMillis - nowMillis).coerceAtLeast(0L)
        workManager(context).enqueueUniqueWork(
            timerName(uri),
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<TimerSweepWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
                .addTag(TAG_TIMER)
                .build(),
        )
    }

    fun cancelTimerSweep(context: Context, uri: String) {
        workManager(context).cancelUniqueWork(timerName(uri))
    }

    fun scheduleNextCleanup(context: Context, settings: AppSettings, nowMillis: Long) {
        workManager(context).enqueueUniqueWork(
            CLEANUP,
            ExistingWorkPolicy.REPLACE,
            cleanupRequest(nowMillis, settings.cleanupTimeMinutes),
        )
        reminderRequest(nowMillis, settings)?.let { request ->
            workManager(context).enqueueUniqueWork(REMINDER, ExistingWorkPolicy.REPLACE, request)
        }
    }

    private fun cleanupRequest(nowMillis: Long, cleanupTimeMinutes: Int) =
        OneTimeWorkRequestBuilder<DailyCleanupWorker>()
            .setInitialDelay(
                CleanupSchedule.nextOccurrenceAfter(nowMillis, cleanupTimeMinutes) - nowMillis,
                TimeUnit.MILLISECONDS,
            )
            .setBackoffCriteria(BackoffPolicy.LINEAR, 60, TimeUnit.SECONDS)
            .addTag(TAG_CLEANUP)
            .build()

    private fun reminderRequest(nowMillis: Long, settings: AppSettings) =
        if (settings.janitorEnabled && settings.cleanupReminderEnabled) {
            val cleanupAt = CleanupSchedule.nextOccurrenceAfter(nowMillis, settings.cleanupTimeMinutes)
            val reminderAt = cleanupAt - REMINDER_LEAD_MINUTES * 60_000L
            val delay = reminderAt - nowMillis
            if (delay >= 60_000L) {
                OneTimeWorkRequestBuilder<PreCleanupReminderWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 60, TimeUnit.SECONDS)
                    .addTag(TAG_REMINDER)
                    .build()
            } else {
                null
            }
        } else {
            null
        }

    private suspend fun settingsOf(context: Context): AppSettings =
        SettingsRepository(TheBombDatabase.get(context).appSettingsDao()).current()

    const val TAG_TIMER = "thebomb_timer"
    const val TAG_CLEANUP = "thebomb_cleanup"
    const val TAG_REMINDER = "thebomb_reminder"
}
