package io.github.mosbee1.thebomb.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.mosbee1.thebomb.MainActivity
import io.github.mosbee1.thebomb.R
import io.github.mosbee1.thebomb.consent.DeletionConsentActivity
import io.github.mosbee1.thebomb.data.local.ScreenshotEntity
import io.github.mosbee1.thebomb.popup.PopupActivity
import io.github.mosbee1.thebomb.util.Permissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

object Notifications {

    private const val CHANNEL_SERVICE = "janitor_service"
    private const val CHANNEL_SCREENSHOT = "screenshot_detected"
    private const val CHANNEL_REMINDER = "cleanup_reminder"
    private const val CHANNEL_CONSENT = "delete_consent"

    /** Public so ScreenshotWatcherService can call ServiceCompat.startForeground. */
    const val SERVICE_NOTIFICATION_ID = 1001

    private const val ID_PERMISSION_WARNING = 4001
    private const val ID_SCREENSHOT_BASE = 10_000
    private const val ID_CONSENT_BASE = 20_000

    const val EXTRA_SCREENSHOT_URI = "io.github.mosbee1.thebomb.extra.SCREENSHOT_URI"
    const val EXTRA_DELETE_URIS = "io.github.mosbee1.thebomb.extra.DELETE_URIS"
    const val EXTRA_NOTIFICATION_ID = "io.github.mosbee1.thebomb.extra.NOTIFICATION_ID"

    private fun screenshotNotificationId(mediaStoreId: Long): Int =
        ID_SCREENSHOT_BASE + (mediaStoreId % 100_000L).toInt()

    private fun consentNotificationId(uris: List<String>): Int =
        ID_CONSENT_BASE + (uris.sorted().joinToString(separator = "|").hashCode() and 0x7FFFFFFF) % 100_000

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val service = NotificationChannel(
            CHANNEL_SERVICE,
            context.getString(R.string.notif_channel_service_name),
            NotificationManager.IMPORTANCE_MIN,
        ).apply {
            description = context.getString(R.string.notif_channel_service_desc)
            setShowBadge(false)
        }
        val screenshot = NotificationChannel(
            CHANNEL_SCREENSHOT,
            context.getString(R.string.notif_channel_screenshot_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notif_channel_screenshot_desc)
        }
        val reminder = NotificationChannel(
            CHANNEL_REMINDER,
            context.getString(R.string.notif_channel_reminder_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notif_channel_reminder_desc)
        }
        val consent = NotificationChannel(
            CHANNEL_CONSENT,
            context.getString(R.string.notif_channel_consent_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notif_channel_consent_desc)
        }
        manager.createNotificationChannels(listOf(service, screenshot, reminder, consent))
    }

    fun serviceNotification(context: Context): Notification {
        ensureChannels(context)
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_service_title))
            .setContentText(context.getString(R.string.notif_service_text))
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openApp)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    suspend fun loadThumbnail(
        context: Context,
        uri: Uri,
        widthPx: Int = 512,
        heightPx: Int = 512,
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.loadThumbnail(uri, Size(widthPx, heightPx), null)
        } catch (t: Throwable) {
            null
        }
    }

    suspend fun screenshotNotification(
        context: Context,
        entity: ScreenshotEntity,
        showFullScreen: Boolean,
    ): Notification {
        ensureChannels(context)
        val contentUri = Uri.parse(entity.uri)

        val openApp = PendingIntent.getActivity(
            context,
            100,
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_SCREENSHOT_URI, entity.uri),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val fullScreen: PendingIntent? = if (showFullScreen) PendingIntent.getActivity(
            context,
            101,
            Intent(context, PopupActivity::class.java)
                .putExtra(EXTRA_SCREENSHOT_URI, entity.uri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        ) else null

        fun action(requestCodeBase: Int, titleRes: Int) =
            NotificationCompat.Action(
                0,
                context.getString(titleRes),
                PendingIntent.getBroadcast(
                    context,
                    requestCodeBase * 1_000_000 + (entity.uri.hashCode() and 0x7FFFFF),
                    Intent(context, NotificationActionReceiver::class.java)
                        .setAction("io.github.mosbee1.thebomb.action.NotificationActionReceiver.$requestCodeBase")
                        .putExtra(EXTRA_SCREENSHOT_URI, entity.uri)
                        .putExtra(
                            EXTRA_NOTIFICATION_ID,
                            screenshotNotificationId(entity.mediaStoreId),
                        ),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )

        val builder = NotificationCompat.Builder(context, CHANNEL_SCREENSHOT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_screenshot_title))
            .setContentText(entity.fileName)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(action(1, R.string.action_keep))
            .addAction(action(2, R.string.action_archive))
            .addAction(action(3, R.string.action_delete_now))

        loadThumbnail(context, contentUri)?.let { bitmap ->
            builder.setLargeIcon(bitmap)
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
                    .bigLargeIcon(null as Bitmap?),
            )
        }

        fullScreen?.let { builder.setFullScreenIntent(it, true) }
        return builder.build()
    }

    fun notifyScreenshot(context: Context, entity: ScreenshotEntity, showFullScreen: Boolean) {
        if (!Permissions.canPostNotifications(context)) return
        val id = screenshotNotificationId(entity.mediaStoreId)
        val notification = runBlocking {
            screenshotNotification(context, entity, showFullScreen)
        }
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (security: SecurityException) {
            // POST_NOTIFICATIONS revoked between check and post; degrade quietly.
        }
    }

    fun cancelScreenshotNotification(context: Context, mediaStoreId: Long) {
        NotificationManagerCompat.from(context).cancel(screenshotNotificationId(mediaStoreId))
    }

    fun notifyConsentNeeded(context: Context, uris: List<String>) {
        ensureChannels(context)
        val consent = PendingIntent.getActivity(
            context,
            300,
            Intent(context, DeletionConsentActivity::class.java)
                .putExtra(EXTRA_DELETE_URIS, uris.toTypedArray())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_CONSENT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_consent_title))
            .setContentText(
                context.resources.getQuantityString(
                    R.plurals.notif_consent_text,
                    uris.size,
                    uris.size,
                ),
            )
            .setContentIntent(consent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            NotificationManagerCompat.from(context)
                .notify(consentNotificationId(uris), notification)
        } catch (security: SecurityException) {
            // Notifications revoked; the rows stay untouched and retry next sweep.
        }
    }

    fun cancelConsentNotifications(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        for (posted in manager.activeNotifications) {
            if (posted.notification.channelId == CHANNEL_CONSENT) {
                manager.cancel(posted.id)
            }
        }
    }

    fun notifyCleanupReminder(context: Context, candidateCount: Int) {
        ensureChannels(context)
        if (!Permissions.canPostNotifications(context)) return
        val openApp = PendingIntent.getActivity(
            context,
            400,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_cleanup_reminder_title))
            .setContentText(
                context.resources.getQuantityString(
                    R.plurals.notif_cleanup_reminder_text,
                    candidateCount,
                    candidateCount,
                ),
            )
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(5001, notification)
        } catch (security: SecurityException) {
            // Degrade quietly; cleanup itself is unaffected.
        }
    }

    fun notifyPermissionWarning(context: Context) {
        ensureChannels(context)
        val openApp = PendingIntent.getActivity(
            context,
            600,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_CONSENT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_permission_warning_title))
            .setContentText(context.getString(R.string.notif_permission_warning_text))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(ID_PERMISSION_WARNING, notification)
        } catch (security: SecurityException) {
            // Nothing further we can do without notifications.
        }
    }

    fun notifyDeletionFailed(context: Context, count: Int) {
        ensureChannels(context)
        val openApp = PendingIntent.getActivity(
            context,
            700,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_CONSENT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_delete_failed_title))
            .setContentText(
                context.resources.getQuantityString(
                    R.plurals.notif_delete_failed_text,
                    count,
                    count,
                ),
            )
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(6001, notification)
        } catch (security: SecurityException) {
            // Degrade quietly.
        }
    }
}
