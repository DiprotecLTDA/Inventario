package com.diprotec.inventario.core.concurrency

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Evita que dos invocaciones casi simultáneas de la misma operación se ejecuten a la vez
 * (p.ej. doble-tap en un botón que lanza una corrutina). `tryAcquire()` es síncrono, por lo
 * que una segunda llamada que llega mientras la primera sigue en curso se descarta de
 * inmediato. El llamador debe liberar el guard en un `finally`.
 */
class SingleFlightGuard {

    private val inFlight = AtomicBoolean(false)

    fun tryAcquire(): Boolean = inFlight.compareAndSet(false, true)

    fun release() {
        inFlight.set(false)
    }
}
