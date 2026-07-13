package com.diprotec.inventario.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ubicaciones")
data class LocationEntity(
    @PrimaryKey val id: String,
    val nombre: String?,
    val vigente: Boolean,
    val rutEmpresa: String?
)