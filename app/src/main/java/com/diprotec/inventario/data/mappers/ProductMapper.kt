package com.diprotec.inventario.data.mappers

import com.diprotec.inventario.data.local.entity.ProductEntity
import com.diprotec.inventario.data.remote.dto.ProductoDto

fun ProductoDto.toEntity(): ProductEntity =
    ProductEntity(
        codigo = codigo,
        codigoSecundario = codigoSecundario,
        descripcion = descripcion,
        vigente = vigente.toApiBool(),
        rutEmpresa = rutEmpresa
    )