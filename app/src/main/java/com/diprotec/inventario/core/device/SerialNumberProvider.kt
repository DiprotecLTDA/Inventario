package com.diprotec.inventario.core.device

import android.content.Context
import android.os.Bundle
import com.unitech.api.deviceinfo.DeviceInfoCtrl
import com.unitech.api.dmi.DmiCtrl

data class SerialNumberResult(
    val serial: String?,
    val errCode: Int,
    val errMsg: String?
) {
    val isSuccess: Boolean get() = errCode == 0 && !serial.isNullOrBlank()
}

fun GetSerialNumber(ctx: Context): SerialNumberResult {
    val BUNDLE_ERROR_CODE = "errorCode"
    val BUNDLE_ERROR_MSG = "errorMsg"
    val RESULT_CODE_SUCCESS = 0

    return try {
        // Igual que el legacy (si falla, no necesariamente es fatal para leer el serial)
        runCatching {
            val dmiCtrl = DmiCtrl(ctx)
            dmiCtrl.DCMO_Set("UsbMtpMode", 1)
        }

        val deviceInfoCtrl = DeviceInfoCtrl(ctx)
        val bundle: Bundle = deviceInfoCtrl.getDeviceSerialNumber()

        val errCode = bundle.getInt(BUNDLE_ERROR_CODE, -1)
        val errMsg = bundle.getString(BUNDLE_ERROR_MSG)

        val serial = if (errCode == RESULT_CODE_SUCCESS) {
            bundle.getString("getDeviceSerialNumber")?.trim()
        } else null

        SerialNumberResult(serial = serial, errCode = errCode, errMsg = errMsg)
    } catch (t: Throwable) {
        SerialNumberResult(serial = null, errCode = -1, errMsg = t.message)
    }
}
