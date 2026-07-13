package com.diprotec.inventario.core.crypto

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceTimestampProvider @Inject constructor() {

    private val formatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .withZone(ZoneOffset.UTC)

    fun nowUtc(): String {
        return formatter.format(Instant.now())
    }
}