package com.diprotec.inventario.core.key

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith


/*
 * Pruebas instrumentadas para KeyFileReader.
 *
 * Validaciones realizadas:
 * 1. Extrae correctamente el claim "app" desde un JWT cuyo payload contiene app = "Inventario".
 * 2. Extrae correctamente el claim "app" cuando el token viene con prefijo "Bearer ".
 * 3. Retorna null cuando el token tiene formato inválido o no corresponde a un JWT válido.
 * 4. Maneja correctamente un JWT que no contiene el claim "app".
 * 5. Valida como verdadero un token cuyo claim app es "Inventario".
 * 6. Valida como verdadero un token cuyo claim app es "inventario", ignorando mayúsculas/minúsculas.
 * 7. Valida como falso un token cuyo claim app corresponde a otra aplicación.
 * 8. Valida como falso un token mal formado.
 * 9. Valida que existsInDownloads retorne false cuando se consulta un archivo inexistente.
 *
 * Estas pruebas no validan el parsing de credenciales en formato clave@@valor,
 * ya que eso se cubre en KeyFileReaderUnitTest dentro de src/test.
 */


@RunWith(AndroidJUnit4::class)
class KeyFileReaderInstrumentedTest {

    @Test
    fun extractJwtAppClaim_conAppInventario_debeRetornarInventario() {
        val token = jwtConApp("Inventario")

        val app = KeyFileReader.extractJwtAppClaim(token)

        assertEquals("Inventario", app)
    }

    @Test
    fun extractJwtAppClaim_conBearer_debeRetornarInventario() {
        val token = "Bearer ${jwtConApp("Inventario")}"

        val app = KeyFileReader.extractJwtAppClaim(token)

        assertEquals("Inventario", app)
    }

    @Test
    fun extractJwtAppClaim_tokenMalFormado_debeRetornarNull() {
        val app = KeyFileReader.extractJwtAppClaim("token_mal_formado")

        assertNull(app)
    }

    @Test
    fun extractJwtAppClaim_tokenSinClaimApp_debeRetornarStringVacioONull() {
        val token = jwtConPayload("""{"sub":"usuario"}""")

        val app = KeyFileReader.extractJwtAppClaim(token)

        assertTrue(app == null || app.isEmpty())
    }

    @Test
    fun isInventarioToken_conAppInventario_debeRetornarTrue() {
        val token = jwtConApp("Inventario")

        val result = KeyFileReader.isInventarioToken(token)

        assertTrue(result)
    }

    @Test
    fun isInventarioToken_conAppInventarioMinuscula_debeRetornarTrue() {
        val token = jwtConApp("inventario")

        val result = KeyFileReader.isInventarioToken(token)

        assertTrue(result)
    }

    @Test
    fun isInventarioToken_conOtraApp_debeRetornarFalse() {
        val token = jwtConApp("OtraApp")

        val result = KeyFileReader.isInventarioToken(token)

        assertFalse(result)
    }

    @Test
    fun isInventarioToken_tokenMalFormado_debeRetornarFalse() {
        val result = KeyFileReader.isInventarioToken("token_mal_formado")

        assertFalse(result)
    }

    @Test
    fun existsInDownloads_archivoInexistente_debeRetornarFalse() {
        val result = KeyFileReader.existsInDownloads("archivo_que_no_deberia_existir.key")

        assertFalse(result)
    }

    private fun jwtConApp(app: String): String {
        return jwtConPayload("""{"app":"$app"}""")
    }

    private fun jwtConPayload(payload: String): String {
        val header = """{"alg":"none","typ":"JWT"}"""

        val header64 = Base64.encodeToString(
            header.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )

        val payload64 = Base64.encodeToString(
            payload.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )

        return "$header64.$payload64."
    }
}