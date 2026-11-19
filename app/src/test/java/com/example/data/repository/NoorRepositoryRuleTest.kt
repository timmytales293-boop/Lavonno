package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.LockMode
import com.example.data.model.Wakto
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoorRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureProfileAndNamazToggle_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = NoorRepository(firestore)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfile("Boss Alice")
            val profile = repo.observeUserProfile(uid).first { it != null }
            assertNotNull(profile)
            assertEquals("Mymensingh", profile?.cityName)

            repo.saveQuranBookmark(18, 10, "Al-Kahf")
            val updated = repo.observeUserProfile(uid).first { it?.lastReadSurah == 18L }
            assertEquals(10L, updated?.lastReadAyah)

            repo.setDirectLockState(LockMode.PUSHUP_30, 30)
            val locked = repo.observeUserProfile(uid).first { it?.activeLockMode == LockMode.PUSHUP_30.code }
            assertEquals(30L, locked?.requiredPushUps)

            repo.updateProfileFields(
                mapOf(
                    "familyRole" to "MA",
                    "isPeriodMode" to true,
                    "periodDayNumber" to 2L,
                    "defensePlanDayBits" to 7L,
                    "ffDailyLimitMinutes" to 45L,
                    "sosPhoneNumber" to "01711223344",
                    "femaleVoiceEnabled" to true,
                    "namazAutoSilentEnabled" to true,
                    "gamblingBlockEnabled" to true
                )
            )
            val extended = repo.observeUserProfile(uid).first { it?.familyRole == "MA" }
            assertEquals(true, extended?.isPeriodMode)
            assertEquals(7L, extended?.defensePlanDayBits)
            assertEquals(45L, extended?.ffDailyLimitMinutes)

            val beforeLog = repo.observeNamazLogs(uid).first().firstOrNull { it.dateKey == "2026-11-15" }
            val expectedAsr = !(beforeLog?.asr ?: false)
            repo.toggleNamaz("2026-11-15", Wakto.ASR, beforeLog)
            val logs = repo.observeNamazLogs(uid).first { list ->
                list.firstOrNull { it.dateKey == "2026-11-15" }?.asr == expectedAsr
            }
            assertEquals(expectedAsr, logs.first { it.dateKey == "2026-11-15" }.asr)
        }
    }

    @Test
    fun observeNamazLogs_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = NoorRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.ensureUserProfile("Alice")
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = NoorRepository(firestore)
        var thrown: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeNamazLogs(aliceUid).first()
            }
        } catch (e: Exception) {
            thrown = e
        }
        val firestoreException = checkNotNull(thrown?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException, got $thrown"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    @Test
    fun observeNamazLogs_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = NoorRepository(firestore)
        var thrown: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeNamazLogs("some_uid").first()
            }
        } catch (e: Exception) {
            thrown = e
        }
        val firestoreException = checkNotNull(thrown?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException, got $thrown"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_noor@test.com"
        const val BOB_EMAIL = "bob_noor@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
