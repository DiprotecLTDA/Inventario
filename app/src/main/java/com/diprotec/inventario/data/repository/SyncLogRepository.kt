package com.diprotec.inventario.data.repository

import com.diprotec.inventario.data.local.dao.SyncLogDao
import com.diprotec.inventario.data.local.entity.SyncLogEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class SyncLogRepository @Inject constructor(
    private val dao: SyncLogDao
) {
    fun observeAll(): Flow<List<SyncLogEntity>> = dao.observeAll()

    suspend fun insert(log: SyncLogEntity) {
        dao.insert(log)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}