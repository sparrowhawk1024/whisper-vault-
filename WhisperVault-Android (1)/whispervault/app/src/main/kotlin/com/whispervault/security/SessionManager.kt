package com.whispervault.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class ChatMessage(
    val id: String               = UUID.randomUUID().toString(),
    val fromUserId: String,
    val content: String,
    val type: MessageType        = MessageType.TEXT,
    val timestamp: Long          = System.currentTimeMillis(),
    val isEdited: Boolean        = false,
    val isDeleted: Boolean       = false,
    val isSaved: Boolean         = false,
    val mediaPath: String?       = null,
    val ttlSeconds: Long         = 0,     // 0 = no expiry
    val reactions: Map<String, List<String>> = emptyMap(),  // emoji -> [userIds]
    val replyToId: String?       = null   // message ID being replied to
)

enum class MessageType { TEXT, IMAGE, VOICE, DELETED, SYSTEM, FILE }

data class ChatSession(
    val withUserId: String,
    val withDisplayName: String,
    val withAvatarId: Int,
    val isLocked: Boolean              = false,
    val messages: MutableList<ChatMessage> = mutableListOf(),
    val unreadCount: Int               = 0,
    val isPinned: Boolean              = false,
    val isMuted: Boolean               = false,
    val defaultTtlSeconds: Long        = 0  // session-level message TTL
)

@Singleton
class SessionManager @Inject constructor() {

    private val _sessions       = MutableStateFlow<Map<String, ChatSession>>(emptyMap())
    val sessions: StateFlow<Map<String, ChatSession>> = _sessions

    private val _currentUserId  = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId

    private val _isGuest        = MutableStateFlow(true)
    val isGuest: StateFlow<Boolean> = _isGuest

    fun setCurrentUser(userId: String, isGuest: Boolean) {
        _currentUserId.value = userId
        _isGuest.value = isGuest
    }

    fun openSession(withUserId: String, displayName: String, avatarId: Int) {
        val current = _sessions.value.toMutableMap()
        if (!current.containsKey(withUserId)) {
            current[withUserId] = ChatSession(withUserId, displayName, avatarId)
            _sessions.value = current
        }
    }

    fun addMessage(withUserId: String, message: ChatMessage) {
        val current = _sessions.value.toMutableMap()
        val session = current[withUserId] ?: return
        current[withUserId] = session.copy(
            messages = (session.messages + message).toMutableList(),
            unreadCount = session.unreadCount + 1
        )
        _sessions.value = current
    }

    fun editMessage(withUserId: String, messageId: String, newContent: String) {
        val current = _sessions.value.toMutableMap()
        val session = current[withUserId] ?: return
        val idx = session.messages.indexOfFirst { it.id == messageId }
        if (idx >= 0) session.messages[idx] = session.messages[idx].copy(content = newContent, isEdited = true)
        _sessions.value = current
    }

    fun deleteMessage(withUserId: String, messageId: String) {
        val current = _sessions.value.toMutableMap()
        val session = current[withUserId] ?: return
        val idx = session.messages.indexOfFirst { it.id == messageId }
        if (idx >= 0) session.messages[idx] = session.messages[idx].copy(
            content = "[Message deleted]", type = MessageType.DELETED, isDeleted = true
        )
        _sessions.value = current
    }

    fun addReaction(withUserId: String, messageId: String, emoji: String, reactorUserId: String) {
        val current = _sessions.value.toMutableMap()
        val session = current[withUserId] ?: return
        val idx = session.messages.indexOfFirst { it.id == messageId }
        if (idx >= 0) {
            val msg = session.messages[idx]
            val updated = msg.reactions.toMutableMap()
            val users = updated.getOrDefault(emoji, emptyList()).toMutableList()
            if (!users.contains(reactorUserId)) users.add(reactorUserId)
            updated[emoji] = users
            session.messages[idx] = msg.copy(reactions = updated)
        }
        _sessions.value = current
    }

    fun pruneExpiredMessages(vaultManager: VaultManager) {
        val current = _sessions.value.toMutableMap()
        current.forEach { (uid, session) ->
            val filtered = session.messages.map { msg ->
                if (msg.ttlSeconds > 0 && vaultManager.messageShouldExpire(msg.timestamp, msg.ttlSeconds))
                    msg.copy(content = "[Message expired]", type = MessageType.DELETED, isDeleted = true)
                else msg
            }.toMutableList()
            current[uid] = session.copy(messages = filtered)
        }
        _sessions.value = current
    }

    fun markRead(withUserId: String) {
        val current = _sessions.value.toMutableMap()
        val session = current[withUserId] ?: return
        current[withUserId] = session.copy(unreadCount = 0)
        _sessions.value = current
    }

    fun pinSession(withUserId: String, pinned: Boolean) {
        val current = _sessions.value.toMutableMap()
        current[withUserId]?.let { current[withUserId] = it.copy(isPinned = pinned) }
        _sessions.value = current
    }

    fun muteSession(withUserId: String, muted: Boolean) {
        val current = _sessions.value.toMutableMap()
        current[withUserId]?.let { current[withUserId] = it.copy(isMuted = muted) }
        _sessions.value = current
    }

    fun setChatLock(withUserId: String, locked: Boolean) {
        val current = _sessions.value.toMutableMap()
        current[withUserId]?.let { current[withUserId] = it.copy(isLocked = locked) }
        _sessions.value = current
    }

    fun clearLockedSessions() {
        _sessions.value = _sessions.value.mapValues { (_, s) ->
            if (s.isLocked) s.copy(messages = mutableListOf()) else s
        }
    }

    fun clearAllSessions() { _sessions.value = emptyMap() }

    fun clearGuestIdentity() {
        clearAllSessions()
        _currentUserId.value = null
        _isGuest.value = true
    }

    fun getSession(withUserId: String): ChatSession? = _sessions.value[withUserId]
}
