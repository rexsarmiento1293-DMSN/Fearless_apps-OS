package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.ChatMessage
import com.example.data.model.JusticeModule
import com.example.data.remote.VaultAiModel
import com.example.ui.UiState
import com.example.ui.components.OutlawAvatarSceneView
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.OutlawCrimson
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.TacticalEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultCardBorder
import com.example.ui.theme.VaultCardSurface
import com.example.ui.theme.VaultObsidian
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun VerdictChatScreen(
    chatMessagesState: UiState<List<ChatMessage>>,
    selectedModule: JusticeModule,
    selectedModel: VaultAiModel,
    enableHighThinking: Boolean,
    enableGoogleSearch: Boolean,
    enableGoogleMaps: Boolean,
    executeAll4Engines: Boolean,
    isAiWorking: Boolean,
    liveConnected: Boolean,
    liveTranscriptLog: List<String>,
    deepMotionGlbUrl: String,
    isTtsSpeaking: Boolean,
    speechAmplitude: Float,
    activeSpeechSnippet: String,
    onUpdateGlbUrl: (String) -> Unit,
    onStopTtsSpeech: () -> Unit,
    onSelectModel: (VaultAiModel) -> Unit,
    onToggleHighThinking: (Boolean) -> Unit,
    onToggleGoogleSearch: (Boolean) -> Unit,
    onToggleGoogleMaps: (Boolean) -> Unit,
    onToggleExecuteAll4Engines: (Boolean) -> Unit,
    onSendMessage: (String) -> Unit,
    onDeleteMessage: (String) -> Unit,
    onSpeakMessage: (String) -> Unit,
    onTranscribePcm: (ByteArray, (String) -> Unit) -> Unit,
    onToggleLiveSession: () -> Unit,
    onSendLiveVoiceTurn: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var messageInput by remember { mutableStateOf("") }
    var isRecordingMic by remember { mutableStateOf(false) }
    var recordingJob by remember { mutableStateOf<Job?>(null) }
    val pcmBuffer = remember { ByteArrayOutputStream() }
    var showLivePanel by remember { mutableStateOf(false) }

    val startMicRecording = {
        pcmBuffer.reset()
        isRecordingMic = true
        recordingJob = scope.launch(Dispatchers.IO) {
            val sampleRate = 16000
            val minBuf = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)
            try {
                val recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuf
                )
                val readBuf = ByteArray(1024)
                recorder.startRecording()
                while (isActive && isRecordingMic) {
                    val read = recorder.read(readBuf, 0, readBuf.size)
                    if (read > 0) {
                        pcmBuffer.write(readBuf, 0, read)
                    }
                }
                recorder.stop()
                recorder.release()
            } catch (_: SecurityException) {
            } catch (_: Exception) {
            }
        }
    }

    val stopMicAndTranscribe = {
        isRecordingMic = false
        recordingJob?.cancel()
        val recordedBytes = pcmBuffer.toByteArray()
        if (recordedBytes.isNotEmpty()) {
            onTranscribePcm(recordedBytes) { transcript ->
                messageInput = if (messageInput.isBlank()) transcript else "$messageInput $transcript"
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startMicRecording()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Top Tactical Control Deck: Models, High Thinking, Search/Maps Grounding, 4 MGD Engines, Live API
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = VaultCardSurface),
            border = BorderStroke(1.dp, VaultCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        VaultAiModel.ARGON_HIGH,
                        VaultAiModel.FLASH_GENERAL,
                        VaultAiModel.FLASH_LITE
                    ).forEach { model ->
                        FilterChip(
                            selected = selectedModel == model,
                            onClick = { onSelectModel(model) },
                            label = { Text("${model.badge}: ${model.modelId}") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SovereignGold,
                                selectedLabelColor = VaultObsidian
                            ),
                            modifier = Modifier.testTag("model_chip_${model.name}")
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = enableHighThinking,
                        onClick = { onToggleHighThinking(!enableHighThinking) },
                        leadingIcon = {
                            Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("ThinkingLevel.HIGH") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TacticalEmerald,
                            selectedLabelColor = VaultObsidian
                        ),
                        modifier = Modifier.testTag("toggle_high_thinking_chip")
                    )

                    FilterChip(
                        selected = enableGoogleSearch,
                        onClick = { onToggleGoogleSearch(!enableGoogleSearch) },
                        leadingIcon = {
                            Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Search Grounding") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan,
                            selectedLabelColor = VaultObsidian
                        ),
                        modifier = Modifier.testTag("toggle_search_grounding_chip")
                    )

                    FilterChip(
                        selected = enableGoogleMaps,
                        onClick = { onToggleGoogleMaps(!enableGoogleMaps) },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Maps Grounding") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan,
                            selectedLabelColor = VaultObsidian
                        ),
                        modifier = Modifier.testTag("toggle_maps_grounding_chip")
                    )

                    FilterChip(
                        selected = executeAll4Engines,
                        onClick = { onToggleExecuteAll4Engines(!executeAll4Engines) },
                        label = { Text("⚡ EXECUTE ALL 4 ENGINES") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OutlawCrimson,
                            selectedLabelColor = TextPrimary
                        ),
                        modifier = Modifier.testTag("toggle_4_engines_chip")
                    )

                    FilterChip(
                        selected = showLivePanel || liveConnected,
                        onClick = {
                            showLivePanel = !showLivePanel
                            if (!liveConnected && showLivePanel) {
                                onToggleLiveSession()
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("LIVE API (gemini-3.8-live)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SovereignGold,
                            selectedLabelColor = VaultObsidian
                        ),
                        modifier = Modifier.testTag("toggle_live_voice_chip")
                    )
                }
            }
        }

        // Optional Live API Real-Time Voice Command Panel
        if (showLivePanel) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = VaultObsidian),
                border = BorderStroke(1.5.dp, SovereignGold),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_voice_panel")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (liveConnected) "🟢 GEMINI 3.8 LIVE VOICE CHANNEL ACTIVE" else "⚪ GEMINI 3.8 LIVE STANDBY",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (liveConnected) TacticalEmerald else TextSecondary
                        )
                        TextButton(
                            onClick = onToggleLiveSession,
                            modifier = Modifier.testTag("connect_disconnect_live_button")
                        ) {
                            Text(
                                text = if (liveConnected) "DISCONNECT" else "CONNECT LIVE",
                                color = if (liveConnected) OutlawCrimson else SovereignGold
                            )
                        }
                    }
                    liveTranscriptLog.takeLast(3).forEach { entry ->
                        Text(
                            text = entry,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onSendLiveVoiceTurn("Tol, bigyan mo ako ng mabilis na payo kapag hinarang ang blotter ko sa Barangay.")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SovereignGold,
                                contentColor = VaultObsidian
                            ),
                            modifier = Modifier.testTag("send_live_turn_button")
                        ) {
                            Text("⚡ SEND LIVE VOICE COMMAND", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3D SceneView Avatar Stage above the user chat log interface
        val latestVaultRuling = (chatMessagesState as? UiState.Success)?.data
            ?.lastOrNull { it.sender != "USER" }?.content
            ?: "Prangkahan tayo, tol. Ang ruling sa ${selectedModule.title} (${selectedModule.statute}): Walang lagay, may seal! Gabay lang ito, hindi legal advice. Kumonsulta sa abogado o PAO."

        OutlawAvatarSceneView(
            glbAssetUrl = deepMotionGlbUrl,
            isTtsSpeaking = isTtsSpeaking,
            isAiDeliberating = isAiWorking,
            speechAmplitude = speechAmplitude,
            activeSpeechSnippet = activeSpeechSnippet,
            onUpdateGlbUrl = onUpdateGlbUrl,
            onTriggerSampleTtsSpeech = { onSpeakMessage(latestVaultRuling) },
            onStopTtsSpeech = onStopTtsSpeech
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Message Thread
        Box(modifier = Modifier.weight(1f)) {
            when (chatMessagesState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SovereignGold)
                    }
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = chatMessagesState.message, color = OutlawCrimson)
                    }
                }
                is UiState.Success -> {
                    if (chatMessagesState.data.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = VaultCardSurface),
                            border = BorderStroke(1.dp, VaultCardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "⚖️ TOL — SOVEREIGN VAULT 1293 READY",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = SovereignGold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Prangkahan tayo, Commander. Walang bolahan dito. Pumili ka ng sample case sa baba o i-type/i-record sa mic ang problema mo para sa verdict-grade ruling na may selyo:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { onSendMessage(selectedModule.samplePrompt) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SovereignGold,
                                        contentColor = VaultObsidian
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("send_sample_prompt_button")
                                ) {
                                    Text("Subukan: ${selectedModule.emoji} ${selectedModule.title}", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("verdict_chat_list"),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(chatMessagesState.data, key = { it.id }) { msg ->
                                ChatMessageCard(
                                    message = msg,
                                    onSpeak = { onSpeakMessage(msg.content) },
                                    onDelete = { onDeleteMessage(msg.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Input Bar + Audio Transcription Mic (gemini-3.5-transcribe)
        Surface(
            color = VaultCardSurface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (isRecordingMic) {
                            stopMicAndTranscribe()
                        } else {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                startMicRecording()
                            } else {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    modifier = Modifier.testTag("mic_transcribe_button")
                ) {
                    Icon(
                        imageVector = if (isRecordingMic) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Transcribe Audio (gemini-3.5-transcribe)",
                        tint = if (isRecordingMic) OutlawCrimson else CyberCyan
                    )
                }

                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    placeholder = {
                        Text(
                            if (isRecordingMic) "Recording mic for gemini-3.5-transcribe... Tap Stop"
                            else "I-type ang kaso o sabihin ang 'EXECUTE ALL 4 ENGINES'..."
                        )
                    },
                    maxLines = 4,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_message_input")
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        val textToSend = messageInput
                        messageInput = ""
                        onSendMessage(textToSend)
                    },
                    enabled = !isAiWorking && messageInput.isNotBlank(),
                    modifier = Modifier.testTag("send_chat_button")
                ) {
                    if (isAiWorking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = SovereignGold,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Verdict Query",
                            tint = SovereignGold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageCard(
    message: ChatMessage,
    onSpeak: () -> Unit,
    onDelete: () -> Unit
) {
    val isUser = message.sender == "USER"
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUser) VaultCardBorder.copy(alpha = 0.65f) else VaultCardSurface
        ),
        border = BorderStroke(
            1.dp,
            if (isUser) CyberCyan.copy(alpha = 0.4f) else SovereignGold.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_msg_${message.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isUser) "👤 COMMANDER (${message.moduleTag})" else "⚖️ VAULT 1293 • ${message.moduleTag} [${message.phase}]",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isUser) CyberCyan else SovereignGold
                )
                Text(
                    text = message.planeStatus,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (message.planeStatus == "IMMORTAL") TacticalEmerald else CyberCyan
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )

            if (message.groundingSources.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = VaultObsidian,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "🌐 SEARCH / MAPS GROUNDING SOURCES:",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan
                        )
                        Text(
                            text = message.groundingSources,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${message.modelUsed} • ${message.sealHash}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Row {
                    if (!isUser) {
                        IconButton(
                            onClick = onSpeak,
                            modifier = Modifier.testTag("tts_chat_${message.id}")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "TTS Speak Verdict",
                                tint = SovereignGold
                            )
                        }
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_chat_${message.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Message",
                            tint = OutlawCrimson
                        )
                    }
                }
            }
        }
    }
}
