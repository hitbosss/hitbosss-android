package com.hitbosss.data.remote

import com.hitbosss.data.remote.dto.AppConfigDto
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
}
