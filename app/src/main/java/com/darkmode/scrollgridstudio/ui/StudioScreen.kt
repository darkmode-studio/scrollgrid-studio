package com.darkmode.scrollgridstudio.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.darkmode.scrollgridstudio.data.DraftStorage
import com.darkmode.scrollgridstudio.export.CollageExporter
import com.darkmode.scrollgridstudio.export.ExportResult
import com.darkmode.scrollgridstudio.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.roundToInt

private val BgCanvas = Color(0xFF090B0E)
private val BgBar = Color(0xFF12151C)
private val BgTray = Color(0xFF151922)
private val BorderSubtle = Color(0xFF232936)
private val AccentGreen = Color(0xFF22C55E)
private val CyanGuide = Color(0xFF38BDF8)
private val DangerRed = Color(0xFFEF4444)
private val TextPrimary = Color(0xFFF3F4F6)
private val TextSecondary = Color(0xFF8B95A5)

private val TextSwatches = listOf(0xFFF3F4F6L, 0xFF0B0D10L, 0xFF22C55EL, 0xFFFACC15L, 0xFF38BDF8L, 0xFFF43F5EL, 0xFFF97316L)
private val BorderSwatches = listOf(null, 0xFFF3F4F6L, 0xFF0B0D10L, 0xFF22C55EL, 0xFFFACC15L, 0xFF38BDF8L)

private enum class LayoutAdjustMetric(val label: String) {
    SPACING("Spacing"),
    MARGIN("Margin"),
    RADIUS("Radius")
}

private enum class FilterAdjustMetric(val label: String) {
    BRIGHTNESS("Bright"),
    CONTRAST("Contrast"),
    SATURATION("Sat"),
    WARMTH("Warmth"),
    VIGNETTE("Vignette")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    initialState: StudioState = StudioState(),
    initialDraftId: String? = null,
    initialScrlTemplate: ScrlTemplate? = null,
    onBackHome: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var state by remember { mutableStateOf(initialState) }
    var undoStack by remember { mutableStateOf<List<StudioState>>(emptyList()) }
    var redoStack by remember { mutableStateOf<List<StudioState>>(emptyList()) }
    var activeDraftId by remember { mutableStateOf(initialDraftId) }

    // Collapsible native tool tray: null = full-screen unobstructed canvas!
    var expandedTab by remember { mutableStateOf<StudioTab?>(null) }
    var layoutMetric by remember { mutableStateOf(LayoutAdjustMetric.SPACING) }
    var filterMetric by remember { mutableStateOf(FilterAdjustMetric.BRIGHTNESS) }

    // Native modals (never cluttering the bottom dock)
    var showTextComposer by remember { mutableStateOf(false) }
    var editingOverlayId by remember { mutableStateOf<String?>(null) }
    var composerText by remember { mutableStateOf("") }
    var composerFont by remember { mutableStateOf(TextFontStyle.SANS_BOLD) }
    var composerColor by remember { mutableStateOf(0xFFF3F4F6L) }
    var composerPill by remember { mutableStateOf(true) }
    var composerAlign by remember { mutableStateOf(TextAlignOption.CENTER) }

    var showHexDialog by remember { mutableStateOf(false) }
    var hexInput by remember { mutableStateOf("#133326") }

    var showExportSheet by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var lastExportResult by remember { mutableStateOf<ExportResult?>(null) }

    // Sleek auto-fading toast pill
    var toastMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2000)
            toastMessage = null
        }
    }

    fun showToast(msg: String) {
        toastMessage = msg
    }

    fun pushState(next: StudioState, recordHistory: Boolean = true) {
        if (recordHistory && next != state) {
            undoStack = (undoStack + state).takeLast(25)
            redoStack = emptyList()
        }
        state = next
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            val prev = undoStack.last()
            redoStack = redoStack + state
            undoStack = undoStack.dropLast(1)
            state = prev
            showToast("Undo")
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            val next = redoStack.last()
            undoStack = undoStack + state
            redoStack = redoStack.dropLast(1)
            state = next
            showToast("Redo")
        }
    }

    fun openTextComposer(existing: TextOverlay? = null) {
        if (existing != null) {
            editingOverlayId = existing.id
            composerText = existing.text
            composerFont = existing.fontStyle
            composerColor = existing.textColorHex
            composerPill = existing.usePill
            composerAlign = existing.alignment
        } else {
            editingOverlayId = null
            composerText = ""
            composerFont = TextFontStyle.SANS_BOLD
            composerColor = 0xFFF3F4F6L
            composerPill = true
            composerAlign = TextAlignOption.CENTER
        }
        showTextComposer = true
    }

    fun applyScrlTemplate(template: ScrlTemplate) {
        val slides = template.slides
        val pool = state.slots + state.extras
        val cap = slides * 2
        val arranged = pool.take(cap).mapIndexed { idx, item ->
            when (template) {
                ScrlTemplate.OVERLAP_3 -> item.copy(
                    freeX = (0.16f + idx * 0.14f).coerceIn(0.1f, 0.9f),
                    freeY = if (idx % 2 == 0) 0.42f else 0.58f,
                    freeW = 0.28f,
                    freeH = 0.56f,
                    rotationDeg = if (idx % 2 == 0) -2f else 2f,
                    shadowDp = 12,
                    zIndex = idx
                )
                ScrlTemplate.MAGAZINE_4 -> item.copy(
                    freeX = (0.12f + idx * 0.11f).coerceIn(0.08f, 0.92f),
                    freeY = if (idx % 2 == 0) 0.36f else 0.64f,
                    freeW = 0.22f,
                    freeH = 0.48f,
                    rotationDeg = 0f,
                    shadowDp = 10,
                    zIndex = idx
                )
                ScrlTemplate.PANORAMA_5 -> item.copy(
                    freeX = (0.10f + idx * 0.09f).coerceIn(0.06f, 0.94f),
                    freeY = 0.5f,
                    freeW = 0.18f,
                    freeH = 0.72f,
                    rotationDeg = 0f,
                    shadowDp = 8,
                    zIndex = idx
                )
            }
        }
        pushState(
            state.copy(
                mode = StudioMode.CAROUSEL,
                carouselSlides = slides,
                slots = arranged,
                extras = pool.drop(cap),
                selectedSlotIndex = 0
            )
        )
        showToast("${template.label} template")
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(24)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val existingCount = state.slots.size + state.extras.size
            val added = uris.mapIndexed { i, uri ->
                val mime = context.contentResolver.getType(uri) ?: ""
                val idx = existingCount + i
                MediaItem(
                    id = UUID.randomUUID().toString(),
                    uri = uri,
                    isVideo = mime.startsWith("video/"),
                    freeX = (0.18f + (idx % 5) * 0.16f).coerceIn(0.12f, 0.88f),
                    freeY = (0.28f + (idx % 3) * 0.2f).coerceIn(0.18f, 0.82f),
                    zIndex = idx
                )
            }
            val pool = state.slots + state.extras + added
            val cap = state.maxCapacity()
            pushState(
                state.copy(
                    slots = pool.take(cap),
                    extras = pool.drop(cap),
                    selectedSlotIndex = 0,
                    swapSourceIndex = null
                )
            )
            if (initialScrlTemplate != null && existingCount == 0) {
                applyScrlTemplate(initialScrlTemplate)
            } else {
                showToast("Added ${added.size} media")
            }
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
            pushState(state.copy(slots = updated))
            showToast("Tile replaced")
        }
    }

    val stickerImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val newSticker = TextOverlay(
                id = UUID.randomUUID().toString(),
                text = "",
                isSticker = true,
                stickerUri = uri,
                usePill = false
            )
            pushState(state.copy(overlays = state.overlays + newSticker, selectedOverlayId = newSticker.id))
            showToast("PNG sticker added")
        }
    }

    fun applyLayoutCapacity(nextMode: StudioMode, nextLayout: LayoutStyle, nextSlides: Int) {
        val clampedSlides = nextSlides.coerceIn(1, 20)
        val temp = state.copy(mode = nextMode, layout = nextLayout, carouselSlides = clampedSlides, swapSourceIndex = null)
        val cap = temp.maxCapacity()
        val pool = temp.slots + temp.extras
        pushState(
            temp.copy(
                slots = pool.take(cap),
                extras = pool.drop(cap),
                selectedSlotIndex = 0,
                activeSlideIndex = temp.activeSlideIndex.coerceIn(0, (clampedSlides - 1).coerceAtLeast(0))
            )
        )
    }

    fun updateSelectedMedia(recordHistory: Boolean = true, transform: (MediaItem) -> MediaItem) {
        if (state.slots.isEmpty()) return
        val idx = state.selectedSlotIndex.coerceIn(0, state.slots.lastIndex)
        val updated = state.slots.toMutableList().apply { this[idx] = transform(this[idx]) }
        pushState(state.copy(slots = updated), recordHistory = recordHistory)
    }

    fun duplicateSelectedMedia() {
        val sel = state.slots.getOrNull(state.selectedSlotIndex) ?: return
        val copy = sel.copy(
            id = UUID.randomUUID().toString(),
            freeX = (sel.freeX + 0.05f).coerceAtMost(0.92f),
            freeY = (sel.freeY + 0.05f).coerceAtMost(0.92f),
            zIndex = (state.slots.maxOfOrNull { it.zIndex } ?: 0) + 1
        )
        pushState(state.copy(slots = state.slots + copy, selectedSlotIndex = state.slots.size))
        showToast("Duplicated layer")
    }

    fun deleteSelectedMedia() {
        if (state.slots.isEmpty()) return
        val idx = state.selectedSlotIndex.coerceIn(0, state.slots.lastIndex)
        val remainingSlots = state.slots.toMutableList().apply { removeAt(idx) }
        val remainingExtras = state.extras.toMutableList()
        if (remainingSlots.size < state.maxCapacity() && remainingExtras.isNotEmpty()) {
            remainingSlots.add(remainingExtras.removeAt(0))
        }
        pushState(
            state.copy(
                slots = remainingSlots,
                extras = remainingExtras,
                selectedSlotIndex = idx.coerceAtMost((remainingSlots.size - 1).coerceAtLeast(0)),
                swapSourceIndex = null
            )
        )
        showToast("Removed tile")
    }

    fun handleTileTap(idx: Int) {
        val swapSrc = state.swapSourceIndex
        if (swapSrc != null && swapSrc != idx && swapSrc in state.slots.indices && idx in state.slots.indices) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            val swapped = state.slots.toMutableList().apply {
                val tmp = this[swapSrc]
                this[swapSrc] = this[idx]
                this[idx] = tmp
            }
            pushState(state.copy(slots = swapped, selectedSlotIndex = idx, swapSourceIndex = null))
            showToast("Tiles swapped")
        } else {
            state = state.copy(selectedSlotIndex = idx, swapSourceIndex = null)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Minimal Native Top Navigation Bar (52dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(BgBar)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackHome, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary, modifier = Modifier.size(21.dp))
                    }
                    IconButton(onClick = { undo() }, enabled = undoStack.isNotEmpty(), modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Undo, "Undo", tint = if (undoStack.isNotEmpty()) TextPrimary else Color(0xFF3C4352), modifier = Modifier.size(19.dp))
                    }
                    IconButton(onClick = { redo() }, enabled = redoStack.isNotEmpty(), modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Redo, "Redo", tint = if (redoStack.isNotEmpty()) TextPrimary else Color(0xFF3C4352), modifier = Modifier.size(19.dp))
                    }
                }

                // Compact Center Mode Indicator Pill (tap to cycle mode)
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val modes = StudioMode.entries
                        val nextMode = modes[(modes.indexOf(state.mode) + 1) % modes.size]
                        applyLayoutCapacity(nextMode, state.layout, state.carouselSlides)
                        showToast("${nextMode.title} Mode")
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1B202C),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(state.mode.title, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Default.UnfoldMore, null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val locked = !state.canvasLocked
                            state = state.copy(canvasLocked = locked)
                            showToast(if (locked) "Canvas Locked" else "Canvas Unlocked")
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (state.canvasLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Lock",
                            tint = if (state.canvasLocked) AccentGreen else TextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            activeDraftId = DraftStorage.saveDraft(context, state, activeDraftId)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showToast("Draft Saved")
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.BookmarkBorder, "Save", tint = TextPrimary, modifier = Modifier.size(19.dp))
                    }
                    Surface(
                        onClick = { showExportSheet = true },
                        shape = RoundedCornerShape(20.dp),
                        color = AccentGreen
                    ) {
                        Text(
                            text = "Export",
                            color = Color(0xFF04140A),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // 2. Massive Full-Bleed Canvas Viewport (75%+ screen when tray collapsed)
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        expandedTab = null
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                val density = LocalDensity.current
                val ratioW = state.exportConfig.outputWidthPx().toFloat()
                val ratioH = state.exportConfig.outputHeightPx().toFloat()
                val slideAspect = (ratioW / ratioH).coerceIn(0.45f, 2.0f)

                val slideHeightDp = (maxHeight - 12.dp).coerceAtLeast(200.dp)
                val singleSlideWidthDp = (slideHeightDp * slideAspect).coerceAtMost(maxWidth)
                val totalCanvasWidthDp = if (state.mode == StudioMode.CAROUSEL) {
                    singleSlideWidthDp * state.carouselSlides
                } else {
                    singleSlideWidthDp
                }

                val carouselScroll = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (state.mode == StudioMode.CAROUSEL) Modifier.horizontalScroll(carouselScroll) else Modifier),
                    contentAlignment = if (state.mode == StudioMode.CAROUSEL) Alignment.CenterStart else Alignment.Center
                ) {
                    val bg = state.backgroundStyle
                    val bgModifier = when {
                        state.customBgHex != null -> Modifier.background(Color(state.customBgHex!!))
                        bg.startHex != bg.endHex && !bg.isBlurPhoto -> Modifier.background(Brush.linearGradient(listOf(Color(bg.startHex), Color(bg.endHex))))
                        else -> Modifier.background(Color(bg.startHex))
                    }

                    Box(
                        modifier = Modifier
                            .width(totalCanvasWidthDp)
                            .height(slideHeightDp)
                            .clip(RoundedCornerShape(12.dp))
                            .then(bgModifier)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                    ) {
                        if (state.customBgHex == null && bg.isBlurPhoto && state.slots.isNotEmpty()) {
                            AsyncImage(
                                model = state.slots.first().uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().blur(18.dp)
                            )
                            Box(Modifier.fillMaxSize().background(Color(0x660B0D10)))
                        }

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
                                Surface(
                                    shape = CircleShape,
                                    color = AccentGreen.copy(alpha = 0.14f),
                                    modifier = Modifier.size(62.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.AddPhotoAlternate, null, tint = AccentGreen, modifier = Modifier.size(28.dp))
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Text("Tap to Add Photos", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text("${state.mode.title} • ${state.exportConfig.aspectRatio.label}", color = TextSecondary, fontSize = 12.sp)
                            }
                        } else if (state.mode == StudioMode.GRID) {
                            BoxWithConstraints(
                                modifier = Modifier.fillMaxSize().padding(state.outerMarginDp.dp)
                            ) {
                                val cw = maxWidth
                                val ch = maxHeight
                                val gap = state.borderDp.dp
                                val rects = state.layout.slotRects()
                                rects.forEachIndexed { idx, slotRect ->
                                    val item = state.slots.getOrNull(idx)
                                    val cellW = (cw * slotRect.width - gap).coerceAtLeast(12.dp)
                                    val cellH = (ch * slotRect.height - gap).coerceAtLeast(12.dp)
                                    val offX = cw * slotRect.left + gap / 2
                                    val offY = ch * slotRect.top + gap / 2

                                    if (item != null) {
                                        val isSelected = idx == state.selectedSlotIndex
                                        val isSwapSrc = idx == state.swapSourceIndex
                                        val mask = if (item.maskShape != TileMaskShape.RECTANGLE) item.maskShape else slotRect.maskShape
                                        val tileShape = ShapePaths.composeShape(mask, state.cornerRadiusDp.dp)
                                        val borderColor = when {
                                            isSwapSrc -> CyanGuide
                                            isSelected -> AccentGreen
                                            state.borderColorHex != null -> Color(state.borderColorHex!!)
                                            else -> Color.Transparent
                                        }
                                        val borderW = if (isSelected || isSwapSrc || state.borderColorHex != null) 2.dp else 0.dp

                                        Box(
                                            modifier = Modifier
                                                .offset(x = offX, y = offY)
                                                .size(width = cellW, height = cellH)
                                                .clip(tileShape)
                                                .background(if (slotRect.polaroidBottomPad > 0f) Color(0xFFF3F4F6) else Color(0xFF1C232E))
                                                .border(borderW, borderColor, tileShape)
                                                .clickable { handleTileTap(idx) }
                                                .pointerInput(item.id, state.canvasLocked) {
                                                    if (!state.canvasLocked) {
                                                        detectTransformGestures { _, pan, zoom, rot ->
                                                            updateSelectedMedia(recordHistory = false) { cur ->
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
                                            val padMod = if (slotRect.polaroidBottomPad > 0f) {
                                                Modifier.padding(start = 6.dp, top = 6.dp, end = 6.dp, bottom = (cellH * slotRect.polaroidBottomPad))
                                            } else Modifier
                                            MediaTileImage(item = item, modifier = Modifier.fillMaxSize().then(padMod))
                                        }
                                    }
                                }
                            }
                        } else {
                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                val totalWpx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
                                val totalHpx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
                                val indexedSorted = state.slots.mapIndexed { i, item -> i to item }.sortedBy { it.second.zIndex }

                                indexedSorted.forEach { (origIdx, item) ->
                                    val isSelected = origIdx == state.selectedSlotIndex
                                    val itemW = maxWidth * item.freeW
                                    val itemH = maxHeight * item.freeH
                                    val leftDp = maxWidth * item.freeX - itemW / 2
                                    val topDp = maxHeight * item.freeY - itemH / 2
                                    val tileShape = ShapePaths.composeShape(item.maskShape, state.cornerRadiusDp.dp)
                                    val borderColor = when {
                                        isSelected -> AccentGreen
                                        state.borderColorHex != null -> Color(state.borderColorHex!!)
                                        else -> Color.Transparent
                                    }

                                    Box(
                                        modifier = Modifier
                                            .offset(x = leftDp, y = topDp)
                                            .size(width = itemW, height = itemH)
                                            .graphicsLayer {
                                                rotationZ = item.rotationDeg
                                                alpha = item.opacity
                                            }
                                            .then(if (item.shadowDp > 0) Modifier.shadow(item.shadowDp.dp, tileShape) else Modifier)
                                            .clip(tileShape)
                                            .background(Color(0xFF1C232E))
                                            .border(if (isSelected || state.borderColorHex != null) 2.dp else 0.dp, borderColor, tileShape)
                                            .clickable { state = state.copy(selectedSlotIndex = origIdx) }
                                            .pointerInput(item.id, state.canvasLocked, item.locked) {
                                                if (!state.canvasLocked && !item.locked) {
                                                    detectTransformGestures { _, pan, zoom, rot ->
                                                        state = state.copy(
                                                            selectedSlotIndex = origIdx,
                                                            slots = state.slots.mapIndexed { i, cur ->
                                                                if (i == origIdx) {
                                                                    cur.copy(
                                                                        freeX = (cur.freeX + pan.x / totalWpx).coerceIn(0.02f, 0.98f),
                                                                        freeY = (cur.freeY + pan.y / totalHpx).coerceIn(0.02f, 0.98f),
                                                                        freeW = (cur.freeW * zoom).coerceIn(0.08f, 1.2f),
                                                                        freeH = (cur.freeH * zoom).coerceIn(0.08f, 1.2f),
                                                                        rotationDeg = cur.rotationDeg + rot
                                                                    )
                                                                } else cur
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                    ) {
                                        MediaTileImage(item = item, modifier = Modifier.fillMaxSize())
                                        if (item.locked) {
                                            Icon(Icons.Default.Lock, null, tint = AccentGreen, modifier = Modifier.align(Alignment.TopStart).padding(4.dp).size(14.dp))
                                        }
                                    }
                                }
                            }
                        }

                        if (state.canvasTexture != CanvasTexture.NONE) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                when (state.canvasTexture) {
                                    CanvasTexture.FILM_GRAIN -> drawRect(Color(0x14FFFFFF))
                                    CanvasTexture.PAPER -> drawRect(Color(0x18F5E8CD))
                                    CanvasTexture.TORN_EDGE -> drawRect(
                                        color = Color(0xCCF3F4F6),
                                        style = Stroke(width = 6f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 6f), 0f))
                                    )
                                    CanvasTexture.NONE -> Unit
                                }
                            }
                        }

                        if (state.showGuides) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                                if (state.mode == StudioMode.CAROUSEL && state.carouselSlides > 1) {
                                    for (s in 1 until state.carouselSlides) {
                                        val x = size.width * (s.toFloat() / state.carouselSlides)
                                        drawLine(CyanGuide.copy(alpha = 0.9f), Offset(x, 0f), Offset(x, size.height), 2.5f, pathEffect = dash)
                                    }
                                } else {
                                    drawLine(CyanGuide.copy(alpha = 0.3f), Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), 1.5f, pathEffect = dash)
                                    drawLine(CyanGuide.copy(alpha = 0.3f), Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), 1.5f, pathEffect = dash)
                                }
                            }
                            if (state.mode == StudioMode.CAROUSEL) {
                                Row(modifier = Modifier.fillMaxWidth().align(Alignment.TopStart)) {
                                    for (s in 1..state.carouselSlides) {
                                        Box(modifier = Modifier.weight(1f).padding(6.dp)) {
                                            Surface(color = Color(0xCC0B0D10), shape = RoundedCornerShape(6.dp)) {
                                                Text("SLIDE $s", color = CyanGuide, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Draggable Text & PNG Sticker Overlays
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val maxW = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
                            val maxH = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
                            state.overlays.forEach { overlay ->
                                val isSel = state.selectedOverlayId == overlay.id
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .graphicsLayer {
                                            translationX = (overlay.xFraction * maxW) - 50f
                                            translationY = (overlay.yFraction * maxH) - 24f
                                            rotationZ = overlay.rotationDeg
                                            alpha = overlay.opacity
                                        }
                                        .clickable {
                                            state = state.copy(selectedOverlayId = overlay.id)
                                            if (!overlay.isSticker) {
                                                openTextComposer(overlay)
                                            } else {
                                                expandedTab = StudioTab.STICKERS
                                            }
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
                                                                    xFraction = (it.xFraction + drag.x / maxW).coerceIn(0.04f, 0.96f),
                                                                    yFraction = (it.yFraction + drag.y / maxH).coerceIn(0.04f, 0.96f)
                                                                )
                                                            } else it
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                ) {
                                    if (overlay.stickerUri != null) {
                                        AsyncImage(
                                            model = overlay.stickerUri,
                                            contentDescription = "Sticker",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .size((slideHeightDp * overlay.stickerSizeFraction).coerceIn(48.dp, 180.dp))
                                                .border(if (isSel) 1.5.dp else 0.dp, if (isSel) AccentGreen else Color.Transparent, RoundedCornerShape(6.dp))
                                        )
                                    } else {
                                        val ff = when (overlay.fontStyle) {
                                            TextFontStyle.SANS_BOLD -> FontFamily.SansSerif
                                            TextFontStyle.SERIF, TextFontStyle.ITALIC -> FontFamily.Serif
                                            TextFontStyle.MONO -> FontFamily.Monospace
                                        }
                                        val fs = if (overlay.fontStyle == TextFontStyle.ITALIC) FontStyle.Italic else FontStyle.Normal
                                        val ta = when (overlay.alignment) {
                                            TextAlignOption.LEFT -> TextAlign.Start
                                            TextAlignOption.CENTER -> TextAlign.Center
                                            TextAlignOption.RIGHT -> TextAlign.End
                                        }
                                        Surface(
                                            color = if (overlay.usePill) Color(0xCC0B0D10) else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp),
                                            border = if (isSel) BorderStroke(1.dp, AccentGreen) else null
                                        ) {
                                            Text(
                                                text = overlay.text,
                                                color = Color(overlay.textColorHex),
                                                fontSize = overlay.fontSizeSp.sp,
                                                fontFamily = ff,
                                                fontStyle = fs,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = ta,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Minimal Floating Corner Controls (Bottom-Left Guides, Bottom-Right Quick Add)
                Surface(
                    onClick = { state = state.copy(showGuides = !state.showGuides) },
                    shape = CircleShape,
                    color = BgBar.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.align(Alignment.BottomStart).size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (state.showGuides) Icons.Default.GridOn else Icons.Default.GridOff,
                            contentDescription = "Guides",
                            tint = if (state.showGuides) CyanGuide else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.mode == StudioMode.CAROUSEL) {
                        Surface(
                            onClick = { applyLayoutCapacity(state.mode, state.layout, state.carouselSlides + 1) },
                            shape = RoundedCornerShape(20.dp),
                            color = BgBar.copy(alpha = 0.92f),
                            border = BorderStroke(1.dp, CyanGuide.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.Add, null, tint = CyanGuide, modifier = Modifier.size(15.dp))
                                Text("Slide", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Surface(
                        onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                        shape = CircleShape,
                        color = AccentGreen,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, "Add Media", tint = Color(0xFF04140A), modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }

            // Visible Extras Filmstrip (only when overflow photos exist)
            if (state.extras.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(BgBar).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("EXTRAS", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemsIndexed(state.extras) { extraIdx, extraItem ->
                            AsyncImage(
                                model = extraItem.uri,
                                contentDescription = "Extra",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (state.slots.isNotEmpty()) {
                                            val targetIdx = state.selectedSlotIndex.coerceIn(0, state.slots.lastIndex)
                                            val newSlots = state.slots.toMutableList()
                                            val newExtras = state.extras.toMutableList()
                                            val displaced = newSlots[targetIdx]
                                            newSlots[targetIdx] = extraItem
                                            newExtras[extraIdx] = displaced
                                            pushState(state.copy(slots = newSlots, extras = newExtras))
                                            showToast("Swapped from Extras")
                                        }
                                    }
                            )
                        }
                    }
                }
            }

            // 3. Animated Collapsible Native Tool Tray (compact, icon-first, single slider)
            AnimatedVisibility(
                visible = expandedTab != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                val tab = expandedTab ?: StudioTab.LAYOUT
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgTray)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (tab) {
                        StudioTab.LAYOUT -> {
                            if (state.mode == StudioMode.CAROUSEL) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ScrlTemplate.entries.forEach { tpl ->
                                        NativePillChip(label = tpl.label, selected = false) { applyScrlTemplate(tpl) }
                                    }
                                    NativePillChip(label = "− Slide", selected = false) {
                                        applyLayoutCapacity(state.mode, state.layout, state.carouselSlides - 1)
                                    }
                                    NativePillChip(label = "+ Slide (${state.carouselSlides})", selected = true) {
                                        applyLayoutCapacity(state.mode, state.layout, state.carouselSlides + 1)
                                    }
                                }
                            } else {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(LayoutStyle.entries) { layout ->
                                        LayoutThumbnailChip(
                                            layout = layout,
                                            selected = state.layout == layout,
                                            onClick = { applyLayoutCapacity(state.mode, layout, state.carouselSlides) }
                                        )
                                    }
                                }
                            }

                            // Single compact metric switcher + native slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LayoutAdjustMetric.entries.forEach { m ->
                                    NativePillChip(label = m.label, selected = layoutMetric == m) { layoutMetric = m }
                                }
                                Spacer(Modifier.weight(1f))
                                NativePillChip(label = state.exportConfig.aspectRatio.label, selected = false) {
                                    val all = AspectRatioOption.entries
                                    val next = all[(all.indexOf(state.exportConfig.aspectRatio) + 1) % all.size]
                                    pushState(state.copy(exportConfig = state.exportConfig.copy(aspectRatio = next)))
                                }
                            }

                            when (layoutMetric) {
                                LayoutAdjustMetric.SPACING -> NativeSliderStepper(
                                    value = state.borderDp.toFloat(),
                                    range = 0f..48f,
                                    display = "${state.borderDp}",
                                    onChange = { pushState(state.copy(borderDp = it.roundToInt()), recordHistory = false) },
                                    onStep = { d -> pushState(state.copy(borderDp = (state.borderDp + d * 2).coerceIn(0, 48))) }
                                )
                                LayoutAdjustMetric.MARGIN -> NativeSliderStepper(
                                    value = state.outerMarginDp.toFloat(),
                                    range = 0f..48f,
                                    display = "${state.outerMarginDp}",
                                    onChange = { pushState(state.copy(outerMarginDp = it.roundToInt()), recordHistory = false) },
                                    onStep = { d -> pushState(state.copy(outerMarginDp = (state.outerMarginDp + d * 2).coerceIn(0, 48))) }
                                )
                                LayoutAdjustMetric.RADIUS -> NativeSliderStepper(
                                    value = state.cornerRadiusDp.toFloat(),
                                    range = 0f..48f,
                                    display = "${state.cornerRadiusDp}",
                                    onChange = { pushState(state.copy(cornerRadiusDp = it.roundToInt()), recordHistory = false) },
                                    onStep = { d -> pushState(state.copy(cornerRadiusDp = (state.cornerRadiusDp + d * 2).coerceIn(0, 48))) }
                                )
                            }
                        }

                        StudioTab.MEDIA -> {
                            val sel = state.slots.getOrNull(state.selectedSlotIndex)
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                NativeIconAction(Icons.Default.SwapHoriz, "Replace", enabled = sel != null) {
                                    replacePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                                }
                                NativeIconAction(Icons.Default.CompareArrows, "Swap", active = state.swapSourceIndex != null, enabled = state.slots.size > 1) {
                                    state = state.copy(swapSourceIndex = if (state.swapSourceIndex == null) state.selectedSlotIndex else null)
                                    if (state.swapSourceIndex != null) showToast("Tap another tile to swap")
                                }
                                NativeIconAction(Icons.Default.FitScreen, if (sel?.fitInside == true) "Fill" else "Fit", active = sel?.fitInside == true, enabled = sel != null) {
                                    updateSelectedMedia { it.copy(fitInside = !it.fitInside) }
                                }
                                NativeIconAction(Icons.Default.Flip, "Flip", enabled = sel != null) {
                                    updateSelectedMedia { it.copy(flipHorizontal = !it.flipHorizontal) }
                                }
                                NativeIconAction(Icons.Default.RotateRight, "Rotate", enabled = sel != null) {
                                    updateSelectedMedia { it.copy(rotationDeg = (it.rotationDeg + 90f) % 360f) }
                                }
                                if (state.mode != StudioMode.GRID && sel != null) {
                                    NativeIconAction(Icons.Default.ContentCopy, "Copy") { duplicateSelectedMedia() }
                                    NativeIconAction(Icons.Default.FlipToFront, "Front") {
                                        val maxZ = (state.slots.maxOfOrNull { it.zIndex } ?: 0) + 1
                                        updateSelectedMedia { it.copy(zIndex = maxZ) }
                                    }
                                    NativeIconAction(Icons.Default.FlipToBack, "Back") {
                                        val minZ = (state.slots.minOfOrNull { it.zIndex } ?: 0) - 1
                                        updateSelectedMedia { it.copy(zIndex = minZ) }
                                    }
                                    NativeIconAction(if (sel.locked) Icons.Default.Lock else Icons.Default.LockOpen, "Lock", active = sel.locked) {
                                        updateSelectedMedia { it.copy(locked = !it.locked) }
                                    }
                                }
                                NativeIconAction(Icons.Default.DeleteOutline, "Delete", tint = DangerRed, enabled = sel != null) {
                                    deleteSelectedMedia()
                                }
                            }

                            if (sel != null) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(TileMaskShape.entries) { mask ->
                                        NativePillChip(label = mask.label, selected = sel.maskShape == mask) {
                                            updateSelectedMedia { it.copy(maskShape = mask) }
                                        }
                                    }
                                }
                            }
                        }

                        StudioTab.BG -> {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                item {
                                    Surface(
                                        onClick = { showHexDialog = true },
                                        shape = CircleShape,
                                        color = BgBar,
                                        border = BorderStroke(1.5.dp, if (state.customBgHex != null) AccentGreen else BorderSubtle),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Colorize, "HEX", tint = AccentGreen, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                items(BackgroundStyle.entries) { bg ->
                                    val selected = state.customBgHex == null && state.backgroundStyle == bg
                                    val mod = if (bg.startHex != bg.endHex && !bg.isBlurPhoto) {
                                        Modifier.background(Brush.linearGradient(listOf(Color(bg.startHex), Color(bg.endHex))))
                                    } else {
                                        Modifier.background(Color(bg.startHex))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .then(mod)
                                            .border(if (selected) 2.5.dp else 1.dp, if (selected) AccentGreen else BorderSubtle, CircleShape)
                                            .clickable { pushState(state.copy(backgroundStyle = bg, customBgHex = null)) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (bg.isBlurPhoto) {
                                            Icon(Icons.Default.BlurOn, null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CanvasTexture.entries.forEach { tex ->
                                    NativePillChip(label = tex.label, selected = state.canvasTexture == tex) {
                                        pushState(state.copy(canvasTexture = tex))
                                    }
                                }
                                Box(Modifier.width(1.dp).height(16.dp).background(BorderSubtle))
                                BorderSwatches.forEach { hex ->
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(if (hex != null) Color(hex) else BgCanvas)
                                            .border(if (state.borderColorHex == hex) 2.dp else 1.dp, if (state.borderColorHex == hex) AccentGreen else BorderSubtle, CircleShape)
                                            .clickable { pushState(state.copy(borderColorHex = hex)) }
                                    )
                                }
                            }
                        }

                        StudioTab.TEXT -> {
                            val selOverlay = state.overlays.find { it.id == state.selectedOverlayId && !it.isSticker }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    onClick = { openTextComposer(null) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = AccentGreen
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Add, null, tint = Color(0xFF04140A), modifier = Modifier.size(16.dp))
                                        Text("Add Text", color = Color(0xFF04140A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (selOverlay != null) {
                                    NativePillChip(label = "Edit", selected = false) { openTextComposer(selOverlay) }
                                    NativePillChip(label = if (selOverlay.usePill) "Pill On" else "Pill Off", selected = selOverlay.usePill) {
                                        pushState(state.copy(overlays = state.overlays.map {
                                            if (it.id == selOverlay.id) it.copy(usePill = !it.usePill) else it
                                        }))
                                    }
                                    Spacer(Modifier.weight(1f))
                                    IconButton(
                                        onClick = {
                                            pushState(state.copy(overlays = state.overlays.filterNot { it.id == selOverlay.id }, selectedOverlayId = null))
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) { Icon(Icons.Default.DeleteOutline, "Delete", tint = DangerRed, modifier = Modifier.size(20.dp)) }
                                }
                            }

                            if (selOverlay != null) {
                                NativeSliderStepper(
                                    value = selOverlay.fontSizeSp,
                                    range = 12f..72f,
                                    display = "${selOverlay.fontSizeSp.roundToInt()}sp",
                                    onChange = { sz ->
                                        state = state.copy(overlays = state.overlays.map {
                                            if (it.id == selOverlay.id) it.copy(fontSizeSp = sz) else it
                                        })
                                    },
                                    onStep = { d ->
                                        pushState(state.copy(overlays = state.overlays.map {
                                            if (it.id == selOverlay.id) it.copy(fontSizeSp = (it.fontSizeSp + d * 2f).coerceIn(12f, 72f)) else it
                                        }))
                                    }
                                )
                            }
                        }

                        StudioTab.STICKERS -> {
                            val selSticker = state.overlays.find { it.id == state.selectedOverlayId && it.isSticker }
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    onClick = { stickerImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = AccentGreen
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.AddPhotoAlternate, null, tint = Color(0xFF04140A), modifier = Modifier.size(15.dp))
                                        Text("PNG", color = Color(0xFF04140A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                listOf("SWIPE →", "01 / 03", "KERALA", "✦", "★", "📷", "🎬", "🔥").forEach { badge ->
                                    NativePillChip(label = badge, selected = false) {
                                        val item = TextOverlay(
                                            id = UUID.randomUUID().toString(),
                                            text = badge,
                                            fontSizeSp = 16f,
                                            usePill = badge.length > 2,
                                            isSticker = true
                                        )
                                        pushState(state.copy(overlays = state.overlays + item, selectedOverlayId = item.id))
                                    }
                                }
                            }
                            if (selSticker != null) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(Modifier.weight(1f)) {
                                        NativeSliderStepper(
                                            value = selSticker.opacity,
                                            range = 0.2f..1f,
                                            display = "${(selSticker.opacity * 100).roundToInt()}%",
                                            onChange = { op ->
                                                state = state.copy(overlays = state.overlays.map {
                                                    if (it.id == selSticker.id) it.copy(opacity = op) else it
                                                })
                                            },
                                            onStep = { d ->
                                                pushState(state.copy(overlays = state.overlays.map {
                                                    if (it.id == selSticker.id) it.copy(opacity = (it.opacity + d * 0.1f).coerceIn(0.2f, 1f)) else it
                                                }))
                                            }
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            pushState(state.copy(overlays = state.overlays.filterNot { it.id == selSticker.id }, selectedOverlayId = null))
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) { Icon(Icons.Default.DeleteOutline, "Delete", tint = DangerRed, modifier = Modifier.size(20.dp)) }
                                }
                            }
                        }

                        StudioTab.FILTERS -> {
                            val sel = state.slots.getOrNull(state.selectedSlotIndex)
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterPreset.entries.forEach { preset ->
                                    NativePillChip(
                                        label = preset.label,
                                        selected = sel != null && sel.brightness == preset.brightness && sel.contrast == preset.contrast && sel.saturation == preset.saturation
                                    ) {
                                        updateSelectedMedia {
                                            it.copy(
                                                brightness = preset.brightness,
                                                contrast = preset.contrast,
                                                saturation = preset.saturation,
                                                warmth = preset.warmth,
                                                vignette = preset.vignette
                                            )
                                        }
                                    }
                                }
                                if (sel != null && state.slots.size > 1) {
                                    NativePillChip(label = "Apply All", selected = true) {
                                        pushState(state.copy(slots = state.slots.map {
                                            it.copy(
                                                brightness = sel.brightness,
                                                contrast = sel.contrast,
                                                saturation = sel.saturation,
                                                warmth = sel.warmth,
                                                vignette = sel.vignette
                                            )
                                        }))
                                        showToast("Applied to all tiles")
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterAdjustMetric.entries.forEach { m ->
                                    NativePillChip(label = m.label, selected = filterMetric == m) { filterMetric = m }
                                }
                            }

                            when (filterMetric) {
                                FilterAdjustMetric.BRIGHTNESS -> NativeSliderStepper(
                                    value = sel?.brightness ?: 0f,
                                    range = -0.5f..0.5f,
                                    display = "${((sel?.brightness ?: 0f) * 100).roundToInt()}",
                                    onChange = { v -> updateSelectedMedia(false) { it.copy(brightness = v) } },
                                    onStep = { d -> updateSelectedMedia { it.copy(brightness = (it.brightness + d * 0.05f).coerceIn(-0.5f, 0.5f)) } }
                                )
                                FilterAdjustMetric.CONTRAST -> NativeSliderStepper(
                                    value = sel?.contrast ?: 1f,
                                    range = 0.5f..2f,
                                    display = "${((sel?.contrast ?: 1f) * 100).roundToInt()}%",
                                    onChange = { v -> updateSelectedMedia(false) { it.copy(contrast = v) } },
                                    onStep = { d -> updateSelectedMedia { it.copy(contrast = (it.contrast + d * 0.1f).coerceIn(0.5f, 2f)) } }
                                )
                                FilterAdjustMetric.SATURATION -> NativeSliderStepper(
                                    value = sel?.saturation ?: 1f,
                                    range = 0f..2f,
                                    display = "${((sel?.saturation ?: 1f) * 100).roundToInt()}%",
                                    onChange = { v -> updateSelectedMedia(false) { it.copy(saturation = v) } },
                                    onStep = { d -> updateSelectedMedia { it.copy(saturation = (it.saturation + d * 0.1f).coerceIn(0f, 2f)) } }
                                )
                                FilterAdjustMetric.WARMTH -> NativeSliderStepper(
                                    value = sel?.warmth ?: 0f,
                                    range = -0.8f..0.8f,
                                    display = "${((sel?.warmth ?: 0f) * 100).roundToInt()}",
                                    onChange = { v -> updateSelectedMedia(false) { it.copy(warmth = v) } },
                                    onStep = { d -> updateSelectedMedia { it.copy(warmth = (it.warmth + d * 0.1f).coerceIn(-0.8f, 0.8f)) } }
                                )
                                FilterAdjustMetric.VIGNETTE -> NativeSliderStepper(
                                    value = sel?.vignette ?: 0f,
                                    range = 0f..0.8f,
                                    display = "${((sel?.vignette ?: 0f) * 100).roundToInt()}%",
                                    onChange = { v -> updateSelectedMedia(false) { it.copy(vignette = v) } },
                                    onStep = { d -> updateSelectedMedia { it.copy(vignette = (it.vignette + d * 0.1f).coerceIn(0f, 0.8f)) } }
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = BorderSubtle)

            // 4. Sleek Native Bottom Icon Dock (60dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(BgBar)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudioTab.entries.forEach { tab ->
                    val selected = expandedTab == tab
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
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                expandedTab = if (expandedTab == tab) null else tab
                            }
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) AccentGreen.copy(alpha = 0.16f) else Color.Transparent)
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Icon(icon, tab.label, tint = if (selected) AccentGreen else TextSecondary, modifier = Modifier.size(20.dp))
                        }
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

        // Floating Auto-Fading Toast Pill (Top Center below Bar)
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 62.dp)
        ) {
            Surface(
                color = Color(0xE6151922),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.5f))
            ) {
                Text(
                    text = toastMessage ?: "",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        // Native Instagram/SCRL-Style Full-Screen Text Composer Overlay
        if (showTextComposer) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xE6080A0D))
                    .imePadding()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showTextComposer = false }) {
                        Text("Cancel", color = TextSecondary, fontSize = 14.sp)
                    }
                    Surface(
                        onClick = {
                            val clean = composerText.trim()
                            if (clean.isNotEmpty()) {
                                val existingId = editingOverlayId
                                if (existingId != null) {
                                    pushState(state.copy(overlays = state.overlays.map {
                                        if (it.id == existingId) {
                                            it.copy(
                                                text = clean,
                                                fontStyle = composerFont,
                                                textColorHex = composerColor,
                                                usePill = composerPill,
                                                alignment = composerAlign
                                            )
                                        } else it
                                    }))
                                } else {
                                    val newOv = TextOverlay(
                                        id = UUID.randomUUID().toString(),
                                        text = clean,
                                        fontStyle = composerFont,
                                        textColorHex = composerColor,
                                        usePill = composerPill,
                                        alignment = composerAlign
                                    )
                                    pushState(state.copy(overlays = state.overlays + newOv, selectedOverlayId = newOv.id))
                                }
                            }
                            showTextComposer = false
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = AccentGreen
                    ) {
                        Text("Done", color = Color(0xFF04140A), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp))
                    }
                }

                // Centered Live Preview Input
                val ff = when (composerFont) {
                    TextFontStyle.SANS_BOLD -> FontFamily.SansSerif
                    TextFontStyle.SERIF, TextFontStyle.ITALIC -> FontFamily.Serif
                    TextFontStyle.MONO -> FontFamily.Monospace
                }
                val fs = if (composerFont == TextFontStyle.ITALIC) FontStyle.Italic else FontStyle.Normal
                val ta = when (composerAlign) {
                    TextAlignOption.LEFT -> TextAlign.Start
                    TextAlignOption.CENTER -> TextAlign.Center
                    TextAlignOption.RIGHT -> TextAlign.End
                }
                Surface(
                    color = if (composerPill) Color(0xCC0B0D10) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 16.dp)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                        if (composerText.isEmpty()) {
                            Text("Type English or മലയാളം...", color = TextSecondary, fontSize = 24.sp, fontFamily = ff)
                        }
                        BasicTextField(
                            value = composerText,
                            onValueChange = { composerText = it },
                            textStyle = TextStyle(
                                color = Color(composerColor),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ff,
                                fontStyle = fs,
                                textAlign = ta
                            ),
                            cursorBrush = SolidColor(AccentGreen),
                            modifier = Modifier.widthIn(min = 160.dp)
                        )
                    }
                }

                // Bottom Style Bar above Keyboard
                Column(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextFontStyle.entries.forEach { style ->
                            NativePillChip(label = style.label, selected = composerFont == style) { composerFont = style }
                        }
                        NativePillChip(label = if (composerPill) "Pill On" else "Pill Off", selected = composerPill) {
                            composerPill = !composerPill
                        }
                        NativePillChip(label = composerAlign.label, selected = false) {
                            val all = TextAlignOption.entries
                            composerAlign = all[(all.indexOf(composerAlign) + 1) % all.size]
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextSwatches.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(hex))
                                    .border(if (composerColor == hex) 2.5.dp else 1.dp, if (composerColor == hex) AccentGreen else BorderSubtle, CircleShape)
                                    .clickable { composerColor = hex }
                            )
                        }
                    }
                }
            }
        }
    }

    // Custom HEX Color Dialog
    if (showHexDialog) {
        AlertDialog(
            onDismissRequest = { showHexDialog = false },
            containerColor = BgTray,
            title = { Text("Custom Background HEX", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { hexInput = it },
                    placeholder = { Text("#133326") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = parseHexColor(hexInput)
                        if (parsed != null) {
                            pushState(state.copy(customBgHex = parsed))
                            showToast("Applied $hexInput")
                        }
                        showHexDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color(0xFF04140A))
                ) { Text("Apply", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showHexDialog = false }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }

    // Async Photo Export Bottom Sheet
    if (showExportSheet) {
        ModalBottomSheet(
            onDismissRequest = { if (!isExporting) showExportSheet = false },
            containerColor = BgTray,
            contentColor = TextPrimary
        ) {
            ExportOptionsSheetContent(
                state = state,
                isExporting = isExporting,
                lastResult = lastExportResult,
                onConfigChange = { newCfg -> state = state.copy(exportConfig = newCfg) },
                onExportGallery = {
                    scope.launch {
                        isExporting = true
                        val res = withContext(Dispatchers.IO) { CollageExporter.exportStudio(context, state, asZip = false) }
                        isExporting = false
                        lastExportResult = res
                        showToast(res?.summary ?: "Export failed")
                    }
                },
                onExportZip = {
                    scope.launch {
                        isExporting = true
                        val res = withContext(Dispatchers.IO) { CollageExporter.exportStudio(context, state, asZip = true) }
                        isExporting = false
                        lastExportResult = res
                        showToast(res?.summary ?: "ZIP export failed")
                    }
                },
                onShare = {
                    scope.launch {
                        val res = lastExportResult ?: run {
                            isExporting = true
                            val created = withContext(Dispatchers.IO) { CollageExporter.exportStudio(context, state, asZip = false) }
                            isExporting = false
                            lastExportResult = created
                            created
                        }
                        if (res != null) CollageExporter.shareUris(context, res.uris, state.exportConfig.format.mimeType)
                    }
                }
            )
        }
    }
}

@Composable
private fun NativePillChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) AccentGreen.copy(alpha = 0.16f) else BgBar,
        border = BorderStroke(1.dp, if (selected) AccentGreen else BorderSubtle)
    ) {
        Text(
            text = label,
            color = if (selected) AccentGreen else TextPrimary,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun NativeIconAction(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    enabled: Boolean = true,
    tint: Color = if (active) AccentGreen else TextPrimary,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = if (active) AccentGreen.copy(alpha = 0.16f) else BgBar,
            border = BorderStroke(1.dp, if (active) AccentGreen else BorderSubtle),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, label, tint = if (enabled) tint else Color(0xFF3E4554), modifier = Modifier.size(18.dp))
            }
        }
        Text(label, color = if (enabled) TextSecondary else Color(0xFF3E4554), fontSize = 10.sp)
    }
}

@Composable
private fun NativeSliderStepper(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: String,
    onChange: (Float) -> Unit,
    onStep: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(onClick = { onStep(-1) }, shape = CircleShape, color = BgBar, border = BorderStroke(1.dp, BorderSubtle), modifier = Modifier.size(28.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Remove, null, tint = TextPrimary, modifier = Modifier.size(15.dp)) }
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = AccentGreen, activeTrackColor = AccentGreen, inactiveTrackColor = BorderSubtle),
            modifier = Modifier.weight(1f).height(26.dp)
        )
        Surface(onClick = { onStep(1) }, shape = CircleShape, color = BgBar, border = BorderStroke(1.dp, BorderSubtle), modifier = Modifier.size(28.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, null, tint = TextPrimary, modifier = Modifier.size(15.dp)) }
        }
        Text(display, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.widthIn(min = 36.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun LayoutThumbnailChip(layout: LayoutStyle, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) AccentGreen.copy(alpha = 0.14f) else BgBar,
        border = BorderStroke(1.dp, if (selected) AccentGreen else BorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Canvas(modifier = Modifier.size(26.dp)) {
                layout.slotRects().forEach { r ->
                    drawRect(
                        color = if (selected) AccentGreen else TextSecondary,
                        topLeft = Offset(r.left * size.width + 1f, r.top * size.height + 1f),
                        size = Size((r.width * size.width - 2f).coerceAtLeast(2f), (r.height * size.height - 2f).coerceAtLeast(2f)),
                        style = Stroke(width = 1.4f)
                    )
                }
            }
            Text(layout.label, color = if (selected) TextPrimary else TextSecondary, fontSize = 9.sp)
        }
    }
}

@Composable
private fun MediaTileImage(item: MediaItem, modifier: Modifier) {
    Box(modifier = modifier) {
        AsyncImage(
            model = item.uri,
            contentDescription = "Media tile",
            contentScale = if (item.fitInside) ContentScale.Fit else ContentScale.Crop,
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
        if (item.vignette > 0.02f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = (item.vignette * 0.75f).coerceIn(0f, 0.75f)))))
            )
        }
        if (item.isVideo) {
            Surface(color = Color(0xCC0B0D10), shape = RoundedCornerShape(6.dp), modifier = Modifier.align(Alignment.TopEnd).padding(5.dp)) {
                Row(modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Videocam, null, tint = AccentGreen, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("VIDEO", color = TextPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ExportOptionsSheetContent(
    state: StudioState,
    isExporting: Boolean,
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
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Export Photo", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Surface(color = Color(0xFF14291E), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, AccentGreen)) {
                Text("${w} × ${h} px • sRGB", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp))
            }
        }

        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ExportMode.entries.forEach { mode ->
                NativePillChip(label = mode.label, selected = cfg.mode == mode) { onConfigChange(cfg.copy(mode = mode)) }
            }
        }

        if (state.mode == StudioMode.CAROUSEL && cfg.mode == ExportMode.AUTO_SLICE) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SlideOrder.entries.forEach { order ->
                    NativePillChip(label = order.label, selected = cfg.slideOrder == order) { onConfigChange(cfg.copy(slideOrder = order)) }
                }
            }
        }

        if (state.mode == StudioMode.CAROUSEL && cfg.mode == ExportMode.SELECTED_SLIDE) {
            NativeSliderStepper(
                value = cfg.selectedSlideNumber.toFloat(),
                range = 1f..state.carouselSlides.toFloat().coerceAtLeast(1f),
                display = "S${cfg.selectedSlideNumber}",
                onChange = { onConfigChange(cfg.copy(selectedSlideNumber = it.roundToInt().coerceIn(1, state.carouselSlides))) },
                onStep = { d -> onConfigChange(cfg.copy(selectedSlideNumber = (cfg.selectedSlideNumber + d).coerceIn(1, state.carouselSlides))) }
            )
        }

        Text("Size & Print", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AspectRatioOption.entries.forEach { ratio ->
                NativePillChip(label = ratio.label, selected = cfg.aspectRatio == ratio) { onConfigChange(cfg.copy(aspectRatio = ratio)) }
            }
        }

        if (cfg.aspectRatio == AspectRatioOption.CUSTOM) {
            NativeSliderStepper(
                value = cfg.customWidthPx.toFloat(),
                range = 360f..4320f,
                display = "${cfg.customWidthPx}W",
                onChange = { onConfigChange(cfg.copy(customWidthPx = (it / 60f).roundToInt() * 60)) },
                onStep = { d -> onConfigChange(cfg.copy(customWidthPx = (cfg.customWidthPx + d * 120).coerceIn(360, 4320))) }
            )
            NativeSliderStepper(
                value = cfg.customHeightPx.toFloat(),
                range = 360f..4320f,
                display = "${cfg.customHeightPx}H",
                onChange = { onConfigChange(cfg.copy(customHeightPx = (it / 60f).roundToInt() * 60)) },
                onStep = { d -> onConfigChange(cfg.copy(customHeightPx = (cfg.customHeightPx + d * 120).coerceIn(360, 4320))) }
            )
        } else if (cfg.aspectRatio.printWidthPx == null) {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ResolutionPreset.entries.forEach { res ->
                    NativePillChip(label = res.label, selected = cfg.resolution == res) { onConfigChange(cfg.copy(resolution = res)) }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ExportFormat.entries.forEach { fmt ->
                NativePillChip(label = fmt.label, selected = cfg.format == fmt) { onConfigChange(cfg.copy(format = fmt)) }
            }
        }

        if (cfg.format != ExportFormat.PNG) {
            NativeSliderStepper(
                value = cfg.qualityPercent.toFloat(),
                range = 80f..100f,
                display = "${cfg.qualityPercent}%",
                onChange = { onConfigChange(cfg.copy(qualityPercent = it.roundToInt())) },
                onStep = { d -> onConfigChange(cfg.copy(qualityPercent = (cfg.qualityPercent + d * 5).coerceIn(80, 100))) }
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Strip EXIF / GPS", color = TextPrimary, fontSize = 13.sp)
            Switch(checked = cfg.stripExif, onCheckedChange = { onConfigChange(cfg.copy(stripExif = it)) })
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Custom Handle Stamp", color = TextPrimary, fontSize = 13.sp)
            Switch(checked = cfg.includeSignature, onCheckedChange = { onConfigChange(cfg.copy(includeSignature = it)) })
        }

        if (cfg.includeSignature) {
            OutlinedTextField(
                value = cfg.signatureText,
                onValueChange = { onConfigChange(cfg.copy(signatureText = it)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (isExporting) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = AccentGreen, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Rendering high-res export...", color = TextPrimary, fontSize = 12.sp)
            }
        } else if (lastResult != null) {
            Surface(color = BgCanvas, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, AccentGreen), modifier = Modifier.fillMaxWidth()) {
                Text(lastResult.summary, color = AccentGreen, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onExportGallery,
                enabled = !isExporting,
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color(0xFF04140A)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(44.dp)
            ) {
                Icon(Icons.Default.Download, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(5.dp))
                Text("Save Gallery", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onExportZip, enabled = !isExporting, shape = RoundedCornerShape(12.dp), modifier = Modifier.height(44.dp)) {
                Icon(Icons.Default.FolderZip, "ZIP", modifier = Modifier.size(18.dp))
            }
            OutlinedButton(onClick = onShare, enabled = !isExporting, shape = RoundedCornerShape(12.dp), modifier = Modifier.height(44.dp)) {
                Icon(Icons.Default.Share, "Share", modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

private fun parseHexColor(raw: String): Long? {
    val clean = raw.trim().removePrefix("#")
    return when (clean.length) {
        6 -> clean.toLongOrNull(16)?.or(0xFF000000L)
        8 -> clean.toLongOrNull(16)
        else -> null
    }
}

private fun composeColorMatrix(item: MediaItem): ColorMatrix {
    val cm = ColorMatrix()
    val sat = ColorMatrix().apply { setToSaturation(item.saturation) }
    cm.timesAssign(sat)
    val c = item.contrast
    val b = item.brightness * 255f
    val rBoost = item.warmth * 35f
    val bBoost = -item.warmth * 35f
    val t = (-0.5f * c + 0.5f) * 255f + b
    cm.timesAssign(
        ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, t + rBoost,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t + bBoost,
                0f, 0f, 0f, 1f, 0f
            )
        )
    )
    return cm
}
