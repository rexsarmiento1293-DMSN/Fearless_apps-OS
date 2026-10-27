package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.JusticeModule
import com.example.data.model.LoopPhase
import com.example.data.model.VaultDocket
import com.example.data.model.VaultLanguage
import com.example.data.remote.LrosContactRadarService
import com.example.data.remote.RadarScanTelemetry
import com.example.ui.UiState
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.OutlawCrimson
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.TacticalEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultCardBorder
import com.example.ui.theme.VaultCardSurface
import com.example.ui.theme.VaultObsidian
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VaultDashboardScreen(
    docketsState: UiState<List<VaultDocket>>,
    selectedModule: JusticeModule,
    fastTriageOutput: String?,
    isAiWorking: Boolean,
    kaggleKey: String = "",
    isBlackCaseUnlocked: Boolean = false,
    blackCaseAccessDenied: Boolean = false,
    activeLanguage: VaultLanguage = VaultLanguage.TAGALOG,
    selectedRadarType: String = "PAO",
    radarTelemetry: RadarScanTelemetry? = null,
    isRadarScanning: Boolean = false,
    onUpdateKaggleKey: (String) -> Unit = {},
    onSubmitBlackCaseKey: () -> Unit = {},
    onSelectLanguage: (VaultLanguage) -> Unit = {},
    onFindNearestProfessional: (String) -> Unit = {},
    onRouteToWolfsDenSmolagents: (JusticeModule) -> Unit = {},
    onSelectModule: (JusticeModule) -> Unit,
    onLaunchModulePromptInChat: (JusticeModule) -> Unit,
    onCreateDocket: (String, String, JusticeModule) -> Unit,
    onAdvancePhase: (VaultDocket) -> Unit,
    onDeleteDocket: (String) -> Unit,
    onFastTriage: (String) -> Unit,
    onSpeakText: (String) -> Unit
) {
    val context = LocalContext.current
    var showNewDocketDialog by remember { mutableStateOf(false) }
    var triageInput by remember { mutableStateOf("") }
    var languageDropdownExpanded by remember { mutableStateOf(false) }
    var showApkBuildGuide by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onFindNearestProfessional(selectedRadarType)
    }

    LaunchedEffect(Unit) {
        if (radarTelemetry == null) {
            onFindNearestProfessional("PAO")
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("vault_dashboard_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Sovereign Command Hero Banner + [FIX 3: 9-LANGUAGE SELECTOR]
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = VaultCardSurface),
                border = BorderStroke(1.5.dp, SovereignGold.copy(alpha = 0.7f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_command_banner")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(VaultObsidian, VaultCardSurface, VaultObsidian)
                            )
                        )
                        .padding(18.dp)
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            color = SovereignGold.copy(alpha = 0.08f),
                            radius = size.minDimension * 0.6f,
                            center = Offset(size.width * 0.88f, size.height * 0.3f)
                        )
                    }
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = SovereignGold.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, SovereignGold)
                            ) {
                                Text(
                                    text = "LROS VR26.10 • I.A.M DIMENSION V.7",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SovereignGold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = "TOKEN: X4R9M2K7 • V.7 SEALED",
                                style = MaterialTheme.typography.labelSmall,
                                color = TacticalEmerald
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "SOVEREIGN VAULT 1293",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = activeLanguage.localizedBanner,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CyberCyan
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // [FIX 3: THE 9-LANGUAGE SELECTOR] Dropdown & Chip Strip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                OutlinedButton(
                                    onClick = { languageDropdownExpanded = true },
                                    border = BorderStroke(1.dp, CyberCyan),
                                    modifier = Modifier.testTag("language_selector_dropdown_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = "Select Language",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "9-Language Active: ${activeLanguage.displayName} (${activeLanguage.isoCode})",
                                        color = TextPrimary
                                    )
                                }
                                DropdownMenu(
                                    expanded = languageDropdownExpanded,
                                    onDismissRequest = { languageDropdownExpanded = false }
                                ) {
                                    VaultLanguage.entries.forEachIndexed { idx, lang ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "${idx + 1}. ${lang.displayName} (${lang.isoCode})",
                                                    fontWeight = if (lang == activeLanguage) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                languageDropdownExpanded = false
                                                onSelectLanguage(lang)
                                            },
                                            modifier = Modifier.testTag("language_option_${lang.displayName}")
                                        )
                                    }
                                }
                            }

                            TextButton(
                                onClick = { showApkBuildGuide = !showApkBuildGuide },
                                modifier = Modifier.testTag("toggle_v7_apk_guide_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = "V.7 APK Build",
                                    tint = SovereignGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("V.7 APK BUILD", color = SovereignGold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            VaultLanguage.entries.forEach { lang ->
                                FilterChip(
                                    selected = activeLanguage == lang,
                                    onClick = { onSelectLanguage(lang) },
                                    label = { Text(lang.displayName) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyberCyan,
                                        selectedLabelColor = VaultObsidian
                                    ),
                                    modifier = Modifier.testTag("lang_chip_${lang.displayName}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        // 4-Phase Loop Engine Status Strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LoopPhase.entries.forEach { phase ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = VaultObsidian,
                                    border = BorderStroke(1.dp, VaultCardBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = phase.stepNumber,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SovereignGold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = phase.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // [FIX 1: THE BLACK CASE KAGGLE TOKEN LOGIC]
        item {
            BlackCaseKaggleVaultCard(
                kaggleKey = kaggleKey,
                isBlackCaseUnlocked = isBlackCaseUnlocked,
                blackCaseAccessDenied = blackCaseAccessDenied,
                onUpdateKaggleKey = onUpdateKaggleKey,
                onSubmitKey = onSubmitBlackCaseKey
            )
        }

        // [FIX 6: ANDROID APK COMPILATION (V.7) CAPACITOR PIPELINE]
        if (showApkBuildGuide) {
            item {
                CapacitorV7ApkCompilationCard()
            }
        }

        // 2. Low-Latency Fast Triage Bar (gemini-3.1-flash-lite)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = VaultCardSurface),
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Fast Triage",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MGD_02 LOW-LATENCY TRIAGE (gemini-3.1-flash-lite • ${activeLanguage.displayName})",
                            style = MaterialTheme.typography.labelLarge,
                            color = CyberCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = triageInput,
                            onValueChange = { triageInput = it },
                            placeholder = { Text("Tanong agad (hal. Ayaw magbigay ng CFA sa Barangay)...") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("fast_triage_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onFastTriage(triageInput) },
                            enabled = !isAiWorking && triageInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = VaultObsidian
                            ),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("fast_triage_button")
                        ) {
                            Text("TRIAGE", fontWeight = FontWeight.Bold)
                        }
                    }
                    if (!fastTriageOutput.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = VaultObsidian,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = fastTriageOutput,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = { onSpeakText(fastTriageOutput) },
                                        modifier = Modifier.testTag("speak_triage_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Speak Triage",
                                            tint = SovereignGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("VOICE SEAL (TTS)", color = SovereignGold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. The 7 Justice Vault Modules + Bioscience Selector (with [FIX 2: 4. SAKUNA])
        item {
            Text(
                text = "THE 7 JUSTICE VAULT MODULES + BIOSCIENCE (LROS V.7)",
                style = MaterialTheme.typography.titleMedium,
                color = SovereignGold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                JusticeModule.entries.forEach { mod ->
                    val isSelected = mod == selectedModule
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectModule(mod) },
                        label = { Text("${mod.emoji} ${mod.code}: ${mod.title}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SovereignGold,
                            selectedLabelColor = VaultObsidian
                        ),
                        modifier = Modifier.testTag("module_chip_${mod.tag}")
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Selected Module Tactical Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = VaultCardSurface),
                border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("selected_module_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedModule.emoji} ${selectedModule.title}",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Surface(
                            color = SovereignGold.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = selectedModule.statute,
                                style = MaterialTheme.typography.labelSmall,
                                color = SovereignGold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // [FIX 2: THE SAKUNA MODULE (LROS) Multi-Hazard Icon & Emergency Hotlines Strip]
                    if (selectedModule == JusticeModule.MODULE_04) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = VaultObsidian,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, CyberCyan)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Thunderstorm, contentDescription = "Typhoon", tint = CyberCyan, modifier = Modifier.size(16.dp))
                                    Icon(Icons.Default.Public, contentDescription = "Earthquake", tint = SovereignGold, modifier = Modifier.size(16.dp))
                                    Icon(Icons.Default.LocalFireDepartment, contentDescription = "Fire", tint = OutlawCrimson, modifier = Modifier.size(16.dp))
                                    Icon(Icons.Default.Water, contentDescription = "Flood", tint = TacticalEmerald, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "MULTI-HAZARD: Bagyo • Lindol • Sunog • Baha",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextPrimary
                                    )
                                }
                            }
                            Surface(
                                color = OutlawCrimson.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, OutlawCrimson)
                            ) {
                                Text(
                                    text = "☎️ NDRRMC: 911 / (02) 8911-5061 • PAGASA: (02) 8284-0800 • BFP: (02) 8426-0219",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = selectedModule.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onLaunchModulePromptInChat(selectedModule) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SovereignGold,
                                contentColor = VaultObsidian
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("execute_module_verdict_button")
                        ) {
                            Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("DELIBERATE IN MGD CHAT", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { showNewDocketDialog = true },
                            border = BorderStroke(1.dp, SovereignGold),
                            modifier = Modifier.testTag("open_new_docket_dialog_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "New Docket", tint = SovereignGold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("BLOTTER", color = SovereignGold)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { onRouteToWolfsDenSmolagents(selectedModule) },
                        border = BorderStroke(1.dp, CyberCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("route_wolfs_den_smolagents_button")
                    ) {
                        Text(
                            text = "🐺 WOLF'S DEN SMOLAGENTS ROUTE (${activeLanguage.displayName} + Lip-Sync)",
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // [FIX 5: RXM/LROS CONTACT RADAR (GPS INTEGRATION)]
        item {
            RxmLrosContactRadarCard(
                selectedRadarType = selectedRadarType,
                radarTelemetry = radarTelemetry,
                isRadarScanning = isRadarScanning,
                onSelectRadarType = { targetType ->
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    if (hasPermission) {
                        onFindNearestProfessional(targetType)
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }
            )
        }

        // 4. Active 4-Phase Loop Engine Dockets (Firestore Synced)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "4-PHASE LOOP DOCKETS (FIRESTORE VAULT)",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                if (isAiWorking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = SovereignGold,
                        strokeWidth = 2.dp
                    )
                }
            }
        }

        when (docketsState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = SovereignGold)
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = OutlawCrimson.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, OutlawCrimson),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = docketsState.message,
                            color = OutlawCrimson,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
            is UiState.Success -> {
                if (docketsState.data.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = VaultCardSurface),
                            border = BorderStroke(1.dp, VaultCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Walang nakabukas na Docket sa Vault, Commander.",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Pindutin ang '+ BLOTTER' para mag-log ng kaso sa 4-Phase Loop Engine (GENESIS -> EXECUTION -> AUDIT_GATE -> IMMORTALIZED).",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(docketsState.data, key = { it.id }) { docket ->
                        DocketItemCard(
                            docket = docket,
                            onAdvancePhase = { onAdvancePhase(docket) },
                            onDelete = { onDeleteDocket(docket.id) },
                            onSpeak = { onSpeakText(docket.rulingText) }
                        )
                    }
                }
            }
        }
    }

    if (showNewDocketDialog) {
        NewDocketDialog(
            initialModule = selectedModule,
            onDismiss = { showNewDocketDialog = false },
            onConfirm = { title, summary, mod ->
                showNewDocketDialog = false
                onCreateDocket(title, summary, mod)
            }
        )
    }
}

@Composable
private fun BlackCaseKaggleVaultCard(
    kaggleKey: String,
    isBlackCaseUnlocked: Boolean,
    blackCaseAccessDenied: Boolean,
    onUpdateKaggleKey: (String) -> Unit,
    onSubmitKey: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "black_case_hoses")
    val hosePulse by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hose_pulse"
    )
    val errorFlashAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 380, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "error_flash"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isBlackCaseUnlocked -> TacticalEmerald
            blackCaseAccessDenied -> OutlawCrimson.copy(alpha = errorFlashAlpha)
            else -> SovereignGold.copy(alpha = 0.6f)
        },
        label = "black_case_border"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VaultObsidian),
        border = BorderStroke(1.5.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("black_case_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isBlackCaseUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = "Black Case Lock",
                        tint = if (isBlackCaseUnlocked) TacticalEmerald else SovereignGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "THE BLACK CASE • KAGGLE WAKE-UP TERMINAL",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isBlackCaseUnlocked) TacticalEmerald else SovereignGold,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (isBlackCaseUnlocked) {
                    Surface(
                        color = TacticalEmerald.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, TacticalEmerald),
                        modifier = Modifier.testTag("black_case_owner_authenticated")
                    ) {
                        Text(
                            text = "OWNER AUTHENTICATED",
                            style = MaterialTheme.typography.labelSmall,
                            color = TacticalEmerald,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else if (blackCaseAccessDenied) {
                    Surface(
                        color = OutlawCrimson.copy(alpha = errorFlashAlpha * 0.3f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, OutlawCrimson.copy(alpha = errorFlashAlpha)),
                        modifier = Modifier.testTag("black_case_access_denied")
                    ) {
                        Text(
                            text = "ACCESS DENIED",
                            style = MaterialTheme.typography.labelSmall,
                            color = OutlawCrimson,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Glowing Hoses Conduit Canvas (Animates when unlocked)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                val width = size.width
                val height = size.height
                val midY = height / 2f
                val activeColor = if (isBlackCaseUnlocked) {
                    TacticalEmerald.copy(alpha = hosePulse)
                } else if (blackCaseAccessDenied) {
                    OutlawCrimson.copy(alpha = errorFlashAlpha * 0.7f)
                } else {
                    VaultCardBorder
                }
                val secondaryGlow = if (isBlackCaseUnlocked) {
                    CyberCyan.copy(alpha = hosePulse * 0.85f)
                } else {
                    SovereignGold.copy(alpha = 0.25f)
                }

                // Draw 3 braided energy hoses feeding the central Black Case core
                for (offsetIdx in listOf(-14f, 0f, 14f)) {
                    val path = Path().apply {
                        moveTo(0f, midY + offsetIdx)
                        cubicTo(
                            width * 0.28f,
                            midY - offsetIdx * 1.4f,
                            width * 0.72f,
                            midY + offsetIdx * 1.4f,
                            width,
                            midY + offsetIdx
                        )
                    }
                    drawPath(
                        path = path,
                        color = if (offsetIdx == 0f) secondaryGlow else activeColor,
                        style = Stroke(
                            width = if (isBlackCaseUnlocked) 6f else 3f,
                            cap = StrokeCap.Round
                        )
                    )
                }
                drawCircle(
                    color = if (isBlackCaseUnlocked) TacticalEmerald else SovereignGold,
                    radius = if (isBlackCaseUnlocked) 12f * hosePulse.coerceAtLeast(0.6f) else 7f,
                    center = Offset(width / 2f, midY)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = kaggleKey,
                    onValueChange = onUpdateKaggleKey,
                    placeholder = { Text("Enter Kaggle Wake-Up Key") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("black_case_kaggle_key_input")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onSubmitKey,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBlackCaseUnlocked) TacticalEmerald else SovereignGold,
                        contentColor = VaultObsidian
                    ),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("black_case_submit_button")
                ) {
                    Text(
                        text = if (isBlackCaseUnlocked) "UNLOCKED" else "UNLOCK",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun RxmLrosContactRadarCard(
    selectedRadarType: String,
    radarTelemetry: RadarScanTelemetry?,
    isRadarScanning: Boolean,
    onSelectRadarType: (String) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_sweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_angle"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VaultCardSurface),
        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("rxm_lros_contact_radar_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "GPS Radar",
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "RXM/LROS CONTACT RADAR (GPS + GOOGLE PLACES)",
                            style = MaterialTheme.typography.labelLarge,
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold
                        )
                        if (radarTelemetry != null) {
                            Text(
                                text = "GPS: ${"%.4f".format(radarTelemetry.userLatitude)}, ${"%.4f".format(radarTelemetry.userLongitude)} • ${radarTelemetry.gpsProviderUsed}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
                if (isRadarScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = CyberCyan,
                        strokeWidth = 2.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Radar Category Selector Buttons: "PAO", "Hospital", "Vet"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple("PAO", "⚖️ PAO Legal", Icons.Default.Gavel),
                    Triple("Hospital", "🏥 Hospital", Icons.Default.LocalHospital),
                    Triple("Vet", "🐾 Vet / K9", Icons.Default.Pets)
                ).forEach { (typeKey, label, icon) ->
                    val isSelected = selectedRadarType.equals(typeKey, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectRadarType(typeKey) },
                        leadingIcon = {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan,
                            selectedLabelColor = VaultObsidian
                        ),
                        modifier = Modifier.testTag("radar_btn_$typeKey")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Geodesic Radar Canvas with Live Blips
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(VaultObsidian, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                val places = radarTelemetry?.places.orEmpty()
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = (size.minDimension / 2f) * 0.86f

                    listOf(0.35f, 0.68f, 1.0f).forEach { ratio ->
                        drawCircle(
                            color = CyberCyan.copy(alpha = 0.25f),
                            radius = maxRadius * ratio,
                            center = center,
                            style = Stroke(width = 1.5f)
                        )
                    }

                    val rad = Math.toRadians(sweepAngle.toDouble())
                    val sweepEnd = Offset(
                        x = center.x + (maxRadius * cos(rad)).toFloat(),
                        y = center.y + (maxRadius * sin(rad)).toFloat()
                    )
                    drawLine(
                        color = TacticalEmerald,
                        start = center,
                        end = sweepEnd,
                        strokeWidth = 3f
                    )

                    // Plot nearest places as radar blips
                    places.forEachIndexed { idx, place ->
                        val angleRad = Math.toRadians((idx * 85.0 + 35.0) % 360.0)
                        val distRatio = (place.distanceKm.toFloat() / 15f).coerceIn(0.22f, 0.88f)
                        val blipCenter = Offset(
                            x = center.x + (maxRadius * distRatio * cos(angleRad)).toFloat(),
                            y = center.y + (maxRadius * distRatio * sin(angleRad)).toFloat()
                        )
                        drawCircle(
                            color = SovereignGold,
                            radius = 6f,
                            center = blipCenter
                        )
                    }

                    drawCircle(
                        color = CyberCyan,
                        radius = 5f,
                        center = center
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Nearest Professionals Results List Below Radar
            val places = radarTelemetry?.places.orEmpty()
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("radar_results_list")
            ) {
                places.forEach { place ->
                    Surface(
                        color = VaultObsidian,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, VaultCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("radar_place_item_${place.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = place.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = place.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "☎️ ${place.phoneOrHotline} • ⭐ ${place.rating}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SovereignGold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = TacticalEmerald.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, TacticalEmerald)
                            ) {
                                Text(
                                    text = "${place.distanceKm} km",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TacticalEmerald,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CapacitorV7ApkCompilationCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VaultCardSurface),
        border = BorderStroke(1.dp, SovereignGold),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("capacitor_v7_apk_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "📦 [FIX 6] V.7 CAPACITOR & ANDROID STUDIO APK COMPILATION",
                style = MaterialTheme.typography.titleSmall,
                color = SovereignGold,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = VaultObsidian,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, VaultCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    LrosContactRadarService.CAPACITOR_APK_COMMANDS.forEach { cmd ->
                        Text(
                            text = "$ $cmd",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = TacticalEmerald
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LrosContactRadarService.CAPACITOR_ANDROID_STUDIO_STEPS.forEach { step ->
                Text(
                    text = step,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun DocketItemCard(
    docket: VaultDocket,
    onAdvancePhase: () -> Unit,
    onDelete: () -> Unit,
    onSpeak: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val module = JusticeModule.fromTag(docket.moduleTag)
    val isImmortal = docket.planeStatus == "IMMORTAL" || docket.phase == "IMMORTALIZED"

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = VaultCardSurface),
        border = BorderStroke(
            1.dp,
            if (isImmortal) TacticalEmerald.copy(alpha = 0.6f) else SovereignGold.copy(alpha = 0.45f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("docket_card_${docket.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = VaultObsidian,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, VaultCardBorder)
                ) {
                    Text(
                        text = "${module.emoji} ${docket.moduleTag} • PHASE: ${docket.phase}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SovereignGold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    color = if (isImmortal) TacticalEmerald.copy(alpha = 0.18f) else CyberCyan.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = docket.planeStatus,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isImmortal) TacticalEmerald else CyberCyan,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = docket.title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = docket.caseSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = if (expanded) 10 else 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = VaultObsidian,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = docket.rulingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    maxLines = if (expanded) 50 else 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔒 ${docket.sealHash}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SovereignGold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSpeak,
                        modifier = Modifier.testTag("speak_docket_${docket.id}")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Speak Ruling",
                            tint = CyberCyan
                        )
                    }
                    if (docket.phase != LoopPhase.IMMORTALIZED.label) {
                        TextButton(
                            onClick = onAdvancePhase,
                            modifier = Modifier.testTag("advance_docket_${docket.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "Advance Phase",
                                tint = TacticalEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("NEXT PHASE", color = TacticalEmerald)
                        }
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_docket_${docket.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Docket",
                            tint = OutlawCrimson
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NewDocketDialog(
    initialModule: JusticeModule,
    onDismiss: () -> Unit,
    onConfirm: (String, String, JusticeModule) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var chosenModule by remember { mutableStateOf(initialModule) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultCardSurface,
        title = {
            Text(
                text = "NEW VAULT 1293 BLOTTER (PHASE 01: GENESIS)",
                style = MaterialTheme.typography.titleLarge,
                color = SovereignGold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Piliin ang Justice Vault Module at ilagay ang buong detalye para sa instant MGD Verdict Seal:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    JusticeModule.entries.forEach { mod ->
                        FilterChip(
                            selected = chosenModule == mod,
                            onClick = { chosenModule = mod },
                            label = { Text("${mod.emoji} ${mod.code}") }
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Pamagat ng Kaso / Reklamo") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_docket_title_input")
                )
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Buong Kwento / Raw Facts (Sino, Saan, Kailan)") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_docket_summary_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, summary, chosenModule) },
                enabled = title.isNotBlank() && summary.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SovereignGold,
                    contentColor = VaultObsidian
                ),
                modifier = Modifier.testTag("confirm_create_docket_button")
            ) {
                Text("SEAL DOCKET", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary)
            }
        }
    )
}
