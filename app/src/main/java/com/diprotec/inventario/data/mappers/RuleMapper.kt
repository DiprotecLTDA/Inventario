package com.diprotec.inventario.data.mappers

import com.diprotec.inventario.data.local.entity.RuleEntity
import com.diprotec.inventario.data.remote.dto.ReglaDto

fun ReglaDto.toEntity(): RuleEntity =
    RuleEntity(
        id = id,
        nombre = nombre,
        nombreApellido = nombreApellido,
        empresa = empresa,
        patente = patente,
        comentario = comentario,
        fotografia = fotografia,
        entradaSalida = entradaSalida,
        listaBlancaNegra = listaBlancaNegra,
        eliminaEnviados = eliminaEnviados,
        vigente = vigente.toApiBool(),
        perfil = perfil,
        rutEmpresa = rutEmpresa
    )