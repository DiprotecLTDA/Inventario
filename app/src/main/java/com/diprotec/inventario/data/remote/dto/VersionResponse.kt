package com.diprotec.inventario.data.remote.dto

import com.diprotec.inventario.core.network.BaseApiResponse
import com.squareup.moshi.Json

data class VersionEntradaRequest(
    @Json(name = "VersionActual") val versionActual: Int,
    @Json(name = "VersionActualNombre") val versionActualNombre: String,
    @Json(name = "Fabricante") val fabricante: String,
    @Json(name = "AndroidVersion") val androidVersion: String,
    @Json(name = "Modelo") val modelo: String
)

data class VersionResponse(
    @Json(name = "Estado") val estado: Int,
    @Json(name = "Respuesta") val respuesta: String?,
    @Json(name = "Data") val data: VersionCheckDataDto?,
    @Json(name = "CodigoError") val codigoError: String?,
    @Json(name = "CorrelationId") val correlationId: String?
) : BaseApiResponse {
    override val apiEstado get() = estado
    override val apiRespuesta get() = respuesta
    override val apiCodigoError get() = codigoError
    override val apiCorrelationId get() = correlationId
}

class VersionCheckDataDto(
    @Json(name = "PuedeOperar") private val puedeOperarRaw: Any?,
    @Json(name = "RequiereActualizacion") private val requiereActualizacionRaw: Any?,
    @Json(name = "ActualizacionObligatoria") private val actualizacionObligatoriaRaw: Any?,
    @Json(name = "Mensaje") val mensaje: String?,
    @Json(name = "Version") val version: VersionDataDto?
) {
    val puedeOperar: String?
        get() = puedeOperarRaw.toApiString()

    val requiereActualizacion: String?
        get() = requiereActualizacionRaw.toApiString()

    val actualizacionObligatoria: String?
        get() = actualizacionObligatoriaRaw.toApiString()

    fun puedeOperarBool(): Boolean = puedeOperarRaw.toBooleanStrictSafe()
    fun requiereActualizacionBool(): Boolean = requiereActualizacionRaw.toBooleanStrictSafe()
    fun actualizacionObligatoriaBool(): Boolean = actualizacionObligatoriaRaw.toBooleanStrictSafe()
}

class VersionDataDto(
    @Json(name = "Id") private val idRaw: Any?,
    @Json(name = "VersionName") private val versionNameRaw: Any?,
    @Json(name = "VersionCode") private val versionCodeRaw: Any?,
    @Json(name = "MinSupportedVersionCode") private val minSupportedVersionCodeRaw: Any?,
    @Json(name = "ForceUpdate") private val forceUpdateRaw: Any?,
    @Json(name = "IsActive") private val isActiveRaw: Any?,
    @Json(name = "IsPublished") private val isPublishedRaw: Any?,
    @Json(name = "Fabricante") private val fabricanteRaw: Any?,
    @Json(name = "AndroidVersion") private val androidVersionRaw: Any?,
    @Json(name = "Modelo") private val modeloRaw: Any?,
    @Json(name = "ApkFileName") private val apkFileNameRaw: Any?,
    @Json(name = "ApkRelativePath") private val apkRelativePathRaw: Any?,
    @Json(name = "ApkSha256") private val apkSha256Raw: Any?,
    @Json(name = "FileSizeBytes") private val fileSizeBytesRaw: Any?,
    @Json(name = "ReleaseNotes") private val releaseNotesRaw: Any?,
    @Json(name = "CreatedAt") private val createdAtRaw: Any?,
    @Json(name = "CreatedBy") private val createdByRaw: Any?,
    @Json(name = "PublishedAt") private val publishedAtRaw: Any?
) {
    val id: String?
        get() = idRaw.toApiString()

    val versionName: String?
        get() = versionNameRaw.toApiString()

    val versionCode: String?
        get() = versionCodeRaw.toApiString()

    val minSupportedVersionCode: String?
        get() = minSupportedVersionCodeRaw.toApiString()

    val forceUpdate: String?
        get() = forceUpdateRaw.toApiString()

    val isActive: String?
        get() = isActiveRaw.toApiString()

    val isPublished: String?
        get() = isPublishedRaw.toApiString()

    val fabricante: String?
        get() = fabricanteRaw.toApiString()

    val androidVersion: String?
        get() = androidVersionRaw.toApiString()

    val modelo: String?
        get() = modeloRaw.toApiString()

    val apkFileName: String?
        get() = apkFileNameRaw.toApiString()

    val apkRelativePath: String?
        get() = apkRelativePathRaw.toApiString()

    val apkSha256: String?
        get() = apkSha256Raw.toApiString()

    val fileSizeBytes: String?
        get() = fileSizeBytesRaw.toApiString()

    val releaseNotes: String?
        get() = releaseNotesRaw.toApiString()

    val createdAt: String?
        get() = createdAtRaw.toApiString()

    val createdBy: String?
        get() = createdByRaw.toApiString()

    val publishedAt: String?
        get() = publishedAtRaw.toApiString()

    fun versionCodeInt(): Int = versionCodeRaw.toIntSafe()
    fun minSupportedVersionCodeInt(): Int = minSupportedVersionCodeRaw.toIntSafe()
    fun isForceUpdate(): Boolean = forceUpdateRaw.toBooleanStrictSafe()
    fun isActiveBool(): Boolean = isActiveRaw.toBooleanStrictSafe()
    fun isPublishedBool(): Boolean = isPublishedRaw.toBooleanStrictSafe()
}

private fun Any?.toApiString(): String? {
    return when (this) {
        null -> null
        is String -> this
        is Number -> {
            val doubleValue = this.toDouble()
            if (doubleValue % 1.0 == 0.0) {
                this.toLong().toString()
            } else {
                this.toString()
            }
        }
        is Boolean -> this.toString()
        else -> this.toString()
    }
}

private fun Any?.toIntSafe(): Int {
    return when (this) {
        null -> 0
        is Int -> this
        is Long -> this.toInt()
        is Double -> this.toInt()
        is Float -> this.toInt()
        is Number -> this.toInt()
        is String -> this.trim().toIntOrNull() ?: 0
        else -> this.toString().trim().toIntOrNull() ?: 0
    }
}

private fun Any?.toBooleanStrictSafe(): Boolean {
    return when (this) {
        null -> false
        is Boolean -> this
        is Int -> this == 1
        is Long -> this == 1L
        is Double -> this.toInt() == 1
        is Float -> this.toInt() == 1
        is Number -> this.toInt() == 1
        is String -> {
            val value = this.trim()
            value.equals("true", ignoreCase = true) || value == "1"
        }
        else -> {
            val value = this.toString().trim()
            value.equals("true", ignoreCase = true) || value == "1"
        }
    }
}
