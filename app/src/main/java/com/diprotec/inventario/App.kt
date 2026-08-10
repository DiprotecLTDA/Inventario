package com.diprotec.inventario

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.diprotec.inventario.core.config.SettingsManager
import com.diprotec.inventario.core.error.AppErrorReporter
import com.diprotec.inventario.core.error.GlobalExceptionHandler
import com.diprotec.inventario.core.key.DeviceKeyInitializer
import com.diprotec.inventario.worker.ErrorEmailWorker
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
    @Inject lateinit var appErrorReporter: AppErrorReporter

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        settings.start(appScope)

        // Instanciar AppErrorReporter aquí lo adjunta a AppErrorReportingBridge de inmediato
        // (su init {} lo hace), y GlobalExceptionHandler lo usa para persistir crashes.
        GlobalExceptionHandler.install(appErrorReporter)

        // Reintenta enviar cualquier error que haya quedado pendiente de una sesión anterior
        // (incluidos los que no se llegaron a enviar antes de un crash).
        ErrorEmailWorker.enqueue(this)

        appScope.launch(Dispatchers.IO) {
            runCatching {
                deviceKeyInitializer.initializeAndLog()
            }.onFailure {
                Log.e("STARTUP", "DeviceKeyInitializer failed", it)
            }
        }
    }
}