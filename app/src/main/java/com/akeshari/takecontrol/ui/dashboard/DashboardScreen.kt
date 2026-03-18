package com.akeshari.takecontrol.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akeshari.takecontrol.data.database.entity.PermissionChangeEntity
import com.akeshari.takecontrol.data.model.*
import com.akeshari.takecontrol.ui.navigation.Routes
import com.akeshari.takecontrol.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onViewAllApps: () -> Unit,
    onFixGroup: (String) -> Unit,
    onNavigateToRadar: (String?) -> Unit,
    onNavigate: (String) -> Unit,
    onAppClick: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(20.dp))
                Text("Scanning your apps...", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, null, tint = RiskSafe, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("100% local — nothing leaves your device", style = MaterialTheme.typography.bodySmall, color = RiskSafe)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Code, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open source on GitHub", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .statusBarsPadding()
        ) {
            // Compact brand header
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Take Control", fontFamily = PressStart2P, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(Icons.Outlined.Refresh, "Refresh", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(8.dp))

                // 1. Compact Score Banner
                CompactScoreCard(state.privacyScore, state.summary, onFixGroup)

                Spacer(Modifier.height(14.dp))

                // 2. Quick Actions
                QuickActionsRow(onNavigate)

                Spacer(Modifier.height(20.dp))

                // 3. Unified Overview (Permissions / Trackers tabs)
                UnifiedOverview(
                    permissionCounts = state.permissionGroupCounts,
                    companies = state.companyOverviews,
                    totalTrackers = state.totalTrackers,
                    onFixGroup = onFixGroup,
                    onViewAllApps = onViewAllApps,
                    onNavigateToRadar = onNavigateToRadar
                )

                Spacer(Modifier.height(20.dp))

                // 4. Top 3 Risky Apps
                SectionHeader("Highest Risk", null, onViewAllApps)
                Spacer(Modifier.height(8.dp))
                state.topRiskyApps.take(3).forEach { app ->
                    RiskyAppCard(app = app, onClick = { onAppClick(app.packageName) })
                    Spacer(Modifier.height(6.dp))
                }

                // 5. Recent Changes
                if (state.recentChanges.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    SectionHeader("Recent Changes", null) {}
                    Spacer(Modifier.height(6.dp))
                    RecentChangesSection(state.recentChanges, onAppClick)
                }

            Spacer(Modifier.height(20.dp))
        }
    }
}

// ── Compact Score Card ──────────────────────────────────────────────────────

@Composable
private fun CompactScoreCard(
    privacyScore: PrivacyScore,
    summary: String,
    onFixGroup: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val scoreColor = when {
        privacyScore.total >= 75 -> RiskSafe
        privacyScore.total >= 50 -> RiskMedium
        privacyScore.total >= 25 -> RiskHigh
        else -> RiskCritical
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            // Score row: number left, details right
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Big score number
                Text(
                    "${privacyScore.total}",
                    fontSize = 44.sp,
                    fontFamily = PressStart2P,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Privacy Score", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(scoreColor.copy(alpha = 0.15f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(privacyScore.total / 100f)
                                .background(scoreColor)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    // Sub-scores inline
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SubScoreInline("Perms", privacyScore.permissionScore)
                        SubScoreInline("Trackers", privacyScore.trackerScore)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Summary
            Text(
                summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Breakdown toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (expanded) "Hide breakdown" else "What's affecting your score?",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    if (privacyScore.groupBreakdowns.isNotEmpty()) {
                        privacyScore.groupBreakdowns.forEach { breakdown ->
                            GroupBreakdownRow(breakdown, onFix = { onFixGroup(breakdown.group.name) })
                            Spacer(Modifier.height(5.dp))
                        }
                    } else {
                        Text("No sensitive permissions granted!", style = MaterialTheme.typography.bodySmall, color = RiskSafe)
                    }
                }
            }
        }
    }
}

@Composable
private fun SubScoreInline(label: String, score: Int) {
    val color = when {
        score >= 75 -> RiskSafe
        score >= 50 -> RiskMedium
        else -> RiskHigh
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Text("$score", fontFamily = JetBrainsMono, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// ── Quick Actions ───────────────────────────────────────────────────────────

@Composable
private fun QuickActionsRow(onNavigate: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction(Icons.Outlined.Timeline, "Activity", Modifier.weight(1f)) { onNavigate(Routes.ACTIVITY_MONITOR) }
            QuickAction(Icons.Outlined.SwapHoriz, "Privacy Picks", Modifier.weight(1f)) { onNavigate(Routes.ALTERNATIVES) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction(Icons.Outlined.Search, "App Lookup", Modifier.weight(1f)) { onNavigate(Routes.PRE_INSTALL) }
            QuickAction(Icons.Outlined.Info, "About", Modifier.weight(1f)) { onNavigate(Routes.SETTINGS) }
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
        }
    }
}

// ── Unified Overview (Permissions / Trackers) ───────────────────────────────

@Composable
private fun UnifiedOverview(
    permissionCounts: Map<PermissionGroup, Int>,
    companies: List<CompanyOverview>,
    totalTrackers: Int,
    onFixGroup: (String) -> Unit,
    onViewAllApps: () -> Unit,
    onNavigateToRadar: (String?) -> Unit
) {
    var showTrackers by remember { mutableStateOf(false) }

    Column {
        // Tab row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(2.dp)
            ) {
                TabChip("Permissions", !showTrackers) { showTrackers = false }
                TabChip("Trackers", showTrackers) { showTrackers = true }
            }
        }

        Spacer(Modifier.height(10.dp))

        if (!showTrackers) {
            // Permission chips
            val matrixGroups = setOf(
                PermissionGroup.LOCATION, PermissionGroup.CAMERA, PermissionGroup.MICROPHONE,
                PermissionGroup.CONTACTS, PermissionGroup.STORAGE, PermissionGroup.SMS,
                PermissionGroup.PHONE, PermissionGroup.SENSORS
            )
            val groups = PermissionGroup.entries.filter { permissionCounts.containsKey(it) }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(groups) { group ->
                    val riskColor = when (group.defaultRisk) {
                        RiskLevel.CRITICAL -> RiskCritical; RiskLevel.HIGH -> RiskHigh
                        RiskLevel.MEDIUM -> RiskMedium; RiskLevel.LOW -> RiskLow
                    }
                    OverviewChip(
                        icon = group.icon,
                        label = group.label.replace("Your ", ""),
                        count = "${permissionCounts[group] ?: 0}",
                        color = riskColor,
                        onClick = if (group in matrixGroups) {{ onFixGroup(group.name) }} else null
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onViewAllApps, modifier = Modifier.align(Alignment.End)) {
                Text("View all in matrix", fontSize = 11.sp)
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, modifier = Modifier.size(14.dp))
            }
        } else {
            // Tracker chips
            if (companies.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(companies) { company ->
                        val color = when {
                            company.appCount > 10 -> RiskCritical; company.appCount > 5 -> RiskHigh
                            company.appCount > 2 -> RiskMedium; else -> RiskLow
                        }
                        OverviewChip(
                            icon = Icons.Outlined.Visibility,
                            label = company.companyName,
                            count = "${company.appCount}",
                            color = color,
                            onClick = { onNavigateToRadar(company.companyName) }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = { onNavigateToRadar(null) }, modifier = Modifier.align(Alignment.End)) {
                    Text("$totalTrackers trackers — view radar", fontSize = 11.sp)
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, modifier = Modifier.size(14.dp))
                }
            } else {
                Text("No trackers detected", style = MaterialTheme.typography.bodySmall, color = RiskSafe)
            }
        }
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Shared Chip ─────────────────────────────────────────────────────────────

@Composable
private fun OverviewChip(icon: ImageVector, label: String, count: String, color: Color, onClick: (() -> Unit)?) {
    Card(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp).widthIn(min = 72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(count, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ── Section Header ──────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, subtitle: String?, onAction: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        TextButton(onClick = onAction) {
            Text("View All", fontSize = 11.sp)
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, modifier = Modifier.size(14.dp))
        }
    }
}

// ── Group Breakdown Row ─────────────────────────────────────────────────────

@Composable
private fun GroupBreakdownRow(breakdown: GroupBreakdown, onFix: () -> Unit) {
    val riskColor = when (breakdown.group.defaultRisk) {
        RiskLevel.CRITICAL -> RiskCritical; RiskLevel.HIGH -> RiskHigh
        RiskLevel.MEDIUM -> RiskMedium; RiskLevel.LOW -> RiskLow
    }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(riskColor.copy(alpha = 0.06f)).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(breakdown.group.icon, null, tint = riskColor, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(breakdown.group.label.replace("Your ", ""), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Text("${breakdown.appsGranted} apps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (breakdown.pointsRecoverable > 0) {
            Text("+${breakdown.pointsRecoverable}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = RiskSafe, modifier = Modifier.padding(end = 4.dp))
        }
        Button(onClick = onFix, contentPadding = PaddingValues(horizontal = 10.dp), modifier = Modifier.height(28.dp), shape = RoundedCornerShape(4.dp), colors = ButtonDefaults.buttonColors(containerColor = riskColor)) {
            Text("Fix", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

// ── Risky App Card ──────────────────────────────────────────────────────────

@Composable
private fun RiskyAppCard(app: AppPermissionInfo, onClick: () -> Unit) {
    val riskColor = when {
        app.riskScore >= 75 -> RiskCritical; app.riskScore >= 50 -> RiskHigh
        app.riskScore >= 25 -> RiskMedium; else -> RiskLow
    }
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).background(riskColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("${app.riskScore}", fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = JetBrainsMono, color = riskColor)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(app.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val trackerText = if (app.trackers.isNotEmpty()) " · ${app.trackers.size} trackers" else ""
                Text("${app.permissions.count { it.isGranted }} perms$trackerText", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
        }
    }
}

// ── Recent Changes ──────────────────────────────────────────────────────────

@Composable
private fun RecentChangesSection(changes: List<PermissionChangeEntity>, onAppClick: (String) -> Unit) {
    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            changes.take(5).forEach { change ->
                val isGranted = change.isNowGranted
                val changeColor = if (isGranted) RiskHigh else RiskSafe
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).clickable { onAppClick(change.packageName) }.padding(vertical = 4.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(changeColor))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${change.appName} ${if (isGranted) "+" else "-"} ${change.permissionLabel}",
                        style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(dateFormat.format(Date(change.detectedAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
