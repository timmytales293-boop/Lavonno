package com.example.ui.quran

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.QuranPlaybackState
import com.example.data.content.QuranCatalog
import com.example.data.model.SurahMeta
import com.example.data.model.UserProfile
import com.example.ui.SurahReaderState
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.NoorBorderGreen
import com.example.ui.theme.NoorCardGreen
import com.example.ui.theme.NoorCrimson
import com.example.ui.theme.NoorDarkGreen
import com.example.ui.theme.NoorDeepestGreen
import com.example.ui.theme.NoorElevatedGreen
import com.example.ui.theme.NoorGold
import com.example.ui.theme.NoorGoldSoft
import com.example.ui.theme.NoorGoldSurface
import com.example.ui.theme.NoorIvory
import com.example.ui.theme.NoorSageText

@Composable
fun QuranScreen(
    surahs: List<SurahMeta>,
    profile: UserProfile?,
    readerState: SurahReaderState,
    audioState: QuranPlaybackState,
    onOpenSurah: (Int) -> Unit,
    onToggleAudio: (Int) -> Unit,
    onBookmarkAyah: (Int, Int, String) -> Unit
) {
    var selectedSurah by rememberSaveable { mutableStateOf<Int?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    if (selectedSurah != null) {
        val surahNum = selectedSurah!!
        val meta = surahs.firstOrNull { it.number == surahNum }
            ?: surahs.firstOrNull()
            ?: QuranCatalog.all114Surahs.first()
        BackHandler { selectedSurah = null }

        SurahReaderView(
            surah = meta,
            readerState = readerState,
            audioState = audioState,
            bookmarkedAyah = if (profile?.lastReadSurah?.toInt() == surahNum) profile.lastReadAyah.toInt() else null,
            onBack = { selectedSurah = null },
            onToggleAudio = { onToggleAudio(surahNum) },
            onBookmarkAyah = { ayahNum ->
                onBookmarkAyah(surahNum, ayahNum, meta.transliteration)
            }
        )
        return
    }

    val filteredSurahs = remember(surahs, searchQuery) {
        if (searchQuery.isBlank()) {
            surahs
        } else {
            val q = searchQuery.trim().lowercase()
            surahs.filter {
                it.transliteration.lowercase().contains(q) ||
                    it.banglaName.lowercase().contains(q) ||
                    it.number.toString() == q
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NoorDarkGreen)
    ) {
        // Header + Search
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(NoorDeepestGreen)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = "আল-কুরআন (১১৪ সূরা)",
                style = MaterialTheme.typography.headlineMedium,
                color = NoorGold,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Bookmark Resume Banner
            val lastSurah = (profile?.lastReadSurah ?: 1L).toInt()
            val lastAyah = (profile?.lastReadAyah ?: 1L).toInt()
            val lastName = profile?.lastReadSurahName ?: "Al-Fatihah"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedSurah = lastSurah
                        onOpenSurah(lastSurah)
                    }
                    .testTag("quran_bookmark_resume"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NoorGoldSurface),
                border = BorderStroke(1.dp, NoorGold)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Bookmark, contentDescription = "Bookmark", tint = NoorGold)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "শেষ পড়ার জায়গা (বুকমার্ক)",
                            style = MaterialTheme.typography.labelMedium,
                            color = NoorGoldSoft
                        )
                        Text(
                            text = "সূরা $lastName ($lastSurah) — আয়াত $lastAyah",
                            style = MaterialTheme.typography.titleMedium,
                            color = NoorIvory,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("সূরা খুঁজুন...", color = NoorSageText) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NoorGold) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quran_search_input"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NoorGold,
                    unfocusedBorderColor = NoorBorderGreen,
                    focusedTextColor = NoorIvory,
                    unfocusedTextColor = NoorIvory,
                    focusedContainerColor = NoorCardGreen,
                    unfocusedContainerColor = NoorCardGreen
                )
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredSurahs, key = { it.number }) { surah ->
                SurahListRow(
                    surah = surah,
                    onClick = {
                        selectedSurah = surah.number
                        onOpenSurah(surah.number)
                    }
                )
            }
        }
    }
}

@Composable
private fun SurahListRow(
    surah: SurahMeta,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("surah_item_${surah.number}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NoorCardGreen),
        border = BorderStroke(1.dp, NoorBorderGreen)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(NoorGoldSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${surah.number}",
                        color = NoorGold,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "${surah.transliteration} (${surah.banglaName})",
                        style = MaterialTheme.typography.titleMedium,
                        color = NoorIvory,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = buildString {
                            append("${surah.revelationType} • ${surah.ayahCount} আয়াত")
                            if (surah.isCachedOffline) append(" • Offline ✓")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (surah.isCachedOffline) NoorGoldSoft else NoorSageText
                    )
                }
            }
            Text(
                text = surah.arabicName,
                fontFamily = AmiriFontFamily,
                fontSize = 22.sp,
                color = NoorGold
            )
        }
    }
}

@Composable
private fun SurahReaderView(
    surah: SurahMeta,
    readerState: SurahReaderState,
    audioState: QuranPlaybackState,
    bookmarkedAyah: Int?,
    onBack: () -> Unit,
    onToggleAudio: () -> Unit,
    onBookmarkAyah: (Int) -> Unit
) {
    val isThisPlaying = audioState.surahNumber == surah.number && audioState.isPlaying
    val isThisPreparing = audioState.surahNumber == surah.number && audioState.isPreparing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NoorDarkGreen)
    ) {
        // Reader Top Bar + Qari Audio Controls
        Surface(color = NoorDeepestGreen) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to 114 Suras",
                                tint = NoorGold
                            )
                        }
                        Column {
                            Text(
                                text = "${surah.number}. ${surah.transliteration}",
                                style = MaterialTheme.typography.titleLarge,
                                color = NoorGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${surah.banglaName} • ${surah.ayahCount} আয়াত",
                                style = MaterialTheme.typography.bodyMedium,
                                color = NoorSageText
                            )
                        }
                    }

                    // Qari Tilawat Play / Pause Button
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = NoorGold,
                        modifier = Modifier
                            .clickable(onClick = onToggleAudio)
                            .testTag("quran_audio_play_pause")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isThisPreparing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = NoorDeepestGreen,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isThisPlaying) "Pause Tilawat" else "Play Tilawat",
                                    tint = NoorDeepestGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isThisPlaying) "Pause" else "তিলাওয়াত",
                                color = NoorDeepestGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                if (!audioState.error.isNullOrBlank()) {
                    Text(
                        text = audioState.error,
                        style = MaterialTheme.typography.labelMedium,
                        color = NoorCrimson,
                        modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                    )
                }
            }
        }

        when {
            readerState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NoorGold)
                }
            }
            !readerState.error.isNullOrBlank() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = readerState.error,
                        style = MaterialTheme.typography.titleMedium,
                        color = NoorIvory,
                        textAlign = TextAlign.Center
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(readerState.ayahs, key = { it.numberInSurah }) { ayah ->
                        val isBookmarked = bookmarkedAyah == ayah.numberInSurah
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isBookmarked) NoorElevatedGreen else NoorCardGreen
                            ),
                            border = BorderStroke(1.dp, if (isBookmarked) NoorGold else NoorBorderGreen)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = NoorGoldSurface
                                    ) {
                                        Text(
                                            text = "আয়াত ${ayah.numberInSurah}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = NoorGold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onBookmarkAyah(ayah.numberInSurah) },
                                        modifier = Modifier.testTag("bookmark_ayah_${ayah.numberInSurah}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bookmark,
                                            contentDescription = "Save Bookmark",
                                            tint = if (isBookmarked) NoorGold else NoorSageText
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = ayah.arabicText,
                                    fontFamily = AmiriFontFamily,
                                    fontSize = 26.sp,
                                    lineHeight = 42.sp,
                                    color = NoorGold,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "উচ্চারণ: ${ayah.banglaTransliteration}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NoorGoldSoft
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "অর্থ: ${ayah.banglaMeaning}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = NoorIvory,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = NoorDeepestGreen.copy(alpha = 0.7f)
                                ) {
                                    Text(
                                        text = ayah.shortTafsir,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = NoorSageText,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
