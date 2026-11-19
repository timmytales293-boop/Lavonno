package com.example.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import java.util.concurrent.TimeUnit

/**
 * Schedules periodic and on-demand background synchronization of Quran, Hadith, and Dua
 * content using Jetpack WorkManager.
 */
object ContentSyncScheduler {

    const val PERIODIC_SYNC_WORK_NAME = "noor_content_periodic_sync"
    const val IMMEDIATE_SYNC_WORK_NAME = "noor_content_immediate_sync"
    const val SYNC_WORK_TAG = "noor_content_sync"
    const val DEFAULT_REPEAT_INTERVAL_HOURS = 24L

    fun buildSyncConstraints(): Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()

    fun buildPeriodicSyncRequest(
        repeatIntervalHours: Long = DEFAULT_REPEAT_INTERVAL_HOURS
    ): PeriodicWorkRequest {
        val clampedHours = repeatIntervalHours.coerceAtLeast(1L)
        return PeriodicWorkRequestBuilder<ContentSyncWorker>(
            repeatInterval = clampedHours,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(buildSyncConstraints())
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(SYNC_WORK_TAG)
            .build()
    }

    fun buildImmediateSyncRequest(): OneTimeWorkRequest {
        return OneTimeWorkRequestBuilder<ContentSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(SYNC_WORK_TAG)
            .build()
    }

    fun schedulePeriodicSync(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(
                PERIODIC_SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                buildPeriodicSyncRequest()
            )
    }

    fun triggerImmediateSync(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(
                IMMEDIATE_SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                buildImmediateSyncRequest()
            )
    }
}
