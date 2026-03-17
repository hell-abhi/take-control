package com.akeshari.takecontrol.ui.matrix

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akeshari.takecontrol.data.model.AppPermissionInfo
import com.akeshari.takecontrol.data.model.PermissionGroup
import com.akeshari.takecontrol.data.model.RiskLevel
import com.akeshari.takecontrol.ui.theme.*
import androidx.compose.foundation.Image
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionMatrixScreen(
    onAppClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: PermissionMatrixViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showLegend by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }

    // The sensitive permission groups we show as columns
    val columns = listOf(
        PermissionGroup.LOCATION,
        PermissionGroup.CAMERA,
        PermissionGroup.MICROPHONE,
        PermissionGroup.CONTACTS,
        PermissionGroup.STORAGE,
        PermissionGroup.SMS,
        PermissionGroup.PHONE,
        PermissionGroup.SENSORS
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permission Matrix") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showLegend = !showLegend }) {
                        Icon(Icons.Outlined.Info, "Legend")
                    }
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(Icons.Outlined.FilterList, "Filters")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Legend
            AnimatedVisibility(visible = showLegend) {
                LegendCard(onDismiss = { showLegend = false })
            }

            // Search
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.search(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search apps...") },
                leadingIcon = { Icon(Icons.Outlined.Search, "Search") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            // Filters
            AnimatedVisibility(visible = showFilters) {
                FilterRow(
                    selectedFilter = state.filter,
                    onFilterSelected = { viewModel.setFilter(it) }
                )
            }

            // Column headers (sticky)
            val scrollState = rememberScrollState()
            ColumnHeaders(columns = columns, scrollState = scrollState)

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // App rows
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.filteredApps, key = { it.packageName }) { app ->
                    AppPermissionRow(
                        app = app,
                        columns = columns,
                        scrollState = scrollState,
                        onClick = { onAppClick(app.packageName) }
                    )
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendCard(onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "What the colors mean",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            LegendItem(color = Color.Gray.copy(alpha = 0.3f), label = "Not requested", description = "App doesn't use this permission")
            Spacer(Modifier.height(6.dp))
            LegendItem(color = RiskLow, label = "Denied", description = "Requested but you denied it")
            Spacer(Modifier.height(6.dp))
            LegendItem(color = RiskMedium, label = "Granted", description = "Permission is active")
            Spacer(Modifier.height(6.dp))
            LegendItem(color = RiskCritical, label = "Suspicious", description = "Granted & unusual for this type of app")
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, description: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(80.dp)
        )
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FilterRow(selectedFilter: AppFilter, onFilterSelected: (AppFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppFilter.entries.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.label, fontSize = 12.sp) }
            )
        }
    }
}

@Composable
private fun ColumnHeaders(
    columns: List<PermissionGroup>,
    scrollState: androidx.compose.foundation.ScrollState
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 8.dp)
    ) {
        // Fixed app name header
        Box(
            modifier = Modifier.width(140.dp).padding(start = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                "App",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Scrollable permission headers
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(end = 16.dp)
        ) {
            columns.forEach { group ->
                Column(
                    modifier = Modifier.width(52.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        group.icon,
                        contentDescription = group.label,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        group.label.replace("Your ", ""),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AppPermissionRow(
    app: AppPermissionInfo,
    columns: List<PermissionGroup>,
    scrollState: androidx.compose.foundation.ScrollState,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Fixed: App icon + name
        Row(
            modifier = Modifier.width(140.dp).padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon
            if (app.icon != null) {
                Image(
                    painter = rememberDrawablePainter(drawable = app.icon),
                    contentDescription = app.appName,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        app.appName.take(1),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                app.appName,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        // Scrollable: Permission dots
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(end = 16.dp)
        ) {
            columns.forEach { group ->
                Box(
                    modifier = Modifier.width(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val cellState = getPermissionCellState(app, group)
                    PermissionDot(state = cellState)
                }
            }
        }
    }
}

@Composable
private fun PermissionDot(state: CellState) {
    val color = when (state) {
        CellState.NOT_REQUESTED -> Color.Gray.copy(alpha = 0.2f)
        CellState.DENIED -> RiskLow
        CellState.GRANTED -> RiskMedium
        CellState.SUSPICIOUS -> RiskCritical
    }
    val borderColor = when (state) {
        CellState.NOT_REQUESTED -> Color.Transparent
        CellState.DENIED -> RiskLow.copy(alpha = 0.5f)
        CellState.GRANTED -> RiskMedium.copy(alpha = 0.5f)
        CellState.SUSPICIOUS -> RiskCritical.copy(alpha = 0.5f)
    }

    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.25f))
            .border(1.5.dp, borderColor, CircleShape)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
                .align(Alignment.Center)
        )
    }
}

enum class CellState { NOT_REQUESTED, DENIED, GRANTED, SUSPICIOUS }

private fun getPermissionCellState(app: AppPermissionInfo, group: PermissionGroup): CellState {
    val permsInGroup = app.permissions.filter { it.group == group }

    if (permsInGroup.isEmpty()) return CellState.NOT_REQUESTED

    val anyGranted = permsInGroup.any { it.isGranted }
    if (!anyGranted) return CellState.DENIED

    // Check if suspicious: high/critical risk permissions that are granted
    val hasSuspicious = permsInGroup.any {
        it.isGranted && (it.riskLevel == RiskLevel.CRITICAL || it.riskLevel == RiskLevel.HIGH)
    }
    return if (hasSuspicious) CellState.SUSPICIOUS else CellState.GRANTED
}
