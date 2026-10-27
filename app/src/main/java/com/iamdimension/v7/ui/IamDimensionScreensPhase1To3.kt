package com.iamdimension.v7.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.JusticeModule
import com.example.data.model.VaultLanguage
import kotlinx.coroutines.delay

/**
 * PHASE 1: EXTERIOR BOOK COVER UI (IMG 1, IMG 2, IMG 4)
 */
@Composable
fun AuthScreen(
    onPortalActivated: () -> Unit
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var imgState by remember { mutableIntStateOf(1) } // 1 = IMG 1 Exterior Cover, 2 = IMG 2 Biometric Lock, 4 = IMG 4 Portal Ready
    var isSigningIn by remember { mutableStateOf(false) }
    var authStatusMessage by remember {
        mutableStateOf(
            "DB: ai-studio-android-iamsover-129d727b-49ce-41e6-b574-5705e44eced2"
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "portal_ring")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_ring"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        IamDimensionPalette.VoidBlack,
                        IamDimensionPalette.DeepNebula,
                        IamDimensionPalette.VoidBlack
                    )
                )
            )
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = IamDimensionPalette.ObsidianCard,
                border = BorderStroke(1.dp, IamDimensionPalette.CyberGold),
                shape = RoundedCornerShape(50),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = "LROS VR26.10 • 0519 PORTAL ZONE • IMG $imgState/19",
                    style = MaterialTheme.typography.labelMedium,
                    color = IamDimensionPalette.CyberGold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
            Text(
                text = "I.A.M DIMENSION V.7",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = IamDimensionPalette.StarlightSilver,
                textAlign = TextAlign.Center
            )
            Text(
                text = "SOVEREIGN VAULT 1293 • REX THE DOPELGANGER",
                style = MaterialTheme.typography.labelMedium,
                color = IamDimensionPalette.PhylaxCyan,
                textAlign = TextAlign.Center
            )
        }

        // Galactic Book Cover Emblem + Interactive IMG 1 / IMG 2 / IMG 4 State Envelope
        Card(
            colors = CardDefaults.cardColors(containerColor = IamDimensionPalette.ObsidianCard),
            border = BorderStroke(2.dp, IamDimensionPalette.CyberGold.copy(alpha = pulse)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val r = size.minDimension / 2f
                        drawCircle(
                            color = IamDimensionPalette.CyberGold.copy(alpha = 0.25f * pulse),
                            radius = r
                        )
                        drawCircle(
                            color = IamDimensionPalette.PhylaxCyan,
                            radius = r * 0.82f,
                            style = Stroke(width = 4f)
                        )
                        drawCircle(
                            color = IamDimensionPalette.NeonMagenta,
                            radius = r * 0.62f,
                            style = Stroke(width = 3f)
                        )
                    }
                    Icon(
                        imageVector = when (imgState) {
                            1 -> Icons.Default.Security
                            2 -> Icons.Default.Fingerprint
                            else -> Icons.Default.LockOpen
                        },
                        contentDescription = "0519 Portal Crest",
                        tint = IamDimensionPalette.CyberGold,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = when (imgState) {
                        1 -> "IMG 1: EXTERIOR BOOK COVER SEALED"
                        2 -> "IMG 2: BIOMETRIC KEYRING & OAUTH HANDSHAKE"
                        else -> "IMG 4: 0519 PORTAL ZONE SYNCHRONIZED"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = IamDimensionPalette.CyberGold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = authStatusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = IamDimensionPalette.MutedSlate,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { imgState = 1 },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("auth_img1_button")
                    ) {
                        Text("IMG 1", color = IamDimensionPalette.StarlightSilver)
                    }
                    OutlinedButton(
                        onClick = { imgState = 2 },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("auth_img2_button")
                    ) {
                        Text("IMG 2", color = IamDimensionPalette.PhylaxCyan)
                    }
                    OutlinedButton(
                        onClick = { imgState = 4 },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("auth_img4_button")
                    ) {
                        Text("IMG 4", color = IamDimensionPalette.CyberGold)
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    imgState = 2
                    isSigningIn = true
                    try {
                        val credentialManager = androidx.credentials.CredentialManager.create(context)
                        com.example.ui.auth.onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isSigningIn = false
                                imgState = 4
                                authStatusMessage = "GOOGLE OAUTH VERIFIED • PORTAL UNLOCKED"
                                onPortalActivated()
                            },
                            onAuthError = { err ->
                                isSigningIn = false
                                authStatusMessage = "OAuth Status: $err"
                            },
                            scope = scope,
                            onAuthCancelled = {
                                isSigningIn = false
                            }
                        )
                    } catch (e: Exception) {
                        isSigningIn = false
                        authStatusMessage = "OAuth Ready: ${e.localizedMessage ?: "CredentialManager initialized"}"
                    }
                },
                enabled = !isSigningIn,
                border = BorderStroke(1.5.dp, IamDimensionPalette.PhylaxCyan),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("register_login_google_button")
            ) {
                Icon(
                    Icons.Default.VerifiedUser,
                    contentDescription = "Google Sign-In",
                    tint = IamDimensionPalette.PhylaxCyan
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "REGISTER / LOGIN (GOOGLE SIGN-IN)",
                    color = IamDimensionPalette.PhylaxCyan,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onPortalActivated,
                colors = ButtonDefaults.buttonColors(
                    containerColor = IamDimensionPalette.CyberGold,
                    contentColor = IamDimensionPalette.VoidBlack
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("enter_0519_portal_button")
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "IGNITE 0519 PORTAL ZONE (IMG 3)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

/**
 * PHASE 1B: BLACK HOLE PORTAL TRANSITION (IMG 3)
 */
@Composable
fun PortalTransitionScreen(
    onTransitionComplete: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "black_hole_vortex")
    val sweep by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vortex_angle"
    )

    LaunchedEffect(Unit) {
        delay(1100)
        onTransitionComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IamDimensionPalette.VoidBlack)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2f
                    drawCircle(
                        color = IamDimensionPalette.NeonMagenta.copy(alpha = 0.25f),
                        radius = radius
                    )
                    drawArc(
                        color = IamDimensionPalette.PhylaxCyan,
                        startAngle = sweep,
                        sweepAngle = 240f,
                        useCenter = false,
                        style = Stroke(width = 8f)
                    )
                    drawArc(
                        color = IamDimensionPalette.CyberGold,
                        startAngle = -sweep,
                        sweepAngle = 180f,
                        useCenter = false,
                        style = Stroke(width = 5f)
                    )
                }
                Text(
                    text = "0519",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = IamDimensionPalette.CyberGold
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "IMG 3: EVENT HORIZON PORTAL TRANSITION",
                style = MaterialTheme.typography.titleMedium,
                color = IamDimensionPalette.PhylaxCyan,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Traversing Wormhole to Interior Avatar Dashboard...",
                style = MaterialTheme.typography.bodyMedium,
                color = IamDimensionPalette.MutedSlate
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onTransitionComplete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = IamDimensionPalette.PhylaxCyan,
                    contentColor = IamDimensionPalette.VoidBlack
                ),
                modifier = Modifier.testTag("skip_portal_transition_button")
            ) {
                Text("ENTER VAULT DASHBOARD NOW", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * PHASE 2: INTERIOR AVATAR DASHBOARD (IMG 5 & IMG 7 MODAL)
 */
@Composable
fun VaultDashboardScreen(
    appState: IamDimensionAppState,
    onNavigateToLros: () -> Unit,
    onNavigateToRxm: () -> Unit,
    onNavigateToWolfsDen: () -> Unit,
    onNavigateToBlackCase: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToForensicLedger: () -> Unit,
    onNavigateToSecuritySeal: () -> Unit,
    onLogout: () -> Unit
) {
    if (appState.showVaultSwitchModal) {
        AlertDialog(
            onDismissRequest = { appState.showVaultSwitchModal = false },
            containerColor = IamDimensionPalette.DeepNebula,
            titleContentColor = IamDimensionPalette.CyberGold,
            textContentColor = IamDimensionPalette.StarlightSilver,
            title = {
                Text(
                    text = "IMG 7: SOVEREIGN VAULT SWITCHER",
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Select target dimension node (Active Language: ${appState.activeLanguage.displayName}):",
                        style = MaterialTheme.typography.bodySmall,
                        color = IamDimensionPalette.MutedSlate
                    )
                    Button(
                        onClick = {
                            appState.showVaultSwitchModal = false
                            onNavigateToLros()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IamDimensionPalette.CyberGold,
                            contentColor = IamDimensionPalette.VoidBlack
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("modal_lros_vault_button")
                    ) {
                        Text("LROS JUGGERNAUT VAULT 1293 (IMG 8-10)", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            appState.showVaultSwitchModal = false
                            onNavigateToRxm()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IamDimensionPalette.PhylaxCyan,
                            contentColor = IamDimensionPalette.VoidBlack
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("modal_rxm_vault_button")
                    ) {
                        Text("RXM PHYLAX VAULT 0519 (IMG 11-13)", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            appState.showVaultSwitchModal = false
                            onNavigateToWolfsDen()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IamDimensionPalette.NeonMagenta,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("modal_wolfs_den_button")
                    ) {
                        Text("WOLF'S DEN 3D .GLB STUDIO (IMG 14-16)", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { appState.showVaultSwitchModal = false }) {
                    Text("CLOSE", color = IamDimensionPalette.CyberGold)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(IamDimensionPalette.VoidBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = IamDimensionPalette.ObsidianCard),
                border = BorderStroke(1.5.dp, IamDimensionPalette.CyberGold),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "IMG 5: INTERIOR AVATAR DASHBOARD",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = IamDimensionPalette.CyberGold
                            )
                            Text(
                                text = "Active Node: ${appState.activeVaultId} • Lang: ${appState.activeLanguage.displayName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = IamDimensionPalette.PhylaxCyan
                            )
                        }
                        Row {
                            IconButton(
                                onClick = onNavigateToSettings,
                                modifier = Modifier.testTag("dashboard_settings_button")
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "9-Language Settings",
                                    tint = IamDimensionPalette.CyberGold
                                )
                            }
                            IconButton(
                                onClick = onLogout,
                                modifier = Modifier.testTag("dashboard_logout_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "Return to Exterior Portal",
                                    tint = IamDimensionPalette.CrimsonAlert
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CATCH BASIN CAPACITY: ${appState.catchBasinCapacityPercent}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (appState.isBlackCaseUnlocked) {
                                IamDimensionPalette.EmeraldMatrix
                            } else {
                                IamDimensionPalette.CyberGold
                            }
                        )
                        TextButton(
                            onClick = { appState.showVaultSwitchModal = true },
                            modifier = Modifier.testTag("open_vault_switcher_modal_button")
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = IamDimensionPalette.PhylaxCyan)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("IMG 7 SWITCH MODAL", color = IamDimensionPalette.PhylaxCyan)
                        }
                    }
                    LinearProgressIndicator(
                        progress = { appState.catchBasinCapacityPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = if (appState.isBlackCaseUnlocked) {
                            IamDimensionPalette.EmeraldMatrix
                        } else {
                            IamDimensionPalette.CyberGold
                        },
                        trackColor = IamDimensionPalette.DeepNebula
                    )
                }
            }
        }

        // Navigation Grid Cards for all 19 Mapped Screens
        item {
            DashboardPortalCard(
                title = "LROS JUGGERNAUT VAULT 1293 (IMG 8 & IMG 9)",
                subtitle = "7 Justice Modules • Module 4 = SAKUNA (RA 10121 / NDRRMC / PAGASA / BFP)",
                accentColor = IamDimensionPalette.CyberGold,
                icon = Icons.Default.Gavel,
                tag = "nav_lros_vault_card",
                onClick = onNavigateToLros
            )
        }

        item {
            DashboardPortalCard(
                title = "LROS FORENSIC LEDGER SCANNER (IMG 10)",
                subtitle = "Chain-of-Custody Hashing • Deep Forensic Packet Audit",
                accentColor = IamDimensionPalette.EmeraldMatrix,
                icon = Icons.Default.AccountBalance,
                tag = "nav_forensic_ledger_card",
                onClick = onNavigateToForensicLedger
            )
        }

        item {
            DashboardPortalCard(
                title = "RXM PHYLAX VAULT 0519 & RADAR (IMG 11-13)",
                subtitle = "Bio-Defense Telemetry • Live GPS PAO / Hospital / Vet Radar",
                accentColor = IamDimensionPalette.PhylaxCyan,
                icon = Icons.Default.Shield,
                tag = "nav_rxm_vault_card",
                onClick = onNavigateToRxm
            )
        }

        item {
            DashboardPortalCard(
                title = "WOLF'S DEN 3D .GLB & SMOLAGENTS (IMG 14-16)",
                subtitle = "DeepMotion 3D Rig • /api/smolagents/route • Real-Time TTS Lip-Sync",
                accentColor = IamDimensionPalette.NeonMagenta,
                icon = Icons.Default.AutoAwesome,
                tag = "nav_wolfs_den_card",
                onClick = onNavigateToWolfsDen
            )
        }

        item {
            DashboardPortalCard(
                title = "THE BLACK CASE MASTER NODE (IMG 17 -> IMG 18)",
                subtitle = if (appState.isBlackCaseUnlocked) {
                    "STATUS: OWNER AUTHENTICATED (100% OVERFLOW CONDUITS ACTIVE)"
                } else {
                    "STATUS: LOCKED AT 60% • ENTER KAGGLE WAKE-UP KEY TO UNLOCK"
                },
                accentColor = if (appState.isBlackCaseUnlocked) {
                    IamDimensionPalette.EmeraldMatrix
                } else {
                    IamDimensionPalette.CrimsonAlert
                },
                icon = if (appState.isBlackCaseUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                tag = "nav_black_case_card",
                onClick = onNavigateToBlackCase
            )
        }

        item {
            DashboardPortalCard(
                title = "9-LANGUAGE STRICT SELECTOR (IMG 6)",
                subtitle = "Current: ${appState.activeLanguage.displayName} • English, Tagalog, Kapampangan, Pangasinan, Ilocano, Ibanag, Bisaya, Maranao, Tausug",
                accentColor = IamDimensionPalette.PhylaxCyan,
                icon = Icons.Default.Language,
                tag = "nav_settings_card",
                onClick = onNavigateToSettings
            )
        }

        item {
            DashboardPortalCard(
                title = "SHA-256 SECURITY VERIFICATION SEAL (IMG 19)",
                subtitle = "Token: X4R9M2K7 • Cryptographic Integrity Ledger Verification",
                accentColor = IamDimensionPalette.CyberGold,
                icon = Icons.Default.VerifiedUser,
                tag = "nav_security_seal_card",
                onClick = onNavigateToSecuritySeal
            )
        }
    }
}

@Composable
private fun DashboardPortalCard(
    title: String,
    subtitle: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = IamDimensionPalette.ObsidianCard),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.75f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = accentColor.copy(alpha = 0.16f),
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = title, tint = accentColor)
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = IamDimensionPalette.StarlightSilver
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = IamDimensionPalette.MutedSlate
                )
            }
        }
    }
}

/**
 * PHASE 2B: SETTINGS SCREEN — 9-LANGUAGE STRICT MODE SELECTOR (IMG 6)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appState: IamDimensionAppState,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var expanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(IamDimensionPalette.VoidBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = IamDimensionPalette.CyberGold
                    )
                }
                Text(
                    text = "IMG 6: 9-LANGUAGE STRICT SELECTOR",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = IamDimensionPalette.CyberGold
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = IamDimensionPalette.ObsidianCard),
                border = BorderStroke(1.dp, IamDimensionPalette.PhylaxCyan),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "STRICT SINGLE-LANGUAGE PROTOCOL",
                        style = MaterialTheme.typography.labelLarge,
                        color = IamDimensionPalette.PhylaxCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Selecting a language below enforces strict single-language AI synthesis across LROS 1293, RXM 0519, and Wolf's Den Smolagents.",
                        style = MaterialTheme.typography.bodySmall,
                        color = IamDimensionPalette.MutedSlate
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = "${appState.activeLanguage.displayName} (${appState.activeLanguage.isoCode})",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Active Vault Language (9 Languages)") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .testTag("nine_language_dropdown_anchor")
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            VaultLanguage.entries.forEach { lang ->
                                DropdownMenuItem(
                                    text = {
                                        Text("${lang.displayName} (${lang.isoCode}) — ${lang.localizedBanner}")
                                    },
                                    onClick = {
                                        appState.activeLanguage = lang
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        items(VaultLanguage.entries) { lang ->
            val isSelected = appState.activeLanguage == lang
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        IamDimensionPalette.DeepNebula
                    } else {
                        IamDimensionPalette.ObsidianCard
                    }
                ),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) IamDimensionPalette.CyberGold else IamDimensionPalette.MutedSlate.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { appState.activeLanguage = lang }
                    .testTag("select_lang_${lang.name.lowercase()}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${lang.displayName} (${lang.isoCode})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) IamDimensionPalette.CyberGold else IamDimensionPalette.StarlightSilver
                        )
                        Text(
                            text = lang.localizedBanner,
                            style = MaterialTheme.typography.bodySmall,
                            color = IamDimensionPalette.MutedSlate
                        )
                    }
                    if (isSelected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Active Language",
                            tint = IamDimensionPalette.EmeraldMatrix
                        )
                    }
                }
            }
        }
    }
}

/**
 * PHASE 3: LROS JUGGERNAUT VAULT 1293 (IMG 8, IMG 9 [MODULE 4 = SAKUNA])
 */
@Composable
fun LrosVaultScreen(
    appState: IamDimensionAppState,
    onOpenForensicLedger: () -> Unit,
    onOpenContactRadar: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(IamDimensionPalette.VoidBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("lros_back_button")) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = IamDimensionPalette.CyberGold
                    )
                }
                Column {
                    Text(
                        text = "IMG 8 & 9: LROS JUGGERNAUT VAULT 1293",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = IamDimensionPalette.CyberGold
                    )
                    Text(
                        text = "7 Justice Modules • Module 4 = 4. SAKUNA (Kapatiran at Bayanihan)",
                        style = MaterialTheme.typography.labelSmall,
                        color = IamDimensionPalette.PhylaxCyan
                    )
                }
            }
        }

        // Highlighted FIX 2: Module 4 SAKUNA Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = IamDimensionPalette.DeepNebula),
                border = BorderStroke(2.dp, IamDimensionPalette.CrimsonAlert),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Multi-Hazard Sakuna Icon",
                            tint = IamDimensionPalette.CrimsonAlert
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MODULE 4: 4. SAKUNA (RA 10121 / NDRRMC / PAGASA / BFP)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = IamDimensionPalette.CyberGold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Human-centric disaster response. Bagyo, Lindol, Sunog, Baha. Focus on Kapatiran at Bayanihan. Access emergency hotlines (NDRRMC, PAGASA, BFP).",
                        style = MaterialTheme.typography.bodySmall,
                        color = IamDimensionPalette.StarlightSilver
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:911")))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sakuna_dial_911")
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("911 / BFP", color = IamDimensionPalette.CrimsonAlert)
                        }
                        OutlinedButton(
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:0289115061")))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sakuna_dial_ndrrmc")
                        ) {
                            Text("NDRRMC", color = IamDimensionPalette.CyberGold)
                        }
                        OutlinedButton(
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:0282840800")))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sakuna_dial_pagasa")
                        ) {
                            Text("PAGASA", color = IamDimensionPalette.PhylaxCyan)
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenForensicLedger,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IamDimensionPalette.CyberGold,
                        contentColor = IamDimensionPalette.VoidBlack
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("lros_open_forensic_ledger_button")
                ) {
                    Text("IMG 10: FORENSIC LEDGER", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onOpenContactRadar,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IamDimensionPalette.PhylaxCyan,
                        contentColor = IamDimensionPalette.VoidBlack
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("lros_open_contact_radar_button")
                ) {
                    Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("GPS RADAR", fontWeight = FontWeight.Bold)
                }
            }
        }

        items(JusticeModule.entries) { module ->
            val selected = appState.selectedLrosModule == module
            Card(
                colors = CardDefaults.cardColors(containerColor = IamDimensionPalette.ObsidianCard),
                border = BorderStroke(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) IamDimensionPalette.CyberGold else IamDimensionPalette.MutedSlate.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { appState.selectedLrosModule = module }
                    .testTag("lros_module_${module.code}")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "${module.code}: ${module.title}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = IamDimensionPalette.CyberGold
                    )
                    Text(
                        text = module.statute,
                        style = MaterialTheme.typography.labelSmall,
                        color = IamDimensionPalette.PhylaxCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = module.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = IamDimensionPalette.StarlightSilver
                    )
                }
            }
        }
    }
}

/**
 * PHASE 3B: LROS FORENSIC LEDGER SCANNER (IMG 10)
 */
@Composable
fun ForensicLedgerScreen(
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var ledgerInput by remember { mutableStateOf("LROS-1293-DOCKET-0519: Chain of Custody Packet") }
    var scannedDigest by remember {
        mutableStateOf("SHA-256: 9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IamDimensionPalette.VoidBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("forensic_back_button")) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = IamDimensionPalette.CyberGold
                )
            }
            Text(
                text = "IMG 10: LROS FORENSIC LEDGER SCANNER",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = IamDimensionPalette.CyberGold
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = IamDimensionPalette.ObsidianCard),
            border = BorderStroke(1.dp, IamDimensionPalette.EmeraldMatrix),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "EVIDENCE CHAIN-OF-CUSTODY HASH GENERATOR",
                    style = MaterialTheme.typography.labelLarge,
                    color = IamDimensionPalette.EmeraldMatrix,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = ledgerInput,
                    onValueChange = { ledgerInput = it },
                    label = { Text("Forensic Evidence Payload") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forensic_payload_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val digest = java.security.MessageDigest.getInstance("SHA-256")
                            .digest(ledgerInput.toByteArray())
                            .joinToString("") { "%02x".format(it) }
                        scannedDigest = "SHA-256: $digest"
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IamDimensionPalette.EmeraldMatrix,
                        contentColor = IamDimensionPalette.VoidBlack
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("compute_forensic_hash_button")
                ) {
                    Text("COMPUTE FORENSIC SHA-256 SEAL", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = scannedDigest,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = IamDimensionPalette.CyberGold
                )
            }
        }
    }
}
