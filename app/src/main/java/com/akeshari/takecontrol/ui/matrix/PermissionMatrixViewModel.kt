package com.akeshari.takecontrol.ui.matrix

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akeshari.takecontrol.data.model.AppPermissionInfo
import com.akeshari.takecontrol.data.model.RiskLevel
import com.akeshari.takecontrol.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AppFilter(val label: String) {
    ALL("All Apps"),
    USER_ONLY("User Apps"),
    HIGH_RISK("High Risk"),
    HAS_LOCATION("Location"),
    HAS_CAMERA("Camera"),
    HAS_MIC("Microphone"),
    HAS_CONTACTS("Contacts"),
    HAS_SMS("SMS Access")
}

data class MatrixState(
    val allApps: List<AppPermissionInfo> = emptyList(),
    val filteredApps: List<AppPermissionInfo> = emptyList(),
    val searchQuery: String = "",
    val filter: AppFilter = AppFilter.ALL,
    val isLoading: Boolean = true
)

@HiltViewModel
class PermissionMatrixViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MatrixState())
    val state: StateFlow<MatrixState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val apps = repository.getInstalledApps()
            _state.value = MatrixState(
                allApps = apps,
                filteredApps = apps,
                isLoading = false
            )
        }
    }

    fun search(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        applyFilters()
    }

    fun setFilter(filter: AppFilter) {
        _state.value = _state.value.copy(filter = filter)
        applyFilters()
    }

    private fun applyFilters() {
        val current = _state.value
        var filtered = current.allApps

        // Search
        if (current.searchQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.appName.contains(current.searchQuery, ignoreCase = true)
            }
        }

        // Filter
        filtered = when (current.filter) {
            AppFilter.ALL -> filtered
            AppFilter.USER_ONLY -> filtered.filter { !it.isSystemApp }
            AppFilter.HIGH_RISK -> filtered.filter { it.riskScore >= 50 }
            AppFilter.HAS_LOCATION -> filtered.filter { app ->
                app.permissions.any {
                    it.group == com.akeshari.takecontrol.data.model.PermissionGroup.LOCATION && it.isGranted
                }
            }
            AppFilter.HAS_CAMERA -> filtered.filter { app ->
                app.permissions.any {
                    it.group == com.akeshari.takecontrol.data.model.PermissionGroup.CAMERA && it.isGranted
                }
            }
            AppFilter.HAS_MIC -> filtered.filter { app ->
                app.permissions.any {
                    it.group == com.akeshari.takecontrol.data.model.PermissionGroup.MICROPHONE && it.isGranted
                }
            }
            AppFilter.HAS_CONTACTS -> filtered.filter { app ->
                app.permissions.any {
                    it.group == com.akeshari.takecontrol.data.model.PermissionGroup.CONTACTS && it.isGranted
                }
            }
            AppFilter.HAS_SMS -> filtered.filter { app ->
                app.permissions.any {
                    it.group == com.akeshari.takecontrol.data.model.PermissionGroup.SMS && it.isGranted
                }
            }
        }

        // Sort by risk score descending
        filtered = filtered.sortedByDescending { it.riskScore }

        _state.value = current.copy(filteredApps = filtered)
    }
}
