---
name: review
description: Revisa el diff actual contra las convenciones del proyecto Android. Uso: /review
disable-model-invocation: true
---

Revisa los cambios pendientes (`git diff`) de la app Android.

Comprueba, en este orden:

1. **Arquitectura**: respeta el flujo de capas (Composable→ViewModel→UseCase→Repository→API). Sin saltos
   (un Composable no llama a un repositorio; un ViewModel no usa Retrofit directamente).
2. **Design system**: cero colores/tipografías hardcodeadas — todo desde `designsystem/theme`.
3. **Estado/UI**: el Composable es pasivo; el estado vive en el `ViewModel` (`StateFlow`); se usa
   `collectAsStateWithLifecycle`. Nada de lógica de negocio en composables.
4. **Concurrencia**: IO en `Dispatchers.IO` (repositorio), nada bloqueante en el main thread, `viewModelScope`
   para corutinas de UI. Sin `GlobalScope`.
5. **DI**: dependencias por constructor + Hilt; nada de singletons manuales ni `object` con estado mutable.
6. **Errores**: los repositorios devuelven `Result`/error de dominio; no se filtran excepciones de red a la UI.
7. **Fidelidad con iOS**: la pantalla/feature equivalente en `../Hitbosss_iOS` se respeta en estética y comportamiento.
8. **Nulos/serialización**: DTOs con campos opcionales y `ignoreUnknownKeys`; nada peta si la API añade campos.
9. **Localización (ADR-005)**: cero texto de usuario hardcodeado — todo `stringResource` con recurso en
   `values/` (es) **y** `values-en/` (en). Marca cualquier `Text("…español…")`.
10. **R8/release (ADR-004)**: si el diff añade/cambia un DTO `@Serializable` o algo con reflexión, ¿pasa
    `assembleRelease` y está cubierto por las keep-rules? Un bug de R8 solo aparece en release, no en debug.
11. **Regresiones / invariantes de feature**: si tocas un subsistema con spec (`.claude/specs/features/`),
    verifica sus invariantes. En especial el **ranking**: el bullet "Tú" debe salir de la lista
    ordenada/filtrada (`displayedEntries`/`entries`), no de la cruda (feature-ranking.md).

Devuelve una lista priorizada (bloqueante / recomendado / nit) con el fichero:línea y la corrección concreta.
