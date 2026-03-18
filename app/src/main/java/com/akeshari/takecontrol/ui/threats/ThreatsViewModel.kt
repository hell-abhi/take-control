package com.akeshari.takecontrol.ui.threats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akeshari.takecontrol.data.model.*
import com.akeshari.takecontrol.data.repository.AppRepository
import com.akeshari.takecontrol.util.TrackerCompanies
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ThreatsState(
    val isLoading: Boolean = true,
    val totalCompanies: Int = 0,
    val totalTrackers: Int = 0,
    val appsWithTrackers: Int = 0,
    val totalUserApps: Int = 0,
    val companyExposures: List<CompanyExposure> = emptyList(),
    val aggregateExposures: List<AggregateExposure> = emptyList(),
    val trackingBridges: List<TrackingBridge> = emptyList(),
    val heatmapCells: List<HeatmapCell> = emptyList(),
    val heatmapCompanies: List<String> = emptyList(),
    val heatmapGroups: List<PermissionGroup> = emptyList()
)

@HiltViewModel
class ThreatsViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ThreatsState())
    val state: StateFlow<ThreatsState> = _state.asStateFlow()

    init {
        loadThreats()
    }

    fun refresh() {
        loadThreats()
    }

    private fun loadThreats() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val allApps = repository.getInstalledApps()
            val userApps = allApps.filter { !it.isSystemApp }
            val appsWithTrackers = userApps.filter { it.trackers.isNotEmpty() }

            val companyExposures = buildCompanyExposures(userApps)
            val aggregateExposures = buildAggregateExposures(userApps, companyExposures)
            val trackingBridges = buildTrackingBridges(userApps)
            val (heatmapCells, heatmapCompanies, heatmapGroups) = buildHeatmap(userApps, companyExposures)

            val uniqueTrackers = userApps.flatMap { it.trackers }.map { it.name }.distinct()

            _state.value = ThreatsState(
                isLoading = false,
                totalCompanies = companyExposures.size,
                totalTrackers = uniqueTrackers.size,
                appsWithTrackers = appsWithTrackers.size,
                totalUserApps = userApps.size,
                companyExposures = companyExposures,
                aggregateExposures = aggregateExposures,
                trackingBridges = trackingBridges,
                heatmapCells = heatmapCells,
                heatmapCompanies = heatmapCompanies,
                heatmapGroups = heatmapGroups
            )
        }
    }

    private fun buildCompanyExposures(userApps: List<AppPermissionInfo>): List<CompanyExposure> {
        // Group: for each company, which apps contain their trackers, and what permissions do those apps have
        val companyMap = mutableMapOf<String, MutableSet<AppPermissionInfo>>()
        val companyTrackers = mutableMapOf<String, MutableSet<String>>()

        for (app in userApps) {
            for (tracker in app.trackers) {
                val company = TrackerCompanies.getCompany(tracker.name)
                companyMap.getOrPut(company) { mutableSetOf() }.add(app)
                companyTrackers.getOrPut(company) { mutableSetOf() }.add(tracker.name)
            }
        }

        return companyMap.map { (company, apps) ->
            // For each permission group, count how many of this company's apps have it granted
            val permissionAccess = mutableMapOf<PermissionGroup, Int>()
            for (app in apps) {
                val grantedGroups = app.permissions
                    .filter { it.isGranted }
                    .map { it.group }
                    .distinct()
                for (group in grantedGroups) {
                    if (group != PermissionGroup.NETWORK && group != PermissionGroup.OTHER) {
                        permissionAccess[group] = (permissionAccess[group] ?: 0) + 1
                    }
                }
            }

            CompanyExposure(
                companyName = company,
                trackerNames = companyTrackers[company]?.sorted() ?: emptyList(),
                apps = apps.map { AppSummary(it.packageName, it.appName) }.sortedBy { it.appName },
                permissionAccess = permissionAccess.toSortedMap(compareByDescending { it.defaultRisk.weight }),
                reachPercentage = if (userApps.isNotEmpty()) apps.size.toFloat() / userApps.size * 100 else 0f
            )
        }.sortedByDescending { it.appCount }
    }

    private fun buildAggregateExposures(
        userApps: List<AppPermissionInfo>,
        companyExposures: List<CompanyExposure>
    ): List<AggregateExposure> {
        // For each permission group, how many companies and apps can access it
        val sensitiveGroups = PermissionGroup.entries.filter {
            it != PermissionGroup.NETWORK && it != PermissionGroup.OTHER
        }

        return sensitiveGroups.mapNotNull { group ->
            val companiesWithAccess = companyExposures.filter { it.permissionAccess.containsKey(group) }
            if (companiesWithAccess.isEmpty()) return@mapNotNull null

            val totalApps = companiesWithAccess.sumOf { it.permissionAccess[group] ?: 0 }

            AggregateExposure(
                group = group,
                companyCount = companiesWithAccess.size,
                appCount = totalApps,
                companyNames = companiesWithAccess.map { it.companyName }
            )
        }.sortedByDescending { it.companyCount * 10 + it.appCount }
    }

    private fun buildTrackingBridges(userApps: List<AppPermissionInfo>): List<TrackingBridge> {
        // Find trackers that appear in 2+ apps (enabling cross-app tracking)
        val trackerApps = mutableMapOf<String, MutableList<AppSummary>>()

        for (app in userApps) {
            for (tracker in app.trackers) {
                trackerApps.getOrPut(tracker.name) { mutableListOf() }
                    .add(AppSummary(app.packageName, app.appName))
            }
        }

        return trackerApps
            .filter { it.value.size >= 2 }
            .map { (trackerName, apps) ->
                TrackingBridge(
                    trackerName = trackerName,
                    companyName = TrackerCompanies.getCompany(trackerName),
                    apps = apps.sortedBy { it.appName }
                )
            }
            .sortedByDescending { it.apps.size }
    }

    private fun buildHeatmap(
        userApps: List<AppPermissionInfo>,
        companyExposures: List<CompanyExposure>
    ): Triple<List<HeatmapCell>, List<String>, List<PermissionGroup>> {
        // Top companies by reach
        val topCompanies = companyExposures.take(6).map { it.companyName }

        // Only groups that have exposure
        val activeGroups = PermissionGroup.entries.filter { group ->
            group != PermissionGroup.NETWORK && group != PermissionGroup.OTHER &&
                    companyExposures.any { it.permissionAccess.containsKey(group) }
        }

        val cells = mutableListOf<HeatmapCell>()
        for (company in topCompanies) {
            val exposure = companyExposures.find { it.companyName == company } ?: continue
            for (group in activeGroups) {
                cells.add(HeatmapCell(company, group, exposure.permissionAccess[group] ?: 0))
            }
        }

        return Triple(cells, topCompanies, activeGroups)
    }
}
