package com.hitbosss.presentation.feature.ranking

import androidx.annotation.StringRes
import com.hitbosss.R
import com.hitbosss.domain.model.Exercise
import com.hitbosss.domain.model.RankingCategory
import com.hitbosss.presentation.feature.ranking.titleRes

/**
 * Nombres de ejercicio/categoría localizados (es/en), sin meter `R` en el dominio.
 * En "Oficial" la pestaña usa el nombre del deporte (marca), no esta categoría.
 */
@StringRes
fun Exercise.titleRes(): Int = when (this) {
    Exercise.Squat -> R.string.exercise_squat
    Exercise.BenchPress -> R.string.exercise_bench
    Exercise.Deadlift -> R.string.exercise_deadlift
    Exercise.SumoDeadlift -> R.string.exercise_sumo
    Exercise.Snatch -> R.string.exercise_snatch
    Exercise.Clean -> R.string.exercise_clean
    Exercise.CleanAndJerk -> R.string.exercise_clean_jerk
}

@StringRes
fun RankingCategory.titleRes(): Int = when (this) {
    RankingCategory.PlOfficial, RankingCategory.CfOfficial -> R.string.category_official
    RankingCategory.Squat -> R.string.exercise_squat
    RankingCategory.BenchPress -> R.string.exercise_bench
    RankingCategory.Deadlift -> R.string.exercise_deadlift
    RankingCategory.SumoDeadlift -> R.string.exercise_sumo
    RankingCategory.Snatch -> R.string.exercise_snatch
    RankingCategory.Clean -> R.string.exercise_clean
    RankingCategory.CleanAndJerk -> R.string.exercise_clean_jerk
}

/** Recurso del nombre por apiValue de Exercise (chips de detalle de grupo/evento). null si no existe. */
@StringRes
fun exerciseTitleResByApi(api: String): Int? =
    Exercise.entries.firstOrNull { it.apiValue.equals(api, true) }?.titleRes()
