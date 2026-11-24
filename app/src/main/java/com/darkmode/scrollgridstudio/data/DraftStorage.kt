package com.darkmode.scrollgridstudio.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.darkmode.scrollgridstudio.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class ImportedFont(
    val name: String,
    val path: String
)

object DraftStorage {
    private const val PREFS_NAME = "scrollgrid_drafts_v1"
    private const val KEY_INDEX = "draft_index"
    private const val KEY_FAVORITE_LAYOUTS = "favorite_layouts"

    fun getFavoriteLayouts(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_FAVORITE_LAYOUTS, emptySet())?.toSet() ?: emptySet()
    }

    fun toggleFavoriteLayout(context: Context, layoutName: String): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = getFavoriteLayouts(context).toMutableSet()
        if (!current.add(layoutName)) current.remove(layoutName)
        prefs.edit().putStringSet(KEY_FAVORITE_LAYOUTS, current).apply()
        return current
    }

    fun listImportedFonts(context: Context): List<ImportedFont> {
        val dir = File(context.filesDir, "imported_fonts")
        if (!dir.exists()) return emptyList()
        return dir.listFiles()
            ?.filter { it.isFile && (it.name.endsWith(".ttf", true) || it.name.endsWith(".otf", true)) }
            ?.sortedBy { it.name.lowercase() }
            ?.map { ImportedFont(name = it.nameWithoutExtension, path = it.absolutePath) }
            ?: emptyList()
    }

    fun importFontFromUri(context: Context, uri: Uri): ImportedFont? {
        return runCatching {
            val dir = File(context.filesDir, "imported_fonts").apply { mkdirs() }
            var displayName = "CustomFont_${System.currentTimeMillis()}.ttf"
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val raw = c.getString(0)
                    if (!raw.isNullOrBlank()) displayName = raw
                }
            }
            val safeName = displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val finalName = if (safeName.endsWith(".ttf", true) || safeName.endsWith(".otf", true)) safeName else "$safeName.ttf"
            val dest = File(dir, finalName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            ImportedFont(name = dest.nameWithoutExtension, path = dest.absolutePath)
        }.getOrNull()
    }

    fun listDrafts(context: Context): List<SavedDraftMeta> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_INDEX, "[]") ?: "[]"
        val arr = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        val result = mutableListOf<SavedDraftMeta>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val previewArr = obj.optJSONArray("previewUris")
            val previews = buildList {
                if (previewArr != null) {
                    for (p in 0 until previewArr.length()) {
                        val u = previewArr.optString(p)
                        if (u.isNotBlank()) add(u)
                    }
                }
            }
            result.add(
                SavedDraftMeta(
                    id = obj.optString("id"),
                    name = obj.optString("name", "Untitled Collage"),
                    updatedAtMillis = obj.optLong("updatedAt", 0L),
                    modeTitle = obj.optString("modeTitle", "Grid"),
                    itemCount = obj.optInt("itemCount", 0),
                    previewUris = previews
                )
            )
        }
        return result.sortedByDescending { it.updatedAtMillis }
    }

    fun saveDraft(context: Context, state: StudioState, existingId: String? = null): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val id = existingId ?: UUID.randomUUID().toString()
        val payload = serializeState(state).toString()

        val rawIndex = prefs.getString(KEY_INDEX, "[]") ?: "[]"
        val arr = runCatching { JSONArray(rawIndex) }.getOrElse { JSONArray() }
        val newArr = JSONArray()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            if (obj.optString("id") != id) newArr.put(obj)
        }
        val previews = JSONArray().apply {
            state.slots.take(4).forEach { put(it.uri.toString()) }
        }
        newArr.put(
            JSONObject().apply {
                put("id", id)
                put("name", state.projectName.ifBlank { "Untitled Collage" })
                put("updatedAt", System.currentTimeMillis())
                put("modeTitle", state.mode.title)
                put("itemCount", state.slots.size)
                put("previewUris", previews)
            }
        )
        prefs.edit()
            .putString("draft_$id", payload)
            .putString(KEY_INDEX, newArr.toString())
            .apply()
        return id
    }

    fun duplicateDraft(context: Context, id: String): String? {
        val existing = loadDraft(context, id) ?: return null
        return saveDraft(context, existing.copy(projectName = "${existing.projectName} Copy"), null)
    }

    fun loadDraft(context: Context, id: String): StudioState? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("draft_$id", null) ?: return null
        return runCatching { deserializeState(JSONObject(raw)) }.getOrNull()
    }

    fun deleteDraft(context: Context, id: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawIndex = prefs.getString(KEY_INDEX, "[]") ?: "[]"
        val arr = runCatching { JSONArray(rawIndex) }.getOrElse { JSONArray() }
        val newArr = JSONArray()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            if (obj.optString("id") != id) newArr.put(obj)
        }
        prefs.edit()
            .remove("draft_$id")
            .putString(KEY_INDEX, newArr.toString())
            .apply()
    }

    private fun serializeState(state: StudioState): JSONObject {
        return JSONObject().apply {
            put("projectName", state.projectName)
            put("mode", state.mode.name)
            put("layout", state.layout.name)
            put("carouselSlides", state.carouselSlides)
            put("activeSlideIndex", state.activeSlideIndex)
            put("showGuides", state.showGuides)
            put("canvasLocked", state.canvasLocked)
            put("activeTab", state.activeTab.name)
            put("selectedSlotIndex", state.selectedSlotIndex)
            put("outerMarginDp", state.outerMarginDp)
            put("borderDp", state.borderDp)
            put("cornerRadiusDp", state.cornerRadiusDp)
            put("frameStyle", state.frameStyle.name)
            put("backgroundStyle", state.backgroundStyle.name)
            put("customBgHex", state.customBgHex ?: -1L)
            put("bgGradientAngleDeg", state.bgGradientAngleDeg.toDouble())
            put("borderColorHex", state.borderColorHex ?: -1L)
            put("canvasTexture", state.canvasTexture.name)
            put("drawingsVisible", state.drawingsVisible)
            put("drawingsLocked", state.drawingsLocked)
            put("aspectRatio", state.exportConfig.aspectRatio.name)
            put("resolution", state.exportConfig.resolution.name)
            put("format", state.exportConfig.format.name)
            put("quality", state.exportConfig.qualityPercent)
            put("transparentBackground", state.exportConfig.transparentBackground)
            put("exportMode", state.exportConfig.mode.name)
            put("slideOrder", state.exportConfig.slideOrder.name)
            put("selectedSlideNumber", state.exportConfig.selectedSlideNumber)
            put("customWidthPx", state.exportConfig.customWidthPx)
            put("customHeightPx", state.exportConfig.customHeightPx)
            put("stripExif", state.exportConfig.stripExif)
            put("includeSignature", state.exportConfig.includeSignature)
            put("signatureText", state.exportConfig.signatureText)
            put("extras", JSONArray().apply { state.extras.forEach { put(serializeMediaItem(it)) } })

            val slotsArr = JSONArray()
            state.slots.forEach { slotsArr.put(serializeMediaItem(it)) }
            put("slots", slotsArr)

            val ovArr = JSONArray()
            state.overlays.forEach { ov ->
                ovArr.put(JSONObject().apply {
                    put("id", ov.id)
                    put("text", ov.text)
                    put("xFraction", ov.xFraction.toDouble())
                    put("yFraction", ov.yFraction.toDouble())
                    put("fontSizeSp", ov.fontSizeSp.toDouble())
                    put("rotationDeg", ov.rotationDeg.toDouble())
                    put("usePill", ov.usePill)
                    put("textColorHex", ov.textColorHex)
                    put("fontStyle", ov.fontStyle.name)
                    put("fontName", ov.fontName)
                    put("customFontPath", ov.customFontPath ?: "")
                    put("alignment", ov.alignment.name)
                    put("opacity", ov.opacity.toDouble())
                    put("isSticker", ov.isSticker)
                    put("stickerUri", ov.stickerUri?.toString() ?: "")
                    put("useOutline", ov.useOutline)
                    put("stickerSizeFraction", ov.stickerSizeFraction.toDouble())
                    put("visible", ov.visible)
                    put("locked", ov.locked)
                    put("zIndex", ov.zIndex)
                    put("useGradient", ov.useGradient)
                    put("gradientEndHex", ov.gradientEndHex)
                    put("outlineColorHex", ov.outlineColorHex)
                    put("outlineWidthDp", ov.outlineWidthDp.toDouble())
                    put("useShadow", ov.useShadow)
                    put("shadowRadiusDp", ov.shadowRadiusDp.toDouble())
                    put("shadowColorHex", ov.shadowColorHex)
                    put("letterSpacingEm", ov.letterSpacingEm.toDouble())
                    put("lineHeightMultiplier", ov.lineHeightMultiplier.toDouble())
                    put("bgColorHex", ov.bgColorHex)
                    put("paddingDp", ov.paddingDp)
                })
            }
            put("overlays", ovArr)

            val strokesArr = JSONArray()
            state.drawingStrokes.forEach { stroke ->
                strokesArr.put(JSONObject().apply {
                    put("id", stroke.id)
                    put("colorHex", stroke.colorHex)
                    put("strokeWidthDp", stroke.strokeWidthDp.toDouble())
                    put("opacity", stroke.opacity.toDouble())
                    put("points", JSONArray().apply {
                        stroke.points.forEach { pt ->
                            put(JSONObject().apply {
                                put("x", pt.x.toDouble())
                                put("y", pt.y.toDouble())
                            })
                        }
                    })
                })
            }
            put("drawingStrokes", strokesArr)
        }
    }

    private fun serializeMediaItem(item: MediaItem): JSONObject = JSONObject().apply {
        put("id", item.id); put("uri", item.uri.toString()); put("isVideo", item.isVideo)
        put("scale", item.scale.toDouble()); put("panX", item.panX.toDouble()); put("panY", item.panY.toDouble())
        put("rotationDeg", item.rotationDeg.toDouble()); put("flipHorizontal", item.flipHorizontal); put("fitInside", item.fitInside)
        put("brightness", item.brightness.toDouble()); put("contrast", item.contrast.toDouble()); put("saturation", item.saturation.toDouble())
        put("warmth", item.warmth.toDouble()); put("vignette", item.vignette.toDouble())
        put("freeX", item.freeX.toDouble()); put("freeY", item.freeY.toDouble()); put("freeW", item.freeW.toDouble()); put("freeH", item.freeH.toDouble())
        put("zIndex", item.zIndex); put("locked", item.locked); put("visible", item.visible); put("opacity", item.opacity.toDouble())
        put("shadowDp", item.shadowDp); put("maskShape", item.maskShape.name)
    }

    private fun deserializeMediaItem(s: JSONObject, index: Int): MediaItem? {
        val uriStr = s.optString("uri")
        if (uriStr.isBlank()) return null
        return MediaItem(
            id = s.optString("id", UUID.randomUUID().toString()),
            uri = Uri.parse(uriStr),
            isVideo = s.optBoolean("isVideo", false),
            scale = s.optDouble("scale", 1.0).toFloat(),
            panX = s.optDouble("panX", 0.0).toFloat(),
            panY = s.optDouble("panY", 0.0).toFloat(),
            rotationDeg = s.optDouble("rotationDeg", 0.0).toFloat(),
            flipHorizontal = s.optBoolean("flipHorizontal", false),
            fitInside = s.optBoolean("fitInside", false),
            brightness = s.optDouble("brightness", 0.0).toFloat(),
            contrast = s.optDouble("contrast", 1.0).toFloat(),
            saturation = s.optDouble("saturation", 1.0).toFloat(),
            warmth = s.optDouble("warmth", 0.0).toFloat(),
            vignette = s.optDouble("vignette", 0.0).toFloat(),
            freeX = s.optDouble("freeX", 0.25).toFloat(),
            freeY = s.optDouble("freeY", 0.25).toFloat(),
            freeW = s.optDouble("freeW", 0.42).toFloat(),
            freeH = s.optDouble("freeH", 0.42).toFloat(),
            zIndex = s.optInt("zIndex", index),
            locked = s.optBoolean("locked", false),
            visible = s.optBoolean("visible", true),
            opacity = s.optDouble("opacity", 1.0).toFloat(),
            shadowDp = s.optInt("shadowDp", 10),
            maskShape = runCatching { TileMaskShape.valueOf(s.optString("maskShape")) }.getOrDefault(TileMaskShape.RECTANGLE)
        )
    }

    private fun deserializeState(obj: JSONObject): StudioState {
        val sArr = obj.optJSONArray("slots") ?: JSONArray()
        val slots = (0 until sArr.length()).mapNotNull { i -> deserializeMediaItem(sArr.optJSONObject(i) ?: JSONObject(), i) }
        val eArr = obj.optJSONArray("extras") ?: JSONArray()
        val extras = (0 until eArr.length()).mapNotNull { i -> deserializeMediaItem(eArr.optJSONObject(i) ?: JSONObject(), i) }

        val overlays = mutableListOf<TextOverlay>()
        val oArr = obj.optJSONArray("overlays") ?: JSONArray()
        for (i in 0 until oArr.length()) {
            val o = oArr.getJSONObject(i)
            val stickerStr = o.optString("stickerUri", "")
            val fontPath = o.optString("customFontPath", "").takeIf { it.isNotBlank() }
            overlays.add(
                TextOverlay(
                    id = o.optString("id", UUID.randomUUID().toString()),
                    text = o.optString("text", ""),
                    xFraction = o.optDouble("xFraction", 0.5).toFloat(),
                    yFraction = o.optDouble("yFraction", 0.5).toFloat(),
                    fontSizeSp = o.optDouble("fontSizeSp", 22.0).toFloat(),
                    rotationDeg = o.optDouble("rotationDeg", 0.0).toFloat(),
                    usePill = o.optBoolean("usePill", true),
                    textColorHex = o.optLong("textColorHex", 0xFFF3F4F6),
                    fontStyle = runCatching { TextFontStyle.valueOf(o.optString("fontStyle")) }.getOrDefault(TextFontStyle.SANS_BOLD),
                    fontName = o.optString("fontName", "Roboto"),
                    customFontPath = fontPath,
                    alignment = runCatching { TextAlignOption.valueOf(o.optString("alignment")) }.getOrDefault(TextAlignOption.CENTER),
                    opacity = o.optDouble("opacity", 1.0).toFloat(),
                    isSticker = o.optBoolean("isSticker", false),
                    stickerUri = if (stickerStr.isNotBlank()) Uri.parse(stickerStr) else null,
                    useOutline = o.optBoolean("useOutline", true),
                    stickerSizeFraction = o.optDouble("stickerSizeFraction", 0.22).toFloat(),
                    visible = o.optBoolean("visible", true),
                    locked = o.optBoolean("locked", false),
                    zIndex = o.optInt("zIndex", 100 + i),
                    useGradient = o.optBoolean("useGradient", false),
                    gradientEndHex = o.optLong("gradientEndHex", 0xFF5B8DEF),
                    outlineColorHex = o.optLong("outlineColorHex", 0xFF000000),
                    outlineWidthDp = o.optDouble("outlineWidthDp", 2.0).toFloat(),
                    useShadow = o.optBoolean("useShadow", false),
                    shadowRadiusDp = o.optDouble("shadowRadiusDp", 6.0).toFloat(),
                    shadowColorHex = o.optLong("shadowColorHex", 0xCC000000),
                    letterSpacingEm = o.optDouble("letterSpacingEm", 0.0).toFloat(),
                    lineHeightMultiplier = o.optDouble("lineHeightMultiplier", 1.25).toFloat(),
                    bgColorHex = o.optLong("bgColorHex", 0xCC0D0F13),
                    paddingDp = o.optInt("paddingDp", 10)
                )
            )
        }

        val strokes = mutableListOf<DrawingStroke>()
        val dArr = obj.optJSONArray("drawingStrokes") ?: JSONArray()
        for (i in 0 until dArr.length()) {
            val dObj = dArr.optJSONObject(i) ?: continue
            val ptsArr = dObj.optJSONArray("points") ?: continue
            val pts = buildList {
                for (p in 0 until ptsArr.length()) {
                    val po = ptsArr.optJSONObject(p) ?: continue
                    add(DrawingPoint(po.optDouble("x", 0.0).toFloat(), po.optDouble("y", 0.0).toFloat()))
                }
            }
            if (pts.size >= 2) {
                strokes.add(
                    DrawingStroke(
                        id = dObj.optString("id", UUID.randomUUID().toString()),
                        points = pts,
                        colorHex = dObj.optLong("colorHex", 0xFFFFFFFF),
                        strokeWidthDp = dObj.optDouble("strokeWidthDp", 4.0).toFloat(),
                        opacity = dObj.optDouble("opacity", 1.0).toFloat()
                    )
                )
            }
        }

        val customBg = obj.optLong("customBgHex", -1L).takeIf { it != -1L }
        val borderCol = obj.optLong("borderColorHex", -1L).takeIf { it != -1L }

        return StudioState(
            projectName = obj.optString("projectName", "Untitled Collage"),
            mode = runCatching { StudioMode.valueOf(obj.optString("mode")) }.getOrDefault(StudioMode.GRID),
            layout = runCatching { LayoutStyle.valueOf(obj.optString("layout")) }.getOrDefault(LayoutStyle.GRID_2X2),
            carouselSlides = obj.optInt("carouselSlides", 3),
            outerMarginDp = obj.optInt("outerMarginDp", 2),
            borderDp = obj.optInt("borderDp", 2),
            cornerRadiusDp = obj.optInt("cornerRadiusDp", 2),
            frameStyle = runCatching { CollageFrameStyle.valueOf(obj.optString("frameStyle")) }.getOrDefault(CollageFrameStyle.NONE),
            backgroundStyle = runCatching { BackgroundStyle.valueOf(obj.optString("backgroundStyle")) }.getOrDefault(BackgroundStyle.OBSIDIAN),
            customBgHex = customBg,
            bgGradientAngleDeg = obj.optDouble("bgGradientAngleDeg", 135.0).toFloat(),
            borderColorHex = borderCol,
            canvasTexture = runCatching { CanvasTexture.valueOf(obj.optString("canvasTexture")) }.getOrDefault(CanvasTexture.NONE),
            activeSlideIndex = obj.optInt("activeSlideIndex", 0),
            showGuides = obj.optBoolean("showGuides", true),
            canvasLocked = obj.optBoolean("canvasLocked", false),
            activeTab = runCatching { StudioTab.valueOf(obj.optString("activeTab")) }.getOrDefault(StudioTab.LAYOUT),
            selectedSlotIndex = obj.optInt("selectedSlotIndex", 0).coerceAtLeast(0),
            slots = slots,
            extras = extras,
            overlays = overlays,
            drawingStrokes = strokes,
            drawingsVisible = obj.optBoolean("drawingsVisible", true),
            drawingsLocked = obj.optBoolean("drawingsLocked", false),
            exportConfig = ExportConfig(
                mode = runCatching { ExportMode.valueOf(obj.optString("exportMode")) }.getOrDefault(ExportMode.AUTO_SLICE),
                slideOrder = runCatching { SlideOrder.valueOf(obj.optString("slideOrder")) }.getOrDefault(SlideOrder.FORWARD),
                selectedSlideNumber = obj.optInt("selectedSlideNumber", 1),
                aspectRatio = runCatching { AspectRatioOption.valueOf(obj.optString("aspectRatio")) }.getOrDefault(AspectRatioOption.PORTRAIT_4_5),
                resolution = runCatching { ResolutionPreset.valueOf(obj.optString("resolution")) }.getOrDefault(ResolutionPreset.IG_1080),
                format = runCatching { ExportFormat.valueOf(obj.optString("format")) }.getOrDefault(ExportFormat.JPG),
                qualityPercent = obj.optInt("quality", 95),
                transparentBackground = obj.optBoolean("transparentBackground", false),
                customWidthPx = obj.optInt("customWidthPx", 1080),
                customHeightPx = obj.optInt("customHeightPx", 1350),
                stripExif = obj.optBoolean("stripExif", true),
                includeSignature = obj.optBoolean("includeSignature", false),
                signatureText = obj.optString("signatureText", "@sandeep_njr")
            )
        )
    }
}
