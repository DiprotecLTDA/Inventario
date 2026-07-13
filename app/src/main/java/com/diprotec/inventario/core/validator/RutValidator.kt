package com.diprotec.inventario.core.validator

object RutValidator {

    fun validateAndNormalize(input: String): String? {
        val clean = input.uppercase().replace("[^0-9K]".toRegex(), "")
        if (clean.length < 2) return null

        val run = clean.dropLast(1)
        val dv = clean.last()

        if (run.isEmpty() || !run.all { it.isDigit() } || run.length > 8) return null

        val expectedDv = computeDv(run)
        return if (expectedDv == dv) "$run-$dv" else null
    }

    private fun computeDv(run: String): Char {
        var suma = 0
        var mul = 2
        for (i in run.length - 1 downTo 0) {
            val dig = run[i] - '0'
            suma += dig * mul
            mul = if (mul == 7) 2 else mul + 1
        }
        val resto = suma % 11
        return when (resto) {
            1 -> 'K'
            0 -> '0'
            else -> ('0' + (11 - resto)).toChar()
        }
    }
}
