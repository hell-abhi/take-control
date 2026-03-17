package com.akeshari.takecontrol.ui.matrix

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionMatrixScreen(
    onAppClick: (String) -> Unit,
    highlightGroup: String? = null,
    viewModel: PermissionMatrixViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showLegend by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(highlightGroup != null) }

    // Apply initial group filter when navigated from "Fix"
    LaunchedEffect(highlightGroup) {
        viewModel.setInitialGroup(highlightGroup)
    }

    // Bottom sheet state
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var selectedGroup by remember { mutableStateOf<PermissionGroup?>(null) }

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

    // Permission detail bottom sheet
    if (selectedGroup != null) {
        PermissionDetailSheet(
            group = selectedGroup!!,
            apps = state.filteredApps,
            sheetState = sheetState,
            onDismiss = {
                scope.launch { sheetState.hide() }
                selectedGroup = null
            },
            onAppClick = onAppClick
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permission Matrix") },
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
                LegendCard()
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

            // Column headers (tappable)
            val scrollState = rememberScrollState()
            ColumnHeaders(
                columns = columns,
                scrollState = scrollState,
                highlightedGroup = state.highlightedGroup,
                onColumnClick = { group ->
                    selectedGroup = group
                    scope.launch { sheetState.show() }
                }
            )

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // App rows
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.filteredApps, key = { it.packageName }) { app ->
                    AppPermissionRow(
                        app = app,
                        columns = columns,
                        scrollState = scrollState,
                        highlightedGroup = state.highlightedGroup,
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

// ── Permission Detail Bottom Sheet ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionDetailSheet(
    group: PermissionGroup,
    apps: List<AppPermissionInfo>,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onAppClick: (String) -> Unit
) {
    val context = LocalContext.current
    val appsWithPermission = apps.filter { app ->
        app.permissions.any { it.group == group && it.isGranted }
    }
    val appsDenied = apps.filter { app ->
        app.permissions.any { it.group == group && !it.isGranted } &&
        !appsWithPermission.contains(app)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        group.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        group.label,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        group.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Risk badge
            val riskColor = when (group.defaultRisk) {
                RiskLevel.CRITICAL -> RiskCritical
                RiskLevel.HIGH -> RiskHigh
                RiskLevel.MEDIUM -> RiskMedium
                RiskLevel.LOW -> RiskLow
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(riskColor.copy(alpha = 0.1f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = riskColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Risk Level: ${group.defaultRisk.name}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = riskColor
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${appsWithPermission.size} apps granted",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))

            // How to disable tutorial
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "How to disable this permission",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    TutorialStep(step = "1", text = "Tap the settings icon next to an app below")
                    TutorialStep(step = "2", text = "Go to \"Permissions\" in the app info page")
                    TutorialStep(step = "3", text = "Find \"${group.label.replace("Your ", "")}\" and set to \"Don't allow\"")
                    TutorialStep(step = "4", text = "Come back and refresh to verify the change")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Apps with this permission granted
            if (appsWithPermission.isNotEmpty()) {
                Text(
                    "Apps with access (${appsWithPermission.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                appsWithPermission.forEach { app ->
                    SheetAppRow(
                        app = app,
                        isGranted = true,
                        onAppClick = { onAppClick(app.packageName) },
                        onOpenSettings = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", app.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }

            // Apps that denied
            if (appsDenied.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Apps that were denied (${appsDenied.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                appsDenied.take(5).forEach { app ->
                    SheetAppRow(
                        app = app,
                        isGranted = false,
                        onAppClick = { onAppClick(app.packageName) },
                        onOpenSettings = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", app.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun TutorialStep(step: String, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                step,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SheetAppRow(
    app: AppPermissionInfo,
    isGranted: Boolean,
    onAppClick: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                RiskCritical.copy(alpha = 0.05f)
            else
                RiskLow.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onAppClick)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon
            if (app.icon != null) {
                Image(
                    painter = rememberDrawablePainter(drawable = app.icon),
                    contentDescription = app.appName,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(app.appName.take(1), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    app.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (isGranted) "Permission granted" else "Permission denied",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isGranted) RiskCritical else RiskLow
                )
            }

            // Open settings button
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Outlined.OpenInNew,
                    contentDescription = "Open app settings",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ── Legend ───────────────────────────────────────────────────────────────────

@Composable
private fun LegendCard() {
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
            Spacer(Modifier.height(10.dp))
            Text(
                "Tap any column header to see details about that permission",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

// ── Filters ─────────────────────────────────────────────────────────────────

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

// ── Column Headers ──────────────────────────────────────────────────────────

@Composable
private fun ColumnHeaders(
    columns: List<PermissionGroup>,
    scrollState: androidx.compose.foundation.ScrollState,
    highlightedGroup: PermissionGroup?,
    onColumnClick: (PermissionGroup) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(140.dp)
                .padding(start = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                "App",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(end = 16.dp)
        ) {
            columns.forEach { group ->
                val isHighlighted = group == highlightedGroup
                Column(
                    modifier = Modifier
                        .width(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .then(
                            if (isHighlighted)
                                Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            else Modifier
                        )
                        .clickable { onColumnClick(group) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        group.icon,
                        contentDescription = group.label,
                        modifier = Modifier.size(if (isHighlighted) 22.dp else 18.dp),
                        tint = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        group.label.replace("Your ", ""),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = if (isHighlighted) 10.sp else 9.sp,
                        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── App Row ─────────────────────────────────────────────────────────────────

@Composable
private fun AppPermissionRow(
    app: AppPermissionInfo,
    columns: List<PermissionGroup>,
    scrollState: androidx.compose.foundation.ScrollState,
    highlightedGroup: PermissionGroup?,
    onClick: () -> Unit
) {
    val dimAlpha = if (app.isSystemApp) 0.5f else 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .width(140.dp)
                .padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (app.icon != null) {
                Image(
                    painter = rememberDrawablePainter(drawable = app.icon),
                    contentDescription = app.appName,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .then(if (app.isSystemApp) Modifier.alpha(dimAlpha) else Modifier)
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    app.appName,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = dimAlpha)
                )
                if (app.isSystemApp) {
                    Text(
                        "System",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(end = 16.dp)
        ) {
            columns.forEach { group ->
                val isHighlighted = group == highlightedGroup
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .then(
                            if (isHighlighted)
                                Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    PermissionDot(state = getPermissionCellState(app, group))
                }
            }
        }
    }
}

// ── Dot & State ─────────────────────────────────────────────────────────────

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

    val hasSuspicious = permsInGroup.any {
        it.isGranted && (it.riskLevel == RiskLevel.CRITICAL || it.riskLevel == RiskLevel.HIGH)
    }
    return if (hasSuspicious) CellState.SUSPICIOUS else CellState.GRANTED
}
