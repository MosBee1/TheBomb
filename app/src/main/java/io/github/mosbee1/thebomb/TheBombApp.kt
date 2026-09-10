package io.github.mosbee1.thebomb

import android.app.Application
import io.github.mosbee1.thebomb.work.BackgroundWork
import kotlinx.coroutines.launch

class TheBombApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.applicationScope.launch {
            container.settingsRepository.initialize()
        }
        BackgroundWork.ensureScheduled(this)
    }
}
