package com.akeshari.takecontrol.data.scanner

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import com.akeshari.takecontrol.data.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val daysSinceUsed: Int,
    val foregroundMinutesToday: Long,
    val dangerousPermissions: List<String>,
    val trackerCount: Int,
    val riskScore: Int
)

@Singleton
class ActivityMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

    fun hasUsagePermission(): Boolean {
        val mode = appOpsManager.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    suspend fun getZombieApps(apps: List<AppPermissionInfo>): List<ZombieApp> = withContext(Dispatchers.IO) {
        if (!hasUsagePermission()) return@withContext emptyList()

        val lastUsedMap = getLastUsedMap()
        val now = System.currentTimeMillis()
        val dangerousGroups = setOf(
            PermissionGroup.LOCATION, PermissionGroup.CAMERA, PermissionGroup.MICROPHONE,
            PermissionGroup.CONTACTS, PermissionGroup.SMS, PermissionGroup.PHONE
        )

        apps.filter { !it.isSystemApp }.mapNotNull { app ->
            val lastUsed = lastUsedMap[app.packageName] ?: 0
            val daysSince = if (lastUsed > 0) ((now - lastUsed) / 86400000).toInt() else -1
            if (daysSince < 30 && daysSince != -1) return@mapNotNull null

            val dangerousPerms = app.permissions
                .filter { it.isGranted && it.group in dangerousGroups }
                .map { it.label }
                .distinct()

            if (dangerousPerms.isEmpty()) return@mapNotNull null

            ZombieApp(
                packageName = app.packageName,
                appName = app.appName,
                daysSinceUsed = if (daysSince == -1) 999 else daysSince,
                dangerousPermissions = dangerousPerms
            )
        }.sortedByDescending { it.dangerousPermissions.size }
    }

    suspend fun getAppUsageAnalysis(apps: List<AppPermissionInfo>): List<AppUsageInfo> = withContext(Dispatchers.IO) {
        if (!hasUsagePermission()) return@withContext emptyList()

        val lastUsedMap = getLastUsedMap()
        val todayUsageMap = getTodayUsageMap()
        val now = System.currentTimeMillis()
        val dangerousGroups = setOf(
            PermissionGroup.LOCATION, PermissionGroup.CAMERA, PermissionGroup.MICROPHONE,
            PermissionGroup.CONTACTS, PermissionGroup.SMS, PermissionGroup.PHONE
        )

        apps.filter { !it.isSystemApp }.map { app ->
            val lastUsed = lastUsedMap[app.packageName] ?: 0
            val daysSince = if (lastUsed > 0) ((now - lastUsed) / 86400000).toInt() else -1
            val fgToday = todayUsageMap[app.packageName] ?: 0

            val dangerousPerms = app.permissions
                .filter { it.isGranted && it.group in dangerousGroups }
                .map { it.label }
                .distinct()

            AppUsageInfo(
                packageName = app.packageName,
                appName = app.appName,
                daysSinceUsed = if (daysSince == -1) 999 else daysSince,
                foregroundMinutesToday = fgToday / 60000,
                dangerousPermissions = dangerousPerms,
                trackerCount = app.trackers.size,
                riskScore = app.riskScore
            )
        }
        // Sort: most dangerous first (high permissions + high trackers + low usage)
        .filter { it.dangerousPermissions.isNotEmpty() || it.trackerCount > 0 }
        .sortedByDescending { it.dangerousPermissions.size * 10 + it.trackerCount * 5 - (it.foregroundMinutesToday * 2).toInt().coerceAtMost(20) }
    }

    private fun getLastUsedMap(): Map<String, Long> {
        val now = System.currentTimeMillis()
        val sixtyDaysAgo = now - 60L * 86400000
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_MONTHLY, sixtyDaysAgo, now)
        val map = mutableMapOf<String, Long>()
        for (stat in stats) {
            val existing = map[stat.packageName] ?: 0
            if (stat.lastTimeUsed > existing) map[stat.packageName] = stat.lastTimeUsed
        }
        return map
    }

    private fun getTodayUsageMap(): Map<String, Long> {
        val now = System.currentTimeMillis()
        val startOfDay = now - (now % 86400000)
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDay, now)
        val map = mutableMapOf<String, Long>()
        for (stat in stats) {
            val existing = map[stat.packageName] ?: 0
            map[stat.packageName] = existing + stat.totalTimeInForeground
        }
        return map
    }
}
