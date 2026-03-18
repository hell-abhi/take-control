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
import com.akeshari.takecontrol.data.model.*
import com.akeshari.takecontrol.ui.common.ExplainerCard
import com.akeshari.takecontrol.ui.common.ExplainerSection
import com.akeshari.takecontrol.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    onBack: () -> Unit,
    onAppClick: (String) -> Unit,
    viewModel: ActivityViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Re-check permission when returning from Settings
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
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            ExplainerCard(
                title = "What does Activity Monitor show?",
                sections = listOf(
                    ExplainerSection(
                        "Real permission usage.",
                        "Not just what apps CAN access, but what they actually DID access — location, camera, microphone — and when."
                    ),
                    ExplainerSection(
                        "Zombie apps.",
                        "Apps you haven't opened in 30+ days but still retain dangerous permissions like location or microphone access."
                    ),
                    ExplainerSection(
                        "Suspicious patterns.",
                        "Background access while you're not using the app, or permission usage at night while you're asleep."
                    )
                )
            )

            Spacer(Modifier.height(16.dp))

            if (!state.hasPermission) {
                // Permission request card
                PermissionRequestCard {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            } else if (state.isLoading) {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        Text("Analyzing app activity...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                // Alerts
                if (state.alerts.isNotEmpty()) {
                    Text("Privacy Alerts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${state.alerts.size} issue${if (state.alerts.size != 1) "s" else ""} detected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    state.alerts.forEach { alert ->
                        AlertCard(alert, onAppClick)
                        Spacer(Modifier.height(8.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                } else {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = RiskSafe.copy(alpha = 0.1f))
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = RiskSafe, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("No privacy alerts. Your apps are behaving well.", style = MaterialTheme.typography.bodyMedium, color = RiskSafe)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Zombie apps
                if (state.zombieApps.isNotEmpty()) {
                    Text("Zombie Apps", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Apps you don't use but still have access to sensitive data",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    state.zombieApps.forEach { zombie ->
                        ZombieAppCard(zombie, onAppClick)
                        Spacer(Modifier.height(8.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Recent permission accesses by type
                if (state.accessByPermission.isNotEmpty()) {
                    Text("Recent Access Timeline", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "When apps last used sensitive permissions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    state.accessByPermission.forEach { (permission, accesses) ->
                        AccessGroupCard(permission, accesses, onAppClick)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Permission Request Card ─────────────────────────────────────────────────

@Composable
private fun PermissionRequestCard(onGrant: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text("Permission Required", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Activity Monitor needs Usage Access to see when apps last used their permissions and detect zombie apps. This is a system permission that you grant via Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Your usage data stays on your device — we never upload anything.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onGrant,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Outlined.Settings, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Open Settings to Grant", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Alert Card ──────────────────────────────────────────────────────────────

@Composable
private fun AlertCard(alert: PrivacyAlert, onAppClick: (String) -> Unit) {
    val (color, icon) = when (alert.type) {
        AlertType.ZOMBIE -> RiskHigh to Icons.Outlined.DeleteSweep
        AlertType.BACKGROUND_SPY -> RiskCritical to Icons.Outlined.VisibilityOff
        AlertType.NIGHT_CRAWLER -> RiskCritical to Icons.Outlined.DarkMode
    }

    Card(
        onClick = { onAppClick(alert.packageName) },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(alert.appName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(color.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(alert.title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = color)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(alert.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── Zombie App Card ─────────────────────────────────────────────────────────

@Composable
private fun ZombieAppCard(zombie: ZombieApp, onAppClick: (String) -> Unit) {
    Card(
        onClick = { onAppClick(zombie.packageName) },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)).background(RiskHigh.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (zombie.daysSinceUsed >= 999) "?" else "${zombie.daysSinceUsed}d",
                    fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RiskHigh
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(zombie.appName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (zombie.daysSinceUsed >= 999) "Never opened" else "Last used ${zombie.daysSinceUsed} days ago",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Still has: ${zombie.dangerousPermissions.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall, color = RiskHigh, maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ── Access Group Card ───────────────────────────────────────────────────────

@Composable
private fun AccessGroupCard(
    permission: String,
    accesses: List<PermissionAccessRecord>,
    onAppClick: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val now = System.currentTimeMillis()

    val color = when (permission) {
        "Location", "Approx Location" -> RiskHigh
        "Camera" -> RiskCritical
        "Microphone" -> RiskCritical
        "Contacts" -> RiskMedium
        else -> RiskLow
    }

    Card(
        onClick = { expanded = !expanded },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(4.dp, 20.dp).clip(RoundedCornerShape(2.dp)).background(color)
                )
                Spacer(Modifier.width(10.dp))
                Text(permission, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(
                    "${accesses.size} app${if (accesses.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    accesses.take(15).forEach { access ->
                        val timeAgo = formatTimeAgo(now - access.lastAccessTime)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onAppClick(access.packageName) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (access.isBackground) {
                                Box(
                                    modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(RiskCritical.copy(alpha = 0.15f)).padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("BG", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = RiskCritical)
                                }
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(access.appName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(timeAgo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimeAgo(millis: Long): String {
    val minutes = millis / (60 * 1000)
    val hours = millis / (60 * 60 * 1000)
    val days = millis / (24 * 60 * 60 * 1000)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 30 -> "${days}d ago"
        else -> "${days / 30}mo ago"
    }
}
