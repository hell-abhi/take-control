package com.akeshari.takecontrol.data.model

import android.graphics.drawable.Drawable

data class AppPermissionInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isSystemApp: Boolean,
    val permissions: List<PermissionDetail>,
    val riskScore: Int // 0-100
)

data class PermissionDetail(
    val permission: String,
    val label: String,
    val group: PermissionGroup,
    val isGranted: Boolean,
    val riskLevel: RiskLevel
)

enum class RiskLevel(val weight: Int) {
    LOW(1),
    MEDIUM(3),
    HIGH(5),
    CRITICAL(10)
}

enum class AppCategory(val label: String) {
    SOCIAL_MEDIA("Social Media"),
    GAMES("Games"),
    SHOPPING("Shopping"),
    FINANCE("Finance & Banking"),
    UTILITIES("Utilities"),
    HEALTH("Health & Fitness"),
    EDUCATION("Education"),
    COMMUNICATION("Communication"),
    ENTERTAINMENT("Entertainment"),
    PRODUCTIVITY("Productivity"),
    OTHER("Other")
}
