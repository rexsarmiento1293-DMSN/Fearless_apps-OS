package com.iamdimension.v7.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.VaultLanguage
import com.example.data.remote.GeminiService
import com.example.data.remote.LrosContactRadarService
import com.example.data.remote.RadarScanTelemetry
import com.example.ui.components.OutlawAvatarSceneView
import kotlinx.coroutines.launch

/**
 * PHASE 4: RXM PHYLAX VAULT 0519 (IMG 11 & IMG 12)
 */
@Composable
fun RxmVaultScreen(
    onOpenContactRadar: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(IamDimensionPalette.VoidBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("rxm_back_button")) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = IamDimensionPalette.PhylaxCyan
                    )
                }
                Column {
                    Text(
                        text = "IMG 11 & 12: RXM PHYLAX VAULT 0519",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = IamDimensionPalette.PhylaxCyan
                    )
                    Text(
                        text = "Bio-Safety Level 4 • Veterinary & Medical Triage Matrix",
                        style = MaterialTheme.typography.labelSmall,
                        color = IamDimensionPalette.CyberGold
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = IamDimensionPalette.ObsidianCard),
                border = BorderStroke(1.5.dp, IamDimensionPalette.PhylaxCyan),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Science,
                            contentDescription = "RXM Bio-Telemetry",
                            tint = IamDimensionPalette.PhylaxCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PHYLAX 0519 BIO-DEFENSE & ZOONOTIC SENTINEL",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = IamDimensionPalette.StarlightSilver
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Real-time human & veterinary rapid response matrix connected to the RXM/LROS GPS Contact Radar (PAO Legal Aid, Emergency Hospitals, and Veterinary Clinics).",
                        style = MaterialTheme.typography.bodySmall,
                        color = IamDimensionPalette.MutedSlate
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onOpenContactRadar,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IamDimensionPalette.PhylaxCyan,
                            contentColor = IamDimensionPalette.VoidBlack
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rxm_launch_contact_radar_button")
                    ) {
                        Icon(Icons.Default.Radar, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("OPEN IMG 13: RXM/LROS GPS CONTACT RADAR", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

/**
 * PHASE 4B: RXM/LROS CONTACT RADAR WITH LIVE GPS (IMG 13 - FIX 5)
 */
@Composable
fun ContactRadarScreen(
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedType by remember { mutableStateOf("PAO") }
    var isScanning by remember { mutableStateOf(false) }
    var telemetry by remember { mutableStateOf<RadarScanTelemetry?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        isScanning = true
        scope.launch {
            telemetry = LrosContactRadarService.findNearestProfessional(context, selectedType)
            isScanning = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(IamDimensionPalette.VoidBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("radar_back_button")) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = IamDimensionPalette.PhylaxCyan
                    )
                }
                Column {
                    Text(
                        text = "IMG 13: RXM/LROS GPS CONTACT RADAR",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = IamDimensionPalette.PhylaxCyan
                    )
                    Text(
                        text = "findNearestProfessional(type) • PAO | Hospital | Vet",
                        style = MaterialTheme.typography.labelSmall,
                        color = IamDimensionPalette.CyberGold
                    )
                }
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("PAO", "Hospital", "Vet").forEach { profType ->
                            Button(
                                onClick = {
                                    selectedType = profType
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedType == profType) {
                                        IamDimensionPalette.PhylaxCyan
                                    } else {
                                        IamDimensionPalette.DeepNebula
                                    },
                                    contentColor = if (selectedType == profType) {
                                        IamDimensionPalette.VoidBlack
                                    } else {
                                        IamDimensionPalette.StarlightSilver
                                    }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("radar_scan_${profType.lowercase()}")
                            ) {
                                Text(profType, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (isScanning) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = IamDimensionPalette.PhylaxCyan
                        )
                    }

                    telemetry?.let { data ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "GPS: ${"%.4f".format(data.userLatitude)}, ${"%.4f".format(data.userLongitude)} • ${data.gpsProviderUsed}",
                            style = MaterialTheme.typography.labelSmall,
                            color = IamDimensionPalette.EmeraldMatrix
                        )
                    }
                }
            }
        }

        val results = telemetry?.places.orEmpty()
        items(results) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = IamDimensionPalette.ObsidianCard),
                border = BorderStroke(1.dp, IamDimensionPalette.CyberGold.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = IamDimensionPalette.CyberGold
                    )
                    Text(
                        text = "${item.address} (${"%.2f".format(item.distanceKm)} km)",
                        style = MaterialTheme.typography.bodySmall,
                        color = IamDimensionPalette.StarlightSilver
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.phoneOrHotline}"))
                                )
                            }
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(item.phoneOrHotline, color = IamDimensionPalette.EmeraldMatrix)
                        }
                        OutlinedButton(
                            onClick = {
                                val geoUri = Uri.parse(
                                    "geo:${item.latitude},${item.longitude}?q=${Uri.encode(item.name)}"
                                )
                                context.startActivity(Intent(Intent.ACTION_VIEW, geoUri))
                            }
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Navigate", color = IamDimensionPalette.PhylaxCyan)
                        }
                    }
                }
            }
        }
    }
}

/**
 * PHASE 5: WOLF'S DEN 3D .GLB & SMOLAGENTS ROUTING (IMG 14, IMG 15, IMG 16 - FIX 4)
 */
@Composable
fun WolfsDenScreen(
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val scope = rememberCoroutineScope()
    val geminiService = remember { GeminiService() }
    var glbUrl by remember { mutableStateOf("models/CesiumMan.glb") }
    var promptText by remember {
        mutableStateOf("Synthesize a Sovereign LROS-1293 directive for SAKUNA Bayanihan response.")
    }
    var responseOutput by remember {
        mutableStateOf("WOLF'S DEN READY • /api/smolagents/route connected to 4-Engine Sovereign Router.")
    }
    var isRouting by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(IamDimensionPalette.VoidBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("wolfs_den_back_button")) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = IamDimensionPalette.NeonMagenta
                    )
                }
                Column {
                    Text(
                        text = "IMG 14-16: WOLF'S DEN DEEPMOTION 3D & SMOLAGENTS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = IamDimensionPalette.NeonMagenta
                    )
                    Text(
                        text = "sendToSmolagents(moduleName, userPrompt) -> wolfAvatarAnimation(responseText)",
                        style = MaterialTheme.typography.labelSmall,
                        color = IamDimensionPalette.CyberGold
                    )
                }
            }
        }

        item {
            OutlawAvatarSceneView(
                glbAssetUrl = glbUrl,
                isTtsSpeaking = isSpeaking,
                isAiDeliberating = isRouting,
                speechAmplitude = if (isSpeaking || isRouting) 0.78f else 0.1f,
                activeSpeechSnippet = responseOutput.take(140),
                onUpdateGlbUrl = { glbUrl = it },
                onTriggerSampleTtsSpeech = { isSpeaking = true },
                onStopTtsSpeech = { isSpeaking = false }
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = IamDimensionPalette.ObsidianCard),
                border = BorderStroke(1.dp, IamDimensionPalette.NeonMagenta),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        label = { Text("Wolf's Den Smolagents Prompt") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wolfs_den_prompt_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (promptText.isNotBlank() && !isRouting) {
                                isRouting = true
                                isSpeaking = true
                                scope.launch {
                                    val reply = geminiService.sendToSmolagents(
                                        moduleName = "WOLF'S DEN 0519",
                                        userPrompt = promptText,
                                        activeLanguage = VaultLanguage.TAGALOG
                                    )
                                    responseOutput = reply.text
                                    isRouting = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IamDimensionPalette.NeonMagenta,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wolfs_den_dispatch_smolagents_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ROUTE VIA /api/smolagents/route", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = responseOutput,
                        style = MaterialTheme.typography.bodySmall,
                        color = IamDimensionPalette.StarlightSilver
                    )
                }
            }
        }
    }
}

/**
 * PHASE 6: THE BLACK CASE MASTER NODE (IMG 17 LOCKED 60% -> IMG 18 UNLOCKED 100% - FIX 1)
 */
@Composable
fun BlackCaseScreen(
    appState: IamDimensionAppState,
    onNavigateToSecuritySeal: () -> Unit,
    onBackToDashboard: () -> Unit
) {
    BackHandler(onBack = onBackToDashboard)
    val scope = rememberCoroutineScope()

    val infiniteTransition = rememberInfiniteTransition(label = "black_case_conduits")
    val conduitPulse by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "conduit_glow"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(IamDimensionPalette.VoidBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackToDashboard, modifier = Modifier.testTag("black_case_back_button")) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = IamDimensionPalette.CyberGold
                    )
                }
                Column {
                    Text(
                        text = if (appState.isBlackCaseUnlocked) {
                            "IMG 18: THE BLACK CASE (100% UNLOCKED)"
                        } else {
                            "IMG 17: THE BLACK CASE (60% LOCKED)"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (appState.isBlackCaseUnlocked) {
                            IamDimensionPalette.EmeraldMatrix
                        } else {
                            IamDimensionPalette.CyberGold
                        }
                    )
                    Text(
                        text = "Catch Basin Capacity: ${appState.catchBasinCapacityPercent}% • Cloud Function: verifyKaggleToken",
                        style = MaterialTheme.typography.labelSmall,
                        color = IamDimensionPalette.PhylaxCyan
                    )
                    Text(
                        text = "DB: ${appState.databaseId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = IamDimensionPalette.MutedSlate
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = IamDimensionPalette.ObsidianCard),
                border = BorderStroke(
                    width = 2.dp,
                    color = when {
                        appState.isBlackCaseUnlocked -> IamDimensionPalette.EmeraldMatrix
                        appState.blackCaseAccessDenied -> IamDimensionPalette.CrimsonAlert
                        else -> IamDimensionPalette.CyberGold
                    }
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Glowing Energy Hoses Conduit Canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                    ) {
                        val hoseColor = when {
                            appState.isBlackCaseUnlocked -> IamDimensionPalette.EmeraldMatrix.copy(alpha = conduitPulse)
                            appState.blackCaseAccessDenied -> IamDimensionPalette.CrimsonAlert.copy(alpha = conduitPulse)
                            else -> IamDimensionPalette.CyberGold.copy(alpha = 0.35f)
                        }
                        val yStep = size.height / 4f
                        for (i in 1..3) {
                            drawLine(
                                color = hoseColor,
                                start = Offset(0f, yStep * i),
                                end = Offset(size.width, yStep * i),
                                strokeWidth = if (appState.isBlackCaseUnlocked) 10f else 5f,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = appState.kaggleKey,
                        onValueChange = {
                            appState.kaggleKey = it
                            appState.blackCaseAccessDenied = false
                        },
                        placeholder = { Text("Enter Kaggle Wake-Up Key") },
                        label = { Text("Enter Kaggle Wake-Up Key") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("black_case_kaggle_key_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            appState.verifyAndUnlockBlackCase()
                            scope.launch {
                                appState.verifyAndUnlockBlackCaseCloudFunction()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IamDimensionPalette.CyberGold,
                            contentColor = IamDimensionPalette.VoidBlack
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("black_case_unlock_submit_button")
                    ) {
                        Icon(
                            if (appState.isBlackCaseUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("VERIFY VIA CLOUD FUNCTION (verifyKaggleToken)", fontWeight = FontWeight.ExtraBold)
                    }

                    if (appState.isBlackCaseUnlocked) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = IamDimensionPalette.EmeraldMatrix
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "OWNER AUTHENTICATED • 100% CATCH BASIN OVERFLOW ACTIVE",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = IamDimensionPalette.EmeraldMatrix,
                                modifier = Modifier.testTag("owner_authenticated_banner")
                            )
                        }
                    } else if (appState.blackCaseAccessDenied) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = IamDimensionPalette.CrimsonAlert
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ACCESS DENIED • INVALID WAKE-UP TOKEN",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = IamDimensionPalette.CrimsonAlert,
                                modifier = Modifier.testTag("access_denied_banner")
                            )
                        }
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onNavigateToSecuritySeal,
                border = BorderStroke(1.dp, IamDimensionPalette.CyberGold),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("black_case_to_security_seal_button")
            ) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = IamDimensionPalette.CyberGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("INSPECT IMG 19: SHA-256 SECURITY SEAL", color = IamDimensionPalette.CyberGold)
            }
        }
    }
}

/**
 * PHASE 6B: SHA-256 SECURITY VERIFICATION SEAL (IMG 19)
 */
@Composable
fun SecurityVerificationScreen(
    appState: IamDimensionAppState,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IamDimensionPalette.VoidBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("security_seal_back_button")) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = IamDimensionPalette.CyberGold
                )
            }
            Text(
                text = "IMG 19: SHA-256 SECURITY VERIFICATION SEAL",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = IamDimensionPalette.CyberGold
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = IamDimensionPalette.ObsidianCard),
            border = BorderStroke(2.dp, IamDimensionPalette.CyberGold),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = "Verified Seal",
                        tint = IamDimensionPalette.EmeraldMatrix,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "LROS VR26.10 • ZERO-ERROR PROTOCOL SEAL",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = IamDimensionPalette.EmeraldMatrix
                        )
                        Text(
                            text = "MASTER TOKEN: X4R9M2K7 • 4X QA VERIFIED",
                            style = MaterialTheme.typography.labelMedium,
                            color = IamDimensionPalette.CyberGold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "SHA-256 DIGEST:\n${appState.sha256SealHash}",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = IamDimensionPalette.PhylaxCyan
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Active Vault: ${appState.activeVaultId}\nActive Language: ${appState.activeLanguage.displayName}\nBlack Case Status: ${if (appState.isBlackCaseUnlocked) "100% UNLOCKED (OWNER AUTHENTICATED)" else "60% LOCKED"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = IamDimensionPalette.StarlightSilver
                )
            }
        }
    }
}
