package com.akeshari.takecontrol.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akeshari.takecontrol.data.model.*
import com.akeshari.takecontrol.data.repository.AppRepository
import com.akeshari.takecontrol.data.scanner.ActivityMonitor
import com.akeshari.takecontrol.data.scanner.AppUsageInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ActivityState(
    val isLoading: Boolean = true,
    val hasPermission: Boolean = false,
    val zombieApps: List<ZombieApp> = emptyList(),
    val overPermissioned: List<AppUsageInfo> = emptyList(), // apps with permissions but rarely used
    val heavyTracked: List<AppUsageInfo> = emptyList()       // apps with most trackers
)

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val repository: AppRepository,
    private val activityMonitor: ActivityMonitor
) : ViewModel() {

    private val _state = MutableStateFlow(ActivityState())
    val state: StateFlow<ActivityState> = _state.asStateFlow()

    init {
        load()
    }

    fun checkPermission() {
        _state.value = _state.value.copy(hasPermission = activityMonitor.hasUsagePermission())
        if (_state.value.hasPermission && _state.value.zombieApps.isEmpty()) load()
    }

    private fun load() {
        viewModelScope.launch {
            val hasPermission = activityMonitor.hasUsagePermission()
            _state.value = _state.value.copy(isLoading = true, hasPermission = hasPermission)

            if (!hasPermission) {
                _state.value = _state.value.copy(isLoading = false)
                return@launch
            }

            val apps = repository.getInstalledApps()
            val zombies = activityMonitor.getZombieApps(apps)
            val usage = activityMonitor.getAppUsageAnalysis(apps)

            // Over-permissioned: apps with dangerous perms but used < 5 min today
            val overPermissioned = usage
                .filter { it.dangerousPermissions.isNotEmpty() && it.foregroundMinutesToday < 5 }
                .take(10)

            // Heavy tracked: apps with 2+ trackers, sorted by tracker count
            val heavyTracked = usage
                .filter { it.trackerCount >= 2 }
                .sortedByDescending { it.trackerCount }
                .take(10)

            _state.value = ActivityState(
                isLoading = false,
                hasPermission = true,
                zombieApps = zombies,
                overPermissioned = overPermissioned,
                heavyTracked = heavyTracked
            )
        }
    }
}
