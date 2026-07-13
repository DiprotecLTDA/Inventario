package com.diprotec.inventario.data.repository

import com.diprotec.inventario.core.config.SettingsManager
import com.diprotec.inventario.core.network.ProtectedHeadersBuilder
import com.diprotec.inventario.data.local.dao.RuleDao
import com.diprotec.inventario.data.local.entity.RuleEntity
import com.diprotec.inventario.data.remote.api.ApiService
import com.diprotec.inventario.data.remote.dto.ReglaDto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleRepositoryImpl @Inject constructor(
    private val ruleDao: RuleDao,
    private val api: ApiService,
    private val settings: SettingsManager,
    private val headersBuilder: ProtectedHeadersBuilder
) : RuleRepository {

    override suspend fun fetchRemoteReglas(): List<ReglaDto> {
        val empresaRut = settings.empresaRut.value.trim()
        require(empresaRut.isNotBlank()) { "Empresa RUT no configurado" }

        val relativeUrl = "/api/website/v1/reglas/$empresaRut/GetReglas"
        val headers = headersBuilder.build(
            method = "GET",
            relativeUrl = relativeUrl
        )

        return api.getReglas(
            empresaRUT = empresaRut,
            apiKey = headers.apiKey,
            authorization = headers.authorization,
            deviceSession = headers.deviceSession,
            deviceSignature = headers.deviceSignature,
            deviceTimestamp = headers.deviceTimestamp
        ).data
    }

    override suspend fun replaceAllReglas(list: List<RuleEntity>) {
        ruleDao.replaceAll(list)
    }

    override fun observeReglas(): Flow<List<RuleEntity>> =
        ruleDao.observeAll()
}