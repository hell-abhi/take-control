package com.akeshari.privacyguardian.data.scanner

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.akeshari.privacyguardian.data.model.AppPermissionInfo
import com.akeshari.privacyguardian.data.model.PermissionDetail
import com.akeshari.privacyguardian.util.PermissionClassifier
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val classifier: PermissionClassifier
) {
    private val packageManager: PackageManager = context.packageManager

    suspend fun scanAllApps(includeSystemApps: Boolean = false): List<AppPermissionInfo> =
        withContext(Dispatchers.IO) {
            val packages = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)

            packages
                .filter { pkg ->
                    includeSystemApps || !isSystemApp(pkg)
                }
                .map { pkg -> buildAppInfo(pkg) }
                .sortedByDescending { it.riskScore }
        }

    private fun buildAppInfo(packageInfo: PackageInfo): AppPermissionInfo {
        val appName = packageInfo.applicationInfo
            ?.loadLabel(packageManager)?.toString() ?: packageInfo.packageName

        val icon = try {
            packageInfo.applicationInfo?.loadIcon(packageManager)
        } catch (_: Exception) {
            null
        }

        val requestedPermissions = packageInfo.requestedPermissions ?: emptyArray()
        val requestedFlags = packageInfo.requestedPermissionsFlags ?: IntArray(requestedPermissions.size)
        val permissions = requestedPermissions.indices.map { i ->
            val isGranted = requestedFlags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0
            classifier.classify(requestedPermissions[i], isGranted)
        }

        val riskScore = calculateRiskScore(permissions)

        return AppPermissionInfo(
            packageName = packageInfo.packageName,
            appName = appName,
            icon = icon,
            isSystemApp = isSystemApp(packageInfo),
            permissions = permissions,
            riskScore = riskScore
        )
    }

    private fun calculateRiskScore(permissions: List<PermissionDetail>): Int {
        if (permissions.isEmpty()) return 0
        val totalRisk = permissions
            .filter { it.isGranted }
            .sumOf { it.riskLevel.weight }
        // Normalize to 0-100
        return (totalRisk * 100 / (permissions.size * 10)).coerceIn(0, 100)
    }

    private fun isSystemApp(packageInfo: PackageInfo): Boolean {
        return packageInfo.applicationInfo?.let {
            it.flags and ApplicationInfo.FLAG_SYSTEM != 0
        } ?: false
    }
}
