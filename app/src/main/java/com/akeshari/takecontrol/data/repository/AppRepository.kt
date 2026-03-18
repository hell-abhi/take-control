package com.akeshari.takecontrol.data.repository

import com.akeshari.takecontrol.data.database.dao.PermissionHistoryDao
import com.akeshari.takecontrol.data.database.entity.PermissionChangeEntity
import com.akeshari.takecontrol.data.database.entity.PermissionSnapshotEntity
import com.akeshari.takecontrol.data.model.AppPermissionInfo
import com.akeshari.takecontrol.data.scanner.PermissionScanner
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val scanner: PermissionScanner,
    private val historyDao: PermissionHistoryDao
) {
    private var cachedApps: List<AppPermissionInfo>? = null

    suspend fun getInstalledApps(forceRefresh: Boolean = false): List<AppPermissionInfo> {
        if (forceRefresh || cachedApps == null) {
            val apps = scanner.scanAllApps(includeSystemApps = true)
            cachedApps = apps
            recordPermissionSnapshot(apps)
        }
        return cachedApps!!
    }

    suspend fun getAppByPackage(packageName: String): AppPermissionInfo? {
        return getInstalledApps().find { it.packageName == packageName }
    }

    fun clearCache() {
        cachedApps = null
    }

    suspend fun getRecentChanges(limit: Int = 50): List<PermissionChangeEntity> {
        return historyDao.getRecentChanges(limit)
    }

    suspend fun getChangesForApp(packageName: String, limit: Int = 20): List<PermissionChangeEntity> {
        return historyDao.getChangesForApp(packageName, limit)
    }

    private suspend fun recordPermissionSnapshot(apps: List<AppPermissionInfo>) {
        val previousSnapshots = historyDao.getAllSnapshots()
        val previousMap = previousSnapshots.associateBy { "${it.packageName}|${it.permission}" }
        val now = System.currentTimeMillis()
        val isFirstScan = previousSnapshots.isEmpty()

        val newSnapshots = mutableListOf<PermissionSnapshotEntity>()
        val changes = mutableListOf<PermissionChangeEntity>()

        for (app in apps) {
            for (perm in app.permissions) {
                val key = "${app.packageName}|${perm.permission}"
                val prev = previousMap[key]

                newSnapshots.add(
                    PermissionSnapshotEntity(
                        packageName = app.packageName,
                        permission = perm.permission,
                        isGranted = perm.isGranted,
                        lastUpdated = now
                    )
                )

                // Record change if state differs (skip first scan to avoid flooding)
                if (!isFirstScan && prev != null && prev.isGranted != perm.isGranted) {
                    changes.add(
                        PermissionChangeEntity(
                            packageName = app.packageName,
                            appName = app.appName,
                            permission = perm.permission,
                            permissionLabel = perm.label,
                            wasGranted = prev.isGranted,
                            isNowGranted = perm.isGranted,
                            detectedAt = now
                        )
                    )
                }
            }
        }

        historyDao.upsertSnapshots(newSnapshots)
        if (changes.isNotEmpty()) {
            historyDao.insertChanges(changes)
        }
    }
}
