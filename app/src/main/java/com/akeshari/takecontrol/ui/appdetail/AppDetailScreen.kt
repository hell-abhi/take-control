package com.akeshari.takecontrol.ui.appdetail

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
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
                title = {
                    Text(
                        state.app?.appName?.uppercase() ?: "APP DETAILS",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = Primary)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                        }
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Outlined.Settings, "App Settings", tint = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        val app = state.app
        if (app == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
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
            item {
                RiskScoreHeader(
                    appName = app.appName,
                    riskScore = app.riskScore,
                    grantedCount = app.permissions.count { it.isGranted },
                    totalCount = app.permissions.size,
                    isSystemApp = app.isSystemApp
                )
            }

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

            val denied = app.permissions.filter { !it.isGranted }
            if (denied.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "DENIED (${denied.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceVar,
                        letterSpacing = 1.sp
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
        riskScore >= 75 -> Accent
        riskScore >= 50 -> RiskHigh
        riskScore >= 25 -> Warning
        else -> Safe
    }
    val riskLabel = when {
        riskScore >= 75 -> "CRITICAL"
        riskScore >= 50 -> "HIGH RISK"
        riskScore >= 25 -> "MEDIUM"
        else -> "LOW RISK"
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Massive risk number with color fill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(riskColor.copy(alpha = 0.12f))
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$riskScore",
                    fontFamily = ArchivoBlack,
                    fontSize = 64.sp,
                    lineHeight = 64.sp,
                    letterSpacing = (-2).sp,
                    color = riskColor
                )
            }

            Spacer(Modifier.height(12.dp))

            // Risk label as colored tag/badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(riskColor)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    riskLabel,
                    fontFamily = ArchivoBlack,
                    fontSize = 14.sp,
                    color = Color.White,
                    letterSpacing = 2.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "$grantedCount of $totalCount permissions granted",
                fontFamily = JetBrainsMono,
                fontSize = 13.sp,
                color = OnSurfaceVar
            )

            if (isSystemApp) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(OnSurfaceVar.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        "SYSTEM APP",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVar,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionGroupHeader(group: PermissionGroup, count: Int) {
    val riskColor = when (group.defaultRisk) {
        RiskLevel.CRITICAL -> Accent
        RiskLevel.HIGH -> RiskHigh
        RiskLevel.MEDIUM -> Warning
        RiskLevel.LOW -> Safe
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        // Colored left border
        Box(
            modifier = Modifier
                .size(4.dp, 24.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(riskColor)
        )
        Spacer(Modifier.width(10.dp))
        Icon(
            group.icon,
            contentDescription = null,
            tint = riskColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "${group.label.uppercase()} ($count)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun PermissionItem(permission: PermissionDetail) {
    val riskColor = when (permission.riskLevel) {
        RiskLevel.CRITICAL -> Accent
        RiskLevel.HIGH -> RiskHigh
        RiskLevel.MEDIUM -> Warning
        RiskLevel.LOW -> Safe
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Square risk indicator
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (permission.isGranted) riskColor else Safe)
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
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = OnSurfaceVar
                )
            }
            // Status badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (permission.isGranted) riskColor.copy(alpha = 0.15f)
                        else Safe.copy(alpha = 0.15f)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    if (permission.isGranted) "GRANTED" else "DENIED",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = if (permission.isGranted) riskColor else Safe
                )
            }
        }
    }
}
