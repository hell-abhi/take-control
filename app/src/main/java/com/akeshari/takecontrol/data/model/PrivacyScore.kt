package com.akeshari.takecontrol.data.model

data class PrivacyScore(
    val total: Int,           // composite: 60% permission + 40% tracker
    val permissionScore: Int, // 0-100: % of sensitive risk avoided
    val trackerScore: Int,    // 0-100: lower tracker exposure = higher
    val riskGranted: Int,
    val riskDenied: Int,
    val riskTotal: Int,
    val groupBreakdowns: List<GroupBreakdown>
)

data class GroupBreakdown(
    val group: PermissionGroup,
    val appsGranted: Int,
    val appsDenied: Int,
    val riskGranted: Int,
    val riskDenied: Int,
    val pointsRecoverable: Int
)

data class CompanyOverview(
    val companyName: String,
    val appCount: Int
)

object PrivacyScoreCalculator {

    private val RISK_WEIGHT = mapOf(
        PermissionGroup.MICROPHONE to 10,
        PermissionGroup.SMS to 10,
        PermissionGroup.LOCATION to 8,
        PermissionGroup.PHONE to 7,
        PermissionGroup.CONTACTS to 6,
        PermissionGroup.CAMERA to 6,
        PermissionGroup.SENSORS to 4,
        PermissionGroup.STORAGE to 3,
        PermissionGroup.CALENDAR to 2,
        PermissionGroup.NETWORK to 0,
        PermissionGroup.OTHER to 0
    )

    private val TRACKER_SEVERITY = mapOf(
        TrackerCategory.ADVERTISING to 3.0,
        TrackerCategory.PROFILING to 3.0,
        TrackerCategory.SOCIAL to 2.0,
        TrackerCategory.ANALYTICS to 1.0,
        TrackerCategory.CRASH_REPORTING to 0.5,
        TrackerCategory.IDENTIFICATION to 0.5
    )

    fun calculate(apps: List<AppPermissionInfo>): PrivacyScore {
        val permissionScore = calculatePermissionScore(apps)
        val trackerScore = calculateTrackerScore(apps)

        // Composite: 60% permissions (user-controllable) + 40% trackers
        val composite = ((permissionScore.first * 0.6) + (trackerScore * 0.4)).toInt().coerceIn(0, 100)

        return PrivacyScore(
            total = composite,
            permissionScore = permissionScore.first,
            trackerScore = trackerScore,
            riskGranted = permissionScore.second,
            riskDenied = permissionScore.third,
            riskTotal = permissionScore.second + permissionScore.third,
            groupBreakdowns = permissionScore.fourth
        )
    }

    private fun calculatePermissionScore(apps: List<AppPermissionInfo>): Quadruple {
        var totalRiskGranted = 0
        var totalRiskDenied = 0
        val groupBreakdowns = mutableListOf<GroupBreakdown>()

        for (group in PermissionGroup.entries) {
            val weight = RISK_WEIGHT[group] ?: 0
            if (weight == 0) continue

            var grantedCount = 0
            var deniedCount = 0

            for (app in apps) {
                val permsInGroup = app.permissions.filter { it.group == group }
                if (permsInGroup.isEmpty()) continue
                val hasGranted = permsInGroup.any { it.isGranted }
                val hasDenied = permsInGroup.any { !it.isGranted }
                if (hasGranted) grantedCount++
                if (hasDenied && !hasGranted) deniedCount++
            }

            val groupRiskGranted = grantedCount * weight
            val groupRiskDenied = deniedCount * weight
            totalRiskGranted += groupRiskGranted
            totalRiskDenied += groupRiskDenied

            if (grantedCount > 0 || deniedCount > 0) {
                groupBreakdowns.add(
                    GroupBreakdown(group, grantedCount, deniedCount, groupRiskGranted, groupRiskDenied, 0)
                )
            }
        }

        val totalRisk = totalRiskGranted + totalRiskDenied
        val score = if (totalRisk > 0) (totalRiskDenied * 100) / totalRisk else 100

        val finalBreakdowns = groupBreakdowns.map { breakdown ->
            if (totalRisk > 0) {
                val newDenied = totalRiskDenied + breakdown.riskGranted
                val newScore = (newDenied * 100) / totalRisk
                breakdown.copy(pointsRecoverable = newScore - score)
            } else breakdown
        }.filter { it.appsGranted > 0 }.sortedByDescending { it.pointsRecoverable }

        return Quadruple(score.coerceIn(0, 100), totalRiskGranted, totalRiskDenied, finalBreakdowns)
    }

    private fun calculateTrackerScore(apps: List<AppPermissionInfo>): Int {
        if (apps.isEmpty()) return 100

        var totalExposure = 0.0
        val maxPossiblePerApp = TRACKER_SEVERITY.values.max() * 3 // assume worst: 3 high-severity trackers per app

        for (app in apps) {
            for (tracker in app.trackers) {
                totalExposure += TRACKER_SEVERITY[tracker.category] ?: 1.0
            }
        }

        val maxExposure = apps.size * maxPossiblePerApp
        if (maxExposure <= 0) return 100

        val exposureRatio = (totalExposure / maxExposure).coerceIn(0.0, 1.0)
        return ((1.0 - exposureRatio) * 100).toInt().coerceIn(0, 100)
    }

    fun getCompanyOverviews(apps: List<AppPermissionInfo>): List<CompanyOverview> {
        val companyApps = mutableMapOf<String, MutableSet<String>>()
        for (app in apps) {
            for (tracker in app.trackers) {
                val company = com.akeshari.takecontrol.util.TrackerCompanies.getCompany(tracker.name)
                companyApps.getOrPut(company) { mutableSetOf() }.add(app.packageName)
            }
        }
        return companyApps.map { (name, pkgs) -> CompanyOverview(name, pkgs.size) }
            .sortedByDescending { it.appCount }
    }
}

// Helper to return 4 values from permission calculation
private data class Quadruple(
    val first: Int,
    val second: Int,
    val third: Int,
    val fourth: List<GroupBreakdown>
)
