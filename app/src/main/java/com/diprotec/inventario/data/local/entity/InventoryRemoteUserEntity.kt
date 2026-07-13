package com.diprotec.inventario.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventario_usuarios_remotos")
data class InventoryRemoteUserEntity(
    @PrimaryKey val id: String,
    val inventarioId: String?,
    val rutUsuario: String?
)