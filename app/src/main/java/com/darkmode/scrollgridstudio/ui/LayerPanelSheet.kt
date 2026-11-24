package com.darkmode.scrollgridstudio.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.darkmode.scrollgridstudio.model.MediaItem
import com.darkmode.scrollgridstudio.model.StudioState
import com.darkmode.scrollgridstudio.model.TextOverlay
import com.darkmode.scrollgridstudio.ui.theme.LocalScrollGridPalette

private sealed class LayerEntry(val id: String, val zIndex: Int) {
    data class OverlayEntry(val overlay: TextOverlay) : LayerEntry("ov_${overlay.id}", overlay.zIndex)
    data class MediaEntry(val index: Int, val item: MediaItem) : LayerEntry("media_${item.id}", item.zIndex)
    data class DrawingEntry(val strokeCount: Int) : LayerEntry("drawing_layer", 90)
}

@Composable
fun LayerPanelContent(
    state: StudioState,
    onSelectMedia: (Int) -> Unit,
    onToggleMediaVisible: (Int) -> Unit,
    onToggleMediaLock: (Int) -> Unit,
    onMoveMediaZ: (Int, Int) -> Unit,
    onDuplicateMedia: (Int) -> Unit,
    onDeleteMedia: (Int) -> Unit,
    onSelectOverlay: (String) -> Unit,
    onToggleOverlayVisible: (String) -> Unit,
    onToggleOverlayLock: (String) -> Unit,
    onMoveOverlayZ: (String, Int) -> Unit,
    onDuplicateOverlay: (String) -> Unit,
    onDeleteOverlay: (String) -> Unit,
    onToggleDrawingsVisible: () -> Unit,
    onToggleDrawingsLock: () -> Unit,
    onClearDrawings: () -> Unit
) {
    val palette = LocalScrollGridPalette.current
    val entries = buildList {
        state.overlays.forEach { add(LayerEntry.OverlayEntry(it)) }
        if (state.drawingStrokes.isNotEmpty()) {
            add(LayerEntry.DrawingEntry(state.drawingStrokes.size))
        }
        state.slots.forEachIndexed { idx, item -> add(LayerEntry.MediaEntry(idx, item)) }
    }.sortedByDescending { it.zIndex }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Layers", color = palette.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${state.slots.size} image · ${state.overlays.count { !it.isSticker }} text · ${state.overlays.count { it.isSticker }} sticker",
                    color = palette.textSecondary,
                    fontSize = 10.sp
                )
            }
            Text("Top → Bottom", color = palette.textSecondary, fontSize = 10.sp)
        }

        if (entries.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = palette.canvas,
                border = BorderStroke(1.dp, palette.border)
            ) {
                Text(
                    "No layers yet. Add photos, text, stickers, or drawings.",
                    color = palette.textSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    when (entry) {
                        is LayerEntry.OverlayEntry -> {
                            val ov = entry.overlay
                            val selected = state.selectedOverlayId == ov.id
                            LayerRowCard(
                                title = if (ov.isSticker) (if (ov.stickerUri != null) "Image Sticker" else "Sticker ${ov.text}") else ov.text.ifBlank { "Text Layer" },
                                subtitle = if (ov.isSticker) "Sticker Layer · z:${ov.zIndex}" else "Text Layer · z:${ov.zIndex}",
                                selected = selected,
                                visible = ov.visible,
                                locked = ov.locked,
                                thumbnail = {
                                    Box(
                                        Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(palette.elevated),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (ov.stickerUri != null) {
                                            AsyncImage(
                                                model = ov.stickerUri,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                if (ov.isSticker) Icons.Default.EmojiEmotions else Icons.Default.Title,
                                                null,
                                                tint = palette.accent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                onSelect = { onSelectOverlay(ov.id) },
                                onToggleVisible = { onToggleOverlayVisible(ov.id) },
                                onToggleLock = { onToggleOverlayLock(ov.id) },
                                onMoveUp = { onMoveOverlayZ(ov.id, 5) },
                                onMoveDown = { onMoveOverlayZ(ov.id, -5) },
                                onDuplicate = { onDuplicateOverlay(ov.id) },
                                onDelete = { onDeleteOverlay(ov.id) }
                            )
                        }
                        is LayerEntry.DrawingEntry -> {
                            LayerRowCard(
                                title = "Drawing Layer",
                                subtitle = "${entry.strokeCount} stroke(s)",
                                selected = false,
                                visible = state.drawingsVisible,
                                locked = state.drawingsLocked,
                                thumbnail = {
                                    Box(
                                        Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(palette.elevated),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Brush, null, tint = palette.coral, modifier = Modifier.size(18.dp))
                                    }
                                },
                                onSelect = {},
                                onToggleVisible = onToggleDrawingsVisible,
                                onToggleLock = onToggleDrawingsLock,
                                onMoveUp = null,
                                onMoveDown = null,
                                onDuplicate = null,
                                onDelete = onClearDrawings
                            )
                        }
                        is LayerEntry.MediaEntry -> {
                            val item = entry.item
                            val idx = entry.index
                            val selected = state.selectedOverlayId == null && state.selectedSlotIndex == idx
                            LayerRowCard(
                                title = if (item.isVideo) "Video Layer ${idx + 1}" else "Photo Layer ${idx + 1}",
                                subtitle = "Image · z:${item.zIndex} · ${(item.opacity * 100).toInt()}%",
                                selected = selected,
                                visible = item.visible,
                                locked = item.locked,
                                thumbnail = {
                                    AsyncImage(
                                        model = item.uri,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .border(0.5.dp, palette.border, RoundedCornerShape(6.dp))
                                    )
                                },
                                onSelect = { onSelectMedia(idx) },
                                onToggleVisible = { onToggleMediaVisible(idx) },
                                onToggleLock = { onToggleMediaLock(idx) },
                                onMoveUp = { onMoveMediaZ(idx, 1) },
                                onMoveDown = { onMoveMediaZ(idx, -1) },
                                onDuplicate = { onDuplicateMedia(idx) },
                                onDelete = { onDeleteMedia(idx) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LayerRowCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    visible: Boolean,
    locked: Boolean,
    thumbnail: @Composable () -> Unit,
    onSelect: () -> Unit,
    onToggleVisible: () -> Unit,
    onToggleLock: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onDuplicate: (() -> Unit)?,
    onDelete: () -> Unit
) {
    val palette = LocalScrollGridPalette.current
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) palette.elevated else palette.cardPaper,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) palette.accent else palette.border)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            thumbnail()
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = if (visible) palette.textPrimary else palette.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(subtitle, color = palette.textSecondary, fontSize = 10.sp, maxLines = 1)
            }
            if (onMoveUp != null && onMoveDown != null) {
                IconButton(onClick = onMoveUp, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.KeyboardArrowUp, "Bring Forward", tint = palette.textSecondary, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onMoveDown, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, "Send Back", tint = palette.textSecondary, modifier = Modifier.size(16.dp))
                }
            }
            IconButton(onClick = onToggleVisible, modifier = Modifier.size(26.dp)) {
                Icon(
                    if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    "Toggle Visibility",
                    tint = if (visible) palette.textPrimary else palette.textSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(15.dp)
                )
            }
            IconButton(onClick = onToggleLock, modifier = Modifier.size(26.dp)) {
                Icon(
                    if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
                    "Toggle Lock",
                    tint = if (locked) palette.coral else palette.textSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
            if (onDuplicate != null) {
                IconButton(onClick = onDuplicate, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.ContentCopy, "Duplicate", tint = palette.textSecondary, modifier = Modifier.size(14.dp))
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Default.DeleteOutline, "Delete", tint = palette.danger, modifier = Modifier.size(15.dp))
            }
        }
    }
}
