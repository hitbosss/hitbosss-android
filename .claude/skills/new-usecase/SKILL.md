---
name: new-usecase
description: Crea un use case Kotlin + repositorio si hace falta. Uso: /new-usecase NombreUseCase Feature
disable-model-invocation: true
---

Crea un use case completo para: $ARGUMENTS

El nombre es la primera palabra; la feature/paquete la segunda.
Ejemplo: `/new-usecase GetUserProfile Profile`.

Lee `domain/usecase/GetRankingUseCase.kt` y `domain/repository/RankingRepository.kt` como referencia.

1. **`domain/usecase/<Nombre>UseCase.kt`**:
   - Clase con `@Inject constructor(private val repository: <X>Repository)`.
   - `suspend operator fun invoke(...): Result<Modelo>` (o `Flow<...>` si es stream).
   - Sin lógica de red/serialización; delega en el repositorio.
2. Si necesita un **repositorio nuevo**:
   - Interfaz en `domain/repository/<X>Repository.kt`.
   - Impl `data/repository/<X>RepositoryImpl.kt` (`@Inject constructor(api)`, `withContext(Dispatchers.IO) { runCatching { ... } }`).
   - DTOs `@Serializable` en `data/remote/dto/` + mapper `toDomain()` en `data/mapper/`.
   - Añadir el endpoint a `data/remote/HitbosssApi.kt`.
   - Bind en `core/di/RepositoryModule.kt` (`@Binds`).
   - Si el repositorio ya existe, NO lo recrees: añade el método.
3. Devuelve `Result<...>` (o un tipo de error de dominio) — nada de excepciones sin capturar hacia el ViewModel.
4. Sigue la nomenclatura del `CLAUDE.md`. Compara con el use case equivalente en iOS
   (`../Hitbosss_iOS/.../Domain/Use Cases/`).
