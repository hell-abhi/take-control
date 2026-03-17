package com.akeshari.takecontrol.data.model

data class PrivacyScore(
    val total: Int,
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
    val pointsRecoverable: Int // how many score points user gains by revoking all in this group
)

object PrivacyScoreCalculator {

    // Risk weight per permission instance, by group
    // These reflect real-world privacy severity
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

    fun calculate(apps: List<AppPermissionInfo>): PrivacyScore {
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

                // Count this app once per group (not per individual permission)
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
                val groupTotal = groupRiskGranted + groupRiskDenied
                // How many score points would the user recover by revoking all granted in this group?
                // This is calculated after we know the total, so we add it below
                groupBreakdowns.add(
                    GroupBreakdown(
                        group = group,
                        appsGranted = grantedCount,
                        appsDenied = deniedCount,
                        riskGranted = groupRiskGranted,
                        riskDenied = groupRiskDenied,
                        pointsRecoverable = 0 // placeholder, calculated below
                    )
                )
            }
        }

        val totalRisk = totalRiskGranted + totalRiskDenied
        val score = if (totalRisk > 0) {
            (totalRiskDenied * 100) / totalRisk
        } else {
            100 // no sensitive permissions at all = perfect
        }

        // Now calculate recoverable points per group
        val finalBreakdowns = groupBreakdowns.map { breakdown ->
            if (totalRisk > 0) {
                // If user revoked all granted in this group, how much would the score increase?
                val newGranted = totalRiskGranted - breakdown.riskGranted
                val newDenied = totalRiskDenied + breakdown.riskGranted
                val newScore = (newDenied * 100) / totalRisk
                breakdown.copy(pointsRecoverable = newScore - score)
            } else {
                breakdown
            }
        }.filter { it.appsGranted > 0 } // only show groups where there's something to fix
            .sortedByDescending { it.pointsRecoverable }

        return PrivacyScore(
            total = score.coerceIn(0, 100),
            riskGranted = totalRiskGranted,
            riskDenied = totalRiskDenied,
            riskTotal = totalRisk,
            groupBreakdowns = finalBreakdowns
        )
    }
}
