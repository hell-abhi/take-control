package com.akeshari.takecontrol.ui.threats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
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

@Composable
fun ThreatsScreen(
    scrollToCompany: String? = null,
    onAppClick: (String) -> Unit,
    viewModel: ThreatsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text("Analyzing trackers...", style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }

    if (state.companyExposures.isEmpty()) {
        Box(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Icon(Icons.Outlined.VerifiedUser, null, tint = RiskSafe, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text("No Trackers Detected", fontFamily = PressStart2P, fontSize = 12.sp, color = RiskSafe)
                Spacer(Modifier.height(8.dp))
                Text("None of your installed apps contain known tracking SDKs", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).statusBarsPadding()
    ) {
        // Header
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Visibility, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text("Tracker Radar", fontFamily = PressStart2P, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(Modifier.height(12.dp))

        // 1. Summary card — compact, no expandable lists
        SummaryCard(state)
        Spacer(Modifier.height(16.dp))

        // 2. Companies — the primary view
        Text("Who's Tracking You", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Tap any company for details", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))

        state.companyExposures.forEach { exposure ->
            CompanyCard(exposure, onAppClick, initiallyExpanded = exposure.companyName == scrollToCompany)
            Spacer(Modifier.height(8.dp))
        }

        // 3. Cross-app tracking
        if (state.trackingBridges.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Cross-App Tracking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Same tracker in multiple apps = your activity is correlated", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))

            state.trackingBridges.take(6).forEach { bridge ->
                BridgeCard(bridge)
                Spacer(Modifier.height(6.dp))
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

// ── Summary Card ────────────────────────────────────────────────────────────

@Composable
private fun SummaryCard(state: ThreatsState) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            // Stats row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SumStat("${state.totalCompanies}", "Companies", RiskCritical)
                SumStat("${state.totalTrackers}", "Trackers", RiskHigh)
                SumStat("${state.appsWithTrackers}", "Apps", RiskMedium)
            }

            // Percentage bar
            if (state.totalUserApps > 0) {
                Spacer(Modifier.height(10.dp))
                val pct = (state.appsWithTrackers.toFloat() / state.totalUserApps * 100).toInt()
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(RiskCritical.copy(alpha = 0.12f))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(pct / 100f).background(RiskCritical))
                }
                Spacer(Modifier.height(4.dp))
                Text("$pct% of your apps contain trackers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SumStat(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Company Card ────────────────────────────────────────────────────────────

@Composable
private fun CompanyCard(exposure: CompanyExposure, onAppClick: (String) -> Unit, initiallyExpanded: Boolean = false) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val color = when {
        exposure.reachPercentage > 40 -> RiskCritical
        exposure.reachPercentage > 20 -> RiskHigh
        exposure.reachPercentage > 10 -> RiskMedium
        else -> RiskLow
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.fillMaxWidth()) {
            // Header — always visible
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Text(exposure.companyName.first().toString(), fontFamily = PressStart2P, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(exposure.companyName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("${exposure.appCount} apps · ${exposure.reachPercentage.toInt()}% of your apps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Expanded — trackers, permissions, apps
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                    // Tracker SDKs
                    Text("Tracker SDKs", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        exposure.trackerNames.forEach { name ->
                            Box(Modifier.clip(RoundedCornerShape(4.dp)).background(color.copy(alpha = 0.08f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text(name, style = MaterialTheme.typography.labelSmall, color = color)
                            }
                        }
                    }

                    // Permissions they can access
                    if (exposure.permissionAccess.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text("Can access via your apps", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        exposure.permissionAccess.forEach { (group, count) ->
                            val gc = when (group.defaultRisk) { RiskLevel.CRITICAL -> RiskCritical; RiskLevel.HIGH -> RiskHigh; RiskLevel.MEDIUM -> RiskMedium; else -> RiskLow }
                            Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(group.icon, null, tint = gc, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("${group.label.replace("Your ", "")} (${count} apps)", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // Apps
                    Spacer(Modifier.height(10.dp))
                    Text("Present in", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        exposure.apps.forEach { app ->
                            Box(
                                Modifier.clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surface).clickable { onAppClick(app.packageName) }.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(app.appName, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Bridge Card ─────────────────────────────────────────────────────────────

@Composable
private fun BridgeCard(bridge: TrackingBridge) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(bridge.trackerName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Text("${bridge.companyName} · links ${bridge.apps.size} apps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                bridge.apps.take(4).forEach { app ->
                    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(RiskHigh.copy(alpha = 0.08f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(app.appName.take(10), style = MaterialTheme.typography.labelSmall, color = RiskHigh, maxLines = 1)
                    }
                }
                if (bridge.apps.size > 4) {
                    Text("+${bridge.apps.size - 4}", style = MaterialTheme.typography.labelSmall, color = RiskHigh)
                }
            }
        }
    }
}
