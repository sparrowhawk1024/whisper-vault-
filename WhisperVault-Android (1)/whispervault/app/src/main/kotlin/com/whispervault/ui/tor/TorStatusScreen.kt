package com.whispervault.ui.tor

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whispervault.security.TorManager
import com.whispervault.security.TorStatus
import com.whispervault.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TorViewModel @Inject constructor(
    val torManager: TorManager
) : ViewModel() {
    val torStatus   = torManager.status
    val circuitInfo = torManager.circuitInfo
    val exitIp      = torManager.exitIp

    fun checkTor()    = viewModelScope.launch { torManager.checkTorStatus() }
    fun openOrbot()   = torManager.openOrbot()
    fun newIdentity() = torManager.requestNewIdentity()
    fun disconnect()  = torManager.disconnect()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorStatusScreen(onBack: () -> Unit, viewModel: TorViewModel = hiltViewModel()) {
    val status      by viewModel.torStatus.collectAsState()
    val circuit     by viewModel.circuitInfo.collectAsState()
    val exitIp      by viewModel.exitIp.collectAsState()
    val isInstalled  = viewModel.torManager.isOrbotInstalled()

    val statusColor = when (status) {
        TorStatus.CONNECTED    -> TorGreen
        TorStatus.CONNECTING   -> VaultWarning
        TorStatus.ERROR        -> VaultError
        TorStatus.DISCONNECTED -> VaultSubtext
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tor Network", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VaultSurface)
            )
        },
        containerColor = VaultBg
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(120.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(statusColor.copy(alpha = 0.3f), Color.Transparent)))
                    .border(2.dp, statusColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (status == TorStatus.CONNECTED) Icons.Default.Security else Icons.Default.Warning,
                        contentDescription = null, tint = statusColor, modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = when (status) {
                            TorStatus.CONNECTED -> "ACTIVE"; TorStatus.CONNECTING -> "..."
                            TorStatus.ERROR -> "ERROR"; TorStatus.DISCONNECTED -> "OFF"
                        },
                        color = statusColor, fontWeight = FontWeight.Bold, fontSize = 12.sp
                    )
                }
            }

            if (!isInstalled) {
                Card(colors = CardDefaults.cardColors(containerColor = TorOrange.copy(alpha = 0.15f)), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = TorOrange)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Orbot not installed", color = TorOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Install Orbot to route traffic through Tor", color = VaultSubtext, fontSize = 12.sp)
                        }
                    }
                }
            }

            circuit?.let { info ->
                Card(colors = CardDefaults.cardColors(containerColor = VaultCard), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Circuit #${info.circuitId}", color = VaultSubtext, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("YOU", color = TorGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            info.hops.forEach { hop ->
                                Icon(Icons.Default.ArrowForward, null, tint = VaultSubtext, modifier = Modifier.size(12.dp))
                                Text(hop, color = VaultOnSurface, fontSize = 12.sp)
                            }
                        }
                        HorizontalDivider(color = VaultSubtext.copy(alpha = 0.3f))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Exit IP", color = VaultSubtext, fontSize = 12.sp)
                            Text(info.exitIp, color = VaultTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { viewModel.checkTor() }, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = VaultPurple)) {
                    Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Check Tor Status")
                }
                if (status == TorStatus.CONNECTED) {
                    OutlinedButton(onClick = { viewModel.newIdentity() }, modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedButtonDefaults.outlinedButtonColors(contentColor = TorGreen)) {
                        Icon(Icons.Default.Shuffle, null); Spacer(Modifier.width(8.dp)); Text("New Identity")
                    }
                }
                OutlinedButton(onClick = { viewModel.openOrbot() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.OpenInNew, null); Spacer(Modifier.width(8.dp))
                    Text(if (isInstalled) "Open Orbot" else "Install Orbot")
                }
            }

            Text(
                "WhisperVault routes all traffic through Tor when Orbot is active. Configure Orbot with SOCKS5 on 127.0.0.1:9050 for full coverage.",
                color = VaultSubtext, fontSize = 11.sp, textAlign = TextAlign.Center
            )
        }
    }
}
