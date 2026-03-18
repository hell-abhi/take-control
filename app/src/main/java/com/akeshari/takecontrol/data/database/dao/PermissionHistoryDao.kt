package com.akeshari.takecontrol.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.akeshari.takecontrol.data.database.entity.PermissionChangeEntity
import com.akeshari.takecontrol.data.database.entity.PermissionSnapshotEntity

@Dao
interface PermissionHistoryDao {

    // Snapshots
    @Query("SELECT * FROM permission_snapshots")
    suspend fun getAllSnapshots(): List<PermissionSnapshotEntity>

    @Query("SELECT * FROM permission_snapshots WHERE packageName = :packageName")
    suspend fun getSnapshotsForApp(packageName: String): List<PermissionSnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSnapshots(snapshots: List<PermissionSnapshotEntity>)

    @Query("DELETE FROM permission_snapshots WHERE packageName = :packageName")
    suspend fun deleteSnapshotsForApp(packageName: String)

    // Changes
    @Insert
    suspend fun insertChanges(changes: List<PermissionChangeEntity>)

    @Query("SELECT * FROM permission_changes ORDER BY detectedAt DESC LIMIT :limit")
    suspend fun getRecentChanges(limit: Int = 50): List<PermissionChangeEntity>

    @Query("SELECT * FROM permission_changes WHERE packageName = :packageName ORDER BY detectedAt DESC LIMIT :limit")
    suspend fun getChangesForApp(packageName: String, limit: Int = 20): List<PermissionChangeEntity>

    @Query("SELECT COUNT(*) FROM permission_snapshots")
    suspend fun getSnapshotCount(): Int
}
