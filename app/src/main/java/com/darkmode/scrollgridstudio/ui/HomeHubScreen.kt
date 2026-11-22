package com.darkmode.scrollgridstudio.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkmode.scrollgridstudio.data.DraftStorage
import com.darkmode.scrollgridstudio.model.*

private val HomeBg = Color(0xFF090B0E)
private val CardSurface = Color(0xFF141820)
private val CardBorder = Color(0xFF232936)
private val Emerald = Color(0xFF22C55E)
private val Cyan = Color(0xFF38BDF8)
private val TextMain = Color(0xFFF3F4F6)
private val TextMuted = Color(0xFF8B95A5)

@Composable
fun HomeHubScreen(
    onStartMode: (StudioMode, LayoutStyle?) -> Unit,
    onStartScrlTemplate: (ScrlTemplate) -> Unit,
    onOpenDraft: (String, StudioState) -> Unit
) {
    val context = LocalContext.current
    var drafts by remember { mutableStateOf(DraftStorage.listDrafts(context)) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Brand Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "ScrollGrid",
                            color = TextMain,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        )
                        Surface(
                            color = Color(0xFF12281B),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Emerald.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "STUDIO",
                                color = Emerald,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Collages, seamless carousels & print layouts",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = CardSurface,
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Emerald, modifier = Modifier.size(19.dp))
                    }
                }
            }
        }

        // Hero Primary Action: Seamless SCRL Carousel
        item {
            Surface(
                onClick = { onStartMode(StudioMode.CAROUSEL, null) },
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Emerald.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(142.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0E2419), Color(0xFF131922), Color(0xFF0E1D2F))
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth(0.66f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(color = Emerald.copy(alpha = 0.16f), shape = RoundedCornerShape(20.dp)) {
                            Text(
                                text = "SEAMLESS SWIPE",
                                color = Emerald,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text("SCRL Carousel", color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Up to 20 seamless slides with overlapping photos & auto-slicing", color = TextMuted, fontSize = 12.sp, lineHeight = 16.sp)
                    }

                    // Mini Carousel Visual Artwork
                    Canvas(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(width = 96.dp, height = 84.dp)
                    ) {
                        val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f)
                        drawRoundRect(
                            color = Color(0xFF1E2633),
                            topLeft = Offset(0f, 8f),
                            size = Size(size.width, size.height - 16f),
                            cornerRadius = CornerRadius(14f, 14f)
                        )
                        drawLine(Cyan, Offset(size.width / 2f, 8f), Offset(size.width / 2f, size.height - 8f), 2f, pathEffect = dash)
                        drawRoundRect(
                            color = Emerald.copy(alpha = 0.85f),
                            topLeft = Offset(size.width * 0.28f, 20f),
                            size = Size(size.width * 0.44f, size.height - 40f),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                    }
                }
            }
        }

        // Secondary 2-Column Hero Cards: Grid Collage + Freeform
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModeActionCard(
                    title = "Grid Collage",
                    subtitle = "15 grids, shapes & print",
                    accent = Cyan,
                    icon = Icons.Default.GridView,
                    modifier = Modifier.weight(1f),
                    onClick = { onStartMode(StudioMode.GRID, LayoutStyle.GRID_2X2) }
                )
                ModeActionCard(
                    title = "Freeform",
                    subtitle = "Layers, shadows & PNGs",
                    accent = Color(0xFFF59E0B),
                    icon = Icons.Default.Layers,
                    modifier = Modifier.weight(1f),
                    onClick = { onStartMode(StudioMode.FREESTYLE, LayoutStyle.GRID_2X2) }
                )
            }
        }

        // Curated Quick-Start Templates
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Quick Templates", color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(ScrlTemplate.entries) { tpl ->
                        Surface(
                            onClick = { onStartScrlTemplate(tpl) },
                            shape = RoundedCornerShape(16.dp),
                            color = CardSurface,
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.width(116.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(62.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF0D1117)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${tpl.slides} SLIDES", color = Emerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(tpl.label, color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                        }
                    }

                    val featuredLayouts = listOf(
                        LayoutStyle.BENTO_4,
                        LayoutStyle.POLAROID_2,
                        LayoutStyle.SLANT_SPLIT_2,
                        LayoutStyle.HEART_COLLAGE_3,
                        LayoutStyle.HEX_HONEYCOMB_4,
                        LayoutStyle.STAR_FEATURE_3
                    )
                    items(featuredLayouts) { layout ->
                        Surface(
                            onClick = { onStartMode(StudioMode.GRID, layout) },
                            shape = RoundedCornerShape(16.dp),
                            color = CardSurface,
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.width(104.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Canvas(modifier = Modifier.size(56.dp)) {
                                    layout.slotRects().forEach { r ->
                                        drawRoundRect(
                                            color = Cyan.copy(alpha = 0.85f),
                                            topLeft = Offset(r.left * size.width + 2f, r.top * size.height + 2f),
                                            size = Size((r.width * size.width - 4f).coerceAtLeast(4f), (r.height * size.height - 4f).coerceAtLeast(4f)),
                                            cornerRadius = CornerRadius(6f, 6f),
                                            style = Stroke(width = 2f)
                                        )
                                    }
                                }
                                Text(layout.label, color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // My Projects (Saved Drafts)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("My Projects", color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("${drafts.size} saved", color = TextMuted, fontSize = 12.sp)
            }
        }

        if (drafts.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CardSurface,
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, null, tint = TextMuted, modifier = Modifier.size(26.dp))
                        Column {
                            Text("No saved drafts yet", color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Projects you save inside the studio appear here for 1-tap editing.", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            items(drafts, key = { it.id }) { draft ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CardSurface,
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val loaded = DraftStorage.loadDraft(context, draft.id)
                            if (loaded != null) onOpenDraft(draft.id, loaded)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1D2430),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.PhotoLibrary, null, tint = Emerald, modifier = Modifier.size(20.dp))
                                }
                            }
                            Column {
                                Text(draft.name, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${draft.modeTitle} • ${draft.itemCount} items", color = TextMuted, fontSize = 12.sp)
                            }
                        }
                        IconButton(
                            onClick = {
                                DraftStorage.deleteDraft(context, draft.id)
                                drafts = DraftStorage.listDrafts(context)
                            }
                        ) {
                            Icon(Icons.Default.DeleteOutline, "Delete", tint = TextMuted, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeActionCard(
    title: String,
    subtitle: String,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = CardSurface,
        border = BorderStroke(1.dp, CardBorder),
        modifier = modifier.height(126.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accent.copy(alpha = 0.14f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
                }
            }
            Column {
                Text(title, color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = TextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
