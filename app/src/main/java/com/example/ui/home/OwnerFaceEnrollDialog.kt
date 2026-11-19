package com.example.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.security.OwnerFaceVerifier
import com.example.ui.theme.NoorCardGreen
import com.example.ui.theme.NoorDarkGreen
import com.example.ui.theme.NoorDeepestGreen
import com.example.ui.theme.NoorGold
import com.example.ui.theme.NoorIvory
import com.example.ui.theme.NoorMintAccent
import com.example.ui.theme.NoorSageText
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun OwnerFaceEnrollDialog(
    onDismiss: () -> Unit,
    onEnrolled: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var statusText by remember { mutableStateOf("আপনার মুখ ক্যামেরার সামনে সোজা রাখুন...") }
    var capturedSignatureJson by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val faceDetector = remember {
        FaceDetection.getClient(OwnerFaceVerifier.createAccurateDetectorOptions())
    }
    val executor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { faceDetector.close() }
            executor.shutdown()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NoorCardGreen,
        titleContentColor = NoorGold,
        textContentColor = NoorIvory,
        title = {
            Text(
                text = "Owner Face সেটআপ",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Owner-এর ছবি কখনো সেভ বা Google Drive-এ আপলোড হবে না — শুধু গাণিতিক ফেস সিগনেচার ফোনে সংরক্ষিত থাকবে।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NoorSageText,
                    textAlign = TextAlign.Center
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NoorDarkGreen),
                    border = BorderStroke(
                        1.5.dp,
                        if (capturedSignatureJson != null) NoorMintAccent else NoorGold
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasCameraPermission) {
                            AndroidView(
                                factory = { ctx ->
                                    val previewView = PreviewView(ctx)
                                    val future = ProcessCameraProvider.getInstance(ctx)
                                    future.addListener({
                                        val provider = future.get()
                                        val preview = Preview.Builder().build().also {
                                            it.surfaceProvider = previewView.surfaceProvider
                                        }
                                        val analysis = ImageAnalysis.Builder()
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .build()
                                            .also { imageAnalysis ->
                                                imageAnalysis.setAnalyzer(executor) { proxy ->
                                                    val mediaImage = proxy.image
                                                    if (mediaImage == null) {
                                                        proxy.close()
                                                        return@setAnalyzer
                                                    }
                                                    val input = InputImage.fromMediaImage(
                                                        mediaImage,
                                                        proxy.imageInfo.rotationDegrees
                                                    )
                                                    faceDetector.process(input)
                                                        .addOnSuccessListener { faces ->
                                                            val face = faces.firstOrNull()
                                                            val sig = face?.let { OwnerFaceVerifier.extractSignature(it) }
                                                            if (sig != null) {
                                                                capturedSignatureJson = sig.toJson()
                                                                statusText = "Owner Face পাওয়া গেছে ✓ — Save করুন!"
                                                            }
                                                        }
                                                        .addOnCompleteListener { proxy.close() }
                                                }
                                            }
                                        runCatching {
                                            provider.unbindAll()
                                            provider.bindToLifecycle(
                                                lifecycleOwner,
                                                CameraSelector.DEFAULT_FRONT_CAMERA,
                                                preview,
                                                analysis
                                            )
                                        }
                                    }, ContextCompat.getMainExecutor(ctx))
                                    previewView
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        } else {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Owner Face এনরোল করতে ক্যামেরা পারমিশন দিন",
                                    color = NoorIvory,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NoorGold,
                                        contentColor = NoorDeepestGreen
                                    )
                                ) {
                                    Text("Allow Camera", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (capturedSignatureJson != null) NoorMintAccent else NoorGold,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val json = capturedSignatureJson ?: return@Button
                    onEnrolled(json)
                },
                enabled = capturedSignatureJson != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NoorGold,
                    contentColor = NoorDeepestGreen
                ),
                modifier = Modifier.testTag("save_owner_face_button")
            ) {
                Text("Save Owner Face", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NoorSageText)
            }
        }
    )
}
