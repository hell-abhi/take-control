package com.akeshari.takecontrol.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.akeshari.takecontrol.data.database.dao.PermissionHistoryDao
import com.akeshari.takecontrol.data.database.entity.PermissionChangeEntity
import com.akeshari.takecontrol.data.database.entity.PermissionSnapshotEntity

@Database(
    entities = [PermissionSnapshotEntity::class, PermissionChangeEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TakeControlDatabase : RoomDatabase() {
    abstract fun permissionHistoryDao(): PermissionHistoryDao
}
