package com.diprotec.inventario.data.mappers

import com.diprotec.inventario.data.local.entity.UnitMeasureEntity
import com.diprotec.inventario.data.remote.dto.UnidadMedidaDto

fun UnidadMedidaDto.toEntity(): UnitMeasureEntity =
    UnitMeasureEntity(
        id = id,
        nombre = nombre,
        valor = valor,
        predeterminado = predeterminado.toApiBool(),
        vigente = vigente.toApiBool(),
        rutEmpresa = rutEmpresa
    )