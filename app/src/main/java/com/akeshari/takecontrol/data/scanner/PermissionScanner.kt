package com.akeshari.takecontrol.data.scanner

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.akeshari.takecontrol.data.model.AppCategory
import com.akeshari.takecontrol.data.model.AppPermissionInfo
import com.akeshari.takecontrol.data.model.PermissionDetail
import com.akeshari.takecontrol.util.PermissionClassifier
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val classifier: PermissionClassifier,
    private val trackerScanner: TrackerScanner
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

        val category = resolveCategory(packageInfo)

        val requestedPermissions = packageInfo.requestedPermissions ?: emptyArray()
        val requestedFlags = packageInfo.requestedPermissionsFlags ?: IntArray(requestedPermissions.size)
        val permissions = requestedPermissions.indices.mapNotNull { i ->
            val perm = requestedPermissions[i]
            // Filter out custom/auto-generated junk permissions
            if (shouldFilterPermission(perm)) return@mapNotNull null
            val isGranted = requestedFlags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0
            classifier.classify(perm, isGranted, category)
        }

        val trackers = trackerScanner.detectTrackers(packageInfo.packageName)
        val riskScore = calculateRiskScore(permissions)

        return AppPermissionInfo(
            packageName = packageInfo.packageName,
            appName = appName,
            icon = icon,
            isSystemApp = isSystemApp(packageInfo),
            permissions = permissions,
            riskScore = riskScore,
            category = category,
            trackers = trackers
        )
    }

    private fun resolveCategory(packageInfo: PackageInfo): AppCategory {
        // Layer 1: Android system category (API 26+)
        val systemCategory = packageInfo.applicationInfo?.category
        val fromSystem = when (systemCategory) {
            ApplicationInfo.CATEGORY_GAME -> AppCategory.GAMES
            ApplicationInfo.CATEGORY_AUDIO -> AppCategory.ENTERTAINMENT
            ApplicationInfo.CATEGORY_VIDEO -> AppCategory.ENTERTAINMENT
            ApplicationInfo.CATEGORY_IMAGE -> AppCategory.ENTERTAINMENT
            ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL_MEDIA
            ApplicationInfo.CATEGORY_NEWS -> AppCategory.ENTERTAINMENT
            ApplicationInfo.CATEGORY_MAPS -> AppCategory.UTILITIES
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.PRODUCTIVITY
            else -> null
        }
        if (fromSystem != null) return fromSystem

        // Layer 2: Package-name heuristic
        return resolveCategoryFromPackage(packageInfo.packageName)
    }

    private fun resolveCategoryFromPackage(packageName: String): AppCategory {
        val pkg = packageName.lowercase()
        return when {
            // Communication
            pkg.contains("whatsapp") || pkg.contains("telegram") || pkg.contains("signal") ||
            pkg.contains("messenger") || pkg.contains("viber") || pkg.contains("discord") ||
            pkg.contains("slack") || pkg.contains("skype") -> AppCategory.COMMUNICATION

            // Social media
            pkg.contains("instagram") || pkg.contains("facebook") || pkg.contains("twitter") ||
            pkg.contains("tiktok") || pkg.contains("snapchat") || pkg.contains("reddit") ||
            pkg.contains("linkedin") || pkg.contains("pinterest") -> AppCategory.SOCIAL_MEDIA

            // Finance
            pkg.contains("banking") || pkg.contains("bank") || pkg.contains("pay") ||
            pkg.contains("wallet") || pkg.contains("finance") || pkg.contains("gpay") ||
            pkg.contains("phonepe") || pkg.contains("paytm") -> AppCategory.FINANCE

            // Shopping
            pkg.contains("amazon") || pkg.contains("flipkart") || pkg.contains("myntra") ||
            pkg.contains("shopping") || pkg.contains("shop") || pkg.contains("swiggy") ||
            pkg.contains("zomato") || pkg.contains("blinkit") -> AppCategory.SHOPPING

            // Health
            pkg.contains("health") || pkg.contains("fitness") || pkg.contains("workout") ||
            pkg.contains("medical") || pkg.contains("yoga") -> AppCategory.HEALTH

            // Education
            pkg.contains("edu") || pkg.contains("learn") || pkg.contains("course") ||
            pkg.contains("study") || pkg.contains("school") || pkg.contains("duolingo") -> AppCategory.EDUCATION

            // Games
            pkg.contains("game") || pkg.contains("play.") && pkg.contains("games") -> AppCategory.GAMES

            // Entertainment
            pkg.contains("youtube") || pkg.contains("netflix") || pkg.contains("spotify") ||
            pkg.contains("music") || pkg.contains("video") || pkg.contains("player") ||
            pkg.contains("hotstar") || pkg.contains("prime") -> AppCategory.ENTERTAINMENT

            // Productivity
            pkg.contains("office") || pkg.contains("docs") || pkg.contains("sheets") ||
            pkg.contains("drive") || pkg.contains("notion") || pkg.contains("calendar") ||
            pkg.contains("notes") || pkg.contains("todo") -> AppCategory.PRODUCTIVITY

            // Utilities
            pkg.contains("calculator") || pkg.contains("clock") || pkg.contains("weather") ||
            pkg.contains("flashlight") || pkg.contains("compass") || pkg.contains("files") ||
            pkg.contains("cleaner") || pkg.contains("launcher") -> AppCategory.UTILITIES

            else -> AppCategory.OTHER
        }
    }

    private fun calculateRiskScore(permissions: List<PermissionDetail>): Int {
        if (permissions.isEmpty()) return 0
        val totalRisk = permissions
            .filter { it.isGranted }
            .sumOf { it.riskLevel.weight }
        // Normalize to 0-100
        return (totalRisk * 100 / (permissions.size * 10)).coerceIn(0, 100)
    }

    private fun shouldFilterPermission(permission: String): Boolean {
        // Filter auto-generated push token permissions, C2D_MESSAGE, app-specific custom permissions
        val p = permission.uppercase()
        if ("TOKEN" in p && !p.startsWith("ANDROID.PERMISSION")) return true
        if ("C2D_MESSAGE" in p) return true
        if ("DYNAMIC_RECEIVER_NOT_EXPORTED" in p) return true
        // Only keep android.permission.*, com.google.android.*, and well-known prefixes
        // Filter random app-specific permissions like com.someapp.permission.SOMETHING
        if (!permission.startsWith("android.permission.") &&
            !permission.startsWith("com.google.android.") &&
            !permission.startsWith("com.samsung.") &&
            permission.contains(".permission.")) {
            // Keep if it's a known pattern we handle
            val suffix = permission.substringAfterLast(".")
            if (suffix.length > 30) return true // random hash/token strings
        }
        return false
    }

    private fun isSystemApp(packageInfo: PackageInfo): Boolean {
        return packageInfo.applicationInfo?.let {
            it.flags and ApplicationInfo.FLAG_SYSTEM != 0
        } ?: false
    }
}
