package com.diprotec.inventario.core.key

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/*
 * Pruebas unitarias para KeyFileReader.
 *
 * Este test valida la lectura y normalización de credenciales desde el contenido
 * de un archivo de configuración en formato texto.
 *
 * Validaciones realizadas:
 * 1. Limpia correctamente el prefijo "Bearer " del token cuando viene en mayúscula.
 * 2. Limpia correctamente el prefijo "bearer " del token cuando viene en minúscula.
 * 3. Elimina espacios al inicio y al final del token.
 * 4. Extrae correctamente token y API Key desde un archivo válido con:
 *    - authorization@@Bearer <token>
 *    - api_key@@<apiKey>
 * 5. Extrae correctamente el token cuando viene sin prefijo Bearer.
 * 6. Reconoce la clave "auth" como equivalente de token de autorización.
 * 7. Reconoce la clave "auth_token" como equivalente de token de autorización.
 * 8. Reconoce la clave "authtoken" como equivalente de token de autorización.
 * 9. Reconoce la clave "apikey" como equivalente de API Key.
 * 10. Reconoce la clave "x_api_key" como equivalente de API Key.
 * 11. Reconoce la clave "x-api-key" como equivalente de API Key.
 * 12. Retorna token y API Key nulos cuando el archivo no usa el separador esperado "@@".
 * 13. Retorna token nulo cuando el archivo solo contiene API Key.
 * 14. Retorna API Key nula cuando el archivo solo contiene token.
 * 15. Retorna token vacío cuando el archivo contiene la clave de autorización sin valor.
 * 16. Retorna API Key vacía cuando el archivo contiene la clave de API Key sin valor.
 *
 * Estas pruebas no validan lectura real desde URI, JWT, Android Context,
 * Downloads ni ContentResolver. Esas validaciones deben realizarse en pruebas
 * instrumentadas dentro de src/androidTest.
 */


class KeyFileReaderUnitTest {

    @Test
    fun sanitizeToken_conBearerMayuscula_debeEliminarPrefijo() {
        val result = KeyFileReader.sanitizeToken("Bearer abc123")

        assertEquals("abc123", result)
    }

    @Test
    fun sanitizeToken_conBearerMinuscula_debeEliminarPrefijo() {
        val result = KeyFileReader.sanitizeToken("bearer abc123")

        assertEquals("abc123", result)
    }

    @Test
    fun sanitizeToken_conEspacios_debeLimpiarEspacios() {
        val result = KeyFileReader.sanitizeToken("   Bearer abc123   ")

        assertEquals("abc123", result)
    }

    @Test
    fun extractAuthTokenAndApiKey_archivoValido_debeExtraerTokenYApiKey() {
        val raw = """
            authorization@@Bearer token_prueba
            api_key@@api_key_prueba
        """.trimIndent()

        val (token, apiKey) = KeyFileReader.extractAuthTokenAndApiKey(raw)

        assertEquals("token_prueba", token)
        assertEquals("api_key_prueba", apiKey)
    }

    @Test
    fun extractAuthTokenAndApiKey_authorizationSinBearer_debeExtraerToken() {
        val raw = """
            authorization@@token_prueba
            api_key@@api_key_prueba
        """.trimIndent()

        val (token, apiKey) = KeyFileReader.extractAuthTokenAndApiKey(raw)

        assertEquals("token_prueba", token)
        assertEquals("api_key_prueba", apiKey)
    }

    @Test
    fun extractAuthTokenAndApiKey_conClaveAuth_debeExtraerToken() {
        val raw = """
            auth@@Bearer token_prueba
            api_key@@api_key_prueba
        """.trimIndent()

        val (token, apiKey) = KeyFileReader.extractAuthTokenAndApiKey(raw)

        assertEquals("token_prueba", token)
        assertEquals("api_key_prueba", apiKey)
    }

    @Test
    fun extractAuthTokenAndApiKey_conClaveAuthToken_debeExtraerToken() {
        val raw = """
            auth_token@@Bearer token_prueba
            api_key@@api_key_prueba
        """.trimIndent()

        val (token, apiKey) = KeyFileReader.extractAuthTokenAndApiKey(raw)

        assertEquals("token_prueba", token)
        assertEquals("api_key_prueba", apiKey)
    }

    @Test
    fun extractAuthTokenAndApiKey_conClaveAuthtoken_debeExtraerToken() {
        val raw = """
            authtoken@@Bearer token_prueba
            api_key@@api_key_prueba
        """.trimIndent()

        val (token, apiKey) = KeyFileReader.extractAuthTokenAndApiKey(raw)

        assertEquals("token_prueba", token)
        assertEquals("api_key_prueba", apiKey)
    }

    @Test
    fun extractAuthTokenAndApiKey_conClaveApiKey_debeExtraerApiKey() {
        val raw = """
            authorization@@Bearer token_prueba
            apikey@@api_key_prueba
        """.trimIndent()

        val (token, apiKey) = KeyFileReader.extractAuthTokenAndApiKey(raw)

        assertEquals("token_prueba", token)
        assertEquals("api_key_prueba", apiKey)
    }

    @Test
    fun extractAuthTokenAndApiKey_conClaveXApiKeyGuionBajo_debeExtraerApiKey() {
        val raw = """
            authorization@@Bearer token_prueba
            x_api_key@@api_key_prueba
        """.trimIndent()

        val (token, apiKey) = KeyFileReader.extractAuthTokenAndApiKey(raw)

        assertEquals("token_prueba", token)
        assertEquals("api_key_prueba", apiKey)
    }

    @Test
    fun extractAuthTokenAndApiKey_conClaveXApiKeyGuionMedio_debeExtraerApiKey() {
        val raw = """
            authorization@@Bearer token_prueba
            x-api-key@@api_key_prueba
        """.trimIndent()

        val (token, apiKey) = KeyFileReader.extractAuthTokenAndApiKey(raw)

        assertEquals("token_prueba", token)
        assertEquals("api_key_prueba", apiKey)
    }

    @Test
    fun extractAuthTokenAndApiKey_archivoSinSeparador_debeRetornarNulos() {
        val raw = """
            authorization Bearer token_prueba
            api_key api_key_prueba
        """.trimIndent()

        val (token, apiKey) = KeyFileReader.extractAuthTokenAndApiKey(raw)

        assertNull(token)
        assertNull(apiKey)
    }

    @Test
    fun extractAuthTokenAndApiKey_archivoSinToken_debeRetornarTokenNulo() {
        val raw = """
            api_key@@api_key_prueba
        """.trimIndent()

        val (token, apiKey) = KeyFileReader.extractAuthTokenAndApiKey(raw)

        assertNull(token)
        assertEquals("api_key_prueba", apiKey)
    }

    @Test
    fun extractAuthTokenAndApiKey_archivoSinApiKey_debeRetornarApiKeyNula() {
        val raw = """
            authorization@@Bearer token_prueba
        """.trimIndent()

        val (token, apiKey) = KeyFileReader.extractAuthTokenAndApiKey(raw)

        assertEquals("token_prueba", token)
        assertNull(apiKey)
    }

    @Test
    fun extractAuthTokenAndApiKey_tokenVacio_debeRetornarTokenVacio() {
        val raw = """
            authorization@@
            api_key@@api_key_prueba
        """.trimIndent()

        val (token, apiKey) = KeyFileReader.extractAuthTokenAndApiKey(raw)

        assertEquals("", token)
        assertEquals("api_key_prueba", apiKey)
    }

    @Test
    fun extractAuthTokenAndApiKey_apiKeyVacia_debeRetornarApiKeyVacia() {
        val raw = """
            authorization@@Bearer token_prueba
            api_key@@
        """.trimIndent()

        val (token, apiKey) = KeyFileReader.extractAuthTokenAndApiKey(raw)

        assertEquals("token_prueba", token)
        assertEquals("", apiKey)
    }
}