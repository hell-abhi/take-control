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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.akeshari.takecontrol.data.model.RiskLevel
import com.akeshari.takecontrol.data.scanner.AppUsageInfo
import com.akeshari.takecontrol.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    onBack: () -> Unit,
    onAppClick: (String) -> Unit,
    onViewMatrix: () -> Unit = {},
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
                // 1. Permission Budget
                SectionCard(
                    title = "Permission Budget",
                    subtitle = "Apps with sensitive permissions (Location, Camera, Mic, Contacts, SMS, Phone)"
                ) {
                    val total = state.totalAppsWithAccess
                    val used = state.appsUsedThisWeek
                    val unused = state.appsNotUsedWithPerms
                    if (total > 0) {
                        Row(Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(6.dp))) {
                            if (used > 0) {
                                Box(Modifier.weight(used.toFloat()).fillMaxHeight().background(RiskSafe), contentAlignment = Alignment.Center) {
                                    Text("$used used", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                            if (unused > 0) {
                                Box(Modifier.weight(unused.toFloat()).fillMaxHeight().background(RiskHigh), contentAlignment = Alignment.Center) {
                                    Text("$unused unused", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("$total apps have sensitive access. $used used this week, $unused not.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onViewMatrix,
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("View in Permission Matrix", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, modifier = Modifier.size(14.dp))
                        }
                    } else {
                        Text("No apps with sensitive permissions found.", style = MaterialTheme.typography.bodySmall, color = RiskSafe)
                    }
                }

                Spacer(Modifier.height(10.dp))

                // 2. Zombie Apps
                if (state.zombieApps.isNotEmpty()) {
                    SectionCard(
                        title = "Zombie Apps (${state.zombieApps.size})",
                        subtitle = "Not opened in 30+ days but still have sensitive permissions. Tap to review.",
                        collapsible = true,
                        defaultExpanded = false
                    ) {
                        state.zombieApps.forEach { app ->
                            ZombieRow(app, onAppClick)
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                // 3. Usage vs Exposure
                if (state.overExposed.isNotEmpty()) {
                    SectionCard(
                        title = "Usage vs Exposure (${state.overExposed.size})",
                        subtitle = "Apps with high privacy cost relative to actual use. More permissions + less usage = higher exposure.",
                        collapsible = true,
                        defaultExpanded = false
                    ) {
                        state.overExposed.forEach { app ->
                            ExposureRow(app, onAppClick)
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }

                if (state.zombieApps.isEmpty() && state.overExposed.isEmpty()) {
                    SectionCard(title = "All Clear", subtitle = "No zombie apps or over-exposed apps found.") {
                        Icon(Icons.Outlined.CheckCircle, null, tint = RiskSafe, modifier = Modifier.size(22.dp))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Section Card (consistent container for all sections) ────────────────────

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    collapsible: Boolean = false,
    defaultExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(defaultExpanded) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            // Header
            Row(
                modifier = if (collapsible) Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable { expanded = !expanded } else Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
                }
                if (collapsible) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Content
            if (collapsible) {
                AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                    Column(Modifier.padding(top = 12.dp)) { content() }
                }
            } else {
                Spacer(Modifier.height(12.dp))
                content()
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

// ── Permission group color helper ───────────────────────────────────────────

@Composable
private fun permGroupColor(risk: RiskLevel): Color = when (risk) {
    RiskLevel.CRITICAL -> RiskCritical; RiskLevel.HIGH -> RiskHigh
    RiskLevel.MEDIUM -> RiskMedium; RiskLevel.LOW -> RiskLow
}

// ── Zombie Row ──────────────────────────────────────────────────────────────

@Composable
private fun ZombieRow(app: AppUsageInfo, onAppClick: (String) -> Unit) {
    Card(
        onClick = { onAppClick(app.packageName) },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(app.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(app.lastOpened, style = MaterialTheme.typography.labelSmall, color = RiskHigh, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(6.dp))
            // Permission chips — use FlowRow-like wrapping via multiple rows
            val chips = app.permissionGroups.take(6)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                chips.take(3).forEach { pg ->
                    val c = permGroupColor(pg.icon.defaultRisk)
                    Row(Modifier.clip(RoundedCornerShape(4.dp)).background(c.copy(alpha = 0.1f)).padding(horizontal = 5.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(pg.icon.icon, null, tint = c, modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(pg.groupName.take(8), fontSize = 9.sp, color = c, fontWeight = FontWeight.Medium, maxLines = 1)
                    }
                }
            }
            if (chips.size > 3) {
                Spacer(Modifier.height(3.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    chips.drop(3).forEach { pg ->
                        val c = permGroupColor(pg.icon.defaultRisk)
                        Row(Modifier.clip(RoundedCornerShape(4.dp)).background(c.copy(alpha = 0.1f)).padding(horizontal = 5.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(pg.icon.icon, null, tint = c, modifier = Modifier.size(11.dp))
                            Spacer(Modifier.width(2.dp))
                            Text(pg.groupName.take(8), fontSize = 9.sp, color = c, fontWeight = FontWeight.Medium, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

// ── Exposure Row ────────────────────────────────────────────────────────────

@Composable
private fun ExposureRow(app: AppUsageInfo, onAppClick: (String) -> Unit) {
    Card(
        onClick = { onAppClick(app.packageName) },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            // Name + usage
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(app.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (app.weeklyMinutes > 0) "${app.weeklyMinutes}m/wk" else "0m/wk",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = JetBrainsMono
                )
            }
            Spacer(Modifier.height(6.dp))

            // Permission icons + tracker count
            Row(verticalAlignment = Alignment.CenterVertically) {
                app.permissionGroups.take(5).forEach { pg ->
                    val c = permGroupColor(pg.icon.defaultRisk)
                    Icon(pg.icon.icon, null, tint = c, modifier = Modifier.size(14.dp).padding(end = 3.dp))
                }
                Text("${app.dangerousPermissions.size} perms", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (app.trackerCount > 0) {
                    Text(" · ${app.trackerCount} trackers", fontSize = 9.sp, color = RiskHigh)
                }
            }
        }
    }
}
