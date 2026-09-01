package com.hitbosss.data.remote

import com.hitbosss.data.remote.dto.AppConfigDto
import com.hitbosss.data.remote.dto.BodyCompositionDto
import com.hitbosss.data.remote.dto.BodyHistoryPointDto
import com.hitbosss.data.remote.dto.CreateGoalRequestDto
import com.hitbosss.data.remote.dto.CreateStrengthGoalRequestDto
import com.hitbosss.data.remote.dto.CreateTrainingRequestDto
import com.hitbosss.data.remote.dto.GoalDto
import com.hitbosss.data.remote.dto.GoalHistoryEntryDto
import com.hitbosss.data.remote.dto.ProgressPhotoDto
import com.hitbosss.data.remote.dto.StrengthStatsDto
import com.hitbosss.data.remote.dto.EvolutionPointDto
import com.hitbosss.data.remote.dto.StrengthBestDto
import com.hitbosss.data.remote.dto.TrainingEntryDto
import com.hitbosss.data.remote.dto.TrendDto
import com.hitbosss.data.remote.dto.UpdateBodyCompositionDto
import com.hitbosss.data.remote.dto.UpdateBodyPointDto
import com.hitbosss.data.remote.dto.CreateUserRequestDto
import com.hitbosss.data.remote.dto.CreateUserResponseDto
import com.hitbosss.data.remote.dto.EventDetailDto
import com.hitbosss.data.remote.dto.EventsResponseDto
import com.hitbosss.data.remote.dto.GroupDetailDto
import com.hitbosss.data.remote.dto.GroupsResponseDto
import com.hitbosss.data.remote.dto.LoginResponseDto
import com.hitbosss.data.remote.dto.MessageResponseDto
import com.hitbosss.data.remote.dto.PersonalInfoDto
import com.hitbosss.data.remote.dto.ReportRequestDto
import com.hitbosss.data.remote.dto.SportRankingDto
import com.hitbosss.data.remote.dto.UploadHitResponseDto
import com.hitbosss.data.remote.dto.HiddenHitsResponseDto
import com.hitbosss.data.remote.dto.HitVisibilityRequestDto
import com.hitbosss.data.remote.dto.UserProfileDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Endpoints REST de la API HitBoss (mismos paths que el OpenAPI / la app iOS).
 * Se irá ampliando por feature en cada fase de la migración.
 */
interface HitbosssApi {

    @GET("config")
    suspend fun getConfig(): AppConfigDto

    /** GET /login. Requiere token Firebase (lo añade el AuthInterceptor). */
    @GET("login")
    suspend fun checkUserExists(): LoginResponseDto

    /** GET /users/profile/{id}. Público. */
    @GET("users/profile/{id}")
    suspend fun getUserProfile(@Path("id") userId: String): UserProfileDto

    /** GET /group/user/{id}. Grupos del usuario + de la comunidad. Requiere auth. */
    @GET("group/user/{id}")
    suspend fun getUserGroups(@Path("id") userId: String): GroupsResponseDto

    /** GET /event/user/{id}. Eventos del usuario + de la comunidad. Requiere auth. */
    @GET("event/user/{id}")
    suspend fun getUserEvents(@Path("id") userId: String): EventsResponseDto

    /** GET /group/{id}. Detalle de grupo. Requiere auth. */
    @GET("group/{id}")
    suspend fun getGroup(@Path("id") groupId: Int): GroupDetailDto

    /** GET /event/{id}. Detalle de evento. Requiere auth. */
    @GET("event/{id}")
    suspend fun getEvent(@Path("id") eventId: Int): EventDetailDto

    /** DELETE /group/{id}/leave — salir del grupo. */
    @DELETE("group/{id}/leave")
    suspend fun leaveGroup(@Path("id") groupId: Int): MessageResponseDto

    /** DELETE /group/{id} — eliminar el grupo (solo admin). */
    @DELETE("group/{id}")
    suspend fun deleteGroup(@Path("id") groupId: Int): MessageResponseDto

    /** DELETE /event/{id}/leave — salir del evento. */
    @DELETE("event/{id}/leave")
    suspend fun leaveEvent(@Path("id") eventId: Int): MessageResponseDto

    /** DELETE /event/{id} — eliminar el evento (solo admin). */
    @DELETE("event/{id}")
    suspend fun deleteEvent(@Path("id") eventId: Int): MessageResponseDto

    /** POST /group/{id}/report — denunciar un grupo (body opcional {comment}). */
    @POST("group/{id}/report")
    suspend fun reportGroup(@Path("id") groupId: Int, @Body body: ReportRequestDto): MessageResponseDto

    /** POST /event/{id}/report — denunciar un evento (body opcional {comment}). */
    @POST("event/{id}/report")
    suspend fun reportEvent(@Path("id") eventId: Int, @Body body: ReportRequestDto): MessageResponseDto

    /** DELETE /event/{id}/reset/{hitId} — resetear (anular) el hit de un participante (solo admin). */
    @DELETE("event/{id}/reset/{hitId}")
    suspend fun resetEventHit(@Path("id") eventId: Int, @Path("hitId") hitId: Int): MessageResponseDto

    /** PATCH /group/{id}/make-admin/{userId} — dar admin a un miembro (solo admin). */
    @PATCH("group/{id}/make-admin/{userId}")
    suspend fun makeGroupAdmin(@Path("id") groupId: Int, @Path("userId") userId: String): MessageResponseDto

    /** DELETE /group/{id}/delete-user/{userId} — expulsar a un miembro (solo admin). */
    @DELETE("group/{id}/delete-user/{userId}")
    suspend fun removeGroupMember(@Path("id") groupId: Int, @Path("userId") userId: String): MessageResponseDto

    /** PATCH /event/{id}/make-admin/{userId} — dar admin a un miembro (solo admin). */
    @PATCH("event/{id}/make-admin/{userId}")
    suspend fun makeEventAdmin(@Path("id") eventId: Int, @Path("userId") userId: String): MessageResponseDto

    /** DELETE /event/{id}/delete-user/{userId} — expulsar a un miembro (solo admin). */
    @DELETE("event/{id}/delete-user/{userId}")
    suspend fun removeEventMember(@Path("id") eventId: Int, @Path("userId") userId: String): MessageResponseDto

    /** PATCH /group/{groupId} (multipart, coverPic). Editar grupo (solo admin). */
    @Multipart
    @PATCH("group/{groupId}")
    suspend fun updateGroup(
        @Path("groupId") groupId: Int,
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part coverPic: MultipartBody.Part?,
    ): MessageResponseDto

    /** PATCH /event/{eventId} (multipart, coverPic). Editar evento (solo admin). */
    @Multipart
    @PATCH("event/{eventId}")
    suspend fun updateEvent(
        @Path("eventId") eventId: Int,
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part coverPic: MultipartBody.Part?,
    ): MessageResponseDto

    /** POST /group/ (multipart). exercises/officialSports como CSV. */
    @Multipart
    @POST("group/")
    suspend fun createGroup(
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part cover: MultipartBody.Part?,
    ): MessageResponseDto

    /** POST /event/ (multipart). exercises como CSV. */
    @Multipart
    @POST("event/")
    suspend fun createEvent(
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part cover: MultipartBody.Part?,
    ): MessageResponseDto

    /** POST /users/{id}/report — denunciar el perfil de un usuario (body opcional {comment}). */
    @POST("users/{id}/report")
    suspend fun reportUser(@Path("id") userId: String, @Body body: ReportRequestDto): MessageResponseDto

    /** DELETE /users/delete/{id} — eliminar cuenta. */
    @DELETE("users/delete/{id}")
    suspend fun deleteAccount(@Path("id") userId: String): MessageResponseDto

    /**
     * GET /ranking/{sport}. Público (sin auth).
     * La respuesta viene como mapa { "<sport>": { officialWilksRanking: {...} } }.
     */
    @GET("ranking/{sport}")
    suspend fun getRanking(
        @Path("sport") sport: String,
        @Query("unit") unit: String? = null,
    ): Map<String, SportRankingDto>

    /** GET /users/personalInformation/{id}. Requiere auth. */
    @GET("users/personalInformation/{id}")
    suspend fun getPersonalInfo(@Path("id") userId: String): PersonalInfoDto

    /** POST /users/{id}. Crea el perfil del usuario tras el registro en Firebase. Requiere auth. */
    @POST("users/{id}")
    suspend fun createUser(
        @Path("id") userId: String,
        @Body body: CreateUserRequestDto,
    ): CreateUserResponseDto

    /** PATCH /users/{id}/personal-info (multipart: campos + fotos). Actualiza el perfil. */
    @Multipart
    @PATCH("users/{userId}/personal-info")
    suspend fun updateProfile(
        @Path("userId") userId: String,
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part profilePic: MultipartBody.Part?,
        @Part coverPic: MultipartBody.Part?,
    ): okhttp3.ResponseBody

    /** POST /ranking/uploadhit (multipart: vídeo + campos). Requiere auth. */
    @Multipart
    @POST("ranking/uploadhit")
    suspend fun uploadHit(
        @Part video: MultipartBody.Part,
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
    ): UploadHitResponseDto

    /** POST /ranking/hit/{hitId}/report — denunciar un hit (body opcional {comment}). */
    @POST("ranking/hit/{hitId}/report")
    suspend fun reportHit(
        @Path("hitId") hitId: Int,
        @Body body: ReportRequestDto,
    ): MessageResponseDto

    /** DELETE /ranking/hit/{hitId} — eliminar un hit propio. Requiere auth. */
    @DELETE("ranking/hit/{hitId}")
    suspend fun deleteHit(@Path("hitId") hitId: Int): MessageResponseDto

    /** PATCH /ranking/hit/{hitId}/visibility — ocultar/mostrar un hit propio (reversible). */
    @PATCH("ranking/hit/{hitId}/visibility")
    suspend fun toggleHitVisibility(@Path("hitId") hitId: Int, @Body body: HitVisibilityRequestDto): MessageResponseDto

    /** GET /ranking/hits/hidden — hits ocultos del usuario (misma forma que participations). */
    @GET("ranking/hits/hidden")
    suspend fun getHiddenHits(
        @Query("unit") unit: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null,
    ): HiddenHitsResponseDto

    /**
     * PATCH /ranking/hit/{hitId} (multipart) — editar un hit propio: nuevo performedAt y,
     * opcionalmente, un vídeo recortado. Requiere auth.
     */
    @Multipart
    @PATCH("ranking/hit/{hitId}")
    suspend fun editHit(
        @Path("hitId") hitId: Int,
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part video: MultipartBody.Part?,
    ): MessageResponseDto

    // ============ Métricas (contrato iOS 663: /user/..., usuario del token) ============

    /** GET /metrics/body-composition — última composición (números en la unidad pedida). */
    @GET("metrics/body-composition")
    suspend fun getBodyComposition(@Query("unit") unit: String? = null): BodyCompositionDto

    /** GET /metrics/body-composition/history — serie de una métrica (weight/height/muscle/fat) en un rango. */
    @GET("metrics/body-composition/history")
    suspend fun getBodyCompositionHistory(
        @Query("metric") metric: String,
        @Query("range") range: String,
        @Query("unit") unit: String? = null,
    ): List<BodyHistoryPointDto>

    /** POST /metrics/body-composition — inserta una medición (solo campos provistos, planos + unit). */
    @POST("metrics/body-composition")
    suspend fun updateBodyComposition(@Body body: UpdateBodyCompositionDto, @Query("tzOffset") tzOffset: Int? = null): MessageResponseDto

    /** PATCH /metrics/body-composition/{metric}/points/{pointId} — edita el valor de un punto de la gráfica. */
    @PATCH("metrics/body-composition/{metric}/points/{pointId}")
    suspend fun updateBodyPoint(
        @Path("metric") metric: String,
        @Path("pointId") pointId: Long,
        @Body body: UpdateBodyPointDto,
    ): BodyHistoryPointDto

    /** DELETE /metrics/body-composition/{metric}/points/{pointId} — borra un punto de la gráfica. */
    @DELETE("metrics/body-composition/{metric}/points/{pointId}")
    suspend fun deleteBodyPoint(@Path("metric") metric: String, @Path("pointId") pointId: Long)

    /** GET /metrics/body-composition/trend — período actual vs anterior (weight/fat/muscle) para la Tendencia. */
    @GET("metrics/body-composition/trend")
    suspend fun getBodyTrend(
        @Query("period") period: String,
        @Query("unit") unit: String? = null,
        @Query("tzOffset") tzOffset: Int? = null,
    ): TrendDto

    /** GET /metrics/goals — objetivo activo de una body-metric (weight/fat/muscle). Null si no hay. */
    @GET("metrics/goals")
    suspend fun getGoal(@Query("metric") metric: String, @Query("unit") unit: String? = null): GoalDto?

    @POST("metrics/goals")
    suspend fun createGoal(@Body body: CreateGoalRequestDto, @Query("tzOffset") tzOffset: Int? = null): GoalDto

    @GET("metrics/goals/history")
    suspend fun getGoalHistory(@Query("metric") metric: String, @Query("unit") unit: String? = null): List<GoalHistoryEntryDto>

    @DELETE("metrics/goals/{goalId}")
    suspend fun deleteGoal(@Path("goalId") goalId: Long)

    /** GET /metrics/progress-photos — fotos de progreso (snapshot de composición en masa). */
    @GET("metrics/progress-photos")
    suspend fun getProgressPhotos(@Query("unit") unit: String? = null): List<ProgressPhotoDto>

    /** POST /metrics/progress-photos (multipart) — sube la foto; el servidor snapshotea la composición. */
    @Multipart
    @POST("metrics/progress-photos")
    suspend fun uploadProgressPhoto(@Part photo: MultipartBody.Part, @Query("tzOffset") tzOffset: Int? = null): ProgressPhotoDto

    /** DELETE /metrics/progress-photos/{photoId} — borra la foto (y su objeto en S3). */
    @DELETE("metrics/progress-photos/{photoId}")
    suspend fun deleteProgressPhoto(@Path("photoId") photoId: Long)

    /** GET /metrics/strength/{exercise}/stats — datos competitivos del ejercicio (server). */
    @GET("metrics/strength/{exercise}/stats")
    suspend fun getStrengthStats(@Path("exercise") exercise: String, @Query("unit") unit: String? = null): StrengthStatsDto

    /** POST /metrics/strength/{exercise}/trainings — marca manual del ejercicio. */
    @POST("metrics/strength/{exercise}/trainings")
    suspend fun createTraining(@Path("exercise") exercise: String, @Body body: CreateTrainingRequestDto): MessageResponseDto

    /** PATCH /metrics/strength/trainings/{trainingId} — edita un entrenamiento por id. */
    @PATCH("metrics/strength/trainings/{trainingId}")
    suspend fun updateTraining(@Path("trainingId") trainingId: Long, @Body body: CreateTrainingRequestDto): TrainingEntryDto

    /** DELETE /metrics/strength/trainings/{trainingId} — borra un entrenamiento por id. */
    @DELETE("metrics/strength/trainings/{trainingId}")
    suspend fun deleteTraining(@Path("trainingId") trainingId: Long)

    /** GET /metrics/strength/{exercise}/evolution — entrenos + HITs etiquetados (para la gráfica). */
    @GET("metrics/strength/{exercise}/evolution")
    suspend fun getStrengthEvolution(
        @Path("exercise") exercise: String,
        @Query("range") range: String,
        @Query("unit") unit: String? = null,
    ): List<EvolutionPointDto>

    /** GET /metrics/strength/bests — mejor marca por ejercicio (comparativa/ownBest). */
    @GET("metrics/strength/bests")
    suspend fun getStrengthBests(@Query("unit") unit: String? = null): List<StrengthBestDto>

    // --- Objetivo de fuerza por ejercicio ---
    @GET("metrics/strength/{exercise}/goal")
    suspend fun getStrengthGoal(@Path("exercise") exercise: String, @Query("unit") unit: String? = null): GoalDto?

    @POST("metrics/strength/{exercise}/goal")
    suspend fun createStrengthGoal(@Path("exercise") exercise: String, @Body body: CreateStrengthGoalRequestDto, @Query("tzOffset") tzOffset: Int? = null): GoalDto

    @GET("metrics/strength/{exercise}/goal/history")
    suspend fun getStrengthGoalHistory(@Path("exercise") exercise: String, @Query("unit") unit: String? = null): List<GoalHistoryEntryDto>

    @DELETE("metrics/strength/goals/{goalId}")
    suspend fun deleteStrengthGoal(@Path("goalId") goalId: Long)
}
