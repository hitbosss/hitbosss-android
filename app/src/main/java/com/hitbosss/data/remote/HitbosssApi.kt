package com.hitbosss.data.remote

import com.hitbosss.data.remote.dto.AppConfigDto
import com.hitbosss.data.remote.dto.BodyLogDto
import com.hitbosss.data.remote.dto.CreateBodyLogRequestDto
import com.hitbosss.data.remote.dto.CreateGoalRequestDto
import com.hitbosss.data.remote.dto.CreateStrengthLogRequestDto
import com.hitbosss.data.remote.dto.CreateUserRequestDto
import com.hitbosss.data.remote.dto.GoalsResponseDto
import com.hitbosss.data.remote.dto.MetricGoalDto
import com.hitbosss.data.remote.dto.ProgressPhotoDto
import com.hitbosss.data.remote.dto.StrengthEntryDto
import com.hitbosss.data.remote.dto.StrengthHistoryDto
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

    // ============ Métricas (sección Métricas: Físico + Fuerza) ============

    /** GET /metrics/{id}/body-logs — histórico de composición corporal. Requiere auth. */
    @GET("metrics/{id}/body-logs")
    suspend fun getBodyLogs(
        @Path("id") userId: String,
        @Query("from") from: Long? = null,
        @Query("to") to: Long? = null,
    ): List<BodyLogDto>

    @POST("metrics/{id}/body-logs")
    suspend fun createBodyLog(@Path("id") userId: String, @Body body: CreateBodyLogRequestDto): BodyLogDto

    @DELETE("metrics/{id}/body-logs/{logId}")
    suspend fun deleteBodyLog(@Path("id") userId: String, @Path("logId") logId: Long)

    /** GET /metrics/{id}/goals — objetivo activo + historial (type=weight|strength). */
    @GET("metrics/{id}/goals")
    suspend fun getGoals(
        @Path("id") userId: String,
        @Query("type") type: String,
        @Query("exercise") exercise: String? = null,
    ): GoalsResponseDto

    @POST("metrics/{id}/goals")
    suspend fun createGoal(@Path("id") userId: String, @Body body: CreateGoalRequestDto): MetricGoalDto

    @DELETE("metrics/{id}/goals/{goalId}")
    suspend fun deleteGoal(@Path("id") userId: String, @Path("goalId") goalId: Long)

    /** GET /metrics/{id}/strength-history — entrenamientos + HITs oficiales de un ejercicio. */
    @GET("metrics/{id}/strength-history")
    suspend fun getStrengthHistory(
        @Path("id") userId: String,
        @Query("exercise") exercise: String,
        @Query("from") from: Long? = null,
        @Query("to") to: Long? = null,
    ): StrengthHistoryDto

    @POST("metrics/{id}/strength-logs")
    suspend fun createStrengthLog(@Path("id") userId: String, @Body body: CreateStrengthLogRequestDto): StrengthEntryDto

    @DELETE("metrics/{id}/strength-logs/{logId}")
    suspend fun deleteStrengthLog(@Path("id") userId: String, @Path("logId") logId: Long)

    /** GET /metrics/{id}/photos — fotos de progreso. */
    @GET("metrics/{id}/photos")
    suspend fun getProgressPhotos(@Path("id") userId: String): List<ProgressPhotoDto>

    /** POST /metrics/{id}/photos (multipart) — foto + medidas opcionales. */
    @Multipart
    @POST("metrics/{id}/photos")
    suspend fun uploadProgressPhoto(
        @Path("id") userId: String,
        @Part photo: MultipartBody.Part,
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
    ): ProgressPhotoDto

    @DELETE("metrics/{id}/photos/{photoId}")
    suspend fun deleteProgressPhoto(@Path("id") userId: String, @Path("photoId") photoId: Long)
}
