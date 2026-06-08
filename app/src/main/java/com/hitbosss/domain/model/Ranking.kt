package com.hitbosss.domain.model

/** Modelos de dominio (sin dependencias de red/serialización). */

data class Measurement(
    val value: Double,
    val unit: String,
)

/** Categorías de ranking: oficial + por ejercicio, por deporte (mismas que iOS). */
enum class RankingCategory(val apiKey: String, val title: String, val sport: Sport) {
    // Nombres = ExerciseType.localizedName de iOS (crossfit en inglés: Snatch/Clean/Clean & Jerk).
    PlOfficial("officialWilksRanking", "Oficial", Sport.Powerlifting),
    Squat("squat", "Sentadilla", Sport.Powerlifting),
    BenchPress("benchpress", "Press banca", Sport.Powerlifting),
    Deadlift("deadlift", "Peso muerto", Sport.Powerlifting),
    SumoDeadlift("sumoDeadlift", "Peso muerto sumo", Sport.Powerlifting),
    CfOfficial("officialWilksRanking", "Oficial", Sport.Crossfit),
    Snatch("snatch", "Snatch", Sport.Crossfit),
    Clean("clean", "Clean", Sport.Crossfit),
    CleanAndJerk("cleanAndJerk", "Clean & Jerk", Sport.Crossfit);

    /** Ejercicio (apiValue) para subir un HIT desde esta categoría; en "Oficial" usa el primero del deporte. */
    val uploadExerciseKey: String
        get() = when (this) {
            PlOfficial -> Squat.apiKey
            CfOfficial -> Snatch.apiKey
            else -> apiKey
        }

    companion object {
        fun forSport(sport: Sport): List<RankingCategory> = entries.filter { it.sport == sport }
    }
}

data class RankingEntry(
    val rank: Int,
    val hitId: Int?,
    val userId: String,
    val username: String,
    val gender: String?,
    val score: Double,           // wilks (oficial = totalWilks; ejercicio = wilksScore)
    val lift: Measurement?,      // totalLift (oficial) o maxLift (ejercicio)
    val levelWeight: String?,
    val levelWilks: String?,
    val profilePicUrl: String?,
    val countryCode: String?,
    val videoUrl: String?,
    val performedAt: Double = 0.0, // segundo del vídeo a buscar (seek), igual que iOS
    val createdAt: Long = 0,       // timestamp unix del hit -> fecha mostrada
)

data class SportRanking(
    val sport: String,
    val byCategory: Map<RankingCategory, List<RankingEntry>>,
)
