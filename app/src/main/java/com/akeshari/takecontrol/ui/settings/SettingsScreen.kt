package com.akeshari.takecontrol.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akeshari.takecontrol.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About", fontFamily = PressStart2P, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // App branding
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Take Control",
                fontFamily = PressStart2P,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    "Version 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = JetBrainsMono,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(24.dp))

            // What this app does
            Text(
                "Your privacy, made visible",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Take Control scans your installed apps to reveal what they can access, which companies are tracking you, and how your data flows. No data leaves your device — everything is analyzed locally.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            // Feature guide
            Text(
                "How to use each screen",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            FeatureGuideItem(
                icon = Icons.Outlined.Shield,
                title = "Score",
                description = "Your overall privacy health. Higher score = fewer risky permissions granted. Tap \"What's affecting your score\" to see which permission groups hurt the most and fix them."
            )
            FeatureGuideItem(
                icon = Icons.Outlined.Visibility,
                title = "Radar",
                description = "See which companies have embedded tracking SDKs in your apps. The heatmap shows how many of your apps give each company access to sensitive data like location and camera."
            )
            FeatureGuideItem(
                icon = Icons.Outlined.GridView,
                title = "Apps",
                description = "The permission matrix — every row is an app, every column is a permission type. Colored dots show granted access. Tap any app to see its full breakdown, trackers, and take action."
            )
            FeatureGuideItem(
                icon = Icons.Outlined.Search,
                title = "Lookup",
                description = "Check any app before installing it. Paste a Play Store link or package name to see its permissions, data safety practices, and risk assessment."
            )

            Spacer(Modifier.height(16.dp))

            // Privacy note
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = RiskSafe.copy(alpha = 0.08f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.Lock, null, tint = RiskSafe, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("100% Local Analysis", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = RiskSafe)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "All scanning happens on your device. No data is collected, uploaded, or shared. The only network request is the optional App Lookup feature which fetches public Play Store data.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FeatureGuideItem(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
