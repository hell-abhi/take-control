package com.akeshari.takecontrol.data.repository

import com.akeshari.takecontrol.data.model.AppPermissionInfo
import com.akeshari.takecontrol.data.scanner.PermissionScanner
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val scanner: PermissionScanner
) {
    private var cachedApps: List<AppPermissionInfo>? = null

    suspend fun getInstalledApps(forceRefresh: Boolean = false): List<AppPermissionInfo> {
        if (forceRefresh || cachedApps == null) {
            cachedApps = scanner.scanAllApps(includeSystemApps = true)
        }
        return cachedApps!!
    }

    suspend fun getAppByPackage(packageName: String): AppPermissionInfo? {
        return getInstalledApps().find { it.packageName == packageName }
    }

    fun clearCache() {
        cachedApps = null
    }
}
