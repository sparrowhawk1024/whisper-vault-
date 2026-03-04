package com.whispervault

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WhisperVaultApp : Application() {

    companion object {
        const val CHANNEL_ID_MESSAGES = "whispervault_messages"
        const val CHANNEL_ID_SECURITY = "whispervault_security"
        const val CHANNEL_ID_TOR      = "whispervault_tor"
        const val CHANNEL_ID_VAULT    = "whispervault_vault"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val msgChannel = NotificationChannel(
                CHANNEL_ID_MESSAGES, "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "New encrypted message alerts"
                setShowBadge(true)
            }

            val secChannel = NotificationChannel(
                CHANNEL_ID_SECURITY, "Security",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Security and Tor alerts" }

            val torChannel = NotificationChannel(
                CHANNEL_ID_TOR, "Tor Network",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Tor connection status updates" }

            val vaultChannel = NotificationChannel(
                CHANNEL_ID_VAULT, "Vault",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Vault lock and emergency wipe alerts" }

            manager.createNotificationChannels(listOf(msgChannel, secChannel, torChannel, vaultChannel))
        }
    }
}
