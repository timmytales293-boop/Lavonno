package com.example.security

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.guard.GuardPreferences
import kotlinx.coroutines.CancellationException
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that uploads captured unrecognized-person security snapshots to the
 * Google Drive "Noor Security" folder, strictly refusing to upload any Owner-verified frame.
 */
class DriveSecurityUploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val photoPath = inputData.getString(KEY_PHOTO_PATH).orEmpty()
        val isOwnerVerified = inputData.getBoolean(KEY_IS_OWNER_VERIFIED, false)
        val reason = inputData.getString(KEY_REASON) ?: "Unrecognized user security alert"

        // Hard privacy invariant: Owner photos NEVER upload and are deleted immediately if present.
        if (isOwnerVerified) {
            if (photoPath.isNotBlank()) runCatching { File(photoPath).delete() }
            GuardPreferences(applicationContext).recordSecurityEvent(
                "Owner verified — photo skipped (never uploaded)"
            )
            return Result.failure(workDataOf(KEY_ERROR to "Owner photo cannot be uploaded"))
        }

        val photoFile = File(photoPath)
        if (!photoFile.exists() || photoFile.length() == 0L) {
            return Result.failure(workDataOf(KEY_ERROR to "Security snapshot file not found"))
        }

        return try {
            val guardPrefs = GuardPreferences(applicationContext)
            val state = guardPrefs.getCurrentState()
            if (!state.driveSecurityEnabled) {
                runCatching { photoFile.delete() }
                return Result.success()
            }

            val tokenProvider = accessTokenProviderOverride
                ?: { GoogleDriveSecurityUploader.tryGetSilentAccessToken(applicationContext) }
            val accessToken = tokenProvider()
            if (accessToken.isNullOrBlank()) {
                guardPrefs.recordSecurityEvent("Drive auth needed to upload security alert")
                return if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
            }

            val uploader = uploaderOverride ?: GoogleDriveSecurityUploader()
            val folderId = uploader.findOrCreateNoorSecurityFolder(
                accessToken = accessToken,
                cachedFolderId = state.driveFolderId.ifBlank { null }
            )
            guardPrefs.saveDriveFolderId(folderId)

            val uploaded = uploader.uploadOtherPersonPhoto(
                accessToken = accessToken,
                folderId = folderId,
                photoFile = photoFile,
                isOwnerVerified = false,
                reason = reason
            )

            // Remove local temp file after successful Drive backup
            runCatching { photoFile.delete() }
            val nowMs = System.currentTimeMillis()
            guardPrefs.recordSecurityEvent(
                message = "Other Person Photo Uploaded -> Google Drive / Noor Security ✓",
                uploadEpochMs = nowMs
            )

            Result.success(
                workDataOf(
                    KEY_DRIVE_FILE_ID to uploaded.fileId,
                    KEY_DRIVE_FOLDER_ID to uploaded.folderId,
                    KEY_UPLOADED_NAME to uploaded.fileName
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Log.w(TAG, "Drive security photo upload failed (attempt ${runAttemptCount + 1})", t)
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure(workDataOf(KEY_ERROR to (t.message ?: "Drive upload failed")))
            }
        }
    }

    companion object {
        private const val TAG = "DriveSecurityWorker"
        private const val MAX_RETRIES = 3

        const val KEY_PHOTO_PATH = "photo_path"
        const val KEY_IS_OWNER_VERIFIED = "is_owner_verified"
        const val KEY_REASON = "reason"
        const val KEY_DRIVE_FILE_ID = "drive_file_id"
        const val KEY_DRIVE_FOLDER_ID = "drive_folder_id"
        const val KEY_UPLOADED_NAME = "uploaded_name"
        const val KEY_ERROR = "error"

        @Volatile
        internal var uploaderOverride: GoogleDriveSecurityUploader? = null

        @Volatile
        internal var accessTokenProviderOverride: (suspend () -> String?)? = null

        fun enqueueAlertUpload(
            context: Context,
            photoFile: File,
            reason: String
        ) {
            val request = OneTimeWorkRequestBuilder<DriveSecurityUploadWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setInputData(
                    workDataOf(
                        KEY_PHOTO_PATH to photoFile.absolutePath,
                        KEY_IS_OWNER_VERIFIED to false,
                        KEY_REASON to reason
                    )
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30L,
                    TimeUnit.SECONDS
                )
                .addTag("noor_drive_security")
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                "noor_drive_upload_${photoFile.name}",
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
