package com.example.security

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class DriveUploadResult(
    val fileId: String,
    val folderId: String,
    val fileName: String
)

/**
 * Google Drive REST v3 Security Uploader for the "Noor Security" folder.
 * Uses least-privilege OAuth scope `https://www.googleapis.com/auth/drive.file` via
 * Android Identity `AuthorizationClient`. Strictly blocks any upload when `isOwnerVerified == true`.
 */
class GoogleDriveSecurityUploader(
    private val httpClient: OkHttpClient = defaultHttpClient,
    private val driveApiBaseUrl: String = DEFAULT_DRIVE_API_BASE,
    private val driveUploadBaseUrl: String = DEFAULT_DRIVE_UPLOAD_BASE
) {

    suspend fun findOrCreateNoorSecurityFolder(
        accessToken: String,
        cachedFolderId: String? = null
    ): String = withContext(Dispatchers.IO) {
        if (!cachedFolderId.isNullOrBlank()) {
            val verifyReq = Request.Builder()
                .url("$driveApiBaseUrl/files/$cachedFolderId?fields=id,name,trashed")
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()
            val exists = runCatching {
                httpClient.newCall(verifyReq).execute().use { resp ->
                    if (!resp.isSuccessful) false
                    else {
                        val json = JSONObject(resp.body?.string().orEmpty())
                        !json.optBoolean("trashed", false) && json.optString("id").isNotBlank()
                    }
                }
            }.getOrDefault(false)
            if (exists) return@withContext cachedFolderId
        }

        val query = "name = '$NOOR_SECURITY_FOLDER_NAME' and mimeType = '$DRIVE_FOLDER_MIME' and trashed = false"
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val searchReq = Request.Builder()
            .url("$driveApiBaseUrl/files?q=$encodedQuery&fields=files(id,name)&spaces=drive")
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        httpClient.newCall(searchReq).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IOException("Drive folder lookup failed: HTTP ${resp.code}")
            }
            val bodyStr = resp.body?.string().orEmpty()
            val files = JSONObject(bodyStr).optJSONArray("files")
            if (files != null && files.length() > 0) {
                val existingId = files.getJSONObject(0).optString("id")
                if (existingId.isNotBlank()) return@withContext existingId
            }
        }

        // Create "Noor Security" folder
        val metadata = JSONObject().apply {
            put("name", NOOR_SECURITY_FOLDER_NAME)
            put("mimeType", DRIVE_FOLDER_MIME)
        }.toString()

        val createReq = Request.Builder()
            .url("$driveApiBaseUrl/files?fields=id,name")
            .addHeader("Authorization", "Bearer $accessToken")
            .post(metadata.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        httpClient.newCall(createReq).execute().use { resp ->
            val bodyStr = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IOException("Drive folder creation failed: HTTP ${resp.code} $bodyStr")
            }
            val folderId = JSONObject(bodyStr).optString("id")
            if (folderId.isBlank()) {
                throw IOException("Drive folder created without ID")
            }
            folderId
        }
    }

    suspend fun uploadOtherPersonPhoto(
        accessToken: String,
        folderId: String,
        photoFile: File,
        isOwnerVerified: Boolean,
        reason: String
    ): DriveUploadResult = withContext(Dispatchers.IO) {
        // Hard privacy enforcement: Owner photo must NEVER be uploaded to Google Drive
        require(!isOwnerVerified) {
            "Privacy Policy Violation: Owner photo must NEVER be uploaded to Google Drive."
        }
        require(photoFile.exists() && photoFile.length() > 0L) {
            "Photo file missing or empty: ${photoFile.absolutePath}"
        }

        val boundary = "noor_drive_boundary_${System.currentTimeMillis()}"
        val metadataJson = JSONObject().apply {
            put("name", photoFile.name)
            put("parents", JSONArray().put(folderId))
            put("description", "Noor Security Alert: $reason")
        }.toString()

        val jpegBytes = photoFile.readBytes()
        val headerPart = buildString {
            append("--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadataJson)
            append("\r\n--$boundary\r\n")
            append("Content-Type: image/jpeg\r\n\r\n")
        }.toByteArray(Charsets.UTF_8)
        val closingPart = "\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8)

        val fullBody = ByteArray(headerPart.size + jpegBytes.size + closingPart.size)
        System.arraycopy(headerPart, 0, fullBody, 0, headerPart.size)
        System.arraycopy(jpegBytes, 0, fullBody, headerPart.size, jpegBytes.size)
        System.arraycopy(closingPart, 0, fullBody, headerPart.size + jpegBytes.size, closingPart.size)

        val uploadReq = Request.Builder()
            .url("$driveUploadBaseUrl/files?uploadType=multipart&fields=id,name,parents")
            .addHeader("Authorization", "Bearer $accessToken")
            .post(fullBody.toRequestBody("multipart/related; boundary=$boundary".toMediaType()))
            .build()

        httpClient.newCall(uploadReq).execute().use { resp ->
            val bodyStr = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IOException("Drive photo upload failed: HTTP ${resp.code} $bodyStr")
            }
            val obj = JSONObject(bodyStr)
            val fileId = obj.optString("id")
            if (fileId.isBlank()) {
                throw IOException("Drive upload succeeded without file ID")
            }
            DriveUploadResult(
                fileId = fileId,
                folderId = folderId,
                fileName = obj.optString("name", photoFile.name)
            )
        }
    }

    companion object {
        private const val TAG = "GoogleDriveSecurity"
        const val NOOR_SECURITY_FOLDER_NAME = "Noor Security"
        const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
        private const val DRIVE_FOLDER_MIME = "application/vnd.google-apps.folder"
        private const val DEFAULT_DRIVE_API_BASE = "https://www.googleapis.com/drive/v3"
        private const val DEFAULT_DRIVE_UPLOAD_BASE = "https://www.googleapis.com/upload/drive/v3"

        private val defaultHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        }

        fun buildDriveAuthorizationRequest(): AuthorizationRequest =
            AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(DRIVE_FILE_SCOPE)))
                .build()

        suspend fun requestDriveAuthorization(context: Context): AuthorizationResult? {
            return runCatching {
                Identity.getAuthorizationClient(context)
                    .authorize(buildDriveAuthorizationRequest())
                    .await()
            }.onFailure { err ->
                Log.w(TAG, "Drive authorization request failed", err)
            }.getOrNull()
        }

        suspend fun tryGetSilentAccessToken(context: Context): String? {
            val result = requestDriveAuthorization(context) ?: return null
            return if (!result.hasResolution()) result.accessToken else null
        }
    }
}
