package com.akeshari.takecontrol.ui.alternatives

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akeshari.takecontrol.data.model.PrivacyAlternative
import com.akeshari.takecontrol.data.model.PrivacyAlternativesData
import com.akeshari.takecontrol.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlternativesScreen(
    onBack: () -> Unit
) {
    var showListView by remember { mutableStateOf(false) }
    val alternatives = PrivacyAlternativesData.ALL

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Picks", fontFamily = PressStart2P, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showListView = !showListView }) {
                        Icon(
                            if (showListView) Icons.Outlined.ViewCarousel else Icons.Outlined.ViewList,
                            if (showListView) "Card view" else "List view"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (showListView) {
            ListView(alternatives, Modifier.padding(padding))
        } else {
            SwipeView(alternatives, Modifier.padding(padding))
        }
    }
}

// ── Swipe View (Bumble-style cards) ─────────────────────────────────────────

@Composable
private fun SwipeView(alternatives: List<PrivacyAlternative>, modifier: Modifier) {
    val pagerState = rememberPagerState(pageCount = { alternatives.size })

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))

        Text(
            "Swipe to discover privacy-friendly apps",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))

        // Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 28.dp),
            pageSpacing = 16.dp
        ) { page ->
            SwipeCard(alternatives[page])
        }

        // Dots indicator
        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            repeat(alternatives.size) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (index == pagerState.currentPage) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == pagerState.currentPage) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                )
            }
        }

        // Counter
        Spacer(Modifier.height(6.dp))
        Text(
            "${pagerState.currentPage + 1} / ${alternatives.size}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SwipeCard(alt: PrivacyAlternative) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(22.dp)
        ) {
            // Category badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(alt.category, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(16.dp))

            // "Instead of" section with app initial
            Text("Instead of", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppInitialBadge(alt.mainstream, RiskHigh)
                Spacer(Modifier.width(12.dp))
                Text(alt.mainstream, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = RiskHigh)
            }
            Spacer(Modifier.height(8.dp))
            Text(alt.mainstreamIssue, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surface)
            Spacer(Modifier.height(18.dp))

            // "Try" section with app initial
            Text("Try", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppInitialBadge(alt.alternative, RiskSafe)
                Spacer(Modifier.width(12.dp))
                Text(alt.alternative, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = RiskSafe)
            }
            Spacer(Modifier.height(8.dp))
            Text(alt.whyBetter, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)

            Spacer(Modifier.height(14.dp))

            // Key features
            alt.keyFeatures.forEach { feature ->
                Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CheckCircle, null, tint = RiskSafe, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(feature, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Get on Play Store button
            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${alt.alternativePackage}"))
                    )
                },
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Outlined.OpenInNew, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Get ${alt.alternative.split(" ").first()} on Play Store", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AppInitialBadge(name: String, color: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            name.first().toString(),
            fontFamily = PressStart2P,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ── List View (table) ───────────────────────────────────────────────────────

@Composable
private fun ListView(alternatives: List<PrivacyAlternative>, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(4.dp))

        // Scrollable table
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            // Header row
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(vertical = 10.dp)
            ) {
                TableCell("What For", 100.dp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                TableCell("You're Using", 120.dp, fontWeight = FontWeight.Bold, color = RiskHigh)
                TableCell("Switch To", 120.dp, fontWeight = FontWeight.Bold, color = RiskSafe)
                TableCell("Why It's Better", 200.dp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surface)

            // Data rows
            alternatives.forEachIndexed { index, alt ->
                Row(
                    modifier = Modifier
                        .background(
                            if (index % 2 == 0) Color.Transparent
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                        .padding(vertical = 10.dp)
                ) {
                    TableCell(alt.category, 100.dp)
                    TableCell(alt.mainstream, 120.dp, color = RiskHigh)
                    TableCell(alt.alternative, 120.dp, color = RiskSafe, fontWeight = FontWeight.SemiBold)
                    TableCell(alt.whyBetter, 200.dp, maxLines = 3)
                }
                if (index < alternatives.size - 1) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                }
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun TableCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = MaterialTheme.colorScheme.onSurface,
    maxLines: Int = 2
) {
    Box(modifier = Modifier.width(width).padding(horizontal = 12.dp)) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = fontWeight,
            color = color,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp
        )
    }
}
