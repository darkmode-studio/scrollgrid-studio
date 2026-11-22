package com.darkmode.scrollgridstudio.ui

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.darkmode.scrollgridstudio.model.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    onDuplicateDraft: (SavedDraftMeta) -> Unit,
    onDeleteDraft: (SavedDraftMeta) -> Unit
) {
    val dateFmt = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }

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
                Text("ScrollGrid Studio", color = HubText, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("Pick up where you left off", color = HubSubtext, fontSize = 12.sp)
            }
            Surface(
                color = Color(0xFF1A2E22),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, HubAccent)
            ) {
                Text(
                    text = "PRO UNLOCKED",
                    color = HubAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Bento Quick-Start Section
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                onClick = onStartPhotoGrid,
                color = Color(0xFF15261D),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, HubAccent),
                modifier = Modifier.fillMaxWidth()
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
                        Box(
                            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(HubAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesomeMosaic, null, tint = Color(0xFF05130A), modifier = Modifier.size(22.dp))
                        }
                        Column {
                            Text("New Photo & Video Grid", color = HubText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Pick photos first • auto-matches layout", color = HubSubtext, fontSize = 11.sp)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = HubAccent)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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

        // 2-Column Visual "My collages" Grid (matching reference screenshot)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("My collages", color = HubText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("${drafts.size} saved", color = HubSubtext, fontSize = 11.sp)
            }

            if (drafts.isEmpty()) {
                Surface(
                    color = HubCard,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, HubBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, null, tint = HubSubtext, modifier = Modifier.size(28.dp))
                        Text("Every collage saved to edit later", color = HubText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("Tap Save or go back from the editor to see visual previews here.", color = HubSubtext, fontSize = 11.sp)
                    }
                }
            } else {
                val rows = drafts.chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    rows.forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            pair.forEach { draft ->
                                CollageDraftGridCard(
                                    draft = draft,
                                    dateText = if (draft.updatedAtMillis > 0L) dateFmt.format(Date(draft.updatedAtMillis)) else "Saved",
                                    onOpen = { onOpenDraft(draft) },
                                    onDuplicate = { onDuplicateDraft(draft) },
                                    onDelete = { onDeleteDraft(draft) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Seamless SCRL Templates
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
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Canvas(
                                modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(6.dp)).background(HubBg)
                            ) {
                                val sw = size.width / tpl.slides
                                for (s in 1 until tpl.slides) {
                                    drawLine(HubCyan.copy(alpha = 0.6f), Offset(s * sw, 0f), Offset(s * sw, size.height), 1.5f)
                                }
                                drawRect(HubAccent.copy(alpha = 0.8f), Offset(size.width * 0.12f, size.height * 0.16f), Size(size.width * 0.32f, size.height * 0.62f), style = Stroke(2f))
                                drawRect(HubText.copy(alpha = 0.7f), Offset(size.width * 0.36f, size.height * 0.28f), Size(size.width * 0.30f, size.height * 0.56f), style = Stroke(2f))
                                drawRect(HubCyan.copy(alpha = 0.8f), Offset(size.width * 0.62f, size.height * 0.14f), Size(size.width * 0.28f, size.height * 0.66f), style = Stroke(2f))
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

        // Featured Grid & Shape Templates
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Shape & Grid Templates", color = HubText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            val featured = listOf(
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
                items(featured) { layout ->
                    Surface(
                        onClick = { onSelectGridTemplate(layout) },
                        color = HubCard,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, HubBorder),
                        modifier = Modifier.width(92.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Canvas(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(6.dp)).background(HubBg).padding(4.dp)) {
                                layout.slotRects().forEach { r ->
                                    drawRect(
                                        color = HubAccent,
                                        topLeft = Offset(r.left * size.width + 1.5f, r.top * size.height + 1.5f),
                                        size = Size((r.width * size.width - 3f).coerceAtLeast(3f), (r.height * size.height - 3f).coerceAtLeast(3f)),
                                        style = Stroke(width = 1.8f)
                                    )
                                }
                            }
                            Text(layout.label, color = HubText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun CollageDraftGridCard(
    draft: SavedDraftMeta,
    dateText: String,
    onOpen: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onOpen() },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Miniature 2x2 Visual Preview Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(HubCard)
                .border(1.dp, HubBorder, RoundedCornerShape(14.dp))
                .padding(3.dp)
        ) {
            if (draft.previewUris.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PhotoLibrary, null, tint = HubSubtext, modifier = Modifier.size(28.dp))
                }
            } else if (draft.previewUris.size == 1) {
                AsyncImage(
                    model = Uri.parse(draft.previewUris.first()),
                    contentDescription = draft.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
                )
            } else {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        draft.previewUris.getOrNull(0)?.let {
                            AsyncImage(model = Uri.parse(it), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(6.dp)))
                        }
                        draft.previewUris.getOrNull(1)?.let {
                            AsyncImage(model = Uri.parse(it), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(6.dp)))
                        }
                    }
                    if (draft.previewUris.size > 2) {
                        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            draft.previewUris.getOrNull(2)?.let {
                                AsyncImage(model = Uri.parse(it), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(6.dp)))
                            }
                            draft.previewUris.getOrNull(3)?.let {
                                AsyncImage(model = Uri.parse(it), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(6.dp)))
                            } ?: Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Title, Date & Overflow (...) Button
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(draft.name, color = HubText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(dateText, color = HubSubtext, fontSize = 10.sp, maxLines = 1)
            }
            Box {
                Surface(
                    onClick = { menuExpanded = true },
                    shape = CircleShape,
                    color = HubCard,
                    border = BorderStroke(1.dp, HubBorder)
                ) {
                    Icon(Icons.Default.MoreHoriz, "More", tint = HubSubtext, modifier = Modifier.padding(4.dp).size(15.dp))
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = HubCard
                ) {
                    DropdownMenuItem(
                        text = { Text("Open", color = HubText, fontSize = 12.sp) },
                        onClick = { menuExpanded = false; onOpen() }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate", color = HubText, fontSize = 12.sp) },
                        onClick = { menuExpanded = false; onDuplicate() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = HubDanger, fontSize = 12.sp) },
                        onClick = { menuExpanded = false; onDelete() }
                    )
                }
            }
        }
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
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(HubBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(19.dp))
            }
            Column {
                Text(title, color = HubText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = HubSubtext, fontSize = 11.sp)
            }
        }
    }
}
