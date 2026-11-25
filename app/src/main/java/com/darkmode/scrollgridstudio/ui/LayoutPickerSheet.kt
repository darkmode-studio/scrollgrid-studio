package com.darkmode.scrollgridstudio.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.darkmode.scrollgridstudio.model.LayoutStyle
import com.darkmode.scrollgridstudio.model.MediaItem
import com.darkmode.scrollgridstudio.model.pickerCategory
import com.darkmode.scrollgridstudio.ui.theme.LocalScrollGridPalette

private val LayoutCategories = listOf(
    "All", "Favorites", "Grid", "Stitch", "Geometric", "Magazine", "Bento", "Film-Strip", "Polaroid", "Scrapbook", "Asymmetric"
)

@Composable
fun LayoutPickerSheetContent(
    currentLayout: LayoutStyle,
    selectedMedia: List<MediaItem>,
    favoriteLayouts: Set<String>,
    onToggleFavorite: (LayoutStyle) -> Unit,
    onApplyLayout: (LayoutStyle) -> Unit,
    onOpenMediaSelection: () -> Unit
) {
    val palette = LocalScrollGridPalette.current
    val mediaCount = selectedMedia.size
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    // Auto-prioritize compatible photo count when user already selected photos
    var countFilter by remember(mediaCount) {
        mutableStateOf(if (mediaCount in 1..12) mediaCount else null)
    }
    var previewLayout by remember { mutableStateOf(currentLayout) }

    val filteredLayouts = remember(query, selectedCategory, countFilter, favoriteLayouts, mediaCount) {
        val q = query.trim().lowercase()
        LayoutStyle.entries
            .filter { layout ->
                val catOk = when (selectedCategory) {
                    "All" -> true
                    "Favorites" -> favoriteLayouts.contains(layout.name)
                    else -> layout.pickerCategory().equals(selectedCategory, ignoreCase = true)
                }
                val countOk = when (val cf = countFilter) {
                    null -> true
                    7 -> layout.slotCount >= 7
                    else -> layout.slotCount == cf
                }
                val queryOk = q.isEmpty() ||
                    layout.label.lowercase().contains(q) ||
                    layout.pickerCategory().lowercase().contains(q) ||
                    layout.category.lowercase().contains(q) ||
                    layout.slotCount.toString() == q
                catOk && countOk && queryOk
            }
            .sortedWith(
                compareByDescending<LayoutStyle> { mediaCount > 0 && it.slotCount == mediaCount }
                    .thenByDescending { favoriteLayouts.contains(it.name) }
                    .thenBy { kotlin.math.abs(it.slotCount - mediaCount.coerceAtLeast(2)) }
            )
    }

    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header + media count badge
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Layout Library", color = palette.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (mediaCount > 0) "Prioritized for $mediaCount selected media · ${filteredLayouts.size} shown"
                    else "${filteredLayouts.size} layouts available",
                    color = palette.textSecondary,
                    fontSize = 11.sp
                )
            }
            TextButton(onClick = onOpenMediaSelection) {
                Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("Media ($mediaCount)", fontSize = 12.sp)
            }
        }

        // Search Bar
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search bento, heart, polaroid, film, magazine…", fontSize = 12.sp, color = palette.textSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = palette.textSecondary, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Close, "Clear search", tint = palette.textSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // Photo Count Filter Chips
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf<Pair<String, Int?>>(
                "All" to null,
                "1" to 1, "2" to 2, "3" to 3, "4" to 4, "5" to 5, "6" to 6, "7+" to 7
            ).forEach { (label, cnt) ->
                val active = countFilter == cnt
                Surface(
                    onClick = { countFilter = if (active && cnt != null) null else cnt },
                    shape = RoundedCornerShape(6.dp),
                    color = if (active) palette.accent else palette.cardPaper,
                    border = BorderStroke(1.dp, if (active) palette.accent else palette.border)
                ) {
                    Text(
                        label,
                        color = if (active) Color.White else palette.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Category Filter Chips
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LayoutCategories.forEach { cat ->
                val active = selectedCategory == cat
                Surface(
                    onClick = { selectedCategory = cat },
                    shape = RoundedCornerShape(6.dp),
                    color = if (active) palette.elevated else Color.Transparent,
                    border = BorderStroke(1.dp, if (active) palette.accent else palette.border)
                ) {
                    Text(
                        cat,
                        color = if (active) palette.textPrimary else palette.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Live Preview Bar (Preview Before Applying)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = palette.cardPaper,
            border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.55f))
        ) {
            Row(
                Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LayoutLivePreviewBox(
                    layout = previewLayout,
                    media = selectedMedia,
                    modifier = Modifier.size(64.dp)
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(previewLayout.label, color = palette.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${previewLayout.slotCount} slots · ${previewLayout.pickerCategory()}",
                        color = palette.textSecondary,
                        fontSize = 11.sp
                    )
                    if (mediaCount > previewLayout.slotCount) {
                        Text(
                            "${mediaCount - previewLayout.slotCount} extra photo(s) preserved in Extras",
                            color = palette.accent,
                            fontSize = 10.sp
                        )
                    }
                }
                Button(
                    onClick = { onApplyLayout(previewLayout) },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = Color.White)
                ) {
                    Text("Apply", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Layout Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredLayouts, key = { it.name }) { layout ->
                val isSelected = previewLayout == layout
                val isFav = favoriteLayouts.contains(layout.name)
                Surface(
                    onClick = { previewLayout = layout },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) palette.elevated else palette.cardPaper,
                    border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) palette.accent else palette.border)
                ) {
                    Box(Modifier.padding(7.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            LayoutLivePreviewBox(
                                layout = layout,
                                media = selectedMedia,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(74.dp)
                            )
                            Text(
                                layout.label,
                                color = palette.textPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            onClick = { onToggleFavorite(layout) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(22.dp),
                            shape = CircleShape,
                            color = Color(0x990D0F14)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFav) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LayoutLivePreviewBox(
    layout: LayoutStyle,
    media: List<MediaItem>,
    modifier: Modifier = Modifier
) {
    val palette = LocalScrollGridPalette.current
    BoxWithConstraints(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(palette.canvas)
            .padding(3.dp)
    ) {
        val rects = remember(layout) { layout.slotRects() }
        if (media.isEmpty()) {
            Canvas(Modifier.fillMaxSize()) {
                val gap = 1.5.dp.toPx()
                rects.forEach { r ->
                    val l = r.left * size.width + gap / 2f
                    val t = r.top * size.height + gap / 2f
                    val w = (r.width * size.width - gap).coerceAtLeast(2f)
                    val h = (r.height * size.height - gap).coerceAtLeast(2f)
                    drawRoundRect(
                        color = palette.accent.copy(alpha = 0.25f),
                        topLeft = Offset(l, t),
                        size = Size(w, h),
                        cornerRadius = CornerRadius(2.dp.toPx())
                    )
                    drawRoundRect(
                        color = palette.border,
                        topLeft = Offset(l, t),
                        size = Size(w, h),
                        cornerRadius = CornerRadius(2.dp.toPx()),
                        style = Stroke(0.8.dp.toPx())
                    )
                }
            }
        } else {
            val boxW = maxWidth
            val boxH = maxHeight
            rects.forEachIndexed { idx, r ->
                val item = media.getOrNull(idx)
                Box(
                    Modifier
                        .offset(x = boxW * r.left, y = boxH * r.top)
                        .size(width = (boxW * r.width - 1.5.dp).coerceAtLeast(4.dp), height = (boxH * r.height - 1.5.dp).coerceAtLeast(4.dp))
                        .clip(RoundedCornerShape(2.dp))
                        .background(palette.elevated)
                        .border(0.5.dp, palette.border, RoundedCornerShape(2.dp))
                ) {
                    if (item != null) {
                        AsyncImage(
                            model = item.uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
