package com.diprotec.inventario.data.repository

import com.diprotec.inventario.data.local.entity.InventoryRemoteEntity
import com.diprotec.inventario.data.local.entity.InventoryRemoteUserEntity
import com.diprotec.inventario.data.remote.dto.InventarioDto
import kotlinx.coroutines.flow.Flow

interface InventoryRemoteRepository {

    suspend fun fetchRemoteInventarios(): List<InventarioDto>

    suspend fun replaceAllInventarios(
        inventarios: List<InventoryRemoteEntity>,
        usuarios: List<InventoryRemoteUserEntity>
    )

    fun observeInventarios(): Flow<List<InventoryRemoteEntity>>

    fun observeInventariosActivos(): Flow<List<InventoryRemoteEntity>>

    fun observeInventariosAsignadosActivos(
        rutUsuario: String
    ): Flow<List<InventoryRemoteEntity>>
}