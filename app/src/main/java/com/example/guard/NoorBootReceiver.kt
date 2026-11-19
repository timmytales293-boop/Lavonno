package com.example.guard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.sync.ContentSyncScheduler
import com.example.domain.prayer.PrayerTimeCalculator

/**
 * Restores scheduled Azan, Pre-Prayer, Female Voice daily reminders, and background
 * WorkManager sync after device reboot or app update.
 */
class NoorBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        val prefs = GuardPreferences(context)
        val guard = prefs.getCurrentState()
        val schedule = PrayerTimeCalculator.calculateDailySchedule(
            latitude = guard.lastLatitude,
            longitude = guard.lastLongitude,
            dateMillis = System.currentTimeMillis()
        )

        NoorAlarmScheduler.schedulePrayerAlerts(
            context = context,
            schedule = schedule,
            azanEnabled = guard.azanEnabled,
            prePrayerEnabled = guard.prePrayerAlertEnabled
        )
        NoorAlarmScheduler.schedulePremiumDailyReminders(
            context = context,
            sleepDuaAuto = guard.nightSleepDuaAutoEnabled
        )
        ContentSyncScheduler.schedulePeriodicSync(context)
    }
}
