package com.diprotec.inventario.data.repository

import com.diprotec.inventario.core.config.SettingsManager
import com.diprotec.inventario.core.message.AppMessages
import com.diprotec.inventario.core.network.ApiCallExecutor
import com.diprotec.inventario.core.network.ProtectedHeadersBuilder
import com.diprotec.inventario.data.local.dao.ProductDao
import com.diprotec.inventario.data.local.entity.ProductEntity
import com.diprotec.inventario.data.remote.api.ApiService
import com.diprotec.inventario.data.remote.dto.ProductoDto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val productDao: ProductDao,
    private val api: ApiService,
    private val apiCallExecutor: ApiCallExecutor,
    private val settings: SettingsManager,
    private val headersBuilder: ProtectedHeadersBuilder
) : ProductRepository {

    override suspend fun fetchRemoteProductos(): List<ProductoDto> {
        val empresaRut = settings.empresaRut.value.trim()
        require(empresaRut.isNotBlank()) { AppMessages.Configuration.EMPRESA_RUT_NO_CONFIGURADO }

        val relativeUrl = "/api/website/v1/productos/$empresaRut/GetProductos"
        val headers = headersBuilder.build(
            method = "GET",
            relativeUrl = relativeUrl
        )

        return apiCallExecutor.execute {
            api.getProductos(
                empresaRUT = empresaRut,
                apiKey = headers.apiKey,
                authorization = headers.authorization,
                deviceSession = headers.deviceSession,
                deviceSignature = headers.deviceSignature,
                deviceTimestamp = headers.deviceTimestamp
            )
        }.data
    }

    override suspend fun replaceAllProductos(list: List<ProductEntity>) {
        productDao.replaceAll(list)
    }

    override fun observeProductos(): Flow<List<ProductEntity>> =
        productDao.observeAll()
}
