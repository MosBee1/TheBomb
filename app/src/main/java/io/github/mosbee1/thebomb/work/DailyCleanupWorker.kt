package io.github.mosbee1.thebomb.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.mosbee1.thebomb.TheBombApp
import io.github.mosbee1.thebomb.util.CleanupSchedule

/**
 * Punctual daily blast. Scheduled as a one-time job at the exact next
 * occurrence of the user's blast time; always re-schedules itself (and the
 * optional reminder) for the following day before finishing.
 */
class DailyCleanupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as TheBombApp).container
        val settings = container.settingsRepository.current()
        val now = container.timeProvider.nowMillis()

        if (settings.janitorEnabled &&
            CleanupSchedule.isCleanupDue(now, settings.cleanupTimeMinutes, settings.lastCleanupHandledAt)
        ) {
            SweepExecutor(
                applicationContext,
                container.screenshotRepository,
                container.settingsRepository,
                container.timeProvider,
            ).runDailyCleanup(settings)
        }

        BackgroundWork.scheduleNextCleanup(applicationContext, settings, now)
        return Result.success()
    }
}
