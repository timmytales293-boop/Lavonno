package com.example.ui.defence

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.ui.lock.PushUpDetector
import com.example.ui.theme.NoorBorderGreen
import com.example.ui.theme.NoorCardGreen
import com.example.ui.theme.NoorCrimson
import com.example.ui.theme.NoorDarkGreen
import com.example.ui.theme.NoorDeepestGreen
import com.example.ui.theme.NoorGold
import com.example.ui.theme.NoorGoldSurface
import com.example.ui.theme.NoorIvory
import com.example.ui.theme.NoorMintAccent
import com.example.ui.theme.NoorSageText
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

/**
 * Voluntary Self-Defence Fitness & Strength Practice pane with live CameraX + Sensor tracking
 * for Push-Up, Squat, and 1 Min Plank, coached aloud by Noor's Female Voice profile.
 */
@Composable
fun SelfDefenseExerciseCard(
    onSpeakCue: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedTarget by rememberSaveable { mutableIntStateOf(10) }
    var isSessionActive by rememberSaveable { mutableStateOf(false) }
    var halfwaySpoken by rememberSaveable { mutableStateOf(false) }
    var completeSpoken by rememberSaveable { mutableStateOf(false) }

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
        if (granted) {
            isSessionActive = true
        }
    }

    val detector = remember(selectedTarget) {
        PushUpDetector(
            context = context,
            initialTargetCount = selectedTarget,
            lockTriggerReason = "Self Defence Exercise Practice",
            enableSecurityCapture = false
        )
    }
    val tracking by detector.state.collectAsStateWithLifecycle()

    DisposableEffect(detector, isSessionActive) {
        if (isSessionActive) {
            detector.startSensors()
        }
        onDispose {
            detector.stop()
        }
    }

    LaunchedEffect(detector, isSessionActive) {
        while (isSessionActive) {
            delay(500L)
            detector.checkContinuityTimeout()
        }
    }

    LaunchedEffect(tracking.count, tracking.isExerciseCompleted, isSessionActive) {
        if (!isSessionActive) return@LaunchedEffect
        val target = tracking.targetCount.coerceAtLeast(1)
        if (!halfwaySpoken && tracking.count >= (target / 2).coerceAtLeast(1) && !tracking.isExerciseCompleted) {
            halfwaySpoken = true
            onSpeakCue("Boss Apni Parben, Allah Apnar Loge Ase")
        }
        if (!completeSpoken && tracking.isExerciseCompleted) {
            completeSpoken = true
            onSpeakCue("Boss Apni Ajke Onnek Valo Korsen, MashAllah! Exercise Complete!")
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NoorGoldSurface),
            border = BorderStroke(1.5.dp, NoorGold)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = NoorGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "সেলফ ডিফেন্স স্ট্রেংথ ও ফিটনেস (Camera + Sensor)",
                        style = MaterialTheme.typography.titleMedium,
                        color = NoorGold,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "হাত ছুটানো ও দৌড়ের শক্তি বাড়াতে Push-Up, Squat বা ১ মিনিট Plank প্র্যাকটিস করুন — Noor ক্যামেরা ও সেন্সর দিয়ে নিজেই গুনবে।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NoorIvory
                )

                // Exercise mode chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExerciseMode.entries.forEach { mode ->
                        val label = when {
                            mode == ExerciseMode.PUSHUP -> "${selectedTarget} Push-Up"
                            mode == ExerciseMode.SQUAT -> if (selectedTarget <= 10) "15 Squat" else "30 Squat"
                            else -> mode.labelBn
                        }
                        FilterChip(
                            selected = tracking.exerciseMode == mode,
                            onClick = {
                                halfwaySpoken = false
                                completeSpoken = false
                                detector.switchExerciseMode(mode, selectedTarget)
                            },
                            label = { Text(label, fontWeight = FontWeight.Bold, maxLines = 1) },
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

                // Target rep presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("টার্গেট:", color = NoorSageText, fontWeight = FontWeight.SemiBold)
                    listOf(10, 20, 30).forEach { reps ->
                        FilterChip(
                            selected = selectedTarget == reps,
                            onClick = {
                                selectedTarget = reps
                                halfwaySpoken = false
                                completeSpoken = false
                                detector.switchExerciseMode(tracking.exerciseMode, reps)
                            },
                            label = { Text("$reps") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NoorMintAccent,
                                selectedLabelColor = NoorDeepestGreen,
                                containerColor = NoorDeepestGreen,
                                labelColor = NoorIvory
                            )
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            if (!hasCameraPermission) {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            } else {
                                isSessionActive = !isSessionActive
                                if (isSessionActive) {
                                    halfwaySpoken = false
                                    completeSpoken = false
                                    onSpeakCue("Boss, Camera + Sensor ব্যায়াম শুরু করুন! Apni Parben!")
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("defense_exercise_toggle_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSessionActive) NoorCrimson else NoorGold,
                            contentColor = if (isSessionActive) NoorIvory else NoorDeepestGreen
                        )
                    ) {
                        Icon(
                            imageVector = if (isSessionActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSessionActive) "থামান" else "ব্যায়াম শুরু করুন",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            halfwaySpoken = false
                            completeSpoken = false
                            detector.switchExerciseMode(tracking.exerciseMode, selectedTarget)
                        },
                        border = BorderStroke(1.dp, NoorGold)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = NoorGold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", color = NoorGold)
                    }
                }
            }
        }

        // Camera Preview + Rep Counter when active
        if (isSessionActive) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NoorDarkGreen),
                border = BorderStroke(1.5.dp, NoorGold.copy(alpha = 0.65f))
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(NoorDeepestGreen.copy(alpha = 0.85f))
                            .border(1.dp, NoorBorderGreen, CircleShape)
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = if (tracking.isCameraActive) NoorMintAccent else NoorGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Camera", color = NoorIvory, fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Sensors,
                                contentDescription = null,
                                tint = if (tracking.isSensorActive) NoorMintAccent else NoorGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sensor", color = NoorIvory, fontSize = 11.sp)
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = NoorCardGreen),
                border = BorderStroke(1.dp, if (tracking.isExerciseCompleted) NoorMintAccent else NoorBorderGreen)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val unit = if (tracking.exerciseMode.isTimedSeconds) "s" else ""
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (tracking.isExerciseCompleted) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NoorMintAccent)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = "${tracking.exerciseMode.labelBn}: ${tracking.count}$unit / ${tracking.targetCount}$unit",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (tracking.isExerciseCompleted) NoorMintAccent else NoorGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("defense_exercise_counter")
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = {
                            if (tracking.targetCount > 0) tracking.count.toFloat() / tracking.targetCount.toFloat() else 0f
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        color = if (tracking.isExerciseCompleted) NoorMintAccent else NoorGold,
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
