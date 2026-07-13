package com.diprotec.inventario.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_logs",
    indices = [
        Index(value = ["sentAt"]),
        Index(value = ["remoteInventoryId"]),
        Index(value = ["remoteInventoryId", "sentAt"]),
        Index(value = ["result"]),
        Index(value = ["eventType"])
    ]
)
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val remoteInventoryId: String,
    val inventoryName: String,
    val eventType: String,
    val capturesCount: Int,
    val inventoryStatus: String,
    val result: String,
    val connectionMode: String,
    val sentAt: Long,
    val message: String? = null
)