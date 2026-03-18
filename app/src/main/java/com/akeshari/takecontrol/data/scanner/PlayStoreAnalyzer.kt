package com.akeshari.takecontrol.data.scanner

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class PlayStoreReport(
    val appName: String,
    val packageName: String,
    val category: String?,
    val rating: String?,
    val downloads: String?,
    val permissionGroups: List<PlayStorePermissionGroup>,
    val dataSafety: DataSafetyInfo
)

data class PlayStorePermissionGroup(
    val groupName: String,
    val permissions: List<String>
)

data class DataSafetyInfo(
    val sharedSummary: String?,
    val collectedSummary: String?,
    val isEncrypted: Boolean,
    val canRequestDeletion: Boolean
)

@Singleton
class PlayStoreAnalyzer @Inject constructor() {

    suspend fun analyze(packageName: String): PlayStoreReport = withContext(Dispatchers.IO) {
        val html = fetchPage(packageName)
        val dataBlock = extractDataBlock(html)
            ?: throw Exception("Could not parse app data from Play Store")

        val appName = extractString(dataBlock, intArrayOf(1, 2, 0, 0)) ?: packageName
        val category = extractString(dataBlock, intArrayOf(1, 2, 79, 0, 0, 0))
        val rating = extractNumber(dataBlock, intArrayOf(1, 2, 51, 0, 1))?.let {
            "%.1f".format(it)
        }
        val downloads = extractLong(dataBlock, intArrayOf(1, 2, 13, 1))?.let {
            formatDownloads(it)
        }
        val permissionGroups = extractPermissions(dataBlock)
        val dataSafety = extractDataSafety(dataBlock)

        PlayStoreReport(
            appName = appName,
            packageName = packageName,
            category = category,
            rating = rating,
            downloads = downloads,
            permissionGroups = permissionGroups,
            dataSafety = dataSafety
        )
    }

    private fun fetchPage(packageName: String): String {
        val url = URL("https://play.google.com/store/apps/details?id=$packageName&hl=en")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10)")
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000

        try {
            val code = connection.responseCode
            if (code == 404) throw Exception("App not found on Play Store")
            if (code != 200) throw Exception("Play Store returned HTTP $code")
            return connection.inputStream.bufferedReader().readText()
        } finally {
            connection.disconnect()
        }
    }

    private fun extractDataBlock(html: String): JSONArray? {
        // Find the AF_initDataCallback block with key 'ds:5' which contains app details
        val pattern = Regex("""AF_initDataCallback\(\{key:\s*'ds:5'.*?data:(.*?), sideChannel:""", RegexOption.DOT_MATCHES_ALL)
        val match = pattern.find(html) ?: return null
        return try {
            JSONArray(match.groupValues[1])
        } catch (_: Exception) {
            null
        }
    }

    private fun extractPermissions(root: JSONArray): List<PlayStorePermissionGroup> {
        val groups = mutableListOf<PlayStorePermissionGroup>()
        try {
            // Permissions at [1][2][74][2][0] — array of permission group entries
            val permSection = root.getJSONArray(1).getJSONArray(2)
                .getJSONArray(74).getJSONArray(2).getJSONArray(0)

            for (i in 0 until permSection.length()) {
                val group = permSection.optJSONArray(i) ?: continue
                val groupName = group.optString(0, null) ?: continue

                val permissions = mutableListOf<String>()
                val permList = group.optJSONArray(2)
                if (permList != null) {
                    for (j in 0 until permList.length()) {
                        val perm = permList.optJSONArray(j) ?: continue
                        val desc = perm.optString(1, null)
                        if (desc != null) permissions.add(desc)
                    }
                }

                // Skip "Other" with just boilerplate permissions
                groups.add(PlayStorePermissionGroup(groupName, permissions))
            }

            // Also check [1][2][74][2][1] for the "Other" group
            try {
                val otherSection = root.getJSONArray(1).getJSONArray(2)
                    .getJSONArray(74).getJSONArray(2).getJSONArray(1)
                for (i in 0 until otherSection.length()) {
                    val group = otherSection.optJSONArray(i) ?: continue
                    val groupName = group.optString(0, null) ?: continue
                    val permissions = mutableListOf<String>()
                    val permList = group.optJSONArray(2)
                    if (permList != null) {
                        for (j in 0 until permList.length()) {
                            val perm = permList.optJSONArray(j) ?: continue
                            val desc = perm.optString(1, null)
                            if (desc != null) permissions.add(desc)
                        }
                    }
                    groups.add(PlayStorePermissionGroup(groupName, permissions))
                }
            } catch (_: Exception) {
                // Other section may not exist
            }
        } catch (_: Exception) {
            // Try fallback: search for permission patterns in nearby indices
            tryFallbackPermissionExtraction(root, groups)
        }
        return groups
    }

    private fun tryFallbackPermissionExtraction(root: JSONArray, groups: MutableList<PlayStorePermissionGroup>) {
        // Search indices 70-80 for the permission section
        try {
            val inner = root.getJSONArray(1).getJSONArray(2)
            for (idx in 70..80) {
                try {
                    val candidate = inner.getJSONArray(idx)
                    val section = candidate.optJSONArray(2)?.optJSONArray(0) ?: continue
                    val firstGroup = section.optJSONArray(0) ?: continue
                    val firstName = firstGroup.optString(0, "")
                    if (firstName in KNOWN_PERMISSION_GROUPS) {
                        // Found it
                        for (i in 0 until section.length()) {
                            val group = section.optJSONArray(i) ?: continue
                            val name = group.optString(0, null) ?: continue
                            val perms = mutableListOf<String>()
                            val permList = group.optJSONArray(2)
                            if (permList != null) {
                                for (j in 0 until permList.length()) {
                                    val p = permList.optJSONArray(j)?.optString(1, null)
                                    if (p != null) perms.add(p)
                                }
                            }
                            groups.add(PlayStorePermissionGroup(name, perms))
                        }
                        return
                    }
                } catch (_: Exception) {
                    continue
                }
            }
        } catch (_: Exception) {}
    }

    private fun extractDataSafety(root: JSONArray): DataSafetyInfo {
        var sharedSummary: String? = null
        var collectedSummary: String? = null
        var isEncrypted = false
        var canDelete = false

        try {
            // Data safety at [1][2][136][1]
            val safetySection = root.getJSONArray(1).getJSONArray(2)
                .getJSONArray(136).getJSONArray(1)

            for (i in 0 until safetySection.length()) {
                val item = safetySection.optJSONArray(i) ?: continue
                val title = item.optString(1, "") ?: ""

                when {
                    "share" in title.lowercase() -> {
                        sharedSummary = extractSafetySummary(item) ?: title
                    }
                    "collect" in title.lowercase() -> {
                        collectedSummary = extractSafetySummary(item) ?: title
                    }
                    "encrypted" in title.lowercase() -> isEncrypted = true
                    "deleted" in title.lowercase() || "delete" in title.lowercase() -> canDelete = true
                }
            }
        } catch (_: Exception) {
            // Try nearby indices (130-140) as fallback
            tryFallbackSafetyExtraction(root)?.let { return it }
        }

        return DataSafetyInfo(sharedSummary, collectedSummary, isEncrypted, canDelete)
    }

    private fun extractSafetySummary(section: JSONArray): String? {
        // Search for summary text like "Location, Personal info and 12 others"
        return findStringsInArray(section)
            .filter { it.length > 5 && "googleusercontent" !in it && "http" !in it }
            .filter { it != section.optString(1, "") }
            .firstOrNull()
    }

    private fun findStringsInArray(arr: JSONArray, depth: Int = 0): List<String> {
        if (depth > 4) return emptyList()
        val results = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val str = arr.optString(i, null)
            if (str != null && str.length > 10) results.add(str)
            val sub = arr.optJSONArray(i)
            if (sub != null) results.addAll(findStringsInArray(sub, depth + 1))
        }
        return results
    }

    private fun tryFallbackSafetyExtraction(root: JSONArray): DataSafetyInfo? {
        try {
            val inner = root.getJSONArray(1).getJSONArray(2)
            for (idx in 130..140) {
                try {
                    val candidate = inner.getJSONArray(idx)
                    val section = candidate.optJSONArray(1) ?: continue
                    val firstItem = section.optJSONArray(0) ?: continue
                    val firstTitle = firstItem.optString(1, "")
                    if ("share" in firstTitle.lowercase() || "collect" in firstTitle.lowercase() || "no data" in firstTitle.lowercase()) {
                        // Found it — parse normally
                        var shared: String? = null
                        var collected: String? = null
                        var encrypted = false
                        var canDel = false
                        for (i in 0 until section.length()) {
                            val item = section.optJSONArray(i) ?: continue
                            val title = item.optString(1, "")
                            when {
                                "share" in title.lowercase() -> shared = extractSafetySummary(item) ?: title
                                "collect" in title.lowercase() -> collected = extractSafetySummary(item) ?: title
                                "encrypted" in title.lowercase() -> encrypted = true
                                "delet" in title.lowercase() -> canDel = true
                            }
                        }
                        return DataSafetyInfo(shared, collected, encrypted, canDel)
                    }
                } catch (_: Exception) {
                    continue
                }
            }
        } catch (_: Exception) {}
        return null
    }

    // Utility functions for safe JSON navigation
    private fun extractString(arr: JSONArray, path: IntArray): String? {
        var current: Any = arr
        for (idx in path) {
            current = (current as? JSONArray)?.opt(idx) ?: return null
        }
        return current as? String
    }

    private fun extractNumber(arr: JSONArray, path: IntArray): Double? {
        var current: Any = arr
        for (idx in path) {
            current = (current as? JSONArray)?.opt(idx) ?: return null
        }
        return when (current) {
            is Double -> current
            is Int -> current.toDouble()
            is Long -> current.toDouble()
            is Float -> current.toDouble()
            else -> null
        }
    }

    private fun extractLong(arr: JSONArray, path: IntArray): Long? {
        var current: Any = arr
        for (idx in path) {
            current = (current as? JSONArray)?.opt(idx) ?: return null
        }
        return when (current) {
            is Long -> current
            is Int -> current.toLong()
            is Double -> current.toLong()
            else -> null
        }
    }

    private fun formatDownloads(count: Long): String = when {
        count >= 1_000_000_000 -> "${count / 1_000_000_000}B+"
        count >= 1_000_000 -> "${count / 1_000_000}M+"
        count >= 1_000 -> "${count / 1_000}K+"
        else -> "$count"
    }

    companion object {
        private val KNOWN_PERMISSION_GROUPS = setOf(
            "Camera", "Contacts", "Location", "Microphone", "Phone",
            "SMS", "Storage", "Calendar", "Sensors", "Identity",
            "Photos/Media/Files", "Wi-Fi connection information",
            "Device ID & call information", "Device & app history",
            "Nearby devices", "Music and audio", "Photos and videos"
        )
    }
}
