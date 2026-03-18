package com.akeshari.takecontrol.data.model

data class CompanyExposure(
    val companyName: String,
    val trackerNames: List<String>,
    val apps: List<AppSummary>,
    val permissionAccess: Map<PermissionGroup, Int>,
    val reachPercentage: Float
) {
    val appCount: Int get() = apps.size
    val totalPermissionReach: Int get() = permissionAccess.values.sum()
}

data class AppSummary(
    val packageName: String,
    val appName: String
)

data class AggregateExposure(
    val group: PermissionGroup,
    val companyCount: Int,
    val appCount: Int,
    val companyNames: List<String>
)

data class TrackingBridge(
    val trackerName: String,
    val companyName: String,
    val apps: List<AppSummary>
)

data class HeatmapCell(
    val companyName: String,
    val group: PermissionGroup,
    val appCount: Int
)
