# Feature: Pipeline de HIT (grabar → editar → subir → compartir)

**Estado:** hecho · **Equivalente iOS:** `../Hitbosss_iOS/.../Presentation/Features/Hit` (+ `Helpers/ExerciseVideoExporter.swift`)

Subsistema **más complejo y frágil** de la app (CameraX + Media3 Transformer + foreground service). Toca con
cuidado y valida contra iOS.

## Flujo completo
```
Grabar (CameraX)  →  EditVideo (recorte + PIN)  →  Subir (foreground service, %)  →  [ver]  →  Compartir (export Media3)
   RecordHit           EditVideoViewModel            HitUploadManager/Service           HitVideoDialog   HitActions
```

## 1. Grabar / Editar — `presentation/feature/hit/EditVideoViewModel.kt`
"Divide el vídeo en **dos partes**": un **PIN** separa la fase *Peso* de la fase *Ejercicio* (1:1 con
`EditVideoViewModel` de iOS). El usuario recorta (`startSec`/`endSec`) y coloca el pin (`pinSec`, absoluto).
- **`performedAt` = `pinSec - startSec`** (relativo al recorte, `coerceAtLeast(0)`). Es "el segundo del vídeo
  donde empieza el ejercicio" (⚠️ NO es una fecha; la fecha del hit es `created_at`).
- `upload(startSec, endSec, pinSec)` sube; `editHit(...)` edita un hit ya subido.
- Contexto: `"global"` | `"g{id}"` (grupo) | `"e{id}"` (evento) | `"edit{hitId}@{performedAt}"`.
- Vídeo remuxado antes de subir: quitar audio AMR y **conservar rotación** (`setOrientationHint`) para
  compat con iOS (si no, iOS no lo reproduce bien).

## 2. Subir — `core/upload/HitUploadManager.kt` + `HitUploadService.kt`
- **Foreground service** (`HitUploadService`, `foregroundServiceType="dataSync"`, permisos en el manifest) que
  mantiene viva la subida y muestra progreso — equivalente Android de la Live Activity de iOS.
- `HitUploadManager`: estado `{ inProgress, progress, phase }`. **Fases**: `Uploading` / `WaitingConnection`
  (reintenta al recuperar red). Guarda `if (inProgress) return` — una subida a la vez.
- **Idempotencia**: se envía `clientRequestId = hit.clientRequestId ?: hit.id` (UUID por intento). El backend
  deduplica con la columna `hit.client_request_id` (UNIQUE parcial) → reintentar no crea duplicados. **No
  quitar este campo.**
- Subida `multipart` (vídeo + campos). Multer del backend usa memoryStorage.
- **Notificación (lock screen)**: canal `hit_upload_v2` con `lockscreenVisibility = PUBLIC` +
  `setVisibility(PUBLIC)` + `CATEGORY_PROGRESS`, `IMPORTANCE_LOW`, `setOngoing`, acción Cancelar.
  ⚠️ El `lockscreenVisibility` de un canal **no se puede cambiar tras crearlo** → si cambias su config, **sube
  el id del canal** (por eso es `_v2`) y borra el viejo. (Xiaomi/HyperOS puede ocultarla igualmente por
  ajuste del SO, fuera de nuestro control.)

## 3. Ver — `presentation/feature/hit/HitVideoDialog.kt`
`HitVideoData.seekSeconds` = `performedAt` (inicio del ejercicio). El visor hace seek ahí. Ranking = bottom
sheet con drag; perfil = overlay negro full-screen (no unificar).

## 4. Compartir / Exportar — `presentation/feature/hit/HitActions.kt` (+ `HitVideoBranding.kt`, `HitVideoEffects.kt`)
`shareVideo → downloadToCache → exportBranded → shareFile`. Composición **Media3 Transformer**:
`[vídeo recortado + overlay header] → [outro 3s con zoom + glitch]` a 720×1280.
- ⚠️ **El clip empieza en el ejercicio, no en 0**: `MediaItem...setClippingConfiguration(setStartPositionMs(
  (seekSeconds*1000).coerceAtLeast(0)))`. Sin esto, se comparte desde el segundo 0 (fue un bug).
- **Paridad iOS** (`ExerciseVideoExporter.swift`): `startTime = performedAt`,
  `videoDuration = assetDuration - performedAt` (hasta el FINAL), `outro = 3s`. Android hace lo mismo. Por eso
  un vídeo compartido dura `(duración - performedAt) + 3s` — **el fichero exportado ya está recortado** (su
  segundo 0 es el ejercicio), así que en Instagram/WhatsApp empieza en el ejercicio. NO se recorta el final.
- `GlitchEffect` (`HitVideoEffects.kt`): `GlEffect` con shaders **inline** (no recursos raw). R8 lo renombra
  sin problema (keep-rule defensiva en `proguard-rules.pro`).
- `shareFile` usa `FileProvider` + `FLAG_GRANT_READ_URI_PERMISSION` (la app destino puede leer). El warning
  `ChooserPreview ... setClipData` es solo del thumbnail del share-sheet, cosmético; el compartir funciona.
- Fallback: si el export con efectos falla (shader), reintenta sin ellos; si no, comparte el vídeo crudo.

## Infra compartida
- **S3 compartido entre entornos**: fotos/vídeos van a un único bucket `hitbosss-media` con ruta determinista
  por uid (`users/{uid}/photos/profilePic.jpg`, `users/{uid}/videos/...`). Mismo uid Firebase en prod y pre →
  el mismo objeto se ve en ambos (no es bug; el media **no** está aislado por entorno).

## Checklist al tocar el pipeline
- [ ] `performedAt` sigue siendo relativo al recorte y se usa como pin/seek/clip de forma consistente.
- [ ] `clientRequestId` intacto (idempotencia).
- [ ] Remux/rotación conservados (compat iOS).
- [ ] Export: clip al inicio del ejercicio + outro 3s, comparado con `ExerciseVideoExporter.swift` (iOS actualizado).
- [ ] Notificación de subida visible en lock screen (canal versionado si cambia su config).
- [ ] Si añades un DTO de hit `@Serializable`, valida `assembleRelease` (R8) — ver ADR-004.
