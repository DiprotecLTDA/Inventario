package com.diprotec.inventario.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory_items",
    indices = [
        Index(value = ["inventoryId"]),
        Index(value = ["inventoryId", "createdAt"]),
        Index(value = ["sincronizado", "remoteInventoryId", "createdAt"]),
        Index(value = ["remoteInventoryId"]),
        Index(value = ["rutUsuario"]),
        Index(value = ["inventoryId", "barcode", "ubicacionId"])
    ]
)
data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val inventoryId: Long,
    val remoteInventoryId: String,

    val ubicacionId: String,
    val ubicacionNombre: String,

    val dispositivoId: String,

    val barcode: String,
    val description: String,
    val quantity: Double,

    // Nombre para mostrar en pantalla
    val unitMeasure: String,

    // ID real para enviar al endpoint
    val unitMeasureId: String = "",

    val fecha: String,
    val hora: String,
    val rutUsuario: String,

    val createdAt: Long = System.currentTimeMillis(),
    val sincronizado: Boolean = false
)