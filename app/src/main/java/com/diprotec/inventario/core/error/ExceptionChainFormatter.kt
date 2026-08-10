package com.diprotec.inventario.core.error

import java.util.IdentityHashMap
import javax.mail.MessagingException

/**
 * Recorre la cadena de `cause` de una excepción (y también `MessagingException.nextException`,
 * porque JavaMail transporta el código SMTP real ahí, fuera de la cadena estándar de `cause`),
 * protegido contra ciclos y acotado en profundidad.
 */
object ExceptionChainFormatter {

    fun format(throwable: Throwable?): String {
        if (throwable == null) return ""

        val builder = StringBuilder()
        val visited = IdentityHashMap<Throwable, Boolean>()
        var current: Throwable? = throwable
        var depth = 0

        while (current != null && depth < MAX_DEPTH) {
            if (visited.containsKey(current)) break
            visited[current] = true

            if (depth > 0) builder.append(" <- ")
            builder.append("${current::class.java.name}: ${current.message}")

            val messagingNext = (current as? MessagingException)?.nextException
            if (messagingNext != null && !visited.containsKey(messagingNext)) {
                builder.append(
                    " [next: ${messagingNext::class.java.name}: ${messagingNext.message}]"
                )
            }

            current = current.cause
            depth++
        }

        return builder.toString()
    }

    private const val MAX_DEPTH = 8
}
