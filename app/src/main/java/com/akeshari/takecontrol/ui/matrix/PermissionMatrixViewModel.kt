package com.akeshari.takecontrol.ui.matrix

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akeshari.takecontrol.data.model.AppPermissionInfo
import com.akeshari.takecontrol.data.model.PermissionGroup
import com.akeshari.takecontrol.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AppFilter(val label: String, val group: PermissionGroup?) {
    USER_ONLY("Installed Apps", null),
    SYSTEM_ONLY("System Apps", null),
    ALL("All Apps", null),
    HIGH_RISK("High Risk", null),
    HAS_LOCATION("Location", PermissionGroup.LOCATION),
    HAS_CAMERA("Camera", PermissionGroup.CAMERA),
    HAS_MIC("Microphone", PermissionGroup.MICROPHONE),
    HAS_CONTACTS("Contacts", PermissionGroup.CONTACTS),
    HAS_SMS("SMS Access", PermissionGroup.SMS),
    HAS_PHONE("Phone", PermissionGroup.PHONE),
    HAS_STORAGE("Storage", PermissionGroup.STORAGE),
    HAS_SENSORS("Sensors", PermissionGroup.SENSORS);

    companion object {
        fun forGroup(groupName: String): AppFilter? {
            val group = try { PermissionGroup.valueOf(groupName) } catch (_: Exception) { null }
            return entries.firstOrNull { it.group == group }
        }
    }
}

data class MatrixState(
    val allApps: List<AppPermissionInfo> = emptyList(),
    val filteredApps: List<AppPermissionInfo> = emptyList(),
    val searchQuery: String = "",
    val filter: AppFilter = AppFilter.USER_ONLY,
    val highlightedGroup: PermissionGroup? = null,
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
            _state.value = _state.value.copy(
                allApps = apps,
                isLoading = false
            )
            applyFilters()
        }
    }

    fun setInitialGroup(groupName: String?) {
        if (groupName == null) return
        val filter = AppFilter.forGroup(groupName) ?: return
        val group = try { PermissionGroup.valueOf(groupName) } catch (_: Exception) { null }
        _state.value = _state.value.copy(
            filter = filter,
            highlightedGroup = group
        )
        applyFilters()
    }

    fun clearHighlight() {
        _state.value = _state.value.copy(highlightedGroup = null)
    }

    fun search(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        applyFilters()
    }

    fun setFilter(filter: AppFilter) {
        _state.value = _state.value.copy(
            filter = filter,
            highlightedGroup = filter.group
        )
        applyFilters()
    }

    private fun applyFilters() {
        val current = _state.value
        var filtered = current.allApps

        if (current.searchQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.appName.contains(current.searchQuery, ignoreCase = true)
            }
        }

        filtered = when (current.filter) {
            AppFilter.USER_ONLY -> filtered.filter { !it.isSystemApp }
            AppFilter.SYSTEM_ONLY -> filtered.filter { it.isSystemApp }
            AppFilter.ALL -> filtered
            AppFilter.HIGH_RISK -> filtered.filter { !it.isSystemApp && it.riskScore >= 50 }
            else -> {
                val group = current.filter.group
                if (group != null) {
                    filtered.filter { app ->
                        app.permissions.any { it.group == group && it.isGranted }
                    }
                } else filtered
            }
        }

        filtered = filtered.sortedByDescending { it.riskScore }
        _state.value = current.copy(filteredApps = filtered)
    }
}
