package com.darkmode.scrollgridstudio.model

import android.net.Uri

enum class StudioMode(val title: String) {
    GRID("Grid"),
    CAROUSEL("Carousel"),
    FREESTYLE("Freeform")
}

enum class TileMaskShape(val label: String) {
    RECTANGLE("Rect"),
    CIRCLE("Circle"),
    HEART("Heart"),
    STAR("Star"),
    HEXAGON("Hex"),
    SLANT_LEFT("Slant L"),
    SLANT_RIGHT("Slant R")
}

data class SlotRect(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val polaroidBottomPad: Float = 0f,
    val maskShape: TileMaskShape = TileMaskShape.RECTANGLE
)

enum class LayoutStyle(val label: String, val slotCount: Int, val category: String, val variant: Int = 0) {
    SINGLE_1("1 Full", 1, "Classic"),
    SPLIT_2_H("2 Split H", 2, "Classic"),
    SPLIT_2_V("2 Split V", 2, "Classic"),
    SLANT_SPLIT_2("Slant 2", 2, "Shapes"),
    POLAROID_2("Polaroid 2", 2, "Classic"),
    SLANT_3("Hero 3", 3, "Hero"),
    STAR_FEATURE_3("Star 3", 3, "Shapes"),
    FILM_3("Film 3", 3, "Hero"),
    HEART_COLLAGE_3("Heart 3", 3, "Shapes"),
    GRID_2X2("2x2 Grid", 4, "Classic"),
    BENTO_4("Bento 4", 4, "Hero"),
    HEX_HONEYCOMB_4("Hex 4", 4, "Shapes"),
    GRID_5("Mosaic 5", 5, "Masonry"),
    GRID_6("2x3 Grid", 6, "Masonry"),
    GRID_9("3x3 Grid", 9, "Masonry"),
    LAYOUT_016("Grid 2 · 016", 2, "Classic", 1),
    LAYOUT_017("Strip 3 · 017", 3, "Editorial", 2),
    LAYOUT_018("Hero Left 4 · 018", 4, "Hero", 3),
    LAYOUT_019("Hero Top 5 · 019", 5, "Hero", 4),
    LAYOUT_020("Bento 6 · 020", 6, "Hero", 5),
    LAYOUT_021("Masonry 7 · 021", 7, "Masonry", 6),
    LAYOUT_022("Grid 8 · 022", 8, "Classic", 7),
    LAYOUT_023("Strip 9 · 023", 9, "Editorial", 8),
    LAYOUT_024("Hero Left 10 · 024", 10, "Hero", 9),
    LAYOUT_025("Hero Top 11 · 025", 11, "Hero", 10),
    LAYOUT_026("Bento 12 · 026", 12, "Hero", 11),
    LAYOUT_027("Masonry 2 · 027", 2, "Masonry", 12),
    LAYOUT_028("Grid 3 · 028", 3, "Classic", 13),
    LAYOUT_029("Strip 4 · 029", 4, "Editorial", 14),
    LAYOUT_030("Hero Left 5 · 030", 5, "Hero", 15),
    LAYOUT_031("Hero Top 6 · 031", 6, "Hero", 16),
    LAYOUT_032("Bento 7 · 032", 7, "Hero", 17),
    LAYOUT_033("Masonry 8 · 033", 8, "Masonry", 18),
    LAYOUT_034("Grid 9 · 034", 9, "Classic", 19),
    LAYOUT_035("Strip 10 · 035", 10, "Editorial", 20),
    LAYOUT_036("Hero Left 11 · 036", 11, "Hero", 21),
    LAYOUT_037("Hero Top 12 · 037", 12, "Hero", 22),
    LAYOUT_038("Bento 2 · 038", 2, "Hero", 23),
    LAYOUT_039("Masonry 3 · 039", 3, "Masonry", 24),
    LAYOUT_040("Grid 4 · 040", 4, "Classic", 25),
    LAYOUT_041("Strip 5 · 041", 5, "Editorial", 26),
    LAYOUT_042("Hero Left 6 · 042", 6, "Hero", 27),
    LAYOUT_043("Hero Top 7 · 043", 7, "Hero", 28),
    LAYOUT_044("Bento 8 · 044", 8, "Hero", 29),
    LAYOUT_045("Masonry 9 · 045", 9, "Masonry", 30),
    LAYOUT_046("Grid 10 · 046", 10, "Classic", 31),
    LAYOUT_047("Strip 11 · 047", 11, "Editorial", 32),
    LAYOUT_048("Hero Left 12 · 048", 12, "Hero", 33),
    LAYOUT_049("Hero Top 2 · 049", 2, "Hero", 34),
    LAYOUT_050("Bento 3 · 050", 3, "Hero", 35),
    LAYOUT_051("Masonry 4 · 051", 4, "Masonry", 36),
    LAYOUT_052("Grid 5 · 052", 5, "Classic", 37),
    LAYOUT_053("Strip 6 · 053", 6, "Editorial", 38),
    LAYOUT_054("Hero Left 7 · 054", 7, "Hero", 39),
    LAYOUT_055("Hero Top 8 · 055", 8, "Hero", 40),
    LAYOUT_056("Bento 9 · 056", 9, "Hero", 41),
    LAYOUT_057("Masonry 10 · 057", 10, "Masonry", 42),
    LAYOUT_058("Grid 11 · 058", 11, "Classic", 43),
    LAYOUT_059("Strip 12 · 059", 12, "Editorial", 44),
    LAYOUT_060("Hero Left 2 · 060", 2, "Hero", 45),
    LAYOUT_061("Hero Top 3 · 061", 3, "Hero", 46),
    LAYOUT_062("Bento 4 · 062", 4, "Hero", 47),
    LAYOUT_063("Masonry 5 · 063", 5, "Masonry", 48),
    LAYOUT_064("Grid 6 · 064", 6, "Classic", 49),
    LAYOUT_065("Strip 7 · 065", 7, "Editorial", 50),
    LAYOUT_066("Hero Left 8 · 066", 8, "Hero", 51),
    LAYOUT_067("Hero Top 9 · 067", 9, "Hero", 52),
    LAYOUT_068("Bento 10 · 068", 10, "Hero", 53),
    LAYOUT_069("Masonry 11 · 069", 11, "Masonry", 54),
    LAYOUT_070("Grid 12 · 070", 12, "Classic", 55),
    LAYOUT_071("Strip 2 · 071", 2, "Editorial", 56),
    LAYOUT_072("Hero Left 3 · 072", 3, "Hero", 57),
    LAYOUT_073("Hero Top 4 · 073", 4, "Hero", 58),
    LAYOUT_074("Bento 5 · 074", 5, "Hero", 59),
    LAYOUT_075("Masonry 6 · 075", 6, "Masonry", 60),
    LAYOUT_076("Grid 7 · 076", 7, "Classic", 61),
    LAYOUT_077("Strip 8 · 077", 8, "Editorial", 62),
    LAYOUT_078("Hero Left 9 · 078", 9, "Hero", 63),
    LAYOUT_079("Hero Top 10 · 079", 10, "Hero", 64),
    LAYOUT_080("Bento 11 · 080", 11, "Hero", 65),
    LAYOUT_081("Masonry 12 · 081", 12, "Masonry", 66),
    LAYOUT_082("Grid 2 · 082", 2, "Classic", 67),
    LAYOUT_083("Strip 3 · 083", 3, "Editorial", 68),
    LAYOUT_084("Hero Left 4 · 084", 4, "Hero", 69),
    LAYOUT_085("Hero Top 5 · 085", 5, "Hero", 70),
    LAYOUT_086("Bento 6 · 086", 6, "Hero", 71),
    LAYOUT_087("Masonry 7 · 087", 7, "Masonry", 72),
    LAYOUT_088("Grid 8 · 088", 8, "Classic", 73),
    LAYOUT_089("Strip 9 · 089", 9, "Editorial", 74),
    LAYOUT_090("Hero Left 10 · 090", 10, "Hero", 75),
    LAYOUT_091("Hero Top 11 · 091", 11, "Hero", 76),
    LAYOUT_092("Bento 12 · 092", 12, "Hero", 77),
    LAYOUT_093("Masonry 2 · 093", 2, "Masonry", 78),
    LAYOUT_094("Grid 3 · 094", 3, "Classic", 79),
    LAYOUT_095("Strip 4 · 095", 4, "Editorial", 80),
    LAYOUT_096("Hero Left 5 · 096", 5, "Hero", 81),
    LAYOUT_097("Hero Top 6 · 097", 6, "Hero", 82),
    LAYOUT_098("Bento 7 · 098", 7, "Hero", 83),
    LAYOUT_099("Masonry 8 · 099", 8, "Masonry", 84),
    LAYOUT_100("Grid 9 · 100", 9, "Classic", 85),
    LAYOUT_101("Strip 10 · 101", 10, "Editorial", 86),
    LAYOUT_102("Hero Left 11 · 102", 11, "Hero", 87),
    LAYOUT_103("Hero Top 12 · 103", 12, "Hero", 88),
    LAYOUT_104("Bento 2 · 104", 2, "Hero", 89),
    LAYOUT_105("Masonry 3 · 105", 3, "Masonry", 90),
    LAYOUT_106("Grid 4 · 106", 4, "Classic", 91),
    LAYOUT_107("Strip 5 · 107", 5, "Editorial", 92),
    LAYOUT_108("Hero Left 6 · 108", 6, "Hero", 93),
    LAYOUT_109("Hero Top 7 · 109", 7, "Hero", 94),
    LAYOUT_110("Bento 8 · 110", 8, "Hero", 95),
    LAYOUT_111("Masonry 9 · 111", 9, "Masonry", 96),
    LAYOUT_112("Grid 10 · 112", 10, "Classic", 97),
    LAYOUT_113("Strip 11 · 113", 11, "Editorial", 98),
    LAYOUT_114("Hero Left 12 · 114", 12, "Hero", 99),
    LAYOUT_115("Hero Top 2 · 115", 2, "Hero", 100),
    LAYOUT_116("Bento 3 · 116", 3, "Hero", 101),
    LAYOUT_117("Masonry 4 · 117", 4, "Masonry", 102),
    LAYOUT_118("Grid 5 · 118", 5, "Classic", 103),
    LAYOUT_119("Strip 6 · 119", 6, "Editorial", 104),
    LAYOUT_120("Hero Left 7 · 120", 7, "Hero", 105),
    LAYOUT_121("Hero Top 8 · 121", 8, "Hero", 106),
    LAYOUT_122("Bento 9 · 122", 9, "Hero", 107),
    LAYOUT_123("Masonry 10 · 123", 10, "Masonry", 108),
    LAYOUT_124("Grid 11 · 124", 11, "Classic", 109),
    LAYOUT_125("Strip 12 · 125", 12, "Editorial", 110),
    LAYOUT_126("Hero Left 2 · 126", 2, "Hero", 111),
    LAYOUT_127("Hero Top 3 · 127", 3, "Hero", 112),
    LAYOUT_128("Bento 4 · 128", 4, "Hero", 113),
    LAYOUT_129("Masonry 5 · 129", 5, "Masonry", 114),
    LAYOUT_130("Grid 6 · 130", 6, "Classic", 115),
    LAYOUT_131("Strip 7 · 131", 7, "Editorial", 116),
    LAYOUT_132("Hero Left 8 · 132", 8, "Hero", 117),
    LAYOUT_133("Hero Top 9 · 133", 9, "Hero", 118),
    LAYOUT_134("Bento 10 · 134", 10, "Hero", 119),
    LAYOUT_135("Masonry 11 · 135", 11, "Masonry", 120),
    LAYOUT_136("Grid 12 · 136", 12, "Classic", 121),
    LAYOUT_137("Strip 2 · 137", 2, "Editorial", 122),
    LAYOUT_138("Hero Left 3 · 138", 3, "Hero", 123),
    LAYOUT_139("Hero Top 4 · 139", 4, "Hero", 124),
    LAYOUT_140("Bento 5 · 140", 5, "Hero", 125),
    LAYOUT_141("Masonry 6 · 141", 6, "Masonry", 126),
    LAYOUT_142("Grid 7 · 142", 7, "Classic", 127),
    LAYOUT_143("Strip 8 · 143", 8, "Editorial", 128),
    LAYOUT_144("Hero Left 9 · 144", 9, "Hero", 129),
    LAYOUT_145("Hero Top 10 · 145", 10, "Hero", 130),
    LAYOUT_146("Bento 11 · 146", 11, "Hero", 131),
    LAYOUT_147("Masonry 12 · 147", 12, "Masonry", 132),
    LAYOUT_148("Grid 2 · 148", 2, "Classic", 133),
    LAYOUT_149("Strip 3 · 149", 3, "Editorial", 134),
    LAYOUT_150("Hero Left 4 · 150", 4, "Hero", 135),
    LAYOUT_151("Hero Top 5 · 151", 5, "Hero", 136),
    LAYOUT_152("Bento 6 · 152", 6, "Hero", 137),
    LAYOUT_153("Masonry 7 · 153", 7, "Masonry", 138),
    LAYOUT_154("Grid 8 · 154", 8, "Classic", 139),
    LAYOUT_155("Strip 9 · 155", 9, "Editorial", 140),
    LAYOUT_156("Hero Left 10 · 156", 10, "Hero", 141),
    LAYOUT_157("Hero Top 11 · 157", 11, "Hero", 142),
    LAYOUT_158("Bento 12 · 158", 12, "Hero", 143),
    LAYOUT_159("Masonry 2 · 159", 2, "Masonry", 144),
    LAYOUT_160("Grid 3 · 160", 3, "Classic", 145),
    LAYOUT_161("Strip 4 · 161", 4, "Editorial", 146),
    LAYOUT_162("Hero Left 5 · 162", 5, "Hero", 147),
    LAYOUT_163("Hero Top 6 · 163", 6, "Hero", 148),
    LAYOUT_164("Bento 7 · 164", 7, "Hero", 149),
    LAYOUT_165("Masonry 8 · 165", 8, "Masonry", 150),
    LAYOUT_166("Grid 9 · 166", 9, "Classic", 151),
    LAYOUT_167("Strip 10 · 167", 10, "Editorial", 152),
    LAYOUT_168("Hero Left 11 · 168", 11, "Hero", 153),
    LAYOUT_169("Hero Top 12 · 169", 12, "Hero", 154),
    LAYOUT_170("Bento 2 · 170", 2, "Hero", 155),
    LAYOUT_171("Masonry 3 · 171", 3, "Masonry", 156),
    LAYOUT_172("Grid 4 · 172", 4, "Classic", 157),
    LAYOUT_173("Strip 5 · 173", 5, "Editorial", 158),
    LAYOUT_174("Hero Left 6 · 174", 6, "Hero", 159),
    LAYOUT_175("Hero Top 7 · 175", 7, "Hero", 160),
    LAYOUT_176("Bento 8 · 176", 8, "Hero", 161),
    LAYOUT_177("Masonry 9 · 177", 9, "Masonry", 162),
    LAYOUT_178("Grid 10 · 178", 10, "Classic", 163),
    LAYOUT_179("Strip 11 · 179", 11, "Editorial", 164),
    LAYOUT_180("Hero Left 12 · 180", 12, "Hero", 165),
    LAYOUT_181("Hero Top 2 · 181", 2, "Hero", 166),
    LAYOUT_182("Bento 3 · 182", 3, "Hero", 167),
    LAYOUT_183("Masonry 4 · 183", 4, "Masonry", 168),
    LAYOUT_184("Grid 5 · 184", 5, "Classic", 169),
    LAYOUT_185("Strip 6 · 185", 6, "Editorial", 170),
    LAYOUT_186("Hero Left 7 · 186", 7, "Hero", 171),
    LAYOUT_187("Hero Top 8 · 187", 8, "Hero", 172),
    LAYOUT_188("Bento 9 · 188", 9, "Hero", 173),
    LAYOUT_189("Masonry 10 · 189", 10, "Masonry", 174),
    LAYOUT_190("Grid 11 · 190", 11, "Classic", 175),
    LAYOUT_191("Strip 12 · 191", 12, "Editorial", 176),
    LAYOUT_192("Hero Left 2 · 192", 2, "Hero", 177),
    LAYOUT_193("Hero Top 3 · 193", 3, "Hero", 178),
    LAYOUT_194("Bento 4 · 194", 4, "Hero", 179),
    LAYOUT_195("Masonry 5 · 195", 5, "Masonry", 180),
    LAYOUT_196("Grid 6 · 196", 6, "Classic", 181),
    LAYOUT_197("Strip 7 · 197", 7, "Editorial", 182),
    LAYOUT_198("Hero Left 8 · 198", 8, "Hero", 183),
    LAYOUT_199("Hero Top 9 · 199", 9, "Hero", 184),
    LAYOUT_200("Bento 10 · 200", 10, "Hero", 185),
    LAYOUT_201("Masonry 11 · 201", 11, "Masonry", 186),
    LAYOUT_202("Grid 12 · 202", 12, "Classic", 187),
    LAYOUT_203("Strip 2 · 203", 2, "Editorial", 188),
    LAYOUT_204("Hero Left 3 · 204", 3, "Hero", 189),
    LAYOUT_205("Hero Top 4 · 205", 4, "Hero", 190),
    LAYOUT_206("Bento 5 · 206", 5, "Hero", 191),
    LAYOUT_207("Masonry 6 · 207", 6, "Masonry", 192);

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
        SLANT_SPLIT_2 -> listOf(
            SlotRect(0f, 0f, 0.56f, 1f, maskShape = TileMaskShape.SLANT_RIGHT),
            SlotRect(0.44f, 0f, 0.56f, 1f, maskShape = TileMaskShape.SLANT_LEFT)
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
        STAR_FEATURE_3 -> listOf(
            SlotRect(0.14f, 0.02f, 0.72f, 0.54f, maskShape = TileMaskShape.STAR),
            SlotRect(0.02f, 0.56f, 0.47f, 0.42f, maskShape = TileMaskShape.CIRCLE),
            SlotRect(0.51f, 0.56f, 0.47f, 0.42f, maskShape = TileMaskShape.CIRCLE)
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
        HEX_HONEYCOMB_4 -> listOf(
            SlotRect(0.04f, 0.04f, 0.45f, 0.45f, maskShape = TileMaskShape.HEXAGON),
            SlotRect(0.51f, 0.04f, 0.45f, 0.45f, maskShape = TileMaskShape.HEXAGON),
            SlotRect(0.04f, 0.51f, 0.45f, 0.45f, maskShape = TileMaskShape.HEXAGON),
            SlotRect(0.51f, 0.51f, 0.45f, 0.45f, maskShape = TileMaskShape.HEXAGON)
        )
        HEART_COLLAGE_3 -> listOf(
            SlotRect(0.12f, 0.04f, 0.76f, 0.54f, maskShape = TileMaskShape.HEART),
            SlotRect(0.02f, 0.58f, 0.47f, 0.40f),
            SlotRect(0.51f, 0.58f, 0.47f, 0.40f)
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
        else -> generatedSlotRects()
    }

    private fun generatedSlotRects(): List<SlotRect> {
        val n = slotCount.coerceAtLeast(1)
        if (n == 1) return listOf(SlotRect(0f, 0f, 1f, 1f))
        val seed = variant / 6
        val family = variant % 6
        fun grid(count: Int, left: Float, top: Float, width: Float, height: Float, columns: Int): List<SlotRect> {
            val rows = (count + columns - 1) / columns
            return (0 until count).map { index ->
                val row = index / columns
                val column = index % columns
                val inRow = minOf(columns, count - row * columns)
                SlotRect(left + column * width / inRow, top + row * height / rows, width / inRow, height / rows)
            }
        }
        return when (family) {
            0 -> { val cols = when { n <= 2 -> 2; n <= 6 -> 3; else -> 4 }; grid(n, 0f, 0f, 1f, 1f, cols) }
            1 -> { val weights = (0 until n).map { 0.82f + (((it + seed) % 4) * 0.12f) }; val total = weights.sum(); var x = 0f; weights.map { w -> val width = w / total; SlotRect(x, 0f, width, 1f).also { x += width } } }
            2, 4 -> { val hero = (0.54f + (seed % 5) * 0.025f).coerceAtMost(0.66f); listOf(SlotRect(0f, 0f, hero, 1f)) + (1 until n).map { i -> val h = 1f / (n - 1); SlotRect(hero, (i - 1) * h, 1f - hero, h) } }
            3 -> { val hero = (0.54f + (seed % 4) * 0.025f).coerceAtMost(0.66f); listOf(SlotRect(0f, 0f, 1f, hero)) + grid(n - 1, 0f, hero, 1f, 1f - hero, minOf(4, n - 1)) }
            else -> { val cols = if (n < 5) 2 else 3; val colWidth = 1f / cols; (0 until n).groupBy { it % cols }.toSortedMap().flatMap { (col, indices) -> val weights = indices.mapIndexed { j, _ -> 0.82f + (((j + seed + col) % 4) * 0.12f) }; val total = weights.sum(); var y = 0f; indices.mapIndexed { j, _ -> val h = weights[j] / total; SlotRect(col * colWidth, y, colWidth, h).also { y += h } } } }
        }
    }
}

enum class ScrlTemplate(val label: String, val slides: Int) {
    OVERLAP_3("Editorial 3", 3),
    MAGAZINE_4("Magazine 4", 4),
    PANORAMA_5("Panorama 5", 5)
}

enum class CanvasTexture(val label: String) {
    NONE("Clean"),
    FILM_GRAIN("Film Grain"),
    PAPER("Warm Paper"),
    TORN_EDGE("Torn Frame")
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
    val saturation: Float,
    val warmth: Float = 0f,
    val vignette: Float = 0f
) {
    ORIGINAL("Original", 0f, 1f, 1f, 0f, 0f),
    BW("B&W", 0.02f, 1.2f, 0f, 0f, 0.25f),
    VINTAGE("Vintage", 0.04f, 0.92f, 0.72f, 0.25f, 0.35f),
    VIVID("Vivid", 0.02f, 1.18f, 1.38f, 0.08f, 0f),
    MATTE("Matte", 0.06f, 0.85f, 0.88f, -0.05f, 0.15f),
    CINEMA("Cinema", -0.04f, 1.25f, 0.82f, -0.12f, 0.4f)
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
    CONTROLS("Controls"),
    STYLE("Style"),
    TEXT("Text"),
    CREATIVE("Creative"),
    FREESTYLE("Freestyle")
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

enum class AspectRatioOption(
    val label: String,
    val shortLabel: String,
    val widthRatio: Int,
    val heightRatio: Int,
    val printWidthPx: Int? = null,
    val printHeightPx: Int? = null
) {
    SQUARE_1_1("1:1 Square", "1:1", 1, 1),
    PORTRAIT_4_5("4:5 Portrait (Feed)", "4:5", 4, 5),
    GRID_3_4("3:4 Classic", "3:4", 3, 4),
    PHOTO_2_3("2:3 Photo", "2:3", 2, 3),
    STORY_9_16("9:16 Story (Reels)", "9:16", 9, 16),
    WIDE_16_9("16:9 Wide", "16:9", 16, 9),
    WIDE_191_1("1.91:1 Landscape (Feed)", "1.91:1", 191, 100),
    PRINT_4X6("Print 4x6 (1200x1800)", "4x6", 4, 6, 1200, 1800),
    PRINT_5X7("Print 5x7 (1500x2100)", "5x7", 5, 7, 1500, 2100),
    PRINT_8X10("Print 8x10 (2400x3000)", "8x10", 8, 10, 2400, 3000),
    PRINT_A4("Print A4 (2480x3508)", "A4", 210, 297, 2480, 3508),
    CUSTOM("Custom sizes (Any px)", "Custom", 4, 5);

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
    val warmth: Float = 0f,
    val vignette: Float = 0f,
    val freeX: Float = 0.25f,
    val freeY: Float = 0.25f,
    val freeW: Float = 0.42f,
    val freeH: Float = 0.42f,
    val zIndex: Int = 0,
    val locked: Boolean = false,
    val opacity: Float = 1f,
    val shadowDp: Int = 10,
    val maskShape: TileMaskShape = TileMaskShape.RECTANGLE
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
    val fontName: String = "Roboto",
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
    val customWidthPx: Int = 1080,
    val customHeightPx: Int = 1350,
    val format: ExportFormat = ExportFormat.JPG,
    val qualityPercent: Int = 95,
    val stripExif: Boolean = true,
    val includeSignature: Boolean = false,
    val signatureText: String = "@sandeep_njr"
) {
    fun outputWidthPx(): Int = when {
        aspectRatio == AspectRatioOption.CUSTOM -> customWidthPx.coerceIn(320, 4320)
        aspectRatio.printWidthPx != null -> aspectRatio.printWidthPx
        else -> resolution.baseWidth
    }

    fun outputHeightPx(): Int = when {
        aspectRatio == AspectRatioOption.CUSTOM -> customHeightPx.coerceIn(320, 4320)
        aspectRatio.printHeightPx != null -> aspectRatio.printHeightPx
        else -> aspectRatio.heightForWidth(resolution.baseWidth)
    }
}

data class SavedDraftMeta(
    val id: String,
    val name: String,
    val updatedAtMillis: Long,
    val modeTitle: String,
    val itemCount: Int,
    val previewUris: List<String> = emptyList()
)

data class StudioState(
    val projectName: String = "Untitled Collage",
    val mode: StudioMode = StudioMode.GRID,
    val layout: LayoutStyle = LayoutStyle.GRID_2X2,
    val activeTab: StudioTab = StudioTab.LAYOUT,
    val carouselSlides: Int = 3,
    val activeSlideIndex: Int = 0,
    val showGuides: Boolean = true,
    val outerMarginDp: Int = 10,
    val borderDp: Int = 10,
    val cornerRadiusDp: Int = 12,
    val backgroundStyle: BackgroundStyle = BackgroundStyle.OBSIDIAN,
    val customBgHex: Long? = null,
    val borderColorHex: Long? = null,
    val canvasTexture: CanvasTexture = CanvasTexture.NONE,
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
