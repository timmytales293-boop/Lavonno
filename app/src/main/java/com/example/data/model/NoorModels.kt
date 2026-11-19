package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

enum class LockMode(val code: String) {
    NONE("NONE"),
    PUSHUP_10("PUSHUP_10"),
    PUSHUP_30("PUSHUP_30"),
    NIGHT("NIGHT"),
    FOCUS("FOCUS");

    companion object {
        fun fromCode(code: String?): LockMode =
            entries.firstOrNull { it.code == code } ?: NONE
    }
}

enum class ExerciseMode(val labelBn: String, val targetCount: Int, val isTimedSeconds: Boolean = false) {
    PUSHUP("30 Push-Up", 30, false),
    SQUAT("30 Squat", 30, false),
    PLANK("1 Min Plank", 60, true)
}

enum class FamilyRole(val code: String, val displayBn: String, val greetingPrefix: String) {
    BOSS("BOSS", "Boss (আমি)", "Boss"),
    MA("MA", "Ma (আম্মু)", "Amma / Ma"),
    BON("BON", "Bon (আপু/বোন)", "Apu / Bon");

    companion object {
        fun fromCode(code: String?): FamilyRole =
            entries.firstOrNull { it.code == code } ?: BOSS
    }
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Boss",
    val cityName: String = "Mymensingh",
    val latitude: Double = 24.7471,
    val longitude: Double = 90.4203,
    val lastReadSurah: Long = 1L,
    val lastReadAyah: Long = 1L,
    val lastReadSurahName: String = "Al-Fatiha",
    val azanEnabled: Boolean = true,
    val prePrayerAlertEnabled: Boolean = true,
    val nightLockEnabled: Boolean = true,
    val pornBlockEnabled: Boolean = true,
    val uninstallLockEnabled: Boolean = true,
    val dailyPhoneLimitMinutes: Long = 120L,
    val focusModeUntilEpochMs: Long = 0L,
    val activeLockMode: String = LockMode.NONE.code,
    val requiredPushUps: Long = 0L,
    val activeLockUntilEpochMs: Long = 0L,
    val femaleVoiceEnabled: Boolean = true,
    val namazAutoSilentEnabled: Boolean = true,
    val ffDailyLimitMinutes: Long = 60L,
    val ffGuardEnabled: Boolean = true,
    val gamblingBlockEnabled: Boolean = true,
    val sosPhoneNumber: String = "",
    val defensePlanDayBits: Long = 0L,
    val lockTriggerReason: String = "",
    val familyRole: String = FamilyRole.BOSS.code,
    @get:PropertyName("isPeriodMode")
    @field:PropertyName("isPeriodMode")
    val isPeriodMode: Boolean = false,
    val periodDayNumber: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class NamazLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val fajr: Boolean = false,
    val dhuhr: Boolean = false,
    val asr: Boolean = false,
    val maghrib: Boolean = false,
    val isha: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun prayedCount(): Int =
        listOf(fajr, dhuhr, asr, maghrib, isha).count { it }
}

@IgnoreExtraProperties
data class AiMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sender: String = "NOOR",
    val text: String = "",
    val reference: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Reminder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val hour: Long = 6L,
    val minute: Long = 0L,
    val enabled: Boolean = true,
    val repeatDaily: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AppLimit(
    @DocumentId val id: String = "",
    val userId: String = "",
    val packageName: String = "",
    val appName: String = "",
    val dailyLimitMinutes: Long = 30L,
    val enabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class Wakto(val displayBn: String, val displayEn: String) {
    FAJR("ফজর", "Fajr"),
    DHUHR("যোহর", "Dhuhr"),
    ASR("আসর", "Asor"),
    MAGHRIB("মাগরিব", "Maghrib"),
    ISHA("এশা", "Isha")
}

data class PrayerScheduleItem(
    val wakto: Wakto,
    val timeEpochMs: Long,
    val formattedTime: String
)

data class SurahMeta(
    val number: Int,
    val arabicName: String,
    val transliteration: String,
    val banglaName: String,
    val revelationType: String,
    val ayahCount: Int,
    val isCachedOffline: Boolean = false
)

data class AyahItem(
    val numberInSurah: Int,
    val arabicText: String,
    val banglaTransliteration: String,
    val banglaMeaning: String,
    val shortTafsir: String
)

data class HadithItem(
    val id: String,
    val book: String,
    val hadithNumber: String,
    val arabicText: String,
    val banglaText: String,
    val simpleExplanation: String,
    val narrator: String
)

data class DuaItem(
    val id: String,
    val category: String,
    val titleBn: String,
    val arabicText: String,
    val banglaPronunciation: String,
    val banglaMeaning: String,
    val reference: String
)

data class AppUsageStat(
    val packageName: String,
    val appName: String,
    val minutesToday: Int
)

data class NamazRakatInfo(
    val waktoName: String,
    val rakatBreakdown: String,
    val whatToRecite: String
)

data class GuideStepItem(
    val stepNumber: Int,
    val titleBn: String,
    val descriptionBn: String,
    val tipBn: String = ""
)

data class NamazRecitationItem(
    val id: String,
    val titleBn: String,
    val arabicText: String,
    val banglaPronunciation: String,
    val banglaMeaning: String,
    val audioSpeakText: String
)

data class DefenseVideoItem(
    val id: String,
    val titleBn: String,
    val durationText: String,
    val summaryBn: String,
    val searchQueryUrl: String
)

data class DefensePlanDay(
    val dayNumber: Int,
    val titleBn: String,
    val drillSummaryBn: String
)

data class ChutanoTechnique(
    val id: String,
    val titleBn: String,
    val stepsBn: List<String>,
    val keyPointBn: String
)

data class EmergencyNumberItem(
    val titleBn: String,
    val number: String,
    val noteBn: String
)
