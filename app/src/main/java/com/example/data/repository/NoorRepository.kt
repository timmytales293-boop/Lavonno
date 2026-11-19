package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AiMessage
import com.example.data.model.AppLimit
import com.example.data.model.LockMode
import com.example.data.model.NamazLog
import com.example.data.model.Reminder
import com.example.data.model.UserProfile
import com.example.data.model.Wakto
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class NoorRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val ex = firestoreError ?: (error as? Exception)
                if (ex != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(ex, OperationType.GET, "users/$userId")
                }
                throw error
            }

    suspend fun ensureUserProfile(displayName: String? = null) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val safeName = displayName?.takeIf { it.isNotBlank() }?.take(100) ?: "Boss"
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "cityName" to "Mymensingh",
                    "latitude" to 24.7471,
                    "longitude" to 90.4203,
                    "lastReadSurah" to 1L,
                    "lastReadAyah" to 1L,
                    "lastReadSurahName" to "Al-Fatiha",
                    "azanEnabled" to true,
                    "prePrayerAlertEnabled" to true,
                    "nightLockEnabled" to true,
                    "pornBlockEnabled" to true,
                    "uninstallLockEnabled" to true,
                    "dailyPhoneLimitMinutes" to 120L,
                    "focusModeUntilEpochMs" to 0L,
                    "activeLockMode" to LockMode.NONE.code,
                    "requiredPushUps" to 0L,
                    "activeLockUntilEpochMs" to 0L,
                    "femaleVoiceEnabled" to true,
                    "namazAutoSilentEnabled" to true,
                    "ffDailyLimitMinutes" to 60L,
                    "ffGuardEnabled" to true,
                    "gamblingBlockEnabled" to true,
                    "sosPhoneNumber" to "",
                    "defensePlanDayBits" to 0L,
                    "lockTriggerReason" to "",
                    "familyRole" to "BOSS",
                    "isPeriodMode" to false,
                    "periodDayNumber" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateProfileFields(fields: Map<String, Any>) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val payload = fields.toMutableMap().apply {
            this["updatedAt"] = FieldValue.serverTimestamp()
        }
        try {
            docRef.update(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun saveQuranBookmark(surahNumber: Int, ayahNumber: Int, surahName: String) {
        updateProfileFields(
            mapOf(
                "lastReadSurah" to surahNumber.toLong(),
                "lastReadAyah" to ayahNumber.toLong(),
                "lastReadSurahName" to surahName.take(80)
            )
        )
    }

    suspend fun setDirectLockState(mode: LockMode, requiredPushUps: Int, untilEpochMs: Long = 0L) {
        updateProfileFields(
            mapOf(
                "activeLockMode" to mode.code,
                "requiredPushUps" to requiredPushUps.toLong(),
                "activeLockUntilEpochMs" to untilEpochMs
            )
        )
    }

    fun observeNamazLogs(userId: String): Flow<List<NamazLog>> =
        db.collection("users").document(userId).collection("namaz_logs")
            .orderBy("dateKey", Query.Direction.DESCENDING)
            .limit(14)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    NamazLog::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val ex = firestoreError ?: (error as? Exception)
                if (ex != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(ex, OperationType.LIST, "users/$userId/namaz_logs")
                }
                throw error
            }

    suspend fun toggleNamaz(dateKey: String, wakto: Wakto, currentLog: NamazLog?) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("namaz_logs").document(dateKey)
        try {
            val resolvedLog = currentLog ?: docRef.get().await().let { snap ->
                if (snap.exists()) snap.toObject(NamazLog::class.java) else null
            }
            if (resolvedLog == null) {
                val payload = mapOf(
                    "userId" to uid,
                    "dateKey" to dateKey,
                    "fajr" to (wakto == Wakto.FAJR),
                    "dhuhr" to (wakto == Wakto.DHUHR),
                    "asr" to (wakto == Wakto.ASR),
                    "maghrib" to (wakto == Wakto.MAGHRIB),
                    "isha" to (wakto == Wakto.ISHA),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val field = when (wakto) {
                    Wakto.FAJR -> "fajr"
                    Wakto.DHUHR -> "dhuhr"
                    Wakto.ASR -> "asr"
                    Wakto.MAGHRIB -> "maghrib"
                    Wakto.ISHA -> "isha"
                }
                val newVal = when (wakto) {
                    Wakto.FAJR -> !resolvedLog.fajr
                    Wakto.DHUHR -> !resolvedLog.dhuhr
                    Wakto.ASR -> !resolvedLog.asr
                    Wakto.MAGHRIB -> !resolvedLog.maghrib
                    Wakto.ISHA -> !resolvedLog.isha
                }
                docRef.update(
                    mapOf(
                        field to newVal,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    fun observeAiMessages(userId: String): Flow<List<AiMessage>> =
        db.collection("users").document(userId).collection("ai_messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(100)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    AiMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val ex = firestoreError ?: (error as? Exception)
                if (ex != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(ex, OperationType.LIST, "users/$userId/ai_messages")
                }
                throw error
            }

    suspend fun addAiMessage(sender: String, text: String, reference: String? = null) {
        val uid = requireUserId()
        val msgId = UUID.randomUUID().toString().replace("-", "").take(24)
        val docRef = db.collection("users").document(uid).collection("ai_messages").document(msgId)
        val payload = buildMap<String, Any> {
            put("userId", uid)
            put("sender", sender)
            put("text", text.take(8000))
            if (!reference.isNullOrBlank()) {
                put("reference", reference.take(200))
            }
            put("createdAt", FieldValue.serverTimestamp())
        }
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    fun observeReminders(userId: String): Flow<List<Reminder>> =
        db.collection("users").document(userId).collection("reminders")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Reminder::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val ex = firestoreError ?: (error as? Exception)
                if (ex != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(ex, OperationType.LIST, "users/$userId/reminders")
                }
                throw error
            }

    suspend fun addReminder(title: String, hour: Int, minute: Int, repeatDaily: Boolean = false): String {
        val uid = requireUserId()
        val remId = UUID.randomUUID().toString().replace("-", "").take(24)
        val docRef = db.collection("users").document(uid).collection("reminders").document(remId)
        val payload = mapOf(
            "userId" to uid,
            "title" to title.take(200),
            "hour" to hour.toLong(),
            "minute" to minute.toLong(),
            "enabled" to true,
            "repeatDaily" to repeatDaily,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return remId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleReminder(reminder: Reminder) {
        val uid = requireUserId()
        require(reminder.id.isNotBlank())
        val docRef = db.collection("users").document(uid).collection("reminders").document(reminder.id)
        try {
            docRef.update(
                mapOf(
                    "enabled" to !reminder.enabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteReminder(reminderId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("reminders").document(reminderId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    fun observeAppLimits(userId: String): Flow<List<AppLimit>> =
        db.collection("users").document(userId).collection("app_limits")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    AppLimit::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val ex = firestoreError ?: (error as? Exception)
                if (ex != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(ex, OperationType.LIST, "users/$userId/app_limits")
                }
                throw error
            }

    suspend fun upsertAppLimit(packageName: String, appName: String, dailyLimitMinutes: Int, enabled: Boolean) {
        val uid = requireUserId()
        val docId = packageName.replace(".", "_")
        val docRef = db.collection("users").document(uid).collection("app_limits").document(docId)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "packageName" to packageName,
                        "appName" to appName.take(80),
                        "dailyLimitMinutes" to dailyLimitMinutes.toLong(),
                        "enabled" to enabled,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "appName" to appName.take(80),
                        "dailyLimitMinutes" to dailyLimitMinutes.toLong(),
                        "enabled" to enabled,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }
}
