package com.akeshari.takecontrol.ui.appdetail

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akeshari.takecontrol.data.model.PermissionDetail
import com.akeshari.takecontrol.data.model.PermissionGroup
import com.akeshari.takecontrol.data.model.RiskLevel
import com.akeshari.takecontrol.ui.theme.*

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
                actions = {
                    // Open system app settings
                    IconButton(onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                        }
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Outlined.Settings, "App Settings")
                    }
                }
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
                    isSystemApp = app.isSystemApp
                )
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

@Composable
private fun RiskScoreHeader(
    appName: String,
    riskScore: Int,
    grantedCount: Int,
    totalCount: Int,
    isSystemApp: Boolean
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
        shape = RoundedCornerShape(20.dp),
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
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(riskColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$riskScore",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = riskColor
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                riskLabel,
                style = MaterialTheme.typography.titleLarge,
                color = riskColor
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "$grantedCount of $totalCount permissions granted",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            group.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
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
        shape = RoundedCornerShape(12.dp),
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
                    .size(8.dp)
                    .clip(CircleShape)
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
