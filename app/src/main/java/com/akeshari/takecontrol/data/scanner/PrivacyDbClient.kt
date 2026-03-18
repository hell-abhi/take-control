package com.akeshari.takecontrol.data.scanner

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class PrivacyDbReport(
    val appName: String,
    val packageName: String,
    val category: String?,
    val rating: String?,
    val downloads: String?,
    val permissionGroups: List<PlayStorePermissionGroup>,
    val dataSafety: DataSafetyInfo,
    val scannedAt: String
)

@Singleton
class PrivacyDbClient @Inject constructor() {

    companion object {
        private const val BASE_URL = "https://raw.githubusercontent.com/hell-abhi/privacy-db/main"
        private const val REPO_URL = "https://github.com/hell-abhi/privacy-db"
    }

    fun getRepoUrl(): String = REPO_URL

    suspend fun fetch(packageName: String): PrivacyDbReport? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/apps/$packageName.json")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            // No cache — always fetch latest from GitHub CDN
            conn.setRequestProperty("Cache-Control", "no-cache")

            try {
                if (conn.responseCode != 200) return@withContext null
                val body = conn.inputStream.bufferedReader().readText()
                parseReport(body, packageName)
            } finally {
                conn.disconnect()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseReport(json: String, packageName: String): PrivacyDbReport {
        val obj = JSONObject(json)

        val permGroups = mutableListOf<PlayStorePermissionGroup>()
        val permsArray = obj.getJSONArray("permissions")
        for (i in 0 until permsArray.length()) {
            val group = permsArray.getJSONObject(i)
            val name = group.getString("group")
            val perms = mutableListOf<String>()
            val permsArr = group.getJSONArray("permissions")
            for (j in 0 until permsArr.length()) {
                perms.add(permsArr.getString(j))
            }
            permGroups.add(PlayStorePermissionGroup(name, perms))
        }

        val safety = obj.optJSONObject("data_safety")
        val dataSafety = DataSafetyInfo(
            sharedSummary = safety?.optString("shared", null),
            collectedSummary = safety?.optString("collected", null),
            isEncrypted = safety?.optBoolean("encrypted", false) ?: false,
            canRequestDeletion = safety?.optBoolean("deletable", false) ?: false
        )

        val downloads = obj.optLong("downloads", 0)

        return PrivacyDbReport(
            appName = obj.optString("app_name", packageName),
            packageName = packageName,
            category = obj.optString("category", null),
            rating = obj.optDouble("rating", 0.0).let { if (it > 0) "%.1f".format(it) else null },
            downloads = if (downloads > 0) formatDownloads(downloads) else null,
            permissionGroups = permGroups,
            dataSafety = dataSafety,
            scannedAt = obj.optString("scanned_at", "")
        )
    }

    private fun formatDownloads(count: Long): String = when {
        count >= 1_000_000_000 -> "${count / 1_000_000_000}B+"
        count >= 1_000_000 -> "${count / 1_000_000}M+"
        count >= 1_000 -> "${count / 1_000}K+"
        else -> "$count"
    }
}
