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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
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
import com.akeshari.takecontrol.data.model.AppPermissionInfo
import com.akeshari.takecontrol.data.model.PermissionGroup
import com.akeshari.takecontrol.data.model.GroupBreakdown
import com.akeshari.takecontrol.data.model.PrivacyScore
import com.akeshari.takecontrol.data.model.RiskLevel
import com.akeshari.takecontrol.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onViewAllApps: () -> Unit,
    onFixGroup: (String) -> Unit,
    onAppClick: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Take Control", fontFamily = PressStart2P, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                },
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
                Spacer(Modifier.height(8.dp))

                // Privacy Score with breakdown
                PrivacyScoreCard(
                    privacyScore = state.privacyScore,
                    userAppCount = state.userAppCount,
                    systemAppCount = state.systemAppCount,
                    totalPermissions = state.totalPermissions,
                    onViewMatrix = onViewAllApps,
                    onFixGroup = onFixGroup
                )

                Spacer(Modifier.height(24.dp))

                // Permission Groups Overview
                Text(
                    "Permission Overview",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                PermissionGroupGrid(state.permissionGroupCounts, onFixGroup)

                Spacer(Modifier.height(24.dp))

                // Top Risky Apps
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Highest Risk Apps",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onViewAllApps) {
                        Text("View All")
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                state.topRiskyApps.forEach { app ->
                    RiskyAppCard(app = app, onClick = { onAppClick(app.packageName) })
                    Spacer(Modifier.height(8.dp))
                }

                // Recent Permission Changes (Feature 3)
                if (state.recentChanges.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Recent Permission Changes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    RecentChangesSection(
                        changes = state.recentChanges,
                        onAppClick = onAppClick
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ── Privacy Score Card with Breakdown ───────────────────────────────────────

@Composable
private fun PrivacyScoreCard(
    privacyScore: PrivacyScore,
    userAppCount: Int,
    systemAppCount: Int,
    totalPermissions: Int,
    onViewMatrix: () -> Unit,
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Score arc
            Box(contentAlignment = Alignment.Center) {
                val scoreColor = when {
                    privacyScore.total >= 75 -> RiskSafe
                    privacyScore.total >= 50 -> RiskMedium
                    privacyScore.total >= 25 -> RiskHigh
                    else -> RiskCritical
                }

                Canvas(modifier = Modifier.size(150.dp)) {
                    val strokeWidth = 18.dp.toPx()
                    drawArc(
                        color = Color.Gray.copy(alpha = 0.2f),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Square)
                    )
                    drawArc(
                        color = scoreColor,
                        startAngle = 135f,
                        sweepAngle = 270f * (animatedScore / 100f),
                        useCenter = false,
                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Square)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${animatedScore.toInt()}",
                        fontSize = 52.sp,
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

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(value = "$userAppCount", label = "Apps")
                StatItem(value = "$totalPermissions", label = "Granted")
            }

            Spacer(Modifier.height(16.dp))

            // "What's affecting your score" toggle
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
                    if (expanded) "Hide score breakdown" else "What's affecting your score?",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Breakdown
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    // Explanation
                    Text(
                        "Score = % of sensitive risk you've avoided",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    // Group breakdowns (things hurting your score)
                    privacyScore.groupBreakdowns.forEach { breakdown ->
                        GroupBreakdownRow(
                            breakdown = breakdown,
                            onFix = { onFixGroup(breakdown.group.name) }
                        )
                        Spacer(Modifier.height(6.dp))
                    }

                    if (privacyScore.groupBreakdowns.isEmpty()) {
                        Text(
                            "No sensitive permissions granted — you're fully in control!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = RiskSafe
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Each \"Fix\" shows which apps to revoke. Your score updates instantly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
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
        // Icon
        Icon(
            breakdown.group.icon,
            contentDescription = null,
            tint = riskColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))

        // Description
        Column(modifier = Modifier.weight(1f)) {
            Text(
                breakdown.group.label.replace("Your ", ""),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                "${breakdown.appsGranted} apps have access",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Recoverable points
        if (breakdown.pointsRecoverable > 0) {
            Text(
                "+${breakdown.pointsRecoverable}",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = RiskSafe,
                modifier = Modifier.padding(end = 4.dp)
            )
        }

        // Fix button
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

// ── Stat / Group / App Cards ────────────────────────────────────────────────

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            fontFamily = JetBrainsMono,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PermissionGroupGrid(groupCounts: Map<PermissionGroup, Int>, onGroupClick: (String) -> Unit) {
    val groups = PermissionGroup.entries.filter { groupCounts.containsKey(it) }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(groups) { group ->
            PermissionGroupChip(
                icon = group.icon,
                label = group.label,
                count = groupCounts[group] ?: 0,
                riskColor = when (group.defaultRisk) {
                    RiskLevel.CRITICAL -> RiskCritical
                    RiskLevel.HIGH -> RiskHigh
                    RiskLevel.MEDIUM -> RiskMedium
                    RiskLevel.LOW -> RiskLow
                },
                onClick = { onGroupClick(group.name) }
            )
        }
    }
}

@Composable
private fun PermissionGroupChip(
    icon: ImageVector,
    label: String,
    count: Int,
    riskColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(riskColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = riskColor, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "$count apps",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                fontFamily = JetBrainsMono
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RiskyAppCard(app: AppPermissionInfo, onClick: () -> Unit) {
    val riskColor = when {
        app.riskScore >= 75 -> RiskCritical
        app.riskScore >= 50 -> RiskHigh
        app.riskScore >= 25 -> RiskMedium
        else -> RiskLow
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(riskColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${app.riskScore}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    fontFamily = JetBrainsMono,
                    color = riskColor
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${app.permissions.count { it.isGranted }} permissions granted",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(riskColor.copy(alpha = 0.15f))
                ) {
                    val granted = app.permissions.count { it.isGranted }
                    val total = app.permissions.size
                    val fraction = if (total > 0) granted.toFloat() / total else 0f
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .background(riskColor)
                    )
                }
            }

            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ── Recent Permission Changes (Feature 3) ───────────────────────────────────

@Composable
private fun RecentChangesSection(
    changes: List<PermissionChangeEntity>,
    onAppClick: (String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            changes.forEach { change ->
                val isGranted = change.isNowGranted
                val changeColor = if (isGranted) RiskHigh else RiskSafe
                val verb = if (isGranted) "gained" else "lost"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onAppClick(change.packageName) }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(changeColor)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${change.appName} $verb ${change.permissionLabel}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            dateFormat.format(Date(change.detectedAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(changeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            if (isGranted) "Granted" else "Revoked",
                            style = MaterialTheme.typography.labelSmall,
                            color = changeColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
