package com.akeshari.takecontrol.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "permission_changes")
data class PermissionChangeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val permission: String,
    val permissionLabel: String,
    val wasGranted: Boolean,
    val isNowGranted: Boolean,
    val detectedAt: Long
)
