package com.darkmode.scrollgridstudio.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.darkmode.scrollgridstudio.model.MediaItem
import com.darkmode.scrollgridstudio.ui.theme.LocalScrollGridPalette

@Composable
fun MediaSelectionSheetContent(
    selectedMedia: List<MediaItem>,
    onPickPhotos: () -> Unit,
    onPickVideos: () -> Unit,
    onPickPhotosAndVideos: () -> Unit,
    onRemoveAt: (Int) -> Unit,
    onMoveItem: (fromIndex: Int, toIndex: Int) -> Unit,
    onClearAll: () -> Unit,
    onChooseLayout: () -> Unit,
    onConfirmSelection: () -> Unit
) {
    val palette = LocalScrollGridPalette.current
    val photoCount = selectedMedia.count { !it.isVideo }
    val videoCount = selectedMedia.count { it.isVideo }

    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Select Media", color = palette.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (selectedMedia.isEmpty()) "Add photos or videos to begin"
                    else "${selectedMedia.size} selected · $photoCount photo(s), $videoCount video(s)",
                    color = palette.textSecondary,
                    fontSize = 11.sp
                )
            }
            if (selectedMedia.isNotEmpty()) {
                TextButton(onClick = onClearAll) {
                    Text("Clear", color = palette.danger, fontSize = 12.sp)
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onPickPhotos,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                border = BorderStroke(1.dp, palette.border)
            ) {
                Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(16.dp), tint = palette.accent)
                Spacer(Modifier.width(5.dp))
                Text("Photos", fontSize = 12.sp, color = palette.textPrimary)
            }
            OutlinedButton(
                onClick = onPickVideos,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                border = BorderStroke(1.dp, palette.border)
            ) {
                Icon(Icons.Default.VideoLibrary, null, modifier = Modifier.size(16.dp), tint = palette.accent)
                Spacer(Modifier.width(5.dp))
                Text("Videos", fontSize = 12.sp, color = palette.textPrimary)
            }
            OutlinedButton(
                onClick = onPickPhotosAndVideos,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                border = BorderStroke(1.dp, palette.border)
            ) {
                Icon(Icons.Default.PermMedia, null, modifier = Modifier.size(16.dp), tint = palette.accent)
                Spacer(Modifier.width(5.dp))
                Text("Mixed", fontSize = 12.sp, color = palette.textPrimary)
            }
        }

        if (selectedMedia.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                color = palette.canvas,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, palette.border)
            ) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, null, tint = palette.textSecondary, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.height(6.dp))
                    Text("No media staged yet", color = palette.textSecondary, fontSize = 12.sp)
                    Text("Pick multiple photos or videos above", color = palette.textSecondary.copy(alpha = 0.7f), fontSize = 10.sp)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(selectedMedia, key = { _, item -> item.id }) { idx, item ->
                    Box(
                        Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.elevated)
                            .border(1.dp, palette.border, RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = item.uri,
                            contentDescription = "Selected media ${idx + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Order badge
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(4.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xCC0D0F14)
                        ) {
                            Text(
                                "${idx + 1}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                        if (item.isVideo) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 4.dp),
                                shape = RoundedCornerShape(4.dp),
                                color = palette.accent.copy(alpha = 0.9f)
                            ) {
                                Text(
                                    "VID",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        // Remove button
                        Surface(
                            onClick = { onRemoveAt(idx) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(22.dp),
                            shape = CircleShape,
                            color = Color(0xCC14171E)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Close, "Remove", tint = Color.White, modifier = Modifier.size(13.dp))
                            }
                        }
                        // Reorder controls
                        Row(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color(0xB30D0F14))
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = { if (idx > 0) onMoveItem(idx, idx - 1) },
                                enabled = idx > 0,
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    Icons.Default.ChevronLeft,
                                    "Move Left",
                                    tint = if (idx > 0) Color.White else Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            IconButton(
                                onClick = { if (idx < selectedMedia.lastIndex) onMoveItem(idx, idx + 1) },
                                enabled = idx < selectedMedia.lastIndex,
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    "Move Right",
                                    tint = if (idx < selectedMedia.lastIndex) Color.White else Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onChooseLayout,
                enabled = selectedMedia.isNotEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, palette.border)
            ) {
                Icon(Icons.Default.GridView, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Layouts", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onConfirmSelection,
                enabled = selectedMedia.isNotEmpty(),
                modifier = Modifier
                    .weight(1.3f)
                    .height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = Color.White)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Confirm (${selectedMedia.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
