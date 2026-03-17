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
import androidx.compose.ui.graphics.Brush
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

    LaunchedEffect(highlightGroup) {
        viewModel.setInitialGroup(highlightGroup)
    }

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
                title = {
                    Text(
                        "MATRIX",
                        style = MaterialTheme.typography.headlineSmall,
                        letterSpacing = 2.sp
                    )
                },
                actions = {
                    IconButton(onClick = { showLegend = !showLegend }) {
                        Icon(Icons.Outlined.Info, "Legend", tint = Primary)
                    }
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(Icons.Outlined.FilterList, "Filters", tint = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedVisibility(visible = showLegend) {
                LegendCard()
            }

            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.search(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = {
                    Text(
                        "Search apps...",
                        color = OnSurfaceVar
                    )
                },
                leadingIcon = { Icon(Icons.Outlined.Search, "Search", tint = Primary) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = SurfaceHigh
                )
            )

            AnimatedVisibility(visible = showFilters) {
                FilterRow(
                    selectedFilter = state.filter,
                    onFilterSelected = { viewModel.setFilter(it) }
                )
            }

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

            HorizontalDivider(thickness = 1.dp, color = SurfaceHigh)

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.filteredApps, key = { it.packageName }) { app ->
                    AppPermissionRow(
                        app = app,
                        columns = columns,
                        scrollState = scrollState,
                        highlightedGroup = state.highlightedGroup,
                        onClick = { onAppClick(app.packageName) }
                    )
                    // Risk-colored thin divider
                    val divColor = when {
                        app.riskScore >= 75 -> Accent.copy(alpha = 0.3f)
                        app.riskScore >= 50 -> RiskHigh.copy(alpha = 0.2f)
                        else -> SurfaceHigh.copy(alpha = 0.4f)
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = divColor)
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

    val riskColor = when (group.defaultRisk) {
        RiskLevel.CRITICAL -> Accent
        RiskLevel.HIGH -> RiskHigh
        RiskLevel.MEDIUM -> Warning
        RiskLevel.LOW -> Safe
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Bold header with accent stripe
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceVariant)
            ) {
                // Accent stripe
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .fillMaxHeight()
                        .background(riskColor)
                        .align(Alignment.CenterStart)
                )
                Row(
                    modifier = Modifier
                        .padding(start = 5.dp)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        group.icon,
                        contentDescription = null,
                        tint = riskColor,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            group.label.uppercase(),
                            style = MaterialTheme.typography.headlineSmall,
                            letterSpacing = 1.sp
                        )
                        Text(
                            group.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVar
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Risk badge
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
                    "RISK: ${group.defaultRisk.name}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = riskColor,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${appsWithPermission.size} granted",
                    fontFamily = JetBrainsMono,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceVar
                )
            }

            Spacer(Modifier.height(16.dp))

            // Tutorial with numbered squares
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "HOW TO DISABLE",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    TutorialStep(step = "1", text = "Tap the settings icon next to an app below")
                    TutorialStep(step = "2", text = "Go to \"Permissions\" in the app info page")
                    TutorialStep(step = "3", text = "Find \"${group.label.replace("Your ", "")}\" and set to \"Don't allow\"")
                    TutorialStep(step = "4", text = "Come back and refresh to verify")
                }
            }

            Spacer(Modifier.height(16.dp))

            if (appsWithPermission.isNotEmpty()) {
                Text(
                    "APPS WITH ACCESS (${appsWithPermission.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
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

            if (appsDenied.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "DENIED (${appsDenied.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceVar,
                    letterSpacing = 1.sp
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
        // Numbered square instead of circle
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                step,
                fontFamily = JetBrainsMono,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = OnSurface
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
    val riskColor = when {
        app.riskScore >= 75 -> Accent
        app.riskScore >= 50 -> RiskHigh
        app.riskScore >= 25 -> Warning
        else -> Safe
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                Accent.copy(alpha = 0.06f)
            else
                Safe.copy(alpha = 0.06f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onAppClick)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                        .background(SurfaceHigh),
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
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (isGranted) "Permission granted" else "Permission denied",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isGranted) Accent else Safe
                )
            }

            // Geometric risk indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(riskColor)
            )
            Spacer(Modifier.width(8.dp))

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Outlined.OpenInNew,
                    contentDescription = "Open app settings",
                    modifier = Modifier.size(18.dp),
                    tint = Primary
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
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "LEGEND",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(12.dp))
            LegendItem(color = OnSurfaceVar.copy(alpha = 0.3f), filled = false, label = "Not requested", description = "App doesn't use this")
            Spacer(Modifier.height(6.dp))
            LegendItem(color = Safe, filled = false, label = "Denied", description = "Requested but denied")
            Spacer(Modifier.height(6.dp))
            LegendItem(color = Warning, filled = true, label = "Granted", description = "Permission is active")
            Spacer(Modifier.height(6.dp))
            LegendItem(color = Accent, filled = true, label = "Suspicious", description = "Granted & high risk")
            Spacer(Modifier.height(10.dp))
            Text(
                "Tap any column header for details",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVar
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, filled: Boolean, label: String, description: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Square indicator matching the matrix
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(3.dp))
                .then(
                    if (filled) Modifier.background(color)
                    else Modifier
                        .background(color.copy(alpha = 0.15f))
                        .border(1.5.dp, color.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                )
        )
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(80.dp)
        )
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVar
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
                label = {
                    Text(
                        filter.label.uppercase(),
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp,
                        fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Primary.copy(alpha = 0.15f),
                    selectedLabelColor = Primary
                )
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
            .background(Surface)
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(140.dp)
                .padding(start = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                "APP",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceVar,
                letterSpacing = 2.sp
            )
        }

        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(end = 16.dp)
        ) {
            columns.forEach { group ->
                val isHighlighted = group == highlightedGroup
                val riskColor = when (group.defaultRisk) {
                    RiskLevel.CRITICAL -> Accent
                    RiskLevel.HIGH -> RiskHigh
                    RiskLevel.MEDIUM -> Warning
                    RiskLevel.LOW -> Safe
                }
                Column(
                    modifier = Modifier
                        .width(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .then(
                            if (isHighlighted)
                                Modifier.background(Primary.copy(alpha = 0.12f))
                            else Modifier
                        )
                        .clickable { onColumnClick(group) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        group.icon,
                        contentDescription = group.label,
                        modifier = Modifier.size(if (isHighlighted) 22.dp else 20.dp),
                        tint = if (isHighlighted) Primary else riskColor
                    )
                    Text(
                        group.label.replace("Your ", ""),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = if (isHighlighted) 10.sp else 9.sp,
                        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isHighlighted) Primary else OnSurfaceVar
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
    val dimAlpha = if (app.isSystemApp) 0.45f else 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
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
                        .background(SurfaceHigh),
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
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = OnSurface.copy(alpha = dimAlpha)
                )
                if (app.isSystemApp) {
                    Text(
                        "SYSTEM",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        color = OnSurfaceVar.copy(alpha = 0.5f),
                        letterSpacing = 1.sp
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
                                Modifier.background(Primary.copy(alpha = 0.04f))
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    PermissionSquare(state = getPermissionCellState(app, group))
                }
            }
        }
    }
}

// ── Square Indicators & State ───────────────────────────────────────────────

@Composable
private fun PermissionSquare(state: CellState) {
    val color = when (state) {
        CellState.NOT_REQUESTED -> OnSurfaceVar.copy(alpha = 0.2f)
        CellState.DENIED -> Safe
        CellState.GRANTED -> Warning
        CellState.SUSPICIOUS -> Accent
    }

    when (state) {
        CellState.NOT_REQUESTED -> {
            // Empty outlined square
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .border(1.dp, color, RoundedCornerShape(3.dp))
            )
        }
        CellState.DENIED -> {
            // Outlined square with subtle fill
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color.copy(alpha = 0.15f))
                    .border(1.5.dp, color.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
            )
        }
        CellState.GRANTED -> {
            // Half-filled square
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color.copy(alpha = 0.35f))
                    .border(1.5.dp, color, RoundedCornerShape(3.dp))
            )
        }
        CellState.SUSPICIOUS -> {
            // Fully filled square with bold border
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
                    .border(2.dp, color.copy(alpha = 0.7f), RoundedCornerShape(3.dp))
            )
        }
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
