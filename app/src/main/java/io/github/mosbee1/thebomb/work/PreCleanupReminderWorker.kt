package io.github.mosbee1.thebomb.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.mosbee1.thebomb.TheBombApp
import io.github.mosbee1.thebomb.notifications.Notifications

/**
 * Fires 30 minutes before the daily blast (when the user enabled reminders).
 * Posts a count of what will be swept; posts nothing when disarmed,
 * reminders are off, or there is nothing to sweep.
 */
class PreCleanupReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as TheBombApp).container
        val settings = container.settingsRepository.current()
        if (settings.janitorEnabled && settings.cleanupReminderEnabled) {
            val candidates = container.screenshotRepository.dailyCleanupCandidates()
            if (candidates.isNotEmpty()) {
                Notifications.notifyCleanupReminder(applicationContext, candidates.size)
            }
        }
        return Result.success()
    }
}
