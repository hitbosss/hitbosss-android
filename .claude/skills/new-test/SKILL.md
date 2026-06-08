---
name: new-test
description: Crea tests JUnit para una clase. Uso: /new-test NombreClase [Mapper|Repository|UseCase|ViewModel]
disable-model-invocation: true
---

Crea tests para: $ARGUMENTS

Primer argumento: la clase. Segundo: el tipo (Mapper | Repository | UseCase | ViewModel).
Tests en `app/src/test/java/com/hitbosss/...` (JUnit + kotlinx-coroutines-test + Turbine para Flows/StateFlow).

Según el tipo:
- **Mapper**: dado un DTO de ejemplo, verifica el modelo de dominio resultante (campos, orden, nulos).
- **Repository**: usa una `HitbosssApi` fake/mockk; verifica que mapea y que envuelve errores en `Result.failure`.
- **UseCase**: repositorio fake; verifica que delega y propaga éxito/fallo.
- **ViewModel**: use case fake; usa `runTest` + dispatcher de test; verifica las transiciones de `UiState`
  (loading → success/error) con Turbine.

Convenciones:
- Nombrado: `fun \`hace X cuando Y\`()`.
- Arrange / Act / Assert claros.
- Sin red real ni Firebase real: todo fake/mockk.
- Un fake mínimo > un mock complejo cuando sea posible.
