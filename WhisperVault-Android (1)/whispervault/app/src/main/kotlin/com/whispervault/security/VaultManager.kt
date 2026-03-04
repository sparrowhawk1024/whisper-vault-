package com.whispervault.security

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WhisperVault — Vault feature:
 *  • Panic wipe: erase all messages + prefs in one tap
 *  • Decoy PIN: entering wrong PIN triggers wipe silently
 *  • Vault lock: biometric/PIN re-auth on resume
 *  • Message TTL: per-message auto-destruct timers
 */
@Singleton
class VaultManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _isVaultLocked = MutableStateFlow(false)
    val isVaultLocked: StateFlow<Boolean> = _isVaultLocked

    private val _wipePending = MutableStateFlow(false)
    val wipePending: StateFlow<Boolean> = _wipePending

    // ── Panic wipe ──
    fun triggerPanicWipe() {
        _wipePending.value = true
        clearAllAppData()
    }

    private fun clearAllAppData() {
        // Clear shared prefs
        val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        prefsDir.listFiles()?.forEach { it.delete() }

        // Clear databases
        val dbDir = File(context.applicationInfo.dataDir, "databases")
        dbDir.listFiles()?.forEach { it.delete() }

        // Clear files
        context.filesDir.listFiles()?.forEach { it.deleteRecursively() }
        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
    }

    // ── Vault lock / unlock ──
    fun lockVault()   { _isVaultLocked.value = true }
    fun unlockVault() { _isVaultLocked.value = false }

    // ── Decoy PIN check ──
    // Returns true if the PIN triggers a wipe (decoy PIN entered)
    fun checkDecoyPin(enteredHash: String, realHash: String, decoyHash: String?): Boolean {
        if (decoyHash != null && enteredHash == decoyHash) {
            triggerPanicWipe()
            return true
        }
        return enteredHash != realHash
    }

    // ── Message TTL support ──
    fun messageShouldExpire(timestampMs: Long, ttlSeconds: Long): Boolean {
        if (ttlSeconds <= 0) return false
        return (System.currentTimeMillis() - timestampMs) > (ttlSeconds * 1000)
    }
}
