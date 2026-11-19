package com.example.ui.hadith

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.content.HadithCatalog
import com.example.data.model.HadithItem
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
fun HadithScreen(
    hadiths: List<HadithItem>
) {
    var selectedBook by rememberSaveable { mutableStateOf("Sob") }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val filtered = remember(hadiths, selectedBook, searchQuery) {
        hadiths.filter { h ->
            val bookMatch = selectedBook == "Sob" || h.book.equals(selectedBook, ignoreCase = true)
            val q = searchQuery.trim().lowercase()
            val textMatch = q.isBlank() ||
                h.banglaText.lowercase().contains(q) ||
                h.simpleExplanation.lowercase().contains(q) ||
                h.hadithNumber.lowercase().contains(q)
            bookMatch && textMatch
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
                text = "সহীহ হাদীস শরীফ",
                style = MaterialTheme.typography.headlineMedium,
                color = NoorGold,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("হাদীস খুঁজুন...", color = NoorSageText) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NoorGold) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hadith_search_input"),
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

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(HadithCatalog.books) { book ->
                    FilterChip(
                        selected = selectedBook == book,
                        onClick = { selectedBook = book },
                        label = {
                            Text(
                                text = when (book) {
                                    "Sob" -> "সব কিতাব"
                                    "Bukhari" -> "বুখারী"
                                    "Muslim" -> "মুসলিম"
                                    "Tirmizi" -> "তিরমিযী"
                                    else -> book
                                },
                                fontWeight = FontWeight.SemiBold
                            )
                        },
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
            items(filtered, key = { it.id }) { hadith ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hadith_card_${hadith.id}"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NoorCardGreen),
                    border = BorderStroke(1.dp, NoorBorderGreen)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = NoorGoldSurface
                        ) {
                            Text(
                                text = hadith.hadithNumber,
                                style = MaterialTheme.typography.labelLarge,
                                color = NoorGold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = hadith.arabicText,
                            fontFamily = AmiriFontFamily,
                            fontSize = 23.sp,
                            lineHeight = 38.sp,
                            color = NoorGold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = hadith.banglaText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = NoorIvory,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NoorDeepestGreen.copy(alpha = 0.75f)
                        ) {
                            Text(
                                text = hadith.simpleExplanation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = NoorMintAccent,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "বর্ণনাকারী: ${hadith.narrator}",
                            style = MaterialTheme.typography.labelMedium,
                            color = NoorGoldSoft
                        )
                    }
                }
            }
        }
    }
}
