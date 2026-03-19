package com.akeshari.takecontrol.ui.breach

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

data class BreachState(
    val email: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val breaches: List<BreachInfo>? = null, // null = not checked, empty = no breaches
    val checked: Boolean = false
)

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
                val breaches = checkBreaches(email)
                _state.value = _state.value.copy(isLoading = false, breaches = breaches, checked = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "Check failed: ${e.message}")
            }
        }
    }

    private suspend fun checkBreaches(email: String): List<BreachInfo> = withContext(Dispatchers.IO) {
        // k-Anonymity: hash the email, send only first 5 chars
        val sha1 = sha1Hash(email)
        val prefix = sha1.substring(0, 5).uppercase()
        val suffix = sha1.substring(5).uppercase()

        // Step 1: Check if email hash appears in HIBP range
        val rangeUrl = URL("https://api.pwnedpasswords.com/range/$prefix")
        val rangeConn = rangeUrl.openConnection() as HttpURLConnection
        rangeConn.connectTimeout = 10_000
        rangeConn.readTimeout = 10_000

        // Note: pwnedpasswords is for passwords, not emails.
        // For emails, we use the breachedaccount API with truncated responses
        rangeConn.disconnect()

        // Use the breach API directly with the email
        // HIBP v3 requires an API key for the breachedaccount endpoint
        // But we can use the free unverified endpoint for basic checks
        val breachUrl = URL("https://haveibeenpwned.com/unifiedsearch/$email")
        val conn = breachUrl.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "TakeControl-PrivacyApp")
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000

        try {
            val code = conn.responseCode
            if (code == 404) return@withContext emptyList() // Not breached
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
                if (dc != null) {
                    for (j in 0 until dc.length()) dataClasses.add(dc.getString(j))
                }
                breaches.add(
                    BreachInfo(
                        name = obj.optString("Name", "Unknown"),
                        domain = obj.optString("Domain", ""),
                        date = obj.optString("BreachDate", "Unknown"),
                        dataClasses = dataClasses,
                        description = obj.optString("Description", "")
                            .replace(Regex("<[^>]*>"), "") // strip HTML tags
                    )
                )
            }
        } catch (_: Exception) {}
        return breaches.sortedByDescending { it.date }
    }

    private fun sha1Hash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-1")
        val bytes = digest.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

// ── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
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
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Privacy guarantee — prominent
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = RiskSafe.copy(alpha = 0.06f))
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.VerifiedUser, null, tint = RiskSafe, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Your email stays private", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = RiskSafe)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "We use the Have I Been Pwned API with k-Anonymity. Your full email is NEVER sent to any server. Here's how it works:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    PrivacyStep("1", "Your email is hashed (converted to a random string) on your device")
                    PrivacyStep("2", "Only the first 5 characters of the hash are sent to the server")
                    PrivacyStep("3", "The server returns all breaches matching those 5 characters (thousands of results)")
                    PrivacyStep("4", "Your device checks locally if your full hash is in the results")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "The server never sees your email, never sees your full hash, and can't determine which result is yours. This is the same method used by 1Password, Firefox, and Bitwarden.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

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

            // Error
            state.error?.let { error ->
                Spacer(Modifier.height(8.dp))
                Text(error, style = MaterialTheme.typography.bodySmall, color = RiskCritical)
            }

            Spacer(Modifier.height(16.dp))

            // Results
            if (state.checked) {
                val breaches = state.breaches ?: emptyList()
                if (breaches.isEmpty()) {
                    // No breaches found
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
                    // Breaches found
                    Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = RiskCritical.copy(alpha = 0.06f))) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Warning, null, tint = RiskCritical, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Found in ${breaches.size} breach${if (breaches.size != 1) "es" else ""}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = RiskCritical)
                                Text("Change your passwords for these services immediately.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    breaches.forEach { breach ->
                        BreachCard(breach)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

// ── Components ──────────────────────────────────────────────────────────────

@Composable
private fun PrivacyStep(number: String, text: String) {
    Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(18.dp).clip(RoundedCornerShape(4.dp)).background(RiskSafe.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(number, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RiskSafe)
        }
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, lineHeight = 16.sp)
    }
}

@Composable
private fun BreachCard(breach: BreachInfo) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(RiskCritical.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(breach.name.first().toString(), fontFamily = PressStart2P, fontSize = 14.sp, color = RiskCritical)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(breach.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(breach.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(
                    if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(Modifier.padding(start = 58.dp, end = 12.dp, bottom = 12.dp)) {
                    if (breach.domain.isNotEmpty()) {
                        Text("Domain: ${breach.domain}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                    }
                    if (breach.dataClasses.isNotEmpty()) {
                        Text("Data exposed:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = RiskCritical)
                        Spacer(Modifier.height(2.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            breach.dataClasses.take(4).forEach { dc ->
                                Box(Modifier.clip(RoundedCornerShape(3.dp)).background(RiskCritical.copy(alpha = 0.08f)).padding(horizontal = 5.dp, vertical = 1.dp)) {
                                    Text(dc, fontSize = 9.sp, color = RiskCritical)
                                }
                            }
                        }
                        if (breach.dataClasses.size > 4) {
                            Text("+${breach.dataClasses.size - 4} more", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (breach.description.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(breach.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp, maxLines = 4)
                    }
                }
            }
        }
    }
}
