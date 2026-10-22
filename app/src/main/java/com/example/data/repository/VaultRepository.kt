package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BioscienceAudit
import com.example.data.model.ChatMessage
import com.example.data.model.VaultDocket
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class VaultRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Sovereign Vault 1293.")
    }

    fun generateSealHash(): String {
        val hex = UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
        return "#VAULT1293-SECURE-$hex"
    }

    // --- Vault Dockets (4-Phase Loop Engine) ---

    fun observeDockets(userId: String = requireUserId()): Flow<List<VaultDocket>> = callbackFlow {
        val path = "vault_dockets"
        val registration = db.collection(path)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(VaultDocket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }?.sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L } ?: emptyList()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    suspend fun getUserDockets(): Result<List<VaultDocket>> = runCatching {
        val uid = requireUserId()
        val path = "vault_dockets"
        try {
            val snapshot = db.collection(path)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(VaultDocket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }
    }

    suspend fun getDocketById(docketId: String): Result<VaultDocket> = runCatching {
        val path = "vault_dockets/$docketId"
        try {
            val doc = db.collection("vault_dockets").document(docketId)
                .get(com.google.firebase.firestore.Source.SERVER)
                .await()
            doc.toObject(VaultDocket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: throw NoSuchElementException("Docket not found: $docketId")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            throw e
        }
    }

    suspend fun createDocket(
        title: String,
        caseSummary: String,
        moduleTag: String,
        phase: String = "GENESIS",
        planeStatus: String = "MORTAL",
        rulingText: String = "Initial blotter recorded. Ready for MGD deliberation."
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docketId = "docket_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("vault_dockets").document(docketId)
        val payload = mapOf(
            "userId" to uid,
            "title" to title.take(200),
            "caseSummary" to caseSummary.take(5000),
            "moduleTag" to moduleTag,
            "phase" to phase,
            "planeStatus" to planeStatus,
            "rulingText" to rulingText.take(15000),
            "sealHash" to generateSealHash(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docketId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun advanceDocketPhase(
        docket: VaultDocket,
        newPhase: String,
        newPlaneStatus: String,
        updatedRuling: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(docket.id.isNotBlank()) { "Docket ID must not be blank" }
        requireNotNull(docket.createdAt) { "Docket createdAt must not be null for update" }
        val docRef = db.collection("vault_dockets").document(docket.id)
        val updated = docket.copy(
            userId = uid,
            phase = newPhase,
            planeStatus = newPlaneStatus,
            rulingText = updatedRuling.take(15000),
            sealHash = generateSealHash(),
            updatedAt = null // Triggers @ServerTimestamp on set() while preserving createdAt
        )
        try {
            docRef.set(updated).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteDocket(docketId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("vault_dockets").document(docketId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Chat Messages (Sovereign Verdict Thread) ---

    fun observeChatMessages(userId: String = requireUserId()): Flow<List<ChatMessage>> = callbackFlow {
        val path = "chat_messages"
        val registration = db.collection(path)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }?.sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE } ?: emptyList()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    suspend fun addChatMessage(
        sender: String,
        moduleTag: String,
        phase: String,
        planeStatus: String,
        content: String,
        modelUsed: String,
        groundingSources: String = ""
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val messageId = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("chat_messages").document(messageId)
        val payload = mapOf(
            "userId" to uid,
            "sender" to sender,
            "moduleTag" to moduleTag.take(60),
            "phase" to phase,
            "planeStatus" to planeStatus,
            "content" to content.take(20000),
            "modelUsed" to modelUsed.take(100),
            "sealHash" to generateSealHash(),
            "groundingSources" to groundingSources.take(5000),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            messageId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteChatMessage(messageId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("chat_messages").document(messageId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Bioscience & Forensics Lab Audits ---

    fun observeBioscienceAudits(userId: String = requireUserId()): Flow<List<BioscienceAudit>> = callbackFlow {
        val path = "bioscience_audits"
        val registration = db.collection(path)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(BioscienceAudit::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }?.sortedByDescending { it.createdAt?.seconds ?: 0L } ?: emptyList()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    suspend fun createBioscienceAudit(
        sampleName: String,
        bioscienceCategory: String,
        findings: String,
        regulatoryCitation: String,
        riskLevel: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val auditId = "bio_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("bioscience_audits").document(auditId)
        val safeRisk = if (riskLevel in listOf("LOW", "MODERATE", "HIGH", "CRITICAL")) riskLevel else "MODERATE"
        val payload = mapOf(
            "userId" to uid,
            "sampleName" to sampleName.take(200),
            "bioscienceCategory" to bioscienceCategory.take(100),
            "findings" to findings.take(15000),
            "regulatoryCitation" to regulatoryCitation.take(500),
            "riskLevel" to safeRisk,
            "sealHash" to generateSealHash(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            auditId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteBioscienceAudit(auditId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("bioscience_audits").document(auditId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
}
