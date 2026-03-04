package com.whispervault.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whispervault.data.AuthRepository
import com.whispervault.data.remote.SocketManager
import com.whispervault.security.SessionManager
import com.whispervault.security.TorManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val socketManager: SocketManager,
    private val torManager: TorManager
) : ViewModel() {

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    val isTorActive = torManager.isTorActive

    fun checkLoginState(): Boolean {
        val loggedIn = authRepository.isLoggedIn()
        _isLoggedIn.value = loggedIn
        if (loggedIn) {
            val userId = authRepository.getUniqueId()!!
            sessionManager.setCurrentUser(userId, false)
            socketManager.connect(authRepository.getToken())
            checkTorStatus()
        }
        return loggedIn
    }

    fun continueAsGuest() {
        val guestId = "GUEST_" + (1000..9999).random().toString()
        sessionManager.setCurrentUser(guestId, true)
        socketManager.connect(null)
        _isLoggedIn.value = false
        checkTorStatus()
    }

    fun logout() {
        socketManager.disconnect()
        sessionManager.clearAllSessions()
        authRepository.logout()
        _isLoggedIn.value = false
    }

    private fun checkTorStatus() {
        viewModelScope.launch {
            torManager.checkTorStatus()
        }
    }
}
