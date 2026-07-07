package com.hitbosss.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressStatsTest {

    private val month = (30.44 * 24 * 3600).toLong()

    @Test
    fun `mejora acumulada = mejor - primera`() {
        val points = listOf(0L to 100.0, month to 110.0, 2 * month to 130.0)
        assertEquals(30.0, ProgressStats.accumulatedImprovement(points)!!, 0.001)
        assertNull(ProgressStats.accumulatedImprovement(listOf(0L to 100.0)))
    }

    @Test
    fun `ritmo de progreso lineal de 10kg por mes`() {
        val points = listOf(0L to 100.0, month to 110.0, 2 * month to 120.0)
        assertEquals(10.0, ProgressStats.progressRatePerMonth(points)!!, 0.01)
        assertNull(ProgressStats.progressRatePerMonth(listOf(0L to 100.0)))
        assertNull(ProgressStats.progressRatePerMonth(listOf(0L to 100.0, 0L to 120.0)))
    }

    @Test
    fun `ventaja comunidad = propia - media`() {
        assertEquals(23.0, ProgressStats.communityAdvantage(123.0, listOf(90.0, 100.0, 110.0))!!, 0.001)
        assertNull(ProgressStats.communityAdvantage(100.0, emptyList()))
    }

    @Test
    fun `porcentaje de usuarios superados`() {
        // 1º de 101 supera al 100%; último no supera a nadie
        assertEquals(100.0, ProgressStats.percentSurpassed(1, 101)!!, 0.001)
        assertEquals(0.0, ProgressStats.percentSurpassed(101, 101)!!, 0.001)
        assertEquals(8.0, ProgressStats.percentSurpassed(24, 26)!!, 0.001)
        assertNull(ProgressStats.percentSurpassed(1, 1))
    }

    @Test
    fun `bmi y franjas`() {
        assertEquals(16.1, BmiCalculator.bmi(51.0, 178.0)!!, 0.1)
        assertEquals(BmiCalculator.Band.Low, BmiCalculator.band(16.1))
        assertEquals(BmiCalculator.Band.Normal, BmiCalculator.band(22.0))
        assertEquals(BmiCalculator.Band.Overweight, BmiCalculator.band(27.0))
        assertEquals(BmiCalculator.Band.Obese, BmiCalculator.band(31.0))
        assertNull(BmiCalculator.bmi(0.0, 178.0))
    }
}
