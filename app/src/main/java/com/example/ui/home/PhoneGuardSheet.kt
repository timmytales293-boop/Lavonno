package com.example.ui.home

import android.app.Activity
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.FemaleVoiceTone
import com.example.audio.VoiceEngineState
import com.example.data.content.IbadahGuideCatalog
import com.example.data.model.FamilyRole
import com.example.data.model.LockMode
import com.example.guard.DailyUsageReport
import com.example.guard.LocalGuardState
import com.example.guard.NoorDeviceAdminReceiver
import com.example.guard.NoorGuardAccessibilityService
import com.example.security.GoogleDriveSecurityUploader
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.launch
import com.example.ui.theme.NoorBorderGreen
import com.example.ui.theme.NoorCardGreen
import com.example.ui.theme.NoorCrimson
import com.example.ui.theme.NoorDarkGreen
import com.example.ui.theme.NoorDeepestGreen
import com.example.ui.theme.NoorElevatedGreen
import com.example.ui.theme.NoorGold
import com.example.ui.theme.NoorGoldSurface
import com.example.ui.theme.NoorIvory
import com.example.ui.theme.NoorMintAccent
import com.example.ui.theme.NoorSageText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneGuardScreen(
    guardState: LocalGuardState,
    usageReport: DailyUsageReport,
    onBack: () -> Unit,
    onRefreshUsage: () -> Unit,
    onSetDailyLimit: (Int) -> Unit,
    onSetFfLimit: (Int) -> Unit,
    onToggleFfGuard: (Boolean) -> Unit,
    onSetAppLimit: (String, String, Int) -> Unit,
    onTogglePornBlock: (Boolean) -> Unit,
    onToggleGamblingBlock: (Boolean) -> Unit,
    onToggleUninstallLock: (Boolean) -> Unit,
    onToggleNightLock: (Boolean) -> Unit,
    onToggleNamazAutoSilent: (Boolean) -> Unit,
    onToggleFemaleVoice: (Boolean) -> Unit,
    onSelectFemaleTone: (FemaleVoiceTone) -> Unit,
    onToggleSleepDuaAuto: (Boolean) -> Unit,
    voiceState: VoiceEngineState,
    onSelectFamilyRole: (FamilyRole) -> Unit,
    onUpdatePeriodTracker: (Boolean, Int) -> Unit,
    onSpeakFemaleReminder: (String) -> Unit,
    onSpeakDailyReport: () -> Unit,
    onActivateFocus1h: () -> Unit,
    onClearFocus: () -> Unit,
    onTriggerLockNow: (LockMode, Int, String) -> Unit,
    onSaveOwnerFace: (String) -> Unit,
    onClearOwnerFace: () -> Unit,
    onToggleDriveSecurity: (Boolean) -> Unit,
    onDriveAuthorized: (String) -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accessibilityActive = isAccessibilityEnabled(context)
    val deviceAdminActive = isDeviceAdminActive(context)
    val dndGranted = isDndAccessGranted(context)
    val isFocusActive = guardState.focusModeUntilMs > System.currentTimeMillis()
    var showOwnerEnrollDialog by remember { mutableStateOf(false) }

    val driveAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            runCatching {
                val authResult = Identity.getAuthorizationClient(context)
                    .getAuthorizationResultFromIntent(result.data)
                val token = authResult.accessToken
                if (!token.isNullOrBlank()) {
                    onDriveAuthorized(token)
                }
            }
        }
    }

    if (showOwnerEnrollDialog) {
        OwnerFaceEnrollDialog(
            onDismiss = { showOwnerEnrollDialog = false },
            onEnrolled = { sigJson ->
                showOwnerEnrollDialog = false
                onSaveOwnerFace(sigJson)
            }
        )
    }

    Scaffold(
        containerColor = NoorDarkGreen,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "গার্ড, FF ছাড় ও প্রিমিয়াম কন্ট্রোল",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(NoorDarkGreen),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // System Guard Permissions Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NoorCardGreen),
                    border = BorderStroke(1.dp, NoorBorderGreen)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "সিস্টেম গার্ড পারমিশন",
                            style = MaterialTheme.typography.titleMedium,
                            color = NoorGold,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(
                                    1.dp,
                                    if (usageReport.hasPermission) NoorMintAccent else NoorGold
                                )
                            ) {
                                Text(
                                    text = if (usageReport.hasPermission) "Usage ✓" else "Usage",
                                    color = if (usageReport.hasPermission) NoorMintAccent else NoorGold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(
                                    1.dp,
                                    if (accessibilityActive) NoorMintAccent else NoorGold
                                )
                            ) {
                                Text(
                                    text = if (accessibilityActive) "Guard ✓" else "Guard",
                                    color = if (accessibilityActive) NoorMintAccent else NoorGold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                                },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(
                                    1.dp,
                                    if (dndGranted) NoorMintAccent else NoorGold
                                )
                            ) {
                                Text(
                                    text = if (dndGranted) "Silent ✓" else "DND",
                                    color = if (dndGranted) NoorMintAccent else NoorGold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                val comp = ComponentName(context, NoorDeviceAdminReceiver::class.java)
                                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, comp)
                                    putExtra(
                                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                        "Noor Uninstall Lock: ৩০ পুশ-আপ/স্কোয়াট ও ইস্তিগফার ছাড়া আনইনস্টল বন্ধ থাকবে।"
                                    )
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, if (deviceAdminActive) NoorMintAccent else NoorGold)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (deviceAdminActive) NoorMintAccent else NoorGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (deviceAdminActive) "Uninstall Admin Guard Active ✓" else "Activate Uninstall Admin Guard",
                                color = if (deviceAdminActive) NoorMintAccent else NoorGold
                            )
                        }
                    }
                }
            }

            // FF CHHAR (Free Fire 1h Limit + Namaz/11PM Lock + Top-up/Betting Block)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NoorCardGreen),
                    border = BorderStroke(1.5.dp, NoorGold.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Games, contentDescription = null, tint = NoorGold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "FF ছাড় (Free Fire কন্ট্রোল)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NoorGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Switch(
                                checked = guardState.ffGuardEnabled,
                                onCheckedChange = onToggleFfGuard,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NoorDeepestGreen,
                                    checkedTrackColor = NoorGold
                                )
                            )
                        }
                        Text(
                            text = "আজ FF খেলা হয়েছে: ${usageReport.ffMinutesToday} মি. / লিমিট ${guardState.ffDailyLimitMinutes} মি.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = NoorMintAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "• নামাজের সময় ও রাত ১১টার পর FF ডিরেক্ট লক\n• FF-এ টপ-আপ, ডায়মন্ড বেটিং ও জুয়া ব্লক\n• সময় শেষ হলে Noor বলবে: \"Boss Apnar FF Time Ses\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NoorSageText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(30, 60, 90).forEach { mins ->
                                FilterChip(
                                    selected = guardState.ffDailyLimitMinutes == mins,
                                    onClick = { onSetFfLimit(mins) },
                                    label = { Text(if (mins == 60) "১ ঘন্টা (Recommended)" else "${mins}m") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NoorGold,
                                        selectedLabelColor = NoorDeepestGreen
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { onSpeakFemaleReminder("Boss Apnar FF Time Ses, Ebar Quran Poren") },
                            border = BorderStroke(1.dp, NoorGold),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = NoorGold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("FF Time Ses ভয়েস শুনুন", color = NoorGold)
                        }
                    }
                }
            }

            // Haram + Porn + Juya/Betting + Nesha/Sud + Namaz Silent + 11 PM Lock
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NoorCardGreen),
                    border = BorderStroke(1.dp, NoorBorderGreen)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "হারাম ব্লক ও স্মার্ট লক",
                            style = MaterialTheme.typography.titleMedium,
                            color = NoorGold,
                            fontWeight = FontWeight.Bold
                        )
                        GuardToggleRow(
                            icon = Icons.Default.Block,
                            title = "All Haram & Porn Full Block",
                            subtitle = "Bangla / English / Banglish খারাপ কীওয়ার্ড, সাইট ও অ্যাপ ডিরেক্ট লক",
                            checked = guardState.pornBlockEnabled,
                            onCheckedChange = onTogglePornBlock
                        )
                        GuardToggleRow(
                            icon = Icons.Default.Casino,
                            title = "জুয়া, নেশা ও সুদ ব্লক",
                            subtitle = "Casino, Betting, Teen Patti, Rummy, মদ/নেশা ও সুদের সাইট ডিরেক্ট লক",
                            checked = guardState.gamblingBlockEnabled,
                            onCheckedChange = onToggleGamblingBlock
                        )
                        GuardToggleRow(
                            icon = Icons.Default.VolumeOff,
                            title = "নামাজের সময় ফোন Auto Silent + Lock",
                            subtitle = "আজান হলেই ১৫ মিনিট ফোন সাইলেন্ট ও অন্য অ্যাপ লক",
                            checked = guardState.namazAutoSilentEnabled,
                            onCheckedChange = onToggleNamazAutoSilent
                        )
                        GuardToggleRow(
                            icon = Icons.Default.Nightlight,
                            title = "রাত ১১টার পর Auto Phone Lock",
                            subtitle = "\"Boss Apnar Ghumer Time Hoise, Phone Rakhen\"",
                            checked = guardState.nightLockEnabled,
                            onCheckedChange = onToggleNightLock
                        )
                        GuardToggleRow(
                            icon = Icons.Default.Lock,
                            title = "Uninstall Lock (৩০ পুশ-আপ / স্কোয়াট)",
                            subtitle = "ডিলিট করতে গেলেই ডিরেক্ট লক",
                            checked = guardState.uninstallLockEnabled,
                            onCheckedChange = onToggleUninstallLock
                        )

                        Button(
                            onClick = { if (isFocusActive) onClearFocus() else onActivateFocus1h() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFocusActive) NoorCrimson else NoorElevatedGreen,
                                contentColor = NoorIvory
                            ),
                            border = BorderStroke(1.dp, NoorGold.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = if (isFocusActive) "Focus Mode বন্ধ করুন" else "Focus Mode (১ ঘন্টা শুধু কুরআন-হাদীস-দোয়া)",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Female Voice ("Noor") AI Reminders + Daily Report + Sleep Dua
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NoorCardGreen),
                    border = BorderStroke(1.dp, NoorGold.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Premium Female Voice — \"Noor\"",
                            style = MaterialTheme.typography.titleMedium,
                            color = NoorGold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (voiceState.isFemaleVoiceMatched) {
                                "প্রোফাইল: ${voiceState.activeVoiceName ?: "Bangla Female"} ✓ (${guardState.femaleVoiceTone.labelBn})"
                            } else {
                                "প্রোফাইল: Noor Soft Bangla Pitch (${guardState.femaleVoiceTone.labelBn})"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = NoorMintAccent
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(FemaleVoiceTone.entries) { tone ->
                                FilterChip(
                                    selected = guardState.femaleVoiceTone == tone,
                                    onClick = { onSelectFemaleTone(tone) },
                                    label = { Text(tone.labelBn, fontWeight = FontWeight.SemiBold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NoorGold,
                                        selectedLabelColor = NoorDeepestGreen,
                                        containerColor = NoorDeepestGreen,
                                        labelColor = NoorIvory
                                    )
                                )
                            }
                        }
                        GuardToggleRow(
                            icon = Icons.Default.RecordVoiceOver,
                            title = "Female Voice AI রিমাইন্ডার",
                            subtitle = "মিষ্টি Female কণ্ঠে নামাজ, পানি নেওয়া, ব্যায়াম ও ঘুমের ডাক",
                            checked = guardState.femaleVoiceEnabled,
                            onCheckedChange = onToggleFemaleVoice
                        )
                        GuardToggleRow(
                            icon = Icons.Default.Bedtime,
                            title = "ঘুমের দোয়া রাতে Auto প্লে",
                            subtitle = "রাত ১০:৪৫ ও ১১টায় ঘুমের দোয়া + ভয়েস রিমাইন্ডার",
                            checked = guardState.nightSleepDuaAutoEnabled,
                            onCheckedChange = onToggleSleepDuaAuto
                        )
                        Button(
                            onClick = onSpeakDailyReport,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NoorGold,
                                contentColor = NoorDeepestGreen
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("আজকের Daily Report (Female Voice) শুনুন", fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = "ট্যাপ করে ১৫টি Female Voice রিমাইন্ডার শুনুন:",
                            style = MaterialTheme.typography.labelLarge,
                            color = NoorGold
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(IbadahGuideCatalog.all15FemaleReminders) { line ->
                                AssistChip(
                                    onClick = { onSpeakFemaleReminder(line) },
                                    label = { Text(line, color = NoorIvory, maxLines = 1) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = NoorDeepestGreen),
                                    border = BorderStroke(1.dp, NoorBorderGreen)
                                )
                            }
                        }
                    }
                }
            }

            // Extra Premium: Family Mode (Boss / Ma / Bon) + Period / Pak Tracker (Meyeder Jonno)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NoorCardGreen),
                    border = BorderStroke(1.dp, NoorBorderGreen)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FamilyRestroom, contentDescription = null, tint = NoorGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ফ্যামিলি মোড (Boss / মা / বোন)",
                                style = MaterialTheme.typography.titleMedium,
                                color = NoorGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FamilyRole.entries.forEach { role ->
                                FilterChip(
                                    selected = guardState.familyRole == role,
                                    onClick = { onSelectFamilyRole(role) },
                                    label = { Text(role.displayBn) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NoorGold,
                                        selectedLabelColor = NoorDeepestGreen
                                    )
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Spa, contentDescription = null, tint = NoorMintAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Period / পাক-পবিত্রতা ট্র্যাকার (মেয়েদের জন্য)",
                                style = MaterialTheme.typography.titleMedium,
                                color = NoorMintAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        GuardToggleRow(
                            icon = Icons.Default.Spa,
                            title = if (guardState.isPeriodMode) "বর্তমান অবস্থা: পিরিয়ড (নামাজ মাফ)" else "বর্তমান অবস্থা: পাক-পবিত্র (নামাজ ফরজ)",
                            subtitle = if (guardState.isPeriodMode) {
                                "দিন ${guardState.periodDayNumber.coerceAtLeast(1)} — এই সময় কুরআন স্পর্শ ও নামাজ মাফ; দোয়া-ইস্তিগফার পড়ুন।"
                            } else {
                                "পিরিয়ড শুরু হলে অন করুন, শেষ হলে গোসল (ফরজ গোসল) করে অফ করুন।"
                            },
                            checked = guardState.isPeriodMode,
                            onCheckedChange = { enabled ->
                                onUpdatePeriodTracker(enabled, if (enabled) guardState.periodDayNumber.coerceAtLeast(1) else 0)
                            }
                        )
                        if (guardState.isPeriodMode) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("দিন: ${guardState.periodDayNumber}", color = NoorIvory, fontWeight = FontWeight.Bold)
                                OutlinedButton(
                                    onClick = { onUpdatePeriodTracker(true, (guardState.periodDayNumber + 1).coerceAtMost(15)) }
                                ) {
                                    Text("+১ দিন", color = NoorGold)
                                }
                                OutlinedButton(
                                    onClick = {
                                        onUpdatePeriodTracker(false, 0)
                                        onSpeakFemaleReminder("Alhamdulillah! পিরিয়ড শেষ — ফরজ গোসল করে পাক হয়ে নিন।")
                                    }
                                ) {
                                    Text("পাক হয়েছি (গোসল)", color = NoorMintAccent)
                                }
                            }
                        }
                    }
                }
            }

            // Phone Usage + Daily Limit
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = NoorGold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "দৈনিক ফোন ব্যবহার ও লিমিট",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NoorGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            OutlinedButton(onClick = onRefreshUsage) {
                                Text("Refresh", color = NoorGold)
                            }
                        }
                        Text(
                            text = if (usageReport.hasPermission) usageReport.formattedSummary() else "Usage Access চালু করলে সঠিক সময় দেখাবে।",
                            style = MaterialTheme.typography.titleMedium,
                            color = NoorIvory
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "দৈনিক লিমিট: ${guardState.dailyPhoneLimitMinutes} মি.",
                            color = NoorGold
                        )
                        Slider(
                            value = guardState.dailyPhoneLimitMinutes.toFloat(),
                            onValueChange = { onSetDailyLimit(it.toInt()) },
                            valueRange = 30f..300f,
                            steps = 8,
                            colors = SliderDefaults.colors(thumbColor = NoorGold, activeTrackColor = NoorGold)
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        val presets = listOf(
                            "com.facebook.katana" to "Facebook",
                            "com.google.android.youtube" to "YouTube",
                            "com.android.chrome" to "Chrome",
                            "com.instagram.android" to "Instagram"
                        )
                        presets.forEach { (pkg, label) ->
                            val currentMin = guardState.appLimits[pkg] ?: 30
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("$label ($currentMin মি.)", color = NoorIvory)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(20, 30, 45).forEach { m ->
                                        FilterChip(
                                            selected = currentMin == m,
                                            onClick = { onSetAppLimit(pkg, label, m) },
                                            label = { Text("${m}m") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = NoorGold,
                                                selectedLabelColor = NoorDeepestGreen
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Direct Lock Test Card ("Allah Dekhtase Mama, Tham")
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NoorGoldSurface),
                    border = BorderStroke(1.dp, NoorGold)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = NoorGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Direct Lock (Istighfar + 30 Push-Up / Squat / Plank)",
                                style = MaterialTheme.typography.titleMedium,
                                color = NoorGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    onTriggerLockNow(
                                        LockMode.PUSHUP_10,
                                        10,
                                        "Boss Apnar Phone Beshi Choltase, Ektu Rakhen"
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("test_10_pushup_lock"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NoorGold,
                                    contentColor = NoorDeepestGreen
                                )
                            ) {
                                Text("১০ পুশ-আপ লক", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    onTriggerLockNow(
                                        LockMode.PUSHUP_30,
                                        30,
                                        "Allah Dekhtase Mama, Tham"
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("test_30_pushup_lock"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NoorCrimson,
                                    contentColor = NoorIvory
                                )
                            ) {
                                Text("৩০ পুশ-আপ লক", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Owner Face + Google Drive "Noor Security" Backup Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NoorCardGreen),
                    border = BorderStroke(1.5.dp, NoorGold.copy(alpha = 0.7f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = NoorGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Noor Security — Owner Face & Google Drive",
                                style = MaterialTheme.typography.titleMedium,
                                color = NoorGold,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "• Owner-এর ছবি কখনো Google Drive-এ আপলোড হবে না\n• অচেনা কেউ আনলক/গার্ড ভঙ্গ করলে ছবি Drive-এর \"Noor Security\" ফোল্ডারে সেভ হবে",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NoorSageText
                        )

                        GuardToggleRow(
                            icon = Icons.Default.Face,
                            title = "Intruder Security Alert Upload",
                            subtitle = if (guardState.ownerFaceEnrolled) {
                                "Owner Face Enrolled ✓ • Owner Photo Never Uploaded"
                            } else {
                                "Owner Face সেট করুন (তাহলে শুধু অচেনা মানুষের ছবি আপলোড হবে)"
                            },
                            checked = guardState.driveSecurityEnabled,
                            onCheckedChange = onToggleDriveSecurity
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showOwnerEnrollDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("enroll_owner_face_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NoorGold,
                                    contentColor = NoorDeepestGreen
                                )
                            ) {
                                Text(
                                    text = if (guardState.ownerFaceEnrolled) "Update Owner Face ✓" else "Enroll Owner Face",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        val auth = GoogleDriveSecurityUploader.requestDriveAuthorization(context)
                                        if (auth != null) {
                                            if (auth.hasResolution() && auth.pendingIntent != null) {
                                                driveAuthLauncher.launch(
                                                    IntentSenderRequest.Builder(auth.pendingIntent!!.intentSender).build()
                                                )
                                            } else if (!auth.accessToken.isNullOrBlank()) {
                                                onDriveAuthorized(auth.accessToken!!)
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("connect_drive_security_button"),
                                border = BorderStroke(
                                    1.dp,
                                    if (guardState.driveFolderId.isNotBlank()) NoorMintAccent else NoorGold
                                )
                            ) {
                                Text(
                                    text = if (guardState.driveFolderId.isNotBlank()) {
                                        "Drive Folder ✓"
                                    } else {
                                        "Connect Drive"
                                    },
                                    color = if (guardState.driveFolderId.isNotBlank()) NoorMintAccent else NoorGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (guardState.ownerFaceEnrolled) {
                            TextButton(
                                onClick = onClearOwnerFace,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Owner Face রিমুভ করুন", color = NoorSageText)
                            }
                        }

                        if (guardState.lastSecurityEvent.isNotBlank()) {
                            Text(
                                text = "সর্বশেষ স্ট্যাটাস: ${guardState.lastSecurityEvent}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = NoorMintAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuardToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = NoorGold)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = NoorIvory,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NoorSageText
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NoorDeepestGreen,
                checkedTrackColor = NoorGold
            )
        )
    }
}

private fun isAccessibilityEnabled(context: Context): Boolean {
    val expected = ComponentName(context, NoorGuardAccessibilityService::class.java).flattenToString()
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
}

private fun isDeviceAdminActive(context: Context): Boolean {
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val comp = ComponentName(context, NoorDeviceAdminReceiver::class.java)
    return dpm.isAdminActive(comp)
}

private fun isDndAccessGranted(context: Context): Boolean {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    return nm.isNotificationPolicyAccessGranted
}
