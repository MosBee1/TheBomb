package io.github.mosbee1.thebomb.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import io.github.mosbee1.thebomb.TheBombApp
import io.github.mosbee1.thebomb.consent.DeletionConsentActivity
import io.github.mosbee1.thebomb.work.BackgroundWork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Handles Defuse / Archive / Detonate taps on the post-screenshot
 * notification. Keep and Archive are pure database writes. Detonate cannot
 * run from a receiver (scoped-storage consent needs a foreground activity),
 * so it launches DeletionConsentActivity - a notification tap is user
 * interaction, so the launch is permitted.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val uri = intent.getStringExtra(Notifications.EXTRA_SCREENSHOT_URI) ?: return
        val notificationId = intent.getIntExtra(Notifications.EXTRA_NOTIFICATION_ID, -1)
        val appContext = context.applicationContext
        val pending = goAsync()

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val container = (appContext as TheBombApp).container
                val repo = container.screenshotRepository
                val entity = repo.observeByUri(uri).first()

                when (intent.action?.substringAfterLast('.')) {
                    ACTION_KEEP_SUFFIX -> {
                        repo.markKept(uri)
                        // Kept = exempt from auto-deletion: clear any armed sweep.
                        BackgroundWork.cancelTimerSweep(appContext, uri)
                    }

                    ACTION_ARCHIVE_SUFFIX -> repo.markArchived(uri)

                    ACTION_DELETE_SUFFIX -> {
                        val deleteIntent = Intent(appContext, DeletionConsentActivity::class.java)
                            .putExtra(Notifications.EXTRA_DELETE_URIS, arrayOf(uri))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        appContext.startActivity(deleteIntent)
                    }
                }

                if (notificationId != -1) {
                    NotificationManagerCompat.from(appContext).cancel(notificationId)
                }
                if (entity != null) {
                    Notifications.cancelScreenshotNotification(appContext, entity.mediaStoreId)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        // Suffixes match the requestCodeBase used when building the
        // notification actions in Notifications - "1", "2", "3".
        private const val ACTION_KEEP_SUFFIX = "1"
        private const val ACTION_ARCHIVE_SUFFIX = "2"
        private const val ACTION_DELETE_SUFFIX = "3"
    }
}
