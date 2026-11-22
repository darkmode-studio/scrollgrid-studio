package com.darkmode.scrollgridstudio.model

import android.net.Uri

enum class StudioMode(val title: String) {
    GRID("Grid"),
    CAROUSEL("Carousel"),
    FREESTYLE("Freeform")
}

data class SlotRect(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val polaroidBottomPad: Float = 0f
)

enum class LayoutStyle(val label: String, val slotCount: Int) {
    SINGLE_1("1 Full", 1),
    SPLIT_2_H("2 Split H", 2),
    SPLIT_2_V("2 Split V", 2),
    POLAROID_2("Polaroid 2", 2),
    SLANT_3("Hero 3", 3),
    FILM_3("Film Strip 3", 3),
    GRID_2X2("2x2 Grid", 4),
    BENTO_4("Bento 4", 4),
    GRID_5("Mosaic 5", 5),
    GRID_6("2x3 Grid", 6),
    GRID_9("3x3 Grid", 9);

    fun slotRects(): List<SlotRect> = when (this) {
        SINGLE_1 -> listOf(SlotRect(0f, 0f, 1f, 1f))
        SPLIT_2_H -> listOf(
            SlotRect(0f, 0f, 0.5f, 1f),
            SlotRect(0.5f, 0f, 0.5f, 1f)
        )
        SPLIT_2_V -> listOf(
            SlotRect(0f, 0f, 1f, 0.5f),
            SlotRect(0f, 0.5f, 1f, 0.5f)
        )
        POLAROID_2 -> listOf(
            SlotRect(0.04f, 0.06f, 0.44f, 0.88f, polaroidBottomPad = 0.14f),
            SlotRect(0.52f, 0.06f, 0.44f, 0.88f, polaroidBottomPad = 0.14f)
        )
        SLANT_3 -> listOf(
            SlotRect(0f, 0f, 0.56f, 1f),
            SlotRect(0.56f, 0f, 0.44f, 0.5f),
            SlotRect(0.56f, 0.5f, 0.44f, 0.5f)
        )
        FILM_3 -> listOf(
            SlotRect(0.06f, 0f, 0.88f, 1f / 3f),
            SlotRect(0.06f, 1f / 3f, 0.88f, 1f / 3f),
            SlotRect(0.06f, 2f / 3f, 0.88f, 1f / 3f)
        )
        GRID_2X2 -> listOf(
            SlotRect(0f, 0f, 0.5f, 0.5f),
            SlotRect(0.5f, 0f, 0.5f, 0.5f),
            SlotRect(0f, 0.5f, 0.5f, 0.5f),
            SlotRect(0.5f, 0.5f, 0.5f, 0.5f)
        )
        BENTO_4 -> listOf(
            SlotRect(0f, 0f, 1f, 0.56f),
            SlotRect(0f, 0.56f, 1f / 3f, 0.44f),
            SlotRect(1f / 3f, 0.56f, 1f / 3f, 0.44f),
            SlotRect(2f / 3f, 0.56f, 1f / 3f, 0.44f)
        )
        GRID_5 -> listOf(
            SlotRect(0f, 0f, 0.5f, 0.52f),
            SlotRect(0.5f, 0f, 0.5f, 0.52f),
            SlotRect(0f, 0.52f, 1f / 3f, 0.48f),
            SlotRect(1f / 3f, 0.52f, 1f / 3f, 0.48f),
            SlotRect(2f / 3f, 0.52f, 1f / 3f, 0.48f)
        )
        GRID_6 -> (0 until 6).map { idx ->
            val r = idx / 2
            val c = idx % 2
            SlotRect(c * 0.5f, r / 3f, 0.5f, 1f / 3f)
        }
        GRID_9 -> (0 until 9).map { idx ->
            val r = idx / 3
            val c = idx % 3
            SlotRect(c / 3f, r / 3f, 1f / 3f, 1f / 3f)
        }
    }
}

enum class BackgroundStyle(
    val label: String,
    val startHex: Long,
    val endHex: Long = startHex,
    val isBlurPhoto: Boolean = false
) {
    OBSIDIAN("Obsidian", 0xFF0B0D10),
    PURE_WHITE("Clean White", 0xFFF8FAFC),
    CHARCOAL("Charcoal", 0xFF14171D),
    FOREST("Forest", 0xFF133326),
    BAMBOO("Bamboo", 0xFF1D3B2B),
    NAVY("Navy", 0xFF0F1E36),
    GRAD_FOREST("Forest Grad", 0xFF0B1F17, 0xFF1E4D36),
    GRAD_SUNSET("Sunset Grad", 0xFF2B1226, 0xFF7C2D12),
    GRAD_OCEAN("Ocean Grad", 0xFF0C1B33, 0xFF0E4D64),
    BLUR_MATCH("Blur Photo", 0xFF1C232E, 0xFF0B0D10, isBlurPhoto = true);

    val hex: Long get() = startHex
}

enum class FilterPreset(
    val label: String,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float
) {
    ORIGINAL("Original", 0f, 1f, 1f),
    BW("B&W", 0.02f, 1.2f, 0f),
    VINTAGE("Vintage", 0.04f, 0.92f, 0.72f),
    VIVID("Vivid", 0.02f, 1.18f, 1.38f),
    MATTE("Matte", 0.06f, 0.85f, 0.88f),
    CINEMA("Cinema", -0.04f, 1.25f, 0.82f)
}

enum class TextFontStyle(val label: String) {
    SANS_BOLD("Sans Bold"),
    SERIF("Serif"),
    MONO("Mono"),
    ITALIC("Italic")
}

enum class TextAlignOption(val label: String) {
    LEFT("Left"),
    CENTER("Center"),
    RIGHT("Right")
}

enum class StudioTab(val label: String) {
    LAYOUT("Layout"),
    MEDIA("Media"),
    BG("BG"),
    TEXT("Text"),
    STICKERS("Stickers"),
    FILTERS("Filters")
}

enum class ExportMode(val label: String) {
    AUTO_SLICE("Auto-Slice Carousel"),
    SINGLE_MERGED("Single Merged Image"),
    SELECTED_SLIDE("Selected Slide Only")
}

enum class SlideOrder(val label: String) {
    FORWARD("Forward (1 -> N)"),
    REVERSE("Reverse (N -> 1)")
}

enum class ResolutionPreset(val label: String, val baseWidth: Int) {
    IG_1080("IG Optimal (1080px)", 1080),
    UHD_2K("2K Sharp (2160px)", 2160),
    UHD_4K("4K Ultra (3240px)", 3240)
}

enum class AspectRatioOption(val label: String, val widthRatio: Int, val heightRatio: Int) {
    PORTRAIT_4_5("4:5 Feed", 4, 5),
    GRID_3_4("3:4 Grid", 3, 4),
    SQUARE_1_1("1:1 Square", 1, 1),
    STORY_9_16("9:16 Story", 9, 16),
    WIDE_191_1("1.91:1 Wide", 191, 100);

    fun heightForWidth(widthPx: Int): Int =
        ((widthPx.toLong() * heightRatio) / widthRatio).toInt().coerceAtLeast(1)
}

enum class ExportFormat(val label: String, val extension: String, val mimeType: String) {
    JPG("JPG", "jpg", "image/jpeg"),
    PNG("PNG (Lossless)", "png", "image/png"),
    WEBP("WebP", "webp", "image/webp")
}

data class MediaItem(
    val id: String,
    val uri: Uri,
    val isVideo: Boolean = false,
    val scale: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val rotationDeg: Float = 0f,
    val flipHorizontal: Boolean = false,
    val fitInside: Boolean = false,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val freeX: Float = 0.25f,
    val freeY: Float = 0.2f,
    val freeW: Float = 0.42f,
    val freeH: Float = 0.42f,
    val zIndex: Int = 0,
    val locked: Boolean = false,
    val opacity: Float = 1f
)

data class TextOverlay(
    val id: String,
    val text: String,
    val xFraction: Float = 0.5f,
    val yFraction: Float = 0.5f,
    val fontSizeSp: Float = 22f,
    val rotationDeg: Float = 0f,
    val usePill: Boolean = true,
    val useOutline: Boolean = true,
    val textColorHex: Long = 0xFFF3F4F6,
    val fontStyle: TextFontStyle = TextFontStyle.SANS_BOLD,
    val alignment: TextAlignOption = TextAlignOption.CENTER,
    val opacity: Float = 1f,
    val isSticker: Boolean = false,
    val stickerUri: Uri? = null,
    val stickerSizeFraction: Float = 0.22f
)

data class ExportConfig(
    val mode: ExportMode = ExportMode.AUTO_SLICE,
    val slideOrder: SlideOrder = SlideOrder.FORWARD,
    val selectedSlideNumber: Int = 1,
    val resolution: ResolutionPreset = ResolutionPreset.IG_1080,
    val aspectRatio: AspectRatioOption = AspectRatioOption.PORTRAIT_4_5,
    val format: ExportFormat = ExportFormat.JPG,
    val qualityPercent: Int = 95,
    val stripExif: Boolean = true,
    val includeSignature: Boolean = false,
    val signatureText: String = "@sandeep_njr"
) {
    fun outputWidthPx(): Int = resolution.baseWidth
    fun outputHeightPx(): Int = aspectRatio.heightForWidth(resolution.baseWidth)
}

data class StudioState(
    val mode: StudioMode = StudioMode.GRID,
    val layout: LayoutStyle = LayoutStyle.GRID_2X2,
    val activeTab: StudioTab = StudioTab.LAYOUT,
    val carouselSlides: Int = 3,
    val activeSlideIndex: Int = 0,
    val showGuides: Boolean = true,
    val outerMarginDp: Int = 10,
    val borderDp: Int = 8,
    val cornerRadiusDp: Int = 12,
    val backgroundStyle: BackgroundStyle = BackgroundStyle.OBSIDIAN,
    val canvasLocked: Boolean = false,
    val swapSourceIndex: Int? = null,
    val selectedSlotIndex: Int = 0,
    val selectedOverlayId: String? = null,
    val slots: List<MediaItem> = emptyList(),
    val extras: List<MediaItem> = emptyList(),
    val overlays: List<TextOverlay> = emptyList(),
    val exportConfig: ExportConfig = ExportConfig()
) {
    fun maxCapacity(): Int = when (mode) {
        StudioMode.CAROUSEL -> carouselSlides * 2
        StudioMode.GRID, StudioMode.FREESTYLE -> layout.slotCount
    }
}
