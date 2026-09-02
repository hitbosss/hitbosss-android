# Feature: Ranking (global + grupos/eventos)

**Estado:** hecho · **Equivalente iOS:** `../Hitbosss_iOS/.../Presentation/Features/Ranking` (fuente de verdad)

## Qué hace
Lista de competidores por deporte (Powerlifting/Crossfit) y por ejercicio (pestañas: oficial + squat/bench/…).
Cada fila: foto, username, país, badge de nivel, peso levantado y Points (Wilks). Abajo, un **bullet "Tú"**
fijo con tu posición. Filtros (nivel, género, edad, ubicación) + búsqueda + **orden por peso o por Points**.
El mismo patrón se reutiliza en rankings de **grupo** y **evento** (comunidad).

## Ficheros
| Fichero | Rol |
|---|---|
| `presentation/feature/ranking/RankingViewModel.kt` | `RankingUiState` (data class con la lógica de filtro/orden como **propiedades computadas**) + `RankingViewModel` (carga, refresh, filtros) |
| `presentation/feature/ranking/RankingScreen.kt` | lista + fila `CurrentUserRow` (bullet "Tú", usa `entry.rank`) |
| `presentation/feature/ranking/RankingFilters.kt` / `RankingFilterSheets.kt` | enums de filtro/orden + hojas (sort sheet, filtros) |
| `presentation/feature/community/EventRankingScreen.kt` / `GroupRankingScreen.kt` | rankings de evento/grupo (misma lógica **inline**, reutilizan `CurrentUserRow`) |
| `data/.../RankingMapper.kt`, `domain/model/Ranking.kt` | DTO→dominio; `RankingEntry`, `SportRanking`, `RankingCategory` |

## Datos
- Endpoint: `GET ranking/:sport` → `SportRanking` con `byCategory[RankingCategory] -> List<RankingEntry>`.
- `RankingEntry`: `userId, username, countryCode, gender, birthDate, latitude/longitude, lift {value,unit},
  score` (=Points/Wilks), `levelWeight`, `levelWilks`, `rank`. Respeta `?unit`.
- Grupos/eventos: el ranking viene dentro del detalle del grupo/evento.

## ⚠️ INVARIANTE crítico (regresión que ya ocurrió — bug de Rustam)
**El bullet "Tú" y las filas de la lista deben salir de la MISMA lista ordenada/filtrada**, para que la
posición (y el badge de nivel) del bullet coincida SIEMPRE con la de la lista bajo el criterio activo.

- La lista visible = `displayedEntries`: `allEntries.filter{…}.sortedWith(orden actual).mapIndexed{ rank = i+1 }`.
  - Orden por Points → `compareByDescending{ score }.thenByDescending{ lift.value }`; por peso → al revés.
  - El badge de nivel usa `levelWilks` si orderByPoints, si no `levelWeight` (`RankingUiState.level()`).
- El bullet "Tú" **DEBE** ser `displayedEntries.firstOrNull { it.userId == currentUserId }`.
  **NO** `allEntries.firstOrNull{…}` (lista cruda, sin reordenar) → eso muestra el `rank` viejo del backend y
  el bullet queda desincronizado de la lista (parece que "no aplica" el orden). Fue exactamente el bug.
- En grupo/evento (`EventRankingScreen`/`GroupRankingScreen`) lo mismo: `currentUserEntry = entries.firstOrNull{…}`
  (la lista ordenada del ejercicio **seleccionado**), **no** `byCategory[officialCat].firstOrNull{…}`.

**Paridad iOS:** `RankingListView.swift` usa el mismo `displayedRanking` (con `originalPosition`) tanto para
las filas como para `CurrentUserRowView` → por eso en iOS siempre coinciden. Si tocas el orden/filtro,
mantén los dos consumiendo la misma lista.

## Divergencia iOS conocida (pendiente, NO reportada como bug)
Con **filtros activos**, Android renumera la lista `1..N` dentro del filtro (`mapIndexed`), mientras iOS
mantiene la posición del ranking **completo** ordenado (`getDisplayedRanking` en iOS filtra DESPUÉS de
`enumerated()`, conservando `originalPosition`). Sin filtros coinciden. Alinear requiere refactor de
`displayedEntries` (ordenar el full → asignar rank → filtrar sin renumerar). Decidir con el equipo antes de tocar.

## Reglas / detalles (paridad iOS)
- Categorías por deporte: `RankingCategory.forSport()`. Powerlifting oficial combina squat+bench+deadlift/sumo.
- Filtro de nivel: iOS filtra siempre por `levelWeight`; Android usa `level()` (cambia con el orden) — revisar
  si se busca paridad estricta.
- Ubicación "Current" = entradas a ≤10 km del GPS (Haversine). "National" = mismo país.
- `notParticipating`: card "Aún no participas" si completó < 3 grupos de ejercicios.

## Checklist al tocar ranking
- [ ] Bullet "Tú" sale de la lista ordenada/filtrada (invariante de arriba), en global **y** grupo/evento.
- [ ] Orden y badge de nivel coherentes entre fila y bullet.
- [ ] Comparado con `RankingViewModel.swift` / `RankingListView.swift` de iOS (rama actualizada).
- [ ] Test de `RankingUiState` verde (ver `app/src/test/.../ranking/RankingUiStateTest.kt`).
