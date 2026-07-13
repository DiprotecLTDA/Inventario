package com.diprotec.inventario.data.repository

import com.diprotec.inventario.data.local.entity.ProductEntity
import com.diprotec.inventario.data.remote.dto.ProductoDto
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    suspend fun fetchRemoteProductos(): List<ProductoDto>
    suspend fun replaceAllProductos(list: List<ProductEntity>)
    fun observeProductos(): Flow<List<ProductEntity>>
}