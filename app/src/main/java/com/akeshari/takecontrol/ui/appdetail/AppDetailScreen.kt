package com.akeshari.takecontrol.ui.appdetail

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
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
import com.akeshari.takecontrol.util.AppAlternatives
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

    LaunchedEffect(packageName) {
        viewModel.loadApp(packageName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.app?.appName ?: "App Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                },
                actions = {}
            )
        }
    ) { padding ->
        val app = state.app
        if (app == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Risk score header
            item {
                RiskScoreHeader(
                    appName = app.appName,
                    riskScore = app.riskScore,
                    grantedCount = app.permissions.count { it.isGranted },
                    totalCount = app.permissions.size,
                    isSystemApp = app.isSystemApp,
                    category = app.category
                )
            }

            // Privacy narratives (Feature 2)
            if (state.narratives.isNotEmpty()) {
                item {
                    NarrativeCard(narratives = state.narratives)
                }
            }

            // Take Action panel (Feature 1)
            item {
                ActionPanel(
                    packageName = packageName,
                    isSystemApp = app.isSystemApp
                )
            }

            // Trackers (Feature 4)
            if (app.trackers.isNotEmpty()) {
                item {
                    TrackerCard(trackers = app.trackers)
                }
            }

            // Alternative apps (Feature 1)
            if (state.alternatives.isNotEmpty()) {
                item {
                    AlternativesCard(alternatives = state.alternatives)
                }
            }

            // Permission timeline (Feature 3)
            if (state.recentChanges.isNotEmpty()) {
                item {
                    TimelineCard(changes = state.recentChanges)
                }
            }

            // Group permissions by category
            val grouped = app.permissions
                .filter { it.isGranted }
                .groupBy { it.group }
                .toSortedMap(compareByDescending { it.defaultRisk.weight })

            grouped.forEach { (group, permissions) ->
                item {
                    Spacer(Modifier.height(8.dp))
                    PermissionGroupHeader(group = group, count = permissions.size)
                }
                items(permissions) { permission ->
                    PermissionItem(permission = permission)
                }
            }

            // Denied permissions
            val denied = app.permissions.filter { !it.isGranted }
            if (denied.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Denied Permissions (${denied.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(denied) { permission ->
                    PermissionItem(permission = permission)
                }
            }
        }
    }
}

// ── Privacy Narrative Card (Feature 2) ──────────────────────────────────────

@Composable
private fun NarrativeCard(narratives: List<String>) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = RiskCritical.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = RiskCritical,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "What this app could do",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = RiskCritical
                )
            }
            Spacer(Modifier.height(12.dp))
            narratives.forEach { narrative ->
                Row(modifier = Modifier.padding(vertical = 3.dp)) {
                    Text(
                        "\u2022",
                        color = RiskCritical,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp, top = 1.dp)
                    )
                    Text(
                        narrative,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun tryStartActivity(context: Context, vararg intents: Intent): Boolean {
    for (intent in intents) {
        try {
            context.startActivity(intent)
            return true
        } catch (_: Exception) {
            continue
        }
    }
    return false
}

// ── Take Action Panel (Feature 1) ───────────────────────────────────────────

@Composable
private fun ActionPanel(packageName: String, isSystemApp: Boolean) {
    val context = LocalContext.current

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
            Text(
                "Take Action",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Manage Permissions
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Outlined.Shield, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Manage", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                // Restrict Background
                OutlinedButton(
                    onClick = {
                        // Try multiple approaches to open app-specific battery settings
                        val launched = tryStartActivity(context,
                            // 1. Direct component (works on Samsung, AOSP 12+)
                            Intent().apply {
                                component = ComponentName(
                                    "com.android.settings",
                                    "com.android.settings.Settings\$AppBatteryUsageActivity"
                                )
                                putExtra("android.provider.extra.APP_PACKAGE", packageName)
                            },
                            // 2. Standard app battery settings action
                            Intent("android.settings.APP_BATTERY_SETTINGS").apply {
                                data = Uri.fromParts("package", packageName, null)
                            },
                            // 3. Fallback: general battery optimization list
                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        )
                        if (!launched) {
                            // Last resort: app info page
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", packageName, null)
                                }
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Outlined.BatteryAlert, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Restrict", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                // Uninstall (not for system apps)
                if (!isSystemApp) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DELETE).apply {
                                data = Uri.parse("package:$packageName")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RiskCritical),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Uninstall", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    }
}

// ── Tracker Card (Feature 4) ────────────────────────────────────────────────

@Composable
private fun TrackerCard(trackers: List<TrackerInfo>) {
    val trackerColor = when {
        trackers.size <= 2 -> RiskMedium
        trackers.size <= 5 -> RiskHigh
        else -> RiskCritical
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = trackerColor.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Visibility,
                    contentDescription = null,
                    tint = trackerColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${trackers.size} Tracker${if (trackers.size != 1) "s" else ""} Detected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = trackerColor
                )
            }
            Spacer(Modifier.height(12.dp))

            trackers.forEach { tracker ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(trackerColor)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        tracker.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        tracker.category.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Alternative Apps (Feature 1) ────────────────────────────────────────────

@Composable
private fun AlternativesCard(alternatives: List<AppAlternatives.Alternative>) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = RiskSafe.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.SwapHoriz,
                    contentDescription = null,
                    tint = RiskSafe,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Privacy-Friendly Alternatives",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = RiskSafe
                )
            }
            Spacer(Modifier.height(12.dp))

            alternatives.forEach { alt ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        alt.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        alt.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Permission Timeline (Feature 3) ─────────────────────────────────────────

@Composable
private fun TimelineCard(changes: List<PermissionChangeEntity>) {
    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Permission Changes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))

            changes.forEach { change ->
                val isGranted = change.isNowGranted
                val changeColor = if (isGranted) RiskHigh else RiskSafe
                val verb = if (isGranted) "granted" else "revoked"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
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
                            "${change.permissionLabel} $verb",
                            style = MaterialTheme.typography.bodyMedium,
                            color = changeColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        dateFormat.format(Date(change.detectedAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Existing Components ─────────────────────────────────────────────────────

@Composable
private fun RiskScoreHeader(
    appName: String,
    riskScore: Int,
    grantedCount: Int,
    totalCount: Int,
    isSystemApp: Boolean,
    category: AppCategory
) {
    val riskColor = when {
        riskScore >= 75 -> RiskCritical
        riskScore >= 50 -> RiskHigh
        riskScore >= 25 -> RiskMedium
        else -> RiskLow
    }
    val riskLabel = when {
        riskScore >= 75 -> "Critical Risk"
        riskScore >= 50 -> "High Risk"
        riskScore >= 25 -> "Medium Risk"
        else -> "Low Risk"
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = riskColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(riskColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$riskScore",
                    fontSize = 44.sp,
                    fontFamily = PressStart2P,
                    fontWeight = FontWeight.Bold,
                    color = riskColor
                )
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(riskColor)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    riskLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "$grantedCount of $totalCount permissions granted",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (category != AppCategory.OTHER) {
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        category.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isSystemApp) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "System App — required by your device",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PermissionGroupHeader(group: PermissionGroup, count: Int) {
    val riskColor = when (group.defaultRisk) {
        RiskLevel.CRITICAL -> RiskCritical
        RiskLevel.HIGH -> RiskHigh
        RiskLevel.MEDIUM -> RiskMedium
        RiskLevel.LOW -> RiskLow
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(4.dp, 22.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(riskColor)
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            group.icon,
            contentDescription = null,
            tint = riskColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "${group.label} ($count)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PermissionItem(permission: PermissionDetail) {
    val riskColor = when (permission.riskLevel) {
        RiskLevel.CRITICAL -> RiskCritical
        RiskLevel.HIGH -> RiskHigh
        RiskLevel.MEDIUM -> RiskMedium
        RiskLevel.LOW -> RiskLow
    }

    Card(
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (permission.isGranted) riskColor else RiskLow)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    permission.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    permission.permission.substringAfterLast("."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                if (permission.isGranted) "Granted" else "Denied",
                style = MaterialTheme.typography.labelSmall,
                color = if (permission.isGranted) riskColor else RiskLow
            )
        }
    }
}
