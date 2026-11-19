package com.example.ui.dua

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.VoiceEngineState
import com.example.data.content.DuaCatalog
import com.example.data.model.DuaItem
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.NoorBorderGreen
import com.example.ui.theme.NoorCardGreen
import com.example.ui.theme.NoorDarkGreen
import com.example.ui.theme.NoorDeepestGreen
import com.example.ui.theme.NoorGold
import com.example.ui.theme.NoorGoldSoft
import com.example.ui.theme.NoorGoldSurface
import com.example.ui.theme.NoorIvory
import com.example.ui.theme.NoorMintAccent
import com.example.ui.theme.NoorSageText

@Composable
fun DuaScreen(
    duas: List<DuaItem>,
    voiceState: VoiceEngineState,
    onToggleDuaAudio: (String, String) -> Unit
) {
    var selectedCategory by rememberSaveable { mutableStateOf("সব") }
    val allCategories = remember { listOf("সব") + DuaCatalog.categories }

    val filteredDuas = remember(duas, selectedCategory) {
        if (selectedCategory == "সব") {
            duas
        } else {
            duas.filter { it.category == selectedCategory }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NoorDarkGreen)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(NoorDeepestGreen)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = "মাসনূন দোয়া সমূহ",
                style = MaterialTheme.typography.headlineMedium,
                color = NoorGold,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(allCategories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NoorGold,
                            selectedLabelColor = NoorDeepestGreen,
                            containerColor = NoorCardGreen,
                            labelColor = NoorIvory
                        )
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredDuas, key = { it.id }) { dua ->
                val audioId = "dua_${dua.id}"
                val isPlaying = voiceState.isSpeaking && voiceState.activeItemId == audioId
                val audioText = "${dua.titleBn}। উচ্চারণ: ${dua.banglaPronunciation} অর্থ: ${dua.banglaMeaning}"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dua_card_${dua.id}"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NoorCardGreen),
                    border = BorderStroke(1.dp, NoorBorderGreen)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = NoorGoldSurface,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Text(
                                    text = "${dua.category} • ${dua.titleBn}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = NoorGold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isPlaying) NoorMintAccent else NoorGold,
                                modifier = Modifier
                                    .clickable { onToggleDuaAudio(audioId, audioText) }
                                    .testTag("dua_audio_${dua.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = if (isPlaying) "Stop Dua Audio" else "Play Dua Audio",
                                        tint = NoorDeepestGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isPlaying) "Stop" else "অডিও",
                                        color = NoorDeepestGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = dua.arabicText,
                            fontFamily = AmiriFontFamily,
                            fontSize = 25.sp,
                            lineHeight = 40.sp,
                            color = NoorGold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "উচ্চারণ: ${dua.banglaPronunciation}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = NoorGoldSoft
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "অর্থ: ${dua.banglaMeaning}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = NoorIvory
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "সূত্র: ${dua.reference}",
                            style = MaterialTheme.typography.labelMedium,
                            color = NoorSageText
                        )
                    }
                }
            }
        }
    }
}
