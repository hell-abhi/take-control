package com.akeshari.takecontrol.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akeshari.takecontrol.data.database.entity.PermissionChangeEntity
import com.akeshari.takecontrol.data.model.*
import com.akeshari.takecontrol.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

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
                    recentChanges = recentChanges
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to scan apps: ${e.message}"
                )
            }
        }
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
