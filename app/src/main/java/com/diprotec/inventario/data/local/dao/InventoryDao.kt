package com.diprotec.inventario.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.diprotec.inventario.data.local.entity.InventoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(inventory: InventoryEntity): Long

    @Query("SELECT * FROM inventories WHERE id = :inventoryId LIMIT 1")
    suspend fun getById(inventoryId: Long): InventoryEntity?

    @Query("SELECT * FROM inventories WHERE remoteInventoryId = :remoteInventoryId LIMIT 1")
    suspend fun getByRemoteId(remoteInventoryId: String): InventoryEntity?

    @Query("""
        SELECT * 
        FROM inventories 
        WHERE remoteInventoryId = :remoteInventoryId
          AND rutUsuario = :rutUsuario
        LIMIT 1
    """)
    suspend fun getByRemoteIdAndUsuario(
        remoteInventoryId: String,
        rutUsuario: String
    ): InventoryEntity?

    @Query("SELECT * FROM inventories WHERE status = :status ORDER BY createdAt DESC")
    fun observeByStatus(status: String): Flow<List<InventoryEntity>>

    @Query("""
        SELECT * 
        FROM inventories 
        WHERE status = :status
          AND rutUsuario = :rutUsuario
        ORDER BY createdAt DESC
    """)
    fun observeByStatusAndUsuario(
        status: String,
        rutUsuario: String
    ): Flow<List<InventoryEntity>>

    @Query("SELECT * FROM inventories ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<InventoryEntity>>

    @Query("""
        SELECT * 
        FROM inventories 
        WHERE rutUsuario = :rutUsuario
        ORDER BY createdAt DESC
    """)
    fun observeAllByUsuario(rutUsuario: String): Flow<List<InventoryEntity>>

    @Query("SELECT * FROM inventories WHERE status = :status ORDER BY createdAt DESC")
    suspend fun getByStatus(status: String): List<InventoryEntity>

    @Query("""
        SELECT * 
        FROM inventories 
        WHERE status = :status
          AND rutUsuario = :rutUsuario
        ORDER BY createdAt DESC
    """)
    suspend fun getByStatusAndUsuario(
        status: String,
        rutUsuario: String
    ): List<InventoryEntity>

    @Query("""
        UPDATE inventories 
        SET status = :status, finishedAt = :finishedAt 
        WHERE id = :inventoryId
    """)
    suspend fun updateStatus(
        inventoryId: Long,
        status: String,
        finishedAt: Long?
    )

    @Query("""
        SELECT * 
        FROM inventories
        WHERE status = :status
          AND finishSynced = 0
        ORDER BY finishedAt ASC, createdAt ASC
    """)
    suspend fun getPendingFinishSync(status: String): List<InventoryEntity>

    @Query("""
        UPDATE inventories
        SET finishSynced = 1,
            finishSyncedAt = :finishSyncedAt
        WHERE id = :inventoryId
    """)
    suspend fun markFinishSynced(
        inventoryId: Long,
        finishSyncedAt: Long
    )
}