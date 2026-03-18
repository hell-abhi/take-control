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
import com.akeshari.takecontrol.data.model.ZombieApp
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
                        Text("Analyzing activity...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                // Summary
                SummaryRow(
                    zombieCount = state.zombieApps.size,
                    overPermCount = state.overPermissioned.size,
                    trackedCount = state.heavyTracked.size
                )

                Spacer(Modifier.height(18.dp))

                // Zombie apps
                if (state.zombieApps.isNotEmpty()) {
                    SectionTitle("Zombie Apps", "Unused 30+ days but still have access")
                    Spacer(Modifier.height(8.dp))
                    state.zombieApps.take(5).forEach { zombie ->
                        ZombieRow(zombie, onAppClick)
                        Spacer(Modifier.height(6.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                }

                // Over-permissioned
                if (state.overPermissioned.isNotEmpty()) {
                    SectionTitle("Rarely Used, Fully Granted", "Apps with sensitive access but < 5 min use today")
                    Spacer(Modifier.height(8.dp))
                    state.overPermissioned.take(5).forEach { app ->
                        UsageRow(app, onAppClick)
                        Spacer(Modifier.height(6.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                }

                // Heavy tracked
                if (state.heavyTracked.isNotEmpty()) {
                    SectionTitle("Most Tracked", "Apps with the most embedded tracking SDKs")
                    Spacer(Modifier.height(8.dp))
                    state.heavyTracked.take(5).forEach { app ->
                        TrackedRow(app, onAppClick)
                        Spacer(Modifier.height(6.dp))
                    }
                }

                if (state.zombieApps.isEmpty() && state.overPermissioned.isEmpty() && state.heavyTracked.isEmpty()) {
                    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = RiskSafe.copy(alpha = 0.1f))) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = RiskSafe, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("All clear!", style = MaterialTheme.typography.bodyMedium, color = RiskSafe)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

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

@Composable
private fun SummaryRow(zombieCount: Int, overPermCount: Int, trackedCount: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard("Zombies", zombieCount, if (zombieCount > 0) RiskHigh else RiskSafe, Icons.Outlined.DeleteSweep, Modifier.weight(1f))
        StatCard("Over-granted", overPermCount, if (overPermCount > 0) RiskMedium else RiskSafe, Icons.Outlined.Shield, Modifier.weight(1f))
        StatCard("Tracked", trackedCount, if (trackedCount > 0) RiskHigh else RiskSafe, Icons.Outlined.Visibility, Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, count: Int, color: androidx.compose.ui.graphics.Color, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)), modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Text("$count", fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ZombieRow(zombie: ZombieApp, onAppClick: (String) -> Unit) {
    Card(onClick = { onAppClick(zombie.packageName) }, shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(RiskHigh.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Text(if (zombie.daysSinceUsed >= 999) "?" else "${zombie.daysSinceUsed}d", fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RiskHigh)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(zombie.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(zombie.dangerousPermissions.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = RiskHigh, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun UsageRow(app: AppUsageInfo, onAppClick: (String) -> Unit) {
    Card(onClick = { onAppClick(app.packageName) }, shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(RiskMedium.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Text("${app.foregroundMinutesToday}m", fontFamily = JetBrainsMono, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RiskMedium)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(app.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${app.dangerousPermissions.size} sensitive perms granted", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (app.trackerCount > 0) {
                Box(Modifier.clip(RoundedCornerShape(4.dp)).background(RiskHigh.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("${app.trackerCount} trackers", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = RiskHigh)
                }
            }
        }
    }
}

@Composable
private fun TrackedRow(app: AppUsageInfo, onAppClick: (String) -> Unit) {
    Card(onClick = { onAppClick(app.packageName) }, shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(RiskCritical.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Text("${app.trackerCount}", fontFamily = JetBrainsMono, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RiskCritical)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(app.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val usageText = if (app.foregroundMinutesToday > 0) "${app.foregroundMinutesToday}m today" else "Not used today"
                Text(usageText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("risk ${app.riskScore}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = RiskHigh)
        }
    }
}
