package com.diprotec.inventario.data.repository

import com.diprotec.inventario.core.config.SettingsManager
import com.diprotec.inventario.core.network.ProtectedHeadersBuilder
import com.diprotec.inventario.data.local.dao.LocationDao
import com.diprotec.inventario.data.local.entity.LocationEntity
import com.diprotec.inventario.data.remote.api.ApiService
import com.diprotec.inventario.data.remote.dto.UbicacionDto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val locationDao: LocationDao,
    private val api: ApiService,
    private val settings: SettingsManager,
    private val headersBuilder: ProtectedHeadersBuilder
) : LocationRepository {

    override suspend fun fetchRemoteUbicaciones(): List<UbicacionDto> {
        val empresaRut = settings.empresaRut.value.trim()
        require(empresaRut.isNotBlank()) { "Empresa RUT no configurado" }

        val relativeUrl = "/api/website/v1/ubicaciones/$empresaRut/GetUbicaciones"
        val headers = headersBuilder.build(
            method = "GET",
            relativeUrl = relativeUrl
        )

        return api.getUbicaciones(
            empresaRUT = empresaRut,
            apiKey = headers.apiKey,
            authorization = headers.authorization,
            deviceSession = headers.deviceSession,
            deviceSignature = headers.deviceSignature,
            deviceTimestamp = headers.deviceTimestamp
        ).data
    }

    override suspend fun replaceAllUbicaciones(list: List<LocationEntity>) {
        locationDao.replaceAll(list)
    }

    override fun observeUbicaciones(): Flow<List<LocationEntity>> =
        locationDao.observeAll()
}