package com.whispervault.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.whispervault.data.AuthRepository
import com.whispervault.data.local.ConnectionDao
import com.whispervault.data.local.ConnectionEntity
import com.whispervault.data.remote.WhisperVaultApi
import com.whispervault.security.SessionManager
import com.whispervault.security.TorManager
import com.whispervault.ui.auth.avatarEmoji
import com.whispervault.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    val authRepository: AuthRepository,
    val sessionManager: SessionManager,
    val torManager: TorManager,
    val connectionDao: ConnectionDao,
    val api: WhisperVaultApi
) : ViewModel() {

    val connections: StateFlow<List<ConnectionEntity>> = connectionDao.getAllConnections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val sessions = sessionManager.sessions
    val isTorActive = torManager.isTorActive
    val isGuest = sessionManager.isGuest

    fun syncConnections() {
        viewModelScope.launch {
            if (!authRepository.isLoggedIn()) return@launch
            try {
                val res = authRepository.getMe()
                if (res.isSuccessful) {
                    val user = res.body() ?: return@launch
                    user.connections?.forEach { userId ->
                        val profile = api.getUser(authRepository.bearerToken(), userId)
                        if (profile.isSuccessful) {
                            val p = profile.body()!!
                            connectionDao.insertOrUpdate(ConnectionEntity(
                                userId = p.uniqueId,
                                displayName = p.displayName,
                                avatarId = p.avatarId
                            ))
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun checkTor() = viewModelScope.launch { torManager.checkTorStatus() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onChatClick: (String) -> Unit,
    onContactsClick: () -> Unit,
    onProfileClick: () -> Unit,
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val connections by viewModel.connections.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val isTorActive by viewModel.isTorActive.collectAsState()
    val isGuest by viewModel.isGuest.collectAsState()
    var showTorDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.syncConnections()
        viewModel.checkTor()
    }

    Scaffold(
        containerColor = GhostBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("WhisperVault", fontWeight = FontWeight.Bold, color = GhostText, fontSize = 18.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(8.dp).clip(CircleShape)
                                    .background(if (isTorActive) GhostGreen else GhostRed)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (isTorActive) "Tor Active" else "Tor Inactive",
                                fontSize = 11.sp,
                                color = if (isTorActive) GhostGreen else GhostTextSecondary
                            )
                        }
                    }
                },
                actions = {
                    if (!isTorActive) {
                        IconButton(onClick = { showTorDialog = true }) {
                            Icon(Icons.Default.Security, null, tint = GhostAmber)
                        }
                    }
                    if (!isGuest) {
                        IconButton(onClick = onContactsClick) {
                            Icon(Icons.Default.PersonAdd, null, tint = GhostText)
                        }
                    }
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.AccountCircle, null, tint = GhostText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GhostSurface)
            )
        },
        floatingActionButton = {
            if (!isGuest) {
                FloatingActionButton(
                    onClick = onContactsClick,
                    containerColor = GhostGreen,
                    contentColor = GhostBackground
                ) {
                    Icon(Icons.Default.Chat, null)
                }
            }
        }
    ) { padding ->
        if (connections.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("👻", fontSize = 64.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("No chats yet", color = GhostTextSecondary, fontSize = 16.sp)
                    if (isGuest) {
                        Text("Login to find contacts", color = GhostTextSecondary.copy(alpha = 0.6f), fontSize = 13.sp)
                    } else {
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = onContactsClick) {
                            Text("Find people from your contacts", color = GhostGreen)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(connections) { conn ->
                    val session = sessions[conn.userId]
                    val unread = session?.unreadCount ?: 0
                    val isLocked = session?.isLocked ?: false

                    ChatListItem(
                        displayName = conn.localContactName ?: conn.displayName,
                        avatarId = conn.avatarId,
                        isLocked = isLocked,
                        unreadCount = unread,
                        onClick = { onChatClick(conn.userId) }
                    )
                }
            }
        }

        if (showTorDialog) {
            TorWarningDialog(
                onDismiss = { showTorDialog = false },
                onOpenOrbot = {
                    showTorDialog = false
                    viewModel.torManager.openOrbot()
                }
            )
        }
    }
}

@Composable
fun ChatListItem(
    displayName: String,
    avatarId: Int,
    isLocked: Boolean,
    unreadCount: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier.size(50.dp).clip(CircleShape).background(GhostSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(avatarEmoji(avatarId), fontSize = 26.sp)
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(displayName, fontWeight = FontWeight.SemiBold, color = GhostText, fontSize = 15.sp)
                if (isLocked) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.Lock, null, tint = GhostLocked, modifier = Modifier.size(14.dp))
                }
            }
            // No message preview — by design
            Text("Tap to open chat", fontSize = 12.sp, color = GhostTextSecondary)
        }

        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .size(22.dp).clip(CircleShape).background(GhostGreen),
                contentAlignment = Alignment.Center
            ) {
                Text("$unreadCount", fontSize = 11.sp, color = GhostBackground, fontWeight = FontWeight.Bold)
            }
        }
    }

    Divider(color = GhostDivider, thickness = 0.5.dp, modifier = Modifier.padding(start = 78.dp))
}

@Composable
fun TorWarningDialog(onDismiss: () -> Unit, onOpenOrbot: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GhostSurface,
        title = { Text("⚠️ Tor Not Active", color = GhostText, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Your IP address is exposed. Enable Orbot (Tor) for maximum privacy before opening chats.",
                color = GhostTextSecondary
            )
        },
        confirmButton = {
            Button(onClick = onOpenOrbot, colors = ButtonDefaults.buttonColors(containerColor = GhostGreen)) {
                Text("Open Orbot", color = GhostBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Continue anyway", color = GhostTextSecondary) }
        }
    )
}
