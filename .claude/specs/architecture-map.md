# Mapa de arquitectura — hitbosss-android

> **Léelo primero.** Índice navegable del repo para orientarte sin re-explorar (eficiencia de contexto).
> Para el *porqué* de las decisiones ver `.claude/specs/decisions/`. Para features concretas ver
> `.claude/specs/features/`. Convenciones de código en `CLAUDE.md`.

## Qué es
App Android (Kotlin + Jetpack Compose) **1:1 con la app iOS** (`../Hitbosss_iOS`). iOS es la **fuente de
verdad** de estética y comportamiento. Antes de comparar con iOS, **verifica que su rama esté actualizada**
(`git -C ../Hitbosss_iOS fetch && git status` — normalmente `develop`).

Núcleo del producto: **ranking de levantamientos (hits)** — grabar vídeo del lift, subirlo, competir en
rankings globales/grupos/eventos. Más: métricas, comunidad, perfil, ajustes.

## Capas (flujo unidireccional)
```
Composable (Screen)  →  ViewModel (@HiltViewModel, StateFlow)  →  Repository (interfaz domain / impl data)  →  HitbosssApi (Retrofit)
        UI pasiva            lógica + UiState                        runCatching { api...toDomain() }: Result      @Serializable DTO + mapper
```
Nunca saltar capas: un Composable no llama a un repositorio; un ViewModel no toca Retrofit.
**Use case solo si tiene lógica propia** (se borraron 55 de reenvío puro; ver ADR-001). Si es
`= repository.metodo()`, el ViewModel llama al repositorio directo.

## Mapa de paquetes (`app/src/main/java/com/hitbosss/`)
| Paquete | Contenido clave |
|---|---|
| `core/di/` | `NetworkModule` (Retrofit/OkHttp/Json), `RepositoryModule` (`@Binds` interfaz→impl) |
| `core/network/` | `AuthInterceptor` (añade el token Firebase a cada request) |
| `core/` | `RefreshCoordinator` (recarga por acción, ver más abajo) |
| `core/upload/` | `HitUploadManager` + `HitUploadService` (foreground service de subida de hit) |
| `data/remote/` | `HitbosssApi.kt` (57 endpoints), `dto/` (`@Serializable`) |
| `data/mapper/` | `toDomain()` DTO→modelo |
| `data/repository/` | `XRepositoryImpl` — `runCatching { api...toDomain() }` → `Result` |
| `domain/model/` | modelos de dominio (sin framework) |
| `domain/repository/` | interfaces de repositorio (la frontera real entre capas) |
| `domain/usecase/` | los ~9 use cases **con lógica** (ej. `UploadHitUseCase`) |
| `presentation/navigation/` | `AppNavHost.kt` + `Routes` (`Destinations.kt`) — TODA la navegación |
| `presentation/designsystem/theme/` | `HitbosssType`, `Color.kt` — **única** fuente de tipografía/color |
| `presentation/designsystem/components/` | componentes reutilizables (incl. `HitPopup`) |
| `presentation/feature/<x>/` | `XScreen` + `XViewModel` (+ `XUi`/`XState`) por feature |

Features (`presentation/feature/`): `auth`, `community`, `forceupdate`, `hit`, `launch`, `main`, `metrics`,
`profile`, `ranking`, `settings`.

## Wiring / infraestructura
- **Networking** (`core/di/NetworkModule.kt`): Retrofit + `retrofit2-kotlinx-serialization-converter`.
  `Json { ignoreUnknownKeys = true }` → **la API puede añadir campos sin romper** el cliente (robustez).
  `AuthInterceptor` mete el token Firebase. Logging `HEADERS` en debug / `NONE` en release.
  Base URL = `BuildConfig.API_BASE_URL` (distinta por buildType, ver abajo).
- **DI**: Hilt. Repos por `@Binds` en `RepositoryModule`; infra por `@Provides` en `NetworkModule`.
  ViewModels `@HiltViewModel` reciben repos/use cases por constructor. Nada de singletons manuales.
- **Navegación**: un solo `AppNavHost` con `startDestination = Routes.LAUNCH`. Flujo:
  LAUNCH → (force-update | auth: welcome/signin/signup/recover/complete-profile) → MAIN (tab bar:
  Ranking / Community / Metrics / Profile). Detalles de grupo/evento, create/edit, saved-hits, settings,
  edit-profile, etc. cuelgan de ahí. **La navegación se cablea SOLO aquí**, no dentro de las pantallas.
- **Recarga de datos**: `RefreshCoordinator` (`core/`) — invalidación **por acción** (p.ej. tras subir un
  hit se recargan ranking/perfil) + pull-to-refresh. Guarda de concurrencia, sin polling.
- **Auth**: Firebase Auth + Google Sign-In. El token va en cada request vía `AuthInterceptor`. `req.user.uid`
  es la identidad; el mismo uid se usa en todos los entornos (por eso el media de S3 se comparte entre
  prod/pre — ver feature-hit-pipeline).

## Build variants (`app/build.gradle.kts`, gitignored; template en `.example`)
| BuildType | API_BASE_URL | Uso |
|---|---|---|
| `debug` | `http://10.0.2.2:3001/api/` | Docker local del Mac (emulador). Firma debug. |
| `lan` | `http://<IP-LAN>:3001/api/` | Docker local vía WiFi (móvil físico). |
| `staging` | `https://pre.api.hitbosss.com/api/` | Preproducción (PRE). Firma debug, debuggable. |
| `release` | `https://api.hitbosss.com/api/` | **Producción. `isMinifyEnabled = true` (R8/ofuscación).** Firma con keystore de prod (`keystore.properties`). |
| `internal` | Render | `initWith(release)` → hereda R8+firma. |

**minSdk 26 · targetSdk 36 · versionName/versionCode en `defaultConfig`.**

## Invariantes que NO se rompen (ver specs/decisions)
1. **R8 solo en release**: un DTO nuevo `@Serializable` debe seguir deserializando bajo ofuscación. Al añadir
   DTOs, **compila `assembleRelease`** y valida (ver ADR-004 + skill `release-build`). Un bug de keep-rules
   NO aparece en debug, solo en release/tienda.
2. **Localización**: nada de strings de usuario hardcodeados; siempre recurso en `values/` (es) **y**
   `values-en/` (en). Inglés desde el `.xcstrings` de iOS (ver ADR-005 + skill `add-string`).
3. **Paridad iOS**: comportamiento y estética 1:1 con `../Hitbosss_iOS` (rama actualizada).
4. **`ignoreUnknownKeys`**: los DTOs no petan si la API añade campos; campos opcionales = nullable con default.

## Estado
Ver `.claude/specs/migration-status.md` (features + estado de ramas/deploy).
