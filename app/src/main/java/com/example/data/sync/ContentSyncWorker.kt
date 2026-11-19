package com.example.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.repository.OfflineContentRepository
import com.example.di.NoorDatabaseModule
import com.example.guard.GuardPreferences
import kotlinx.coroutines.CancellationException

/**
 * Background WorkManager [CoroutineWorker] that periodically fetches updated Quran, Hadith,
 * and Dua content from remote APIs and updates the local Room cache ([com.example.data.local.NoorCacheDatabase]).
 */
class ContentSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = repositoryOverride
            ?: NoorDatabaseModule.provideOfflineContentRepository(applicationContext)

        return try {
            val summary = repository.syncContentFromRemote()
            val syncedAtMs = System.currentTimeMillis()
            val summaryLabel =
                "Surahs:${summary.surahsUpdated}, Ayahs:${summary.ayahsUpdated}, Hadiths:${summary.hadithsUpdated}, Duas:${summary.duasUpdated}"

            runCatching {
                GuardPreferences(applicationContext).recordContentSyncSuccess(
                    epochMs = syncedAtMs,
                    summary = summaryLabel
                )
            }

            Result.success(
                workDataOf(
                    KEY_SURAHS_SYNCED to summary.surahsUpdated,
                    KEY_AYAHS_SYNCED to summary.ayahsUpdated,
                    KEY_HADITHS_SYNCED to summary.hadithsUpdated,
                    KEY_DUAS_SYNCED to summary.duasUpdated,
                    KEY_PARTIAL_FAILURES to summary.partialFailures,
                    KEY_SYNCED_AT_MS to syncedAtMs
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Log.w(TAG, "Background content sync attempt ${runAttemptCount + 1} failed", t)
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure(
                    workDataOf(
                        KEY_ERROR_MESSAGE to (t.message ?: "Content sync failed")
                    )
                )
            }
        }
    }

    companion object {
        private const val TAG = "ContentSyncWorker"
        const val MAX_RETRY_ATTEMPTS = 3

        const val KEY_SURAHS_SYNCED = "surahs_synced"
        const val KEY_AYAHS_SYNCED = "ayahs_synced"
        const val KEY_HADITHS_SYNCED = "hadiths_synced"
        const val KEY_DUAS_SYNCED = "duas_synced"
        const val KEY_PARTIAL_FAILURES = "partial_failures"
        const val KEY_SYNCED_AT_MS = "synced_at_ms"
        const val KEY_ERROR_MESSAGE = "error_message"

        @Volatile
        internal var repositoryOverride: OfflineContentRepository? = null
    }
}
