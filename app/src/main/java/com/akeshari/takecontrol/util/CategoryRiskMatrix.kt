package com.akeshari.takecontrol.util

import com.akeshari.takecontrol.data.model.AppCategory
import com.akeshari.takecontrol.data.model.PermissionGroup
import com.akeshari.takecontrol.data.model.RiskLevel

object CategoryRiskMatrix {

    private val EXPECTED_PERMISSIONS: Map<AppCategory, Set<PermissionGroup>> = mapOf(
        AppCategory.SOCIAL_MEDIA to setOf(
            PermissionGroup.CAMERA, PermissionGroup.MICROPHONE, PermissionGroup.CONTACTS,
            PermissionGroup.STORAGE, PermissionGroup.LOCATION, PermissionGroup.NETWORK
        ),
        AppCategory.COMMUNICATION to setOf(
            PermissionGroup.CAMERA, PermissionGroup.MICROPHONE, PermissionGroup.CONTACTS,
            PermissionGroup.PHONE, PermissionGroup.SMS, PermissionGroup.NETWORK
        ),
        AppCategory.ENTERTAINMENT to setOf(
            PermissionGroup.STORAGE, PermissionGroup.NETWORK
        ),
        AppCategory.GAMES to setOf(
            PermissionGroup.NETWORK, PermissionGroup.SENSORS, PermissionGroup.STORAGE
        ),
        AppCategory.SHOPPING to setOf(
            PermissionGroup.LOCATION, PermissionGroup.CAMERA, PermissionGroup.NETWORK,
            PermissionGroup.STORAGE
        ),
        AppCategory.FINANCE to setOf(
            PermissionGroup.CAMERA, PermissionGroup.NETWORK, PermissionGroup.SENSORS
        ),
        AppCategory.HEALTH to setOf(
            PermissionGroup.SENSORS, PermissionGroup.LOCATION, PermissionGroup.CAMERA,
            PermissionGroup.STORAGE, PermissionGroup.NETWORK
        ),
        AppCategory.EDUCATION to setOf(
            PermissionGroup.CAMERA, PermissionGroup.MICROPHONE, PermissionGroup.STORAGE,
            PermissionGroup.NETWORK
        ),
        AppCategory.PRODUCTIVITY to setOf(
            PermissionGroup.STORAGE, PermissionGroup.CALENDAR, PermissionGroup.CONTACTS,
            PermissionGroup.NETWORK
        ),
        AppCategory.UTILITIES to setOf(
            PermissionGroup.NETWORK, PermissionGroup.STORAGE
        ),
        AppCategory.OTHER to emptySet()
    )

    fun assessContextualRisk(
        staticRisk: RiskLevel,
        group: PermissionGroup,
        category: AppCategory
    ): RiskLevel {
        if (category == AppCategory.OTHER) return staticRisk

        val expected = EXPECTED_PERMISSIONS[category] ?: return staticRisk

        return if (group in expected) {
            RiskLevel.LOW
        } else {
            escalate(staticRisk)
        }
    }

    private fun escalate(risk: RiskLevel): RiskLevel = when (risk) {
        RiskLevel.LOW -> RiskLevel.MEDIUM
        RiskLevel.MEDIUM -> RiskLevel.HIGH
        RiskLevel.HIGH -> RiskLevel.CRITICAL
        RiskLevel.CRITICAL -> RiskLevel.CRITICAL
    }
}
