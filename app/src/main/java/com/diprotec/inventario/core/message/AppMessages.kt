package com.diprotec.inventario.core.message

object AppMessages {
    const val ERROR_SERVIDOR_GENERICO =
        "No se pudo completar la operación en el servidor."

    object Configuration {
        const val EMPRESA_RUT_NO_CONFIGURADO = "Empresa RUT no configurado"
        const val AUTHORIZATION_NO_CONFIGURADO = "Authorization no configurado"
        const val API_KEY_NO_CONFIGURADA = "X-API-KEY no configurada"
        const val FALTAN_PARAMETROS = "Faltan parámetros de configuración."
        const val FALTAN_PARAMETROS_DETALLE =
            "Faltan parámetros (Base URL / Empresa / API Key / Authorization). Configure la app."
    }

    object Device {
        const val NO_ACTIVADO = "El dispositivo no está activado"
        const val NO_ACTIVADO_CONFIGURACION =
            "El dispositivo no está activado. Debe activarlo en Configuración."
        const val NO_ACTIVADO_CORTO = "Dispositivo no activado."
    }

    object About {
        const val SIN_CONEXION_CONSULTAR_VERSION =
            "Sin conexión a internet. No se puede consultar versión."
        const val ACTUALIZACION_OBLIGATORIA_ENCONTRADA =
            "Se encontró una actualización obligatoria."
        const val ACTUALIZACION_OPCIONAL_ENCONTRADA =
            "Se encontró una actualización opcional."
        const val APLICACION_ACTUALIZADA = "La aplicación está actualizada."
        const val NO_SE_PUDO_CONSULTAR_VERSION = "No se pudo consultar versión"
    }

    object Download {
        const val NO_SE_PUDO_LEER_ESTADO = "No se pudo leer el estado de la descarga"
        const val ARCHIVO_NO_SE_PUDO_ABRIR =
            "La descarga terminó, pero no se pudo abrir el archivo"
        const val SIN_URL_ACTUALIZACION = "No hay URL de actualización disponible"
        const val SIN_NOMBRE_ARCHIVO = "No hay nombre de archivo para la actualización"
        const val INICIADA = "Descarga iniciada"
        const val PERMITIR_APPS_DESCONOCIDAS =
            "Permite instalar apps desconocidas para continuar"

        fun fallo(reasonText: String): String = "Descarga falló: $reasonText"

        fun noSePudoAbrirInstalador(detail: String?): String =
            "No se pudo abrir el instalador: $detail"
    }

    object DataUsage {
        const val REGISTROS_LIMPIADOS = "Registros limpiados"
    }

    object Login {
        const val RUT_INVALIDO = "RUT inválido (dígito verificador incorrecto)."
        const val INGRESE_CONTRASENA = "Ingrese la contraseña"
        const val INGRESO_EXITOSO = "Ingreso exitoso"

        fun resumenSincronizacion(
            users: Int,
            capturas: Int,
            finalizados: Int
        ): String =
            "Usuarios: $users | Capturas enviadas: $capturas | Cierres enviados: $finalizados"
    }

    object Credentials {
        const val NO_SE_PUDO_LEER_ARCHIVO = "No pude leer el archivo seleccionado."
        const val ARCHIVO_SIN_CREDENCIALES_VALIDAS =
            "El archivo no contiene authToken/apiKey válidos."
        const val ARCHIVO_FORMATO_INESPERADO =
            "El archivo no contiene authToken/apiKey en el formato esperado."
        const val CARGADAS_CORRECTAMENTE = "Credenciales cargadas correctamente"
        const val ERROR_GUARDANDO = "Error guardando credenciales."

        fun noSePudoGuardar(detail: String?): String =
            "No pude guardar credenciales: $detail"

        fun tokenNoCorresponde(appClaim: String?): String =
            "El token no corresponde a Inventario. app=${appClaim ?: "desconocida"}"
    }

    object Startup {
        const val VERIFICANDO_APLICACION = "Verificando aplicación…"
        const val PREPARANDO_SESION = "Preparando sesión del dispositivo…"
        const val CONSULTANDO_ACTUALIZACION = "Consultando actualización…"
        const val SIN_CONEXION = "Sin conexión a internet."
        const val NO_SE_PUDO_VERIFICAR_OFFLINE =
            "No se pudo verificar la aplicación. Puede continuar trabajando offline con los datos almacenados."
        const val NO_SE_PUDO_VERIFICAR = "No se pudo verificar la aplicación."
        const val ACTUALIZACION_OBLIGATORIA = "Actualización obligatoria disponible."
        const val NUEVA_VERSION = "Nueva versión disponible."
        const val DESCARGANDO_ACTUALIZACION = "Descargando actualización…"
        const val SINCRONIZANDO_USUARIOS = "Sincronizando usuarios…"
        const val NO_SE_PUDO_SINCRONIZAR_USUARIOS_OFFLINE =
            "No se pudo sincronizar usuarios. Puede continuar trabajando offline con los datos almacenados."
        const val NO_SE_PUDO_SINCRONIZAR_USUARIOS = "No se pudo sincronizar usuarios."
        const val MODO_OFFLINE = "Modo offline."
        const val APLICACION_LISTA = "Aplicación lista."
    }

    object Inventory {
        const val NO_ENCONTRADO = "No se encontró el inventario"
        const val SIN_INVENTARIOS_DISPONIBLES = "No hay inventarios disponibles para crear"
        const val SIN_USUARIO = "No hay usuario logueado"
        const val DEBE_SELECCIONAR_INVENTARIO = "Debe seleccionar un inventario"
        const val NO_ESTA_PENDIENTE = "El inventario no está pendiente"
        const val DEBE_INGRESAR_CODIGO = "Debe escanear o ingresar un código"
        const val CODIGO_DEMASIADO_LARGO = "El código no puede superar los 50 caracteres"
        const val CODIGO_INVALIDO = "El código solo puede contener letras y números"
        const val DEBE_SELECCIONAR_UBICACION = "Debe seleccionar una ubicación"
        const val CANTIDAD_INVALIDA = "Ingrese una cantidad válida mayor a 0"
        const val DEBE_SELECCIONAR_UNIDAD = "Debe seleccionar una unidad de medida"
        const val PRODUCTO_REGISTRADO = "Producto registrado"
        const val FINALIZADO_LOCALMENTE = "Inventario finalizado localmente"

        fun cierrePendiente(detail: String?): String =
            "Finalizado localmente. Pendiente de sincronizar cierre: $detail"
    }

    object Settings {
        const val INGRESE_BASE_URL = "Debes ingresar la Base URL."
        const val BASE_URL_PROTOCOLO = "La Base URL debe comenzar con http:// o https://"
        const val BASE_URL_SLASH_FINAL = "La Base URL debe terminar con /"
        const val INGRESE_RUT_EMPRESA = "Debes ingresar el RUT de la empresa."
        const val RUT_EMPRESA_INVALIDO = "RUT de empresa inválido"
        const val CARGUE_ARCHIVO_CREDENCIALES = "Debes cargar el archivo de credenciales."
        const val INGRESE_ACTIVATION_CODE = "Debes ingresar el Activation Code."
        const val ERROR_GUARDANDO_CONFIGURACION = "Error guardando configuración"
        const val CONFIGURACION_GUARDADA = "Configuración guardada correctamente"
    }

    object PendingSync {
        const val SIN_CONEXION =
            "Sin conexión a internet. No se puede sincronizar pendientes."
        const val FINALIZADA = "Sincronización de pendientes finalizada"
        const val FALLIDA = "No se pudo sincronizar pendientes"
        const val CANCELADA = "Sincronización cancelada"
        const val REINTENTO_AUTOMATICO =
            "No se pudo sincronizar ahora. Se reintentará automáticamente."
    }

    object ApiSync {
        const val CAPTURAS_ENVIADAS_CORRECTAMENTE = "Capturas enviadas correctamente"
        const val INVENTARIO_FINALIZADO_CORRECTAMENTE =
            "Inventario finalizado correctamente"
    }
}
