package com.diprotec.inventario.data.repository

import com.diprotec.inventario.data.local.entity.RuleEntity
import com.diprotec.inventario.data.remote.dto.ReglaDto
import kotlinx.coroutines.flow.Flow

interface RuleRepository {
    suspend fun fetchRemoteReglas(): List<ReglaDto>
    suspend fun replaceAllReglas(list: List<RuleEntity>)
    fun observeReglas(): Flow<List<RuleEntity>>
}