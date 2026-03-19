package com.akeshari.takecontrol.ui.preinstall

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akeshari.takecontrol.data.model.RiskLevel
import com.akeshari.takecontrol.data.scanner.PlayStorePermissionGroup
import com.akeshari.takecontrol.ui.common.ExplainerCard
import com.akeshari.takecontrol.ui.common.ExplainerSection
import com.akeshari.takecontrol.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreInstallCheckScreen(
    onBack: () -> Unit = {},
    initialQuery: String? = null,
    viewModel: PreInstallCheckViewModel = hiltViewModel()
) {
    // Auto-fill and analyze if navigated with a query
    LaunchedEffect(initialQuery) {
        if (initialQuery != null && initialQuery != "{query}") {
            viewModel.updateQuery(initialQuery)
            viewModel.analyze()
        }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Lookup", fontFamily = PressStart2P, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search input
            item { SearchSection(state, viewModel) }

            // Loading
            if (state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(12.dp))
                            Text("Analyzing from Play Store...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Error
            state.error?.let { error ->
                item { ErrorCard(error) }
            }

            // Results
            if (state.appName != null && !state.isLoading) {
                // App header
                item {
                    AppHeaderCard(
                        appName = state.appName!!,
                        packageName = state.packageName ?: "",
                        source = state.source,
                        category = when (state.source) {
                            AnalysisSource.LOCAL -> state.localApp?.category?.label
                            AnalysisSource.PLAY_STORE -> state.playStoreReport?.category
                            else -> null
                        },
                        rating = state.playStoreReport?.rating,
                        downloads = state.playStoreReport?.downloads
                    )
                }

                // Verdict
                state.verdict?.let { verdict ->
                    item {
                        VerdictCard(verdict = verdict, level = state.verdictLevel ?: RiskLevel.LOW)
                    }
                }

                // Narratives
                if (state.narratives.isNotEmpty()) {
                    item { NarrativeCard(state.narratives) }
                }

                // Local app: trackers
                state.localApp?.let { app ->
                    if (app.trackers.isNotEmpty()) {
                        item {
                            TrackerSummaryCard(
                                count = app.trackers.size,
                                names = app.trackers.map { "${it.name} (${it.category.label})" }
                            )
                        }
                    }

                    // Local app: permission risk breakdown
                    item { RiskSummaryChips(app.permissions.filter { it.isGranted }.map { it.riskLevel }) }

                    // Local app: action buttons
                    item {
                        ActionRow(packageName = app.packageName, isInstalled = true)
                    }
                }

                // Play Store: data safety
                state.playStoreReport?.let { report ->
                    item { DataSafetyCard(report.dataSafety) }

                    // Permission groups
                    if (report.permissionGroups.isNotEmpty()) {
                        item {
                            Text(
                                "Permissions Requested",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        item {
                            RiskSummaryChips(state.permissionRisks.map { it.riskLevel })
                        }
                        items(report.permissionGroups.filter { it.permissions.isNotEmpty() }) { group ->
                            PermissionGroupCard(group, state.permissionRisks)
                        }
                    }

                    // Not installed — suggest caution or install
                    item {
                        ActionRow(packageName = report.packageName, isInstalled = false)
                    }
                }

                // Source attribution + contribute
                item {
                    val sourceText = when (state.source) {
                        AnalysisSource.LOCAL -> "Analyzed from your installed app"
                        AnalysisSource.PLAY_STORE -> "Data from Google Play Store listing"
                        else -> ""
                    }
                    Text(sourceText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SearchSection(state: PreInstallState, viewModel: PreInstallCheckViewModel) {
    Column {
        ExplainerCard(
            title = "How App Lookup works",
            sections = listOf(
                ExplainerSection(
                    "Check before you install.",
                    "Enter a package name or paste a Google Play Store link to see what an app requests before you install it."
                ),
                ExplainerSection(
                    "For installed apps,",
                    "we run a full local scan — permissions, risk scores, and embedded tracker SDKs — all on your device."
                ),
                ExplainerSection(
                    "For apps not on your phone,",
                    "we fetch the public Play Store listing to show permissions, data safety practices (what data is shared or collected), and an overall risk verdict."
                )
            )
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.query,
            onValueChange = { viewModel.updateQuery(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("com.instagram.android") },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.analyze() }),
            trailingIcon = {
                IconButton(onClick = { viewModel.analyze() }) {
                    Icon(Icons.Outlined.Search, "Analyze")
                }
            }
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Paste a Play Store link or enter a package name",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorCard(error: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = RiskCritical.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Warning, null, tint = RiskCritical, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(error, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AppHeaderCard(
    appName: String,
    packageName: String,
    source: AnalysisSource?,
    category: String?,
    rating: String?,
    downloads: String?
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(appName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                category?.let { InfoChip(it) }
                rating?.let { InfoChip("$it \u2605") }
                downloads?.let { InfoChip("$it downloads") }
                if (source == AnalysisSource.LOCAL) InfoChip("Installed")
            }
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun VerdictCard(verdict: String, level: RiskLevel) {
    val color = when (level) {
        RiskLevel.CRITICAL -> RiskCritical
        RiskLevel.HIGH -> RiskHigh
        RiskLevel.MEDIUM -> RiskMedium
        RiskLevel.LOW -> RiskSafe
    }
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(verdict, fontFamily = PressStart2P, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun NarrativeCard(narratives: List<String>) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = RiskCritical.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Warning, null, tint = RiskCritical, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("What this app could do", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = RiskCritical)
            }
            Spacer(Modifier.height(10.dp))
            narratives.forEach { narrative ->
                Row(modifier = Modifier.padding(vertical = 3.dp)) {
                    Text("\u2022", color = RiskCritical, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp, top = 1.dp))
                    Text(narrative, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun TrackerSummaryCard(count: Int, names: List<String>) {
    val color = when { count > 5 -> RiskCritical; count > 2 -> RiskHigh; else -> RiskMedium }
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Visibility, null, tint = color, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("$count Trackers Detected", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            }
            Spacer(Modifier.height(8.dp))
            names.forEach { name ->
                Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(color))
                    Spacer(Modifier.width(10.dp))
                    Text(name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun DataSafetyCard(safety: com.akeshari.takecontrol.data.scanner.DataSafetyInfo) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.PrivacyTip, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Data Safety", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))

            safety.sharedSummary?.let {
                SafetyRow(
                    label = if ("no data" in it.lowercase()) it else "Shares data with third parties",
                    detail = if ("no data" in it.lowercase()) null else it,
                    isPositive = "no data" in it.lowercase()
                )
            }
            safety.collectedSummary?.let {
                SafetyRow(
                    label = if ("no data" in it.lowercase()) it else "Collects data",
                    detail = if ("no data" in it.lowercase()) null else it,
                    isPositive = "no data" in it.lowercase()
                )
            }
            SafetyRow(
                label = if (safety.isEncrypted) "Data encrypted in transit" else "Encryption not confirmed",
                isPositive = safety.isEncrypted
            )
            SafetyRow(
                label = if (safety.canRequestDeletion) "You can request data deletion" else "No data deletion option",
                isPositive = safety.canRequestDeletion
            )
        }
    }
}

@Composable
private fun SafetyRow(label: String, detail: String? = null, isPositive: Boolean) {
    val color = if (isPositive) RiskSafe else RiskHigh
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            if (isPositive) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
            null, tint = color, modifier = Modifier.size(16.dp).padding(top = 2.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            detail?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RiskSummaryChips(riskLevels: List<RiskLevel>) {
    val critical = riskLevels.count { it == RiskLevel.CRITICAL }
    val high = riskLevels.count { it == RiskLevel.HIGH }
    val medium = riskLevels.count { it == RiskLevel.MEDIUM }
    val low = riskLevels.count { it == RiskLevel.LOW }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (critical > 0) RiskChip("$critical Critical", RiskCritical)
        if (high > 0) RiskChip("$high High", RiskHigh)
        if (medium > 0) RiskChip("$medium Medium", RiskMedium)
        if (low > 0) RiskChip("$low Low", RiskLow)
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
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
private fun PermissionGroupCard(group: PlayStorePermissionGroup, risks: List<PermissionRisk>) {
    val groupRisks = risks.filter { it.groupName == group.groupName }
    val maxRisk = groupRisks.maxByOrNull { it.riskLevel.weight }?.riskLevel ?: RiskLevel.LOW
    val color = when (maxRisk) {
        RiskLevel.CRITICAL -> RiskCritical
        RiskLevel.HIGH -> RiskHigh
        RiskLevel.MEDIUM -> RiskMedium
        RiskLevel.LOW -> RiskLow
    }

    Card(
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(4.dp, 18.dp).clip(RoundedCornerShape(2.dp)).background(color))
                Spacer(Modifier.width(10.dp))
                Text(
                    group.groupName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
            }
            Spacer(Modifier.height(6.dp))
            group.permissions.forEach { perm ->
                val risk = groupRisks.find { it.name == perm }
                val permColor = when (risk?.riskLevel) {
                    RiskLevel.CRITICAL -> RiskCritical
                    RiskLevel.HIGH -> RiskHigh
                    RiskLevel.MEDIUM -> RiskMedium
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(permColor))
                    Spacer(Modifier.width(8.dp))
                    Text(perm, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ActionRow(packageName: String, isInstalled: Boolean) {
    val context = LocalContext.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isInstalled) {
            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Icon(Icons.Outlined.Shield, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Manage Permissions", fontSize = 12.sp)
            }
        } else {
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                    context.startActivity(intent)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Icon(Icons.Outlined.OpenInNew, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("View on Play Store", fontSize = 12.sp)
            }
        }
    }
}

