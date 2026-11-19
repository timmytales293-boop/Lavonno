package com.example.ui.lock

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.data.model.ExerciseMode
import com.example.guard.GuardPreferences
import com.example.security.DriveSecurityUploadWorker
import com.example.security.FaceVerificationResult
import com.example.security.OwnerFaceVerifier
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.sqrt

data class PushUpTrackingState(
    val exerciseMode: ExerciseMode = ExerciseMode.PUSHUP,
    val count: Int = 0,
    val targetCount: Int = 30,
    val isCameraActive: Boolean = false,
    val isSensorActive: Boolean = false,
    val isFaceVisible: Boolean = false,
    val isDownPhase: Boolean = false,
    val sensorMovedInCycle: Boolean = false,
    val statusHint: String = "Boss, Phone samne rakhun ebong Exercise suru korun",
    val resetNotice: String? = null,
    val isExerciseCompleted: Boolean = false,
    val securityStatusLabel: String? = null,
    val isOwnerVerified: Boolean? = null
)

/**
 * Dual-verified Anti-Cheat Exercise & Security Detector supporting:
 * 1) PUSHUP (10 or 30): Camera Pose Nose/Eyes dip+rise + Accelerometer/Proximity movement
 * 2) SQUAT (30): Camera Pose Hip/Nose squat dip+rise + Accelerometer impulse
 * 3) PLANK (60s): Continuous Camera face/shoulder presence + steady horizontal hold
 * 4) SECURITY FACE CHECK: If Owner Face is enrolled, checks whether the person on camera is the
 *    Owner (never uploaded) or an unrecognized person (alert snapshot queued for Drive "Noor Security").
 */
class PushUpDetector(
    context: Context,
    initialTargetCount: Int,
    private val lockTriggerReason: String = "Security Lock Active",
    private val enableSecurityCapture: Boolean = true
) : SensorEventListener {

    private val appContext = context.applicationContext
    private val guardPrefs = GuardPreferences(appContext)
    private val sensorManager =
        appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val proximitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    private val poseDetector = PoseDetection.getClient(
        PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
    )

    private val faceDetector = FaceDetection.getClient(
        OwnerFaceVerifier.createAccurateDetectorOptions()
    )

    private var activeTarget = initialTargetCount.coerceAtLeast(1)
    private val _state = MutableStateFlow(PushUpTrackingState(targetCount = activeTarget))
    val state: StateFlow<PushUpTrackingState> = _state.asStateFlow()

    private var baselineY: Float? = null
    private var wentDownByCamera = false
    private var sensorMovedDuringRep = false
    private var lastAccelMag = 9.81f
    private var lastRepTimestampMs = 0L
    private var lastPlankTickMs = 0L
    private var isPausedResetApplied = false

    @Volatile
    private var securityCheckCompleted = false
    private var otherPersonConsecutiveHits = 0

    fun switchExerciseMode(mode: ExerciseMode, baseTargetPushUps: Int) {
        activeTarget = when (mode) {
            ExerciseMode.PLANK -> 60
            ExerciseMode.SQUAT -> if (baseTargetPushUps <= 10) 15 else 30
            ExerciseMode.PUSHUP -> baseTargetPushUps.coerceAtLeast(10)
        }
        baselineY = null
        wentDownByCamera = false
        sensorMovedDuringRep = false
        lastRepTimestampMs = 0L
        lastPlankTickMs = 0L
        isPausedResetApplied = false
        _state.value = _state.value.copy(
            exerciseMode = mode,
            count = 0,
            targetCount = activeTarget,
            isDownPhase = false,
            sensorMovedInCycle = false,
            resetNotice = null,
            isExerciseCompleted = false,
            statusHint = when (mode) {
                ExerciseMode.PUSHUP -> "Camera + Sensor Ready — Push-Up suru korun!"
                ExerciseMode.SQUAT -> "Camera + Sensor Ready — Squat (উঠা-বসা) suru korun!"
                ExerciseMode.PLANK -> "Plank Position nin — ৬০ সেকেন্ড ক্যামেরার সামনে স্থির থাকুন!"
            }
        )
    }

    fun startSensors() {
        val accelOk = accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        } ?: false
        val proxOk = proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        } ?: false
        _state.value = _state.value.copy(isSensorActive = accelOk || proxOk)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        runCatching { poseDetector.close() }
        runCatching { faceDetector.close() }
    }

    fun checkContinuityTimeout(nowMs: Long = System.currentTimeMillis()) {
        val current = _state.value
        if (current.isExerciseCompleted) return
        if (current.exerciseMode == ExerciseMode.PLANK) {
            if (current.isFaceVisible) {
                if (lastPlankTickMs == 0L) {
                    lastPlankTickMs = nowMs
                } else if (nowMs - lastPlankTickMs >= 1000L) {
                    lastPlankTickMs = nowMs
                    val nextSec = (current.count + 1).coerceAtMost(activeTarget)
                    val done = nextSec >= activeTarget
                    _state.value = current.copy(
                        count = nextSec,
                        isExerciseCompleted = done,
                        resetNotice = null,
                        statusHint = if (done) "MashAllah! 1 Min Plank Complete!" else "Plank: $nextSec / 60s — ধৈর্য ধরে রাখুন!"
                    )
                }
            } else if (current.count > 0) {
                if (lastPlankTickMs > 0L && (nowMs - lastPlankTickMs) > 3000L) {
                    lastPlankTickMs = 0L
                    _state.value = current.copy(
                        count = 0,
                        resetNotice = "Plank ভেঙ্গে গেছে! আবার ০ সেকেন্ড থেকে শুরু করুন।"
                    )
                }
            }
            return
        }

        if (current.count > 0 && lastRepTimestampMs > 0L && (nowMs - lastRepTimestampMs) > PAUSE_RESET_MS) {
            if (!isPausedResetApplied) {
                isPausedResetApplied = true
                wentDownByCamera = false
                sensorMovedDuringRep = false
                _state.value = current.copy(
                    count = 0,
                    isDownPhase = false,
                    sensorMovedInCycle = false,
                    resetNotice = "Thamlen keno Boss! Abar 0 Theke Suru Hobe"
                )
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val mag = sqrt(x * x + y * y + z * z)
                val delta = abs(mag - lastAccelMag)
                lastAccelMag = mag
                val threshold = if (_state.value.exerciseMode == ExerciseMode.SQUAT) 1.05f else 1.35f
                if (delta > threshold) {
                    markSensorMovement()
                }
            }
            Sensor.TYPE_PROXIMITY -> {
                val maxRange = event.sensor.maximumRange
                if (event.values[0] < maxRange) {
                    markSensorMovement()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun markSensorMovement() {
        if (!sensorMovedDuringRep) {
            sensorMovedDuringRep = true
            _state.value = _state.value.copy(sensorMovedInCycle = true)
        }
    }

    fun createImageAnalyzer(): ImageAnalysis.Analyzer {
        _state.value = _state.value.copy(isCameraActive = true)
        return ImageAnalysis.Analyzer { imageProxy ->
            processImageProxy(imageProxy)
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val frameHeight = image.height.coerceAtLeast(1).toFloat()

        val guard = guardPrefs.getCurrentState()
        val shouldCheckOwnerFace = enableSecurityCapture &&
            !securityCheckCompleted && guard.ownerFaceEnrolled && guard.driveSecurityEnabled
        val pendingTasks = AtomicInteger(if (shouldCheckOwnerFace) 2 else 1)

        fun completeOneTask() {
            if (pendingTasks.decrementAndGet() == 0) {
                imageProxy.close()
            }
        }

        if (shouldCheckOwnerFace) {
            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    if (securityCheckCompleted) return@addOnSuccessListener
                    val face = faces.firstOrNull() ?: return@addOnSuccessListener
                    val signature = OwnerFaceVerifier.extractSignature(face) ?: return@addOnSuccessListener
                    when (OwnerFaceVerifier.verifyCandidateAgainstStoredJson(guard.ownerFaceSignatureJson, signature)) {
                        FaceVerificationResult.OWNER_MATCH_NEVER_UPLOAD -> {
                            securityCheckCompleted = true
                            otherPersonConsecutiveHits = 0
                            guardPrefs.recordSecurityEvent("Owner verified — photo skipped (never uploaded)")
                            _state.value = _state.value.copy(
                                isOwnerVerified = true,
                                securityStatusLabel = "Owner ✓ (Photo Never Uploaded)"
                            )
                        }
                        FaceVerificationResult.OTHER_PERSON_DETECTED -> {
                            otherPersonConsecutiveHits++
                            if (otherPersonConsecutiveHits >= 3) {
                                securityCheckCompleted = true
                                val saved = OwnerFaceVerifier.saveOtherPersonFrameToPendingFile(appContext, imageProxy)
                                if (saved != null) {
                                    DriveSecurityUploadWorker.enqueueAlertUpload(
                                        context = appContext,
                                        photoFile = saved,
                                        reason = lockTriggerReason
                                    )
                                }
                                _state.value = _state.value.copy(
                                    isOwnerVerified = false,
                                    securityStatusLabel = "Other Person Detected -> Drive Noor Security"
                                )
                            }
                        }
                        FaceVerificationResult.NO_OWNER_ENROLLED -> {
                            securityCheckCompleted = true
                        }
                    }
                }
                .addOnCompleteListener {
                    completeOneTask()
                }
        }

        poseDetector.process(image)
            .addOnSuccessListener { pose ->
                val nose = pose.getPoseLandmark(PoseLandmark.NOSE)
                val leftEye = pose.getPoseLandmark(PoseLandmark.LEFT_EYE)
                val rightEye = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE)
                val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)

                val trackPoint = nose ?: leftEye ?: rightEye ?: leftShoulder
                if (trackPoint == null || trackPoint.inFrameLikelihood < 0.45f) {
                    _state.value = _state.value.copy(
                        isCameraActive = true,
                        isFaceVisible = false,
                        statusHint = "Boss, Face Dekha Jaitase Na"
                    )
                    return@addOnSuccessListener
                }

                if (_state.value.exerciseMode == ExerciseMode.PLANK) {
                    _state.value = _state.value.copy(
                        isCameraActive = true,
                        isFaceVisible = true
                    )
                    return@addOnSuccessListener
                }

                val normY = (trackPoint.position.y / frameHeight).coerceIn(0f, 1f)
                val base = baselineY
                if (base == null) {
                    baselineY = normY
                } else if (!wentDownByCamera && normY < base) {
                    baselineY = base * 0.85f + normY * 0.15f
                }

                val activeBase = baselineY ?: normY
                val dipDelta = if (_state.value.exerciseMode == ExerciseMode.SQUAT) 0.095f else 0.085f
                val wentDownThreshold = activeBase + dipDelta
                val cameUpThreshold = activeBase + 0.035f

                if (!wentDownByCamera && normY >= wentDownThreshold) {
                    wentDownByCamera = true
                    _state.value = _state.value.copy(
                        isCameraActive = true,
                        isFaceVisible = true,
                        isDownPhase = true,
                        statusHint = "Niche namchen Boss! Ebar upore uthen..."
                    )
                } else if (wentDownByCamera && normY <= cameUpThreshold) {
                    val now = System.currentTimeMillis()
                    wentDownByCamera = false
                    if (sensorMovedDuringRep) {
                        sensorMovedDuringRep = false
                        isPausedResetApplied = false
                        lastRepTimestampMs = now
                        val nextCount = (_state.value.count + 1).coerceAtMost(activeTarget)
                        val completed = nextCount >= activeTarget
                        val remaining = (activeTarget - nextCount).coerceAtLeast(0)
                        _state.value = _state.value.copy(
                            count = nextCount,
                            isCameraActive = true,
                            isFaceVisible = true,
                            isDownPhase = false,
                            sensorMovedInCycle = false,
                            resetNotice = null,
                            isExerciseCompleted = completed,
                            statusHint = if (completed) {
                                "MashAllah Boss! Exercise Complete!"
                            } else {
                                "Ar $remaining ta Baki Boss!"
                            }
                        )
                    } else {
                        _state.value = _state.value.copy(
                            isCameraActive = true,
                            isFaceVisible = true,
                            isDownPhase = false,
                            sensorMovedInCycle = false,
                            statusHint = "Sensor e movement paoa jayni Boss — Faki dewa jaibo na!"
                        )
                    }
                } else {
                    _state.value = _state.value.copy(
                        isCameraActive = true,
                        isFaceVisible = true,
                        statusHint = if (_state.value.count == 0) {
                            "Boss, Camera + Sensor Ready — ${_state.value.exerciseMode.labelBn} suru korun!"
                        } else {
                            val remaining = (activeTarget - _state.value.count).coerceAtLeast(0)
                            "Ar $remaining ta Baki Boss!"
                        }
                    )
                }
            }
            .addOnCompleteListener {
                completeOneTask()
            }
    }

    companion object {
        private const val PAUSE_RESET_MS = 5500L
    }
}
