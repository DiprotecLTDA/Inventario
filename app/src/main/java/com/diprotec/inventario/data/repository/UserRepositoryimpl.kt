package com.diprotec.inventario.data.repository

import com.diprotec.inventario.core.config.SettingsManager
import com.diprotec.inventario.core.network.ProtectedHeadersBuilder
import com.diprotec.inventario.data.local.dao.UserDao
import com.diprotec.inventario.data.local.entity.UserEntity
import com.diprotec.inventario.data.remote.api.ApiService
import com.diprotec.inventario.data.remote.dto.UserDto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val api: ApiService,
    private val settings: SettingsManager,
    private val headersBuilder: ProtectedHeadersBuilder
) : UserRepository {

    override suspend fun fetchRemoteUsers(): List<UserDto> {
        val empresaRut = settings.empresaRut.value.trim()
        require(empresaRut.isNotBlank()) { "Empresa RUT no configurado" }

        val relativeUrl = "/api/website/v1/usuarios/$empresaRut/GetUsuarios"

        val headers = headersBuilder.build(
            method = "GET",
            relativeUrl = relativeUrl
        )

        return api.getUsers(
            empresaRUT = empresaRut,
            apiKey = headers.apiKey,
            authorization = headers.authorization,
            deviceSession = headers.deviceSession,
            deviceSignature = headers.deviceSignature,
            deviceTimestamp = headers.deviceTimestamp
        ).data
    }

    override suspend fun replaceAllUsers(list: List<UserEntity>) {
        userDao.clearAll()
        userDao.upsertAll(list)
    }

    override fun observeUsers(): Flow<List<UserEntity>> = userDao.observeAll()
}