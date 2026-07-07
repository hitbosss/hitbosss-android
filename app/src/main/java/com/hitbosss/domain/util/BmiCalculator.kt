package com.hitbosss.domain.util

/** IMC y su clasificación (franjas del slider de Figma: 16–35). */
object BmiCalculator {

    enum class Band { Low, Normal, Overweight, Obese }

    /** IMC = kg / m². Null si faltan datos. */
    fun bmi(weightKg: Double, heightCm: Double): Double? {
        if (weightKg <= 0 || heightCm <= 0) return null
        val meters = heightCm / 100.0
        return weightKg / (meters * meters)
    }

    fun band(bmi: Double): Band = when {
        bmi < 18.5 -> Band.Low
        bmi < 25.0 -> Band.Normal
        bmi < 30.0 -> Band.Overweight
        else -> Band.Obese
    }
}
