package com.diprotec.inventario.core.error

/**
 * Enmascara secretos (tokens, API keys, RUT, cookies, query strings) antes de guardar o
 * mostrar cualquier texto técnico de diagnóstico. Se usa tanto para el reporte de errores
 * como para logging general de red.
 */
object ErrorSanitizer {

    private val SECRET_PATTERNS: List<Pair<Regex, String>> = listOf(
        Regex("(?i)(bearer)\\s+[A-Za-z0-9\\-_.]+") to "$1 ***",
        Regex("(?i)(api[_-]?key)\\s*[:=]\\s*[^\\s,&\"]+") to "$1=***",
        Regex("(?i)(auth[_-]?token)\\s*[:=]\\s*[^\\s,&\"]+") to "$1=***",
        Regex("(?i)(authorization)\\s*[:=]\\s*[^\\s,&\"]+") to "$1=***",
        Regex("(?i)(password)\\s*[:=]\\s*[^\\s,&\"]+") to "$1=***",
        Regex("(?i)(x-device-signature)\\s*[:=]\\s*[^\\s,&\"]+") to "$1=***",
        Regex("(?i)(x-device-session)\\s*[:=]\\s*[^\\s,&\"]+") to "$1=***",
        Regex("(?i)(cookie)\\s*[:=]\\s*[^\\s,&\"]+") to "$1=***"
    )

    private val QUERY_STRING_REGEX = Regex("(https?://[^\\s?\"]+)\\?[^\\s\"]+")

    private val SENSITIVE_HEADERS = setOf(
        "authorization",
        "x-api-key",
        "x-device-session",
        "x-device-signature",
        "x-device-timestamp",
        "cookie"
    )

    fun sanitize(text: String?): String? {
        if (text.isNullOrBlank()) return text

        // Tipado explícito (no inferido de `text: String?`): `result` se reasigna dentro de
        // un lambda, y Kotlin no puede promover un `var` capturado por closure a no-nulo aunque
        // aquí siempre lo sea; declararlo String de entrada evita el error de smart-cast.
        var result: String = text

        SECRET_PATTERNS.forEach { (pattern, replacement) ->
            result = pattern.replace(result, replacement)
        }

        result = QUERY_STRING_REGEX.replace(result) { match -> "${match.groupValues[1]}?***" }

        return result
    }

    fun sanitizeForLog(text: String?, maxLength: Int = MAX_TEXT_LENGTH): String? {
        val sanitized = sanitize(text) ?: return null
        return if (sanitized.length > maxLength) sanitized.take(maxLength) + "…" else sanitized
    }

    fun sanitizeStackTrace(text: String?): String? {
        return sanitizeForLog(text, MAX_STACKTRACE_LENGTH)
    }

    fun sanitizeHeader(name: String, value: String): String {
        return if (name.lowercase() in SENSITIVE_HEADERS) "***" else value
    }

    /** Deja los últimos 4 caracteres visibles (p.ej. para RUTs), el resto enmascarado. */
    fun maskIdentifier(value: String?): String? {
        if (value.isNullOrBlank()) return value
        if (value.length <= 4) return "***"

        return "***" + value.takeLast(4)
    }

    private const val MAX_TEXT_LENGTH = 4096
    private const val MAX_STACKTRACE_LENGTH = 12000
}
