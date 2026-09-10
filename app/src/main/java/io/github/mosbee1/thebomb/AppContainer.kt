package io.github.mosbee1.thebomb

import android.content.Context
import io.github.mosbee1.thebomb.data.SettingsRepository
import io.github.mosbee1.thebomb.data.ScreenshotRepository
import io.github.mosbee1.thebomb.data.local.TheBombDatabase
import io.github.mosbee1.thebomb.util.SystemTimeProvider
import io.github.mosbee1.thebomb.util.TimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val timeProvider: TimeProvider = SystemTimeProvider

    private val database: TheBombDatabase by lazy {
        TheBombDatabase.get(appContext)
    }

    val screenshotRepository: ScreenshotRepository by lazy {
        ScreenshotRepository(appContext, database.screenshotDao(), timeProvider)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(database.appSettingsDao())
    }
}
