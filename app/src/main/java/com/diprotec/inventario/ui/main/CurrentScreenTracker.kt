package com.diprotec.inventario.ui.main

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Ruta/pantalla activa, actualizada por el `NavGraph` en cada navegación. Se adjunta como
 * contexto a cada reporte de error técnico para saber en qué pantalla ocurrió.
 */
object CurrentScreenTracker {

    private val _currentScreen = MutableStateFlow<String?>(null)
    val currentScreen: StateFlow<String?> = _currentScreen

    fun update(route: String?) {
        _currentScreen.value = route
    }
}
