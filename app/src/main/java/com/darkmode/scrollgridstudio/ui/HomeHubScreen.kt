package com.darkmode.scrollgridstudio.ui

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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

private val StudioBg = Color(0xFF101210)
private val StudioPanel = Color(0xFF191D19)
private val StudioRaised = Color(0xFF222821)
private val StudioLine = Color(0xFF30382F)
private val StudioLime = Color(0xFFC5F36A)
private val StudioCream = Color(0xFFF2F0E8)
private val StudioMuted = Color(0xFFA1A99D)
private val StudioOrange = Color(0xFFE6A36A)

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
    var templateFilter by remember { mutableStateOf("All") }
    val dateFmt = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    val featured = remember {
        listOf(LayoutStyle.BENTO_4, LayoutStyle.POLAROID_2, LayoutStyle.SLANT_3, LayoutStyle.FILM_3,
            LayoutStyle.HEART_COLLAGE_3, LayoutStyle.HEX_HONEYCOMB_4, LayoutStyle.GRID_2X2, LayoutStyle.GRID_9)
    }
    val visibleTemplates = featured.filter { templateFilter == "All" || it.category.equals(templateFilter, true) }

    Column(Modifier.fillMaxSize().background(StudioBg).statusBarsPadding().navigationBarsPadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(27.dp)
        ) {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(StudioLime), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.GridView, null, tint = StudioBg, modifier = Modifier.size(21.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("SCROLLGRID", color = StudioCream, fontSize = 15.sp, fontWeight = FontWeight.Black, letterSpacing = 1.8.sp)
                        Text("CREATIVE STUDIO", color = StudioMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.1.sp)
                    }
                }
                Surface(color = StudioPanel, shape = CircleShape, border = BorderStroke(1.dp, StudioLine)) {
                    Row(Modifier.padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(StudioLime))
                        Text("STUDIO", color = StudioCream, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("YOUR CANVAS.", color = StudioMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.2.sp)
                    Text("Make it\nworth a swipe.", color = StudioCream, fontSize = 35.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1.3).sp)
                }
                Box(
                    Modifier.fillMaxWidth().height(218.dp).clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF354333), Color(0xFF1C2820), Color(0xFF171B17)))
                ) {
                    HeroArtwork(Modifier.fillMaxSize())
                    Column(Modifier.align(Alignment.BottomStart).padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("THE CREATIVE PLAYGROUND", color = StudioLime, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
                        Text("Your next story\nstarts here.", color = Color.White, fontSize = 23.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold)
                    }
                    Surface(onClick = onStartPhotoGrid, color = StudioLime, shape = CircleShape, modifier = Modifier.align(Alignment.BottomEnd).padding(17.dp)) {
                        Row(Modifier.padding(start = 15.dp, end = 12.dp, top = 11.dp, bottom = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Create", color = StudioBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ArrowUpward, null, tint = StudioBg, modifier = Modifier.size(15.dp))
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StartModeCard("Carousel", "Seamless swipe posts", Icons.Default.ViewCarousel, StudioOrange, Modifier.weight(1f)) { onStartCarousel(3) }
                    StartModeCard("Freeform", "Layer it your way", Icons.Default.DashboardCustomize, StudioLime, Modifier.weight(1f), onStartFreeform)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                SectionHeading("YOUR WORKSPACE", "Recent projects", if (drafts.isEmpty()) "0 PROJECTS" else "\${drafts.size} PROJECTS")
                if (drafts.isEmpty()) {
                    Surface(color = StudioPanel, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, StudioLine), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                            Box(Modifier.size(48.dp).clip(RoundedCornerShape(13.dp)).background(StudioRaised), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.FolderOpen, null, tint = StudioLime, modifier = Modifier.size(23.dp))
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                                Text("A clean slate.", color = StudioCream, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Your saved collages will live here. Start creating to fill this space.", color = StudioMuted, fontSize = 11.sp, lineHeight = 16.sp)
                            }
                        }
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 2.dp)) {
                        items(drafts, key = { it.id }) { draft ->
                            DraftCard(draft, if (draft.updatedAtMillis > 0L) dateFmt.format(Date(draft.updatedAtMillis)) else "Saved",
                                { onOpenDraft(draft) }, { onDuplicateDraft(draft) }, { onDeleteDraft(draft) })
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                SectionHeading("CURATED STARTING POINTS", "Explore templates", "VIEW ALL")
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Classic", "Hero", "Shapes", "Masonry").forEach { category ->
                        val selected = templateFilter == category
                        Surface(onClick = { templateFilter = category }, color = if (selected) StudioLime else StudioPanel,
                            shape = CircleShape, border = BorderStroke(1.dp, if (selected) StudioLime else StudioLine)) {
                            Text(category, color = if (selected) StudioBg else StudioMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 15.dp, vertical = 8.dp))
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    visibleTemplates.chunked(2).forEach { rowTemplates ->
                        Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                            rowTemplates.forEach { layout ->
                                TemplateCard(layout, Modifier.weight(1f)) { onSelectGridTemplate(layout) }
                            }
                            if (rowTemplates.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading("BUILT FOR THE FEED", "Swipe stories", "SCRL")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    items(ScrlTemplate.entries.toList()) { template -> CarouselCard(template) { onSelectScrlTemplate(template) } }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Row(
            Modifier.fillMaxWidth().background(StudioBg).border(BorderStroke(0.5.dp, StudioLine)).padding(horizontal = 28.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(Icons.Default.Home, "Home", true)
            BottomNavItem(Icons.Default.GridView, "Templates", false)
            BottomNavItem(Icons.Default.FolderOpen, "Projects", false)
            BottomNavItem(Icons.Default.Settings, "Settings", false)
        }
    }
}

@Composable
private fun HeroArtwork(modifier: Modifier = Modifier) {
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            drawCircle(StudioLime.copy(alpha = .08f), radius = h * .78f, center = androidx.compose.ui.geometry.Offset(w * .83f, h * .30f))
            drawCircle(StudioLime.copy(alpha = .12f), radius = h * .56f, center = androidx.compose.ui.geometry.Offset(w * .83f, h * .30f), style = Stroke(1.2.dp.toPx()))
            drawLine(Color.White.copy(alpha = .12f), androidx.compose.ui.geometry.Offset(w * .56f, 0f), androidx.compose.ui.geometry.Offset(w * .56f, h), 1.dp.toPx())
            drawLine(Color.White.copy(alpha = .12f), androidx.compose.ui.geometry.Offset(w * .78f, 0f), androidx.compose.ui.geometry.Offset(w * .78f, h), 1.dp.toPx())
        }
        Box(Modifier.align(Alignment.TopEnd).padding(top = 19.dp, end = 27.dp).size(90.dp, 132.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFFB7A68A)).padding(4.dp)) {
            Canvas(Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp))) {
                drawRect(Brush.linearGradient(listOf(Color(0xFF82917C), Color(0xFF263E35))))
                drawCircle(Color(0xFFE7C58E), size.minDimension * .18f, androidx.compose.ui.geometry.Offset(size.width * .68f, size.height * .25f))
                val mountain = Path().apply { moveTo(0f, size.height * .74f); lineTo(size.width * .42f, size.height * .38f); lineTo(size.width * .72f, size.height * .66f); lineTo(size.width, size.height * .46f); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
                drawPath(mountain, Color(0xFF263B31))
            }
        }
        Box(Modifier.align(Alignment.TopEnd).padding(top = 76.dp, end = 115.dp).size(78.dp, 105.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE9DCC4)).padding(4.dp)) {
            Canvas(Modifier.fillMaxSize().clip(RoundedCornerShape(5.dp))) {
                drawRect(Brush.linearGradient(listOf(Color(0xFFBD8F68), Color(0xFF6E4E3F))))
                drawCircle(Color(0xFFD9B88E), size.minDimension * .18f, androidx.compose.ui.geometry.Offset(size.width * .48f, size.height * .36f))
                drawRect(Color(0xFF34463A), androidx.compose.ui.geometry.Offset(size.width * .18f, size.height * .58f), androidx.compose.ui.geometry.Size(size.width * .64f, size.height * .45f))
            }
        }
        Surface(Modifier.align(Alignment.TopEnd).padding(top = 17.dp, end = 16.dp), color = StudioLime, shape = CircleShape) {
            Text("01 / 03", color = StudioBg, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
        }
    }
}

@Composable
private fun SectionHeading(eyebrow: String, title: String, trailing: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(eyebrow, color = StudioLime, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
            Text(title, color = StudioCream, fontSize = 21.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.5).sp)
        }
        Text(trailing, color = StudioMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 4.dp))
    }
}

@Composable
private fun StartModeCard(title: String, detail: String, icon: ImageVector, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier, color = StudioPanel, shape = RoundedCornerShape(17.dp), border = BorderStroke(1.dp, StudioLine)) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(accent.copy(alpha = .13f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(19.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
                Text(title, color = StudioCream, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(detail, color = StudioMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun DraftCard(draft: SavedDraftMeta, dateText: String, onOpen: () -> Unit, onDuplicate: () -> Unit, onDelete: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    Column(Modifier.width(190.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(16.dp)).background(StudioRaised).clickable(onClick = onOpen)) {
            if (draft.previewUris.isEmpty()) {
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF303B30), Color(0xFF1A211B)))), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Collections, null, tint = StudioLime.copy(alpha = .8f), modifier = Modifier.size(30.dp))
                }
            } else {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    draft.previewUris.take(3).forEachIndexed { index, uri ->
                        AsyncImage(model = Uri.parse(uri), contentDescription = if (index == 0) draft.name else null, contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(if (index == 0) 1.2f else 1f).fillMaxHeight())
                    }
                }
            }
            Surface(Modifier.align(Alignment.TopEnd).padding(7.dp), color = Color.Black.copy(alpha = .58f), shape = CircleShape) {
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.MoreHoriz, "Project options", tint = Color.White, modifier = Modifier.size(17.dp))
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }, containerColor = StudioRaised) {
                        DropdownMenuItem(text = { Text("Open") }, onClick = { menuExpanded = false; onOpen() })
                        DropdownMenuItem(text = { Text("Duplicate") }, onClick = { menuExpanded = false; onDuplicate() })
                        DropdownMenuItem(text = { Text("Delete", color = Color(0xFFFF8B80)) }, onClick = { menuExpanded = false; onDelete() })
                    }
                }
            }
            Surface(Modifier.align(Alignment.BottomStart).padding(9.dp), color = Color.Black.copy(alpha = .58f), shape = CircleShape) {
                Text(draft.modeTitle.uppercase(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
            }
        }
        Column(Modifier.clickable(onClick = onOpen).padding(horizontal = 2.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(draft.name, color = StudioCream, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$dateText  ·  \${draft.itemCount} items", color = StudioMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun TemplateCard(layout: LayoutStyle, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier, color = StudioPanel, shape = RoundedCornerShape(17.dp), border = BorderStroke(1.dp, StudioLine)) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(Modifier.fillMaxWidth().aspectRatio(1.18f).clip(RoundedCornerShape(11.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF2A352B), Color(0xFF171D18)))).padding(10.dp)) {
                LayoutArtwork(layout, Modifier.fillMaxSize())
                Surface(Modifier.align(Alignment.TopEnd), color = StudioLime, shape = CircleShape) {
                    Text("\${layout.slotCount}P", color = StudioBg, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(layout.label, color = StudioCream, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(layout.category.uppercase(), color = StudioMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            }
        }
    }
}

@Composable
private fun LayoutArtwork(layout: LayoutStyle, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val gap = 3.dp.toPx()
        val palette = listOf(Color(0xFFB8A88A), Color(0xFF6F8973), Color(0xFFC17C55), Color(0xFF596B62), Color(0xFFD5C9AD))
        layout.slotRects().forEachIndexed { index, rect ->
            val left = rect.left * size.width + gap
            val top = rect.top * size.height + gap
            val width = (rect.width * size.width - gap * 2).coerceAtLeast(2f)
            val height = (rect.height * size.height - gap * 2).coerceAtLeast(2f)
            drawRoundRect(palette[index % palette.size], androidx.compose.ui.geometry.Offset(left, top),
                androidx.compose.ui.geometry.Size(width, height), androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()))
            drawRoundRect(Color.White.copy(alpha = .12f), androidx.compose.ui.geometry.Offset(left, top),
                androidx.compose.ui.geometry.Size(width, height), androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()), style = Stroke(1.dp.toPx()))
        }
    }
}

@Composable
private fun CarouselCard(template: ScrlTemplate, onClick: () -> Unit) {
    Surface(onClick = onClick, color = StudioPanel, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, StudioLine), modifier = Modifier.width(220.dp)) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth().height(92.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(template.slides.coerceAtMost(5)) { index ->
                    Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(5.dp))
                        .background(listOf(Color(0xFF536B55), Color(0xFFB78B68), Color(0xFF687C78), Color(0xFFD1BFA0), Color(0xFF465747))[index]))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(template.label, color = StudioCream, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("\${template.slides} seamless slides", color = StudioMuted, fontSize = 10.sp)
                }
                Icon(Icons.Default.ArrowOutward, null, tint = StudioLime, modifier = Modifier.size(17.dp))
            }
        }
    }
}

@Composable
private fun BottomNavItem(icon: ImageVector, label: String, selected: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = if (selected) StudioLime else StudioMuted, modifier = Modifier.size(19.dp))
        Text(label, color = if (selected) StudioLime else StudioMuted, fontSize = 9.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}
