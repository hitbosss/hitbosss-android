---
name: new-usecase
description: Crea un endpoint nuevo (repositorio + API) y, solo si hace falta, un use case. Uso: /new-usecase NombreUseCase Feature
disable-model-invocation: true
---

Crea la operación completa para: $ARGUMENTS

El nombre es la primera palabra; la feature/paquete la segunda.
Ejemplo: `/new-usecase GetUserProfile Profile`.

## 0. ¿Hace falta un use case?

**Un use case que solo hace `= repository.metodo(args)` NO se crea**: el ViewModel inyecta el
repositorio y lo llama directo. Se borraron 55 así; no los reintroduzcas.

Créalo solo si tiene lógica propia (combina fuentes, transforma parámetros, calcula, reintenta).
Referencia: `UploadHitUseCase` en `domain/usecase/HitUseCases.kt`.

## 1. Repositorio (esto sí, siempre)

- Interfaz en `domain/repository/<X>Repository.kt` — si ya existe, añade el método, NO la recrees.
- Impl `data/repository/<X>RepositoryImpl.kt`: `runCatching { api.loQueSea(...).toDomain() }`.
  **Sin `withContext(Dispatchers.IO)`**: las `suspend fun` de Retrofit ya salen del hilo principal
  solas; envuelve solo trabajo bloqueante de verdad (`Tasks.await` en `AuthRepositoryImpl`).
- DTOs `@Serializable` en `data/remote/dto/` + mapper `toDomain()` en `data/mapper/`.
- Endpoint en `data/remote/HitbosssApi.kt`; `@Binds` en `core/di/RepositoryModule.kt` si el
  repositorio es nuevo.
- Devuelve `Result<...>` — nada de excepciones sin capturar hacia el ViewModel.

## 2. Robustez R8 (si añades DTO nuevo)

Un DTO nuevo `@Serializable` bajo `data.remote.dto.**` ya está cubierto por las keep-rules (ADR-004). Si lo
pones fuera de ese paquete o usas reflexión, **compila `assembleRelease`** y valida que no rompe en release
(R8 solo corre en release, no en debug). Ver skill `release-build`. Y recuerda: **no commitear Android** hasta
que José lo diga.
