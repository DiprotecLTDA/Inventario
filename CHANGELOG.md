# Changelog

Todos los cambios notables de este proyecto se documentan en este archivo.

El formato se basa en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/)
y el proyecto sigue versionado semántico.

## [Sin publicar]

Unificación visual y saneamiento de código de la edición Unitech (sin RFID).

### Añadido
- Componentes de UI compartidos en `ui/theme/InventoryComponents.kt`:
  `InventoryTopBar`, `StatusChip`, `SegmentedToggle`, `OutlinedInfoCard`,
  `AppPrimaryButton` e `inventoryTextFieldColors()`.
- Tokens de color semánticos en `ui/theme/Color.kt`
  (`BrandPrimary`, `BrandPrimaryDark`, `BrandAccent`, `BrandSurfaceTint`) y de estado
  canónicos (`StatusOnline` #2E7D32, `StatusWarning` #F9A825, `StatusError` #C62828).
- Migración de base de datos `26 → 27` (no-op documentado) para completar la
  ruta continua `25 → 26 → 27 → 28`.
- `README.md` y `CHANGELOG.md`.

#### Alineación con los estándares de la app Zebra TC27
- **`core/message/AppMessages.kt`**: fuente única de mensajes de usuario, agrupados por
  área; los mensajes con interpolación se exponen como funciones de formato idéntico.
- **Contratos de red** (`core/network/`): `BaseApiResponse` (envelope `Estado`/`Respuesta`/
  `Data`/`CodigoError`/`CorrelationId`), `ApiException` (con `httpCode`, `estado`,
  `codigoError`, `correlationId` y `cause`) y `ApiCallExecutor` (`@Singleton`), único punto
  de ejecución y parseo de errores de la API.
- **Escala tipográfica completa** en `ui/theme/Type.kt`: una sola `FontFamily` y los estilos
  que faltaban (`titleSmall`, `bodySmall`, `headlineMedium`).
- **`ui/theme/Shape.kt`** (`AppShapes`, radios 10/16/24 dp) y **`ui/theme/Dimens.kt`**
  (escala de espaciado, alturas, iconos, bordes y elevaciones).
- Duración opcional por evento en los mensajes flotantes (`FloatingMessageEvent.durationMillis`).

### Cambiado
- **Unificación de diseño**: botones, campos de texto, top-bars, tarjetas, chips de
  estado y selectores segmentados ahora usan los componentes compartidos.
- Barra de estado y degradados alineados al rojo de marca `#C0392B`.
- Esquema oscuro corregido para que el texto sea legible (`onBackground`/`onSurface`).
- Mensajes flotantes: el tipo `INFO` usa el ícono correcto; `SUCCESS`/`ERROR` con
  colores semánticos diferenciados.
- Fallback destructivo de Room acotado a versiones legacy previas a la 25
  (`fallbackToDestructiveMigrationFrom`), evitando el borrado silencioso de datos
  no sincronizados ante huecos de migración.
- Nombres de color renombrados de `Teal*`/`ButtonRed*` a tokens de marca reales.
- **Refactor DRY en `SyncService`**: helper genérico `syncCatalog(label, fetch, map, replace)`
  del que dependen los cinco catálogos (usuarios, reglas, ubicaciones, productos, unidades de
  medida); `syncInventariosRemotos` se mantiene aparte. Comportamiento, logs y excepciones
  idénticos.
- **Refactor DRY en workers**: nuevo `worker/SyncWorkRunner.kt` con el manejo común de errores
  (`IOException` → retry, HTTP ≥ 500 → retry / resto → failure, inesperado configurable).
  `CatalogSyncWorker`, `PendingInventorySyncWorker` y `StartupSyncWorker` lo usan preservando su
  semántica exacta de `failure`/`retry` (Pending reintenta ante error inesperado; los otros
  fallan).
- Indicador de sesión en `MainMenuScreen` usa el token `BrandPrimary` en vez de un color hex
  hardcodeado.
- **Estandarización de nombres de archivo** para coincidir con su clase:
  `DynamicBaseUrlinterceptor.kt` → `DynamicBaseUrlInterceptor.kt` y
  `UserRepositoryimpl.kt` → `UserRepositoryImpl.kt`.
- **Mensajes de usuario centralizados**: los literales de flotantes, de estado de UI
  (`errorMessage`/`successMessage`) y las validaciones de configuración repetidas de
  repositorios y `SyncService` pasan a referenciar `AppMessages`. Textos sin reformular.
- **Migración de toda la red JSON al `ApiCallExecutor`**: los 11 DTOs de respuesta implementan
  `BaseApiResponse`; las 11 firmas de `ApiService` devuelven `Response<T>`; los 6 repositorios
  y los servicios `SyncService`/`ActivateDeviceService`/`VersionService` ejecutan sus llamadas
  vía `apiCallExecutor.execute { ... }`. Se eliminó el manejo manual de `isSuccessful`/
  `errorBody`/`body` y el criterio de éxito quedó unificado (`Estado` 0 o 200).
  Los DTOs de *request* no se tocaron: su JSON de salida es idéntico.
- Homogeneizados a `@Json` + camelCase los DTOs de respuesta de login, activación, envío de
  capturas y finalización (antes usaban PascalCase crudo).
- `SyncWorkRunner` mapea `ApiException` preservando la semántica previa de reintentos:
  HTTP ≥ 500 → retry, 4xx → failure, `cause` de red → retry, resto → comportamiento propio de
  cada worker.
- `Theme.kt` cablea `shapes = AppShapes` junto a `colorScheme` y `typography`.
- Barra de estado, degradados y colores de estado alineados a los tokens canónicos.

### Corregido
- **Riesgo de pérdida de datos**: hueco de migración `26 → 27` que, con el fallback
  destructivo global anterior, podía recrear (borrar) la base de datos al actualizar
  desde esquemas v25/v26.
- Colisión de color: el color de error era idéntico al de marca; ahora `Error` es
  un rojo distinguible (`#D32F2F`).
- API deprecadas en pantalla de consumo de datos (`Divider`, `ArrowBack`).
- `String.format` sin `Locale` en el formateo de tamaño en "Acerca de".
- **`UserRepositoryImpl.replaceAllUsers` ahora es atómico**: usa `userDao.replaceAll(list)`
  (`@Transaction`) en vez de `clearAll()` + `upsertAll()` sueltos, evitando dejar la tabla de
  usuarios vacía si el proceso se interrumpe entre ambas llamadas. Consistente con el resto de
  los repositorios de catálogo.
- **Mensajes flotantes duplicados**: el host estaba montado dos veces (en `NavGraph` y en
  `CaptureInventoryScreen`), por lo que en la pantalla de captura cada mensaje se renderizaba
  dos veces. Ahora hay un host único en el `Box` raíz del `NavGraph`; la captura conserva sus
  800 ms mediante la duración por evento (el resto usa los 2000 ms globales).
- **Jerga técnica expuesta al usuario**: los errores de API mostraban cadenas del tipo
  `"… falló. Estado=…, CodigoError=…"`. Ahora se muestra el campo `Respuesta` del servidor y,
  si no viene, un mensaje seguro según el código HTTP; el detalle técnico queda en `Log`.
  Tampoco se expone texto de Retrofit/Moshi/Java: `UnknownHostException`, `SocketTimeoutException`
  e `IOException` se traducen a mensajes en español. `CancellationException` se relanza sin
  envolver, preservando la cancelación estructurada de corrutinas.
- **Tipografía inconsistente**: `bodySmall` y `titleSmall` se usaban sin estar definidos y caían
  al default de Material (otra familia y tamaño). Ahora están declarados en la escala.

### Eliminado
- Código muerto verificado (0 usos): clúster `Barcode*`
  (`BarcodeService`, `BarcodeRepository`, `BarcodeDao`, `BarcodeEntity`),
  catálogos locales sin uso (`ProductCatalog`, `UnitCatalog`, `CatalogModels`),
  y utilidades sin uso (`TextFormatter`, `HashExt`/`String.sha256()`,
  `NetworkUsageCallFactory`).
- **Suite de pruebas completa** y toda su configuración:
  - 10 pruebas unitarias (`app/src/test/`): `ActivateDeviceServiceTest`,
    `CaptureInventoryViewModelFinalizeTest`, `KeyFileReaderUnitTest`,
    `LoginViewModelSyncTest`, `MainDispatcherRule`, `SettingsViewModelSaveTest`,
    `SettingsViewModelValidationTest`, `SyncServiceCatalogFailureTest`,
    `SyncServicePendingInventoryTest`, `SyncServiceTest`.
  - 4 pruebas instrumentadas (`app/src/androidTest/`): `CaptureInventoryScreenTest`,
    `InventoryListScreenTest`, `KeyFileReaderInstrumentedTest`,
    `SettingsDataStoreInstrumentedTest`.
  - En `app/build.gradle.kts`: `testInstrumentationRunner`, el bloque `testOptions`
    y todas las dependencias `testImplementation`/`androidTestImplementation`
    (JUnit, MockK, Robolectric, Espresso, coroutines-test, compose ui-test) y los
    `debugImplementation` de `ui-test-manifest`.

### Seguridad
- Firma de release externalizada a `keystore.properties` (ignorada); se agregó la
  plantilla `keystore.properties.example`. El keystore `.jks` y las contraseñas de
  firma dejaron de estar versionados.

### Infraestructura
- `.gitignore` alineado a repositorio solo-fuente: ignora `*.apk/*.aab/*.dm`,
  `build/`, `/app/{free,debug,release}/`, `*.jks/*.keystore`, `keystore.properties`
  y logs de JVM. Regla `/Inventario/` anclada para no ignorar el paquete `inventario/`.
- Se dejaron de versionar los artefactos de build (`app/free/release/*`).

## [1.4.2]

- Línea base de la aplicación Inventario para dispositivos Unitech (sin RFID):
  login por RUT, activación de dispositivo firmada, sincronización de catálogos,
  captura de inventario, inventarios pendientes/finalizados, historial de envíos,
  consumo de datos y actualización in-app.
