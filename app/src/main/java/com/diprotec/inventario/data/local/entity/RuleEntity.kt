package com.diprotec.inventario.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reglas")
data class RuleEntity(
    @PrimaryKey val id: String,
    val nombre: String?,
    val nombreApellido: String?,
    val empresa: String?,
    val patente: String?,
    val comentario: String?,
    val fotografia: String?,
    val entradaSalida: String?,
    val listaBlancaNegra: String?,
    val eliminaEnviados: String?,
    val vigente: Boolean,
    val perfil: String?,
    val rutEmpresa: String?
)