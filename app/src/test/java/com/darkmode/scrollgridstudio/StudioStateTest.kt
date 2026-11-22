package com.darkmode.scrollgridstudio

import com.darkmode.scrollgridstudio.model.AspectRatioOption
import com.darkmode.scrollgridstudio.model.ExportConfig
import com.darkmode.scrollgridstudio.model.LayoutStyle
import com.darkmode.scrollgridstudio.model.ResolutionPreset
import com.darkmode.scrollgridstudio.model.StudioMode
import com.darkmode.scrollgridstudio.model.StudioState
import org.junit.Assert.assertEquals
import org.junit.Test

class StudioStateTest {
    @Test
    fun capacityMatchesModeAndSlides() {
        val state = StudioState(
            mode = StudioMode.CAROUSEL,
            carouselSlides = 4,
            layout = LayoutStyle.GRID_2X2
        )
        assertEquals(8, state.maxCapacity())
    }

    @Test
    fun allLayoutsMatchSlotCounts() {
        LayoutStyle.entries.forEach { layout ->
            assertEquals(layout.slotCount, layout.slotRects().size)
        }
    }

    @Test
    fun exportDimensionsMatchPresetAndPrintSizes() {
        val igCfg = ExportConfig(
            resolution = ResolutionPreset.IG_1080,
            aspectRatio = AspectRatioOption.PORTRAIT_4_5
        )
        assertEquals(1080, igCfg.outputWidthPx())
        assertEquals(1350, igCfg.outputHeightPx())

        val a4Cfg = ExportConfig(aspectRatio = AspectRatioOption.PRINT_A4)
        assertEquals(2480, a4Cfg.outputWidthPx())
        assertEquals(3508, a4Cfg.outputHeightPx())

        val customCfg = ExportConfig(
            aspectRatio = AspectRatioOption.CUSTOM,
            customWidthPx = 1600,
            customHeightPx = 900
        )
        assertEquals(1600, customCfg.outputWidthPx())
        assertEquals(900, customCfg.outputHeightPx())
    }
}
