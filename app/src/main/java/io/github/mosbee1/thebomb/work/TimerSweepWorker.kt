package io.github.mosbee1.thebomb.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.mosbee1.thebomb.TheBombApp
import io.github.mosbee1.thebomb.util.CleanupSchedule

/**
 * The 15-minute safety-net sweep: deletes every elapsed fuse (consent
 * fallback if needed), reconciles externally deleted files, and catches up
 * the daily blast if the punctual worker was killed by aggressive OEM
 * battery managers.
 */
class TimerSweepWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as TheBombApp).container
        val settings = container.settingsRepository.current()
        if (!settings.janitorEnabled) return Result.success()

        val now = container.timeProvider.nowMillis()

        val due = container.screenshotRepository.dueTimers(now)
        executor().sweepDueTimers(due)

        container.screenshotRepository.reconcileDeletedExternally()

        if (CleanupSchedule.isCleanupDue(now, settings.cleanupTimeMinutes, settings.lastCleanupHandledAt)) {
            executor().runDailyCleanup(settings)
        }
        return Result.success()
    }

    private fun executor(): SweepExecutor {
        val container = (applicationContext as TheBombApp).container
        return SweepExecutor(
            applicationContext,
            container.screenshotRepository,
            container.settingsRepository,
            container.timeProvider,
        )
    }
}
