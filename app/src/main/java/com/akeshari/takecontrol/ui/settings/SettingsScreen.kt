package com.akeshari.takecontrol.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akeshari.takecontrol.data.model.PrivacyScore
import com.akeshari.takecontrol.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    viewModel: AboutViewModel = hiltViewModel()
) {
    val score by viewModel.score.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About", fontFamily = PressStart2P, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                }
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
                Icon(Icons.Outlined.Lock, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(12.dp))
            Text("Take Control", fontFamily = PressStart2P, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Version 1.0.0", style = MaterialTheme.typography.bodySmall, fontFamily = JetBrainsMono, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(16.dp))

            // 100% Local Analysis
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = RiskSafe.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.VerifiedUser, null, tint = RiskSafe, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("100% Local Analysis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = RiskSafe)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "All scanning happens entirely on your device. No data is collected, uploaded, or shared with anyone — ever.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "The only network request is the optional App Lookup feature, which fetches publicly available Play Store data.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Score Methodology
            Text("How Your Score Works", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            score?.let { s -> ScoreMethodologyCard(s) }

            Spacer(Modifier.height(24.dp))

            // What this app does
            Text("Your privacy, made visible", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "Take Control scans your installed apps to reveal what they can access, which companies are tracking you, and how your data flows.",
                style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            // Feature guide
            Text("How to use each screen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            FeatureGuideItem(Icons.Outlined.Shield, "Score", "Your overall privacy health. Higher score = fewer risky permissions and trackers. Tap \"What's affecting your score\" to see which permission groups hurt the most and fix them.")
            FeatureGuideItem(Icons.Outlined.Visibility, "Radar", "See which companies have embedded tracking SDKs in your apps. The heatmap shows how many of your apps give each company access to sensitive data like location and camera.")
            FeatureGuideItem(Icons.Outlined.GridView, "Apps", "The permission matrix — every row is an app, every column is a permission type. Colored dots show granted access. Tap any app to see its full breakdown, trackers, and take action.")
            FeatureGuideItem(Icons.Outlined.Search, "Lookup", "Check any app before installing it. Paste a Play Store link or package name to see its permissions, data safety practices, and risk assessment.")

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Score Methodology ───────────────────────────────────────────────────────

@Composable
private fun ScoreMethodologyCard(score: PrivacyScore) {
    val permColor = when {
        score.permissionScore >= 75 -> RiskSafe
        score.permissionScore >= 50 -> RiskMedium
        else -> RiskHigh
    }
    val trackerColor = when {
        score.trackerScore >= 75 -> RiskSafe
        score.trackerScore >= 50 -> RiskMedium
        else -> RiskHigh
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {

            // The formula
            Text("Your score right now", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            FormulaRow("Score", "= 60% × Permissions + 40% × Trackers")
            Spacer(Modifier.height(4.dp))
            FormulaRow("", "= 0.6 × ${score.permissionScore} + 0.4 × ${score.trackerScore}")
            Spacer(Modifier.height(4.dp))
            val computedPerm = (score.permissionScore * 0.6).toInt()
            val computedTracker = (score.trackerScore * 0.4).toInt()
            FormulaRow("", "= $computedPerm + $computedTracker = ${score.total}")

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surface)
            Spacer(Modifier.height(16.dp))

            // Permission score explained
            Text("Permission Score", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = permColor)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(permColor.copy(alpha = 0.08f)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${score.permissionScore}", fontFamily = PressStart2P, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = permColor)
                Spacer(Modifier.width(12.dp))
                Text("/ 100", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Measures what % of sensitive permission risk you've avoided by denying access.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (score.riskTotal > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Total risk pool: ${score.riskTotal} points. You avoided ${score.riskDenied} (denied) and allowed ${score.riskGranted} (granted). Score = ${score.riskDenied} ÷ ${score.riskTotal} × 100.",
                    style = MaterialTheme.typography.bodySmall, fontFamily = JetBrainsMono, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Risk weights: Microphone & SMS (10), Location (8), Phone (7), Contacts & Camera (6), Sensors (4), Storage (3), Calendar (2).",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp
            )

            Spacer(Modifier.height(16.dp))

            // Tracker score explained
            Text("Tracker Score", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = trackerColor)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(trackerColor.copy(alpha = 0.08f)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${score.trackerScore}", fontFamily = PressStart2P, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = trackerColor)
                Spacer(Modifier.width(12.dp))
                Text("/ 100", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Measures how much tracking SDK exposure your apps have, weighted by severity.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Severity weights: Advertising & Profiling (3×, worst — pure surveillance for profit), Social (2×, data sharing), Analytics (1×, often benign), Crash Reporting & Identification (0.5×, mostly functional).",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surface)
            Spacer(Modifier.height(12.dp))

            // Why 60/40
            Text("Why 60% / 40%?", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Permissions get 60% weight because they're what you can directly control — revoking a permission immediately improves your privacy. Trackers get 40% because they represent hidden risk you didn't choose, but the only fix is uninstalling the app.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun FormulaRow(label: String, formula: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (label.isNotEmpty()) {
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(50.dp))
        } else {
            Spacer(Modifier.width(50.dp))
        }
        Text(formula, style = MaterialTheme.typography.bodySmall, fontFamily = JetBrainsMono, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FeatureGuideItem(icon: ImageVector, title: String, description: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
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
