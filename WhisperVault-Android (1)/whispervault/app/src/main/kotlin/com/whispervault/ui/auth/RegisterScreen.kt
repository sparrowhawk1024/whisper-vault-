package com.whispervault.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.whispervault.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val state: StateFlow<RegisterState> = _state

    fun register(email: String, password: String, confirmPw: String, mobile: String) {
        if (email.isBlank() || password.isBlank() || mobile.isBlank()) {
            _state.value = RegisterState.Error("All fields are required")
            return
        }
        if (password != confirmPw) {
            _state.value = RegisterState.Error("Passwords do not match")
            return
        }
        if (password.length < 8) {
            _state.value = RegisterState.Error("Password must be at least 8 characters")
            return
        }
        viewModelScope.launch {
            _state.value = RegisterState.Loading
            try {
                val res = authRepository.register(email.trim(), password, mobile.trim())
                if (res.isSuccessful) _state.value = RegisterState.Success(email.trim())
                else _state.value = RegisterState.Error(res.errorBody()?.string() ?: "Registration failed")
            } catch (e: Exception) {
                _state.value = RegisterState.Error("Network error. Check connection.")
            }
        }
    }
}

sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    data class Success(val email: String) : RegisterState()
    data class Error(val message: String) : RegisterState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onSuccess: (String) -> Unit,
    onLoginClick: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPw by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var showPw by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (state is RegisterState.Success) onSuccess((state as RegisterState.Success).email)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GhostBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        Text("Create Account", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = GhostText)
        Text("Your identity, your control.", fontSize = 14.sp, color = GhostTextSecondary)

        Spacer(Modifier.height(32.dp))

        GhostTextField(value = email, onValueChange = { email = it }, label = "Email Address",
            keyboardType = KeyboardType.Email)
        Spacer(Modifier.height(12.dp))

        GhostTextField(
            value = password, onValueChange = { password = it }, label = "Password (min 8 chars)",
            keyboardType = KeyboardType.Password,
            visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPw = !showPw }) {
                    Icon(if (showPw) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        null, tint = GhostTextSecondary)
                }
            }
        )
        Spacer(Modifier.height(12.dp))

        GhostTextField(
            value = confirmPw, onValueChange = { confirmPw = it }, label = "Confirm Password",
            keyboardType = KeyboardType.Password,
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(Modifier.height(12.dp))

        GhostTextField(
            value = mobile, onValueChange = { mobile = it }, label = "Mobile Number (+countrycode)",
            keyboardType = KeyboardType.Phone
        )

        Spacer(Modifier.height(8.dp))
        Text(
            "Mobile number is encrypted and only used for contact discovery.",
            fontSize = 11.sp,
            color = GhostTextSecondary.copy(alpha = 0.7f)
        )

        if (state is RegisterState.Error) {
            Spacer(Modifier.height(12.dp))
            Text((state as RegisterState.Error).message, color = GhostRed, fontSize = 13.sp)
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { viewModel.register(email, password, confirmPw, mobile) },
            enabled = state !is RegisterState.Loading,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GhostGreen),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (state is RegisterState.Loading)
                CircularProgressIndicator(color = GhostBackground, modifier = Modifier.size(20.dp))
            else
                Text("Continue", color = GhostBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(Modifier.height(16.dp))

        TextButton(onClick = onLoginClick, modifier = Modifier.fillMaxWidth()) {
            Text("Already have an account? Sign In", color = GhostGreen, fontSize = 14.sp)
        }
    }
}
