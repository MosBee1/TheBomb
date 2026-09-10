package io.github.mosbee1.thebomb.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.mosbee1.thebomb.TheBombApp
import io.github.mosbee1.thebomb.data.model.AppSettings
import io.github.mosbee1.thebomb.service.ScreenshotWatcherService
import io.github.mosbee1.thebomb.util.Permissions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Snapshot of every runtime permission / special-access grant the app uses. */
data class PermissionsState(
    val images: Boolean = false,
    val notifications: Boolean = false,
    val fullScreenIntent: Boolean = true,
    val batteryOptimization: Boolean = false,
    val overlay: Boolean = false,
)

/**
 * App-scoped state shared across tabs: settings, permission state, and the
 * watcher lifecycle for the master switch. Held at activity scope so all
 * tabs and the onboarding flow observe the same truth.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as TheBombApp).container

    val settings: StateFlow<AppSettings> = container.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _permissions = MutableStateFlow(PermissionsState())
    val permissions: StateFlow<PermissionsState> = _permissions.asStateFlow()

    init {
        refreshPermissions()
        // Covers force-stop + "app restarted while armed".
        viewModelScope.launch {
            if (container.settingsRepository.current().janitorEnabled) {
                ScreenshotWatcherService.start(application)
            }
        }
    }

    /** Re-evaluates every grant; called on resume and after any request. */
    fun refreshPermissions() {
        val context = getApplication<Application>()
        _permissions.value = PermissionsState(
            images = Permissions.hasImageRead(context),
            notifications = Permissions.canPostNotifications(context),
            fullScreenIntent = Permissions.canUseFullScreenIntent(context),
            batteryOptimization = Permissions.isIgnoringBatteryOptimizations(context),
            overlay = Permissions.canDrawOverOtherApps(context),
        )
    }

    fun completeOnboarding() {
        viewModelScope.launch { container.settingsRepository.completeOnboarding() }
    }

    fun setJanitorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            container.settingsRepository.setJanitorEnabled(enabled)
            if (enabled) {
                ScreenshotWatcherService.start(getApplication())
            } else {
                ScreenshotWatcherService.stop(getApplication())
            }
        }
    }
}
