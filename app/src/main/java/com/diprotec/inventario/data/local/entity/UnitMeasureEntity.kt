package com.diprotec.inventario.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unidad_medidas")
data class UnitMeasureEntity(
    @PrimaryKey val id: String,
    val nombre: String?,
    val valor: String?,
    val predeterminado: Boolean,
    val vigente: Boolean,
    val rutEmpresa: String?
)