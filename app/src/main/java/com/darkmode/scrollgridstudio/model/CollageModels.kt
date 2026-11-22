package com.darkmode.scrollgridstudio.model

import android.net.Uri

enum class StudioMode(val title: String) {
    GRID("Grid"),
    CAROUSEL("Carousel"),
    FREESTYLE("Freeform")
}

enum class LayoutStyle(val label: String, val slotCount: Int) {
    GRID_2X2("2x2 Grid", 4),
    BENTO_4("Bento 4", 4),
    SLANT_3("Slant 3", 3),
    FILM_3("Film Strip 3", 3),
    POLAROID_2("Polaroid 2", 2)
}

enum class BackgroundStyle(val label: String, val hex: Long) {
    OBSIDIAN("Obsidian", 0xFF0B0D10),
    CHARCOAL("Charcoal", 0xFF14171D),
    FOREST("Forest", 0xFF133326),
    BAMBOO("Bamboo", 0xFF1D3B2B),
    NAVY("Navy", 0xFF0F1E36),
    BLUR_MATCH("Slate", 0xFF1C232E)
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
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f
)

data class TextOverlay(
    val id: String,
    val text: String,
    val xFraction: Float = 0.5f,
    val yFraction: Float = 0.5f,
    val fontSizeSp: Float = 22f,
    val usePill: Boolean = true,
    val useOutline: Boolean = true,
    val textColorHex: Long = 0xFFF3F4F6,
    val isSticker: Boolean = false
)

data class ExportConfig(
    val mode: ExportMode = ExportMode.AUTO_SLICE,
    val slideOrder: SlideOrder = SlideOrder.FORWARD,
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
    val borderDp: Int = 8,
    val cornerRadiusDp: Int = 12,
    val backgroundStyle: BackgroundStyle = BackgroundStyle.OBSIDIAN,
    val canvasLocked: Boolean = false,
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
