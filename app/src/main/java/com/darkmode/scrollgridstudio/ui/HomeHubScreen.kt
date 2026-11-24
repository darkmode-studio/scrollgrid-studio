package com.darkmode.scrollgridstudio.ui

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.darkmode.scrollgridstudio.R
import com.darkmode.scrollgridstudio.model.*
import com.darkmode.scrollgridstudio.ui.theme.LocalScrollGridPalette
import com.darkmode.scrollgridstudio.ui.theme.ThemePreference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BgSurface: Color @Composable get() = LocalScrollGridPalette.current.paper
private val TextMain: Color @Composable get() = LocalScrollGridPalette.current.ink
private val TextMuted: Color @Composable get() = LocalScrollGridPalette.current.mutedInk
private val Accent: Color @Composable get() = LocalScrollGridPalette.current.accent
private val AccentSoft: Color @Composable get() = LocalScrollGridPalette.current.bluePale
private val CardBg: Color @Composable get() = LocalScrollGridPalette.current.cardPaper
private val BorderLine: Color @Composable get() = LocalScrollGridPalette.current.hairline
private val ElevatedBg: Color @Composable get() = LocalScrollGridPalette.current.elevated
private val DangerRed: Color @Composable get() = LocalScrollGridPalette.current.danger

@Composable
fun HomeHubScreen(
    drafts: List<SavedDraftMeta>,
    themePreference: ThemePreference,
    onThemePreferenceChange: (ThemePreference) -> Unit,
    onNewProject: () -> Unit,
    onStartPhotoGrid: () -> Unit,
    onStartCarousel: (Int) -> Unit,
    onStartFreeform: () -> Unit,
    onOpenTemplateLibrary: () -> Unit,
    onSelectScrlTemplate: (ScrlTemplate) -> Unit,
    onSelectGridTemplate: (LayoutStyle) -> Unit,
    onOpenDraft: (SavedDraftMeta) -> Unit,
    onDuplicateDraft: (SavedDraftMeta) -> Unit,
    onDeleteDraft: (SavedDraftMeta) -> Unit
) {
    var projectTab by remember { mutableStateOf("Recent") }
    var templateCategory by remember { mutableStateOf("All") }
    var showThemeMenu by remember { mutableStateOf(false) }
    val dateFmt = remember { SimpleDateFormat("MMM d · HH:mm", Locale.getDefault()) }

    val featuredLayouts = remember {
        listOf(
            LayoutStyle.GRID_2X2,
            LayoutStyle.BENTO_4,
            LayoutStyle.POLAROID_2,
            LayoutStyle.FILM_3,
            LayoutStyle.HEART_COLLAGE_3,
            LayoutStyle.HEX_HONEYCOMB_4,
            LayoutStyle.SLANT_3,
            LayoutStyle.GRID_6
        )
    }
    val visibleTemplates = remember(templateCategory) {
        if (templateCategory == "All") featuredLayouts
        else LayoutStyle.entries.filter { it.pickerCategory().equals(templateCategory, ignoreCase = true) }.take(8)
    }
    val recentProjects = remember(drafts) { drafts.take(8) }

    Column(
        Modifier
            .fillMaxSize()
            .background(BgSurface)
            .statusBarsPadding()
    ) {
        // Compact Studio Top Bar
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Image(
                    painter = painterResource(R.drawable.ic_scrollgrid_mark),
                    contentDescription = "Collage Studio Pro",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Column {
                    Text(
                        "Collage Studio Pro",
                        color = TextMain,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        "CREATIVE WORKSPACE",
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp
                    )
                }
            }
            Box {
                Surface(
                    onClick = { showThemeMenu = true },
                    shape = RoundedCornerShape(8.dp),
                    color = CardBg,
                    border = BorderStroke(1.dp, BorderLine)
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            when (themePreference) {
                                ThemePreference.DARK -> Icons.Default.DarkMode
                                ThemePreference.LIGHT -> Icons.Default.LightMode
                                ThemePreference.SYSTEM -> Icons.Default.SettingsBrightness
                            },
                            contentDescription = "Theme",
                            tint = TextMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(themePreference.label, color = TextMain, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
                DropdownMenu(expanded = showThemeMenu, onDismissRequest = { showThemeMenu = false }) {
                    ThemePreference.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            leadingIcon = {
                                Icon(
                                    when (option) {
                                        ThemePreference.SYSTEM -> Icons.Default.SettingsBrightness
                                        ThemePreference.LIGHT -> Icons.Default.LightMode
                                        ThemePreference.DARK -> Icons.Default.DarkMode
                                    },
                                    null
                                )
                            },
                            trailingIcon = { if (themePreference == option) Icon(Icons.Default.Check, null) },
                            onClick = {
                                onThemePreferenceChange(option)
                                showThemeMenu = false
                            }
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = BorderLine, thickness = 1.dp)

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Primary Create Action (Immediately Accessible)
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onNewProject,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("New Project", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("Pick Media", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
                }

                // 4 Core Studio Workflows
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StudioModeTile(
                        title = "Grid Collage",
                        subtitle = "200+ layouts",
                        icon = Icons.Default.GridView,
                        modifier = Modifier.weight(1f),
                        onClick = onStartPhotoGrid
                    )
                    StudioModeTile(
                        title = "Freestyle",
                        subtitle = "Free layers",
                        icon = Icons.Default.DashboardCustomize,
                        modifier = Modifier.weight(1f),
                        onClick = onStartFreeform
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StudioModeTile(
                        title = "Carousel Studio",
                        subtitle = "Seamless swipe",
                        icon = Icons.Default.ViewCarousel,
                        modifier = Modifier.weight(1f),
                        onClick = { onStartCarousel(3) }
                    )
                    StudioModeTile(
                        title = "Template Library",
                        subtitle = "Search & filter",
                        icon = Icons.Default.AutoAwesomeMosaic,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenTemplateLibrary
                    )
                }
            }

            // Recent & Saved Projects
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Recent", "Saved (${drafts.size})").forEachIndexed { idx, label ->
                            val isRecent = idx == 0
                            val active = (projectTab == "Recent" && isRecent) || (projectTab == "Saved" && !isRecent)
                            Surface(
                                onClick = { projectTab = if (isRecent) "Recent" else "Saved" },
                                shape = RoundedCornerShape(8.dp),
                                color = if (active) ElevatedBg else Color.Transparent,
                                border = BorderStroke(1.dp, if (active) Accent.copy(alpha = 0.6f) else BorderLine)
                            ) {
                                Text(
                                    label,
                                    color = if (active) TextMain else TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    Text(
                        if (projectTab == "Recent") "Recent Projects" else "All Saved",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                val shownProjects = if (projectTab == "Recent") recentProjects else drafts
                if (shownProjects.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 18.dp)
                            .fillMaxWidth(),
                        color = CardBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderLine)
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AccentSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.FolderOpen, null, tint = Accent, modifier = Modifier.size(20.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text("No saved projects yet", color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Projects save automatically when you tap Save in the editor.",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                } else if (projectTab == "Recent") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(shownProjects, key = { it.id }) { draft ->
                            ProjectPreviewCard(
                                draft = draft,
                                dateText = if (draft.updatedAtMillis > 0L) dateFmt.format(Date(draft.updatedAtMillis)) else "Saved",
                                onOpen = { onOpenDraft(draft) },
                                onDuplicate = { onDuplicateDraft(draft) },
                                onDelete = { onDeleteDraft(draft) }
                            )
                        }
                    }
                } else {
                    Column(
                        Modifier.padding(horizontal = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        shownProjects.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                pair.forEach { draft ->
                                    ProjectPreviewCard(
                                        draft = draft,
                                        dateText = if (draft.updatedAtMillis > 0L) dateFmt.format(Date(draft.updatedAtMillis)) else "Saved",
                                        modifier = Modifier.weight(1f),
                                        onOpen = { onOpenDraft(draft) },
                                        onDuplicate = { onDuplicateDraft(draft) },
                                        onDelete = { onDeleteDraft(draft) }
                                    )
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Template Library Preview
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Template Library", color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Browse all →",
                        color = Accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onOpenTemplateLibrary() }
                    )
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("All", "Grid", "Bento", "Geometric", "Magazine", "Polaroid", "Film-Strip", "Scrapbook").forEach { cat ->
                        val selected = templateCategory == cat
                        Surface(
                            onClick = { templateCategory = cat },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) ElevatedBg else CardBg,
                            border = BorderStroke(1.dp, if (selected) Accent else BorderLine)
                        ) {
                            Text(
                                cat,
                                color = if (selected) TextMain else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(visibleTemplates, key = { it.name }) { layout ->
                        TemplatePreviewTile(layout = layout, onClick = { onSelectGridTemplate(layout) })
                    }
                }
            }

            // Carousel Presets
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Carousel Presets",
                    color = TextMain,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(ScrlTemplate.entries.toList()) { template ->
                        Surface(
                            onClick = { onSelectScrlTemplate(template) },
                            shape = RoundedCornerShape(10.dp),
                            color = CardBg,
                            border = BorderStroke(1.dp, BorderLine),
                            modifier = Modifier.width(156.dp)
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ViewCarousel, null, tint = Accent, modifier = Modifier.size(18.dp))
                                    Text("${template.slides} slides", color = TextMuted, fontSize = 10.sp)
                                }
                                Text(template.label, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    repeat(template.slides.coerceAtMost(5)) {
                                        Box(
                                            Modifier
                                                .weight(1f)
                                                .height(16.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(ElevatedBg)
                                                .border(0.5.dp, BorderLine, RoundedCornerShape(3.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioModeTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = CardBg,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, BorderLine)
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Accent, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = TextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ProjectPreviewCard(
    draft: SavedDraftMeta,
    dateText: String,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onOpen,
        modifier = modifier.widthIn(min = 160.dp),
        color = CardBg,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, BorderLine)
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(ElevatedBg)
            ) {
                if (draft.previewUris.isEmpty()) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        null,
                        tint = TextMuted,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(26.dp)
                    )
                } else {
                    val previews = draft.previewUris.take(4)
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        previews.chunked(2).forEach { rowUris ->
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                rowUris.forEach { uriStr ->
                                    AsyncImage(
                                        model = Uri.parse(uriStr),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )
                                }
                            }
                        }
                    }
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xCC0D0F14)
                ) {
                    Text(
                        draft.modeTitle,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    draft.name,
                    color = TextMain,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("${draft.itemCount} items · $dateText", color = TextMuted, fontSize = 10.sp, maxLines = 1)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onDuplicate, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.ContentCopy, "Duplicate", tint = TextMuted, modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.DeleteOutline, "Delete", tint = DangerRed, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

@Composable
private fun TemplatePreviewTile(layout: LayoutStyle, onClick: () -> Unit) {
    val accentCol = Accent
    val lineCol = BorderLine
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = CardBg,
        border = BorderStroke(1.dp, BorderLine),
        modifier = Modifier.width(106.dp)
    ) {
        Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(ElevatedBg)
                    .padding(6.dp)
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val gap = 2.dp.toPx()
                    layout.slotRects().forEach { r ->
                        val l = r.left * size.width + gap / 2f
                        val t = r.top * size.height + gap / 2f
                        val w = (r.width * size.width - gap).coerceAtLeast(2f)
                        val h = (r.height * size.height - gap).coerceAtLeast(2f)
                        drawRoundRect(
                            color = accentCol.copy(alpha = 0.25f),
                            topLeft = Offset(l, t),
                            size = Size(w, h),
                            cornerRadius = CornerRadius(3.dp.toPx())
                        )
                        drawRoundRect(
                            color = lineCol,
                            topLeft = Offset(l, t),
                            size = Size(w, h),
                            cornerRadius = CornerRadius(3.dp.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx())
                        )
                    }
                }
            }
            Text(
                layout.label,
                color = TextMain,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text("${layout.slotCount} photos · ${layout.pickerCategory()}", color = TextMuted, fontSize = 9.sp, maxLines = 1)
        }
    }
}
