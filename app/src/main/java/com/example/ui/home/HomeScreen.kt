package com.example.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.content.HadithCatalog
import com.example.data.content.QuranCatalog
import com.example.data.model.NamazLog
import com.example.data.model.UserProfile
import com.example.data.model.Wakto
import com.example.domain.prayer.PrayerTimeCalculator
import com.example.guard.DailyUsageReport
import com.example.guard.LocalGuardState
import com.example.ui.PrayerCountdownUi
import com.example.ui.theme.NoorBorderGreen
import com.example.ui.theme.NoorCardGreen
import com.example.ui.theme.NoorCrimson
import com.example.ui.theme.NoorCrimsonSurface
import com.example.ui.theme.NoorDarkGreen
import com.example.ui.theme.NoorDeepestGreen
import com.example.ui.theme.NoorElevatedGreen
import com.example.ui.theme.NoorGold
import com.example.ui.theme.NoorGoldSoft
import com.example.ui.theme.NoorGoldSurface
import com.example.ui.theme.NoorIvory
import com.example.ui.theme.NoorMintAccent
import com.example.ui.theme.NoorSageText
import java.util.Calendar
import java.util.Locale

@Composable
fun HomeScreen(
    countdownUi: PrayerCountdownUi,
    profile: UserProfile?,
    namazLogs: List<NamazLog>,
    guardState: LocalGuardState,
    usageReport: DailyUsageReport,
    proactiveReminders: List<String>,
    missedPrayersToday: List<Wakto>,
    weeklyPrayedCount: Int,
    onToggleNamaz: (Wakto, NamazLog?) -> Unit,
    onToggleAzan: (Boolean) -> Unit,
    onTestAzan: () -> Unit,
    onSpeakReminder: (String) -> Unit,
    onSpeakDailyReport: () -> Unit,
    onLocationDetected: (String, Double, Double) -> Unit,
    onOpenQuran: () -> Unit,
    onOpenHadith: () -> Unit,
    onOpenAi: () -> Unit,
    onOpenNamazGuide: () -> Unit,
    onOpenOjuIstinja: () -> Unit,
    onOpenSelfDefense: () -> Unit,
    onOpenPhoneGuard: () -> Unit,
    onDismissBanner: () -> Unit
) {
    val context = LocalContext.current
    val dayOfYear = remember { Calendar.getInstance().get(Calendar.DAY_OF_YEAR) }
    val todayKey = remember { PrayerTimeCalculator.todayDateKey() }
    val todayLog = namazLogs.firstOrNull { it.dateKey == todayKey }
    val dailyAyah = remember(dayOfYear) { QuranCatalog.dailyAyahOfTheDay(dayOfYear) }
    val dailyHadith = remember(dayOfYear) { HadithCatalog.dailyHadithOfTheDay(dayOfYear) }
    var locationUpdatedMsg by remember { mutableStateOf<String?>(null) }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fetchCoarseLocation(context) { city, lat, lon ->
                onLocationDetected(city, lat, lon)
                locationUpdatedMsg = "$city - Auto"
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NoorDarkGreen),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Post-Unlock AI Message Banner
        if (!guardState.postUnlockBanner.isNullOrBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = NoorGoldSurface),
                    border = BorderStroke(1.5.dp, NoorGold)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Noor — ${guardState.familyRole.greetingPrefix}",
                                style = MaterialTheme.typography.labelLarge,
                                color = NoorGold
                            )
                            Text(
                                text = guardState.postUnlockBanner,
                                style = MaterialTheme.typography.titleMedium,
                                color = NoorIvory,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(
                            onClick = onDismissBanner,
                            border = BorderStroke(1.dp, NoorGold)
                        ) {
                            Text("Thik Ase", color = NoorGold)
                        }
                    }
                }
            }
        }

        // 1. SOBAR UPORE: Next Namaz Countdown - Dark Green + Gold Premium Look
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("prayer_countdown_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = NoorDeepestGreen),
                border = BorderStroke(1.5.dp, NoorGold.copy(alpha = 0.7f))
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_noor_sanctuary_banner),
                        contentDescription = null,
                        modifier = Modifier
                            .matchParentSize()
                            .alpha(0.24f),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        NoorDeepestGreen.copy(alpha = 0.75f),
                                        NoorDarkGreen.copy(alpha = 0.9f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(NoorCardGreen.copy(alpha = 0.9f))
                                    .clickable {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (granted) {
                                            fetchCoarseLocation(context) { city, lat, lon ->
                                                onLocationDetected(city, lat, lon)
                                                locationUpdatedMsg = "$city - Auto"
                                            }
                                        } else {
                                            locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Location",
                                    tint = NoorGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = locationUpdatedMsg ?: "${countdownUi.cityName} - Auto",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = NoorIvory
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = NoorGoldSurface,
                                border = BorderStroke(1.dp, NoorGold.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = guardState.familyRole.displayBn,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NoorGoldSoft,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Next: ${countdownUi.nextPrayerName} - Baki",
                            style = MaterialTheme.typography.titleLarge,
                            color = NoorGoldSoft,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = countdownUi.countdownText,
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 48.sp),
                            color = NoorGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("countdown_timer_text")
                        )
                    }
                }
            }
        }

        // 2. PREMIUM FEMALE VOICE REMINDER ("NOOR") + DAILY REPORT VOICE
        item {
            val activeHeadline = proactiveReminders.firstOrNull() ?: "Assalamualaikum Boss, Ajker Din Ta Valo Hok"
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("female_voice_reminder_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NoorElevatedGreen),
                border = BorderStroke(1.dp, NoorGold.copy(alpha = 0.55f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = "Noor Female Voice",
                                tint = NoorGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Noor — Female Voice Reminder",
                                style = MaterialTheme.typography.titleMedium,
                                color = NoorGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = { onSpeakReminder(activeHeadline) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Play Noor Voice",
                                tint = NoorGold
                            )
                        }
                    }

                    Text(
                        text = activeHeadline,
                        style = MaterialTheme.typography.bodyLarge,
                        color = NoorIvory,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(onClick = onOpenAi)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onSpeakDailyReport,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NoorGold,
                                contentColor = NoorDeepestGreen
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Daily Report শুনুন", fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onOpenAi,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, NoorGold),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Live Voice AI", color = NoorGold, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }

                    if (missedPrayersToday.isNotEmpty() && !guardState.isPeriodMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val missReport = missedPrayersToday.joinToString(", ") { "${it.displayEn} Miss" }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NoorCrimsonSurface,
                            border = BorderStroke(1.dp, NoorCrimson.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "AI Report: $missReport",
                                style = MaterialTheme.typography.labelLarge,
                                color = NoorIvory,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. 2x2 PREMIUM GUIDE HUB: Namaz+Chobi | Oju & Istinja | Self Defence & SOS | FF & Haram Guard
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HubBentoCard(
                        icon = Icons.Default.Mosque,
                        title = "নামাজ ও ছবি",
                        subtitle = "৫ ওয়াক্ত, সূরা ও অডিও",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hub_namaz_card"),
                        onClick = onOpenNamazGuide
                    )
                    HubBentoCard(
                        icon = Icons.Default.WaterDrop,
                        title = "অজু ও ইস্তিনজা",
                        subtitle = "পানি নেওয়া ও পাক-নাপাক",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hub_oju_card"),
                        onClick = onOpenOjuIstinja
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HubBentoCard(
                        icon = Icons.Default.Shield,
                        title = "Self Defence",
                        subtitle = "SOS, ৫ মি. ও ছুটানো",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hub_defence_card"),
                        onClick = onOpenSelfDefense
                    )
                    HubBentoCard(
                        icon = Icons.Default.Games,
                        title = "FF ছাড় ও গার্ড",
                        subtitle = "FF ${usageReport.ffMinutesToday}/${guardState.ffDailyLimitMinutes}m • হারাম ব্লক",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hub_guard_card"),
                        onClick = onOpenPhoneGuard
                    )
                }
            }
        }

        // Optional Period / Pak Status Chip if active or Family Mode = Ma/Bon
        if (guardState.isPeriodMode) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenPhoneGuard),
                    shape = RoundedCornerShape(16.dp),
                    color = NoorCardGreen,
                    border = BorderStroke(1.dp, NoorMintAccent)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Spa, contentDescription = null, tint = NoorMintAccent)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "পিরিয়ড ট্র্যাকার চালু (দিন ${guardState.periodDayNumber.coerceAtLeast(1)}) — নামাজ এখন মাফ, দোয়া ও জিকির করুন।",
                            color = NoorIvory,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // 4. Daily Ayat + Daily Hadith
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenQuran)
                        .testTag("daily_ayat_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = NoorCardGreen),
                    border = BorderStroke(1.dp, NoorBorderGreen)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = "Ajker Ayat",
                            tint = NoorGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "আজকের আয়াত (${dailyAyah.second})",
                                style = MaterialTheme.typography.labelMedium,
                                color = NoorGoldSoft
                            )
                            Text(
                                text = dailyAyah.first,
                                style = MaterialTheme.typography.bodyLarge,
                                color = NoorIvory,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenHadith)
                        .testTag("daily_hadith_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = NoorCardGreen),
                    border = BorderStroke(1.dp, NoorBorderGreen)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Ajker Hadith",
                            tint = NoorGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "আজকের হাদীস (${dailyHadith.hadithNumber})",
                                style = MaterialTheme.typography.labelMedium,
                                color = NoorGoldSoft
                            )
                            Text(
                                text = dailyHadith.banglaText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = NoorIvory,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 5. NAMAZ TRACKER + AZAN ALARM
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("namaz_tracker_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = NoorCardGreen),
                border = BorderStroke(1.dp, NoorBorderGreen)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "নামাজ ট্র্যাকার ও আজান",
                                style = MaterialTheme.typography.titleLarge,
                                color = NoorGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "সপ্তাহে $weeklyPrayedCount/35 ওয়াক্ত পড়ছেন",
                                style = MaterialTheme.typography.bodyMedium,
                                color = NoorMintAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onTestAzan) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Test Azan Sound",
                                    tint = NoorGold
                                )
                            }
                            Switch(
                                checked = guardState.azanEnabled,
                                onCheckedChange = onToggleAzan,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NoorDeepestGreen,
                                    checkedTrackColor = NoorGold
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = NoorBorderGreen)
                    Spacer(modifier = Modifier.height(10.dp))

                    countdownUi.schedule.forEach { item ->
                        val isPrayed = when (item.wakto) {
                            Wakto.FAJR -> todayLog?.fajr == true
                            Wakto.DHUHR -> todayLog?.dhuhr == true
                            Wakto.ASR -> todayLog?.asr == true
                            Wakto.MAGHRIB -> todayLog?.maghrib == true
                            Wakto.ISHA -> todayLog?.isha == true
                        }
                        val isMissed = !guardState.isPeriodMode && !isPrayed && (item.wakto in countdownUi.passedPrayers)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isPrayed) NoorElevatedGreen else NoorDeepestGreen.copy(alpha = 0.65f)
                                )
                                .clickable { onToggleNamaz(item.wakto, todayLog) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("namaz_row_${item.wakto.name.lowercase(Locale.US)}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${item.wakto.displayBn} (${item.wakto.displayEn})",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = NoorIvory,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (isMissed) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = NoorCrimsonSurface
                                        ) {
                                            Text(
                                                text = "${item.wakto.displayEn} Miss",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = NoorCrimson,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = item.formattedTime,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NoorGoldSoft
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isPrayed) NoorGold else NoorCardGreen,
                                border = BorderStroke(1.dp, if (isPrayed) NoorGold else NoorBorderGreen)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isPrayed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = if (isPrayed) "Prayed" else "Mark Prayed",
                                        tint = if (isPrayed) NoorDeepestGreen else NoorGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isPrayed) "পড়ছি ✓" else "পড়ছি",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isPrayed) NoorDeepestGreen else NoorIvory,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun HubBentoCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NoorCardGreen),
        border = BorderStroke(1.dp, NoorGold.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(NoorGoldSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = NoorGold, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = NoorGold,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = NoorSageText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@SuppressLint("MissingPermission")
private fun fetchCoarseLocation(
    context: Context,
    onResult: (String, Double, Double) -> Unit
) {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val loc = runCatching {
        lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
    }.getOrNull()

    if (loc == null) {
        onResult("Mymensingh", 24.7471, 90.4203)
        return
    }

    val cityName = runCatching {
        @Suppress("DEPRECATION")
        val list = Geocoder(context, Locale.getDefault()).getFromLocation(loc.latitude, loc.longitude, 1)
        list?.firstOrNull()?.locality ?: list?.firstOrNull()?.subAdminArea ?: "Mymensingh"
    }.getOrDefault("Mymensingh")

    onResult(cityName, loc.latitude, loc.longitude)
}
