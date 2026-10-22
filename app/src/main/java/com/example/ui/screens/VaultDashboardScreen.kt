package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.JusticeModule
import com.example.data.model.LoopPhase
import com.example.data.model.VaultDocket
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

@Composable
fun VaultDashboardScreen(
    docketsState: UiState<List<VaultDocket>>,
    selectedModule: JusticeModule,
    fastTriageOutput: String?,
    isAiWorking: Boolean,
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("vault_dashboard_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Sovereign Command Hero Banner
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
                                    text = "POWER PORTAL ZONE 1293 • BACOOR",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SovereignGold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = "v5.0 SEALED",
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
                            text = "Commander Rex Sarmiento • \"Walang Lagay, May Seal.\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CyberCyan
                        )
                        Spacer(modifier = Modifier.height(12.dp))
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
                            text = "MGD_02 LOW-LATENCY TRIAGE (gemini-3.1-flash-lite)",
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

        // 3. The 7 Justice Vault Modules + Bioscience Selector
        item {
            Text(
                text = "THE 7 JUSTICE VAULT MODULES + BIOSCIENCE",
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
                modifier = Modifier.fillMaxWidth()
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
                }
            }
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
