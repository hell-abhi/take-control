package com.akeshari.takecontrol.data.model

data class PrivacyScore(
    val total: Int,
    val deductions: List<ScoreDeduction>,
    val bonus: ScoreBonus
)

data class ScoreDeduction(
    val group: PermissionGroup,
    val appCount: Int,
    val pointsLost: Int,
    val description: String
)

data class ScoreBonus(
    val deniedCount: Int,
    val pointsGained: Int
)

object PrivacyScoreCalculator {

    // Points deducted for the FIRST app in a group, then diminishing returns
    // This reflects that the first app with mic access is alarming,
    // but the 10th one doesn't make things 10x worse
    private val BASE_DEDUCTION = mapOf(
        PermissionGroup.MICROPHONE to 8,
        PermissionGroup.SMS to 8,
        PermissionGroup.LOCATION to 7,
        PermissionGroup.PHONE to 5,
        PermissionGroup.CONTACTS to 4,
        PermissionGroup.CAMERA to 4,
        PermissionGroup.SENSORS to 3,
        PermissionGroup.STORAGE to 2,
        PermissionGroup.CALENDAR to 1,
        PermissionGroup.NETWORK to 0,
        PermissionGroup.OTHER to 0
    )

    private const val BONUS_PER_DENIAL = 1
    private const val MAX_BONUS = 20
    private const val MAX_DEDUCTION_PER_GROUP = 15
    private const val MAX_TOTAL_DEDUCTION = 85

    fun calculate(apps: List<AppPermissionInfo>): PrivacyScore {
        val deductions = mutableListOf<ScoreDeduction>()

        for (group in PermissionGroup.entries) {
            val base = BASE_DEDUCTION[group] ?: 0
            if (base == 0) continue

            val appsWithGranted = apps.count { app ->
                app.permissions.any { it.group == group && it.isGranted }
            }
            if (appsWithGranted == 0) continue

            // Diminishing returns: first app costs full base, each additional costs less
            // e.g. base=8: 1 app=8, 2 apps=11, 3 apps=13, 5 apps=15(cap)
            val rawPoints = (base + (appsWithGranted - 1) * (base * 0.4)).toInt()
            val points = rawPoints.coerceAtMost(MAX_DEDUCTION_PER_GROUP)

            deductions.add(
                ScoreDeduction(
                    group = group,
                    appCount = appsWithGranted,
                    pointsLost = points,
                    description = "${appsWithGranted} apps ${group.description.lowercase()}"
                )
            )
        }

        // Sort by points lost descending
        deductions.sortByDescending { it.pointsLost }

        // Bonus for denied permissions
        val deniedCount = apps.sumOf { app ->
            app.permissions.count { !it.isGranted }
        }
        val bonusPoints = (deniedCount * BONUS_PER_DENIAL).coerceAtMost(MAX_BONUS)
        val bonus = ScoreBonus(deniedCount = deniedCount, pointsGained = bonusPoints)

        val totalDeductions = deductions.sumOf { it.pointsLost }.coerceAtMost(MAX_TOTAL_DEDUCTION)
        val total = (100 - totalDeductions + bonusPoints).coerceIn(0, 100)

        return PrivacyScore(
            total = total,
            deductions = deductions,
            bonus = bonus
        )
    }
}
