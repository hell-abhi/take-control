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

    private val DEDUCTION_PER_APP = mapOf(
        PermissionGroup.MICROPHONE to 12,
        PermissionGroup.SMS to 12,
        PermissionGroup.LOCATION to 10,
        PermissionGroup.PHONE to 8,
        PermissionGroup.CONTACTS to 6,
        PermissionGroup.CAMERA to 5,
        PermissionGroup.SENSORS to 4,
        PermissionGroup.STORAGE to 3,
        PermissionGroup.CALENDAR to 2,
        PermissionGroup.NETWORK to 0,
        PermissionGroup.OTHER to 0
    )

    private const val BONUS_PER_DENIAL = 1
    private const val MAX_DEDUCTION_PER_GROUP = 30

    fun calculate(apps: List<AppPermissionInfo>): PrivacyScore {
        val deductions = mutableListOf<ScoreDeduction>()

        for (group in PermissionGroup.entries) {
            val perApp = DEDUCTION_PER_APP[group] ?: 0
            if (perApp == 0) continue

            val appsWithGranted = apps.count { app ->
                app.permissions.any { it.group == group && it.isGranted }
            }
            if (appsWithGranted == 0) continue

            val points = (appsWithGranted * perApp).coerceAtMost(MAX_DEDUCTION_PER_GROUP)

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
        val bonusPoints = (deniedCount * BONUS_PER_DENIAL).coerceAtMost(15)
        val bonus = ScoreBonus(deniedCount = deniedCount, pointsGained = bonusPoints)

        val totalDeductions = deductions.sumOf { it.pointsLost }
        val total = (100 - totalDeductions + bonusPoints).coerceIn(0, 100)

        return PrivacyScore(
            total = total,
            deductions = deductions,
            bonus = bonus
        )
    }
}
