---
name: new-screen
description: Crea pantalla Compose + ViewModel (Hilt). Uso: /new-screen NombrePantalla Feature
disable-model-invocation: true
---

Crea una nueva pantalla para: $ARGUMENTS

El primer argumento es el nombre; el segundo (opcional) la feature/paquete.
Ejemplo: `/new-screen Profile Profile` → `presentation/feature/profile/`.

1. Lee `presentation/feature/ranking/` como referencia de estructura y convenciones.
2. Crea `presentation/feature/<feature>/<Nombre>Screen.kt`:
   - Composable **pasivo**, sin lógica de negocio.
   - Recibe `viewModel: <Nombre>ViewModel = hiltViewModel()`.
   - Lee el estado con `collectAsStateWithLifecycle()`.
   - Colores/tipografía SIEMPRE desde `designsystem/theme` (`HitbosssType`, tokens de `Color.kt`).
3. Crea `presentation/feature/<feature>/<Nombre>ViewModel.kt`:
   - `@HiltViewModel`, recibe sus use cases por `@Inject constructor`.
   - Expone `data class <Nombre>UiState(...)` vía `StateFlow`.
   - Toda la lógica aquí; lanza con `viewModelScope.launch`.
4. NO añadas navegación dentro de la pantalla — se cablea en `presentation/navigation/`.
5. Si la pantalla necesita un use case nuevo, créalo con `/new-usecase`.
6. Sigue exactamente las convenciones del `CLAUDE.md`.
7. Compara con la pantalla equivalente en `../Hitbosss_iOS/.../Features/` para mantener la estética.
