package com.example.ui.lock

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExerciseMode
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.NoorBorderGreen
import com.example.ui.theme.NoorCardGreen
import com.example.ui.theme.NoorCrimson
import com.example.ui.theme.NoorCrimsonSurface
import com.example.ui.theme.NoorDarkGreen
import com.example.ui.theme.NoorDeepestGreen
import com.example.ui.theme.NoorGold
import com.example.ui.theme.NoorGoldSurface
import com.example.ui.theme.NoorIvory
import com.example.ui.theme.NoorMintAccent
import com.example.ui.theme.NoorSageText
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@Composable
fun PushUpLockScreen(
    targetPushUps: Int,
    triggerReminder: String,
    onSpeakIstighfar: () -> Unit,
    onUnlocked: () -> Unit
) {
    // Disable system Back button until Istighfar + Camera/Sensor exercise are completed
    BackHandler(enabled = true) { }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var istighfarDone by rememberSaveable { mutableStateOf(false) }
    val bannerTitle = triggerReminder.ifBlank { "Allah Dekhtase Mama, Tham" }
    val detector = remember(targetPushUps, bannerTitle) {
        PushUpDetector(context, targetPushUps, bannerTitle)
    }
    val tracking by detector.state.collectAsStateWithLifecycle()

    DisposableEffect(detector) {
        detector.startSensors()
        onDispose { detector.stop() }
    }

    LaunchedEffect(detector) {
        while (true) {
            delay(500L)
            detector.checkContinuityTimeout()
        }
    }

    LaunchedEffect(tracking.isExerciseCompleted, istighfarDone) {
        if (tracking.isExerciseCompleted && istighfarDone) {
            delay(650L)
            onUnlocked()
        }
    }

    val remaining = (tracking.targetCount - tracking.count).coerceAtLeast(0)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = NoorDeepestGreen
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Reminder Header ("Allah Dekhtase Mama, Tham")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = NoorCrimsonSurface,
                border = BorderStroke(1.dp, NoorCrimson)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Direct Lock",
                        tint = NoorGold,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = bannerTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = NoorGold,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("lock_reminder_text")
                    )
                }
            }

            // Step 1: 1ta Istighfar / Dua Unlock Requirement
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (istighfarDone) NoorCardGreen else NoorGoldSurface
                ),
                border = BorderStroke(1.dp, if (istighfarDone) NoorMintAccent else NoorGold)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "১। আনলক দোয়া / ইস্তিগফার",
                            style = MaterialTheme.typography.labelLarge,
                            color = NoorGold,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = onSpeakIstighfar,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Listen Istighfar",
                                tint = NoorGold
                            )
                        }
                    }
                    Text(
                        text = "أَسْتَغْفِرُ اللَّهَ الْعَظِيمَ — আস্তাগফিরুল্লাহাল আজীম",
                        fontFamily = AmiriFontFamily,
                        fontSize = 19.sp,
                        color = NoorIvory,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = { istighfarDone = true },
                        enabled = !istighfarDone,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("istighfar_confirm_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (istighfarDone) NoorMintAccent else NoorGold,
                            contentColor = NoorDeepestGreen
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (istighfarDone) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = if (istighfarDone) "ইস্তিগফার সম্পন্ন ✓" else "১ বার ইস্তিগফার পড়েছি (ট্যাপ করুন)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Exercise Mode Switcher: 30 Push-Up | 30 Squat | 1 Min Plank
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExerciseMode.entries.forEach { mode ->
                    val label = if (mode == ExerciseMode.PUSHUP && targetPushUps <= 10) "10 Push-Up"
                    else if (mode == ExerciseMode.SQUAT && targetPushUps <= 10) "15 Squat"
                    else mode.labelBn
                    FilterChip(
                        selected = tracking.exerciseMode == mode,
                        onClick = { detector.switchExerciseMode(mode, targetPushUps) },
                        label = {
                            Text(
                                text = label,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NoorGold,
                            selectedLabelColor = NoorDeepestGreen,
                            containerColor = NoorCardGreen,
                            labelColor = NoorIvory
                        )
                    )
                }
            }

            // Live Camera Preview + Face & Sensor Verification
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = NoorDarkGreen),
                border = BorderStroke(1.5.dp, NoorGold.copy(alpha = 0.6f))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission) {
                        val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
                        DisposableEffect(Unit) {
                            onDispose { cameraExecutor.shutdown() }
                        }
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.surfaceProvider = previewView.surfaceProvider
                                    }
                                    val analysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()
                                        .also {
                                            it.setAnalyzer(cameraExecutor, detector.createImageAnalyzer())
                                        }
                                    runCatching {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_FRONT_CAMERA,
                                            preview,
                                            analysis
                                        )
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Camera Required",
                                tint = NoorGold,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Boss, Exercise Gunar Jonno Camera + Sensor Access Lagbe",
                                style = MaterialTheme.typography.titleMedium,
                                color = NoorIvory,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NoorGold,
                                    contentColor = NoorDeepestGreen
                                ),
                                modifier = Modifier.testTag("grant_camera_lock_button")
                            ) {
                                Text("Allow Camera", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Overlay pill showing Camera + Sensor status
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(10.dp)
                            .clip(CircleShape)
                            .background(NoorDeepestGreen.copy(alpha = 0.88f))
                            .border(1.dp, NoorBorderGreen, CircleShape)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = if (hasCameraPermission && tracking.isCameraActive) NoorMintAccent else NoorCrimson,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Camera: ${if (hasCameraPermission && tracking.isCameraActive) "ON" else "OFF"}",
                                color = NoorIvory,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = null,
                                tint = if (tracking.isSensorActive) NoorMintAccent else NoorGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Sensor: ${if (tracking.isSensorActive) "ON" else "READY"}",
                                color = NoorIvory,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (hasCameraPermission && !tracking.isFaceVisible) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(10.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(NoorCrimson.copy(alpha = 0.92f))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Boss, Face Dekha Jaitase Na",
                                color = NoorIvory,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    } else if (!tracking.securityStatusLabel.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(10.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(NoorDeepestGreen.copy(alpha = 0.9f))
                                .border(
                                    1.dp,
                                    if (tracking.isOwnerVerified == true) NoorMintAccent else NoorGold,
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = tracking.securityStatusLabel!!,
                                color = if (tracking.isOwnerVerified == true) NoorMintAccent else NoorGold,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Counter & Progress Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = NoorCardGreen),
                border = BorderStroke(1.dp, NoorBorderGreen)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val unitLabel = if (tracking.exerciseMode.isTimedSeconds) "s" else ""
                    Text(
                        text = "${tracking.exerciseMode.name}: ${tracking.count}$unitLabel / ${tracking.targetCount}$unitLabel",
                        style = MaterialTheme.typography.titleMedium,
                        color = NoorGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("pushup_counter_text")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when {
                            tracking.isExerciseCompleted && !istighfarDone -> "ওপরে ১ বার ইস্তিগফার ট্যাপ করলেই খুলবে!"
                            remaining == 0 -> "MashAllah Boss! Khule Ditesi..."
                            tracking.exerciseMode.isTimedSeconds -> "Ar ${remaining}s Plank Baki Boss!"
                            else -> "Ar $remaining ta Baki Boss!"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = NoorIvory,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = {
                            if (tracking.targetCount > 0) tracking.count.toFloat() / tracking.targetCount.toFloat() else 0f
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        color = NoorGold,
                        trackColor = NoorDeepestGreen
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = tracking.resetNotice ?: tracking.statusHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (tracking.resetNotice != null) NoorCrimson else NoorSageText,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
