package com.diprotec.inventario.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.diprotec.inventario.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("DELETE FROM productos")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ProductEntity>)

    @Transaction
    suspend fun replaceAll(items: List<ProductEntity>) {
        clearAll()
        upsertAll(items)
    }

    @Query("SELECT * FROM productos ORDER BY descripcion")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("""
        SELECT * 
        FROM productos 
        WHERE codigo = :codigo 
           OR codigoSecundario = :codigo 
        LIMIT 1
    """)
    suspend fun findByCodigo(codigo: String): ProductEntity?

    @Query("SELECT COUNT(*) FROM productos")
    suspend fun count(): Int
}