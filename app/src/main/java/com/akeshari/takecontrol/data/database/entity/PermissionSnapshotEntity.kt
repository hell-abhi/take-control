package com.akeshari.takecontrol.data.database.entity

import androidx.room.Entity

@Entity(
    tableName = "permission_snapshots",
    primaryKeys = ["packageName", "permission"]
)
data class PermissionSnapshotEntity(
    val packageName: String,
    val permission: String,
    val isGranted: Boolean,
    val lastUpdated: Long
)
