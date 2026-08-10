package com.diprotec.inventario.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.diprotec.inventario.data.local.entity.AppErrorEntity

@Dao
interface AppErrorDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AppErrorEntity): Long

    @Query(
        """
        SELECT * FROM app_errors
        WHERE localState = :state
        ORDER BY
            CASE severity
                WHEN 'CRITICAL' THEN 0
                WHEN 'ERROR' THEN 1
                WHEN 'WARNING' THEN 2
                ELSE 3
            END,
            createdAt ASC
        LIMIT :limit
        """
    )
    suspend fun claimPendingBatch(state: String, limit: Int): List<AppErrorEntity>

    @Query("UPDATE app_errors SET localState = :newState WHERE id IN (:ids)")
    suspend fun updateState(ids: List<Long>, newState: String)

    @Query(
        "UPDATE app_errors SET localState = :newState, attemptCount = attemptCount + 1, " +
                "sendError = :sendError WHERE id = :id"
    )
    suspend fun markAttempt(id: Long, newState: String, sendError: String?)

    @Query("SELECT attemptCount FROM app_errors WHERE id = :id")
    suspend fun getAttemptCount(id: Long): Int?

    /** Si el proceso murió mientras se enviaba un lote, vuelve esos registros a PENDING. */
    @Query("UPDATE app_errors SET localState = :pendingState WHERE localState = :sendingState")
    suspend fun recoverInterruptedSending(sendingState: String, pendingState: String)

    @Query("DELETE FROM app_errors WHERE localState = :state AND createdAt < :olderThan")
    suspend fun purgeOlderThan(state: String, olderThan: Long)

    @Query("SELECT id FROM app_errors WHERE localState = :state ORDER BY createdAt ASC")
    suspend fun idsByState(state: String): List<Long>

    @Query("DELETE FROM app_errors WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM app_errors WHERE localState = :state")
    suspend fun countByState(state: String): Int
}
