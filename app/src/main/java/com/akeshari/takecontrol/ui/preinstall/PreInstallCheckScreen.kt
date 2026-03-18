package com.akeshari.takecontrol.ui.preinstall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akeshari.takecontrol.data.model.RiskLevel
import com.akeshari.takecontrol.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreInstallCheckScreen(
    viewModel: PreInstallCheckViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Pre-Install Check", fontFamily = PressStart2P, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search input
            item {
                Text(
                    "Check an app before installing",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = state.query,
                    onValueChange = { viewModel.updateQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Package name or Play Store URL") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.analyze() }),
                    trailingIcon = {
                        IconButton(onClick = { viewModel.analyze() }) {
                            Icon(Icons.Outlined.Search, "Search")
                        }
                    }
                )

                Spacer(Modifier.height(4.dp))
                Text(
                    "e.g. com.example.app or https://play.google.com/store/apps/details?id=com.example.app",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Loading
            if (state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(12.dp))
                            Text("Fetching report from Exodus Privacy...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Error
            state.error?.let { error ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = RiskCritical.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Warning, null, tint = RiskCritical)
                            Spacer(Modifier.width(12.dp))
                            Text(error, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Report
            state.report?.let { report ->
                // App header
                item {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                            Text(
                                report.appName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                report.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Version: ${report.version}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Tracker summary
                item {
                    val trackerColor = when {
                        report.trackerCount == 0 -> RiskSafe
                        report.trackerCount <= 3 -> RiskMedium
                        report.trackerCount <= 6 -> RiskHigh
                        else -> RiskCritical
                    }

                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = trackerColor.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Visibility, null, tint = trackerColor)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "${report.trackerCount} Trackers Detected",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = trackerColor
                                )
                            }
                            if (report.trackerNames.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                report.trackerNames.forEach { name ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 3.dp),
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
                                            name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Permission risk breakdown
                if (report.riskAssessment.isNotEmpty()) {
                    item {
                        Text(
                            "Permission Risk Assessment",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Risk summary chips
                    item {
                        val criticalCount = report.riskAssessment.count { it.riskLevel == RiskLevel.CRITICAL }
                        val highCount = report.riskAssessment.count { it.riskLevel == RiskLevel.HIGH }
                        val mediumCount = report.riskAssessment.count { it.riskLevel == RiskLevel.MEDIUM }
                        val lowCount = report.riskAssessment.count { it.riskLevel == RiskLevel.LOW }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (criticalCount > 0) RiskChip("$criticalCount Critical", RiskCritical)
                            if (highCount > 0) RiskChip("$highCount High", RiskHigh)
                            if (mediumCount > 0) RiskChip("$mediumCount Medium", RiskMedium)
                            if (lowCount > 0) RiskChip("$lowCount Low", RiskLow)
                        }
                    }

                    // Individual permissions
                    items(report.riskAssessment) { risk ->
                        val riskColor = when (risk.riskLevel) {
                            RiskLevel.CRITICAL -> RiskCritical
                            RiskLevel.HIGH -> RiskHigh
                            RiskLevel.MEDIUM -> RiskMedium
                            RiskLevel.LOW -> RiskLow
                        }
                        Card(
                            shape = RoundedCornerShape(6.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(riskColor)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        risk.label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        risk.permission.substringAfterLast("."),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    risk.riskLevel.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = riskColor
                                )
                            }
                        }
                    }
                }

                // Verdict
                item {
                    val overallRisk = when {
                        report.trackerCount > 6 || report.riskAssessment.count { it.riskLevel == RiskLevel.CRITICAL } > 2 ->
                            "High Risk" to RiskCritical
                        report.trackerCount > 3 || report.riskAssessment.any { it.riskLevel == RiskLevel.CRITICAL } ->
                            "Moderate Risk" to RiskHigh
                        report.trackerCount > 0 ->
                            "Low Risk" to RiskMedium
                        else ->
                            "Looks Safe" to RiskSafe
                    }

                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = overallRisk.second.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Verdict",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                overallRisk.first,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                fontFamily = PressStart2P,
                                fontSize = 14.sp,
                                color = overallRisk.second
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Based on ${report.trackerCount} trackers and ${report.riskAssessment.size} permissions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Attribution
                item {
                    Text(
                        "Data from Exodus Privacy — an independent privacy audit platform",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun RiskChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}
