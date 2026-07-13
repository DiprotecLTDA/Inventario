package com.diprotec.inventario.data.repository

import com.diprotec.inventario.data.local.entity.LocationEntity
import com.diprotec.inventario.data.remote.dto.UbicacionDto
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    suspend fun fetchRemoteUbicaciones(): List<UbicacionDto>
    suspend fun replaceAllUbicaciones(list: List<LocationEntity>)
    fun observeUbicaciones(): Flow<List<LocationEntity>>
}