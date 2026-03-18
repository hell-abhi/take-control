package com.akeshari.takecontrol.ui.alternatives

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akeshari.takecontrol.data.model.PrivacyAlternative
import com.akeshari.takecontrol.data.model.PrivacyAlternativesData
import com.akeshari.takecontrol.ui.theme.*
import com.google.accompanist.drawablepainter.rememberDrawablePainter

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

// ── App Icon Helper ─────────────────────────────────────────────────────────

@Composable
private fun AppIcon(packageName: String, appName: String, color: Color, size: Int = 40) {
    val context = LocalContext.current
    val icon = remember(packageName) { getAppIcon(context, packageName) }

    if (icon != null) {
        Image(
            painter = rememberDrawablePainter(icon),
            contentDescription = appName,
            modifier = Modifier.size(size.dp).clip(RoundedCornerShape(10.dp))
        )
    } else {
        // Fallback: styled initial
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                appName.first().toString(),
                fontFamily = PressStart2P,
                fontSize = (size / 3).sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

private fun getAppIcon(context: Context, packageName: String): Drawable? {
    return try {
        context.packageManager.getApplicationIcon(packageName)
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
}

// ── Swipe View ──────────────────────────────────────────────────────────────

@Composable
private fun SwipeView(alternatives: List<PrivacyAlternative>, modifier: Modifier) {
    val pagerState = rememberPagerState(pageCount = { alternatives.size })

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        Text("Swipe to discover privacy-friendly apps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 28.dp),
            pageSpacing = 16.dp
        ) { page ->
            SwipeCard(alternatives[page])
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
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
        Spacer(Modifier.height(4.dp))
        Text("${pagerState.currentPage + 1} / ${alternatives.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

            // "Instead of" section
            Text("Instead of", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(alt.mainstreamPackage, alt.mainstream, RiskHigh)
                Spacer(Modifier.width(12.dp))
                Text(alt.mainstream, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = RiskHigh)
            }
            Spacer(Modifier.height(8.dp))
            Text(alt.mainstreamIssue, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surface)
            Spacer(Modifier.height(18.dp))

            // "Try" section
            Text("Try", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(alt.alternativePackage, alt.alternative, RiskSafe)
                Spacer(Modifier.width(12.dp))
                Text(alt.alternative, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = RiskSafe)
            }
            Spacer(Modifier.height(8.dp))
            Text(alt.whyBetter, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)

            Spacer(Modifier.height(14.dp))
            alt.keyFeatures.forEach { feature ->
                Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CheckCircle, null, tint = RiskSafe, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(feature, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${alt.alternativePackage}")))
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

// ── List View (card-based) ──────────────────────────────────────────────────

@Composable
private fun ListView(alternatives: List<PrivacyAlternative>, modifier: Modifier) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        Text(
            "All privacy alternatives",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))

        alternatives.forEach { alt ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    // Category
                    Text(alt.category, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(10.dp))

                    // Mainstream → Alternative
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mainstream app
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            AppIcon(alt.mainstreamPackage, alt.mainstream, RiskHigh, size = 44)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                alt.mainstream,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = RiskHigh,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Arrow
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowForward,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Alternative app
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            AppIcon(alt.alternativePackage, alt.alternative, RiskSafe, size = 44)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                alt.alternative.split("(").first().trim(),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = RiskSafe,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // One-line reason
                    Text(
                        alt.whyBetter.split(".").first() + ".",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(10.dp))

                    // Get button
                    OutlinedButton(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${alt.alternativePackage}")))
                        },
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Outlined.OpenInNew, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Get ${alt.alternative.split(" ").first()}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
