package com.diprotec.inventario.data.repository

import com.diprotec.inventario.data.local.entity.AppErrorEntity

interface AppErrorRepository {
    suspend fun insert(entity: AppErrorEntity): Long
    suspend fun claimPending(limit: Int): List<AppErrorEntity>
    suspend fun markSending(ids: List<Long>)
    suspend fun markSent(id: Long)
    suspend fun markFailed(id: Long, sendError: String?, permanent: Boolean)
    suspend fun recoverInterruptedSending()
    suspend fun purge()
}
