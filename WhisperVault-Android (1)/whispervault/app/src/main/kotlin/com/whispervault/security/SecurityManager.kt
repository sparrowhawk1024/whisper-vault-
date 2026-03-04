package com.whispervault.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityManager @Inject constructor() {

    // ── ECDH Key Exchange ──
    // Each session generates an ephemeral ECDH keypair
    // Public key shared with peer, shared secret derived, used for AES-GCM

    private var ephemeralPrivateKey: PrivateKey? = null
    private var sessionSecretKey: SecretKey? = null

    fun generateEphemeralECDHKeyPair(): String {
        val keyPairGen = KeyPairGenerator.getInstance("EC")
        keyPairGen.initialize(256)
        val keyPair = keyPairGen.generateKeyPair()
        ephemeralPrivateKey = keyPair.private
        return Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
    }

    fun deriveSessionKey(peerPublicKeyBase64: String) {
        val peerPublicKeyBytes = Base64.decode(peerPublicKeyBase64, Base64.NO_WRAP)
        val keyFactory = java.security.KeyFactory.getInstance("EC")
        val peerPublicKey = keyFactory.generatePublic(
            java.security.spec.X509EncodedKeySpec(peerPublicKeyBytes)
        )
        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(ephemeralPrivateKey)
        keyAgreement.doPhase(peerPublicKey, true)
        val sharedSecret = keyAgreement.generateSecret()

        // Derive AES-256 key from shared secret using SHA-256
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(sharedSecret)
        sessionSecretKey = SecretKeySpec(keyBytes, "AES")
    }

    // ── AES-GCM Encryption ──
    fun encryptMessage(plaintext: String): EncryptedPayload {
        val key = sessionSecretKey ?: generateFallbackKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return EncryptedPayload(
            data = Base64.encodeToString(encrypted, Base64.NO_WRAP),
            iv = Base64.encodeToString(iv, Base64.NO_WRAP)
        )
    }

    fun decryptMessage(payload: EncryptedPayload): String {
        val key = sessionSecretKey ?: generateFallbackKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = Base64.decode(payload.iv, Base64.NO_WRAP)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        val decrypted = cipher.doFinal(Base64.decode(payload.data, Base64.NO_WRAP))
        return String(decrypted, Charsets.UTF_8)
    }

    fun clearSessionKeys() {
        ephemeralPrivateKey = null
        sessionSecretKey = null
    }

    // ── Android Keystore — for local saved content ──
    private val KEYSTORE_ALIAS = "WhisperVaultSavedContent"

    fun getOrCreateLocalKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            keyGen.init(
                KeyGenParameterSpec.Builder(
                    KEYSTORE_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setUserAuthenticationRequired(false) // PIN/password checked in app layer
                    .build()
            )
            keyGen.generateKey()
        }
        return keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey
    }

    fun encryptLocalData(plaintext: ByteArray): EncryptedPayload {
        val key = getOrCreateLocalKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.doFinal(plaintext)
        return EncryptedPayload(
            data = Base64.encodeToString(encrypted, Base64.NO_WRAP),
            iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        )
    }

    fun decryptLocalData(payload: EncryptedPayload): ByteArray {
        val key = getOrCreateLocalKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, Base64.decode(payload.iv, Base64.NO_WRAP)))
        return cipher.doFinal(Base64.decode(payload.data, Base64.NO_WRAP))
    }

    // ── PIN/Password Hash ──
    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val salt = "WhisperVaultPinSalt_v1"
        return Base64.encodeToString(digest.digest((salt + pin).toByteArray()), Base64.NO_WRAP)
    }

    private fun generateFallbackKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        return keyGen.generateKey().also { sessionSecretKey = it }
    }
}

data class EncryptedPayload(val data: String, val iv: String)
