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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.darkmode.scrollgridstudio.data.DraftStorage
import com.darkmode.scrollgridstudio.export.CollageExporter
import com.darkmode.scrollgridstudio.export.ExportResult
import com.darkmode.scrollgridstudio.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.roundToInt

private enum class AppScreen { HOME, EDITOR, PREVIEW }

private val BgCanvas = Color(0xFF0B0D10)
private val BgPanel = Color(0xFF14171D)
private val BorderSubtle = Color(0xFF242933)
private val AccentGreen = Color(0xFF22C55E)
private val CyanGuide = Color(0xFF38BDF8)
private val DangerRed = Color(0xFFEF4444)
private val TextPrimary = Color(0xFFF3F4F6)
private val TextSecondary = Color(0xFF9CA3AF)

private val TextSwatches = listOf(
    0xFFF3F4F6L,
    0xFF0B0D10L,
    0xFF22C55EL,
    0xFFFACC15L,
    0xFF38BDF8L,
    0xFFF43F5EL,
    0xFFF97316L
)

private val BorderSwatches = listOf(
    null,
    0xFFF3F4F6L,
    0xFF0B0D10L,
    0xFF22C55EL,
    0xFFFACC15L,
    0xFF38BDF8L
)

private fun bestLayoutForCount(count: Int): LayoutStyle = when {
    count <= 1 -> LayoutStyle.SINGLE_1
    count == 2 -> LayoutStyle.SPLIT_2_H
    count == 3 -> LayoutStyle.SLANT_3
    count == 4 -> LayoutStyle.GRID_2X2
    count == 5 -> LayoutStyle.GRID_5
    count == 6 -> LayoutStyle.GRID_6
    else -> LayoutStyle.GRID_9
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
    var state by remember { mutableStateOf(StudioState()) }
    var undoStack by remember { mutableStateOf<List<StudioState>>(emptyList()) }
    var redoStack by remember { mutableStateOf<List<StudioState>>(emptyList()) }
    var activeDraftId by remember { mutableStateOf<String?>(null) }
    var draftsList by remember { mutableStateOf(DraftStorage.listDrafts(context)) }

    var textInput by remember { mutableStateOf("") }
    var hexInput by remember { mutableStateOf("#0B0D10") }
    var showExportSheet by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var lastExportResult by remember { mutableStateOf<ExportResult?>(null) }
    var statusBanner by remember { mutableStateOf("ScrollGrid Studio • Ready") }

    fun pushState(next: StudioState, recordHistory: Boolean = true) {
        if (recordHistory && next != state) {
            undoStack = (undoStack + state).takeLast(25)
            redoStack = emptyList()
        }
        state = next
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.last()
            redoStack = redoStack + state
            undoStack = undoStack.dropLast(1)
            state = prev
            statusBanner = "Undid last change"
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.last()
            undoStack = undoStack + state
            redoStack = redoStack.dropLast(1)
            state = next
            statusBanner = "Redid change"
        }
    }

    // Photo-first smart picker from Home Hub
    val homeGridPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(24)
    ) { uris: List<Uri> ->
        val items = uris.mapIndexed { i, uri ->
            val mime = context.contentResolver.getType(uri) ?: ""
            MediaItem(
                id = UUID.randomUUID().toString(),
                uri = uri,
                isVideo = mime.startsWith("video/"),
                freeX = (0.18f + (i % 5) * 0.16f).coerceIn(0.12f, 0.88f),
                freeY = (0.28f + (i % 3) * 0.2f).coerceIn(0.18f, 0.82f),
                zIndex = i
            )
        }
        val matched = bestLayoutForCount(items.size)
        val cap = matched.slotCount
        activeDraftId = null
        undoStack = emptyList()
        redoStack = emptyList()
        state = StudioState(
            projectName = "Grid (${items.size} photos)",
            mode = StudioMode.GRID,
            layout = matched,
            slots = items.take(cap),
            extras = items.drop(cap)
        )
        statusBanner = "Auto-matched ${matched.label} for ${items.size} item(s)"
        currentScreen = AppScreen.EDITOR
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
            statusBanner = "Added ${added.size} item(s) • ${pool.take(cap).size}/$cap active"
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
            statusBanner = "Replaced tile #${idx + 1}"
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
            statusBanner = "Added PNG/photo sticker"
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

    fun applyScrlTemplate(template: ScrlTemplate, resetProject: Boolean = false) {
        val slides = template.slides
        val base = if (resetProject) StudioState(projectName = template.label) else state
        val pool = base.slots + base.extras
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
        val next = base.copy(
            mode = StudioMode.CAROUSEL,
            carouselSlides = slides,
            slots = arranged,
            extras = pool.drop(cap),
            selectedSlotIndex = 0
        )
        if (resetProject) {
            activeDraftId = null
            undoStack = emptyList()
            redoStack = emptyList()
            state = next
        } else {
            pushState(next)
        }
        statusBanner = "${template.label} (${slides} slides)"
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
        statusBanner = "Duplicated layer"
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
        statusBanner = "Removed media item"
    }

    fun handleTileTap(idx: Int) {
        val swapSrc = state.swapSourceIndex
        if (swapSrc != null && swapSrc != idx && swapSrc in state.slots.indices && idx in state.slots.indices) {
            val swapped = state.slots.toMutableList().apply {
                val tmp = this[swapSrc]
                this[swapSrc] = this[idx]
                this[idx] = tmp
            }
            pushState(state.copy(slots = swapped, selectedSlotIndex = idx, swapSourceIndex = null))
            statusBanner = "Swapped tile #${swapSrc + 1} with #${idx + 1}"
        } else {
            state = state.copy(selectedSlotIndex = idx, swapSourceIndex = null)
        }
    }

    when (currentScreen) {
        AppScreen.HOME -> {
            HomeHubScreen(
                drafts = draftsList,
                onStartPhotoGrid = {
                    homeGridPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                },
                onStartCarousel = { slides ->
                    activeDraftId = null
                    undoStack = emptyList()
                    redoStack = emptyList()
                    state = StudioState(
                        projectName = "Carousel ($slides slides)",
                        mode = StudioMode.CAROUSEL,
                        carouselSlides = slides
                    )
                    statusBanner = "New $slides-slide seamless carousel"
                    currentScreen = AppScreen.EDITOR
                },
                onStartFreeform = {
                    activeDraftId = null
                    undoStack = emptyList()
                    redoStack = emptyList()
                    state = StudioState(projectName = "Freeform Collage", mode = StudioMode.FREESTYLE)
                    statusBanner = "New freeform canvas"
                    currentScreen = AppScreen.EDITOR
                },
                onSelectScrlTemplate = { tpl ->
                    applyScrlTemplate(tpl, resetProject = true)
                    currentScreen = AppScreen.EDITOR
                },
                onSelectGridTemplate = { layout ->
                    activeDraftId = null
                    undoStack = emptyList()
                    redoStack = emptyList()
                    state = StudioState(projectName = layout.label, mode = StudioMode.GRID, layout = layout)
                    statusBanner = "Started ${layout.label} template"
                    currentScreen = AppScreen.EDITOR
                },
                onOpenDraft = { draft ->
                    val loaded = DraftStorage.loadDraft(context, draft.id)
                    if (loaded != null) {
                        activeDraftId = draft.id
                        undoStack = emptyList()
                        redoStack = emptyList()
                        state = loaded
                        statusBanner = "Opened '${loaded.projectName}'"
                        currentScreen = AppScreen.EDITOR
                    }
                },
                onDeleteDraft = { draft ->
                    DraftStorage.deleteDraft(context, draft.id)
                    draftsList = DraftStorage.listDrafts(context)
                }
            )
        }

        AppScreen.PREVIEW -> {
            SwipePreviewScreen(
                state = state,
                onBackToEdit = { currentScreen = AppScreen.EDITOR },
                onOpenExport = { showExportSheet = true }
            )
        }

        AppScreen.EDITOR -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgCanvas)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // 1. Minimal Focused Top Bar: Home, Mode, Undo/Redo, Preview, Lock, Export
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(BgPanel)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (state.slots.isNotEmpty() || state.overlays.isNotEmpty()) {
                                activeDraftId = DraftStorage.saveDraft(context, state, activeDraftId)
                            }
                            draftsList = DraftStorage.listDrafts(context)
                            currentScreen = AppScreen.HOME
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, "Home", tint = TextPrimary, modifier = Modifier.size(18.dp))
                    }

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
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    IconButton(onClick = { undo() }, enabled = undoStack.isNotEmpty(), modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Undo, "Undo", tint = if (undoStack.isNotEmpty()) TextPrimary else Color(0xFF4B5563), modifier = Modifier.size(17.dp))
                    }
                    IconButton(onClick = { redo() }, enabled = redoStack.isNotEmpty(), modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Redo, "Redo", tint = if (redoStack.isNotEmpty()) TextPrimary else Color(0xFF4B5563), modifier = Modifier.size(17.dp))
                    }
                    IconButton(onClick = { currentScreen = AppScreen.PREVIEW }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.PlayArrow, "Swipe Preview", tint = CyanGuide, modifier = Modifier.size(20.dp))
                    }

                    Surface(
                        onClick = {
                            val locked = !state.canvasLocked
                            state = state.copy(canvasLocked = locked)
                            statusBanner = if (locked) "Canvas locked" else "Canvas unlocked"
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (state.canvasLocked) Color(0xFF1A2E22) else BgCanvas,
                        border = BorderStroke(1.dp, if (state.canvasLocked) AccentGreen else BorderSubtle)
                    ) {
                        Icon(
                            imageVector = if (state.canvasLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Lock Canvas",
                            tint = if (state.canvasLocked) AccentGreen else TextSecondary,
                            modifier = Modifier.padding(5.dp).size(15.dp)
                        )
                    }

                    Button(
                        onClick = { showExportSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color(0xFF05130A)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Export", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = BorderSubtle)

                // Status & Draft Strip
                Row(
                    modifier = Modifier.fillMaxWidth().background(BgCanvas).padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (state.swapSourceIndex != null) "Tap another tile to swap with #${state.swapSourceIndex!! + 1}" else statusBanner,
                        color = if (state.swapSourceIndex != null) CyanGuide else TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Save",
                            color = AccentGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                activeDraftId = DraftStorage.saveDraft(context, state, activeDraftId)
                                draftsList = DraftStorage.listDrafts(context)
                                statusBanner = "Saved to My Collages"
                            }
                        )
                        if (state.slots.isNotEmpty() || state.overlays.isNotEmpty()) {
                            Text(
                                text = "Reset",
                                color = DangerRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable {
                                    pushState(StudioState(projectName = state.projectName, mode = state.mode, layout = state.layout, carouselSlides = state.carouselSlides))
                                    statusBanner = "Canvas cleared"
                                }
                            )
                        }
                    }
                }

                // 2. Focused Canvas Viewport (~68% of screen)
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val density = LocalDensity.current
                    val ratioW = state.exportConfig.outputWidthPx().toFloat()
                    val ratioH = state.exportConfig.outputHeightPx().toFloat()
                    val slideAspect = (ratioW / ratioH).coerceIn(0.45f, 2.0f)

                    val slideHeightDp = (maxHeight - 8.dp).coerceAtLeast(180.dp)
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
                                .clip(RoundedCornerShape(10.dp))
                                .then(bgModifier)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
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
                                    Icon(Icons.Default.AddPhotoAlternate, null, tint = AccentGreen, modifier = Modifier.size(36.dp))
                                    Spacer(Modifier.height(6.dp))
                                    Text("Tap to Add Photos or Videos", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Tap Play icon above anytime for Swipe Preview", color = TextSecondary, fontSize = 11.sp)
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
                                        drawLine(CyanGuide.copy(alpha = 0.35f), Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), 1.5f, pathEffect = dash)
                                        drawLine(CyanGuide.copy(alpha = 0.35f), Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), 1.5f, pathEffect = dash)
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
                                                state = state.copy(selectedOverlayId = overlay.id, activeTab = if (overlay.isSticker) StudioTab.STICKERS else StudioTab.TEXT)
                                                if (!overlay.isSticker) textInput = overlay.text
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

                    Surface(
                        color = BgPanel.copy(alpha = 0.94f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                                }
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, null, tint = AccentGreen, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("Add", color = TextPrimary, fontSize = 11.sp)
                            }
                            if (state.mode == StudioMode.CAROUSEL) {
                                Box(Modifier.width(1.dp).height(12.dp).background(BorderSubtle))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        applyLayoutCapacity(state.mode, state.layout, state.carouselSlides + 1)
                                    }
                                ) {
                                    Icon(Icons.Default.Add, null, tint = CyanGuide, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(2.dp))
                                    Text("Slide (${state.carouselSlides})", color = TextPrimary, fontSize = 11.sp)
                                }
                            }
                            Box(Modifier.width(1.dp).height(12.dp).background(BorderSubtle))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { state = state.copy(showGuides = !state.showGuides) }
                            ) {
                                Icon(
                                    if (state.showGuides) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    null,
                                    tint = if (state.showGuides) CyanGuide else TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text("Guides", color = TextPrimary, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Visible Extras Tray
                if (state.extras.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(BgCanvas).padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Extras (${state.extras.size})", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            itemsIndexed(state.extras) { extraIdx, extraItem ->
                                AsyncImage(
                                    model = extraItem.uri,
                                    contentDescription = "Extra photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                                        .clickable {
                                            if (state.slots.isNotEmpty()) {
                                                val targetIdx = state.selectedSlotIndex.coerceIn(0, state.slots.lastIndex)
                                                val newSlots = state.slots.toMutableList()
                                                val newExtras = state.extras.toMutableList()
                                                val displaced = newSlots[targetIdx]
                                                newSlots[targetIdx] = extraItem
                                                newExtras[extraIdx] = displaced
                                                pushState(state.copy(slots = newSlots, extras = newExtras))
                                                statusBanner = "Swapped extra into tile #${targetIdx + 1}"
                                            }
                                        }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = BorderSubtle)

                // 3. Contextual Tool Sub-Panel
                Column(
                    modifier = Modifier.fillMaxWidth().background(BgPanel).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    when (state.activeTab) {
                        StudioTab.LAYOUT -> {
                            if (state.mode == StudioMode.CAROUSEL) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ScrlTemplate.entries.forEach { tpl ->
                                        SuggestionChip(
                                            onClick = { applyScrlTemplate(tpl) },
                                            label = { Text(tpl.label, fontSize = 11.sp) }
                                        )
                                    }
                                }
                                StepperRow(
                                    label = "Carousel Slides",
                                    valueText = "${state.carouselSlides} slides",
                                    onMinus = { applyLayoutCapacity(state.mode, state.layout, state.carouselSlides - 1) },
                                    onPlus = { applyLayoutCapacity(state.mode, state.layout, state.carouselSlides + 1) }
                                )
                            } else {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(LayoutStyle.entries) { layout ->
                                        LayoutThumbnailChip(
                                            layout = layout,
                                            selected = state.layout == layout,
                                            onClick = { applyLayoutCapacity(state.mode, layout, state.carouselSlides) }
                                        )
                                    }
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(AspectRatioOption.entries) { ratio ->
                                    FilterChip(
                                        selected = state.exportConfig.aspectRatio == ratio,
                                        onClick = { pushState(state.copy(exportConfig = state.exportConfig.copy(aspectRatio = ratio))) },
                                        label = { Text(ratio.label, fontSize = 10.sp) }
                                    )
                                }
                            }
                            StepperRow(
                                label = "Outer Margin",
                                valueText = "${state.outerMarginDp} px",
                                onMinus = { pushState(state.copy(outerMarginDp = (state.outerMarginDp - 2).coerceAtLeast(0))) },
                                onPlus = { pushState(state.copy(outerMarginDp = (state.outerMarginDp + 2).coerceAtMost(48))) }
                            )
                            StepperRow(
                                label = "Inner Spacing",
                                valueText = "${state.borderDp} px",
                                onMinus = { pushState(state.copy(borderDp = (state.borderDp - 2).coerceAtLeast(0))) },
                                onPlus = { pushState(state.copy(borderDp = (state.borderDp + 2).coerceAtMost(48))) }
                            )
                            StepperRow(
                                label = "Corner Radius",
                                valueText = "${state.cornerRadiusDp} px",
                                onMinus = { pushState(state.copy(cornerRadiusDp = (state.cornerRadiusDp - 2).coerceAtLeast(0))) },
                                onPlus = { pushState(state.copy(cornerRadiusDp = (state.cornerRadiusDp + 2).coerceAtMost(48))) }
                            )
                        }

                        StudioTab.MEDIA -> {
                            val sel = state.slots.getOrNull(state.selectedSlotIndex)
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) { Text("Add", fontSize = 11.sp) }
                                OutlinedButton(
                                    onClick = { replacePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                                    enabled = sel != null,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) { Text("Replace", fontSize = 11.sp) }
                                OutlinedButton(
                                    onClick = {
                                        if (sel != null) {
                                            state = state.copy(swapSourceIndex = if (state.swapSourceIndex == null) state.selectedSlotIndex else null)
                                        }
                                    },
                                    enabled = state.slots.size > 1,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) { Text("Swap", fontSize = 11.sp) }
                                OutlinedButton(
                                    onClick = { updateSelectedMedia { it.copy(fitInside = !it.fitInside) } },
                                    enabled = sel != null,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) { Text(if (sel?.fitInside == true) "Fill" else "Fit", fontSize = 11.sp) }
                                OutlinedButton(
                                    onClick = { updateSelectedMedia { it.copy(flipHorizontal = !it.flipHorizontal) } },
                                    enabled = sel != null,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) { Icon(Icons.Default.Flip, null, modifier = Modifier.size(15.dp)) }
                                OutlinedButton(
                                    onClick = { updateSelectedMedia { it.copy(rotationDeg = (it.rotationDeg + 90f) % 360f) } },
                                    enabled = sel != null,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) { Text("90°", fontSize = 11.sp) }
                                if (state.mode != StudioMode.GRID && sel != null) {
                                    OutlinedButton(onClick = { duplicateSelectedMedia() }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                                        Icon(Icons.Default.ContentCopy, "Duplicate", modifier = Modifier.size(15.dp))
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val maxZ = (state.slots.maxOfOrNull { it.zIndex } ?: 0) + 1
                                            updateSelectedMedia { it.copy(zIndex = maxZ) }
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) { Icon(Icons.Default.FlipToFront, "Front", modifier = Modifier.size(15.dp)) }
                                    OutlinedButton(
                                        onClick = {
                                            val minZ = (state.slots.minOfOrNull { it.zIndex } ?: 0) - 1
                                            updateSelectedMedia { it.copy(zIndex = minZ) }
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) { Icon(Icons.Default.FlipToBack, "Back", modifier = Modifier.size(15.dp)) }
                                    OutlinedButton(
                                        onClick = { updateSelectedMedia { it.copy(locked = !it.locked) } },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) { Icon(if (sel.locked) Icons.Default.Lock else Icons.Default.LockOpen, "Lock", modifier = Modifier.size(15.dp)) }
                                }
                                OutlinedButton(
                                    onClick = { deleteSelectedMedia() },
                                    enabled = sel != null,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) { Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(15.dp)) }
                            }

                            if (sel != null) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(TileMaskShape.entries) { mask ->
                                        FilterChip(
                                            selected = sel.maskShape == mask,
                                            onClick = { updateSelectedMedia { it.copy(maskShape = mask) } },
                                            label = { Text(mask.label, fontSize = 10.sp) }
                                        )
                                    }
                                }
                            }

                            StepperRow(
                                label = if (state.mode == StudioMode.GRID) "Tile Zoom" else "Drop Shadow",
                                valueText = if (state.mode == StudioMode.GRID) "${((sel?.scale ?: 1f) * 100).roundToInt()}%" else "${sel?.shadowDp ?: 0} dp",
                                onMinus = {
                                    if (state.mode == StudioMode.GRID) {
                                        updateSelectedMedia { it.copy(scale = (it.scale - 0.1f).coerceAtLeast(0.5f)) }
                                    } else {
                                        updateSelectedMedia { it.copy(shadowDp = (it.shadowDp - 2).coerceAtLeast(0)) }
                                    }
                                },
                                onPlus = {
                                    if (state.mode == StudioMode.GRID) {
                                        updateSelectedMedia { it.copy(scale = (it.scale + 0.1f).coerceAtMost(4f)) }
                                    } else {
                                        updateSelectedMedia { it.copy(shadowDp = (it.shadowDp + 2).coerceAtMost(24)) }
                                    }
                                }
                            )
                        }

                        StudioTab.BG -> {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(BackgroundStyle.entries) { bg ->
                                    val selected = state.customBgHex == null && state.backgroundStyle == bg
                                    val mod = if (bg.startHex != bg.endHex && !bg.isBlurPhoto) {
                                        Modifier.background(Brush.linearGradient(listOf(Color(bg.startHex), Color(bg.endHex))))
                                    } else {
                                        Modifier.background(Color(bg.startHex))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .then(mod)
                                            .border(if (selected) 2.dp else 1.dp, if (selected) AccentGreen else BorderSubtle, RoundedCornerShape(10.dp))
                                            .clickable { pushState(state.copy(backgroundStyle = bg, customBgHex = null)) }
                                            .padding(horizontal = 10.dp, vertical = 7.dp)
                                    ) {
                                        Text(bg.label, color = if (bg == BackgroundStyle.PURE_WHITE) Color(0xFF0B0D10) else TextPrimary, fontSize = 11.sp)
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = hexInput,
                                    onValueChange = { hexInput = it },
                                    placeholder = { Text("#133326", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        val parsed = parseHexColor(hexInput)
                                        if (parsed != null) {
                                            pushState(state.copy(customBgHex = parsed))
                                            statusBanner = "Applied custom BG $hexInput"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color(0xFF05130A))
                                ) { Text("HEX", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CanvasTexture.entries.forEach { tex ->
                                    FilterChip(
                                        selected = state.canvasTexture == tex,
                                        onClick = { pushState(state.copy(canvasTexture = tex)) },
                                        label = { Text(tex.label, fontSize = 10.sp) }
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                                Text("Border:", color = TextSecondary, fontSize = 10.sp)
                                BorderSwatches.forEach { hex ->
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
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
                                            if (selOverlay != null) {
                                                pushState(state.copy(overlays = state.overlays.map {
                                                    if (it.id == selOverlay.id) it.copy(text = textInput.trim()) else it
                                                }))
                                            } else {
                                                val newOverlay = TextOverlay(id = UUID.randomUUID().toString(), text = textInput.trim())
                                                pushState(state.copy(overlays = state.overlays + newOverlay, selectedOverlayId = newOverlay.id))
                                            }
                                            textInput = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color(0xFF05130A))
                                ) { Text(if (selOverlay != null) "Update" else "Add", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            }
                            if (selOverlay != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextFontStyle.entries.forEach { style ->
                                        FilterChip(
                                            selected = selOverlay.fontStyle == style,
                                            onClick = {
                                                pushState(state.copy(overlays = state.overlays.map {
                                                    if (it.id == selOverlay.id) it.copy(fontStyle = style) else it
                                                }))
                                            },
                                            label = { Text(style.label, fontSize = 10.sp) }
                                        )
                                    }
                                    FilterChip(
                                        selected = selOverlay.usePill,
                                        onClick = {
                                            pushState(state.copy(overlays = state.overlays.map {
                                                if (it.id == selOverlay.id) it.copy(usePill = !it.usePill) else it
                                            }))
                                        },
                                        label = { Text("Pill", fontSize = 10.sp) }
                                    )
                                    IconButton(
                                        onClick = {
                                            pushState(state.copy(overlays = state.overlays.filterNot { it.id == selOverlay.id }, selectedOverlayId = null))
                                            textInput = ""
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) { Icon(Icons.Default.Delete, "Delete Text", tint = DangerRed, modifier = Modifier.size(18.dp)) }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    TextSwatches.forEach { hex ->
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(Color(hex))
                                                .border(if (selOverlay.textColorHex == hex) 2.dp else 1.dp, if (selOverlay.textColorHex == hex) AccentGreen else BorderSubtle, CircleShape)
                                                .clickable {
                                                    pushState(state.copy(overlays = state.overlays.map {
                                                        if (it.id == selOverlay.id) it.copy(textColorHex = hex) else it
                                                    }))
                                                }
                                        )
                                    }
                                }
                                StepperRow(
                                    label = "Font Size",
                                    valueText = "${selOverlay.fontSizeSp.roundToInt()} sp",
                                    onMinus = {
                                        pushState(state.copy(overlays = state.overlays.map {
                                            if (it.id == selOverlay.id) it.copy(fontSizeSp = (it.fontSizeSp - 2f).coerceAtLeast(12f)) else it
                                        }))
                                    },
                                    onPlus = {
                                        pushState(state.copy(overlays = state.overlays.map {
                                            if (it.id == selOverlay.id) it.copy(fontSizeSp = (it.fontSizeSp + 2f).coerceAtMost(72f)) else it
                                        }))
                                    }
                                )
                            }
                        }

                        StudioTab.STICKERS -> {
                            val selSticker = state.overlays.find { it.id == state.selectedOverlayId && it.isSticker }
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalButton(
                                    onClick = { stickerImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Import PNG", fontSize = 11.sp)
                                }
                                listOf("SWIPE →", "01 / 03", "KERALA", "SHOT ON MOBILE", "✦", "★", "📷", "🎬", "🔥").forEach { badge ->
                                    SuggestionChip(
                                        onClick = {
                                            val item = TextOverlay(id = UUID.randomUUID().toString(), text = badge, fontSizeSp = 16f, usePill = badge.length > 2, isSticker = true)
                                            pushState(state.copy(overlays = state.overlays + item, selectedOverlayId = item.id))
                                        },
                                        label = { Text(badge, fontSize = 11.sp) }
                                    )
                                }
                            }
                            if (selSticker != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Opacity: ${(selSticker.opacity * 100).roundToInt()}%", color = TextSecondary, fontSize = 11.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                pushState(state.copy(overlays = state.overlays.map {
                                                    if (it.id == selSticker.id) it.copy(opacity = (it.opacity - 0.15f).coerceAtLeast(0.2f)) else it
                                                }))
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) { Text("-", fontSize = 12.sp) }
                                        OutlinedButton(
                                            onClick = {
                                                pushState(state.copy(overlays = state.overlays.map {
                                                    if (it.id == selSticker.id) it.copy(opacity = (it.opacity + 0.15f).coerceAtMost(1f)) else it
                                                }))
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) { Text("+", fontSize = 12.sp) }
                                        IconButton(
                                            onClick = {
                                                pushState(state.copy(overlays = state.overlays.filterNot { it.id == selSticker.id }, selectedOverlayId = null))
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) { Icon(Icons.Default.Delete, "Delete Sticker", tint = DangerRed, modifier = Modifier.size(18.dp)) }
                                    }
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
                                    FilterChip(
                                        selected = sel != null && sel.brightness == preset.brightness && sel.contrast == preset.contrast && sel.saturation == preset.saturation,
                                        onClick = {
                                            updateSelectedMedia {
                                                it.copy(
                                                    brightness = preset.brightness,
                                                    contrast = preset.contrast,
                                                    saturation = preset.saturation,
                                                    warmth = preset.warmth,
                                                    vignette = preset.vignette
                                                )
                                            }
                                        },
                                        label = { Text(preset.label, fontSize = 11.sp) }
                                    )
                                }
                                if (sel != null && state.slots.size > 1) {
                                    FilledTonalButton(
                                        onClick = {
                                            pushState(state.copy(slots = state.slots.map {
                                                it.copy(brightness = sel.brightness, contrast = sel.contrast, saturation = sel.saturation, warmth = sel.warmth, vignette = sel.vignette)
                                            }))
                                            statusBanner = "Applied filter to all ${state.slots.size} photos"
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) { Text("Apply All", fontSize = 11.sp) }
                                }
                            }
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
                                label = "Warmth",
                                valueText = "${((sel?.warmth ?: 0f) * 100).roundToInt()}",
                                onMinus = { updateSelectedMedia { it.copy(warmth = (it.warmth - 0.1f).coerceAtLeast(-0.8f)) } },
                                onPlus = { updateSelectedMedia { it.copy(warmth = (it.warmth + 0.1f).coerceAtMost(0.8f)) } }
                            )
                            StepperRow(
                                label = "Vignette",
                                valueText = "${((sel?.vignette ?: 0f) * 100).roundToInt()}%",
                                onMinus = { updateSelectedMedia { it.copy(vignette = (it.vignette - 0.1f).coerceAtLeast(0f)) } },
                                onPlus = { updateSelectedMedia { it.copy(vignette = (it.vignette + 0.1f).coerceAtMost(0.8f)) } }
                            )
                        }
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = BorderSubtle)

                // 4. Bottom Tool Dock
                Row(
                    modifier = Modifier.fillMaxWidth().height(56.dp).background(BgPanel).padding(horizontal = 4.dp),
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
                                .padding(vertical = 5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(icon, tab.label, tint = if (selected) AccentGreen else TextSecondary, modifier = Modifier.size(19.dp))
                            Text(tab.label, color = if (selected) AccentGreen else TextSecondary, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }

    // Shared Async Export Sheet (accessible from both Editor and Swipe Preview)
    if (showExportSheet) {
        ModalBottomSheet(
            onDismissRequest = { if (!isExporting) showExportSheet = false },
            containerColor = BgPanel,
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
                        statusBanner = res?.summary ?: "Export failed"
                    }
                },
                onExportZip = {
                    scope.launch {
                        isExporting = true
                        val res = withContext(Dispatchers.IO) { CollageExporter.exportStudio(context, state, asZip = true) }
                        isExporting = false
                        lastExportResult = res
                        statusBanner = res?.summary ?: "ZIP export failed"
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
                        if (res != null) {
                            CollageExporter.shareUris(context, res.uris, state.exportConfig.format.mimeType)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun LayoutThumbnailChip(layout: LayoutStyle, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) Color(0xFF1A2E22) else BgCanvas,
        border = BorderStroke(1.dp, if (selected) AccentGreen else BorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Canvas(modifier = Modifier.size(28.dp)) {
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
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = (item.vignette * 0.75f).coerceIn(0f, 0.75f)))
                        )
                    )
            )
        }
        if (item.isVideo) {
            Surface(
                color = Color(0xCC0B0D10),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.align(Alignment.TopEnd).padding(5.dp)
            ) {
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
private fun StepperRow(label: String, valueText: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(onClick = onMinus, shape = CircleShape, color = BgCanvas, border = BorderStroke(1.dp, BorderSubtle)) {
                Icon(Icons.Default.Remove, "Decrease $label", tint = TextPrimary, modifier = Modifier.padding(4.dp).size(15.dp))
            }
            Text(valueText, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.widthIn(min = 58.dp))
            Surface(onClick = onPlus, shape = CircleShape, color = BgCanvas, border = BorderStroke(1.dp, BorderSubtle)) {
                Icon(Icons.Default.Add, "Increase $label", tint = TextPrimary, modifier = Modifier.padding(4.dp).size(15.dp))
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Photo Export Options", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Surface(color = Color(0xFF1A2E22), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, AccentGreen)) {
                Text("${w} × ${h} px • sRGB", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }

        Text("Export Mode", color = TextSecondary, fontSize = 12.sp)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ExportMode.entries.forEach { mode ->
                FilterChip(selected = cfg.mode == mode, onClick = { onConfigChange(cfg.copy(mode = mode)) }, label = { Text(mode.label, fontSize = 11.sp) })
            }
        }

        if (state.mode == StudioMode.CAROUSEL && cfg.mode == ExportMode.AUTO_SLICE) {
            Text("Slide Save Order", color = TextSecondary, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SlideOrder.entries.forEach { order ->
                    FilterChip(selected = cfg.slideOrder == order, onClick = { onConfigChange(cfg.copy(slideOrder = order)) }, label = { Text(order.label, fontSize = 11.sp) })
                }
            }
        }

        if (state.mode == StudioMode.CAROUSEL && cfg.mode == ExportMode.SELECTED_SLIDE) {
            StepperRow(
                label = "Slide to Export",
                valueText = "Slide ${cfg.selectedSlideNumber.coerceIn(1, state.carouselSlides)}",
                onMinus = { onConfigChange(cfg.copy(selectedSlideNumber = (cfg.selectedSlideNumber - 1).coerceAtLeast(1))) },
                onPlus = { onConfigChange(cfg.copy(selectedSlideNumber = (cfg.selectedSlideNumber + 1).coerceAtMost(state.carouselSlides))) }
            )
        }

        Text("Aspect Ratio & Print Sizes", color = TextSecondary, fontSize = 12.sp)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AspectRatioOption.entries.forEach { ratio ->
                FilterChip(selected = cfg.aspectRatio == ratio, onClick = { onConfigChange(cfg.copy(aspectRatio = ratio)) }, label = { Text(ratio.label, fontSize = 11.sp) })
            }
        }

        if (cfg.aspectRatio == AspectRatioOption.CUSTOM) {
            StepperRow(
                label = "Custom Width",
                valueText = "${cfg.customWidthPx} px",
                onMinus = { onConfigChange(cfg.copy(customWidthPx = (cfg.customWidthPx - 120).coerceAtLeast(360))) },
                onPlus = { onConfigChange(cfg.copy(customWidthPx = (cfg.customWidthPx + 120).coerceAtMost(4320))) }
            )
            StepperRow(
                label = "Custom Height",
                valueText = "${cfg.customHeightPx} px",
                onMinus = { onConfigChange(cfg.copy(customHeightPx = (cfg.customHeightPx - 120).coerceAtLeast(360))) },
                onPlus = { onConfigChange(cfg.copy(customHeightPx = (cfg.customHeightPx + 120).coerceAtMost(4320))) }
            )
        } else if (cfg.aspectRatio.printWidthPx == null) {
            Text("Resolution", color = TextSecondary, fontSize = 12.sp)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ResolutionPreset.entries.forEach { res ->
                    FilterChip(selected = cfg.resolution == res, onClick = { onConfigChange(cfg.copy(resolution = res)) }, label = { Text(res.label, fontSize = 11.sp) })
                }
            }
        }

        Text("File Format", color = TextSecondary, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ExportFormat.entries.forEach { fmt ->
                FilterChip(selected = cfg.format == fmt, onClick = { onConfigChange(cfg.copy(format = fmt)) }, label = { Text(fmt.label, fontSize = 11.sp) })
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

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Strip EXIF / GPS Metadata", color = TextPrimary, fontSize = 12.sp)
            Switch(checked = cfg.stripExif, onCheckedChange = { onConfigChange(cfg.copy(stripExif = it)) })
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Custom Handle Stamp", color = TextPrimary, fontSize = 12.sp)
            Switch(checked = cfg.includeSignature, onCheckedChange = { onConfigChange(cfg.copy(includeSignature = it)) })
        }

        if (cfg.includeSignature) {
            OutlinedTextField(
                value = cfg.signatureText,
                onValueChange = { onConfigChange(cfg.copy(signatureText = it)) },
                label = { Text("Signature Handle", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (isExporting) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = AccentGreen, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Rendering high-res export...", color = TextPrimary, fontSize = 12.sp)
            }
        } else if (lastResult != null) {
            Surface(color = BgCanvas, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, AccentGreen), modifier = Modifier.fillMaxWidth()) {
                Text(lastResult.summary, color = AccentGreen, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onExportGallery,
                enabled = !isExporting,
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color(0xFF05130A)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Save Gallery", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onExportZip, enabled = !isExporting, shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.FolderZip, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Save ZIP", fontSize = 12.sp)
            }
            OutlinedButton(onClick = onShare, enabled = !isExporting, shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Default.Share, "Share", modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
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
