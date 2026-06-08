# HitBoss · Migración iOS → Android nativo

Migración de la app iOS (`Hitbosss_iOS`, SwiftUI + Clean Architecture) a **Android nativo**
(**Kotlin + Jetpack Compose**), manteniendo **estética y funcionamiento**. Misma API (la dockerizada).

---

## Estado actual

**✅ Fase 1 — Base del proyecto + design system + primera feature (hecha)**

| Bloque | Estado |
|--------|--------|
| Proyecto Gradle (Kotlin DSL, version catalog), Compose, Hilt | ✅ |
| Design system: **paleta completa** (Color.kt) + **tipografía Inter** (Type.kt) + tema | ✅ |
| Fuentes Inter (Regular/SemiBold/Italic) portadas | ✅ |
| Networking: Retrofit + OkHttp + entorno por buildType + `X-App-Version: 2` + token Firebase | ✅ |
| Feature **Ranking** end-to-end (DTO→mapper→repo→usecase→VM→Compose) contra el Docker local | ✅ |

**⏳ Pendiente:** el resto de features (ver checklist abajo) y el login con Firebase.

---

## Cómo abrir y ejecutar

1. **Android Studio** (Ladybug+). `File → Open` → carpeta `Hitbosss_Android`. Deja que sincronice Gradle
   (descargará dependencias; el wrapper está fijado a Gradle 8.9 / AGP 8.7.3).
2. **Levanta la API local** (el Docker que ya tienes): `cd ../hitbosss-api && docker compose up -d`.
3. **Emulador**: crea/inicia un emulador (API 26+). 
   > ⚠️ Clave: en el emulador, `localhost` es el propio emulador. Para llegar al Docker del Mac se usa
   > **`10.0.2.2`** — ya está configurado en `build.gradle.kts` (debug → `http://10.0.2.2:3001/api/`)
   > y permitido en `network_security_config.xml`. Es el equivalente al "localhost del simulador" de iOS.
4. **Run** (▶) con el buildType **debug**. Verás el **Ranking** con datos reales de Supabase PRE.

### Firebase (para el login, fase posterior)
El proyecto compila y los endpoints **públicos** (config, ranking, deeplink) funcionan **sin Firebase**.
Para el login hace falta registrar la app Android en el proyecto Firebase `powerranking-40b11`:
1. Firebase console → proyecto `powerranking-40b11` → añadir app **Android**, package **`com.hitbosss`**.
2. Añadir la **huella SHA-1** del keystore de debug (la da `keytool` sobre `~/.android/debug.keystore`).
3. Activar el proveedor **Google** en Authentication → Sign-in method (genera el OAuth web client).
4. Descargar **`google-services.json`** a `app/` (está gitignored).
5. Plugin `google-services` ya activado; el código lee el web client ID de `R.string.default_web_client_id`
   (lo genera el plugin desde el json) → **no hay que configurar `WEB_CLIENT_ID` a mano**.

> El plugin `google-services` está activo: el proyecto **no compilará hasta que añadas `google-services.json`**.

### Tooling de Claude (`.claude/`)
Se ha portado el setup de skills/specs del iOS, adaptado a Android:
- `CLAUDE.md` — arquitectura y convenciones Android.
- `.claude/skills/` — `/new-screen`, `/new-usecase`, `/new-test`, `/review` (adaptadas a Compose/Hilt).
- `.claude/specs/decisions/` — ADRs (Clean+MVVM, Firebase, entornos).
- `.claude/specs/migration-status.md` — **estado vivo de la migración** (qué está hecho y qué falta).
- `.claude/specs/features/_template.md` — plantilla para especificar cada feature antes de portarla.

---

## Mapeo de equivalencias iOS → Android

| iOS (SwiftUI) | Android (elegido) |
|---------------|-------------------|
| SwiftUI | Jetpack Compose |
| Clean Architecture + MVVM + Coordinator | Clean Architecture + MVVM + Navigation Compose |
| `@Observable` / ViewModels | `ViewModel` + `StateFlow` |
| `URLSession` + `Endpoint` | Retrofit + OkHttp |
| `Codable` DTOs | `@Serializable` (kotlinx.serialization) |
| Composition Root (factories DI) | **Hilt** (módulos `@Module`/`@Binds`) |
| `HBConstants`/`AppEnvironment` | `Environment` + `BuildConfig` por buildType |
| Firebase iOS SDK + GoogleSignIn | Firebase Android + Credentials Manager + Google ID |
| `Colors.xcassets` | `Color.kt` (tokens Compose) |
| `InterFont.swift` | `Type.kt` (`HitbosssType`) + `res/font` |
| `async/await` | Coroutines + `suspend` |
| Multipart upload (Multer) | OkHttp `MultipartBody` |
| `Localizable.xcstrings` (en/es) | `res/values/strings.xml` + `values-es/` |
| Deeplinks `hitbosss://` (`CFBundleURLSchemes`) | `intent-filter` en el Manifest (ya añadido) |

---

## Estructura de paquetes (Clean Architecture, espeja iOS)

```
com.hitbosss
├── core/
│   ├── di/            # NetworkModule, RepositoryModule (Hilt)
│   └── network/       # Environment, AuthInterceptor, TokenProvider
├── data/
│   ├── remote/        # HitbosssApi (Retrofit) + dto/
│   ├── mapper/        # DTO -> dominio
│   └── repository/    # *RepositoryImpl
├── domain/
│   ├── model/         # entidades de dominio
│   ├── repository/    # interfaces
│   └── usecase/       # casos de uso
└── presentation/
    ├── designsystem/theme/   # Color, Type, Theme  (+ components/ en fases siguientes)
    └── feature/<feature>/    # Screen + ViewModel + UiState
```

---

## Roadmap de features (por orden sugerido)

Cada feature replica su equivalente de `Hitbosss_iOS/.../Presentation/Features/`.

- [x] **Ranking** (slice de prueba: ranking oficial wilks)
- [x] **ForceUpdate** (`GET /config` + chequeo de versión + pantalla)
- [x] **Auth** (login Firebase + Google Sign-In vía Credentials Manager + check-user-exists)
- [x] **Navigation root** (Launch → ForceUpdate/Welcome/CompleteProfile/Main) con Navigation Compose
- [ ] **CompleteProfile** (registro: username, altura, peso, género, país) — actualmente stub
- [ ] **MainTab** (Ranking, Community, Hit, Profile, Settings)
- [ ] **Ranking** completo (filtros, por ejercicio, ranking del usuario, detalle de hit)
- [ ] **Profile** (perfil, info personal, fotos, social networks)
- [ ] **Hit** (grabación/selección de vídeo + subida multipart con progreso)
- [ ] **Community** (grupos y eventos: listado, detalle, miembros, crear/editar)
- [ ] **Settings**
- [ ] **Design system components** (botones, textfields, segmented, dropdowns, popups… ~26 de iOS)
- [ ] **i18n** (portar `Localizable.xcstrings` en/es a `strings.xml`)
- [ ] **Tests** (equivalentes a los de iOS: mappers, repos, use cases, viewmodels)

---

## Notas

- **Light-only**, igual que iOS (no se añade dark theme salvo que se pida).
- `minSdk = 26` cubre el grueso del parque y permite iconos adaptivos y APIs modernas.
- El `applicationId` de debug es `com.hitbosss.debug` (suffix) para poder tener debug y release a la vez.
- Esta es una migración grande: se avanza **feature a feature**, validando cada una contra la API local.
