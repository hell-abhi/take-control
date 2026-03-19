package com.akeshari.takecontrol.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akeshari.takecontrol.data.database.entity.PermissionChangeEntity
import com.akeshari.takecontrol.data.model.*
import com.akeshari.takecontrol.data.repository.AppRepository
import com.akeshari.takecontrol.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Recommendation(
    val text: String,
    val actionRoute: String? = null,
    val packageName: String? = null
)

data class DashboardState(
    val isLoading: Boolean = true,
    val privacyScore: PrivacyScore = PrivacyScore(0, 0, 0, 0, 0, 0, emptyList()),
    val summary: String = "",
    val userAppCount: Int = 0,
    val totalPermissions: Int = 0,
    val appsWithTrackers: Int = 0,
    val totalTrackers: Int = 0,
    val topRiskyApps: List<AppPermissionInfo> = emptyList(),
    val permissionGroupCounts: Map<PermissionGroup, Int> = emptyMap(),
    val companyOverviews: List<CompanyOverview> = emptyList(),
    val recentChanges: List<PermissionChangeEntity> = emptyList(),
    val recommendations: List<Recommendation> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        loadDashboard()
    }

    fun refresh() {
        repository.clearCache()
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val apps = repository.getInstalledApps(forceRefresh = false)
                val userApps = apps.filter { !it.isSystemApp }

                val totalPermissions = userApps.sumOf { it.permissions.count { p -> p.isGranted } }
                val privacyScore = PrivacyScoreCalculator.calculate(userApps)
                val companyOverviews = PrivacyScoreCalculator.getCompanyOverviews(userApps)

                val groupCounts = mutableMapOf<PermissionGroup, Int>()
                for (group in PermissionGroup.entries) {
                    val count = userApps.count { app ->
                        app.permissions.any { it.group == group && it.isGranted }
                    }
                    if (count > 0) groupCounts[group] = count
                }

                val appsWithTrackers = userApps.count { it.trackers.isNotEmpty() }
                val totalTrackers = userApps.flatMap { it.trackers }.map { it.name }.distinct().size

                val summary = generateSummary(privacyScore, appsWithTrackers, userApps.size)
                val recentChanges = repository.getRecentChanges(10)
                val recommendations = generateRecommendations(userApps, privacyScore)

                _state.value = DashboardState(
                    isLoading = false,
                    privacyScore = privacyScore,
                    summary = summary,
                    userAppCount = userApps.size,
                    totalPermissions = totalPermissions,
                    appsWithTrackers = appsWithTrackers,
                    totalTrackers = totalTrackers,
                    topRiskyApps = userApps.take(5),
                    permissionGroupCounts = groupCounts,
                    companyOverviews = companyOverviews,
                    recentChanges = recentChanges,
                    recommendations = recommendations
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to scan apps: ${e.message}"
                )
            }
        }
    }

    private fun generateRecommendations(
        apps: List<AppPermissionInfo>,
        score: PrivacyScore
    ): List<Recommendation> {
        val recs = mutableListOf<Recommendation>()

        // 1. Apps in OTHER/UTILITIES category with HIGH/CRITICAL risk permissions
        val suspiciousCategories = setOf(AppCategory.OTHER, AppCategory.UTILITIES)
        val highRiskGroups = setOf(
            PermissionGroup.MICROPHONE, PermissionGroup.CAMERA, PermissionGroup.LOCATION,
            PermissionGroup.SMS, PermissionGroup.CONTACTS, PermissionGroup.PHONE
        )
        for (app in apps) {
            if (app.category in suspiciousCategories) {
                val riskyGranted = app.permissions.filter {
                    it.isGranted && it.group in highRiskGroups &&
                        (it.riskLevel == RiskLevel.HIGH || it.riskLevel == RiskLevel.CRITICAL)
                }
                if (riskyGranted.isNotEmpty()) {
                    val perm = riskyGranted.first()
                    recs.add(
                        Recommendation(
                            text = "Revoke ${perm.group.label.replace("Your ", "")} from ${app.appName} — a ${app.category.label.lowercase()} app doesn't need it",
                            actionRoute = Routes.appDetail(app.packageName),
                            packageName = app.packageName
                        )
                    )
                }
            }
            if (recs.size >= 6) break
        }

        // 2. Apps with alternatives that have trackers
        for (app in apps) {
            if (app.trackers.isNotEmpty()) {
                val alternatives = PrivacyAlternativesData.getAlternativesForPackage(app.packageName)
                if (alternatives.isNotEmpty()) {
                    val alt = alternatives.first()
                    recs.add(
                        Recommendation(
                            text = "${app.appName} has ${app.trackers.size} trackers. Consider switching to ${alt.alternative}",
                            actionRoute = Routes.ALTERNATIVES,
                            packageName = app.packageName
                        )
                    )
                }
            }
            if (recs.size >= 8) break
        }

        // 3. Apps with many sensitive permissions for their category
        for (app in apps) {
            if (app.category in suspiciousCategories) {
                val sensitiveCount = app.permissions.count {
                    it.isGranted && it.group in highRiskGroups
                }
                if (sensitiveCount >= 3) {
                    val alreadyHas = recs.any { it.packageName == app.packageName }
                    if (!alreadyHas) {
                        recs.add(
                            Recommendation(
                                text = "Review ${app.appName} — it has $sensitiveCount sensitive permissions for a ${app.category.label.lowercase()} app",
                                actionRoute = Routes.appDetail(app.packageName),
                                packageName = app.packageName
                            )
                        )
                    }
                }
            }
            if (recs.size >= 10) break
        }

        // 4. Low permission score
        if (score.permissionScore < 50) {
            recs.add(
                Recommendation(
                    text = "Your permission score is ${score.permissionScore}/100. Check the breakdown to see which permissions to revoke",
                    actionRoute = Routes.permissionMatrix()
                )
            )
        }

        // 5. Low tracker score
        if (score.trackerScore < 50) {
            recs.add(
                Recommendation(
                    text = "Many of your apps contain tracking SDKs. Check the Radar for details",
                    actionRoute = Routes.threats()
                )
            )
        }

        // Prioritize: alternatives suggestions first (high impact), then risky perms, then scores
        // Already roughly in priority order, just take top 4
        return recs.take(4)
    }

    private fun generateSummary(score: PrivacyScore, appsWithTrackers: Int, totalApps: Int): String {
        return when {
            score.total >= 80 -> "Your phone is well-protected. Keep it up."
            score.total >= 60 -> "Good standing, but some apps have more access than they need."
            score.total >= 40 -> "Several apps have risky access. Review the breakdowns below."
            else -> "Your privacy needs attention — multiple apps have invasive access."
        }
    }
}
