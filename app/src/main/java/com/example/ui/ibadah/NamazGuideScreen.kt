package com.example.ui.ibadah

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.VoiceEngineState
import com.example.data.content.IbadahGuideCatalog
import com.example.data.model.NamazLog
import com.example.data.model.PrayerScheduleItem
import com.example.data.model.Wakto
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.NoorBorderGreen
import com.example.ui.theme.NoorCardGreen
import com.example.ui.theme.NoorDarkGreen
import com.example.ui.theme.NoorDeepestGreen
import com.example.ui.theme.NoorElevatedGreen
import com.example.ui.theme.NoorGold
import com.example.ui.theme.NoorGoldSoft
import com.example.ui.theme.NoorGoldSurface
import com.example.ui.theme.NoorIvory
import com.example.ui.theme.NoorMintAccent
import com.example.ui.theme.NoorSageText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NamazGuideScreen(
    schedule: List<PrayerScheduleItem>,
    todayLog: NamazLog?,
    weeklyPrayedCount: Int,
    isPeriodMode: Boolean,
    voiceState: VoiceEngineState,
    onToggleNamaz: (Wakto, NamazLog?) -> Unit,
    onToggleSpeak: (String, String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("ধাপ ও ছবি", "৫ ওয়াক্ত ও রাকাত", "সূরা ও দোয়া")

    Scaffold(
        containerColor = NoorDarkGreen,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "নামাজ শিক্ষা ও ট্র্যাকার",
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
            TabRow(
                selectedTabIndex = tabIndex,
                containerColor = NoorDeepestGreen,
                contentColor = NoorGold,
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
                        text = {
                            Text(
                                text = title,
                                color = if (tabIndex == idx) NoorGold else NoorSageText,
                                fontWeight = FontWeight.SemiBold
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
                if (tabIndex == 0) {
                    // Step by Step Bangla + Chobi
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = NoorCardGreen),
                            border = BorderStroke(1.5.dp, NoorGold.copy(alpha = 0.55f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "নামাজের ৪টি মূল ভঙ্গি (ছবি সহ)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NoorGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Image(
                                    painter = painterResource(id = R.drawable.img_namaz_postures),
                                    contentDescription = "Namaz Posture Diagram",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(175.dp)
                                        .clip(RoundedCornerShape(14.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "কিয়াম (দাঁড়ানো) → রুকু → সিজদা → তাশাহহুদ (বসা)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NoorGoldSoft
                                )
                            }
                        }
                    }

                    items(IbadahGuideCatalog.namazSteps, key = { it.stepNumber }) { step ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = NoorCardGreen),
                            border = BorderStroke(1.dp, NoorBorderGreen)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = step.titleBn,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NoorGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = step.descriptionBn,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = NoorIvory
                                )
                            }
                        }
                    }
                } else if (tabIndex == 1) {
                    // 5 Wakto Tracker + Rakat Guide
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = NoorCardGreen),
                            border = BorderStroke(1.dp, NoorGold.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = if (isPeriodMode) {
                                        "পিরিয়ড মোড চালু আছে — এখন নামাজ মাফ, দোয়া-জিকির করুন"
                                    } else {
                                        "আজকের নামাজ ট্র্যাকার • সপ্তাহে $weeklyPrayedCount/35 ওয়াক্ত"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NoorMintAccent,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                schedule.forEach { item ->
                                    val isPrayed = when (item.wakto) {
                                        Wakto.FAJR -> todayLog?.fajr == true
                                        Wakto.DHUHR -> todayLog?.dhuhr == true
                                        Wakto.ASR -> todayLog?.asr == true
                                        Wakto.MAGHRIB -> todayLog?.maghrib == true
                                        Wakto.ISHA -> todayLog?.isha == true
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isPrayed) NoorElevatedGreen else NoorDeepestGreen)
                                            .clickable { onToggleNamaz(item.wakto, todayLog) }
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                            .testTag("guide_namaz_tick_${item.wakto.name}"),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${item.wakto.displayBn} (${item.formattedTime})",
                                            color = NoorIvory,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isPrayed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = null,
                                                tint = if (isPrayed) NoorGold else NoorSageText,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isPrayed) "পড়ছি ✓" else "পড়ছি",
                                                color = if (isPrayed) NoorGold else NoorSageText,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    items(IbadahGuideCatalog.waktoRakats, key = { it.waktoName }) { info ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = NoorCardGreen),
                            border = BorderStroke(1.dp, NoorBorderGreen)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = info.waktoName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NoorGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = info.rakatBreakdown,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = NoorMintAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = info.whatToRecite,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NoorIvory
                                )
                            }
                        }
                    }
                } else {
                    // Sura + Dua with Audio Playback
                    items(IbadahGuideCatalog.namazRecitations, key = { it.id }) { rec ->
                        val isPlaying = voiceState.isSpeaking && voiceState.activeItemId == "namaz_${rec.id}"
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = NoorCardGreen),
                            border = BorderStroke(1.dp, NoorBorderGreen)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = rec.titleBn,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = NoorGold,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (isPlaying) NoorMintAccent else NoorGold,
                                        modifier = Modifier.clickable {
                                            onToggleSpeak("namaz_${rec.id}", rec.audioSpeakText)
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = "Audio",
                                                tint = NoorDeepestGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isPlaying) "Stop" else "শুনুন",
                                                color = NoorDeepestGreen,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = rec.arabicText,
                                    fontFamily = AmiriFontFamily,
                                    fontSize = 24.sp,
                                    lineHeight = 38.sp,
                                    color = NoorGold,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "উচ্চারণ: ${rec.banglaPronunciation}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = NoorGoldSoft
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "অর্থ: ${rec.banglaMeaning}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NoorIvory
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
