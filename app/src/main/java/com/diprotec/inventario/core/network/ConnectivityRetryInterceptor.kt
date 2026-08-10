package com.diprotec.inventario.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Primer interceptor de la cadena OkHttp: da resiliencia ante una red que recién se está
 * levantando (típico justo tras el arranque/activación) para no fallar por una condición
 * transitoria de DNS/conexión.
 *
 * - Sin ninguna red utilizable: falla rápido (sin reintentos) para no demorar el paso a modo
 *   offline.
 * - Con red presente pero aún no validada por el sistema: espera hasta [NETWORK_WAIT_MILLIS]
 *   a que se valide, cortando antes si la red desaparece.
 * - Ante fallos transitorios de DNS/conexión (`UnknownHostException`, `ConnectException`,
 *   `SocketTimeoutException`), reintenta hasta [MAX_ATTEMPTS] veces con backoff lineal.
 */
@Singleton
class ConnectivityRetryInterceptor @Inject constructor(
    @ApplicationContext private val context: Context
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (!hasAnyNetwork()) {
            Log.d(TAG, "Sin red disponible, sin reintentos")
            return chain.proceed(request)
        }

        if (!isNetworkValidated()) {
            waitForValidatedNetwork()
        }

        var attempt = 0
        var lastError: IOException? = null

        while (attempt < MAX_ATTEMPTS) {
            attempt++

            try {
                return chain.proceed(request)
            } catch (e: UnknownHostException) {
                lastError = e
            } catch (e: ConnectException) {
                lastError = e
            } catch (e: SocketTimeoutException) {
                lastError = e
            }

            if (attempt < MAX_ATTEMPTS) {
                Log.w(TAG, "Reintento $attempt/$MAX_ATTEMPTS tras fallo transitorio de red")
                runCatching { Thread.sleep(BACKOFF_MILLIS * attempt) }
            }
        }

        throw lastError ?: IOException("Fallo de red desconocido")
    }

    private fun hasAnyNetwork(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false

        return cm.getNetworkCapabilities(network) != null
    }

    private fun isNetworkValidated(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false

        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun waitForValidatedNetwork() {
        val deadline = System.currentTimeMillis() + NETWORK_WAIT_MILLIS

        while (System.currentTimeMillis() < deadline) {
            if (!hasAnyNetwork()) return
            if (isNetworkValidated()) return

            runCatching { Thread.sleep(POLL_INTERVAL_MILLIS) }
        }
    }

    companion object {
        private const val TAG = "ConnectivityRetry"
        private const val NETWORK_WAIT_MILLIS = 8_000L
        private const val POLL_INTERVAL_MILLIS = 250L
        private const val MAX_ATTEMPTS = 5
        private const val BACKOFF_MILLIS = 300L
    }
}
