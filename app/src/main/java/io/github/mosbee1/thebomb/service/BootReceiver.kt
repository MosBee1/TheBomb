package io.github.mosbee1.thebomb.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.mosbee1.thebomb.TheBombApp
import io.github.mosbee1.thebomb.work.BackgroundWork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Restarts the watcher after boot or app update - only when the user armed
 * the app. Also re-asserts WorkManager schedules (belt and braces;
 * WorkManager persists across reboots on its own).
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val container = (appContext as TheBombApp).container
                val settings = container.settingsRepository.current()
                BackgroundWork.ensureScheduled(appContext)
                if (settings.janitorEnabled) {
                    ScreenshotWatcherService.start(appContext)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
