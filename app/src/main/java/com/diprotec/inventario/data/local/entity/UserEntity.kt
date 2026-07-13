package com.diprotec.inventario.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val rut: String,
    val nombre: String,
    val email: String?,
    val telefono: String?,
    val perfil: String?,
    val vigente: Boolean,
    val perfilId: Int? = null,
    val passwordHash: String? = null,
    val passwordSalt: String? = null,
    val passwordAlgoritmo: String? = null
)