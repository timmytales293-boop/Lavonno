package com.example.guard

import android.content.Context
import android.content.SharedPreferences
import com.example.audio.FemaleVoiceTone
import com.example.data.model.FamilyRole
import com.example.data.model.LockMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

data class LocalGuardState(
    val activeLockMode: LockMode = LockMode.NONE,
    val requiredPushUps: Int = 0,
    val lockTriggerReason: String = "",
    val pornBlockEnabled: Boolean = true,
    val gamblingBlockEnabled: Boolean = true,
    val uninstallLockEnabled: Boolean = true,
    val nightLockEnabled: Boolean = true,
    val azanEnabled: Boolean = true,
    val prePrayerAlertEnabled: Boolean = true,
    val femaleVoiceEnabled: Boolean = true,
    val femaleVoiceTone: FemaleVoiceTone = FemaleVoiceTone.SOFT_MISTI,
    val namazAutoSilentEnabled: Boolean = true,
    val dailyPhoneLimitMinutes: Int = 120,
    val ffDailyLimitMinutes: Int = 60,
    val ffGuardEnabled: Boolean = true,
    val focusModeUntilMs: Long = 0L,
    val postUnlockBanner: String? = null,
    val sosPhoneNumber: String = "",
    val defensePlanDayBits: Int = 0,
    val ojuChecklistBits: Int = 0,
    val pakChecklistBits: Int = 0,
    val familyRole: FamilyRole = FamilyRole.BOSS,
    val isPeriodMode: Boolean = false,
    val periodDayNumber: Int = 0,
    val nightSleepDuaAutoEnabled: Boolean = true,
    val appLimits: Map<String, Int> = DEFAULT_APP_LIMITS,
    val lastContentSyncEpochMs: Long = 0L,
    val lastContentSyncSummary: String = "",
    val ownerFaceEnrolled: Boolean = false,
    val ownerFaceSignatureJson: String = "",
    val driveSecurityEnabled: Boolean = true,
    val driveFolderId: String = "",
    val lastSecurityEvent: String = "",
    val lastSecurityUploadEpochMs: Long = 0L,
    val lastLatitude: Double = 24.7471,
    val lastLongitude: Double = 90.4203
) {
    companion object {
        val DEFAULT_APP_LIMITS = mapOf(
            "com.facebook.katana" to 30,
            "com.google.android.youtube" to 45,
            "com.android.chrome" to 45,
            "com.instagram.android" to 30,
            "com.zhiliaoapp.musically" to 20,
            "com.dts.freefireth" to 60,
            "com.dts.freefiremax" to 60
        )
    }
}

class GuardPreferences(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _state = MutableStateFlow(readState())
    val state: StateFlow<LocalGuardState> = _state.asStateFlow()

    private fun readState(): LocalGuardState {
        val mode = LockMode.fromCode(prefs.getString(KEY_LOCK_MODE, LockMode.NONE.code))
        val limitsJson = prefs.getString(KEY_APP_LIMITS, null)
        val limits = if (limitsJson.isNullOrBlank()) {
            LocalGuardState.DEFAULT_APP_LIMITS
        } else {
            runCatching {
                val obj = JSONObject(limitsJson)
                buildMap {
                    obj.keys().forEach { k -> put(k, obj.optInt(k, 30)) }
                }
            }.getOrDefault(LocalGuardState.DEFAULT_APP_LIMITS)
        }

        return LocalGuardState(
            activeLockMode = mode,
            requiredPushUps = prefs.getInt(KEY_REQUIRED_PUSHUPS, 0),
            lockTriggerReason = prefs.getString(KEY_LOCK_REASON, "").orEmpty(),
            pornBlockEnabled = prefs.getBoolean(KEY_PORN_BLOCK, true),
            gamblingBlockEnabled = prefs.getBoolean(KEY_GAMBLING_BLOCK, true),
            uninstallLockEnabled = prefs.getBoolean(KEY_UNINSTALL_LOCK, true),
            nightLockEnabled = prefs.getBoolean(KEY_NIGHT_LOCK, true),
            azanEnabled = prefs.getBoolean(KEY_AZAN_ENABLED, true),
            prePrayerAlertEnabled = prefs.getBoolean(KEY_PRE_PRAYER, true),
            femaleVoiceEnabled = prefs.getBoolean(KEY_FEMALE_VOICE, true),
            femaleVoiceTone = FemaleVoiceTone.fromCode(
                prefs.getString(KEY_FEMALE_VOICE_TONE, FemaleVoiceTone.SOFT_MISTI.code)
            ),
            namazAutoSilentEnabled = prefs.getBoolean(KEY_NAMAZ_SILENT, true),
            dailyPhoneLimitMinutes = prefs.getInt(KEY_DAILY_LIMIT_MIN, 120),
            ffDailyLimitMinutes = prefs.getInt(KEY_FF_LIMIT_MIN, 60),
            ffGuardEnabled = prefs.getBoolean(KEY_FF_GUARD, true),
            focusModeUntilMs = prefs.getLong(KEY_FOCUS_UNTIL, 0L),
            postUnlockBanner = prefs.getString(KEY_UNLOCK_BANNER, null),
            sosPhoneNumber = prefs.getString(KEY_SOS_PHONE, "").orEmpty(),
            defensePlanDayBits = prefs.getInt(KEY_DEFENSE_DAYS, 0),
            ojuChecklistBits = prefs.getInt(KEY_OJU_CHECKLIST, 0),
            pakChecklistBits = prefs.getInt(KEY_PAK_CHECKLIST, 0),
            familyRole = FamilyRole.fromCode(prefs.getString(KEY_FAMILY_ROLE, FamilyRole.BOSS.code)),
            isPeriodMode = prefs.getBoolean(KEY_PERIOD_MODE, false),
            periodDayNumber = prefs.getInt(KEY_PERIOD_DAY, 0),
            nightSleepDuaAutoEnabled = prefs.getBoolean(KEY_SLEEP_DUA_AUTO, true),
            appLimits = limits,
            lastContentSyncEpochMs = prefs.getLong(KEY_LAST_SYNC_EPOCH, 0L),
            lastContentSyncSummary = prefs.getString(KEY_LAST_SYNC_SUMMARY, "").orEmpty(),
            ownerFaceEnrolled = !prefs.getString(KEY_OWNER_FACE_SIG, "").isNullOrBlank(),
            ownerFaceSignatureJson = prefs.getString(KEY_OWNER_FACE_SIG, "").orEmpty(),
            driveSecurityEnabled = prefs.getBoolean(KEY_DRIVE_SECURITY_ENABLED, true),
            driveFolderId = prefs.getString(KEY_DRIVE_FOLDER_ID, "").orEmpty(),
            lastSecurityEvent = prefs.getString(KEY_LAST_SECURITY_EVENT, "").orEmpty(),
            lastSecurityUploadEpochMs = prefs.getLong(KEY_LAST_SECURITY_UPLOAD_MS, 0L),
            lastLatitude = Double.fromBits(prefs.getLong(KEY_LAST_LAT_BITS, java.lang.Double.doubleToRawLongBits(24.7471))),
            lastLongitude = Double.fromBits(prefs.getLong(KEY_LAST_LON_BITS, java.lang.Double.doubleToRawLongBits(90.4203)))
        )
    }

    fun getCurrentState(): LocalGuardState = readState()

    fun recordContentSyncSuccess(epochMs: Long, summary: String) {
        prefs.edit()
            .putLong(KEY_LAST_SYNC_EPOCH, epochMs)
            .putString(KEY_LAST_SYNC_SUMMARY, summary.take(180))
            .apply()
        _state.value = readState()
    }

    fun setDirectPushUpLock(mode: LockMode, requiredPushUps: Int, triggerReason: String = "") {
        prefs.edit()
            .putString(KEY_LOCK_MODE, mode.code)
            .putInt(KEY_REQUIRED_PUSHUPS, requiredPushUps)
            .putString(KEY_LOCK_REASON, triggerReason)
            .commit()
        _state.value = readState()
    }

    fun clearLockWithBanner(bannerMessage: String?) {
        prefs.edit()
            .putString(KEY_LOCK_MODE, LockMode.NONE.code)
            .putInt(KEY_REQUIRED_PUSHUPS, 0)
            .putString(KEY_LOCK_REASON, "")
            .putString(KEY_UNLOCK_BANNER, bannerMessage)
            .commit()
        _state.value = readState()
    }

    fun clearBanner() {
        prefs.edit().remove(KEY_UNLOCK_BANNER).apply()
        _state.value = readState()
    }

    fun setPornBlock(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PORN_BLOCK, enabled).apply()
        _state.value = readState()
    }

    fun setGamblingBlock(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GAMBLING_BLOCK, enabled).apply()
        _state.value = readState()
    }

    fun setUninstallLock(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_UNINSTALL_LOCK, enabled).apply()
        _state.value = readState()
    }

    fun setNightLock(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NIGHT_LOCK, enabled).apply()
        _state.value = readState()
    }

    fun setAzanEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AZAN_ENABLED, enabled).apply()
        _state.value = readState()
    }

    fun setPrePrayerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PRE_PRAYER, enabled).apply()
        _state.value = readState()
    }

    fun setFemaleVoiceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FEMALE_VOICE, enabled).apply()
        _state.value = readState()
    }

    fun setFemaleVoiceTone(tone: FemaleVoiceTone) {
        prefs.edit().putString(KEY_FEMALE_VOICE_TONE, tone.code).apply()
        _state.value = readState()
    }

    fun setNamazAutoSilentEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NAMAZ_SILENT, enabled).apply()
        _state.value = readState()
    }

    fun setDailyPhoneLimitMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_DAILY_LIMIT_MIN, minutes.coerceIn(15, 720)).apply()
        _state.value = readState()
    }

    fun setFfDailyLimitMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_FF_LIMIT_MIN, minutes.coerceIn(10, 360)).apply()
        setAppLimit("com.dts.freefireth", minutes.coerceIn(10, 360))
        setAppLimit("com.dts.freefiremax", minutes.coerceIn(10, 360))
        _state.value = readState()
    }

    fun setFfGuardEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FF_GUARD, enabled).apply()
        _state.value = readState()
    }

    fun setFocusModeUntil(untilMs: Long) {
        prefs.edit().putLong(KEY_FOCUS_UNTIL, untilMs).apply()
        _state.value = readState()
    }

    fun setSosPhoneNumber(number: String) {
        prefs.edit().putString(KEY_SOS_PHONE, number.take(30)).apply()
        _state.value = readState()
    }

    fun setDefensePlanDayBits(bits: Int) {
        prefs.edit().putInt(KEY_DEFENSE_DAYS, bits.coerceIn(0, 127)).apply()
        _state.value = readState()
    }

    fun toggleOjuChecklistBit(index: Int) {
        val current = readState().ojuChecklistBits
        val updated = current xor (1 shl index)
        prefs.edit().putInt(KEY_OJU_CHECKLIST, updated).apply()
        _state.value = readState()
    }

    fun togglePakChecklistBit(index: Int) {
        val current = readState().pakChecklistBits
        val updated = current xor (1 shl index)
        prefs.edit().putInt(KEY_PAK_CHECKLIST, updated).apply()
        _state.value = readState()
    }

    fun setFamilyRole(role: FamilyRole) {
        prefs.edit().putString(KEY_FAMILY_ROLE, role.code).apply()
        _state.value = readState()
    }

    fun setPeriodState(isPeriod: Boolean, dayNumber: Int) {
        prefs.edit()
            .putBoolean(KEY_PERIOD_MODE, isPeriod)
            .putInt(KEY_PERIOD_DAY, dayNumber.coerceIn(0, 45))
            .apply()
        _state.value = readState()
    }

    fun setNightSleepDuaAuto(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SLEEP_DUA_AUTO, enabled).apply()
        _state.value = readState()
    }

    fun setAppLimit(packageName: String, minutes: Int) {
        val updated = readState().appLimits.toMutableMap()
        updated[packageName] = minutes.coerceIn(1, 720)
        val obj = JSONObject()
        updated.forEach { (k, v) -> obj.put(k, v) }
        prefs.edit().putString(KEY_APP_LIMITS, obj.toString()).apply()
        _state.value = readState()
    }

    fun hasAskedCameraOnboarding(): Boolean =
        prefs.getBoolean(KEY_CAMERA_ASKED, false)

    fun markCameraOnboardingAsked() {
        prefs.edit().putBoolean(KEY_CAMERA_ASKED, true).apply()
    }

    fun setActiveNamazWindowUntil(untilMs: Long) {
        prefs.edit().putLong(KEY_NAMAZ_WINDOW_UNTIL, untilMs).apply()
    }

    fun isWithinNamazWindow(nowMs: Long = System.currentTimeMillis()): Boolean =
        nowMs <= prefs.getLong(KEY_NAMAZ_WINDOW_UNTIL, 0L)

    fun saveOwnerFaceSignature(signatureJson: String) {
        prefs.edit()
            .putString(KEY_OWNER_FACE_SIG, signatureJson)
            .putString(KEY_LAST_SECURITY_EVENT, "Owner Face Enrolled ✓ (Owner Photo Never Uploaded)")
            .commit()
        _state.value = readState()
    }

    fun clearOwnerFaceSignature() {
        prefs.edit()
            .remove(KEY_OWNER_FACE_SIG)
            .putString(KEY_LAST_SECURITY_EVENT, "Owner Face Removed")
            .apply()
        _state.value = readState()
    }

    fun setDriveSecurityEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DRIVE_SECURITY_ENABLED, enabled).apply()
        _state.value = readState()
    }

    fun saveDriveFolderId(folderId: String) {
        prefs.edit().putString(KEY_DRIVE_FOLDER_ID, folderId).apply()
        _state.value = readState()
    }

    fun recordSecurityEvent(message: String, uploadEpochMs: Long? = null) {
        prefs.edit().apply {
            putString(KEY_LAST_SECURITY_EVENT, message.take(200))
            if (uploadEpochMs != null) {
                putLong(KEY_LAST_SECURITY_UPLOAD_MS, uploadEpochMs)
            }
        }.apply()
        _state.value = readState()
    }

    fun saveLocationCoordinates(lat: Double, lon: Double) {
        prefs.edit()
            .putLong(KEY_LAST_LAT_BITS, java.lang.Double.doubleToRawLongBits(lat))
            .putLong(KEY_LAST_LON_BITS, java.lang.Double.doubleToRawLongBits(lon))
            .apply()
        _state.value = readState()
    }

    companion object {
        private const val PREFS_NAME = "noor_guard_prefs"
        private const val KEY_LOCK_MODE = "lock_mode"
        private const val KEY_REQUIRED_PUSHUPS = "required_pushups"
        private const val KEY_LOCK_REASON = "lock_reason"
        private const val KEY_PORN_BLOCK = "porn_block"
        private const val KEY_GAMBLING_BLOCK = "gambling_block"
        private const val KEY_UNINSTALL_LOCK = "uninstall_lock"
        private const val KEY_NIGHT_LOCK = "night_lock"
        private const val KEY_AZAN_ENABLED = "azan_enabled"
        private const val KEY_PRE_PRAYER = "pre_prayer"
        private const val KEY_FEMALE_VOICE = "female_voice"
        private const val KEY_FEMALE_VOICE_TONE = "female_voice_tone"
        private const val KEY_NAMAZ_SILENT = "namaz_silent"
        private const val KEY_DAILY_LIMIT_MIN = "daily_limit_min"
        private const val KEY_FF_LIMIT_MIN = "ff_limit_min"
        private const val KEY_FF_GUARD = "ff_guard"
        private const val KEY_FOCUS_UNTIL = "focus_until"
        private const val KEY_UNLOCK_BANNER = "unlock_banner"
        private const val KEY_SOS_PHONE = "sos_phone"
        private const val KEY_DEFENSE_DAYS = "defense_days"
        private const val KEY_OJU_CHECKLIST = "oju_checklist"
        private const val KEY_PAK_CHECKLIST = "pak_checklist"
        private const val KEY_FAMILY_ROLE = "family_role"
        private const val KEY_PERIOD_MODE = "period_mode"
        private const val KEY_PERIOD_DAY = "period_day"
        private const val KEY_SLEEP_DUA_AUTO = "sleep_dua_auto"
        private const val KEY_APP_LIMITS = "app_limits"
        private const val KEY_CAMERA_ASKED = "camera_asked"
        private const val KEY_NAMAZ_WINDOW_UNTIL = "namaz_window_until"
        private const val KEY_LAST_SYNC_EPOCH = "last_content_sync_epoch"
        private const val KEY_LAST_SYNC_SUMMARY = "last_content_sync_summary"
        private const val KEY_OWNER_FACE_SIG = "owner_face_sig_json"
        private const val KEY_DRIVE_SECURITY_ENABLED = "drive_security_enabled"
        private const val KEY_DRIVE_FOLDER_ID = "drive_noor_security_folder_id"
        private const val KEY_LAST_SECURITY_EVENT = "last_security_event"
        private const val KEY_LAST_SECURITY_UPLOAD_MS = "last_security_upload_ms"
        private const val KEY_LAST_LAT_BITS = "last_latitude_bits"
        private const val KEY_LAST_LON_BITS = "last_longitude_bits"
    }
}
