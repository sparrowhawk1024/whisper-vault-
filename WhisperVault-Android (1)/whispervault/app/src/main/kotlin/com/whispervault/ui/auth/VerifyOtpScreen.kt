package com.whispervault.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
class VerifyOtpViewModel @Inject constructor(private val repo: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow<OtpState>(OtpState.Idle)
    val state: StateFlow<OtpState> = _state

    fun verify(email: String, otp: String) {
        if (otp.length != 6) { _state.value = OtpState.Error("Enter 6-digit code"); return }
        viewModelScope.launch {
            _state.value = OtpState.Loading
            try {
                val res = repo.verifyOtp(email, otp)
                if (res.isSuccessful) _state.value = OtpState.Success
                else _state.value = OtpState.Error("Invalid or expired code")
            } catch (e: Exception) {
                _state.value = OtpState.Error("Network error")
            }
        }
    }

    fun resend(email: String) {
        viewModelScope.launch {
            try { repo.resendOtp(email) } catch (_: Exception) {}
        }
    }
}

sealed class OtpState {
    object Idle : OtpState(); object Loading : OtpState(); object Success : OtpState()
    data class Error(val message: String) : OtpState()
}

@Composable
fun VerifyOtpScreen(
    email: String,
    onVerified: () -> Unit,
    viewModel: VerifyOtpViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var otp by remember { mutableStateOf("") }

    LaunchedEffect(state) { if (state is OtpState.Success) onVerified() }

    Column(
        modifier = Modifier.fillMaxSize().background(GhostBackground).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(60.dp))
        Text("Check your email", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = GhostText)
        Spacer(Modifier.height(8.dp))
        Text("A 6-digit code was sent to\n$email", fontSize = 14.sp, color = GhostTextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(40.dp))

        OutlinedTextField(
            value = otp,
            onValueChange = { if (it.length <= 6) otp = it },
            label = { Text("6-Digit Code", color = GhostTextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GhostGreen, unfocusedBorderColor = GhostSurfaceVariant,
                focusedTextColor = GhostText, unfocusedTextColor = GhostText,
                cursorColor = GhostGreen, focusedContainerColor = GhostSurface, unfocusedContainerColor = GhostSurface
            ),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true
        )

        if (state is OtpState.Error) {
            Spacer(Modifier.height(8.dp))
            Text((state as OtpState.Error).message, color = GhostRed, fontSize = 13.sp)
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { viewModel.verify(email, otp) },
            enabled = state !is OtpState.Loading,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GhostGreen),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (state is OtpState.Loading) CircularProgressIndicator(color = GhostBackground, modifier = Modifier.size(20.dp))
            else Text("Verify", color = GhostBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = { viewModel.resend(email) }) {
            Text("Resend code", color = GhostTextSecondary, fontSize = 14.sp)
        }
    }
}
