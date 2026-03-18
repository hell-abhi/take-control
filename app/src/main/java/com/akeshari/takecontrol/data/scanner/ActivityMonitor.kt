package com.akeshari.takecontrol.data.scanner

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import com.akeshari.takecontrol.data.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class PermissionGroupAccess(
    val groupName: String,
    val icon: com.akeshari.takecontrol.data.model.PermissionGroup
)

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val lastOpened: String,
    val lastOpenedDays: Int,        // -1 = never, 0 = today
    val weeklyMinutes: Long,
    val permissionGroups: List<PermissionGroupAccess>, // Location, Camera, etc.
    val dangerousPermissions: List<String>,
    val trackerCount: Int,
    val riskScore: Int,
    val exposureRatio: Float
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

    suspend fun analyze(apps: List<AppPermissionInfo>): List<AppUsageInfo> = withContext(Dispatchers.IO) {
        if (!hasUsagePermission()) return@withContext emptyList()

        val now = System.currentTimeMillis()
        val lastUsedMap = getLastUsedMap()
        val weeklyUsageMap = getWeeklyUsageMap()

        val dangerousGroups = setOf(
            PermissionGroup.LOCATION, PermissionGroup.CAMERA, PermissionGroup.MICROPHONE,
            PermissionGroup.CONTACTS, PermissionGroup.SMS, PermissionGroup.PHONE
        )

        apps.filter { !it.isSystemApp }.mapNotNull { app ->
            val grantedDangerous = app.permissions.filter { it.isGranted && it.group in dangerousGroups }
            val dangerousPerms = grantedDangerous.map { it.label }.distinct()
            val permGroups = grantedDangerous.map { it.group }.distinct()
                .map { PermissionGroupAccess(it.label.replace("Your ", ""), it) }

            // Skip apps with no privacy surface
            if (dangerousPerms.isEmpty() && app.trackers.isEmpty()) return@mapNotNull null

            val lastUsed = lastUsedMap[app.packageName] ?: 0
            val daysSince = if (lastUsed > 0) ((now - lastUsed) / 86400000).toInt() else -1
            val weeklyMin = (weeklyUsageMap[app.packageName] ?: 0) / 60000

            // Exposure ratio: how much privacy cost per minute of actual use
            val privacyCost = dangerousPerms.size * 3f + app.trackers.size * 2f
            val usageValue = (weeklyMin + 1f).coerceAtLeast(1f) // avoid div by 0
            val ratio = privacyCost / usageValue

            AppUsageInfo(
                packageName = app.packageName,
                appName = app.appName,
                lastOpened = formatLastOpened(daysSince),
                lastOpenedDays = daysSince,
                weeklyMinutes = weeklyMin,
                permissionGroups = permGroups,
                dangerousPermissions = dangerousPerms,
                trackerCount = app.trackers.size,
                riskScore = app.riskScore,
                exposureRatio = ratio
            )
        }
    }

    private fun getLastUsedMap(): Map<String, Long> {
        val now = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_MONTHLY, now - 90L * 86400000, now)
        val map = mutableMapOf<String, Long>()
        for (stat in stats) {
            val existing = map[stat.packageName] ?: 0
            if (stat.lastTimeUsed > existing) map[stat.packageName] = stat.lastTimeUsed
        }
        return map
    }

    private fun getWeeklyUsageMap(): Map<String, Long> {
        val now = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_WEEKLY, now - 7L * 86400000, now)
        val map = mutableMapOf<String, Long>()
        for (stat in stats) {
            map[stat.packageName] = (map[stat.packageName] ?: 0) + stat.totalTimeInForeground
        }
        return map
    }

    private fun formatLastOpened(days: Int): String = when {
        days == -1 -> "Never opened"
        days == 0 -> "Today"
        days == 1 -> "Yesterday"
        days < 7 -> "$days days ago"
        days < 30 -> "${days / 7} weeks ago"
        days < 365 -> "${days / 30} months ago"
        else -> "Over a year ago"
    }
}
