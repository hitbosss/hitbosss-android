package com.hitbosss.presentation.feature.ranking

import androidx.annotation.StringRes
import com.hitbosss.R

/** Orden del ranking — igual que OrderByOption de iOS. */
enum class RankingOrder(@StringRes val label: Int) {
    Lift(R.string.order_lift),
    Points(R.string.order_points),
}

/** Localización — igual que LocationOption de iOS. */
enum class RankingLocation(@StringRes val label: Int) {
    World(R.string.loc_world),
    National(R.string.loc_national),
    Current(R.string.loc_current),
}

/** Categorías de edad — igual que AgeCategoryOption de iOS. */
enum class AgeCategory(@StringRes val label: Int) {
    SubJunior(R.string.age_subjunior),
    Junior(R.string.age_junior),
    Open(R.string.age_open),
    Masters(R.string.age_masters);

    /** Mismos rangos que FilterRankingUseCase de iOS. */
    fun matches(age: Int): Boolean = when (this) {
        SubJunior -> age <= 18
        Junior -> age in 19..23
        Open -> age >= 24
        Masters -> age >= 40
    }
}

/** Género — igual que GenderOption de iOS. */
enum class RankingGender(@StringRes val label: Int, val apiValue: String?) {
    Male(R.string.common_male, "male"),
    Female(R.string.common_female, "female"),
    Both(R.string.gender_both, null),
}
