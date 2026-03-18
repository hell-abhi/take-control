package com.akeshari.takecontrol.ui.activity

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akeshari.takecontrol.data.scanner.AppUsageInfo
import com.akeshari.takecontrol.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    onBack: () -> Unit,
    onAppClick: (String) -> Unit,
    viewModel: ActivityViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.checkPermission()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity Monitor", fontFamily = PressStart2P, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(8.dp))

            if (!state.hasPermission) {
                PermissionCard { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
            } else if (state.isLoading) {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        Text("Analyzing app usage...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                // Permission Budget
                PermissionBudgetCard(state)
                Spacer(Modifier.height(18.dp))

                // Zombie Apps (collapsible)
                if (state.zombieApps.isNotEmpty()) {
                    CollapsibleSection(
                        title = "Zombie Apps (${state.zombieApps.size})",
                        subtitle = "Not opened in 30+ days but still have sensitive permissions. Tap any app to review and revoke access."
                    ) {
                        state.zombieApps.forEach { app ->
                            ZombieRow(app, onAppClick)
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                // Usage vs Exposure (collapsible)
                if (state.overExposed.isNotEmpty()) {
                    CollapsibleSection(
                        title = "Usage vs Exposure (${state.overExposed.size})",
                        subtitle = "Apps ranked by privacy cost relative to how much you actually use them. A high bar means the app has lots of access but you barely use it — consider revoking permissions."
                    ) {
                        state.overExposed.forEach { app ->
                            ExposureRow(app, onAppClick)
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }

                if (state.zombieApps.isEmpty() && state.overExposed.isEmpty()) {
                    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = RiskSafe.copy(alpha = 0.1f))) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = RiskSafe, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Looking good! No zombie apps or over-exposed apps found.", style = MaterialTheme.typography.bodyMedium, color = RiskSafe)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Collapsible Section ─────────────────────────────────────────────────────

@Composable
private fun CollapsibleSection(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    var expanded by remember { mutableStateOf(true) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { expanded = !expanded }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
            Column { content() }
        }
    }
}

// ── Permission Budget ───────────────────────────────────────────────────────

@Composable
private fun PermissionBudgetCard(state: ActivityState) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text("Permission Budget", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Apps with sensitive permissions (Location, Camera, Mic, Contacts, SMS, Phone)",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            // Visual: used vs unused bar
            val total = state.totalAppsWithAccess
            val used = state.appsUsedThisWeek
            val unused = state.appsNotUsedWithPerms
            if (total > 0) {
                Row(Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(6.dp))) {
                    if (used > 0) {
                        Box(Modifier.weight(used.toFloat()).fillMaxHeight().background(RiskSafe), contentAlignment = Alignment.Center) {
                            Text("$used used", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                        }
                    }
                    if (unused > 0) {
                        Box(Modifier.weight(unused.toFloat()).fillMaxHeight().background(RiskHigh), contentAlignment = Alignment.Center) {
                            Text("$unused unused", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "$total apps have sensitive access. $used were used this week, $unused were not.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Permission Request ──────────────────────────────────────────────────────

@Composable
private fun PermissionCard(onGrant: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Usage Access Required", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text("Needed to detect zombie apps and analyze usage patterns. All data stays on your device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onGrant, modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(8.dp)) {
                Text("Grant in Settings", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Zombie Row ──────────────────────────────────────────────────────────────

@Composable
private fun ZombieRow(app: AppUsageInfo, onAppClick: (String) -> Unit) {
    Card(
        onClick = { onAppClick(app.packageName) },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(app.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(app.lastOpened, style = MaterialTheme.typography.labelSmall, color = RiskHigh, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(6.dp))
            // Permission group icons with labels
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                app.permissionGroups.take(5).forEach { pg ->
                    val color = when (pg.icon.defaultRisk) {
                        com.akeshari.takecontrol.data.model.RiskLevel.CRITICAL -> RiskCritical
                        com.akeshari.takecontrol.data.model.RiskLevel.HIGH -> RiskHigh
                        com.akeshari.takecontrol.data.model.RiskLevel.MEDIUM -> RiskMedium
                        else -> RiskLow
                    }
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(color.copy(alpha = 0.1f)).padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(pg.icon.icon, null, tint = color, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(pg.groupName, fontSize = 9.sp, color = color, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// ── Exposure Row ────────────────────────────────────────────────────────────

@Composable
private fun ExposureRow(app: AppUsageInfo, onAppClick: (String) -> Unit) {
    val maxRatio = 20f
    val barFraction = (app.exposureRatio / maxRatio).coerceIn(0f, 1f)
    val barColor = when {
        app.exposureRatio > 10 -> RiskCritical
        app.exposureRatio > 5 -> RiskHigh
        app.exposureRatio > 2 -> RiskMedium
        else -> RiskLow
    }

    Card(
        onClick = { onAppClick(app.packageName) },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            // App name + tracker badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(app.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (app.trackerCount > 0) {
                    Box(Modifier.clip(RoundedCornerShape(3.dp)).background(RiskHigh.copy(alpha = 0.1f)).padding(horizontal = 5.dp, vertical = 1.dp)) {
                        Text("${app.trackerCount} trackers", fontSize = 9.sp, color = RiskHigh, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))

            // Permission groups + usage
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Permission group icons
                app.permissionGroups.take(4).forEach { pg ->
                    val color = when (pg.icon.defaultRisk) {
                        com.akeshari.takecontrol.data.model.RiskLevel.CRITICAL -> RiskCritical
                        com.akeshari.takecontrol.data.model.RiskLevel.HIGH -> RiskHigh
                        com.akeshari.takecontrol.data.model.RiskLevel.MEDIUM -> RiskMedium
                        else -> RiskLow
                    }
                    Icon(pg.icon.icon, null, tint = color, modifier = Modifier.size(14.dp).padding(end = 2.dp))
                }
                Spacer(Modifier.weight(1f))
                Text(
                    if (app.weeklyMinutes > 0) "${app.weeklyMinutes}m/week" else "Not used this week",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))

            // Exposure bar with labels
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Low", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(barColor.copy(alpha = 0.12f))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(barFraction).clip(RoundedCornerShape(3.dp)).background(barColor))
                }
                Spacer(Modifier.width(4.dp))
                Text("High", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
