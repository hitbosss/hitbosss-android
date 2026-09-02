package com.hitbosss.presentation.feature.ranking

import com.hitbosss.domain.model.Measurement
import com.hitbosss.domain.model.RankingCategory
import com.hitbosss.domain.model.RankingEntry
import com.hitbosss.domain.model.SportRanking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regresión del invariante de feature-ranking.md: el bullet "Tú" (`currentUserEntry`) debe salir de la
 * lista ordenada/filtrada (`displayedEntries`) y su `rank` coincidir con la posición en esa lista bajo el
 * criterio de orden activo. El bug original leía la lista cruda del backend y el bullet quedaba desincronizado.
 *
 * Fixture con peso y puntos DISCRIMINANTES (el orden por peso != el orden por puntos):
 *   me: 150 kg / 90 pts   ·   b: 135 kg / 102 pts   ·   c: 140 kg / 100 pts
 *   por peso  desc: me(1), c(2), b(3)
 *   por points desc: b(1), c(2), me(3)
 * El `rank` que trae cada entry del backend se pone a la posición-por-peso a propósito (para que el test
 * falle si el bullet lo usa en vez de recalcular).
 */
class RankingUiStateTest {

    private fun entry(
        userId: String,
        weightKg: Double,
        score: Double,
        backendRank: Int,
        levelWilks: String = "intermediate",
    ) = RankingEntry(
        rank = backendRank,
        hitId = 1,
        userId = userId,
        username = userId,
        gender = "male",
        score = score,
        lift = Measurement(value = weightKg, unit = "kg"),
        levelWeight = "advanced",
        levelWilks = levelWilks,
        profilePicUrl = null,
        countryCode = "ES",
        videoUrl = null,
    )

    private fun state(order: RankingOrder) = RankingUiState(
        ranking = SportRanking(
            sport = "powerlifting",
            byCategory = mapOf(
                RankingCategory.Squat to listOf(
                    entry("me", weightKg = 150.0, score = 90.0, backendRank = 1),
                    entry("b", weightKg = 135.0, score = 102.0, backendRank = 3),
                    entry("c", weightKg = 140.0, score = 100.0, backendRank = 2),
                ),
            ),
        ),
        selectedCategory = RankingCategory.Squat,
        orderBy = order,
        currentUserId = "me",
    )

    @Test
    fun `por points la lista se ordena por score descendente y renumera`() {
        val s = state(RankingOrder.Points)
        assertEquals(listOf("b", "c", "me"), s.displayedEntries.map { it.userId })
        assertEquals(listOf(1, 2, 3), s.displayedEntries.map { it.rank })
    }

    @Test
    fun `el bullet Tu sale de la lista mostrada y coincide en posicion bajo Points`() {
        val s = state(RankingOrder.Points)
        val bullet = s.currentUserEntry
        val fromList = s.displayedEntries.firstOrNull { it.userId == "me" }
        // Es la MISMA entrada (por valor) que la de la lista mostrada, no la cruda del backend.
        // (displayedEntries es computada y hace .copy() por acceso, así que se compara por equals, no identidad.)
        assertEquals(fromList, bullet)
        // Bajo Points, "me" es el 3º, no el 1º que traía del backend.
        assertEquals(3, bullet?.rank)
    }

    @Test
    fun `el bullet Tu sigue al criterio de orden bajo Lift`() {
        val s = state(RankingOrder.Lift)
        assertEquals(listOf("me", "c", "b"), s.displayedEntries.map { it.userId })
        assertEquals(1, s.currentUserEntry?.rank) // por peso, "me" (150 kg) es el 1º
    }

    @Test
    fun `invariante generico bullet rank == posicion en displayedEntries`() {
        for (order in RankingOrder.entries) {
            val s = state(order)
            val expected = s.displayedEntries.indexOfFirst { it.userId == "me" } + 1
            assertEquals("orden $order", expected, s.currentUserEntry?.rank)
        }
    }

    @Test
    fun `con filtro activo se conserva la posicion GLOBAL, no se renumera 1 a N`() {
        // Por Points: b(#1), c(#2), me(#3). Un filtro de nivel deja fuera a c.
        // La lista mostrada debe conservar las posiciones globales [1, 3], NO renumerar a [1, 2] (paridad iOS).
        val s = RankingUiState(
            ranking = SportRanking(
                sport = "powerlifting",
                byCategory = mapOf(
                    RankingCategory.Squat to listOf(
                        entry("me", weightKg = 150.0, score = 90.0, backendRank = 1, levelWilks = "elite"),
                        entry("b", weightKg = 135.0, score = 102.0, backendRank = 3, levelWilks = "elite"),
                        entry("c", weightKg = 140.0, score = 100.0, backendRank = 2, levelWilks = "intermediate"),
                    ),
                ),
            ),
            selectedCategory = RankingCategory.Squat,
            orderBy = RankingOrder.Points,
            currentUserId = "me",
            selectedLevels = setOf("elite"), // deja fuera a "c" (intermediate)
        )
        assertEquals(listOf("b", "me"), s.displayedEntries.map { it.userId })
        assertEquals(listOf(1, 3), s.displayedEntries.map { it.rank }) // global, no [1, 2]
        assertEquals(3, s.currentUserEntry?.rank)
    }
}
