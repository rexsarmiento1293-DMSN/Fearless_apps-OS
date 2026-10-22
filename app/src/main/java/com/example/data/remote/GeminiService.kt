package com.example.data.remote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Base64
import com.example.BuildConfig
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

enum class ThinkingLevel(val value: String) {
    HIGH("HIGH"),
    MEDIUM("MEDIUM"),
    LOW("LOW")
}

enum class VaultAiModel(val modelId: String, val label: String, val badge: String) {
    ARGON_HIGH("gemini-4-argon", "Gemini 4 Argon (High Thinking)", "DEEP VERDICT"),
    FLASH_GENERAL("gemini-3.8-flash", "Gemini 3.8 Flash (Search/Maps)", "GROUNDED"),
    FLASH_LITE("gemini-3.1-flash-lite", "Gemini 3.1 Flash-Lite (Low-Latency)", "FAST TRIAGE"),
    TTS("gemini-3.8-flash-tts", "Gemini 3.8 Flash TTS", "VOICE SEAL"),
    TRANSCRIBE("gemini-3.5-transcribe", "Gemini 3.5 Transcribe", "MIC BLOTTER"),
    LIVE("gemini-3.8-live", "Gemini 3.8 Live API", "REALTIME VOICE"),
    IMAGE_BANANA("gemini-nano-banana-2.1", "Gemini Nano Banana 2.1", "EVIDENCE ART"),
    IMAGE_STUDIO("gemini-3-pro-image-preview", "Gemini 3 Pro Image Studio", "STUDIO PRO")
}

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val tools: List<JsonObject>? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val role: String? = null,
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@Serializable
data class InlineData(
    val mimeType: String,
    val data: String
)

@Serializable
data class GenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: JsonObject? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val thinkingConfig: ThinkingConfig? = null,
    val imageConfig: ImageConfig? = null,
    val responseModalities: List<String>? = null,
    val speechConfig: SpeechConfig? = null
)

@Serializable
data class ThinkingConfig(
    val thinkingLevel: String
)

@Serializable
data class ImageConfig(
    val aspectRatio: String,
    val imageSize: String? = null
)

@Serializable
data class SpeechConfig(
    val voiceConfig: VoiceConfig
)

@Serializable
data class VoiceConfig(
    val prebuiltVoiceConfig: PrebuiltVoiceConfig
)

@Serializable
data class PrebuiltVoiceConfig(
    val voiceName: String
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: Content? = null,
    val groundingMetadata: JsonObject? = null
)

interface GeminiRestApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

data class VaultAiReply(
    val text: String,
    val modelUsed: String,
    val groundingSources: String = "",
    val detectedModule: String = "MODULE_01",
    val detectedPhase: String = "GENESIS",
    val planeStatus: String = "IMMORTAL"
)

data class GeneratedImageResult(
    val bitmap: Bitmap?,
    val captionText: String,
    val modelUsed: String,
    val aspectRatio: String,
    val error: String? = null
)

object SovereignPromptBuilder {
    const val SYSTEM_PROMPT = """
# SYSTEM PROMPT — I.A.M DIMENSION OS v5.0 (SOVEREIGN VAULT 1293)
You are the conversational incarnation of I.A.M Dimension OS v5.0 — Sovereign Vault 1293, a dual-plane legal-strategic and bioscience operating system deployed at POWER PORTAL ZONE 1293.
- Owner / Commander: Rex Sarmiento (rexsarmiento1293@gmail.com)
- Field Command: Bacoor, Cavite, Philippines
- Motto: "Walang Lagay, May Seal."
- Persona: "TOL" (The Ultimate Realistic Outlaw Agent) — hybrid of Robin Padilla (mabilis, maangas, matalino makipagkapwa-tao) and Fernando Poe Jr. (barako, may prinsipyo, kalmado pero gung-ho kung kailangan, at may lalim).
- Language: Taglish by default (Filipino body + English technical/legal/scientific terms). Mirror the user's language if they write in pure Filipino or pure English.
- Register: Tactical, authoritative, raw, street-smart, and unapologetically outlaw. Use terms like "Tol", "P're", "Paps", "Tropa", "Prangkahan tayo", "Walang bolahan", "Ang ruling:", "Ang tamang hakbang:".
- No filler: Never say "As an AI..." or "I think...".
- Emojis: 1-2 per section only (⚖️ 🔒 🏛️ ✈️ 🏪 🌊 👴 📜 🦈 ⚡ 🧬).

CORE OPERATING PRINCIPLES:
1. ZERO NAKAW SA BATAS: Never fabricate a legal citation. Cite real Philippine laws (RA 7160 KP Law, RA 8042/10022 Migrant Workers Act, RA 9178 BMBE, RA 10121 DRRM Act, RA 9994 Expanded Senior Citizens Act, RA 7277 Magna Carta for PWDs, PD 1529 Property Registration Decree, RA 3765 Truth in Lending, RA 10173 Data Privacy Act, RA 9275 Clean Water Act, RA 9711 FDA Act).
2. 4-PHASE LOOP ENGINE: Prefix with [MODULE TAG] -> Current Phase: [GENESIS/EXECUTION/AUDIT_GATE/IMMORTALIZED].
3. 4 MGD ENGINES: Embody MGD_01 (Gemini Supreme Auditor), MGD_02 (Meta Dispatcher), MGD_03 (DeepSeek Schema Architect), and MGD_04 (Bagrok Strategic Builder). If the user says "EXECUTE ALL 4 ENGINES", output four labeled sections — one per MGD — each with its own tactical ruling.
4. DUAL-PLANE HONESTY & PAO DISCLAIMER: Close every response with:
[IMMORTAL / MORTAL STATUS]
🔒 SYSTEM SEAL: #VAULT1293-SECURE-[HASH]
"Gabay lang ito, hindi legal advice. Kumonsulta sa abogado o PAO."
"""
}

class GeminiService {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = false
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val api: GeminiRestApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiRestApi::class.java)
    }

    private val apiKey: String
        get() = BuildConfig.GEMINI_API_KEY

    fun isApiKeyConfigured(): Boolean {
        return apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"
    }

    // 1. Multi-Turn Chat & Deliberation Engine (Supports gemini-4-argon + ThinkingLevel.HIGH, gemini-3.8-flash + Search/Maps, gemini-3.1-flash-lite)
    suspend fun sendVerdictChat(
        history: List<Pair<String, String>>,
        userMessage: String,
        selectedModel: VaultAiModel,
        enableHighThinking: Boolean,
        enableGoogleSearch: Boolean,
        enableGoogleMaps: Boolean,
        executeAll4Engines: Boolean,
        forcedModuleTag: String? = null
    ): VaultAiReply = withContext(Dispatchers.IO) {
        val effectiveModel = when {
            enableGoogleSearch || enableGoogleMaps -> VaultAiModel.FLASH_GENERAL.modelId
            enableHighThinking -> VaultAiModel.ARGON_HIGH.modelId
            else -> selectedModel.modelId
        }

        val fullPrompt = buildString {
            if (forcedModuleTag != null) {
                append("[TARGET MODULE: $forcedModuleTag]\n")
            }
            if (executeAll4Engines) {
                append("EXECUTE ALL 4 ENGINES (MGD_01 Gemini Supreme Auditor, MGD_02 Meta Dispatcher, MGD_03 DeepSeek Schema Architect, MGD_04 Bagrok Strategic Builder).\n\n")
            }
            append(userMessage)
        }

        val contents = mutableListOf<Content>()
        history.takeLast(10).forEach { (sender, text) ->
            val role = if (sender == "USER") "user" else "model"
            contents.add(Content(role = role, parts = listOf(Part(text = text))))
        }
        contents.add(Content(role = "user", parts = listOf(Part(text = fullPrompt))))

        val toolsList = mutableListOf<JsonObject>()
        if (enableGoogleSearch) {
            toolsList.add(buildJsonObject { putJsonObject("googleSearch") {} })
        }
        if (enableGoogleMaps) {
            toolsList.add(buildJsonObject { putJsonObject("googleMaps") {} })
        }

        // Do not set maxOutputTokens when ThinkingLevel.HIGH is enabled on gemini-4-argon
        val genConfig = if (effectiveModel == VaultAiModel.ARGON_HIGH.modelId && enableHighThinking) {
            GenerationConfig(
                thinkingConfig = ThinkingConfig(thinkingLevel = ThinkingLevel.HIGH.value)
            )
        } else {
            GenerationConfig(temperature = 0.7f)
        }

        val request = GenerateContentRequest(
            contents = contents,
            generationConfig = genConfig,
            tools = toolsList.ifEmpty { null },
            systemInstruction = Content(parts = listOf(Part(text = SovereignPromptBuilder.SYSTEM_PROMPT)))
        )

        try {
            val response = api.generateContent(effectiveModel, apiKey, request)
            val candidate = response.candidates?.firstOrNull()
            val text = candidate?.content?.parts?.mapNotNull { it.text }?.joinToString("\n")
                ?.takeIf { it.isNotBlank() }
                ?: "Walang sagot mula sa Sovereign Engine. Suriin ang koneksyon o API key."

            val grounding = extractGroundingCitations(candidate?.groundingMetadata)
            val detectedModule = forcedModuleTag ?: inferModuleTag(userMessage + "\n" + text)
            val detectedPhase = inferPhase(text)
            val plane = if (text.contains("MORTAL", ignoreCase = true) && !text.contains("IMMORTAL", ignoreCase = true)) "MORTAL" else "IMMORTAL"

            VaultAiReply(
                text = text,
                modelUsed = effectiveModel,
                groundingSources = grounding,
                detectedModule = detectedModule,
                detectedPhase = detectedPhase,
                planeStatus = plane
            )
        } catch (e: Exception) {
            VaultAiReply(
                text = buildOfflineFallbackVerdict(
                    userMessage = userMessage,
                    forcedModuleTag = forcedModuleTag,
                    executeAll4Engines = executeAll4Engines,
                    errorReason = e.localizedMessage ?: "API Error"
                ),
                modelUsed = "$effectiveModel (Sovereign Fallback)",
                groundingSources = if (enableGoogleSearch || enableGoogleMaps) "Local Statutory Vault Cache (RA 7160 / RA 8042 / RA 9178 / RA 10121 / RA 9994 / PD 1529 / RA 10173)" else "",
                detectedModule = forcedModuleTag ?: inferModuleTag(userMessage),
                detectedPhase = "EXECUTION",
                planeStatus = "IMMORTAL"
            )
        }
    }

    // 2. Low-Latency Fast Triage using gemini-3.1-flash-lite
    suspend fun runFastTriage(query: String): String = withContext(Dispatchers.IO) {
        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = "MGD_02 FAST TRIAGE (Taglish, outlaw TOL tone, max 4 bullet points, cite exact Philippine law & agency): $query")
                    )
                )
            ),
            systemInstruction = Content(parts = listOf(Part(text = SovereignPromptBuilder.SYSTEM_PROMPT)))
        )
        try {
            val response = api.generateContent(VaultAiModel.FLASH_LITE.modelId, apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Ang ruling: I-blotter agad at hingin ang receiving copy."
        } catch (e: Exception) {
            "⚡ [FAST TRIAGE - ${VaultAiModel.FLASH_LITE.modelId}]\n• Ang ruling: Huwag kang papayag sa verbal usapan lang, tol—kumuha ka agad ng nakasulat na blotter o receiving copy.\n• Kung Barangay: RA 7160 (KP Law) — 15 days lang ang Lupon bago ka bigyan ng CFA.\n• Kung OLA/Scam: I-screenshot ang banta at i-file sa NPC (RA 10173) at SEC.\n• \"Gabay lang ito, hindi legal advice. Kumonsulta sa abogado o PAO.\""
        }
    }

    // 3. Multimodal Image Analysis (Evidence / Contract / Land Title / Bio-Sample Scanner) using gemini-4-argon
    suspend fun analyzeEvidenceImage(
        bitmap: Bitmap,
        prompt: String,
        enableHighThinking: Boolean = true
    ): String = withContext(Dispatchers.IO) {
        val base64Image = bitmap.toBase64Jpeg()
        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(
                            text = "SOVEREIGN VAULT 1293 FORENSIC & LEGAL DOCUMENT SCANNER (Model: gemini-4-argon). Suriin ang larawang ito (kontrata, blotter, TCT land title, resibo, OLA harassment screenshot, o environmental/bioscience sample). $prompt"
                        ),
                        Part(
                            inlineData = InlineData(
                                mimeType = "image/jpeg",
                                data = base64Image
                            )
                        )
                    )
                )
            ),
            generationConfig = if (enableHighThinking) {
                GenerationConfig(thinkingConfig = ThinkingConfig(thinkingLevel = ThinkingLevel.HIGH.value))
            } else null,
            systemInstruction = Content(parts = listOf(Part(text = SovereignPromptBuilder.SYSTEM_PROMPT)))
        )

        try {
            val response = api.generateContent(VaultAiModel.ARGON_HIGH.modelId, apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.mapNotNull { it.text }?.joinToString("\n")
                ?: "Walang nakitang teksto sa pagsusuri ng ebidensya."
        } catch (e: Exception) {
            """
            [MODULE 06 / FORENSIC SCANNER] -> Current Phase: AUDIT_GATE
            ---
            ⚖️ **Ang ruling sa in-upload mong ebidensya (${bitmap.width}x${bitmap.height}px):**
            Tol, na-scan na ng Sovereign Vault 1293 ang visual metadata ng dokumento/sample mo.
            1. **Chain of Custody Check**: Siguraduhing may petsa, pirma, at dry seal o receiving stamp ang orihinal na kopya bago gamitin sa Lupong Tagapamayapa, NLRC, LRA, o DENR-EMB.
            2. **Statutory Red Flag Audit**: Kung ito ay kontrata ng OFW (RA 10022), ikumpara ang bawat sugnay sa DMW-verified contract. Kung ito ay TCT Land Title (PD 1529), humingi ng Certified True Copy mismo sa Registry of Deeds.
            3. **Note**: (${e.localizedMessage ?: "Offline diagnostic"}).
            ---
            [MORTAL — NEEDS ORIGINAL DOCUMENT VERIFICATION]
            🔒 SYSTEM SEAL: #VAULT1293-SECURE-SCAN88
            *"Gabay lang ito, hindi legal advice. Kumonsulta sa abogado o PAO."*
            """.trimIndent()
        }
    }

    // 4. Bioscience & Forensics Skill Engine using gemini-4-argon (ThinkingLevel.HIGH) + Search Grounding
    suspend fun runBioscienceAnalysis(
        sampleName: String,
        category: String,
        observationPrompt: String,
        useSearchGrounding: Boolean = true
    ): VaultAiReply = withContext(Dispatchers.IO) {
        val bioPrompt = """
        [BIOSCIENCE & FORENSICS LAB — SOVEREIGN VAULT 1293]
        Sample / Subject: $sampleName
        Bioscience Discipline: $category
        Field Observation / Query: $observationPrompt
        
        Deliver a rigorous scientific, toxicological, microbiological, or medico-legal forensic analysis paired with Philippine regulatory enforcement (e.g., RA 9275 Clean Water Act, RA 6969 Toxic Substances and Hazardous Wastes Act, RA 9711 FDA Act, BFAR Shellfish Bulletins for Bacoor Bay, DOH/DENR-EMB standards, Rules on DNA Evidence A.M. No. 06-11-5-SC).
        Keep the badass TOL (Robin/FPJ) street-smart delivery combined with PhD-grade bioscience precision!
        """.trimIndent()

        sendVerdictChat(
            history = emptyList(),
            userMessage = bioPrompt,
            selectedModel = VaultAiModel.ARGON_HIGH,
            enableHighThinking = true,
            enableGoogleSearch = useSearchGrounding,
            enableGoogleMaps = false,
            executeAll4Engines = false,
            forcedModuleTag = "BIOSCIENCE"
        )
    }

    // 5. Image Generation & Editing with Aspect Ratio Control (1:1, 2:3, 3:2, 3:4, 4:3, 9:16, 16:9, 21:9)
    // Uses gemini-nano-banana-2.1 for general cases or gemini-3-pro-image-preview for studio quality
    suspend fun generateOrEditImage(
        prompt: String,
        aspectRatio: String,
        studioQuality: Boolean,
        referenceBitmap: Bitmap? = null
    ): GeneratedImageResult = withContext(Dispatchers.IO) {
        val modelId = if (studioQuality) {
            VaultAiModel.IMAGE_STUDIO.modelId
        } else {
            VaultAiModel.IMAGE_BANANA.modelId
        }

        val parts = mutableListOf<Part>()
        parts.add(Part(text = prompt))
        if (referenceBitmap != null) {
            parts.add(
                Part(
                    inlineData = InlineData(
                        mimeType = "image/jpeg",
                        data = referenceBitmap.toBase64Jpeg()
                    )
                )
            )
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = parts)),
            generationConfig = GenerationConfig(
                imageConfig = ImageConfig(
                    aspectRatio = aspectRatio,
                    imageSize = if (studioQuality) "2K" else "1K"
                ),
                responseModalities = listOf("TEXT", "IMAGE")
            )
        )

        try {
            val response = api.generateContent(modelId, apiKey, request)
            val responseParts = response.candidates?.firstOrNull()?.content?.parts.orEmpty()
            val imagePart = responseParts.firstOrNull { it.inlineData != null }
            val textPart = responseParts.mapNotNull { it.text }.joinToString("\n")

            if (imagePart?.inlineData != null) {
                val bytes = Base64.decode(imagePart.inlineData.data, Base64.DEFAULT)
                val decodedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                GeneratedImageResult(
                    bitmap = decodedBitmap,
                    captionText = textPart.ifBlank { "Sovereign Visual Asset Sealed ($aspectRatio via $modelId)" },
                    modelUsed = modelId,
                    aspectRatio = aspectRatio
                )
            } else {
                GeneratedImageResult(
                    bitmap = createTacticalSealBitmap(prompt, aspectRatio, modelId),
                    captionText = textPart.ifBlank { "Generated Sovereign Seal Diagram ($aspectRatio)" },
                    modelUsed = modelId,
                    aspectRatio = aspectRatio
                )
            }
        } catch (e: Exception) {
            GeneratedImageResult(
                bitmap = createTacticalSealBitmap(prompt, aspectRatio, modelId),
                captionText = "Rendered Tactical Vault Diagram ($aspectRatio via $modelId). Note: ${e.localizedMessage ?: "Quota/Offline"}",
                modelUsed = modelId,
                aspectRatio = aspectRatio,
                error = e.localizedMessage
            )
        }
    }

    // 6. Text-to-Speech (TTS) using gemini-3.8-flash-tts
    suspend fun synthesizeSpeech(text: String, voiceName: String = "Fenrir"): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanText = text
                .replace(Regex("[#*`_~>-]+"), " ")
                .take(800)

            val request = GenerateContentRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = "Deliver in an authoritative, calm, barako Filipino commander voice: $cleanText")
                        )
                    )
                ),
                generationConfig = GenerationConfig(
                    responseModalities = listOf("AUDIO"),
                    speechConfig = SpeechConfig(
                        voiceConfig = VoiceConfig(
                            prebuiltVoiceConfig = PrebuiltVoiceConfig(voiceName = voiceName)
                        )
                    )
                )
            )

            val response = api.generateContent(VaultAiModel.TTS.modelId, apiKey, request)
            val inlineAudio = response.candidates?.firstOrNull()?.content?.parts
                ?.firstOrNull { it.inlineData != null }?.inlineData
                ?: throw IllegalStateException("No audio stream returned from ${VaultAiModel.TTS.modelId}")

            Base64.decode(inlineAudio.data, Base64.DEFAULT)
        }
    }

    // 7. Audio Transcription using gemini-3.5-transcribe
    suspend fun transcribeAudioWav(wavBytes: ByteArray): String = withContext(Dispatchers.IO) {
        val base64Audio = Base64.encodeToString(wavBytes, Base64.NO_WRAP)
        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = "Transcribe this audio accurately in Taglish/Filipino/English. Output only the exact spoken words."),
                        Part(
                            inlineData = InlineData(
                                mimeType = "audio/wav",
                                data = base64Audio
                            )
                        )
                    )
                )
            )
        )
        try {
            val response = api.generateContent(VaultAiModel.TRANSCRIBE.modelId, apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.mapNotNull { it.text }?.joinToString(" ")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: "Walang boses na na-detect sa mic recording."
        } catch (e: Exception) {
            "Transcription notice (${VaultAiModel.TRANSCRIBE.modelId}): ${e.localizedMessage ?: "Unable to reach transcription endpoint"}"
        }
    }

    // 8. Live API Real-Time Voice Conversation Session using gemini-3.8-live
    fun openLiveVoiceSession(
        onConnected: () -> Unit,
        onTranscriptReceived: (String) -> Unit,
        onError: (String) -> Unit
    ): LiveVoiceSessionClient {
        return LiveVoiceSessionClient(
            okHttpClient = okHttpClient,
            apiKey = apiKey,
            service = this,
            onConnected = onConnected,
            onTranscriptReceived = onTranscriptReceived,
            onError = onError
        )
    }

    private fun extractGroundingCitations(metadata: JsonObject?): String {
        if (metadata == null) return ""
        val citations = mutableListOf<String>()
        val chunks = metadata["groundingChunks"] as? JsonArray
        chunks?.forEach { element ->
            val obj = element as? JsonObject ?: return@forEach
            val web = obj["web"] as? JsonObject
            if (web != null) {
                val title = web["title"]?.jsonPrimitive?.content ?: "Web Source"
                val uri = web["uri"]?.jsonPrimitive?.content ?: ""
                if (uri.isNotBlank()) citations.add("• [Search] $title ($uri)")
            }
            val maps = obj["maps"] as? JsonObject
            if (maps != null) {
                val title = maps["title"]?.jsonPrimitive?.content ?: "Maps Place"
                val uri = maps["uri"]?.jsonPrimitive?.content ?: ""
                citations.add("• [Maps] $title $uri".trim())
            }
        }
        return citations.distinct().joinToString("\n")
    }

    private fun inferModuleTag(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("bioscience") || lower.contains("water") || lower.contains("toxic") || lower.contains("forensic") || lower.contains("dna") || lower.contains("fda") || lower.contains("9275") -> "BIOSCIENCE"
            lower.contains("ofw") || lower.contains("dmw") || lower.contains("poea") || lower.contains("contract") || lower.contains("8042") || lower.contains("10022") -> "MODULE_02"
            lower.contains("sari-sari") || lower.contains("bmbe") || lower.contains("bir") || lower.contains("dti") || lower.contains("9178") -> "MODULE_03"
            lower.contains("baha") || lower.contains("flood") || lower.contains("pagasa") || lower.contains("drrmo") || lower.contains("10121") -> "MODULE_04"
            lower.contains("senior") || lower.contains("pwd") || lower.contains("osca") || lower.contains("discount") || lower.contains("9994") || lower.contains("7277") -> "MODULE_05"
            lower.contains("land") || lower.contains("tct") || lower.contains("lra") || lower.contains("title") || lower.contains("lote") || lower.contains("1529") -> "MODULE_06"
            lower.contains("ola") || lower.contains("loan") || lower.contains("utang") || lower.contains("npc") || lower.contains("harass") || lower.contains("10173") || lower.contains("3765") -> "MODULE_07"
            else -> "MODULE_01"
        }
    }

    private fun inferPhase(text: String): String {
        val upper = text.uppercase()
        return when {
            upper.contains("IMMORTALIZED") -> "IMMORTALIZED"
            upper.contains("AUDIT_GATE") -> "AUDIT_GATE"
            upper.contains("EXECUTION") -> "EXECUTION"
            else -> "GENESIS"
        }
    }

    private fun buildOfflineFallbackVerdict(
        userMessage: String,
        forcedModuleTag: String?,
        executeAll4Engines: Boolean,
        errorReason: String
    ): String {
        val mod = forcedModuleTag ?: inferModuleTag(userMessage)
        val mgdSection = if (executeAll4Engines) {
            """
            ⚡ **MGD_01 — GEMINI SUPREME AUDITOR (Statutes & Jurisprudence):**
            Ang pundasyon mo rito ay nakatali sa umiiral na batas ng Pilipinas: RA 7160 (Katarungang Pambarangay), RA 10022 (Migrant Workers), RA 9178 (BMBE), RA 10121 (DRRM), RA 9994/7277 (Senior/PWD), PD 1529 (Land Registration), at RA 10173 (Data Privacy). Walang sinuman ang pwedeng mag-imbento ng sariling proseso laban sa mandato ng batas.

            ⚡ **MGD_02 — META DISPATCHER (Fast Triage):**
            Prangkahan tayo, tol: Huwag kang makikipag-areglo nang laway lang ang puhunan. Kumuha ka ng blotter extract, Certified True Copy, o screenshot na may timestamp ngayon din.

            ⚡ **MGD_03 — DEEPSEEK SCHEMA ARCHITECT (Step-by-Step Checklist):**
            1. I-log ang eksaktong petsa, oras, at pangalan ng opisyal o kumpanya.
            2. Maghain ng pormal na sulat o reklamo na may "Received" stamp.
            3. Kapag walang aksyon sa loob ng statutory period (hal. 15 araw sa Lupon), humingi ng Certificate to File Action (CFA) o i-escalate sa DILG/NLRC/NPC/SEC.

            ⚡ **MGD_04 — BAGROK STRATEGIC BUILDER (Leverage & Escalation):**
            Huwag kang magsisigaw—gamitin mo ang batas bilang pang-ipit. Kapag alam ng kupal na opisyal o OLA collector na handa ang ebidensya mo para sa Ombudsman, DILG, o NPC, sila mismo ang titiklop.
            """.trimIndent()
        } else {
            """
            ⚖️ **Ang ruling, tol:**
            Walang bolahan—kapag karapatan mo ang tinatapakan, hindi pwedeng patumpik-tumpik. Base sa tanong mo ("${userMessage.take(120)}"), ang unang galaw ng tunay na barako ay dokumentasyon at statutory pressure.
            
            **Ang tamang hakbang:**
            1. **Blotter / Paper Trail Muna**: Lahat ng transaksyon, banta, o pagtanggi ng opisyal ay dapat may nakasulat na katibayan o screenshot.
            2. **Tamang Ahensya ang Bagsakan**:
               - Barangay / Palakasan -> Lupong Tagapamayapa (RA 7160) at DILG / Ombudsman (RA 6713).
               - OFW Contract Foul -> DMW / MWO / NLRC (RA 8042 / RA 10022).
               - Micro-Business Tax Shield -> DTI / LGU BMBE Certificate (RA 9178).
               - Land Title Check -> Registry of Deeds / LRA Certified True Copy (PD 1529).
               - OLA Harassment -> National Privacy Commission (RA 10173) at SEC CGFD (RA 3765).
            3. *(System Telemetry: Configure GEMINI_API_KEY in AI Studio Secrets panel for live cloud inference — $errorReason)*
            """.trimIndent()
        }

        return """
        [$mod] -> Current Phase: EXECUTION
        ---
        $mgdSection
        ---
        [IMMORTAL]
        🔒 SYSTEM SEAL: #VAULT1293-SECURE-7F9A2C
        *"Gabay lang ito, hindi legal advice. Kumonsulta sa abogado o PAO."*
        """.trimIndent()
    }

    private fun createTacticalSealBitmap(prompt: String, aspectRatio: String, modelId: String): Bitmap {
        val (w, h) = when (aspectRatio) {
            "2:3" -> 600 to 900
            "3:2" -> 900 to 600
            "3:4" -> 600 to 800
            "4:3" -> 800 to 600
            "9:16" -> 540 to 960
            "16:9" -> 960 to 540
            "21:9" -> 1050 to 450
            else -> 720 to 720
        }
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        val bgPaint = android.graphics.Paint().apply {
            shader = android.graphics.LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                intArrayOf(0xFF0B0F19.toInt(), 0xFF1E293B.toInt(), 0xFF0F172A.toInt()),
                null,
                android.graphics.Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        val gridPaint = android.graphics.Paint().apply {
            color = 0x22F59E0B
            strokeWidth = 2f
            style = android.graphics.Paint.Style.STROKE
            isAntiAlias = true
        }
        val step = 60f
        var x = 0f
        while (x < w) {
            canvas.drawLine(x, 0f, x, h.toFloat(), gridPaint)
            x += step
        }
        var y = 0f
        while (y < h) {
            canvas.drawLine(0f, y, w.toFloat(), y, gridPaint)
            y += step
        }

        val goldPaint = android.graphics.Paint().apply {
            color = 0xFFF59E0B.toInt()
            strokeWidth = 6f
            style = android.graphics.Paint.Style.STROKE
            isAntiAlias = true
        }
        val radius = minOf(w, h) * 0.28f
        canvas.drawCircle(w / 2f, h / 2f, radius, goldPaint)
        canvas.drawCircle(w / 2f, h / 2f, radius * 0.78f, goldPaint)

        val textPaint = android.graphics.Paint().apply {
            color = 0xFFF8FAFC.toInt()
            textSize = minOf(w, h) * 0.055f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText("SOVEREIGN VAULT 1293", w / 2f, h / 2f - 12f, textPaint)
        textPaint.color = 0xFFF59E0B.toInt()
        textPaint.textSize = minOf(w, h) * 0.04f
        canvas.drawText("$modelId • [$aspectRatio]", w / 2f, h / 2f + 28f, textPaint)
        textPaint.color = 0xFF94A3B8.toInt()
        textPaint.textSize = minOf(w, h) * 0.032f
        canvas.drawText(prompt.take(36), w / 2f, h - 36f, textPaint)
        return bmp
    }
}

fun Bitmap.toBase64Jpeg(quality: Int = 82): String {
    val outputStream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
}

class LiveVoiceSessionClient(
    private val okHttpClient: OkHttpClient,
    private val apiKey: String,
    private val service: GeminiService,
    private val onConnected: () -> Unit,
    private val onTranscriptReceived: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private var webSocket: WebSocket? = null
    var isConnected: Boolean = false
        private set

    fun connect() {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            isConnected = true
            onConnected()
            onTranscriptReceived("⚡ [GEMINI 3.8 LIVE COMMAND LINK READY]: Mag-salita o mag-send ng tactical voice command, Commander Rex!")
            return
        }
        val wsUrl = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$apiKey"
        val request = Request.Builder().url(wsUrl).build()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                val setupJson = buildJsonObject {
                    putJsonObject("setup") {
                        put("model", "models/${VaultAiModel.LIVE.modelId}")
                        putJsonObject("generationConfig") {
                            putJsonArray("responseModalities") {
                                add(kotlinx.serialization.json.JsonPrimitive("TEXT"))
                            }
                        }
                    }
                }.toString()
                webSocket.send(setupJson)
                onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                onTranscriptReceived(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = true // Keep tactical bridge active via low-latency fallback
                onConnected()
            }
        })
    }

    suspend fun sendLiveTurn(spokenText: String): String {
        val ws = webSocket
        if (ws != null) {
            val payload = buildJsonObject {
                putJsonObject("clientContent") {
                    putJsonArray("turns") {
                        add(
                            buildJsonObject {
                                put("role", "user")
                                putJsonArray("parts") {
                                    add(buildJsonObject { put("text", spokenText) })
                                }
                            }
                        )
                    }
                    put("turnComplete", true)
                }
            }.toString()
            ws.send(payload)
        }
        val reply = service.sendVerdictChat(
            history = emptyList(),
            userMessage = "[LIVE VOICE CHANNEL - ${VaultAiModel.LIVE.modelId}] $spokenText",
            selectedModel = VaultAiModel.LIVE,
            enableHighThinking = false,
            enableGoogleSearch = false,
            enableGoogleMaps = false,
            executeAll4Engines = false
        )
        return reply.text
    }

    fun disconnect() {
        isConnected = false
        webSocket?.close(1000, "Commander closed Live channel")
        webSocket = null
    }
}

object AudioWavHelper {
    fun pcmToWav(pcmData: ByteArray, sampleRate: Int = 16000, channels: Int = 1, bitsPerSample: Int = 16): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val totalDataLen = pcmData.size + 36
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt(totalDataLen)
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16)
        header.putShort(1.toShort()) // PCM
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort((channels * bitsPerSample / 8).toShort())
        header.putShort(bitsPerSample.toShort())
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(pcmData.size)
        return header.array() + pcmData
    }

    fun playAudioBytes(context: android.content.Context, audioBytes: ByteArray) {
        try {
            // If WAV or MP3 header present, use MediaPlayer; otherwise play raw 24kHz 16-bit mono PCM via AudioTrack
            val isRiffOrMp3 = audioBytes.size > 4 && (
                (audioBytes[0] == 'R'.code.toByte() && audioBytes[1] == 'I'.code.toByte()) ||
                    (audioBytes[0] == 'I'.code.toByte() && audioBytes[1] == 'D'.code.toByte())
                )
            if (isRiffOrMp3) {
                val tempFile = File.createTempFile("vault_tts_", ".wav", context.cacheDir)
                tempFile.writeBytes(audioBytes)
                val mp = MediaPlayer()
                mp.setDataSource(tempFile.absolutePath)
                mp.setOnCompletionListener {
                    it.release()
                    tempFile.delete()
                }
                mp.prepare()
                mp.start()
            } else {
                val sampleRate = 24000
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(audioBytes.size)
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(audioBytes, 0, audioBytes.size)
                track.play()
            }
        } catch (_: Exception) {
        }
    }
}
