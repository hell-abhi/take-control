package com.akeshari.takecontrol.data.scanner

import android.content.Context
import android.content.pm.PackageManager
import com.akeshari.takecontrol.data.model.TrackerCategory
import com.akeshari.takecontrol.data.model.TrackerInfo
import com.akeshari.takecontrol.util.TrackerSignatures
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackerScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val packageManager: PackageManager = context.packageManager

    fun detectTrackers(packageName: String): List<TrackerInfo> {
        val componentNames = getComponentNames(packageName)
        if (componentNames.isEmpty()) return emptyList()

        val detected = mutableSetOf<String>() // dedupe by tracker name
        val trackers = mutableListOf<TrackerInfo>()

        for (signature in TrackerSignatures.ALL) {
            if (signature.name in detected) continue
            if (componentNames.any { it.startsWith(signature.classPrefix) }) {
                detected.add(signature.name)
                trackers.add(TrackerInfo(name = signature.name, category = signature.category))
            }
        }

        // Deduplicate: if we detect both "Facebook SDK" and "Facebook Login", keep the broader one
        return deduplicateFacebookTrackers(trackers)
    }

    private fun getComponentNames(packageName: String): List<String> {
        return try {
            val flags = PackageManager.GET_ACTIVITIES or
                    PackageManager.GET_SERVICES or
                    PackageManager.GET_RECEIVERS or
                    PackageManager.GET_PROVIDERS
            val pkgInfo = packageManager.getPackageInfo(packageName, flags)

            val names = mutableListOf<String>()
            pkgInfo.activities?.forEach { names.add(it.name) }
            pkgInfo.services?.forEach { names.add(it.name) }
            pkgInfo.receivers?.forEach { names.add(it.name) }
            pkgInfo.providers?.forEach { names.add(it.name) }
            names
        } catch (_: PackageManager.NameNotFoundException) {
            emptyList()
        }
    }

    private fun deduplicateFacebookTrackers(trackers: List<TrackerInfo>): List<TrackerInfo> {
        val hasFacebookSdk = trackers.any { it.name == "Facebook SDK" }
        if (!hasFacebookSdk) return trackers

        // If we have the generic Facebook SDK, remove the more specific ones
        return trackers.filter { tracker ->
            tracker.name != "Facebook Login" && tracker.name != "Facebook Share"
        }
    }
}
