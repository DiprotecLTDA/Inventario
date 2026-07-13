package com.diprotec.inventario.core.crypto

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceCanonicalStringBuilder @Inject constructor() {

    fun build(
        method: String,
        relativeUrl: String,
        timestamp: String
    ): String {
        return "${method.trim().uppercase()}|${relativeUrl.trim()}|${timestamp.trim()}"
    }
}