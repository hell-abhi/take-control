package com.akeshari.takecontrol.util

import android.Manifest
import com.akeshari.takecontrol.data.model.AppCategory
import com.akeshari.takecontrol.data.model.PermissionDetail
import com.akeshari.takecontrol.data.model.PermissionGroup
import com.akeshari.takecontrol.data.model.RiskLevel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionClassifier @Inject constructor() {

    fun classify(
        permission: String,
        isGranted: Boolean,
        category: AppCategory = AppCategory.OTHER
    ): PermissionDetail {
        val group = mapToGroup(permission)
        val staticRisk = assessRisk(permission, group)
        val riskLevel = CategoryRiskMatrix.assessContextualRisk(staticRisk, group, category)
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
        // Check exact match first, then raw string map
        return PERMISSION_LABELS[permission]
            ?: RAW_PERMISSION_LABELS[permission]
            ?: permission
                .substringAfterLast(".")
                .replace("_", " ")
                .lowercase()
                .replaceFirstChar { it.uppercase() }
    }

    companion object {
        // Permissions accessible via Manifest.permission constants
        private val PERMISSION_LABELS = mapOf(
            // Location
            Manifest.permission.ACCESS_FINE_LOCATION to "Precise GPS location",
            Manifest.permission.ACCESS_COARSE_LOCATION to "Approximate location",
            Manifest.permission.ACCESS_BACKGROUND_LOCATION to "Location even when app is closed",

            // Camera
            Manifest.permission.CAMERA to "Take photos and videos",

            // Microphone
            Manifest.permission.RECORD_AUDIO to "Record audio through mic",

            // Contacts
            Manifest.permission.READ_CONTACTS to "Read your contacts",
            Manifest.permission.WRITE_CONTACTS to "Modify your contacts",
            Manifest.permission.GET_ACCOUNTS to "Find accounts on device",

            // Storage
            Manifest.permission.READ_EXTERNAL_STORAGE to "Read files and media",
            Manifest.permission.WRITE_EXTERNAL_STORAGE to "Modify or delete files",
            Manifest.permission.READ_MEDIA_IMAGES to "Access your photos",
            Manifest.permission.READ_MEDIA_VIDEO to "Access your videos",
            Manifest.permission.READ_MEDIA_AUDIO to "Access your music and audio",

            // Phone
            Manifest.permission.READ_PHONE_STATE to "Read phone number and device ID",
            Manifest.permission.CALL_PHONE to "Make calls without your input",
            Manifest.permission.READ_CALL_LOG to "Read your call history",
            Manifest.permission.WRITE_CALL_LOG to "Modify your call history",
            Manifest.permission.PROCESS_OUTGOING_CALLS to "Intercept outgoing calls",

            // SMS
            Manifest.permission.SEND_SMS to "Send SMS messages (may cost money)",
            Manifest.permission.RECEIVE_SMS to "Receive and read incoming SMS",
            Manifest.permission.READ_SMS to "Read all your SMS messages",
            Manifest.permission.RECEIVE_MMS to "Receive and read MMS messages",

            // Calendar
            Manifest.permission.READ_CALENDAR to "Read your calendar events",
            Manifest.permission.WRITE_CALENDAR to "Add or change calendar events",

            // Sensors
            Manifest.permission.BODY_SENSORS to "Access heart rate and body sensors",
            Manifest.permission.ACTIVITY_RECOGNITION to "Detect physical activity (walking, driving)",

            // Network
            Manifest.permission.INTERNET to "Full internet access",
            Manifest.permission.ACCESS_NETWORK_STATE to "View network connections",
            Manifest.permission.ACCESS_WIFI_STATE to "View Wi-Fi connections"
        )

        // Permissions only available as raw strings (not in Manifest.permission)
        private val RAW_PERMISSION_LABELS = mapOf(
            // Biometrics & Security
            "android.permission.USE_BIOMETRIC" to "Authenticate with fingerprint or face",
            "android.permission.USE_FINGERPRINT" to "Authenticate with fingerprint",

            // Notifications
            "android.permission.POST_NOTIFICATIONS" to "Show notifications",

            // Bluetooth
            "android.permission.BLUETOOTH" to "Connect to Bluetooth devices",
            "android.permission.BLUETOOTH_ADMIN" to "Manage Bluetooth settings",
            "android.permission.BLUETOOTH_CONNECT" to "Connect to paired Bluetooth devices",
            "android.permission.BLUETOOTH_SCAN" to "Find nearby Bluetooth devices",
            "android.permission.BLUETOOTH_ADVERTISE" to "Broadcast to nearby Bluetooth devices",

            // Wi-Fi
            "android.permission.CHANGE_WIFI_STATE" to "Connect and disconnect Wi-Fi",
            "android.permission.ACCESS_WIFI_STATE" to "View Wi-Fi connections",
            "android.permission.NEARBY_WIFI_DEVICES" to "Find and connect to nearby Wi-Fi devices",

            // NFC
            "android.permission.NFC" to "Use NFC for payments and data transfer",

            // Vibration & System
            "android.permission.VIBRATE" to "Control vibration",
            "android.permission.WAKE_LOCK" to "Prevent device from sleeping",
            "android.permission.RECEIVE_BOOT_COMPLETED" to "Start automatically when device boots",
            "android.permission.FOREGROUND_SERVICE" to "Run background tasks with notification",
            "android.permission.FOREGROUND_SERVICE_LOCATION" to "Track location in background service",
            "android.permission.FOREGROUND_SERVICE_CAMERA" to "Use camera in background service",
            "android.permission.FOREGROUND_SERVICE_MICROPHONE" to "Use microphone in background service",
            "android.permission.FOREGROUND_SERVICE_DATA_SYNC" to "Sync data in background",
            "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" to "Play media in background",
            "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" to "Request unrestricted battery usage",
            "android.permission.SCHEDULE_EXACT_ALARM" to "Schedule exact-time alarms",
            "android.permission.USE_EXACT_ALARM" to "Set precise alarms and reminders",

            // System UI
            "android.permission.SYSTEM_ALERT_WINDOW" to "Draw over other apps",
            "android.permission.REQUEST_INSTALL_PACKAGES" to "Install other apps",
            "android.permission.REQUEST_DELETE_PACKAGES" to "Request to uninstall other apps",

            // Accounts
            "android.permission.MANAGE_ACCOUNTS" to "Add or remove accounts",
            "android.permission.AUTHENTICATE_ACCOUNTS" to "Act as account authenticator",
            "android.permission.USE_CREDENTIALS" to "Use account credentials",

            // Storage (newer)
            "android.permission.MANAGE_EXTERNAL_STORAGE" to "Access all files on device",
            "android.permission.ACCESS_MEDIA_LOCATION" to "Read location from your photos",
            "android.permission.READ_MEDIA_VISUAL_USER_SELECTED" to "Access photos you select",

            // Phone state (newer)
            "android.permission.READ_PHONE_NUMBERS" to "Read your phone number",
            "android.permission.ANSWER_PHONE_CALLS" to "Answer incoming calls automatically",
            "android.permission.READ_BASIC_PHONE_STATE" to "Detect when you're on a call",
            "android.permission.ACCEPT_HANDOVER" to "Continue a call from another app",

            // Network
            "android.permission.CHANGE_NETWORK_STATE" to "Change network connectivity",
            "android.permission.ACCESS_LOCATION_EXTRA_COMMANDS" to "Access extra location provider commands",

            // Misc
            "android.permission.FLASHLIGHT" to "Control flashlight",
            "android.permission.EXPAND_STATUS_BAR" to "Expand or collapse status bar",
            "android.permission.REORDER_TASKS" to "Reorder running apps",
            "android.permission.MODIFY_AUDIO_SETTINGS" to "Change audio and volume settings",
            "android.permission.QUERY_ALL_PACKAGES" to "See all installed apps",
            "android.permission.UPDATE_PACKAGES_WITHOUT_USER_ACTION" to "Update apps silently",
            "android.permission.PACKAGE_USAGE_STATS" to "Monitor your app usage history",
            "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" to "Read all your notifications",
            "android.permission.BIND_ACCESSIBILITY_SERVICE" to "Observe and control screen content",

            // Ad tracking
            "android.permission.ACCESS_ADSERVICES_AD_ID" to "Read your advertising ID",
            "android.permission.ACCESS_ADSERVICES_TOPICS" to "Access your ad interest topics",
            "android.permission.ACCESS_ADSERVICES_ATTRIBUTION" to "Track ad attribution",
            "android.permission.ACCESS_ADSERVICES_CUSTOM_AUDIENCE" to "Target you with custom ad audiences",

            // Google-specific
            "com.google.android.c2dm.permission.RECEIVE" to "Receive push notifications",
            "com.google.android.providers.gsf.permission.READ_GSERVICES" to "Read Google service settings",
            "com.google.android.gms.permission.ACTIVITY_RECOGNITION" to "Detect physical activity (walking, driving)"
        )
    }
}
