package com.hitbosss.presentation.navigation

/** Rutas del grafo de navegación (equivale a las Screen del AppCoordinator de iOS). */
object Routes {
    const val LAUNCH = "launch"
    const val FORCE_UPDATE = "force_update"
    const val WELCOME = "welcome"
    const val SIGN_IN = "sign_in"
    const val SIGN_UP = "sign_up"
    const val RECOVER_PASSWORD = "recover_password"
    const val COMPLETE_PROFILE = "complete_profile"
    const val MAIN = "main"
    const val SETTINGS = "settings"
    const val RECORD_HIT = "record_hit/{exercise}/{weight}"
    fun recordHit(exercise: String, weight: Double) = "record_hit/$exercise/$weight"

    const val GROUP_DETAIL = "group_detail/{id}"
    const val EVENT_DETAIL = "event_detail/{id}"
    const val EVENT_RANKING = "event_ranking/{id}"
    const val GROUP_RANKING = "group_ranking/{id}"
    fun groupDetail(id: Int) = "group_detail/$id"
    fun eventDetail(id: Int) = "event_detail/$id"
    fun eventRanking(id: Int) = "event_ranking/$id"
    fun groupRanking(id: Int) = "group_ranking/$id"

    const val USER_PROFILE = "user_profile/{userId}"
    fun userProfile(userId: String) = "user_profile/$userId"

    const val CREATE_GROUP = "create_group"
    const val CREATE_EVENT = "create_event"
    const val CALCULATOR = "calculator"
    const val EDIT_PROFILE = "edit_profile"
    const val COMMUNITY_RULES = "community_rules"
    const val TUTORIALS = "tutorials"
    const val PRIVACY_POLICY = "privacy_policy"
}
