package com.akeshari.takecontrol.ui.appdetail

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun AppDetailScreen(
    packageName: String,
    onBack: () -> Unit,
    viewModel: AppDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(packageName) { viewModel.loadApp(packageName) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.app?.appName ?: "App Details") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        val app = state.app
        if (app == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        val riskColor = when { app.riskScore >= 75 -> RiskCritical; app.riskScore >= 50 -> RiskHigh; app.riskScore >= 25 -> RiskMedium; else -> RiskLow }
        val grantedCount = app.permissions.count { it.isGranted }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Compact header — score left, details right
            item {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = riskColor.copy(alpha = 0.08f))) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Score
                        Box(Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)).background(riskColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                            Text("${app.riskScore}", fontSize = 28.sp, fontFamily = PressStart2P, fontWeight = FontWeight.Bold, color = riskColor)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            val riskLabel = when { app.riskScore >= 75 -> "Critical Risk"; app.riskScore >= 50 -> "High Risk"; app.riskScore >= 25 -> "Medium Risk"; else -> "Low Risk" }
                            Text(riskLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = riskColor)
                            Spacer(Modifier.height(4.dp))
                            // Stats chips
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatChip("$grantedCount/${app.permissions.size}", "perms")
                                if (app.trackers.isNotEmpty()) StatChip("${app.trackers.size}", "trackers", RiskHigh)
                                if (app.category != AppCategory.OTHER) StatChip(app.category.label, color = MaterialTheme.colorScheme.primary)
                            }
                            if (app.isSystemApp) {
                                Spacer(Modifier.height(4.dp))
                                Text("System App", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // 2. Narratives — what this app could do
            if (state.narratives.isNotEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = RiskCritical.copy(alpha = 0.06f))) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Text("What this app could do", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = RiskCritical)
                            Spacer(Modifier.height(6.dp))
                            state.narratives.forEach { n ->
                                Row(Modifier.padding(vertical = 2.dp)) {
                                    Text("\u2022", color = RiskCritical, modifier = Modifier.padding(end = 6.dp, top = 1.dp))
                                    Text(n, style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 3. Trackers — collapsible
            if (app.trackers.isNotEmpty()) {
                item {
                    val trackerColor = when { app.trackers.size > 5 -> RiskCritical; app.trackers.size > 2 -> RiskHigh; else -> RiskMedium }
                    CollapsibleCard(
                        title = "${app.trackers.size} Tracker${if (app.trackers.size != 1) "s" else ""} Detected",
                        icon = Icons.Outlined.Visibility,
                        color = trackerColor,
                        defaultExpanded = false
                    ) {
                        app.trackers.forEach { tracker ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(trackerColor))
                                Spacer(Modifier.width(8.dp))
                                Text(tracker.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                Text(tracker.category.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // 4. Permission groups — each collapsible
            val grouped = app.permissions.filter { it.isGranted }.groupBy { it.group }.toSortedMap(compareByDescending { it.defaultRisk.weight })

            if (grouped.isNotEmpty()) {
                item {
                    Text("Granted Permissions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                grouped.forEach { (group, perms) ->
                    item {
                        val gc = when (group.defaultRisk) { RiskLevel.CRITICAL -> RiskCritical; RiskLevel.HIGH -> RiskHigh; RiskLevel.MEDIUM -> RiskMedium; else -> RiskLow }
                        CollapsibleCard(
                            title = "${group.label} (${perms.size})",
                            icon = group.icon,
                            color = gc,
                            defaultExpanded = false
                        ) {
                            perms.forEach { perm ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                    val pc = when (perm.riskLevel) { RiskLevel.CRITICAL -> RiskCritical; RiskLevel.HIGH -> RiskHigh; RiskLevel.MEDIUM -> RiskMedium; else -> RiskLow }
                                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(pc))
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(perm.label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                    }
                                    Text(perm.riskLevel.name, style = MaterialTheme.typography.labelSmall, color = pc)
                                }
                            }
                        }
                    }
                }
            }

            // 5. Denied — collapsed summary
            val denied = app.permissions.filter { !it.isGranted }
            if (denied.isNotEmpty()) {
                item {
                    CollapsibleCard(
                        title = "Denied Permissions (${denied.size})",
                        icon = Icons.Outlined.CheckCircle,
                        color = RiskSafe,
                        defaultExpanded = false
                    ) {
                        denied.forEach { perm ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(RiskSafe))
                                Spacer(Modifier.width(8.dp))
                                Text(perm.label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // 6. Alternatives
            if (state.alternatives.isNotEmpty()) {
                item {
                    CollapsibleCard(
                        title = "Privacy-Friendly Alternatives",
                        icon = Icons.Outlined.SwapHoriz,
                        color = RiskSafe,
                        defaultExpanded = false
                    ) {
                        state.alternatives.forEach { alt ->
                            Column(Modifier.padding(vertical = 4.dp)) {
                                Text(alt.alternative, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = RiskSafe)
                                Text(alt.whyBetter, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // 7. Recent changes
            if (state.recentChanges.isNotEmpty()) {
                item {
                    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
                    CollapsibleCard(
                        title = "Permission Changes (${state.recentChanges.size})",
                        icon = Icons.Outlined.History,
                        color = MaterialTheme.colorScheme.primary,
                        defaultExpanded = false
                    ) {
                        state.recentChanges.forEach { change ->
                            val cc = if (change.isNowGranted) RiskHigh else RiskSafe
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(cc))
                                Spacer(Modifier.width(8.dp))
                                Text("${change.permissionLabel} ${if (change.isNowGranted) "granted" else "revoked"}", style = MaterialTheme.typography.bodySmall, color = cc, modifier = Modifier.weight(1f))
                                Text(dateFormat.format(Date(change.detectedAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // 8. Action button
            item {
                Button(
                    onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.fromParts("package", packageName, null) }) },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Outlined.Settings, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Open App Settings", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Reusable Components ─────────────────────────────────────────────────────

@Composable
private fun StatChip(value: String, label: String = "", color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(color.copy(alpha = 0.1f)).padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(value, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color, fontFamily = JetBrainsMono)
        if (label.isNotEmpty()) {
            Spacer(Modifier.width(2.dp))
            Text(label, fontSize = 9.sp, color = color)
        }
    }
}

@Composable
private fun CollapsibleCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    defaultExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(defaultExpanded) }
    Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { expanded = !expanded }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)
                )
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp)) {
                    content()
                }
            }
        }
    }
}
