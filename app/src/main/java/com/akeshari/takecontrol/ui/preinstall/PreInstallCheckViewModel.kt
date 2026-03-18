package com.akeshari.takecontrol.ui.preinstall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akeshari.takecontrol.data.model.RiskLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

data class PreInstallReport(
    val appName: String,
    val packageName: String,
    val trackerCount: Int,
    val trackerNames: List<String>,
    val permissions: List<String>,
    val riskAssessment: List<PermissionRisk>,
    val version: String
)

data class PermissionRisk(
    val permission: String,
    val label: String,
    val riskLevel: RiskLevel
)

data class PreInstallState(
    val isLoading: Boolean = false,
    val report: PreInstallReport? = null,
    val error: String? = null,
    val query: String = ""
)

@HiltViewModel
class PreInstallCheckViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(PreInstallState())
    val state: StateFlow<PreInstallState> = _state.asStateFlow()

    fun updateQuery(query: String) {
        _state.value = _state.value.copy(query = query)
    }

    fun analyze() {
        val input = _state.value.query.trim()
        if (input.isBlank()) return

        val packageName = extractPackageName(input)
        if (packageName.isBlank() || !packageName.contains(".")) {
            _state.value = _state.value.copy(error = "Invalid package name. Enter a package name like com.example.app or paste a Play Store link.")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, report = null)
            try {
                val report = fetchExodusReport(packageName)
                _state.value = _state.value.copy(isLoading = false, report = report)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Could not fetch report: ${e.message ?: "Unknown error"}. The app may not be in the Exodus database."
                )
            }
        }
    }

    private fun extractPackageName(input: String): String {
        // Handle Play Store URLs
        if (input.contains("play.google.com")) {
            val regex = Regex("[?&]id=([^&]+)")
            val match = regex.find(input)
            if (match != null) return match.groupValues[1]
        }
        // Handle market:// URLs
        if (input.startsWith("market://details?id=")) {
            return input.removePrefix("market://details?id=").substringBefore("&")
        }
        // Assume it's a package name
        return input
    }

    private suspend fun fetchExodusReport(packageName: String): PreInstallReport =
        withContext(Dispatchers.IO) {
            // Fetch tracker list first
            val trackerMap = fetchTrackerMap()

            // Fetch app report
            val url = URL("https://reports.exodus-privacy.eu.org/api/search/$packageName")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            try {
                val responseCode = connection.responseCode
                if (responseCode != 200) {
                    throw Exception("App not found (HTTP $responseCode)")
                }

                val responseBody = connection.inputStream.bufferedReader().readText()
                val reports = JSONObject(responseBody)

                // The response is { "package_name": [ ...reports... ] }
                val appReports = reports.getJSONArray(packageName)
                if (appReports.length() == 0) {
                    throw Exception("No reports found for this app")
                }

                // Get the latest report
                val latest = appReports.getJSONObject(appReports.length() - 1)
                val appName = latest.optString("name", packageName)
                val version = latest.optString("version_name", "Unknown")

                // Parse trackers
                val trackerIds = latest.getJSONArray("trackers")
                val trackerNames = (0 until trackerIds.length()).map { i ->
                    val id = trackerIds.getInt(i).toString()
                    trackerMap[id] ?: "Unknown Tracker (#$id)"
                }

                // Parse permissions
                val permArray = latest.getJSONArray("permissions")
                val permissions = (0 until permArray.length()).map { permArray.getString(it) }

                // Assess risk for each permission
                val riskAssessment = permissions
                    .filter { it.startsWith("android.permission.") }
                    .map { perm ->
                        PermissionRisk(
                            permission = perm,
                            label = formatPermissionLabel(perm),
                            riskLevel = assessPermissionRisk(perm)
                        )
                    }
                    .sortedByDescending { it.riskLevel.weight }

                PreInstallReport(
                    appName = appName,
                    packageName = packageName,
                    trackerCount = trackerNames.size,
                    trackerNames = trackerNames,
                    permissions = permissions,
                    riskAssessment = riskAssessment,
                    version = version
                )
            } finally {
                connection.disconnect()
            }
        }

    private fun fetchTrackerMap(): Map<String, String> {
        return try {
            val url = URL("https://reports.exodus-privacy.eu.org/api/trackers")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            try {
                val body = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(body)
                val trackers = json.getJSONObject("trackers")
                val map = mutableMapOf<String, String>()
                for (key in trackers.keys()) {
                    val tracker = trackers.getJSONObject(key)
                    map[key] = tracker.optString("name", "Unknown")
                }
                map
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun formatPermissionLabel(permission: String): String {
        return permission
            .substringAfterLast(".")
            .replace("_", " ")
            .lowercase()
            .replaceFirstChar { it.uppercase() }
    }

    private fun assessPermissionRisk(permission: String): RiskLevel = when (permission) {
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.RECORD_AUDIO",
        "android.permission.READ_SMS",
        "android.permission.SEND_SMS",
        "android.permission.READ_CALL_LOG" -> RiskLevel.CRITICAL

        "android.permission.CAMERA",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.READ_CONTACTS",
        "android.permission.CALL_PHONE" -> RiskLevel.HIGH

        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.READ_CALENDAR" -> RiskLevel.MEDIUM

        else -> RiskLevel.LOW
    }
}
