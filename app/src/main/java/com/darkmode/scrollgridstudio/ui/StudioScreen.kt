package com.darkmode.scrollgridstudio.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.darkmode.scrollgridstudio.export.CollageExporter
import com.darkmode.scrollgridstudio.export.ExportResult
import com.darkmode.scrollgridstudio.model.*
import java.util.UUID
import kotlin.math.roundToInt

private val BgCanvas = Color(0xFF0B0D10)
private val BgPanel = Color(0xFF14171D)
private val BorderSubtle = Color(0xFF242933)
private val AccentGreen = Color(0xFF22C55E)
private val CyanGuide = Color(0xFF38BDF8)
private val TextPrimary = Color(0xFFF3F4F6)
private val TextSecondary = Color(0xFF9CA3AF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen() {
    val context = LocalContext.current
    var state by remember { mutableStateOf(StudioState()) }
    var textInput by remember { mutableStateOf("") }
    var showExportSheet by remember { mutableStateOf(false) }
    var lastExportResult by remember { mutableStateOf<ExportResult?>(null) }
    var statusBanner by remember { mutableStateOf("ScrollGrid Studio • Flat Dark Workspace Ready") }

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
            statusBanner = "Loaded ${added.size} item(s) • ${state.slots.size}/$cap active"
        }
    }

    val replacePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null && state.slots.isNotEmpty()) {
            val idx = state.selectedSlotIndex.coerceIn(0, state.slots.lastIndex)
            val mime = context.contentResolver.getType(uri) ?: ""
            val updated = state.slots.toMutableList().apply {
                this[idx] = this[idx].copy(
                    uri = uri,
                    isVideo = mime.startsWith("video/"),
                    scale = 1f,
                    panX = 0f,
                    panY = 0f,
                    rotationDeg = 0f
                )
            }
            state = state.copy(slots = updated)
            statusBanner = "Replaced tile #${idx + 1}"
        }
    }

    fun applyLayoutCapacity(nextMode: StudioMode, nextLayout: LayoutStyle, nextSlides: Int) {
        val clampedSlides = nextSlides.coerceIn(1, 20)
        val temp = state.copy(mode = nextMode, layout = nextLayout, carouselSlides = clampedSlides)
        val cap = temp.maxCapacity()
        val pool = temp.slots + temp.extras
        state = temp.copy(
            slots = pool.take(cap),
            extras = pool.drop(cap),
            selectedSlotIndex = 0,
            activeSlideIndex = temp.activeSlideIndex.coerceIn(0, (clampedSlides - 1).coerceAtLeast(0))
        )
    }

    fun updateSelectedMedia(transform: (MediaItem) -> MediaItem) {
        if (state.slots.isEmpty()) return
        val idx = state.selectedSlotIndex.coerceIn(0, state.slots.lastIndex)
        val updated = state.slots.toMutableList().apply {
            this[idx] = transform(this[idx])
        }
        state = state.copy(slots = updated)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(BgPanel)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudioMode.entries.forEach { mode ->
                    val selected = state.mode == mode
                    Surface(
                        onClick = { applyLayoutCapacity(mode, state.layout, state.carouselSlides) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) Color(0xFF232934) else Color.Transparent,
                        border = BorderStroke(1.dp, if (selected) AccentGreen else BorderSubtle)
                    ) {
                        Text(
                            text = mode.title,
                            color = if (selected) TextPrimary else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Surface(
                onClick = {
                    val locked = !state.canvasLocked
                    state = state.copy(canvasLocked = locked)
                    statusBanner = if (locked) "Canvas locked against accidental touches" else "Canvas unlocked"
                },
                shape = RoundedCornerShape(8.dp),
                color = if (state.canvasLocked) Color(0xFF1A2E22) else BgCanvas,
                border = BorderStroke(1.dp, if (state.canvasLocked) AccentGreen else BorderSubtle)
            ) {
                Icon(
                    imageVector = if (state.canvasLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "Lock Canvas",
                    tint = if (state.canvasLocked) AccentGreen else TextSecondary,
                    modifier = Modifier
                        .padding(7.dp)
                        .size(18.dp)
                )
            }

            Button(
                onClick = { showExportSheet = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    contentColor = Color(0xFF05130A)
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Export", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        HorizontalDivider(thickness = 1.dp, color = BorderSubtle)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgCanvas)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = statusBanner,
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${state.exportConfig.aspectRatio.label} • ${state.exportConfig.outputWidthPx()}px",
                color = AccentGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            val canvasAspect = if (state.mode == StudioMode.CAROUSEL) {
                (state.exportConfig.aspectRatio.widthRatio.toFloat() * state.carouselSlides.coerceAtMost(3)) /
                    state.exportConfig.aspectRatio.heightRatio.toFloat()
            } else {
                state.exportConfig.aspectRatio.widthRatio.toFloat() /
                    state.exportConfig.aspectRatio.heightRatio.toFloat()
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(canvasAspect.coerceIn(0.45f, 2.2f), matchHeightConstraintsFirst = true)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(state.backgroundStyle.hex))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            ) {
                if (state.slots.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("Tap to Add Photos or Videos", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Grid • Seamless SCRL Carousel • Freeform", color = TextSecondary, fontSize = 11.sp)
                    }
                } else {
                    val cols = if (state.mode == StudioMode.CAROUSEL) state.carouselSlides * 2 else 2
                    val rows = if (state.mode == StudioMode.CAROUSEL) 1 else ((state.slots.size + 1) / 2).coerceAtLeast(1)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(state.borderDp.dp),
                        verticalArrangement = Arrangement.spacedBy(state.borderDp.dp)
                    ) {
                        for (r in 0 until rows) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(state.borderDp.dp)
                            ) {
                                for (c in 0 until cols) {
                                    val idx = r * cols + c
                                    val item = state.slots.getOrNull(idx)
                                    if (item != null) {
                                        val isSelected = idx == state.selectedSlotIndex
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(state.cornerRadiusDp.dp))
                                                .background(Color(0xFF1C232E))
                                                .border(
                                                    width = if (isSelected) 2.dp else 0.dp,
                                                    color = if (isSelected) AccentGreen else Color.Transparent,
                                                    shape = RoundedCornerShape(state.cornerRadiusDp.dp)
                                                )
                                                .clickable {
                                                    state = state.copy(selectedSlotIndex = idx)
                                                }
                                                .pointerInput(item.id, state.canvasLocked) {
                                                    if (!state.canvasLocked) {
                                                        detectTransformGestures { _, pan, zoom, rot ->
                                                            updateSelectedMedia { cur ->
                                                                if (cur.id == item.id) {
                                                                    cur.copy(
                                                                        scale = (cur.scale * zoom).coerceIn(0.5f, 4f),
                                                                        panX = cur.panX + pan.x,
                                                                        panY = cur.panY + pan.y,
                                                                        rotationDeg = cur.rotationDeg + rot
                                                                    )
                                                                } else cur
                                                            }
                                                        }
                                                    }
                                                }
                                        ) {
                                            AsyncImage(
                                                model = item.uri,
                                                contentDescription = "Tile ${idx + 1}",
                                                contentScale = ContentScale.Crop,
                                                colorFilter = ColorFilter.colorMatrix(composeColorMatrix(item)),
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .graphicsLayer {
                                                        scaleX = (if (item.flipHorizontal) -1f else 1f) * item.scale
                                                        scaleY = item.scale
                                                        translationX = item.panX
                                                        translationY = item.panY
                                                        rotationZ = item.rotationDeg
                                                    }
                                            )
                                            if (item.isVideo) {
                                                Surface(
                                                    color = Color(0xCC0B0D10),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(6.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Default.Videocam, null, tint = AccentGreen, modifier = Modifier.size(12.dp))
                                                        Spacer(Modifier.width(3.dp))
                                                        Text("VIDEO", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                if (state.showGuides) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                        if (state.mode == StudioMode.CAROUSEL && state.carouselSlides > 1) {
                            for (s in 1 until state.carouselSlides) {
                                val x = size.width * (s.toFloat() / state.carouselSlides)
                                drawLine(
                                    color = CyanGuide.copy(alpha = 0.85f),
                                    start = Offset(x, 0f),
                                    end = Offset(x, size.height),
                                    strokeWidth = 2f,
                                    pathEffect = dash
                                )
                            }
                        } else {
                            drawLine(
                                color = CyanGuide.copy(alpha = 0.35f),
                                start = Offset(size.width / 2f, 0f),
                                end = Offset(size.width / 2f, size.height),
                                strokeWidth = 1.5f,
                                pathEffect = dash
                            )
                            drawLine(
                                color = CyanGuide.copy(alpha = 0.35f),
                                start = Offset(0f, size.height / 2f),
                                end = Offset(size.width, size.height / 2f),
                                strokeWidth = 1.5f,
                                pathEffect = dash
                            )
                        }
                    }
                }

                state.overlays.forEach { overlay ->
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val maxW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                        val maxH = constraints.maxHeight.toFloat().coerceAtLeast(1f)
                        Surface(
                            color = if (overlay.usePill) Color(0xCC0B0D10) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            border = if (state.selectedOverlayId == overlay.id) BorderStroke(1.dp, AccentGreen) else null,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .graphicsLayer {
                                    translationX = (overlay.xFraction * maxW) - 60f
                                    translationY = (overlay.yFraction * maxH) - 24f
                                }
                                .pointerInput(overlay.id, state.canvasLocked) {
                                    if (!state.canvasLocked) {
                                        detectDragGestures { change, drag ->
                                            change.consume()
                                            state = state.copy(
                                                selectedOverlayId = overlay.id,
                                                overlays = state.overlays.map {
                                                    if (it.id == overlay.id) {
                                                        it.copy(
                                                            xFraction = (it.xFraction + drag.x / maxW).coerceIn(0.05f, 0.95f),
                                                            yFraction = (it.yFraction + drag.y / maxH).coerceIn(0.05f, 0.95f)
                                                        )
                                                    } else it
                                                }
                                            )
                                        }
                                    }
                                }
                        ) {
                            Text(
                                text = overlay.text,
                                color = Color(overlay.textColorHex),
                                fontSize = overlay.fontSizeSp.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Surface(
                color = BgPanel.copy(alpha = 0.95f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.mode == StudioMode.CAROUSEL) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                applyLayoutCapacity(state.mode, state.layout, state.carouselSlides + 1)
                            }
                        ) {
                            Icon(Icons.Default.Add, null, tint = AccentGreen, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Slide (${state.carouselSlides})", color = TextPrimary, fontSize = 11.sp)
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                            }
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, null, tint = AccentGreen, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Add Media", color = TextPrimary, fontSize = 11.sp)
                        }
                    }

                    Box(Modifier.width(1.dp).height(14.dp).background(BorderSubtle))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            state = state.copy(showGuides = !state.showGuides)
                        }
                    ) {
                        Icon(
                            if (state.showGuides) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            null,
                            tint = if (state.showGuides) CyanGuide else TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Guides", color = TextPrimary, fontSize = 11.sp)
                    }
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = BorderSubtle)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgPanel)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (state.activeTab) {
                StudioTab.LAYOUT -> {
                    if (state.mode == StudioMode.CAROUSEL) {
                        StepperRow(
                            label = "Carousel Slides",
                            valueText = "${state.carouselSlides} slides",
                            onMinus = { applyLayoutCapacity(state.mode, state.layout, state.carouselSlides - 1) },
                            onPlus = { applyLayoutCapacity(state.mode, state.layout, state.carouselSlides + 1) }
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(LayoutStyle.entries) { layout ->
                                val selected = state.layout == layout
                                FilterChip(
                                    selected = selected,
                                    onClick = { applyLayoutCapacity(state.mode, layout, state.carouselSlides) },
                                    label = { Text(layout.label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                    StepperRow(
                        label = "Spacing",
                        valueText = "${state.borderDp} px",
                        onMinus = { state = state.copy(borderDp = (state.borderDp - 2).coerceAtLeast(0)) },
                        onPlus = { state = state.copy(borderDp = (state.borderDp + 2).coerceAtMost(48)) }
                    )
                    StepperRow(
                        label = "Corner Radius",
                        valueText = "${state.cornerRadiusDp} px",
                        onMinus = { state = state.copy(cornerRadiusDp = (state.cornerRadiusDp - 2).coerceAtLeast(0)) },
                        onPlus = { state = state.copy(cornerRadiusDp = (state.cornerRadiusDp + 2).coerceAtMost(48)) }
                    )
                }

                StudioTab.MEDIA -> {
                    val selectedItem = state.slots.getOrNull(state.selectedSlotIndex)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                replacePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                            },
                            enabled = selectedItem != null,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.SwapHoriz, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Replace", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                updateSelectedMedia { it.copy(flipHorizontal = !it.flipHorizontal) }
                            },
                            enabled = selectedItem != null,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Flip, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Flip", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                updateSelectedMedia { it.copy(rotationDeg = (it.rotationDeg + 90f) % 360f) }
                            },
                            enabled = selectedItem != null,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.RotateRight, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("90°", fontSize = 12.sp)
                        }
                    }
                    StepperRow(
                        label = "Tile Zoom",
                        valueText = "${((selectedItem?.scale ?: 1f) * 100).roundToInt()}%",
                        onMinus = { updateSelectedMedia { it.copy(scale = (it.scale - 0.1f).coerceAtLeast(0.5f)) } },
                        onPlus = { updateSelectedMedia { it.copy(scale = (it.scale + 0.1f).coerceAtMost(4f)) } }
                    )
                }

                StudioTab.BG -> {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(BackgroundStyle.entries) { bg ->
                            val selected = state.backgroundStyle == bg
                            Surface(
                                onClick = { state = state.copy(backgroundStyle = bg) },
                                shape = RoundedCornerShape(10.dp),
                                color = Color(bg.hex),
                                border = BorderStroke(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) AccentGreen else BorderSubtle
                                )
                            ) {
                                Text(
                                    text = bg.label,
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                StudioTab.TEXT -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("English or മലയാളം text...", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (textInput.isNotBlank()) {
                                    val newOverlay = TextOverlay(
                                        id = UUID.randomUUID().toString(),
                                        text = textInput.trim()
                                    )
                                    state = state.copy(
                                        overlays = state.overlays + newOverlay,
                                        selectedOverlayId = newOverlay.id
                                    )
                                    textInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color(0xFF05130A))
                        ) {
                            Text("Add", fontWeight = FontWeight.Bold)
                        }
                    }
                    val selOverlay = state.overlays.find { it.id == state.selectedOverlayId } ?: state.overlays.lastOrNull()
                    if (selOverlay != null) {
                        StepperRow(
                            label = "Font Size",
                            valueText = "${selOverlay.fontSizeSp.roundToInt()} sp",
                            onMinus = {
                                state = state.copy(overlays = state.overlays.map {
                                    if (it.id == selOverlay.id) it.copy(fontSizeSp = (it.fontSizeSp - 2f).coerceAtLeast(12f)) else it
                                })
                            },
                            onPlus = {
                                state = state.copy(overlays = state.overlays.map {
                                    if (it.id == selOverlay.id) it.copy(fontSizeSp = (it.fontSizeSp + 2f).coerceAtMost(72f)) else it
                                })
                            }
                        )
                    }
                }

                StudioTab.STICKERS -> {
                    val badges = listOf("SWIPE →", "01 / 03", "KERALA", "SHOT ON MOBILE", "★", "✦")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(badges) { badge ->
                            SuggestionChip(
                                onClick = {
                                    val item = TextOverlay(
                                        id = UUID.randomUUID().toString(),
                                        text = badge,
                                        fontSizeSp = 15f,
                                        usePill = true,
                                        isSticker = true
                                    )
                                    state = state.copy(
                                        overlays = state.overlays + item,
                                        selectedOverlayId = item.id
                                    )
                                },
                                label = { Text(badge, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                StudioTab.FILTERS -> {
                    val sel = state.slots.getOrNull(state.selectedSlotIndex)
                    StepperRow(
                        label = "Brightness",
                        valueText = "${((sel?.brightness ?: 0f) * 100).roundToInt()}",
                        onMinus = { updateSelectedMedia { it.copy(brightness = (it.brightness - 0.05f).coerceAtLeast(-0.5f)) } },
                        onPlus = { updateSelectedMedia { it.copy(brightness = (it.brightness + 0.05f).coerceAtMost(0.5f)) } }
                    )
                    StepperRow(
                        label = "Contrast",
                        valueText = "${((sel?.contrast ?: 1f) * 100).roundToInt()}%",
                        onMinus = { updateSelectedMedia { it.copy(contrast = (it.contrast - 0.1f).coerceAtLeast(0.5f)) } },
                        onPlus = { updateSelectedMedia { it.copy(contrast = (it.contrast + 0.1f).coerceAtMost(2.0f)) } }
                    )
                    StepperRow(
                        label = "Saturation",
                        valueText = "${((sel?.saturation ?: 1f) * 100).roundToInt()}%",
                        onMinus = { updateSelectedMedia { it.copy(saturation = (it.saturation - 0.1f).coerceAtLeast(0f)) } },
                        onPlus = { updateSelectedMedia { it.copy(saturation = (it.saturation + 0.1f).coerceAtMost(2.0f)) } }
                    )
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = BorderSubtle)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .background(BgPanel)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StudioTab.entries.forEach { tab ->
                val selected = state.activeTab == tab
                val icon: ImageVector = when (tab) {
                    StudioTab.LAYOUT -> Icons.Default.GridView
                    StudioTab.MEDIA -> Icons.Default.PhotoLibrary
                    StudioTab.BG -> Icons.Default.Palette
                    StudioTab.TEXT -> Icons.Default.Title
                    StudioTab.STICKERS -> Icons.Default.AutoAwesome
                    StudioTab.FILTERS -> Icons.Default.Tune
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { state = state.copy(activeTab = tab) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.label,
                        tint = if (selected) AccentGreen else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = tab.label,
                        color = if (selected) AccentGreen else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }

    if (showExportSheet) {
        ModalBottomSheet(
            onDismissRequest = { showExportSheet = false },
            containerColor = BgPanel,
            contentColor = TextPrimary
        ) {
            ExportOptionsSheetContent(
                state = state,
                lastResult = lastExportResult,
                onConfigChange = { newCfg ->
                    state = state.copy(exportConfig = newCfg)
                },
                onExportGallery = {
                    val res = CollageExporter.exportStudio(context, state, asZip = false)
                    lastExportResult = res
                    statusBanner = res?.summary ?: "Export failed"
                },
                onExportZip = {
                    val res = CollageExporter.exportStudio(context, state, asZip = true)
                    lastExportResult = res
                    statusBanner = res?.summary ?: "ZIP export failed"
                },
                onShare = {
                    val res = lastExportResult ?: CollageExporter.exportStudio(context, state, asZip = false).also {
                        lastExportResult = it
                    }
                    if (res != null) {
                        CollageExporter.shareUris(context, res.uris, state.exportConfig.format.mimeType)
                    }
                }
            )
        }
    }
}

@Composable
private fun StepperRow(
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = onMinus,
                shape = CircleShape,
                color = BgCanvas,
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Icon(Icons.Default.Remove, "Decrease $label", tint = TextPrimary, modifier = Modifier.padding(4.dp).size(16.dp))
            }
            Text(
                text = valueText,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.widthIn(min = 64.dp)
            )
            Surface(
                onClick = onPlus,
                shape = CircleShape,
                color = BgCanvas,
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Icon(Icons.Default.Add, "Increase $label", tint = TextPrimary, modifier = Modifier.padding(4.dp).size(16.dp))
            }
        }
    }
}

@Composable
private fun ExportOptionsSheetContent(
    state: StudioState,
    lastResult: ExportResult?,
    onConfigChange: (ExportConfig) -> Unit,
    onExportGallery: () -> Unit,
    onExportZip: () -> Unit,
    onShare: () -> Unit
) {
    val cfg = state.exportConfig
    val w = cfg.outputWidthPx()
    val h = cfg.outputHeightPx()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Photo Export Options", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Surface(
                color = Color(0xFF1A2E22),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, AccentGreen)
            ) {
                Text(
                    text = "${w} × ${h} px • sRGB",
                    color = AccentGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Text("Export Mode", color = TextSecondary, fontSize = 12.sp)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ExportMode.entries.forEach { mode ->
                FilterChip(
                    selected = cfg.mode == mode,
                    onClick = { onConfigChange(cfg.copy(mode = mode)) },
                    label = { Text(mode.label, fontSize = 11.sp) }
                )
            }
        }

        if (state.mode == StudioMode.CAROUSEL && cfg.mode == ExportMode.AUTO_SLICE) {
            Text("Slide Save Order", color = TextSecondary, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SlideOrder.entries.forEach { order ->
                    FilterChip(
                        selected = cfg.slideOrder == order,
                        onClick = { onConfigChange(cfg.copy(slideOrder = order)) },
                        label = { Text(order.label, fontSize = 11.sp) }
                    )
                }
            }
        }

        Text("Resolution", color = TextSecondary, fontSize = 12.sp)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ResolutionPreset.entries.forEach { res ->
                FilterChip(
                    selected = cfg.resolution == res,
                    onClick = { onConfigChange(cfg.copy(resolution = res)) },
                    label = { Text(res.label, fontSize = 11.sp) }
                )
            }
        }

        Text("Aspect Ratio", color = TextSecondary, fontSize = 12.sp)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AspectRatioOption.entries.forEach { ratio ->
                FilterChip(
                    selected = cfg.aspectRatio == ratio,
                    onClick = { onConfigChange(cfg.copy(aspectRatio = ratio)) },
                    label = { Text(ratio.label, fontSize = 11.sp) }
                )
            }
        }

        Text("File Format", color = TextSecondary, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ExportFormat.entries.forEach { fmt ->
                FilterChip(
                    selected = cfg.format == fmt,
                    onClick = { onConfigChange(cfg.copy(format = fmt)) },
                    label = { Text(fmt.label, fontSize = 11.sp) }
                )
            }
        }

        if (cfg.format != ExportFormat.PNG) {
            StepperRow(
                label = "Compression Quality",
                valueText = "${cfg.qualityPercent}%",
                onMinus = { onConfigChange(cfg.copy(qualityPercent = (cfg.qualityPercent - 5).coerceAtLeast(80))) },
                onPlus = { onConfigChange(cfg.copy(qualityPercent = (cfg.qualityPercent + 5).coerceAtMost(100))) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Strip EXIF / GPS Metadata", color = TextPrimary, fontSize = 12.sp)
            Switch(
                checked = cfg.stripExif,
                onCheckedChange = { onConfigChange(cfg.copy(stripExif = it)) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Custom Handle Stamp (${cfg.signatureText})", color = TextPrimary, fontSize = 12.sp)
            Switch(
                checked = cfg.includeSignature,
                onCheckedChange = { onConfigChange(cfg.copy(includeSignature = it)) }
            )
        }

        if (lastResult != null) {
            Surface(
                color = BgCanvas,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, AccentGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = lastResult.summary,
                    color = AccentGreen,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onExportGallery,
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color(0xFF05130A)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Save Gallery", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onExportZip,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.FolderZip, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Save ZIP", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = onShare,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Share, "Share", modifier = Modifier.size(16.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

private fun composeColorMatrix(item: MediaItem): ColorMatrix {
    val cm = ColorMatrix()
    val sat = ColorMatrix().apply { setToSaturation(item.saturation) }
    cm.timesAssign(sat)
    val c = item.contrast
    val b = item.brightness * 255f
    val t = (-0.5f * c + 0.5f) * 255f + b
    cm.timesAssign(
        ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
                0f, 0f, 0f, 1f, 0f
            )
        )
    )
    return cm
}
