package com.diprotec.inventario.core.error

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.withContext

/**
 * Suprime el reporte de errores dentro de un bloque `withoutReporting { }`, propagado a
 * través de corrutinas (incluso si el bloque cambia de hilo/dispatcher). Se usa para evitar
 * que un fallo dentro del propio subsistema de envío de reportes (p.ej. `ErrorEmailWorker`)
 * genere, a su vez, más reportes de error.
 */
object ErrorReportingGuard {

    private val suppressed = ThreadLocal.withInitial { false }

    fun isSuppressed(): Boolean = suppressed.get() == true

    suspend fun <T> withoutReporting(block: suspend () -> T): T {
        return withContext(SuppressReportingElement()) {
            block()
        }
    }

    private class SuppressReportingElement : ThreadContextElement<Boolean> {

        override val key: CoroutineContext.Key<*> = Key

        override fun updateThreadContext(context: CoroutineContext): Boolean {
            val previous = suppressed.get()
            suppressed.set(true)
            return previous
        }

        override fun restoreThreadContext(context: CoroutineContext, oldState: Boolean) {
            suppressed.set(oldState)
        }

        companion object Key : CoroutineContext.Key<SuppressReportingElement>
    }
}
