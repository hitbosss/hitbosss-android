package com.hitbosss.presentation.feature.ranking

import com.hitbosss.domain.model.RankingEntry

/**
 * Orden del ranking por criterio (peso levantado o Points/Wilks) con desempate. Fuente única para el
 * ranking global y los de grupo/evento — antes estaba duplicado en tres sitios y la semántica podía divergir.
 */
val RankingOrder.comparator: Comparator<RankingEntry>
    get() = when (this) {
        RankingOrder.Points -> compareByDescending<RankingEntry> { it.score }.thenByDescending { it.lift?.value ?: 0.0 }
        RankingOrder.Lift -> compareByDescending<RankingEntry> { it.lift?.value ?: 0.0 }.thenByDescending { it.score }
    }

/**
 * Ordena la lista COMPLETA por el criterio y asigna la posición **global** (`rank` = índice+1 en el ranking
 * completo ordenado), como el `originalPosition` de iOS. Filtrar o buscar DESPUÉS de esto **no** renumera:
 * cada entrada conserva su posición real del ranking (1:1 con iOS `getDisplayedRanking`, que filtra tras
 * enumerar). El bullet "Tú" y las filas consumen esta misma lista → sus posiciones siempre coinciden.
 */
fun List<RankingEntry>.rankedBy(order: RankingOrder): List<RankingEntry> =
    sortedWith(order.comparator).mapIndexed { index, entry -> entry.copy(rank = index + 1) }
