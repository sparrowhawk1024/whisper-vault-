package com.whispervault.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.whispervault.security.SessionManager
import com.whispervault.data.AuthRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ChatClearService : Service() {

    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var authRepository: AuthRepository

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Stays running while app is alive
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // App was swiped from recents — clear all in-memory chats
        sessionManager.clearAllSessions()
        sessionManager.clearSessionKeys() // needs to be added to SecurityManager

        // If guest, destroy identity too
        if (sessionManager.isGuest.value) {
            sessionManager.clearGuestIdentity()
        }
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }
}
