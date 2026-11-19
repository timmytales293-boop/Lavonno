package com.example.security

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import com.example.data.sync.ContentSyncScheduler
import com.example.guard.GuardPreferences
import com.example.guard.LocalGuardState
import com.example.guard.NoorBootReceiver
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class NoorSecuritySuiteTest {

    private lateinit var context: Context
    private lateinit var tempFiles: MutableList<File>

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        tempFiles = mutableListOf()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    @After
    fun teardown() {
        DriveSecurityUploadWorker.uploaderOverride = null
        DriveSecurityUploadWorker.accessTokenProviderOverride = null
        tempFiles.forEach { runCatching { it.delete() } }
    }

    @Test
    fun ownerFaceVerifier_matchesOwnerAcrossScaleAndRejectsOtherPerson() {
        val owner = OwnerFaceVerifier.buildSignatureFromPoints(
            boxWidth = 200.0,
            boxHeight = 250.0,
            leftEyeX = 70.0,
            leftEyeY = 90.0,
            rightEyeX = 130.0,
            rightEyeY = 90.0,
            noseX = 100.0,
            noseY = 135.0,
            mouthLeftX = 80.0,
            mouthLeftY = 175.0,
            mouthRightX = 120.0,
            mouthRightY = 175.0
        )!!

        // Same owner zoomed 1.5x and shifted across the frame
        val ownerScaledShifted = OwnerFaceVerifier.buildSignatureFromPoints(
            boxWidth = 300.0,
            boxHeight = 375.0,
            leftEyeX = 205.0,
            leftEyeY = 235.0,
            rightEyeX = 295.0,
            rightEyeY = 235.0,
            noseX = 250.0,
            noseY = 302.5,
            mouthLeftX = 220.0,
            mouthLeftY = 362.5,
            mouthRightX = 280.0,
            mouthRightY = 362.5
        )!!

        // Unrecognized person with markedly different eye/nose/mouth geometry
        val otherPerson = OwnerFaceVerifier.buildSignatureFromPoints(
            boxWidth = 260.0,
            boxHeight = 220.0,
            leftEyeX = 60.0,
            leftEyeY = 70.0,
            rightEyeX = 180.0,
            rightEyeY = 72.0,
            noseX = 120.0,
            noseY = 120.0,
            mouthLeftX = 70.0,
            mouthLeftY = 190.0,
            mouthRightX = 170.0,
            mouthRightY = 190.0
        )!!

        assertEquals(
            FaceVerificationResult.NO_OWNER_ENROLLED,
            OwnerFaceVerifier.verifyCandidateAgainstStoredJson("", ownerScaledShifted)
        )
        assertEquals(
            FaceVerificationResult.OWNER_MATCH_NEVER_UPLOAD,
            OwnerFaceVerifier.verifyCandidateAgainstStoredJson(owner.toJson(), ownerScaledShifted)
        )
        assertEquals(
            FaceVerificationResult.OTHER_PERSON_DETECTED,
            OwnerFaceVerifier.verifyCandidateAgainstStoredJson(owner.toJson(), otherPerson)
        )
    }

    @Test
    fun guardPreferences_includesChromeDefaultLimitAndOwnerFaceSignaturePersistence() {
        assertEquals(45, LocalGuardState.DEFAULT_APP_LIMITS["com.android.chrome"])

        val prefs = GuardPreferences(context)
        prefs.saveOwnerFaceSignature("[0.3,1.25,0.75,0.66]")
        val enrolled = prefs.getCurrentState()
        assertTrue(enrolled.ownerFaceEnrolled)
        assertEquals("[0.3,1.25,0.75,0.66]", enrolled.ownerFaceSignatureJson)

        prefs.clearOwnerFaceSignature()
        val cleared = prefs.getCurrentState()
        assertFalse(cleared.ownerFaceEnrolled)
        assertEquals("", cleared.ownerFaceSignatureJson)
    }

    @Test
    fun driveUploader_createsNoorSecurityFolderAndBlocksOwnerPhotoUpload() = runBlocking {
        val createdFolderRequested = AtomicInteger(0)
        val uploadRequested = AtomicInteger(0)

        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request()
                val url = req.url.toString()
                val bodyJson = when {
                    req.method == "GET" && url.contains("/files?") -> {
                        // Folder lookup returns empty so creation is exercised
                        """{"files":[]}"""
                    }
                    req.method == "POST" && url.contains("/files?fields=id,name") -> {
                        createdFolderRequested.incrementAndGet()
                        """{"id":"folder_noor_sec_1","name":"Noor Security"}"""
                    }
                    req.method == "POST" && url.contains("uploadType=multipart") -> {
                        uploadRequested.incrementAndGet()
                        """{"id":"drive_file_99","name":"intruder.jpg","parents":["folder_noor_sec_1"]}"""
                    }
                    else -> """{}"""
                }
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(bodyJson.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val uploader = GoogleDriveSecurityUploader(
            httpClient = mockClient,
            driveApiBaseUrl = "https://mock.drive.local/drive/v3",
            driveUploadBaseUrl = "https://mock.drive.local/upload/drive/v3"
        )

        val folderId = uploader.findOrCreateNoorSecurityFolder(accessToken = "test_token")
        assertEquals("folder_noor_sec_1", folderId)
        assertEquals(1, createdFolderRequested.get())

        val fakePhoto = File.createTempFile("noor_test_", ".jpg", context.cacheDir).apply {
            writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte()))
            tempFiles.add(this)
        }

        // Hard privacy rule: Owner-verified frame MUST throw and never hit the network
        var ownerRejected = false
        try {
            uploader.uploadOtherPersonPhoto(
                accessToken = "test_token",
                folderId = folderId,
                photoFile = fakePhoto,
                isOwnerVerified = true,
                reason = "Owner check"
            )
        } catch (_: IllegalArgumentException) {
            ownerRejected = true
        }
        assertTrue(ownerRejected)
        assertEquals(0, uploadRequested.get())

        // Other-person frame uploads cleanly to "Noor Security" folder
        val uploaded = uploader.uploadOtherPersonPhoto(
            accessToken = "test_token",
            folderId = folderId,
            photoFile = fakePhoto,
            isOwnerVerified = false,
            reason = "Other person lock check"
        )
        assertEquals("drive_file_99", uploaded.fileId)
        assertEquals("folder_noor_sec_1", uploaded.folderId)
        assertEquals(1, uploadRequested.get())
    }

    @Test
    fun driveSecurityWorker_skipsOwnerPhotosAndUploadsOtherPersonPhotos() = runBlocking {
        val networkCalls = AtomicInteger(0)
        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                networkCalls.incrementAndGet()
                val req = chain.request()
                val bodyJson = when {
                    req.method == "GET" && req.url.toString().contains("/files?") ->
                        """{"files":[{"id":"folder_noor_sec_existing","name":"Noor Security"}]}"""
                    req.method == "POST" && req.url.toString().contains("uploadType=multipart") ->
                        """{"id":"drive_alert_777","name":"alert.jpg","parents":["folder_noor_sec_existing"]}"""
                    else -> """{}"""
                }
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(bodyJson.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        DriveSecurityUploadWorker.uploaderOverride = GoogleDriveSecurityUploader(
            httpClient = mockClient,
            driveApiBaseUrl = "https://mock.drive.local/drive/v3",
            driveUploadBaseUrl = "https://mock.drive.local/upload/drive/v3"
        )
        DriveSecurityUploadWorker.accessTokenProviderOverride = { "fake_access_token" }

        val guardPrefs = GuardPreferences(context)
        guardPrefs.setDriveSecurityEnabled(true)

        // 1. Owner verified -> MUST refuse upload, delete temp file, and return failure
        val ownerPhoto = File.createTempFile("owner_should_delete_", ".jpg", context.cacheDir).apply {
            writeBytes(byteArrayOf(1, 2, 3, 4))
            tempFiles.add(this)
        }
        val ownerWorker = TestListenableWorkerBuilder<DriveSecurityUploadWorker>(context)
            .setInputData(
                workDataOf(
                    DriveSecurityUploadWorker.KEY_PHOTO_PATH to ownerPhoto.absolutePath,
                    DriveSecurityUploadWorker.KEY_IS_OWNER_VERIFIED to true,
                    DriveSecurityUploadWorker.KEY_REASON to "Owner test"
                )
            )
            .build()
        val ownerResult = ownerWorker.doWork()
        assertTrue(ownerResult is ListenableWorker.Result.Failure)
        assertFalse(ownerPhoto.exists())
        assertEquals(0, networkCalls.get())

        // 2. Other person -> uploads to Noor Security folder, deletes local temp file, updates prefs
        val intruderPhoto = File.createTempFile("intruder_upload_", ".jpg", context.cacheDir).apply {
            writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 10, 20))
            tempFiles.add(this)
        }
        val intruderWorker = TestListenableWorkerBuilder<DriveSecurityUploadWorker>(context)
            .setInputData(
                workDataOf(
                    DriveSecurityUploadWorker.KEY_PHOTO_PATH to intruderPhoto.absolutePath,
                    DriveSecurityUploadWorker.KEY_IS_OWNER_VERIFIED to false,
                    DriveSecurityUploadWorker.KEY_REASON to "Unrecognized user on lock screen"
                )
            )
            .build()
        val intruderResult = intruderWorker.doWork()
        assertTrue(intruderResult is ListenableWorker.Result.Success)
        val output = (intruderResult as ListenableWorker.Result.Success).outputData
        assertEquals("drive_alert_777", output.getString(DriveSecurityUploadWorker.KEY_DRIVE_FILE_ID))
        assertEquals("folder_noor_sec_existing", output.getString(DriveSecurityUploadWorker.KEY_DRIVE_FOLDER_ID))
        assertFalse(intruderPhoto.exists())
        assertTrue(guardPrefs.getCurrentState().lastSecurityUploadEpochMs > 0L)
        assertTrue(guardPrefs.getCurrentState().lastSecurityEvent.contains("Noor Security"))
    }

    @Test
    fun bootReceiver_reschedulesAlarmsAndPeriodicSyncOnBootCompleted() {
        val receiver = NoorBootReceiver()
        receiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = Shadows.shadowOf(alarmManager)
        assertTrue(shadowAlarmManager.scheduledAlarms.isNotEmpty())

        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(ContentSyncScheduler.PERIODIC_SYNC_WORK_NAME)
            .get()
        assertTrue(workInfos.isNotEmpty())
    }
}
