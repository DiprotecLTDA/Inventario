package com.diprotec.inventario.data.repository

import com.diprotec.inventario.core.config.SettingsManager
import com.diprotec.inventario.core.message.AppMessages
import com.diprotec.inventario.core.network.ApiCallExecutor
import com.diprotec.inventario.core.network.ProtectedHeadersBuilder
import com.diprotec.inventario.data.local.dao.InventoryRemoteDao
import com.diprotec.inventario.data.local.entity.InventoryRemoteEntity
import com.diprotec.inventario.data.local.entity.InventoryRemoteUserEntity
import com.diprotec.inventario.data.remote.api.ApiService
import com.diprotec.inventario.data.remote.dto.InventarioDto
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class InventoryRemoteRepositoryImpl @Inject constructor(
    private val dao: InventoryRemoteDao,
    private val api: ApiService,
    private val apiCallExecutor: ApiCallExecutor,
    private val settings: SettingsManager,
    private val headersBuilder: ProtectedHeadersBuilder
) : InventoryRemoteRepository {

    override suspend fun fetchRemoteInventarios(): List<InventarioDto> {
        val empresaRut = settings.empresaRut.value.trim()
        require(empresaRut.isNotBlank()) { AppMessages.Configuration.EMPRESA_RUT_NO_CONFIGURADO }

        val relativeUrl = "/api/website/v1/inventarios/$empresaRut/GetInventarios"

        val headers = headersBuilder.build(
            method = "GET",
            relativeUrl = relativeUrl
        )

        return apiCallExecutor.execute {
            api.getInventarios(
                empresaRUT = empresaRut,
                apiKey = headers.apiKey,
                authorization = headers.authorization,
                deviceSession = headers.deviceSession,
                deviceSignature = headers.deviceSignature,
                deviceTimestamp = headers.deviceTimestamp
            )
        }.data
    }

    override suspend fun replaceAllInventarios(
        inventarios: List<InventoryRemoteEntity>,
        usuarios: List<InventoryRemoteUserEntity>
    ) {
        dao.replaceAll(
            inventarios = inventarios,
            usuarios = usuarios
        )
    }

    override fun observeInventarios(): Flow<List<InventoryRemoteEntity>> =
        dao.observeAll()

    override fun observeInventariosActivos(): Flow<List<InventoryRemoteEntity>> =
        dao.observeActivos()

    override fun observeInventariosAsignadosActivos(
        rutUsuario: String
    ): Flow<List<InventoryRemoteEntity>> =
        dao.observeAsignadosActivosPorUsuario(rutUsuario)
}
