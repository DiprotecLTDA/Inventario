package com.diprotec.inventario.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "network_usage_logs",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["source"]),
        Index(value = ["operation"]),
        Index(value = ["endpoint"])
    ]
)
data class NetworkUsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val createdAt: Long,
    val source: String,
    val operation: String,
    val method: String,
    val endpoint: String,
    val url: String,
    val requestBytes: Long,
    val responseBytes: Long,
    val totalBytes: Long,
    val statusCode: Int?,
    val durationMs: Long,
    val success: Boolean,
    val errorMessage: String?
)