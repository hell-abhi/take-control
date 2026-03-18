package com.akeshari.takecontrol.data.scanner

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.akeshari.takecontrol.data.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    private val packageManager = context.packageManager

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

        val now = System.currentTimeMillis()
        val sixtyDaysAgo = now - 60L * 24 * 60 * 60 * 1000
        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_MONTHLY,
            sixtyDaysAgo,
            now
        )

        val lastUsedMap = mutableMapOf<String, Long>()
        for (stat in usageStats) {
            val existing = lastUsedMap[stat.packageName] ?: 0
            if (stat.lastTimeUsed > existing) {
                lastUsedMap[stat.packageName] = stat.lastTimeUsed
            }
        }

        val dangerousGroups = setOf(
            PermissionGroup.LOCATION, PermissionGroup.CAMERA, PermissionGroup.MICROPHONE,
            PermissionGroup.CONTACTS, PermissionGroup.SMS, PermissionGroup.PHONE
        )

        apps.filter { !it.isSystemApp }.mapNotNull { app ->
            val lastUsed = lastUsedMap[app.packageName] ?: 0
            val daysSince = if (lastUsed > 0) ((now - lastUsed) / (24 * 60 * 60 * 1000)).toInt() else -1

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

    suspend fun getRecentAccesses(apps: List<AppPermissionInfo>): List<PermissionAccessRecord> = withContext(Dispatchers.IO) {
        val records = mutableListOf<PermissionAccessRecord>()
        val opStrings = listOf(
            AppOpsManager.OPSTR_FINE_LOCATION to "Location",
            AppOpsManager.OPSTR_CAMERA to "Camera",
            AppOpsManager.OPSTR_RECORD_AUDIO to "Microphone",
            AppOpsManager.OPSTR_READ_CONTACTS to "Contacts",
            AppOpsManager.OPSTR_COARSE_LOCATION to "Approx Location"
        )

        for (app in apps) {
            if (app.isSystemApp) continue
            val uid = getUid(app.packageName)
            if (uid == -1) continue

            for ((opStr, label) in opStrings) {
                try {
                    val result = getLastAccessViaReflection(uid, app.packageName, opStr)
                    if (result.first > 0) {
                        records.add(PermissionAccessRecord(
                            packageName = app.packageName,
                            appName = app.appName,
                            permission = label,
                            lastAccessTime = result.first,
                            isBackground = result.second
                        ))
                    }
                } catch (_: Exception) {}
            }
        }

        records.sortedByDescending { it.lastAccessTime }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getLastAccessViaReflection(uid: Int, packageName: String, opStr: String): Pair<Long, Boolean> {
        try {
            // getOpsForPackage returns List<PackageOps> but PackageOps is hidden in SDK 35
            val method = appOpsManager.javaClass.getMethod(
                "getOpsForPackage", Int::class.javaPrimitiveType, String::class.java, Array<String>::class.java
            )
            val pkgOpsList = method.invoke(appOpsManager, uid, packageName, arrayOf(opStr)) as? List<*>
                ?: return Pair(0L, false)

            for (pkgOps in pkgOpsList) {
                if (pkgOps == null) continue
                // Call getOps() on PackageOps
                val getOpsMethod = pkgOps.javaClass.getMethod("getOps")
                val opEntries = getOpsMethod.invoke(pkgOps) as? List<*> ?: continue

                for (opEntry in opEntries) {
                    if (opEntry == null) continue
                    val clazz = opEntry.javaClass

                    // Try API 30+: foreground/background split
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        try {
                            val fgM = clazz.getMethod("getLastAccessForegroundTime", Int::class.javaPrimitiveType)
                            val bgM = clazz.getMethod("getLastAccessBackgroundTime", Int::class.javaPrimitiveType)
                            val fg = fgM.invoke(opEntry, 0xFFFF) as Long
                            val bg = bgM.invoke(opEntry, 0xFFFF) as Long
                            if (fg > 0 || bg > 0) return Pair(maxOf(fg, bg), bg > fg)
                        } catch (_: Exception) {}
                    }

                    // Try API 29: getLastAccessTime(int)
                    try {
                        val m = clazz.getMethod("getLastAccessTime", Int::class.javaPrimitiveType)
                        val t = m.invoke(opEntry, 0xFFFF) as Long
                        if (t > 0) return Pair(t, false)
                    } catch (_: Exception) {}

                    // Try legacy: getTime()
                    try {
                        val m = clazz.getMethod("getTime")
                        val t = m.invoke(opEntry) as Long
                        if (t > 0) return Pair(t, false)
                    } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}

        return Pair(0L, false)
    }

    suspend fun generateAlerts(
        apps: List<AppPermissionInfo>,
        zombies: List<ZombieApp>,
        recentAccesses: List<PermissionAccessRecord>
    ): List<PrivacyAlert> = withContext(Dispatchers.IO) {
        val alerts = mutableListOf<PrivacyAlert>()
        val now = System.currentTimeMillis()
        val twentyFourHours = 24 * 60 * 60 * 1000L

        // Zombie alerts
        zombies.take(5).forEach { zombie ->
            val daysText = if (zombie.daysSinceUsed >= 999) "Never opened" else "Unused for ${zombie.daysSinceUsed} days"
            alerts.add(PrivacyAlert(
                type = AlertType.ZOMBIE,
                appName = zombie.appName,
                packageName = zombie.packageName,
                title = "Zombie App",
                description = "$daysText — still has: ${zombie.dangerousPermissions.joinToString(", ")}",
                severity = RiskLevel.HIGH
            ))
        }

        // Background spy alerts
        recentAccesses
            .filter { it.isBackground && (now - it.lastAccessTime) < twentyFourHours }
            .groupBy { it.packageName }
            .forEach { (pkg, accesses) ->
                val appName = accesses.first().appName
                val perms = accesses.map { it.permission }.distinct().joinToString(", ")
                alerts.add(PrivacyAlert(
                    type = AlertType.BACKGROUND_SPY,
                    appName = appName,
                    packageName = pkg,
                    title = "Background Access",
                    description = "Accessed $perms in the background in the last 24 hours",
                    severity = RiskLevel.CRITICAL
                ))
            }

        // Night crawler alerts
        val cal = Calendar.getInstance()
        recentAccesses
            .filter { (now - it.lastAccessTime) < twentyFourHours }
            .filter {
                cal.timeInMillis = it.lastAccessTime
                cal.get(Calendar.HOUR_OF_DAY) in 0..5
            }
            .groupBy { it.packageName }
            .forEach { (pkg, accesses) ->
                val appName = accesses.first().appName
                val perms = accesses.map { it.permission }.distinct().joinToString(", ")
                cal.timeInMillis = accesses.first().lastAccessTime
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                val minute = cal.get(Calendar.MINUTE)
                alerts.add(PrivacyAlert(
                    type = AlertType.NIGHT_CRAWLER,
                    appName = appName,
                    packageName = pkg,
                    title = "Night Access",
                    description = "Accessed $perms at ${"%02d:%02d".format(hour, minute)} while you were likely asleep",
                    severity = RiskLevel.HIGH
                ))
            }

        // Deduplicate by package (keep highest severity)
        alerts.groupBy { it.packageName }.map { (_, appAlerts) ->
            appAlerts.maxByOrNull { it.severity.weight }!!
        }.sortedByDescending { it.severity.weight }
    }

    private fun getUid(packageName: String): Int {
        return try {
            packageManager.getApplicationInfo(packageName, 0).uid
        } catch (_: PackageManager.NameNotFoundException) {
            -1
        }
    }
}
