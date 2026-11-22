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
                out.add(
                    SavedDraftMeta(
                        id = id,
                        name = obj.optString("projectName", "Untitled Collage"),
                        updatedAtMillis = obj.optLong("updatedAt", 0L),
                        modeTitle = obj.optString("mode", StudioMode.GRID.name),
                        itemCount = obj.optJSONArray("slots")?.length() ?: 0
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

            val slotsArr = JSONArray()
            state.slots.forEach { item ->
                slotsArr.put(JSONObject().apply {
                    put("id", item.id)
                    put("uri", item.uri.toString())
                    put("isVideo", item.isVideo)
                    put("scale", item.scale.toDouble())
                    put("panX", item.panX.toDouble())
                    put("panY", item.panY.toDouble())
                    put("rotationDeg", item.rotationDeg.toDouble())
                    put("flipHorizontal", item.flipHorizontal)
                    put("fitInside", item.fitInside)
                    put("brightness", item.brightness.toDouble())
                    put("contrast", item.contrast.toDouble())
                    put("saturation", item.saturation.toDouble())
                    put("warmth", item.warmth.toDouble())
                    put("vignette", item.vignette.toDouble())
                    put("freeX", item.freeX.toDouble())
                    put("freeY", item.freeY.toDouble())
                    put("freeW", item.freeW.toDouble())
                    put("freeH", item.freeH.toDouble())
                    put("zIndex", item.zIndex)
                    put("shadowDp", item.shadowDp)
                    put("maskShape", item.maskShape.name)
                })
            }
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
                    put("alignment", ov.alignment.name)
                    put("opacity", ov.opacity.toDouble())
                    put("isSticker", ov.isSticker)
                    put("stickerUri", ov.stickerUri?.toString() ?: "")
                })
            }
            put("overlays", ovArr)
        }
    }

    private fun deserializeState(obj: JSONObject): StudioState {
        val slots = mutableListOf<MediaItem>()
        val sArr = obj.optJSONArray("slots") ?: JSONArray()
        for (i in 0 until sArr.length()) {
            val s = sArr.getJSONObject(i)
            val uriStr = s.optString("uri")
            if (uriStr.isBlank()) continue
            slots.add(
                MediaItem(
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
                    zIndex = s.optInt("zIndex", i),
                    shadowDp = s.optInt("shadowDp", 10),
                    maskShape = runCatching { TileMaskShape.valueOf(s.optString("maskShape")) }.getOrDefault(TileMaskShape.RECTANGLE)
                )
            )
        }

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
                    alignment = runCatching { TextAlignOption.valueOf(o.optString("alignment")) }.getOrDefault(TextAlignOption.CENTER),
                    opacity = o.optDouble("opacity", 1.0).toFloat(),
                    isSticker = o.optBoolean("isSticker", false),
                    stickerUri = if (stickerStr.isNotBlank()) Uri.parse(stickerStr) else null
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
            borderDp = obj.optInt("borderDp", 8),
            cornerRadiusDp = obj.optInt("cornerRadiusDp", 12),
            backgroundStyle = runCatching { BackgroundStyle.valueOf(obj.optString("backgroundStyle")) }.getOrDefault(BackgroundStyle.OBSIDIAN),
            customBgHex = customBg,
            borderColorHex = borderCol,
            canvasTexture = runCatching { CanvasTexture.valueOf(obj.optString("canvasTexture")) }.getOrDefault(CanvasTexture.NONE),
            slots = slots,
            overlays = overlays,
            exportConfig = ExportConfig(
                aspectRatio = runCatching { AspectRatioOption.valueOf(obj.optString("aspectRatio")) }.getOrDefault(AspectRatioOption.PORTRAIT_4_5),
                resolution = runCatching { ResolutionPreset.valueOf(obj.optString("resolution")) }.getOrDefault(ResolutionPreset.IG_1080),
                format = runCatching { ExportFormat.valueOf(obj.optString("format")) }.getOrDefault(ExportFormat.JPG),
                qualityPercent = obj.optInt("quality", 95)
            )
        )
    }
}
