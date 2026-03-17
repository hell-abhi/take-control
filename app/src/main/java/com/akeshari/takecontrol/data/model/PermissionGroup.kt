package com.akeshari.takecontrol.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class PermissionGroup(
    val label: String,
    val description: String,
    val icon: ImageVector,
    val defaultRisk: RiskLevel
) {
    LOCATION(
        "Your Location",
        "Can track where you are",
        Icons.Outlined.LocationOn,
        RiskLevel.HIGH
    ),
    CAMERA(
        "Your Camera",
        "Can take photos and videos",
        Icons.Outlined.CameraAlt,
        RiskLevel.HIGH
    ),
    MICROPHONE(
        "Your Conversations",
        "Can listen through your mic",
        Icons.Outlined.Mic,
        RiskLevel.CRITICAL
    ),
    CONTACTS(
        "Your Identity",
        "Can read your contacts and accounts",
        Icons.Outlined.Contacts,
        RiskLevel.HIGH
    ),
    STORAGE(
        "Your Files",
        "Can access photos, media, and files",
        Icons.Outlined.Folder,
        RiskLevel.MEDIUM
    ),
    PHONE(
        "Your Calls",
        "Can make and manage phone calls",
        Icons.Outlined.Phone,
        RiskLevel.HIGH
    ),
    SMS(
        "Your Messages",
        "Can read and send SMS",
        Icons.Outlined.Sms,
        RiskLevel.CRITICAL
    ),
    CALENDAR(
        "Your Schedule",
        "Can read and modify your calendar",
        Icons.Outlined.CalendarToday,
        RiskLevel.MEDIUM
    ),
    SENSORS(
        "Your Activity",
        "Can access body sensors and activity",
        Icons.Outlined.Sensors,
        RiskLevel.MEDIUM
    ),
    NETWORK(
        "Internet Access",
        "Can connect to the internet",
        Icons.Outlined.Wifi,
        RiskLevel.LOW
    ),
    OTHER(
        "Other",
        "Other permissions",
        Icons.Outlined.Shield,
        RiskLevel.LOW
    )
}
