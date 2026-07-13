package com.diprotec.inventario.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.diprotec.inventario.data.local.entity.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Query("DELETE FROM ubicaciones")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<LocationEntity>)

    @Transaction
    suspend fun replaceAll(items: List<LocationEntity>) {
        clearAll()
        upsertAll(items)
    }

    @Query("SELECT * FROM ubicaciones ORDER BY nombre")
    fun observeAll(): Flow<List<LocationEntity>>

    @Query("""
        SELECT * 
        FROM ubicaciones 
        WHERE vigente = 0 
        ORDER BY nombre
    """)
    fun observeActivas(): Flow<List<LocationEntity>>

    @Query("SELECT COUNT(*) FROM ubicaciones")
    suspend fun count(): Int
}