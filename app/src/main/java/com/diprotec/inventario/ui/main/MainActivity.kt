package com.diprotec.inventario.ui.main

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.diprotec.inventario.core.config.SettingsManager
import com.diprotec.inventario.core.session.SessionManager
import com.diprotec.inventario.service.SessionLifecycleService
import com.diprotec.inventario.service.VersionService
import com.diprotec.inventario.ui.theme.InventarioTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settings: SettingsManager
    @Inject lateinit var session: SessionManager
    @Inject lateinit var versionService: VersionService

    private var versionCodeAtInstallStart: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startService(
            Intent(this, SessionLifecycleService::class.java)
        )

        window.statusBarColor = Color.parseColor("#C0392B")

        WindowCompat.getInsetsController(
            window,
            window.decorView
        ).isAppearanceLightStatusBars = false

        versionCodeAtInstallStart = versionService.getCurrentVersionCodeInt()

        setContent {
            InventarioTheme {
                AppNavHost(
                    settings = settings,
                    session = session
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
            session.touchIfValid()

            val hasPendingUpdate =
                settings.pendingUpdateMandatory.value ||
                        settings.pendingDownloadId.value > 0L ||
                        settings.apkDownloaded.value

            if (!hasPendingUpdate) return@launch

            val currentVersionCode = versionService.getCurrentVersionCodeInt()

            if (currentVersionCode > versionCodeAtInstallStart) {
                settings.clearPendingUpdate()
                versionCodeAtInstallStart = currentVersionCode
            }
        }
    }
}
