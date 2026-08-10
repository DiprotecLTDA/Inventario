package com.diprotec.inventario.service

import android.content.Context
import android.util.Log
import com.diprotec.inventario.core.config.SettingsManager
import com.diprotec.inventario.core.device.GetSerialNumber
import com.diprotec.inventario.core.message.AppMessages
import com.diprotec.inventario.core.network.ApiCallExecutor
import com.diprotec.inventario.core.network.ApiException
import com.diprotec.inventario.core.network.ProtectedHeadersBuilder
import com.diprotec.inventario.data.local.entity.SyncLogEntity
import com.diprotec.inventario.data.local.inventory.InventoryStatus
import com.diprotec.inventario.data.mappers.toEntity
import com.diprotec.inventario.data.remote.api.ApiService
import com.diprotec.inventario.data.remote.dto.FinalizarInventarioRequest
import com.diprotec.inventario.data.remote.dto.RegistroInventarioCapturaRequest
import com.diprotec.inventario.data.remote.dto.RegistroInventarioRequest
import com.diprotec.inventario.data.repository.InventoryRemoteRepository
import com.diprotec.inventario.data.repository.InventoryRepository
import com.diprotec.inventario.data.repository.LocationRepository
import com.diprotec.inventario.data.repository.ProductRepository
import com.diprotec.inventario.data.repository.RuleRepository
import com.diprotec.inventario.data.repository.SyncLogRepository
import com.diprotec.inventario.data.repository.UnitMeasureRepository
import com.diprotec.inventario.data.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

@Singleton
class SyncService @Inject constructor(
    private val userRepository: UserRepository,
    private val ruleRepository: RuleRepository,
    private val locationRepository: LocationRepository,
    private val productRepository: ProductRepository,
    private val unitMeasureRepository: UnitMeasureRepository,
    private val inventoryRemoteRepository: InventoryRemoteRepository,
    private val versionService: VersionService,
    private val api: ApiService,
    private val apiCallExecutor: ApiCallExecutor,
    private val settings: SettingsManager,
    private val inventoryRepository: InventoryRepository,
    private val syncLogRepository: SyncLogRepository,
    private val headersBuilder: ProtectedHeadersBuilder,
    @ApplicationContext private val context: Context
) {

    private val deviceSessionMutex = Mutex()

    private fun authorizationHeader(): String {
        val token = settings.authToken.value.trim()
        require(token.isNotBlank()) { AppMessages.Configuration.AUTHORIZATION_NO_CONFIGURADO }
        return "Bearer $token"
    }

    private fun requireConfigured() {
        val empresaRut = settings.empresaRut.value.trim()
        val apiKey = settings.apiKey.value.trim()
        val authToken = settings.authToken.value.trim()

        require(empresaRut.isNotBlank()) { AppMessages.Configuration.EMPRESA_RUT_NO_CONFIGURADO }
        require(apiKey.isNotBlank()) { AppMessages.Configuration.API_KEY_NO_CONFIGURADA }
        require(authToken.isNotBlank()) { AppMessages.Configuration.AUTHORIZATION_NO_CONFIGURADO }
    }

    private fun requireActivated() {
        require(settings.deviceActivated.value) {
            "El dispositivo no está activado"
        }
    }

    suspend fun warmUpDeviceSession(): String = ensureDeviceSession()

    private suspend fun ensureDeviceSession(): String = withContext(Dispatchers.IO) {
        requireConfigured()

        settings.deviceSession.value.let { existing ->
            if (existing.isNotBlank()) return@withContext existing
        }

        deviceSessionMutex.withLock {
            settings.deviceSession.value.let { existing ->
                if (existing.isNotBlank()) return@withLock existing
            }

            val empresaRut = settings.empresaRut.value.trim()
            val apiKey = settings.apiKey.value.trim()

            val res = GetSerialNumber(context)
            val serial = res.serial.orEmpty().trim()

            if (serial.isBlank()) {
                throw IllegalStateException("No se pudo obtener serial: ${res.errMsg}")
            }

            val jsonString = "\"$serial\""
            val body = jsonString.toRequestBody("application/json".toMediaType())

            val response = apiCallExecutor.execute {
                api.loginDispositivo(
                    empresaRUT = empresaRut,
                    apiKey = apiKey,
                    authorization = authorizationHeader(),
                    serialNumberPlain = body
                )
            }

            val deviceSession = response.data.orEmpty()
            settings.saveDeviceSession(deviceSession)
            deviceSession
        }
    }

    private suspend fun <D, E> syncCatalog(
        label: String,
        fetch: suspend () -> List<D>,
        map: (D) -> E,
        replace: suspend (List<E>) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        requireConfigured()
        requireActivated()
        ensureDeviceSession()

        val mapped = fetch().map(map)
        replace(mapped)

        Log.d(TAG, "$label: ${mapped.size}")
        mapped.size
    }

    suspend fun syncUsers(): Int = syncCatalog(
        label = "Usuarios sincronizados",
        fetch = userRepository::fetchRemoteUsers,
        map = { it.toEntity() },
        replace = userRepository::replaceAllUsers
    )

    suspend fun syncReglas(): Int = syncCatalog(
        label = "Reglas sincronizadas",
        fetch = ruleRepository::fetchRemoteReglas,
        map = { it.toEntity() },
        replace = ruleRepository::replaceAllReglas
    )

    suspend fun syncUbicaciones(): Int = syncCatalog(
        label = "Ubicaciones sincronizadas",
        fetch = locationRepository::fetchRemoteUbicaciones,
        map = { it.toEntity() },
        replace = locationRepository::replaceAllUbicaciones
    )

    suspend fun syncProductos(): Int = syncCatalog(
        label = "Productos sincronizados",
        fetch = productRepository::fetchRemoteProductos,
        map = { it.toEntity() },
        replace = productRepository::replaceAllProductos
    )

    suspend fun syncUnidadMedidas(): Int = syncCatalog(
        label = "UnidadMedidas sincronizadas",
        fetch = unitMeasureRepository::fetchRemoteUnidadMedidas,
        map = { it.toEntity() },
        replace = unitMeasureRepository::replaceAllUnidadMedidas
    )

    suspend fun syncInventariosRemotos(): Int = withContext(Dispatchers.IO) {
        requireConfigured()
        requireActivated()
        ensureDeviceSession()

        val remote = inventoryRemoteRepository.fetchRemoteInventarios()
        val inventarios = remote.map { it.toEntity() }

        val usuarios = remote.flatMap { inventario ->
            inventario.usuarios.orEmpty().map { usuario ->
                usuario.toEntity()
            }
        }

        inventoryRemoteRepository.replaceAllInventarios(
            inventarios = inventarios,
            usuarios = usuarios
        )

        Log.d(TAG, "Inventarios remotos sincronizados: ${inventarios.size}")
        inventarios.size
    }

    suspend fun checkStartupUpdateAndSavePending(): VersionCheckStartupResult =
        withContext(Dispatchers.IO) {
            requireConfigured()
            requireActivated()
            ensureDeviceSession()

            val data = versionService.checkVersion()
            val version = data?.version

            val apiRequiresUpdate = data?.requiereActualizacionBool() == true
            val hasNewVersion = versionService.hasNewVersion(data) || apiRequiresUpdate
            val isMandatory = data?.actualizacionObligatoriaBool() == true

            val updateUrl = if (hasNewVersion) {
                versionService.buildApkUrl(version)
            } else {
                null
            }

            val apkFileName = version?.apkFileName?.trim().orEmpty()

            if (hasNewVersion && !updateUrl.isNullOrBlank() && apkFileName.isNotBlank()) {
                settings.savePendingUpdate(
                    mandatory = isMandatory,
                    downloadId = -1L,
                    apkFileName = apkFileName,
                    apkUrl = updateUrl,
                    apkDownloaded = false
                )

                Log.d(
                    TAG,
                    "Startup update detected. mandatory=$isMandatory, apk=$apkFileName, url=$updateUrl"
                )
            } else {
                settings.clearPendingUpdate()

                Log.d(
                    TAG,
                    "Startup update not required. " +
                            "apiRequiresUpdate=$apiRequiresUpdate, " +
                            "hasNewVersion=$hasNewVersion, " +
                            "apkFileName=$apkFileName, " +
                            "updateUrl=$updateUrl"
                )
            }

            VersionCheckStartupResult(
                versionName = version?.versionName,
                hasNewVersion = hasNewVersion,
                mandatory = isMandatory,
                apkFileName = apkFileName,
                apkUrl = updateUrl
            )
        }

    suspend fun syncRegistroInventarios(): Int = withContext(Dispatchers.IO) {
        requireConfigured()
        requireActivated()
        ensureDeviceSession()

        val empresaRut = settings.empresaRut.value.trim()
        val pendientes = inventoryRepository.getCapturasPendientesSincronizar()

        if (pendientes.isEmpty()) {
            Log.d(TAG, "No hay capturas pendientes para sincronizar")
            return@withContext 0
        }

        val groupedByInventory = pendientes.groupBy {
            it.remoteInventoryId to it.rutUsuario
        }

        var totalSincronizadas = 0
        var primerFallo: Throwable? = null

        groupedByInventory.forEach { (key, capturas) ->
            val inventarioId = key.first
            val rutUsuario = key.second

            val inventory = inventoryRepository.getInventoryByRemoteIdAndUsuario(
                remoteInventoryId = inventarioId,
                rutUsuario = rutUsuario
            )

            val inventoryName = inventory?.name?.takeIf { it.isNotBlank() }
                ?: "Inventario $inventarioId"

            val inventoryStatus = inventory?.status ?: InventoryStatus.PENDING.name

            // Se envía en bloques acotados para no arriesgar timeout/rechazo del servidor
            // con inventarios grandes. Cada bloque se marca sincronizado por separado: si un
            // bloque falla, el progreso de los bloques/grupos ya confirmados no se descarta.
            capturas.chunked(CAPTURAS_POR_ENVIO).forEach { bloque ->
                try {
                    val relativeUrl =
                        "/api/website/v1/inventarios/$empresaRut/SendRegistroInventario"

                    val headers = headersBuilder.build(
                        method = "POST",
                        relativeUrl = relativeUrl
                    )

                    val request = RegistroInventarioRequest(
                        InventarioId = inventarioId,
                        Capturas = bloque.map { item ->
                            RegistroInventarioCapturaRequest(
                                UbicacionId = item.ubicacionId,
                                DispositivoId = item.dispositivoId,
                                ProductoCodigo = item.barcode,
                                Cantidad = formatCantidad(item.quantity),
                                UnidadMedidaId = item.unitMeasureId.ifBlank { item.unitMeasure },
                                Fecha = item.fecha,
                                Hora = item.hora,
                                RutUsuario = item.rutUsuario
                            )
                        }
                    )

                    val response = apiCallExecutor.execute {
                        api.sendRegistroInventario(
                            empresaRUT = empresaRut,
                            apiKey = headers.apiKey,
                            authorization = headers.authorization,
                            deviceSession = headers.deviceSession,
                            deviceSignature = headers.deviceSignature,
                            deviceTimestamp = headers.deviceTimestamp,
                            body = request
                        )
                    }

                    val ids = bloque.map { it.id }
                    inventoryRepository.markCapturasSincronizadas(ids)
                    totalSincronizadas += ids.size
                    val successMessage = response.respuesta
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: AppMessages.ApiSync.CAPTURAS_ENVIADAS_CORRECTAMENTE

                    insertSyncLog(
                        remoteInventoryId = inventarioId,
                        inventoryName = inventoryName,
                        eventType = EVENT_CAPTURES_SENT,
                        capturesCount = ids.size,
                        inventoryStatus = inventoryStatus,
                        result = RESULT_ENVIADO,
                        connectionMode = MODE_ONLINE_API,
                        message = successMessage
                    )

                    Log.d(
                        TAG,
                        "Inventario $inventarioId sincronizado. RutUsuario=$rutUsuario Capturas=${ids.size}"
                    )
                } catch (t: Throwable) {
                    insertSyncLog(
                        remoteInventoryId = inventarioId,
                        inventoryName = inventoryName,
                        eventType = EVENT_CAPTURES_FAILED,
                        capturesCount = bloque.size,
                        inventoryStatus = inventoryStatus,
                        result = RESULT_ERROR,
                        connectionMode = connectionModeForError(t),
                        message = t.message ?: t::class.java.simpleName
                    )

                    if (primerFallo == null) {
                        primerFallo = t
                    }
                }
            }
        }

        Log.d(TAG, "Capturas sincronizadas: $totalSincronizadas")

        primerFallo?.let { throw it }

        totalSincronizadas
    }

    suspend fun syncFinishInventarios(): Int = withContext(Dispatchers.IO) {
        requireConfigured()
        requireActivated()
        ensureDeviceSession()

        val empresaRut = settings.empresaRut.value.trim()
        val pendientes = inventoryRepository.getInventariosPendientesFinishSync()

        if (pendientes.isEmpty()) {
            Log.d(TAG, "No hay inventarios finalizados pendientes de sincronizar")
            return@withContext 0
        }

        var total = 0

        pendientes.forEach { inventory ->
            val inventarioId = inventory.remoteInventoryId.toLongOrNull()

            if (inventarioId == null) {
                Log.w(
                    TAG,
                    "No se puede finalizar remoto. remoteInventoryId inválido=${inventory.remoteInventoryId}"
                )
                return@forEach
            }

            val rutUsuario = inventory.rutUsuario
                .ifBlank {
                    throw IllegalStateException(
                        "Inventario ${inventory.remoteInventoryId} sin rut de usuario para finalizar"
                    )
                }

            try {
                val relativeUrl =
                    "/api/website/v1/inventarios/$empresaRut/FinishInventario"

                val headers = headersBuilder.build(
                    method = "POST",
                    relativeUrl = relativeUrl
                )

                val response = apiCallExecutor.execute {
                    api.finishInventario(
                        empresaRUT = empresaRut,
                        apiKey = headers.apiKey,
                        authorization = headers.authorization,
                        deviceSession = headers.deviceSession,
                        deviceSignature = headers.deviceSignature,
                        deviceTimestamp = headers.deviceTimestamp,
                        body = FinalizarInventarioRequest(
                            InventarioId = inventarioId,
                            UsuarioRUT = rutUsuario
                        )
                    )
                }

                inventoryRepository.markFinishSynced(inventory.id)
                total++
                val successMessage = response.respuesta
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: AppMessages.ApiSync.INVENTARIO_FINALIZADO_CORRECTAMENTE

                insertSyncLog(
                    remoteInventoryId = inventory.remoteInventoryId,
                    inventoryName = inventory.name.ifBlank {
                        "Inventario ${inventory.remoteInventoryId}"
                    },
                    eventType = EVENT_INVENTORY_FINISHED,
                    capturesCount = 0,
                    inventoryStatus = InventoryStatus.FINISHED.name,
                    result = RESULT_ENVIADO,
                    connectionMode = MODE_ONLINE_API,
                    message = successMessage
                )

                Log.d(
                    TAG,
                    "FinishInventario OK. localId=${inventory.id}, remoteId=${inventory.remoteInventoryId}, rutUsuario=$rutUsuario"
                )
            } catch (t: Throwable) {
                insertSyncLog(
                    remoteInventoryId = inventory.remoteInventoryId,
                    inventoryName = inventory.name.ifBlank {
                        "Inventario ${inventory.remoteInventoryId}"
                    },
                    eventType = EVENT_FINISH_FAILED,
                    capturesCount = 0,
                    inventoryStatus = inventory.status,
                    result = RESULT_ERROR,
                    connectionMode = connectionModeForError(t),
                    message = t.message ?: t::class.java.simpleName
                )

                throw t
            }
        }

        total
    }

    suspend fun finishInventarioRemoto(
        inventoryId: Long,
        usuarioRut: String
    ): Boolean = withContext(Dispatchers.IO) {
        requireConfigured()
        requireActivated()
        ensureDeviceSession()

        val inventory = inventoryRepository.getInventoryById(inventoryId)
            ?: throw IllegalStateException("No se encontró el inventario local")

        val inventarioRemotoId = inventory.remoteInventoryId.toLongOrNull()
            ?: throw IllegalStateException("remoteInventoryId inválido: ${inventory.remoteInventoryId}")

        val empresaRut = settings.empresaRut.value.trim()

        try {
            val relativeUrl =
                "/api/website/v1/inventarios/$empresaRut/FinishInventario"

            val headers = headersBuilder.build(
                method = "POST",
                relativeUrl = relativeUrl
            )

            val response = apiCallExecutor.execute {
                api.finishInventario(
                    empresaRUT = empresaRut,
                    apiKey = headers.apiKey,
                    authorization = headers.authorization,
                    deviceSession = headers.deviceSession,
                    deviceSignature = headers.deviceSignature,
                    deviceTimestamp = headers.deviceTimestamp,
                    body = FinalizarInventarioRequest(
                        InventarioId = inventarioRemotoId,
                        UsuarioRUT = usuarioRut
                    )
                )
            }

            inventoryRepository.markFinishSynced(inventory.id)
            val successMessage = response.respuesta
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: AppMessages.ApiSync.INVENTARIO_FINALIZADO_CORRECTAMENTE

            insertSyncLog(
                remoteInventoryId = inventory.remoteInventoryId,
                inventoryName = inventory.name.ifBlank {
                    "Inventario ${inventory.remoteInventoryId}"
                },
                eventType = EVENT_INVENTORY_FINISHED,
                capturesCount = 0,
                inventoryStatus = InventoryStatus.FINISHED.name,
                result = RESULT_ENVIADO,
                connectionMode = MODE_ONLINE_API,
                message = successMessage
            )

            true
        } catch (t: Throwable) {
            insertSyncLog(
                remoteInventoryId = inventory.remoteInventoryId,
                inventoryName = inventory.name.ifBlank {
                    "Inventario ${inventory.remoteInventoryId}"
                },
                eventType = EVENT_FINISH_FAILED,
                capturesCount = 0,
                inventoryStatus = inventory.status,
                result = RESULT_ERROR,
                connectionMode = connectionModeForError(t),
                message = t.message ?: t::class.java.simpleName
            )

            throw t
        }
    }

    suspend fun syncAllInventarioPendiente(): InventarioSyncSummary =
        withContext(Dispatchers.IO) {
            val capturas = runCatching { syncRegistroInventarios() }
                .onFailure { Log.e(TAG, "syncRegistroInventarios failed", it) }
                .getOrDefault(0)

            val finalizados = runCatching { syncFinishInventarios() }
                .onFailure { Log.e(TAG, "syncFinishInventarios failed", it) }
                .getOrDefault(0)

            InventarioSyncSummary(
                capturas = capturas,
                finalizados = finalizados
            )
        }

    private fun formatCantidad(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(Locale.US, "%.3f", value)
                .trimEnd('0')
                .trimEnd('.')
        }
    }

    suspend fun syncAllCatalogs(): SyncSummary = withContext(Dispatchers.IO) {
        requireConfigured()
        requireActivated()
        ensureDeviceSession()

        Log.d(TAG, "==== SYNC REGLAS ====")
        val reglas = runCatching { syncReglas() }
            .onFailure { Log.e(TAG, "syncReglas failed", it) }
            .getOrDefault(0)

        Log.d(TAG, "==== SYNC USUARIOS ====")
        val users = runCatching { syncUsers() }
            .onFailure { Log.e(TAG, "syncUsers failed", it) }
            .getOrDefault(0)

        Log.d(TAG, "==== SYNC UBICACIONES ====")
        val ubicaciones = runCatching { syncUbicaciones() }
            .onFailure { Log.e(TAG, "syncUbicaciones failed", it) }
            .getOrDefault(0)

        Log.d(TAG, "==== SYNC PRODUCTOS ====")
        val productos = runCatching { syncProductos() }
            .onFailure { Log.e(TAG, "syncProductos failed", it) }
            .getOrDefault(0)

        Log.d(TAG, "==== SYNC UNIDAD MEDIDAS ====")
        val unidadMedidas = runCatching { syncUnidadMedidas() }
            .onFailure { Log.e(TAG, "syncUnidadMedidas failed", it) }
            .getOrDefault(0)

        Log.d(TAG, "==== SYNC INVENTARIOS REMOTOS ====")
        val inventarios = runCatching { syncInventariosRemotos() }
            .onFailure { Log.e(TAG, "syncInventariosRemotos failed", it) }
            .getOrDefault(0)

        val summary = SyncSummary(
            users = users,
            reglas = reglas,
            ubicaciones = ubicaciones,
            productos = productos,
            unidadMedidas = unidadMedidas,
            inventarios = inventarios,
            version = null
        )

        Log.d(TAG, "Sync completa: $summary")
        summary
    }

    private fun connectionModeForError(t: Throwable): String {
        return if (
            t is IOException ||
            (t is ApiException && t.cause is IOException)
        ) {
            MODE_LOCAL_ROOM
        } else {
            MODE_ONLINE_API
        }
    }

    private suspend fun insertSyncLog(
        remoteInventoryId: String,
        inventoryName: String,
        eventType: String,
        capturesCount: Int,
        inventoryStatus: String,
        result: String,
        connectionMode: String,
        message: String?
    ) {
        runCatching {
            syncLogRepository.insert(
                SyncLogEntity(
                    remoteInventoryId = remoteInventoryId,
                    inventoryName = inventoryName,
                    eventType = eventType,
                    capturesCount = capturesCount,
                    inventoryStatus = inventoryStatus,
                    result = result,
                    connectionMode = connectionMode,
                    sentAt = System.currentTimeMillis(),
                    message = message
                )
            )
        }.onFailure {
            Log.e(TAG, "No se pudo guardar SyncLog", it)
        }
    }

    companion object {
        private const val TAG = "SyncService"
        private const val CAPTURAS_POR_ENVIO = 500

        private const val EVENT_CAPTURES_SENT = "CAPTURES_SENT"
        private const val EVENT_CAPTURES_FAILED = "CAPTURES_FAILED"
        private const val EVENT_INVENTORY_FINISHED = "INVENTORY_FINISHED"
        private const val EVENT_FINISH_FAILED = "FINISH_FAILED"

        private const val RESULT_ENVIADO = "ENVIADO"
        private const val RESULT_ERROR = "ERROR"

        private const val MODE_ONLINE_API = "ONLINE_API"
        private const val MODE_LOCAL_ROOM = "LOCAL_ROOM"
    }
}

data class SyncSummary(
    val users: Int,
    val reglas: Int,
    val ubicaciones: Int,
    val productos: Int,
    val unidadMedidas: Int,
    val inventarios: Int,
    val version: String?
)

data class InventarioSyncSummary(
    val capturas: Int,
    val finalizados: Int
)

data class VersionCheckStartupResult(
    val versionName: String?,
    val hasNewVersion: Boolean,
    val mandatory: Boolean,
    val apkFileName: String,
    val apkUrl: String?
)
