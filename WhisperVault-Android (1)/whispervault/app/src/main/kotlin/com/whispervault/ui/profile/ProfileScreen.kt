package com.whispervault.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whispervault.data.AuthRepository
import com.whispervault.data.remote.SocketManager
import com.whispervault.security.SecurityManager
import com.whispervault.security.SessionManager
import com.whispervault.ui.auth.AVATAR_LIST
import com.whispervault.ui.auth.avatarEmoji
import com.whispervault.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    val authRepository: AuthRepository,
    val sessionManager: SessionManager,
    val securityManager: SecurityManager,
    val socketManager: SocketManager
) : ViewModel() {
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving

    fun updateDisplayName(name: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _saving.value = true
            try {
                val res = authRepository.updateDisplayName(name)
                if (res.isSuccessful) {
                    authRepository.saveProfile(
                        authRepository.getUniqueId()!!, name,
                        authRepository.getAvatarId(), authRepository.getEmail()!!
                    )
                }
            } catch (_: Exception) {}
            _saving.value = false
            onDone()
        }
    }

    fun updateAvatar(avatarId: Int, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                authRepository.updateAvatar(avatarId)
                authRepository.saveProfile(
                    authRepository.getUniqueId()!!,
                    authRepository.getDisplayName()!!,
                    avatarId, authRepository.getEmail()!!
                )
            } catch (_: Exception) {}
            onDone()
        }
    }

    fun logout(onDone: () -> Unit) {
        socketManager.disconnect()
        securityManager.clearSessionKeys()
        sessionManager.clearAllSessions()
        authRepository.logout()
        onDone()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val repo = viewModel.authRepository
    var displayName by remember { mutableStateOf(repo.getDisplayName() ?: "") }
    var avatarId by remember { mutableStateOf(repo.getAvatarId()) }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    val saving by viewModel.saving.collectAsState()

    Scaffold(
        containerColor = GhostBackground,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = GhostText) }
                },
                title = { Text("Profile", color = GhostText, fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GhostSurface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // Avatar
            Box(
                modifier = Modifier.size(100.dp).clip(CircleShape).background(GhostSurfaceVariant)
                    .clickable { showAvatarPicker = true },
                contentAlignment = Alignment.Center
            ) {
                Text(avatarEmoji(avatarId), fontSize = 50.sp)
            }
            Text("Tap to change", fontSize = 11.sp, color = GhostTextSecondary)

            Spacer(Modifier.height(20.dp))

            // Display name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(displayName, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GhostText)
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { showNameDialog = true }) {
                    Icon(Icons.Default.Edit, null, tint = GhostGreen, modifier = Modifier.size(18.dp))
                }
            }

            // User ID (readonly)
            val uniqueId = repo.getUniqueId() ?: ""
            Surface(
                color = GhostSurfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("ID: ", color = GhostTextSecondary, fontSize = 13.sp)
                    Text(uniqueId, color = GhostGreen, fontSize = 13.sp, fontWeight = FontWeight.Mono)
                }
            }
            Text("Share your ID for others to find you", fontSize = 11.sp, color = GhostTextSecondary.copy(alpha = 0.6f))

            Spacer(Modifier.height(40.dp))

            // Security notice
            Surface(color = GhostSurfaceVariant, shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🔒 Privacy Settings", color = GhostText, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("• Messages auto-delete on app close", color = GhostTextSecondary, fontSize = 13.sp)
                    Text("• Screenshots are blocked", color = GhostTextSecondary, fontSize = 13.sp)
                    Text("• Phone number never visible to others", color = GhostTextSecondary, fontSize = 13.sp)
                    Text("• E2E encryption on all chats", color = GhostTextSecondary, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.weight(1f))

            OutlinedButton(
                onClick = { viewModel.logout(onLogout) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GhostRed),
                border = androidx.compose.foundation.BorderStroke(1.dp, GhostRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Sign Out", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // Name edit dialog
    if (showNameDialog) {
        var newName by remember { mutableStateOf(displayName) }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            containerColor = GhostSurface,
            title = { Text("Change Display Name", color = GhostText) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { if (it.length <= 30) newName = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GhostGreen, unfocusedBorderColor = GhostSurfaceVariant,
                        focusedTextColor = GhostText, unfocusedTextColor = GhostText,
                        cursorColor = GhostGreen, focusedContainerColor = GhostSurface, unfocusedContainerColor = GhostSurface
                    )
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateDisplayName(newName) { displayName = newName; showNameDialog = false }
                }, colors = ButtonDefaults.buttonColors(containerColor = GhostGreen)) {
                    Text("Save", color = GhostBackground)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Cancel", color = GhostTextSecondary) }
            }
        )
    }

    // Avatar picker dialog
    if (showAvatarPicker) {
        AlertDialog(
            onDismissRequest = { showAvatarPicker = false },
            containerColor = GhostSurface,
            title = { Text("Choose Avatar", color = GhostText) },
            text = {
                LazyVerticalGrid(columns = GridCells.Fixed(5), modifier = Modifier.height(300.dp)) {
                    items(AVATAR_LIST) { id ->
                        Box(
                            modifier = Modifier.padding(4.dp).aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (avatarId == id) GhostGreen.copy(alpha = 0.2f) else GhostSurfaceVariant)
                                .border(
                                    width = if (avatarId == id) 2.dp else 0.dp,
                                    color = if (avatarId == id) GhostGreen else androidx.compose.ui.graphics.Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { avatarId = id },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(avatarEmoji(id), fontSize = 26.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateAvatar(avatarId) { showAvatarPicker = false }
                }, colors = ButtonDefaults.buttonColors(containerColor = GhostGreen)) {
                    Text("Save", color = GhostBackground)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAvatarPicker = false }) { Text("Cancel", color = GhostTextSecondary) }
            }
        )
    }
}
