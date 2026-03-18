package com.akeshari.takecontrol.ui.threats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akeshari.takecontrol.data.model.*
import com.akeshari.takecontrol.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreatsScreen(
    onAppClick: (String) -> Unit,
    viewModel: ThreatsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tracker Radar", fontFamily = PressStart2P, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Text("Analyzing tracker ecosystem...", style = MaterialTheme.typography.bodyMedium)
                }
            }
            return@Scaffold
        }

        if (state.companyExposures.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(Icons.Outlined.VerifiedUser, null, tint = RiskSafe, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("No Trackers Detected", fontFamily = PressStart2P, fontSize = 12.sp, color = RiskSafe)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "None of your installed apps contain known tracking SDKs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Explainer card
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        "What are trackers?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Trackers are hidden software libraries (SDKs) that companies like Google, Meta, and others embed inside apps. Even if you never use Facebook directly, their SDK inside other apps can monitor your activity, build a profile on you, and share data with advertisers — all without your knowledge.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "This screen scans your installed apps for known tracking SDKs and shows which companies can see your data, through which apps, and what they can access.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Top-level stats
            TopStatsRow(state)

            // Threat heatmap — right below ecosystem overview
            if (state.heatmapCells.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("Threat Heatmap", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Companies vs permissions — darker = more apps giving access",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                ThreatHeatmap(state.heatmapCells, state.heatmapCompanies, state.heatmapGroups)
            }

            Spacer(Modifier.height(24.dp))

            // Aggregate data exposure
            Text("Your Data Exposure", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Who can access what — tap to see which companies",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            state.aggregateExposures.forEach { exposure ->
                AggregateExposureRow(exposure)
                Spacer(Modifier.height(6.dp))
            }

            Spacer(Modifier.height(24.dp))

            // Company exposure cards
            Text("Company Reach", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Each company's tracker footprint on your device",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            state.companyExposures.forEach { exposure ->
                CompanyExposureCard(exposure, onAppClick)
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(24.dp))

            // Cross-app tracking bridges
            if (state.trackingBridges.isNotEmpty()) {
                Text("Cross-App Tracking", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Same tracker in multiple apps = your activity is correlated",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                state.trackingBridges.take(8).forEach { bridge ->
                    TrackingBridgeCard(bridge)
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Top Stats ───────────────────────────────────────────────────────────────

@Composable
private fun TopStatsRow(state: ThreatsState) {
    // Track which stat is expanded: 0=none, 1=companies, 2=trackers, 3=apps
    var expandedStat by remember { mutableIntStateOf(0) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = RiskCritical.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Warning, null, tint = RiskCritical, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ecosystem Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(
                    value = "${state.totalCompanies}",
                    label = "Companies\nTracking You",
                    color = RiskCritical,
                    selected = expandedStat == 1,
                    onClick = { expandedStat = if (expandedStat == 1) 0 else 1 }
                )
                StatColumn(
                    value = "${state.totalTrackers}",
                    label = "Unique\nTrackers",
                    color = RiskHigh,
                    selected = expandedStat == 2,
                    onClick = { expandedStat = if (expandedStat == 2) 0 else 2 }
                )
                StatColumn(
                    value = "${state.appsWithTrackers}",
                    label = "Apps With\nTrackers",
                    color = RiskMedium,
                    selected = expandedStat == 3,
                    onClick = { expandedStat = if (expandedStat == 3) 0 else 3 }
                )
            }

            // Expanded detail lists
            AnimatedVisibility(
                visible = expandedStat != 0,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = RiskCritical.copy(alpha = 0.1f))
                    Spacer(Modifier.height(8.dp))
                    when (expandedStat) {
                        1 -> {
                            Text("Companies tracking you", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            state.companyExposures.forEach { company ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(RiskCritical))
                                    Spacer(Modifier.width(8.dp))
                                    Text(company.companyName, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    Text(
                                        "${company.appCount} apps",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        2 -> {
                            val allTrackers = state.companyExposures
                                .flatMap { c -> c.trackerNames.map { it to c.companyName } }
                                .distinctBy { it.first }
                            Text("Unique trackers found", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            allTrackers.forEach { (tracker, company) ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(RiskHigh))
                                    Spacer(Modifier.width(8.dp))
                                    Text(tracker, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    Text(
                                        company,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        3 -> {
                            val appsWithTrackersList = state.companyExposures
                                .flatMap { it.apps }
                                .distinctBy { it.packageName }
                                .sortedBy { it.appName }
                            Text("Apps containing trackers", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            appsWithTrackersList.forEach { app ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(RiskMedium))
                                    Spacer(Modifier.width(8.dp))
                                    Text(app.appName, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            if (state.totalUserApps > 0) {
                Spacer(Modifier.height(12.dp))
                val pct = (state.appsWithTrackers.toFloat() / state.totalUserApps * 100).toInt()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(RiskCritical.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(pct / 100f)
                            .background(RiskCritical)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "$pct% of your apps contain trackers",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatColumn(value: String, label: String, color: Color, selected: Boolean = false, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) color.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            value,
            fontSize = 28.sp,
            fontFamily = PressStart2P,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}

// ── Aggregate Data Exposure ─────────────────────────────────────────────────

@Composable
private fun AggregateExposureRow(exposure: AggregateExposure) {
    var expanded by remember { mutableStateOf(false) }
    val riskColor = when (exposure.group.defaultRisk) {
        RiskLevel.CRITICAL -> RiskCritical
        RiskLevel.HIGH -> RiskHigh
        RiskLevel.MEDIUM -> RiskMedium
        RiskLevel.LOW -> RiskLow
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(riskColor.copy(alpha = 0.06f))
            .clickable { expanded = !expanded }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(exposure.group.icon, null, tint = riskColor, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    exposure.group.label.replace("Your ", ""),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${exposure.companyCount} companies via ${exposure.appCount} apps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                null,
                tint = riskColor,
                modifier = Modifier.size(18.dp)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(start = 46.dp, end = 12.dp, bottom = 10.dp)) {
                exposure.companyNames.forEach { name ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(riskColor))
                        Spacer(Modifier.width(8.dp))
                        Text(name, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

// ── Threat Heatmap ──────────────────────────────────────────────────────────

@Composable
private fun ThreatHeatmap(
    cells: List<HeatmapCell>,
    companies: List<String>,
    groups: List<PermissionGroup>
) {
    val maxCount = cells.maxOfOrNull { it.appCount } ?: 1

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            // Scrollable heatmap
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                // Company labels column
                Column {
                    // Empty corner cell
                    Box(Modifier.height(40.dp).width(80.dp))
                    companies.forEach { company ->
                        Box(
                            modifier = Modifier.height(36.dp).width(80.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                company,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Data columns
                groups.forEach { group ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Group header
                        Box(
                            modifier = Modifier.height(40.dp).width(44.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                group.icon,
                                contentDescription = group.label,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Cells
                        companies.forEach { company ->
                            val cell = cells.find { it.companyName == company && it.group == group }
                            val count = cell?.appCount ?: 0
                            val intensity = if (maxCount > 0) count.toFloat() / maxCount else 0f
                            val cellColor = when {
                                count == 0 -> Color.Transparent
                                intensity > 0.66f -> RiskCritical
                                intensity > 0.33f -> RiskHigh
                                else -> RiskMedium
                            }

                            Box(
                                modifier = Modifier
                                    .height(36.dp)
                                    .width(44.dp)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (count > 0) cellColor.copy(alpha = 0.15f + intensity * 0.55f)
                                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (count > 0) {
                                    Text(
                                        "$count",
                                        fontSize = 11.sp,
                                        fontFamily = JetBrainsMono,
                                        fontWeight = FontWeight.Bold,
                                        color = cellColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Company Exposure Card ───────────────────────────────────────────────────

@Composable
private fun CompanyExposureCard(exposure: CompanyExposure, onAppClick: (String) -> Unit) {
    val reachColor = when {
        exposure.reachPercentage > 40 -> RiskCritical
        exposure.reachPercentage > 20 -> RiskHigh
        exposure.reachPercentage > 10 -> RiskMedium
        else -> RiskLow
    }

    var expanded by remember { mutableStateOf(false) }

    Card(
        onClick = { expanded = !expanded },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Company initial
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(reachColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        exposure.companyName.first().toString(),
                        fontSize = 18.sp,
                        fontFamily = PressStart2P,
                        fontWeight = FontWeight.Bold,
                        color = reachColor
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        exposure.companyName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${exposure.appCount} apps  ·  ${exposure.reachPercentage.toInt()}% reach",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Reach bar
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(reachColor.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(exposure.reachPercentage / 100f)
                        .background(reachColor)
                )
            }

            // Permission access summary (always visible)
            if (exposure.permissionAccess.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    exposure.permissionAccess.entries.take(5).forEach { (group, count) ->
                        val riskColor = when (group.defaultRisk) {
                            RiskLevel.CRITICAL -> RiskCritical
                            RiskLevel.HIGH -> RiskHigh
                            RiskLevel.MEDIUM -> RiskMedium
                            RiskLevel.LOW -> RiskLow
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(riskColor.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(group.icon, null, tint = riskColor, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("$count", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = riskColor)
                        }
                    }
                }
            }

            // Expanded details
            if (expanded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surface)
                Spacer(Modifier.height(8.dp))

                // Tracker SDKs
                Text("Tracker SDKs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(
                    exposure.trackerNames.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(Modifier.height(10.dp))

                // Permission details
                Text("Data Access", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                exposure.permissionAccess.forEach { (group, count) ->
                    val riskColor = when (group.defaultRisk) {
                        RiskLevel.CRITICAL -> RiskCritical
                        RiskLevel.HIGH -> RiskHigh
                        RiskLevel.MEDIUM -> RiskMedium
                        RiskLevel.LOW -> RiskLow
                    }
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(group.icon, null, tint = riskColor, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${group.label.replace("Your ", "")} via $count app${if (count != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Apps list
                Text("Present In", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                exposure.apps.forEach { app ->
                    Text(
                        app.appName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }
    }
}

// ── Cross-App Tracking Bridge ───────────────────────────────────────────────

@Composable
private fun TrackingBridgeCard(bridge: TrackingBridge) {
    Card(
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tracker info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    bridge.trackerName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    bridge.companyName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // App count badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(RiskHigh.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "${bridge.apps.size} apps",
                    fontSize = 11.sp,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    color = RiskHigh
                )
            }
        }

        // App names
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            bridge.apps.forEach { app ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        app.appName,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
