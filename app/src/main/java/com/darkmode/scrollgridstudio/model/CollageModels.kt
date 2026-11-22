package com.darkmode.scrollgridstudio.model

import android.net.Uri

enum class StudioMode(val title: String) {
    GRID("Grid & Shapes"),
    CAROUSEL("Seamless Carousel"),
    FREESTYLE("Freestyle")
}

enum class LayoutStyle(val label: String, val slotCount: Int) {
    GRID_2X2("2x2 Grid", 4),
    BENTO_4("Bento 4", 4),
    SLANT_3("Slant 3", 3),
    FILM_3("Film Strip 3", 3),
    POLAROID_2("Polaroid 2", 2)
}

enum class BackgroundStyle(val label: String) {
    OBSIDIAN("Obsidian Dark"),
    FOREST("Kerala Forest"),
    BAMBOO("Bamboo Mist"),
    NAVY("Deep Navy"),
    BLUR_MATCH("Soft Slate")
}

data class MediaItem(
    val id: String,
    val uri: Uri,
    val isVideo: Boolean = false,
    val scale: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val brightness: Float = 1f,
    val contrast: Float = 1f
)

data class TextOverlay(
    val id: String,
    val text: String,
    val xFraction: Float = 0.5f,
    val yFraction: Float = 0.5f,
    val fontSizeSp: Float = 22f,
    val usePill: Boolean = true,
    val useOutline: Boolean = true
)

data class StudioState(
    val mode: StudioMode = StudioMode.GRID,
    val layout: LayoutStyle = LayoutStyle.GRID_2X2,
    val carouselSlides: Int = 3,
    val borderDp: Int = 8,
    val cornerRadiusDp: Int = 14,
    val backgroundStyle: BackgroundStyle = BackgroundStyle.OBSIDIAN,
    val canvasLocked: Boolean = false,
    val selectedSlotIndex: Int = 0,
    val slots: List<MediaItem> = emptyList(),
    val extras: List<MediaItem> = emptyList(),
    val overlays: List<TextOverlay> = emptyList(),
    val exportWidthPx: Int = 2160,
    val exportHeightPx: Int = 2700
) {
    fun maxCapacity(): Int = when (mode) {
        StudioMode.CAROUSEL -> carouselSlides * 2
        StudioMode.GRID, StudioMode.FREESTYLE -> layout.slotCount
    }
}
