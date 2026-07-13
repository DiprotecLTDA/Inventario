package com.diprotec.inventario.data.repository

import com.diprotec.inventario.core.config.SettingsManager
import com.diprotec.inventario.data.local.dao.ProductDao
import com.diprotec.inventario.data.local.dao.LocationDao
import com.diprotec.inventario.data.local.entity.LocationEntity
import com.diprotec.inventario.data.local.entity.InventoryRemoteEntity
import com.diprotec.inventario.data.local.dao.InventoryDao
import com.diprotec.inventario.data.local.entity.InventoryEntity
import com.diprotec.inventario.data.local.inventory.InventoryGroupedRow
import com.diprotec.inventario.data.local.dao.InventoryItemDao
import com.diprotec.inventario.data.local.entity.InventoryItemEntity
import com.diprotec.inventario.data.local.inventory.InventoryStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class InventoryRepository @Inject constructor(
    private val inventoryDao: InventoryDao,
    private val inventoryItemDao: InventoryItemDao,
    private val productDao: ProductDao,
    private val locationDao: LocationDao,
    private val settings: SettingsManager
) {

    fun observeUbicacionesActivas(): Flow<List<LocationEntity>> =
        locationDao.observeActivas()

    suspend fun findDescriptionByBarcode(barcode: String): String {
        return productDao.findByCodigo(barcode.trim())
            ?.descripcion
            ?.takeIf { it.isNotBlank() }
            ?: "Producto no registrado"
    }

    suspend fun createInventoryFromRemote(
        remote: InventoryRemoteEntity,
        rutUsuario: String
    ): Long {
        val usuario = rutUsuario.trim()
        val remoteId = remote.id.trim()

        require(usuario.isNotBlank()) {
            "No hay usuario logueado para crear inventario"
        }

        require(remoteId.isNotBlank()) {
            "Inventario remoto inválido"
        }

        val existing = inventoryDao.getByRemoteIdAndUsuario(remoteId, usuario)
        if (existing != null) return existing.id

        return inventoryDao.insert(
            InventoryEntity(
                remoteInventoryId = remoteId,
                rutUsuario = usuario,
                name = remote.descripcion.orEmpty(),
                fecha = remote.fecha,
                hora = remote.hora,
                desde = remote.desde,
                hasta = remote.hasta,
                rutAdministrador = remote.rutAdministrador,
                rutEmpresa = remote.rutEmpresa,
                status = InventoryStatus.PENDING.name
            )
        )
    }

    suspend fun getInventoryById(inventoryId: Long): InventoryEntity? =
        inventoryDao.getById(inventoryId)

    fun observeInventoriesByStatus(status: InventoryStatus): Flow<List<InventoryEntity>> =
        inventoryDao.observeByStatus(status.name)

    fun observeInventoriesByStatusAndUsuario(
        status: InventoryStatus,
        rutUsuario: String
    ): Flow<List<InventoryEntity>> =
        inventoryDao.observeByStatusAndUsuario(status.name, rutUsuario.trim())

    fun observeAllInventories(): Flow<List<InventoryEntity>> =
        inventoryDao.observeAll()

    fun observeAllInventoriesByUsuario(rutUsuario: String): Flow<List<InventoryEntity>> =
        inventoryDao.observeAllByUsuario(rutUsuario.trim())

    fun observePendingInventories(): Flow<List<InventoryEntity>> =
        observeInventoriesByStatus(InventoryStatus.PENDING)

    suspend fun getInventoryByRemoteId(remoteInventoryId: String): InventoryEntity? =
        inventoryDao.getByRemoteId(remoteInventoryId)

    suspend fun getInventoryByRemoteIdAndUsuario(
        remoteInventoryId: String,
        rutUsuario: String
    ): InventoryEntity? =
        inventoryDao.getByRemoteIdAndUsuario(
            remoteInventoryId = remoteInventoryId,
            rutUsuario = rutUsuario.trim()
        )

    suspend fun expirePendingInventories() {
        val pending = inventoryDao.getByStatus(InventoryStatus.PENDING.name)

        pending
            .filter { isExpired(it.hasta) }
            .forEach { inventory ->
                inventoryDao.updateStatus(
                    inventoryId = inventory.id,
                    status = InventoryStatus.EXPIRED.name,
                    finishedAt = System.currentTimeMillis()
                )
            }
    }

    suspend fun expirePendingInventoriesByUsuario(rutUsuario: String) {
        val pending = inventoryDao.getByStatusAndUsuario(
            status = InventoryStatus.PENDING.name,
            rutUsuario = rutUsuario.trim()
        )

        pending
            .filter { isExpired(it.hasta) }
            .forEach { inventory ->
                inventoryDao.updateStatus(
                    inventoryId = inventory.id,
                    status = InventoryStatus.EXPIRED.name,
                    finishedAt = System.currentTimeMillis()
                )
            }
    }

    fun isInventoryVisible(inventory: InventoryEntity): Boolean =
        !isExpired(inventory.hasta)

    suspend fun finalizeInventory(inventoryId: Long) {
        inventoryDao.updateStatus(
            inventoryId = inventoryId,
            status = InventoryStatus.FINISHED.name,
            finishedAt = System.currentTimeMillis()
        )
    }

    suspend fun registerInventoryItem(
        inventoryId: Long,
        ubicacionId: String,
        ubicacionNombre: String,
        barcode: String,
        quantity: Double,
        unitMeasure: String,
        unitMeasureId: String,
        rutUsuario: String
    ) {
        val inventory = inventoryDao.getById(inventoryId) ?: return
        val now = Date()
        val normalizedBarcode = barcode.trim()
        val description = findDescriptionByBarcode(normalizedBarcode)

        val dispositivoId = settings.deviceId.value.trim()
        require(dispositivoId.isNotBlank()) {
            "DispositivoId no configurado. Debe reactivar el dispositivo."
        }

        inventoryItemDao.insert(
            InventoryItemEntity(
                inventoryId = inventoryId,
                remoteInventoryId = inventory.remoteInventoryId,
                ubicacionId = ubicacionId,
                ubicacionNombre = ubicacionNombre,
                dispositivoId = dispositivoId,
                barcode = normalizedBarcode,
                description = description,
                quantity = quantity,
                unitMeasure = unitMeasure,
                unitMeasureId = unitMeasureId,
                fecha = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now),
                hora = SimpleDateFormat("HH:mm:ss", Locale.US).format(now),
                rutUsuario = rutUsuario,
                sincronizado = false
            )
        )
    }

    fun observeInventoryItems(inventoryId: Long): Flow<List<InventoryItemEntity>> =
        inventoryItemDao.observeInventoryItems(inventoryId)

    fun observeGroupedInventoryItems(inventoryId: Long): Flow<List<InventoryGroupedRow>> =
        inventoryItemDao.observeGroupedInventoryItems(inventoryId)

    suspend fun deleteInventoryItem(
        inventoryId: Long,
        itemId: Long
    ) {
        val inventory = inventoryDao.getById(inventoryId)
            ?: throw IllegalStateException("No se encontró el inventario")

        if (inventory.status == InventoryStatus.FINISHED.name) {
            throw IllegalStateException("No se pueden eliminar capturas de un inventario finalizado")
        }

        inventoryItemDao.deleteById(itemId)
    }

    suspend fun getCapturasPendientesSincronizar(): List<InventoryItemEntity> =
        inventoryItemDao.getPendientesSincronizar()

    suspend fun markCapturasSincronizadas(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            inventoryItemDao.markSincronizados(ids)
        }
    }

    suspend fun getInventariosPendientesFinishSync(): List<InventoryEntity> =
        inventoryDao.getPendingFinishSync(InventoryStatus.FINISHED.name)

    suspend fun markFinishSynced(inventoryId: Long) {
        inventoryDao.markFinishSynced(
            inventoryId = inventoryId,
            finishSyncedAt = System.currentTimeMillis()
        )
    }

    private fun isExpired(hasta: String?): Boolean {
        if (hasta.isNullOrBlank()) return false

        val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.US).apply {
            isLenient = false
        }

        val limit = runCatching { formatter.parse(hasta) }.getOrNull()
            ?: return false

        val today = formatter.parse(formatter.format(Date())) ?: Date()

        return today.after(limit)
    }
}