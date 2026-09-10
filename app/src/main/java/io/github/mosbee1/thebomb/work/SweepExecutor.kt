package io.github.mosbee1.thebomb.work

import android.content.Context
import io.github.mosbee1.thebomb.data.DeletionOutcome
import io.github.mosbee1.thebomb.data.SettingsRepository
import io.github.mosbee1.thebomb.data.ScreenshotRepository
import io.github.mosbee1.thebomb.data.local.ScreenshotEntity
import io.github.mosbee1.thebomb.data.model.AppSettings
import io.github.mosbee1.thebomb.notifications.Notifications
import io.github.mosbee1.thebomb.util.TimeProvider

class SweepExecutor(
    private val context: Context,
    private val screenshots: ScreenshotRepository,
    private val settings: SettingsRepository,
    private val timeProvider: TimeProvider,
) {

    data class SweepReport(
        val deletedNow: Int,
        val awaitingConsent: Int,
        val failed: Int,
    ) {
        companion object {
            val EMPTY = SweepReport(0, 0, 0)
        }
    }

    suspend fun sweepDueTimers(due: List<ScreenshotEntity>): SweepReport {
        if (due.isEmpty()) return SweepReport.EMPTY
        return deleteBatch(due.map { it.uri })
    }

    /**
     * Stamps lastCleanupHandledAt FIRST (through the settings repository) so
     * a crash mid-deletion cannot re-run the same day's blast repeatedly;
     * any stragglers simply remain archived candidates for tomorrow.
     */
    suspend fun runDailyCleanup(current: AppSettings): SweepReport {
        val now = timeProvider.nowMillis()
        settings.setLastCleanupHandledAt(now)
        val candidates = screenshots.dailyCleanupCandidates()
        if (candidates.isEmpty()) return SweepReport.EMPTY
        return deleteBatch(candidates.map { it.uri })
    }

    private suspend fun deleteBatch(uris: List<String>): SweepReport {
        if (uris.isEmpty()) return SweepReport.EMPTY
        return when (val outcome = screenshots.deleteFromMediaStore(uris)) {
            is DeletionOutcome.DeletedDirectly -> {
                screenshots.onDeletionApproved(uris)
                SweepReport(deletedNow = uris.size, awaitingConsent = 0, failed = 0)
            }

            is DeletionOutcome.ConsentRequired -> {
                Notifications.notifyConsentNeeded(context, uris)
                SweepReport(deletedNow = 0, awaitingConsent = uris.size, failed = 0)
            }

            is DeletionOutcome.Failed -> {
                Notifications.notifyDeletionFailed(context, uris.size)
                SweepReport(deletedNow = 0, awaitingConsent = 0, failed = uris.size)
            }
        }
    }
}
