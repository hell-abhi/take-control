package com.akeshari.takecontrol.data.model

data class ZombieApp(
    val packageName: String,
    val appName: String,
    val daysSinceUsed: Int,
    val dangerousPermissions: List<String> // human-readable permission labels
)

data class PermissionAccessRecord(
    val packageName: String,
    val appName: String,
    val permission: String,
    val lastAccessTime: Long, // millis, 0 = never
    val isBackground: Boolean
)

data class AppAccessSummary(
    val packageName: String,
    val appName: String,
    val locationAccesses: Int,
    val cameraAccesses: Int,
    val microphoneAccesses: Int,
    val lastLocationTime: Long,
    val lastCameraTime: Long,
    val lastMicTime: Long,
    val backgroundLocationAccesses: Int
)

enum class AlertType {
    ZOMBIE,          // not used in 30+ days but holds dangerous permissions
    BACKGROUND_SPY,  // accessed mic/camera/location in background recently
    NIGHT_CRAWLER    // accessed permissions between 12AM-6AM
}

data class PrivacyAlert(
    val type: AlertType,
    val appName: String,
    val packageName: String,
    val title: String,
    val description: String,
    val severity: RiskLevel
)
