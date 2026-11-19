package com.example.domain.prayer

import com.example.data.model.PrayerScheduleItem
import com.example.data.model.Wakto
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

object PrayerTimeCalculator {

    private const val FAJR_ANGLE = 18.0
    private const val ISHA_ANGLE = 18.0

    fun calculateDailySchedule(
        latitude: Double = 24.7471,
        longitude: Double = 90.4203,
        timeZoneOffsetHours: Double = defaultTimeZoneOffsetHours(),
        dateMillis: Long = System.currentTimeMillis()
    ): List<PrayerScheduleItem> {
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)

        val jd = julianDate(year, month, day) - longitude / (15.0 * 24.0)
        val (declination, eqTime) = sunPosition(jd + 0.5)

        val dhuhrHours = fixHour(12.0 + timeZoneOffsetHours - longitude / 15.0 - eqTime)
        val sunriseHours = dhuhrHours - sunAngleTime(0.833, latitude, declination)
        val sunsetHours = dhuhrHours + sunAngleTime(0.833, latitude, declination)
        val fajrHours = dhuhrHours - sunAngleTime(FAJR_ANGLE, latitude, declination)
        // Hanafi Asr (shadow factor = 2) standard across Bangladesh
        val asrHours = dhuhrHours + asrAngleTime(2.0, latitude, declination)
        val maghribHours = sunsetHours
        val ishaHours = dhuhrHours + sunAngleTime(ISHA_ANGLE, latitude, declination)

        val dayStart = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val formatter = SimpleDateFormat("hh:mm a", Locale.US)

        fun toItem(wakto: Wakto, decimalHours: Double): PrayerScheduleItem {
            val totalSeconds = (decimalHours * 3600.0).toLong().coerceAtLeast(0L)
            val epochMs = dayStart + totalSeconds * 1000L
            return PrayerScheduleItem(
                wakto = wakto,
                timeEpochMs = epochMs,
                formattedTime = formatter.format(Date(epochMs))
            )
        }

        // Order: Fajr -> Dhuhr -> Asor -> Maghrib -> Isha
        return listOf(
            toItem(Wakto.FAJR, fajrHours),
            toItem(Wakto.DHUHR, dhuhrHours + (2.0 / 60.0)), // 2 min ihtiyat
            toItem(Wakto.ASR, asrHours),
            toItem(Wakto.MAGHRIB, maghribHours + (1.0 / 60.0)),
            toItem(Wakto.ISHA, ishaHours)
        )
    }

    fun nextPrayer(
        latitude: Double = 24.7471,
        longitude: Double = 90.4203,
        nowMillis: Long = System.currentTimeMillis()
    ): PrayerScheduleItem {
        val today = calculateDailySchedule(latitude, longitude, dateMillis = nowMillis)
        val upcomingToday = today.firstOrNull { it.timeEpochMs > nowMillis }
        if (upcomingToday != null) return upcomingToday

        // After Isha -> Next is tomorrow's Fajr
        val tomorrowMillis = nowMillis + 24L * 60L * 60L * 1000L
        return calculateDailySchedule(latitude, longitude, dateMillis = tomorrowMillis).first()
    }

    fun passedPrayersToday(
        latitude: Double = 24.7471,
        longitude: Double = 90.4203,
        nowMillis: Long = System.currentTimeMillis()
    ): Set<Wakto> {
        val today = calculateDailySchedule(latitude, longitude, dateMillis = nowMillis)
        return today.filter { it.timeEpochMs <= nowMillis }.map { it.wakto }.toSet()
    }

    fun formatCountdown(targetEpochMs: Long, nowMillis: Long): String {
        val diffSeconds = ((targetEpochMs - nowMillis) / 1000L).coerceAtLeast(0L)
        val hours = diffSeconds / 3600L
        val minutes = (diffSeconds % 3600L) / 60L
        val seconds = diffSeconds % 60L
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun isFriday(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
    }

    fun todayDateKey(nowMillis: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date(nowMillis))
    }

    private fun defaultTimeZoneOffsetHours(): Double {
        val tz = TimeZone.getDefault()
        return tz.getOffset(System.currentTimeMillis()) / 3_600_000.0
    }

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = darctan2(dcos(e) * dsin(l), dcos(l)) / 15.0
        val decl = darcsin(dsin(e) * dsin(l))
        val eqTime = q / 15.0 - fixHour(ra)
        return decl to eqTime
    }

    private fun sunAngleTime(angle: Double, lat: Double, decl: Double): Double {
        val numerator = -dsin(angle) - dsin(lat) * dsin(decl)
        val denominator = dcos(lat) * dcos(decl)
        val cosVal = (numerator / denominator).coerceIn(-0.9999, 0.9999)
        return darccos(cosVal) / 15.0
    }

    private fun asrAngleTime(factor: Double, lat: Double, decl: Double): Double {
        val angle = -darccot(factor + dtan(kotlin.math.abs(lat - decl)))
        return sunAngleTime(angle, lat, decl)
    }

    private fun dsin(d: Double) = sin(Math.toRadians(d))
    private fun dcos(d: Double) = cos(Math.toRadians(d))
    private fun dtan(d: Double) = tan(Math.toRadians(d))
    private fun darcsin(x: Double) = Math.toDegrees(asin(x))
    private fun darccos(x: Double) = Math.toDegrees(acos(x))
    private fun darctan2(y: Double, x: Double) = Math.toDegrees(atan2(y, x))
    private fun darccot(x: Double) = Math.toDegrees(atan(1.0 / x))

    private fun fixAngle(a: Double): Double {
        var res = a % 360.0
        if (res < 0) res += 360.0
        return res
    }

    private fun fixHour(h: Double): Double {
        var res = h % 24.0
        if (res < 0) res += 24.0
        return res
    }
}
