package io.github.mosbee1.thebomb.service

import android.app.Service
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.provider.MediaStore
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.mosbee1.thebomb.TheBombApp
import io.github.mosbee1.thebomb.data.local.ScreenshotEntity
import io.github.mosbee1.thebomb.notifications.Notifications
import io.github.mosbee1.thebomb.popup.PopupActivity
import io.github.mosbee1.thebomb.util.Permissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

/**
 * Foreground service (dataSync type) hosting the MediaStore ContentObserver
 * that detects screenshots in real time. Popup and notification are
 * mutually exclusive: when the overlay grant lets us show the popup
 * directly, it replaces the heads-up instead of doubling it.
 */
class ScreenshotWatcherService : Service() {

    private lateinit var scope: CoroutineScope
    private var observerThread: HandlerThread? = null
    private var observer: ContentObserver? = null

    /** Only images added after this (seconds, DATE_ADDED epoch) are scanned. */
    private var scanCutoffSeconds: Long = 0L

    private val scanMutex = Mutex()
    private var scanPending = false

    private var settingsObserverJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scanCutoffSeconds = System.currentTimeMillis() / 1000L - STARTUP_LOOKBACK_SECONDS
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Notifications.ensureChannels(this)
        ServiceCompat.startForeground(
            this,
            Notifications.SERVICE_NOTIFICATION_ID,
            Notifications.serviceNotification(this),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )

        registerObserver()
        observeSettings()
        scope.launch { scan() }
        return START_STICKY
    }

    override fun onDestroy() {
        observer?.let { contentResolver.unregisterContentObserver(it) }
        observerThread?.quitSafely()
        observerThread = null
        observer = null
        settingsObserverJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun registerObserver() {
        if (observer != null) return
        observerThread = HandlerThread("screenshot-watcher").also { it.start() }
        val handler = Handler(observerThread!!.looper)
        observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                requestScan()
            }
        }
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer!!,
        )
    }

    /** Coalesces observer storms (several callbacks per screenshot). */
    private fun requestScan() {
        scope.launch {
            val gotLock = scanMutex.tryLock()
            if (!gotLock) {
                scanPending = true
                return@launch
            }
            try {
                scan()
                while (scanPending) {
                    scanPending = false
                    scan()
                }
            } finally {
                scanMutex.unlock()
            }
        }
    }

    /** Stops the service the moment the user disarms the app. */
    private fun observeSettings() {
        if (settingsObserverJob != null) return
        val container = (applicationContext as TheBombApp).container
        settingsObserverJob = scope.launch {
            container.settingsRepository.settings.collectLatest { settings ->
                if (!settings.janitorEnabled) {
                    stopSelf()
                }
            }
        }
    }

    private suspend fun scan() {
        if (!Permissions.hasImageRead(this)) {
            Notifications.notifyPermissionWarning(this)
            return
        }
        val inserted = withContext(Dispatchers.IO) { queryRecentScreenshots() }
        if (inserted == null) {
            Notifications.notifyPermissionWarning(this)
            return
        }
        scanCutoffSeconds = System.currentTimeMillis() / 1000L - CLOCK_SKEW_OVERLAP_SECONDS
        for (entity in inserted) {
            onNewScreenshot(entity)
        }
        val container = (applicationContext as TheBombApp).container
        container.screenshotRepository.reconcileDeletedExternally()
    }

    /** Returns newly recorded rows, or null on a query-level failure. */
    private suspend fun queryRecentScreenshots(): List<ScreenshotEntity>? {
        val container = (applicationContext as TheBombApp).container
        val repo = container.screenshotRepository
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.RELATIVE_PATH,
        )
        val selection =
            "${MediaStore.Images.Media.DATE_ADDED} >= ? AND ${MediaStore.Images.Media.IS_PENDING} = 0"
        val selectionArgs = arrayOf(scanCutoffSeconds.toString())
        val results = mutableListOf<ScreenshotEntity>()
        return try {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Images.Media.DATE_ADDED} DESC",
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val takenCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: ""
                    val path = cursor.getString(pathCol) ?: ""
                    if (!looksLikeScreenshot(name, path)) continue
                    val dateTaken = cursor.getLong(takenCol)
                    val createdAt = if (dateTaken > 0) dateTaken else cursor.getLong(addedCol) * 1000L
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id,
                    )
                    val entity = ScreenshotEntity(
                        uri = uri.toString(),
                        mediaStoreId = id,
                        fileName = name,
                        createdAtMillis = createdAt,
                        sizeBytes = cursor.getLong(sizeCol),
                    )
                    if (repo.recordScreenshot(entity)) results += entity
                }
            }
            results
        } catch (security: SecurityException) {
            null
        } catch (failure: Throwable) {
            null
        }
    }

    private suspend fun onNewScreenshot(entity: ScreenshotEntity) {
        val container = (applicationContext as TheBombApp).container
        val settings = container.settingsRepository.current()
        if (settings.autoArchiveEnabled) {
            container.screenshotRepository.markArchived(entity.uri)
        }
        // Mutually exclusive surfacing: popup shown directly = no heads-up.
        // Popup off in Settings, no overlay grant, or blocked launch =
        // notification only.
        var popupShown = false
        if (settings.popupEnabled && Permissions.canDrawOverOtherApps(this)) {
            val popup = Intent(this, PopupActivity::class.java)
                .putExtra(Notifications.EXTRA_SCREENSHOT_URI, entity.uri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                startActivity(popup)
                popupShown = true
            } catch (t: Throwable) {
                // Launch blocked; fall through to notification.
            }
        }
        if (!popupShown) {
            Notifications.notifyScreenshot(this, entity, settings.popupEnabled)
        }
    }

    companion object {
        private const val STARTUP_LOOKBACK_SECONDS = 120L
        private const val CLOCK_SKEW_OVERLAP_SECONDS = 5L

        fun looksLikeScreenshot(fileName: String, relativePath: String): Boolean =
            fileName.contains("screenshot", ignoreCase = true) ||
                relativePath.contains("screenshot", ignoreCase = true)

        fun start(context: Context) {
            val intent = Intent(context, ScreenshotWatcherService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ScreenshotWatcherService::class.java))
        }
    }
}
