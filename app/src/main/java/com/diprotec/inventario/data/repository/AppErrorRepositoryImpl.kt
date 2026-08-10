package com.diprotec.inventario.data.repository

import com.diprotec.inventario.core.error.ErrorLocalState
import com.diprotec.inventario.data.local.dao.AppErrorDao
import com.diprotec.inventario.data.local.entity.AppErrorEntity
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppErrorRepositoryImpl @Inject constructor(
    private val dao: AppErrorDao
) : AppErrorRepository {

    override suspend fun insert(entity: AppErrorEntity): Long = dao.insert(entity)

    override suspend fun claimPending(limit: Int): List<AppErrorEntity> =
        dao.claimPendingBatch(ErrorLocalState.PENDING.name, limit)

    override suspend fun markSending(ids: List<Long>) {
        if (ids.isEmpty()) return
        dao.updateState(ids, ErrorLocalState.SENDING.name)
    }

    override suspend fun markSent(id: Long) {
        dao.markAttempt(id, ErrorLocalState.SENT.name, null)
    }

    override suspend fun markFailed(id: Long, sendError: String?, permanent: Boolean) {
        val attemptCount = dao.getAttemptCount(id) ?: 0
        val exhausted = attemptCount + 1 >= MAX_SEND_ATTEMPTS

        val newState = if (permanent || exhausted) {
            ErrorLocalState.FAILED.name
        } else {
            ErrorLocalState.PENDING.name
        }

        dao.markAttempt(id, newState, sendError)
    }

    override suspend fun recoverInterruptedSending() {
        dao.recoverInterruptedSending(
            sendingState = ErrorLocalState.SENDING.name,
            pendingState = ErrorLocalState.PENDING.name
        )
    }

    override suspend fun purge() {
        val cutoff = System.currentTimeMillis() - RETENTION_MS
        dao.purgeOlderThan(ErrorLocalState.SENT.name, cutoff)

        val remainingSentIds = dao.idsByState(ErrorLocalState.SENT.name)
        if (remainingSentIds.size > MAX_SENT_ROWS) {
            val toDelete = remainingSentIds.take(remainingSentIds.size - MAX_SENT_ROWS)
            dao.deleteByIds(toDelete)
        }
    }

    private companion object {
        private val RETENTION_MS = TimeUnit.DAYS.toMillis(14)
        private const val MAX_SENT_ROWS = 1000
        private const val MAX_SEND_ATTEMPTS = 5
    }
}
