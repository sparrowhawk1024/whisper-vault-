package com.whispervault.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.whispervault.ui.theme.*
import com.whispervault.viewmodel.AppViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onReady: (Boolean) -> Unit,
    viewModel: AppViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        delay(800) // Show splash briefly
        val loggedIn = viewModel.checkLoginState()
        onReady(loggedIn)
    }

    Box(
        modifier = Modifier.fillMaxSize().background(GhostBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("👻", fontSize = 72.sp)
            Spacer(Modifier.height(16.dp))
            Text("WhisperVault", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = GhostGreen)
            Spacer(Modifier.height(8.dp))
            Text("Encrypted. Anonymous. Gone.", fontSize = 13.sp, color = GhostTextSecondary)
            Spacer(Modifier.height(40.dp))
            CircularProgressIndicator(color = GhostGreen, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
        }
    }
}
