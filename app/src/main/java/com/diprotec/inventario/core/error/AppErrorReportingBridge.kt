package com.diprotec.inventario.core.error

/**
 * Puente estático para reportar errores desde código sin acceso a inyección de dependencias
 * (funciones top-level como [operationError]/[runStep]). `AppErrorReporter` se adjunta a sí
 * mismo aquí al construirse (es `@Singleton`, ocurre una sola vez por proceso).
 */
object AppErrorReportingBridge {

    @Volatile
    private var reporter: AppErrorReporter? = null

    internal fun attach(reporter: AppErrorReporter) {
        this.reporter = reporter
    }

    fun report(
        action: String,
        throwable: Throwable? = null,
        module: String? = null,
        endpoint: String? = null,
        screen: String? = null,
        inventoryId: Long? = null
    ) {
        if (ErrorReportingGuard.isSuppressed()) return
        if (throwable is OperationException) return

        reporter?.report(
            ErrorContext(
                action = action,
                throwable = throwable,
                module = module,
                endpoint = endpoint,
                screen = screen,
                inventoryId = inventoryId
            )
        )
    }
}
