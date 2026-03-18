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
import com.akeshari.takecontrol.data.model.*
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
                // Summary stats
                SummaryRow(state)
                Spacer(Modifier.height(16.dp))

                // Zombie apps
                if (state.zombieApps.isNotEmpty()) {
                    SectionTitle("Zombie Apps", "${state.zombieApps.size} unused apps still have access")
                    Spacer(Modifier.height(8.dp))
                    state.zombieApps.take(5).forEach { zombie ->
                        ZombieRow(zombie, onAppClick)
                        Spacer(Modifier.height(6.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Recent access by permission
                if (state.accessByPermission.isNotEmpty()) {
                    SectionTitle("Last Accessed", "When apps last used sensitive permissions")
                    Spacer(Modifier.height(8.dp))
                    state.accessByPermission.forEach { (perm, accesses) ->
                        AccessRow(perm, accesses, onAppClick)
                        Spacer(Modifier.height(6.dp))
                    }
                }

                if (state.zombieApps.isEmpty() && state.accessByPermission.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = RiskSafe.copy(alpha = 0.1f))
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = RiskSafe, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("All clear. No zombie apps or suspicious access detected.", style = MaterialTheme.typography.bodyMedium, color = RiskSafe)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
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
            Text("Activity Monitor needs Usage Access to detect zombie apps and see when permissions were last used. Data stays on your device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onGrant, modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(8.dp)) {
                Icon(Icons.Outlined.Settings, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Grant in Settings", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Summary Stats ───────────────────────────────────────────────────────────

@Composable
private fun SummaryRow(state: ActivityState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryCard(
            count = state.zombieApps.size,
            label = "Zombie\nApps",
            color = if (state.zombieApps.isNotEmpty()) RiskHigh else RiskSafe,
            icon = Icons.Outlined.DeleteSweep,
            modifier = Modifier.weight(1f)
        )
        val bgCount = state.alerts.count { it.type == AlertType.BACKGROUND_SPY }
        SummaryCard(
            count = bgCount,
            label = "Background\nAccess",
            color = if (bgCount > 0) RiskCritical else RiskSafe,
            icon = Icons.Outlined.VisibilityOff,
            modifier = Modifier.weight(1f)
        )
        val nightCount = state.alerts.count { it.type == AlertType.NIGHT_CRAWLER }
        SummaryCard(
            count = nightCount,
            label = "Night\nAccess",
            color = if (nightCount > 0) RiskCritical else RiskSafe,
            icon = Icons.Outlined.DarkMode,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryCard(count: Int, label: String, color: androidx.compose.ui.graphics.Color, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)), modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text("$count", fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 14.sp, maxLines = 2)
        }
    }
}

// ── Section Title ───────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

// ── Zombie Row ──────────────────────────────────────────────────────────────

@Composable
private fun ZombieRow(zombie: ZombieApp, onAppClick: (String) -> Unit) {
    Card(
        onClick = { onAppClick(zombie.packageName) },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(RiskHigh.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (zombie.daysSinceUsed >= 999) "?" else "${zombie.daysSinceUsed}d",
                    fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RiskHigh
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(zombie.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    zombie.dangerousPermissions.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall, color = RiskHigh, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ── Access Row ──────────────────────────────────────────────────────────────

@Composable
private fun AccessRow(permission: String, accesses: List<PermissionAccessRecord>, onAppClick: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val color = when (permission) {
        "Location", "Approx Location" -> RiskHigh
        "Camera", "Microphone" -> RiskCritical
        else -> RiskMedium
    }
    val now = remember { System.currentTimeMillis() }

    Card(
        onClick = { expanded = !expanded },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(4.dp, 20.dp).clip(RoundedCornerShape(2.dp)).background(color))
                Spacer(Modifier.width(10.dp))
                Text(permission, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                // Most recent access
                val latest = accesses.firstOrNull()
                if (latest != null) {
                    if (latest.isBackground) {
                        Box(Modifier.clip(RoundedCornerShape(3.dp)).background(RiskCritical.copy(alpha = 0.15f)).padding(horizontal = 4.dp, vertical = 1.dp)) {
                            Text("BG", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = RiskCritical)
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(formatAgo(now - latest.lastAccessTime), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(4.dp))
                Icon(if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(Modifier.padding(top = 8.dp, start = 14.dp)) {
                    accesses.take(10).forEach { access ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).clickable { onAppClick(access.packageName) }.padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(access.appName, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (access.isBackground) {
                                Box(Modifier.clip(RoundedCornerShape(3.dp)).background(RiskCritical.copy(alpha = 0.15f)).padding(horizontal = 4.dp, vertical = 1.dp)) {
                                    Text("BG", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = RiskCritical)
                                }
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(formatAgo(now - access.lastAccessTime), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

private fun formatAgo(millis: Long): String {
    val m = millis / 60000; val h = millis / 3600000; val d = millis / 86400000
    return when { m < 1 -> "now"; m < 60 -> "${m}m"; h < 24 -> "${h}h"; d < 30 -> "${d}d"; else -> "${d/30}mo" }
}
