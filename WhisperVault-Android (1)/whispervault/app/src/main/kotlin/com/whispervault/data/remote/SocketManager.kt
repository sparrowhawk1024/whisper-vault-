package com.whispervault.data.remote

import android.util.Log
import com.whispervault.BuildConfig
import com.whispervault.security.EncryptedPayload
import com.whispervault.security.SecurityManager
import com.whispervault.security.SessionManager
import com.whispervault.security.ChatMessage
import com.whispervault.security.MessageType
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class SocketEvent(val type: String, val data: JSONObject)

@Singleton
class SocketManager @Inject constructor(
    private val securityManager: SecurityManager,
    private val sessionManager: SessionManager
) {
    private var socket: Socket? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _events = MutableSharedFlow<SocketEvent>(extraBufferCapacity = 100)
    val events: SharedFlow<SocketEvent> = _events

    val isConnected: Boolean get() = socket?.connected() == true

    fun connect(token: String?) {
        try {
            val opts = IO.Options().apply {
                auth = mapOf("token" to (token ?: ""))
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                reconnectionDelay = 1500
                timeout = 20000
            }
            socket = IO.socket(BuildConfig.SOCKET_URL, opts)
            bindEvents()
            socket?.connect()
        } catch (e: Exception) {
            Log.e("SocketManager", "Connect error: ${e.message}")
        }
    }

    private fun bindEvents() {
        socket?.apply {
            on(Socket.EVENT_CONNECT)    { Log.d("WV-Socket", "Connected") }
            on(Socket.EVENT_DISCONNECT) { Log.d("WV-Socket", "Disconnected") }

            on("newMessage") { args ->
                val data = args[0] as JSONObject
                scope.launch {
                    try {
                        val fromUserId = data.getString("fromUserId")
                        val encData    = data.getString("encryptedData")
                        val iv         = data.getString("iv")
                        val messageId  = data.getString("messageId")
                        val type       = data.optString("type", "text")
                        val edited     = data.optBoolean("edited", false)
                        val timestamp  = data.optLong("timestamp", System.currentTimeMillis())
                        val ttl        = data.optLong("ttlSeconds", 0)
                        val replyTo    = data.optString("replyToId", null)

                        val decrypted = securityManager.decryptMessage(EncryptedPayload(encData, iv))
                        val msgType = when (type) {
                            "image" -> MessageType.IMAGE
                            "voice" -> MessageType.VOICE
                            "file"  -> MessageType.FILE
                            "deleted" -> MessageType.DELETED
                            else -> MessageType.TEXT
                        }
                        sessionManager.addMessage(fromUserId, ChatMessage(
                            id = messageId, fromUserId = fromUserId,
                            content = decrypted, type = msgType,
                            timestamp = timestamp, isEdited = edited,
                            ttlSeconds = ttl, replyToId = replyTo
                        ))
                        _events.emit(SocketEvent("newMessage", data))
                    } catch (e: Exception) {
                        Log.e("WV-Socket", "Decrypt error: ${e.message}")
                    }
                }
            }

            on("messageDeleted") { args ->
                val data = args[0] as JSONObject
                scope.launch {
                    sessionManager.deleteMessage(data.getString("deletedBy"), data.getString("messageId"))
                    _events.emit(SocketEvent("messageDeleted", data))
                }
            }

            on("messageEdited") { args ->
                val data = args[0] as JSONObject
                scope.launch {
                    try {
                        val msgId     = data.getString("messageId")
                        val decrypted = securityManager.decryptMessage(
                            EncryptedPayload(data.getString("encryptedData"), data.getString("iv"))
                        )
                        _events.emit(SocketEvent("messageEdited", data.put("decrypted", decrypted)))
                    } catch (e: Exception) { Log.e("WV-Socket", "Edit decrypt error: ${e.message}") }
                }
            }

            on("messageReaction") { args ->
                val data = args[0] as JSONObject
                scope.launch {
                    sessionManager.addReaction(
                        data.getString("fromUserId"),
                        data.getString("messageId"),
                        data.getString("emoji"),
                        data.getString("fromUserId")
                    )
                    _events.emit(SocketEvent("messageReaction", data))
                }
            }

            on("userTyping")     { args -> scope.launch { _events.emit(SocketEvent("userTyping", args[0] as JSONObject)) } }
            on("userOnline")     { args -> scope.launch { _events.emit(SocketEvent("userOnline", args[0] as JSONObject)) } }
            on("chatLockChanged"){ args -> scope.launch { _events.emit(SocketEvent("chatLockChanged", args[0] as JSONObject)) } }
            on("receivePublicKey") { args ->
                val data = args[0] as JSONObject
                scope.launch {
                    securityManager.deriveSessionKey(data.getString("publicKey"))
                    _events.emit(SocketEvent("receivePublicKey", data))
                }
            }
            on("incomingChatRequest") { args ->
                scope.launch { _events.emit(SocketEvent("incomingChatRequest", args[0] as JSONObject)) }
            }
        }
    }

    fun joinRoom(withUserId: String) {
        socket?.emit("joinRoom", JSONObject().put("withUserId", withUserId))
        val myPublicKey = securityManager.generateEphemeralECDHKeyPair()
        socket?.emit("sharePublicKey", JSONObject()
            .put("toUserId", withUserId)
            .put("publicKey", myPublicKey))
    }

    fun sendMessage(toUserId: String, plaintext: String, type: String = "text", ttlSeconds: Long = 0, replyToId: String? = null): String {
        val messageId = UUID.randomUUID().toString()
        val encrypted = securityManager.encryptMessage(plaintext)
        val payload = JSONObject()
            .put("toUserId", toUserId)
            .put("encryptedData", encrypted.data)
            .put("iv", encrypted.iv)
            .put("messageId", messageId)
            .put("type", type)
            .put("timestamp", System.currentTimeMillis())
            .put("ttlSeconds", ttlSeconds)
        replyToId?.let { payload.put("replyToId", it) }
        socket?.emit("sendMessage", payload)
        return messageId
    }

    fun sendReaction(toUserId: String, messageId: String, emoji: String) {
        socket?.emit("reactMessage", JSONObject()
            .put("toUserId", toUserId)
            .put("messageId", messageId)
            .put("emoji", emoji))
    }

    fun deleteMessageForAll(toUserId: String, messageId: String) {
        socket?.emit("deleteMessage", JSONObject().put("toUserId", toUserId).put("messageId", messageId))
    }

    fun editMessage(toUserId: String, messageId: String, newText: String) {
        val encrypted = securityManager.encryptMessage(newText)
        socket?.emit("editMessage", JSONObject()
            .put("toUserId", toUserId).put("messageId", messageId)
            .put("encryptedData", encrypted.data).put("iv", encrypted.iv))
    }

    fun setChatLock(toUserId: String, locked: Boolean) {
        socket?.emit("setChatLock", JSONObject().put("toUserId", toUserId).put("locked", locked))
    }

    fun sendTyping(toUserId: String, isTyping: Boolean) {
        socket?.emit("typing", JSONObject().put("toUserId", toUserId).put("isTyping", isTyping))
    }

    fun notifyChatRequest(toUserId: String, requestId: String) {
        socket?.emit("notifyChatRequest", JSONObject().put("toUserId", toUserId).put("requestId", requestId))
    }

    fun disconnect() { socket?.disconnect(); socket = null }
}
