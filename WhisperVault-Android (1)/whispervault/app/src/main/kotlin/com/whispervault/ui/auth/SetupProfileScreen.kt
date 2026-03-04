package com.whispervault.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.whispervault.data.AuthRepository
import com.whispervault.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// 50 predefined animated avatar IDs
val AVATAR_LIST = (1..50).toList()

// Map avatar ID to emoji or URL (in real app, these are GIF assets bundled in res/raw)
fun avatarEmoji(id: Int): String {
    val emojis = listOf("👻","🐱","🦊","🐺","🦁","🐯","🐼","🐨","🐸","🦋",
        "🐙","🦑","🦈","🐉","🦄","🤖","👾","🎭","🃏","🎪",
        "🌙","⭐","🔮","🎯","💎","🗝️","🧩","🎲","🌀","⚡",
        "🌊","🔥","❄️","🌪️","🌈","🦅","🦉","🦝","🦨","🐧",
        "🧠","👁️","🦾","🎭","🌺","🍄","🦠","🎸","🎺","🥷")
    return emojis.getOrElse(id - 1) { "👻" }
}

@HiltViewModel
class SetupProfileViewModel @Inject constructor(private val repo: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow<ProfileSetupState>(ProfileSetupState.Idle)
    val state: StateFlow<ProfileSetupState> = _state

    fun setup(email: String, displayName: String, avatarId: Int) {
        if (displayName.isBlank()) { _state.value = ProfileSetupState.Error("Enter a display name"); return }
        viewModelScope.launch {
            _state.value = ProfileSetupState.Loading
            try {
                val res = repo.setupProfile(email, displayName.trim(), avatarId)
                if (res.isSuccessful) _state.value = ProfileSetupState.Success
                else _state.value = ProfileSetupState.Error("Setup failed")
            } catch (e: Exception) {
                _state.value = ProfileSetupState.Error("Network error")
            }
        }
    }
}

sealed class ProfileSetupState {
    object Idle : ProfileSetupState(); object Loading : ProfileSetupState(); object Success : ProfileSetupState()
    data class Error(val msg: String) : ProfileSetupState()
}

@Composable
fun SetupProfileScreen(
    email: String,
    onComplete: () -> Unit,
    viewModel: SetupProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var displayName by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf(1) }

    LaunchedEffect(state) { if (state is ProfileSetupState.Success) onComplete() }

    Column(
        modifier = Modifier.fillMaxSize().background(GhostBackground).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Set Up Profile", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = GhostText)
        Text("Choose a name and avatar", fontSize = 14.sp, color = GhostTextSecondary)
        Spacer(Modifier.height(24.dp))

        // Preview of selected avatar
        Box(
            modifier = Modifier.size(80.dp).clip(CircleShape)
                .background(GhostSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(avatarEmoji(selectedAvatar), fontSize = 40.sp)
        }

        Spacer(Modifier.height(16.dp))

        GhostTextField(value = displayName, onValueChange = { displayName = it }, label = "Display Name / Nickname")

        Spacer(Modifier.height(16.dp))
        Text("Choose Avatar", fontSize = 14.sp, color = GhostTextSecondary, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))

        // Avatar grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AVATAR_LIST) { avatarId ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selectedAvatar == avatarId) GhostGreen.copy(alpha = 0.2f) else GhostSurfaceVariant)
                        .border(
                            width = if (selectedAvatar == avatarId) 2.dp else 0.dp,
                            color = if (selectedAvatar == avatarId) GhostGreen else androidx.compose.ui.graphics.Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedAvatar = avatarId },
                    contentAlignment = Alignment.Center
                ) {
                    Text(avatarEmoji(avatarId), fontSize = 28.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (state is ProfileSetupState.Error)
            Text((state as ProfileSetupState.Error).msg, color = GhostRed, fontSize = 13.sp)

        Button(
            onClick = { viewModel.setup(email, displayName, selectedAvatar) },
            enabled = state !is ProfileSetupState.Loading,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GhostGreen),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (state is ProfileSetupState.Loading)
                CircularProgressIndicator(color = GhostBackground, modifier = Modifier.size(20.dp))
            else
                Text("Complete Setup", color = GhostBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(16.dp))
    }
}
