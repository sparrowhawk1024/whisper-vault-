package com.whispervault.data

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.content.Context
import com.whispervault.data.local.AppPrefDao
import com.whispervault.data.local.AppPrefEntity
import com.whispervault.data.remote.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: WhisperVaultApi,
    private val prefDao: AppPrefDao
) {
    companion object {
        const val KEY_TOKEN = "auth_token"
        const val KEY_UNIQUE_ID = "unique_id"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_AVATAR_ID = "avatar_id"
        const val KEY_EMAIL = "email"
        const val KEY_SAVE_PIN_HASH = "save_pin_hash"
        const val KEY_SAVE_PIN_TYPE = "save_pin_type" // "pin" or "password"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "whispervault_secure",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // ── Token ──
    fun saveToken(token: String) = securePrefs.edit().putString(KEY_TOKEN, token).apply()
    fun getToken(): String? = securePrefs.getString(KEY_TOKEN, null)
    fun clearToken() = securePrefs.edit().remove(KEY_TOKEN).apply()

    fun bearerToken() = "Bearer ${getToken()}"

    // ── Profile ──
    fun saveProfile(uniqueId: String, displayName: String, avatarId: Int, email: String) {
        securePrefs.edit()
            .putString(KEY_UNIQUE_ID, uniqueId)
            .putString(KEY_DISPLAY_NAME, displayName)
            .putInt(KEY_AVATAR_ID, avatarId)
            .putString(KEY_EMAIL, email)
            .apply()
    }

    fun getUniqueId() = securePrefs.getString(KEY_UNIQUE_ID, null)
    fun getDisplayName() = securePrefs.getString(KEY_DISPLAY_NAME, null)
    fun getAvatarId() = securePrefs.getInt(KEY_AVATAR_ID, 1)
    fun getEmail() = securePrefs.getString(KEY_EMAIL, null)

    fun isLoggedIn() = getToken() != null && getUniqueId() != null

    fun logout() {
        securePrefs.edit().clear().apply()
    }

    // ── Saved content PIN ──
    fun savePinHash(hash: String, type: String) {
        securePrefs.edit()
            .putString(KEY_SAVE_PIN_HASH, hash)
            .putString(KEY_SAVE_PIN_TYPE, type)
            .apply()
    }

    fun getPinHash() = securePrefs.getString(KEY_SAVE_PIN_HASH, null)
    fun getPinType() = securePrefs.getString(KEY_SAVE_PIN_TYPE, null)
    fun hasSavePin() = getPinHash() != null

    // ── API calls ──
    suspend fun register(email: String, password: String, mobile: String) =
        api.register(RegisterRequest(email, password, mobile))

    suspend fun verifyOtp(email: String, otp: String) =
        api.verifyOtp(VerifyOtpRequest(email, otp))

    suspend fun setupProfile(email: String, displayName: String, avatarId: Int) =
        api.setupProfile(SetupProfileRequest(email, displayName, avatarId))

    suspend fun login(email: String, password: String) =
        api.login(LoginRequest(email, password))

    suspend fun getMe() = api.getMe(bearerToken())

    suspend fun updateDisplayName(name: String) = api.updateDisplayName(bearerToken(), UpdateNameRequest(name))
    suspend fun updateAvatar(avatarId: Int) = api.updateAvatar(bearerToken(), UpdateAvatarRequest(avatarId))

    suspend fun discoverContacts(mobiles: List<String>) =
        api.discoverContacts(bearerToken(), DiscoverContactsRequest(mobiles))

    suspend fun sendChatRequest(toUserId: String) =
        api.sendChatRequest(bearerToken(), ChatRequestBody(toUserId))

    suspend fun respondRequest(requestId: String, accept: Boolean) =
        api.respondChatRequest(bearerToken(), RespondRequestBody(requestId, accept))

    suspend fun confirmRequest(requestId: String) =
        api.confirmChatRequest(bearerToken(), ConfirmRequestBody(requestId))

    suspend fun getPendingRequests() = api.getPendingRequests(bearerToken())
}
