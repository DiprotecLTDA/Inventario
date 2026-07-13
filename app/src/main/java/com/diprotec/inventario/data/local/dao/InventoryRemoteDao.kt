package com.diprotec.inventario.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.diprotec.inventario.data.local.entity.InventoryRemoteEntity
import com.diprotec.inventario.data.local.entity.InventoryRemoteUserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryRemoteDao {

    @Query("DELETE FROM inventario_usuarios_remotos")
    suspend fun clearUsuarios()

    @Query("DELETE FROM inventarios_remotos")
    suspend fun clearInventarios()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInventarios(items: List<InventoryRemoteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUsuarios(items: List<InventoryRemoteUserEntity>)

    @Transaction
    suspend fun replaceAll(
        inventarios: List<InventoryRemoteEntity>,
        usuarios: List<InventoryRemoteUserEntity>
    ) {
        clearUsuarios()
        clearInventarios()
        upsertInventarios(inventarios)
        upsertUsuarios(usuarios)
    }

    @Query("SELECT * FROM inventarios_remotos ORDER BY fecha DESC, hora DESC")
    fun observeAll(): Flow<List<InventoryRemoteEntity>>

    @Query("SELECT * FROM inventarios_remotos WHERE vigente = 0 ORDER BY fecha DESC, hora DESC")
    fun observeActivos(): Flow<List<InventoryRemoteEntity>>

    @Query("""
        SELECT DISTINCT i.*
        FROM inventarios_remotos i
        INNER JOIN inventario_usuarios_remotos iu
            ON iu.inventarioId = i.id
        WHERE iu.rutUsuario = :rutUsuario
          AND i.vigente = 0
        ORDER BY i.fecha DESC, i.hora DESC
    """)
    fun observeAsignadosActivosPorUsuario(
        rutUsuario: String
    ): Flow<List<InventoryRemoteEntity>>

    @Query("SELECT * FROM inventario_usuarios_remotos WHERE rutUsuario = :rutUsuario")
    suspend fun getByUsuario(rutUsuario: String): List<InventoryRemoteUserEntity>

    @Query("SELECT COUNT(*) FROM inventarios_remotos")
    suspend fun count(): Int
}