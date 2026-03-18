package com.akeshari.takecontrol.ui.communitydb

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
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
    val apps: List<PrivacyDbReport> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class CommunityDbViewModel @Inject constructor(
    private val client: PrivacyDbClient
) : ViewModel() {
    private val _state = MutableStateFlow(CommunityDbState())
    val state: StateFlow<CommunityDbState> = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            _state.value = CommunityDbState(isLoading = true)
            try {
                val packages = fetchIndex()
                val reports = packages.mapNotNull { client.fetch(it) }
                    .sortedBy { it.appName.lowercase() }
                _state.value = CommunityDbState(isLoading = false, apps = reports)
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
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            item {
                Text(
                    "Pre-scanned app permissions from the open-source Privacy DB. Tap any app to see full details.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Contribute
            item {
                Card(
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/hell-abhi/privacy-db"))) },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
                ) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Code, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Contribute", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            Text("Submit a Play Store URL to add an app", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Outlined.OpenInNew, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Loading
            if (state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            Text("Loading ${""} apps...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Error
            state.error?.let { error ->
                item {
                    Text(error, style = MaterialTheme.typography.bodySmall, color = RiskCritical)
                }
            }

            // App count
            if (!state.isLoading && state.apps.isNotEmpty()) {
                item {
                    Text("${state.apps.size} apps scanned", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                }
            }

            // App rows
            items(state.apps, key = { it.packageName }) { app ->
                AppRow(app, onLookup)
            }
        }
    }
}

@Composable
private fun AppRow(app: PrivacyDbReport, onLookup: (String) -> Unit) {
    val totalPerms = app.permissionGroups.sumOf { it.permissions.size }
    val riskColor = when {
        totalPerms > 20 -> RiskCritical
        totalPerms > 10 -> RiskHigh
        totalPerms > 5 -> RiskMedium
        else -> RiskLow
    }

    Card(
        onClick = { onLookup(app.packageName) },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            // Permission count badge
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(riskColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("$totalPerms", fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = riskColor)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(app.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    app.category?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    app.rating?.let { Text("$it\u2605", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    app.downloads?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
    }
}
