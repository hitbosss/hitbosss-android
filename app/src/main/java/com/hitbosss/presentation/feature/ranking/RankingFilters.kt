package com.hitbosss.presentation.feature.ranking

/** Orden del ranking — igual que OrderByOption de iOS. */
enum class RankingOrder(val label: String) {
    Lift("Peso levantado"),
    Points("Points"),
}

/** Localización — igual que LocationOption de iOS. */
enum class RankingLocation(val label: String) {
    World("Mundial"),
    National("Nacional"),
    Current("Actual"),
}

/** Categorías de edad — igual que AgeCategoryOption de iOS. */
enum class AgeCategory(val label: String) {
    SubJunior("SubJunior (14 a 18 años)"),
    Junior("Junior (19 a 23 años)"),
    Open("Open (24 a 39 años)"),
    Masters("Masters (Más de 40 años)"),
}

/** Género — igual que GenderOption de iOS. */
enum class RankingGender(val label: String, val apiValue: String?) {
    Male("Hombre", "male"),
    Female("Mujer", "female"),
    Both("Ambos", null),
}
