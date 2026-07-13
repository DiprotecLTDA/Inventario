package com.diprotec.inventario.core.format

object RutFormat {

    fun sanitizeInput(raw: String): String {
        val upper = raw.uppercase()

        val withoutInvalidChars = upper
            .filter { it.isDigit() || it == 'K' || it == '-' }

        val digits = StringBuilder()
        var dv: Char? = null
        var hasDash = false

        for (c in withoutInvalidChars) {
            when {
                c.isDigit() && !hasDash && digits.length < 8 -> {
                    digits.append(c)
                }

                c == '-' && digits.isNotEmpty() && !hasDash -> {
                    hasDash = true
                }

                hasDash && dv == null && (c.isDigit() || c == 'K') -> {
                    dv = c
                }

                !hasDash && digits.length in 1..8 && dv == null && c == 'K' -> {
                    hasDash = true
                    dv = c
                }
            }
        }

        return buildString {
            append(digits)

            if (hasDash) {
                append("-")
            }

            if (dv != null) {
                append(dv)
            }
        }
    }

    fun sanitizeForTyping(raw: String): String {
        return raw
            .uppercase()
            .filter { it.isDigit() || it == 'K' }
            .take(9)
    }

    fun formatInput(raw: String): String {
        val cleaned = raw
            .uppercase()
            .filter { it.isDigit() || it == 'K' }
            .take(9)

        if (cleaned.isEmpty()) return ""

        if (cleaned.length < 2) return cleaned

        val run = cleaned.dropLast(1)
        val dv = cleaned.last()

        return "$run-$dv"
    }

    fun isComplete(formatted: String): Boolean {
        val parts = formatted.split('-')
        if (parts.size != 2) return false

        val (base, dv) = parts

        return base.isNotEmpty()
                && base.length in 1..8
                && base.all { it.isDigit() }
                && dv.length == 1
                && (dv[0].isDigit() || dv[0].uppercaseChar() == 'K')
    }
}

object RutInput {

    fun sanitizeForTyping(raw: String): String {
        return RutFormat.sanitizeForTyping(raw)
    }

    fun formatForDisplay(raw: String): String {
        return RutFormat.formatInput(raw)
    }
}