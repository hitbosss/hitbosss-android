package com.hitbosss.data.mapper

import com.hitbosss.data.remote.dto.BodyCompositionDto
import com.hitbosss.data.remote.dto.BodyHistoryPointDto
import com.hitbosss.data.remote.dto.GoalDto
import com.hitbosss.data.remote.dto.GoalHistoryEntryDto
import com.hitbosss.data.remote.dto.ProgressPhotoDto
import com.hitbosss.data.remote.dto.StrengthStatsDto
import com.hitbosss.data.remote.dto.TrainingEntryDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifica que los DTOs + mapper casan EXACTAMENTE con las respuestas del contrato /api/metrics
 * (mismos nombres de campo, {value,unit}→Double, y los derivados de mobile). Los JSON de abajo son
 * la forma real que produce el backend (ver OpenAPI / tests del servicio).
 */
class MetricsContractTest {

    private val json = Json { ignoreUnknownKeys = true }
    private fun near(a: Double?, b: Double) = a != null && kotlin.math.abs(a - b) < 0.05

    @Test fun bodyComposition_unwraps_and_derives() {
        val payload = """
            {"weight":{"value":78.5,"unit":"kg"},"height":{"value":180.0,"unit":"cm"},
             "muscle":{"value":38.9,"unit":"kg"},"fat":{"value":14.2,"unit":"kg"},
             "weightMeasuredAt":1782000000,"heightMeasuredAt":1782000000,
             "muscleMeasuredAt":1780500000,"fatMeasuredAt":1780500000}
        """.trimIndent()
        val d = json.decodeFromString<BodyCompositionDto>(payload).toDomain()
        assertTrue(near(d.weightKg, 78.5)); assertTrue(near(d.heightCm, 180.0))
        assertTrue(near(d.muscleKg, 38.9)); assertTrue(near(d.fatKg, 14.2))
        // derivados de mobile
        assertTrue("fat% = 14.2/78.5*100", near(d.fatPercent, 18.09))
        assertTrue("other = 78.5-38.9-14.2", near(d.otherKg, 25.4))
        assertEquals(1780500000L, d.fatMeasuredAt)
    }

    @Test fun bodyComposition_nullFields_areNull() {
        val d = json.decodeFromString<BodyCompositionDto>("""{"weight":null,"fat":{"value":10.0,"unit":"kg"}}""").toDomain()
        assertNull(d.weightKg)
        assertNull("sin peso no hay %", d.fatPercent)
        assertNull("sin peso/músculo no hay otras", d.otherKg)
    }

    @Test fun goal_onlyTarget() {
        val g = json.decodeFromString<GoalDto>("""{"metric":"weight","target":{"value":75.0,"unit":"kg"},"createdAt":1782000000}""").toDomain()!!
        assertEquals("weight", g.metric); assertTrue(near(g.targetValue, 75.0))
    }

    @Test fun goalHistory_reachedFrozen() {
        val list = json.decodeFromString<List<GoalHistoryEntryDto>>(
            """[{"id":42,"target":{"value":85.0,"unit":"kg"},"createdAt":1,"archivedAt":2,"reached":false},
                {"id":43,"target":{"value":90.0,"unit":"kg"},"createdAt":3,"archivedAt":null,"reached":null}]""",
        ).mapNotNull { it.toDomain() }
        assertEquals(2, list.size)
        assertEquals(false, list[0].reached); assertEquals(2L, list[0].archivedAt)
        assertNull("objetivo activo → reached null", list[1].reached)
    }

    @Test fun strengthStats_measurementAndPlainPercent() {
        val s = json.decodeFromString<StrengthStatsDto>(
            """{"accumulatedImprovement":{"value":30.0,"unit":"kg"},"progressRate":null,
                "communityAdvantage":{"value":12.0,"unit":"kg"},"rankingPercentage":68.0}""",
        ).toDomain()
        assertTrue(near(s.accumulatedImprovementKg, 30.0))
        assertNull(s.progressRateKgPerMonth)
        assertTrue(near(s.communityAdvantageKg, 12.0))
        assertTrue(near(s.rankingPercentage, 68.0))
    }

    // El mapper a dominio se borró con GetTrainingsUseCase (nadie lo llamaba); el DTO sigue vivo
    // porque PATCH /metrics/strength/trainings/{id} devuelve uno.
    @Test fun training_decodesWeightAndDate() {
        val t = json.decodeFromString<List<TrainingEntryDto>>(
            """[{"weight":{"value":117.5,"unit":"kg"},"performedAt":1782000000}]""",
        )
        assertEquals(1, t.size); assertTrue(near(t[0].weight?.value, 117.5)); assertEquals(1782000000L, t[0].performedAt)
    }

    @Test fun progressPhoto_fatPercentDerived() {
        val p = json.decodeFromString<List<ProgressPhotoDto>>(
            """[{"id":1,"photoUrl":"http://x/p.jpg","takenAt":1782000000,
                 "weight":{"value":80.0,"unit":"kg"},"fat":{"value":16.0,"unit":"kg"},"muscle":{"value":35.0,"unit":"kg"}}]""",
        ).mapNotNull { it.toDomain() }
        assertEquals(1L, p[0].id); assertTrue(near(p[0].weightKg, 80.0))
        assertTrue("fat% = 16/80*100 = 20", near(p[0].fatPercent, 20.0))
    }

    // El `id` (fila de body_log) es obligatorio: sin él no se puede editar ni borrar el punto
    // desde la gráfica (PATCH/DELETE /body-composition/{metric}/points/{pointId}).
    @Test fun history_point_ignoresUnitKeepsValue() {
        val h = json.decodeFromString<List<BodyHistoryPointDto>>(
            """[{"id":91,"value":78.5,"unit":"kg","measuredAt":1782000000}]""",
        ).mapNotNull { it.toDomain() }
        assertEquals(1, h.size)
        assertEquals(91L, h[0].id); assertTrue(near(h[0].value, 78.5)); assertEquals(1782000000L, h[0].measuredAt)
    }
}
