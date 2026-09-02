# ADR 004: R8 / ofuscación en release + keep-rules

**Estado:** Aceptada (2026-09-01)

## Contexto
Google Play marcaba la app con "Optimización por debajo del umbral — Ofuscación (1%)": el `release` tenía
`isMinifyEnabled = false`, así que R8 no ofuscaba ni encogía el código.

## Decisión
`release` (y por herencia `internal`) usan **`isMinifyEnabled = true`** con las keep-rules de
`app/proguard-rules.pro`. Se deja `shrinkResources = false` (menos riesgo; opcional a futuro para tamaño).

Keep-rules mínimas necesarias para esta app:
- **kotlinx.serialization**: keep de los `$$serializer` + `Companion` de los tipos `@Serializable`; además, por
  seguridad, keep completo de `data.remote.dto.**` y `domain.model.**` (fracción diminuta del código → la
  cobertura de ofuscación sigue siendo alta). Esto blinda el JSON de red.
- **enums serializados**: `values()`/`valueOf`.
- **Media3 GL effect custom**: keep de `GlitchEffect` (por si el wiring de efectos lo necesita).
- `-dontwarn` de okhttp/okio/retrofit/annotations.

## Consecuencias / regla de robustez (IMPORTANTE)
⚠️ **R8 solo corre en `release`.** Un fallo de keep-rules (p.ej. un DTO que R8 rompe) **NO aparece en `debug`**,
solo en release/tienda → es el caso típico de "algo viejo se rompe al meter algo nuevo".

**Al añadir/cambiar un DTO `@Serializable` o algo con reflexión:**
1. `./gradlew :app:assembleRelease` (o el gradle cacheado) debe pasar.
2. Debe generar `app/build/outputs/mapping/release/mapping.txt` (prueba de que ofusca).
3. Idealmente instalar el APK release en un dispositivo/emulador y comprobar que **deserializa** (login +
   carga de datos) sin `SerializationException`/`NoClassDefFound`/`VerifyError` en logcat.
4. Si algo peta solo en release, casi siempre es una keep-rule que falta → añádela en `proguard-rules.pro`.

Los nuevos paquetes `data.remote.dto.**` ya están cubiertos por el keep amplio, así que un DTO nuevo bajo ese
paquete está protegido por defecto. Si metes serialización fuera de esos paquetes, añade su keep.

Ver el skill `release-build` para el checklist completo (versión, firma, verificación).

## Guardar el mapping
Cada `mapping.txt` de release desofusca los crashes de **esa** versión en Play. Guardarlo/subirlo por versión.
