package com.diprotec.inventario.core.network

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

interface BaseUrlProvider {
    fun getBaseUrl(): String
}

@Singleton
class DynamicBaseUrlInterceptor @Inject constructor(
    private val baseUrlProvider: BaseUrlProvider
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val configured = runCatching { baseUrlProvider.getBaseUrl().toHttpUrl() }.getOrNull()
            ?: return chain.proceed(req)

        val newUrl = req.url.newBuilder()
            .scheme(configured.scheme)
            .host(configured.host)
            .port(configured.port)
            .build()

        return chain.proceed(req.newBuilder().url(newUrl).build())
    }
}
