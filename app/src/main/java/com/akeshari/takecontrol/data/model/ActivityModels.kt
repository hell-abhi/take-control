package com.akeshari.takecontrol.data.model

data class ZombieApp(
    val packageName: String,
    val appName: String,
    val daysSinceUsed: Int,
    val dangerousPermissions: List<String>
)
