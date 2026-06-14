package com.hitbosss.domain.usecase

import javax.inject.Inject
import androidx.compose.foundation.layout.size

/**
 * Equivale a CheckMinimumVersionUseCase de iOS: compara la versión instalada con la mínima
 * que exige la API. Devuelve true si hay que forzar actualización.
 * En fallo de red NO bloquea (mismo criterio que iOS): devuelve false.
 */
class CheckMinimumVersionUseCase @Inject constructor() {

    operator fun invoke(currentVersion: String, minimumVersion: String): Boolean =
        compareVersions(currentVersion, minimumVersion) < 0

    /** -1 si a<b, 0 si igual, 1 si a>b. Compara por segmentos numéricos (1.2.0). */
    private fun compareVersions(a: String, b: String): Int {
        val pa = a.split(".").map { it.toIntOrNull() ?: 0 }
        val pb = b.split(".").map { it.toIntOrNull() ?: 0 }
        val n = maxOf(pa.size, pb.size)
        for (i in 0 until n) {
            val x = pa.getOrElse(i) { 0 }
            val y = pb.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return 0
    }
}
