package com.example.ui.defence

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.content.SelfDefenseCatalog
import com.example.ui.theme.NoorBorderGreen
import com.example.ui.theme.NoorCardGreen
import com.example.ui.theme.NoorCrimson
import com.example.ui.theme.NoorCrimsonSurface
import com.example.ui.theme.NoorDarkGreen
import com.example.ui.theme.NoorDeepestGreen
import com.example.ui.theme.NoorGold
import com.example.ui.theme.NoorGoldSoft
import com.example.ui.theme.NoorGoldSurface
import com.example.ui.theme.NoorIvory
import com.example.ui.theme.NoorMintAccent
import com.example.ui.theme.NoorSageText
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelfDefenseScreen(
    initialTab: Int = 0,
    savedSosPhone: String,
    defensePlanDayBits: Int,
    defaultLat: Double,
    defaultLon: Double,
    onSaveSosPhone: (String) -> Unit,
    onTogglePlanDay: (Int) -> Unit,
    onSpeakCue: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var tabIndex by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
    val tabs = listOf("SOS ও গাইড", "৫ মি. ও ৭ দিন", "ছুটানো ব্যায়াম", "ফিটনেস ব্যায়াম")

    var sosInput by rememberSaveable(savedSosPhone) { mutableStateOf(savedSosPhone) }
    var pendingSosPhone by remember { mutableStateOf("") }
    val sosLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        launchEmergencySmsWithLocation(
            context = context,
            phoneNumber = pendingSosPhone,
            fallbackLat = defaultLat,
            fallbackLon = defaultLon
        )
    }

    var practiceRunning by rememberSaveable { mutableStateOf(false) }
    var practiceRemainingSec by rememberSaveable { mutableIntStateOf(300) }
    var sprintRunning by rememberSaveable { mutableStateOf(false) }
    var sprintRemainingSec by rememberSaveable { mutableIntStateOf(10) }

    LaunchedEffect(practiceRunning) {
        while (practiceRunning && practiceRemainingSec > 0) {
            delay(1000L)
            practiceRemainingSec--
            when (practiceRemainingSec) {
                180 -> onSpeakCue("Boss 3 Min Baki, হাত ছুটানো ও কনুইয়ের আঘাত অনুশীলন করুন")
                60 -> onSpeakCue("Boss 1 Min Baki, চিৎকার ও ১০ সেকেন্ড স্প্রিন্টের প্রস্তুতি নিন")
                0 -> {
                    practiceRunning = false
                    onSpeakCue("Boss Apni Ajke Onnek Valo Korsen, MashAllah! ৫ মিনিট সেলফ ডিফেন্স শেষ।")
                }
            }
        }
    }

    LaunchedEffect(sprintRunning) {
        while (sprintRunning && sprintRemainingSec > 0) {
            delay(1000L)
            sprintRemainingSec--
            when (sprintRemainingSec) {
                5 -> onSpeakCue("দ্রুত দৌড়ান, আর ৫ সেকেন্ড!")
                0 -> {
                    sprintRunning = false
                    onSpeakCue("MashAllah Boss! ১০ সেকেন্ড স্প্রিন্ট কমপ্লিট!")
                }
            }
        }
    }

    Scaffold(
        containerColor = NoorDarkGreen,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "আত্মরক্ষা ও SOS সুরক্ষা",
                        color = NoorGold,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NoorGold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NoorDeepestGreen)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(NoorDarkGreen)
        ) {
            ScrollableTabRow(
                selectedTabIndex = tabIndex,
                containerColor = NoorDeepestGreen,
                contentColor = NoorGold,
                edgePadding = 12.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                        color = NoorGold
                    )
                }
            ) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = tabIndex == idx,
                        onClick = { tabIndex = idx },
                        modifier = Modifier.testTag("defense_tab_$idx"),
                        text = {
                            Text(
                                text = title,
                                color = if (tabIndex == idx) NoorGold else NoorSageText,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (tabIndex) {
                    0 -> {
                        // Emergency SOS Card + Location + Family SMS
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = NoorCrimsonSurface),
                                border = BorderStroke(1.5.dp, NoorCrimson)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = NoorGold)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "জরুরি SOS — লোকেশন + পরিবারকে SMS",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = NoorGold,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = sosInput,
                                            onValueChange = { sosInput = it },
                                            placeholder = { Text("পরিবারের নম্বর (যেমন 017...)", color = NoorSageText) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = NoorGold,
                                                unfocusedBorderColor = NoorBorderGreen,
                                                focusedTextColor = NoorIvory,
                                                unfocusedTextColor = NoorIvory
                                            )
                                        )
                                        IconButton(
                                            onClick = { onSaveSosPhone(sosInput.trim()) }
                                        ) {
                                            Icon(Icons.Default.Save, contentDescription = "Save SOS", tint = NoorGold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            val target = sosInput.trim()
                                            if (target.isNotBlank()) onSaveSosPhone(target)
                                            val hasLocation = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (hasLocation) {
                                                launchEmergencySmsWithLocation(
                                                    context = context,
                                                    phoneNumber = target,
                                                    fallbackLat = defaultLat,
                                                    fallbackLon = defaultLon
                                                )
                                            } else {
                                                pendingSosPhone = target
                                                sosLocationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(54.dp)
                                            .testTag("emergency_sos_button"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = NoorCrimson,
                                            contentColor = NoorIvory
                                        ),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(Icons.Default.Shield, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "EMERGENCY SOS পাঠান (Location + SMS)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Visual illustration
                        item {
                            Image(
                                painter = painterResource(id = R.drawable.img_self_defence_stance),
                                contentDescription = "Self Defence Stance",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(165.dp)
                                    .clip(RoundedCornerShape(18.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Emergency Number List
                        item {
                            Text(
                                text = "জরুরি হেল্পলাইন নম্বর সমূহ",
                                style = MaterialTheme.typography.titleMedium,
                                color = NoorGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(SelfDefenseCatalog.emergencyNumbers, key = { it.number }) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = NoorCardGreen),
                                border = BorderStroke(1.dp, NoorBorderGreen)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.titleBn, color = NoorIvory, fontWeight = FontWeight.Bold)
                                        Text(item.noteBn, color = NoorSageText, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Button(
                                        onClick = {
                                            val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.number}"))
                                            runCatching { context.startActivity(dial) }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = NoorGold,
                                            contentColor = NoorDeepestGreen
                                        )
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(item.number, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Kharap Poristhiti Guide
                        item {
                            Text(
                                text = "খারাপ পরিস্থিতি গাইড (Step by Step Bangla)",
                                style = MaterialTheme.typography.titleMedium,
                                color = NoorGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(SelfDefenseCatalog.badSituationGuide, key = { it.stepNumber }) { step ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = NoorCardGreen),
                                border = BorderStroke(1.dp, NoorBorderGreen)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(step.titleBn, color = NoorGold, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(step.descriptionBn, color = NoorIvory)
                                }
                            }
                        }

                        // Bangla Interactive Drills & Video Tutorials
                        item {
                            Text(
                                text = "ভিডিও ও ইন-অ্যাপ Bangla ড্রিল টিউটোরিয়াল",
                                style = MaterialTheme.typography.titleMedium,
                                color = NoorGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(SelfDefenseCatalog.banglaVideos, key = { it.id }) { video ->
                            var showInAppPlayer by remember { mutableStateOf(false) }
                            if (showInAppPlayer) {
                                val matchingTechnique = SelfDefenseCatalog.chutanoTechniques.firstOrNull { tech ->
                                    when (video.id) {
                                        "vid_wrist" -> tech.id == "hat_dhora"
                                        "vid_back" -> tech.id == "pison_theke"
                                        else -> tech.id == "jama_orna"
                                    }
                                } ?: SelfDefenseCatalog.chutanoTechniques.first()

                                androidx.compose.material3.AlertDialog(
                                    onDismissRequest = { showInAppPlayer = false },
                                    containerColor = NoorCardGreen,
                                    titleContentColor = NoorGold,
                                    textContentColor = NoorIvory,
                                    title = {
                                        Text(video.titleBn, fontWeight = FontWeight.Bold)
                                    },
                                    text = {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Image(
                                                painter = painterResource(id = R.drawable.img_self_defence_stance),
                                                contentDescription = video.titleBn,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(140.dp)
                                                    .clip(RoundedCornerShape(12.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                            matchingTechnique.stepsBn.forEachIndexed { idx, step ->
                                                Text("${idx + 1}. $step", color = NoorIvory)
                                            }
                                            Text(
                                                text = "মূল কৌশল: ${matchingTechnique.keyPointBn}",
                                                color = NoorMintAccent,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                val speech = matchingTechnique.stepsBn.joinToString("। ") + "। " + matchingTechnique.keyPointBn
                                                onSpeakCue(speech)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = NoorGold,
                                                contentColor = NoorDeepestGreen
                                            )
                                        ) {
                                            Text("ভয়েস গাইড শুনুন", fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    dismissButton = {
                                        OutlinedButton(
                                            onClick = {
                                                showInAppPlayer = false
                                                runCatching {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(video.searchQueryUrl)))
                                                }
                                            },
                                            border = BorderStroke(1.dp, NoorGold)
                                        ) {
                                            Text("YouTube", color = NoorGold)
                                        }
                                    }
                                )
                            }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showInAppPlayer = true },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = NoorCardGreen),
                                border = BorderStroke(1.dp, NoorGold.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircleOutline,
                                        contentDescription = "Open Bangla Drill Guide",
                                        tint = NoorGold,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(video.titleBn, color = NoorIvory, fontWeight = FontWeight.Bold)
                                        Text("${video.durationText} • ${video.summaryBn}", color = NoorSageText, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // Daily 5 Min Practice + 7 Din Plan + Checklist
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = NoorGoldSurface),
                                border = BorderStroke(1.5.dp, NoorGold)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "দৈনিক ৫ মিনিট সেলফ ডিফেন্স প্র্যাকটিস",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = NoorGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val mins = practiceRemainingSec / 60
                                    val secs = practiceRemainingSec % 60
                                    Text(
                                        text = String.format("%02d:%02d", mins, secs),
                                        fontSize = 44.sp,
                                        color = NoorGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = {
                                                practiceRunning = !practiceRunning
                                                if (practiceRunning) {
                                                    onSpeakCue("Boss Apnar Self Defence Practice Er Time Hoise, 5 Min Practice Koren")
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = NoorGold,
                                                contentColor = NoorDeepestGreen
                                            )
                                        ) {
                                            Icon(Icons.Default.Timer, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (practiceRunning) "Pause" else "Start 5 Min")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                practiceRunning = false
                                                practiceRemainingSec = 300
                                            },
                                            border = BorderStroke(1.dp, NoorGold)
                                        ) {
                                            Text("Reset", color = NoorGold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    SelfDefenseCatalog.daily5MinDrills.forEach { drill ->
                                        Text(
                                            text = "• $drill",
                                            color = NoorIvory,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = "৭ দিনের সেলফ ডিফেন্স প্ল্যান ও চেকলিস্ট",
                                style = MaterialTheme.typography.titleMedium,
                                color = NoorGold,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(SelfDefenseCatalog.sevenDayPlan, key = { it.dayNumber }) { day ->
                            val bitIndex = day.dayNumber - 1
                            val checked = (defensePlanDayBits and (1 shl bitIndex)) != 0
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTogglePlanDay(bitIndex) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = NoorCardGreen),
                                border = BorderStroke(1.dp, if (checked) NoorMintAccent else NoorBorderGreen)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                        contentDescription = day.titleBn,
                                        tint = if (checked) NoorMintAccent else NoorGold
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(day.titleBn, color = NoorGold, fontWeight = FontWeight.Bold)
                                        Text(day.drillSummaryBn, color = NoorIvory, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // Chutano / Palano Beyam Tab
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = NoorGoldSurface),
                                border = BorderStroke(1.5.dp, NoorGold)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, tint = NoorGold)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "ছুটাইয়া ১০ সেকেন্ড স্প্রিন্ট + চিৎকার ড্রিল",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = NoorGold,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "${sprintRemainingSec}s",
                                        fontSize = 42.sp,
                                        color = NoorGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = {
                                                sprintRemainingSec = 10
                                                sprintRunning = true
                                                onSpeakCue("Boss Apnar Chutano Beyam Er Time Hoise! হাত ছুটাইয়া চিৎকার দিয়ে দৌড়ান!")
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = NoorGold,
                                                contentColor = NoorDeepestGreen
                                            )
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("10 Sec Sprint Start", fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                sprintRunning = false
                                                sprintRemainingSec = 10
                                            },
                                            border = BorderStroke(1.dp, NoorGold)
                                        ) {
                                            Text("Reset", color = NoorGold)
                                        }
                                    }
                                }
                            }
                        }

                        items(SelfDefenseCatalog.chutanoTechniques, key = { it.id }) { tech ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = NoorCardGreen),
                                border = BorderStroke(1.dp, NoorBorderGreen)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = tech.titleBn,
                                            color = NoorGold,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedButton(
                                            onClick = {
                                                val cue = "${tech.titleBn}। " + tech.stepsBn.joinToString("। ") + "। " + tech.keyPointBn
                                                onSpeakCue(cue)
                                            },
                                            border = BorderStroke(1.dp, NoorGold),
                                            modifier = Modifier.testTag("speak_technique_${tech.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = "ভয়েস শুনুন",
                                                tint = NoorGold,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("শুনুন", color = NoorGold, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    tech.stepsBn.forEachIndexed { idx, s ->
                                        Text("${idx + 1}. $s", color = NoorIvory, modifier = Modifier.padding(vertical = 2.dp))
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = NoorDeepestGreen
                                    ) {
                                        Text(
                                            text = "মূল কৌশল: ${tech.keyPointBn}",
                                            color = NoorMintAccent,
                                            modifier = Modifier.padding(10.dp),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        // Self Defence Strength & Camera/Sensor Exercise Tab
                        item {
                            SelfDefenseExerciseCard(onSpeakCue = onSpeakCue)
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun launchEmergencySmsWithLocation(
    context: Context,
    phoneNumber: String,
    fallbackLat: Double,
    fallbackLon: Double
) {
    var lat = fallbackLat
    var lon = fallbackLon
    val hasCoarse = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    if (hasCoarse) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val loc = runCatching {
            lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
        }.getOrNull()
        if (loc != null) {
            lat = loc.latitude
            lon = loc.longitude
        }
    }
    val mapsUrl = "https://maps.google.com/?q=$lat,$lon"
    val body = "জরুরি SOS! আমি বিপদে আছি, দয়া করে দ্রুত সাহায্য করুন। আমার লোকেশন: $mapsUrl"
    val smsUri = Uri.parse("smsto:${phoneNumber.ifBlank { "" }}")
    val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
        putExtra("sms_body", body)
    }
    runCatching { context.startActivity(smsIntent) }
}
