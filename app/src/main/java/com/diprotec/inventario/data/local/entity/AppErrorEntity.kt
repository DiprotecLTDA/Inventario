package com.diprotec.inventario.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cola local de errores de funcionamiento de la app (excepciones, fallos de BD local, lógica
 * interna) pendientes de reportar por correo. NO incluye errores de red/API/HTTP: esos se
 * excluyen deliberadamente del reporte (ver `AppErrorReporter`).
 */
@Entity(
    tableName = "app_errors",
    indices = [
        Index(value = ["fingerprint"]),
        Index(value = ["localState"]),
        Index(value = ["createdAt"]),
        Index(value = ["fingerprint", "lastSeenAt"])
    ]
)
data class AppErrorEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val fingerprint: String,
    val localState: String,
    val createdAt: Long,
    val lastSeenAt: Long,
    val type: String,
    val severity: String,
    val module: String?,
    val action: String,
    val message: String?,
    val stackTrace: String?,
    val endpoint: String?,
    val screen: String?,
    val inventoryId: Long?,
    val workerName: String?,
    val deviceManufacturer: String?,
    val deviceModel: String?,
    val deviceSerial: String?,
    val appVersionName: String?,
    val appVersionCode: Long?,
    val attemptCount: Int = 0,
    val sendError: String? = null
)
