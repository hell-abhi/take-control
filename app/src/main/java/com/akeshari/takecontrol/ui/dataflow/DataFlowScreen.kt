package com.akeshari.takecontrol.ui.dataflow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.akeshari.takecontrol.data.model.AppPermissionInfo
import com.akeshari.takecontrol.data.repository.AppRepository
import com.akeshari.takecontrol.ui.theme.*
import com.akeshari.takecontrol.util.TrackerCompanies
import com.akeshari.takecontrol.util.TrackerCountries
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Data Models ─────────────────────────────────────────────────────────────

data class CountryExposure(
    val countryName: String,
    val flag: String,
    val region: String,
    val companies: List<CompanyDetail>,
    val totalApps: Int
)

data class CompanyDetail(
    val companyName: String,
    val appNames: List<String>,
    val trackerNames: List<String>
)

data class DataFlowState(
    val isLoading: Boolean = true,
    val countries: List<CountryExposure> = emptyList(),
    val totalCountries: Int = 0,
    val totalCompanies: Int = 0
)

// ── ViewModel ───────────────────────────────────────────────────────────────

@HiltViewModel
class DataFlowViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {
    private val _state = MutableStateFlow(DataFlowState())
    val state: StateFlow<DataFlowState> = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val apps = repository.getInstalledApps().filter { !it.isSystemApp }

            // Build: country → company → apps
            val countryMap = mutableMapOf<String, MutableMap<String, MutableSet<Pair<String, String>>>>() // country → company → Set<(appName, trackerName)>

            for (app in apps) {
                for (tracker in app.trackers) {
                    val company = TrackerCompanies.getCompany(tracker.name)
                    val country = TrackerCountries.getCountry(company)
                    val key = "${country.name}|${country.flag}|${country.region}"

                    countryMap.getOrPut(key) { mutableMapOf() }
                        .getOrPut(company) { mutableSetOf() }
                        .add(Pair(app.appName, tracker.name))
                }
            }

            val countries = countryMap.map { (key, companies) ->
                val parts = key.split("|")
                val companyDetails = companies.map { (company, appTrackers) ->
                    CompanyDetail(
                        companyName = company,
                        appNames = appTrackers.map { it.first }.distinct().sorted(),
                        trackerNames = appTrackers.map { it.second }.distinct().sorted()
                    )
                }.sortedByDescending { it.appNames.size }

                CountryExposure(
                    countryName = parts[0],
                    flag = parts[1],
                    region = parts[2],
                    companies = companyDetails,
                    totalApps = companyDetails.flatMap { it.appNames }.distinct().size
                )
            }.sortedByDescending { it.totalApps }

            val totalCompanies = countries.flatMap { it.companies }.map { it.companyName }.distinct().size

            _state.value = DataFlowState(
                isLoading = false,
                countries = countries,
                totalCountries = countries.size,
                totalCompanies = totalCompanies
            )
        }
    }
}

// ── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataFlowScreen(
    onBack: () -> Unit,
    onAppClick: (String) -> Unit = {},
    viewModel: DataFlowViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data Flow Map", fontFamily = PressStart2P, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Summary
            Text(
                "Where your data potentially flows based on tracker company headquarters",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            // Stats
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = RiskCritical.copy(alpha = 0.06f))
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FlowStat("${state.totalCountries}", "Countries", RiskCritical)
                    FlowStat("${state.totalCompanies}", "Companies", RiskHigh)
                    FlowStat("${state.countries.sumOf { it.totalApps }}", "App Links", RiskMedium)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Region groups
            val byRegion = state.countries.groupBy { it.region }
            val regionOrder = listOf("Americas", "Europe", "Asia", "Middle East", "Africa", "Oceania")

            regionOrder.forEach { region ->
                val regionCountries = byRegion[region] ?: return@forEach

                Text(region, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))

                regionCountries.forEach { country ->
                    CountryCard(country)
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(8.dp))
            }

            // Disclaimer
            Text(
                "Based on tracker company headquarters. Actual data may be processed in multiple jurisdictions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )

            Spacer(Modifier.height(20.dp))
        }
    }
}

// ── Components ──────────────────────────────────────────────────────────────

@Composable
private fun FlowStat(value: String, label: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CountryCard(country: CountryExposure) {
    var expanded by remember { mutableStateOf(false) }
    val intensity = when {
        country.totalApps > 15 -> RiskCritical
        country.totalApps > 5 -> RiskHigh
        country.totalApps > 2 -> RiskMedium
        else -> RiskLow
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.fillMaxWidth()) {
            // Header
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { expanded = !expanded }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(country.flag, fontSize = 24.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(country.countryName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("${country.companies.size} companies · ${country.totalApps} apps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // Intensity bar
                Box(Modifier.width(40.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(intensity.copy(alpha = 0.15f))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth((country.totalApps / 20f).coerceAtMost(1f)).background(intensity))
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expanded: companies + apps
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(Modifier.padding(start = 52.dp, end = 14.dp, bottom = 12.dp)) {
                    country.companies.forEach { company ->
                        Column(Modifier.padding(vertical = 4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(intensity))
                                Spacer(Modifier.width(6.dp))
                                Text(company.companyName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.width(6.dp))
                                Text("${company.appNames.size} apps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            // Tracker SDKs
                            Text(
                                company.trackerNames.joinToString(", "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                            // Apps
                            Text(
                                company.appNames.joinToString(", "),
                                style = MaterialTheme.typography.labelSmall,
                                color = intensity,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
