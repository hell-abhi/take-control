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
    val zombieApps: List<AppUsageInfo> = emptyList(),
    val overExposed: List<AppUsageInfo> = emptyList()
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
        val has = activityMonitor.hasUsagePermission()
        _state.value = _state.value.copy(hasPermission = has)
        if (has && _state.value.zombieApps.isEmpty()) load()
    }

    private fun load() {
        viewModelScope.launch {
            val has = activityMonitor.hasUsagePermission()
            _state.value = _state.value.copy(isLoading = true, hasPermission = has)
            if (!has) { _state.value = _state.value.copy(isLoading = false); return@launch }

            val apps = repository.getInstalledApps()
            val allUsage = activityMonitor.analyze(apps)

            val zombies = allUsage.filter { it.lastOpenedDays >= 30 || it.lastOpenedDays == -1 }
                .filter { it.dangerousPermissions.isNotEmpty() }
                .sortedByDescending { it.dangerousPermissions.size }

            // All apps with dangerous permissions that weren't used this week (minus zombies which have their own section)
            val overExposed = allUsage
                .filter { !(it.lastOpenedDays >= 30 || it.lastOpenedDays == -1) || it.dangerousPermissions.isEmpty() }
                .filter { it.dangerousPermissions.isNotEmpty() }
                .sortedByDescending { it.exposureRatio }

            // Budget: only count apps with dangerous permissions (not tracker-only)
            val withDangerousPerms = allUsage.filter { it.dangerousPermissions.isNotEmpty() }
            _state.value = ActivityState(
                isLoading = false,
                hasPermission = true,
                zombieApps = zombies,
                overExposed = overExposed
            )
        }
    }
}
