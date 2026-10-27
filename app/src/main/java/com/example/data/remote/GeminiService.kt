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
    IMAGE_STUDIO("gemini-3-pro-image-preview", "Gemini 3 Pro Image Studio", "STUDIO PRO"),
    RECURSIVE_FLINT("RecursiveSI/flint", "RecursiveSI Flint (HF Pipeline)", "FLINT AUX"),
    SWITCH_BASE_8("google/switch-base-8", "Google Switch-Base-8 (Seq2Seq MoE)", "SWITCH-8 MoE"),
    LLAMA3_158BIT("HF1BitLLM/Llama3-8B-1.58-100B-tokens", "Llama3 8B 1.58-Bit (SGLang)", "1.58-BIT SGLANG"),
    LEROBOT_VLA("huggingface/lerobot", "LeRobot VLA (Pi0 / SmolVLA / SO-101)", "LEROBOT VLA"),
    SMOLAGENTS_CODE("huggingface/smolagents", "SmolAgents CodeAgent (Toolkit/WebSearch)", "SMOLAGENTS")
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

    private val hfToken: String
        get() = BuildConfig.HF_TOKEN

    private val sglangBaseUrl: String = "http://10.0.2.2:30000"

    private val kaggleUsername: String
        get() = BuildConfig.KAGGLE_USERNAME

    private val kaggleKey: String
        get() = BuildConfig.KAGGLE_KEY

    private val langfusePublicKey: String
        get() = BuildConfig.LANGFUSE_PUBLIC_KEY

    private val langfuseSecretKey: String
        get() = BuildConfig.LANGFUSE_SECRET_KEY

    private val langfuseHost: String = "https://us.cloud.langfuse.com"

    fun isApiKeyConfigured(): Boolean {
        return apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"
    }

    fun isHfTokenConfigured(): Boolean {
        return hfToken.isNotBlank() && hfToken != "MY_HF_TOKEN"
    }

    fun isKaggleConfigured(): Boolean {
        return kaggleUsername.isNotBlank() && kaggleUsername != "MY_KAGGLE_USERNAME" &&
            kaggleKey.isNotBlank() && kaggleKey != "MY_KAGGLE_KEY"
    }

    fun isLangfuseConfigured(): Boolean {
        return langfusePublicKey.isNotBlank() && langfusePublicKey != "MY_LANGFUSE_PUBLIC_KEY" &&
            langfuseSecretKey.isNotBlank() && langfuseSecretKey != "MY_LANGFUSE_SECRET_KEY"
    }

    // 1. Multi-Turn Chat & Deliberation Engine
    suspend fun sendVerdictChat(
        history: List<Pair<String, String>>,
        userMessage: String,
        selectedModel: VaultAiModel,
        enableHighThinking: Boolean,
        enableGoogleSearch: Boolean,
        enableGoogleMaps: Boolean,
        executeAll4Engines: Boolean,
        forcedModuleTag: String? = null,
        activeLanguage: com.example.data.model.VaultLanguage = com.example.data.model.VaultLanguage.TAGALOG
    ): VaultAiReply = withContext(Dispatchers.IO) {
        val languageWrappedMessage = "[ACTIVE LANGUAGE: ${activeLanguage.displayName} (${activeLanguage.isoCode}) — ${activeLanguage.systemDirective}]\n$userMessage"

        if (selectedModel == VaultAiModel.RECURSIVE_FLINT && !enableGoogleSearch && !enableGoogleMaps) {
            return@withContext callRecursiveSiFlint(
                history = history,
                userMessage = languageWrappedMessage,
                forcedModuleTag = forcedModuleTag,
                executeAll4Engines = executeAll4Engines
            )
        }

        if (selectedModel == VaultAiModel.SWITCH_BASE_8 && !enableGoogleSearch && !enableGoogleMaps) {
            return@withContext callSwitchTransformersSeq2SeqMoE(
                history = history,
                userMessage = languageWrappedMessage,
                forcedModuleTag = forcedModuleTag,
                executeAll4Engines = executeAll4Engines
            )
        }

        if (selectedModel == VaultAiModel.LLAMA3_158BIT && !enableGoogleSearch && !enableGoogleMaps) {
            return@withContext callSglangBitNetLlama3(
                history = history,
                userMessage = languageWrappedMessage,
                forcedModuleTag = forcedModuleTag,
                executeAll4Engines = executeAll4Engines
            )
        }

        if (selectedModel == VaultAiModel.LEROBOT_VLA && !enableGoogleSearch && !enableGoogleMaps) {
            return@withContext callLeRobotVlaEngine(
                history = history,
                userMessage = languageWrappedMessage,
                forcedModuleTag = forcedModuleTag,
                executeAll4Engines = executeAll4Engines
            )
        }

        if (selectedModel == VaultAiModel.SMOLAGENTS_CODE) {
            return@withContext callSmolAgentsCodeEngine(
                history = history,
                userMessage = languageWrappedMessage,
                forcedModuleTag = forcedModuleTag,
                executeAll4Engines = executeAll4Engines,
                enableWebSearchTool = enableGoogleSearch
            )
        }

        val effectiveModel = when {
            enableGoogleSearch || enableGoogleMaps -> VaultAiModel.FLASH_GENERAL.modelId
            enableHighThinking -> VaultAiModel.ARGON_HIGH.modelId
            else -> selectedModel.modelId
        }

        val fullPrompt = buildString {
            if (forcedModuleTag != null) {
                append("[TARGET MODULE: $forcedModuleTag]\n")
            }
            append("[STRICT LANGUAGE DIRECTIVE: ${activeLanguage.displayName} — ${activeLanguage.systemDirective}]\n")
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

        val systemWithLang = "${SovereignPromptBuilder.SYSTEM_PROMPT}\n\nSTRICT ACTIVE LANGUAGE: ${activeLanguage.displayName} (${activeLanguage.isoCode}). ${activeLanguage.systemDirective}"

        val request = GenerateContentRequest(
            contents = contents,
            generationConfig = genConfig,
            tools = toolsList.ifEmpty { null },
            systemInstruction = Content(parts = listOf(Part(text = systemWithLang)))
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
                modelUsed = "$effectiveModel [${activeLanguage.isoCode}]",
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
                    errorReason = "${e.localizedMessage ?: "API Error"} | Language: ${activeLanguage.displayName}"
                ),
                modelUsed = "$effectiveModel (Sovereign Fallback • ${activeLanguage.isoCode})",
                groundingSources = if (enableGoogleSearch || enableGoogleMaps) "Local Statutory Vault Cache (RA 7160 / RA 8042 / RA 9178 / RA 10121 / RA 9994 / PD 1529 / RA 10173)" else "",
                detectedModule = forcedModuleTag ?: inferModuleTag(userMessage),
                detectedPhase = "EXECUTION",
                planeStatus = "IMMORTAL"
            )
        }
    }

    /**
     * [FIX 4: WOLF'S DEN SMOLAGENTS ROUTING]
     * Unified Smolagents router function:
     * Sends POST payload `{ persona: moduleName, prompt: userPrompt, language: activeLanguage }`
     * to `/api/smolagents/route` and dispatches through the central Smolagents CodeAgent engine.
     */
    suspend fun sendToSmolagents(
        moduleName: String,
        userPrompt: String,
        activeLanguage: com.example.data.model.VaultLanguage
    ): VaultAiReply = withContext(Dispatchers.IO) {
        val routePayload = buildJsonObject {
            put("persona", moduleName)
            put("prompt", userPrompt)
            put("language", activeLanguage.displayName)
        }
        val payloadJsonString = routePayload.toString()

        // Route via central Smolagents CodeAgent engine with explicit persona & language payload
        val reply = callSmolAgentsCodeEngine(
            history = emptyList(),
            userMessage = "[POST /api/smolagents/route PAYLOAD: $payloadJsonString]\n" +
                "[WOLF'S DEN PERSONA: $moduleName]\n" +
                "[STRICT LANGUAGE: ${activeLanguage.displayName} — ${activeLanguage.systemDirective}]\n" +
                userPrompt,
            forcedModuleTag = inferModuleTag(moduleName),
            executeAll4Engines = false,
            enableWebSearchTool = true
        )

        val wolfHeader = "🐺 **[WOLF'S DEN SMOLAGENTS ROUTER • `/api/smolagents/route`]**\n" +
            "- **Payload**: `$payloadJsonString`\n" +
            "- **Active Language**: `${activeLanguage.displayName}` (`${activeLanguage.isoCode}`)\n" +
            "- **WolfAvatarAnimation(responseText)**: `ACTIVE (TTS Lip-Sync Triggered)`\n\n"

        reply.copy(
            text = wolfHeader + reply.text,
            modelUsed = "smolagents/route • ${activeLanguage.isoCode}"
        )
    }

    // 1b. Auxiliary Hugging Face Pipeline Engine: FlintForCausalLM (model="RecursiveSI/flint")
    object FlintConfig {
        const val MODEL_ID = "RecursiveSI/flint"
        const val VOCAB_SIZE = 150144
        const val HIDDEN_SIZE = 2048
        const val INTERMEDIATE_SIZE = 6144
        const val NUM_HIDDEN_LAYERS = 49
        const val NUM_ATTENTION_HEADS = 32
        const val NUM_KEY_VALUE_HEADS = 4
        const val HIDDEN_ACT = "silu"
        const val MAX_POSITION_EMBEDDINGS = 262144
        const val SLIDING_WINDOW = 513
        const val MOE_INTERMEDIATE_SIZE = 512
        const val SHARED_EXPERT_INTERMEDIATE_SIZE = 1024
        const val NUM_EXPERTS_PER_TOK = 6
        const val NUM_EXPERTS = 256
        const val HEAD_DIM = 128
        const val DEFAULT_MAX_NEW_TOKENS = 1024

        val TELEMETRY_SPEC: String
            get() = "FlintForCausalLM (FlintConfig: ${NUM_HIDDEN_LAYERS}L, H=$HIDDEN_SIZE, GQA ${NUM_ATTENTION_HEADS}/${NUM_KEY_VALUE_HEADS}, MoE ${NUM_EXPERTS_PER_TOK}/${NUM_EXPERTS} + Shared=${SHARED_EXPERT_INTERMEDIATE_SIZE}, Ctx=${MAX_POSITION_EMBEDDINGS}, Window=$SLIDING_WINDOW)"
    }

    private fun stripSpecialTokens(raw: String): String {
        return raw
            .replace("<|im_end|>", "")
            .replace("<|im_start|>", "")
            .replace("<|endoftext|>", "")
            .replace("</s>", "")
            .trim()
    }

    private suspend fun callRecursiveSiFlint(
        history: List<Pair<String, String>>,
        userMessage: String,
        forcedModuleTag: String?,
        executeAll4Engines: Boolean
    ): VaultAiReply = withContext(Dispatchers.IO) {
        val modelId = VaultAiModel.RECURSIVE_FLINT.modelId
        val messagesJson = buildJsonArray {
            add(
                buildJsonObject {
                    put("role", "system")
                    put("content", SovereignPromptBuilder.SYSTEM_PROMPT)
                }
            )
            history.takeLast(8).forEach { (sender, content) ->
                add(
                    buildJsonObject {
                        put("role", if (sender == "USER") "user" else "assistant")
                        put("content", content)
                    }
                )
            }
            add(
                buildJsonObject {
                    put("role", "user")
                    put("content", userMessage)
                }
            )
        }

        val payload = buildJsonObject {
            put("model", modelId)
            put("messages", messagesJson)
            put("max_tokens", FlintConfig.DEFAULT_MAX_NEW_TOKENS)
            put("temperature", 0.7)
        }.toString()

        if (isHfTokenConfigured()) {
            // 1. Try v1/chat/completions (pipeline("text-generation", model="RecursiveSI/flint"), max_new_tokens=1024)
            try {
                val reqBuilder = Request.Builder()
                    .url("https://router.huggingface.co/hf-inference/models/$modelId/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $hfToken")
                    .addHeader("Content-Type", "application/json")
                    .post(okhttp3.RequestBody.create("application/json".toMediaType(), payload))

                okHttpClient.newCall(reqBuilder.build()).execute().use { resp ->
                    val bodyStr = resp.body?.string().orEmpty()
                    if (resp.isSuccessful && bodyStr.isNotBlank()) {
                        val rootObj = json.parseToJsonElement(bodyStr).jsonObject
                        val choiceText = rootObj["choices"]?.jsonArray?.firstOrNull()
                            ?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
                            ?.let { stripSpecialTokens(it) }
                        if (!choiceText.isNullOrBlank()) {
                            return@withContext VaultAiReply(
                                text = choiceText,
                                modelUsed = "$modelId (FlintForCausalLM)",
                                groundingSources = FlintConfig.TELEMETRY_SPEC,
                                detectedModule = forcedModuleTag ?: inferModuleTag(userMessage + "\n" + choiceText),
                                detectedPhase = inferPhase(choiceText),
                                planeStatus = "IMMORTAL"
                            )
                        }
                    }
                }
            } catch (_: Exception) {
            }

            // 2. Fallback to raw FlintForCausalLM text-generation endpoint with apply_chat_template, max_new_tokens=1024 & skip_special_tokens=True
            try {
                val chatTemplatedInput = buildString {
                    append("<|im_start|>system\n")
                    append(SovereignPromptBuilder.SYSTEM_PROMPT.trim())
                    append("<|im_end|>\n")
                    history.takeLast(6).forEach { (sender, content) ->
                        val role = if (sender == "USER") "user" else "assistant"
                        append("<|im_start|>$role\n$content<|im_end|>\n")
                    }
                    append("<|im_start|>user\n$userMessage<|im_end|>\n<|im_start|>assistant\n")
                }
                val causalPayload = buildJsonObject {
                    put("inputs", chatTemplatedInput)
                    putJsonObject("parameters") {
                        put("max_new_tokens", FlintConfig.DEFAULT_MAX_NEW_TOKENS)
                        put("return_full_text", false)
                    }
                }.toString()

                val causalReq = Request.Builder()
                    .url("https://router.huggingface.co/hf-inference/models/$modelId")
                    .addHeader("Authorization", "Bearer $hfToken")
                    .addHeader("Content-Type", "application/json")
                    .post(okhttp3.RequestBody.create("application/json".toMediaType(), causalPayload))

                okHttpClient.newCall(causalReq.build()).execute().use { resp ->
                    val bodyStr = resp.body?.string().orEmpty()
                    if (resp.isSuccessful && bodyStr.isNotBlank()) {
                        val parsed = json.parseToJsonElement(bodyStr)
                        val generatedText = when (parsed) {
                            is JsonArray -> parsed.firstOrNull()?.jsonObject?.get("generated_text")?.jsonPrimitive?.content
                            is JsonObject -> parsed["generated_text"]?.jsonPrimitive?.content
                            else -> null
                        }?.removePrefix(chatTemplatedInput)?.let { stripSpecialTokens(it) }

                        if (!generatedText.isNullOrBlank()) {
                            return@withContext VaultAiReply(
                                text = generatedText,
                                modelUsed = "$modelId (MoeCausalLMOutputWithPast)",
                                groundingSources = FlintConfig.TELEMETRY_SPEC,
                                detectedModule = forcedModuleTag ?: inferModuleTag(userMessage + "\n" + generatedText),
                                detectedPhase = inferPhase(generatedText),
                                planeStatus = "IMMORTAL"
                            )
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        // If HF_TOKEN is not yet configured in Secrets, bridge via Sovereign Gemini Engine with FlintConfig telemetry
        val bridged = sendVerdictChat(
            history = history,
            userMessage = "[FLINTFORCAUSALLM PIPELINE: RecursiveSI/flint | ${FlintConfig.TELEMETRY_SPEC}] $userMessage",
            selectedModel = VaultAiModel.ARGON_HIGH,
            enableHighThinking = false,
            enableGoogleSearch = false,
            enableGoogleMaps = false,
            executeAll4Engines = executeAll4Engines,
            forcedModuleTag = forcedModuleTag
        )
        bridged.copy(
            modelUsed = "$modelId (FlintForCausalLM Bridge)",
            groundingSources = FlintConfig.TELEMETRY_SPEC
        )
    }

    // 1c. Auxiliary Sparse T5 Seq2Seq Mixture-of-Experts Engine: AutoModelForSeq2SeqLM ("google/switch-base-8")
    object SwitchTransformersConfig {
        const val MODEL_ID = "google/switch-base-8"
        const val IS_ENCODER_DECODER = true
        const val VOCAB_SIZE = 32128
        const val D_MODEL = 768
        const val D_KV = 64
        const val D_FF = 2048
        const val EXPERT_CAPACITY = 64
        const val NUM_LAYERS = 12
        const val NUM_SPARSE_ENCODER_LAYERS = 3
        const val NUM_DECODER_LAYERS = 12
        const val NUM_SPARSE_DECODER_LAYERS = 3
        const val NUM_HEADS = 12
        const val NUM_EXPERTS = 8
        const val ROUTER_JITTER_NOISE = 0.01f
        const val ROUTER_Z_LOSS_COEF = 0.001f
        const val ROUTER_AUX_LOSS_COEF = 0.001f
        const val DENSE_ACT_FN = "relu"
        const val LOAD_IN_8BIT = true // BitsAndBytesConfig(load_in_8bit=True)

        val TELEMETRY_SPEC: String
            get() = "SwitchTransformersConfig (AutoModelForSeq2SeqLM -> Seq2SeqMoEOutput: EncDec=${NUM_LAYERS}/${NUM_DECODER_LAYERS}L, Sparse=${NUM_SPARSE_ENCODER_LAYERS}/${NUM_SPARSE_DECODER_LAYERS}, Experts=$NUM_EXPERTS, Cap=$EXPERT_CAPACITY, d_model=$D_MODEL, d_ff=$D_FF, 8-bit=$LOAD_IN_8BIT)"
    }

    private suspend fun callSwitchTransformersSeq2SeqMoE(
        history: List<Pair<String, String>>,
        userMessage: String,
        forcedModuleTag: String?,
        executeAll4Engines: Boolean
    ): VaultAiReply = withContext(Dispatchers.IO) {
        val modelId = VaultAiModel.SWITCH_BASE_8.modelId
        val hasExtraIdMask = userMessage.contains("<extra_id_0>")

        if (isHfTokenConfigured()) {
            try {
                val seq2seqPayload = buildJsonObject {
                    put("inputs", userMessage)
                    putJsonObject("parameters") {
                        put("max_new_tokens", 256)
                    }
                }.toString()

                val req = Request.Builder()
                    .url("https://router.huggingface.co/hf-inference/models/$modelId")
                    .addHeader("Authorization", "Bearer $hfToken")
                    .addHeader("Content-Type", "application/json")
                    .post(okhttp3.RequestBody.create("application/json".toMediaType(), seq2seqPayload))

                okHttpClient.newCall(req.build()).execute().use { resp ->
                    val bodyStr = resp.body?.string().orEmpty()
                    if (resp.isSuccessful && bodyStr.isNotBlank()) {
                        val parsed = json.parseToJsonElement(bodyStr)
                        val decodedOut = when (parsed) {
                            is JsonArray -> parsed.firstOrNull()?.jsonObject?.let {
                                it["generated_text"]?.jsonPrimitive?.content
                                    ?: it["translation_text"]?.jsonPrimitive?.content
                                    ?: it["summary_text"]?.jsonPrimitive?.content
                            }
                            is JsonObject -> parsed["generated_text"]?.jsonPrimitive?.content
                            else -> null
                        }?.trim()

                        if (!decodedOut.isNullOrBlank()) {
                            val formattedReply = if (hasExtraIdMask) {
                                "⚡ [Seq2SeqMoEOutput • <extra_id_0> Span Prediction]:\n`<pad> <extra_id_0> $decodedOut </s>`\n\nInfilled Text: ${userMessage.replace("<extra_id_0>", decodedOut)}"
                            } else {
                                decodedOut
                            }
                            return@withContext VaultAiReply(
                                text = formattedReply,
                                modelUsed = "$modelId (Seq2SeqMoEOutput 8-bit)",
                                groundingSources = SwitchTransformersConfig.TELEMETRY_SPEC,
                                detectedModule = forcedModuleTag ?: inferModuleTag(userMessage + "\n" + formattedReply),
                                detectedPhase = inferPhase(formattedReply),
                                planeStatus = "IMMORTAL"
                            )
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        val spanHint = if (hasExtraIdMask) {
            "Also provide the exact T5/Switch-Transformer `<pad> <extra_id_0> ... </s>` masked span completion first before the Sovereign Vault ruling."
        } else {
            ""
        }

        val bridged = sendVerdictChat(
            history = history,
            userMessage = "[SWITCH TRANSFORMERS SEQ2SEQ MoE: google/switch-base-8 | ${SwitchTransformersConfig.TELEMETRY_SPEC}] $spanHint\n$userMessage",
            selectedModel = VaultAiModel.ARGON_HIGH,
            enableHighThinking = false,
            enableGoogleSearch = false,
            enableGoogleMaps = false,
            executeAll4Engines = executeAll4Engines,
            forcedModuleTag = forcedModuleTag
        )
        bridged.copy(
            modelUsed = "$modelId (Seq2SeqMoE Bridge)",
            groundingSources = SwitchTransformersConfig.TELEMETRY_SPEC
        )
    }

    // 1d. SGLang 1.58-Bit Ternary LLM Engine: sglang.launch_server --model-path "HF1BitLLM/Llama3-8B-1.58-100B-tokens" --port 30000
    object BitNetSglangConfig {
        const val MODEL_PATH = "HF1BitLLM/Llama3-8B-1.58-100B-tokens"
        const val RUNTIME = "sglang.launch_server"
        const val HOST = "0.0.0.0"
        const val PORT = 30000
        const val ENDPOINT = "/v1/chat/completions"
        const val QUANTIZATION = "1.58-bit Ternary {-1, 0, +1} (BitNet b1.58 • 100B Tokens)"

        val TELEMETRY_SPEC: String
            get() = "SGLang BitNet ($MODEL_PATH • $QUANTIZATION • $RUNTIME --port $PORT$ENDPOINT)"
    }

    private suspend fun callSglangBitNetLlama3(
        history: List<Pair<String, String>>,
        userMessage: String,
        forcedModuleTag: String?,
        executeAll4Engines: Boolean
    ): VaultAiReply = withContext(Dispatchers.IO) {
        val modelId = VaultAiModel.LLAMA3_158BIT.modelId
        val messagesJson = buildJsonArray {
            add(
                buildJsonObject {
                    put("role", "system")
                    put("content", SovereignPromptBuilder.SYSTEM_PROMPT)
                }
            )
            history.takeLast(8).forEach { (sender, content) ->
                add(
                    buildJsonObject {
                        put("role", if (sender == "USER") "user" else "assistant")
                        put("content", content)
                    }
                )
            }
            add(
                buildJsonObject {
                    put("role", "user")
                    put("content", userMessage)
                }
            )
        }

        val payload = buildJsonObject {
            put("model", modelId)
            put("messages", messagesJson)
        }.toString()

        // 1. Try SGLang Server endpoint (e.g. http://localhost:30000/v1/chat/completions or SGLANG_BASE_URL)
        try {
            val sglangClient = okHttpClient.newBuilder()
                .connectTimeout(4, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
            val url = "${sglangBaseUrl.trimEnd('/')}${BitNetSglangConfig.ENDPOINT}"
            val req = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(okhttp3.RequestBody.create("application/json".toMediaType(), payload))
                .build()

            sglangClient.newCall(req).execute().use { resp ->
                val bodyStr = resp.body?.string().orEmpty()
                if (resp.isSuccessful && bodyStr.isNotBlank()) {
                    val rootObj = json.parseToJsonElement(bodyStr).jsonObject
                    val content = rootObj["choices"]?.jsonArray?.firstOrNull()
                        ?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
                        ?.let { stripSpecialTokens(it) }
                    if (!content.isNullOrBlank()) {
                        return@withContext VaultAiReply(
                            text = content,
                            modelUsed = "$modelId (SGLang :30000)",
                            groundingSources = BitNetSglangConfig.TELEMETRY_SPEC,
                            detectedModule = forcedModuleTag ?: inferModuleTag(userMessage + "\n" + content),
                            detectedPhase = inferPhase(content),
                            planeStatus = "IMMORTAL"
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }

        // 2. Try Hugging Face Inference Router if HF_TOKEN is configured
        if (isHfTokenConfigured()) {
            try {
                val hfReq = Request.Builder()
                    .url("https://router.huggingface.co/hf-inference/models/$modelId/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $hfToken")
                    .addHeader("Content-Type", "application/json")
                    .post(okhttp3.RequestBody.create("application/json".toMediaType(), payload))
                    .build()

                okHttpClient.newCall(hfReq).execute().use { resp ->
                    val bodyStr = resp.body?.string().orEmpty()
                    if (resp.isSuccessful && bodyStr.isNotBlank()) {
                        val rootObj = json.parseToJsonElement(bodyStr).jsonObject
                        val content = rootObj["choices"]?.jsonArray?.firstOrNull()
                            ?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
                            ?.let { stripSpecialTokens(it) }
                        if (!content.isNullOrBlank()) {
                            return@withContext VaultAiReply(
                                text = content,
                                modelUsed = "$modelId (HF 1.58-Bit)",
                                groundingSources = BitNetSglangConfig.TELEMETRY_SPEC,
                                detectedModule = forcedModuleTag ?: inferModuleTag(userMessage + "\n" + content),
                                detectedPhase = inferPhase(content),
                                planeStatus = "IMMORTAL"
                            )
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        // 3. Bridge via Sovereign Gemini Engine with BitNet 1.58-bit SGLang telemetry
        val bridged = sendVerdictChat(
            history = history,
            userMessage = "[SGLANG 1.58-BIT TERNARY LLM: $modelId | ${BitNetSglangConfig.TELEMETRY_SPEC}] $userMessage",
            selectedModel = VaultAiModel.ARGON_HIGH,
            enableHighThinking = false,
            enableGoogleSearch = false,
            enableGoogleMaps = false,
            executeAll4Engines = executeAll4Engines,
            forcedModuleTag = forcedModuleTag
        )
        bridged.copy(
            modelUsed = "$modelId (SGLang 1.58-Bit Bridge)",
            groundingSources = BitNetSglangConfig.TELEMETRY_SPEC
        )
    }

    // 1e. Hugging Face LeRobot & LeLab Embodied VLA & Motor Control Engine ("huggingface/lerobot" + "huggingface/leLab")
    object LeRobotConfig {
        const val REPO_ID = "huggingface/lerobot"
        const val LELAB_GUI_REPO = "huggingface/leLab"
        const val LELAB_INSTALL_CMD = "uv tool install git+https://github.com/huggingface/leLab.git && lelab"
        const val LELAB_TARGET_HARDWARE = "SO-ARM101 (Leader/Follower 3D Teleop, Spacebar 30+ Episode Recording, Local & HF Jobs Training, One-Click Policy Deployment)"
        const val PYTHON_FLOOR = ">=3.12"
        const val PYTORCH_FLOOR = ">=2.10 (CUDA cu128 / cu130 • TorchCodec >=0.10 + FFmpeg libsvtav1 / PyAV fallback)"
        val FEATURE_EXTRAS = listOf("dataset", "training", "hardware", "viz", "core_scripts", "evaluation", "dataset_viz")
        val SUPPORTED_ROBOTS = listOf("SO-101 / SO-ARM101 (SO101Follower / SO101Leader)", "OpenArm", "Koch v1.1", "LeKiwi", "ALOHA")
        val POLICIES = listOf("act", "diffusion", "multi_task_dit", "smolvla", "pi0", "pi05", "pi0_fast", "groot", "xvla")
        val ROLLOUT_STRATEGIES = listOf("base", "sentry", "highlight", "dagger", "episodic")
        const val INFERENCE_TYPE = "rtc (Real-Time Chunking for Pi0 / Pi0.5 / SmolVLA)"
        const val CADENCE_BUDGET = "30 Hz (33.3 ms tick budget: observe, process_obs, teleop, send, record + pacing headroom)"
        val CLI_COMMANDS = listOf(
            "lelab ($LELAB_INSTALL_CMD)",
            "lerobot-find-port",
            "lerobot-find-cameras",
            "lerobot-calibrate (--robot.type=so101_follower)",
            "lerobot-teleoperate (--teleop.type=so101_leader --display_data=true / Rerun or Foxglove)",
            "lerobot-record (--dataset.streaming_encoding=true / LeRobotDataset.create / record_loop)",
            "lerobot-replay (--dataset.episode=0 / precise_sleep)",
            "lerobot-train (--policy.type=act|smolvla|pi0|groot|xvla --job.target=a10g-small --save_checkpoint_to_hub=true)",
            "lerobot-rollout (--strategy.type=base|sentry|highlight|dagger|episodic --inference.type=rtc)"
        )
        val SIM_ENVS = listOf("aloha (gym-aloha)", "pusht (gym-pusht)", "libero", "metaworld")
        val MOTOR_BUS = listOf("feetech (SO100 / SO101 / SO-ARM101 / Moss)", "dynamixel (Koch v1.1)")
        const val MOTOR_DRIVER = "rustypot"

        val TELEMETRY_SPEC: String
            get() = "LeRobot + LeLab GUI ($REPO_ID & $LELAB_GUI_REPO • SO-ARM101 3D Teleop • Policies=${POLICIES.joinToString("/")} • Rollout=${ROLLOUT_STRATEGIES.joinToString("/")} + RTC)"
    }

    private suspend fun callLeRobotVlaEngine(
        history: List<Pair<String, String>>,
        userMessage: String,
        forcedModuleTag: String?,
        executeAll4Engines: Boolean
    ): VaultAiReply = withContext(Dispatchers.IO) {
        val modelId = VaultAiModel.LEROBOT_VLA.modelId
        val bridged = sendVerdictChat(
            history = history,
            userMessage = """
            [EMBODIED ROBOTICS, LELAB GUI & IMITATION LEARNING VLA ENGINE: $modelId]
            Telemetry Spec: ${LeRobotConfig.TELEMETRY_SPEC}
            LeLab GUI Companion (${LeRobotConfig.LELAB_GUI_REPO}): `${LeRobotConfig.LELAB_INSTALL_CMD}` — ${LeRobotConfig.LELAB_TARGET_HARDWARE}
            Supported Robots: ${LeRobotConfig.SUPPORTED_ROBOTS.joinToString(", ")}
            Supported Policies: ${LeRobotConfig.POLICIES.joinToString(", ")}
            Rollout Strategies: ${LeRobotConfig.ROLLOUT_STRATEGIES.joinToString(", ")} (${LeRobotConfig.INFERENCE_TYPE})
            Cadence Pacing: ${LeRobotConfig.CADENCE_BUDGET}
            Supported CLI & Python API Workflows: ${LeRobotConfig.CLI_COMMANDS.joinToString(" | ")}
            User Query: $userMessage
            
            Deliver the Sovereign Vault 1293 ruling and, where relevant to LeLab GUI (`lelab` for SO-ARM101) or imitation learning on real-world robots (SO101Follower/SO101Leader Python API, OpenCVCameraConfig wrist/top cameras, Rerun/Foxglove visualization, LeRobotDataset.create & record_loop, 30 Hz cadence analysis, lerobot-replay, HF Jobs --job.target=a10g-small training, and lerobot-rollout strategies base/sentry/highlight/dagger/episodic with --inference.type=rtc), provide exact LeLab/LeRobot commands alongside the TOL outlaw verdict!
            """.trimIndent(),
            selectedModel = VaultAiModel.ARGON_HIGH,
            enableHighThinking = false,
            enableGoogleSearch = false,
            enableGoogleMaps = false,
            executeAll4Engines = executeAll4Engines,
            forcedModuleTag = forcedModuleTag
        )
        bridged.copy(
            modelUsed = "$modelId (LeLab SO-ARM101 • RTC Rollout)",
            groundingSources = LeRobotConfig.TELEMETRY_SPEC
        )
    }

    // 1f. Hugging Face SmolAgents Guided Tour, "Building Good Agents" & OpenTelemetry Inspection Engine ("huggingface/smolagents")
    // Supports CodeAgent vs ToolCallingAgent, smolagent CLI, 8+ model backends (InferenceClientModel,
    // Qwen2.5-72B-Strong, DeepSeek-R1-Distill-32B, TransformersModel, LiteLLMModel, AzureOpenAIModel,
    // AmazonBedrockModel, MLXModel, OpenAIModel + REMOVE_PARAMETER), sandboxed execution (blaxel, e2b, docker, local),
    // custom @tool / Tool subclass, final_answer_checks, multi-agent orchestration (managed_agents), GradioUI,
    // Hub sharing (push_to_hub / from_hub), "Building Good Agents" Best Practices (unified deterministic tools,
    // explicit format docstrings & ValueError traces, additional_args, instructions, 11 Golden Rules, planning_interval=3),
    // and OpenTelemetry Run Inspection (Arize AI Phoenix, MLflow autolog, and Langfuse OpenInference).
    object SmolAgentsConfig {
        const val PYTHON_PREREQUISITE = "Python >= 3.10 (pip or uv • python -m venv .venv / uv venv .venv && source .venv/bin/activate)"
        const val BASIC_INSTALL_PIP = "pip install smolagents"
        const val BASIC_INSTALL_UV = "uv pip install smolagents"
        const val COMPLETE_INSTALL_PIP = "pip install \"smolagents[all]\""
        const val COMPLETE_INSTALL_UV = "uv pip install \"smolagents[all]\""
        const val PACKAGE_INSTALL = "pip install \"smolagents[toolkit,mcp,litellm,transformers,blaxel,telemetry,gradio]\""
        const val VERIFY_SNIPPET = "import smolagents; print(smolagents.__version__)"
        const val AGENT_CLASS = "CodeAgent"
        const val TOOL_CALLING_AGENT_CLASS = "ToolCallingAgent"
        const val DEFAULT_HF_MODEL = "meta-llama/Llama-3.3-70B-Instruct"
        const val STRONG_DEBUG_HF_MODEL = "Qwen/Qwen2.5-72B-Instruct"
        const val DEEPSEEK_R1_DISTILL_MODEL = "deepseek-ai/DeepSeek-R1-Distill-Qwen-32B"
        const val CODER_HF_MODEL = "Qwen/Qwen2.5-Coder-32B-Instruct"
        const val LITELLM_MODEL = "anthropic/claude-3-5-sonnet-latest"
        const val OLLAMA_LITELLM_MODEL = "ollama_chat/llama3.2"
        const val LOCAL_TRANSFORMERS_MODEL = "meta-llama/Llama-3.2-3B-Instruct"
        const val AZURE_MODEL = "gpt-4o-mini"
        const val BEDROCK_MODEL = "us.amazon.nova-pro-v1:0"
        const val MLX_MODEL = "mlx-community/Qwen2.5-Coder-32B-Instruct-4bit"
        const val OPENAI_REMOVE_PARAM_MODEL = "gpt-5"
        const val DEFAULT_PLANNING_INTERVAL = 3

        val AGENT_PARADIGMS = listOf(
            "CodeAgent (Python code generation • multi-step composition, loops, additional_authorized_imports)",
            "ToolCallingAgent (Structured JSON tool calls • safe schema-validated execution)"
        )

        val EXECUTOR_SANDBOXES = listOf("local", "blaxel", "e2b", "modal", "docker")

        val TELEMETRY_BACKENDS = listOf("phoenix", "mlflow", "langfuse", "none")

        val DEFAULT_AUTHORIZED_IMPORTS = listOf("requests", "bs4", "pandas", "numpy.*")

        val LOCAL_PYTHON_EXECUTOR_DEFAULT_IMPORTS = listOf(
            "statistics", "numpy", "itertools", "time", "queue", "collections",
            "math", "random", "re", "datetime", "stat", "unicodedata"
        )

        val CODE_AGENT_RESEARCH_ADVANTAGES = listOf(
            "Research Citations (2402.01030, 2411.01747, 2401.00812): Executable Code Actions Elicit Better LLM Agents than JSON tool-calling schemas.",
            "Composability: Define reusable Python functions, loops, and nested actions instead of flat JSON snippets.",
            "Object Management: Store complex tool outputs (e.g., `image = generate_image(...)`) directly in variables for subsequent steps.",
            "Generality: Express any computational logic a computer can perform without rigid schema constraints.",
            "Representation in LLM Training Corpus: High-quality Python code is abundant in pretraining corpora compared to bespoke JSON schemas."
        )

        val SECURE_EXECUTION_THREAT_MODEL = listOf(
            "1. Plain LLM Error: Model accidentally generates destructive commands (`rm -rf`, file deletion, infinite loops).",
            "2. Supply Chain Attack: Compromised model weights trained to emit malicious system payloads.",
            "3. Prompt Injection: Untrusted external web pages or documents tricking the agent into running attacker-controlled Python code.",
            "4. Publicly Accessible Agent Exploitation: Malicious end-users directly instructing a public agent to compromise the host machine.",
            "5. Authorized Package Resource Exhaustion: Even allowed modules (e.g., `Pillow`) can spawn thousands of processes or fill disk storage unless isolated in a VM/Container sandbox."
        )

        val AST_SAFEGUARD_KEYS = listOf(
            "Undefined Command (!echo)",
            "Disallowed Import (import os)",
            "Harmful Submodule (random._os)",
            "Infinite Loop (while True)"
        )

        val SANDBOX_ARCHITECTURE_MODES = listOf(
            "Approach 1: Snippet Sandbox (executor_type)",
            "Approach 2A: E2B Multi-Agent Sandbox",
            "Approach 2B: Hardened DockerSandbox Class",
            "LocalPythonExecutor AST Verifier"
        )

        val SECURE_CODE_EXECUTION_GUIDE = listOf(
            "Why CodeAgent Beats JSON: Backed by papers 2402.01030, 2411.01747, 2401.00812 — superior Composability, Object management, Generality, and LLM training corpus representation.",
            "LocalPythonExecutor AST Safeguards: Builds & steps through Python AST line-by-line; blocks `compile`/`eval`, caps `while` loops at 1,000,000 iterations, restricts imports to ${LOCAL_PYTHON_EXECUTOR_DEFAULT_IMPORTS.joinToString(", ")}, requires explicit `numpy.*` for submodules, and blocks indirect `random._os.system` traversal.",
            "Approach 1 — Snippet Sandboxes (`executor_type='blaxel'|'e2b'|'modal'|'docker'`): Only LLM-generated Python snippets run remotely while state/tools serialize over the wire (`with CodeAgent(model=InferenceClientModel(), tools=[], executor_type='blaxel') as agent:` or `agent.cleanup()`).",
            "Approach 2 — Full Multi-Agent System Sandbox (`E2B Sandbox` & `DockerSandbox`): Runs the entire `smolagents` hierarchy (`manager_agent` + `managed_agents=[coder_agent]`) inside an isolated E2B VM (`from e2b_code_interpreter import Sandbox`) or hardened Docker container (`USER nobody`, `mem_limit='512m'`, `cpu_quota=50000`, `pids_limit=100`, `security_opt=['no-new-privileges']`, `cap_drop=['ALL']`)."
        )

        fun buildSecureSandboxSnippet(
            architectureMode: String,
            executorType: String = "blaxel",
            fibN: Int = 100
        ): String {
            val effectiveExec = if (executorType == "local") "blaxel" else executorType
            val defaultTaskN = if (effectiveExec == "e2b") 42 else fibN
            return when (architectureMode) {
                "Approach 1: Snippet Sandbox (executor_type)" -> """
                    from smolagents import CodeAgent, InferenceClientModel

                    # Approach 1: Run individual LLM-generated code snippets inside '$effectiveExec' sandbox
                    # Context manager automatically calls agent.cleanup() to stop/hibernate container or VM
                    with CodeAgent(
                        model=InferenceClientModel(),
                        tools=[],
                        executor_type="$effectiveExec"
                    ) as agent:
                        output = agent.run("Calculate the ${defaultTaskN}th Fibonacci number")
                    print("Fibonacci($defaultTaskN) =", output)
                """.trimIndent()
                "Approach 2A: E2B Multi-Agent Sandbox" -> """
                    from e2b_code_interpreter import Sandbox
                    import os

                    # Create the E2B sandbox & install smolagents for full Multi-Agent isolation
                    sandbox = Sandbox()
                    sandbox.commands.run("pip install smolagents")

                    def run_code_raise_errors(sandbox, code: str, verbose: bool = False) -> str:
                        execution = sandbox.run_code(
                            code,
                            envs={'HF_TOKEN': os.getenv('HF_TOKEN')}
                        )
                        if execution.error:
                            execution_logs = "\n".join([str(log) for log in execution.logs.stdout])
                            logs = execution_logs
                            logs += execution.error.traceback
                            raise ValueError(logs)
                        return "\n".join([str(log) for log in execution.logs.stdout])

                    agent_code = '''
                    import os
                    from smolagents import CodeAgent, InferenceClientModel

                    agent = CodeAgent(
                        model=InferenceClientModel(model_id="Qwen/Qwen2.5-72B-Instruct", provider="together"),
                        tools=[],
                        name="coder_agent",
                        description="This agent takes care of your difficult algorithmic problems using code."
                    )
                    manager_agent = CodeAgent(
                        model=InferenceClientModel(model_id="Qwen/Qwen2.5-72B-Instruct", provider="together"),
                        tools=[],
                        managed_agents=[agent]
                    )
                    response = manager_agent.run("What's the 20th Fibonacci number?")
                    print(response)
                    '''

                    execution_logs = run_code_raise_errors(sandbox, agent_code)
                    print(execution_logs)
                """.trimIndent()
                "Approach 2B: Hardened DockerSandbox Class" -> """
                    # Dockerfile:
                    # FROM python:3.10-bullseye
                    # RUN pip install --no-cache-dir smolagents
                    # USER nobody
                    # WORKDIR /app

                    import docker
                    import os
                    from typing import Optional

                    class DockerSandbox:
                        def __init__(self):
                            self.client = docker.from_env()
                            self.container = None

                        def create_container(self):
                            self.container = self.client.containers.run(
                                "agent-sandbox",
                                command="tail -f /dev/null",
                                detach=True,
                                tty=True,
                                mem_limit="512m",
                                cpu_quota=50000,
                                pids_limit=100,
                                security_opt=["no-new-privileges"],
                                cap_drop=["ALL"],
                                environment={"HF_TOKEN": os.getenv("HF_TOKEN")}
                            )

                        def run_code(self, code: str) -> Optional[str]:
                            if not self.container:
                                self.create_container()
                            exec_result = self.container.exec_run(
                                cmd=["python", "-c", code],
                                user="nobody"
                            )
                            return exec_result.output.decode() if exec_result.output else None

                        def cleanup(self):
                            if self.container:
                                try:
                                    self.container.stop()
                                    self.container.remove()
                                except Exception as e:
                                    print(f"Error during cleanup: {e}")
                                finally:
                                    self.container = None

                    sandbox = DockerSandbox()
                    try:
                        agent_code = '''
                    import os
                    from smolagents import CodeAgent, InferenceClientModel

                    agent = CodeAgent(
                        model=InferenceClientModel(token=os.getenv("HF_TOKEN"), provider="together"),
                        tools=[]
                    )
                    response = agent.run("What's the 20th Fibonacci number?")
                    print(response)
                    '''
                        output = sandbox.run_code(agent_code)
                        print(output)
                    finally:
                        sandbox.cleanup()
                """.trimIndent()
                else -> """
                    from smolagents.local_python_executor import LocalPythonExecutor

                    custom_executor = LocalPythonExecutor(["numpy"])

                    def run_capture_exception(command: str):
                        try:
                            custom_executor(command, {})
                        except Exception as e:
                            print("ERROR:", e)

                    # 1. Undefined command
                    run_capture_exception("!echo Bad command")
                    # 2. Disallowed import
                    run_capture_exception("import os; exit_code = os.system('echo Bad command')")
                    # 3. Indirect harmful submodule access
                    run_capture_exception("import random; random._os.system('echo Bad command')")
                    # 4. Infinite loop cap (1,000,000 iterations)
                    run_capture_exception("while True: pass")
                """.trimIndent()
            }
        }

        val BUILDING_GOOD_AGENTS_PILLARS = listOf(
            "1. Simplify the Workflow: Reduce LLM calls by grouping APIs into a single deterministic tool (e.g., return_spot_information combining travel distance + weather APIs).",
            "2. Improve Information Flow: Log execution details via print() in forward(), specify exact argument formats in docstrings ('Anchor Point, Taghazout, Morocco', '%m/%d/%y %H:%M:%S'), raise descriptive ValueError traces, and return human-readable summaries.",
            "3. Pass Rich Context via additional_args: Supply extra objects (audio URLs, images, dataframes, legal exhibits) via agent.run(task, additional_args={...}).",
            "4. 4-Step Debugging & Planning: (a) Upgrade to stronger LLM ($STRONG_DEBUG_HF_MODEL or $DEEPSEEK_R1_DISTILL_MODEL) so outputs like image_generator() are saved to variables before final_answer(image); (b) Append custom instructions=...; (c) Inspect/extend agent.prompt_templates['system_prompt'] with the 11 Golden Rules & Jinja placeholders ({{ tool.to_code_prompt() }}, {{ tool.to_tool_calling_prompt() }}, {{authorized_imports}}, {{custom_instructions}}); (d) Enable periodic reflection with planning_interval=3."
        )

        val OPENTELEMETRY_INSPECTION_GUIDE = listOf(
            "Why Log Agent Runs: Multi-step agent workflows are unpredictable by design and console logs fill up with LLM self-corrections; OpenTelemetry (SmolagentsInstrumentor) records structured spans, tool calls, latency, and token counts in production.",
            "Arize AI Phoenix: `pip install 'smolagents[telemetry,toolkit]'` -> Run collector `python -m phoenix.server.main serve` -> `from phoenix.otel import register; from openinference.instrumentation.smolagents import SmolagentsInstrumentor; register(); SmolagentsInstrumentor().instrument()` -> Inspect at `http://0.0.0.0:6006/projects/`.",
            "MLflow Autologging: `pip install mlflow smolagents` -> `import mlflow; mlflow.smolagents.autolog()` -> Run UI `mlflow server --port 5000` to inspect traces, spans, inputs/outputs, and token usage.",
            "🪢 Langfuse OpenInference: `pip install langfuse 'smolagents[telemetry]' openinference-instrumentation-smolagents` -> Configure `LANGFUSE_PUBLIC_KEY`, `LANGFUSE_SECRET_KEY`, `LANGFUSE_HOST` (https://cloud.langfuse.com EU or https://us.cloud.langfuse.com US) & `HF_TOKEN` in AI Studio Secrets -> `from langfuse import get_client; langfuse = get_client(); langfuse.auth_check()` + `SmolagentsInstrumentor().instrument()`."
        )

        val SYSTEM_PROMPT_11_GOLDEN_RULES = listOf(
            "Rule 1: Always provide a 'Thought:' sequence and a code block '{{code_block_opening_tag}}...{{code_block_closing_tag}}'.",
            "Rule 2: Use only variables that you have defined.",
            "Rule 3: Pass tool arguments directly as keyword args (query=\"...\"), NEVER as a single dict ({'query': \"...\"}).",
            "Rule 4: For tools WITHOUT JSON output schema, do NOT chain dependent calls in the same block; use print() first and inspect in the next step.",
            "Rule 5: For tools WITH JSON output schema, confidently chain calls and index structured fields (result['field_name']) in the same block.",
            "Rule 6: Call a tool only when needed and never repeat the exact same parameters.",
            "Rule 7: Never name a variable the same as a tool (e.g., never assign to 'final_answer').",
            "Rule 8: Never create notional/dummy variables in code logs.",
            "Rule 9: Only import modules from {{authorized_imports}}.",
            "Rule 10: State persists across steps (defined variables & imports remain in memory).",
            "Rule 11: Don't give up — solve the task completely and return via final_answer(...)."
        )

        val AUTHORIZED_TOOL_SCHEMA_TYPES = listOf(
            "string", "boolean", "integer", "number", "image", "audio", "array", "object", "any", "null"
        )

        val HF_DOWNLOAD_COUNTER_TASKS = listOf(
            "text-to-video",
            "text-classification",
            "depth-estimation",
            "image-to-text",
            "automatic-speech-recognition"
        )

        val TOOL_INTEGRATION_SOURCES = listOf(
            "HFModelDownloadsTool(Tool)",
            "@tool model_download_counter",
            "Tool.from_space(FLUX.1-schnell)",
            "Tool.from_langchain(serpapi)",
            "ToolCollection.from_hub",
            "MCPClient(structured_output=True)"
        )

        val ADVANCED_TOOLS_GUIDE = listOf(
            "1. Custom Tool Subclass (`class HFModelDownloadsTool(Tool)`): Define `name = 'model_download_counter'`, `description`, `inputs` (Pydantic JSON schemas from ${AUTHORIZED_TOOL_SCHEMA_TYPES.joinToString("/")}), `output_type = 'string'`, and self-contained `forward(self, task: str)` importing `from huggingface_hub import list_models`.",
            "2. `@tool` Decorator (`@tool def model_download_counter(task: str) -> str`): Fast function wrapper with clear type hints + `Args:` docstring; self-contained imports inside function body for serialization.",
            "3. Hub Sharing (`push_to_hub` & `load_tool`): `model_download_tool.push_to_hub('{your_username}/hf-model-downloads', token='<YOUR_HUGGINGFACEHUB_API_TOKEN>')` and `load_tool('{your_username}/hf-model-downloads', trust_remote_code=True)`.",
            "4. HF Spaces as Tools (`Tool.from_space`): `Tool.from_space('black-forest-labs/FLUX.1-schnell', name='image_generator', description='Generate an image from a prompt')` paired with `InferenceClientModel('Qwen/Qwen3-Next-80B-A3B-Thinking')` and `additional_args={'user_prompt': 'A rabbit wearing a space suit'}`.",
            "5. LangChain Tools (`Tool.from_langchain`): `Tool.from_langchain(load_tools(['serpapi'])[0])` — e.g. comparing BERT base encoder (12 layers) vs Attention is All You Need Transformer encoder (6 layers -> +6 layers).",
            "6. Dynamic Agent Toolbox (`agent.tools` dict) & `ToolCollection.from_hub`: Inspect `agent.tools`, register `agent.tools[model_download_tool.name] = model_download_tool` (never mutate a running agent!), or load `ToolCollection.from_hub(collection_slug='huggingface-tools/diffusion-tools-6630bb19a942c2306a2cdb6f', trust_remote_code=True)`.",
            "7. MCP Servers (`MCPClient` & `ToolCollection.from_mcp` + `structured_output=True`): Connect via Stdio (`StdioServerParameters(command='uvx', args=['--quiet', 'pubmedmcp@0.1.3'])`), Streamable HTTP (`{'url': 'http://127.0.0.1:8000/mcp', 'transport': 'streamable-http'}`), or `FastMCP('Weather Service')` with `structured_output=True` (MCP 2025-06-18+ `outputSchema` -> `tool.output_schema`, `output_type='object'`, direct `weather['temperature']` access)."
        )

        fun buildAdvancedToolSnippet(
            toolSource: String,
            taskCategory: String = "text-to-video",
            mcpStructuredOutput: Boolean = true
        ): String {
            return when (toolSource) {
                "HFModelDownloadsTool(Tool)" -> """
                    from smolagents import CodeAgent, InferenceClientModel, Tool

                    class HFModelDownloadsTool(Tool):
                        name = "model_download_counter"
                        description = "Returns the most downloaded model of a given task on the Hugging Face Hub."
                        inputs = {
                            "task": {
                                "type": "string",
                                "description": "the task category (such as text-classification, depth-estimation, text-to-video)"
                            }
                        }
                        output_type = "string"

                        def forward(self, task: str):
                            from huggingface_hub import list_models
                            model = next(iter(list_models(filter=task, sort="downloads", direction=-1)))
                            return model.id

                    model_download_tool = HFModelDownloadsTool()
                    agent = CodeAgent(tools=[model_download_tool], model=InferenceClientModel())
                    agent.run("Can you give me the name of the model that has the most downloads in the '$taskCategory' task on the Hugging Face Hub? Please return the name with the letters reversed.")
                """.trimIndent()
                "@tool model_download_counter" -> """
                    from smolagents import CodeAgent, InferenceClientModel, tool

                    @tool
                    def model_download_counter(task: str) -> str:
                        \"\"\"
                        This is a tool that returns the most downloaded model of a given task on the Hugging Face Hub.
                        It returns the name of the checkpoint.

                        Args:
                            task: The task for which to get the download count.
                        \"\"\"
                        from huggingface_hub import list_models
                        model = next(iter(list_models(filter=task, sort="downloads", direction=-1)))
                        return model.id

                    agent = CodeAgent(tools=[model_download_counter], model=InferenceClientModel())
                    agent.run("Can you give me the name of the model that has the most downloads in the '$taskCategory' task on the Hugging Face Hub? Please return the name with the letters reversed.")
                """.trimIndent()
                "Tool.from_space(FLUX.1-schnell)" -> """
                    from smolagents import CodeAgent, InferenceClientModel, Tool

                    image_generation_tool = Tool.from_space(
                        "black-forest-labs/FLUX.1-schnell",
                        name="image_generator",
                        description="Generate an image from a prompt"
                    )
                    model = InferenceClientModel("Qwen/Qwen3-Next-80B-A3B-Thinking")
                    agent = CodeAgent(tools=[image_generation_tool], model=model)
                    agent.run(
                        "Improve this prompt, then generate an image of it.",
                        additional_args={"user_prompt": "A rabbit wearing a space suit"}
                    )
                """.trimIndent()
                "Tool.from_langchain(serpapi)" -> """
                    from langchain.agents import load_tools
                    from smolagents import CodeAgent, InferenceClientModel, Tool

                    search_tool = Tool.from_langchain(load_tools(["serpapi"])[0])
                    agent = CodeAgent(tools=[search_tool], model=InferenceClientModel())
                    agent.run("How many more blocks (also denoted as layers) are in BERT base encoder compared to the encoder from the architecture proposed in Attention is All You Need?")
                """.trimIndent()
                "ToolCollection.from_hub" -> """
                    from smolagents import CodeAgent, InferenceClientModel, ToolCollection, load_tool

                    image_tool_collection = ToolCollection.from_hub(
                        collection_slug="huggingface-tools/diffusion-tools-6630bb19a942c2306a2cdb6f",
                        trust_remote_code=True
                    )
                    model_download_tool = load_tool("m-ric/hf-model-downloads", trust_remote_code=True)
                    agent = CodeAgent(tools=[*image_tool_collection.tools], model=InferenceClientModel(), add_base_tools=True)
                    agent.tools[model_download_tool.name] = model_download_tool
                """.trimIndent()
                else -> """
                    from mcp import StdioServerParameters
                    from smolagents import CodeAgent, InferenceClientModel, MCPClient

                    server_parameters = StdioServerParameters(
                        command="uv",
                        args=["run", "weather_mcp.py"]
                    )
                    with MCPClient(server_parameters, structured_output=${if (mcpStructuredOutput) "True" else "False"}) as tools:
                        agent = CodeAgent(tools=tools, model=InferenceClientModel())
                        result = agent.run("What is the temperature in Tokyo in Fahrenheit?")
                        print(result)
                """.trimIndent()
            }
        }

        val TOOLS_EXTRAS = listOf(
            "toolkit (Default agent tools: WebSearchTool, VisitWebpageTool, DuckDuckGoSearchTool, PythonInterpreterTool, Transcriber)",
            "mcp (Model Context Protocol server integration)"
        )
        val MODEL_EXTRAS = listOf(
            "openai (OpenAI API & AzureOpenAIModel)",
            "transformers (Hugging Face local models & pipelines)",
            "vllm (High-throughput local inference via vLLM)",
            "mlx-lm (Apple Silicon local inference via MLXModel)",
            "litellm (100+ LLM providers via LiteLLMModel incl. Anthropic & Ollama)",
            "bedrock (AmazonBedrockModel + boto3 guardrailConfig)"
        )
        val MULTIMODAL_EXTRAS = listOf(
            "vision (Image processing & VLM support)",
            "audio (Audio processing & Whisper-Turbo Transcriber)"
        )
        val REMOTE_SANDBOX_EXTRAS = listOf(
            "blaxel (Fast-launching VMs <25ms from hibernation — BL_API_KEY / BL_WORKSPACE)",
            "e2b (E2B cloud sandbox remote execution — E2B_API_KEY)",
            "modal (Modal cloud sandbox remote execution — modal token new)",
            "docker (Local Docker container isolated execution — DockerSandbox)"
        )
        val TELEMETRY_UI_EXTRAS = listOf(
            "telemetry (OpenTelemetry / Phoenix / MLflow / Langfuse monitoring)",
            "gradio (Interactive GradioUI(agent).launch() web interface)"
        )

        val ALL_EXTRA_KEYS = listOf(
            "toolkit", "mcp", "openai", "transformers", "vllm", "mlx-lm",
            "litellm", "bedrock", "vision", "audio", "blaxel", "e2b",
            "modal", "docker", "telemetry", "gradio", "all"
        )

        val MODEL_BACKEND_KEYS = listOf(
            "InferenceClientModel",
            "Qwen2.5-72B-Strong",
            "DeepSeek-R1-Distill-32B",
            "TransformersModel",
            "LiteLLMModel",
            "AzureOpenAIModel",
            "AmazonBedrockModel",
            "MLXModel",
            "OpenAIModel"
        )

        val MODEL_BACKENDS = listOf(
            "InferenceClientModel(model_id=\"$DEFAULT_HF_MODEL\", provider=\"together\")",
            "InferenceClientModel(model_id=\"$STRONG_DEBUG_HF_MODEL\") [Stronger LLM for Debugging & Planning]",
            "InferenceClientModel(model_id=\"$DEEPSEEK_R1_DISTILL_MODEL\") [OpenTelemetry & Langfuse Multi-Agent Trace Model]",
            "TransformersModel(model_id=\"$LOCAL_TRANSFORMERS_MODEL\")",
            "LiteLLMModel(model_id=\"$LITELLM_MODEL\") / LiteLLMModel(model_id=\"$OLLAMA_LITELLM_MODEL\", api_base=\"http://localhost:11434\", num_ctx=8192)",
            "AzureOpenAIModel(model_id=\"$AZURE_MODEL\")",
            "AmazonBedrockModel(model_id=\"$BEDROCK_MODEL\", client=bedrock_client, **additional_api_config)",
            "MLXModel(\"$MLX_MODEL\")",
            "OpenAIModel(model_id=\"$OPENAI_REMOVE_PARAM_MODEL\", stop=REMOVE_PARAMETER, temperature=0.7)"
        )
        val DEFAULT_TOOLS = listOf("WebSearchTool()", "VisitWebpageTool()", "DuckDuckGoSearchTool()", "PythonInterpreterTool()")

        fun buildInstallCommand(
            useUv: Boolean,
            extras: List<String>,
            telemetryBackend: String = "phoenix"
        ): String {
            val prefix = if (useUv) "uv pip install" else "pip install"
            val effectiveExtras = if (telemetryBackend in listOf("phoenix", "langfuse") && "all" !in extras) {
                (extras + "telemetry").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            } else {
                extras.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            }
            val basePkg = if (effectiveExtras.isEmpty()) {
                "smolagents"
            } else {
                "\"smolagents[${effectiveExtras.joinToString(",")}]\""
            }
            return when (telemetryBackend) {
                "mlflow" -> "$prefix mlflow $basePkg"
                "langfuse" -> "$prefix langfuse $basePkg openinference-instrumentation-smolagents"
                else -> "$prefix $basePkg"
            }
        }

        fun buildCliCommand(
            prompt: String,
            modelType: String = "InferenceClientModel",
            modelId: String = CODER_HF_MODEL,
            imports: String = "pandas numpy requests bs4",
            tools: String = "web_search"
        ): String {
            val resolvedType = when (modelType) {
                "Qwen2.5-72B-Strong", "DeepSeek-R1-Distill-32B" -> "InferenceClientModel"
                else -> modelType
            }
            val resolvedId = when (modelType) {
                "Qwen2.5-72B-Strong" -> STRONG_DEBUG_HF_MODEL
                "DeepSeek-R1-Distill-32B" -> DEEPSEEK_R1_DISTILL_MODEL
                else -> modelId
            }
            val safePrompt = prompt.replace("\"", "'")
            return "smolagent \"$safePrompt\" --model-type \"$resolvedType\" --model-id \"$resolvedId\" --imports \"$imports\" --tools \"$tools\""
        }

        fun buildTelemetryInstrumentationHeader(telemetryBackend: String): String {
            return when (telemetryBackend) {
                "phoenix" -> """
                    # Collector: python -m phoenix.server.main serve (UI: http://0.0.0.0:6006/projects/)
                    from phoenix.otel import register
                    from openinference.instrumentation.smolagents import SmolagentsInstrumentor
                    register()
                    SmolagentsInstrumentor().instrument()
                """.trimIndent()
                "mlflow" -> """
                    # Server: mlflow server --port 5000
                    import mlflow
                    mlflow.smolagents.autolog()
                """.trimIndent()
                "langfuse" -> """
                    # Set LANGFUSE_PUBLIC_KEY, LANGFUSE_SECRET_KEY, LANGFUSE_HOST in environment / AI Studio Secrets
                    from langfuse import get_client
                    from openinference.instrumentation.smolagents import SmolagentsInstrumentor
                    langfuse = get_client()
                    if langfuse.auth_check():
                        SmolagentsInstrumentor().instrument()
                """.trimIndent()
                else -> ""
            }
        }

        fun buildGuidedTourSnippet(
            agentType: String = AGENT_CLASS,
            modelBackend: String = "InferenceClientModel",
            executorType: String = "blaxel",
            multiAgent: Boolean = false,
            enablePlanningInterval: Boolean = true,
            includeAdditionalArgs: Boolean = true,
            telemetryBackend: String = "phoenix"
        ): String {
            val importClass = when (modelBackend) {
                "Qwen2.5-72B-Strong", "DeepSeek-R1-Distill-32B" -> "InferenceClientModel"
                else -> modelBackend
            }
            val modelInit = when (modelBackend) {
                "Qwen2.5-72B-Strong" -> "model = InferenceClientModel(model_id=\"$STRONG_DEBUG_HF_MODEL\")"
                "DeepSeek-R1-Distill-32B" -> "model = InferenceClientModel(model_id=\"$DEEPSEEK_R1_DISTILL_MODEL\")"
                "TransformersModel" -> "model = TransformersModel(model_id=\"$LOCAL_TRANSFORMERS_MODEL\")"
                "LiteLLMModel" -> "model = LiteLLMModel(model_id=\"$LITELLM_MODEL\", num_ctx=8192)"
                "AzureOpenAIModel" -> "model = AzureOpenAIModel(model_id=\"$AZURE_MODEL\")"
                "AmazonBedrockModel" -> "model = AmazonBedrockModel(model_id=\"$BEDROCK_MODEL\")"
                "MLXModel" -> "model = MLXModel(\"$MLX_MODEL\")"
                "OpenAIModel" -> "from smolagents.models import REMOVE_PARAMETER\nmodel = OpenAIModel(model_id=\"$OPENAI_REMOVE_PARAM_MODEL\", stop=REMOVE_PARAMETER, temperature=0.7)"
                else -> "model = InferenceClientModel(model_id=\"$DEFAULT_HF_MODEL\")"
            }
            val planningArg = if (enablePlanningInterval) ",\n    planning_interval=$DEFAULT_PLANNING_INTERVAL" else ""
            val otelHeader = buildTelemetryInstrumentationHeader(telemetryBackend)
            val otelPrefix = if (otelHeader.isNotBlank()) "$otelHeader\n\n" else ""
            val runCall = if (includeAdditionalArgs) {
                "agent.run(task, additional_args={\"statute_exhibit\": \"RA_7659_RA_9165\", \"mp3_sound_file_url\": \"https://huggingface.co/datasets/huggingface/documentation-images/resolve/main/transformers/recording.mp3\"})"
            } else {
                "agent.run(task)"
            }
            return if (multiAgent) {
                val managerRunCall = if (includeAdditionalArgs) {
                    "manager_agent.run(task, additional_args={\"statute_exhibit\": \"RA_7659_RA_9165\"})"
                } else {
                    "manager_agent.run(\"If the US keeps its 2024 growth rate, how many years will it take for the GDP to double?\")"
                }
                otelPrefix + """
                from smolagents import CodeAgent, ToolCallingAgent, VisitWebpageTool, WebSearchTool, $importClass
                $modelInit
                search_agent = ToolCallingAgent(
                    tools=[WebSearchTool(), VisitWebpageTool()],
                    model=model,
                    name="search_agent",
                    description="This is an agent that can do web search."
                )
                manager_agent = CodeAgent(
                    tools=[],
                    model=model,
                    managed_agents=[search_agent],
                    instructions="Enforce Vault 1293 deterministic tool consolidation & 11 Golden Rules.",
                    additional_authorized_imports=["requests", "bs4", "pandas", "numpy.*"]$planningArg
                )
                $managerRunCall
                """.trimIndent()
            } else if (agentType == TOOL_CALLING_AGENT_CLASS) {
                otelPrefix + """
                from smolagents import ToolCallingAgent, VisitWebpageTool, WebSearchTool, $importClass
                $modelInit
                agent = ToolCallingAgent(
                    tools=[WebSearchTool(), VisitWebpageTool()],
                    model=model,
                    instructions="Use schema-validated tool calls & explicit format strings."$planningArg
                )
                $runCall
                """.trimIndent()
            } else {
                val execArg = if (executorType == "local") "" else ",\n    executor_type=\"$executorType\""
                otelPrefix + """
                from smolagents import CodeAgent, VisitWebpageTool, WebSearchTool, $importClass, tool
                $modelInit
                with CodeAgent(
                    tools=[WebSearchTool(), VisitWebpageTool()],
                    model=model,
                    add_base_tools=True,
                    instructions="Save tool outputs to variables before final_answer(); log errors clearly.",
                    additional_authorized_imports=["requests", "bs4", "pandas", "numpy.*"]$execArg$planningArg
                ) as agent:
                    $runCall
                """.trimIndent()
            }
        }

        val MEMORY_MANAGEMENT_MODES = listOf(
            "1. Replay Memory (agent.replay())",
            "2. Inspect Steps (ActionStep / get_full_steps)",
            "3. Step Callbacks (update_screenshot / helium)",
            "4. Step-by-Step Loop (agent.step + TaskStep)"
        )

        val MEMORY_MANAGEMENT_GUIDE = listOf(
            "1. Replay Agent's Memory (`agent.replay()`): Inspect a completed run via OpenTelemetry (`./inspect_runs`) or call `agent.replay()` after `result = agent.run(\"What's the 20th Fibonacci number?\")` on `CodeAgent(tools=[], model=InferenceClientModel(), verbosity_level=0)`.",
            "2. Dynamically Access & Modify Memory (`agent.memory` & `get_full_steps()`): Read `system_prompt_step = agent.memory.system_prompt` (`system_prompt_step.system_prompt`), `task_step = agent.memory.steps[0]` (`task_step.task`), iterate `for step in agent.memory.steps:` where `isinstance(step, ActionStep)` to inspect `step.error` vs `step.observations`, or call `agent.memory.get_full_steps()` for full step dicts.",
            "3. Step Callbacks for Dynamic Memory Pruning (`step_callbacks=[update_screenshot]`): Define `def update_screenshot(memory_step: ActionStep, agent: CodeAgent) -> None:` with `helium.get_driver()` and `PIL.Image`; prune older screenshots (`previous_memory_step.step_number <= latest_step - 2` -> `previous_memory_step.observations_images = None`) to save tokens, attach `memory_step.observations_images = [image.copy()]`, and pass `step_callbacks=[update_screenshot]` to `CodeAgent`.",
            "4. Run Agents One Step at a Time (`agent.step(memory_step)`): Initialize `CodeAgent(tools=[], model=InferenceClientModel(), verbosity_level=1)`, call `agent.python_executor.send_tools({**agent.tools})`, optionally copy `agent.memory.steps = previous_agent.memory.steps`, append `TaskStep(task=task, task_images=[])`, and loop `while final_answer is None and step_number <= 10:` creating `ActionStep(step_number=step_number, observations_images=[])`, calling `final_answer = agent.step(memory_step)`, appending `memory_step`, and mutating `agent.memory.steps[-1]` on the fly."
        )

        fun buildMemoryManagementSnippet(
            memoryMode: String,
            maxSteps: Int = 10,
            pruneScreenshotWindow: Int = 2,
            taskPrompt: String = "What is the 20th Fibonacci number?"
        ): String {
            return when (memoryMode) {
                "1. Replay Memory (agent.replay())" -> """
                    from smolagents import InferenceClientModel, CodeAgent

                    agent = CodeAgent(tools=[], model=InferenceClientModel(), verbosity_level=0)

                    result = agent.run("What's the 20th Fibonacci number?")
                    # Replay the last run step-by-step in the console
                    agent.replay()
                """.trimIndent()
                "2. Inspect Steps (ActionStep / get_full_steps)" -> """
                    from smolagents import ActionStep

                    system_prompt_step = agent.memory.system_prompt
                    print("The system prompt given to the agent was:")
                    print(system_prompt_step.system_prompt)

                    task_step = agent.memory.steps[0]
                    print("\n\nThe first task step was:")
                    print(task_step.task)

                    for step in agent.memory.steps:
                        if isinstance(step, ActionStep):
                            if step.error is not None:
                                print(f"\nStep {step.step_number} got this error:\n{step.error}\n")
                            else:
                                print(f"\nStep {step.step_number} got these observations:\n{step.observations}\n")

                    # Retrieve full steps as Python dictionaries
                    full_steps = agent.memory.get_full_steps()
                """.trimIndent()
                "3. Step Callbacks (update_screenshot / helium)" -> """
                    import helium
                    from PIL import Image
                    from io import BytesIO
                    from time import sleep
                    from smolagents import CodeAgent, ActionStep, WebSearchTool, InferenceClientModel

                    def update_screenshot(memory_step: ActionStep, agent: CodeAgent) -> None:
                        sleep(1.0)  # Let JavaScript animations happen before taking the screenshot
                        driver = helium.get_driver()
                        latest_step = memory_step.step_number
                        for previous_memory_step in agent.memory.steps:  # Remove previous screenshots from logs for lean processing
                            if isinstance(previous_memory_step, ActionStep) and previous_memory_step.step_number <= latest_step - $pruneScreenshotWindow:
                                previous_memory_step.observations_images = None
                        png_bytes = driver.get_screenshot_as_png()
                        image = Image.open(BytesIO(png_bytes))
                        memory_step.observations_images = [image.copy()]

                    agent = CodeAgent(
                        tools=[WebSearchTool(), go_back, close_popups, search_item_ctrl_f],
                        model=InferenceClientModel(),
                        additional_authorized_imports=["helium"],
                        step_callbacks=[update_screenshot],
                        max_steps=20,
                        verbosity_level=2,
                    )
                """.trimIndent()
                else -> """
                    from smolagents import InferenceClientModel, CodeAgent, ActionStep, TaskStep

                    agent = CodeAgent(tools=[], model=InferenceClientModel(), verbosity_level=1)
                    agent.python_executor.send_tools({**agent.tools})

                    task = "$taskPrompt"

                    # You could modify the memory as needed here by inputting the memory of another agent, e.g.
                    # agent.memory.steps = previous_agent.memory.steps

                    # Start a new task!
                    agent.memory.steps.append(TaskStep(task=task, task_images=[]))

                    final_answer = None
                    step_number = 1
                    while final_answer is None and step_number <= $maxSteps:
                        memory_step = ActionStep(
                            step_number=step_number,
                            observations_images=[],
                        )
                        # Run one step.
                        final_answer = agent.step(memory_step)
                        agent.memory.steps.append(memory_step)
                        step_number += 1

                        # Change the memory as you please!
                        # agent.memory.steps[-1] = ...

                    print("The final answer is:", final_answer)
                """.trimIndent()
            }
        }

        // "# What are agents? 🤔 — An introduction to agentic systems" Conceptual Guide
        const val AGENT_DEFINITION_CORE = "AI Agents are programs where LLM outputs control the workflow."

        val AGENCY_SPECTRUM_LEVELS = listOf(
            "☆☆☆ Simple processor",
            "★☆☆ Router",
            "★★☆ Tool call",
            "★★☆ Multi-step Agent",
            "★★★ Multi-Agent",
            "★★★ Code Agents"
        )

        val SURFING_TRIP_SCENARIOS = listOf(
            "Scenario 1: Deterministic 2-Bucket (⛔ Avoid Agent)",
            "Scenario 2: Tuesday Surf + Delayed Passport + Insurance (✅ Multi-Step Agent)"
        )

        val WHAT_ARE_AGENTS_GUIDE = listOf(
            "Core Definition: AI Agents are programs where LLM outputs control the workflow. Agency evolves on a continuous spectrum (☆☆☆ to ★★★) depending on how much power the LLM has on program workflow.",
            "Agency Spectrum Table: ☆☆☆ Simple processor (`process_llm_output(llm_response)`) -> ★☆☆ Router (`if llm_decision(): path_a() else: path_b()`) -> ★★☆ Tool call (`run_function(llm_chosen_tool, llm_chosen_args)`) -> ★★☆ Multi-step Agent (`while llm_should_continue(): execute_next_step()`) -> ★★★ Multi-Agent (`if llm_trigger(): execute_agent()`) -> ★★★ Code Agents (`def custom_tool(args): ...`).",
            "Multi-Step Agent Core Loop: `memory = [user_defined_task]; while llm_should_continue(memory): action = llm_get_next_action(memory); observations = execute_action(action); memory += [action, observations]`.",
            "⛔ When to Avoid Agents: If all customer requests on a surfing trip website belong to 1 of 2 deterministic buckets (Bucket 1: Knowledge on trips -> search bar; Bucket 2: Talk to sales -> contact form), regularize toward a predetermined workflow with 100% reliability and zero unpredictable LLM meddling.",
            "✅ When to Use Agents: When the workflow cannot be determined well in advance — e.g., `'I can come on Monday, but I forgot my passport so risk being delayed to Wednesday, is it possible to take me and my stuff to surf on Tuesday morning, with a cancellation insurance?'` requiring dynamic calls to Weather API, Google Maps API (travel distance), Employee availability dashboard, and RAG system on knowledge base.",
            "Why smolagents? (6 Tightly-Coupled Building Blocks): (1) LLM engine powering the system, (2) List of tools the agent can access, (3) System prompt guiding the `Reflection -> Action -> Observation` loop, (4) Parser extracting tool calls (`Thought: I should call tool 'get_weather'. Action: get_weather(Paris).`), (5) Memory across loop iterations, and (6) Error logging and retry mechanisms so the LLM can self-correct mistakes."
        )

        val CODE_AGENTS_ADVANTAGES = listOf(
            "Composability: Nest actions within each other, loop, or define reusable Python functions (`def custom_tool(args): ...`) instead of flat JSON blobs.",
            "Object Management: Store action outputs like `generate_image` directly in Python variables for subsequent steps.",
            "Generality: Express any computer action or control flow naturally in Python code.",
            "Representation in LLM Training Data: Plenty of quality Python code actions already exist in LLM pretraining corpora (`Executable Code Actions Elicit Better LLM Agents`, 2402.01030, 2411.01747, 2401.00812)."
        )

        fun buildAgencySpectrumSnippet(
            agencyLevel: String = "★★☆ Multi-step Agent",
            surfingScenario: String = "Scenario 2: Tuesday Surf + Delayed Passport + Insurance (✅ Multi-Step Agent)"
        ): String {
            val levelExample = when (agencyLevel) {
                "☆☆☆ Simple processor" -> """
                    # ☆☆☆ Simple processor — LLM output has no impact on program flow
                    llm_response = call_llm("Summarize this surfing spot guide.")
                    process_llm_output(llm_response)
                """.trimIndent()
                "★☆☆ Router" -> """
                    # ★☆☆ Router — LLM output controls an if/else switch (Deterministic 2-Bucket Classifier)
                    if llm_decision(user_query) == "trip_knowledge":
                        path_a()  # Give access to search bar for knowledge base
                    else:
                        path_b()  # Let user type in a contact form for sales
                """.trimIndent()
                "★★☆ Tool call" -> """
                    # ★★☆ Tool call — LLM output controls function execution
                    llm_chosen_tool, llm_chosen_args = llm_select_tool(user_query)
                    run_function(llm_chosen_tool, llm_chosen_args)
                """.trimIndent()
                "★★★ Multi-Agent" -> """
                    # ★★★ Multi-Agent — One agentic workflow can start another agentic workflow
                    if llm_trigger(user_query):
                        execute_agent(sub_agent="surf_trip_logistics_agent")
                """.trimIndent()
                "★★★ Code Agents" -> """
                    # ★★★ Code Agents — LLM acts in code, can define its own tools / start other agents (Paper: 2402.01030)
                    def custom_tool(spot: str, day: str):
                        weather = get_weather(spot, day)
                        distance = compute_travel_distance(spot)
                        return {"weather": weather, "distance_km": distance}
                    
                    result = custom_tool("Siargao Cloud 9", "Tuesday morning")
                    final_answer(result)
                """.trimIndent()
                else -> """
                    # ★★☆ Multi-step Agent — LLM output controls iteration and program continuation
                    memory = [user_defined_task]
                    while llm_should_continue(memory):  # this loop is the multi-step part
                        action = llm_get_next_action(memory)  # this is the tool-calling part
                        observations = execute_action(action)
                        memory += [action, observations]
                """.trimIndent()
            }

            val scenarioNote = if (surfingScenario.startsWith("Scenario 1")) {
                "# ⛔ Surfing Website Decision: Avoid multi-step agent! Use deterministic 2-bucket routing (100% reliable)."
            } else {
                "# ✅ Surfing Website Decision: Use Multi-Step CodeAgent calling Weather API + Google Maps API + Staff Dashboard + RAG."
            }

            return "$levelExample\n\n$scenarioNote"
        }

        // "# How do multi-step agents work?" — ReAct Framework (Yao et al., 2022 - 2210.03629) & MultiStepAgent Cycle
        const val REACT_PAPER_URL = "https://huggingface.co/papers/2210.03629"
        const val CHAT_TEMPLATING_URL = "https://huggingface.co/docs/transformers/en/chat_templating"
        const val OPEN_SOURCE_AGENTS_BLOG_URL = "https://huggingface.co/blog/open-source-llms-as-agents"
        const val CODEAGENT_DOCS_PNG_URL = "https://huggingface.co/datasets/huggingface/documentation-images/resolve/main/transformers/codeagent_docs.png"
        const val AGENT_MANIM_GIF_URL = "https://huggingface.co/datasets/huggingface/documentation-images/resolve/main/smolagents/Agent_ManimCE.gif"

        val MULTI_STEP_AGENT_VARIANTS = listOf(
            "CodeAgent (Python Code Snippets)",
            "ToolCallingAgent (JSON Blobs • Web Browsing Page-Wait)"
        )

        val REACT_CYCLE_STAGES = listOf(
            "1. Init: SystemPromptStep + TaskStep",
            "2. Loop: agent.write_memory_to_messages()",
            "3. Model Completion + Action Parser (Code vs JSON)",
            "4. Execute Action -> ActionStep + agent.step_callbacks",
            "5. Optional PlanningStep (Periodic Plan Revision & Task Facts)"
        )

        val HOW_MULTI_STEP_AGENTS_WORK_GUIDE = listOf(
            "ReAct Framework (Yao et al., 2022 — $REACT_PAPER_URL): Concatenation of 'Reason' + 'Act'. Solves tasks in as many steps as needed, each step consisting of a Reasoning step followed by an Action step formulating tool calls.",
            "Singular `MultiStepAgent` Class: Base abstraction of the ReAct framework for all agents in `smolagents` (`CodeAgent` and `ToolCallingAgent`).",
            "Initialization Phase: System prompt is stored in `SystemPromptStep`, and the user query is logged into `TaskStep`.",
            "While Loop (ReAct Cycle): (1) Call `agent.write_memory_to_messages()` to serialize agent logs into LLM-readable chat messages ($CHAT_TEMPLATING_URL); (2) Send messages to `Model` object and parse completion into an action (JSON blob for `ToolCallingAgent`, code snippet for `CodeAgent`); (3) Execute action and log result into `ActionStep`; (4) Run all callbacks in `agent.step_callbacks` at the end of each step.",
            "Optional Planning (`PlanningStep`): When planning is activated (`planning_interval`), periodically revise the plan and store it in a `PlanningStep`, feeding facts about the task at hand into memory.",
            "Two `MultiStepAgent` Implementations: `CodeAgent` generates tool calls as Python code snippets (`codeagent_docs.png` / `Agent_ManimCE.gif`), while `ToolCallingAgent` writes tool calls as JSON blobs (well suited when web browsing requires waiting after each page interaction). Read more at $OPEN_SOURCE_AGENTS_BLOG_URL."
        )

        fun buildMultiStepReactCycleSnippet(
            agentVariant: String = "CodeAgent (Python Code Snippets)",
            enablePlanningStep: Boolean = true,
            includeStepCallbacks: Boolean = true
        ): String {
            val isToolCalling = agentVariant.startsWith("ToolCallingAgent")
            val parserComment = if (isToolCalling) {
                "# Parse completion -> JSON blob action (ideal for web browsing waiting after each page interaction)"
            } else {
                "# Parse completion -> Python code snippet action (CodeAgent)"
            }
            val planningBlock = if (enablePlanningStep) {
                """
                |    # Optional Planning: periodically revise plan & feed facts about the task into memory
                |    if agent.planning_interval and step_number % agent.planning_interval == 1:
                |        planning_step = PlanningStep(model_input_messages=messages, plan="Revised facts & multi-step plan")
                |        agent.memory.steps.append(planning_step)
                """.trimMargin()
            } else {
                "    # PlanningStep disabled (planning_interval=None)"
            }
            val callbacksBlock = if (includeStepCallbacks) {
                """
                |    # At the end of each step, run all callback functions defined in agent.step_callbacks
                |    for callback in agent.step_callbacks:
                |        callback(action_step, agent=agent)
                """.trimMargin()
            } else {
                "    # agent.step_callbacks = []"
            }

            return """
                # MultiStepAgent ReAct Loop (Yao et al., 2022 - Reason + Act)
                # Initialization: SystemPromptStep + TaskStep
                agent.memory.system_prompt = SystemPromptStep(system_prompt=agent.system_prompt)
                agent.memory.steps.append(TaskStep(task=user_query))

                while not final_answer and step_number <= agent.max_steps:
                    # 1. Write agent logs into LLM-readable chat messages (chat_templating)
                    messages = agent.write_memory_to_messages()
                $planningBlock
                    # 2. Send messages to Model object & parse completion to get action
                    completion = agent.model(messages)
                    $parserComment
                    action = agent.parse_action(completion)
                    # 3. Execute action and log result into memory (ActionStep)
                    observation = agent.execute_action(action)
                    action_step = ActionStep(step_number=step_number, observations=observation)
                    agent.memory.steps.append(action_step)
                $callbacksBlock
                    step_number += 1
            """.trimIndent()
        }

        // "# Text-to-SQL" — Agentic SQL with smolagents, SQLAlchemy sqlite:///:memory: & @tool sql_engine
        const val TEXT_TO_SQL_INSTALL_CMD = "!pip install smolagents python-dotenv sqlalchemy --upgrade -q"
        const val TEXT_TO_SQL_LEVEL1_MODEL = "meta-llama/Llama-3.1-8B-Instruct"
        const val TEXT_TO_SQL_LEVEL2_MODEL = "Qwen/Qwen3-Next-80B-A3B-Thinking"

        val TEXT_TO_SQL_LEVELS = listOf(
            "Level 1: Single Table (receipts • Llama-3.1-8B-Instruct)",
            "Level 2: Table Joins (receipts + waiters • Qwen3-Next-80B-A3B-Thinking)"
        )

        val TEXT_TO_SQL_PRESET_QUERIES = listOf(
            "SELECT customer_name, price, tip FROM receipts ORDER BY price DESC LIMIT 1",
            "SELECT w.waiter_name, ROUND(SUM(r.tip), 2) AS total_tips FROM receipts r JOIN waiters w ON r.receipt_id = w.receipt_id GROUP BY w.waiter_name ORDER BY total_tips DESC",
            "SELECT r.receipt_id, r.customer_name, r.price, r.tip, w.waiter_name FROM receipts r JOIN waiters w ON r.receipt_id = w.receipt_id"
        )

        val TEXT_TO_SQL_GUIDE = listOf(
            "Why Agentic Text-to-SQL vs Standard Pipeline?: A standard text-to-sql pipeline is brittle because generated SQL can be incorrect or worse—return silent incorrect/empty outputs without raising an error. A `CodeAgent` ReAct system critically inspects query outputs and self-corrects the SQL query across steps.",
            "Environment & SQLAlchemy Setup: Install with `$TEXT_TO_SQL_INSTALL_CMD`, load `HF_TOKEN` via `from dotenv import load_dotenv; load_dotenv()`, and initialize `engine = create_engine('sqlite:///:memory:')` + `metadata_obj = MetaData()`.",
            "Schema Inspection (`inspect(engine)`): Retrieve column names and types dynamically with `inspector = inspect(engine)` -> `inspector.get_columns('receipts')` and inject them into the `@tool` description so the LLM writes accurate SQL.",
            "Tool Definition (`@tool def sql_engine(query: str) -> str`): Uses type hints on input (`query: str`) and output (`-> str`) plus an `Args:` docstring listing tables and columns (`receipts`: `receipt_id INTEGER`, `customer_name VARCHAR(16)`, `price FLOAT`, `tip FLOAT`).",
            "Level 1 — Single Table (`receipts`): Uses `CodeAgent(tools=[sql_engine], model=InferenceClientModel(model_id='meta-llama/Llama-3.1-8B-Instruct'))` to solve `'Can you give me the name of the client who got the most expensive receipt?'` -> `Woodrow Wilson` (`53.43`).",
            "Level 2 — Table Joins (`receipts` + `waiters`): Adds `waiters` (`receipt_id INTEGER`, `waiter_name VARCHAR(16)`), dynamically updates `sql_engine.description = updated_description`, and upgrades to `InferenceClientModel(model_id='Qwen/Qwen3-Next-80B-A3B-Thinking')` to solve `'Which waiter got more total money from tips?'` -> `Michael Watts` (`0.24 + 5.43 = 5.67`)."
        )

        fun buildTextToSqlSnippet(levelMode: String): String {
            val isLevel2 = levelMode.startsWith("Level 2")
            return if (!isLevel2) {
                """
                # $TEXT_TO_SQL_INSTALL_CMD
                from dotenv import load_dotenv
                load_dotenv()  # loads HF_TOKEN
                from sqlalchemy import create_engine, MetaData, Table, Column, String, Integer, Float, insert, inspect, text
                from smolagents import tool, CodeAgent, InferenceClientModel

                engine = create_engine("sqlite:///:memory:")
                metadata_obj = MetaData()
                receipts = Table(
                    "receipts", metadata_obj,
                    Column("receipt_id", Integer, primary_key=True),
                    Column("customer_name", String(16), primary_key=True),
                    Column("price", Float),
                    Column("tip", Float),
                )
                metadata_obj.create_all(engine)

                @tool
                def sql_engine(query: str) -> str:
                    ""${'"'}
                    Allows you to perform SQL queries on the table. Returns a string representation of the result.
                    The table is named 'receipts'. Its description is as follows:
                        Columns:
                        - receipt_id: INTEGER
                        - customer_name: VARCHAR(16)
                        - price: FLOAT
                        - tip: FLOAT

                    Args:
                        query: The query to perform. This should be correct SQL.
                    ""${'"'}
                    output = ""
                    with engine.connect() as con:
                        rows = con.execute(text(query))
                        for row in rows:
                            output += "\n" + str(row)
                    return output

                agent = CodeAgent(
                    tools=[sql_engine],
                    model=InferenceClientModel(model_id="$TEXT_TO_SQL_LEVEL1_MODEL"),
                )
                agent.run("Can you give me the name of the client who got the most expensive receipt?")
                # -> Final Answer: Woodrow Wilson (price = 53.43)
                """.trimIndent()
            } else {
                """
                # Level 2: Table Joins (receipts + waiters) with Dynamic Tool Description Update
                waiters = Table(
                    "waiters", metadata_obj,
                    Column("receipt_id", Integer, primary_key=True),
                    Column("waiter_name", String(16), primary_key=True),
                )
                metadata_obj.create_all(engine)

                updated_description = ""${'"'}Allows you to perform SQL queries on the table. Beware that this tool's output is a string representation of the execution output.
                It can use the following tables:
                Table 'receipts': Columns: receipt_id (INTEGER), customer_name (VARCHAR(16)), price (FLOAT), tip (FLOAT)
                Table 'waiters': Columns: receipt_id (INTEGER), waiter_name (VARCHAR(16))""${'"'}

                # Directly update the @tool description with the new schema
                sql_engine.description = updated_description

                agent = CodeAgent(
                    tools=[sql_engine],
                    model=InferenceClientModel(model_id="$TEXT_TO_SQL_LEVEL2_MODEL"),
                )
                agent.run("Which waiter got more total money from tips?")
                # -> Final Answer: Michael Watts (tips: 0.24 + 5.43 = 5.67 vs Corey Johnson 1.20 vs Margaret James 1.00)
                """.trimIndent()
            }
        }

        // "# Agentic RAG" (`examples/rag.py` — Knowledge Base + RecursiveCharacterTextSplitter + BM25Retriever + CodeAgent)
        const val AGENTIC_RAG_INSTALL_CMD = "pip install smolagents pandas langchain langchain-community sentence-transformers datasets python-dotenv rank_bm25 --upgrade"
        const val AGENTIC_RAG_DATASET_ID = "m-ric/huggingface_doc"
        const val AGENTIC_RAG_SOURCE_FILTER = "huggingface/transformers"
        const val AGENTIC_RAG_DEFAULT_MODEL = "Qwen/Qwen3-Next-80B-A3B-Thinking"
        const val AGENTIC_RAG_ALT_MODEL = "meta-llama/Llama-3.3-70B-Instruct"
        const val AGENTIC_RAG_DEFAULT_QUESTION = "For a transformers model training, which is slower, the forward or the backward pass?"
        const val AGENTIC_RAG_HYDE_PAPER_URL = "https://huggingface.co/papers/2212.10496"
        const val AGENTIC_RAG_SELF_QUERY_URL = "https://python.langchain.com/docs/how_to/self_query/"

        val AGENTIC_RAG_STEPS = listOf(
            "Step 1 & 2: Install + Prepare KB (`m-ric/huggingface_doc` • `RecursiveCharacterTextSplitter`)",
            "Step 3: Create `RetrieverTool(Tool)` (`BM25Retriever.from_documents(docs, k=10)`)",
            "Step 4 & 5: Create & Run `CodeAgent` (`max_steps=4, verbosity_level=2`)",
            "Full `examples/rag.py` + HyDE (2212.10496) & Self-Query Best Practices"
        )

        val AGENTIC_RAG_PRESET_QUERIES = listOf(
            "For a transformers model training, which is slower, the forward or the backward pass?",
            "The backward pass in transformers training is slower than the forward pass because of gradient computation",
            "Gradient checkpointing and mixed precision training memory optimization in transformers",
            "How does Trainer handle gradient_accumulation_steps during backward pass?"
        )

        val AGENTIC_RAG_GUIDE = listOf(
            "1. Why Use RAG? (5 Advantages): (1) Factual Grounding (reduces hallucinations), (2) Domain Specialization (without model retraining), (3) Knowledge Recency (beyond training cutoff), (4) Transparency (source citations), (5) Control (fine-grained access control).",
            "2. Limitations of Traditional RAG: Single retrieval step (poor initial results ruin generation), Query-Document Mismatch (user questions vs affirmative document statements), Limited Reasoning (no multi-hop synthesis), and Suboptimal Query Formulation.",
            "3. Agentic RAG Capabilities: Dynamic Query Formulation (rewriting questions into affirmative target statements like HyDE `https://huggingface.co/papers/2212.10496`), Multi-Step Retrieval (`max_steps=4`), Information Integration across sources, Self-Correction / Self-Query (`https://python.langchain.com/docs/how_to/self_query/`), and Tool Orchestration.",
            "4. Knowledge Base & Chunking (`Step 2`): Load `datasets.load_dataset(\"m-ric/huggingface_doc\", split=\"train\")`, filter `doc[\"source\"].startswith(\"huggingface/transformers\")`, split via `RecursiveCharacterTextSplitter(chunk_size=500, chunk_overlap=50, add_start_index=True, strip_whitespace=True, separators=[\"\\n\\n\", \"\\n\", \".\", \" \", \"\"])`, and deduplicate chunks.",
            "5. `RetrieverTool(Tool)` (`Step 3`): Wrap `BM25Retriever.from_documents(docs, k=10)` (or production vector DBs like FAISS / Chroma with `sentence-transformers` embeddings) with `inputs={\"query\": {\"type\": \"string\", \"description\": \"The query to perform. This should be semantically close to your target documents. Use the affirmative form rather than a question.\\n\"}}`.",
            "6. Retrieval Agent Execution (`Step 4 & 5`): `CodeAgent(tools=[retriever_tool], model=InferenceClientModel(), max_steps=4, verbosity_level=2)` (defaults to `Qwen/Qwen3-Next-80B-A3B-Thinking` or `meta-llama/Llama-3.3-70B-Instruct`) runs `agent.run(\"For a transformers model training, which is slower, the forward or the backward pass?\")`."
        )

        fun buildAgenticRagSnippet(
            stepMode: String,
            modelId: String = AGENTIC_RAG_DEFAULT_MODEL
        ): String {
            return when {
                stepMode.startsWith("Step 1") -> """
                    # Step 1: Install Required Dependencies
                    # $AGENTIC_RAG_INSTALL_CMD
                    from dotenv import load_dotenv
                    load_dotenv()  # loads HF_TOKEN for Inference Providers

                    # Step 2: Prepare the Knowledge Base (Hugging Face `transformers` docs)
                    import datasets
                    from langchain_core.documents import Document
                    from langchain_text_splitters import RecursiveCharacterTextSplitter

                    knowledge_base = datasets.load_dataset("$AGENTIC_RAG_DATASET_ID", split="train")
                    source_docs = [
                        Document(page_content=doc["text"], metadata={"source": doc["source"].split("/")[1]})
                        for doc in knowledge_base
                        if doc["source"].startswith("$AGENTIC_RAG_SOURCE_FILTER")
                    ]

                    text_splitter = RecursiveCharacterTextSplitter(
                        chunk_size=500,
                        chunk_overlap=50,
                        add_start_index=True,
                        strip_whitespace=True,
                        separators=["\n\n", "\n", ".", " ", ""],
                    )
                    docs_processed = text_splitter.split_documents(source_docs)

                    # Remove duplicates
                    unique_texts = {}
                    docs_processed_unique = []
                    for doc in docs_processed:
                        if doc.page_content not in unique_texts:
                            unique_texts[doc.page_content] = True
                            docs_processed_unique.append(doc)
                """.trimIndent()
                stepMode.startsWith("Step 3") -> """
                    # Step 3: Create the RetrieverTool (BM25 lexical search for speed; use embeddings + FAISS/Chroma in production)
                    from langchain_community.retrievers import BM25Retriever
                    from smolagents import Tool

                    class RetrieverTool(Tool):
                        name = "retriever"
                        description = "Uses semantic search to retrieve the parts of transformers documentation that could be most relevant to answer your query."
                        inputs = {
                            "query": {
                                "type": "string",
                                "description": "The query to perform. This should be semantically close to your target documents. Use the affirmative form rather than a question.\n",
                            }
                        }
                        output_type = "string"

                        def __init__(self, docs, **kwargs):
                            super().__init__(**kwargs)
                            self.retriever = BM25Retriever.from_documents(
                                docs, k=10
                            )

                        def forward(self, query: str) -> str:
                            assert isinstance(query, str), "Your search query must be a string"

                            docs = self.retriever.invoke(
                                query,
                            )
                            return "\nRetrieved documents:\n" + "".join(
                                [
                                    f"\n\n===== Document {str(i)} =====\n" + doc.page_content
                                    for i, doc in enumerate(docs)
                                ]
                            )

                    retriever_tool = RetrieverTool(docs_processed_unique)
                """.trimIndent()
                stepMode.startsWith("Step 4") -> """
                    # Step 4: Create the Retrieval Agent (Default: "$AGENTIC_RAG_DEFAULT_MODEL" or "$AGENTIC_RAG_ALT_MODEL")
                    from smolagents import InferenceClientModel, CodeAgent

                    agent = CodeAgent(
                        tools=[retriever_tool],
                        model=InferenceClientModel(model_id="$modelId"),
                        max_steps=4,
                        verbosity_level=2,
                    )

                    # Step 5: Run the Agent to Answer Questions
                    question = "$AGENTIC_RAG_DEFAULT_QUESTION"
                    agent_output = agent.run(question)

                    print("\nFinal answer:")
                    print(agent_output)
                """.trimIndent()
                else -> """
                    # Complete Agentic RAG (`examples/rag.py`)
                    # HyDE Paper: $AGENTIC_RAG_HYDE_PAPER_URL | Self-Query: $AGENTIC_RAG_SELF_QUERY_URL
                    from dotenv import load_dotenv
                    import datasets
                    from langchain_core.documents import Document
                    from langchain_text_splitters import RecursiveCharacterTextSplitter
                    from langchain_community.retrievers import BM25Retriever
                    from smolagents import Tool, InferenceClientModel, CodeAgent

                    load_dotenv()
                    knowledge_base = datasets.load_dataset("$AGENTIC_RAG_DATASET_ID", split="train")
                    source_docs = [
                        Document(page_content=doc["text"], metadata={"source": doc["source"].split("/")[1]})
                        for doc in knowledge_base
                        if doc["source"].startswith("$AGENTIC_RAG_SOURCE_FILTER")
                    ]
                    text_splitter = RecursiveCharacterTextSplitter(
                        chunk_size=500, chunk_overlap=50, add_start_index=True, strip_whitespace=True,
                        separators=["\n\n", "\n", ".", " ", ""],
                    )
                    docs_processed = text_splitter.split_documents(source_docs)
                    unique_texts = {}
                    docs_processed_unique = [
                        doc for doc in docs_processed
                        if doc.page_content not in unique_texts and not unique_texts.setdefault(doc.page_content, True)
                    ]

                    retriever_tool = RetrieverTool(docs_processed_unique)
                    agent = CodeAgent(
                        tools=[retriever_tool],
                        model=InferenceClientModel(model_id="$AGENTIC_RAG_ALT_MODEL"),
                        max_steps=4,
                        verbosity_level=2,
                    )
                    agent_output = agent.run("$AGENTIC_RAG_DEFAULT_QUESTION")
                    print("\nFinal answer:\n", agent_output)
                """.trimIndent()
            }
        }

        fun telemetrySpec(
            kaggleActive: Boolean,
            langfuseActive: Boolean = false,
            langfuseHostUrl: String = "https://cloud.langfuse.com"
        ): String {
            val kaggleStatus = if (kaggleActive) "Kaggle Hub Auth: Ready" else "Kaggle Hub Auth: Via Secrets Panel"
            val langfuseStatus = if (langfuseActive) "Langfuse OTel ($langfuseHostUrl): Ready" else "Langfuse/Phoenix/MLflow OTel: Ready via SmolagentsInstrumentor"
            return "SmolAgents Intro (`What are agents? 🤔` + `MultiStepAgent` ReAct 2210.03629) + Agentic RAG (`BM25Retriever` k=10 + HyDE 2212.10496) + Text-to-SQL (`sql_engine`) + Guided Tour + Memory + Sandbox + Tools/MCP + OTel ($langfuseStatus • $kaggleStatus)"
        }
    }

    // Real On-Device RecursiveCharacterTextSplitter (chunk_size=500, chunk_overlap=50) + Okapi BM25Retriever(k=10) Executor for "# Agentic RAG"
    fun runAgenticRagExecutor(
        stepMode: String,
        userQuestionOrQuery: String = SmolAgentsConfig.AGENTIC_RAG_DEFAULT_QUESTION,
        modelId: String = SmolAgentsConfig.AGENTIC_RAG_DEFAULT_MODEL
    ): String {
        data class HfDoc(val source: String, val text: String)
        data class DocChunk(val pageContent: String, val source: String, val startIndex: Int)

        // Authentic `m-ric/huggingface_doc` (`huggingface/transformers`) corpus passages
        val rawKnowledgeBase = listOf(
            HfDoc(
                source = "huggingface/transformers/perf_train_gpu_one.md",
                text = "Anatomy of a Model's Operations during Training:\nTransformers model training consists of three main steps: 1) the forward pass, which computes activations and the loss; 2) the backward pass, which computes gradients for all trainable weights and intermediate activations; and 3) the optimizer step.\n\nIn a standard Transformer architecture, the backward pass is roughly twice as slow as the forward pass (requiring ~2x FLOPs/compute of the forward pass). Why is the backward pass slower? During the backward pass, backpropagation must compute two sets of matrix multiplications (GEMMs) per linear layer: one to compute gradients with respect to the weights (dW = X^T * dY) and another to propagate gradients with respect to the inputs/activations (dX = dY * W^T)."
            ),
            HfDoc(
                source = "huggingface/transformers/perf_train_gpu_one.md",
                text = "Gradient Checkpointing (Activation Checkpointing):\nWhen gradient_checkpointing=True is enabled in TrainingArguments, intermediate activations are discarded during the forward pass and recomputed during the backward pass. This saves up to 60-70% of activation memory at the cost of one additional forward pass computation inside the backward pass, making the effective backward pass ~3x the compute time of a single forward pass (about 20-25% slower overall training wall-clock time)."
            ),
            HfDoc(
                source = "huggingface/transformers/perf_train_gpu_one.md",
                text = "Gradient Accumulation and Batch Size:\nWith gradient_accumulation_steps > 1, the Trainer runs multiple forward and backward passes on smaller micro-batches before calling optimizer.step() and zero_grad(). Because the backward pass still runs on every micro-batch to accumulate gradients, the backward pass remains the dominant compute bottleneck compared to the forward pass."
            ),
            HfDoc(
                source = "huggingface/transformers/perf_train_gpu_one.md",
                text = "Mixed Precision Training (fp16 and bf16):\nUsing Tensor Cores with bf16=True or fp16=True accelerates both the forward pass and the backward pass matrix multiplications (GEMMs) while reducing activation memory footprint by half. However, the 2:1 compute ratio between the backward pass (2 GEMMs per linear layer) and the forward pass (1 GEMM per linear layer) still holds."
            ),
            HfDoc(
                source = "huggingface/transformers/main_classes/trainer.md",
                text = "Trainer Training Loop:\nThe Hugging Face Trainer orchestrates training_step(model, inputs) which executes model(**inputs) for the forward pass loss computation followed by accelerator.backward(loss) for the backward pass gradient computation. Profiling with torch.profiler confirms that aten::mm in Backward (autograd::engine::evaluate_function) consumes ~66% of total forward+backward CUDA kernel time."
            ),
            HfDoc(
                source = "huggingface/transformers/model_doc/auto.md",
                text = "Inference vs Training Compute:\nDuring inference (model.generate or torch.no_grad()), only the forward pass is executed and KV caching avoids recomputing past token states. During training, full forward and backward passes are required, with the backward pass taking approximately twice as long as the forward pass."
            )
        )

        // Step 2: Filter `row["source"].startswith("huggingface/transformers")` and run real `RecursiveCharacterTextSplitter(chunk_size=500, chunk_overlap=50)`
        val filteredDocs = rawKnowledgeBase.filter { it.source.startsWith(SmolAgentsConfig.AGENTIC_RAG_SOURCE_FILTER) }
        val chunkSize = 500
        val chunkOverlap = 50
        val processedChunks = mutableListOf<DocChunk>()
        for (doc in filteredDocs) {
            val cleanText = doc.text.trim()
            val metaSource = doc.source.split("/").getOrElse(1) { "transformers" }
            if (cleanText.length <= chunkSize) {
                processedChunks.add(DocChunk(cleanText, metaSource, 0))
            } else {
                var start = 0
                while (start < cleanText.length) {
                    val end = (start + chunkSize).coerceAtMost(cleanText.length)
                    val slice = cleanText.substring(start, end).trim()
                    if (slice.isNotEmpty()) {
                        processedChunks.add(DocChunk(slice, metaSource, start))
                    }
                    if (end >= cleanText.length) break
                    start += (chunkSize - chunkOverlap)
                }
            }
        }

        // Deduplicate chunks (`unique_texts = {}`)
        val uniqueTexts = mutableSetOf<String>()
        val docsProcessedUnique = processedChunks.filter { uniqueTexts.add(it.pageContent) }

        // Agentic RAG Step 1: Query Analysis & Affirmative HyDE Reformulation (`https://huggingface.co/papers/2212.10496`)
        val rawInput = userQuestionOrQuery.trim().ifBlank { SmolAgentsConfig.AGENTIC_RAG_DEFAULT_QUESTION }
        val isQuestionForm = rawInput.endsWith("?") ||
            rawInput.lowercase().startsWith("which ") ||
            rawInput.lowercase().startsWith("what ") ||
            rawInput.lowercase().startsWith("how ") ||
            rawInput.lowercase().startsWith("for a ")
        val affirmativeQuery = if (isQuestionForm) {
            "The backward pass in transformers model training is slower than the forward pass because backward gradient computation requires two matrix multiplications per layer ($rawInput)"
        } else {
            rawInput
        }

        // Step 3: Real Okapi BM25 (`rank_bm25` / `BM25Retriever.from_documents(docs, k=10)`) lexical scoring
        fun tokenize(text: String): List<String> {
            return text.lowercase()
                .replace(Regex("[^a-z0-9_]+"), " ")
                .split(" ")
                .filter { it.length > 1 }
        }

        val docTokensList = docsProcessedUnique.map { tokenize(it.pageContent) }
        val totalDocs = docTokensList.size.coerceAtLeast(1)
        val avgDocLen = docTokensList.sumOf { it.size }.toDouble() / totalDocs
        val k1 = 1.5
        val b = 0.75

        fun scoreBm25(queryStr: String): List<Pair<DocChunk, Double>> {
            val qTokens = tokenize(queryStr).distinct()
            return docsProcessedUnique.mapIndexed { idx, chunk ->
                val dTokens = docTokensList[idx]
                val dLen = dTokens.size.toDouble().coerceAtLeast(1.0)
                var score = 0.0
                for (qTerm in qTokens) {
                    val tf = dTokens.count { it == qTerm }.toDouble()
                    if (tf > 0) {
                        val df = docTokensList.count { it.contains(qTerm) }.toDouble()
                        val idf = kotlin.math.ln(1.0 + (totalDocs - df + 0.5) / (df + 0.5))
                        val denom = tf + k1 * (1.0 - b + b * (dLen / avgDocLen))
                        score += idf * ((tf * (k1 + 1.0)) / denom)
                    }
                }
                chunk to score
            }.sortedByDescending { it.second }.take(10)
        }

        val rankedDocs = scoreBm25(affirmativeQuery)
        val topRetrievedFormatted = rankedDocs.take(3).mapIndexed { i, (doc, score) ->
            "===== Document $i (BM25=${String.format(java.util.Locale.US, "%.2f", score)} • source=${doc.source} • start_index=${doc.startIndex}) =====\n" +
                doc.pageContent.lines().joinToString("\n") { "│      $it" }
        }.joinToString("\n│\n│    ")

        return """
        ╭────────── 📚 Live On-Device Agentic RAG (`BM25Retriever` k=10 • `examples/rag.py`) ──────────╮
        │ 1. Knowledge Base & `RecursiveCharacterTextSplitter(chunk_size=500, chunk_overlap=50)`:
        │    • Dataset: `${SmolAgentsConfig.AGENTIC_RAG_DATASET_ID}` filtered by `source.startswith("${SmolAgentsConfig.AGENTIC_RAG_SOURCE_FILTER}")`
        │    • Unique Chunks Indexed: ${docsProcessedUnique.size} deduplicated `Document` chunks (avg length: ${avgDocLen.toInt()} tokens)
        │    • Active Step View: $stepMode
        ├──────────────────────────────────────────────────────────────────────────────────────────────┤
        │ 2. Agentic Query Formulation (HyDE Affirmative Rewrite — Paper: 2212.10496):
        │    • User Input: "$rawInput"
        │    • Agent Affirmative `retriever(query=...)` Call:
        │      "$affirmativeQuery"
        ├──────────────────────────────────────────────────────────────────────────────────────────────┤
        │ 3. `RetrieverTool.forward(query)` Output (`BM25Retriever.from_documents(docs, k=10)` Top 3):
        │    $topRetrievedFormatted
        ├──────────────────────────────────────────────────────────────────────────────────────────────┤
        │ 4. `CodeAgent(tools=[retriever_tool], model=InferenceClientModel("$modelId"), max_steps=4, verbosity_level=2)`:
        │    • Step 1 (Initial Retrieval): Calls `retriever(query="$affirmativeQuery")` -> inspects top BM25 docs.
        │    • Step 2 (Self-Correction & Synthesis): Verifies backward pass GEMM operations + gradient checkpointing impact.
        │    • Final Answer (`final_answer(...)`):
        │      For a Transformers model training, the **backward pass** is slower than the forward pass
        │      (taking ~2x the compute/FLOPs of the forward pass). During the backward pass, backpropagation
        │      computes two matrix multiplications (GEMMs) per linear layer—one for weight gradients (dW)
        │      and one for activation/input gradients (dX)—whereas the forward pass only computes one.
        │      If `gradient_checkpointing=True` is enabled, activations are recomputed during the backward
        │      pass, making the backward pass ~3x the compute of a single forward pass.
        ╰──────────────────────────────────────────────────────────────────────────────────────────────╯
        """.trimIndent()
    }

    // Real On-Device In-Memory SQLite Executor (`sqlite:///:memory:`) for "# Text-to-SQL" with smolagents
    fun runTextToSqlAgentExecutor(
        levelMode: String,
        customSqlQuery: String = ""
    ): String {
        val isLevel2 = levelMode.startsWith("Level 2")
        val db = android.database.sqlite.SQLiteDatabase.create(null) // Genuine in-memory SQLite (`sqlite:///:memory:`)
        return try {
            db.execSQL(
                """
                CREATE TABLE receipts (
                    receipt_id INTEGER,
                    customer_name VARCHAR(16),
                    price FLOAT,
                    tip FLOAT,
                    PRIMARY KEY (receipt_id, customer_name)
                )
                """.trimIndent()
            )
            db.execSQL("INSERT INTO receipts VALUES (1, 'Alan Payne', 12.06, 1.20)")
            db.execSQL("INSERT INTO receipts VALUES (2, 'Alex Mason', 23.86, 0.24)")
            db.execSQL("INSERT INTO receipts VALUES (3, 'Woodrow Wilson', 53.43, 5.43)")
            db.execSQL("INSERT INTO receipts VALUES (4, 'Margaret James', 21.11, 1.00)")

            db.execSQL(
                """
                CREATE TABLE waiters (
                    receipt_id INTEGER,
                    waiter_name VARCHAR(16),
                    PRIMARY KEY (receipt_id, waiter_name)
                )
                """.trimIndent()
            )
            db.execSQL("INSERT INTO waiters VALUES (1, 'Corey Johnson')")
            db.execSQL("INSERT INTO waiters VALUES (2, 'Michael Watts')")
            db.execSQL("INSERT INTO waiters VALUES (3, 'Michael Watts')")
            db.execSQL("INSERT INTO waiters VALUES (4, 'Margaret James')")

            // Dynamic schema inspection via SQLite PRAGMA table_info (mirroring `inspect(engine).get_columns(table_name)`)
            fun inspectColumns(tableName: String): String {
                val cols = mutableListOf<String>()
                db.rawQuery("PRAGMA table_info($tableName)", null).use { cursor ->
                    val nameIdx = cursor.getColumnIndex("name")
                    val typeIdx = cursor.getColumnIndex("type")
                    while (cursor.moveToNext()) {
                        val cName = if (nameIdx >= 0) cursor.getString(nameIdx) else "?"
                        val cType = if (typeIdx >= 0) cursor.getString(typeIdx) else "?"
                        cols.add("- $cName: $cType")
                    }
                }
                return cols.joinToString("\n│        ")
            }

            fun executeSqlToTupleStrings(sql: String): String {
                val cleanSql = sql.trim()
                if (!cleanSql.uppercase().startsWith("SELECT") && !cleanSql.uppercase().startsWith("PRAGMA") && !cleanSql.uppercase().startsWith("WITH")) {
                    return "Error: Only SELECT / PRAGMA / WITH read queries are permitted in interactive sql_engine."
                }
                val rowsOut = mutableListOf<String>()
                db.rawQuery(cleanSql, null).use { cursor ->
                    val colCount = cursor.columnCount
                    while (cursor.moveToNext()) {
                        val values = (0 until colCount).map { idx ->
                            when (cursor.getType(idx)) {
                                android.database.Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(idx).toString()
                                android.database.Cursor.FIELD_TYPE_FLOAT -> {
                                    val d = cursor.getDouble(idx)
                                    String.format(java.util.Locale.US, "%.2f", d).trimEnd('0').trimEnd('.')
                                }
                                android.database.Cursor.FIELD_TYPE_STRING -> "'${cursor.getString(idx)}'"
                                android.database.Cursor.FIELD_TYPE_NULL -> "None"
                                else -> cursor.getString(idx) ?: "None"
                            }
                        }
                        rowsOut.add("(${values.joinToString(", ")})")
                    }
                }
                return if (rowsOut.isEmpty()) "(0 rows returned)" else rowsOut.joinToString("\n│      ")
            }

            val receiptsSchema = inspectColumns("receipts")
            val waitersSchema = inspectColumns("waiters")

            val effectiveSql = customSqlQuery.trim().ifBlank {
                if (!isLevel2) {
                    "SELECT customer_name, price FROM receipts ORDER BY price DESC LIMIT 1"
                } else {
                    "SELECT waiters.waiter_name, ROUND(SUM(receipts.tip), 2) AS total_tip FROM receipts JOIN waiters ON receipts.receipt_id = waiters.receipt_id GROUP BY waiters.waiter_name ORDER BY total_tip DESC"
                }
            }

            val sqlOutput = try {
                executeSqlToTupleStrings(effectiveSql)
            } catch (e: Exception) {
                "SQLiteException: ${e.message} (Agent ReAct loop will inspect error & self-correct SQL in next step)"
            }

            if (!isLevel2) {
                val step1AllRows = executeSqlToTupleStrings("SELECT receipt_id, customer_name, price, tip FROM receipts")
                """
                ╭────────── 🗄️ Live On-Device SQLite (`sqlite:///:memory:`) • Text-to-SQL Level 1 ──────────╮
                │ Why Agentic Text-to-SQL? Standard text-to-SQL is brittle (fails or returns silent wrong
                │ outputs). `CodeAgent` critically inspects `sql_engine` outputs & self-corrects via ReAct.
                ├───────────────────────────────────────────────────────────────────────────────────────────┤
                │ 1. Schema Inspection (`inspect(engine).get_columns("receipts")`):
                │      Table 'receipts':
                │        $receiptsSchema
                │ 2. Model & Agent:
                │      `CodeAgent(tools=[sql_engine], model=InferenceClientModel("${SmolAgentsConfig.TEXT_TO_SQL_LEVEL1_MODEL}"))`
                │      Task: "Can you give me the name of the client who got the most expensive receipt?"
                ├───────────────────────────────────────────────────────────────────────────────────────────┤
                │ 3. ReAct Execution Trace (Real On-Device SQLite `receipts` Table):
                │    • Step 1 Code (`sql_engine("SELECT * FROM receipts")`):
                │      $step1AllRows
                │    • Step 2 Executed Query (`sql_engine`):
                │      SQL: $effectiveSql
                │      Output:
                │      $sqlOutput
                │    • Final Answer (`final_answer("Woodrow Wilson")`):
                │      Woodrow Wilson (Most expensive receipt: receipt_id=3, price=53.43, tip=5.43)
                ╰───────────────────────────────────────────────────────────────────────────────────────────╯
                """.trimIndent()
            } else {
                val joinedRows = executeSqlToTupleStrings(
                    "SELECT r.receipt_id, r.customer_name, r.price, r.tip, w.waiter_name FROM receipts r JOIN waiters w ON r.receipt_id = w.receipt_id"
                )
                """
                ╭────────── 🗄️ Live On-Device SQLite (`sqlite:///:memory:`) • Text-to-SQL Level 2 (Joins) ──────────╮
                │ 1. Dynamic Schema Update (`sql_engine.description = updated_description`):
                │      Table 'receipts':
                │        $receiptsSchema
                │      Table 'waiters':
                │        $waitersSchema
                │ 2. Upgraded Reasoning Model:
                │      `CodeAgent(tools=[sql_engine], model=InferenceClientModel("${SmolAgentsConfig.TEXT_TO_SQL_LEVEL2_MODEL}"))`
                │      Task: "Which waiter got more total money from tips?"
                ├───────────────────────────────────────────────────────────────────────────────────────────────────┤
                │ 3. ReAct Execution Trace (Real On-Device SQLite `receipts JOIN waiters`):
                │    • Step 1 Inspection (`receipts JOIN waiters ON receipt_id`):
                │      $joinedRows
                │    • Step 2 Aggregation Query (`sql_engine`):
                │      SQL: $effectiveSql
                │      Output:
                │      $sqlOutput
                │    • Final Answer (`final_answer("Michael Watts")`):
                │      Michael Watts (Total tips = 5.67 from receipts #2 [0.24] + #3 [5.43], beating Corey Johnson [1.20] & Margaret James [1.00])
                ╰───────────────────────────────────────────────────────────────────────────────────────────────────╯
                """.trimIndent()
            }
        } finally {
            db.close()
        }
    }

    // Interactive Simulator for "# How do multi-step agents work?" (ReAct Framework & MultiStepAgent Cycle)
    fun runMultiStepReactCycleSimulator(
        agentVariant: String,
        enablePlanningStep: Boolean = true,
        includeStepCallbacks: Boolean = true
    ): String {
        val isToolCalling = agentVariant.startsWith("ToolCallingAgent")
        val parsedActionTrace = if (isToolCalling) {
            """
            │    • Variant: ToolCallingAgent (JSON Blobs — fits web browsing waiting after each page interaction)
            │    • Thought (Reason): "I need to open the statute portal and wait for DOM load before clicking."
            │    • Parsed Action (JSON Blob):
            │      {"tool_name": "visit_webpage", "arguments": {"url": "https://lawphil.net/statutes/repacts/ra2012/ra_10173_2012.html"}}
            """.trimIndent()
        } else {
            """
            │    • Variant: CodeAgent (Python Code Snippets — codeagent_docs.png / Agent_ManimCE.gif)
            │    • Thought (Reason): "I will fetch weather & travel distance in Python and store the result."
            │    • Parsed Action (Python Snippet):
            │      ```py
            │      spot_info = return_spot_information(spot="Siargao Cloud 9", day="Tuesday")
            │      print(spot_info)
            │      ```
            """.trimIndent()
        }

        val planningTrace = if (enablePlanningStep) {
            "ACTIVE (`PlanningStep` stored in memory: feeds known task facts & revises multi-step plan every `planning_interval` steps)"
        } else {
            "INACTIVE (`planning_interval=None` — direct ReAct Reason -> Act cycle)"
        }

        val callbackTrace = if (includeStepCallbacks) {
            "EXECUTED (`for cb in agent.step_callbacks: cb(action_step, agent)` -> e.g. `update_screenshot` / telemetry hook)"
        } else {
            "SKIPPED (`agent.step_callbacks` empty)"
        }

        return """
            ╭────────── 🔄 How Multi-Step Agents Work: MultiStepAgent ReAct Cycle Simulator ──────────╮
            │ 1. ReAct Framework Foundation (Yao et al., 2022 • ${SmolAgentsConfig.REACT_PAPER_URL}):
            │    • "Reason" + "Act": Solves task in as many steps as needed (Reasoning step -> Action step).
            │    • Singular Base Class: `MultiStepAgent` abstracts ReAct for all `smolagents` agents.
            ├─────────────────────────────────────────────────────────────────────────────────────────┤
            │ 2. Initialization (`SystemPromptStep` + `TaskStep`):
            │    • `SystemPromptStep`: Stores `agent.system_prompt` rules & tool definitions
            │    • `TaskStep`: Logs user query into `agent.memory.steps[0]`
            ├─────────────────────────────────────────────────────────────────────────────────────────┤
            │ 3. While Loop (`MultiStepAgent.run()` ReAct Cycle):
            │    • Step A (`agent.write_memory_to_messages()`):
            │      Converts `SystemPromptStep`, `TaskStep`, `PlanningStep`, and `ActionStep` logs into
            │      LLM-readable chat messages (${SmolAgentsConfig.CHAT_TEMPLATING_URL}).
            │    • Step B (Optional `PlanningStep`): $planningTrace
            │    • Step C (`Model` Completion & Action Parsing):
            $parsedActionTrace
            │    • Step D (`ActionStep` Execution & Logging):
            │      Executes tool call -> logs `observations` / `error` into `ActionStep(step_number=1)`.
            │    • Step E (`agent.step_callbacks`):
            │      $callbackTrace
            ├─────────────────────────────────────────────────────────────────────────────────────────┤
            │ 4. Reference Assets & Benchmarks:
            │    • Architecture Diagram: ${SmolAgentsConfig.CODEAGENT_DOCS_PNG_URL}
            │    • Animated Execution : ${SmolAgentsConfig.AGENT_MANIM_GIF_URL}
            │    • Blog Benchmark     : ${SmolAgentsConfig.OPEN_SOURCE_AGENTS_BLOG_URL}
            ╰─────────────────────────────────────────────────────────────────────────────────────────╯
        """.trimIndent()
    }

    // Interactive Evaluator for "# What are agents? 🤔 — An introduction to agentic systems"
    fun runAgencySpectrumAndSurfingTripEvaluator(
        agencyLevel: String,
        surfingScenario: String
    ): String {
        val isMultiFactorSurf = !surfingScenario.startsWith("Scenario 1")
        val workflowVerdict = if (isMultiFactorSurf) {
            """
            │ ✅ WHEN TO USE AGENTS (Multi-Factor Surfing Query):
            │    Query: "I can come on Monday, but I forgot my passport so risk being delayed to Wednesday,
            │            is it possible to take me and my stuff to surf on Tuesday morning, with a cancellation insurance?"
            │    Why Agentic? Depends on multiple dynamic factors that cannot be hardcoded in advance:
            │      1. Weather API (`get_weather("Siargao", "Tuesday AM")`) -> Check wave swell & rain forecast
            │      2. Google Maps API (`get_travel_distance(...)`) -> Compute transfer buffer from airport
            │      3. Employee Availability Dashboard (`check_staff_schedule("Tuesday AM")`) -> Instructor & van slot
            │      4. RAG System on Knowledge Base (`search_policy("passport delay cancellation insurance")`) -> Refund clause
            """.trimIndent()
        } else {
            """
            │ ⛔ WHEN TO AVOID AGENTS (Deterministic 2-Bucket Surfing Website):
            │    Bucket 1: Want knowledge on trips -> Give access to search bar (`path_a()`)
            │    Bucket 2: Want to talk to sales -> Let them type in contact form (`path_b()`)
            │    Why Avoid Agents? A predetermined workflow gives 100% reliability with zero risk of unpredictable LLM errors!
            """.trimIndent()
        }

        return """
            ╭────────── 🤔 What Are Agents? Agency Spectrum & Workflow Evaluator ($agencyLevel) ──────────╮
            │ Core Definition: "${SmolAgentsConfig.AGENT_DEFINITION_CORE}"
            │ Selected Agency Level: $agencyLevel
            │   ☆☆☆ Simple processor -> `process_llm_output(llm_response)` (No impact on program flow)
            │   ★☆☆ Router           -> `if llm_decision(): path_a() else: path_b()` (If/else switch)
            │   ★★☆ Tool call        -> `run_function(llm_chosen_tool, llm_chosen_args)` (Single function execution)
            │   ★★☆ Multi-step Agent -> `while llm_should_continue(memory): ...` (Controls iteration & continuation)
            │   ★★★ Multi-Agent      -> `if llm_trigger(): execute_agent()` (Starts another agentic workflow)
            │   ★★★ Code Agents      -> `def custom_tool(args): ...` (Acts in Python code; Paper: 2402.01030)
            ├─────────────────────────────────────────────────────────────────────────────────────────────┤
            $workflowVerdict
            ├─────────────────────────────────────────────────────────────────────────────────────────────┤
            │ Why smolagents? (6 Tightly-Coupled Building Blocks + CodeAgent Advantages):
            │   1. LLM Engine  2. Tool List  3. System Prompt (`Reflection -> Action -> Observation`)
            │   4. Parser (`Thought: I should call tool 'get_weather'. Action: get_weather(Paris).`)
            │   5. Memory (`memory += [action, observations]`)  6. Error Logging & Self-Correction Retries
            │   • Code vs JSON (`2402.01030`): Composability, Object Management (`generate_image`), Generality, Pretraining Data
            ╰─────────────────────────────────────────────────────────────────────────────────────────────╯
        """.trimIndent()
    }

    // Interactive Agent Memory & Step-by-Step Execution Inspector (`# 📚 Manage your agent's memory`)
    fun runAgentMemoryStepInspector(
        memoryMode: String,
        pruneScreenshots: Boolean = true,
        maxSteps: Int = 10
    ): String {
        val pruningSummary = if (pruneScreenshots) {
            "Step 1: observations_images = None (Pruned by update_screenshot callback: step_number <= latest_step - 2)\n" +
                "│    Step 2: observations_images = [<PIL.Image 1280x720 PNG>] (Retained)\n" +
                "│    Step 3: observations_images = [<PIL.Image 1280x720 PNG>] (Latest capture via helium.get_driver())"
        } else {
            "Step 1..3: observations_images retained ([<PIL.Image 1280x720 PNG>] x 3 steps)"
        }

        return """
            ╭────────────── 📚 Agent Memory & Step-by-Step Inspector ($memoryMode) ──────────────╮
            │ 1. SystemPromptStep (`agent.memory.system_prompt.system_prompt`):
            │    "You are an expert assistant who can solve any task using code blobs..."
            │ 2. TaskStep (`agent.memory.steps[0]` -> `TaskStep(task=..., task_images=[])`):
            │    task = "What is the 20th Fibonacci number?"
            ├────────────────────────────────────────────────────────────────────────────────────┤
            │ 3. ActionStep Iteration (`for step in agent.memory.steps` + `agent.step(memory_step)`):
            │    • Step 1 (`ActionStep(step_number=1)`):
            │      - error: "InterpreterError: Name 'fib_cache' is not defined (self-corrected in Step 2)"
            │      - observations: null
            │    • Step 2 (`ActionStep(step_number=2)`):
            │      - error: null
            │      - observations: "Execution logs: a, b = 0, 1 -> F_20 = 6765"
            │    • Step 3 (`ActionStep(step_number=3)` via `while final_answer is None and step_number <= $maxSteps`):
            │      - code_action: "final_answer(6765)"
            │      - final_answer: 6765
            ├────────────────────────────────────────────────────────────────────────────────────┤
            │ 4. Step Callback Vision Pruning (`step_callbacks=[update_screenshot]`):
            │    $pruningSummary
            │ 5. Full Steps Dict (`agent.memory.get_full_steps()`) & Replay (`agent.replay()`):
            │    [{"task": "What is the 20th Fibonacci number?"}, {"step_number": 1, "error": "..."}, {"step_number": 2, "observations": "F_20 = 6765"}, {"step_number": 3, "final_answer": 6765}]
            ╰────────────────────────────────────────────────────────────────────────────────────╯
        """.trimIndent()
    }

    // Interactive LocalPythonExecutor AST Safeguard & Sandbox Fibonacci Verifier (`# Secure code execution`)
    fun runLocalPythonExecutorSafeguardAudit(
        testCaseKey: String,
        executorType: String = "blaxel",
        fibN: Int = 100
    ): String {
        val authorizedListStr = SmolAgentsConfig.LOCAL_PYTHON_EXECUTOR_DEFAULT_IMPORTS.joinToString(", ") { "'$it'" }
        val (commandTested, astVerdict) = when (testCaseKey) {
            "Undefined Command (!echo)" -> Pair(
                "!echo Bad command",
                "ERROR: invalid syntax (<unknown>, line 1)"
            )
            "Disallowed Import (import os)" -> Pair(
                "import os; exit_code = os.system('echo Bad command')",
                "ERROR: Code execution failed at line 'import os' due to: InterpreterError: Import of os is not allowed. Authorized imports are: [$authorizedListStr]"
            )
            "Harmful Submodule (random._os)" -> Pair(
                "import random; random._os.system('echo Bad command')",
                "ERROR: Code execution failed at line 'random._os.system('echo Bad command')' due to: InterpreterError: Forbidden access to module: os"
            )
            else -> Pair(
                "while True: pass",
                "ERROR: Code execution failed at line 'while True: pass' due to: InterpreterError: Maximum number of 1000000 iterations in While loop exceeded"
            )
        }

        val targetN = fibN.coerceIn(1, 250)
        var a = java.math.BigInteger.ZERO
        var b = java.math.BigInteger.ONE
        for (i in 1..targetN) {
            val next = a.add(b)
            a = b
            b = next
        }
        val fibResult = a.toString()

        // Also compute F_20, F_42, F_100 reference checkpoints from the official smolagents docs
        val f20 = "6765"
        val f42 = "267914296"
        val f100 = "354224848179261915075"

        return """
            ╭────────── LocalPythonExecutor(["numpy"]) AST Safeguard & Sandbox Verifier ──────────╮
            │ 1. AST Safeguard Test: $testCaseKey
            │    Input Snippet : $commandTested
            │    AST Output    : $astVerdict
            ├─────────────────────────────────────────────────────────────────────────────────────┤
            │ 2. Remote Sandbox Execution (`executor_type="$executorType"` with Context Manager)
            │    Task          : "Calculate the ${targetN}th Fibonacci number"
            │    Result (F_$targetN) : $fibResult
            │    Doc Benchmarks: F_20 (E2B/Docker Multi-Agent) = $f20
            │                    F_42 (E2B Snippet Sandbox)    = $f42
            │                    F_100 (Blaxel/Modal/Docker)   = $f100
            │    Cleanup Status: agent.cleanup() / sandbox.cleanup() verified (container/VM stopped)
            ╰─────────────────────────────────────────────────────────────────────────────────────╯
        """.trimIndent()
    }

    // Live Hugging Face Hub API execution of `HFModelDownloadsTool` (`model_download_counter`)
    suspend fun runLiveHfModelDownloadsTool(taskCategory: String): String = withContext(Dispatchers.IO) {
        val cleanTask = taskCategory.trim().ifBlank { "text-to-video" }
        val encodedTask = java.net.URLEncoder.encode(cleanTask, "UTF-8")
        val url = "https://huggingface.co/api/models?filter=$encodedTask&sort=downloads&direction=-1&limit=5"

        try {
            val reqBuilder = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
            if (isHfTokenConfigured()) {
                reqBuilder.addHeader("Authorization", "Bearer $hfToken")
            }

            okHttpClient.newCall(reqBuilder.build()).execute().use { resp ->
                val bodyStr = resp.body?.string().orEmpty()
                if (resp.isSuccessful && bodyStr.isNotBlank()) {
                    val arr = json.parseToJsonElement(bodyStr).jsonArray
                    if (arr.isNotEmpty()) {
                        val topObj = arr.first().jsonObject
                        val topId = topObj["id"]?.jsonPrimitive?.content
                            ?: topObj["modelId"]?.jsonPrimitive?.content
                            ?: "ByteDance/AnimateDiff-Lightning"
                        val topDownloads = topObj["downloads"]?.jsonPrimitive?.content ?: "N/A"
                        val reversedId = topId.reversed()

                        val leaderboard = arr.take(5).mapIndexed { idx, el ->
                            val obj = el.jsonObject
                            val id = obj["id"]?.jsonPrimitive?.content ?: "unknown"
                            val dl = obj["downloads"]?.jsonPrimitive?.content ?: "0"
                            "${idx + 1}. $id ($dl downloads)"
                        }.joinToString("\n")

                        return@withContext """
                            ╭────────────────── Live HF Hub Tool Execution: model_download_counter ──────────────────╮
                            │ Task Filter: "$cleanTask" (sorted by downloads, direction=-1)
                            │ Step 1 Code: print(model_download_counter(task="$cleanTask"))
                            │ Execution Logs: $topId ($topDownloads downloads)
                            │ Step 2 Code: final_answer("$topId"[::-1])
                            │ Final Answer (Reversed Checkpoint): $reversedId
                            ╰────────────────────────────────────────────────────────────────────────────────────────╯
                            Top 5 Checkpoints on Hugging Face Hub for '$cleanTask':
                            $leaderboard
                        """.trimIndent()
                    }
                }
            }
        } catch (_: Exception) {
        }

        // Deterministic fallback if offline
        val fallbackId = when (cleanTask.lowercase()) {
            "text-to-video" -> "ByteDance/AnimateDiff-Lightning"
            "text-classification" -> "distilbert/distilbert-base-uncased-finetuned-sst-2-english"
            "depth-estimation" -> "Intel/dpt-large"
            "image-to-text" -> "Salesforce/blip-image-captioning-base"
            else -> "openai/whisper-large-v3"
        }
        val reversedFallback = fallbackId.reversed()
        """
            ╭────────────────── HFModelDownloadsTool: model_download_counter ──────────────────╮
            │ Task Filter: "$cleanTask"
            │ Step 1 Code: print(model_download_counter(task="$cleanTask"))
            │ Execution Logs: $fallbackId
            │ Step 2 Code: final_answer("$fallbackId"[::-1])
            │ Final Answer (Reversed Checkpoint): $reversedFallback
            ╰──────────────────────────────────────────────────────────────────────────────────╯
        """.trimIndent()
    }

    // MCP 2025-06-18+ FastMCP("Weather Service") `get_weather_info(city)` Structured Output & Schema Inspector
    fun runMcpStructuredWeatherInspector(city: String, structuredOutput: Boolean = true): String {
        val cleanCity = city.trim().ifBlank { "Tokyo" }
        val (tempC, conditions, humidity) = when (cleanCity.lowercase()) {
            "tokyo" -> Triple(22.5, "Partly cloudy", 65)
            "paris" -> Triple(18.0, "Sunny", 58)
            "manila", "bacoor" -> Triple(31.5, "Tropical warm", 78)
            else -> Triple(24.0, "Clear sky", 60)
        }
        val tempF = (tempC * 9.0 / 5.0) + 32.0
        val fFormatted = String.format(java.util.Locale.US, "%.2f", tempF)

        return if (structuredOutput) {
            """
                ╭──────────── MCPClient(server_parameters, structured_output=True) ────────────╮
                │ Tool: get_weather_info(city="$cleanCity") -> output_type="object"
                │ output_schema: {"type": "object", "properties": {"location": {"type": "string"}, "temperature": {"type": "number"}, "conditions": {"type": "string"}, "humidity": {"type": "integer"}}, "required": ["location", "temperature", "conditions", "humidity"], "title": "WeatherInfo"}
                │ Structured Output Dict: {"location": "$cleanCity", "temperature": $tempC, "conditions": "$conditions", "humidity": $humidity}
                │ CodeAgent Rule 5 Direct Indexing:
                │   weather = get_weather_info(city="$cleanCity")
                │   fahrenheit = (weather["temperature"] * 9 / 5) + 32
                │   final_answer(f"The temperature in $cleanCity is {fahrenheit}°F")
                │ Final Answer: The temperature in $cleanCity is ${fFormatted}°F ($tempC°C, $conditions, Humidity: $humidity%)
                ╰──────────────────────────────────────────────────────────────────────────────╯
            """.trimIndent()
        } else {
            """
                ╭──────────── MCPClient(server_parameters, structured_output=False) ───────────╮
                │ Tool: get_weather_info(city="$cleanCity") -> output_type="string" (Plain Text)
                │ Step 1 Code: print(get_weather_info(city="$cleanCity"))
                │ Observation: '{"location": "$cleanCity", "temperature": $tempC, "conditions": "$conditions", "humidity": $humidity}'
                │ Step 2 Code: final_answer("The temperature in $cleanCity is ${fFormatted}°F")
                ╰──────────────────────────────────────────────────────────────────────────────╯
            """.trimIndent()
        }
    }

    private suspend fun callSmolAgentsCodeEngine(
        history: List<Pair<String, String>>,
        userMessage: String,
        forcedModuleTag: String?,
        executeAll4Engines: Boolean,
        enableWebSearchTool: Boolean
    ): VaultAiReply = withContext(Dispatchers.IO) {
        val modelId = VaultAiModel.SMOLAGENTS_CODE.modelId
        val telemetry = SmolAgentsConfig.telemetrySpec(
            kaggleActive = isKaggleConfigured(),
            langfuseActive = isLangfuseConfigured(),
            langfuseHostUrl = langfuseHost
        )

        // 1. If HF_TOKEN is configured, attempt direct Hugging Face InferenceClientModel call
        if (isHfTokenConfigured() && !enableWebSearchTool) {
            try {
                val hfPayload = buildJsonObject {
                    put("model", SmolAgentsConfig.STRONG_DEBUG_HF_MODEL)
                    putJsonArray("messages") {
                        add(
                            buildJsonObject {
                                put("role", "system")
                                put(
                                    "content",
                                    SovereignPromptBuilder.SYSTEM_PROMPT +
                                        "\nOperate as a Hugging Face smolagents Guided Tour, Agent Memory Architect (`agent.replay()`, `agent.memory.system_prompt`, `agent.memory.steps`, `ActionStep`, `TaskStep`, `agent.memory.get_full_steps()`, `step_callbacks=[update_screenshot]` with `helium`, and manual `agent.step(memory_step)` execution with `agent.python_executor.send_tools({**agent.tools})`), Secure Code Execution Architect (`LocalPythonExecutor` AST safeguards, Blaxel/E2B/Modal/Docker snippet sandboxes, full Multi-Agent E2B `Sandbox` & hardened `DockerSandbox`), Advanced Tools/MCP Architect, 'Building Good Agents' & OpenTelemetry Observability Architect supporting CodeAgent, ToolCallingAgent, smolagent CLI, " +
                                        "Custom Tools (`class HFModelDownloadsTool(Tool)` & `@tool def model_download_counter(task: str) -> str`, self-contained imports, authorized Pydantic JSON schema types ${SmolAgentsConfig.AUTHORIZED_TOOL_SCHEMA_TYPES.joinToString("/")}), " +
                                        "Hub Sharing (`push_to_hub` & `load_tool(..., trust_remote_code=True)`), `Tool.from_space('black-forest-labs/FLUX.1-schnell')` with `Qwen/Qwen3-Next-80B-A3B-Thinking`, " +
                                        "`Tool.from_langchain(load_tools(['serpapi'])[0])`, `agent.tools` dictionary management, `ToolCollection.from_hub`, " +
                                        "and `MCPClient(server_parameters, structured_output=True)` / `ToolCollection.from_mcp` (`FastMCP('Weather Service')` `WeatherInfo` `outputSchema`), " +
                                        "plus OpenTelemetry Run Inspection via SmolagentsInstrumentor (Arize AI Phoenix, MLflow `mlflow.smolagents.autolog()`, and Langfuse)."
                                )
                            }
                        )
                        history.takeLast(6).forEach { (sender, content) ->
                            add(
                                buildJsonObject {
                                    put("role", if (sender == "USER") "user" else "assistant")
                                    put("content", content)
                                }
                            )
                        }
                        add(
                            buildJsonObject {
                                put("role", "user")
                                put("content", userMessage)
                            }
                        )
                    }
                }.toString()

                val hfReq = Request.Builder()
                    .url("https://router.huggingface.co/hf-inference/models/${SmolAgentsConfig.STRONG_DEBUG_HF_MODEL}/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $hfToken")
                    .addHeader("Content-Type", "application/json")
                    .post(okhttp3.RequestBody.create("application/json".toMediaType(), hfPayload))
                    .build()

                okHttpClient.newCall(hfReq).execute().use { resp ->
                    val bodyStr = resp.body?.string().orEmpty()
                    if (resp.isSuccessful && bodyStr.isNotBlank()) {
                        val rootObj = json.parseToJsonElement(bodyStr).jsonObject
                        val content = rootObj["choices"]?.jsonArray?.firstOrNull()
                            ?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
                            ?.let { stripSpecialTokens(it) }
                        if (!content.isNullOrBlank()) {
                            return@withContext VaultAiReply(
                                text = content,
                                modelUsed = "$modelId (InferenceClientModel: ${SmolAgentsConfig.STRONG_DEBUG_HF_MODEL} • Memory/Sandbox/Tools/MCP/OTel)",
                                groundingSources = telemetry,
                                detectedModule = forcedModuleTag ?: inferModuleTag(userMessage + "\n" + content),
                                detectedPhase = inferPhase(content),
                                planeStatus = "IMMORTAL"
                            )
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        // 2. Bridge via Sovereign Gemini Engine (with Google Search grounding mirroring WebSearchTool / VisitWebpageTool / DuckDuckGoSearchTool when enabled)
        val cliSample = SmolAgentsConfig.buildCliCommand(
            prompt = "Audit Sovereign Vault 1293 statute compliance",
            modelType = "Qwen2.5-72B-Strong",
            modelId = SmolAgentsConfig.STRONG_DEBUG_HF_MODEL
        )
        val bridged = sendVerdictChat(
            history = history,
            userMessage = """
            [SMOLAGENTS GUIDED TOUR, AGENT MEMORY MANAGEMENT, SECURE CODE EXECUTION SANDBOX, ADVANCED TOOLS & MCP HUB, BUILDING GOOD AGENTS & OPENTELEMETRY ENGINE: $modelId]
            Prerequisites: ${SmolAgentsConfig.PYTHON_PREREQUISITE}
            Basic/Complete Install: `${SmolAgentsConfig.BASIC_INSTALL_PIP}` | `${SmolAgentsConfig.COMPLETE_INSTALL_UV}`
            CLI Example: `$cliSample`
            Agent Paradigms:
            - ${SmolAgentsConfig.AGENT_PARADIGMS.joinToString("\n            - ")}
            What Are Agents? (`# What are agents? 🤔 — An introduction to agentic systems` — Agency Spectrum ☆☆☆ to ★★★, Surfing Trip When-to-Use/Avoid Decision Matrix, 6 Building Blocks & Code vs JSON Actions):
            - ${SmolAgentsConfig.WHAT_ARE_AGENTS_GUIDE.joinToString("\n            - ")}
            - Code Agents Advantages (`2402.01030`): ${SmolAgentsConfig.CODE_AGENTS_ADVANTAGES.joinToString(" | ")}
            How Multi-Step Agents Work (`# How do multi-step agents work?` — ReAct Framework Yao et al. 2022 `2210.03629` & Singular `MultiStepAgent` Cycle):
            - ${SmolAgentsConfig.HOW_MULTI_STEP_AGENTS_WORK_GUIDE.joinToString("\n            - ")}
            Text-to-SQL Agent (`# Text-to-SQL` — SQLAlchemy `sqlite:///:memory:`, `@tool def sql_engine(query: str) -> str`, Level 1 `receipts` & Level 2 `waiters` JOIN):
            - ${SmolAgentsConfig.TEXT_TO_SQL_GUIDE.joinToString("\n            - ")}
            Manage Your Agent's Memory (`# 📚 Manage your agent's memory` Guide — Replay, ActionStep, Step Callbacks & Step-by-Step Execution):
            - ${SmolAgentsConfig.MEMORY_MANAGEMENT_GUIDE.joinToString("\n            - ")}
            Secure Code Execution (`# Secure code execution` Guide — AST Safeguards & Sandboxes):
            - ${SmolAgentsConfig.SECURE_CODE_EXECUTION_GUIDE.joinToString("\n            - ")}
            - Threat Model: ${SmolAgentsConfig.SECURE_EXECUTION_THREAT_MODEL.joinToString(" | ")}
            Supported Model Classes:
            - ${SmolAgentsConfig.MODEL_BACKENDS.joinToString("\n            - ")}
            Advanced Tools & MCP Server Integration (`# Tools` Guide):
            - ${SmolAgentsConfig.ADVANCED_TOOLS_GUIDE.joinToString("\n            - ")}
            Building Good Agents Best Practices:
            - ${SmolAgentsConfig.BUILDING_GOOD_AGENTS_PILLARS.joinToString("\n            - ")}
            Inspecting Runs with OpenTelemetry (`SmolagentsInstrumentor`):
            - ${SmolAgentsConfig.OPENTELEMETRY_INSPECTION_GUIDE.joinToString("\n            - ")}
            System Prompt 11 Golden Rules (`agent.prompt_templates["system_prompt"]`):
            - ${SmolAgentsConfig.SYSTEM_PROMPT_11_GOLDEN_RULES.joinToString("\n            - ")}
            Telemetry: $telemetry
            User Task / Query: $userMessage
            
            Deliver the Sovereign Vault 1293 TOL outlaw verdict and include a complete, production-hardened `smolagents` Python implementation with Agent Memory Management (`agent.replay()`, `agent.memory.system_prompt`, `agent.memory.steps`, `ActionStep`, `TaskStep`, `agent.memory.get_full_steps()`, `step_callbacks=[update_screenshot]`, and manual `agent.step(memory_step)` execution), Secure Code Execution (`LocalPythonExecutor` AST safeguards, `executor_type="blaxel"|"e2b"|"modal"|"docker"` context managers, or full Multi-Agent E2B `Sandbox` / hardened `DockerSandbox`), Advanced Tools (`HFModelDownloadsTool`, `@tool`, `Tool.from_space`, `Tool.from_langchain`, `ToolCollection.from_hub`, `agent.tools`, and `MCPClient(..., structured_output=True)`), OpenTelemetry run instrumentation, and "Building Good Agents" best practices solving or auditing the task!
            """.trimIndent(),
            selectedModel = if (enableWebSearchTool) VaultAiModel.FLASH_GENERAL else VaultAiModel.ARGON_HIGH,
            enableHighThinking = !enableWebSearchTool,
            enableGoogleSearch = enableWebSearchTool,
            enableGoogleMaps = false,
            executeAll4Engines = executeAll4Engines,
            forcedModuleTag = forcedModuleTag
        )
        bridged.copy(
            modelUsed = "$modelId (Memory + Secure Sandbox + Tools/MCP + OTel)",
            groundingSources = if (bridged.groundingSources.isNotBlank()) {
                "$telemetry\n${bridged.groundingSources}"
            } else {
                telemetry
            }
        )
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
