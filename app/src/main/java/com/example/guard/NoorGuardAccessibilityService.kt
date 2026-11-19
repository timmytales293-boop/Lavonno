package com.example.guard

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.MainActivity
import com.example.data.model.LockMode
import java.util.Calendar

class NoorGuardAccessibilityService : AccessibilityService() {

    private var lastUsageCheckMs = 0L

    // All Haram keywords: Bangla / English / Banglish (Adult + Porn + Gambling + Nesha/Sud)
    private val blockedAdultTerms = listOf(
        "pornhub", "xvideos", "xnxx", "xhamster", "redtube", "youporn",
        "brazzers", "onlyfans", "chaturbate", "spankbang", "porn", "xxxvideo",
        "hentai", "nsfw", "sex video", "nude", "adult video", "bangbros",
        "xvideo", "nangta", "choti", "kharap video", "kharap pic", "hot video",
        "sexy video", "bf video", "পর্ন", "সেক্স", "নগ্ন", "অশ্লীল", "খারাপ ভিডিও", "চটি"
    )

    private val blockedGamblingAndInterestTerms = listOf(
        "1xbet", "melbet", "bet365", "betway", "mostbet", "linebet", "parimatch",
        "baji999", "baji live", "jeetbuzz", "crickex", "casino", "betting",
        "teen patti", "teenpatti", "rummy", "poker", "blackjack", "roulette",
        "slot game", "juya", "জুয়া", "বেটিং", "ক্যাসিনো", "তিন পাত্তি", "রামি",
        "nesha", "sud ", "interest loan", "alcohol", "vodka", "whisky",
        "cannabis", "মদ", "নেশা", "সুদ", "ইয়াবা", "গাঁজা"
    )

    private val ffBettingAndTopUpTerms = listOf(
        "top up", "top-up", "topup", "codashop", "unipin", "diamond buy",
        "luck royale", "bet", "spin", "টপ আপ", "ডায়মন্ড", "জুয়া"
    )

    private val blockedGamblingPackageHints = listOf(
        "teenpatti", "rummy", "casino", "1xbet", "melbet", "mostbet", "bet365", "poker"
    )

    private val freeFirePackages = setOf(
        "com.dts.freefireth",
        "com.dts.freefiremax"
    )

    private val uninstallPackages = setOf(
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.settings"
    )

    private val monitoredContentPackages = setOf(
        "com.android.chrome",
        "org.mozilla.firefox",
        "com.sec.android.app.sbrowser",
        "com.opera.browser",
        "com.brave.browser",
        "com.google.android.youtube",
        "com.facebook.katana",
        "com.facebook.lite",
        "com.instagram.android",
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.google.android.googlequicksearchbox"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        if (pkg == "com.android.systemui" || pkg.contains("launcher")) return

        val prefs = GuardPreferences(this)
        val guard = prefs.getCurrentState()
        val now = System.currentTimeMillis()

        // 1. If a Push-Up / Squat / Plank Direct Lock is ALREADY active -> return user directly to Noor
        if (guard.activeLockMode == LockMode.PUSHUP_30 || guard.activeLockMode == LockMode.PUSHUP_10) {
            engageDirectLock()
            return
        }

        val eventText = collectEventText(event)
        val pkgLower = pkg.lowercase()

        // 2. Gambling / Casino / Teen Patti / Rummy app package block -> Direct 30 Lock + Allah Dekhtase Mama, Tham
        if (guard.gamblingBlockEnabled && blockedGamblingPackageHints.any { pkgLower.contains(it) }) {
            prefs.setDirectPushUpLock(LockMode.PUSHUP_30, 30, REMINDER_ALLAH_DEKHTASE)
            engageDirectLock()
            return
        }

        // 3. Haram / Porn / Gambling / Nesha / Sud keyword search or site open -> Direct 30 Lock + Reminder
        val isMonitoredAppOrBrowser = pkg in monitoredContentPackages || pkgLower.contains("browser") || pkgLower.contains("search")
        if (isMonitoredAppOrBrowser) {
            val hitAdult = guard.pornBlockEnabled && blockedAdultTerms.any { eventText.contains(it) }
            val hitHaramGambling = guard.gamblingBlockEnabled && blockedGamblingAndInterestTerms.any { eventText.contains(it) }
            if (hitAdult || hitHaramGambling) {
                prefs.setDirectPushUpLock(LockMode.PUSHUP_30, 30, REMINDER_ALLAH_DEKHTASE)
                engageDirectLock()
                return
            }
        }

        // 4. Uninstall Lock -> Direct 30 Push-Up Lock (NO warning screen!)
        if (guard.uninstallLockEnabled && pkg in uninstallPackages) {
            val targetsNoor = eventText.contains("noor") || eventText.contains("noorislamic")
            val isUninstallAction = eventText.contains("uninstall") ||
                eventText.contains("deactivate") ||
                eventText.contains("remove") ||
                eventText.contains("delete")
            if (targetsNoor && isUninstallAction) {
                prefs.setDirectPushUpLock(LockMode.PUSHUP_30, 30, "Noor Uninstall Lock Active")
                engageDirectLock()
                return
            }
        }

        // 5. Namaz Time Auto Silent + Lock, Night 11 PM Auto Lock, and Focus Mode
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val isNightLockWindow = guard.nightLockEnabled && (hour >= 23 || hour < 4)
        val isFocusWindow = guard.focusModeUntilMs > now
        val isNamazWindow = guard.namazAutoSilentEnabled && prefs.isWithinNamazWindow(now)

        if (isNamazWindow) {
            enableSilentModeIfPermitted(this)
            engageDirectLock()
            return
        }

        // 6. Free Fire Guard: Lock after 11 PM, block Top-Up/Betting inside FF, enforce 1h daily limit
        if (guard.ffGuardEnabled && pkg in freeFirePackages) {
            if (isNightLockWindow) {
                engageDirectLock()
                return
            }
            if (guard.gamblingBlockEnabled && ffBettingAndTopUpTerms.any { eventText.contains(it) }) {
                prefs.setDirectPushUpLock(LockMode.PUSHUP_30, 30, REMINDER_ALLAH_DEKHTASE)
                engageDirectLock()
                return
            }
        }

        if (isNightLockWindow || isFocusWindow) {
            engageDirectLock()
            return
        }

        // 7. Daily Phone Limit / FF 1h Limit / App Restrict -> Direct 10 Push-Up Lock (throttled every 15s)
        if (now - lastUsageCheckMs >= 15_000L) {
            lastUsageCheckMs = now
            val report = UsageStatsHelper.getTodayUsageReport(this)
            if (report.hasPermission) {
                val ffExceeded = guard.ffGuardEnabled && report.ffMinutesToday >= guard.ffDailyLimitMinutes
                if (ffExceeded) {
                    prefs.setDirectPushUpLock(
                        LockMode.PUSHUP_10,
                        10,
                        "Boss Apnar FF Time Ses, Ebar Quran Poren"
                    )
                    engageDirectLock()
                    return
                }
                val appLimitMin = guard.appLimits[pkg]
                val appUsedMin = report.topApps.firstOrNull { it.packageName == pkg }?.minutesToday ?: 0
                val appExceeded = appLimitMin != null && appUsedMin >= appLimitMin
                val totalExceeded = report.totalMinutes >= guard.dailyPhoneLimitMinutes
                if (appExceeded || totalExceeded) {
                    prefs.setDirectPushUpLock(
                        LockMode.PUSHUP_10,
                        10,
                        "Boss Apnar Phone Beshi Choltase, Ektu Rakhen"
                    )
                    engageDirectLock()
                }
            }
        }
    }

    private fun collectEventText(event: AccessibilityEvent): String {
        val sb = StringBuilder()
        event.text.forEach { sb.append(it).append(' ') }
        event.contentDescription?.let { sb.append(it).append(' ') }
        rootInActiveWindow?.let { root ->
            appendNodeText(root, sb, depth = 0)
        }
        return sb.toString().lowercase()
    }

    private fun appendNodeText(node: AccessibilityNodeInfo, sb: StringBuilder, depth: Int) {
        if (depth > 5) return
        node.text?.let { sb.append(it).append(' ') }
        node.contentDescription?.let { sb.append(it).append(' ') }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            appendNodeText(child, sb, depth + 1)
        }
    }

    private fun engageDirectLock() {
        performGlobalAction(GLOBAL_ACTION_HOME)
        val lockIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(lockIntent)
    }

    override fun onInterrupt() {
        // No-op
    }

    companion object {
        const val REMINDER_ALLAH_DEKHTASE = "Allah Dekhtase Mama, Tham"

        fun enableSilentModeIfPermitted(context: Context) {
            runCatching {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (nm.isNotificationPolicyAccessGranted) {
                    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    if (am.ringerMode != AudioManager.RINGER_MODE_SILENT) {
                        am.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    }
                }
            }
        }
    }
}
