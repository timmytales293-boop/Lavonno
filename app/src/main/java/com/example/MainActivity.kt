package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.audio.NoorVoiceEngine
import com.example.audio.QuranAudioPlayer
import com.example.data.firebase.configureAppCheck
import com.example.data.local.NoorCacheDatabase
import com.example.data.model.LockMode
import com.example.data.repository.NoorRepository
import com.example.data.repository.OfflineContentRepository
import com.example.data.sync.ContentSyncScheduler
import com.example.domain.prayer.PrayerTimeCalculator
import com.example.guard.GuardPreferences
import com.example.ui.NoorViewModel
import com.example.ui.UiState
import com.example.ui.ai.NoorAiScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.defence.SelfDefenseScreen
import com.example.ui.dua.DuaScreen
import com.example.ui.hadith.HadithScreen
import com.example.ui.home.HomeScreen
import com.example.ui.home.PhoneGuardScreen
import com.example.ui.ibadah.NamazGuideScreen
import com.example.ui.ibadah.OjuIstinjaScreen
import com.example.ui.lock.PushUpLockScreen
import com.example.ui.quran.QuranScreen
import com.example.ui.theme.NoorCardGreen
import com.example.ui.theme.NoorDarkGreen
import com.example.ui.theme.NoorDeepestGreen
import com.example.ui.theme.NoorGold
import com.example.ui.theme.NoorIvory
import com.example.ui.theme.NoorSageText
import com.example.ui.theme.NoorTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

enum class NoorTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    QURAN("quran", "Quran", Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
    HADITH("hadith", "Hadith", Icons.Filled.AutoStories, Icons.Outlined.AutoStories),
    DUA("dua", "Dua", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    AI("ai", "AI", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome)
}

enum class NoorSubScreen {
    NONE,
    PHONE_GUARD,
    NAMAZ_GUIDE,
    OJU_ISTINJA,
    SELF_DEFENSE
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Mandatory Step 1: Configure App Check with launch intent before UI or Firebase operations
        configureAppCheck(this, intent)

        // Schedule periodic background sync for Quran, Hadith, and Dua Room cache
        ContentSyncScheduler.schedulePeriodicSync(this)

        setContent {
            NoorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NoorDarkGreen
                ) {
                    NoorAppRoot()
                }
            }
        }
    }
}

@Composable
fun NoorAppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            val cm = CredentialManager.create(context)
            attemptAutoSignIn(
                context = context,
                credentialManager = cm,
                onAuthSuccess = { currentUser = Firebase.auth.currentUser },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
    } else {
        key(user.uid) {
            val viewModel: NoorViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val dbId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(dbId)
                        NoorViewModel(
                            repository = NoorRepository(db),
                            offlineRepository = com.example.di.NoorDatabaseModule.provideOfflineContentRepository(app),
                            guardPrefs = GuardPreferences(app),
                            quranAudioPlayer = QuranAudioPlayer(),
                            voiceEngine = NoorVoiceEngine(app),
                            appContext = app,
                            currentUserId = user.uid,
                            userDisplayName = user.displayName
                        )
                    }
                }
            )
            NoorAuthenticatedShell(viewModel = viewModel)
        }
    }
}

@Composable
fun NoorAuthenticatedShell(viewModel: NoorViewModel) {
    val context = LocalContext.current
    val guardPrefs = remember(context) { GuardPreferences(context) }

    val guardState by viewModel.guardState.collectAsStateWithLifecycle()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val countdownUi by viewModel.countdownUi.collectAsStateWithLifecycle()
    val namazLogs by viewModel.namazLogsState.collectAsStateWithLifecycle()
    val aiMessages by viewModel.aiMessagesState.collectAsStateWithLifecycle()
    val reminders by viewModel.remindersState.collectAsStateWithLifecycle()
    val usageReport by viewModel.usageReport.collectAsStateWithLifecycle()
    val readerState by viewModel.surahReader.collectAsStateWithLifecycle()
    val cachedSurahs by viewModel.cachedSurahsState.collectAsStateWithLifecycle()
    val cachedHadiths by viewModel.cachedHadithsState.collectAsStateWithLifecycle()
    val cachedDuas by viewModel.cachedDuasState.collectAsStateWithLifecycle()
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val liveVoiceReply by viewModel.liveVoiceReplyEnabled.collectAsStateWithLifecycle()
    val streamingText by viewModel.streamingAiText.collectAsStateWithLifecycle()
    val aiError by viewModel.aiError.collectAsStateWithLifecycle()

    // First-launch Camera + Sensor access prompt for Push-Up / Squat / Plank Lock
    var showCameraConsentDialog by remember {
        mutableStateOf(
            !guardPrefs.hasAskedCameraOnboarding() &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        guardPrefs.markCameraOnboardingAsked()
        showCameraConsentDialog = false
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notifGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!notifGranted && !showCameraConsentDialog) {
                permissionsLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        }
    }

    // DIRECT LOCK: NO Warning Screen — Istighfar + Camera/Sensor Push-Up / Squat / Plank Lock!
    if (guardState.activeLockMode == LockMode.PUSHUP_10 || guardState.activeLockMode == LockMode.PUSHUP_30) {
        val target = if (guardState.requiredPushUps > 0) {
            guardState.requiredPushUps
        } else if (guardState.activeLockMode == LockMode.PUSHUP_10) 10 else 30

        PushUpLockScreen(
            targetPushUps = target,
            triggerReminder = guardState.lockTriggerReason,
            onSpeakIstighfar = {
                viewModel.speakFemaleText("আস্তাগফিরুল্লাহাল আজীম, আল্লাহ মাফ করুন।")
            },
            onUnlocked = {
                viewModel.completePushUpUnlock(guardState.activeLockMode)
            }
        )
        return
    }

    var currentTab by rememberSaveable { mutableStateOf(NoorTab.HOME) }
    var activeSubScreen by rememberSaveable { mutableStateOf(NoorSubScreen.NONE) }

    val profile = (profileState as? UiState.Success)?.data
    val todayKey = remember { PrayerTimeCalculator.todayDateKey() }
    val todayLog = namazLogs.firstOrNull { it.dateKey == todayKey }
    val weeklyPrayed = viewModel.computeWeeklyPrayedCount(namazLogs)
    val missedToday = viewModel.computeMissedPrayersToday(todayLog, countdownUi.passedPrayers)
    val proactiveReminders = viewModel.buildProactiveBossReminders(
        todayLog = todayLog,
        logs = namazLogs,
        countdown = countdownUi,
        usage = usageReport
    )

    if (showCameraConsentDialog) {
        AlertDialog(
            onDismissRequest = {
                guardPrefs.markCameraOnboardingAsked()
                showCameraConsentDialog = false
            },
            containerColor = NoorCardGreen,
            titleContentColor = NoorGold,
            textContentColor = NoorIvory,
            icon = {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = NoorGold
                )
            },
            title = { Text("Camera + Sensor Access", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Boss, Push-Up / Squat / Plank Gunar Jonno Camera + Sensor Access Lagbe। Allow না দিলে গার্ড লক কাজ করবে না।"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val perms = buildList {
                            add(Manifest.permission.CAMERA)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }.toTypedArray()
                        permissionsLauncher.launch(perms)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NoorGold,
                        contentColor = NoorDeepestGreen
                    ),
                    modifier = Modifier.testTag("camera_consent_allow_button")
                ) {
                    Text("Allow", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        guardPrefs.markCameraOnboardingAsked()
                        showCameraConsentDialog = false
                    }
                ) {
                    Text("Pore", color = NoorSageText)
                }
            }
        )
    }

    BackHandler(enabled = activeSubScreen != NoorSubScreen.NONE) {
        activeSubScreen = NoorSubScreen.NONE
    }

    when (activeSubScreen) {
        NoorSubScreen.NAMAZ_GUIDE -> {
            NamazGuideScreen(
                schedule = countdownUi.schedule,
                todayLog = todayLog,
                weeklyPrayedCount = weeklyPrayed,
                isPeriodMode = guardState.isPeriodMode,
                voiceState = voiceState,
                onToggleNamaz = { wakto, log -> viewModel.toggleNamaz(wakto, log) },
                onToggleSpeak = { id, text -> viewModel.toggleSpeakAudio(id, text) },
                onBack = { activeSubScreen = NoorSubScreen.NONE }
            )
            return
        }
        NoorSubScreen.OJU_ISTINJA -> {
            OjuIstinjaScreen(
                ojuChecklistBits = guardState.ojuChecklistBits,
                pakChecklistBits = guardState.pakChecklistBits,
                voiceState = voiceState,
                onToggleOjuBit = { viewModel.toggleOjuBit(it) },
                onTogglePakBit = { viewModel.togglePakBit(it) },
                onToggleSpeak = { id, text -> viewModel.toggleSpeakAudio(id, text) },
                onBack = { activeSubScreen = NoorSubScreen.NONE }
            )
            return
        }
        NoorSubScreen.SELF_DEFENSE -> {
            SelfDefenseScreen(
                savedSosPhone = guardState.sosPhoneNumber,
                defensePlanDayBits = guardState.defensePlanDayBits,
                defaultLat = profile?.latitude ?: 24.7471,
                defaultLon = profile?.longitude ?: 90.4203,
                onSaveSosPhone = { viewModel.saveSosPhoneNumber(it) },
                onTogglePlanDay = { viewModel.toggleDefensePlanDay(it) },
                onSpeakCue = { viewModel.speakFemaleText(it) },
                onBack = { activeSubScreen = NoorSubScreen.NONE }
            )
            return
        }
        NoorSubScreen.PHONE_GUARD -> {
            PhoneGuardScreen(
                guardState = guardState,
                usageReport = usageReport,
                onBack = { activeSubScreen = NoorSubScreen.NONE },
                onRefreshUsage = { viewModel.refreshPhoneUsageAndEnforceAgentRule() },
                onSetDailyLimit = { viewModel.setDailyPhoneLimit(it) },
                onSetFfLimit = { viewModel.setFfDailyLimit(it) },
                onToggleFfGuard = { viewModel.toggleFfGuard(it) },
                onSetAppLimit = { pkg, name, min -> viewModel.setAppDailyLimit(pkg, name, min) },
                onTogglePornBlock = { viewModel.togglePornBlock(it) },
                onToggleGamblingBlock = { viewModel.toggleGamblingBlock(it) },
                onToggleUninstallLock = { viewModel.toggleUninstallLock(it) },
                onToggleNightLock = { viewModel.toggleNightLock(it) },
                onToggleNamazAutoSilent = { viewModel.toggleNamazAutoSilent(it) },
                onToggleFemaleVoice = { viewModel.toggleFemaleVoice(it) },
                onSelectFemaleTone = { viewModel.selectFemaleVoiceTone(it) },
                onToggleSleepDuaAuto = { viewModel.toggleSleepDuaAuto(it) },
                voiceState = voiceState,
                onSelectFamilyRole = { viewModel.selectFamilyRole(it) },
                onUpdatePeriodTracker = { period, day -> viewModel.updatePeriodTracker(period, day) },
                onSpeakFemaleReminder = { viewModel.speakFemaleText(it) },
                onSpeakDailyReport = { viewModel.speakDailyReport(todayLog, namazLogs) },
                onActivateFocus1h = { viewModel.activateFocusModeOneHour() },
                onClearFocus = { viewModel.clearFocusMode() },
                onTriggerLockNow = { mode, count, reason ->
                    activeSubScreen = NoorSubScreen.NONE
                    viewModel.triggerPushUpLock(mode, count, reason)
                },
                onSaveOwnerFace = { viewModel.saveOwnerFaceSignature(it) },
                onClearOwnerFace = { viewModel.clearOwnerFaceSignature() },
                onToggleDriveSecurity = { viewModel.toggleDriveSecurity(it) },
                onDriveAuthorized = { viewModel.verifyAndCreateDriveSecurityFolder(it) }
            )
            return
        }
        NoorSubScreen.NONE -> Unit
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 700.dp

        Scaffold(
            containerColor = NoorDarkGreen,
            contentWindowInsets = WindowInsets.navigationBars,
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = NoorDeepestGreen,
                        contentColor = NoorGold
                    ) {
                        NoorTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, fontWeight = FontWeight.SemiBold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NoorDeepestGreen,
                                    selectedTextColor = NoorGold,
                                    indicatorColor = NoorGold,
                                    unselectedIconColor = NoorSageText,
                                    unselectedTextColor = NoorSageText
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = NoorDeepestGreen,
                        contentColor = NoorGold,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        NoorTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = NoorDeepestGreen,
                                    selectedTextColor = NoorGold,
                                    indicatorColor = NoorGold,
                                    unselectedIconColor = NoorSageText,
                                    unselectedTextColor = NoorSageText
                                )
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 720.dp)
                            .fillMaxSize()
                    ) {
                        when (currentTab) {
                            NoorTab.HOME -> HomeScreen(
                                countdownUi = countdownUi,
                                profile = profile,
                                namazLogs = namazLogs,
                                guardState = guardState,
                                usageReport = usageReport,
                                proactiveReminders = proactiveReminders,
                                missedPrayersToday = missedToday,
                                weeklyPrayedCount = weeklyPrayed,
                                onToggleNamaz = { wakto, log -> viewModel.toggleNamaz(wakto, log) },
                                onToggleAzan = { viewModel.toggleAzan(it) },
                                onTestAzan = { viewModel.testAzanSound() },
                                onSpeakReminder = { viewModel.speakFemaleText(it) },
                                onSpeakDailyReport = { viewModel.speakDailyReport(todayLog, namazLogs) },
                                onLocationDetected = { city, lat, lon ->
                                    viewModel.updateLocation(city, lat, lon)
                                },
                                onOpenQuran = { currentTab = NoorTab.QURAN },
                                onOpenHadith = { currentTab = NoorTab.HADITH },
                                onOpenAi = { currentTab = NoorTab.AI },
                                onOpenNamazGuide = { activeSubScreen = NoorSubScreen.NAMAZ_GUIDE },
                                onOpenOjuIstinja = { activeSubScreen = NoorSubScreen.OJU_ISTINJA },
                                onOpenSelfDefense = { activeSubScreen = NoorSubScreen.SELF_DEFENSE },
                                onOpenPhoneGuard = { activeSubScreen = NoorSubScreen.PHONE_GUARD },
                                onDismissBanner = { viewModel.dismissUnlockBanner() }
                            )
                            NoorTab.QURAN -> QuranScreen(
                                surahs = cachedSurahs,
                                profile = profile,
                                readerState = readerState,
                                audioState = audioState,
                                onOpenSurah = { viewModel.openSurah(it) },
                                onToggleAudio = { viewModel.toggleSurahAudio(it) },
                                onBookmarkAyah = { s, a, name ->
                                    viewModel.bookmarkSurahAyah(s, a, name)
                                }
                            )
                            NoorTab.HADITH -> HadithScreen(hadiths = cachedHadiths)
                            NoorTab.DUA -> DuaScreen(
                                duas = cachedDuas,
                                voiceState = voiceState,
                                onToggleDuaAudio = { id, text ->
                                    viewModel.toggleSpeakAudio(id, text)
                                }
                            )
                            NoorTab.AI -> NoorAiScreen(
                                messages = aiMessages,
                                reminders = reminders,
                                streamingText = streamingText,
                                errorText = aiError,
                                liveVoiceReplyEnabled = liveVoiceReply,
                                voiceState = voiceState,
                                onToggleLiveVoiceReply = { viewModel.setLiveVoiceReply(it) },
                                onSendMessage = { q, speak -> viewModel.sendAiMessage(q, speak) },
                                onSpeakBubble = { id, text -> viewModel.toggleSpeakAudio(id, text) },
                                onStopVoice = { viewModel.stopVoice() },
                                onDeleteReminder = { viewModel.deleteReminder(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}
