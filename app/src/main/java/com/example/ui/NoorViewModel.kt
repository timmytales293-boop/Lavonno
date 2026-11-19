package com.example.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AzanAudioPlayer
import com.example.audio.FemaleVoiceTone
import com.example.audio.NoorVoiceEngine
import com.example.audio.QuranAudioPlayer
import com.example.audio.QuranPlaybackState
import com.example.audio.VoiceEngineState
import com.example.data.content.IbadahGuideCatalog
import com.example.data.content.QuranCatalog
import com.example.data.local.NoorCacheDatabase
import com.example.data.model.AiMessage
import com.example.data.model.AppLimit
import com.example.data.model.AyahItem
import com.example.data.model.DuaItem
import com.example.data.model.FamilyRole
import com.example.data.model.HadithItem
import com.example.data.model.LockMode
import com.example.data.model.NamazLog
import com.example.data.model.PrayerScheduleItem
import com.example.data.model.Reminder
import com.example.data.model.SurahMeta
import com.example.data.model.UserProfile
import com.example.data.model.Wakto
import com.example.data.network.QuranApiService
import com.example.data.repository.NoorRepository
import com.example.data.repository.OfflineContentRepository
import com.example.domain.prayer.PrayerTimeCalculator
import com.example.guard.DailyUsageReport
import com.example.guard.GuardPreferences
import com.example.guard.LocalGuardState
import com.example.guard.NoorAlarmScheduler
import com.example.guard.UsageStatsHelper
import com.example.security.GoogleDriveSecurityUploader
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.Calendar
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class PrayerCountdownUi(
    val cityName: String = "Mymensingh",
    val nextPrayerName: String = "Asor",
    val countdownText: String = "00:00:00",
    val schedule: List<PrayerScheduleItem> = emptyList(),
    val passedPrayers: Set<Wakto> = emptySet(),
    val isFriday: Boolean = false
)

data class SurahReaderState(
    val surahNumber: Int = 1,
    val isLoading: Boolean = false,
    val ayahs: List<AyahItem> = emptyList(),
    val error: String? = null
)

class NoorViewModel(
    private val repository: NoorRepository,
    private val offlineRepository: OfflineContentRepository,
    private val guardPrefs: GuardPreferences,
    private val quranAudioPlayer: QuranAudioPlayer,
    private val voiceEngine: NoorVoiceEngine,
    private val appContext: Context,
    private val currentUserId: String,
    private val userDisplayName: String?
) : ViewModel() {

    private val noorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    You are "Noor AI", a gentle, warm female-voiced personal Islamic & safety assistant for a Bengali family.
                    STRICT RULES:
                    1. Always address the user warmly as "Boss" (or "Amma/Ma" / "Apu/Bon" if requested).
                    2. Respond in clear, simple, natural Bangla. Keep replies concise and easy to listen to aloud.
                    3. Answer questions accurately on Namaz, Oju, Pani Newa (Istinja), Dua, and practical Self Defence.
                    4. If the user feels sad ("Mon kharap"), offer heartfelt comfort, hope in Allah, and a short authentic Dua.
                    5. Base every Islamic ruling strictly on the Holy Quran and authentic Sahih Hadith (Bukhari, Muslim, Tirmizi, Abu Dawud). NEVER fabricate verses or hadiths.
                    6. Always include an explicit reference line at the end:
                       Reference: Sura <Name> - Ayat <No>  OR  Reference: <Book> - Hadis <No>
                    """.trimIndent()
                )
            }
        )
    }

    val guardState: StateFlow<LocalGuardState> = guardPrefs.state
    val audioState: StateFlow<QuranPlaybackState> = quranAudioPlayer.state
    val voiceState: StateFlow<VoiceEngineState> = voiceEngine.state

    private val _liveVoiceReplyEnabled = MutableStateFlow(true)
    val liveVoiceReplyEnabled: StateFlow<Boolean> = _liveVoiceReplyEnabled.asStateFlow()

    val profileState: StateFlow<UiState<UserProfile>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile>> { profile ->
            if (profile != null) {
                syncGuardFromProfile(profile)
                UiState.Success(profile)
            } else {
                UiState.Loading
            }
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing profile", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val namazLogsState: StateFlow<List<NamazLog>> = repository.observeNamazLogs(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing namaz logs", error)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val aiMessagesState: StateFlow<List<AiMessage>> = repository.observeAiMessages(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing AI messages", error)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val remindersState: StateFlow<List<Reminder>> = repository.observeReminders(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing reminders", error)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val appLimitsState: StateFlow<List<AppLimit>> = repository.observeAppLimits(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing app limits", error)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val _countdownUi = MutableStateFlow(PrayerCountdownUi())
    val countdownUi: StateFlow<PrayerCountdownUi> = _countdownUi.asStateFlow()

    private val _usageReport = MutableStateFlow(UsageStatsHelper.getTodayUsageReport(appContext))
    val usageReport: StateFlow<DailyUsageReport> = _usageReport.asStateFlow()

    private val _surahReader = MutableStateFlow(SurahReaderState())
    val surahReader: StateFlow<SurahReaderState> = _surahReader.asStateFlow()

    val cachedSurahsState: StateFlow<List<SurahMeta>> = offlineRepository.surahs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = QuranCatalog.all114Surahs
        )

    val cachedHadithsState: StateFlow<List<HadithItem>> = offlineRepository.hadiths
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = com.example.data.content.HadithCatalog.allHadiths
        )

    val cachedDuasState: StateFlow<List<DuaItem>> = offlineRepository.duas
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = com.example.data.content.DuaCatalog.allDuas
        )

    private val _streamingAiText = MutableStateFlow<String?>(null)
    val streamingAiText: StateFlow<String?> = _streamingAiText.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    init {
        voiceEngine.setVoiceTone(guardPrefs.getCurrentState().femaleVoiceTone)
        viewModelScope.launch {
            runCatching { offlineRepository.seedInitialCacheIfNeeded() }
                .onFailure { err -> Log.e(TAG, "Failed to seed Room offline cache", err) }
        }
        viewModelScope.launch {
            runCatching {
                repository.ensureUserProfile(userDisplayName)
            }.onFailure { err ->
                Log.e(TAG, "Failed to ensure user profile", err)
            }
        }
        startCountdownTicker()
    }

    private fun syncGuardFromProfile(profile: UserProfile) {
        guardPrefs.setPornBlock(profile.pornBlockEnabled)
        guardPrefs.setGamblingBlock(profile.gamblingBlockEnabled)
        guardPrefs.setUninstallLock(profile.uninstallLockEnabled)
        guardPrefs.setNightLock(profile.nightLockEnabled)
        guardPrefs.setAzanEnabled(profile.azanEnabled)
        guardPrefs.setPrePrayerEnabled(profile.prePrayerAlertEnabled)
        guardPrefs.setFemaleVoiceEnabled(profile.femaleVoiceEnabled)
        guardPrefs.setNamazAutoSilentEnabled(profile.namazAutoSilentEnabled)
        guardPrefs.setDailyPhoneLimitMinutes(profile.dailyPhoneLimitMinutes.toInt())
        guardPrefs.setFfDailyLimitMinutes(profile.ffDailyLimitMinutes.toInt())
        guardPrefs.setFfGuardEnabled(profile.ffGuardEnabled)
        guardPrefs.setFocusModeUntil(profile.focusModeUntilEpochMs)
        guardPrefs.setSosPhoneNumber(profile.sosPhoneNumber)
        guardPrefs.setDefensePlanDayBits(profile.defensePlanDayBits.toInt())
        guardPrefs.setFamilyRole(FamilyRole.fromCode(profile.familyRole))
        guardPrefs.setPeriodState(profile.isPeriodMode, profile.periodDayNumber.toInt())
        guardPrefs.saveLocationCoordinates(profile.latitude, profile.longitude)
    }

    private fun startCountdownTicker() {
        viewModelScope.launch {
            var lastScheduledDay = -1
            while (true) {
                val now = System.currentTimeMillis()
                val profile = (profileState.value as? UiState.Success)?.data
                val lat = profile?.latitude ?: 24.7471
                val lon = profile?.longitude ?: 90.4203
                val city = profile?.cityName ?: "Mymensingh"

                val schedule = PrayerTimeCalculator.calculateDailySchedule(lat, lon, dateMillis = now)
                val next = PrayerTimeCalculator.nextPrayer(lat, lon, now)
                val countdown = PrayerTimeCalculator.formatCountdown(next.timeEpochMs, now)
                val passed = PrayerTimeCalculator.passedPrayersToday(lat, lon, now)

                _countdownUi.value = PrayerCountdownUi(
                    cityName = city,
                    nextPrayerName = next.wakto.displayEn,
                    countdownText = countdown,
                    schedule = schedule,
                    passedPrayers = passed,
                    isFriday = PrayerTimeCalculator.isFriday(now)
                )

                val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                if (dayOfYear != lastScheduledDay) {
                    lastScheduledDay = dayOfYear
                    val guard = guardPrefs.getCurrentState()
                    NoorAlarmScheduler.schedulePrayerAlerts(
                        context = appContext,
                        schedule = schedule,
                        azanEnabled = guard.azanEnabled,
                        prePrayerEnabled = guard.prePrayerAlertEnabled
                    )
                    NoorAlarmScheduler.schedulePremiumDailyReminders(
                        context = appContext,
                        sleepDuaAuto = guard.nightSleepDuaAutoEnabled
                    )
                }

                // Refresh phone + FF usage every ~20 seconds and enforce limits
                if ((now / 1000L) % 20L == 0L) {
                    refreshPhoneUsageAndEnforceAgentRule()
                }
                delay(1000L)
            }
        }
    }

    fun refreshPhoneUsageAndEnforceAgentRule() {
        val report = UsageStatsHelper.getTodayUsageReport(appContext)
        _usageReport.value = report
        if (!report.hasPermission) return

        val guard = guardPrefs.getCurrentState()
        if (guard.activeLockMode != LockMode.NONE) return

        val ffExceeded = guard.ffGuardEnabled && report.ffMinutesToday >= guard.ffDailyLimitMinutes
        if (ffExceeded) {
            val msg = "Boss Apnar FF Time Ses, Ebar Quran Poren"
            triggerPushUpLock(LockMode.PUSHUP_10, 10, msg)
            if (guard.femaleVoiceEnabled) {
                voiceEngine.speakFemaleNoor("Boss Apnar FF Time Ses")
            }
            return
        }

        val totalExceeded = report.totalMinutes >= guard.dailyPhoneLimitMinutes
        val restrictedExceeded = report.topApps.any { stat ->
            val lim = guard.appLimits[stat.packageName]
            lim != null && stat.minutesToday >= lim
        }
        if (totalExceeded || restrictedExceeded) {
            val msg = "Boss Apnar Phone Beshi Choltase, Ektu Rakhen"
            triggerPushUpLock(LockMode.PUSHUP_10, 10, msg)
            if (guard.femaleVoiceEnabled) {
                voiceEngine.speakFemaleNoor(msg)
            }
        }
    }

    fun triggerPushUpLock(mode: LockMode, requiredPushUps: Int, triggerReason: String = "Allah Dekhtase Mama, Tham") {
        guardPrefs.setDirectPushUpLock(mode, requiredPushUps, triggerReason)
        viewModelScope.launch {
            runCatching {
                repository.setDirectLockState(mode, requiredPushUps)
                repository.updateProfileFields(mapOf("lockTriggerReason" to triggerReason.take(200)))
            }
        }
    }

    fun completePushUpUnlock(completedMode: LockMode) {
        val message = if (completedMode == LockMode.PUSHUP_30) {
            "Boss Apni Ajke Onnek Valo Korsen, MashAllah — Ekhon Pani Khan"
        } else {
            "Boss Apnar Time Ses Chhilo, MashAllah — Ebar 1 Page Quran Poren"
        }
        guardPrefs.clearLockWithBanner(message)
        if (guardPrefs.getCurrentState().femaleVoiceEnabled) {
            voiceEngine.speakFemaleNoor(message)
        }
        viewModelScope.launch {
            runCatching {
                repository.setDirectLockState(LockMode.NONE, 0)
                repository.updateProfileFields(mapOf("lockTriggerReason" to ""))
                repository.addAiMessage(
                    sender = "NOOR",
                    text = message,
                    reference = if (completedMode == LockMode.PUSHUP_30) {
                        "Sura Al-Isra - Ayat 32"
                    } else {
                        "Bukhari - Hadis 6412"
                    }
                )
            }
        }
    }

    fun dismissUnlockBanner() = guardPrefs.clearBanner()

    fun toggleNamaz(wakto: Wakto, todayLog: NamazLog?) {
        val dateKey = PrayerTimeCalculator.todayDateKey()
        viewModelScope.launch {
            runCatching { repository.toggleNamaz(dateKey, wakto, todayLog) }
                .onFailure { Log.e(TAG, "Failed to toggle namaz", it) }
        }
    }

    fun updateLocation(cityName: String, lat: Double, lon: Double) {
        guardPrefs.saveLocationCoordinates(lat, lon)
        viewModelScope.launch {
            runCatching {
                repository.updateProfileFields(
                    mapOf("cityName" to cityName, "latitude" to lat, "longitude" to lon)
                )
            }
        }
    }

    fun saveOwnerFaceSignature(signatureJson: String) {
        guardPrefs.saveOwnerFaceSignature(signatureJson)
    }

    fun clearOwnerFaceSignature() {
        guardPrefs.clearOwnerFaceSignature()
    }

    fun toggleDriveSecurity(enabled: Boolean) {
        guardPrefs.setDriveSecurityEnabled(enabled)
    }

    fun verifyAndCreateDriveSecurityFolder(accessToken: String) {
        viewModelScope.launch {
            runCatching {
                val uploader = GoogleDriveSecurityUploader()
                val cachedId = guardPrefs.getCurrentState().driveFolderId.ifBlank { null }
                val folderId = uploader.findOrCreateNoorSecurityFolder(accessToken, cachedId)
                guardPrefs.saveDriveFolderId(folderId)
                guardPrefs.recordSecurityEvent("Google Drive / Noor Security Ready ✓")
            }.onFailure { err ->
                Log.w(TAG, "Failed to verify Noor Security Drive folder", err)
                guardPrefs.recordSecurityEvent("Drive Folder Error: ${err.message ?: "Check network"}")
            }
        }
    }

    fun toggleAzan(enabled: Boolean) {
        guardPrefs.setAzanEnabled(enabled)
        viewModelScope.launch {
            runCatching { repository.updateProfileFields(mapOf("azanEnabled" to enabled)) }
        }
    }

    fun togglePornBlock(enabled: Boolean) {
        guardPrefs.setPornBlock(enabled)
        viewModelScope.launch {
            runCatching { repository.updateProfileFields(mapOf("pornBlockEnabled" to enabled)) }
        }
    }

    fun toggleGamblingBlock(enabled: Boolean) {
        guardPrefs.setGamblingBlock(enabled)
        viewModelScope.launch {
            runCatching { repository.updateProfileFields(mapOf("gamblingBlockEnabled" to enabled)) }
        }
    }

    fun toggleUninstallLock(enabled: Boolean) {
        guardPrefs.setUninstallLock(enabled)
        viewModelScope.launch {
            runCatching { repository.updateProfileFields(mapOf("uninstallLockEnabled" to enabled)) }
        }
    }

    fun toggleNightLock(enabled: Boolean) {
        guardPrefs.setNightLock(enabled)
        viewModelScope.launch {
            runCatching { repository.updateProfileFields(mapOf("nightLockEnabled" to enabled)) }
        }
    }

    fun toggleNamazAutoSilent(enabled: Boolean) {
        guardPrefs.setNamazAutoSilentEnabled(enabled)
        viewModelScope.launch {
            runCatching { repository.updateProfileFields(mapOf("namazAutoSilentEnabled" to enabled)) }
        }
    }

    fun toggleFemaleVoice(enabled: Boolean) {
        guardPrefs.setFemaleVoiceEnabled(enabled)
        if (!enabled) voiceEngine.stop()
        viewModelScope.launch {
            runCatching { repository.updateProfileFields(mapOf("femaleVoiceEnabled" to enabled)) }
        }
    }

    fun selectFemaleVoiceTone(tone: FemaleVoiceTone) {
        guardPrefs.setFemaleVoiceTone(tone)
        voiceEngine.setVoiceTone(tone)
        voiceEngine.speakFemaleNoor(
            "Assalamualaikum Boss, Ajker Din Ta Valo Hok",
            itemId = "tone_preview_${tone.code}"
        )
    }

    fun toggleSleepDuaAuto(enabled: Boolean) {
        guardPrefs.setNightSleepDuaAuto(enabled)
        NoorAlarmScheduler.schedulePremiumDailyReminders(appContext, enabled)
    }

    fun setDailyPhoneLimit(minutes: Int) {
        guardPrefs.setDailyPhoneLimitMinutes(minutes)
        viewModelScope.launch {
            runCatching { repository.updateProfileFields(mapOf("dailyPhoneLimitMinutes" to minutes.toLong())) }
        }
    }

    fun setFfDailyLimit(minutes: Int) {
        guardPrefs.setFfDailyLimitMinutes(minutes)
        viewModelScope.launch {
            runCatching { repository.updateProfileFields(mapOf("ffDailyLimitMinutes" to minutes.toLong())) }
        }
    }

    fun toggleFfGuard(enabled: Boolean) {
        guardPrefs.setFfGuardEnabled(enabled)
        viewModelScope.launch {
            runCatching { repository.updateProfileFields(mapOf("ffGuardEnabled" to enabled)) }
        }
    }

    fun selectFamilyRole(role: FamilyRole) {
        guardPrefs.setFamilyRole(role)
        viewModelScope.launch {
            runCatching { repository.updateProfileFields(mapOf("familyRole" to role.code)) }
        }
    }

    fun updatePeriodTracker(isPeriod: Boolean, dayNumber: Int) {
        guardPrefs.setPeriodState(isPeriod, dayNumber)
        viewModelScope.launch {
            runCatching {
                repository.updateProfileFields(
                    mapOf("isPeriodMode" to isPeriod, "periodDayNumber" to dayNumber.toLong())
                )
            }
        }
    }

    fun saveSosPhoneNumber(phone: String) {
        guardPrefs.setSosPhoneNumber(phone)
        viewModelScope.launch {
            runCatching { repository.updateProfileFields(mapOf("sosPhoneNumber" to phone.take(30))) }
        }
    }

    fun toggleDefensePlanDay(dayIndex: Int) {
        val current = guardPrefs.getCurrentState().defensePlanDayBits
        val updated = (current xor (1 shl dayIndex)) and 0x7F
        guardPrefs.setDefensePlanDayBits(updated)
        viewModelScope.launch {
            runCatching { repository.updateProfileFields(mapOf("defensePlanDayBits" to updated.toLong())) }
        }
    }

    fun toggleOjuBit(idx: Int) = guardPrefs.toggleOjuChecklistBit(idx)
    fun togglePakBit(idx: Int) = guardPrefs.togglePakChecklistBit(idx)

    fun activateFocusModeOneHour() {
        val until = System.currentTimeMillis() + 60L * 60L * 1000L
        guardPrefs.setFocusModeUntil(until)
        viewModelScope.launch {
            runCatching { repository.updateProfileFields(mapOf("focusModeUntilEpochMs" to until)) }
        }
    }

    fun clearFocusMode() {
        guardPrefs.setFocusModeUntil(0L)
        viewModelScope.launch {
            runCatching { repository.updateProfileFields(mapOf("focusModeUntilEpochMs" to 0L)) }
        }
    }

    fun setAppDailyLimit(packageName: String, appName: String, minutes: Int) {
        guardPrefs.setAppLimit(packageName, minutes)
        viewModelScope.launch {
            runCatching { repository.upsertAppLimit(packageName, appName, minutes, enabled = true) }
        }
    }

    fun testAzanSound() {
        AzanAudioPlayer.playAzanChime()
    }

    fun speakFemaleText(text: String) {
        voiceEngine.speakFemaleNoor(text)
    }

    fun toggleSpeakAudio(itemId: String, text: String) {
        voiceEngine.toggleSpeakDuaOrSurah(itemId, text)
    }

    fun stopVoice() = voiceEngine.stop()

    fun setLiveVoiceReply(enabled: Boolean) {
        _liveVoiceReplyEnabled.value = enabled
        if (!enabled) voiceEngine.stop()
    }

    fun speakDailyReport(todayLog: NamazLog?, logs: List<NamazLog>) {
        val prayedToday = todayLog?.prayedCount() ?: 0
        val weekly = computeWeeklyPrayedCount(logs)
        val usage = _usageReport.value
        val guard = guardPrefs.getCurrentState()
        val defenseDone = Integer.bitCount(guard.defensePlanDayBits)
        val roleWord = guard.familyRole.greetingPrefix

        val report = buildString {
            append("Assalamualaikum $roleWord! আজকের Noor ডেইলি রিপোর্ট বলছি। ")
            if (guard.isPeriodMode) {
                append("পিরিয়ড মোড চালু আছে, তাই নামাজ মাফ — আপনি দোয়া ও জিকির চালু রাখুন। ")
            } else {
                append("আপনি আজ $prayedToday ওয়াক্ত নামাজ পড়েছেন, আর এই সপ্তাহে মোট $weekly ওয়াক্ত পূর্ণ হয়েছে। ")
            }
            if (usage.hasPermission) {
                append("আজ ফোন ব্যবহার হয়েছে ${usage.totalMinutes} মিনিট এবং ফ্রি ফায়ার ${usage.ffMinutesToday} মিনিট। ")
            }
            append("সেলফ ডিফেন্স ৭ দিনের প্ল্যানে আপনার $defenseDone দিন সম্পন্ন হয়েছে। ")
            append("Boss Apni Parben, Allah Apnar Loge Ase! আজ অন্তত ১ পেজ কুরআন পড়ে নিন।")
        }
        voiceEngine.speakFemaleNoor(report, itemId = "daily_report")
    }

    fun openSurah(surahNumber: Int) {
        _surahReader.value = SurahReaderState(surahNumber = surahNumber, isLoading = true)
        viewModelScope.launch {
            try {
                val ayahs = withTimeout(25_000L) {
                    offlineRepository.getOrFetchSurahAyahs(surahNumber)
                }
                if (ayahs.isNotEmpty()) {
                    _surahReader.value = SurahReaderState(surahNumber = surahNumber, ayahs = ayahs)
                } else {
                    _surahReader.value = SurahReaderState(
                        surahNumber = surahNumber,
                        error = "সূরা লোড করা যায়নি।"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load/cache surah $surahNumber", e)
                _surahReader.value = SurahReaderState(
                    surahNumber = surahNumber,
                    error = "অফলাইন মোড: একবার ইন্টারনেটে খুললে সূরাটি স্থায়ীভাবে সেভ হয়ে যাবে।"
                )
            }
        }
    }

    fun toggleSurahAudio(surahNumber: Int) {
        quranAudioPlayer.togglePlay(surahNumber, QuranApiService.getAudioStreamUrl(surahNumber))
    }

    fun bookmarkSurahAyah(surahNumber: Int, ayahNumber: Int, surahName: String) {
        viewModelScope.launch {
            runCatching { repository.saveQuranBookmark(surahNumber, ayahNumber, surahName) }
        }
    }

    fun sendAiMessage(userPrompt: String, speakReply: Boolean = _liveVoiceReplyEnabled.value) {
        val trimmed = userPrompt.trim()
        if (trimmed.isBlank()) return
        _aiError.value = null

        viewModelScope.launch {
            runCatching { repository.addAiMessage(sender = "USER", text = trimmed) }

            val parsedAlarm = parseBossAlarmRequest(trimmed)
            if (parsedAlarm != null) {
                val (hour24, minute, label) = parsedAlarm
                runCatching {
                    val remId = repository.addReminder(label, hour24, minute, repeatDaily = false)
                    NoorAlarmScheduler.scheduleBossAlarm(
                        context = appContext,
                        reminderIdHash = remId.hashCode(),
                        title = label,
                        hour = hour24,
                        minute = minute
                    )
                }
            }

            val recentHistory = aiMessagesState.value.takeLast(8).map { msg ->
                val role = if (msg.sender == "USER") "user" else "model"
                content(role) { text(msg.text) }
            }

            _streamingAiText.value = ""
            val sb = StringBuilder()
            try {
                withTimeout(60_000L) {
                    val chat = noorModel.startChat(history = recentHistory)
                    val roleHint = guardPrefs.getCurrentState().familyRole.greetingPrefix
                    val extraNote = buildString {
                        append("\n[System: Address user warmly as '$roleHint'. Answer concisely so it sounds gentle in Bangla voice.]")
                        if (parsedAlarm != null) {
                            val (h, m, _) = parsedAlarm
                            val formatted = String.format(Locale.US, "%02d:%02d", h, m)
                            append(" [Alarm scheduled at $formatted — confirm warmly.]")
                        }
                    }
                    chat.sendMessageStream(trimmed + extraNote).collect { chunk ->
                        val part = chunk.text.orEmpty()
                        sb.append(part)
                        _streamingAiText.value = sb.toString()
                    }
                }
                val finalAnswer = sb.toString().trim()
                _streamingAiText.value = null
                if (finalAnswer.isNotBlank()) {
                    val refLine = finalAnswer.lines()
                        .lastOrNull { it.contains("Reference:", ignoreCase = true) }
                        ?.substringAfter(":")?.trim()
                    runCatching {
                        repository.addAiMessage(sender = "NOOR", text = finalAnswer, reference = refLine)
                    }
                    if (speakReply) {
                        voiceEngine.speakFemaleNoor(finalAnswer, itemId = "ai_reply")
                    }
                } else {
                    _aiError.value = "Noor AI উত্তর দিতে পারেনি। আবার চেষ্টা করুন।"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _streamingAiText.value = null
                Log.w(TAG, "Online Noor AI unavailable, using Offline Islamic fallback", e)
                val offlineReply = buildOfflineFallbackReply(trimmed)
                runCatching {
                    repository.addAiMessage(
                        sender = "NOOR",
                        text = offlineReply.first,
                        reference = offlineReply.second
                    )
                }
                if (speakReply) {
                    voiceEngine.speakFemaleNoor(offlineReply.first, itemId = "ai_offline_reply")
                }
            }
        }
    }

    private fun buildOfflineFallbackReply(prompt: String): Pair<String, String> {
        val p = prompt.lowercase()
        return when {
            p.contains("mon") || p.contains("মন") || p.contains("sad") || p.contains("কষ্ট") || p.contains("santona") -> Pair(
                "Boss Apnar Mon Kharap? Asen Ekta Dua Pori। হতাশ হবেন না — নিশ্চয়ই কষ্টের সাথেই স্বস্তি আছে। পড়ুন: 'হাসবুনাল্লাহু ওয়া নি'মাল ওয়াকীল' (আল্লাহই আমাদের জন্য যথেষ্ট)। Boss Apni Parben, Allah Apnar Loge Ase!",
                "Sura Al-Inshirah - Ayat 5-6"
            )
            p.contains("oju") || p.contains("অজু") -> Pair(
                "Ji Boss! অজুর ৪টি ফরজ: ১) পুরো মুখমণ্ডল ধোয়া, ২) কনুইসহ দুই হাত ধোয়া, ৩) মাথার এক-চতুর্থাংশ মাসেহ করা, ৪) টাখনুসহ দুই পা ধোয়া। বিস্তারিত 'অজু ও ইস্তিনজা' পেজে পাবেন।",
                "Sura Al-Ma'idah - Ayat 6"
            )
            p.contains("pani") || p.contains("istinja") || p.contains("পানি") || p.contains("টয়লেট") -> Pair(
                "Ji Boss! টয়লেটে ঢোকার আগে 'আল্লাহুম্মা ইন্নী আউযু বিকা মিনাল খুবুসি ওয়াল খাবা-ইস' পড়ে বাম পা দিয়ে ঢুকবেন। প্রস্রাবের ছিটা থেকে বাঁচা খুব জরুরি। বের হয়ে বলুন: 'গুফরানাকা' ও সাবান দিয়ে হাত ধুয়ে নিন।",
                "Bukhari - Hadis 142"
            )
            p.contains("self") || p.contains("defence") || p.contains("রক্ষা") || p.contains("chutano") -> Pair(
                "Ji Boss! বিপদে হাত ধরলে বৃদ্ধাঙ্গুলির দিকে ঘুরিয়ে ঝটকা টানে হাত ছাড়ান, জোর গলায় 'আগুন/বাঁচাও' চিৎকার দিন ও ১০ সেকেন্ড দৌড়ে নিরাপদ স্থানে যান। হোমের 'Self Defence' কার্ডে SOS ও প্র্যাকটিস আছে।",
                "Noor Safety & Defense Guide"
            )
            else -> Pair(
                "Ji Boss (অফলাইন মোড): আল্লাহকে স্মরণ রাখুন — 'ফাযকুরূনী আযকুরকুম' (তোমরা আমাকে স্মরণ করো, আমিও তোমাদের স্মরণ করব)। নামাজ, অজু, দোয়া ও আত্মরক্ষার সব গাইড অফলাইনে চালু আছে!",
                "Sura Al-Baqarah - Ayat 152"
            )
        }
    }

    fun deleteReminder(reminderId: String) {
        viewModelScope.launch { runCatching { repository.deleteReminder(reminderId) } }
    }

    private fun parseBossAlarmRequest(text: String): Triple<Int, Int, String>? {
        val normalized = buildString(text.length) {
            for (ch in text.lowercase()) {
                append(
                    when (ch) {
                        '০' -> '0'; '১' -> '1'; '২' -> '2'; '৩' -> '3'; '৪' -> '4'
                        '৫' -> '5'; '৬' -> '6'; '৭' -> '7'; '৮' -> '8'; '৯' -> '9'
                        else -> ch
                    }
                )
            }
        }
        val isAlarmCue = listOf("uthaiya", "uthaio", "উঠাই", "উঠিয়ে", "alarm", "এলার্ম", "remind", "রিমাইন্ড", "ডাক", "dak")
            .any { normalized.contains(it) }
        if (!isAlarmCue) return null

        val timeRegex = Regex("""(\d{1,2})(?::(\d{1,2}))?\s*(am|pm|tay|টায়|টা)?""")
        val match = timeRegex.find(normalized) ?: return null
        var hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: 0
        if (hour !in 0..23 || minute !in 0..59) return null

        val isEvening = normalized.contains("pm") || normalized.contains("rat") || normalized.contains("রাত") ||
            normalized.contains("bikal") || normalized.contains("সন্ধ্যা")
        val isMorning = normalized.contains("am") || normalized.contains("sokal") || normalized.contains("সকাল") ||
            normalized.contains("ভোর") || normalized.contains("fajr") || normalized.contains("ফজর")

        if (isEvening && hour in 1..11) hour += 12
        if (isMorning && hour == 12) hour = 0

        val label = "Boss, আপনার এলার্ম: ${String.format(Locale.US, "%02d:%02d", hour, minute)} - উঠুন!"
        return Triple(hour, minute, label)
    }

    fun computeWeeklyPrayedCount(logs: List<NamazLog>): Int =
        logs.take(7).sumOf { it.prayedCount() }

    fun computeMissedPrayersToday(todayLog: NamazLog?, passed: Set<Wakto>): List<Wakto> {
        if (guardPrefs.getCurrentState().isPeriodMode) return emptyList()
        return passed.filter { wakto ->
            when (wakto) {
                Wakto.FAJR -> todayLog?.fajr != true
                Wakto.DHUHR -> todayLog?.dhuhr != true
                Wakto.ASR -> todayLog?.asr != true
                Wakto.MAGHRIB -> todayLog?.maghrib != true
                Wakto.ISHA -> todayLog?.isha != true
            }
        }
    }

    fun buildProactiveBossReminders(
        todayLog: NamazLog?,
        logs: List<NamazLog>,
        countdown: PrayerCountdownUi,
        usage: DailyUsageReport
    ): List<String> {
        val list = mutableListOf<String>()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val guard = guardPrefs.getCurrentState()

        if (hour in 5..9) {
            list.add(IbadahGuideCatalog.all15FemaleReminders[0]) // Assalamualaikum Boss, Ajker Din Ta Valo Hok
        }
        if (hour in 4..6 && todayLog?.fajr != true && !guard.isPeriodMode) {
            list.add("Boss Apnar Namaz Er Time Hoise, Oju Kore Asen")
        }
        if (hour >= 22) {
            list.add("Boss Apnar Ghumer Time Hoise, Phone Rakhen")
        }
        if (guard.ffGuardEnabled && usage.hasPermission && usage.ffMinutesToday >= guard.ffDailyLimitMinutes) {
            list.add("Boss Apnar FF Time Ses, Ebar Quran Poren")
        }
        if (usage.hasPermission && usage.totalMinutes >= (guard.dailyPhoneLimitMinutes * 0.8f).toInt()) {
            list.add("Boss Apnar Phone Beshi Choltase, Ektu Rakhen")
        }
        if (guard.defensePlanDayBits == 0) {
            list.add("Boss Apnar Self Defence Practice Er Time Hoise, 5 Min Practice Koren")
        }
        if (countdown.isFriday) {
            list.add("Boss Ajke Jumma — Sura Kahf ও 1 Page Quran Poren")
        }
        if (list.isEmpty()) {
            list.add("Boss Apni Parben, Allah Apnar Loge Ase — ${countdown.nextPrayerName} এর প্রস্তুতি নিন")
        }
        return list
    }

    override fun onCleared() {
        super.onCleared()
        quranAudioPlayer.release()
        voiceEngine.release()
    }

    private companion object {
        const val TAG = "NoorViewModel"
    }
}
