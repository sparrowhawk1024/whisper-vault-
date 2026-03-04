package com.whispervault.data.remote

import retrofit2.Response
import retrofit2.http.*

// ── Request Models ──
data class RegisterRequest(val email: String, val password: String, val mobile: String)
data class VerifyOtpRequest(val email: String, val otp: String)
data class SetupProfileRequest(val email: String, val displayName: String, val avatarId: Int)
data class LoginRequest(val email: String, val password: String)
data class UpdateNameRequest(val displayName: String)
data class UpdateAvatarRequest(val avatarId: Int)
data class UpdateStatusRequest(val statusEmoji: String, val statusText: String)
data class DiscoverContactsRequest(val mobiles: List<String>)
data class ChatRequestBody(val toUserId: String)
data class RespondRequestBody(val requestId: String, val accept: Boolean)
data class ConfirmRequestBody(val requestId: String)
data class ReportUserRequest(val targetUserId: String, val reason: String)

// ── Response Models ──
data class RegisterResponse(val message: String, val userId: String)
data class VerifyOtpResponse(val message: String, val needsProfile: Boolean, val uniqueId: String)
data class LoginResponse(val token: String, val user: UserDto)
data class UserDto(
    val uniqueId: String,
    val displayName: String,
    val avatarId: Int,
    val connections: List<String>? = null,
    val statusEmoji: String?       = null,
    val statusText: String?        = null,
    val isOnline: Boolean          = false,
    val lastSeen: Long?            = null
)
data class DiscoveredUser(
    val uniqueId: String,
    val displayName: String,
    val avatarId: Int,
    val mobileHash: String
)
data class DiscoverResponse(val matched: List<DiscoveredUser>)
data class GenericResponse(val message: String)
data class ChatRequestResponse(val message: String, val requestId: String?)
data class PendingRequestsResponse(
    val incoming: List<ChatRequestDto>,
    val awaitingConfirm: List<ChatRequestDto>
)
data class ChatRequestDto(val _id: String, val fromUserId: String, val toUserId: String, val status: String)

interface WhisperVaultApi {

    // ── Auth ──
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<RegisterResponse>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body body: VerifyOtpRequest): Response<VerifyOtpResponse>

    @POST("auth/setup-profile")
    suspend fun setupProfile(@Body body: SetupProfileRequest): Response<GenericResponse>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("auth/resend-otp")
    suspend fun resendOtp(@Body body: Map<String, String>): Response<GenericResponse>

    @POST("auth/logout")
    suspend fun logout(@Header("Authorization") token: String): Response<GenericResponse>

    // ── User ──
    @GET("user/me")
    suspend fun getMe(@Header("Authorization") token: String): Response<UserDto>

    @PATCH("user/display-name")
    suspend fun updateDisplayName(
        @Header("Authorization") token: String,
        @Body body: UpdateNameRequest
    ): Response<GenericResponse>

    @PATCH("user/avatar")
    suspend fun updateAvatar(
        @Header("Authorization") token: String,
        @Body body: UpdateAvatarRequest
    ): Response<GenericResponse>

    @PATCH("user/status")
    suspend fun updateStatus(
        @Header("Authorization") token: String,
        @Body body: UpdateStatusRequest
    ): Response<GenericResponse>

    @GET("user/{uniqueId}")
    suspend fun getUser(
        @Header("Authorization") token: String,
        @Path("uniqueId") uniqueId: String
    ): Response<UserDto>

    // ── Contacts ──
    @POST("contacts/discover")
    suspend fun discoverContacts(
        @Header("Authorization") token: String,
        @Body body: DiscoverContactsRequest
    ): Response<DiscoverResponse>

    // ── Chat requests ──
    @POST("chat/request")
    suspend fun sendChatRequest(
        @Header("Authorization") token: String,
        @Body body: ChatRequestBody
    ): Response<ChatRequestResponse>

    @POST("chat/respond")
    suspend fun respondChatRequest(
        @Header("Authorization") token: String,
        @Body body: RespondRequestBody
    ): Response<GenericResponse>

    @POST("chat/confirm")
    suspend fun confirmChatRequest(
        @Header("Authorization") token: String,
        @Body body: ConfirmRequestBody
    ): Response<GenericResponse>

    @GET("chat/requests/pending")
    suspend fun getPendingRequests(
        @Header("Authorization") token: String
    ): Response<PendingRequestsResponse>

    // ── Safety ──
    @POST("safety/report")
    suspend fun reportUser(
        @Header("Authorization") token: String,
        @Body body: ReportUserRequest
    ): Response<GenericResponse>

    @DELETE("safety/block/{userId}")
    suspend fun blockUser(
        @Header("Authorization") token: String,
        @Path("userId") userId: String
    ): Response<GenericResponse>
}
