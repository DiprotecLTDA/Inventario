package com.diprotec.inventario.data.mappers

import com.diprotec.inventario.data.local.entity.InventoryRemoteEntity
import com.diprotec.inventario.data.local.entity.InventoryRemoteUserEntity
import com.diprotec.inventario.data.remote.dto.InventarioDto
import com.diprotec.inventario.data.remote.dto.InventarioUsuarioDto

fun InventarioDto.toEntity(): InventoryRemoteEntity =
    InventoryRemoteEntity(
        id = id,
        descripcion = descripcion,
        fecha = fecha,
        hora = hora,
        desde = desde,
        hasta = hasta,
        rutAdministrador = rutAdministrador,
        vigente = vigente.toApiBool(),
        rutEmpresa = rutEmpresa
    )

fun InventarioUsuarioDto.toEntity(): InventoryRemoteUserEntity {
    return InventoryRemoteUserEntity(
        id = "${inventarioId.orEmpty()}_${rutUsuario.orEmpty()}",
        inventarioId = inventarioId.orEmpty(),
        rutUsuario = rutUsuario.orEmpty()
    )
}