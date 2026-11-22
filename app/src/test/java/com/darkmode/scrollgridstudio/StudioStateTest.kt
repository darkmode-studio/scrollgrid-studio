package com.darkmode.scrollgridstudio

import com.darkmode.scrollgridstudio.model.LayoutStyle
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
}
