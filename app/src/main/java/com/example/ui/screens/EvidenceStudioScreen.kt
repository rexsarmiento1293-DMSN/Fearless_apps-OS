package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.remote.DriveFileRunResult
import com.example.data.remote.GeneratedImageResult
import com.example.data.remote.GoogleDriveFileRunner
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.TacticalEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultCardBorder
import com.example.ui.theme.VaultCardSurface
import com.example.ui.theme.VaultObsidian

@Composable
fun EvidenceStudioScreen(
    isAiWorking: Boolean,
    driveRunResult: DriveFileRunResult?,
    evidenceScanOutput: String?,
    generatedImageResult: GeneratedImageResult?,
    onRunDriveUrl: (String, String, String) -> Unit,
    onRunLocalDriveUri: (Uri, String) -> Unit,
    onBindGlbTo3DRig: (String) -> Unit,
    onScanEvidencePhoto: (Bitmap, String) -> Unit,
    onGenerateOrEditImage: (String, String, Boolean, Bitmap?) -> Unit,
    onSpeakText: (String) -> Unit
) {
    val context = LocalContext.current
    val aspectRatios = listOf("1:1", "2:3", "3:2", "3:4", "4:3", "9:16", "16:9", "21:9")

    var driveUrl by rememberSaveable {
        mutableStateOf(GoogleDriveFileRunner.DEFAULT_DRIVE_FILE_URL)
    }
    var driveOauthToken by rememberSaveable { mutableStateOf("") }
    var driveDirective by rememberSaveable {
        mutableStateOf("Run and audit Google Drive file 1EqnCS2skwrJCnpEwavhSC_Wk3seb3K3B in Sovereign Vault 1293.")
    }
    var hasAutoTriggeredDriveRun by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasAutoTriggeredDriveRun && driveRunResult == null) {
            hasAutoTriggeredDriveRun = true
            onRunDriveUrl(driveUrl, driveOauthToken, driveDirective)
        }
    }

    var selectedEvidenceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scanPrompt by remember {
        mutableStateOf("I-audit ang dokumentong ito (TCT Land Title, OFW Contract, Barangay Blotter, o OLA Screenshot) at tukuyin ang lahat ng red flags at legal violations.")
    }

    var imagePrompt by remember {
        mutableStateOf("Sovereign Vault 1293 tactical seal emblem with golden scales of justice, obsidian armor plating, and cyber-legal telemetry")
    }
    var selectedAspectRatio by remember { mutableStateOf("16:9") }
    var useStudioQualityPro by remember { mutableStateOf(false) }

    val driveDocumentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onRunLocalDriveUri(uri, driveDirective)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                selectedEvidenceBitmap = bmp
            } catch (_: Exception) {
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("evidence_studio_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 0. Google Drive Sovereign File Runner (OAuth drive.readonly + Android Drive SAF Provider)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = VaultCardSurface),
                border = BorderStroke(1.5.dp, TacticalEmerald),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("google_drive_runner_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Google Drive Runner",
                            tint = TacticalEmerald,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "🚀 GOOGLE DRIVE FILE RUNNER",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = "OAuth2 drive.readonly (${GoogleDriveFileRunner.OAUTH_PROJECT_ID}) • File ID: ${GoogleDriveFileRunner.extractDriveFileId(driveUrl)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TacticalEmerald
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = driveUrl,
                        onValueChange = { driveUrl = it },
                        label = { Text("Google Drive Link or File ID") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("drive_url_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = driveOauthToken,
                        onValueChange = { driveOauthToken = it },
                        label = { Text("Optional OAuth Bearer Token (ya29... for Drive v3 REST API)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("drive_oauth_token_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = driveDirective,
                        onValueChange = { driveDirective = it },
                        label = { Text("Execution / Audit Directive") },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("drive_directive_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onRunDriveUrl(driveUrl, driveOauthToken, driveDirective)
                            },
                            enabled = !isAiWorking && driveUrl.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TacticalEmerald,
                                contentColor = VaultObsidian
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("run_drive_file_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RUN LINK", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                driveDocumentPickerLauncher.launch(arrayOf("*/*"))
                            },
                            enabled = !isAiWorking,
                            border = BorderStroke(1.dp, SovereignGold),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("open_drive_saf_button")
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = SovereignGold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("OPEN DRIVE APP", color = SovereignGold, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (driveRunResult != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = VaultObsidian,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, TacticalEmerald.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("drive_run_output_surface")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "⚡ ${driveRunResult.executionMode}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SovereignGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "File: ${driveRunResult.fileName} • MIME: ${driveRunResult.mimeType} • Channel: ${driveRunResult.sourceChannel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberCyan,
                                    fontFamily = FontFamily.Monospace
                                )

                                if (driveRunResult.previewBitmap != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, VaultCardBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(170.dp)
                                    ) {
                                        Image(
                                            bitmap = driveRunResult.previewBitmap.asImageBitmap(),
                                            contentDescription = "Drive Payload Preview",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = driveRunResult.executionOutput,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    if (!driveRunResult.localGlbPathOrUrl.isNullOrBlank()) {
                                        TextButton(
                                            onClick = { onBindGlbTo3DRig(driveRunResult.localGlbPathOrUrl) },
                                            modifier = Modifier.testTag("bind_drive_glb_button")
                                        ) {
                                            Text("🦴 BIND TO 3D RIG", color = TacticalEmerald, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    TextButton(
                                        onClick = { onSpeakText(driveRunResult.executionOutput) },
                                        modifier = Modifier.testTag("speak_drive_output_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Speak Drive Run Output",
                                            tint = SovereignGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("SPEAK VERDICT", color = SovereignGold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1. Multimodal Evidence & Document Scanner (gemini-4-argon)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = VaultCardSurface),
                border = BorderStroke(1.5.dp, CyberCyan),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = "Evidence Scanner",
                            tint = CyberCyan,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "📸 EVIDENCE & CONTRACT SCANNER",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = "Powered by gemini-4-argon Multimodal Image Understanding",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            border = BorderStroke(1.dp, CyberCyan),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_evidence_photo_button")
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = CyberCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("UPLOAD PHOTO", color = CyberCyan)
                        }

                        OutlinedButton(
                            onClick = {
                                // Generate a synthetic sample document bitmap for instant testing when no gallery image is picked
                                val sampleBmp = Bitmap.createBitmap(640, 400, Bitmap.Config.ARGB_8888)
                                val canvas = android.graphics.Canvas(sampleBmp)
                                canvas.drawColor(0xFF162035.toInt())
                                val paint = android.graphics.Paint().apply {
                                    color = 0xFFF59E0B.toInt()
                                    textSize = 26f
                                    isAntiAlias = true
                                }
                                canvas.drawText("SAMPLE TCT LAND TITLE / OFW CONTRACT", 40f, 80f, paint)
                                paint.color = 0xFFF8FAFC.toInt()
                                paint.textSize = 20f
                                canvas.drawText("Registry of Deeds - Bacoor, Cavite (PD 1529)", 40f, 140f, paint)
                                canvas.drawText("Clause 4: Unverified placement fee deduction", 40f, 200f, paint)
                                selectedEvidenceBitmap = sampleBmp
                            },
                            border = BorderStroke(1.dp, SovereignGold),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("load_sample_evidence_button")
                        ) {
                            Text("SAMPLE DOC", color = SovereignGold)
                        }
                    }

                    if (selectedEvidenceBitmap != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, VaultCardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        ) {
                            Image(
                                bitmap = selectedEvidenceBitmap!!.asImageBitmap(),
                                contentDescription = "Selected Evidence Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = scanPrompt,
                        onValueChange = { scanPrompt = it },
                        label = { Text("Forensic Audit Instructions") },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("evidence_scan_prompt_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val bmp = selectedEvidenceBitmap ?: Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
                            onScanEvidencePhoto(bmp, scanPrompt)
                        },
                        enabled = !isAiWorking,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = VaultObsidian
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("analyze_evidence_button")
                    ) {
                        Text("ANALYZE PHOTO WITH GEMINI-4-ARGON", fontWeight = FontWeight.Bold)
                    }

                    if (!evidenceScanOutput.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = VaultObsidian,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = evidenceScanOutput,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { onSpeakText(evidenceScanOutput) }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Speak Scan Output",
                                            tint = SovereignGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("SPEAK VERDICT", color = SovereignGold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Visual Seal & Evidence Art Generator + Editor (gemini-nano-banana-2.1 & gemini-3-pro-image-preview)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = VaultCardSurface),
                border = BorderStroke(1.5.dp, SovereignGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Visual Studio",
                            tint = SovereignGold,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "🎨 SOVEREIGN IMAGE GENERATOR & EDITOR",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = "gemini-nano-banana-2.1 & gemini-3-pro-image-preview",
                                style = MaterialTheme.typography.labelSmall,
                                color = SovereignGold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Select Aspect Ratio (8 Precision Formats):",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        aspectRatios.forEach { ratio ->
                            FilterChip(
                                selected = selectedAspectRatio == ratio,
                                onClick = { selectedAspectRatio = ratio },
                                label = { Text(ratio) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SovereignGold,
                                    selectedLabelColor = VaultObsidian
                                ),
                                modifier = Modifier.testTag("aspect_ratio_chip_$ratio")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (useStudioQualityPro) "Studio Quality (gemini-3-pro-image-preview)"
                                else "General / Fast Edit (gemini-nano-banana-2.1)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = if (selectedEvidenceBitmap != null) "Editing uploaded reference image + prompt"
                                else "Generating new visual from text prompt",
                                style = MaterialTheme.typography.labelSmall,
                                color = TacticalEmerald
                            )
                        }
                        Switch(
                            checked = useStudioQualityPro,
                            onCheckedChange = { useStudioQualityPro = it },
                            modifier = Modifier.testTag("studio_quality_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = imagePrompt,
                        onValueChange = { imagePrompt = it },
                        label = { Text("Image Creation or Editing Prompt") },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("image_generation_prompt_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            onGenerateOrEditImage(
                                imagePrompt,
                                selectedAspectRatio,
                                useStudioQualityPro,
                                selectedEvidenceBitmap
                            )
                        },
                        enabled = !isAiWorking && imagePrompt.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SovereignGold,
                            contentColor = VaultObsidian
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("generate_image_button")
                    ) {
                        if (isAiWorking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = VaultObsidian,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (selectedEvidenceBitmap != null) "EDIT IMAGE ($selectedAspectRatio)"
                                else "GENERATE IMAGE ($selectedAspectRatio)",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (generatedImageResult?.bitmap != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, SovereignGold),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("generated_image_card")
                        ) {
                            Column {
                                Image(
                                    bitmap = generatedImageResult.bitmap.asImageBitmap(),
                                    contentDescription = generatedImageResult.captionText,
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "${generatedImageResult.modelUsed} • Aspect Ratio ${generatedImageResult.aspectRatio}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SovereignGold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = generatedImageResult.captionText,
                                        style = MaterialTheme.typography.bodyMedium,
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
}
