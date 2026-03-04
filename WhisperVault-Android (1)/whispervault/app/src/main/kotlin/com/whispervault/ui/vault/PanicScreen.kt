package com.whispervault.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.whispervault.security.VaultManager
import com.whispervault.security.SessionManager
import com.whispervault.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PanicViewModel @Inject constructor(
    val vaultManager: VaultManager,
    val sessionManager: SessionManager
) : ViewModel() {
    fun wipeEverything() {
        sessionManager.clearAllSessions()
        vaultManager.triggerPanicWipe()
    }
}

/**
 * Panic Wipe Screen — accessible via shake gesture or hidden button.
 * One tap destroys all messages, keys, and preferences.
 */
@Composable
fun PanicScreen(onWipeComplete: () -> Unit, viewModel: PanicViewModel = hiltViewModel()) {
    var showConfirm by remember { mutableStateOf(false) }
    var wiped       by remember { mutableStateOf(false) }

    if (wiped) {
        Box(Modifier.fillMaxSize().background(VaultBg), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Default.CheckCircle, null, tint = VaultSuccess, modifier = Modifier.size(64.dp))
                Text("Vault Wiped", color = VaultSuccess, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("All data has been destroyed", color = VaultSubtext, fontSize = 14.sp)
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    onWipeComplete()
                }
            }
        }
        return
    }

    Box(Modifier.fillMaxSize().background(VaultBg), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Icon(Icons.Default.DeleteForever, null, tint = VaultError, modifier = Modifier.size(72.dp))
            Text("Panic Wipe", color = VaultError, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                "This will permanently destroy ALL messages, contacts, keys, and app data. This action cannot be undone.",
                color = VaultOnSurface, fontSize = 14.sp, textAlign = TextAlign.Center
            )

            if (!showConfirm) {
                Button(
                    onClick = { showConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = VaultError)
                ) {
                    Icon(Icons.Default.Warning, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Wipe Everything", fontWeight = FontWeight.Bold)
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = VaultError.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Are you absolutely sure?", color = VaultError, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { showConfirm = false }, modifier = Modifier.weight(1f)) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = { viewModel.wipeEverything(); wiped = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = VaultError)
                            ) {
                                Text("WIPE", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
