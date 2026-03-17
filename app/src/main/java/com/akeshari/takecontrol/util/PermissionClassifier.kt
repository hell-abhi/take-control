package com.akeshari.takecontrol.util

import android.Manifest
import com.akeshari.takecontrol.data.model.PermissionDetail
import com.akeshari.takecontrol.data.model.PermissionGroup
import com.akeshari.takecontrol.data.model.RiskLevel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionClassifier @Inject constructor() {

    fun classify(permission: String, isGranted: Boolean): PermissionDetail {
        val group = mapToGroup(permission)
        val riskLevel = assessRisk(permission, group)
        val label = formatLabel(permission)

        return PermissionDetail(
            permission = permission,
            label = label,
            group = group,
            isGranted = isGranted,
            riskLevel = riskLevel
        )
    }

    private fun mapToGroup(permission: String): PermissionGroup = when (permission) {
        // Location
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION -> PermissionGroup.LOCATION

        // Camera
        Manifest.permission.CAMERA -> PermissionGroup.CAMERA

        // Microphone
        Manifest.permission.RECORD_AUDIO -> PermissionGroup.MICROPHONE

        // Contacts
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS,
        Manifest.permission.GET_ACCOUNTS -> PermissionGroup.CONTACTS

        // Storage
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_AUDIO -> PermissionGroup.STORAGE

        // Phone
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.WRITE_CALL_LOG,
        Manifest.permission.PROCESS_OUTGOING_CALLS -> PermissionGroup.PHONE

        // SMS
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_MMS -> PermissionGroup.SMS

        // Calendar
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR -> PermissionGroup.CALENDAR

        // Sensors
        Manifest.permission.BODY_SENSORS,
        Manifest.permission.ACTIVITY_RECOGNITION -> PermissionGroup.SENSORS

        // Network
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.ACCESS_WIFI_STATE -> PermissionGroup.NETWORK

        else -> PermissionGroup.OTHER
    }

    private fun assessRisk(permission: String, group: PermissionGroup): RiskLevel = when (permission) {
        // Critical: these can spy on you
        Manifest.permission.ACCESS_BACKGROUND_LOCATION -> RiskLevel.CRITICAL
        Manifest.permission.RECORD_AUDIO -> RiskLevel.CRITICAL
        Manifest.permission.READ_SMS -> RiskLevel.CRITICAL
        Manifest.permission.SEND_SMS -> RiskLevel.CRITICAL
        Manifest.permission.READ_CALL_LOG -> RiskLevel.CRITICAL
        Manifest.permission.CAMERA -> RiskLevel.HIGH
        Manifest.permission.ACCESS_FINE_LOCATION -> RiskLevel.HIGH
        Manifest.permission.READ_CONTACTS -> RiskLevel.HIGH
        Manifest.permission.CALL_PHONE -> RiskLevel.HIGH

        // Medium
        Manifest.permission.READ_EXTERNAL_STORAGE -> RiskLevel.MEDIUM
        Manifest.permission.WRITE_EXTERNAL_STORAGE -> RiskLevel.MEDIUM
        Manifest.permission.READ_CALENDAR -> RiskLevel.MEDIUM

        // Low
        Manifest.permission.INTERNET -> RiskLevel.LOW
        Manifest.permission.ACCESS_NETWORK_STATE -> RiskLevel.LOW

        else -> group.defaultRisk
    }

    private fun formatLabel(permission: String): String {
        // "android.permission.ACCESS_FINE_LOCATION" -> "Access Fine Location"
        return permission
            .substringAfterLast(".")
            .replace("_", " ")
            .lowercase()
            .replaceFirstChar { it.uppercase() }
    }
}
