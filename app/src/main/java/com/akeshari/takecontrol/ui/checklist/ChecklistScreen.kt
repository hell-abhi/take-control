package com.akeshari.takecontrol.ui.checklist

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akeshari.takecontrol.ui.theme.*

// ── Data ────────────────────────────────────────────────────────────────────

data class ChecklistItem(
    val id: String,
    val title: String,
    val why: String,
    val howToFix: String,
    val icon: ImageVector,
    val settingsAction: String,
    val category: String
)

private val CHECKLIST = listOf(
    // Advertising & Tracking
    ChecklistItem(
        id = "ad_id",
        title = "Turn off ad personalization",
        why = "Google assigns your device an advertising ID that lets companies track you across every app. Disabling it stops cross-app ad tracking.",
        howToFix = "Tap below → Delete advertising ID or opt out of Ads Personalization.",
        icon = Icons.Outlined.PersonOff,
        settingsAction = "com.google.android.gms.settings.ADS_PRIVACY",
        category = "Tracking"
    ),
    ChecklistItem(
        id = "usage_diagnostics",
        title = "Disable usage & diagnostics sharing",
        why = "Your phone sends anonymous usage data to Google/Samsung. While \"anonymous,\" this data can still be correlated to identify you.",
        howToFix = "Tap below → scroll to Usage & Diagnostics → turn it off.",
        icon = Icons.Outlined.Analytics,
        settingsAction = Settings.ACTION_PRIVACY_SETTINGS,
        category = "Tracking"
    ),
    ChecklistItem(
        id = "personalize_app_data",
        title = "Disable \"Personalize using app data\"",
        why = "Samsung uses your app usage patterns to personalize ads and recommendations across Samsung services.",
        howToFix = "Tap below → look for Customization Service → turn it off.",
        icon = Icons.Outlined.PhoneAndroid,
        settingsAction = Settings.ACTION_PRIVACY_SETTINGS,
        category = "Tracking"
    ),

    // Location
    ChecklistItem(
        id = "wifi_scanning",
        title = "Disable Wi-Fi scanning (always available)",
        why = "Even with Wi-Fi off, your phone scans for networks in the background — revealing your location to Google and nearby trackers.",
        howToFix = "Tap below → Wi-Fi scanning → turn it off.",
        icon = Icons.Outlined.Wifi,
        settingsAction = "android.settings.LOCATION_SCANNING_SETTINGS",
        category = "Location"
    ),
    ChecklistItem(
        id = "bt_scanning",
        title = "Disable Bluetooth scanning",
        why = "Similar to Wi-Fi scanning — your phone broadcasts Bluetooth signals that can be used for indoor location tracking, even with Bluetooth off.",
        howToFix = "Tap below → Bluetooth scanning → turn it off.",
        icon = Icons.Outlined.Bluetooth,
        settingsAction = "android.settings.LOCATION_SCANNING_SETTINGS",
        category = "Location"
    ),
    ChecklistItem(
        id = "google_location_history",
        title = "Turn off Google Location History",
        why = "Google records everywhere you go and stores it in your timeline — indefinitely. This data is used for ads and has been subpoenaed by law enforcement.",
        howToFix = "Tap below → find Location History or Timeline → turn it off and delete existing data.",
        icon = Icons.Outlined.LocationOff,
        settingsAction = Settings.ACTION_LOCATION_SOURCE_SETTINGS,
        category = "Location"
    ),

    // Network
    ChecklistItem(
        id = "private_dns",
        title = "Use a private DNS provider",
        why = "By default, every website you visit is visible to your ISP through DNS queries. Private DNS encrypts these lookups.",
        howToFix = "Tap below → More connection settings → Private DNS → set to dns.adguard.com or one.one.one.one",
        icon = Icons.Outlined.Dns,
        settingsAction = Settings.ACTION_WIRELESS_SETTINGS,
        category = "Network"
    ),

    // Security
    ChecklistItem(
        id = "screen_lock",
        title = "Enable screen lock with biometrics",
        why = "A phone without a lock screen is an open book. Biometric lock (fingerprint/face) makes it fast and secure.",
        howToFix = "Tap below → set up fingerprint or face unlock if you haven't already.",
        icon = Icons.Outlined.Fingerprint,
        settingsAction = "android.settings.BIOMETRIC_ENROLL",
        category = "Security"
    ),
    ChecklistItem(
        id = "install_unknown",
        title = "Disable \"Install unknown apps\" for all sources",
        why = "If any app can sideload other apps, malware can install itself. Only enable this temporarily when you need it.",
        howToFix = "Tap below → make sure all apps are set to \"Not allowed\".",
        icon = Icons.Outlined.Block,
        settingsAction = Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
        category = "Security"
    ),
    ChecklistItem(
        id = "find_my_device",
        title = "Review Find My Device settings",
        why = "Find My Device shares your real-time location with Google. Useful if you lose your phone, but a constant location beacon. Make a conscious choice.",
        howToFix = "Tap below → review whether Find My Device should be enabled or disabled for your use case.",
        icon = Icons.Outlined.LocationSearching,
        settingsAction = Settings.ACTION_SECURITY_SETTINGS,
        category = "Security"
    ),

    // Apps
    ChecklistItem(
        id = "auto_update",
        title = "Enable automatic app updates",
        why = "App updates often fix security vulnerabilities. Running outdated apps with known exploits is a risk.",
        howToFix = "Tap below → opens Play Store → Profile icon → Settings → Auto-update apps → Over Wi-Fi only.",
        icon = Icons.Outlined.SystemUpdate,
        settingsAction = "android.intent.action.VIEW",
        category = "Apps"
    ),
    ChecklistItem(
        id = "review_permissions",
        title = "Review app permissions regularly",
        why = "Apps gain permissions through updates. A photo editor that didn't need your mic last month might request it now.",
        howToFix = "Go to the Apps tab in Take Control to see the full permission matrix.",
        icon = Icons.Outlined.Shield,
        settingsAction = Settings.ACTION_APPLICATION_SETTINGS,
        category = "Apps"
    )
)

// ── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    // Track completed items in memory (resets on screen close — could persist in SharedPrefs later)
    val completed = remember { mutableStateMapOf<String, Boolean>() }
    val completedCount = completed.count { it.value }
    val totalCount = CHECKLIST.size
    val progress = completedCount.toFloat() / totalCount

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Checklist", fontFamily = PressStart2P, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Progress card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$completedCount/$totalCount", fontFamily = JetBrainsMono, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = if (completedCount == totalCount) RiskSafe else MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Steps completed", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("Secure your phone beyond just apps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surface)) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(progress).clip(RoundedCornerShape(3.dp)).background(if (completedCount == totalCount) RiskSafe else MaterialTheme.colorScheme.primary))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Group by category
            val categories = CHECKLIST.groupBy { it.category }
            val categoryOrder = listOf("Tracking", "Location", "Network", "Security", "Apps")

            categoryOrder.forEach { category ->
                val items = categories[category] ?: return@forEach
                Text(category, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))

                items.forEach { item ->
                    ChecklistRow(
                        item = item,
                        isCompleted = completed[item.id] == true,
                        onToggle = { completed[item.id] = !(completed[item.id] ?: false) },
                        onOpenSettings = { openSettings(context, item.settingsAction) }
                    )
                    Spacer(Modifier.height(6.dp))
                }
                Spacer(Modifier.height(10.dp))
            }

            // Disclaimer
            Text(
                "Settings locations may vary by device manufacturer and Android version.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

// ── Checklist Row ───────────────────────────────────────────────────────────

@Composable
private fun ChecklistRow(
    item: ChecklistItem,
    isCompleted: Boolean,
    onToggle: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) RiskSafe.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.fillMaxWidth()) {
            // Header row
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox
                Checkbox(
                    checked = isCompleted,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = RiskSafe,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Icon(item.icon, null, tint = if (isCompleted) RiskSafe else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    color = if (isCompleted) RiskSafe else MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expanded detail
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(Modifier.padding(start = 50.dp, end = 12.dp, bottom = 12.dp)) {
                    // Why it matters
                    Text("Why it matters", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(2.dp))
                    Text(item.why, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)

                    Spacer(Modifier.height(8.dp))

                    // How to fix
                    Text("How to fix", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(2.dp))
                    Text(item.howToFix, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)

                    Spacer(Modifier.height(10.dp))

                    // Open Settings button
                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.fillMaxWidth().height(34.dp),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Outlined.Settings, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Open Settings", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ── Settings Launcher ───────────────────────────────────────────────────────

private fun openSettings(context: Context, action: String) {
    try {
        if (action == "android.intent.action.VIEW") {
            // Play Store settings
            context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=com.android.vending")))
        } else {
            context.startActivity(Intent(action))
        }
    } catch (_: Exception) {
        try {
            context.startActivity(Intent(Settings.ACTION_SETTINGS))
        } catch (_: Exception) {}
    }
}
