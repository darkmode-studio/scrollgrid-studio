package com.darkmode.scrollgridstudio.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkmode.scrollgridstudio.model.*

private val HubBg = Color(0xFF0B0D10)
private val HubCard = Color(0xFF14171D)
private val HubBorder = Color(0xFF242933)
private val HubAccent = Color(0xFF22C55E)
private val HubCyan = Color(0xFF38BDF8)
private val HubDanger = Color(0xFFEF4444)
private val HubText = Color(0xFFF3F4F6)
private val HubSubtext = Color(0xFF9CA3AF)

@Composable
fun HomeHubScreen(
    drafts: List<SavedDraftMeta>,
    onStartPhotoGrid: () -> Unit,
    onStartCarousel: (Int) -> Unit,
    onStartFreeform: () -> Unit,
    onSelectScrlTemplate: (ScrlTemplate) -> Unit,
    onSelectGridTemplate: (LayoutStyle) -> Unit,
    onOpenDraft: (SavedDraftMeta) -> Unit,
    onDeleteDraft: (SavedDraftMeta) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HubBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Brand Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ScrollGrid Studio",
                    color = HubText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Collage Maker + Seamless SCRL Carousels",
                    color = HubSubtext,
                    fontSize = 12.sp
                )
            }
            Surface(
                color = Color(0xFF1A2E22),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, HubAccent)
            ) {
                Text(
                    text = "ALL UNLOCKED",
                    color = HubAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Bento Quick-Start Section
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Primary Hero Card: Photo-First Smart Grid
            Surface(
                onClick = onStartPhotoGrid,
                color = Color(0xFF15261D),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, HubAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(HubAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesomeMosaic, null, tint = Color(0xFF05130A), modifier = Modifier.size(24.dp))
                        }
                        Column {
                            Text("Photo & Video Grid", color = HubText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Pick photos first • auto-matches layout", color = HubSubtext, fontSize = 11.sp)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = HubAccent)
                }
            }

            // Secondary Bento Row: SCRL Carousel + Freeform Canvas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickStartTile(
                    title = "SCRL Carousel",
                    subtitle = "Multi-slide swipe",
                    icon = Icons.Default.ViewCarousel,
                    accent = HubCyan,
                    onClick = { onStartCarousel(3) },
                    modifier = Modifier.weight(1f)
                )
                QuickStartTile(
                    title = "Freeform",
                    subtitle = "Open scrapbook",
                    icon = Icons.Default.DashboardCustomize,
                    accent = HubAccent,
                    onClick = onStartFreeform,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // SCRL Editorial Carousel Templates
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Seamless Carousel Templates", color = HubText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(ScrlTemplate.entries) { tpl ->
                    Surface(
                        onClick = { onSelectScrlTemplate(tpl) },
                        color = HubCard,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, HubBorder),
                        modifier = Modifier.width(136.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Mini multi-slide wireframe preview
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(HubBg)
                            ) {
                                val sw = size.width / tpl.slides
                                for (s in 1 until tpl.slides) {
                                    drawLine(
                                        color = HubCyan.copy(alpha = 0.6f),
                                        start = Offset(s * sw, 0f),
                                        end = Offset(s * sw, size.height),
                                        strokeWidth = 1.5f
                                    )
                                }
                                // Overlapping editorial cards crossing seams
                                drawRect(
                                    color = HubAccent.copy(alpha = 0.8f),
                                    topLeft = Offset(size.width * 0.12f, size.height * 0.16f),
                                    size = Size(size.width * 0.32f, size.height * 0.62f),
                                    style = Stroke(width = 2f)
                                )
                                drawRect(
                                    color = HubText.copy(alpha = 0.7f),
                                    topLeft = Offset(size.width * 0.36f, size.height * 0.28f),
                                    size = Size(size.width * 0.30f, size.height * 0.56f),
                                    style = Stroke(width = 2f)
                                )
                                drawRect(
                                    color = HubCyan.copy(alpha = 0.8f),
                                    topLeft = Offset(size.width * 0.62f, size.height * 0.14f),
                                    size = Size(size.width * 0.28f, size.height * 0.66f),
                                    style = Stroke(width = 2f)
                                )
                            }
                            Column {
                                Text(tpl.label, color = HubText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("${tpl.slides} seamless slides", color = HubSubtext, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // Featured Shape & Grid Templates
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Shape & Grid Templates", color = HubText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            val featuredLayouts = listOf(
                LayoutStyle.POLAROID_2,
                LayoutStyle.SLANT_SPLIT_2,
                LayoutStyle.BENTO_4,
                LayoutStyle.HEART_COLLAGE_3,
                LayoutStyle.STAR_FEATURE_3,
                LayoutStyle.HEX_HONEYCOMB_4,
                LayoutStyle.FILM_3,
                LayoutStyle.GRID_9
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(featuredLayouts) { layout ->
                    Surface(
                        onClick = { onSelectGridTemplate(layout) },
                        color = HubCard,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, HubBorder),
                        modifier = Modifier.width(96.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(HubBg)
                                    .padding(4.dp)
                            ) {
                                layout.slotRects().forEach { r ->
                                    drawRect(
                                        color = HubAccent,
                                        topLeft = Offset(r.left * size.width + 1.5f, r.top * size.height + 1.5f),
                                        size = Size(
                                            (r.width * size.width - 3f).coerceAtLeast(3f),
                                            (r.height * size.height - 3f).coerceAtLeast(3f)
                                        ),
                                        style = Stroke(width = 1.8f)
                                    )
                                }
                            }
                            Text(
                                text = layout.label,
                                color = HubText,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // My Collages (Saved Projects)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("My Collages (${drafts.size})", color = HubText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Stored offline", color = HubSubtext, fontSize = 11.sp)
            }

            if (drafts.isEmpty()) {
                Surface(
                    color = HubCard,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, HubBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, null, tint = HubSubtext, modifier = Modifier.size(28.dp))
                        Text("No saved projects yet", color = HubText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("Tap 'Save Draft' inside the editor to keep collages here.", color = HubSubtext, fontSize = 11.sp)
                    }
                }
            } else {
                drafts.forEach { draft ->
                    Surface(
                        color = HubCard,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, HubBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenDraft(draft) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1F2631)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, null, tint = HubCyan, modifier = Modifier.size(18.dp))
                                }
                                Column {
                                    Text(
                                        text = draft.name,
                                        color = HubText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${draft.modeTitle} • ${draft.itemCount} media",
                                        color = HubSubtext,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            IconButton(onClick = { onDeleteDraft(draft) }) {
                                Icon(Icons.Default.Delete, "Delete project", tint = HubDanger, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun QuickStartTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = HubCard,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, HubBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(HubBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(title, color = HubText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = HubSubtext, fontSize = 11.sp)
            }
        }
    }
}
