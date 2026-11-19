package com.example.guard

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.PrayerScheduleItem
import java.util.Calendar

object NoorAlarmScheduler {

    fun schedulePrayerAlerts(
        context: Context,
        schedule: List<PrayerScheduleItem>,
        azanEnabled: Boolean,
        prePrayerEnabled: Boolean
    ) {
        NoorAlarmReceiver.ensureChannels(context)
        val now = System.currentTimeMillis()
        schedule.forEachIndexed { index, item ->
            val preTime = item.timeEpochMs - 10 * 60 * 1000L
            if (prePrayerEnabled && preTime > now) {
                scheduleExactOrFallback(
                    context = context,
                    triggerAtMs = preTime,
                    requestCode = 2000 + index,
                    type = NoorAlarmReceiver.TYPE_PRE_PRAYER,
                    title = "Noor — ওজুর সময়",
                    body = "Boss, ${item.wakto.displayEn} er 10 Min Baki - Oju Kore Nin"
                )
            }
            if (azanEnabled && item.timeEpochMs > now) {
                scheduleExactOrFallback(
                    context = context,
                    triggerAtMs = item.timeEpochMs,
                    requestCode = 3000 + index,
                    type = NoorAlarmReceiver.TYPE_AZAN,
                    title = "Noor Azan — ${item.wakto.displayEn}",
                    body = "Boss Apnar Namaz Er Time Hoise, Oju Kore Asen"
                )
            }
        }
    }

    fun schedulePremiumDailyReminders(context: Context, sleepDuaAuto: Boolean) {
        NoorAlarmReceiver.ensureChannels(context)
        // Morning Greeting (7:30 AM)
        scheduleDailyAt(context, 4101, 7, 30, NoorAlarmReceiver.TYPE_REMINDER,
            "Noor — সকালের শুভেচ্ছা", "Assalamualaikum Boss, Ajker Din Ta Valo Hok")
        // Hydration / Pani Newa (11:00 AM)
        scheduleDailyAt(context, 4102, 11, 0, NoorAlarmReceiver.TYPE_REMINDER,
            "Noor — পানি খাওয়ার সময়", "Boss Apnar Pani Newar Time Hoise")
        // Self Defence 5 Min Practice (4:45 PM)
        scheduleDailyAt(context, 4103, 16, 45, NoorAlarmReceiver.TYPE_REMINDER,
            "Noor — Self Defence 5 Min", "Boss Apnar Self Defence Practice Er Time Hoise, 5 Min Practice Koren")
        // Chutano Beyam (5:30 PM)
        scheduleDailyAt(context, 4104, 17, 30, NoorAlarmReceiver.TYPE_REMINDER,
            "Noor — ছুটানো ব্যায়াম", "Boss Apnar Chutano Beyam Er Time Hoise")
        // Night Sleep Dua + 11 PM Sleep Reminder
        if (sleepDuaAuto) {
            scheduleDailyAt(context, 4105, 22, 45, NoorAlarmReceiver.TYPE_NIGHT_SLEEP_DUA,
                "Noor — ঘুমের দোয়া", "বিসমিকাল্লাহুম্মা আমূতু ওয়া আহইয়া। Boss Apnar Ghumer Time Hoise, Phone Rakhen")
        }
        scheduleDailyAt(context, 4106, 23, 0, NoorAlarmReceiver.TYPE_REMINDER,
            "Noor — রাত ১১টা লক", "Boss Apnar Ghumer Time Hoise, Phone Rakhen")
    }

    private fun scheduleDailyAt(
        context: Context,
        requestCode: Int,
        hour: Int,
        minute: Int,
        type: String,
        title: String,
        body: String
    ) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        scheduleExactOrFallback(context, cal.timeInMillis, requestCode, type, title, body)
    }

    fun scheduleBossAlarm(
        context: Context,
        reminderIdHash: Int,
        title: String,
        hour: Int,
        minute: Int
    ) {
        NoorAlarmReceiver.ensureChannels(context)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        scheduleExactOrFallback(
            context = context,
            triggerAtMs = cal.timeInMillis,
            requestCode = 5000 + (reminderIdHash and 0x7FFF),
            type = NoorAlarmReceiver.TYPE_ALARM,
            title = "Noor Alarm - Boss!",
            body = title
        )
    }

    private fun scheduleExactOrFallback(
        context: Context,
        triggerAtMs: Long,
        requestCode: Int,
        type: String,
        title: String,
        body: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NoorAlarmReceiver::class.java).apply {
            putExtra(NoorAlarmReceiver.EXTRA_TYPE, type)
            putExtra(NoorAlarmReceiver.EXTRA_TITLE, title)
            putExtra(NoorAlarmReceiver.EXTRA_BODY, body)
            putExtra(NoorAlarmReceiver.EXTRA_NOTIF_ID, requestCode)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
        }
    }
}
