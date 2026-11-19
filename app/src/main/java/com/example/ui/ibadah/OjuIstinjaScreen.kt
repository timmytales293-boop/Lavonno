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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Pause
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.VoiceEngineState
import com.example.data.content.IbadahGuideCatalog
import com.example.ui.theme.AmiriFontFamily
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OjuIstinjaScreen(
    initialTab: Int = 0,
    ojuChecklistBits: Int,
    pakChecklistBits: Int,
    voiceState: VoiceEngineState,
    onToggleOjuBit: (Int) -> Unit,
    onTogglePakBit: (Int) -> Unit,
    onToggleSpeak: (String, String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    var tabIndex by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    val tabs = listOf("অজু (Oju)", "পানি নেওয়া / ইস্তিনজা")

    Scaffold(
        containerColor = NoorDarkGreen,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "পবিত্রতা — অজু ও ইস্তিনজা",
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
                    item {
                        Image(
                            painter = painterResource(id = R.drawable.img_oju_steps),
                            contentDescription = "Oju Illustration",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(165.dp)
                                .clip(RoundedCornerShape(18.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    item {
                        Text(
                            text = "অজু করার নিয়ম (Step by Step Bangla)",
                            style = MaterialTheme.typography.titleLarge,
                            color = NoorGold,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(IbadahGuideCatalog.ojuSteps, key = { it.stepNumber }) { step ->
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

                    // Oju Vonger Karon
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = NoorCrimsonSurface),
                            border = BorderStroke(1.dp, NoorCrimson.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "অজু ভঙ্গের কারণ সমূহ",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NoorGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                IbadahGuideCatalog.ojuBreakers.forEach { reason ->
                                    Text(
                                        text = reason,
                                        color = NoorIvory,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Interactive Oju Checklist
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = NoorCardGreen),
                            border = BorderStroke(1.dp, NoorGold.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "অজু চেকলিস্ট",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NoorGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                IbadahGuideCatalog.ojuChecklistItems.forEachIndexed { idx, label ->
                                    val checked = (ojuChecklistBits and (1 shl idx)) != 0
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onToggleOjuBit(idx) }
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                            contentDescription = label,
                                            tint = if (checked) NoorMintAccent else NoorGold
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(text = label, color = NoorIvory)
                                    }
                                }
                            }
                        }
                    }

                    // Oju End Dua + Audio
                    item {
                        DuaAudioBoxCard(
                            id = "oju_end_dua",
                            title = "অজু শেষের দোয়া (কালিমা শাহাদাত)",
                            arabic = IbadahGuideCatalog.ojuAfterDuaArabic,
                            bangla = IbadahGuideCatalog.ojuAfterDuaBangla,
                            voiceState = voiceState,
                            onToggleSpeak = onToggleSpeak
                        )
                    }
                } else {
                    // Pani Newa / Istinja Tab
                    item {
                        Text(
                            text = "পানি নেওয়ার নিয়ম (ইস্তিনজা — Step by Step)",
                            style = MaterialTheme.typography.titleLarge,
                            color = NoorGold,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(IbadahGuideCatalog.istinjaSteps, key = { it.stepNumber }) { step ->
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

                    item {
                        DuaAudioBoxCard(
                            id = "toilet_before",
                            title = "টয়লেটে প্রবেশের আগের দোয়া",
                            arabic = IbadahGuideCatalog.toiletBeforeDua.first,
                            bangla = IbadahGuideCatalog.toiletBeforeDua.second,
                            voiceState = voiceState,
                            onToggleSpeak = onToggleSpeak
                        )
                    }

                    item {
                        DuaAudioBoxCard(
                            id = "toilet_after",
                            title = "টয়লেট থেকে বের হওয়ার দোয়া",
                            arabic = IbadahGuideCatalog.toiletAfterDua.first,
                            bangla = IbadahGuideCatalog.toiletAfterDua.second,
                            voiceState = voiceState,
                            onToggleSpeak = onToggleSpeak
                        )
                    }

                    // Pak / Napak Interactive Checklist
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = NoorCardGreen),
                            border = BorderStroke(1.dp, NoorGold.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "পাক / নাপাক চেকলিস্ট",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NoorGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                IbadahGuideCatalog.pakNapakChecklist.forEachIndexed { idx, pair ->
                                    val checked = (pakChecklistBits and (1 shl idx)) != 0
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onTogglePakBit(idx) }
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                            contentDescription = pair.first,
                                            tint = if (checked) NoorMintAccent else NoorGold
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(text = pair.first, color = NoorIvory)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DuaAudioBoxCard(
    id: String,
    title: String,
    arabic: String,
    bangla: String,
    voiceState: VoiceEngineState,
    onToggleSpeak: (String, String) -> Unit
) {
    val isPlaying = voiceState.isSpeaking && voiceState.activeItemId == id
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NoorGoldSurface),
        border = BorderStroke(1.dp, NoorGold)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = NoorGold,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isPlaying) NoorMintAccent else NoorGold,
                    modifier = Modifier.clickable { onToggleSpeak(id, bangla) }
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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = arabic,
                fontFamily = AmiriFontFamily,
                fontSize = 23.sp,
                lineHeight = 36.sp,
                color = NoorGold,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = bangla, color = NoorIvory, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
