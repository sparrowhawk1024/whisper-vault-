package com.whispervault.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Nothing to restore — chats are ephemeral
            // Connections are persisted in Room DB and will load on next app open
        }
    }
}
