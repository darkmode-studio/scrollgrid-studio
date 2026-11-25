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
    STITCH_V_2("Stitch V 2", 2, "Stitch"),
    STITCH_V_3("Stitch V 3", 3, "Stitch"),
    STITCH_V_4("Stitch V 4", 4, "Stitch"),
    STITCH_H_2("Stitch H 2", 2, "Stitch"),
    STITCH_H_3("Stitch H 3", 3, "Stitch"),
    STITCH_H_4("Stitch H 4", 4, "Stitch"),
    LAYOUT_016("Grid 2 · 016", 2, "Classic", 1),
    LAYOUT_017("Strip 3 · 017", 3, "Editorial", 2),
    LAYOUT_018("Hero Left 4 · 018", 4, "Hero", 3),
    LAYOUT_019("Hero Top 5 · 019", 5, "Hero", 4),
    LAYOUT_020("Bento 6 · 020", 6, "Bento", 5),
    LAYOUT_021("Masonry 7 · 021", 7, "Masonry", 6),
    LAYOUT_022("Grid 8 · 022", 8, "Classic", 7),
    LAYOUT_023("Strip 9 · 023", 9, "Editorial", 8),
    LAYOUT_024("Hero Left 10 · 024", 10, "Hero", 9),
    LAYOUT_025("Hero Top 11 · 025", 11, "Hero", 10),
    LAYOUT_026("Bento 12 · 026", 12, "Bento", 11),
    LAYOUT_027("Masonry 16 · 027", 16, "Masonry", 12),
    LAYOUT_028("Grid 20 · 028", 20, "Classic", 13),
    LAYOUT_029("Strip 2 · 029", 2, "Editorial", 14),
    LAYOUT_030("Hero Left 3 · 030", 3, "Hero", 15),
    LAYOUT_031("Hero Top 4 · 031", 4, "Hero", 16),
    LAYOUT_032("Bento 5 · 032", 5, "Bento", 17),
    LAYOUT_033("Masonry 6 · 033", 6, "Masonry", 18),
    LAYOUT_034("Grid 7 · 034", 7, "Classic", 19),
    LAYOUT_035("Strip 8 · 035", 8, "Editorial", 20),
    LAYOUT_036("Hero Left 9 · 036", 9, "Hero", 21),
    LAYOUT_037("Hero Top 10 · 037", 10, "Hero", 22),
    LAYOUT_038("Bento 11 · 038", 11, "Bento", 23),
    LAYOUT_039("Masonry 12 · 039", 12, "Masonry", 24),
    LAYOUT_040("Grid 16 · 040", 16, "Classic", 25),
    LAYOUT_041("Strip 20 · 041", 20, "Editorial", 26),
    LAYOUT_042("Hero Left 2 · 042", 2, "Hero", 27),
    LAYOUT_043("Hero Top 3 · 043", 3, "Hero", 28),
    LAYOUT_044("Bento 4 · 044", 4, "Bento", 29),
    LAYOUT_045("Masonry 5 · 045", 5, "Masonry", 30),
    LAYOUT_046("Grid 6 · 046", 6, "Classic", 31),
    LAYOUT_047("Strip 7 · 047", 7, "Editorial", 32),
    LAYOUT_048("Hero Left 8 · 048", 8, "Hero", 33),
    LAYOUT_049("Hero Top 9 · 049", 9, "Hero", 34),
    LAYOUT_050("Bento 10 · 050", 10, "Bento", 35),
    LAYOUT_051("Masonry 11 · 051", 11, "Masonry", 36),
    LAYOUT_052("Grid 12 · 052", 12, "Classic", 37),
    LAYOUT_053("Strip 16 · 053", 16, "Editorial", 38),
    LAYOUT_054("Hero Left 20 · 054", 20, "Hero", 39),
    LAYOUT_055("Hero Top 2 · 055", 2, "Hero", 40),
    LAYOUT_056("Bento 3 · 056", 3, "Bento", 41),
    LAYOUT_057("Masonry 4 · 057", 4, "Masonry", 42),
    LAYOUT_058("Grid 5 · 058", 5, "Classic", 43),
    LAYOUT_059("Strip 6 · 059", 6, "Editorial", 44),
    LAYOUT_060("Hero Left 7 · 060", 7, "Hero", 45),
    LAYOUT_061("Hero Top 8 · 061", 8, "Hero", 46),
    LAYOUT_062("Bento 9 · 062", 9, "Bento", 47),
    LAYOUT_063("Masonry 10 · 063", 10, "Masonry", 48),
    LAYOUT_064("Grid 11 · 064", 11, "Classic", 49),
    LAYOUT_065("Strip 12 · 065", 12, "Editorial", 50),
    LAYOUT_066("Hero Left 16 · 066", 16, "Hero", 51),
    LAYOUT_067("Hero Top 20 · 067", 20, "Hero", 52),
    LAYOUT_068("Bento 2 · 068", 2, "Bento", 53),
    LAYOUT_069("Masonry 3 · 069", 3, "Masonry", 54),
    LAYOUT_070("Grid 4 · 070", 4, "Classic", 55),
    LAYOUT_071("Strip 5 · 071", 5, "Editorial", 56),
    LAYOUT_072("Hero Left 6 · 072", 6, "Hero", 57),
    LAYOUT_073("Hero Top 7 · 073", 7, "Hero", 58),
    LAYOUT_074("Bento 8 · 074", 8, "Bento", 59),
    LAYOUT_075("Masonry 9 · 075", 9, "Masonry", 60),
    LAYOUT_076("Grid 10 · 076", 10, "Classic", 61),
    LAYOUT_077("Strip 11 · 077", 11, "Editorial", 62),
    LAYOUT_078("Hero Left 12 · 078", 12, "Hero", 63),
    LAYOUT_079("Hero Top 16 · 079", 16, "Hero", 64),
    LAYOUT_080("Bento 20 · 080", 20, "Bento", 65),
    LAYOUT_081("Masonry 2 · 081", 2, "Masonry", 66),
    LAYOUT_082("Grid 3 · 082", 3, "Classic", 67),
    LAYOUT_083("Strip 4 · 083", 4, "Editorial", 68),
    LAYOUT_084("Hero Left 5 · 084", 5, "Hero", 69),
    LAYOUT_085("Hero Top 6 · 085", 6, "Hero", 70),
    LAYOUT_086("Bento 7 · 086", 7, "Bento", 71),
    LAYOUT_087("Masonry 8 · 087", 8, "Masonry", 72),
    LAYOUT_088("Grid 9 · 088", 9, "Classic", 73),
    LAYOUT_089("Strip 10 · 089", 10, "Editorial", 74),
    LAYOUT_090("Hero Left 11 · 090", 11, "Hero", 75),
    LAYOUT_091("Hero Top 12 · 091", 12, "Hero", 76),
    LAYOUT_092("Bento 16 · 092", 16, "Bento", 77),
    LAYOUT_093("Masonry 20 · 093", 20, "Masonry", 78),
    LAYOUT_094("Grid 2 · 094", 2, "Classic", 79),
    LAYOUT_095("Strip 3 · 095", 3, "Editorial", 80),
    LAYOUT_096("Hero Left 4 · 096", 4, "Hero", 81),
    LAYOUT_097("Hero Top 5 · 097", 5, "Hero", 82),
    LAYOUT_098("Bento 6 · 098", 6, "Bento", 83),
    LAYOUT_099("Masonry 7 · 099", 7, "Masonry", 84),
    LAYOUT_100("Grid 8 · 100", 8, "Classic", 85),
    LAYOUT_101("Strip 9 · 101", 9, "Editorial", 86),
    LAYOUT_102("Hero Left 10 · 102", 10, "Hero", 87),
    LAYOUT_103("Hero Top 11 · 103", 11, "Hero", 88),
    LAYOUT_104("Bento 12 · 104", 12, "Bento", 89),
    LAYOUT_105("Masonry 16 · 105", 16, "Masonry", 90),
    LAYOUT_106("Grid 20 · 106", 20, "Classic", 91),
    LAYOUT_107("Strip 2 · 107", 2, "Editorial", 92),
    LAYOUT_108("Hero Left 3 · 108", 3, "Hero", 93),
    LAYOUT_109("Hero Top 4 · 109", 4, "Hero", 94),
    LAYOUT_110("Bento 5 · 110", 5, "Bento", 95),
    LAYOUT_111("Masonry 6 · 111", 6, "Masonry", 96),
    LAYOUT_112("Grid 7 · 112", 7, "Classic", 97),
    LAYOUT_113("Strip 8 · 113", 8, "Editorial", 98),
    LAYOUT_114("Hero Left 9 · 114", 9, "Hero", 99),
    LAYOUT_115("Hero Top 10 · 115", 10, "Hero", 100),
    LAYOUT_116("Bento 11 · 116", 11, "Bento", 101),
    LAYOUT_117("Masonry 12 · 117", 12, "Masonry", 102),
    LAYOUT_118("Grid 16 · 118", 16, "Classic", 103),
    LAYOUT_119("Strip 20 · 119", 20, "Editorial", 104),
    LAYOUT_120("Hero Left 2 · 120", 2, "Hero", 105),
    LAYOUT_121("Hero Top 3 · 121", 3, "Hero", 106),
    LAYOUT_122("Bento 4 · 122", 4, "Bento", 107),
    LAYOUT_123("Masonry 5 · 123", 5, "Masonry", 108),
    LAYOUT_124("Grid 6 · 124", 6, "Classic", 109),
    LAYOUT_125("Strip 7 · 125", 7, "Editorial", 110),
    LAYOUT_126("Hero Left 8 · 126", 8, "Hero", 111),
    LAYOUT_127("Hero Top 9 · 127", 9, "Hero", 112),
    LAYOUT_128("Bento 10 · 128", 10, "Bento", 113),
    LAYOUT_129("Masonry 11 · 129", 11, "Masonry", 114),
    LAYOUT_130("Grid 12 · 130", 12, "Classic", 115),
    LAYOUT_131("Strip 16 · 131", 16, "Editorial", 116),
    LAYOUT_132("Hero Left 20 · 132", 20, "Hero", 117),
    LAYOUT_133("Hero Top 2 · 133", 2, "Hero", 118),
    LAYOUT_134("Bento 3 · 134", 3, "Bento", 119),
    LAYOUT_135("Masonry 4 · 135", 4, "Masonry", 120),
    LAYOUT_136("Grid 5 · 136", 5, "Classic", 121),
    LAYOUT_137("Strip 6 · 137", 6, "Editorial", 122),
    LAYOUT_138("Hero Left 7 · 138", 7, "Hero", 123),
    LAYOUT_139("Hero Top 8 · 139", 8, "Hero", 124),
    LAYOUT_140("Bento 9 · 140", 9, "Bento", 125),
    LAYOUT_141("Masonry 10 · 141", 10, "Masonry", 126),
    LAYOUT_142("Grid 11 · 142", 11, "Classic", 127),
    LAYOUT_143("Strip 12 · 143", 12, "Editorial", 128),
    LAYOUT_144("Hero Left 16 · 144", 16, "Hero", 129),
    LAYOUT_145("Hero Top 20 · 145", 20, "Hero", 130),
    LAYOUT_146("Bento 2 · 146", 2, "Bento", 131),
    LAYOUT_147("Masonry 3 · 147", 3, "Masonry", 132),
    LAYOUT_148("Grid 4 · 148", 4, "Classic", 133),
    LAYOUT_149("Strip 5 · 149", 5, "Editorial", 134),
    LAYOUT_150("Hero Left 6 · 150", 6, "Hero", 135),
    LAYOUT_151("Hero Top 7 · 151", 7, "Hero", 136),
    LAYOUT_152("Bento 8 · 152", 8, "Bento", 137),
    LAYOUT_153("Masonry 9 · 153", 9, "Masonry", 138),
    LAYOUT_154("Grid 10 · 154", 10, "Classic", 139),
    LAYOUT_155("Strip 11 · 155", 11, "Editorial", 140),
    LAYOUT_156("Hero Left 12 · 156", 12, "Hero", 141),
    LAYOUT_157("Hero Top 16 · 157", 16, "Hero", 142),
    LAYOUT_158("Bento 20 · 158", 20, "Bento", 143),
    LAYOUT_159("Masonry 2 · 159", 2, "Masonry", 144),
    LAYOUT_160("Grid 3 · 160", 3, "Classic", 145),
    LAYOUT_161("Strip 4 · 161", 4, "Editorial", 146),
    LAYOUT_162("Hero Left 5 · 162", 5, "Hero", 147),
    LAYOUT_163("Hero Top 6 · 163", 6, "Hero", 148),
    LAYOUT_164("Bento 7 · 164", 7, "Bento", 149),
    LAYOUT_165("Masonry 8 · 165", 8, "Masonry", 150),
    LAYOUT_166("Grid 9 · 166", 9, "Classic", 151),
    LAYOUT_167("Strip 10 · 167", 10, "Editorial", 152),
    LAYOUT_168("Hero Left 11 · 168", 11, "Hero", 153),
    LAYOUT_169("Hero Top 12 · 169", 12, "Hero", 154),
    LAYOUT_170("Bento 16 · 170", 16, "Bento", 155),
    LAYOUT_171("Masonry 20 · 171", 20, "Masonry", 156),
    LAYOUT_172("Grid 2 · 172", 2, "Classic", 157),
    LAYOUT_173("Strip 3 · 173", 3, "Editorial", 158),
    LAYOUT_174("Hero Left 4 · 174", 4, "Hero", 159),
    LAYOUT_175("Hero Top 5 · 175", 5, "Hero", 160),
    LAYOUT_176("Bento 6 · 176", 6, "Bento", 161),
    LAYOUT_177("Masonry 7 · 177", 7, "Masonry", 162),
    LAYOUT_178("Grid 8 · 178", 8, "Classic", 163),
    LAYOUT_179("Strip 9 · 179", 9, "Editorial", 164),
    LAYOUT_180("Hero Left 10 · 180", 10, "Hero", 165),
    LAYOUT_181("Hero Top 11 · 181", 11, "Hero", 166),
    LAYOUT_182("Bento 12 · 182", 12, "Bento", 167),
    LAYOUT_183("Masonry 16 · 183", 16, "Masonry", 168),
    LAYOUT_184("Grid 20 · 184", 20, "Classic", 169),
    LAYOUT_185("Strip 2 · 185", 2, "Editorial", 170),
    LAYOUT_186("Hero Left 3 · 186", 3, "Hero", 171),
    LAYOUT_187("Hero Top 4 · 187", 4, "Hero", 172),
    LAYOUT_188("Bento 5 · 188", 5, "Bento", 173),
    LAYOUT_189("Masonry 6 · 189", 6, "Masonry", 174),
    LAYOUT_190("Grid 7 · 190", 7, "Classic", 175),
    LAYOUT_191("Strip 8 · 191", 8, "Editorial", 176),
    LAYOUT_192("Hero Left 9 · 192", 9, "Hero", 177),
    LAYOUT_193("Hero Top 10 · 193", 10, "Hero", 178),
    LAYOUT_194("Bento 11 · 194", 11, "Bento", 179),
    LAYOUT_195("Masonry 12 · 195", 12, "Masonry", 180),
    LAYOUT_196("Grid 16 · 196", 16, "Classic", 181),
    LAYOUT_197("Strip 20 · 197", 20, "Editorial", 182),
    LAYOUT_198("Hero Left 2 · 198", 2, "Hero", 183),
    LAYOUT_199("Hero Top 3 · 199", 3, "Hero", 184),
    LAYOUT_200("Bento 4 · 200", 4, "Bento", 185),
    LAYOUT_201("Masonry 5 · 201", 5, "Masonry", 186),
    LAYOUT_202("Grid 6 · 202", 6, "Classic", 187),
    LAYOUT_203("Strip 7 · 203", 7, "Editorial", 188),
    LAYOUT_204("Hero Left 8 · 204", 8, "Hero", 189),
    LAYOUT_205("Hero Top 9 · 205", 9, "Hero", 190),
    LAYOUT_206("Bento 10 · 206", 10, "Bento", 191),
    LAYOUT_207("Masonry 11 · 207", 11, "Masonry", 192);

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
        STITCH_V_2, STITCH_V_3, STITCH_V_4 -> (0 until slotCount).map { idx ->
            val h = 1f / slotCount
            SlotRect(0f, idx * h, 1f, h)
        }
        STITCH_H_2, STITCH_H_3, STITCH_H_4 -> (0 until slotCount).map { idx ->
            val w = 1f / slotCount
            SlotRect(idx * w, 0f, w, 1f)
        }
        else -> generatedSlotRects()
    }

    private fun generatedSlotRects(): List<SlotRect> {
        val n = slotCount.coerceAtLeast(1)
        if (n == 1) return listOf(SlotRect(0f, 0f, 1f, 1f))
        // Generated variants are numbered from 1. Keep the family index aligned
        // with the visible name sequence: Grid, Strip, Hero Left, Hero Top, Bento, Masonry.
        val normalizedVariant = (variant - 1).coerceAtLeast(0)
        val seed = normalizedVariant / 6
        val family = normalizedVariant % 6
        fun grid(count: Int, left: Float, top: Float, width: Float, height: Float, columns: Int): List<SlotRect> {
            if (count <= 0) return emptyList()
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
            2 -> { val hero = (0.54f + (seed % 5) * 0.025f).coerceAtMost(0.66f); listOf(SlotRect(0f, 0f, hero, 1f)) + (1 until n).map { i -> val h = 1f / (n - 1); SlotRect(hero, (i - 1) * h, 1f - hero, h) } }
            3 -> { val hero = (0.54f + (seed % 4) * 0.025f).coerceAtMost(0.66f); listOf(SlotRect(0f, 0f, 1f, hero)) + grid(n - 1, 0f, hero, 1f, 1f - hero, minOf(4, n - 1)) }
            4 -> {
                // Bento: a dominant feature tile with a distinct right rail and lower row.
                if (n == 2) listOf(SlotRect(0f, 0f, 0.62f, 1f), SlotRect(0.62f, 0f, 0.38f, 1f))
                else if (n == 3) listOf(SlotRect(0f, 0f, 0.62f, 1f), SlotRect(0.62f, 0f, 0.38f, 0.5f), SlotRect(0.62f, 0.5f, 0.38f, 0.5f))
                else {
                    val heroW = 0.62f + (seed % 3) * 0.02f
                    val railW = 1f - heroW
                    val heroH = 0.62f
                    listOf(SlotRect(0f, 0f, heroW, heroH), SlotRect(heroW, 0f, railW, heroH)) +
                        grid(n - 2, 0f, heroH, 1f, 1f - heroH, minOf(4, n - 2))
                }
            }
            else -> { val cols = if (n < 5) 2 else 3; val colWidth = 1f / cols; (0 until n).groupBy { it % cols }.toSortedMap().flatMap { (col, indices) -> val weights = indices.mapIndexed { j, _ -> 0.82f + (((j + seed + col) % 4) * 0.12f) }; val total = weights.sum(); var y = 0f; indices.mapIndexed { j, _ -> val h = weights[j] / total; SlotRect(col * colWidth, y, colWidth, h).also { y += h } } } }
        }
    }
}

fun LayoutStyle.pickerCategory(): String = when {
    this == LayoutStyle.POLAROID_2 -> "Polaroid"
    this == LayoutStyle.FILM_3 -> "Film-Strip"
    this == LayoutStyle.SLANT_SPLIT_2 || this == LayoutStyle.STAR_FEATURE_3 ||
        this == LayoutStyle.HEART_COLLAGE_3 || this == LayoutStyle.HEX_HONEYCOMB_4 -> "Geometric"
    category.equals("Bento", ignoreCase = true) || this == LayoutStyle.BENTO_4 -> "Bento"
    category.equals("Editorial", ignoreCase = true) -> "Magazine"
    category.equals("Masonry", ignoreCase = true) -> "Scrapbook"
    category.equals("Stitch", ignoreCase = true) -> "Stitch"
    category.equals("Hero", ignoreCase = true) -> "Asymmetric"
    else -> "Grid"
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

enum class CollageFrameStyle(val label: String) {
    NONE("None"),
    MINIMAL("Minimal"),
    DOUBLE_LINE("Double Line"),
    POLAROID("Polaroid"),
    FILM("Film Strip"),
    SHADOW_CARD("Shadow Card"),
    TORN_PAPER("Torn Paper"),
    WASHI_TAPE("Washi Tape")
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
    PHOTOS("Photos"),
    TEXT("Text"),
    STICKERS("Stickers"),
    FRAMES("Frames"),
    BACKGROUND("Background"),
    DRAWING("Drawing"),
    FILTERS("Filters"),
    ADJUSTMENTS("Adjust"),
    LAYERS("Layers")
}

enum class ExportMode(val label: String) {
    AUTO_SLICE("Individual Slides"),
    SINGLE_MERGED("Merged Image"),
    SELECTED_SLIDE("Selected Slide Only")
}

enum class SlideOrder(val label: String) {
    FORWARD("Forward (1 -> N)"),
    REVERSE("Reverse (N -> 1)")
}

enum class ResolutionPreset(val label: String, val baseWidth: Int) {
    IG_1080("HD (1080px)", 1080),
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
    PNG("PNG", "png", "image/png"),
    WEBP("WebP", "webp", "image/webp")
}

data class DrawingPoint(
    val x: Float,
    val y: Float
)

data class DrawingStroke(
    val id: String,
    val points: List<DrawingPoint>,
    val colorHex: Long = 0xFFFFFFFF,
    val strokeWidthDp: Float = 4f,
    val opacity: Float = 1f
)

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
    val visible: Boolean = true,
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
    val customFontPath: String? = null,
    val alignment: TextAlignOption = TextAlignOption.CENTER,
    val opacity: Float = 1f,
    val isSticker: Boolean = false,
    val stickerUri: Uri? = null,
    val stickerSizeFraction: Float = 0.22f,
    val visible: Boolean = true,
    val locked: Boolean = false,
    val zIndex: Int = 100,
    val useGradient: Boolean = false,
    val gradientEndHex: Long = 0xFF5B8DEF,
    val outlineColorHex: Long = 0xFF000000,
    val outlineWidthDp: Float = 2f,
    val useShadow: Boolean = false,
    val shadowRadiusDp: Float = 6f,
    val shadowColorHex: Long = 0xCC000000,
    val letterSpacingEm: Float = 0f,
    val lineHeightMultiplier: Float = 1.25f,
    val bgColorHex: Long = 0xCC0D0F13,
    val paddingDp: Int = 10
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
    val transparentBackground: Boolean = false,
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
    val outerMarginDp: Int = 2,
    val borderDp: Int = 2,
    val cornerRadiusDp: Int = 2,
    val frameStyle: CollageFrameStyle = CollageFrameStyle.NONE,
    val backgroundStyle: BackgroundStyle = BackgroundStyle.OBSIDIAN,
    val customBgHex: Long? = null,
    val bgGradientAngleDeg: Float = 135f,
    val borderColorHex: Long? = null,
    val canvasTexture: CanvasTexture = CanvasTexture.NONE,
    val canvasLocked: Boolean = false,
    val swapSourceIndex: Int? = null,
    val selectedSlotIndex: Int = 0,
    val selectedOverlayId: String? = null,
    val slots: List<MediaItem> = emptyList(),
    val extras: List<MediaItem> = emptyList(),
    val overlays: List<TextOverlay> = emptyList(),
    val drawingStrokes: List<DrawingStroke> = emptyList(),
    val drawingsVisible: Boolean = true,
    val drawingsLocked: Boolean = false,
    val exportConfig: ExportConfig = ExportConfig()
) {
    fun maxCapacity(): Int = when (mode) {
        StudioMode.CAROUSEL -> carouselSlides * 2
        StudioMode.GRID, StudioMode.FREESTYLE -> layout.slotCount
    }
}
