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
  (`BrandPrimary`, `BrandPrimaryDark`, `BrandAccent`, `BrandSurfaceTint`,
  `StatusOnline`, `StatusChecking`, `StatusOffline`, `Success`, `Error`).
- Migración de base de datos `26 → 27` (no-op documentado) para completar la
  ruta continua `25 → 26 → 27 → 28`.
- `README.md` y `CHANGELOG.md`.

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

### Corregido
- **Riesgo de pérdida de datos**: hueco de migración `26 → 27` que, con el fallback
  destructivo global anterior, podía recrear (borrar) la base de datos al actualizar
  desde esquemas v25/v26.
- Colisión de color: el color de error era idéntico al de marca; ahora `Error` es
  un rojo distinguible (`#D32F2F`).
- API deprecadas en pantalla de consumo de datos (`Divider`, `ArrowBack`).
- `String.format` sin `Locale` en el formateo de tamaño en "Acerca de".

### Eliminado
- Código muerto verificado (0 usos): clúster `Barcode*`
  (`BarcodeService`, `BarcodeRepository`, `BarcodeDao`, `BarcodeEntity`),
  catálogos locales sin uso (`ProductCatalog`, `UnitCatalog`, `CatalogModels`),
  y utilidades sin uso (`TextFormatter`, `HashExt`/`String.sha256()`,
  `NetworkUsageCallFactory`).

## [1.4.2]

- Línea base de la aplicación Inventario para dispositivos Unitech (sin RFID):
  login por RUT, activación de dispositivo firmada, sincronización de catálogos,
  captura de inventario, inventarios pendientes/finalizados, historial de envíos,
  consumo de datos y actualización in-app.
