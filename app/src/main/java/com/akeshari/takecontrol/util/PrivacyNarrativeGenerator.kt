package com.akeshari.takecontrol.util

import com.akeshari.takecontrol.data.model.PermissionDetail
import com.akeshari.takecontrol.data.model.PermissionGroup

object PrivacyNarrativeGenerator {

    fun generate(permissions: List<PermissionDetail>): List<String> {
        val granted = permissions.filter { it.isGranted }.map { it.group }.toSet()
        val narratives = mutableListOf<String>()

        // Audio surveillance
        if (PermissionGroup.MICROPHONE in granted && PermissionGroup.NETWORK in granted) {
            narratives.add("Can silently record audio and send it over the internet")
        }

        // Visual surveillance + location tagging
        if (PermissionGroup.CAMERA in granted && PermissionGroup.LOCATION in granted && PermissionGroup.NETWORK in granted) {
            narratives.add("Can photograph your surroundings and tag your exact location")
        } else if (PermissionGroup.CAMERA in granted && PermissionGroup.NETWORK in granted) {
            narratives.add("Can take photos or videos and upload them without your knowledge")
        }

        // Location tracking
        val hasBackgroundLocation = permissions.any {
            it.isGranted && it.permission == "android.permission.ACCESS_BACKGROUND_LOCATION"
        }
        if (hasBackgroundLocation) {
            narratives.add("Can track your location 24/7, even when the app is closed")
        } else if (PermissionGroup.LOCATION in granted) {
            narratives.add("Can pinpoint your exact location while the app is open")
        }

        // 2FA / SMS interception
        if (PermissionGroup.SMS in granted && PermissionGroup.NETWORK in granted) {
            narratives.add("Can read your SMS messages, including 2FA codes, and send them to a remote server")
        } else if (PermissionGroup.SMS in granted) {
            narratives.add("Can read and send SMS messages on your behalf")
        }

        // Call surveillance
        if (PermissionGroup.PHONE in granted && PermissionGroup.NETWORK in granted) {
            narratives.add("Can access your call history and phone identity")
        }

        // Contact harvesting
        if (PermissionGroup.CONTACTS in granted && PermissionGroup.NETWORK in granted) {
            narratives.add("Can harvest your entire contact list and upload it")
        }

        // File exfiltration
        if (PermissionGroup.STORAGE in granted && PermissionGroup.NETWORK in granted) {
            narratives.add("Can read your files, photos, and documents and send them online")
        }

        // Full surveillance combo
        if (PermissionGroup.MICROPHONE in granted && PermissionGroup.CAMERA in granted &&
            PermissionGroup.LOCATION in granted && PermissionGroup.NETWORK in granted
        ) {
            // Replace individual ones with this mega-narrative
            narratives.clear()
            narratives.add("Has full surveillance capability — can see through your camera, listen via your microphone, track your location, and transmit everything over the internet")
        }

        // Calendar snooping
        if (PermissionGroup.CALENDAR in granted && PermissionGroup.NETWORK in granted) {
            narratives.add("Can read your calendar events, meetings, and schedule")
        }

        // Body sensors
        if (PermissionGroup.SENSORS in granted && PermissionGroup.NETWORK in granted) {
            narratives.add("Can access body sensor data like heart rate and step count")
        }

        return narratives
    }
}
