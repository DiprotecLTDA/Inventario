# ============================================================
# ProGuard / R8 rules
# Proyecto: com.diprotec.inventario
# ============================================================


# ============================================================
# Atributos necesarios para Retrofit, Moshi, Gson y Kotlin
# ============================================================

-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepattributes *Annotation*


# ============================================================
# Kotlin
# ============================================================

# Moshi con KotlinJsonAdapterFactory usa metadata de Kotlin.
-keep class kotlin.Metadata { *; }

# Evitar warnings comunes de Kotlin/Retrofit en minificación.
-dontwarn kotlin.Unit
-dontwarn kotlin.jvm.internal.**
-dontwarn kotlin.reflect.**


# ============================================================
# Retrofit
# ============================================================

# Mantener interfaces Retrofit y sus anotaciones.
# Tu ApiService vive en com.diprotec.inventario.data.remote.api.
-keep interface com.diprotec.inventario.data.remote.api.** { *; }

# Evitar warnings comunes de Retrofit.
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-dontwarn retrofit2.Platform$Java8


# ============================================================
# Moshi
# ============================================================

# Mantener todos los DTOs remotos usados por Retrofit/Moshi.
# Esto es clave para evitar ERR_MODELO_INVALIDO en release.
-keep class com.diprotec.inventario.data.remote.dto.** { *; }

# Mantener adaptadores Moshi generados si en el futuro usas generateAdapter = true.
-keep class **JsonAdapter { *; }
-keep class **JsonAdapter$* { *; }

# Evitar warnings comunes de Moshi.
-dontwarn com.squareup.moshi.**


# ============================================================
# Gson
# ============================================================

# El proyecto incluye Gson. Estas reglas evitan problemas si alguna clase
# se serializa/deserializa por reflexión.
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

-dontwarn com.google.gson.**


# ============================================================
# OkHttp / Okio
# ============================================================

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.ParametersAreNonnullByDefault


# ============================================================
# Room
# ============================================================

# Room normalmente genera las reglas necesarias, pero estas ayudan a evitar
# problemas si alguna entity se usa también por reflexión o serialización.
-keep class com.diprotec.inventario.data.local.entity.** { *; }

# Mantener base de datos Room.
-keep class com.diprotec.inventario.data.local.database.** { *; }

# No mantener DAOs completos salvo que sea necesario.
# Room genera implementaciones en compilación; mantener DAOs suele no hacer falta.
-dontwarn androidx.room.**


# ============================================================
# Hilt / Dagger
# ============================================================

# Hilt/Dagger normalmente agregan reglas automáticamente.
# Aquí solo se evitan warnings comunes sin forzar keep agresivo.
-dontwarn dagger.hilt.**
-dontwarn hilt_aggregated_deps.**
-dontwarn javax.inject.**


# ============================================================
# WorkManager
# ============================================================

# Tus workers están en com.diprotec.inventario.worker.
# WorkManager puede instanciar Workers por nombre de clase.
-keep class com.diprotec.inventario.worker.** { *; }

-dontwarn androidx.work.**


# ============================================================
# DataStore
# ============================================================

-dontwarn androidx.datastore.**


# ============================================================
# Jetpack Compose
# ============================================================

# Compose normalmente no requiere reglas manuales.
# Se dejan solo dontwarn conservadores.
-dontwarn androidx.compose.**
-dontwarn androidx.lifecycle.**
-dontwarn androidx.navigation.**


# ============================================================
# CameraX
# ============================================================

-dontwarn androidx.camera.**


# ============================================================
# Coil
# ============================================================

-dontwarn coil.**
-dontwarn kotlinx.coroutines.**


# ============================================================
# SDK local Unitech
# ============================================================

# Tu proyecto incluye app/libs/UnitechSDK_1.2.13.jar.
# Estas reglas son conservadoras para evitar que R8 rompa clases del SDK.
# Si Android Studio marca algún paquete como "Unresolved class name", puedes
# comentar solo esa línea, pero primero prueba compilando.
-keep class com.unitech.** { *; }
-keep class tw.com.ute.** { *; }
-keep class com.ute.** { *; }
-keep class unitech.** { *; }

-dontwarn com.unitech.**
-dontwarn tw.com.ute.**
-dontwarn com.ute.**
-dontwarn unitech.**


# ============================================================
# Seguridad / criptografía Android
# ============================================================

# Mantener clases propias de firma y keystore.
# No son necesariamente requeridas por R8, pero son sensibles en el flujo
# de activación/sesión del dispositivo.
-keep class com.diprotec.inventario.core.crypto.** { *; }
-keep class com.diprotec.inventario.core.key.** { *; }
-keep class com.diprotec.inventario.core.device.** { *; }
-keep class com.diprotec.inventario.core.network.ProtectedHeaders { *; }

-dontwarn java.security.**
-dontwarn javax.crypto.**


# ============================================================
# Modelos/configuración internos que pueden participar en reflexión
# ============================================================

-keep class com.diprotec.inventario.core.config.** { *; }
-keep class com.diprotec.inventario.data.local.catalog.** { *; }
-keep class com.diprotec.inventario.data.local.inventory.** { *; }


# ============================================================
# Application class
# ============================================================

-keep class com.diprotec.inventario.App { *; }



# ============================================================
# Scanner / Unitech integrado
# ============================================================

# Mantener clases propias relacionadas al scanner.
# CaptureInventoryScreen usa com.diprotec.inventario.ui.scan.UnitechScan
# para recibir directamente el broadcast del lector.
-keep class com.diprotec.inventario.ui.scan.** { *; }

# Mantener BroadcastReceivers si existen receivers propios o de librerías.
-keep class * extends android.content.BroadcastReceiver { *; }

# Paquetes posibles del servicio/SDK Unitech observados en logs.
-keep class com.menglong.** { *; }
-keep class menglong.** { *; }

-dontwarn com.menglong.**
-dontwarn menglong.**


# ============================================================
# Fin
# ============================================================