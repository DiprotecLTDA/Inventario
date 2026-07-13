package com.diprotec.inventario.data.mappers

import com.diprotec.inventario.data.local.entity.UserEntity
import com.diprotec.inventario.data.remote.dto.UserDto

internal fun String?.toApiBool(): Boolean {
    return this.equals("true", ignoreCase = true) ||
            this.equals("1", ignoreCase = true) ||
            this.equals("si", ignoreCase = true) ||
            this.equals("sí", ignoreCase = true)
}

fun UserDto.toEntity(): UserEntity =
    UserEntity(
        rut = rut,
        nombre = nombre,
        email = email,
        telefono = telefono,
        perfil = perfil,
        vigente = vigente.toApiBool(),
        perfilId = perfilId,
        passwordHash = passwordHash,
        passwordSalt = passwordSalt,
        passwordAlgoritmo = passwordAlgoritmo
    )