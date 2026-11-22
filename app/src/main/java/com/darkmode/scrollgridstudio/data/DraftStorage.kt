package com.darkmode.scrollgridstudio.data

import android.content.Context
import android.net.Uri
import com.darkmode.scrollgridstudio.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object DraftStorage {
    private const val PREFS_NAME = "scrollgrid_drafts_v1"
    private const val KEY_INDEX = "draft_ids"

    fun listDrafts(context: Context): List<SavedDraftMeta> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val idsRaw = prefs.getString(KEY_INDEX, "[]") ?: "[]"
        val arr = JSONArray(idsRaw)
        val out = mutableListOf<SavedDraftMeta>()
        for (i in 0 until arr.length()) {
            val id = arr.optString(i)
            val jsonStr = prefs.getString("draft_$id", null) ?: continue
            runCatching {
                val obj = JSONObject(jsonStr)
                val slotsArr = obj.optJSONArray("slots") ?: JSONArray()
                val previews = mutableListOf<String>()
                for (j in 0 until minOf(slotsArr.length(), 4)) {
                    val u = slotsArr.optJSONObject(j)?.optString("uri", "") ?: ""
                    if (u.isNotBlank()) previews.add(u)
                }
                out.add(
                    SavedDraftMeta(
                        id = id,
                        name = obj.optString("projectName", "Untitled Collage"),
                        updatedAtMillis = obj.optLong("updatedAt", 0L),
                        modeTitle = obj.optString("mode", StudioMode.GRID.name),
                        itemCount = slotsArr.length(),
                        previewUris = previews
                    )
                )
            }
        }
        return out.sortedByDescending { it.updatedAtMillis }
    }

    fun saveDraft(context: Context, state: StudioState, existingId: String? = null): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val id = existingId ?: UUID.randomUUID().toString()
        val obj = serializeState(state)
        obj.put("updatedAt", System.currentTimeMillis())

        val ids = mutableListOf<String>()
        val arr = JSONArray(prefs.getString(KEY_INDEX, "[]") ?: "[]")
        for (i in 0 until arr.length()) {
            val item = arr.optString(i)
            if (item.isNotBlank() && item != id) ids.add(item)
        }
        ids.add(0, id)

        val newArr = JSONArray()
        ids.take(30).forEach { newArr.put(it) }

        prefs.edit()
            .putString("draft_$id", obj.toString())
            .putString(KEY_INDEX, newArr.toString())
            .apply()
        return id
    }

    fun duplicateDraft(context: Context, id: String): String? {
        val loaded = loadDraft(context, id) ?: return null
        return saveDraft(context, loaded.copy(projectName = "${loaded.projectName} Copy"), existingId = null)
    }

    fun loadDraft(context: Context, id: String): StudioState? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("draft_$id", null) ?: return null
        return runCatching { deserializeState(JSONObject(raw)) }.getOrNull()
    }

    fun deleteDraft(context: Context, id: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString(KEY_INDEX, "[]") ?: "[]")
        val newArr = JSONArray()
        for (i in 0 until arr.length()) {
            val item = arr.optString(i)
            if (item != id && item.isNotBlank()) newArr.put(item)
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
            put("backgroundStyle", state.backgroundStyle.name)
            put("customBgHex", state.customBgHex ?: -1L)
            put("borderColorHex", state.borderColorHex ?: -1L)
            put("canvasTexture", state.canvasTexture.name)
            put("aspectRatio", state.exportConfig.aspectRatio.name)
            put("resolution", state.exportConfig.resolution.name)
            put("format", state.exportConfig.format.name)
            put("quality", state.exportConfig.qualityPercent)
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
                    put("alignment", ov.alignment.name)
                    put("opacity", ov.opacity.toDouble())
                    put("isSticker", ov.isSticker)
                    put("stickerUri", ov.stickerUri?.toString() ?: "")
                    put("useOutline", ov.useOutline)
                    put("stickerSizeFraction", ov.stickerSizeFraction.toDouble())
                })
            }
            put("overlays", ovArr)
        }
    }

    private fun serializeMediaItem(item: MediaItem): JSONObject = JSONObject().apply {
        put("id", item.id); put("uri", item.uri.toString()); put("isVideo", item.isVideo)
        put("scale", item.scale.toDouble()); put("panX", item.panX.toDouble()); put("panY", item.panY.toDouble())
        put("rotationDeg", item.rotationDeg.toDouble()); put("flipHorizontal", item.flipHorizontal); put("fitInside", item.fitInside)
        put("brightness", item.brightness.toDouble()); put("contrast", item.contrast.toDouble()); put("saturation", item.saturation.toDouble())
        put("warmth", item.warmth.toDouble()); put("vignette", item.vignette.toDouble())
        put("freeX", item.freeX.toDouble()); put("freeY", item.freeY.toDouble()); put("freeW", item.freeW.toDouble()); put("freeH", item.freeH.toDouble())
        put("zIndex", item.zIndex); put("locked", item.locked); put("opacity", item.opacity.toDouble())
        put("shadowDp", item.shadowDp); put("maskShape", item.maskShape.name)
    }

    private fun deserializeMediaItem(s: JSONObject, index: Int): MediaItem? {
        val uriStr = s.optString("uri")
        if (uriStr.isBlank()) return null
        return MediaItem(
            id = s.optString("id", UUID.randomUUID().toString()), uri = Uri.parse(uriStr), isVideo = s.optBoolean("isVideo", false),
            scale = s.optDouble("scale", 1.0).toFloat(), panX = s.optDouble("panX", 0.0).toFloat(), panY = s.optDouble("panY", 0.0).toFloat(),
            rotationDeg = s.optDouble("rotationDeg", 0.0).toFloat(), flipHorizontal = s.optBoolean("flipHorizontal", false), fitInside = s.optBoolean("fitInside", false),
            brightness = s.optDouble("brightness", 0.0).toFloat(), contrast = s.optDouble("contrast", 1.0).toFloat(), saturation = s.optDouble("saturation", 1.0).toFloat(),
            warmth = s.optDouble("warmth", 0.0).toFloat(), vignette = s.optDouble("vignette", 0.0).toFloat(),
            freeX = s.optDouble("freeX", 0.25).toFloat(), freeY = s.optDouble("freeY", 0.25).toFloat(), freeW = s.optDouble("freeW", 0.42).toFloat(), freeH = s.optDouble("freeH", 0.42).toFloat(),
            zIndex = s.optInt("zIndex", index), locked = s.optBoolean("locked", false), opacity = s.optDouble("opacity", 1.0).toFloat(),
            shadowDp = s.optInt("shadowDp", 10), maskShape = runCatching { TileMaskShape.valueOf(s.optString("maskShape")) }.getOrDefault(TileMaskShape.RECTANGLE)
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
                    alignment = runCatching { TextAlignOption.valueOf(o.optString("alignment")) }.getOrDefault(TextAlignOption.CENTER),
                    opacity = o.optDouble("opacity", 1.0).toFloat(),
                    isSticker = o.optBoolean("isSticker", false),
                    stickerUri = if (stickerStr.isNotBlank()) Uri.parse(stickerStr) else null,
                    useOutline = o.optBoolean("useOutline", true),
                    stickerSizeFraction = o.optDouble("stickerSizeFraction", 0.22).toFloat()
                )
            )
        }

        val customBg = obj.optLong("customBgHex", -1L).takeIf { it != -1L }
        val borderCol = obj.optLong("borderColorHex", -1L).takeIf { it != -1L }

        return StudioState(
            projectName = obj.optString("projectName", "Untitled Collage"),
            mode = runCatching { StudioMode.valueOf(obj.optString("mode")) }.getOrDefault(StudioMode.GRID),
            layout = runCatching { LayoutStyle.valueOf(obj.optString("layout")) }.getOrDefault(LayoutStyle.GRID_2X2),
            carouselSlides = obj.optInt("carouselSlides", 3),
            outerMarginDp = obj.optInt("outerMarginDp", 10),
            borderDp = obj.optInt("borderDp", 10),
            cornerRadiusDp = obj.optInt("cornerRadiusDp", 12),
            backgroundStyle = runCatching { BackgroundStyle.valueOf(obj.optString("backgroundStyle")) }.getOrDefault(BackgroundStyle.OBSIDIAN),
            customBgHex = customBg,
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
            exportConfig = ExportConfig(
                mode = runCatching { ExportMode.valueOf(obj.optString("exportMode")) }.getOrDefault(ExportMode.AUTO_SLICE),
                slideOrder = runCatching { SlideOrder.valueOf(obj.optString("slideOrder")) }.getOrDefault(SlideOrder.FORWARD),
                selectedSlideNumber = obj.optInt("selectedSlideNumber", 1),
                aspectRatio = runCatching { AspectRatioOption.valueOf(obj.optString("aspectRatio")) }.getOrDefault(AspectRatioOption.PORTRAIT_4_5),
                resolution = runCatching { ResolutionPreset.valueOf(obj.optString("resolution")) }.getOrDefault(ResolutionPreset.IG_1080),
                format = runCatching { ExportFormat.valueOf(obj.optString("format")) }.getOrDefault(ExportFormat.JPG),
                qualityPercent = obj.optInt("quality", 95),
                customWidthPx = obj.optInt("customWidthPx", 1080),
                customHeightPx = obj.optInt("customHeightPx", 1350),
                stripExif = obj.optBoolean("stripExif", true),
                includeSignature = obj.optBoolean("includeSignature", false),
                signatureText = obj.optString("signatureText", "@sandeep_njr")
            )
        )
    }
}
