package com.hitbosss.presentation.navigation

import android.net.Uri
import kotlinx.coroutines.launch

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
    // context: "global" | "g{id}" (grupo) | "e{id}" (evento) — para subir el HIT al contexto correcto.
    const val RECORD_HIT = "record_hit/{exercise}/{weight}/{context}"
    fun recordHit(exercise: String, weight: Double, context: String = "global") =
        "record_hit/$exercise/$weight/$context"

    // Editar/dividir vídeo tras grabar (el path del vídeo va URL-encoded por las barras).
    const val EDIT_VIDEO = "edit_video/{exercise}/{weight}/{context}/{video}"
    fun editVideo(exercise: String, weight: Double, context: String, videoPath: String) =
        "edit_video/$exercise/$weight/$context/${Uri.encode(videoPath)}"

    /**
     * Editar un HIT ya subido (desde el perfil): reusa EDIT_VIDEO con context = "edit{hitId}@{performedAt}".
     * videoPath es el fichero local ya descargado del vídeo remoto.
     */
    fun editUploadedHit(exercise: String, weight: Double, hitId: Int, performedAt: Double, videoPath: String) =
        "edit_video/$exercise/$weight/edit$hitId@$performedAt/${Uri.encode(videoPath)}"

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

    const val EDIT_GROUP = "edit_group/{id}"
    const val EDIT_EVENT = "edit_event/{id}"
    fun editGroup(id: Int) = "edit_group/$id"
    fun editEvent(id: Int) = "edit_event/$id"

    // type: "group" | "event"
    const val COMMUNITY_MEMBERS = "community_members/{type}/{id}"
    fun communityMembers(type: String, id: Int) = "community_members/$type/$id"

    const val SAVED_HITS = "saved_hits"
    const val CREATE_GROUP = "create_group"
    const val CREATE_EVENT = "create_event"
    const val CALCULATOR = "calculator"
    const val EDIT_PROFILE = "edit_profile"
    const val COMMUNITY_RULES = "community_rules"
    const val HIDDEN_HITS = "hidden_hits"
    const val TUTORIALS = "tutorials"
    // Tutorial enfocado a un ejercicio (desde "Cómo grabar tu HIT" del modal de subir).
    const val TUTORIALS_EXERCISE = "tutorials/{apiKey}"
    fun tutorialsExercise(apiKey: String) = "tutorials/$apiKey"
    const val PRIVACY_POLICY = "privacy_policy"

    // Detalle de evolución de Métricas. type: "weight" | "fat" | "muscle"
    const val METRIC_DETAIL = "metric_detail/{type}"
    fun metricDetail(type: String) = "metric_detail/$type"
}
