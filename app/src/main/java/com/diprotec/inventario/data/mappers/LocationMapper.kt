package com.diprotec.inventario.data.mappers

import com.diprotec.inventario.data.local.entity.LocationEntity
import com.diprotec.inventario.data.remote.dto.UbicacionDto

fun UbicacionDto.toEntity(): LocationEntity =
    LocationEntity(
        id = id,
        nombre = nombre,
        vigente = vigente.toApiBool(),
        rutEmpresa = rutEmpresa
    )