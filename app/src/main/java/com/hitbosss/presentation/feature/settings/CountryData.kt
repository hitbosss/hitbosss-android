package com.hitbosss.presentation.feature.settings

import java.util.Locale

/** Lista de países (código ISO + nombre en español), generada desde Locale. */
object CountryData {
    data class Country(val code: String, val name: String)

    private val es = Locale("es")

    val all: List<Country> by lazy {
        Locale.getISOCountries()
            .map { Country(it.lowercase(), Locale("", it).getDisplayCountry(es)) }
            .filter { it.name.isNotBlank() && it.name != it.code.uppercase() }
            .sortedBy { it.name }
    }

    fun name(code: String): String =
        all.firstOrNull { it.code.equals(code, true) }?.name ?: code

    /** "Es (España)" */
    fun label(code: String): String {
        if (code.isBlank()) return ""
        return "${code.lowercase().replaceFirstChar { it.uppercase() }} (${name(code)})"
    }
}
