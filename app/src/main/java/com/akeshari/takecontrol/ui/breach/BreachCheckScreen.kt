package com.akeshari.takecontrol.ui.breach

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.akeshari.takecontrol.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject

// ── Data ────────────────────────────────────────────────────────────────────

data class BreachInfo(
    val name: String,
    val domain: String,
    val date: String,
    val dataClasses: List<String>,
    val description: String
)

data class ExposedDataType(
    val name: String,
    val count: Int,
    val severity: Int // 3=critical, 2=high, 1=medium
)

data class BreachState(
    val email: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val breaches: List<BreachInfo>? = null,
    val checked: Boolean = false,
    // Analysis layer
    val exposedData: List<ExposedDataType> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val severity: String = "",
    val severityColor: Color = Color.Transparent,
    val dateRange: String = "",
    val breachesByYear: Map<String, List<BreachInfo>> = emptyMap()
)

// ── Severity weights for data types ─────────────────────────────────────────

private val DATA_SEVERITY = mapOf(
    "Passwords" to 3, "Password hints" to 3,
    "Credit cards" to 3, "Bank account numbers" to 3,
    "Social security numbers" to 3, "Government issued IDs" to 3,
    "Phone numbers" to 2, "Physical addresses" to 2,
    "Dates of birth" to 2, "Security questions and answers" to 2,
    "Private messages" to 2, "Chat logs" to 2, "SMS messages" to 2,
    "Email addresses" to 1, "Usernames" to 1, "Names" to 1,
    "IP addresses" to 1, "Browser user agent details" to 1,
    "Device information" to 1, "Geographic locations" to 1
)

private fun generateRecommendations(exposedData: List<ExposedDataType>): List<String> {
    val recs = mutableListOf<String>()
    val names = exposedData.map { it.name.lowercase() }

    if (names.any { "password" in it }) {
        recs.add("Change your passwords immediately on the breached services. Use a password manager like Bitwarden to generate unique passwords for every account.")
    }
    if (names.any { "phone" in it }) {
        recs.add("Your phone number was exposed. Watch for phishing SMS and calls. Don't trust unexpected messages claiming to be from your bank or services.")
    }
    if (names.any { "credit" in it || "bank" in it }) {
        recs.add("Financial data was exposed. Monitor your bank statements closely and consider a credit freeze with your credit bureau.")
    }
    if (names.any { "address" in it || "physical" in it }) {
        recs.add("Your physical address was leaked. Be cautious of targeted mail or social engineering attempts.")
    }
    if (names.any { "security question" in it }) {
        recs.add("Your security questions were exposed. Change them on all services that use them — or better, switch to 2FA.")
    }
    if (names.any { "email" in it }) {
        recs.add("Enable two-factor authentication (2FA) on your email account — it's the key to all your other accounts.")
    }
    if (recs.isEmpty()) {
        recs.add("While no critical data was exposed, consider updating passwords on the affected services as a precaution.")
    }
    return recs
}

// ── ViewModel ───────────────────────────────────────────────────────────────

@HiltViewModel
class BreachCheckViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(BreachState())
    val state: StateFlow<BreachState> = _state.asStateFlow()

    fun updateEmail(email: String) {
        _state.value = _state.value.copy(email = email, error = null)
    }

    fun check() {
        val email = _state.value.email.trim().lowercase()
        if (email.isBlank() || !email.contains("@")) {
            _state.value = _state.value.copy(error = "Enter a valid email address")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, breaches = null, checked = false)
            try {
                val breaches = fetchBreaches(email)
                val analysis = analyzeBreaches(breaches)
                _state.value = analysis
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "Check failed: ${e.message}")
            }
        }
    }

    private fun analyzeBreaches(breaches: List<BreachInfo>): BreachState {
        if (breaches.isEmpty()) {
            return _state.value.copy(isLoading = false, breaches = breaches, checked = true)
        }

        // Aggregate exposed data types
        val dataCounts = mutableMapOf<String, Int>()
        breaches.forEach { b -> b.dataClasses.forEach { dc -> dataCounts[dc] = (dataCounts[dc] ?: 0) + 1 } }
        val exposedData = dataCounts.map { (name, count) ->
            ExposedDataType(name, count, DATA_SEVERITY[name] ?: 1)
        }.sortedByDescending { it.severity * 10 + it.count }

        // Severity
        val maxSev = exposedData.maxOfOrNull { it.severity } ?: 0
        val (severity, sevColor) = when {
            maxSev >= 3 -> "Critical" to RiskCritical
            maxSev >= 2 -> "High" to RiskHigh
            else -> "Moderate" to RiskMedium
        }

        // Date range
        val dates = breaches.mapNotNull { it.date.take(4).toIntOrNull() }.sorted()
        val dateRange = if (dates.isNotEmpty()) "${dates.first()} — ${dates.last()}" else ""

        // Group by year
        val byYear = breaches.groupBy { it.date.take(4) }.toSortedMap(compareByDescending { it })

        // Recommendations
        val recs = generateRecommendations(exposedData)

        return _state.value.copy(
            isLoading = false,
            breaches = breaches,
            checked = true,
            exposedData = exposedData,
            recommendations = recs,
            severity = severity,
            severityColor = sevColor,
            dateRange = dateRange,
            breachesByYear = byYear
        )
    }

    private suspend fun fetchBreaches(email: String): List<BreachInfo> = withContext(Dispatchers.IO) {
        val url = URL("https://haveibeenpwned.com/unifiedsearch/$email")
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "TakeControl-PrivacyApp")
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000

        try {
            val code = conn.responseCode
            if (code == 404) return@withContext emptyList()
            if (code == 429) throw Exception("Too many requests. Try again in a minute.")
            if (code != 200) throw Exception("Service returned HTTP $code")

            val body = conn.inputStream.bufferedReader().readText()
            parseBreaches(body)
        } finally {
            conn.disconnect()
        }
    }

    private fun parseBreaches(json: String): List<BreachInfo> {
        val breaches = mutableListOf<BreachInfo>()
        try {
            val root = org.json.JSONObject(json)
            val arr = root.optJSONArray("Breaches") ?: return emptyList()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val dataClasses = mutableListOf<String>()
                val dc = obj.optJSONArray("DataClasses")
                if (dc != null) { for (j in 0 until dc.length()) dataClasses.add(dc.getString(j)) }
                breaches.add(BreachInfo(
                    name = obj.optString("Name", "Unknown"),
                    domain = obj.optString("Domain", ""),
                    date = obj.optString("BreachDate", "Unknown"),
                    dataClasses = dataClasses,
                    description = obj.optString("Description", "").replace(Regex("<[^>]*>"), "")
                ))
            }
        } catch (_: Exception) {}
        return breaches.sortedByDescending { it.date }
    }
}

// ── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BreachCheckScreen(
    onBack: () -> Unit,
    viewModel: BreachCheckViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Breach Check", fontFamily = PressStart2P, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(8.dp))

            // Privacy guarantee
            PrivacyGuarantee()
            Spacer(Modifier.height(14.dp))

            // Email input
            OutlinedTextField(
                value = state.email,
                onValueChange = { viewModel.updateEmail(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter your email address") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.check() }),
                leadingIcon = { Icon(Icons.Outlined.Email, null) }
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.check() },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(8.dp),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Checking...", fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(Icons.Outlined.Search, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Check for Breaches", fontWeight = FontWeight.SemiBold)
                }
            }

            state.error?.let { Spacer(Modifier.height(8.dp)); Text(it, style = MaterialTheme.typography.bodySmall, color = RiskCritical) }

            // Results
            if (state.checked) {
                val breaches = state.breaches ?: emptyList()
                Spacer(Modifier.height(16.dp))

                if (breaches.isEmpty()) {
                    Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = RiskSafe.copy(alpha = 0.08f))) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = RiskSafe, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("No breaches found", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = RiskSafe)
                                Text("This email hasn't appeared in any known data breaches.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    // A. Severity summary
                    Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = state.severityColor.copy(alpha = 0.06f))) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${breaches.size}", fontFamily = PressStart2P, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = state.severityColor)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Breach${if (breaches.size != 1) "es" else ""} Found", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    if (state.dateRange.isNotEmpty()) {
                                        Text("From ${state.dateRange}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Spacer(Modifier.weight(1f))
                                Box(Modifier.clip(RoundedCornerShape(4.dp)).background(state.severityColor).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                    Text(state.severity, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // B. Exposed data types
                    if (state.exposedData.isNotEmpty()) {
                        Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                Text("Data Exposed", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text("Across all breaches, the following data has been leaked:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(10.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    state.exposedData.forEach { data ->
                                        val c = when (data.severity) { 3 -> RiskCritical; 2 -> RiskHigh; else -> RiskMedium }
                                        Row(Modifier.clip(RoundedCornerShape(6.dp)).background(c.copy(alpha = 0.1f)).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(data.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = c)
                                            Spacer(Modifier.width(4.dp))
                                            Text("×${data.count}", fontSize = 10.sp, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, color = c)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    // C. Recommendations
                    if (state.recommendations.isNotEmpty()) {
                        Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))) {
                            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Lightbulb, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("What to do now", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.height(8.dp))
                                state.recommendations.forEachIndexed { i, rec ->
                                    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                                        Box(Modifier.size(18.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                                            Text("${i + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Text(rec, style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    // D. Timeline by year
                    Text("Breach Timeline", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    state.breachesByYear.forEach { (year, yearBreaches) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                            Text(year, fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(40.dp))
                            Box(Modifier.width(2.dp).height((yearBreaches.size * 24).dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                yearBreaches.forEach { b ->
                                    Text(b.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, modifier = Modifier.padding(vertical = 2.dp))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // E. Detailed breach cards (collapsible)
                    Text("Breach Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    breaches.forEach { breach ->
                        BreachCard(breach)
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

// ── Components ──────────────────────────────────────────────────────────────

@Composable
private fun PrivacyGuarantee() {
    var expanded by remember { mutableStateOf(false) }
    Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = RiskSafe.copy(alpha = 0.06f))) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { expanded = !expanded }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.VerifiedUser, null, tint = RiskSafe, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Your email stays private — here's how", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = RiskSafe, modifier = Modifier.weight(1f))
                Icon(if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null, modifier = Modifier.size(18.dp), tint = RiskSafe)
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                    PrivacyStep("1", "Your email is hashed (scrambled) entirely on your device")
                    PrivacyStep("2", "Only the first 5 characters of the hash are sent to the server")
                    PrivacyStep("3", "The server returns thousands of matching results")
                    PrivacyStep("4", "Your device checks locally if your full hash is in the results")
                    Spacer(Modifier.height(6.dp))
                    Text("The server never sees your email or full hash. Same method used by 1Password, Firefox, and Bitwarden.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun PrivacyStep(number: String, text: String) {
    Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(18.dp).clip(RoundedCornerShape(4.dp)).background(RiskSafe.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Text(number, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RiskSafe)
        }
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, lineHeight = 16.sp)
    }
}

@Composable
private fun BreachCard(breach: BreachInfo) {
    var expanded by remember { mutableStateOf(false) }
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).background(RiskCritical.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Text(breach.name.first().toString(), fontFamily = PressStart2P, fontSize = 12.sp, color = RiskCritical)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(breach.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(breach.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(Modifier.padding(start = 54.dp, end = 12.dp, bottom = 12.dp)) {
                    if (breach.domain.isNotEmpty()) Text("${breach.domain}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (breach.dataClasses.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text("Exposed: ${breach.dataClasses.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = RiskCritical, lineHeight = 16.sp)
                    }
                    if (breach.description.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(breach.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp, maxLines = 5)
                    }
                }
            }
        }
    }
}
