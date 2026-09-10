package io.github.mosbee1.thebomb.ui.common

import android.content.Context
import android.content.Intent
import io.github.mosbee1.thebomb.TheBombApp
import io.github.mosbee1.thebomb.consent.DeletionConsentActivity
import io.github.mosbee1.thebomb.notifications.Notifications
import io.github.mosbee1.thebomb.work.BackgroundWork

/**
 * Single entry point for every triage action (home list, kept tab, popup,
 * notification receiver share the same semantics). Detonate never runs from
 * the UI layer directly: scoped-storage consent must launch from a foreground
 * activity, so this starts DeletionConsentActivity, which performs the flow.
 */
object Triage {

    suspend fun keep(context: Context, uri: String) {
        val app = context.applicationContext as TheBombApp
        app.container.screenshotRepository.markKept(uri)
        // Kept items are exempt from auto-deletion: any armed fuse goes.
        BackgroundWork.cancelTimerSweep(context.applicationContext, uri)
    }

    suspend fun archive(context: Context, uri: String) {
        val app = context.applicationContext as TheBombApp
        app.container.screenshotRepository.markArchived(uri)
    }

    suspend fun unarchive(context: Context, uri: String) {
        val app = context.applicationContext as TheBombApp
        app.container.screenshotRepository.markPending(uri)
    }

    fun deleteNow(context: Context, uris: List<String>) {
        if (uris.isEmpty()) return
        val intent = Intent(context, DeletionConsentActivity::class.java)
            .putExtra(Notifications.EXTRA_DELETE_URIS, uris.toTypedArray())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** Arms the fuse and schedules the punctual one-time sweep. Returns fire-at. */
    suspend fun setTimer(context: Context, uri: String, durationMillis: Long): Long {
        val app = context.applicationContext as TheBombApp
        val container = app.container
        val fireAt = container.screenshotRepository.setDeleteTimer(uri, durationMillis)
        BackgroundWork.scheduleTimerSweep(
            context.applicationContext,
            uri,
            fireAt,
            container.timeProvider.nowMillis(),
        )
        return fireAt
    }

    suspend fun cancelTimer(context: Context, uri: String) {
        val app = context.applicationContext as TheBombApp
        app.container.screenshotRepository.cancelDeleteTimer(uri)
        BackgroundWork.cancelTimerSweep(context.applicationContext, uri)
    }
}
