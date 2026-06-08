package com.hitbosss.domain.util

import kotlin.math.pow
import kotlin.math.roundToInt

/** Portado 1:1 de WilksCalculator.swift. Trabaja internamente en KG. */
object WilksCalculator {

    const val KG_TO_LBS = 2.20462

    private data class Coefficients(
        val a: Double, val b: Double, val c: Double,
        val d: Double, val e: Double, val f: Double,
    )

    private val male = Coefficients(
        a = -216.0475144, b = 16.2606339, c = -0.002388645,
        d = -0.00113732, e = 7.01863E-06, f = -1.291E-08,
    )

    private val female = Coefficients(
        a = 594.31747775582, b = -27.23842536447, c = 0.82112226871,
        d = -0.00930733913, e = 4.731582E-05, f = -9.054E-08,
    )

    /** bodyWeightKg y weightLiftedKg en kilogramos; gender = "male" | "female". */
    fun calculate(bodyWeightKg: Double, weightLiftedKg: Double, gender: String): Double {
        val c = if (gender.equals("female", ignoreCase = true)) female else male
        val pc = bodyWeightKg
        val denominator = c.a +
            c.b * pc +
            c.c * pc.pow(2) +
            c.d * pc.pow(3) +
            c.e * pc.pow(4) +
            c.f * pc.pow(5)
        val wilksCoefficient = 500.0 / denominator
        val wilksPoints = wilksCoefficient * weightLiftedKg
        return (wilksPoints * 100).roundToInt() / 100.0
    }
}
