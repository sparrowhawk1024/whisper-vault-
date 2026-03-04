package com.whispervault.ui.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whispervault.ui.theme.*

@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onGuestClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(GhostBackground, GhostSurface)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(60.dp))

            // Logo area
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(
                            Brush.radialGradient(listOf(GhostGreen.copy(alpha = 0.3f), GhostBackground))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = GhostGreen,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "WhisperVault",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = GhostText
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Encrypted. Anonymous. Private.",
                    fontSize = 14.sp,
                    color = GhostTextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(32.dp))

                // Feature badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    FeatureBadge("E2E Encrypted")
                    FeatureBadge("No Logs")
                    FeatureBadge("Auto-Clear")
                }
            }

            // Action buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onRegisterClick,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GhostGreen),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "Create Account",
                        color = GhostBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                OutlinedButton(
                    onClick = onLoginClick,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GhostGreen),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, GhostGreen),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Sign In", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }

                TextButton(
                    onClick = onGuestClick,
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text(
                        "Continue as Guest",
                        color = GhostTextSecondary,
                        fontSize = 14.sp
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Guest chats are never saved.\nAll messages auto-delete on exit.",
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    color = GhostTextSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun FeatureBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(GhostSurfaceVariant)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text, fontSize = 11.sp, color = GhostGreen, fontWeight = FontWeight.Medium)
    }
}
