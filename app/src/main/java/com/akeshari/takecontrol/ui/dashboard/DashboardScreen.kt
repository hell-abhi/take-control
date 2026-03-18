package com.akeshari.takecontrol.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akeshari.takecontrol.data.database.entity.PermissionChangeEntity
import com.akeshari.takecontrol.data.model.*
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
    onAppClick: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Take Control", fontFamily = PressStart2P, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Outlined.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Text("Scanning your apps...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                // 1. Dynamic summary
                Spacer(Modifier.height(4.dp))
                Text(
                    state.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                // 2. Composite Privacy Score
                CompositeScoreCard(
                    privacyScore = state.privacyScore,
                    userAppCount = state.userAppCount,
                    totalPermissions = state.totalPermissions,
                    appsWithTrackers = state.appsWithTrackers,
                    onFixGroup = onFixGroup
                )

                Spacer(Modifier.height(24.dp))

                // 3. Permission Overview
                SectionHeader("Permission Overview", "Tap to see which apps", onViewAllApps)
                Spacer(Modifier.height(10.dp))
                PermissionGroupGrid(state.permissionGroupCounts, onFixGroup)

                Spacer(Modifier.height(24.dp))

                // 4. Tracking Overview
                if (state.companyOverviews.isNotEmpty()) {
                    SectionHeader("Tracking Overview", "${state.totalTrackers} trackers from ${state.companyOverviews.size} companies") { onNavigateToRadar(null) }
                    Spacer(Modifier.height(10.dp))
                    CompanyOverviewGrid(state.companyOverviews, onNavigateToRadar)
                    Spacer(Modifier.height(24.dp))
                }

                // 5. Highest Risk Apps
                SectionHeader("Highest Risk Apps", null, onViewAllApps)
                Spacer(Modifier.height(8.dp))
                state.topRiskyApps.forEach { app ->
                    RiskyAppCard(app = app, onClick = { onAppClick(app.packageName) })
                    Spacer(Modifier.height(8.dp))
                }

                // 6. Recent Permission Changes
                if (state.recentChanges.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Recent Changes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    RecentChangesSection(state.recentChanges, onAppClick)
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ── Section Header ──────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, subtitle: String?, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        TextButton(onClick = onAction) {
            Text("View All", fontSize = 12.sp)
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, modifier = Modifier.size(14.dp))
        }
    }
}

// ── Composite Score Card ────────────────────────────────────────────────────

@Composable
private fun CompositeScoreCard(
    privacyScore: PrivacyScore,
    userAppCount: Int,
    totalPermissions: Int,
    appsWithTrackers: Int,
    onFixGroup: (String) -> Unit
) {
    val animatedScore by animateFloatAsState(
        targetValue = privacyScore.total.toFloat(),
        animationSpec = tween(1000),
        label = "score"
    )
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Score arc
            val scoreColor = when {
                privacyScore.total >= 75 -> RiskSafe
                privacyScore.total >= 50 -> RiskMedium
                privacyScore.total >= 25 -> RiskHigh
                else -> RiskCritical
            }

            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(150.dp)) {
                    val strokeWidth = 18.dp.toPx()
                    drawArc(
                        color = Color.Gray.copy(alpha = 0.2f),
                        startAngle = 135f, sweepAngle = 270f, useCenter = false,
                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Square)
                    )
                    drawArc(
                        color = scoreColor,
                        startAngle = 135f, sweepAngle = 270f * (animatedScore / 100f), useCenter = false,
                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Square)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${animatedScore.toInt()}",
                        fontSize = 48.sp,
                        fontFamily = PressStart2P, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Privacy Score",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Sub-scores bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SubScoreChip("Permissions", privacyScore.permissionScore)
                SubScoreChip("Trackers", privacyScore.trackerScore)
            }

            Spacer(Modifier.height(12.dp))

            // Quick stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickStat("$userAppCount", "Apps")
                QuickStat("$totalPermissions", "Granted")
                QuickStat("$appsWithTrackers", "Tracked")
            }

            Spacer(Modifier.height(14.dp))

            // Score breakdown toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { expanded = !expanded }
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (expanded) "Hide breakdown" else "What's affecting your score?",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)
                )
            }

            // Breakdown
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        "Permission score = % of sensitive risk avoided. Tracker score = how few tracking SDKs your apps contain.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    privacyScore.groupBreakdowns.forEach { breakdown ->
                        GroupBreakdownRow(breakdown, onFix = { onFixGroup(breakdown.group.name) })
                        Spacer(Modifier.height(6.dp))
                    }

                    if (privacyScore.groupBreakdowns.isEmpty()) {
                        Text(
                            "No sensitive permissions granted — you're fully in control!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = RiskSafe
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Each \"Fix\" shows which apps to revoke.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SubScoreChip(label: String, score: Int) {
    val color = when {
        score >= 75 -> RiskSafe
        score >= 50 -> RiskMedium
        score >= 25 -> RiskHigh
        else -> RiskCritical
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Text(
            "$score",
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = color
        )
    }
}

@Composable
private fun QuickStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GroupBreakdownRow(breakdown: GroupBreakdown, onFix: () -> Unit) {
    val riskColor = when (breakdown.group.defaultRisk) {
        RiskLevel.CRITICAL -> RiskCritical
        RiskLevel.HIGH -> RiskHigh
        RiskLevel.MEDIUM -> RiskMedium
        RiskLevel.LOW -> RiskLow
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(riskColor.copy(alpha = 0.06f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(breakdown.group.icon, null, tint = riskColor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(breakdown.group.label.replace("Your ", ""), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text("${breakdown.appsGranted} apps have access", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (breakdown.pointsRecoverable > 0) {
            Text("+${breakdown.pointsRecoverable}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RiskSafe, modifier = Modifier.padding(end = 4.dp))
        }
        Button(
            onClick = onFix,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            modifier = Modifier.height(32.dp),
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = riskColor)
        ) {
            Text("Fix", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

// ── Permission Overview ─────────────────────────────────────────────────────

@Composable
private fun PermissionGroupGrid(groupCounts: Map<PermissionGroup, Int>, onGroupClick: (String) -> Unit) {
    val groups = PermissionGroup.entries.filter { groupCounts.containsKey(it) }
    val matrixGroups = setOf(
        PermissionGroup.LOCATION, PermissionGroup.CAMERA, PermissionGroup.MICROPHONE,
        PermissionGroup.CONTACTS, PermissionGroup.STORAGE, PermissionGroup.SMS,
        PermissionGroup.PHONE, PermissionGroup.SENSORS
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(groups) { group ->
            val riskColor = when (group.defaultRisk) {
                RiskLevel.CRITICAL -> RiskCritical
                RiskLevel.HIGH -> RiskHigh
                RiskLevel.MEDIUM -> RiskMedium
                RiskLevel.LOW -> RiskLow
            }
            OverviewChip(
                icon = group.icon,
                label = group.label.replace("Your ", ""),
                count = "${groupCounts[group] ?: 0} apps",
                color = riskColor,
                onClick = if (group in matrixGroups) {{ onGroupClick(group.name) }} else null
            )
        }
    }
}

// ── Tracking Overview ───────────────────────────────────────────────────────

@Composable
private fun CompanyOverviewGrid(companies: List<CompanyOverview>, onNavigateToRadar: (String?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(companies) { company ->
            val color = when {
                company.appCount > 10 -> RiskCritical
                company.appCount > 5 -> RiskHigh
                company.appCount > 2 -> RiskMedium
                else -> RiskLow
            }
            OverviewChip(
                icon = Icons.Outlined.Visibility,
                label = company.companyName,
                count = "${company.appCount} apps",
                color = color,
                onClick = { onNavigateToRadar(company.companyName) }
            )
        }
    }
}

// ── Shared Chip ─────────────────────────────────────────────────────────────

@Composable
private fun OverviewChip(
    icon: ImageVector,
    label: String,
    count: String,
    color: Color,
    onClick: (() -> Unit)?
) {
    Card(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp).widthIn(min = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(count, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, fontFamily = JetBrainsMono)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ── Risky App Card ──────────────────────────────────────────────────────────

@Composable
private fun RiskyAppCard(app: AppPermissionInfo, onClick: () -> Unit) {
    val riskColor = when {
        app.riskScore >= 75 -> RiskCritical
        app.riskScore >= 50 -> RiskHigh
        app.riskScore >= 25 -> RiskMedium
        else -> RiskLow
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(6.dp)).background(riskColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("${app.riskScore}", fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = JetBrainsMono, color = riskColor)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(app.appName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val trackerText = if (app.trackers.isNotEmpty()) " · ${app.trackers.size} trackers" else ""
                Text(
                    "${app.permissions.count { it.isGranted }} permissions$trackerText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, "View details", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
    }
}

// ── Recent Changes ──────────────────────────────────────────────────────────

@Composable
private fun RecentChangesSection(changes: List<PermissionChangeEntity>, onAppClick: (String) -> Unit) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            changes.forEach { change ->
                val isGranted = change.isNowGranted
                val changeColor = if (isGranted) RiskHigh else RiskSafe
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onAppClick(change.packageName) }
                        .padding(vertical = 5.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(changeColor))
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${change.appName} ${if (isGranted) "gained" else "lost"} ${change.permissionLabel}",
                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(dateFormat.format(Date(change.detectedAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(changeColor.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(if (isGranted) "Granted" else "Revoked", style = MaterialTheme.typography.labelSmall, color = changeColor, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
