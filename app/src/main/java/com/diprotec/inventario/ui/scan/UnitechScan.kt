package com.diprotec.inventario.ui.scan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import java.nio.charset.StandardCharsets

/**
 * Gestión de scanner integrado Unitech vía broadcast:
 * - Desactiva scan2key al registrar (para no “tipear” en campos).
 * - Activa scan2key al desregistrar.
 * - Escucha resultados en acción "unitech.scanservice.dataall".
 *
 * Callback: (codigo, codeId) -> Unit
 */
class UnitechScan(
    private val context: Context,
    private val onScan: (code: String, codeId: Int) -> Unit
) {

    private val ACTION_SETTING = "unitech.scanservice.setting"
    private val ACTION_SCAN2KEY = "unitech.scanservice.scan2key_setting"
    private val ACTION_DATAALL = "unitech.scanservice.dataall"

    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action != ACTION_DATAALL) return
            val extras: Bundle = intent.extras ?: return

            // Java: bundle.getByteArray("databyte") y bundle.getInt("datatype")
            val bytes = extras.getByteArray("databyte")
            val codeId = extras.getInt("datatype", 0)
            val result = bytes?.toString(StandardCharsets.UTF_8)

            if (!result.isNullOrEmpty() && codeId != 0) {
                onScan(result, codeId)
            } else {
                // Aquí podrías notificar error si quieres
            }
        }
    }

    /** Desactiva scan2key y registra el receiver */
    fun register() {
        if (registered) return
        // Desactivar scan2key (equivalente a tu sendBroadcast con scan2key=false)
        val disable = Intent().apply {
            action = ACTION_SCAN2KEY
            putExtra("scan2key", false)
        }
        context.sendBroadcast(disable)

        // Registrar receiver
        val filter = IntentFilter().apply { addAction(ACTION_DATAALL) }
        context.registerReceiver(receiver, filter)
        registered = true
    }

    /** Activa scan2key y desregistra el receiver */
    fun unregister() {
        if (!registered) return
        // Activar scan2key (equivalente a tu sendBroadcast con scan2key=true)
        val enable = Intent().apply {
            action = ACTION_SCAN2KEY
            putExtra("scan2key", true)
        }
        context.sendBroadcast(enable)

        context.unregisterReceiver(receiver)
        registered = false
    }
}
