package com.diprotec.inventario.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.diprotec.inventario.data.local.entity.SyncLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncLogDao {

    @Insert
    suspend fun insert(log: SyncLogEntity)

    @Query("SELECT * FROM sync_logs ORDER BY sentAt DESC")
    fun observeAll(): Flow<List<SyncLogEntity>>

    @Query("DELETE FROM sync_logs")
    suspend fun clearAll()
}