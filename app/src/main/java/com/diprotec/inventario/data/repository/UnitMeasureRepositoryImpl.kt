package com.diprotec.inventario.data.repository

import com.diprotec.inventario.core.config.SettingsManager
import com.diprotec.inventario.core.message.AppMessages
import com.diprotec.inventario.core.network.ProtectedHeadersBuilder
import com.diprotec.inventario.data.local.dao.UnitMeasureDao
import com.diprotec.inventario.data.local.entity.UnitMeasureEntity
import com.diprotec.inventario.data.remote.api.ApiService
import com.diprotec.inventario.data.remote.dto.UnidadMedidaDto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnitMeasureRepositoryImpl @Inject constructor(
    private val unitMeasureDao: UnitMeasureDao,
    private val api: ApiService,
    private val settings: SettingsManager,
    private val headersBuilder: ProtectedHeadersBuilder
) : UnitMeasureRepository {

    override suspend fun fetchRemoteUnidadMedidas(): List<UnidadMedidaDto> {
        val empresaRut = settings.empresaRut.value.trim()
        require(empresaRut.isNotBlank()) { AppMessages.Configuration.EMPRESA_RUT_NO_CONFIGURADO }

        val relativeUrl = "/api/website/v1/unidadmedidas/$empresaRut/GetUnidadMedidas"
        val headers = headersBuilder.build(
            method = "GET",
            relativeUrl = relativeUrl
        )

        return api.getUnidadMedidas(
            empresaRUT = empresaRut,
            apiKey = headers.apiKey,
            authorization = headers.authorization,
            deviceSession = headers.deviceSession,
            deviceSignature = headers.deviceSignature,
            deviceTimestamp = headers.deviceTimestamp
        ).data
    }

    override suspend fun replaceAllUnidadMedidas(list: List<UnitMeasureEntity>) {
        unitMeasureDao.replaceAll(list)
    }

    override fun observeUnidadMedidas(): Flow<List<UnitMeasureEntity>> =
        unitMeasureDao.observeAll()
}
