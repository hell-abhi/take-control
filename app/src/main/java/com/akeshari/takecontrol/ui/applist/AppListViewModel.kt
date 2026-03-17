package com.akeshari.takecontrol.ui.applist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akeshari.takecontrol.data.model.AppPermissionInfo
import com.akeshari.takecontrol.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortBy { RISK, NAME, PERMISSIONS }

data class AppListState(
    val allApps: List<AppPermissionInfo> = emptyList(),
    val filteredApps: List<AppPermissionInfo> = emptyList(),
    val searchQuery: String = "",
    val sortBy: SortBy = SortBy.RISK,
    val isLoading: Boolean = true
)

@HiltViewModel
class AppListViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AppListState())
    val state: StateFlow<AppListState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val apps = repository.getInstalledApps()
            _state.value = _state.value.copy(
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

    fun setSortBy(sortBy: SortBy) {
        _state.value = _state.value.copy(sortBy = sortBy)
        applyFilters()
    }

    private fun applyFilters() {
        val current = _state.value
        var filtered = current.allApps

        if (current.searchQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.appName.contains(current.searchQuery, ignoreCase = true) ||
                it.packageName.contains(current.searchQuery, ignoreCase = true)
            }
        }

        filtered = when (current.sortBy) {
            SortBy.RISK -> filtered.sortedByDescending { it.riskScore }
            SortBy.NAME -> filtered.sortedBy { it.appName.lowercase() }
            SortBy.PERMISSIONS -> filtered.sortedByDescending { it.permissions.count { p -> p.isGranted } }
        }

        _state.value = current.copy(filteredApps = filtered)
    }
}
