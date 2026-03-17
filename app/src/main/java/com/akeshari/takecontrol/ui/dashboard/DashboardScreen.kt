package com.akeshari.takecontrol.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akeshari.takecontrol.data.model.AppPermissionInfo
import com.akeshari.takecontrol.data.model.GroupBreakdown
import com.akeshari.takecontrol.data.model.PermissionGroup
import com.akeshari.takecontrol.data.model.PrivacyScore
import com.akeshari.takecontrol.data.model.RiskLevel
import com.akeshari.takecontrol.ui.theme.*

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
                            tint = Primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "TAKE CONTROL",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Outlined.Refresh, "Refresh", tint = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Primary)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "SCANNING",
                        style = MaterialTheme.typography.headlineSmall,
                        color = OnSurfaceVar,
                        letterSpacing = 4.sp
                    )
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

                PrivacyScoreCard(
                    privacyScore = state.privacyScore,
                    userAppCount = state.userAppCount,
                    systemAppCount = state.systemAppCount,
                    totalPermissions = state.totalPermissions,
                    onViewMatrix = onViewAllApps,
                    onFixGroup = onFixGroup
                )

                Spacer(Modifier.height(28.dp))

                // Section divider — color block
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Primary, PrimaryDim, Color.Transparent)
                            )
                        )
                )
                Spacer(Modifier.height(20.dp))

                Text(
                    "PERMISSIONS",
                    style = MaterialTheme.typography.headlineSmall,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(14.dp))
                PermissionGroupGrid(state.permissionGroupCounts)

                Spacer(Modifier.height(28.dp))

                // Section divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Accent, Warning, Color.Transparent)
                            )
                        )
                )
                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "HIGHEST RISK",
                        style = MaterialTheme.typography.headlineSmall,
                        letterSpacing = 2.sp
                    )
                    TextButton(onClick = onViewAllApps) {
                        Text(
                            "VIEW ALL",
                            style = MaterialTheme.typography.labelLarge,
                            color = Primary,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Primary
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                state.topRiskyApps.forEachIndexed { index, app ->
                    RiskyAppCard(app = app, onClick = { onAppClick(app.packageName) }, index = index)
                    Spacer(Modifier.height(10.dp))
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ── Privacy Score Card ──────────────────────────────────────────────────────

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
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "score"
    )
    var expanded by remember { mutableStateOf(false) }

    val scoreColor = when {
        privacyScore.total >= 75 -> Safe
        privacyScore.total >= 50 -> Warning
        privacyScore.total >= 25 -> RiskHigh
        else -> Accent
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceVariant
        )
    ) {
        Box {
            // Gradient accent stripe along left edge
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(listOf(scoreColor, Primary))
                    )
                    .align(Alignment.CenterStart)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 5.dp)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Oversized score number
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(scoreColor.copy(alpha = 0.12f))
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${animatedScore.toInt()}",
                            fontFamily = ArchivoBlack,
                            fontSize = 80.sp,
                            lineHeight = 80.sp,
                            letterSpacing = (-3).sp,
                            color = scoreColor
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "PRIVACY SCORE",
                            style = MaterialTheme.typography.labelLarge,
                            color = OnSurfaceVar,
                            letterSpacing = 3.sp
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Stats row with monospace numbers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(value = "$userAppCount", label = "Apps")
                    StatItem(value = "$totalPermissions", label = "Granted")
                    StatItem(value = "$systemAppCount", label = "System")
                }

                Spacer(Modifier.height(16.dp))

                // Breakdown toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { expanded = !expanded }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (expanded) "HIDE BREAKDOWN" else "WHAT'S HURTING YOUR SCORE?",
                        style = MaterialTheme.typography.labelLarge,
                        color = Primary,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Breakdown
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                    exit = shrinkVertically()
                ) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Text(
                            "Score = % of sensitive risk you've avoided",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVar
                        )
                        Spacer(Modifier.height(12.dp))

                        privacyScore.groupBreakdowns.forEach { breakdown ->
                            GroupBreakdownRow(
                                breakdown = breakdown,
                                onFix = { onFixGroup(breakdown.group.name) }
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        if (privacyScore.groupBreakdowns.isEmpty()) {
                            Text(
                                "No sensitive permissions granted — fully in control!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Safe
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Each \"Fix\" shows which apps to revoke. Score updates instantly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVar
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupBreakdownRow(breakdown: GroupBreakdown, onFix: () -> Unit) {
    val riskColor = when (breakdown.group.defaultRisk) {
        RiskLevel.CRITICAL -> Accent
        RiskLevel.HIGH -> RiskHigh
        RiskLevel.MEDIUM -> Warning
        RiskLevel.LOW -> Safe
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(riskColor.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color block indicator (square, not circle)
        Box(
            modifier = Modifier
                .size(4.dp, 32.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(riskColor)
        )
        Spacer(Modifier.width(12.dp))
        Icon(
            breakdown.group.icon,
            contentDescription = null,
            tint = riskColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                breakdown.group.label.replace("Your ", ""),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${breakdown.appsGranted} apps have access",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVar
            )
        }

        if (breakdown.pointsRecoverable > 0) {
            Text(
                "+${breakdown.pointsRecoverable}",
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Safe,
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        // Fix button as filled chip
        Button(
            onClick = onFix,
            modifier = Modifier.height(32.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = riskColor,
                contentColor = Color.White
            )
        ) {
            Text(
                "FIX",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

// ── Stat / Group / App Cards ────────────────────────────────────────────────

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            fontFamily = JetBrainsMono,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Primary
        )
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVar,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun PermissionGroupGrid(groupCounts: Map<PermissionGroup, Int>) {
    val groups = PermissionGroup.entries.filter { groupCounts.containsKey(it) }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(groups) { group ->
            PermissionGroupCard(
                icon = group.icon,
                label = group.label,
                count = groupCounts[group] ?: 0,
                riskColor = when (group.defaultRisk) {
                    RiskLevel.CRITICAL -> Accent
                    RiskLevel.HIGH -> RiskHigh
                    RiskLevel.MEDIUM -> Warning
                    RiskLevel.LOW -> Safe
                }
            )
        }
    }
}

@Composable
private fun PermissionGroupCard(
    icon: ImageVector,
    label: String,
    count: Int,
    riskColor: Color
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceVariant
        )
    ) {
        Box {
            // Left border stripe
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(riskColor)
                    .align(Alignment.CenterStart)
            )
            Column(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = riskColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "$count",
                    fontFamily = JetBrainsMono,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnBackground
                )
                Text(
                    "apps",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVar
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    label.replace("Your ", ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVar
                )
            }
        }
    }
}

@Composable
private fun RiskyAppCard(app: AppPermissionInfo, onClick: () -> Unit, index: Int) {
    val riskColor = when {
        app.riskScore >= 75 -> Accent
        app.riskScore >= 50 -> RiskHigh
        app.riskScore >= 25 -> Warning
        else -> Safe
    }

    val grantedCount = app.permissions.count { it.isGranted }
    val totalCount = app.permissions.size
    val ratio = if (totalCount > 0) grantedCount.toFloat() / totalCount else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Oversized risk number
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(riskColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${app.riskScore}",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = riskColor
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                // Geometric indicator bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(OnSurfaceVar.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ratio)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(riskColor)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "$grantedCount / $totalCount permissions granted",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = JetBrainsMono,
                    color = OnSurfaceVar,
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = "View details",
                tint = OnSurfaceVar,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
