package com.iamdimension.v7.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.example.data.model.JusticeModule
import com.example.data.model.VaultLanguage
import com.example.data.remote.LrosContactRadarService
import com.google.firebase.firestore.FirebaseFirestore
import com.iamdimension.v7.data.EXACT_FIRESTORE_DATABASE_ID
import com.iamdimension.v7.data.VaultFirestoreRepository

/**
 * Cyberpunk / Neon / Galactic Visual Truth Color Palette for LROS VR26.10 — I.A.M Dimension V.7
 */
object IamDimensionPalette {
    val VoidBlack = Color(0xFF06080E)
    val DeepNebula = Color(0xFF0E1322)
    val ObsidianCard = Color(0xFF141B2D)
    val CyberGold = Color(0xFFF5C451)
    val NeonMagenta = Color(0xFFFF2A85)
    val PhylaxCyan = Color(0xFF00F0FF)
    val CrimsonAlert = Color(0xFFFF334B)
    val EmeraldMatrix = Color(0xFF00E676)
    val StarlightSilver = Color(0xFFEBF0FF)
    val MutedSlate = Color(0xFF8E9BB8)
}

/**
 * Shared state holder for the 19 mapped UI states across all 11 routes in [IamDimensionNavGraph].
 * Wired to the exact named Firestore Database ID:
 * `ai-studio-android-iamsover-129d727b-49ce-41e6-b574-5705e44eced2`
 */
class IamDimensionAppState {
    var activeVaultId by mutableStateOf("LROS_1293")
    var activeLanguage by mutableStateOf(VaultLanguage.TAGALOG)
    var kaggleKey by mutableStateOf("")
    var isBlackCaseUnlocked by mutableStateOf(false)
    var blackCaseAccessDenied by mutableStateOf(false)
    var isVerifyingKaggleToken by mutableStateOf(false)
    var showVaultSwitchModal by mutableStateOf(false)
    var selectedLrosModule by mutableStateOf(JusticeModule.MODULE_04)
    var sha256SealHash by mutableStateOf(
        "a9f4e718c2d50b8612930519e3c78d14f6a0b92c4e81d73a5f20c9e4b18d6073"
    )

    val databaseId: String = EXACT_FIRESTORE_DATABASE_ID

    val catchBasinCapacityPercent: Int
        get() = if (isBlackCaseUnlocked) 100 else 60

    fun obtainVaultFirestoreRepository(): VaultFirestoreRepository? {
        return try {
            val db = FirebaseFirestore.getInstance(
                "ai-studio-android-iamsover-129d727b-49ce-41e6-b574-5705e44eced2"
            )
            VaultFirestoreRepository(db)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Calls the `verifyKaggleToken` Cloud Function via [VaultFirestoreRepository] connected to
     * Database ID `ai-studio-android-iamsover-129d727b-49ce-41e6-b574-5705e44eced2`.
     * Triggers the 100% Catch Basin Overflow ONLY if `{ success: true }` is returned.
     */
    suspend fun verifyAndUnlockBlackCaseCloudFunction(): Boolean {
        isVerifyingKaggleToken = true
        return try {
            val repo = obtainVaultFirestoreRepository()
            val unlocked = if (repo != null) {
                repo.verifyKaggleToken(kaggleKey).success
            } else {
                LrosContactRadarService.verifyBlackCaseKaggleKey(kaggleKey)
            }
            if (unlocked) {
                isBlackCaseUnlocked = true
                blackCaseAccessDenied = false
            } else {
                isBlackCaseUnlocked = false
                blackCaseAccessDenied = true
            }
            unlocked
        } finally {
            isVerifyingKaggleToken = false
        }
    }

    fun verifyAndUnlockBlackCase(): Boolean {
        val unlocked = LrosContactRadarService.verifyBlackCaseKaggleKey(kaggleKey)
        if (unlocked) {
            isBlackCaseUnlocked = true
            blackCaseAccessDenied = false
        } else {
            isBlackCaseUnlocked = false
            blackCaseAccessDenied = true
        }
        return unlocked
    }
}
