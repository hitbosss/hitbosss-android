# Hitbosss Android — CLAUDE.md

App Android nativa (Kotlin + Jetpack Compose), **migración 1:1 de la app iOS** (`../Hitbosss_iOS`).
Mantener **estética y funcionamiento**. Misma API REST (la dockerizada en `../hitbosss-api`).

## Flujo de trabajo

| Quiero... | Hacer |
|---|---|
| Pantalla nueva | `/new-screen NombrePantalla Feature` |
| Use case nuevo | `/new-usecase NombreUseCase Feature` |
| Tests nuevos | `/new-test NombreClase [Mapper\|Repository\|UseCase\|ViewModel]` |
| Revisar cambios | `/review` |
| Decisión arquitectónica | Crear ADR en `.claude/specs/decisions/` |
| Portar una feature de iOS | Leer la feature en `../Hitbosss_iOS/.../Features/<X>`, crear spec en `.claude/specs/features/`, implementar |

Estado de la migración: `.claude/specs/migration-status.md`.

## Recuperación de contexto (sesión nueva)

Leer en este orden antes de tocar nada:
1. **`.claude/specs/architecture-map.md`** (mapa navegable del repo — orienta sin re-explorar) + este fichero + `.claude/specs/migration-status.md` (estado vivo — mantenerlo actualizado al completar features).
   - Si vas a tocar un subsistema con spec, léela: `.claude/specs/features/feature-ranking.md`, `feature-hit-pipeline.md`. Decisiones de robustez: `decisions/004-release-r8-obfuscacion.md`, `005-localizacion.md`.
2. `git log --oneline -10` y `git diff --stat` (trabajo en curso real).
3. `../hitbosss-api/PENDING.md`, `PENDING-2.1.0.md` y `CHANGELOG.md` (tareas API y contrato PRE vs PRO).
4. Si se toca iOS: `../Hitbosss_iOS/Hitbosss/CLAUDE.md`.

Repos hermanos: `../hitbosss-api` (API), `../Hitbosss_iOS` (referencia), `../Hitbosss_Android` (scaffold viejo, NO trabajar ahí).

## El producto
App de fuerza: el usuario compite en un ranking global grabando vídeos de sus ejercicios desde la app.
Deportes: **Powerlifting** y **Crossfit**. Núcleo = subida de hits (vídeo + stats) y rankings.

## Arquitectura — Clean Architecture + MVVM (espeja iOS)

```
// Lectura
API → DTO → Mapper → Model(dominio) → Repository → ViewModel → Composable

// Escritura (subida vídeo, crear grupos, registro…)
Composable → ViewModel → Repository → API

// UseCase solo se intercala cuando hay lógica propia (ver convenciones)
… → ViewModel → UseCase → Repository → …
```

| Capa iOS | Capa Android | Paquete |
|----------|--------------|---------|
| Composition Root (factories) | Hilt (`@Module`/`@Binds`) | `core/di/` |
| Data (DTO/Mapper/Networking/Repo) | igual | `data/` |
| Domain (Entities/UseCases/Repo types) | igual | `domain/` |
| Presentation (Features/Design System/Navigation) | igual | `presentation/` |

## Estructura de paquetes

```
com.hitbosss
├── core/{di, network}
├── data/{remote(+dto), mapper, repository}
├── domain/{model, repository, usecase}
└── presentation/{designsystem/theme, navigation, feature/<feature>}
```

## Convenciones (Android)

- **Composables de pantalla**: `XxxScreen`. View pasiva, sin lógica de negocio. Estado vía `ViewModel`.
- **ViewModels**: `XxxViewModel` con `@HiltViewModel`, exponen `StateFlow<XxxUiState>`. Lógica aquí, nunca en el Composable.
- **Use Cases**: SOLO si tienen lógica propia (combinan fuentes, transforman parámetros, calculan, reintentan). Clase con `@Inject constructor` y un `operator fun invoke(...)`. **Un use case que solo hace `= repository.metodo(args)` no se escribe**: el ViewModel inyecta el repositorio y llama directo (se borraron 55 así).
- **Repositorios**: interfaz en `domain/repository/`, impl `XxxRepositoryImpl` en `data/repository/`, bind en `core/di/RepositoryModule`. Son la dependencia normal de un ViewModel.
- **DTOs**: `@Serializable`, sufijo `Dto`, en `data/remote/dto/`. Mappers en `data/mapper/` (`fun XxxDto.toDomain()`).
- **Colores y tipografía**: SIEMPRE desde `presentation/designsystem/theme/` (`Color.kt`, `HitbosssType`). Nunca `Color(0x...)` suelto en UI.
- **Navegación**: la gestiona el `NavHost` (`presentation/navigation/`), no las pantallas.
- **Concurrencia**: `suspend` + Coroutines. **Sin `withContext(Dispatchers.IO)` alrededor de llamadas Retrofit** — sus `suspend fun` ya salen del hilo principal solas; envolver solo trabajo bloqueante de verdad (`Tasks.await` de Firebase, ficheros grandes). UI state en el main thread vía `StateFlow`.
- Comentarios en inglés/español cortos, solo cuando aportan.

## Decisiones tomadas (no abrir a debate)
- Clean Architecture + MVVM (ADR 001).
- Firebase Auth se mantiene aunque la DB sea Supabase (ADR 002).
- Entornos por **buildType**: debug→Docker local (`10.0.2.2:3001`), staging→PRE, release→PROD (ADR 003).
- Light-only (igual que iOS).
- Vídeo: no se sube desde galería, se graba en la app (igual que iOS).

## Flujos críticos (nunca pueden fallar)
- **Auth doble capa**: Firebase ≠ registro en la DB. Tras login Firebase → `GET /login` para ver si el usuario existe; si no, completar perfil. **Si la API falla, NO asumir que no está registrado** (no dejar al usuario en bucle).
- **Subida de vídeo**: es la prueba de competición; no puede perderse. Guardar un *local hit* para reintentar.

