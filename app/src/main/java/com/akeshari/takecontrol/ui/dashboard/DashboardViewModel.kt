package com.akeshari.takecontrol.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akeshari.takecontrol.data.model.AppPermissionInfo
import com.akeshari.takecontrol.data.model.PermissionGroup
import com.akeshari.takecontrol.data.model.PrivacyScore
import com.akeshari.takecontrol.data.model.PrivacyScoreCalculator
import com.akeshari.takecontrol.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardState(
    val isLoading: Boolean = true,
    val privacyScore: PrivacyScore = PrivacyScore(0, 0, 0, 0, emptyList()),
    val totalApps: Int = 0,
    val totalPermissions: Int = 0,
    val topRiskyApps: List<AppPermissionInfo> = emptyList(),
    val permissionGroupCounts: Map<PermissionGroup, Int> = emptyMap(),
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
                val apps = repository.getInstalledApps(forceRefresh = true)

                val totalPermissions = apps.sumOf { it.permissions.count { p -> p.isGranted } }
                val privacyScore = PrivacyScoreCalculator.calculate(apps)

                val groupCounts = apps
                    .flatMap { it.permissions }
                    .filter { it.isGranted }
                    .groupBy { it.group }
                    .mapValues { it.value.size }

                _state.value = DashboardState(
                    isLoading = false,
                    privacyScore = privacyScore,
                    totalApps = apps.size,
                    totalPermissions = totalPermissions,
                    topRiskyApps = apps.take(5),
                    permissionGroupCounts = groupCounts
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to scan apps: ${e.message}"
                )
            }
        }
    }
}
