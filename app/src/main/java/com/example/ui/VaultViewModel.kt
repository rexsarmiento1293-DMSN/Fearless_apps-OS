package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BioscienceAudit
import com.example.data.model.ChatMessage
import com.example.data.model.JusticeModule
import com.example.data.model.LoopPhase
import com.example.data.model.VaultDocket
import com.example.data.remote.AudioWavHelper
import com.example.data.remote.DriveFileRunResult
import com.example.data.remote.GeminiService
import com.example.data.remote.GeneratedImageResult
import com.example.data.remote.GoogleDriveFileRunner
import com.example.data.remote.LiveVoiceSessionClient
import com.example.data.remote.VaultAiModel
import com.example.data.repository.VaultRepository
import com.example.ui.components.DEFAULT_DEEPMOTION_GLB_URL
import android.net.Uri
import kotlin.math.abs
import kotlin.math.sin
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class VaultViewModel(
    private val repository: VaultRepository,
    private val geminiService: GeminiService,
    private val currentUserId: String
) : ViewModel() {

    private val googleDriveRunner = GoogleDriveFileRunner(geminiService)

    val docketsState: StateFlow<UiState<List<VaultDocket>>> = repository.observeDockets(currentUserId)
        .map<List<VaultDocket>, UiState<List<VaultDocket>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing dockets", error)
            emit(UiState.Error(error.message ?: "Failed to load dockets"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val chatMessagesState: StateFlow<UiState<List<ChatMessage>>> = repository.observeChatMessages(currentUserId)
        .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing chat messages", error)
            emit(UiState.Error(error.message ?: "Failed to load chat thread"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val bioscienceState: StateFlow<UiState<List<BioscienceAudit>>> = repository.observeBioscienceAudits(currentUserId)
        .map<List<BioscienceAudit>, UiState<List<BioscienceAudit>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing bioscience audits", error)
            emit(UiState.Error(error.message ?: "Failed to load bioscience audits"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    private val _selectedModule = MutableStateFlow(JusticeModule.MODULE_01)
    val selectedModule = _selectedModule.asStateFlow()

    // [FIX 1: THE BLACK CASE KAGGLE TOKEN LOGIC]
    private val _kaggleKey = MutableStateFlow("")
    val kaggleKey = _kaggleKey.asStateFlow()

    private val _isBlackCaseUnlocked = MutableStateFlow(false)
    val isBlackCaseUnlocked = _isBlackCaseUnlocked.asStateFlow()

    private val _blackCaseAccessDenied = MutableStateFlow(false)
    val blackCaseAccessDenied = _blackCaseAccessDenied.asStateFlow()

    // [FIX 3: THE 9-LANGUAGE SELECTOR] (Strict single active language state)
    private val _activeLanguage = MutableStateFlow(com.example.data.model.VaultLanguage.TAGALOG)
    val activeLanguage = _activeLanguage.asStateFlow()

    // [FIX 5: RXM/LROS CONTACT RADAR (GPS INTEGRATION)]
    private val _selectedRadarType = MutableStateFlow("PAO")
    val selectedRadarType = _selectedRadarType.asStateFlow()

    private val _radarTelemetry = MutableStateFlow<com.example.data.remote.RadarScanTelemetry?>(null)
    val radarTelemetry = _radarTelemetry.asStateFlow()

    private val _isRadarScanning = MutableStateFlow(false)
    val isRadarScanning = _isRadarScanning.asStateFlow()

    private val _selectedModel = MutableStateFlow(VaultAiModel.ARGON_HIGH)
    val selectedModel = _selectedModel.asStateFlow()

    private val _enableHighThinking = MutableStateFlow(true)
    val enableHighThinking = _enableHighThinking.asStateFlow()

    private val _enableGoogleSearch = MutableStateFlow(false)
    val enableGoogleSearch = _enableGoogleSearch.asStateFlow()

    private val _enableGoogleMaps = MutableStateFlow(false)
    val enableGoogleMaps = _enableGoogleMaps.asStateFlow()

    private val _executeAll4Engines = MutableStateFlow(false)
    val executeAll4Engines = _executeAll4Engines.asStateFlow()

    private val _isAiWorking = MutableStateFlow(false)
    val isAiWorking = _isAiWorking.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner = _statusBanner.asStateFlow()

    private val _fastTriageOutput = MutableStateFlow<String?>(null)
    val fastTriageOutput = _fastTriageOutput.asStateFlow()

    private val _evidenceScanOutput = MutableStateFlow<String?>(null)
    val evidenceScanOutput = _evidenceScanOutput.asStateFlow()

    private val _generatedImage = MutableStateFlow<GeneratedImageResult?>(null)
    val generatedImage = _generatedImage.asStateFlow()

    private val _driveRunResult = MutableStateFlow<DriveFileRunResult?>(null)
    val driveRunResult = _driveRunResult.asStateFlow()

    private val _liveConnected = MutableStateFlow(false)
    val liveConnected = _liveConnected.asStateFlow()

    private val _liveTranscriptLog = MutableStateFlow<List<String>>(emptyList())
    val liveTranscriptLog = _liveTranscriptLog.asStateFlow()

    // 3D SceneView DeepMotion .glb Asset & Skeletal Speech Animation Handler State
    private val _deepMotionGlbUrl = MutableStateFlow(DEFAULT_DEEPMOTION_GLB_URL)
    val deepMotionGlbUrl = _deepMotionGlbUrl.asStateFlow()

    private val _isTtsSpeaking = MutableStateFlow(false)
    val isTtsSpeaking = _isTtsSpeaking.asStateFlow()

    private val _speechAmplitude = MutableStateFlow(0f)
    val speechAmplitude = _speechAmplitude.asStateFlow()

    private val _activeSpeechSnippet = MutableStateFlow("")
    val activeSpeechSnippet = _activeSpeechSnippet.asStateFlow()

    private var ttsSpeechJob: Job? = null
    private var liveClient: LiveVoiceSessionClient? = null

    fun updateKaggleKey(newKey: String) {
        _kaggleKey.value = newKey
        if (_blackCaseAccessDenied.value) {
            _blackCaseAccessDenied.value = false
        }
    }

    fun submitBlackCaseKaggleKey() {
        val unlocked = com.example.data.remote.LrosContactRadarService.verifyBlackCaseKaggleKey(_kaggleKey.value)
        if (unlocked) {
            _isBlackCaseUnlocked.value = true
            _blackCaseAccessDenied.value = false
            _statusBanner.value = "🔓 BLACK CASE: OWNER AUTHENTICATED • Glowing Conduits Online"
        } else {
            _isBlackCaseUnlocked.value = false
            _blackCaseAccessDenied.value = true
            _statusBanner.value = "🚨 BLACK CASE: ACCESS DENIED — Invalid Kaggle Wake-Up Key"
        }
    }

    fun selectLanguage(language: com.example.data.model.VaultLanguage) {
        _activeLanguage.value = language
        _statusBanner.value = "🌐 ${language.localizedBanner}"
    }

    fun findNearestProfessional(context: Context, type: String) {
        _selectedRadarType.value = type
        viewModelScope.launch {
            _isRadarScanning.value = true
            try {
                val result = com.example.data.remote.LrosContactRadarService.findNearestProfessional(context, type)
                _radarTelemetry.value = result
                _statusBanner.value = "📡 RXM/LROS Radar: Found ${result.places.size} nearest $type nodes (${result.gpsProviderUsed})"
            } finally {
                _isRadarScanning.value = false
            }
        }
    }

    fun wolfAvatarAnimation(context: Context, responseText: String) {
        speakVerdictText(context, responseText)
    }

    fun sendToSmolagents(
        context: Context,
        moduleName: String,
        userPrompt: String
    ) {
        if (userPrompt.isBlank()) return
        viewModelScope.launch {
            _isAiWorking.value = true
            try {
                val activeLang = _activeLanguage.value
                val moduleTag = _selectedModule.value.tag
                repository.addChatMessage(
                    sender = "USER",
                    moduleTag = moduleTag,
                    phase = "GENESIS",
                    planeStatus = "MORTAL",
                    content = "[WOLF'S DEN -> $moduleName | ${activeLang.displayName}] $userPrompt",
                    modelUsed = "SMOLAGENTS_ROUTE_REQ"
                )

                val reply = geminiService.sendToSmolagents(
                    moduleName = moduleName,
                    userPrompt = userPrompt,
                    activeLanguage = activeLang
                )

                repository.addChatMessage(
                    sender = "VAULT",
                    moduleTag = reply.detectedModule,
                    phase = reply.detectedPhase,
                    planeStatus = reply.planeStatus,
                    content = reply.text,
                    modelUsed = reply.modelUsed,
                    groundingSources = reply.groundingSources
                )

                // Trigger WolfAvatarAnimation(responseText) to simulate TTS Lip-sync
                wolfAvatarAnimation(context, reply.text)
            } finally {
                _isAiWorking.value = false
            }
        }
    }

    fun updateDeepMotionGlbUrl(newUrl: String) {
        if (newUrl.isNotBlank()) {
            _deepMotionGlbUrl.value = newUrl.trim()
            _statusBanner.value = "🦴 DeepMotion 3D Asset bound: ${newUrl.substringAfterLast('/')}"
        }
    }

    fun runGoogleDriveFile(
        context: Context,
        driveUrl: String,
        oauthAccessToken: String = "",
        customInstruction: String = ""
    ) {
        if (driveUrl.isBlank()) return
        viewModelScope.launch {
            _isAiWorking.value = true
            try {
                val res = googleDriveRunner.runDriveFileFromUrl(
                    context = context,
                    driveUrlOrId = driveUrl,
                    oauthAccessToken = oauthAccessToken,
                    customInstruction = customInstruction
                )
                _driveRunResult.value = res
                if (!res.localGlbPathOrUrl.isNullOrBlank()) {
                    _deepMotionGlbUrl.value = res.localGlbPathOrUrl
                }
                _statusBanner.value = "🚀 Executed Google Drive File (${res.fileId}) via ${res.executionMode}"
            } finally {
                _isAiWorking.value = false
            }
        }
    }

    fun runLocalDriveUri(
        context: Context,
        uri: Uri,
        customInstruction: String = ""
    ) {
        viewModelScope.launch {
            _isAiWorking.value = true
            try {
                val res = googleDriveRunner.runLocalUriFile(
                    context = context,
                    uri = uri,
                    customInstruction = customInstruction
                )
                _driveRunResult.value = res
                if (!res.localGlbPathOrUrl.isNullOrBlank()) {
                    _deepMotionGlbUrl.value = res.localGlbPathOrUrl
                }
                _statusBanner.value = "🚀 Executed Google Drive SAF File (${res.fileName}) via ${res.executionMode}"
            } finally {
                _isAiWorking.value = false
            }
        }
    }

    fun stopTtsSpeech() {
        ttsSpeechJob?.cancel()
        ttsSpeechJob = null
        _isTtsSpeaking.value = false
        _speechAmplitude.value = 0f
        _activeSpeechSnippet.value = ""
    }

    fun selectModule(module: JusticeModule) {
        _selectedModule.value = module
    }

    fun selectModel(model: VaultAiModel) {
        _selectedModel.value = model
        if (
            model == VaultAiModel.RECURSIVE_FLINT ||
            model == VaultAiModel.SWITCH_BASE_8 ||
            model == VaultAiModel.LLAMA3_158BIT ||
            model == VaultAiModel.LEROBOT_VLA ||
            model == VaultAiModel.FLASH_LITE
        ) {
            _enableHighThinking.value = false
        }
    }

    fun toggleHighThinking(enabled: Boolean) {
        _enableHighThinking.value = enabled
        if (enabled) {
            _selectedModel.value = VaultAiModel.ARGON_HIGH
        }
    }

    fun toggleGoogleSearch(enabled: Boolean) {
        _enableGoogleSearch.value = enabled
    }

    fun toggleGoogleMaps(enabled: Boolean) {
        _enableGoogleMaps.value = enabled
    }

    fun toggleExecuteAll4Engines(enabled: Boolean) {
        _executeAll4Engines.value = enabled
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun createNewDocket(
        title: String,
        caseSummary: String,
        module: JusticeModule,
        generateImmediateRuling: Boolean = true
    ) {
        if (title.isBlank() || caseSummary.isBlank()) return
        viewModelScope.launch {
            _isAiWorking.value = true
            try {
                val initialRuling = if (generateImmediateRuling) {
                    val reply = geminiService.sendVerdictChat(
                        history = emptyList(),
                        userMessage = "Case Title: $title\nCase Facts: $caseSummary",
                        selectedModel = _selectedModel.value,
                        enableHighThinking = _enableHighThinking.value,
                        enableGoogleSearch = _enableGoogleSearch.value,
                        enableGoogleMaps = _enableGoogleMaps.value,
                        executeAll4Engines = _executeAll4Engines.value,
                        forcedModuleTag = module.tag,
                        activeLanguage = _activeLanguage.value
                    )
                    reply.text
                } else {
                    "Initial blotter logged in Sovereign Vault 1293. Ready for MGD deliberation."
                }
                val result = repository.createDocket(
                    title = title,
                    caseSummary = caseSummary,
                    moduleTag = module.tag,
                    phase = LoopPhase.GENESIS.label,
                    planeStatus = if (generateImmediateRuling) "IMMORTAL" else "MORTAL",
                    rulingText = initialRuling
                )
                result.onSuccess {
                    _statusBanner.value = "🔒 Docket sealed into Firestore Vault (${module.code})."
                }.onFailure {
                    _statusBanner.value = "Error creating docket: ${it.localizedMessage}"
                }
            } finally {
                _isAiWorking.value = false
            }
        }
    }

    fun advanceDocketPhase(docket: VaultDocket) {
        viewModelScope.launch {
            _isAiWorking.value = true
            try {
                val currentPhase = LoopPhase.entries.firstOrNull { it.label == docket.phase } ?: LoopPhase.GENESIS
                val nextPhase = currentPhase.nextPhase()
                val newPlane = if (nextPhase == LoopPhase.IMMORTALIZED || nextPhase == LoopPhase.AUDIT_GATE) "IMMORTAL" else "MORTAL"

                val reply = geminiService.sendVerdictChat(
                    history = emptyList(),
                    userMessage = "Advance Docket '${docket.title}' (${docket.caseSummary}) to Phase ${nextPhase.stepNumber}: ${nextPhase.label}. Provide concrete action steps and required agency documents for this phase.",
                    selectedModel = VaultAiModel.ARGON_HIGH,
                    enableHighThinking = true,
                    enableGoogleSearch = false,
                    enableGoogleMaps = false,
                    executeAll4Engines = false,
                    forcedModuleTag = docket.moduleTag,
                    activeLanguage = _activeLanguage.value
                )

                repository.advanceDocketPhase(
                    docket = docket,
                    newPhase = nextPhase.label,
                    newPlaneStatus = newPlane,
                    updatedRuling = reply.text
                ).onSuccess {
                    _statusBanner.value = "⚡ Docket advanced to Phase ${nextPhase.stepNumber}: ${nextPhase.label} [$newPlane]"
                }.onFailure {
                    _statusBanner.value = "Update error: ${it.localizedMessage}"
                }
            } finally {
                _isAiWorking.value = false
            }
        }
    }

    fun deleteDocket(docketId: String) {
        viewModelScope.launch {
            repository.deleteDocket(docketId)
        }
    }

    fun sendChatPrompt(userText: String) {
        if (userText.isBlank()) return
        viewModelScope.launch {
            _isAiWorking.value = true
            try {
                val moduleTag = _selectedModule.value.tag
                repository.addChatMessage(
                    sender = "USER",
                    moduleTag = moduleTag,
                    phase = "GENESIS",
                    planeStatus = "MORTAL",
                    content = userText,
                    modelUsed = "COMMANDER_INPUT"
                )

                val currentMessages = (chatMessagesState.value as? UiState.Success)?.data.orEmpty()
                val historyPairs = currentMessages.takeLast(8).map { it.sender to it.content }

                val reply = geminiService.sendVerdictChat(
                    history = historyPairs,
                    userMessage = userText,
                    selectedModel = _selectedModel.value,
                    enableHighThinking = _enableHighThinking.value,
                    enableGoogleSearch = _enableGoogleSearch.value,
                    enableGoogleMaps = _enableGoogleMaps.value,
                    executeAll4Engines = _executeAll4Engines.value,
                    forcedModuleTag = moduleTag,
                    activeLanguage = _activeLanguage.value
                )

                repository.addChatMessage(
                    sender = "VAULT",
                    moduleTag = reply.detectedModule,
                    phase = reply.detectedPhase,
                    planeStatus = reply.planeStatus,
                    content = reply.text,
                    modelUsed = reply.modelUsed,
                    groundingSources = reply.groundingSources
                )
            } finally {
                _isAiWorking.value = false
            }
        }
    }

    fun deleteChatMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteChatMessage(messageId)
        }
    }

    fun triggerFastTriage(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isAiWorking.value = true
            try {
                _fastTriageOutput.value = geminiService.runFastTriage(query)
            } finally {
                _isAiWorking.value = false
            }
        }
    }

    fun speakVerdictText(context: Context, text: String) {
        ttsSpeechJob?.cancel()
        ttsSpeechJob = viewModelScope.launch {
            val cleanSnippet = text.replace(Regex("[#*`_~>-]+"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
                .take(120)
            _activeSpeechSnippet.value = "🔊 [${VaultAiModel.TTS.modelId}] $cleanSnippet"
            _isTtsSpeaking.value = true
            _speechAmplitude.value = 0.65f
            _statusBanner.value = "🔊 Synthesizing voice seal (${VaultAiModel.TTS.modelId}) + DeepMotion 3D Speech Rig..."

            // Launch concurrent skeletal lip-sync & outlaw gesture cadence while TTS synthesizes and speaks
            val cadenceJob = launch {
                var step = 0
                while (isActive) {
                    val syllableWave = 0.35f + abs(sin(step * 0.55f)) * 0.60f
                    _speechAmplitude.value = syllableWave
                    step++
                    delay(65L)
                }
            }

            try {
                val res = geminiService.synthesizeSpeech(text)
                res.onSuccess { audioBytes ->
                    AudioWavHelper.playAudioBytes(context, audioBytes)
                    _statusBanner.value = "🔊 Playing Sovereign Voice Verdict (${VaultAiModel.TTS.modelId}) + 3D Skeletal Speech Active."
                    val estimatedDurationMs = ((audioBytes.size / 48).toLong()).coerceIn(3500L, 12000L)
                    delay(estimatedDurationMs)
                }.onFailure {
                    // Keep 3D outlaw skeletal speech handler articulating the verdict cadence even when offline/fallback
                    _statusBanner.value = "🔊 3D Outlaw Skeletal Speech Rig Active (${VaultAiModel.TTS.modelId} Seal Cadence)."
                    val wordCount = cleanSnippet.split(" ").size.coerceAtLeast(8)
                    val estimatedDurationMs = (wordCount * 280L).coerceIn(4000L, 9000L)
                    delay(estimatedDurationMs)
                }
            } finally {
                cadenceJob.cancel()
                _isTtsSpeaking.value = false
                _speechAmplitude.value = 0f
                _activeSpeechSnippet.value = ""
            }
        }
    }

    fun transcribeRecordedPcm(pcmBytes: ByteArray, onResult: (String) -> Unit) {
        viewModelScope.launch {
            _isAiWorking.value = true
            try {
                val wavBytes = AudioWavHelper.pcmToWav(pcmBytes)
                val transcript = geminiService.transcribeAudioWav(wavBytes)
                onResult(transcript)
                _statusBanner.value = "🎙️ Transcribed via ${VaultAiModel.TRANSCRIBE.modelId}"
            } finally {
                _isAiWorking.value = false
            }
        }
    }

    fun runBioscienceLabAudit(
        sampleName: String,
        category: String,
        observationPrompt: String,
        riskLevel: String,
        useSearchGrounding: Boolean
    ) {
        if (sampleName.isBlank() || observationPrompt.isBlank()) return
        viewModelScope.launch {
            _isAiWorking.value = true
            try {
                val reply = geminiService.runBioscienceAnalysis(
                    sampleName = sampleName,
                    category = category,
                    observationPrompt = observationPrompt,
                    useSearchGrounding = useSearchGrounding
                )
                val citation = when {
                    category.contains("Water", ignoreCase = true) -> "RA 9275 Philippine Clean Water Act / DENR-EMB DAO 2016-08"
                    category.contains("Food", ignoreCase = true) || category.contains("Drug", ignoreCase = true) -> "RA 9711 FDA Act of 2009 / RA 10611 Food Safety Act"
                    category.contains("DNA", ignoreCase = true) || category.contains("Medico", ignoreCase = true) -> "A.M. No. 06-11-5-SC Rule on DNA Evidence / PNP Forensics"
                    else -> "RA 6969 Toxic Substances & Hazardous Wastes Act / BFAR Bulletin"
                }
                repository.createBioscienceAudit(
                    sampleName = sampleName,
                    bioscienceCategory = category,
                    findings = reply.text,
                    regulatoryCitation = citation,
                    riskLevel = riskLevel
                ).onSuccess {
                    _statusBanner.value = "🧬 Bioscience Audit sealed in Firestore Vault ($riskLevel RISK)."
                }.onFailure {
                    _statusBanner.value = "Failed to save Bioscience Audit: ${it.localizedMessage}"
                }
            } finally {
                _isAiWorking.value = false
            }
        }
    }

    fun deleteBioscienceAudit(auditId: String) {
        viewModelScope.launch {
            repository.deleteBioscienceAudit(auditId)
        }
    }

    fun scanEvidencePhoto(bitmap: Bitmap, prompt: String) {
        viewModelScope.launch {
            _isAiWorking.value = true
            try {
                _evidenceScanOutput.value = geminiService.analyzeEvidenceImage(
                    bitmap = bitmap,
                    prompt = prompt,
                    enableHighThinking = true
                )
                _statusBanner.value = "📸 Evidence analyzed with ${VaultAiModel.ARGON_HIGH.modelId}."
            } finally {
                _isAiWorking.value = false
            }
        }
    }

    fun createOrEditVisualAsset(
        prompt: String,
        aspectRatio: String,
        studioQuality: Boolean,
        referenceBitmap: Bitmap?
    ) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _isAiWorking.value = true
            try {
                val result = geminiService.generateOrEditImage(
                    prompt = prompt,
                    aspectRatio = aspectRatio,
                    studioQuality = studioQuality,
                    referenceBitmap = referenceBitmap
                )
                _generatedImage.value = result
                _statusBanner.value = "🎨 Visual asset rendered (${result.aspectRatio} via ${result.modelUsed})."
            } finally {
                _isAiWorking.value = false
            }
        }
    }

    fun toggleLiveVoiceSession() {
        if (_liveConnected.value) {
            liveClient?.disconnect()
            liveClient = null
            _liveConnected.value = false
            _liveTranscriptLog.value = _liveTranscriptLog.value + "🔒 Live Session Closed."
        } else {
            val client = geminiService.openLiveVoiceSession(
                onConnected = {
                    _liveConnected.value = true
                },
                onTranscriptReceived = { msg ->
                    _liveTranscriptLog.value = (_liveTranscriptLog.value + msg).takeLast(20)
                },
                onError = { err ->
                    _liveTranscriptLog.value = (_liveTranscriptLog.value + "⚠️ $err").takeLast(20)
                }
            )
            liveClient = client
            client.connect()
        }
    }

    fun sendLiveVoiceTurn(spokenText: String) {
        if (spokenText.isBlank()) return
        viewModelScope.launch {
            _isAiWorking.value = true
            try {
                _liveTranscriptLog.value = (_liveTranscriptLog.value + "🎙️ YOU: $spokenText").takeLast(20)
                val client = liveClient
                val response = if (client != null) {
                    client.sendLiveTurn(spokenText)
                } else {
                    geminiService.runFastTriage(spokenText)
                }
                _liveTranscriptLog.value = (_liveTranscriptLog.value + "⚡ VAULT 1293 (LIVE): $response").takeLast(20)
            } finally {
                _isAiWorking.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        liveClient?.disconnect()
    }

    private companion object {
        const val TAG = "VaultViewModel"
    }
}
