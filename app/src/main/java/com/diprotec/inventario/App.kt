package com.diprotec.inventario

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.diprotec.inventario.core.config.SettingsManager
import com.diprotec.inventario.core.key.DeviceKeyInitializer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class App : Application(), Configuration.Provider {

    @Inject lateinit var settings: SettingsManager
    @Inject lateinit var deviceKeyInitializer: DeviceKeyInitializer
    @Inject lateinit var workerFactory: HiltWorkerFactory

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        settings.start(appScope)

        appScope.launch(Dispatchers.IO) {
            runCatching {
                deviceKeyInitializer.initializeAndLog()
            }.onFailure {
                Log.e("STARTUP", "DeviceKeyInitializer failed", it)
            }
        }
    }
}