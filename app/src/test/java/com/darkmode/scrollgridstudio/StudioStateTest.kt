package com.darkmode.scrollgridstudio

import com.darkmode.scrollgridstudio.model.AspectRatioOption
import com.darkmode.scrollgridstudio.model.ExportConfig
import com.darkmode.scrollgridstudio.model.LayoutStyle
import com.darkmode.scrollgridstudio.model.ResolutionPreset
import com.darkmode.scrollgridstudio.model.StudioMode
import com.darkmode.scrollgridstudio.model.StudioState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun generatedLayoutCategoriesMatchTheirNumberedFamilies() {
        val expectedCategories = listOf("Classic", "Editorial", "Hero", "Hero", "Bento", "Masonry")
        LayoutStyle.entries.filter { it.variant > 0 }.forEach { layout ->
            val family = (layout.variant - 1) % expectedCategories.size
            assertEquals("Wrong family for ${layout.name}", expectedCategories[family], layout.category)
        }
    }

    @Test
    fun everyLayoutRectangleHasPositiveAreaAndStaysInsideCanvas() {
        LayoutStyle.entries.forEach { layout ->
            layout.slotRects().forEachIndexed { index, rect ->
                assertTrue("${layout.name} slot $index has non-positive width", rect.width > 0f)
                assertTrue("${layout.name} slot $index has non-positive height", rect.height > 0f)
                assertTrue("${layout.name} slot $index starts left of canvas", rect.left >= 0f)
                assertTrue("${layout.name} slot $index starts above canvas", rect.top >= 0f)
                assertTrue("${layout.name} slot $index exceeds right edge", rect.left + rect.width <= 1.001f)
                assertTrue("${layout.name} slot $index exceeds bottom edge", rect.top + rect.height <= 1.001f)
            }
        }
    }

    @Test
    fun bentoGeometryIsDistinctFromHeroLeftForRepresentativeSlotCounts() {
        listOf(2, 3, 4, 6, 9).forEach { count ->
            val heroLeft = LayoutStyle.entries.first {
                it.variant > 0 && (it.variant - 1) % 6 == 2 && it.slotCount == count
            }
            val bento = LayoutStyle.entries.first {
                it.variant > 0 && (it.variant - 1) % 6 == 4 && it.slotCount == count
            }
            assertTrue(
                "Bento and Hero Left should not share geometry for $count slots",
                bento.slotRects() != heroLeft.slotRects()
            )
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
