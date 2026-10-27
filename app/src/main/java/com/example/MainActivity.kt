package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.remote.GeminiService
import com.example.data.repository.VaultRepository
import com.example.ui.VaultViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutVault
import com.example.ui.screens.BioscienceLabScreen
import com.example.ui.screens.EvidenceStudioScreen
import com.example.ui.screens.LeRobotProfilerScreen
import com.example.ui.screens.VaultDashboardScreen
import com.example.ui.screens.VerdictChatScreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OutlawCrimson
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.TacticalEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultCardBorder
import com.example.ui.theme.VaultCardSurface
import com.example.ui.theme.VaultObsidian
import com.example.ui.theme.VaultSurface
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.iamdimension.v7.ui.IamDimensionAppState
import com.iamdimension.v7.ui.IamDimensionNavGraph
import com.iamdimension.v7.ui.IamDimensionPalette
import com.iamdimension.v7.ui.IamDimensionV7Theme
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppNavigation()
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

enum class VaultTab(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    DASHBOARD("dashboard", "7 Modules", Icons.Default.Dashboard),
    VERDICT_CHAT("verdict_chat", "MGD Verdict", Icons.AutoMirrored.Filled.Chat),
    LEROBOT_PROFILER("lerobot_profiler", "LeRobot Profiler", Icons.Default.Speed),
    BIOSCIENCE("bioscience_lab", "Bioscience", Icons.Default.Science),
    EVIDENCE_STUDIO("evidence_studio", "Studio & Scan", Icons.Default.AutoAwesome)
}

@Composable
fun AppNavigation() {
    var showV7PortalNavGraph by rememberSaveable { mutableStateOf(true) }
    val navController = rememberNavController()
    val appState = remember { IamDimensionAppState() }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = IamDimensionPalette.DeepNebula,
            border = BorderStroke(1.dp, IamDimensionPalette.CyberGold.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showV7PortalNavGraph) {
                        "0519 PORTAL ZONE • V.7 NAVGRAPH (19 IMGS)"
                    } else {
                        "VAULT 1293 • 5-TAB CLOUD SUITE"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = IamDimensionPalette.CyberGold
                )
                TextButton(
                    onClick = { showV7PortalNavGraph = !showV7PortalNavGraph },
                    modifier = Modifier.testTag("switch_portal_mode_button")
                ) {
                    Text(
                        text = if (showV7PortalNavGraph) "OPEN 5-TAB SUITE" else "OPEN 0519 NAVGRAPH",
                        style = MaterialTheme.typography.labelSmall,
                        color = IamDimensionPalette.PhylaxCyan
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (showV7PortalNavGraph) {
                IamDimensionV7Theme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = IamDimensionPalette.VoidBlack
                    ) {
                        IamDimensionNavGraph(
                            navController = navController,
                            appState = appState
                        )
                    }
                }
            } else {
                val auth = remember { FirebaseAuth.getInstance() }
                val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
                val user = currentUser

                if (user == null) {
                    AuthScreen(
                        onAuthSuccess = {}
                    )
                } else {
                    SovereignVaultMainScreen(
                        currentUserId = user.uid,
                        userEmail = user.email ?: "Commander Rex"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SovereignVaultMainScreen(
    currentUserId: String,
    userEmail: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val viewModel: VaultViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val db = FirebaseFirestore.getInstance(
                    "ai-studio-android-iamsover-129d727b-49ce-41e6-b574-5705e44eced2"
                )
                VaultViewModel(
                    repository = VaultRepository(db),
                    geminiService = GeminiService(),
                    currentUserId = currentUserId
                )
            }
        }
    )

    var currentTab by rememberSaveable { mutableStateOf(VaultTab.DASHBOARD) }

    if (currentTab != VaultTab.DASHBOARD) {
        BackHandler {
            currentTab = VaultTab.DASHBOARD
        }
    }

    val docketsState by viewModel.docketsState.collectAsStateWithLifecycle()
    val chatMessagesState by viewModel.chatMessagesState.collectAsStateWithLifecycle()
    val bioscienceState by viewModel.bioscienceState.collectAsStateWithLifecycle()
    val selectedModule by viewModel.selectedModule.collectAsStateWithLifecycle()
    val kaggleKey by viewModel.kaggleKey.collectAsStateWithLifecycle()
    val isBlackCaseUnlocked by viewModel.isBlackCaseUnlocked.collectAsStateWithLifecycle()
    val blackCaseAccessDenied by viewModel.blackCaseAccessDenied.collectAsStateWithLifecycle()
    val activeLanguage by viewModel.activeLanguage.collectAsStateWithLifecycle()
    val selectedRadarType by viewModel.selectedRadarType.collectAsStateWithLifecycle()
    val radarTelemetry by viewModel.radarTelemetry.collectAsStateWithLifecycle()
    val isRadarScanning by viewModel.isRadarScanning.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val enableHighThinking by viewModel.enableHighThinking.collectAsStateWithLifecycle()
    val enableGoogleSearch by viewModel.enableGoogleSearch.collectAsStateWithLifecycle()
    val enableGoogleMaps by viewModel.enableGoogleMaps.collectAsStateWithLifecycle()
    val executeAll4Engines by viewModel.executeAll4Engines.collectAsStateWithLifecycle()
    val isAiWorking by viewModel.isAiWorking.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val fastTriageOutput by viewModel.fastTriageOutput.collectAsStateWithLifecycle()
    val evidenceScanOutput by viewModel.evidenceScanOutput.collectAsStateWithLifecycle()
    val generatedImage by viewModel.generatedImage.collectAsStateWithLifecycle()
    val driveRunResult by viewModel.driveRunResult.collectAsStateWithLifecycle()
    val liveConnected by viewModel.liveConnected.collectAsStateWithLifecycle()
    val liveTranscriptLog by viewModel.liveTranscriptLog.collectAsStateWithLifecycle()
    val deepMotionGlbUrl by viewModel.deepMotionGlbUrl.collectAsStateWithLifecycle()
    val isTtsSpeaking by viewModel.isTtsSpeaking.collectAsStateWithLifecycle()
    val speechAmplitude by viewModel.speechAmplitude.collectAsStateWithLifecycle()
    val activeSpeechSnippet by viewModel.activeSpeechSnippet.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = VaultObsidian,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VaultSurface,
                    titleContentColor = TextPrimary
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Vault 1293 Emblem",
                            tint = SovereignGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "I.A.M DIMENSION OS v7.0 (${activeLanguage.isoCode})",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "VAULT 1293 • $userEmail • ${activeLanguage.displayName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SovereignGold
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            signOutVault(
                                context = context,
                                onSignOutComplete = {},
                                scope = scope
                            )
                        },
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out",
                            tint = OutlawCrimson
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = VaultSurface,
                contentColor = TextPrimary
            ) {
                VaultTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VaultObsidian,
                            selectedTextColor = SovereignGold,
                            indicatorColor = SovereignGold,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!statusBanner.isNullOrBlank()) {
                Surface(
                    color = VaultCardSurface,
                    border = BorderStroke(1.dp, TacticalEmerald.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("status_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusBanner!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TacticalEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearStatusBanner() }) {
                            Text("OK", color = SovereignGold)
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (currentTab) {
                    VaultTab.DASHBOARD -> {
                        VaultDashboardScreen(
                            docketsState = docketsState,
                            selectedModule = selectedModule,
                            fastTriageOutput = fastTriageOutput,
                            isAiWorking = isAiWorking,
                            kaggleKey = kaggleKey,
                            isBlackCaseUnlocked = isBlackCaseUnlocked,
                            blackCaseAccessDenied = blackCaseAccessDenied,
                            activeLanguage = activeLanguage,
                            selectedRadarType = selectedRadarType,
                            radarTelemetry = radarTelemetry,
                            isRadarScanning = isRadarScanning,
                            onUpdateKaggleKey = { viewModel.updateKaggleKey(it) },
                            onSubmitBlackCaseKey = { viewModel.submitBlackCaseKaggleKey() },
                            onSelectLanguage = { viewModel.selectLanguage(it) },
                            onFindNearestProfessional = { type ->
                                viewModel.findNearestProfessional(context, type)
                            },
                            onRouteToWolfsDenSmolagents = { mod ->
                                viewModel.selectModule(mod)
                                currentTab = VaultTab.VERDICT_CHAT
                                viewModel.sendToSmolagents(context, mod.title, mod.samplePrompt)
                            },
                            onSelectModule = { viewModel.selectModule(it) },
                            onLaunchModulePromptInChat = { mod ->
                                viewModel.selectModule(mod)
                                currentTab = VaultTab.VERDICT_CHAT
                                viewModel.sendChatPrompt(mod.samplePrompt)
                            },
                            onCreateDocket = { title, summary, mod ->
                                viewModel.createNewDocket(title, summary, mod)
                            },
                            onAdvancePhase = { docket ->
                                viewModel.advanceDocketPhase(docket)
                            },
                            onDeleteDocket = { id ->
                                viewModel.deleteDocket(id)
                            },
                            onFastTriage = { query ->
                                viewModel.triggerFastTriage(query)
                            },
                            onSpeakText = { text ->
                                viewModel.speakVerdictText(context, text)
                            }
                        )
                    }

                    VaultTab.VERDICT_CHAT -> {
                        VerdictChatScreen(
                            chatMessagesState = chatMessagesState,
                            selectedModule = selectedModule,
                            selectedModel = selectedModel,
                            enableHighThinking = enableHighThinking,
                            enableGoogleSearch = enableGoogleSearch,
                            enableGoogleMaps = enableGoogleMaps,
                            executeAll4Engines = executeAll4Engines,
                            isAiWorking = isAiWorking,
                            liveConnected = liveConnected,
                            liveTranscriptLog = liveTranscriptLog,
                            deepMotionGlbUrl = deepMotionGlbUrl,
                            isTtsSpeaking = isTtsSpeaking,
                            speechAmplitude = speechAmplitude,
                            activeSpeechSnippet = activeSpeechSnippet,
                            onUpdateGlbUrl = { viewModel.updateDeepMotionGlbUrl(it) },
                            onStopTtsSpeech = { viewModel.stopTtsSpeech() },
                            onSelectModel = { viewModel.selectModel(it) },
                            onToggleHighThinking = { viewModel.toggleHighThinking(it) },
                            onToggleGoogleSearch = { viewModel.toggleGoogleSearch(it) },
                            onToggleGoogleMaps = { viewModel.toggleGoogleMaps(it) },
                            onToggleExecuteAll4Engines = { viewModel.toggleExecuteAll4Engines(it) },
                            onSendMessage = { viewModel.sendChatPrompt(it) },
                            onDeleteMessage = { viewModel.deleteChatMessage(it) },
                            onSpeakMessage = { viewModel.speakVerdictText(context, it) },
                            onTranscribePcm = { pcm, cb -> viewModel.transcribeRecordedPcm(pcm, cb) },
                            onToggleLiveSession = { viewModel.toggleLiveVoiceSession() },
                            onSendLiveVoiceTurn = { viewModel.sendLiveVoiceTurn(it) },
                            activeLanguage = activeLanguage,
                            onSendToSmolagents = { modName, prompt ->
                                viewModel.sendToSmolagents(context, modName, prompt)
                            }
                        )
                    }

                    VaultTab.LEROBOT_PROFILER -> {
                        LeRobotProfilerScreen()
                    }

                    VaultTab.BIOSCIENCE -> {
                        BioscienceLabScreen(
                            bioscienceState = bioscienceState,
                            isAiWorking = isAiWorking,
                            onRunBioscienceAudit = { sample, cat, obs, risk, search ->
                                viewModel.runBioscienceLabAudit(sample, cat, obs, risk, search)
                            },
                            onDeleteAudit = { viewModel.deleteBioscienceAudit(it) },
                            onSpeakFindings = { viewModel.speakVerdictText(context, it) }
                        )
                    }

                    VaultTab.EVIDENCE_STUDIO -> {
                        EvidenceStudioScreen(
                            isAiWorking = isAiWorking,
                            driveRunResult = driveRunResult,
                            evidenceScanOutput = evidenceScanOutput,
                            generatedImageResult = generatedImage,
                            onRunDriveUrl = { url, token, directive ->
                                viewModel.runGoogleDriveFile(context, url, token, directive)
                            },
                            onRunLocalDriveUri = { uri, directive ->
                                viewModel.runLocalDriveUri(context, uri, directive)
                            },
                            onBindGlbTo3DRig = { glbPathOrUrl ->
                                viewModel.updateDeepMotionGlbUrl(glbPathOrUrl)
                                currentTab = VaultTab.VERDICT_CHAT
                            },
                            onScanEvidencePhoto = { bmp, prompt ->
                                viewModel.scanEvidencePhoto(bmp, prompt)
                            },
                            onGenerateOrEditImage = { prompt, ratio, pro, refBmp ->
                                viewModel.createOrEditVisualAsset(prompt, ratio, pro, refBmp)
                            },
                            onSpeakText = { viewModel.speakVerdictText(context, it) }
                        )
                    }
                }
            }
        }
    }
}
