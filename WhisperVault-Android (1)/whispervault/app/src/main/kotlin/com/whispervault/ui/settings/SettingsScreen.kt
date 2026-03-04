package com.whispervault.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.whispervault.data.AuthRepository
import com.whispervault.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(val authRepository: AuthRepository) : ViewModel()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onTorClick: () -> Unit,
    onPanicClick: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var screenshotBlocked  by remember { mutableStateOf(true) }
    var autoDeleteMessages by remember { mutableStateOf(false) }
    var biometricLock      by remember { mutableStateOf(false) }
    var incognitoKeyboard  by remember { mutableStateOf(true) }
    var torAutoConnect     by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VaultSurface)
            )
        },
        containerColor = VaultBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Privacy ──
            item { SettingsSectionHeader("Privacy & Security") }
            item {
                SettingsToggle(Icons.Default.Screenshot, "Block Screenshots",
                    "Prevent screen recording and screenshots", screenshotBlocked) { screenshotBlocked = it }
            }
            item {
                SettingsToggle(Icons.Default.Keyboard, "Incognito Keyboard",
                    "Disable keyboard suggestions and history", incognitoKeyboard) { incognitoKeyboard = it }
            }
            item {
                SettingsToggle(Icons.Default.Fingerprint, "Biometric Lock",
                    "Require fingerprint to unlock app", biometricLock) { biometricLock = it }
            }
            item {
                SettingsToggle(Icons.Default.Timer, "Auto-Delete Messages",
                    "Messages expire 24h after being read", autoDeleteMessages) { autoDeleteMessages = it }
            }

            // ── Tor ──
            item { SettingsSectionHeader("Tor Network") }
            item {
                SettingsToggle(Icons.Default.VpnLock, "Auto-Connect Tor",
                    "Enable Tor routing on app start", torAutoConnect) { torAutoConnect = it }
            }
            item {
                SettingsAction(Icons.Default.Security, "Tor Status & Circuit",
                    "View your Tor circuit and exit node") { onTorClick() }
            }

            // ── Vault ──
            item { SettingsSectionHeader("Vault") }
            item {
                SettingsAction(Icons.Default.DeleteForever, "Panic Wipe",
                    "Instantly destroy all app data", tint = VaultError) { onPanicClick() }
            }

            // ── Account ──
            item { SettingsSectionHeader("Account") }
            item {
                SettingsAction(Icons.Default.Logout, "Log Out",
                    "Sign out and clear session", tint = VaultError) { onLogout() }
            }

            // ── About ──
            item { SettingsSectionHeader("About") }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = VaultCard), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("WhisperVault", color = VaultPurple, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Version 2.0.0", color = VaultSubtext, fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("End-to-end encrypted messaging with Tor support.", color = VaultOnSurface, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(title, color = VaultPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
}

@Composable
fun SettingsToggle(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = VaultCard), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = VaultPurple, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = VaultOnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = VaultSubtext, fontSize = 12.sp)
            }
            Switch(checked = checked, onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedThumbColor = VaultPurple, checkedTrackColor = VaultPurple.copy(alpha = 0.4f)))
        }
    }
}

@Composable
fun SettingsAction(icon: ImageVector, title: String, subtitle: String, tint: androidx.compose.ui.graphics.Color = VaultPurple, onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = VaultCard), modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = if (tint == VaultError) VaultError else VaultOnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = VaultSubtext, fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = VaultSubtext)
        }
    }
}
