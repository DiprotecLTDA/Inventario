package com.diprotec.inventario.data.repository

import com.diprotec.inventario.data.local.entity.UnitMeasureEntity
import com.diprotec.inventario.data.remote.dto.UnidadMedidaDto
import kotlinx.coroutines.flow.Flow

interface UnitMeasureRepository {
    suspend fun fetchRemoteUnidadMedidas(): List<UnidadMedidaDto>
    suspend fun replaceAllUnidadMedidas(list: List<UnitMeasureEntity>)
    fun observeUnidadMedidas(): Flow<List<UnitMeasureEntity>>
}