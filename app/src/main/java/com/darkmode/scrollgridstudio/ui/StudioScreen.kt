package com.darkmode.scrollgridstudio.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.darkmode.scrollgridstudio.data.DraftStorage
import com.darkmode.scrollgridstudio.data.ImportedFont
import com.darkmode.scrollgridstudio.export.CollageExporter
import com.darkmode.scrollgridstudio.export.ExportResult
import com.darkmode.scrollgridstudio.model.*
import com.darkmode.scrollgridstudio.ui.theme.LocalScrollGridPalette
import com.darkmode.scrollgridstudio.ui.theme.ThemePreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

private enum class AppScreen { HOME, EDITOR, SWIPE_PREVIEW }

private val StickerCatalog = listOf("✨", "🔥", "❤️", "🌿", "📸", "🎬", "⭐", "🌙", "🎨", "💫", "📍", "🏷️")
private val BrushColors = listOf(0xFFFFFFFFL, 0xFF0D0F14L, 0xFF5B8DEFL, 0xFFE07A5FL, 0xFF34D399L, 0xFFFBBF24L, 0xFFFB7185L)
private val BorderSwatches = listOf(null, 0xFFFFFFFFL, 0xFF0D0F14L, 0xFF5B8DEFL, 0xFFE07A5FL, 0xFFFBBF24L)

private fun bestLayoutForCount(count: Int): LayoutStyle = when {
    count <= 1 -> LayoutStyle.SINGLE_1
    count == 2 -> LayoutStyle.SPLIT_2_H
    count == 3 -> LayoutStyle.SLANT_3
    count == 4 -> LayoutStyle.GRID_2X2
    count == 5 -> LayoutStyle.GRID_5
    count == 6 -> LayoutStyle.GRID_6
    else -> LayoutStyle.GRID_9
}

private fun iconForTab(tab: StudioTab): ImageVector = when (tab) {
    StudioTab.LAYOUT -> Icons.Default.GridView
    StudioTab.PHOTOS -> Icons.Default.PhotoLibrary
    StudioTab.TEXT -> Icons.Default.Title
    StudioTab.STICKERS -> Icons.Default.EmojiEmotions
    StudioTab.FRAMES -> Icons.Default.FilterFrames
    StudioTab.BACKGROUND -> Icons.Default.Wallpaper
    StudioTab.DRAWING -> Icons.Default.Brush
    StudioTab.FILTERS -> Icons.Default.AutoFixHigh
    StudioTab.ADJUSTMENTS -> Icons.Default.Tune
    StudioTab.LAYERS -> Icons.Default.Layers
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    themePreference: ThemePreference = ThemePreference.DARK,
    onThemePreferenceChange: (ThemePreference) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = LocalScrollGridPalette.current

    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
    var currentDraftId by remember { mutableStateOf<String?>(null) }
    var savedDrafts by remember { mutableStateOf(DraftStorage.listDrafts(context)) }
    var favoriteLayouts by remember { mutableStateOf(DraftStorage.getFavoriteLayouts(context)) }
    var importedFonts by remember { mutableStateOf(DraftStorage.listImportedFonts(context)) }

    var state by remember { mutableStateOf(StudioState()) }
    var undoStack by remember { mutableStateOf<List<StudioState>>(emptyList()) }
    var redoStack by remember { mutableStateOf<List<StudioState>>(emptyList()) }

    // Staged media selection preserved when moving between Media Picker <-> Layout Picker <-> Editor
    var stagedMedia by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var showMediaSheet by remember { mutableStateOf(false) }
    var showLayoutPicker by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showRatioMenu by remember { mutableStateOf(false) }
    var showToolDrawer by remember { mutableStateOf(false) }

    // Drawing brush state
    var brushColorHex by remember { mutableStateOf(0xFFFFFFFFL) }
    var brushWidthDp by remember { mutableStateOf(4f) }
    var brushOpacity by remember { mutableStateOf(1f) }
    var activeStrokePoints by remember { mutableStateOf<List<DrawingPoint>>(emptyList()) }

    // Text input state
    var textInput by remember { mutableStateOf("") }

    // Export progress state
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableStateOf(0f) }
    var exportStage by remember { mutableStateOf("") }
    var lastExportResult by remember { mutableStateOf<ExportResult?>(null) }

    var replaceTargetIdx by remember { mutableStateOf<Int?>(null) }
    var statusToast by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(statusToast) {
        statusToast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            statusToast = null
        }
    }

    fun retainPickerUri(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun pushState(next: StudioState, recordHistory: Boolean = true) {
        if (recordHistory && next != state) {
            undoStack = (undoStack + state).takeLast(30)
            redoStack = emptyList()
        }
        state = next
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack = (redoStack + state).takeLast(30)
        state = undoStack.last()
        undoStack = undoStack.dropLast(1)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack = (undoStack + state).takeLast(30)
        state = redoStack.last()
        redoStack = redoStack.dropLast(1)
    }

    fun applyMediaListToState(media: List<MediaItem>, targetLayout: LayoutStyle? = null) {
        val chosenLayout = targetLayout ?: if (state.mode == StudioMode.GRID && media.isNotEmpty()) bestLayoutForCount(media.size) else state.layout
        val cap = when (state.mode) {
            StudioMode.CAROUSEL -> state.carouselSlides * 2
            StudioMode.GRID, StudioMode.FREESTYLE -> chosenLayout.slotCount
        }
        val active = media.take(cap).mapIndexed { idx, item ->
            item.copy(
                zIndex = idx,
                freeX = if (state.mode == StudioMode.FREESTYLE) 0.25f + (idx % 3) * 0.24f else item.freeX,
                freeY = if (state.mode == StudioMode.FREESTYLE) 0.28f + (idx / 3) * 0.24f else item.freeY
            )
        }
        val overflow = media.drop(cap)
        pushState(
            state.copy(
                layout = chosenLayout,
                slots = active,
                extras = overflow,
                selectedSlotIndex = 0,
                selectedOverlayId = null
            )
        )
    }

    fun applyLayoutPreservingMedia(nextLayout: LayoutStyle) {
        val combined = state.slots + state.extras
        val cap = when (state.mode) {
            StudioMode.CAROUSEL -> state.carouselSlides * 2
            StudioMode.GRID, StudioMode.FREESTYLE -> nextLayout.slotCount
        }
        val active = combined.take(cap)
        val overflow = combined.drop(cap)
        pushState(
            state.copy(
                layout = nextLayout,
                slots = active,
                extras = overflow,
                selectedSlotIndex = state.selectedSlotIndex.coerceIn(0, (active.size - 1).coerceAtLeast(0))
            )
        )
    }

    fun applyScrlTemplate(template: ScrlTemplate, resetProject: Boolean = false) {
        val base = if (resetProject) StudioState(projectName = template.label) else state
        pushState(
            base.copy(
                mode = StudioMode.CAROUSEL,
                carouselSlides = template.slides,
                activeSlideIndex = 0,
                selectedOverlayId = null
            )
        )
        currentScreen = AppScreen.EDITOR
    }

    fun updateSelectedMedia(recordHistory: Boolean = true, transform: (MediaItem) -> MediaItem) {
        val idx = state.selectedSlotIndex
        if (idx !in state.slots.indices) return
        val updated = state.slots.toMutableList()
        updated[idx] = transform(updated[idx])
        pushState(state.copy(slots = updated), recordHistory)
    }

    fun updateOverlayById(id: String, recordHistory: Boolean = true, transform: (TextOverlay) -> TextOverlay) {
        val updated = state.overlays.map { if (it.id == id) transform(it) else it }
        pushState(state.copy(overlays = updated), recordHistory)
    }

    fun swapMediaSlots(a: Int, b: Int) {
        if (a == b || a !in state.slots.indices || b !in state.slots.indices) return
        val list = state.slots.toMutableList()
        val tmp = list[a]
        list[a] = list[b]
        list[b] = tmp
        pushState(state.copy(slots = list, selectedSlotIndex = b, swapSourceIndex = null))
        statusToast = "Swapped cells ${a + 1} & ${b + 1}"
    }

    fun saveCurrentDraft() {
        val id = DraftStorage.saveDraft(context, state, currentDraftId)
        currentDraftId = id
        savedDrafts = DraftStorage.listDrafts(context)
        statusToast = "Project saved"
    }

    // Activity Result Launchers
    val pickPhotosLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris ->
        if (uris.isNotEmpty()) {
            val added = uris.map { uri ->
                retainPickerUri(uri)
                val mime = context.contentResolver.getType(uri) ?: ""
                MediaItem(id = UUID.randomUUID().toString(), uri = uri, isVideo = mime.startsWith("video/"))
            }
            stagedMedia = stagedMedia + added
        }
    }

    val replaceMediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val target = replaceTargetIdx
        replaceTargetIdx = null
        if (uri != null && target != null && target in state.slots.indices) {
            retainPickerUri(uri)
            val mime = context.contentResolver.getType(uri) ?: ""
            val updated = state.slots.toMutableList()
            updated[target] = updated[target].copy(
                uri = uri,
                isVideo = mime.startsWith("video/"),
                scale = 1f,
                panX = 0f,
                panY = 0f,
                rotationDeg = 0f
            )
            pushState(state.copy(slots = updated))
            stagedMedia = updated + state.extras
        }
    }

    val stickerImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            retainPickerUri(uri)
            val newSticker = TextOverlay(
                id = UUID.randomUUID().toString(),
                text = "",
                isSticker = true,
                stickerUri = uri,
                zIndex = (state.overlays.maxOfOrNull { it.zIndex } ?: 100) + 1
            )
            pushState(state.copy(overlays = state.overlays + newSticker, selectedOverlayId = newSticker.id))
        }
    }

    val importFontLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val imported: ImportedFont? = DraftStorage.importFontFromUri(context, uri)
            if (imported != null) {
                importedFonts = DraftStorage.listImportedFonts(context)
                state.selectedOverlayId?.let { id ->
                    updateOverlayById(id) { it.copy(fontName = imported.name, customFontPath = imported.path) }
                }
                statusToast = "Imported font: ${imported.name}"
            } else {
                statusToast = "Could not import font file"
            }
        }
    }

    when (currentScreen) {
        AppScreen.HOME -> {
            HomeHubScreen(
                drafts = savedDrafts,
                themePreference = themePreference,
                onThemePreferenceChange = onThemePreferenceChange,
                onNewProject = {
                    currentDraftId = null
                    state = StudioState(mode = StudioMode.GRID)
                    stagedMedia = emptyList()
                    currentScreen = AppScreen.EDITOR
                    showMediaSheet = true
                },
                onStartPhotoGrid = {
                    currentDraftId = null
                    state = StudioState(mode = StudioMode.GRID, layout = LayoutStyle.GRID_2X2)
                    stagedMedia = emptyList()
                    currentScreen = AppScreen.EDITOR
                    showMediaSheet = true
                },
                onStartCarousel = { slides ->
                    currentDraftId = null
                    state = StudioState(mode = StudioMode.CAROUSEL, carouselSlides = slides)
                    stagedMedia = emptyList()
                    currentScreen = AppScreen.EDITOR
                    showMediaSheet = true
                },
                onStartFreeform = {
                    currentDraftId = null
                    state = StudioState(mode = StudioMode.FREESTYLE)
                    stagedMedia = emptyList()
                    currentScreen = AppScreen.EDITOR
                    showMediaSheet = true
                },
                onOpenTemplateLibrary = {
                    currentDraftId = null
                    state = StudioState(mode = StudioMode.GRID)
                    currentScreen = AppScreen.EDITOR
                    showLayoutPicker = true
                },
                onSelectScrlTemplate = { applyScrlTemplate(it, resetProject = true) },
                onSelectGridTemplate = { layout ->
                    currentDraftId = null
                    state = StudioState(mode = StudioMode.GRID, layout = layout)
                    stagedMedia = emptyList()
                    currentScreen = AppScreen.EDITOR
                    showMediaSheet = true
                },
                onOpenDraft = { meta ->
                    val loaded = DraftStorage.loadDraft(context, meta.id)
                    if (loaded != null) {
                        currentDraftId = meta.id
                        state = loaded
                        stagedMedia = loaded.slots + loaded.extras
                        undoStack = emptyList()
                        redoStack = emptyList()
                        currentScreen = AppScreen.EDITOR
                    } else {
                        statusToast = "Could not open saved project"
                    }
                },
                onDuplicateDraft = { meta ->
                    DraftStorage.duplicateDraft(context, meta.id)
                    savedDrafts = DraftStorage.listDrafts(context)
                    statusToast = "Duplicated project"
                },
                onDeleteDraft = { meta ->
                    DraftStorage.deleteDraft(context, meta.id)
                    savedDrafts = DraftStorage.listDrafts(context)
                    statusToast = "Deleted project"
                }
            )
            return
        }

        AppScreen.SWIPE_PREVIEW -> {
            SwipePreviewScreen(
                state = state,
                onBackToEdit = { currentScreen = AppScreen.EDITOR },
                onOpenExport = {
                    currentScreen = AppScreen.EDITOR
                    showExportSheet = true
                }
            )
            return
        }

        AppScreen.EDITOR -> Unit
    }

    // =================== EDITOR SCREEN (CANVAS-FIRST) ===================
    val selectedMedia = state.slots.getOrNull(state.selectedSlotIndex)
    val selectedOverlay = state.overlays.find { it.id == state.selectedOverlayId }
    val isDrawingMode = state.activeTab == StudioTab.DRAWING && showToolDrawer

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 1. Minimal Studio Top Bar
        Row(
            Modifier
                .fillMaxWidth()
                .background(palette.panel)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = {
                    saveCurrentDraft()
                    currentScreen = AppScreen.HOME
                },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(Icons.Default.Home, "Home", tint = palette.textPrimary, modifier = Modifier.size(19.dp))
            }

            // Aspect Ratio Pill
            Box {
                Surface(
                    onClick = { showRatioMenu = true },
                    shape = RoundedCornerShape(6.dp),
                    color = palette.elevated,
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            state.exportConfig.aspectRatio.shortLabel,
                            color = palette.textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(Icons.Default.ArrowDropDown, null, tint = palette.textSecondary, modifier = Modifier.size(15.dp))
                    }
                }
                DropdownMenu(expanded = showRatioMenu, onDismissRequest = { showRatioMenu = false }) {
                    AspectRatioOption.entries.forEach { ratio ->
                        DropdownMenuItem(
                            text = { Text(ratio.label, fontSize = 12.sp) },
                            trailingIcon = {
                                if (state.exportConfig.aspectRatio == ratio) {
                                    Icon(Icons.Default.Check, null, tint = palette.accent, modifier = Modifier.size(16.dp))
                                }
                            },
                            onClick = {
                                showRatioMenu = false
                                pushState(state.copy(exportConfig = state.exportConfig.copy(aspectRatio = ratio)))
                                if (ratio == AspectRatioOption.CUSTOM) showExportSheet = true
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            IconButton(onClick = { undo() }, enabled = undoStack.isNotEmpty(), modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Undo,
                    "Undo",
                    tint = if (undoStack.isNotEmpty()) palette.textPrimary else palette.textSecondary.copy(alpha = 0.35f),
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = { redo() }, enabled = redoStack.isNotEmpty(), modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Redo,
                    "Redo",
                    tint = if (redoStack.isNotEmpty()) palette.textPrimary else palette.textSecondary.copy(alpha = 0.35f),
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = { saveCurrentDraft() }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Save, "Save", tint = palette.textSecondary, modifier = Modifier.size(18.dp))
            }
            if (state.mode == StudioMode.CAROUSEL) {
                IconButton(onClick = { currentScreen = AppScreen.SWIPE_PREVIEW }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Visibility, "Swipe Preview", tint = palette.textSecondary, modifier = Modifier.size(18.dp))
                }
            }
            Button(
                onClick = { showExportSheet = true },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = Color.White)
            ) {
                Text("Export", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        HorizontalDivider(color = palette.border, thickness = 1.dp)

        // 2. Canvas Viewport (Visual Priority)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            val ratio = state.exportConfig.aspectRatio.widthRatio.toFloat() /
                state.exportConfig.aspectRatio.heightRatio.toFloat().coerceAtLeast(0.1f)
            val canvasModifier = Modifier
                .fillMaxHeight()
                .aspectRatio(ratio.coerceIn(0.45f, 2.1f), matchHeightConstraintsFirst = true)

            BoxWithConstraints(
                modifier = canvasModifier
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, palette.border, RoundedCornerShape(4.dp))
                    .background(
                        if (state.customBgHex != null) Color(state.customBgHex!!)
                        else Color(state.backgroundStyle.startHex)
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            state = state.copy(selectedOverlayId = null, swapSourceIndex = null)
                        })
                    }
            ) {
                val canvasW = maxWidth
                val canvasH = maxHeight

                // Gradient / Blur Background
                if (state.customBgHex == null && state.backgroundStyle.startHex != state.backgroundStyle.endHex) {
                    Canvas(Modifier.fillMaxSize()) {
                        val rad = Math.toRadians(state.bgGradientAngleDeg.toDouble())
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = (cos(rad) * size.width / 2f).toFloat()
                        val dy = (sin(rad) * size.height / 2f).toFloat()
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(state.backgroundStyle.startHex), Color(state.backgroundStyle.endHex)),
                                start = Offset(cx - dx, cy - dy),
                                end = Offset(cx + dx, cy + dy)
                            )
                        )
                    }
                }

                // Grid or Freeform/Carousel Media Slots
                if (state.mode == StudioMode.GRID) {
                    val outer = state.outerMarginDp.dp
                    val gap = state.borderDp.dp
                    val innerW = (canvasW - outer * 2).coerceAtLeast(24.dp)
                    val innerH = (canvasH - outer * 2).coerceAtLeast(24.dp)
                    val rects = remember(state.layout) { state.layout.slotRects() }

                    rects.forEachIndexed { idx, rect ->
                        val item = state.slots.getOrNull(idx)
                        val isSelected = state.selectedOverlayId == null && state.selectedSlotIndex == idx
                        val isSwapSrc = state.swapSourceIndex == idx
                        val slotW = (innerW * rect.width - gap).coerceAtLeast(16.dp)
                        val slotH = (innerH * rect.height - gap).coerceAtLeast(16.dp)
                        val slotX = outer + innerW * rect.left + gap / 2
                        val slotY = outer + innerH * rect.top + gap / 2

                        Box(
                            modifier = Modifier
                                .offset(x = slotX, y = slotY)
                                .size(slotW, slotH)
                                .clip(RoundedCornerShape(state.cornerRadiusDp.dp))
                                .background(palette.elevated.copy(alpha = 0.45f))
                                .border(
                                    width = when {
                                        isSwapSrc -> 2.dp
                                        isSelected -> 1.5.dp
                                        state.borderColorHex != null -> 1.dp
                                        else -> 0.5.dp
                                    },
                                    color = when {
                                        isSwapSrc -> palette.coral
                                        isSelected -> palette.accent
                                        state.borderColorHex != null -> Color(state.borderColorHex!!)
                                        else -> palette.border.copy(alpha = 0.6f)
                                    },
                                    shape = RoundedCornerShape(state.cornerRadiusDp.dp)
                                )
                                .pointerInput(idx, state.swapSourceIndex, item?.locked, isDrawingMode) {
                                    if (isDrawingMode) return@pointerInput
                                    detectTapGestures(
                                        onTap = {
                                            val swapSrc = state.swapSourceIndex
                                            if (swapSrc != null && swapSrc != idx && item != null) {
                                                swapMediaSlots(swapSrc, idx)
                                            } else if (item == null) {
                                                stagedMedia = state.slots + state.extras
                                                showMediaSheet = true
                                            } else {
                                                state = state.copy(selectedSlotIndex = idx, selectedOverlayId = null, swapSourceIndex = null)
                                            }
                                        },
                                        onDoubleTap = {
                                            if (item != null) {
                                                state = state.copy(
                                                    selectedSlotIndex = idx,
                                                    selectedOverlayId = null,
                                                    activeTab = StudioTab.ADJUSTMENTS
                                                )
                                                showToolDrawer = true
                                            }
                                        },
                                        onLongPress = {
                                            if (item != null && state.slots.size > 1) {
                                                state = state.copy(selectedSlotIndex = idx, swapSourceIndex = idx)
                                                statusToast = "Tap another photo to swap"
                                            }
                                        }
                                    )
                                }
                                .pointerInput(idx, item?.locked, isDrawingMode) {
                                    if (isDrawingMode || item == null || item.locked) return@pointerInput
                                    detectTransformGestures { _, pan, zoom, rotation ->
                                        val cur = state.slots.getOrNull(idx) ?: return@detectTransformGestures
                                        val updated = state.slots.toMutableList()
                                        updated[idx] = cur.copy(
                                            scale = (cur.scale * zoom).coerceIn(0.5f, 4f),
                                            panX = (cur.panX + pan.x / 300f).coerceIn(-1f, 1f),
                                            panY = (cur.panY + pan.y / 300f).coerceIn(-1f, 1f),
                                            rotationDeg = cur.rotationDeg + rotation
                                        )
                                        state = state.copy(slots = updated, selectedSlotIndex = idx, selectedOverlayId = null)
                                    }
                                }
                        ) {
                            if (item != null && item.visible) {
                                EditorMediaTile(item = item, modifier = Modifier.fillMaxSize())
                            } else if (item == null) {
                                Icon(
                                    Icons.Default.Add,
                                    "Add Photo",
                                    tint = palette.textSecondary,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(22.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Freestyle & Carousel Layers
                    val sorted = state.slots.mapIndexed { idx, it -> idx to it }.filter { it.second.visible }.sortedBy { it.second.zIndex }
                    sorted.forEach { (idx, item) ->
                        val isSelected = state.selectedOverlayId == null && state.selectedSlotIndex == idx
                        val w = (canvasW * item.freeW).coerceAtLeast(44.dp)
                        val h = (canvasH * item.freeH).coerceAtLeast(44.dp)
                        val x = canvasW * item.freeX - w / 2
                        val y = canvasH * item.freeY - h / 2

                        Box(
                            Modifier
                                .offset(x = x, y = y)
                                .size(w, h)
                                .graphicsLayer { rotationZ = item.rotationDeg; alpha = item.opacity }
                                .clip(RoundedCornerShape(state.cornerRadiusDp.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                    color = if (isSelected) palette.accent else Color.Transparent,
                                    shape = RoundedCornerShape(state.cornerRadiusDp.dp)
                                )
                                .pointerInput(idx, item.locked, isDrawingMode) {
                                    if (isDrawingMode) return@pointerInput
                                    detectTapGestures(
                                        onTap = { state = state.copy(selectedSlotIndex = idx, selectedOverlayId = null) },
                                        onDoubleTap = {
                                            state = state.copy(selectedSlotIndex = idx, selectedOverlayId = null, activeTab = StudioTab.ADJUSTMENTS)
                                            showToolDrawer = true
                                        }
                                    )
                                }
                                .pointerInput(idx, item.locked, isDrawingMode) {
                                    if (isDrawingMode || item.locked) return@pointerInput
                                    detectTransformGestures { _, pan, zoom, rotation ->
                                        val cur = state.slots.getOrNull(idx) ?: return@detectTransformGestures
                                        val updated = state.slots.toMutableList()
                                        updated[idx] = cur.copy(
                                            freeX = (cur.freeX + pan.x / canvasW.toPx()).coerceIn(0.05f, 0.95f),
                                            freeY = (cur.freeY + pan.y / canvasH.toPx()).coerceIn(0.05f, 0.95f),
                                            freeW = (cur.freeW * zoom).coerceIn(0.15f, 1.2f),
                                            freeH = (cur.freeH * zoom).coerceIn(0.15f, 1.2f),
                                            rotationDeg = cur.rotationDeg + rotation
                                        )
                                        state = state.copy(slots = updated, selectedSlotIndex = idx, selectedOverlayId = null)
                                    }
                                }
                        ) {
                            EditorMediaTile(item = item, modifier = Modifier.fillMaxSize())
                        }
                    }
                }

                // Collage Frame Overlay
                if (state.frameStyle != CollageFrameStyle.NONE) {
                    Canvas(Modifier.fillMaxSize()) {
                        when (state.frameStyle) {
                            CollageFrameStyle.MINIMAL -> drawRect(Color.White, style = Stroke(4.dp.toPx()))
                            CollageFrameStyle.DOUBLE_LINE -> {
                                drawRect(Color.White, style = Stroke(3.dp.toPx()))
                                val inset = 6.dp.toPx()
                                drawRect(
                                    Color.White.copy(alpha = 0.75f),
                                    topLeft = Offset(inset, inset),
                                    size = Size(size.width - inset * 2, size.height - inset * 2),
                                    style = Stroke(1.dp.toPx())
                                )
                            }
                            CollageFrameStyle.POLAROID -> {
                                val side = 8.dp.toPx()
                                val bottom = 26.dp.toPx()
                                val col = Color(0xFFF7F5F0)
                                drawRect(col, size = Size(size.width, side))
                                drawRect(col, size = Size(side, size.height))
                                drawRect(col, topLeft = Offset(size.width - side, 0f), size = Size(side, size.height))
                                drawRect(col, topLeft = Offset(0f, size.height - bottom), size = Size(size.width, bottom))
                            }
                            CollageFrameStyle.FILM -> {
                                val barW = 10.dp.toPx()
                                drawRect(Color(0xFF111317), size = Size(barW, size.height))
                                drawRect(Color(0xFF111317), topLeft = Offset(size.width - barW, 0f), size = Size(barW, size.height))
                                var y = 6.dp.toPx()
                                while (y < size.height - 6.dp.toPx()) {
                                    drawRoundRect(Color(0xFFE5E7EB), Offset(2.5.dp.toPx(), y), Size(5.dp.toPx(), 6.dp.toPx()), CornerRadius(1.dp.toPx()))
                                    drawRoundRect(Color(0xFFE5E7EB), Offset(size.width - 7.5.dp.toPx(), y), Size(5.dp.toPx(), 6.dp.toPx()), CornerRadius(1.dp.toPx()))
                                    y += 14.dp.toPx()
                                }
                            }
                            CollageFrameStyle.SHADOW_CARD -> drawRect(Color.Black.copy(alpha = 0.45f), style = Stroke(8.dp.toPx()))
                            CollageFrameStyle.TORN_PAPER -> drawRect(
                                Color(0xFFF4EFE6),
                                style = Stroke(6.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 5f, 8f, 4f)))
                            )
                            CollageFrameStyle.WASHI_TAPE -> {
                                val tape = Color(0xBFE8D5B7)
                                val tw = 28.dp.toPx()
                                val th = 9.dp.toPx()
                                drawRect(tape, Offset(4.dp.toPx(), 4.dp.toPx()), Size(tw, th))
                                drawRect(tape, Offset(size.width - tw - 4.dp.toPx(), 4.dp.toPx()), Size(tw, th))
                                drawRect(tape, Offset(4.dp.toPx(), size.height - th - 4.dp.toPx()), Size(tw, th))
                                drawRect(tape, Offset(size.width - tw - 4.dp.toPx(), size.height - th - 4.dp.toPx()), Size(tw, th))
                            }
                            CollageFrameStyle.NONE -> Unit
                        }
                    }
                }

                // Drawing Strokes Layer
                if (state.drawingsVisible) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (isDrawingMode && !state.drawingsLocked) {
                                    Modifier.pointerInput(brushColorHex, brushWidthDp, brushOpacity) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                val pt = DrawingPoint(
                                                    (offset.x / size.width).coerceIn(0f, 1f),
                                                    (offset.y / size.height).coerceIn(0f, 1f)
                                                )
                                                activeStrokePoints = listOf(pt)
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                val pt = DrawingPoint(
                                                    (change.position.x / size.width).coerceIn(0f, 1f),
                                                    (change.position.y / size.height).coerceIn(0f, 1f)
                                                )
                                                activeStrokePoints = activeStrokePoints + pt
                                            },
                                            onDragEnd = {
                                                if (activeStrokePoints.size >= 2) {
                                                    val newStroke = DrawingStroke(
                                                        id = UUID.randomUUID().toString(),
                                                        points = activeStrokePoints,
                                                        colorHex = brushColorHex,
                                                        strokeWidthDp = brushWidthDp,
                                                        opacity = brushOpacity
                                                    )
                                                    pushState(state.copy(drawingStrokes = state.drawingStrokes + newStroke))
                                                }
                                                activeStrokePoints = emptyList()
                                            },
                                            onDragCancel = { activeStrokePoints = emptyList() }
                                        )
                                    }
                                } else Modifier
                            )
                    ) {
                        val allStrokes = if (activeStrokePoints.size >= 2) {
                            state.drawingStrokes + DrawingStroke("active", activeStrokePoints, brushColorHex, brushWidthDp, brushOpacity)
                        } else state.drawingStrokes

                        allStrokes.forEach { stroke ->
                            if (stroke.points.size < 2) return@forEach
                            val path = Path()
                            stroke.points.forEachIndexed { idx, pt ->
                                val px = pt.x * size.width
                                val py = pt.y * size.height
                                if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                            }
                            drawPath(
                                path = path,
                                color = Color(stroke.colorHex).copy(alpha = stroke.opacity),
                                style = Stroke(
                                    width = stroke.strokeWidthDp.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }

                // Text & Sticker Overlays
                state.overlays.filter { it.visible }.sortedBy { it.zIndex }.forEach { overlay ->
                    val isSel = state.selectedOverlayId == overlay.id
                    Box(
                        modifier = Modifier
                            .offset(
                                x = canvasW * overlay.xFraction - 60.dp,
                                y = canvasH * overlay.yFraction - 24.dp
                            )
                            .graphicsLayer {
                                rotationZ = overlay.rotationDeg
                                alpha = overlay.opacity
                            }
                            .border(
                                width = if (isSel) 1.dp else 0.dp,
                                color = if (isSel) palette.accent else Color.Transparent,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .pointerInput(overlay.id, overlay.locked, isDrawingMode) {
                                if (isDrawingMode) return@pointerInput
                                detectTapGestures(
                                    onTap = {
                                        state = state.copy(selectedOverlayId = overlay.id)
                                        if (!overlay.isSticker) textInput = overlay.text
                                    },
                                    onDoubleTap = {
                                        state = state.copy(
                                            selectedOverlayId = overlay.id,
                                            activeTab = if (overlay.isSticker) StudioTab.STICKERS else StudioTab.TEXT
                                        )
                                        if (!overlay.isSticker) textInput = overlay.text
                                        showToolDrawer = true
                                    }
                                )
                            }
                            .pointerInput(overlay.id, overlay.locked, isDrawingMode) {
                                if (isDrawingMode || overlay.locked) return@pointerInput
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    updateOverlayById(overlay.id, recordHistory = false) { cur ->
                                        cur.copy(
                                            xFraction = (cur.xFraction + dragAmount.x / canvasW.toPx()).coerceIn(0.05f, 0.95f),
                                            yFraction = (cur.yFraction + dragAmount.y / canvasH.toPx()).coerceIn(0.05f, 0.95f)
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
                                modifier = Modifier.size((canvasH * overlay.stickerSizeFraction).coerceIn(36.dp, 180.dp))
                            )
                        } else {
                            val ff = resolveComposeFontFamily(overlay)
                            val fs = if (overlay.fontStyle == TextFontStyle.ITALIC) FontStyle.Italic else FontStyle.Normal
                            val ta = when (overlay.alignment) {
                                TextAlignOption.LEFT -> TextAlign.Start
                                TextAlignOption.CENTER -> TextAlign.Center
                                TextAlignOption.RIGHT -> TextAlign.End
                            }
                            val bgMod = if (overlay.usePill && !overlay.isSticker) {
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(overlay.bgColorHex))
                                    .padding(horizontal = overlay.paddingDp.dp, vertical = (overlay.paddingDp * 0.6f).dp)
                            } else Modifier.padding(4.dp)

                            val textShadow = when {
                                overlay.useShadow -> Shadow(
                                    color = Color(overlay.shadowColorHex),
                                    offset = Offset(0f, 3f),
                                    blurRadius = overlay.shadowRadiusDp * 2f
                                )
                                overlay.useOutline -> Shadow(
                                    color = Color(overlay.outlineColorHex),
                                    offset = Offset(0f, 0f),
                                    blurRadius = overlay.outlineWidthDp * 2f
                                )
                                else -> null
                            }
                            val baseStyle = (if (overlay.useGradient) {
                                TextStyle(
                                    brush = Brush.linearGradient(listOf(Color(overlay.textColorHex), Color(overlay.gradientEndHex))),
                                    fontSize = overlay.fontSizeSp.sp,
                                    fontFamily = ff,
                                    fontStyle = fs,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = ta,
                                    lineHeight = (overlay.fontSizeSp * overlay.lineHeightMultiplier).sp,
                                    shadow = textShadow
                                )
                            } else {
                                TextStyle(
                                    color = Color(overlay.textColorHex),
                                    fontSize = overlay.fontSizeSp.sp,
                                    fontFamily = ff,
                                    fontStyle = fs,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = ta,
                                    lineHeight = (overlay.fontSizeSp * overlay.lineHeightMultiplier).sp,
                                    shadow = textShadow
                                )
                            }).let { st ->
                                // Preserve Malayalam conjuncts/ligatures when letterSpacingEm == 0
                                if (overlay.letterSpacingEm > 0.001f) st.copy(letterSpacing = overlay.letterSpacingEm.em) else st
                            }

                            Box(bgMod) {
                                Text(text = overlay.text, style = baseStyle)
                            }
                        }
                    }
                }
            }
        }

        // 3. Contextual Editing Controls Strip (adapts to Image / Text / Background selection)
        Row(
            Modifier
                .fillMaxWidth()
                .background(palette.panel)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                selectedOverlay != null -> {
                    ContextChip(Icons.Default.Edit, if (selectedOverlay.isSticker) "Style" else "Edit Text") {
                        if (!selectedOverlay.isSticker) textInput = selectedOverlay.text
                        state = state.copy(activeTab = if (selectedOverlay.isSticker) StudioTab.STICKERS else StudioTab.TEXT)
                        showToolDrawer = true
                    }
                    ContextChip(
                        if (selectedOverlay.locked) Icons.Default.Lock else Icons.Default.LockOpen,
                        if (selectedOverlay.locked) "Unlock" else "Lock"
                    ) {
                        updateOverlayById(selectedOverlay.id) { it.copy(locked = !it.locked) }
                    }
                    ContextChip(Icons.Default.ContentCopy, "Duplicate") {
                        val copy = selectedOverlay.copy(
                            id = UUID.randomUUID().toString(),
                            xFraction = (selectedOverlay.xFraction + 0.05f).coerceAtMost(0.9f),
                            yFraction = (selectedOverlay.yFraction + 0.05f).coerceAtMost(0.9f),
                            zIndex = (state.overlays.maxOfOrNull { it.zIndex } ?: 100) + 1
                        )
                        pushState(state.copy(overlays = state.overlays + copy, selectedOverlayId = copy.id))
                    }
                    ContextChip(Icons.Default.DeleteOutline, "Delete") {
                        pushState(state.copy(overlays = state.overlays.filterNot { it.id == selectedOverlay.id }, selectedOverlayId = null))
                    }
                    ContextChip(Icons.Default.Layers, "Layers") {
                        state = state.copy(activeTab = StudioTab.LAYERS)
                        showToolDrawer = true
                    }
                }

                selectedMedia != null -> {
                    ContextChip(Icons.Default.PhotoCameraBack, "Replace") {
                        replaceTargetIdx = state.selectedSlotIndex
                        replaceMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                    }
                    ContextChip(
                        if (selectedMedia.fitInside) Icons.Default.CropFree else Icons.Default.AspectRatio,
                        if (selectedMedia.fitInside) "Fill" else "Fit"
                    ) {
                        updateSelectedMedia { it.copy(fitInside = !it.fitInside) }
                    }
                    ContextChip(
                        Icons.Default.SwapHoriz,
                        if (state.swapSourceIndex != null) "Cancel Swap" else "Swap",
                        highlighted = state.swapSourceIndex != null
                    ) {
                        state = state.copy(
                            swapSourceIndex = if (state.swapSourceIndex == null) state.selectedSlotIndex else null
                        )
                        if (state.swapSourceIndex != null) statusToast = "Tap another photo cell to swap"
                    }
                    ContextChip(Icons.Default.RotateRight, "Rotate") {
                        updateSelectedMedia { it.copy(rotationDeg = (it.rotationDeg + 90f) % 360f) }
                    }
                    ContextChip(Icons.Default.Flip, "Flip") {
                        updateSelectedMedia { it.copy(flipHorizontal = !it.flipHorizontal) }
                    }
                    ContextChip(Icons.Default.ZoomIn, "Zoom +") {
                        updateSelectedMedia { it.copy(scale = (it.scale * 1.2f).coerceIn(0.5f, 4f)) }
                    }
                    ContextChip(Icons.Default.FilterCenterFocus, "Center") {
                        updateSelectedMedia { it.copy(panX = 0f, panY = 0f) }
                    }
                    ContextChip(Icons.Default.RestartAlt, "Reset") {
                        updateSelectedMedia { it.copy(scale = 1f, panX = 0f, panY = 0f, rotationDeg = 0f, flipHorizontal = false, fitInside = false) }
                    }
                    ContextChip(Icons.Default.Tune, "Adjust") {
                        state = state.copy(activeTab = StudioTab.ADJUSTMENTS)
                        showToolDrawer = true
                    }
                }

                else -> {
                    ContextChip(Icons.Default.AddPhotoAlternate, "Select Media") {
                        stagedMedia = state.slots + state.extras
                        showMediaSheet = true
                    }
                    ContextChip(Icons.Default.GridView, "Layouts") { showLayoutPicker = true }
                    ContextChip(Icons.Default.Wallpaper, "Background") {
                        state = state.copy(activeTab = StudioTab.BACKGROUND)
                        showToolDrawer = true
                    }
                    ContextChip(Icons.Default.FilterFrames, "Frames") {
                        state = state.copy(activeTab = StudioTab.FRAMES)
                        showToolDrawer = true
                    }
                }
            }
        }

        // 4. Extras Tray (photos selected but not currently used in layout)
        if (state.extras.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(palette.cardPaper)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column {
                    Text("Extras · ${state.extras.size}", color = palette.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Tap to place", color = palette.textSecondary, fontSize = 9.sp)
                }
                LazyRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(state.extras, key = { _, it -> it.id }) { extraIdx, extraItem ->
                        Box(
                            Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, palette.border, RoundedCornerShape(6.dp))
                                .clickable {
                                    val targetIdx = state.selectedSlotIndex.coerceIn(0, (state.slots.size - 1).coerceAtLeast(0))
                                    val newSlots = state.slots.toMutableList()
                                    val newExtras = state.extras.toMutableList()
                                    if (targetIdx in newSlots.indices) {
                                        val displaced = newSlots[targetIdx]
                                        newSlots[targetIdx] = extraItem
                                        newExtras[extraIdx] = displaced
                                    } else {
                                        newSlots.add(extraItem)
                                        newExtras.removeAt(extraIdx)
                                    }
                                    pushState(state.copy(slots = newSlots, extras = newExtras))
                                    stagedMedia = newSlots + newExtras
                                }
                        ) {
                            AsyncImage(
                                model = extraItem.uri,
                                contentDescription = "Extra photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                onClick = {
                                    val nextExtras = state.extras.toMutableList().also { it.removeAt(extraIdx) }
                                    pushState(state.copy(extras = nextExtras))
                                    stagedMedia = state.slots + nextExtras
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(16.dp),
                                shape = CircleShape,
                                color = Color(0xCC0D0F14)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Close, "Remove extra", tint = Color.White, modifier = Modifier.size(10.dp))
                                }
                            }
                        }
                    }
                }
                TextButton(
                    onClick = {
                        pushState(state.copy(extras = emptyList()))
                        stagedMedia = state.slots
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Text("Clear", color = palette.danger, fontSize = 11.sp)
                }
            }
        }

        // 5. Collapsible Contextual Tool Drawer
        if (showToolDrawer) {
            HorizontalDivider(color = palette.border, thickness = 1.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(palette.panel)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(state.activeTab.label, color = palette.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showToolDrawer = false }, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.KeyboardArrowDown, "Collapse", tint = palette.textSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                when (state.activeTab) {
                    StudioTab.LAYOUT -> {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StudioMode.entries.forEach { m ->
                                FilterChip(
                                    selected = state.mode == m,
                                    onClick = { pushState(state.copy(mode = m)) },
                                    label = { Text(m.title, fontSize = 11.sp) }
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            Button(
                                onClick = { showLayoutPicker = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = Color.White)
                            ) {
                                Text("Browse 200+", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        CompactSlider("Spacing", "${state.borderDp}dp", state.borderDp.toFloat(), 0f..24f) {
                            pushState(state.copy(borderDp = it.toInt()), recordHistory = false)
                        }
                        CompactSlider("Margin", "${state.outerMarginDp}dp", state.outerMarginDp.toFloat(), 0f..32f) {
                            pushState(state.copy(outerMarginDp = it.toInt()), recordHistory = false)
                        }
                        CompactSlider("Corners", "${state.cornerRadiusDp}dp", state.cornerRadiusDp.toFloat(), 0f..32f) {
                            pushState(state.copy(cornerRadiusDp = it.toInt()), recordHistory = false)
                        }
                    }

                    StudioTab.PHOTOS -> {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    stagedMedia = state.slots + state.extras
                                    showMediaSheet = true
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Manage Media (${state.slots.size + state.extras.size})", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    if (state.slots.isNotEmpty()) {
                                        replaceTargetIdx = state.selectedSlotIndex
                                        replaceMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, palette.border)
                            ) {
                                Text("Replace", fontSize = 12.sp)
                            }
                        }
                        if (selectedMedia != null) {
                            CompactSlider("Zoom", "${String.format("%.1f", selectedMedia.scale)}x", selectedMedia.scale, 0.5f..3.5f) { v ->
                                updateSelectedMedia(recordHistory = false) { it.copy(scale = v) }
                            }
                            CompactSlider("Opacity", "${(selectedMedia.opacity * 100).toInt()}%", selectedMedia.opacity, 0.1f..1f) { v ->
                                updateSelectedMedia(recordHistory = false) { it.copy(opacity = v) }
                            }
                        }
                    }

                    StudioTab.TEXT -> {
                        val selText = state.overlays.find { it.id == state.selectedOverlayId && !it.isSticker }
                        TypographyPanelContent(
                            selectedText = selText,
                            textInput = textInput,
                            onTextInputChange = { textInput = it },
                            importedFonts = importedFonts,
                            onImportFontClick = { importFontLauncher.launch(arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/octet-stream", "*/*")) },
                            onAddOrUpdateText = {
                                val clean = textInput.trim()
                                if (clean.isEmpty()) return@TypographyPanelContent
                                if (selText != null) {
                                    updateOverlayById(selText.id) { it.copy(text = clean) }
                                } else {
                                    val newText = TextOverlay(
                                        id = UUID.randomUUID().toString(),
                                        text = clean,
                                        zIndex = (state.overlays.maxOfOrNull { it.zIndex } ?: 100) + 1
                                    )
                                    pushState(state.copy(overlays = state.overlays + newText, selectedOverlayId = newText.id))
                                }
                            },
                            onUpdateSelectedText = { transform ->
                                selText?.let { updateOverlayById(it.id, recordHistory = false, transform = transform) }
                            }
                        )
                    }

                    StudioTab.STICKERS -> {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { stickerImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, palette.border)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("PNG Sticker", fontSize = 11.sp)
                            }
                            LazyRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(StickerCatalog) { emoji ->
                                    Surface(
                                        onClick = {
                                            val newSt = TextOverlay(
                                                id = UUID.randomUUID().toString(),
                                                text = emoji,
                                                fontSizeSp = 36f,
                                                usePill = false,
                                                useOutline = false,
                                                isSticker = true,
                                                zIndex = (state.overlays.maxOfOrNull { it.zIndex } ?: 100) + 1
                                            )
                                            pushState(state.copy(overlays = state.overlays + newSt, selectedOverlayId = newSt.id))
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = palette.cardPaper,
                                        border = BorderStroke(1.dp, palette.border)
                                    ) {
                                        Text(emoji, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                                    }
                                }
                            }
                        }
                        val selSticker = state.overlays.find { it.id == state.selectedOverlayId && it.isSticker }
                        if (selSticker != null) {
                            CompactSlider(
                                "Size",
                                if (selSticker.stickerUri != null) "${(selSticker.stickerSizeFraction * 100).toInt()}%" else "${selSticker.fontSizeSp.toInt()}sp",
                                if (selSticker.stickerUri != null) selSticker.stickerSizeFraction else selSticker.fontSizeSp,
                                if (selSticker.stickerUri != null) 0.1f..0.6f else 18f..80f
                            ) { v ->
                                updateOverlayById(selSticker.id, recordHistory = false) {
                                    if (it.stickerUri != null) it.copy(stickerSizeFraction = v) else it.copy(fontSizeSp = v)
                                }
                            }
                            CompactSlider("Opacity", "${(selSticker.opacity * 100).toInt()}%", selSticker.opacity, 0.15f..1f) { v ->
                                updateOverlayById(selSticker.id, recordHistory = false) { it.copy(opacity = v) }
                            }
                        }
                    }

                    StudioTab.FRAMES -> {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(CollageFrameStyle.entries.toList()) { frame ->
                                FilterChip(
                                    selected = state.frameStyle == frame,
                                    onClick = { pushState(state.copy(frameStyle = frame)) },
                                    label = { Text(frame.label, fontSize = 11.sp) }
                                )
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Border", color = palette.textSecondary, fontSize = 11.sp)
                            BorderSwatches.forEach { hex ->
                                val active = state.borderColorHex == hex
                                Box(
                                    Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (hex != null) Color(hex) else palette.canvas)
                                        .border(if (active) 2.dp else 1.dp, if (active) palette.accent else palette.border, CircleShape)
                                        .clickable { pushState(state.copy(borderColorHex = hex)) }
                                )
                            }
                        }
                    }

                    StudioTab.BACKGROUND -> {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(BackgroundStyle.entries.toList()) { bg ->
                                val active = state.customBgHex == null && state.backgroundStyle == bg
                                Surface(
                                    onClick = { pushState(state.copy(backgroundStyle = bg, customBgHex = null)) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(bg.startHex),
                                    border = BorderStroke(if (active) 2.dp else 1.dp, if (active) palette.accent else palette.border)
                                ) {
                                    Text(
                                        bg.label,
                                        color = if (bg == BackgroundStyle.PURE_WHITE) Color.Black else Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                        if (state.backgroundStyle.startHex != state.backgroundStyle.endHex) {
                            CompactSlider("Angle", "${state.bgGradientAngleDeg.toInt()}°", state.bgGradientAngleDeg, 0f..360f) { v ->
                                pushState(state.copy(bgGradientAngleDeg = v), recordHistory = false)
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(CanvasTexture.entries.toList()) { tex ->
                                FilterChip(
                                    selected = state.canvasTexture == tex,
                                    onClick = { pushState(state.copy(canvasTexture = tex)) },
                                    label = { Text(tex.label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    StudioTab.DRAWING -> {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BrushColors.forEach { hex ->
                                val active = brushColorHex == hex
                                Box(
                                    Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(hex))
                                        .border(if (active) 2.dp else 1.dp, if (active) palette.accent else palette.border, CircleShape)
                                        .clickable { brushColorHex = hex }
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            IconButton(
                                onClick = {
                                    if (state.drawingStrokes.isNotEmpty()) {
                                        pushState(state.copy(drawingStrokes = state.drawingStrokes.dropLast(1)))
                                    }
                                },
                                enabled = state.drawingStrokes.isNotEmpty(),
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.Undo, "Undo Stroke", tint = palette.textPrimary, modifier = Modifier.size(16.dp))
                            }
                            TextButton(
                                onClick = { pushState(state.copy(drawingStrokes = emptyList())) },
                                enabled = state.drawingStrokes.isNotEmpty()
                            ) {
                                Text("Clear", color = palette.danger, fontSize = 11.sp)
                            }
                        }
                        CompactSlider("Brush Size", "${brushWidthDp.toInt()}dp", brushWidthDp, 1f..20f) { brushWidthDp = it }
                        CompactSlider("Opacity", "${(brushOpacity * 100).toInt()}%", brushOpacity, 0.1f..1f) { brushOpacity = it }
                        Text("Draw directly on the canvas above.", color = palette.textSecondary, fontSize = 10.sp)
                    }

                    StudioTab.FILTERS -> {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Filter Presets", color = palette.textSecondary, fontSize = 11.sp)
                            if (selectedMedia != null && state.slots.size > 1) {
                                TextButton(onClick = {
                                    val updated = state.slots.map {
                                        it.copy(
                                            brightness = selectedMedia.brightness,
                                            contrast = selectedMedia.contrast,
                                            saturation = selectedMedia.saturation,
                                            warmth = selectedMedia.warmth,
                                            vignette = selectedMedia.vignette
                                        )
                                    }
                                    pushState(state.copy(slots = updated))
                                    statusToast = "Applied filter to all photos"
                                }) {
                                    Text("Apply to All", fontSize = 11.sp, color = palette.accent)
                                }
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(FilterPreset.entries.toList()) { preset ->
                                FilterChip(
                                    selected = selectedMedia?.contrast == preset.contrast && selectedMedia.saturation == preset.saturation,
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
                        }
                    }

                    StudioTab.ADJUSTMENTS -> {
                        if (selectedMedia == null) {
                            Text("Select a photo on the canvas to adjust.", color = palette.textSecondary, fontSize = 11.sp)
                        } else {
                            CompactSlider("Brightness", "${(selectedMedia.brightness * 100).toInt()}", selectedMedia.brightness, -0.5f..0.5f) { v ->
                                updateSelectedMedia(recordHistory = false) { it.copy(brightness = v) }
                            }
                            CompactSlider("Contrast", "${String.format("%.2f", selectedMedia.contrast)}", selectedMedia.contrast, 0.5f..1.8f) { v ->
                                updateSelectedMedia(recordHistory = false) { it.copy(contrast = v) }
                            }
                            CompactSlider("Saturation", "${String.format("%.2f", selectedMedia.saturation)}", selectedMedia.saturation, 0f..2f) { v ->
                                updateSelectedMedia(recordHistory = false) { it.copy(saturation = v) }
                            }
                            CompactSlider("Warmth", "${(selectedMedia.warmth * 100).toInt()}", selectedMedia.warmth, -0.5f..0.5f) { v ->
                                updateSelectedMedia(recordHistory = false) { it.copy(warmth = v) }
                            }
                        }
                    }

                    StudioTab.LAYERS -> {
                        LayerPanelContent(
                            state = state,
                            onSelectMedia = { idx -> state = state.copy(selectedSlotIndex = idx, selectedOverlayId = null) },
                            onToggleMediaVisible = { idx ->
                                val list = state.slots.toMutableList()
                                list[idx] = list[idx].copy(visible = !list[idx].visible)
                                pushState(state.copy(slots = list))
                            },
                            onToggleMediaLock = { idx ->
                                val list = state.slots.toMutableList()
                                list[idx] = list[idx].copy(locked = !list[idx].locked)
                                pushState(state.copy(slots = list))
                            },
                            onMoveMediaZ = { idx, delta ->
                                val list = state.slots.toMutableList()
                                list[idx] = list[idx].copy(zIndex = list[idx].zIndex + delta)
                                pushState(state.copy(slots = list))
                            },
                            onDuplicateMedia = { idx ->
                                val src = state.slots[idx]
                                val copy = src.copy(
                                    id = UUID.randomUUID().toString(),
                                    freeX = (src.freeX + 0.05f).coerceAtMost(0.9f),
                                    freeY = (src.freeY + 0.05f).coerceAtMost(0.9f),
                                    zIndex = (state.slots.maxOfOrNull { it.zIndex } ?: 0) + 1
                                )
                                pushState(state.copy(slots = state.slots + copy, selectedSlotIndex = state.slots.size))
                            },
                            onDeleteMedia = { idx ->
                                val rem = state.slots.toMutableList().also { it.removeAt(idx) }
                                val remExtras = state.extras.toMutableList()
                                if (rem.size < state.maxCapacity() && remExtras.isNotEmpty()) {
                                    rem.add(remExtras.removeAt(0))
                                }
                                pushState(state.copy(slots = rem, extras = remExtras, selectedSlotIndex = 0))
                                stagedMedia = rem + remExtras
                            },
                            onSelectOverlay = { id ->
                                state = state.copy(selectedOverlayId = id)
                                state.overlays.find { it.id == id && !it.isSticker }?.let { textInput = it.text }
                            },
                            onToggleOverlayVisible = { id -> updateOverlayById(id) { it.copy(visible = !it.visible) } },
                            onToggleOverlayLock = { id -> updateOverlayById(id) { it.copy(locked = !it.locked) } },
                            onMoveOverlayZ = { id, delta -> updateOverlayById(id) { it.copy(zIndex = it.zIndex + delta) } },
                            onDuplicateOverlay = { id ->
                                val src = state.overlays.find { it.id == id } ?: return@LayerPanelContent
                                val copy = src.copy(
                                    id = UUID.randomUUID().toString(),
                                    xFraction = (src.xFraction + 0.05f).coerceAtMost(0.9f),
                                    yFraction = (src.yFraction + 0.05f).coerceAtMost(0.9f),
                                    zIndex = (state.overlays.maxOfOrNull { it.zIndex } ?: 100) + 1
                                )
                                pushState(state.copy(overlays = state.overlays + copy, selectedOverlayId = copy.id))
                            },
                            onDeleteOverlay = { id ->
                                pushState(state.copy(overlays = state.overlays.filterNot { it.id == id }, selectedOverlayId = null))
                            },
                            onToggleDrawingsVisible = { pushState(state.copy(drawingsVisible = !state.drawingsVisible)) },
                            onToggleDrawingsLock = { pushState(state.copy(drawingsLocked = !state.drawingsLocked)) },
                            onClearDrawings = { pushState(state.copy(drawingStrokes = emptyList())) }
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = palette.border, thickness = 1.dp)

        // 6. Organized 10-Category Studio Bottom Toolbar
        Row(
            Modifier
                .fillMaxWidth()
                .background(palette.panel)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StudioTab.entries.forEach { tab ->
                val active = state.activeTab == tab && showToolDrawer
                Surface(
                    onClick = {
                        if (state.activeTab == tab && showToolDrawer) {
                            showToolDrawer = false
                        } else {
                            state = state.copy(activeTab = tab)
                            showToolDrawer = true
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = if (active) palette.elevated else Color.Transparent,
                    border = if (active) BorderStroke(1.dp, palette.accent.copy(alpha = 0.7f)) else null
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            iconForTab(tab),
                            contentDescription = tab.label,
                            tint = if (active) palette.accent else palette.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            tab.label,
                            color = if (active) palette.textPrimary else palette.textSecondary,
                            fontSize = 10.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }

    // =================== MODAL SHEETS ===================
    if (showMediaSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMediaSheet = false },
            containerColor = palette.panel
        ) {
            MediaSelectionSheetContent(
                selectedMedia = stagedMedia,
                onPickPhotos = { pickPhotosLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onPickVideos = { pickPhotosLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
                onPickPhotosAndVideos = { pickPhotosLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                onRemoveAt = { idx ->
                    if (idx in stagedMedia.indices) stagedMedia = stagedMedia.toMutableList().also { it.removeAt(idx) }
                },
                onMoveItem = { from, to ->
                    if (from in stagedMedia.indices && to in stagedMedia.indices) {
                        val list = stagedMedia.toMutableList()
                        val item = list.removeAt(from)
                        list.add(to, item)
                        stagedMedia = list
                    }
                },
                onClearAll = { stagedMedia = emptyList() },
                onChooseLayout = {
                    applyMediaListToState(stagedMedia)
                    showMediaSheet = false
                    showLayoutPicker = true
                },
                onConfirmSelection = {
                    applyMediaListToState(stagedMedia)
                    showMediaSheet = false
                }
            )
        }
    }

    if (showLayoutPicker) {
        ModalBottomSheet(
            onDismissRequest = { showLayoutPicker = false },
            containerColor = palette.panel
        ) {
            val allMedia = if (stagedMedia.isNotEmpty()) stagedMedia else (state.slots + state.extras)
            LayoutPickerSheetContent(
                currentLayout = state.layout,
                selectedMedia = allMedia,
                favoriteLayouts = favoriteLayouts,
                onToggleFavorite = { layout ->
                    favoriteLayouts = DraftStorage.toggleFavoriteLayout(context, layout.name)
                },
                onApplyLayout = { layout ->
                    if (stagedMedia.isNotEmpty() && state.slots.isEmpty()) {
                        applyMediaListToState(stagedMedia, targetLayout = layout)
                    } else {
                        applyLayoutPreservingMedia(layout)
                    }
                    showLayoutPicker = false
                },
                onOpenMediaSelection = {
                    stagedMedia = allMedia
                    showLayoutPicker = false
                    showMediaSheet = true
                }
            )
        }
    }

    if (showExportSheet) {
        ModalBottomSheet(
            onDismissRequest = { if (!isExporting) showExportSheet = false },
            containerColor = palette.panel
        ) {
            ExportSheetContent(
                state = state,
                isExporting = isExporting,
                exportProgress = exportProgress,
                exportStage = exportStage,
                lastResult = lastExportResult,
                onConfigChange = { nextCfg ->
                    pushState(state.copy(exportConfig = nextCfg), recordHistory = false)
                    lastExportResult = null
                },
                onExportGallery = {
                    scope.launch {
                        isExporting = true
                        exportProgress = 0f
                        exportStage = "Starting…"
                        val res = withContext(Dispatchers.IO) {
                            CollageExporter.exportStudio(context, state, asZip = false) { p, msg ->
                                exportProgress = p
                                exportStage = msg
                            }
                        }
                        isExporting = false
                        lastExportResult = res
                    }
                },
                onExportZip = {
                    scope.launch {
                        isExporting = true
                        exportProgress = 0f
                        exportStage = "Starting ZIP…"
                        val res = withContext(Dispatchers.IO) {
                            CollageExporter.exportStudio(context, state, asZip = true) { p, msg ->
                                exportProgress = p
                                exportStage = msg
                            }
                        }
                        isExporting = false
                        lastExportResult = res
                    }
                },
                onShare = {
                    scope.launch {
                        val existing = lastExportResult?.takeIf { it.uris.isNotEmpty() }
                        val res = existing ?: run {
                            isExporting = true
                            exportProgress = 0f
                            exportStage = "Preparing to share…"
                            val created = withContext(Dispatchers.IO) {
                                CollageExporter.exportStudio(context, state, asZip = false) { p, msg ->
                                    exportProgress = p
                                    exportStage = msg
                                }
                            }
                            isExporting = false
                            lastExportResult = created
                            created
                        }
                        if (res.uris.isNotEmpty()) {
                            CollageExporter.shareUris(context, res.uris, state.exportConfig.format.mimeType)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun ContextChip(
    icon: ImageVector,
    label: String,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    val palette = LocalScrollGridPalette.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = if (highlighted) palette.bluePale else palette.cardPaper,
        border = BorderStroke(1.dp, if (highlighted) palette.accent else palette.border)
    ) {
        Row(
            Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, tint = if (highlighted) palette.accent else palette.textPrimary, modifier = Modifier.size(14.dp))
            Text(label, color = palette.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun CompactSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    val palette = LocalScrollGridPalette.current
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, color = palette.textSecondary, fontSize = 11.sp, modifier = Modifier.width(64.dp))
        Slider(value = value, onValueChange = onValueChange, valueRange = range, modifier = Modifier.weight(1f))
        Text(valueText, color = palette.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(42.dp))
    }
}

@Composable
private fun EditorMediaTile(item: MediaItem, modifier: Modifier = Modifier) {
    Box(modifier) {
        AsyncImage(
            model = item.uri,
            contentDescription = null,
            contentScale = if (item.fitInside) ContentScale.Fit else ContentScale.Crop,
            colorFilter = ColorFilter.colorMatrix(composeColorMatrix(item)),
            modifier = Modifier
                .fillMaxSize()
                .alpha(item.opacity)
                .graphicsLayer {
                    scaleX = if (item.flipHorizontal) -item.scale else item.scale
                    scaleY = item.scale
                    translationX = item.panX * 180f
                    translationY = item.panY * 180f
                    rotationZ = item.rotationDeg
                }
        )
        if (item.isVideo) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp),
                shape = RoundedCornerShape(4.dp),
                color = Color(0xCC0D0F14)
            ) {
                Text(
                    "VIDEO",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }
    }
}

private fun composeColorMatrix(item: MediaItem): ColorMatrix {
    val sat = ColorMatrix().apply { setToSaturation(item.saturation) }
    val c = item.contrast
    val t = (-0.5f * c + 0.5f + item.brightness) * 255f
    val warmR = (1f + item.warmth * 0.25f).coerceIn(0.6f, 1.4f)
    val warmB = (1f - item.warmth * 0.25f).coerceIn(0.6f, 1.4f)
    val adjust = ColorMatrix(
        floatArrayOf(
            c * warmR, 0f, 0f, 0f, t,
            0f, c, 0f, 0f, t,
            0f, 0f, c * warmB, 0f, t,
            0f, 0f, 0f, 1f, 0f
        )
    )
    sat.timesAssign(adjust)
    return sat
}
