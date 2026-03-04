package com.whispervault.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.*
import androidx.lifecycle.viewModelScope
import com.whispervault.data.AuthRepository
import com.whispervault.data.remote.SocketManager
import com.whispervault.security.*
import com.whispervault.ui.auth.avatarEmoji
import com.whispervault.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    val authRepository: AuthRepository,
    val sessionManager: SessionManager,
    val socketManager: SocketManager
) : ViewModel() {

    lateinit var withUserId: String
    var typingJob: Job? = null

    val sessions = sessionManager.sessions

    fun initChat(userId: String) {
        withUserId = userId
        socketManager.joinRoom(userId)
    }

    fun sendText(text: String) {
        if (text.isBlank()) return
        val myId = sessionManager.currentUserId.value ?: return
        val msgId = socketManager.sendMessage(withUserId, text, "text")
        sessionManager.addMessage(withUserId, ChatMessage(
            id = msgId, fromUserId = myId, content = text, type = MessageType.TEXT
        ))
    }

    fun deleteForEveryone(messageId: String) {
        socketManager.deleteMessageForAll(withUserId, messageId)
        sessionManager.deleteMessage(withUserId, messageId)
    }

    fun editMessage(messageId: String, newText: String) {
        socketManager.editMessage(withUserId, messageId, newText)
        sessionManager.editMessage(withUserId, messageId, newText)
    }

    fun toggleLock() {
        val session = sessionManager.getSession(withUserId) ?: return
        val newLocked = !session.isLocked
        sessionManager.setChatLock(withUserId, newLocked)
        socketManager.setChatLock(withUserId, newLocked)
    }

    fun onTyping() {
        typingJob?.cancel()
        socketManager.sendTyping(withUserId, true)
        typingJob = viewModelScope.launch {
            delay(2000)
            socketManager.sendTyping(withUserId, false)
        }
    }

    fun myId() = sessionManager.currentUserId.value ?: ""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    withUserId: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    val session = sessions[withUserId]
    val messages = session?.messages ?: emptyList()
    val isLocked = session?.isLocked ?: false

    var inputText by remember { mutableStateOf("") }
    var editingMessageId by remember { mutableStateOf<String?>(null) }
    var editText by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(withUserId) { viewModel.initChat(withUserId) }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        containerColor = GhostBackground,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = GhostText)
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(GhostSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(avatarEmoji(session?.withAvatarId ?: 1), fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(session?.withDisplayName ?: withUserId, color = GhostText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text(withUserId, color = GhostTextSecondary, fontSize = 11.sp)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleLock() }) {
                        Icon(
                            if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            null,
                            tint = if (isLocked) GhostLocked else GhostTextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GhostSurface)
            )
        },
        bottomBar = {
            Column {
                if (isLocked) {
                    Box(
                        modifier = Modifier.fillMaxWidth().background(GhostLocked.copy(alpha = 0.1f))
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🔒 Locked Chat — clears on background", fontSize = 11.sp, color = GhostLocked)
                    }
                }
                ChatInputBar(
                    text = inputText,
                    onTextChange = {
                        inputText = it
                        viewModel.onTyping()
                    },
                    onSend = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendText(inputText)
                            inputText = ""
                        }
                    }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 8.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isMine = msg.fromUserId == viewModel.myId()
                MessageBubble(
                    message = msg,
                    isMine = isMine,
                    onDeleteForAll = if (isMine) {{ showDeleteDialog = msg.id }} else null,
                    onEdit = if (isMine && msg.type != MessageType.DELETED) {{
                        editingMessageId = msg.id
                        editText = msg.content
                    }} else null
                )
            }
        }

        // Edit dialog
        if (editingMessageId != null) {
            AlertDialog(
                onDismissRequest = { editingMessageId = null },
                containerColor = GhostSurface,
                title = { Text("Edit Message", color = GhostText) },
                text = {
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GhostGreen, unfocusedBorderColor = GhostSurfaceVariant,
                            focusedTextColor = GhostText, unfocusedTextColor = GhostText, cursorColor = GhostGreen,
                            focusedContainerColor = GhostSurface, unfocusedContainerColor = GhostSurface
                        )
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.editMessage(editingMessageId!!, editText)
                        editingMessageId = null
                    }, colors = ButtonDefaults.buttonColors(containerColor = GhostGreen)) {
                        Text("Save", color = GhostBackground)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingMessageId = null }) { Text("Cancel", color = GhostTextSecondary) }
                }
            )
        }

        // Delete confirm dialog
        if (showDeleteDialog != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                containerColor = GhostSurface,
                title = { Text("Delete Message?", color = GhostText) },
                text = { Text("This will be deleted for everyone.", color = GhostTextSecondary) },
                confirmButton = {
                    Button(onClick = {
                        viewModel.deleteForEveryone(showDeleteDialog!!)
                        showDeleteDialog = null
                    }, colors = ButtonDefaults.buttonColors(containerColor = GhostRed)) {
                        Text("Delete for Everyone", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel", color = GhostTextSecondary) }
                }
            )
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isMine: Boolean,
    onDeleteForAll: (() -> Unit)?,
    onEdit: (() -> Unit)?
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Box {
            Surface(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { if (isMine) showMenu = true }
                    ),
                shape = RoundedCornerShape(
                    topStart = 18.dp, topEnd = 18.dp,
                    bottomStart = if (isMine) 18.dp else 4.dp,
                    bottomEnd = if (isMine) 4.dp else 18.dp
                ),
                color = if (isMine) GhostBubbleSent else GhostBubbleReceived
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (message.type == MessageType.DELETED) {
                        Text("🚫 Message deleted", color = GhostTextSecondary, fontSize = 13.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    } else {
                        Text(message.content, color = GhostText, fontSize = 15.sp)
                    }
                    Row(
                        modifier = Modifier.align(Alignment.End).padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (message.isEdited) {
                            Text("edited", color = GhostTextSecondary, fontSize = 10.sp)
                        }
                        Text(
                            formatTime(message.timestamp),
                            color = GhostTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Context menu
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(GhostSurface)
            ) {
                onEdit?.let {
                    DropdownMenuItem(
                        text = { Text("✏️ Edit", color = GhostText) },
                        onClick = { showMenu = false; it() }
                    )
                }
                onDeleteForAll?.let {
                    DropdownMenuItem(
                        text = { Text("🗑️ Delete for Everyone", color = GhostRed) },
                        onClick = { showMenu = false; it() }
                    )
                }
            }
        }
    }
}

@Composable
fun ChatInputBar(text: String, onTextChange: (String) -> Unit, onSend: () -> Unit) {
    Surface(color = GhostSurface) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Message...", color = GhostTextSecondary) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GhostSurfaceVariant, unfocusedBorderColor = GhostSurfaceVariant,
                    focusedTextColor = GhostText, unfocusedTextColor = GhostText, cursorColor = GhostGreen,
                    focusedContainerColor = GhostSurfaceVariant, unfocusedContainerColor = GhostSurfaceVariant
                ),
                shape = RoundedCornerShape(24.dp),
                singleLine = false,
                maxLines = 4
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                modifier = Modifier.size(46.dp).clip(CircleShape).background(
                    if (text.isNotBlank()) GhostGreen else GhostSurfaceVariant
                )
            ) {
                Icon(Icons.Default.Send, null, tint = if (text.isNotBlank()) GhostBackground else GhostTextSecondary)
            }
        }
    }
}

fun formatTime(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    return java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(date)
}
