# ADR 001: Clean Architecture + MVVM (Compose)

**Estado:** Aceptada

## Contexto
La app Android es una migración 1:1 de la app iOS, que usa Clean Architecture + MVVM + Coordinator. El
equipo necesita la misma separación de capas para poder portar feature a feature con paridad y testear cada
capa de forma aislada.

## Decisión
Replicar la arquitectura iOS en Android:
- **domain** (modelos, interfaces de repositorio, use cases) sin dependencias de framework.
- **data** (DTO `@Serializable`, mappers, `RepositoryImpl`, Retrofit `HitbosssApi`).
- **presentation** (Compose `Screen` + `ViewModel` con `StateFlow` + design system).
- **DI con Hilt** sustituye al Composition Root / factories de iOS.

Flujo unidireccional: `Composable → ViewModel → UseCase → Repository → API` (y la inversa para lecturas).

## Consecuencias
✅ Paridad de capas con iOS → portar es mecánico y revisable.
✅ Testable por capas (mappers, repos, use cases, viewmodels).
⚠️ Algo de boilerplate (interfaz + impl + binding) — se asume por consistencia.

## Enmienda (2026-08-26): el use case deja de ser obligatorio

55 de los 64 use cases solo hacían `= repository.metodo(args)`; se borran y el ViewModel inyecta
la interfaz de repositorio directamente. Sobreviven los 9 con lógica propia.

Las interfaces de `domain/repository/` y sus `@Binds` se quedan: son la frontera real entre capas.

**Regla:** un use case se escribe cuando tiene lógica. Si es reenvío puro, no se escribe.

## Equivalencias
| iOS | Android |
|-----|---------|
| Composition Root / Factory `.live`/`.mock` | Hilt `@Module`/`@Binds` |
| `UseCaseType`/`UseCaseLive` | `class XUseCase` + interfaz de repositorio |
| `ObservableObject`/`@Observable` | `ViewModel` + `StateFlow` |
| SwiftUI `View` | `@Composable Screen` |
