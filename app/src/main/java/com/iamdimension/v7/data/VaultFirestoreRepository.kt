package com.iamdimension.v7.data

import com.example.data.model.BioscienceAudit
import com.example.data.model.ChatMessage
import com.example.data.model.VaultDocket
import com.example.data.remote.LrosContactRadarService
import com.example.data.repository.VaultRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Exact AI Studio Firebase Integration Constants for Commander Rex (`rexsarmiento1293@gmail.com`)
 */
const val EXACT_FIREBASE_PROJECT_ID = "gen-lang-client-0597903520"
const val EXACT_ANDROID_PACKAGE_NAME = "com.iamdimension.v7"
const val EXACT_FIRESTORE_DATABASE_ID = "ai-studio-android-iamsover-129d727b-49ce-41e6-b574-5705e44eced2"

@Serializable
data class KaggleVerificationResponse(
    val success: Boolean,
    val message: String = "",
    val databaseId: String = EXACT_FIRESTORE_DATABASE_ID
)

/**
 * Initializes and provides the exact named Firestore database instance:
 * `FirebaseFirestore.getInstance("ai-studio-android-iamsover-129d727b-49ce-41e6-b574-5705e44eced2")`
 * Never uses the default `(default)` database instance.
 */
class VaultFirestoreRepository(
    val db: FirebaseFirestore = FirebaseFirestore.getInstance(
        "ai-studio-android-iamsover-129d727b-49ce-41e6-b574-5705e44eced2"
    )
) {
    private val delegate = VaultRepository(db)

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    fun observeDockets(userId: String): Flow<List<VaultDocket>> = delegate.observeDockets(userId)

    suspend fun getUserDockets(): Result<List<VaultDocket>> = delegate.getUserDockets()

    suspend fun createDocket(
        title: String,
        caseSummary: String,
        moduleTag: String,
        phase: String = "GENESIS",
        planeStatus: String = "MORTAL",
        rulingText: String = "Initial blotter recorded. Ready for MGD deliberation."
    ): Result<String> = delegate.createDocket(
        title = title,
        caseSummary = caseSummary,
        moduleTag = moduleTag,
        phase = phase,
        planeStatus = planeStatus,
        rulingText = rulingText
    )

    fun observeChatMessages(userId: String): Flow<List<ChatMessage>> = delegate.observeChatMessages(userId)

    fun observeBioscienceAudits(userId: String): Flow<List<BioscienceAudit>> = delegate.observeBioscienceAudits(userId)

    /**
     * Calls the `verifyKaggleToken` Cloud Function connected to Database ID
     * `ai-studio-android-iamsover-129d727b-49ce-41e6-b574-5705e44eced2`.
     * Returns `{ success: true }` ONLY if the token is verified.
     */
    suspend fun verifyKaggleToken(userInputToken: String): KaggleVerificationResponse = withContext(Dispatchers.IO) {
        val trimmed = userInputToken.trim()
        if (trimmed.isEmpty()) {
            return@withContext KaggleVerificationResponse(
                success = false,
                message = "ACCESS DENIED • Empty Wake-Up Token",
                databaseId = EXACT_FIRESTORE_DATABASE_ID
            )
        }

        val endpoint = "https://us-central1-$EXACT_FIREBASE_PROJECT_ID.cloudfunctions.net/verifyKaggleToken"
        val payloadJson = """
            {
              "data": {
                "token": "$trimmed",
                "databaseId": "$EXACT_FIRESTORE_DATABASE_ID",
                "uid": "${auth.currentUser?.uid.orEmpty()}"
              }
            }
        """.trimIndent()

        try {
            val request = Request.Builder()
                .url(endpoint)
                .post(payloadJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string().orEmpty()
                    val root = json.parseToJsonElement(bodyStr).jsonObject
                    val directSuccess = root["success"]?.jsonPrimitive?.booleanOrNull
                    val nestedSuccess = root["result"]?.jsonObject?.get("success")?.jsonPrimitive?.booleanOrNull
                        ?: root["data"]?.jsonObject?.get("success")?.jsonPrimitive?.booleanOrNull
                    val isVerified = directSuccess ?: nestedSuccess ?: false
                    return@withContext KaggleVerificationResponse(
                        success = isVerified,
                        message = if (isVerified) {
                            "OWNER AUTHENTICATED • 100% CATCH BASIN OVERFLOW ACTIVE"
                        } else {
                            "ACCESS DENIED • INVALID WAKE-UP TOKEN"
                        },
                        databaseId = EXACT_FIRESTORE_DATABASE_ID
                    )
                }
            }
        } catch (_: Exception) {
            // Fall through to deterministic cryptographic checksum verifier when offline / local test
        }

        val verified = LrosContactRadarService.verifyBlackCaseKaggleKey(trimmed)
        KaggleVerificationResponse(
            success = verified,
            message = if (verified) {
                "OWNER AUTHENTICATED • 100% CATCH BASIN OVERFLOW ACTIVE"
            } else {
                "ACCESS DENIED • INVALID WAKE-UP TOKEN"
            },
            databaseId = EXACT_FIRESTORE_DATABASE_ID
        )
    }

    companion object {
        const val DATABASE_ID = "ai-studio-android-iamsover-129d727b-49ce-41e6-b574-5705e44eced2"

        fun createWithExactDatabase(): VaultFirestoreRepository {
            val firestoreInstance = FirebaseFirestore.getInstance(
                "ai-studio-android-iamsover-129d727b-49ce-41e6-b574-5705e44eced2"
            )
            return VaultFirestoreRepository(firestoreInstance)
        }
    }
}
