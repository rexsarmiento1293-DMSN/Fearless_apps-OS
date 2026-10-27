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
    onSendLiveVoiceTurn: (String) -> Unit,
    activeLanguage: com.example.data.model.VaultLanguage = com.example.data.model.VaultLanguage.TAGALOG,
    onSendToSmolagents: (String, String) -> Unit = { _, _ -> }
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
                        VaultAiModel.FLASH_LITE,
                        VaultAiModel.RECURSIVE_FLINT,
                        VaultAiModel.SWITCH_BASE_8,
                        VaultAiModel.LLAMA3_158BIT,
                        VaultAiModel.LEROBOT_VLA,
                        VaultAiModel.SMOLAGENTS_CODE
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

        // Interactive SmolAgents Guided Tour, "Building Good Agents" & OpenTelemetry Configurator Deck when SMOLAGENTS_CODE is active
        if (selectedModel == VaultAiModel.SMOLAGENTS_CODE) {
            val localGeminiService = remember { com.example.data.remote.GeminiService() }
            var useUvInstaller by remember { mutableStateOf(true) }
            var selectedAgentType by remember { mutableStateOf("CodeAgent") }
            var selectedModelBackend by remember { mutableStateOf("Qwen2.5-72B-Strong") }
            var selectedExecutor by remember { mutableStateOf("blaxel") }
            var selectedTelemetryBackend by remember { mutableStateOf("phoenix") }
            var useMultiAgent by remember { mutableStateOf(false) }
            var enablePlanningInterval by remember { mutableStateOf(true) }
            var includeAdditionalArgs by remember { mutableStateOf(true) }
            var showGoldenRules by remember { mutableStateOf(false) }
            var showOtelGuide by remember { mutableStateOf(false) }
            var showAdvancedToolsHub by remember { mutableStateOf(true) }
            var showSecureSandboxHub by remember { mutableStateOf(true) }
            var showMemoryHub by remember { mutableStateOf(true) }
            var showWhatAreAgentsHub by remember { mutableStateOf(true) }
            var showTextToSqlHub by remember { mutableStateOf(true) }
            var selectedTextToSqlLevel by remember {
                mutableStateOf("Level 1: Single Table (receipts • Llama-3.1-8B-Instruct)")
            }
            var customSqlInput by remember { mutableStateOf("") }
            var showAgenticRagHub by remember { mutableStateOf(true) }
            var selectedAgenticRagStep by remember {
                mutableStateOf("Step 1 & 2: Install + Prepare KB (`m-ric/huggingface_doc` • `RecursiveCharacterTextSplitter`)")
            }
            var selectedAgenticRagModel by remember {
                mutableStateOf(com.example.data.remote.GeminiService.SmolAgentsConfig.AGENTIC_RAG_DEFAULT_MODEL)
            }
            var customRagQueryInput by remember {
                mutableStateOf(com.example.data.remote.GeminiService.SmolAgentsConfig.AGENTIC_RAG_DEFAULT_QUESTION)
            }
            var selectedAgencyLevel by remember {
                mutableStateOf("★★☆ Multi-step Agent")
            }
            var selectedSurfingScenario by remember {
                mutableStateOf("Scenario 2: Tuesday Surf + Delayed Passport + Insurance (✅ Multi-Step Agent)")
            }
            var selectedMultiStepVariant by remember {
                mutableStateOf("CodeAgent (Python Code Snippets)")
            }
            var enableReactPlanningStep by remember { mutableStateOf(true) }
            var enableReactStepCallbacks by remember { mutableStateOf(true) }
            var selectedMemoryMode by remember {
                mutableStateOf("1. Replay Memory (agent.replay())")
            }
            var pruneScreenshotCallback by remember { mutableStateOf(true) }
            var selectedSandboxArchMode by remember {
                mutableStateOf("Approach 1: Snippet Sandbox (executor_type)")
            }
            var selectedAstSafeguardKey by remember {
                mutableStateOf("Disallowed Import (import os)")
            }
            var selectedFibTarget by remember { mutableStateOf(100) }
            var selectedToolSource by remember { mutableStateOf("HFModelDownloadsTool(Tool)") }
            var selectedHfTask by remember { mutableStateOf("text-to-video") }
            var mcpStructuredOutput by remember { mutableStateOf(true) }
            var liveToolExecutionLog by remember { mutableStateOf<String?>(null) }
            var isRunningLiveHfTool by remember { mutableStateOf(false) }
            var selectedExtras by remember {
                mutableStateOf(setOf("toolkit", "mcp", "litellm", "blaxel", "e2b", "modal", "docker", "telemetry", "gradio"))
            }
            val currentInstallCmd = remember(useUvInstaller, selectedExtras, selectedTelemetryBackend) {
                com.example.data.remote.GeminiService.SmolAgentsConfig.buildInstallCommand(
                    useUv = useUvInstaller,
                    extras = selectedExtras.toList(),
                    telemetryBackend = selectedTelemetryBackend
                )
            }
            val currentCliCmd = remember(selectedModule, selectedModelBackend) {
                com.example.data.remote.GeminiService.SmolAgentsConfig.buildCliCommand(
                    prompt = "Audit ${selectedModule.code} (${selectedModule.statute})",
                    modelType = selectedModelBackend
                )
            }
            val currentSnippet = remember(
                selectedAgentType,
                selectedModelBackend,
                selectedExecutor,
                useMultiAgent,
                enablePlanningInterval,
                includeAdditionalArgs,
                selectedTelemetryBackend
            ) {
                com.example.data.remote.GeminiService.SmolAgentsConfig.buildGuidedTourSnippet(
                    agentType = selectedAgentType,
                    modelBackend = selectedModelBackend,
                    executorType = selectedExecutor,
                    multiAgent = useMultiAgent,
                    enablePlanningInterval = enablePlanningInterval,
                    includeAdditionalArgs = includeAdditionalArgs,
                    telemetryBackend = selectedTelemetryBackend
                )
            }
            val currentAdvancedToolSnippet = remember(selectedToolSource, selectedHfTask, mcpStructuredOutput) {
                com.example.data.remote.GeminiService.SmolAgentsConfig.buildAdvancedToolSnippet(
                    toolSource = selectedToolSource,
                    taskCategory = selectedHfTask,
                    mcpStructuredOutput = mcpStructuredOutput
                )
            }
            val currentSecureSandboxSnippet = remember(selectedSandboxArchMode, selectedExecutor, selectedFibTarget) {
                com.example.data.remote.GeminiService.SmolAgentsConfig.buildSecureSandboxSnippet(
                    architectureMode = selectedSandboxArchMode,
                    executorType = selectedExecutor,
                    fibN = selectedFibTarget
                )
            }
            val currentMemorySnippet = remember(selectedMemoryMode, pruneScreenshotCallback, selectedFibTarget) {
                com.example.data.remote.GeminiService.SmolAgentsConfig.buildMemoryManagementSnippet(
                    memoryMode = selectedMemoryMode,
                    maxSteps = 10,
                    pruneScreenshotWindow = if (pruneScreenshotCallback) 2 else 99,
                    taskPrompt = "What is the ${selectedFibTarget}th Fibonacci number?"
                )
            }
            val currentAgencySpectrumSnippet = remember(selectedAgencyLevel, selectedSurfingScenario) {
                com.example.data.remote.GeminiService.SmolAgentsConfig.buildAgencySpectrumSnippet(
                    agencyLevel = selectedAgencyLevel,
                    surfingScenario = selectedSurfingScenario
                )
            }
            val currentMultiStepReactSnippet = remember(selectedMultiStepVariant, enableReactPlanningStep, enableReactStepCallbacks) {
                com.example.data.remote.GeminiService.SmolAgentsConfig.buildMultiStepReactCycleSnippet(
                    agentVariant = selectedMultiStepVariant,
                    enablePlanningStep = enableReactPlanningStep,
                    includeStepCallbacks = enableReactStepCallbacks
                )
            }
            val currentTextToSqlSnippet = remember(selectedTextToSqlLevel) {
                com.example.data.remote.GeminiService.SmolAgentsConfig.buildTextToSqlSnippet(
                    levelMode = selectedTextToSqlLevel
                )
            }
            val currentAgenticRagSnippet = remember(selectedAgenticRagStep, selectedAgenticRagModel) {
                com.example.data.remote.GeminiService.SmolAgentsConfig.buildAgenticRagSnippet(
                    stepMode = selectedAgenticRagStep,
                    modelId = selectedAgenticRagModel
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = VaultObsidian),
                border = BorderStroke(1.2.dp, CyberCyan.copy(alpha = 0.75f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("smolagents_config_panel")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🤖 SMOLAGENTS • MEMORY, SANDBOX, TOOLS, MCP & OTEL",
                            style = MaterialTheme.typography.labelLarge,
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = selectedAgentType == "CodeAgent",
                                onClick = {
                                    selectedAgentType = if (selectedAgentType == "CodeAgent") {
                                        "ToolCallingAgent"
                                    } else {
                                        "CodeAgent"
                                    }
                                },
                                label = { Text(selectedAgentType) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TacticalEmerald.copy(alpha = 0.22f),
                                    selectedLabelColor = TacticalEmerald
                                ),
                                modifier = Modifier.testTag("smolagents_agent_paradigm_toggle")
                            )
                            FilterChip(
                                selected = useUvInstaller,
                                onClick = { useUvInstaller = !useUvInstaller },
                                label = {
                                    Text(if (useUvInstaller) "uv pip" else "pip")
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                                    selectedLabelColor = CyberCyan
                                ),
                                modifier = Modifier.testTag("smolagents_pkg_manager_toggle")
                            )
                        }
                    }

                    // Model Backend, OpenTelemetry Backend & Sandbox Executor + Good Agents Controls Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = showAgenticRagHub,
                            onClick = { showAgenticRagHub = !showAgenticRagHub },
                            label = { Text("📚 Agentic RAG (BM25)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TacticalEmerald.copy(alpha = 0.25f),
                                selectedLabelColor = TacticalEmerald
                            ),
                            modifier = Modifier.testTag("smolagents_agentic_rag_toggle")
                        )
                        FilterChip(
                            selected = showTextToSqlHub,
                            onClick = { showTextToSqlHub = !showTextToSqlHub },
                            label = { Text("🗄️ Text-to-SQL Agent") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SovereignGold.copy(alpha = 0.25f),
                                selectedLabelColor = SovereignGold
                            ),
                            modifier = Modifier.testTag("smolagents_text_to_sql_toggle")
                        )
                        FilterChip(
                            selected = showWhatAreAgentsHub,
                            onClick = { showWhatAreAgentsHub = !showWhatAreAgentsHub },
                            label = { Text("🤔 Agents & MultiStep ReAct") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyan.copy(alpha = 0.25f),
                                selectedLabelColor = CyberCyan
                            ),
                            modifier = Modifier.testTag("smolagents_what_are_agents_toggle")
                        )
                        FilterChip(
                            selected = showMemoryHub,
                            onClick = { showMemoryHub = !showMemoryHub },
                            label = { Text("📚 Memory & Step Loop") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SovereignGold.copy(alpha = 0.25f),
                                selectedLabelColor = SovereignGold
                            ),
                            modifier = Modifier.testTag("smolagents_memory_hub_toggle")
                        )
                        FilterChip(
                            selected = showSecureSandboxHub,
                            onClick = { showSecureSandboxHub = !showSecureSandboxHub },
                            label = { Text("🔒 Secure Sandbox & AST") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OutlawCrimson.copy(alpha = 0.25f),
                                selectedLabelColor = OutlawCrimson
                            ),
                            modifier = Modifier.testTag("smolagents_secure_sandbox_toggle")
                        )
                        FilterChip(
                            selected = showAdvancedToolsHub,
                            onClick = { showAdvancedToolsHub = !showAdvancedToolsHub },
                            label = { Text("🛠️ Tools & MCP Hub") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TacticalEmerald.copy(alpha = 0.25f),
                                selectedLabelColor = TacticalEmerald
                            ),
                            modifier = Modifier.testTag("smolagents_tools_hub_toggle")
                        )
                        com.example.data.remote.GeminiService.SmolAgentsConfig.TELEMETRY_BACKENDS.forEach { otelKey ->
                            val isSelected = selectedTelemetryBackend == otelKey
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTelemetryBackend = otelKey },
                                label = { Text("otel:$otelKey") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SovereignGold.copy(alpha = 0.25f),
                                    selectedLabelColor = SovereignGold
                                ),
                                modifier = Modifier.testTag("smolagents_otel_$otelKey")
                            )
                        }
                        com.example.data.remote.GeminiService.SmolAgentsConfig.MODEL_BACKEND_KEYS.forEach { backendKey ->
                            val isSelected = selectedModelBackend == backendKey
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedModelBackend = backendKey },
                                label = { Text(backendKey) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                                    selectedLabelColor = CyberCyan
                                ),
                                modifier = Modifier.testTag("smolagents_backend_$backendKey")
                            )
                        }
                        com.example.data.remote.GeminiService.SmolAgentsConfig.EXECUTOR_SANDBOXES.forEach { sandbox ->
                            val isSelected = selectedExecutor == sandbox
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedExecutor = sandbox },
                                label = { Text("exec:$sandbox") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OutlawCrimson.copy(alpha = 0.22f),
                                    selectedLabelColor = OutlawCrimson
                                ),
                                modifier = Modifier.testTag("smolagents_sandbox_$sandbox")
                            )
                        }
                        FilterChip(
                            selected = enablePlanningInterval,
                            onClick = { enablePlanningInterval = !enablePlanningInterval },
                            label = { Text("planning_interval=3") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SovereignGold.copy(alpha = 0.25f),
                                selectedLabelColor = SovereignGold
                            ),
                            modifier = Modifier.testTag("smolagents_planning_interval_toggle")
                        )
                        FilterChip(
                            selected = includeAdditionalArgs,
                            onClick = { includeAdditionalArgs = !includeAdditionalArgs },
                            label = { Text("additional_args") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyan.copy(alpha = 0.25f),
                                selectedLabelColor = CyberCyan
                            ),
                            modifier = Modifier.testTag("smolagents_additional_args_toggle")
                        )
                        FilterChip(
                            selected = useMultiAgent,
                            onClick = { useMultiAgent = !useMultiAgent },
                            label = { Text("managed_agents") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TacticalEmerald.copy(alpha = 0.25f),
                                selectedLabelColor = TacticalEmerald
                            ),
                            modifier = Modifier.testTag("smolagents_multiagent_toggle")
                        )
                        FilterChip(
                            selected = showOtelGuide,
                            onClick = { showOtelGuide = !showOtelGuide },
                            label = { Text("OTel Guide") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SovereignGold.copy(alpha = 0.25f),
                                selectedLabelColor = SovereignGold
                            ),
                            modifier = Modifier.testTag("smolagents_otel_guide_toggle")
                        )
                        FilterChip(
                            selected = showGoldenRules,
                            onClick = { showGoldenRules = !showGoldenRules },
                            label = { Text("11 System Rules") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OutlawCrimson.copy(alpha = 0.25f),
                                selectedLabelColor = OutlawCrimson
                            ),
                            modifier = Modifier.testTag("smolagents_golden_rules_toggle")
                        )
                    }

                    // Agentic RAG Hub (`# Agentic RAG` — `m-ric/huggingface_doc` + `RecursiveCharacterTextSplitter` + `BM25Retriever(k=10)` + HyDE 2212.10496)
                    if (showAgenticRagHub) {
                        Surface(
                            color = VaultCardSurface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, TacticalEmerald.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("smolagents_agentic_rag_hub")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "📚 AGENTIC RAG (m-ric/huggingface_doc • RecursiveCharacterTextSplitter(500, 50) • BM25Retriever(k=10) • HyDE 2212.10496)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TacticalEmerald,
                                    fontWeight = FontWeight.Bold
                                )

                                // Step Selector Chips
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    com.example.data.remote.GeminiService.SmolAgentsConfig.AGENTIC_RAG_STEPS.forEachIndexed { idx, stepStr ->
                                        val isSelected = selectedAgenticRagStep == stepStr
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedAgenticRagStep = stepStr },
                                            label = { Text(stepStr) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = TacticalEmerald.copy(alpha = 0.22f),
                                                selectedLabelColor = TacticalEmerald
                                            ),
                                            modifier = Modifier.testTag("smolagents_agentic_rag_step_$idx")
                                        )
                                    }
                                }

                                // Model Selector & Preset Queries Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        com.example.data.remote.GeminiService.SmolAgentsConfig.AGENTIC_RAG_DEFAULT_MODEL,
                                        com.example.data.remote.GeminiService.SmolAgentsConfig.AGENTIC_RAG_ALT_MODEL
                                    ).forEachIndexed { mIdx, ragModel ->
                                        FilterChip(
                                            selected = selectedAgenticRagModel == ragModel,
                                            onClick = { selectedAgenticRagModel = ragModel },
                                            label = { Text(ragModel) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = SovereignGold.copy(alpha = 0.22f),
                                                selectedLabelColor = SovereignGold
                                            ),
                                            modifier = Modifier.testTag("smolagents_agentic_rag_model_$mIdx")
                                        )
                                    }
                                    com.example.data.remote.GeminiService.SmolAgentsConfig.AGENTIC_RAG_PRESET_QUERIES.forEachIndexed { qIdx, presetQ ->
                                        FilterChip(
                                            selected = customRagQueryInput == presetQ,
                                            onClick = { customRagQueryInput = presetQ },
                                            label = { Text("Q${qIdx + 1}: ${presetQ.take(36)}...") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                                                selectedLabelColor = CyberCyan
                                            ),
                                            modifier = Modifier.testTag("smolagents_agentic_rag_preset_$qIdx")
                                        )
                                    }
                                }

                                // 6-Point Guide Summary
                                com.example.data.remote.GeminiService.SmolAgentsConfig.AGENTIC_RAG_GUIDE.forEach { bullet ->
                                    Text(
                                        text = "• $bullet",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(vertical = 1.dp)
                                    )
                                }

                                OutlinedTextField(
                                    value = customRagQueryInput,
                                    onValueChange = { customRagQueryInput = it },
                                    label = { Text("Question / Affirmative Retriever Query (`RetrieverTool.forward(query: str)`)") },
                                    placeholder = { Text(com.example.data.remote.GeminiService.SmolAgentsConfig.AGENTIC_RAG_DEFAULT_QUESTION) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .testTag("smolagents_agentic_rag_query_input")
                                )

                                Text(
                                    text = currentAgenticRagSnippet,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = CyberCyan,
                                    modifier = Modifier
                                        .padding(vertical = 4.dp)
                                        .testTag("smolagents_agentic_rag_snippet")
                                )

                                Button(
                                    onClick = {
                                        liveToolExecutionLog = localGeminiService.runAgenticRagExecutor(
                                            stepMode = selectedAgenticRagStep,
                                            userQuestionOrQuery = customRagQueryInput,
                                            modelId = selectedAgenticRagModel
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = TacticalEmerald.copy(alpha = 0.22f),
                                        contentColor = TacticalEmerald
                                    ),
                                    border = BorderStroke(1.dp, TacticalEmerald),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("smolagents_run_agentic_rag_btn")
                                ) {
                                    Text(
                                        text = "📚 RUN LIVE ON-DEVICE CHUNKER (500/50) + OKAPI BM25 (k=10) & CODEAGENT TRACE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Text-to-SQL Agent Hub (`# Text-to-SQL` — SQLAlchemy `sqlite:///:memory:` + `@tool def sql_engine(query: str) -> str` + Level 1/Level 2 Joins)
                    if (showTextToSqlHub) {
                        Surface(
                            color = VaultCardSurface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("smolagents_text_to_sql_hub")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "🗄️ TEXT-TO-SQL AGENT (SQLAlchemy sqlite:///:memory: • inspect(engine) • @tool sql_engine • Level 1 receipts & Level 2 waiters JOIN)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SovereignGold,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    com.example.data.remote.GeminiService.SmolAgentsConfig.TEXT_TO_SQL_LEVELS.forEachIndexed { idx, levelStr ->
                                        FilterChip(
                                            selected = selectedTextToSqlLevel == levelStr,
                                            onClick = { selectedTextToSqlLevel = levelStr },
                                            label = { Text(levelStr) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = SovereignGold.copy(alpha = 0.25f),
                                                selectedLabelColor = SovereignGold
                                            ),
                                            modifier = Modifier.testTag("smolagents_text_to_sql_level_$idx")
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    com.example.data.remote.GeminiService.SmolAgentsConfig.TEXT_TO_SQL_PRESET_QUERIES.forEachIndexed { idx, presetSql ->
                                        FilterChip(
                                            selected = customSqlInput == presetSql,
                                            onClick = { customSqlInput = presetSql },
                                            label = {
                                                Text(
                                                    when (idx) {
                                                        0 -> "Preset 1: Max Receipt (Woodrow Wilson)"
                                                        1 -> "Preset 2: Waiter Tips JOIN (Michael Watts)"
                                                        else -> "Preset 3: All Receipts + Waiters"
                                                    }
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = CyberCyan.copy(alpha = 0.25f),
                                                selectedLabelColor = CyberCyan
                                            ),
                                            modifier = Modifier.testTag("smolagents_text_to_sql_preset_$idx")
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = customSqlInput,
                                    onValueChange = { customSqlInput = it },
                                    label = { Text("Live SQLite Query (`sql_engine(query: str) -> str`)") },
                                    placeholder = { Text("SELECT customer_name, price FROM receipts ORDER BY price DESC LIMIT 1") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .testTag("smolagents_text_to_sql_query_input")
                                )

                                Text(
                                    text = currentTextToSqlSnippet,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = CyberCyan,
                                    modifier = Modifier
                                        .padding(vertical = 4.dp)
                                        .testTag("smolagents_text_to_sql_snippet")
                                )

                                Button(
                                    onClick = {
                                        liveToolExecutionLog = localGeminiService.runTextToSqlAgentExecutor(
                                            levelMode = selectedTextToSqlLevel,
                                            customSqlQuery = customSqlInput
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SovereignGold.copy(alpha = 0.22f),
                                        contentColor = SovereignGold
                                    ),
                                    border = BorderStroke(1.dp, SovereignGold),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("smolagents_run_text_to_sql_btn")
                                ) {
                                    Text(
                                        text = "🗄️ RUN LIVE ON-DEVICE SQLITE (`sqlite:///:memory:`) & REACT TRACE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Manage Your Agent's Memory Hub (`# 📚 Manage your agent's memory`)
                    if (showMemoryHub) {
                        Surface(
                            color = VaultCardSurface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.65f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("smolagents_memory_hub")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "📚 MANAGE AGENT MEMORY (agent.replay() • ActionStep/TaskStep • get_full_steps() • step_callbacks=[update_screenshot] • agent.step())",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SovereignGold,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    com.example.data.remote.GeminiService.SmolAgentsConfig.MEMORY_MANAGEMENT_MODES.forEachIndexed { idx, memMode ->
                                        val isSelected = selectedMemoryMode == memMode
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedMemoryMode = memMode },
                                            label = { Text(memMode) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = SovereignGold.copy(alpha = 0.25f),
                                                selectedLabelColor = SovereignGold
                                            ),
                                            modifier = Modifier.testTag("smolagents_memory_mode_$idx")
                                        )
                                    }
                                    FilterChip(
                                        selected = pruneScreenshotCallback,
                                        onClick = { pruneScreenshotCallback = !pruneScreenshotCallback },
                                        label = { Text(if (pruneScreenshotCallback) "Prune Screenshots (<=latest-2)" else "Keep All Screenshots") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CyberCyan.copy(alpha = 0.25f),
                                            selectedLabelColor = CyberCyan
                                        ),
                                        modifier = Modifier.testTag("smolagents_memory_prune_toggle")
                                    )
                                }

                                Text(
                                    text = currentMemorySnippet,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = CyberCyan,
                                    modifier = Modifier
                                        .padding(vertical = 4.dp)
                                        .testTag("smolagents_memory_snippet")
                                )

                                Button(
                                    onClick = {
                                        liveToolExecutionLog = localGeminiService.runAgentMemoryStepInspector(
                                            memoryMode = selectedMemoryMode,
                                            pruneScreenshots = pruneScreenshotCallback,
                                            maxSteps = 10
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SovereignGold.copy(alpha = 0.22f),
                                        contentColor = SovereignGold
                                    ),
                                    border = BorderStroke(1.dp, SovereignGold),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("smolagents_run_memory_inspector_btn")
                                ) {
                                    Text(
                                        text = "📚 INSPECT MEMORY STEPS, REPLAY & STEP CALLBACK ($selectedMemoryMode)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // What Are Agents? Agency Spectrum (☆☆☆ -> ★★★) & How Multi-Step Agents Work (`MultiStepAgent` ReAct Loop)
                    if (showWhatAreAgentsHub) {
                        Surface(
                            color = VaultCardSurface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.65f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("smolagents_what_are_agents_hub")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "🤔 WHAT ARE AGENTS? (Agency Spectrum ☆☆☆→★★★ • Surfing Trip Decision Matrix • 6 Building Blocks • Code vs JSON 2402.01030)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CyberCyan,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    com.example.data.remote.GeminiService.SmolAgentsConfig.AGENCY_SPECTRUM_LEVELS.forEachIndexed { idx, level ->
                                        FilterChip(
                                            selected = selectedAgencyLevel == level,
                                            onClick = { selectedAgencyLevel = level },
                                            label = { Text(level) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = CyberCyan.copy(alpha = 0.25f),
                                                selectedLabelColor = CyberCyan
                                            ),
                                            modifier = Modifier.testTag("smolagents_agency_level_$idx")
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    com.example.data.remote.GeminiService.SmolAgentsConfig.SURFING_TRIP_SCENARIOS.forEachIndexed { idx, scenario ->
                                        FilterChip(
                                            selected = selectedSurfingScenario == scenario,
                                            onClick = { selectedSurfingScenario = scenario },
                                            label = { Text(scenario) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = TacticalEmerald.copy(alpha = 0.25f),
                                                selectedLabelColor = TacticalEmerald
                                            ),
                                            modifier = Modifier.testTag("smolagents_surfing_scenario_$idx")
                                        )
                                    }
                                }

                                Text(
                                    text = currentAgencySpectrumSnippet,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = SovereignGold,
                                    modifier = Modifier
                                        .padding(vertical = 4.dp)
                                        .testTag("smolagents_agency_spectrum_snippet")
                                )

                                Button(
                                    onClick = {
                                        liveToolExecutionLog = localGeminiService.runAgencySpectrumAndSurfingTripEvaluator(
                                            agencyLevel = selectedAgencyLevel,
                                            surfingScenario = selectedSurfingScenario
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CyberCyan.copy(alpha = 0.22f),
                                        contentColor = CyberCyan
                                    ),
                                    border = BorderStroke(1.dp, CyberCyan),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("smolagents_run_agency_evaluator_btn")
                                ) {
                                    Text(
                                        text = "🤔 EVALUATE AGENCY SPECTRUM ($selectedAgencyLevel) & SURFING WORKFLOW",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // "# How do multi-step agents work?" — ReAct Framework (Yao et al., 2022) & MultiStepAgent Cycle
                                Text(
                                    text = "🔄 HOW MULTI-STEP AGENTS WORK (ReAct Yao et al. 2210.03629 • MultiStepAgent • write_memory_to_messages • PlanningStep)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TacticalEmerald,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    com.example.data.remote.GeminiService.SmolAgentsConfig.MULTI_STEP_AGENT_VARIANTS.forEachIndexed { idx, variant ->
                                        FilterChip(
                                            selected = selectedMultiStepVariant == variant,
                                            onClick = { selectedMultiStepVariant = variant },
                                            label = { Text(variant) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = TacticalEmerald.copy(alpha = 0.25f),
                                                selectedLabelColor = TacticalEmerald
                                            ),
                                            modifier = Modifier.testTag("smolagents_multistep_variant_$idx")
                                        )
                                    }
                                    FilterChip(
                                        selected = enableReactPlanningStep,
                                        onClick = { enableReactPlanningStep = !enableReactPlanningStep },
                                        label = { Text(if (enableReactPlanningStep) "PlanningStep: ON" else "PlanningStep: OFF") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SovereignGold.copy(alpha = 0.25f),
                                            selectedLabelColor = SovereignGold
                                        ),
                                        modifier = Modifier.testTag("smolagents_react_planning_step_toggle")
                                    )
                                    FilterChip(
                                        selected = enableReactStepCallbacks,
                                        onClick = { enableReactStepCallbacks = !enableReactStepCallbacks },
                                        label = { Text(if (enableReactStepCallbacks) "step_callbacks: ON" else "step_callbacks: OFF") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CyberCyan.copy(alpha = 0.25f),
                                            selectedLabelColor = CyberCyan
                                        ),
                                        modifier = Modifier.testTag("smolagents_react_callbacks_toggle")
                                    )
                                }

                                Text(
                                    text = currentMultiStepReactSnippet,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = CyberCyan,
                                    modifier = Modifier
                                        .padding(vertical = 4.dp)
                                        .testTag("smolagents_multistep_react_snippet")
                                )

                                Button(
                                    onClick = {
                                        liveToolExecutionLog = localGeminiService.runMultiStepReactCycleSimulator(
                                            agentVariant = selectedMultiStepVariant,
                                            enablePlanningStep = enableReactPlanningStep,
                                            includeStepCallbacks = enableReactStepCallbacks
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = TacticalEmerald.copy(alpha = 0.22f),
                                        contentColor = TacticalEmerald
                                    ),
                                    border = BorderStroke(1.dp, TacticalEmerald),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("smolagents_run_multistep_react_btn")
                                ) {
                                    Text(
                                        text = "🔄 SIMULATE MULTISTEPAGENT REACT LOOP ($selectedMultiStepVariant)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Secure Code Execution & LocalPythonExecutor AST Safeguard Hub (`# Secure code execution`)
                    if (showSecureSandboxHub) {
                        Surface(
                            color = VaultCardSurface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, OutlawCrimson.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("smolagents_secure_sandbox_hub")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "🔒 SECURE CODE EXECUTION (LocalPythonExecutor AST • Blaxel/E2B/Modal/Docker • E2B Multi-Agent • DockerSandbox)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = OutlawCrimson,
                                    fontWeight = FontWeight.Bold
                                )

                                // Sandbox Architecture Mode Chips
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    com.example.data.remote.GeminiService.SmolAgentsConfig.SANDBOX_ARCHITECTURE_MODES.forEachIndexed { idx, archMode ->
                                        val isSelected = selectedSandboxArchMode == archMode
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedSandboxArchMode = archMode },
                                            label = { Text(archMode) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = OutlawCrimson.copy(alpha = 0.25f),
                                                selectedLabelColor = OutlawCrimson
                                            ),
                                            modifier = Modifier.testTag("smolagents_sandbox_arch_$idx")
                                        )
                                    }
                                }

                                // AST Safeguard Test Selector + Fibonacci Target Selector (F_20, F_42, F_100)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    com.example.data.remote.GeminiService.SmolAgentsConfig.AST_SAFEGUARD_KEYS.forEachIndexed { idx, astKey ->
                                        val isSelected = selectedAstSafeguardKey == astKey
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedAstSafeguardKey = astKey },
                                            label = { Text(astKey) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = SovereignGold.copy(alpha = 0.25f),
                                                selectedLabelColor = SovereignGold
                                            ),
                                            modifier = Modifier.testTag("smolagents_ast_safeguard_$idx")
                                        )
                                    }
                                    listOf(20, 42, 100).forEach { fibN ->
                                        FilterChip(
                                            selected = selectedFibTarget == fibN,
                                            onClick = { selectedFibTarget = fibN },
                                            label = { Text("Fib($fibN)") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = CyberCyan.copy(alpha = 0.25f),
                                                selectedLabelColor = CyberCyan
                                            ),
                                            modifier = Modifier.testTag("smolagents_fib_target_$fibN")
                                        )
                                    }
                                }

                                Text(
                                    text = currentSecureSandboxSnippet,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = CyberCyan,
                                    modifier = Modifier
                                        .padding(vertical = 4.dp)
                                        .testTag("smolagents_secure_sandbox_snippet")
                                )

                                Button(
                                    onClick = {
                                        liveToolExecutionLog = localGeminiService.runLocalPythonExecutorSafeguardAudit(
                                            testCaseKey = selectedAstSafeguardKey,
                                            executorType = selectedExecutor,
                                            fibN = selectedFibTarget
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = OutlawCrimson.copy(alpha = 0.22f),
                                        contentColor = OutlawCrimson
                                    ),
                                    border = BorderStroke(1.dp, OutlawCrimson),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("smolagents_run_ast_sandbox_verifier_btn")
                                ) {
                                    Text(
                                        text = "🛡️ VERIFY AST SAFEGUARD ($selectedAstSafeguardKey) & FIB($selectedFibTarget)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Advanced Tools & MCP Server Integration Hub (`# Tools` Guide)
                    if (showAdvancedToolsHub) {
                        Surface(
                            color = VaultCardSurface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, TacticalEmerald.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("smolagents_advanced_tools_hub")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "🛠️ ADVANCED TOOLS & MCP HUB (HFModelDownloadsTool • Space • LangChain • Hub Collection • MCP structured_output=True)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TacticalEmerald,
                                    fontWeight = FontWeight.Bold
                                )

                                // Tool Source Selector Chips
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    com.example.data.remote.GeminiService.SmolAgentsConfig.TOOL_INTEGRATION_SOURCES.forEachIndexed { idx, src ->
                                        val isSelected = selectedToolSource == src
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedToolSource = src },
                                            label = { Text(src) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = TacticalEmerald.copy(alpha = 0.25f),
                                                selectedLabelColor = TacticalEmerald
                                            ),
                                            modifier = Modifier.testTag("smolagents_tool_src_$idx")
                                        )
                                    }
                                }

                                // Task Category Chips for HFModelDownloadsTool + MCP structured_output toggle
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    com.example.data.remote.GeminiService.SmolAgentsConfig.HF_DOWNLOAD_COUNTER_TASKS.forEach { taskKey ->
                                        val isSelected = selectedHfTask == taskKey
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedHfTask = taskKey },
                                            label = { Text("task:$taskKey") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = SovereignGold.copy(alpha = 0.25f),
                                                selectedLabelColor = SovereignGold
                                            ),
                                            modifier = Modifier.testTag("smolagents_hf_task_$taskKey")
                                        )
                                    }
                                    FilterChip(
                                        selected = mcpStructuredOutput,
                                        onClick = { mcpStructuredOutput = !mcpStructuredOutput },
                                        label = { Text("structured_output=${if (mcpStructuredOutput) "True" else "False"}") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CyberCyan.copy(alpha = 0.25f),
                                            selectedLabelColor = CyberCyan
                                        ),
                                        modifier = Modifier.testTag("smolagents_mcp_structured_toggle")
                                    )
                                }

                                Text(
                                    text = currentAdvancedToolSnippet,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = CyberCyan,
                                    modifier = Modifier
                                        .padding(vertical = 4.dp)
                                        .testTag("smolagents_advanced_tool_snippet")
                                )

                                // Action Buttons: Live HF Hub Model Download Counter + MCP WeatherInfo Structured Output Inspector
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                isRunningLiveHfTool = true
                                                try {
                                                    liveToolExecutionLog = localGeminiService.runLiveHfModelDownloadsTool(selectedHfTask)
                                                } finally {
                                                    isRunningLiveHfTool = false
                                                }
                                            }
                                        },
                                        enabled = !isRunningLiveHfTool,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SovereignGold.copy(alpha = 0.22f),
                                            contentColor = SovereignGold
                                        ),
                                        border = BorderStroke(1.dp, SovereignGold),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("smolagents_run_live_hf_counter_btn")
                                    ) {
                                        Text(
                                            text = if (isRunningLiveHfTool) "⏳ HF HUB..." else "🔥 LIVE HF DOWNLOADS ($selectedHfTask)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            liveToolExecutionLog = localGeminiService.runMcpStructuredWeatherInspector(
                                                city = "Tokyo",
                                                structuredOutput = mcpStructuredOutput
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = TacticalEmerald.copy(alpha = 0.22f),
                                            contentColor = TacticalEmerald
                                        ),
                                        border = BorderStroke(1.dp, TacticalEmerald),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("smolagents_run_mcp_weather_btn")
                                    ) {
                                        Text(
                                            text = "🌤️ MCP WEATHER SCHEMA",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (!liveToolExecutionLog.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = liveToolExecutionLog!!,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = SovereignGold,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("smolagents_live_tool_output")
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Install: $currentInstallCmd\nCLI: $currentCliCmd\n$currentSnippet",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = SovereignGold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    if (showOtelGuide) {
                        Text(
                            text = com.example.data.remote.GeminiService.SmolAgentsConfig.OPENTELEMETRY_INSPECTION_GUIDE.joinToString("\n"),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = TacticalEmerald,
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .testTag("smolagents_otel_guide_list")
                        )
                    }

                    if (showGoldenRules) {
                        Text(
                            text = com.example.data.remote.GeminiService.SmolAgentsConfig.SYSTEM_PROMPT_11_GOLDEN_RULES.joinToString("\n"),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = CyberCyan,
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .testTag("smolagents_golden_rules_list")
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        com.example.data.remote.GeminiService.SmolAgentsConfig.ALL_EXTRA_KEYS.forEach { extraKey ->
                            val isSelected = selectedExtras.contains(extraKey)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedExtras = if (extraKey == "all") {
                                        if (isSelected) setOf("toolkit") else setOf("all")
                                    } else {
                                        val withoutAll = selectedExtras - "all"
                                        if (isSelected) withoutAll - extraKey else withoutAll + extraKey
                                    }
                                },
                                label = { Text("[$extraKey]") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SovereignGold.copy(alpha = 0.24f),
                                    selectedLabelColor = SovereignGold
                                ),
                                modifier = Modifier.testTag("smolagents_extra_$extraKey")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onSendMessage(
                                    "Tol, patakbuhin ang smolagents $selectedAgentType ($selectedModelBackend, tool=\"$selectedToolSource\", hf_task=\"$selectedHfTask\", mcp_structured_output=$mcpStructuredOutput, otel=\"$selectedTelemetryBackend\", executor_type=\"$selectedExecutor\", planning_interval=${if (enablePlanningInterval) 3 else 0}, additional_args=$includeAdditionalArgs) gamit ang `$currentInstallCmd` para sa ${selectedModule.code} (${selectedModule.statute}). Isama ang custom `HFModelDownloadsTool` (`model_download_counter` na may reversed letters `[::-1]`), `Tool.from_space('black-forest-labs/FLUX.1-schnell')`, `Tool.from_langchain(load_tools(['serpapi'])[0])`, `ToolCollection.from_hub`, `agent.tools` dict management, at `MCPClient(server_parameters, structured_output=$mcpStructuredOutput)` (`FastMCP('Weather Service')` `WeatherInfo` `outputSchema`)."
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan.copy(alpha = 0.2f),
                                contentColor = CyberCyan
                            ),
                            border = BorderStroke(1.dp, CyberCyan),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("smolagents_dispatch_button")
                        ) {
                            Text(
                                text = "⚡ RUN ${selectedAgentType.uppercase()} (${selectedTelemetryBackend.uppercase()})",
                                color = CyberCyan,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = {
                                useMultiAgent = true
                                enablePlanningInterval = true
                                if (selectedTelemetryBackend == "none") {
                                    selectedTelemetryBackend = "phoenix"
                                }
                                onSendMessage(
                                    "Tol, i-deploy ang OpenTelemetry-instrumented Multi-Agent & MCP Tool System (`otel=$selectedTelemetryBackend` via `SmolagentsInstrumentor`, `MCPClient(server_parameters, structured_output=$mcpStructuredOutput)`, `HFModelDownloadsTool` para sa '$selectedHfTask', `search_agent = ToolCallingAgent(tools=[WebSearchTool(), VisitWebpageTool()], model=model, name='search_agent')`, `manager_agent = CodeAgent(managed_agents=[search_agent], planning_interval=3)`) gamit ang `$selectedModelBackend` para sa ${selectedModule.code} (${selectedModule.statute})."
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TacticalEmerald.copy(alpha = 0.2f),
                                contentColor = TacticalEmerald
                            ),
                            border = BorderStroke(1.dp, TacticalEmerald),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("smolagents_multiagent_dispatch_button")
                        ) {
                            Text(
                                text = "🔭 OTEL + MCP MULTI-AGENT",
                                color = TacticalEmerald,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
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
                            else "[${activeLanguage.displayName}] I-type ang kaso o Wolf's Den prompt..."
                        )
                    },
                    maxLines = 4,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_message_input")
                )

                Spacer(modifier = Modifier.width(6.dp))

                TextButton(
                    onClick = {
                        val textToSend = messageInput
                        messageInput = ""
                        onSendToSmolagents(selectedModule.title, textToSend)
                    },
                    enabled = !isAiWorking && messageInput.isNotBlank(),
                    modifier = Modifier.testTag("wolfs_den_smolagents_send_button")
                ) {
                    Text(
                        text = "🐺 WOLF",
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold
                    )
                }

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
