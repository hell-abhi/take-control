package com.akeshari.takecontrol.ui.activity

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
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
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
        ) {
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
                // Permission Budget summary
                PermissionBudgetCard(state)

                Spacer(Modifier.height(20.dp))

                // Zombie Apps
                if (state.zombieApps.isNotEmpty()) {
                    Text("Zombie Apps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Not opened in 30+ days, still have sensitive access", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    state.zombieApps.forEach { app ->
                        ZombieRow(app, onAppClick)
                        Spacer(Modifier.height(6.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Usage vs Exposure
                if (state.overExposed.isNotEmpty()) {
                    Text("Usage vs Exposure", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Apps ranked by privacy cost relative to how much you use them", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    state.overExposed.forEach { app ->
                        ExposureRow(app, onAppClick)
                        Spacer(Modifier.height(6.dp))
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

// ── Permission Budget Card ──────────────────────────────────────────────────

@Composable
private fun PermissionBudgetCard(state: ActivityState) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text("Permission Budget", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                BudgetStat("${state.totalAppsWithAccess}", "Have\nAccess", MaterialTheme.colorScheme.primary)
                BudgetStat("${state.appsUsedThisWeek}", "Used This\nWeek", RiskSafe)
                BudgetStat("${state.appsNotUsedWithPerms}", "Unused With\nAccess", if (state.appsNotUsedWithPerms > 0) RiskHigh else RiskSafe)
            }

            if (state.appsNotUsedWithPerms > 0) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "${state.appsNotUsedWithPerms} apps have sensitive permissions but you didn't use them this week",
                    style = MaterialTheme.typography.bodySmall,
                    color = RiskHigh
                )
            }
        }
    }
}

@Composable
private fun BudgetStat(value: String, label: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 13.sp, maxLines = 2)
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
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            // Last opened badge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(52.dp)
            ) {
                Text(app.lastOpened, style = MaterialTheme.typography.labelSmall, color = RiskHigh, fontWeight = FontWeight.SemiBold, maxLines = 2, lineHeight = 13.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(app.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                // Permission chips
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    app.dangerousPermissions.take(3).forEach { perm ->
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(RiskHigh.copy(alpha = 0.1f)).padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(perm.split(" ").first(), fontSize = 9.sp, color = RiskHigh, fontWeight = FontWeight.Medium)
                        }
                    }
                    if (app.dangerousPermissions.size > 3) {
                        Text("+${app.dangerousPermissions.size - 3}", fontSize = 9.sp, color = RiskHigh)
                    }
                }
            }
        }
    }
}

// ── Exposure Row ────────────────────────────────────────────────────────────

@Composable
private fun ExposureRow(app: AppUsageInfo, onAppClick: (String) -> Unit) {
    // Exposure bar: ratio of privacy cost to usage
    val maxRatio = 20f // cap for visual
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(app.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (app.trackerCount > 0) {
                    Box(Modifier.clip(RoundedCornerShape(3.dp)).background(RiskHigh.copy(alpha = 0.1f)).padding(horizontal = 5.dp, vertical = 1.dp)) {
                        Text("${app.trackerCount} trackers", fontSize = 9.sp, color = RiskHigh, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))

            // Usage info
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Used: ${if (app.weeklyMinutes > 0) "${app.weeklyMinutes}m/week" else "0m this week"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${app.dangerousPermissions.size} sensitive perms", style = MaterialTheme.typography.labelSmall, color = barColor)
            }

            Spacer(Modifier.height(6.dp))

            // Exposure bar
            Box(
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(barColor.copy(alpha = 0.12f))
            ) {
                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(barFraction).background(barColor))
            }
        }
    }
}
