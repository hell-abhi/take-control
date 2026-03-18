package com.akeshari.takecontrol.ui.communitydb

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.akeshari.takecontrol.data.model.PermissionGroup
import com.akeshari.takecontrol.data.model.RiskLevel
import com.akeshari.takecontrol.data.scanner.PlayStorePermissionGroup
import com.akeshari.takecontrol.data.scanner.PrivacyDbClient
import com.akeshari.takecontrol.data.scanner.PrivacyDbReport
import com.akeshari.takecontrol.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

// ── ViewModel ───────────────────────────────────────────────────────────────

data class CommunityDbState(
    val isLoading: Boolean = true,
    val allApps: List<PrivacyDbReport> = emptyList(),
    val filteredApps: List<PrivacyDbReport> = emptyList(),
    val searchQuery: String = "",
    val error: String? = null
)

@HiltViewModel
class CommunityDbViewModel @Inject constructor(
    private val client: PrivacyDbClient
) : ViewModel() {
    private val _state = MutableStateFlow(CommunityDbState())
    val state: StateFlow<CommunityDbState> = _state.asStateFlow()

    init { load() }

    fun search(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        applyFilter()
    }

    private fun applyFilter() {
        val current = _state.value
        val filtered = if (current.searchQuery.isBlank()) {
            current.allApps
        } else {
            current.allApps.filter {
                it.appName.contains(current.searchQuery, ignoreCase = true) ||
                        it.packageName.contains(current.searchQuery, ignoreCase = true)
            }
        }
        _state.value = current.copy(filteredApps = filtered)
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = CommunityDbState(isLoading = true)
            try {
                val packages = fetchIndex()
                val reports = packages.mapNotNull { client.fetch(it) }
                    .sortedBy { it.appName.lowercase() }
                _state.value = CommunityDbState(isLoading = false, allApps = reports, filteredApps = reports)
            } catch (e: Exception) {
                _state.value = CommunityDbState(isLoading = false, error = "Failed to load: ${e.message}")
            }
        }
    }

    private suspend fun fetchIndex(): List<String> = withContext(Dispatchers.IO) {
        val url = URL("https://raw.githubusercontent.com/hell-abhi/privacy-db/main/index.json")
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("Cache-Control", "no-cache")
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        try {
            val body = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(body)
            val arr = json.getJSONArray("packages")
            (0 until arr.length()).map { arr.getString(it) }
        } finally {
            conn.disconnect()
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private val MATRIX_COLUMNS = listOf(
    PermissionGroup.LOCATION,
    PermissionGroup.CAMERA,
    PermissionGroup.MICROPHONE,
    PermissionGroup.CONTACTS,
    PermissionGroup.STORAGE,
    PermissionGroup.SMS,
    PermissionGroup.PHONE,
    PermissionGroup.SENSORS
)

private fun mapToGroup(groupName: String): PermissionGroup? = when (groupName.lowercase()) {
    "location" -> PermissionGroup.LOCATION
    "camera" -> PermissionGroup.CAMERA
    "microphone" -> PermissionGroup.MICROPHONE
    "contacts", "identity" -> PermissionGroup.CONTACTS
    "storage", "photos/media/files", "photos and videos", "music and audio" -> PermissionGroup.STORAGE
    "sms" -> PermissionGroup.SMS
    "phone", "device id & call information" -> PermissionGroup.PHONE
    "sensors" -> PermissionGroup.SENSORS
    else -> null
}

private fun appHasGroup(app: PrivacyDbReport, group: PermissionGroup): Boolean {
    return app.permissionGroups.any { mapToGroup(it.groupName) == group && it.permissions.isNotEmpty() }
}

private fun groupColor(group: PermissionGroup): Color = when (group.defaultRisk) {
    RiskLevel.CRITICAL -> RiskCritical
    RiskLevel.HIGH -> RiskHigh
    RiskLevel.MEDIUM -> RiskMedium
    RiskLevel.LOW -> RiskLow
}

// ── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityDbScreen(
    onBack: () -> Unit,
    onLookup: (String) -> Unit,
    viewModel: CommunityDbViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Community DB", fontFamily = PressStart2P, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/hell-abhi/privacy-db")))
                    }) {
                        Icon(Icons.Outlined.Code, "Contribute")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Description
            Text(
                "Open-source privacy database. Tap any app for full details. Tap { } to contribute.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Search
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.search(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                placeholder = { Text("Search apps...") },
                leadingIcon = { Icon(Icons.Outlined.Search, "Search") },
                singleLine = true,
                shape = RoundedCornerShape(6.dp)
            )

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text("Loading privacy database...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else if (state.error != null) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(state.error!!, style = MaterialTheme.typography.bodyMedium, color = RiskCritical)
                }
            } else {
                // Count
                Text(
                    "${state.filteredApps.size} apps",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // Column headers
                val scrollState = rememberScrollState()
                ColumnHeaders(scrollState)
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                // App rows
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.filteredApps, key = { it.packageName }) { app ->
                        AppMatrixRow(app, scrollState, onLookup)
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

// ── Column Headers ──────────────────────────────────────────────────────────

@Composable
private fun ColumnHeaders(scrollState: androidx.compose.foundation.ScrollState) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        // App name column
        Spacer(Modifier.width(16.dp))
        Box(Modifier.width(120.dp))

        // Permission columns
        Row(Modifier.horizontalScroll(scrollState)) {
            MATRIX_COLUMNS.forEach { group ->
                Box(Modifier.width(40.dp), contentAlignment = Alignment.Center) {
                    Icon(group.icon, group.label, tint = groupColor(group), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ── App Matrix Row ──────────────────────────────────────────────────────────

@Composable
private fun AppMatrixRow(
    app: PrivacyDbReport,
    scrollState: androidx.compose.foundation.ScrollState,
    onLookup: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLookup(app.packageName) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App info
        Spacer(Modifier.width(16.dp))
        Column(
            modifier = Modifier.width(120.dp)
        ) {
            Text(
                app.appName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                app.category?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                }
            }
        }

        // Permission dots
        Row(Modifier.horizontalScroll(scrollState)) {
            MATRIX_COLUMNS.forEach { group ->
                Box(Modifier.width(40.dp), contentAlignment = Alignment.Center) {
                    if (appHasGroup(app, group)) {
                        Box(
                            Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(groupColor(group))
                        )
                    }
                }
            }
        }
    }
}

