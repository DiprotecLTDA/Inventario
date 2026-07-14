# Inventario — Diprotec (edición Unitech sin RFID)

Aplicación Android para la **toma de inventario** en dispositivos de mano **Unitech**
(modelo de referencia **EA520_US**), usando el lector de códigos de barra por hardware del
equipo (sin RFID). Está diseñada para operar **offline-first**: captura local en Room y
sincronización en segundo plano contra la API de Diprotec cuando hay conectividad.

- **Paquete:** `com.diprotec.inventario`
- **Versión:** 1.0.0 (`versionCode` 10000)
- **APK generado:** `unitech_520_inventario_1.0.0.apk`
- **minSdk:** 26 · **targetSdk:** 30 · **compileSdk:** 34

---

## Tabla de contenido
- [Características](#características)
- [Arquitectura](#arquitectura)
- [Stack técnico](#stack-técnico)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Flujo de datos y sincronización](#flujo-de-datos-y-sincronización)
- [Seguridad de dispositivo](#seguridad-de-dispositivo)
- [Sistema de diseño](#sistema-de-diseño)
- [Compilación y ejecución](#compilación-y-ejecución)
- [Configuración inicial](#configuración-inicial)
- [Pruebas](#pruebas)

---

## Características

- **Inicio de sesión por RUT** con validación de dígito verificador.
- **Activación de dispositivo** contra el backend, con firma criptográfica de cabeceras.
- **Sincronización de catálogos**: usuarios, reglas, ubicaciones, productos, unidades de medida
  e inventarios remotos.
- **Captura de inventario** con lector Unitech por hardware, en dos modos:
  - **Unidad**: registra cada lectura como una unidad.
  - **Cantidad**: registra código + cantidad + unidad de medida.
- **Listado de capturas** agrupado / desagrupado, con eliminación mientras el inventario está abierto.
- **Inventarios pendientes y finalizados**, con reanudación de trabajo.
- **Sincronización en segundo plano** (WorkManager): catálogos, envío de capturas y finalización.
- **Historial de envíos** (sync log) y **consumo de datos** por origen, operación y endpoint.
- **Actualización in-app** (descarga de APK e instalación asistida).
- **Sesión con expiración** automática (3 horas de inactividad) y cierre al remover la tarea.

## Arquitectura

Arquitectura por capas con inyección de dependencias (Hilt) y patrón MVVM en la UI:

```
UI (Jetpack Compose + ViewModels)
        │  StateFlow / eventos
Servicios de dominio (SyncService, AuthService, VersionService, …)
        │
Repositorios (interfaz + Impl)
        │
Fuentes de datos:  Room (local)  ·  Retrofit/Moshi (remoto)  ·  DataStore (settings)
```

- **UI**: pantallas `@Composable` sin lógica de negocio; el estado vive en los `ViewModel`.
- **Servicios**: orquestan casos de uso (sincronización, autenticación, versiones, actualización).
- **Repositorios**: exponen interfaces; las `*Impl` combinan API + DAO.
- **DI**: `di/AppModule.kt` provee base de datos, red, repositorios y utilidades.

## Stack técnico

| Área | Tecnología |
|------|------------|
| Lenguaje | Kotlin (coroutines / Flow) |
| UI | Jetpack Compose + Material 3 |
| DI | Hilt (Dagger) |
| Persistencia local | Room + DataStore Preferences |
| Red | Retrofit + Moshi + OkHttp |
| Trabajo en segundo plano | WorkManager (Hilt Worker) |
| Escáner | Lector de hardware Unitech (`UnitechScan`) |

## Estructura del proyecto

```
app/src/main/java/com/diprotec/inventario/
├── core/          # auth, config, crypto, device, format, key, network, session, validator
├── data/
│   ├── local/     # Room (dao, entity, database), DataStore, modelos de inventario
│   ├── remote/    # ApiService + DTOs
│   ├── mappers/   # DTO ↔ Entity
│   └── repository/# Interfaces + implementaciones
├── di/            # AppModule (Hilt)
├── service/       # SyncService, AuthService, VersionService, UpdateService, …
├── ui/            # Pantallas Compose + theme + componentes compartidos
└── worker/        # CatalogSyncWorker, PendingInventorySyncWorker, StartupSyncWorker
```

## Flujo de datos y sincronización

1. **Arranque** (`StartupGate`): valida sesión/actualización y dispara `StartupSyncWorker`.
2. **Catálogos** (`CatalogSyncWorker`): descarga y reemplaza catálogos locales de forma periódica.
3. **Captura**: se guarda en Room (`inventory_items`) marcada como no sincronizada.
4. **Envío** (`PendingInventorySyncWorker`): agrupa capturas por inventario/usuario y las envía;
   al finalizar un inventario, ejecuta `FinishInventario`.
5. **Bitácora**: cada envío (éxito o error) queda registrado en `sync_logs`.

La base de datos Room está en **versión 28**, con ruta de migración continua **25 → 26 → 27 → 28**.
El fallback destructivo está **acotado a versiones legacy previas a la 25**
(`fallbackToDestructiveMigrationFrom`), de modo que un hueco de migración futuro falle de forma
visible en lugar de borrar datos no sincronizados.

## Seguridad de dispositivo

- Las credenciales se cargan desde un archivo `inventario.key` (importado desde Configuración
  o al iniciar sesión) y se guardan en DataStore.
- El dispositivo se activa contra el backend y firma sus peticiones protegidas con una clave
  del **Android Keystore** (`core/key`, `core/crypto`), construyendo cabeceras firmadas
  (`ProtectedHeadersBuilder`) con `deviceSession`, `deviceSignature` y `deviceTimestamp`.

## Sistema de diseño

- **Marca:** rojo Diprotec `#C0392B`. Tokens semánticos centralizados en
  `ui/theme/Color.kt` (`BrandPrimary`, `BrandAccent`, `Error`, `StatusOnline/Checking/Offline`, …).
- **Componentes compartidos** en `ui/theme/InventoryComponents.kt`: `InventoryTopBar`,
  `StatusChip`, `SegmentedToggle`, `OutlinedInfoCard`, `AppPrimaryButton` e
  `inventoryTextFieldColors()`.
- Botones, campos, top-bars, tarjetas y chips están unificados en estos componentes.

## Compilación y ejecución

Requisitos: JDK 17, Android SDK (compileSdk 34).

```bash
# Debug (sin minificar)
./gradlew assembleFreeDebug

# Release firmada y minificada con R8 (requiere keystore.properties, ver abajo)
./gradlew assembleFreeRelease
```

El APK se genera como `unitech_520_inventario_<versión>.apk`. El nombre de la app instalada
es **Inventario** (`android:label` del flavor `free`).

> **Firma:** copia `keystore.properties.example` a `keystore.properties` (no se versiona)
> y completa `storeFile`, `storePassword`, `keyAlias` y `keyPassword`. Sin ese archivo,
> las builds de release se generan sin firmar.

> ⚠️ **Valida siempre con `assembleFreeRelease` los cambios que toquen red, DTOs, Moshi o
> reflexión.** En `debug` la minificación está desactivada (`isMinifyEnabled = false`), así que
> los fallos causados por R8 **no se reproducen en debug**. Toda clase que Moshi deserialice por
> reflexión necesita una regla `-keep` en `app/proguard-rules.pro`: hoy están cubiertos
> `data.remote.dto.**` y `core.network.ApiErrorResponse`. Si agregas un DTO fuera de esos
> paquetes, añade su regla o la app **crashea al iniciar solo en release**.

## Configuración inicial

1. Abrir **Configuración** e importar el archivo `inventario.key` (**Credenciales**).
2. Completar **Base URL**, **Empresa RUT** y **Código de activación**.
3. Guardar: el dispositivo se activa y queda listo para sincronizar e inventariar.

## Pruebas

El proyecto actualmente **no incluye una suite de pruebas automatizadas**. La validación se
realiza de forma manual sobre el dispositivo (compilación, captura y sincronización).
