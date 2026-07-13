package com.diprotec.inventario.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.diprotec.inventario.data.local.entity.UnitMeasureEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UnitMeasureDao {

    @Query("DELETE FROM unidad_medidas")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<UnitMeasureEntity>)

    @Transaction
    suspend fun replaceAll(items: List<UnitMeasureEntity>) {
        clearAll()
        upsertAll(items)
    }

    @Query("SELECT * FROM unidad_medidas ORDER BY nombre")
    fun observeAll(): Flow<List<UnitMeasureEntity>>

    @Query("SELECT COUNT(*) FROM unidad_medidas")
    suspend fun count(): Int
}