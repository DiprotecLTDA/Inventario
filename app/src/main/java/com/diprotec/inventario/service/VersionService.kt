package com.diprotec.inventario.service

import android.content.Context
import android.os.Build
import android.util.Log
import com.diprotec.inventario.core.config.SettingsManager
import com.diprotec.inventario.core.network.ProtectedHeadersBuilder
import com.diprotec.inventario.data.remote.api.ApiService
import com.diprotec.inventario.data.remote.dto.VersionCheckDataDto
import com.diprotec.inventario.data.remote.dto.VersionDataDto
import com.diprotec.inventario.data.remote.dto.VersionEntradaRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VersionService @Inject constructor(
    private val api: ApiService,
    private val settings: SettingsManager,
    private val headersBuilder: ProtectedHeadersBuilder,
    @ApplicationContext private val context: Context
) {

    fun getCurrentVersionName(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "0.0.0"
        } catch (e: Exception) {
            "0.0.0"
        }
    }

    fun getCurrentVersionNameForApi(): String {
        return getCurrentVersionName()
            .trim()
            .replace(".", ",")
    }

    fun getCurrentVersionCode(): String {
        return try {
            val pm = context.packageManager
            val packageName = context.packageName
            val packageInfo = pm.getPackageInfo(packageName, 0)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toString()
            }
        } catch (e: Exception) {
            "0"
        }
    }

    fun getCurrentVersionCodeInt(): Int {
        return getCurrentVersionCode().trim().toIntOrNull() ?: 0
    }

    suspend fun checkVersion(): VersionCheckDataDto? {
        return try {
            val empresaRut = settings.empresaRut.value.trim()
            require(empresaRut.isNotBlank()) { "Empresa RUT no configurado" }

            val versionActual = getCurrentVersionCodeInt()
            val versionActualNombre = getCurrentVersionName().trim()
            val fabricante = Build.MANUFACTURER.orEmpty().trim()
            val androidVersion = Build.VERSION.RELEASE.orEmpty().trim()
            val modelo = Build.MODEL.orEmpty().trim()

            val relativeUrl =
                "/api/website/v1/versiones/$empresaRut/GetVersion"

            Log.d("VERSION_SYNC", "empresaRut=$empresaRut")
            Log.d("VERSION_SYNC", "versionActual=$versionActual")
            Log.d("VERSION_SYNC", "versionActualNombre=$versionActualNombre")
            Log.d("VERSION_SYNC", "fabricante=$fabricante")
            Log.d("VERSION_SYNC", "androidVersion=$androidVersion")
            Log.d("VERSION_SYNC", "modelo=$modelo")
            Log.d("VERSION_SYNC", "relativeUrl=$relativeUrl")

            val headers = headersBuilder.build(
                method = "POST",
                relativeUrl = relativeUrl
            )

            val request = VersionEntradaRequest(
                versionActual = versionActual,
                versionActualNombre = versionActualNombre,
                fabricante = fabricante,
                androidVersion = androidVersion,
                modelo = modelo
            )

            val response = api.getVersion(
                empresaRUT = empresaRut,
                apiKey = headers.apiKey,
                authorization = headers.authorization,
                deviceSession = headers.deviceSession,
                deviceSignature = headers.deviceSignature,
                deviceTimestamp = headers.deviceTimestamp,
                body = request
            )

            Log.d("VERSION_SYNC", "estado=${response.estado}")
            Log.d("VERSION_SYNC", "respuesta=${response.respuesta}")
            Log.d("VERSION_SYNC", "codigoError=${response.codigoError}")
            Log.d("VERSION_SYNC", "correlationId=${response.correlationId}")

            if (response.estado != 200) return null

            val data = response.data
            val version = data?.version

            Log.d("VERSION_SYNC", "puedeOperar=${data?.puedeOperar}")
            Log.d("VERSION_SYNC", "requiereActualizacion=${data?.requiereActualizacion}")
            Log.d("VERSION_SYNC", "actualizacionObligatoria=${data?.actualizacionObligatoria}")
            Log.d("VERSION_SYNC", "versionName=${version?.versionName}")
            Log.d("VERSION_SYNC", "versionCode=${version?.versionCode}")
            Log.d("VERSION_SYNC", "forceUpdate=${version?.forceUpdate}")
            Log.d("VERSION_SYNC", "isActive=${version?.isActive}")
            Log.d("VERSION_SYNC", "isPublished=${version?.isPublished}")
            Log.d("VERSION_SYNC", "fabricanteRemoto=${version?.fabricante}")
            Log.d("VERSION_SYNC", "androidVersionRemoto=${version?.androidVersion}")
            Log.d("VERSION_SYNC", "modeloRemoto=${version?.modelo}")
            Log.d("VERSION_SYNC", "apkFileName=${version?.apkFileName}")
            Log.d("VERSION_SYNC", "apkRelativePath=${version?.apkRelativePath}")
            Log.d("VERSION_SYNC", "fileSizeBytes=${version?.fileSizeBytes}")

            data
        } catch (t: retrofit2.HttpException) {
            Log.e("VERSION_SYNC", "HTTP ${t.code()} consultando versión", t)
            null
        } catch (t: Throwable) {
            Log.e("VERSION_SYNC", "Error consultando versión", t)
            null
        }
    }

    fun hasNewVersion(data: VersionCheckDataDto?): Boolean {
        val version = data?.version ?: return false

        if (!version.isActiveBool() || !version.isPublishedBool()) return false

        return version.versionCodeInt() > getCurrentVersionCodeInt()
    }

    fun isUpdateRequired(data: VersionCheckDataDto?): Boolean {
        val version = data?.version ?: return false
        val localVersionCode = getCurrentVersionCodeInt()
        val remoteVersionCode = version.versionCodeInt()

        if (remoteVersionCode <= localVersionCode) return false
        if (!version.isActiveBool() || !version.isPublishedBool()) return false

        return data.actualizacionObligatoriaBool() ||
                version.isForceUpdate() ||
                localVersionCode < version.minSupportedVersionCodeInt()
    }

    fun canOperate(data: VersionCheckDataDto?): Boolean {
        return data?.puedeOperarBool() ?: true
    }

    fun buildApkUrl(version: VersionDataDto?): String? {
        val apkRelativePath = version?.apkRelativePath?.trim().orEmpty()
        val apkFileName = version?.apkFileName?.trim().orEmpty()

        if (apkRelativePath.isBlank() || apkFileName.isBlank()) {
            return null
        }

        val cleanPath = apkRelativePath.trimEnd('/')
        val cleanFileName = apkFileName.trimStart('/')

        return "$cleanPath/$cleanFileName"
    }
}