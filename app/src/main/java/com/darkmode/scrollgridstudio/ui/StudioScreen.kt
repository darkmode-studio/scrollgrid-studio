package com.darkmode.scrollgridstudio.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.darkmode.scrollgridstudio.export.CollageExporter
import com.darkmode.scrollgridstudio.model.*
import java.util.UUID

@Composable
fun StudioScreen() {
    val context = LocalContext.current
    var state by remember { mutableStateOf(StudioState()) }
    var textInput by remember { mutableStateOf("") }
    var statusBanner by remember { mutableStateOf("Full studio unlocked • Grid, Carousel & Typography ready") }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(24)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val added = uris.map { uri ->
                val mime = context.contentResolver.getType(uri) ?: ""
                MediaItem(
                    id = UUID.randomUUID().toString(),
                    uri = uri,
                    isVideo = mime.startsWith("video/")
                )
            }
            val pool = state.slots + state.extras + added
            val cap = state.maxCapacity()
            state = state.copy(
                slots = pool.take(cap),
                extras = pool.drop(cap),
                selectedSlotIndex = 0
            )
            statusBanner = "Added ${added.size} items • ${state.extras.size} queued in Extras"
        }
    }

    fun applyLayoutCapacity(nextMode: StudioMode, nextLayout: LayoutStyle, nextSlides: Int) {
        val tempState = state.copy(mode = nextMode, layout = nextLayout, carouselSlides = nextSlides)
        val cap = tempState.maxCapacity()
        val pool = tempState.slots + tempState.extras
        state = tempState.copy(
            slots = pool.take(cap),
            extras = pool.drop(cap),
            selectedSlotIndex = 0
        )
    }

    val canvasBgColor = when (state.backgroundStyle) {
        BackgroundStyle.OBSIDIAN -> Color(0xFF0C0F14)
        BackgroundStyle.FOREST -> Color(0xFF133326)
        BackgroundStyle.BAMBOO -> Color(0xFF1D3B2B)
        BackgroundStyle.NAVY -> Color(0xFF0F1E36)
        BackgroundStyle.BLUR_MATCH -> Color(0xFF1C232E)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top Header: Modes + Lock + Export
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StudioMode.entries.forEach { mode ->
                FilterChip(
                    selected = state.mode == mode,
                    onClick = { applyLayoutCapacity(mode, state.layout, state.carouselSlides) },
                    label = { Text(mode.title) }
                )
            }
            IconButton(
                onClick = {
                    val locked = !state.canvasLocked
                    state = state.copy(canvasLocked = locked)
                    statusBanner = if (locked) "Canvas locked against accidental touch edits" else "Canvas unlocked"
                }
            ) {
                Icon(
                    imageVector = if (state.canvasLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "Toggle Canvas Lock",
                    tint = if (state.canvasLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            Button(
                onClick = {
                    val savedName = CollageExporter.exportPng(
                        context = context,
                        slots = state.slots,
                        overlays = state.overlays,
                        backgroundStyle = state.backgroundStyle,
                        borderDp = state.borderDp,
                        widthPx = state.exportWidthPx,
                        heightPx = state.exportHeightPx
                    )
                    statusBanner = if (savedName != null) {
                        "Saved $savedName to Pictures/ScrollGridStudio"
                    } else {
                        "Unable to export image"
                    }
                }
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Export")
            }
        }

        // Layout Presets or Carousel Slide Count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.mode == StudioMode.CAROUSEL) {
                Text("Slides:", style = MaterialTheme.typography.labelLarge)
                IconButton(onClick = {
                    val next = (state.carouselSlides - 1).coerceAtLeast(2)
                    applyLayoutCapacity(state.mode, state.layout, next)
                }) { Icon(Icons.Default.Remove, contentDescription = "Fewer slides") }
                Text("${state.carouselSlides}", fontWeight = FontWeight.Bold)
                IconButton(onClick = {
                    val next = (state.carouselSlides + 1).coerceAtMost(10)
                    applyLayoutCapacity(state.mode, state.layout, next)
                }) { Icon(Icons.Default.Add, contentDescription = "More slides") }
            } else {
                LayoutStyle.entries.forEach { layout ->
                    FilterChip(
                        selected = state.layout == layout,
                        onClick = { applyLayoutCapacity(state.mode, layout, state.carouselSlides) },
                        label = { Text(layout.label) }
                    )
                }
            }
        }

        // Stepper Controls: Border & Radius
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Gap ${state.borderDp}dp", style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = {
                    state = state.copy(borderDp = (state.borderDp - 2).coerceAtLeast(0))
                }) { Icon(Icons.Default.Remove, contentDescription = "Gap minus") }
                IconButton(onClick = {
                    state = state.copy(borderDp = (state.borderDp + 2).coerceAtMost(32))
                }) { Icon(Icons.Default.Add, contentDescription = "Gap plus") }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Corner ${state.cornerRadiusDp}dp", style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = {
                    state = state.copy(cornerRadiusDp = (state.cornerRadiusDp - 2).coerceAtLeast(0))
                }) { Icon(Icons.Default.Remove, contentDescription = "Radius minus") }
                IconButton(onClick = {
                    state = state.copy(cornerRadiusDp = (state.cornerRadiusDp + 2).coerceAtMost(40))
                }) { Icon(Icons.Default.Add, contentDescription = "Radius plus") }
            }
        }

        // Palette Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackgroundStyle.entries.forEach { bg ->
                FilterChip(
                    selected = state.backgroundStyle == bg,
                    onClick = { state = state.copy(backgroundStyle = bg) },
                    label = { Text(bg.label) }
                )
            }
        }

        // Interactive Canvas Stage
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(canvasBgColor)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .pointerInput(state.canvasLocked) {
                    if (!state.canvasLocked) {
                        detectTransformGestures { _, _, _, _ -> }
                    }
                }
                .padding(state.borderDp.dp)
        ) {
            if (state.slots.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Select photos or videos to begin", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        picker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    }) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Pick Media")
                    }
                }
            } else {
                val isCarousel = state.mode == StudioMode.CAROUSEL
                if (isCarousel) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(state.borderDp.dp)
                    ) {
                        state.slots.forEachIndexed { idx, item ->
                            MediaCellCard(
                                item = item,
                                isSelected = idx == state.selectedSlotIndex,
                                cornerRadiusDp = state.cornerRadiusDp,
                                canvasLocked = state.canvasLocked,
                                onSelect = { state = state.copy(selectedSlotIndex = idx) },
                                onSwapWithNext = {
                                    if (idx + 1 < state.slots.size && !state.canvasLocked) {
                                        val mutable = state.slots.toMutableList()
                                        val temp = mutable[idx]
                                        mutable[idx] = mutable[idx + 1]
                                        mutable[idx + 1] = temp
                                        state = state.copy(slots = mutable)
                                    }
                                },
                                modifier = Modifier
                                    .width(240.dp)
                                    .fillMaxHeight()
                            )
                        }
                    }
                } else {
                    val rows = state.slots.chunked(2)
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(state.borderDp.dp)
                    ) {
                        rows.forEachIndexed { rowIdx, rowItems ->
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(state.borderDp.dp)
                            ) {
                                rowItems.forEachIndexed { colIdx, item ->
                                    val globalIdx = rowIdx * 2 + colIdx
                                    MediaCellCard(
                                        item = item,
                                        isSelected = globalIdx == state.selectedSlotIndex,
                                        cornerRadiusDp = state.cornerRadiusDp,
                                        canvasLocked = state.canvasLocked,
                                        onSelect = { state = state.copy(selectedSlotIndex = globalIdx) },
                                        onSwapWithNext = {
                                            if (globalIdx + 1 < state.slots.size && !state.canvasLocked) {
                                                val mutable = state.slots.toMutableList()
                                                val temp = mutable[globalIdx]
                                                mutable[globalIdx] = mutable[globalIdx + 1]
                                                mutable[globalIdx + 1] = temp
                                                state = state.copy(slots = mutable)
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )
                                }
                                if (rowItems.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Overlays
            state.overlays.forEach { overlay ->
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (overlay.usePill) Color(0xCC0C0F14) else Color.Transparent)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = overlay.text,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Extras Queue Strip
        if (state.extras.isNotEmpty()) {
            Text(
                "Extras Queue (${state.extras.size}) — tap to swap into active frame",
                style = MaterialTheme.typography.labelMedium
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(state.extras, key = { it.id }) { extra ->
                    AsyncImage(
                        model = extra.uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (state.slots.isNotEmpty()) {
                                    val targetIdx = state.selectedSlotIndex.coerceIn(0, state.slots.lastIndex)
                                    val replaced = state.slots[targetIdx]
                                    val nextSlots = state.slots.toMutableList().apply { this[targetIdx] = extra }
                                    val nextExtras = state.extras.filterNot { it.id == extra.id } + replaced
                                    state = state.copy(slots = nextSlots, extras = nextExtras)
                                }
                            }
                    )
                }
            }
        }

        // Bottom Text & Media Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Add English / Malayalam caption") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        state = state.copy(
                            overlays = state.overlays + TextOverlay(
                                id = UUID.randomUUID().toString(),
                                text = textInput.trim()
                            )
                        )
                        textInput = ""
                    }
                }
            ) {
                Icon(Icons.Default.TextFields, contentDescription = "Add caption")
            }
            IconButton(
                onClick = {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add media")
            }
        }

        Text(
            text = statusBanner,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun MediaCellCard(
    item: MediaItem,
    isSelected: Boolean,
    cornerRadiusDp: Int,
    canvasLocked: Boolean,
    onSelect: () -> Unit,
    onSwapWithNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    var panOffsetX by remember(item.id) { mutableFloatStateOf(0f) }
    var panOffsetY by remember(item.id) { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadiusDp.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(cornerRadiusDp.dp)
            )
            .clickable { onSelect() }
            .pointerInput(item.id, canvasLocked) {
                if (!canvasLocked) {
                    detectDragGestures(
                        onDragEnd = {
                            if (kotlin.math.abs(panOffsetX) > 90f) {
                                onSwapWithNext()
                            }
                            panOffsetX = 0f
                            panOffsetY = 0f
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        panOffsetX += dragAmount.x
                        panOffsetY += dragAmount.y
                    }
                }
            }
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = panOffsetX * 0.25f
                    translationY = panOffsetY * 0.25f
                }
        )
        if (item.isVideo) {
            Surface(
                color = Color(0xCC0C0F14),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            ) {
                Text(
                    text = "VIDEO",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
