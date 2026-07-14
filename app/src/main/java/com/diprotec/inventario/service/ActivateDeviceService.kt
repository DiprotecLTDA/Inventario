package com.diprotec.inventario.service

import com.diprotec.inventario.core.config.SettingsManager
import com.diprotec.inventario.core.network.ApiCallExecutor
import com.diprotec.inventario.data.remote.api.ApiService
import com.diprotec.inventario.data.remote.dto.ActivateDispositivoRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivateDeviceService @Inject constructor(
    private val api: ApiService,
    private val apiCallExecutor: ApiCallExecutor,
    private val settings: SettingsManager
) {

    private fun authorizationHeader(): String {
        val token = settings.authToken.value.trim()
        require(token.isNotBlank()) { "Authorization no configurado" }
        return "Bearer $token"
    }

    suspend fun activate(
        empresaRut: String,
        serialNumber: String,
        activationCode: String,
        publicKey: String
    ): String {
        val response = apiCallExecutor.execute {
            api.activateDispositivo(
                empresaRUT = empresaRut,
                apiKey = settings.apiKey.value.trim(),
                authorization = authorizationHeader(),
                body = ActivateDispositivoRequest(
                    SerialNumber = serialNumber,
                    ActivationCode = activationCode,
                    PublicKey = publicKey
                )
            )
        }

        val deviceId = extractDeviceId(response.data)

        if (deviceId.isBlank()) {
            throw IllegalStateException("ActivateDispositivo no devolvió DispositivoId")
        }

        return deviceId
    }

    private fun extractDeviceId(data: Any?): String {
        return when (data) {
            is String -> data.trim()
            is Number -> data.toLong().toString()
            is Map<*, *> -> {
                val possibleKeys = listOf(
                    "DispositivoId",
                    "dispositivoId",
                    "DeviceId",
                    "deviceId",
                    "Id",
                    "id"
                )

                possibleKeys.firstNotNullOfOrNull { key ->
                    data[key]?.toString()?.trim()?.takeIf { it.isNotBlank() }
                }.orEmpty()
            }
            else -> ""
        }
    }
}
