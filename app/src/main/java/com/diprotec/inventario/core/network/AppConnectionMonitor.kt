package com.diprotec.inventario.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.diprotec.inventario.ui.connection.AppConnectionMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class AppConnectionMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun checkMode(): AppConnectionMode = withContext(Dispatchers.IO) {
        if (hasNetwork()) {
            AppConnectionMode.ONLINE_API
        } else {
            AppConnectionMode.LOCAL_ROOM
        }
    }

    fun observeMode(): Flow<AppConnectionMode> {
        return callbackFlow {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    trySend(Unit)
                }

                override fun onLost(network: Network) {
                    trySend(Unit)
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    trySend(Unit)
                }

                override fun onUnavailable() {
                    trySend(Unit)
                }
            }

            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            runCatching {
                cm.registerNetworkCallback(request, callback)
            }

            trySend(Unit)

            awaitClose {
                runCatching {
                    cm.unregisterNetworkCallback(callback)
                }
            }
        }
            .conflate()
            .map { checkMode() }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
    }

    private fun hasNetwork(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false

        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}