package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BioscienceAudit
import com.example.ui.UiState
import com.example.ui.theme.BioscienceViolet
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
fun BioscienceLabScreen(
    bioscienceState: UiState<List<BioscienceAudit>>,
    isAiWorking: Boolean,
    onRunBioscienceAudit: (String, String, String, String, Boolean) -> Unit,
    onDeleteAudit: (String) -> Unit,
    onSpeakFindings: (String) -> Unit
) {
    val categories = listOf(
        "Water Quality & Toxicology (RA 9275)",
        "Shellfish & Marine Biotoxins (BFAR Bacoor Bay)",
        "Food & Drug Safety Audit (RA 9711 FDA)",
        "Medico-Legal & DNA Forensics"
    )
    val riskLevels = listOf("LOW", "MODERATE", "HIGH", "CRITICAL")

    var sampleName by remember { mutableStateOf("Zapote River / Bacoor Bay Mussel Zone Sample #01") }
    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var selectedRisk by remember { mutableStateOf("HIGH") }
    var observation by remember {
        mutableStateOf("May amoy kemikal, mataas na turbidity, at posibleng Paralytic Shellfish Poisoning (PSP) o heavy metal runoff malapit sa kabahayan at tahungan.")
    }
    var useSearchGrounding by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("bioscience_lab_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = VaultCardSurface),
                border = BorderStroke(1.5.dp, BioscienceViolet),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = "Bioscience Lab",
                            tint = BioscienceViolet,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "🧬 BIOSCIENCE & FORENSICS LAB",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = "Environmental Toxicology, BFAR Red Tide, FDA Safety & Medico-Legal DNA",
                                style = MaterialTheme.typography.labelSmall,
                                color = BioscienceViolet
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Discipline / Regulatory Framework:",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BioscienceViolet,
                                    selectedLabelColor = TextPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = sampleName,
                        onValueChange = { sampleName = it },
                        label = { Text("Sample / Subject Identifier") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bio_sample_name_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = observation,
                        onValueChange = { observation = it },
                        label = { Text("Field Observation / Lab Parameters / Symptoms") },
                        minLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bio_observation_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bio-Risk Severity:",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            riskLevels.forEach { risk ->
                                FilterChip(
                                    selected = selectedRisk == risk,
                                    onClick = { selectedRisk = risk },
                                    label = { Text(risk) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = when (risk) {
                                            "CRITICAL", "HIGH" -> OutlawCrimson
                                            "MODERATE" -> SovereignGold
                                            else -> TacticalEmerald
                                        },
                                        selectedLabelColor = VaultObsidian
                                    )
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Live Google Search Grounding (BFAR / DENR / FDA):",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Switch(
                            checked = useSearchGrounding,
                            onCheckedChange = { useSearchGrounding = it },
                            modifier = Modifier.testTag("bio_search_grounding_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            onRunBioscienceAudit(
                                sampleName,
                                selectedCategory,
                                observation,
                                selectedRisk,
                                useSearchGrounding
                            )
                        },
                        enabled = !isAiWorking && sampleName.isNotBlank() && observation.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BioscienceViolet,
                            contentColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("run_bioscience_audit_button")
                    ) {
                        if (isAiWorking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = TextPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Biotech, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "EXECUTE BIOSCIENCE FORENSIC AUDIT (gemini-4-argon)",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "SEALED BIOSCIENCE & FORENSIC AUDITS (FIRESTORE)",
                style = MaterialTheme.typography.titleMedium,
                color = SovereignGold
            )
        }

        when (bioscienceState) {
            is UiState.Loading -> {
                item {
                    CircularProgressIndicator(color = BioscienceViolet)
                }
            }
            is UiState.Error -> {
                item {
                    Text(text = bioscienceState.message, color = OutlawCrimson)
                }
            }
            is UiState.Success -> {
                if (bioscienceState.data.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = VaultCardSurface),
                            border = BorderStroke(1.dp, VaultCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Wala pang naka-record na Bioscience Audit. Patakbuhin ang forensic analyzer sa taas para i-seal sa Firestore.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(bioscienceState.data, key = { it.id }) { audit ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = VaultCardSurface),
                            border = BorderStroke(1.dp, BioscienceViolet.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bio_audit_card_${audit.id}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🧬 ${audit.sampleName}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                    Surface(
                                        color = OutlawCrimson.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "${audit.riskLevel} RISK",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = OutlawCrimson,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${audit.bioscienceCategory} • ${audit.regulatoryCitation}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberCyan
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = VaultObsidian,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = audit.findings,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary,
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
                                        text = "🔒 ${audit.sealHash}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SovereignGold
                                    )
                                    Row {
                                        IconButton(onClick = { onSpeakFindings(audit.findings) }) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = "Speak Bioscience Audit",
                                                tint = SovereignGold
                                            )
                                        }
                                        IconButton(onClick = { onDeleteAudit(audit.id) }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Audit",
                                                tint = OutlawCrimson
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
