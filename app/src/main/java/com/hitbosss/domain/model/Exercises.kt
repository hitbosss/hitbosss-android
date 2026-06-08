package com.hitbosss.domain.model

/** Deportes y ejercicios (rawValue camelCase, igual que ExerciseType de iOS). */
enum class Sport(val apiValue: String, val title: String) {
    Powerlifting("powerlifting", "Powerlifting"),
    Crossfit("crossfit", "CrossHIT"),
}

enum class Exercise(val apiValue: String, val title: String, val sport: Sport) {
    Squat("squat", "Sentadilla", Sport.Powerlifting),
    BenchPress("benchpress", "Press banca", Sport.Powerlifting),
    Deadlift("deadlift", "Peso muerto", Sport.Powerlifting),
    SumoDeadlift("sumoDeadlift", "Peso muerto sumo", Sport.Powerlifting),
    Snatch("snatch", "Snatch", Sport.Crossfit),
    Clean("clean", "Clean", Sport.Crossfit),
    CleanAndJerk("cleanAndJerk", "Clean & Jerk", Sport.Crossfit);

    companion object {
        fun forSport(sport: Sport): List<Exercise> = entries.filter { it.sport == sport }
    }
}
