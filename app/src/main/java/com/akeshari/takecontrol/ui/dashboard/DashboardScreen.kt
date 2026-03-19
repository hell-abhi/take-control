package com.akeshari.takecontrol.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import android.content.Context
import android.content.Intent
import android.net.Uri
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
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .statusBarsPadding()
    ) {
        // Brand header — always visible
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text("Take Control", fontFamily = PressStart2P, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = { viewModel.refresh() }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.Refresh, "Refresh", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(10.dp))

        // 1. Score — loading state or real data
        if (state.isLoading) {
            ScanningCard()
        } else {
            CompactScoreCard(state.privacyScore, state.summary, state.companyOverviews, state.recommendations, onFixGroup, onNavigate, onAppClick, onShare = { sharePrivacyScore(context, state.privacyScore, state.userAppCount, state.appsWithTrackers, state.totalTrackers) })
        }

        Spacer(Modifier.height(8.dp))

        // 2. Trust badges — always visible
        TrustBadges()

        Spacer(Modifier.height(16.dp))

        // 3. Quick Actions — always visible, usable during scan
        QuickActionsRow(onNavigate)

        Spacer(Modifier.height(16.dp))

        // 4. Overview
        if (!state.isLoading) {
            UnifiedOverview(
                permissionCounts = state.permissionGroupCounts,
                companies = state.companyOverviews,
                totalTrackers = state.totalTrackers,
                onFixGroup = onFixGroup,
                onViewAllApps = onViewAllApps,
                onNavigateToRadar = onNavigateToRadar
            )
        } else {
            // Skeleton overview
            Text("Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(3) {
                    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.weight(1f)) {
                        Box(Modifier.fillMaxWidth().height(70.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 5. Top Risky Apps
        SectionHeader("Highest Risk", null, onViewAllApps)
        Spacer(Modifier.height(6.dp))
        if (!state.isLoading) {
            state.topRiskyApps.take(3).forEach { app ->
                RiskyAppCard(app = app, onClick = { onAppClick(app.packageName) })
                Spacer(Modifier.height(6.dp))
            }
        } else {
            repeat(3) {
                Card(shape = RoundedCornerShape(6.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Box(Modifier.fillMaxWidth().height(52.dp).padding(14.dp), contentAlignment = Alignment.CenterStart) {
                        Box(Modifier.fillMaxWidth(0.6f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)))
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        // 6. Recent Changes
        Spacer(Modifier.height(10.dp))
        Text("Recent Changes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        if (!state.isLoading) {
            if (state.recentChanges.isNotEmpty()) {
                RecentChangesSection(state.recentChanges, onAppClick)
            } else {
                Text(
                    "Permission changes will appear here after your next scan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                "Scanning...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ── Scanning Card (inline, not full-screen) ─────────────────────────────────

@Composable
private fun ScanningCard() {
    val tips = remember { PRIVACY_TIPS.shuffled() }
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(4000)
            currentIndex = (currentIndex + 1) % tips.size
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
            Spacer(Modifier.height(12.dp))
            Text("Scanning your apps...", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            // Privacy tip
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Lightbulb, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Did you know?", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(4.dp))
                    AnimatedContent(
                        targetState = currentIndex,
                        transitionSpec = { (fadeIn(animationSpec = tween(500)) + slideInVertically(animationSpec = tween(500)) { it / 2 }).togetherWith(fadeOut(animationSpec = tween(300))) },
                        label = "tip"
                    ) { index ->
                        Text(tips[index], style = MaterialTheme.typography.bodySmall, lineHeight = 17.sp)
                    }
                }
            }
        }
    }
}

private val PRIVACY_TIPS = listOf(
    "Your phone has more sensors than a spy satellite from the 1990s.",
    "The average app requests 5 permissions — many it doesn't actually need.",
    "Location data can reveal where you live, work, and who you visit.",
    "A flashlight app that asks for your contacts? That's a red flag.",
    "Trackers in apps can follow you across different apps and websites.",
    "Denying a permission doesn't break most apps — they just lose that one feature.",
    "Free apps aren't free. You pay with your data.",
    "Your keyboard app can see everything you type — including passwords.",
    "Background location access means an app tracks you 24/7, even when closed.",
    "Meta's SDK is embedded in millions of apps — even ones you'd never expect.",
    "2FA codes sent via SMS can be intercepted. Use an authenticator app instead.",
    "Apps can read your clipboard — including copied passwords and links.",
    "A VPN hides your traffic from your ISP, but the VPN provider can see everything.",
    "Your advertising ID lets companies track you across every app on your phone.",
    "App permissions granted years ago may no longer be needed. Review them regularly.",
    "Over 75% of Android apps contain at least one third-party tracking SDK.",
    "Data brokers buy and sell your location data — often sourced from apps you use daily.",
    "End-to-end encryption means only you and the recipient can read the message.",
    "Open source apps let anyone verify there's no hidden tracking code.",
    "Revoking microphone access from apps you don't voice-chat with is always a good idea."
)

@Composable
private fun ScanningScreen() {
    val tips = remember { PRIVACY_TIPS.shuffled() }
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(4000)
            currentIndex = (currentIndex + 1) % tips.size
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(20.dp))
        Text("Scanning your apps...", style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.height(28.dp))

        // Privacy tip card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lightbulb, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Did you know?", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(8.dp))
                AnimatedContent(
                    targetState = currentIndex,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(500)) + slideInVertically(animationSpec = tween(500)) { it / 2 })
                            .togetherWith(fadeOut(animationSpec = tween(300)))
                    },
                    label = "tip"
                ) { index ->
                    Text(
                        tips[index],
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Trust badges
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lock, null, tint = RiskSafe, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("100% Local", style = MaterialTheme.typography.labelSmall, color = RiskSafe)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Code, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Open Source", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── Trust Badges ────────────────────────────────────────────────────────────

@Composable
private fun TrustBadges() {
    val context = androidx.compose.ui.platform.LocalContext.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = RiskSafe.copy(alpha = 0.06f)),
            modifier = Modifier.weight(1f)
        ) {
            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.VerifiedUser, null, tint = RiskSafe, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Column {
                    Text("100% Local", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = RiskSafe)
                    Text("Nothing leaves your device", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Card(
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/hell-abhi/take-control")))
            },
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.weight(1f)
        ) {
            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Code, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Column {
                    Text("Open Source", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text("View on GitHub", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ── Compact Score Card ──────────────────────────────────────────────────────

@Composable
private fun CompactScoreCard(
    privacyScore: PrivacyScore,
    summary: String,
    companyOverviews: List<CompanyOverview>,
    recommendations: List<Recommendation>,
    onFixGroup: (String) -> Unit,
    onNavigate: (String) -> Unit,
    onAppClick: (String) -> Unit,
    onShare: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    var showTrackers by remember { mutableStateOf(false) }
    var showRecommendations by remember { mutableStateOf(false) }
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
            // Score row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${privacyScore.total}", fontSize = 44.sp, fontFamily = PressStart2P, fontWeight = FontWeight.Bold, color = scoreColor)
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Privacy Score", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Outlined.Share, "Share score", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(scoreColor.copy(alpha = 0.15f))) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(privacyScore.total / 100f).background(scoreColor))
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SubScoreInline("Perms", privacyScore.permissionScore)
                        SubScoreInline("Trackers", privacyScore.trackerScore)
                    }
                }
            }

            // Summary
            Spacer(Modifier.height(6.dp))
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Breakdown toggle
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { expanded = !expanded }.padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (expanded) "Hide breakdown" else "What's affecting your score?", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Icon(if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }

            // Breakdown with Permissions/Trackers toggle
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    // Toggle
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)).padding(2.dp)
                    ) {
                        BreakdownTab("Permissions", !showTrackers) { showTrackers = false }
                        BreakdownTab("Trackers", showTrackers) { showTrackers = true }
                    }
                    Spacer(Modifier.height(8.dp))

                    if (!showTrackers) {
                        // Permission rows
                        if (privacyScore.groupBreakdowns.isNotEmpty()) {
                            privacyScore.groupBreakdowns.forEach { breakdown ->
                                GroupBreakdownRow(breakdown, onFix = { onFixGroup(breakdown.group.name) })
                                Spacer(Modifier.height(5.dp))
                            }
                        } else {
                            Text("No sensitive permissions granted!", style = MaterialTheme.typography.bodySmall, color = RiskSafe)
                        }
                    } else {
                        // Tracker company rows
                        if (companyOverviews.isNotEmpty()) {
                            companyOverviews.forEach { company ->
                                val color = when {
                                    company.appCount > 10 -> RiskCritical
                                    company.appCount > 5 -> RiskHigh
                                    company.appCount > 2 -> RiskMedium
                                    else -> RiskLow
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(color.copy(alpha = 0.06f)).padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(company.companyName.first().toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text(company.companyName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                    Text("${company.appCount} apps", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(Modifier.height(5.dp))
                            }
                        } else {
                            Text("No trackers detected!", style = MaterialTheme.typography.bodySmall, color = RiskSafe)
                        }
                    }
                }
            }

            // Recommended Actions toggle (inside score card)
            if (recommendations.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { showRecommendations = !showRecommendations }.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (showRecommendations) "Hide recommendations" else "Recommended actions (${recommendations.size})", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Icon(if (showRecommendations) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                AnimatedVisibility(visible = showRecommendations, enter = expandVertically(), exit = shrinkVertically()) {
                    Column(modifier = Modifier.padding(top = 6.dp)) {
                        recommendations.forEach { rec ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)).clickable {
                                    val route = rec.actionRoute
                                    if (route != null) {
                                        if (route.startsWith("app_detail/")) onAppClick(route.removePrefix("app_detail/"))
                                        else onNavigate(route)
                                    }
                                }.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.Lightbulb, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(rec.text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), lineHeight = 16.sp)
                                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BreakdownTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 5.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
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
            QuickAction(Icons.Outlined.GppMaybe, "Breach Check", Modifier.weight(1f)) { onNavigate(Routes.BREACH_CHECK) }
            QuickAction(Icons.Outlined.SwapHoriz, "Privacy Picks", Modifier.weight(1f)) { onNavigate(Routes.ALTERNATIVES) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction(Icons.Outlined.Search, "App Lookup", Modifier.weight(1f)) { onNavigate(Routes.preInstall()) }
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
            TextButton(onClick = onViewAllApps, modifier = Modifier.align(Alignment.End).height(30.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
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
                TextButton(onClick = { onNavigateToRadar(null) }, modifier = Modifier.align(Alignment.End).height(30.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
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

// ── Recommended Actions ─────────────────────────────────────────────────────

@Composable
private fun RecommendationsSection(
    recommendations: List<Recommendation>,
    onNavigate: (String) -> Unit,
    onAppClick: (String) -> Unit
) {
    Column {
        Text("Recommended Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        recommendations.forEach { rec ->
            Card(
                onClick = {
                    val route = rec.actionRoute
                    if (route != null) {
                        if (route.startsWith("app_detail/")) {
                            onAppClick(route.removePrefix("app_detail/"))
                        } else {
                            onNavigate(route)
                        }
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        rec.text,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

// ── Share Privacy Score ─────────────────────────────────────────────────────

private fun sharePrivacyScore(context: Context, score: PrivacyScore, appCount: Int, appsWithTrackers: Int, totalTrackers: Int) {
    // Generate a shareable image
    val width = 800
    val height = 500
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    val bgPaint = android.graphics.Paint().apply { color = 0xFF1E1E1E.toInt() }
    val primaryPaint = android.graphics.Paint().apply { color = 0xFFF5A623.toInt(); textSize = 80f; isFakeBoldText = true; isAntiAlias = true }
    val titlePaint = android.graphics.Paint().apply { color = 0xFFF0EDE8.toInt(); textSize = 32f; isFakeBoldText = true; isAntiAlias = true }
    val bodyPaint = android.graphics.Paint().apply { color = 0xFF8A8580.toInt(); textSize = 24f; isAntiAlias = true }
    val statPaint = android.graphics.Paint().apply { color = 0xFFF0EDE8.toInt(); textSize = 28f; isFakeBoldText = true; isAntiAlias = true }
    val accentPaint = android.graphics.Paint().apply { color = 0xFFF5A623.toInt(); textSize = 24f; isAntiAlias = true }

    // Background
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // Score
    canvas.drawText("${score.total}", 60f, 120f, primaryPaint)
    canvas.drawText("Privacy Score", 60f, 165f, titlePaint)

    // Sub-scores
    canvas.drawText("Permissions: ${score.permissionScore}/100", 60f, 220f, bodyPaint)
    canvas.drawText("Trackers: ${score.trackerScore}/100", 60f, 255f, bodyPaint)

    // Divider
    val divPaint = android.graphics.Paint().apply { color = 0xFF3A3A3A.toInt() }
    canvas.drawRect(60f, 280f, (width - 60).toFloat(), 282f, divPaint)

    // Stats
    canvas.drawText("$appCount apps scanned", 60f, 325f, statPaint)
    canvas.drawText("$appsWithTrackers contain trackers ($totalTrackers unique)", 60f, 365f, bodyPaint)

    // Branding
    canvas.drawText("Take Control — Your Privacy, Made Visible", 60f, 430f, accentPaint)
    canvas.drawText("play.google.com/store/apps/details?id=com.akeshari.takecontrol", 60f, 465f, bodyPaint)

    // Save to cache and share
    try {
        val file = java.io.File(context.cacheDir, "privacy_score.png")
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "My privacy score is ${score.total}/100. Check yours with Take Control!\nhttps://play.google.com/store/apps/details?id=com.akeshari.takecontrol")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share your privacy score"))
    } catch (_: Exception) {
        // Fallback to text share
        val text = "My privacy score is ${score.total}/100 (Permissions: ${score.permissionScore} | Trackers: ${score.trackerScore}). Check yours with Take Control!"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share your privacy score"))
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
