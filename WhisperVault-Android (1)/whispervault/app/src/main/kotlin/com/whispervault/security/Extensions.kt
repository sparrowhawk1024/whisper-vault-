package com.whispervault.security

// Extension to add clearSessionKeys to SecurityManager
// Already handled inside SecurityManager.kt
// This file adds it to SessionManager for service use

fun SessionManager.clearSessionKeys() {
    // Proxy call — actual key clearing happens in SecurityManager
    // SessionManager just clears in-memory chat data
    clearAllSessions()
}
