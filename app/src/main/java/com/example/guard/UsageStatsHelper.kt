package com.example.guard

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import com.example.data.model.AppUsageStat
import java.util.Calendar

data class DailyUsageReport(
    val hasPermission: Boolean,
    val totalMinutes: Int,
    val ffMinutesToday: Int = 0,
    val topApps: List<AppUsageStat>
) {
    fun formattedSummary(): String {
        val hrs = totalMinutes / 60
        val mins = totalMinutes % 60
        val totalPart = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
        val topPart = topApps.take(2).joinToString(", ") { stat ->
            val ah = stat.minutesToday / 60
            val am = stat.minutesToday % 60
            val t = if (ah > 0) "${ah}h ${am}m" else "${am}m"
            "${stat.appName} $t"
        }
        return if (topPart.isBlank()) "Total $totalPart" else "Total $totalPart - $topPart"
    }
}

object UsageStatsHelper {

    private val friendlyNames = mapOf(
        "com.facebook.katana" to "Facebook",
        "com.facebook.lite" to "Facebook Lite",
        "com.google.android.youtube" to "YouTube",
        "com.instagram.android" to "Instagram",
        "com.zhiliaoapp.musically" to "TikTok",
        "com.ss.android.ugc.trill" to "TikTok",
        "com.android.chrome" to "Chrome",
        "com.whatsapp" to "WhatsApp",
        "com.dts.freefireth" to "Free Fire",
        "com.dts.freefiremax" to "Free Fire MAX"
    )

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getTodayUsageReport(context: Context): DailyUsageReport {
        if (!hasUsageStatsPermission(context)) {
            return DailyUsageReport(
                hasPermission = false,
                totalMinutes = 0,
                topApps = emptyList()
            )
        }

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        val end = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end).orEmpty()

        val pm = context.packageManager
        val merged = mutableMapOf<String, Long>()
        for (item in stats) {
            if (item.packageName == context.packageName) continue
            val fgMs = item.totalTimeInForeground
            if (fgMs > 60_000L) {
                merged[item.packageName] = (merged[item.packageName] ?: 0L) + fgMs
            }
        }

        val appStats = merged.entries
            .sortedByDescending { it.value }
            .map { (pkg, ms) ->
                val label = friendlyNames[pkg] ?: runCatching {
                    val info = pm.getApplicationInfo(pkg, 0)
                    pm.getApplicationLabel(info).toString()
                }.getOrElse { pkg.substringAfterLast('.') }
                AppUsageStat(
                    packageName = pkg,
                    appName = label,
                    minutesToday = (ms / 60_000L).toInt()
                )
            }

        val totalMinutes = appStats.sumOf { it.minutesToday }
        val ffMinutes = appStats
            .filter { it.packageName == "com.dts.freefireth" || it.packageName == "com.dts.freefiremax" }
            .sumOf { it.minutesToday }
        return DailyUsageReport(
            hasPermission = true,
            totalMinutes = totalMinutes,
            ffMinutesToday = ffMinutes,
            topApps = appStats
        )
    }

    fun isKnownAppInstalled(context: Context, packageName: String): Boolean {
        return runCatching {
            context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            true
        }.getOrElse {
            runCatching {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
                true
            }.getOrDefault(false)
        }
    }
}
