package com.akeshari.takecontrol.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akeshari.takecontrol.data.model.*
import com.akeshari.takecontrol.data.repository.AppRepository
import com.akeshari.takecontrol.data.scanner.ActivityMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ActivityState(
    val isLoading: Boolean = true,
    val hasPermission: Boolean = false,
    val alerts: List<PrivacyAlert> = emptyList(),
    val zombieApps: List<ZombieApp> = emptyList(),
    val recentAccesses: List<PermissionAccessRecord> = emptyList(),
    val accessByPermission: Map<String, List<PermissionAccessRecord>> = emptyMap()
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

    fun refresh() {
        load()
    }

    fun checkPermission() {
        _state.value = _state.value.copy(hasPermission = activityMonitor.hasUsagePermission())
        if (_state.value.hasPermission) load()
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
            val accesses = activityMonitor.getRecentAccesses(apps)
            val alerts = activityMonitor.generateAlerts(apps, zombies, accesses)

            val accessByPermission = accesses.groupBy { it.permission }

            _state.value = ActivityState(
                isLoading = false,
                hasPermission = true,
                alerts = alerts,
                zombieApps = zombies,
                recentAccesses = accesses,
                accessByPermission = accessByPermission
            )
        }
    }
}
