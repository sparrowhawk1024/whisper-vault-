package com.whispervault.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whispervault.data.AuthRepository
import com.whispervault.data.remote.SocketManager
import com.whispervault.security.SessionManager
import com.whispervault.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repo: AuthRepository,
    private val sessionManager: SessionManager,
    private val socketManager: SocketManager
) : ViewModel() {
    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) { _state.value = LoginState.Error("Enter email and password"); return }
        viewModelScope.launch {
            _state.value = LoginState.Loading
            try {
                val res = repo.login(email.trim(), password)
                if (res.isSuccessful) {
                    val body = res.body()!!
                    repo.saveToken(body.token)
                    repo.saveProfile(body.user.uniqueId, body.user.displayName, body.user.avatarId, email.trim())
                    sessionManager.setCurrentUser(body.user.uniqueId, false)
                    socketManager.connect(body.token)
                    _state.value = LoginState.Success
                } else {
                    _state.value = LoginState.Error("Invalid credentials")
                }
            } catch (e: Exception) {
                _state.value = LoginState.Error("Network error. Check connection.")
            }
        }
    }
}

sealed class LoginState {
    object Idle : LoginState(); object Loading : LoginState(); object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPw by remember { mutableStateOf(false) }

    LaunchedEffect(state) { if (state is LoginState.Success) onLoginSuccess() }

    Column(
        modifier = Modifier.fillMaxSize().background(GhostBackground).padding(24.dp)
    ) {
        Spacer(Modifier.height(60.dp))
        Text("Welcome back", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = GhostText)
        Text("Sign in to your Ghost identity", fontSize = 14.sp, color = GhostTextSecondary)
        Spacer(Modifier.height(40.dp))

        GhostTextField(value = email, onValueChange = { email = it }, label = "Email", keyboardType = KeyboardType.Email)
        Spacer(Modifier.height(12.dp))
        GhostTextField(
            value = password, onValueChange = { password = it }, label = "Password",
            keyboardType = KeyboardType.Password,
            visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPw = !showPw }) {
                    Icon(if (showPw) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = GhostTextSecondary)
                }
            }
        )

        if (state is LoginState.Error) {
            Spacer(Modifier.height(8.dp))
            Text((state as LoginState.Error).message, color = GhostRed, fontSize = 13.sp)
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { viewModel.login(email, password) },
            enabled = state !is LoginState.Loading,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GhostGreen),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (state is LoginState.Loading) CircularProgressIndicator(color = GhostBackground, modifier = Modifier.size(20.dp))
            else Text("Sign In", color = GhostBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onRegisterClick, modifier = Modifier.fillMaxWidth()) {
            Text("No account? Create one", color = GhostGreen, fontSize = 14.sp)
        }
    }
}
