package com.hitbosss.domain.util

/**
 * Datos competitivos de la pestaña Fuerza. Todo se calcula en el dispositivo a partir del
 * histórico propio (entrenamientos + hits) y del ranking del ejercicio que la app ya descarga.
 */
object ProgressStats {

    /** Mejora acumulada: mejor marca menos la primera registrada. Null con <2 puntos. */
    fun accumulatedImprovement(points: List<Pair<Long, Double>>): Double? {
        if (points.size < 2) return null
        val sorted = points.sortedBy { it.first }
        return sorted.maxOf { it.second } - sorted.first().second
    }

    /**
     * Ritmo de progreso en kg/mes: pendiente de la regresión lineal simple sobre
     * (tiempo unix seg, kg). Null con <2 puntos o si todos son del mismo instante.
     */
    fun progressRatePerMonth(points: List<Pair<Long, Double>>): Double? {
        if (points.size < 2) return null
        val n = points.size.toDouble()
        val meanX = points.sumOf { it.first.toDouble() } / n
        val meanY = points.sumOf { it.second } / n
        val denom = points.sumOf { (it.first - meanX) * (it.first - meanX) }
        if (denom == 0.0) return null
        val slopePerSecond = points.sumOf { (it.first - meanX) * (it.second - meanY) } / denom
        return slopePerSecond * SECONDS_PER_MONTH
    }

    /** Ventaja frente a la comunidad: tu mejor marca menos la media de las marcas del ranking. */
    fun communityAdvantage(ownBest: Double, communityLifts: List<Double>): Double? {
        if (communityLifts.isEmpty()) return null
        return ownBest - communityLifts.average()
    }

    /** Porcentaje de usuarios superados según tu posición (1-indexed) en un ranking de `total`. */
    fun percentSurpassed(rank: Int, total: Int): Double? {
        if (total <= 1 || rank < 1) return null
        return (total - rank).toDouble() / (total - 1) * 100.0
    }

    private const val SECONDS_PER_MONTH = 30.44 * 24 * 3600
}
