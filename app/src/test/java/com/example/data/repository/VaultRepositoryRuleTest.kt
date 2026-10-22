package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.fail
import org.junit.Test

class VaultRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createDocket_validPayload_createsDocumentAndReturnsId() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = VaultRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createDocket(
                title = "Barangay Captain Refused Blotter",
                caseSummary = "Bacoor barangay hall refused to blotter noise and boundary dispute.",
                moduleTag = "MODULE_01"
            )
        }
        assertThat(createResult.isSuccess).isTrue()
        val docketId = createResult.getOrThrow()
        assertThat(docketId).isNotEmpty()
    }

    @Test
    fun getUserDockets_authenticatedOwner_returnsMatchingDockets() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = VaultRepository(firestore)
        val docketId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createDocket(
                title = "OFW Contract Substitution",
                caseSummary = "Salary reduced upon arrival in Dubai.",
                moduleTag = "MODULE_02"
            ).getOrThrow()
        }

        val docketsResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getUserDockets() }
        assertThat(docketsResult.isSuccess).isTrue()
        assertThat(docketsResult.getOrThrow().map { it.id }).contains(docketId)
    }

    @Test
    fun observeDockets_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = VaultRepository(firestore)
        val docketId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createDocket(
                title = "OLA Harassment Complaint",
                caseSummary = "Online lending app blasted contacts before due date.",
                moduleTag = "MODULE_07"
            ).getOrThrow()
        }

        val emittedDockets = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeDockets(uid).first { list -> list.any { it.id == docketId } }
        }
        assertThat(emittedDockets.map { it.id }).contains(docketId)
    }

    @Test
    fun getDocketById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = VaultRepository(firestore)
        val docketId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createDocket(
                title = "Private Land Title Verification",
                caseSummary = "Checking TCT authenticity at Registry of Deeds.",
                moduleTag = "MODULE_06"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = VaultRepository(firestore)
        val result = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getDocketById(docketId) }
        assertThat(result.isFailure).isTrue()
        val exception = result.exceptionOrNull() as? FirebaseFirestoreException
        assertThat(exception?.code).isEqualTo(FirebaseFirestoreException.Code.PERMISSION_DENIED)
    }

    @Test
    fun observeDockets_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = VaultRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeDockets("unauthenticated_uid").first()
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED")
        } catch (e: Throwable) {
            val fsException = generateSequence<Throwable>(e) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            assertThat(fsException?.code).isEqualTo(FirebaseFirestoreException.Code.PERMISSION_DENIED)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@vault1293.com"
        const val BOB_EMAIL = "bob@vault1293.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
