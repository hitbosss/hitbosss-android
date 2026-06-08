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

## Equivalencias
| iOS | Android |
|-----|---------|
| Composition Root / Factory `.live`/`.mock` | Hilt `@Module`/`@Binds` |
| `UseCaseType`/`UseCaseLive` | `class XUseCase` + interfaz de repositorio |
| `ObservableObject`/`@Observable` | `ViewModel` + `StateFlow` |
| SwiftUI `View` | `@Composable Screen` |
