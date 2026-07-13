package com.diprotec.inventario.core.network

object NetworkUsageContext {

    private val current = ThreadLocal<NetworkUsageTag?>()

    fun currentTag(): NetworkUsageTag? = current.get()

    suspend fun <T> runWith(
        source: String,
        operation: String,
        block: suspend () -> T
    ): T {
        val previous = current.get()
        current.set(
            NetworkUsageTag(
                source = source,
                operation = operation
            )
        )

        return try {
            block()
        } finally {
            current.set(previous)
        }
    }
}