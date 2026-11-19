package com.example.security

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.hypot
import kotlin.math.sqrt

data class FaceSignature(
    val features: List<Double>
) {
    fun toJson(): String {
        val arr = JSONArray()
        features.forEach { arr.put(it) }
        return arr.toString()
    }

    companion object {
        fun fromJson(raw: String): FaceSignature? {
            if (raw.isBlank()) return null
            return runCatching {
                val arr = JSONArray(raw)
                if (arr.length() < 4) return null
                val list = buildList {
                    for (i in 0 until arr.length()) {
                        add(arr.getDouble(i))
                    }
                }
                FaceSignature(list)
            }.getOrNull()
        }
    }
}

enum class FaceVerificationResult {
    NO_OWNER_ENROLLED,
    OWNER_MATCH_NEVER_UPLOAD,
    OTHER_PERSON_DETECTED
}

/**
 * On-device ML Kit Face geometry verifier.
 * Stores ONLY a numeric facial landmark ratio signature for the Owner — never saves or uploads
 * the Owner's photo. Detects when an unrecognized person is using the device so a security alert
 * photo can be queued for the "Noor Security" Google Drive folder.
 */
object OwnerFaceVerifier {

    const val MAX_OWNER_DISTANCE_THRESHOLD = 0.24

    fun createAccurateDetectorOptions(): FaceDetectorOptions =
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.18f)
            .build()

    fun extractSignature(face: Face): FaceSignature? {
        val box = face.boundingBox
        val boxW = box.width().toDouble()
        val boxH = box.height().toDouble()
        if (boxW <= 10.0 || boxH <= 10.0) return null

        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position ?: return null
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position ?: return null
        val nose = face.getLandmark(FaceLandmark.NOSE_BASE)?.position ?: return null
        val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position ?: return null
        val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position ?: return null
        val mouthBottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position

        val leftCheek = face.getLandmark(FaceLandmark.LEFT_CHEEK)?.position
        val rightCheek = face.getLandmark(FaceLandmark.RIGHT_CHEEK)?.position

        return buildSignatureFromPoints(
            boxWidth = boxW,
            boxHeight = boxH,
            leftEyeX = leftEye.x.toDouble(),
            leftEyeY = leftEye.y.toDouble(),
            rightEyeX = rightEye.x.toDouble(),
            rightEyeY = rightEye.y.toDouble(),
            noseX = nose.x.toDouble(),
            noseY = nose.y.toDouble(),
            mouthLeftX = mouthLeft.x.toDouble(),
            mouthLeftY = mouthLeft.y.toDouble(),
            mouthRightX = mouthRight.x.toDouble(),
            mouthRightY = mouthRight.y.toDouble(),
            mouthBottomX = mouthBottom?.x?.toDouble(),
            mouthBottomY = mouthBottom?.y?.toDouble(),
            leftCheekX = leftCheek?.x?.toDouble(),
            leftCheekY = leftCheek?.y?.toDouble(),
            rightCheekX = rightCheek?.x?.toDouble(),
            rightCheekY = rightCheek?.y?.toDouble()
        )
    }

    fun buildSignatureFromPoints(
        boxWidth: Double,
        boxHeight: Double,
        leftEyeX: Double,
        leftEyeY: Double,
        rightEyeX: Double,
        rightEyeY: Double,
        noseX: Double,
        noseY: Double,
        mouthLeftX: Double,
        mouthLeftY: Double,
        mouthRightX: Double,
        mouthRightY: Double,
        mouthBottomX: Double? = null,
        mouthBottomY: Double? = null,
        leftCheekX: Double? = null,
        leftCheekY: Double? = null,
        rightCheekX: Double? = null,
        rightCheekY: Double? = null
    ): FaceSignature? {
        val eyeDist = hypot(rightEyeX - leftEyeX, rightEyeY - leftEyeY)
        if (eyeDist < 8.0 || boxWidth <= 10.0 || boxHeight <= 10.0) return null

        val eyeMidX = (leftEyeX + rightEyeX) / 2.0
        val eyeMidY = (leftEyeY + rightEyeY) / 2.0
        val mouthMidX = (mouthLeftX + mouthRightX) / 2.0
        val mouthMidY = (mouthLeftY + mouthRightY) / 2.0

        val eyeToBoxRatio = eyeDist / boxWidth
        val aspectRatio = boxWidth / boxHeight
        val noseToEyeMid = hypot(noseX - eyeMidX, noseY - eyeMidY) / eyeDist
        val mouthWidthRatio = hypot(mouthRightX - mouthLeftX, mouthRightY - mouthLeftY) / eyeDist
        val noseToMouthMid = hypot(mouthMidX - noseX, mouthMidY - noseY) / eyeDist
        val eyeToMouthMid = hypot(mouthMidX - eyeMidX, mouthMidY - eyeMidY) / eyeDist
        val leftEyeToNose = hypot(noseX - leftEyeX, noseY - leftEyeY) / eyeDist
        val rightEyeToNose = hypot(noseX - rightEyeX, noseY - rightEyeY) / eyeDist
        val mouthDepthRatio = if (mouthBottomX != null && mouthBottomY != null) {
            hypot(mouthBottomX - noseX, mouthBottomY - noseY) / eyeDist
        } else {
            noseToMouthMid * 1.2
        }
        val cheekSpanRatio = if (leftCheekX != null && leftCheekY != null && rightCheekX != null && rightCheekY != null) {
            hypot(rightCheekX - leftCheekX, rightCheekY - leftCheekY) / eyeDist
        } else {
            1.65
        }

        return FaceSignature(
            features = listOf(
                eyeToBoxRatio,
                aspectRatio,
                noseToEyeMid,
                mouthWidthRatio,
                noseToMouthMid,
                eyeToMouthMid,
                leftEyeToNose,
                rightEyeToNose,
                mouthDepthRatio,
                cheekSpanRatio
            )
        )
    }

    fun computeDistance(a: FaceSignature, b: FaceSignature): Double {
        val size = minOf(a.features.size, b.features.size)
        if (size == 0) return Double.MAX_VALUE
        var sumSq = 0.0
        for (i in 0 until size) {
            val diff = a.features[i] - b.features[i]
            sumSq += diff * diff
        }
        return sqrt(sumSq / size)
    }

    fun isOwnerMatch(
        storedOwnerSignature: FaceSignature,
        candidateSignature: FaceSignature,
        threshold: Double = MAX_OWNER_DISTANCE_THRESHOLD
    ): Boolean {
        return computeDistance(storedOwnerSignature, candidateSignature) <= threshold
    }

    fun verifyCandidateAgainstStoredJson(
        storedOwnerJson: String,
        candidate: FaceSignature
    ): FaceVerificationResult {
        val ownerSig = FaceSignature.fromJson(storedOwnerJson)
            ?: return FaceVerificationResult.NO_OWNER_ENROLLED
        return if (isOwnerMatch(ownerSig, candidate)) {
            FaceVerificationResult.OWNER_MATCH_NEVER_UPLOAD
        } else {
            FaceVerificationResult.OTHER_PERSON_DETECTED
        }
    }

    @OptIn(ExperimentalGetImage::class)
    fun saveOtherPersonFrameToPendingFile(
        context: Context,
        imageProxy: ImageProxy
    ): File? {
        val image = imageProxy.image ?: return null
        if (image.format != ImageFormat.YUV_420_888) return null

        return runCatching {
            val width = image.width
            val height = image.height
            val yBuffer = image.planes[0].buffer.duplicate().apply { rewind() }
            val uBuffer = image.planes[1].buffer.duplicate().apply { rewind() }
            val vBuffer = image.planes[2].buffer.duplicate().apply { rewind() }

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, width, height), 82, out)

            val dir = File(context.applicationContext.filesDir, "noor_security_pending").apply {
                if (!exists()) mkdirs()
            }
            val target = File(dir, "noor_alert_${System.currentTimeMillis()}.jpg")
            FileOutputStream(target).use { fos ->
                fos.write(out.toByteArray())
            }
            target
        }.getOrNull()
    }
}
