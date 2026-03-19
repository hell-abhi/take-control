package com.akeshari.takecontrol.ui.preinstall

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akeshari.takecontrol.data.model.AppPermissionInfo
import com.akeshari.takecontrol.data.model.RiskLevel
import com.akeshari.takecontrol.data.repository.AppRepository
import com.akeshari.takecontrol.data.scanner.DataSafetyInfo
import com.akeshari.takecontrol.data.scanner.PlayStoreAnalyzer
import com.akeshari.takecontrol.data.scanner.PlayStorePermissionGroup
import com.akeshari.takecontrol.data.scanner.PlayStoreReport
import com.akeshari.takecontrol.util.PrivacyNarrativeGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AnalysisSource { LOCAL, PLAY_STORE }

data class PermissionRisk(
    val name: String,
    val groupName: String,
    val riskLevel: RiskLevel
)

data class PreInstallState(
    val isLoading: Boolean = false,
    val query: String = "",
    val error: String? = null,
    // Shared fields
    val appName: String? = null,
    val packageName: String? = null,
    val source: AnalysisSource? = null,
    val narratives: List<String> = emptyList(),
    val verdict: String? = null,
    val verdictLevel: RiskLevel? = null,
    // Local analysis
    val localApp: AppPermissionInfo? = null,
    // Play Store analysis
    val playStoreReport: PlayStoreReport? = null,
    val permissionRisks: List<PermissionRisk> = emptyList()
)

@HiltViewModel
class PreInstallCheckViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AppRepository,
    private val playStoreAnalyzer: PlayStoreAnalyzer
) : ViewModel() {

    private val _state = MutableStateFlow(PreInstallState())
    val state: StateFlow<PreInstallState> = _state.asStateFlow()

    fun updateQuery(query: String) {
        _state.value = _state.value.copy(query = query, error = null)
    }

    fun analyze() {
        val input = _state.value.query.trim()
        if (input.isBlank()) return

        val packageName = extractPackageName(input)
        if (packageName.isBlank() || !packageName.contains(".")) {
            _state.value = _state.value.copy(
                error = "Enter a valid package name (e.g., com.instagram.android) or paste a Play Store link."
            )
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, localApp = null, playStoreReport = null)

            // Priority: 1. Local (installed) → 2. Play Store scraping
            if (isInstalled(packageName)) {
                analyzeLocal(packageName)
            } else {
                analyzeFromPlayStore(packageName)
            }
        }
    }

    private suspend fun analyzeLocal(packageName: String) {
        try {
            val app = repository.getAppByPackage(packageName)
            if (app != null) {
                val narratives = PrivacyNarrativeGenerator.generate(app.permissions)
                val (verdict, level) = computeVerdict(
                    criticalCount = app.permissions.count { it.isGranted && it.riskLevel == RiskLevel.CRITICAL },
                    highCount = app.permissions.count { it.isGranted && it.riskLevel == RiskLevel.HIGH },
                    trackerCount = app.trackers.size
                )
                _state.value = _state.value.copy(
                    isLoading = false,
                    appName = app.appName,
                    packageName = packageName,
                    source = AnalysisSource.LOCAL,
                    localApp = app,
                    narratives = narratives,
                    verdict = verdict,
                    verdictLevel = level
                )
            } else {
                // Shouldn't happen since we checked isInstalled, but fallback
                analyzeFromPlayStore(packageName)
            }
        } catch (e: Exception) {
            analyzeFromPlayStore(packageName)
        }
    }

    private suspend fun analyzeFromPlayStore(packageName: String) {
        try {
            val report = playStoreAnalyzer.analyze(packageName)
            val risks = classifyPlayStorePermissions(report.permissionGroups)
            val narratives = generateNarrativesFromGroups(report.permissionGroups)
            val criticalCount = risks.count { it.riskLevel == RiskLevel.CRITICAL }
            val highCount = risks.count { it.riskLevel == RiskLevel.HIGH }
            val (verdict, level) = computeVerdict(criticalCount, highCount, 0)

            _state.value = _state.value.copy(
                isLoading = false,
                appName = report.appName,
                packageName = packageName,
                source = AnalysisSource.PLAY_STORE,
                playStoreReport = report,
                permissionRisks = risks,
                narratives = narratives,
                verdict = verdict,
                verdictLevel = level
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isLoading = false,
                error = when {
                    "not found" in (e.message ?: "").lowercase() -> "App not found on Google Play Store. Check the package name."
                    "HTTP" in (e.message ?: "") -> "Could not reach Play Store. Check your internet connection."
                    else -> "Analysis failed: ${e.message}"
                }
            )
        }
    }

    private fun classifyPlayStorePermissions(groups: List<PlayStorePermissionGroup>): List<PermissionRisk> {
        val risks = mutableListOf<PermissionRisk>()
        for (group in groups) {
            val groupRisk = assessGroupRisk(group.groupName)
            for (perm in group.permissions) {
                val permRisk = assessPermDescriptionRisk(perm, groupRisk)
                risks.add(PermissionRisk(perm, group.groupName, permRisk))
            }
        }
        return risks.sortedByDescending { it.riskLevel.weight }
    }

    private fun assessGroupRisk(groupName: String): RiskLevel = when (groupName.lowercase()) {
        "microphone" -> RiskLevel.CRITICAL
        "sms" -> RiskLevel.CRITICAL
        "camera" -> RiskLevel.HIGH
        "location" -> RiskLevel.HIGH
        "contacts" -> RiskLevel.HIGH
        "phone" -> RiskLevel.HIGH
        "identity" -> RiskLevel.HIGH
        "device id & call information" -> RiskLevel.HIGH
        "calendar" -> RiskLevel.MEDIUM
        "storage", "photos/media/files", "photos and videos", "music and audio" -> RiskLevel.MEDIUM
        "sensors", "device & app history" -> RiskLevel.MEDIUM
        else -> RiskLevel.LOW
    }

    private fun assessPermDescriptionRisk(desc: String, groupDefault: RiskLevel): RiskLevel {
        val lower = desc.lowercase()
        return when {
            "background" in lower && "location" in lower -> RiskLevel.CRITICAL
            "record audio" in lower -> RiskLevel.CRITICAL
            "read sms" in lower || "send sms" in lower || "receive text" in lower -> RiskLevel.CRITICAL
            "read call log" in lower -> RiskLevel.CRITICAL
            "precise location" in lower -> RiskLevel.HIGH
            "take pictures" in lower -> RiskLevel.HIGH
            "read your contacts" in lower -> RiskLevel.HIGH
            "call phone" in lower || "directly call" in lower -> RiskLevel.HIGH
            else -> groupDefault
        }
    }

    private fun generateNarrativesFromGroups(groups: List<PlayStorePermissionGroup>): List<String> {
        val groupNames = groups.map { it.groupName.lowercase() }.toSet()
        val allPerms = groups.flatMap { it.permissions }.map { it.lowercase() }.toSet()
        val narratives = mutableListOf<String>()

        val hasMic = "microphone" in groupNames
        val hasCamera = "camera" in groupNames
        val hasLocation = "location" in groupNames
        val hasSms = "sms" in groupNames
        val hasContacts = "contacts" in groupNames
        val hasPhone = "phone" in groupNames
        val hasStorage = "storage" in groupNames || "photos/media/files" in groupNames

        // Full surveillance
        if (hasMic && hasCamera && hasLocation) {
            narratives.add("Has full surveillance capability — can see through your camera, listen via your microphone, and track your location")
            return narratives
        }

        if (hasMic) narratives.add("Can record audio through your microphone")
        if (hasCamera) narratives.add("Can take photos and videos with your camera")
        if (hasLocation) {
            if (allPerms.any { "precise" in it || "gps" in it }) {
                narratives.add("Can pinpoint your exact GPS location")
            } else {
                narratives.add("Can determine your approximate location")
            }
        }
        if (hasSms) {
            if (allPerms.any { "send sms" in it }) {
                narratives.add("Can read and send SMS messages, including 2FA codes")
            } else {
                narratives.add("Can read your SMS messages, including 2FA codes")
            }
        }
        if (hasContacts) narratives.add("Can access your entire contact list")
        if (hasPhone && allPerms.any { "call log" in it }) {
            narratives.add("Can read your call history")
        }
        if (hasStorage) narratives.add("Can access your files, photos, and media")

        return narratives
    }

    private fun computeVerdict(criticalCount: Int, highCount: Int, trackerCount: Int): Pair<String, RiskLevel> {
        return when {
            criticalCount >= 3 || (criticalCount >= 1 && highCount >= 3 && trackerCount > 5) ->
                "High Risk" to RiskLevel.CRITICAL
            criticalCount >= 1 || highCount >= 3 || trackerCount > 3 ->
                "Moderate Risk" to RiskLevel.HIGH
            highCount >= 1 || trackerCount > 0 ->
                "Low Risk" to RiskLevel.MEDIUM
            else ->
                "Looks Safe" to RiskLevel.LOW
        }
    }

    private fun isInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun extractPackageName(input: String): String {
        // Play Store URLs
        if ("play.google.com" in input) {
            val regex = Regex("[?&]id=([^&]+)")
            regex.find(input)?.let { return it.groupValues[1] }
        }
        if (input.startsWith("market://details?id=")) {
            return input.removePrefix("market://details?id=").substringBefore("&")
        }
        return input
    }
}
