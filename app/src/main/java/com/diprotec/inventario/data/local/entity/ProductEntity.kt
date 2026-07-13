package com.diprotec.inventario.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "productos",
    indices = [
        Index(value = ["codigoSecundario"]),
        Index(value = ["descripcion"]),
        Index(value = ["vigente"]),
        Index(value = ["rutEmpresa"])
    ]
)
data class ProductEntity(
    @PrimaryKey val codigo: String,
    val codigoSecundario: String?,
    val descripcion: String?,
    val vigente: Boolean,
    val rutEmpresa: String?
)