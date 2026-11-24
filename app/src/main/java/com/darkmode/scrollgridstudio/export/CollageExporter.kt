package com.darkmode.scrollgridstudio.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.darkmode.scrollgridstudio.model.*
import com.darkmode.scrollgridstudio.ui.ShapePaths
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class ExportResult(
    val uris: List<Uri>,
    val fileNames: List<String>,
    val summary: String,
    val errorMessage: String? = null
)

object CollageExporter {

    fun exportStudio(
        context: Context,
        state: StudioState,
        asZip: Boolean = false,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): ExportResult {
        val cfg = state.exportConfig
        val slideW = cfg.outputWidthPx()
        val slideH = cfg.outputHeightPx()

        onProgress(0.05f, "Preparing ${slideW}×${slideH}px canvas…")

        val totalSlides = if (state.mode == StudioMode.CAROUSEL) {
            state.carouselSlides.coerceIn(1, 20)
        } else {
            1
        }
        val totalWidthLong = slideW.toLong() * totalSlides.toLong()
        val totalPixels = totalWidthLong * slideH.toLong()
        if (totalWidthLong > 32768L || totalPixels > 48_000_000L) {
            return ExportResult(
                uris = emptyList(),
                fileNames = emptyList(),
                summary = "Export size too large",
                errorMessage = "Canvas (${totalWidthLong}×${slideH}px) exceeds safe memory limits. Try a lower resolution or fewer slides."
            )
        }
        val totalWidth = totalWidthLong.toInt()
        val master = try {
            Bitmap.createBitmap(totalWidth, slideH, Bitmap.Config.ARGB_8888)
        } catch (_: OutOfMemoryError) {
            return ExportResult(
                uris = emptyList(),
                fileNames = emptyList(),
                summary = "Out of memory",
                errorMessage = "Device ran out of memory allocating ${totalWidth}×${slideH}px. Switch to HD (1080px) and retry."
            )
        }
        val canvas = Canvas(master)

        val supportsAlpha = cfg.format == ExportFormat.PNG || cfg.format == ExportFormat.WEBP
        val useTransparent = cfg.transparentBackground && supportsAlpha

        onProgress(0.15f, "Rendering background…")
        if (!useTransparent) {
            drawBackground(context, canvas, state, totalWidth, slideH)
        } else {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        }

        val pxFactor = slideW / 360f
        onProgress(0.32f, "Compositing photos & layers…")
        if (state.slots.isNotEmpty()) {
            when (state.mode) {
                StudioMode.GRID -> drawGridSlots(context, canvas, state, totalWidth, slideH, pxFactor)
                StudioMode.CAROUSEL, StudioMode.FREESTYLE -> drawFreeformLayers(context, canvas, state, totalWidth, slideH, pxFactor)
            }
        }

        onProgress(0.55f, "Applying frames, textures & strokes…")
        if (!useTransparent) {
            drawCanvasTexture(canvas, state.canvasTexture, totalWidth, slideH, pxFactor)
        }
        drawCollageFrame(canvas, state.frameStyle, totalWidth, slideH, pxFactor)
        if (state.drawingsVisible && state.drawingStrokes.isNotEmpty()) {
            drawDrawingStrokes(canvas, state.drawingStrokes, totalWidth, slideH, pxFactor)
        }

        onProgress(0.68f, "Rendering typography & stickers…")
        drawOverlays(context, canvas, state, totalWidth, slideH, pxFactor)

        if (cfg.includeSignature && cfg.signatureText.isNotBlank()) {
            val sigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(190, 243, 244, 246)
                textSize = 13f * pxFactor
                textAlign = Paint.Align.RIGHT
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val margin = 20f * pxFactor
            for (s in 0 until totalSlides) {
                val rightEdge = (s + 1) * slideW - margin
                canvas.drawText(cfg.signatureText, rightEdge, slideH - margin, sigPaint)
            }
        }

        onProgress(0.78f, "Slicing output…")
        val slices: List<Bitmap> = when {
            state.mode == StudioMode.CAROUSEL && cfg.mode == ExportMode.SELECTED_SLIDE && totalSlides > 1 -> {
                val targetIdx = (cfg.selectedSlideNumber - 1).coerceIn(0, totalSlides - 1)
                listOf(Bitmap.createBitmap(master, targetIdx * slideW, 0, slideW, slideH)).also { master.recycle() }
            }
            state.mode == StudioMode.CAROUSEL && cfg.mode == ExportMode.AUTO_SLICE && totalSlides > 1 -> {
                val orderedIndices = if (cfg.slideOrder == SlideOrder.REVERSE) {
                    (totalSlides - 1 downTo 0).toList()
                } else {
                    (0 until totalSlides).toList()
                }
                orderedIndices.map { idx ->
                    Bitmap.createBitmap(master, idx * slideW, 0, slideW, slideH)
                }.also { master.recycle() }
            }
            else -> listOf(master)
        }

        val timestamp = System.currentTimeMillis()
        if (asZip) {
            onProgress(0.88f, "Packing ZIP archive…")
            val zipName = "CollageStudioPro_${timestamp}.zip"
            val zipUri = saveZipToDownloads(context, zipName, slices, cfg)
            slices.forEach { if (!it.isRecycled) it.recycle() }
            onProgress(1f, "Done")
            return if (zipUri != null) {
                ExportResult(listOf(zipUri), listOf(zipName), "Saved $zipName (${slices.size} file(s))")
            } else {
                ExportResult(emptyList(), emptyList(), "ZIP export failed", "Could not write ZIP archive to Downloads.")
            }
        }

        val savedUris = mutableListOf<Uri>()
        val savedNames = mutableListOf<String>()
        slices.forEachIndexed { idx, bmp ->
            val pct = 0.80f + (0.18f * (idx + 1) / slices.size.coerceAtLeast(1))
            onProgress(pct, "Saving ${cfg.format.label} (${idx + 1}/${slices.size})…")
            val suffix = if (slices.size > 1) "_Slide_${String.format("%02d", idx + 1)}" else ""
            val fileName = "CollageStudioPro_${timestamp}${suffix}.${cfg.format.extension}"
            val uri = saveBitmapToGallery(context, bmp, fileName, cfg)
            if (uri != null) {
                savedUris.add(uri)
                savedNames.add(fileName)
            }
            if (!bmp.isRecycled) bmp.recycle()
        }

        onProgress(1f, "Export complete")
        return if (savedUris.isNotEmpty()) {
            val outW = if (cfg.mode == ExportMode.SINGLE_MERGED) totalWidth else slideW
            ExportResult(
                uris = savedUris,
                fileNames = savedNames,
                summary = "Saved ${savedUris.size} ${cfg.format.label} file(s) • ${outW}×${slideH}px"
            )
        } else {
            ExportResult(
                uris = emptyList(),
                fileNames = emptyList(),
                summary = "Export failed",
                errorMessage = "Could not save exported image(s) to MediaStore Gallery. Check storage permissions."
            )
        }
    }

    private fun drawBackground(
        context: Context,
        canvas: Canvas,
        state: StudioState,
        width: Int,
        height: Int
    ) {
        if (state.customBgHex != null) {
            canvas.drawColor(state.customBgHex.toInt())
            return
        }
        val bg = state.backgroundStyle
        if (bg.isBlurPhoto && state.slots.isNotEmpty()) {
            val src = decodeMediaBitmap(context, state.slots.first().uri, 160, 160)
            if (src != null) {
                val blurred = Bitmap.createScaledBitmap(src, 32, 32, true)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(blurred, null, Rect(0, 0, width, height), paint)
                canvas.drawColor(Color.argb(120, 10, 12, 16))
                blurred.recycle()
                src.recycle()
                return
            }
        }
        if (bg.startHex != bg.endHex) {
            val rad = Math.toRadians(state.bgGradientAngleDeg.toDouble())
            val cx = width / 2f
            val cy = height / 2f
            val dx = (cos(rad) * width / 2f).toFloat()
            val dy = (sin(rad) * height / 2f).toFloat()
            val shader = LinearGradient(
                cx - dx, cy - dy, cx + dx, cy + dy,
                bg.startHex.toInt(), bg.endHex.toInt(), Shader.TileMode.CLAMP
            )
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        } else {
            canvas.drawColor(bg.startHex.toInt())
        }
    }

    private fun drawGridSlots(
        context: Context,
        canvas: Canvas,
        state: StudioState,
        totalWidth: Int,
        totalHeight: Int,
        pxFactor: Float
    ) {
        val outer = state.outerMarginDp * pxFactor
        val gap = state.borderDp * pxFactor
        val radius = state.cornerRadiusDp * pxFactor
        val contentLeft = outer
        val contentTop = outer
        val contentW = (totalWidth - outer * 2f).coerceAtLeast(1f)
        val contentH = (totalHeight - outer * 2f).coerceAtLeast(1f)

        val rects = state.layout.slotRects()
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1C232E") }
        val polaroidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F3F4F6") }
        val strokePaint = state.borderColorHex?.let { hex ->
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = hex.toInt()
                style = Paint.Style.STROKE
                strokeWidth = 3f * pxFactor
            }
        }

        rects.forEachIndexed { idx, slotRect ->
            val item = state.slots.getOrNull(idx) ?: return@forEachIndexed
            if (!item.visible) return@forEachIndexed
            val halfGap = gap / 2f
            val l = contentLeft + slotRect.left * contentW + halfGap
            val t = contentTop + slotRect.top * contentH + halfGap
            val r = contentLeft + (slotRect.left + slotRect.width) * contentW - halfGap
            val b = contentTop + (slotRect.top + slotRect.height) * contentH - halfGap
            if (r <= l || b <= t) return@forEachIndexed

            val outerRect = RectF(l, t, r, b)
            val mask = if (item.maskShape != TileMaskShape.RECTANGLE) item.maskShape else slotRect.maskShape

            if (slotRect.polaroidBottomPad > 0f) {
                canvas.drawRoundRect(outerRect, radius, radius, polaroidPaint)
                val p = 10f * pxFactor
                val bottomExtra = outerRect.height() * slotRect.polaroidBottomPad
                val photoRect = RectF(l + p, t + p, r - p, b - bottomExtra)
                drawMediaIntoRect(context, canvas, item, photoRect, radius * 0.5f, pxFactor, framePaint, mask, strokePaint, false)
            } else {
                drawMediaIntoRect(context, canvas, item, outerRect, radius, pxFactor, framePaint, mask, strokePaint, false)
            }
        }
    }

    private fun drawFreeformLayers(
        context: Context,
        canvas: Canvas,
        state: StudioState,
        totalWidth: Int,
        totalHeight: Int,
        pxFactor: Float
    ) {
        val radius = state.cornerRadiusDp * pxFactor
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1C232E") }
        val strokePaint = state.borderColorHex?.let { hex ->
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = hex.toInt()
                style = Paint.Style.STROKE
                strokeWidth = 3f * pxFactor
            }
        }
        val sorted = state.slots.filter { it.visible }.sortedBy { it.zIndex }
        sorted.forEach { item ->
            val w = (item.freeW * totalWidth).coerceAtLeast(40f)
            val h = (item.freeH * totalHeight).coerceAtLeast(40f)
            val cx = item.freeX * totalWidth
            val cy = item.freeY * totalHeight
            val rect = RectF(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f)
            drawMediaIntoRect(context, canvas, item, rect, radius, pxFactor, framePaint, item.maskShape, strokePaint, drawShadow = item.shadowDp > 0)
        }
    }

    private fun drawMediaIntoRect(
        context: Context,
        canvas: Canvas,
        item: MediaItem,
        rect: RectF,
        radius: Float,
        pxFactor: Float,
        framePaint: Paint,
        mask: TileMaskShape,
        strokePaint: Paint?,
        drawShadow: Boolean
    ) {
        val clipPath = ShapePaths.androidPath(mask, rect, radius)

        if (drawShadow && item.shadowDp > 0) {
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(120, 0, 0, 0)
                maskFilter = BlurMaskFilter((item.shadowDp * pxFactor).coerceAtLeast(2f), BlurMaskFilter.Blur.NORMAL)
            }
            canvas.save()
            canvas.translate(0f, 4f * pxFactor)
            canvas.drawPath(clipPath, shadowPaint)
            canvas.restore()
        }

        canvas.drawPath(clipPath, framePaint)

        decodeMediaBitmap(context, item.uri, rect.width().toInt(), rect.height().toInt())?.let { decoded ->
            canvas.save()
            canvas.clipPath(clipPath)

            val cx = rect.centerX() + item.panX * pxFactor
            val cy = rect.centerY() + item.panY * pxFactor
            canvas.translate(cx, cy)
            canvas.rotate(item.rotationDeg)
            val sx = (if (item.flipHorizontal) -1f else 1f) * item.scale
            canvas.scale(sx, item.scale)

            val drawScale = if (item.fitInside) {
                minOf(rect.width() / decoded.width.toFloat(), rect.height() / decoded.height.toFloat())
            } else {
                maxOf(rect.width() / decoded.width.toFloat(), rect.height() / decoded.height.toFloat())
            }
            val dw = decoded.width * drawScale
            val dh = decoded.height * drawScale

            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(buildColorMatrix(item))
                alpha = (item.opacity.coerceIn(0.1f, 1f) * 255).toInt()
            }
            canvas.drawBitmap(decoded, null, RectF(-dw / 2f, -dh / 2f, dw / 2f, dh / 2f), paint)
            canvas.restore()
            decoded.recycle()

            if (item.vignette > 0.02f) {
                canvas.save()
                canvas.clipPath(clipPath)
                val vRad = maxOf(rect.width(), rect.height()) * 0.72f
                val vShader = RadialGradient(
                    rect.centerX(), rect.centerY(), vRad.coerceAtLeast(1f),
                    intArrayOf(Color.TRANSPARENT, Color.argb((item.vignette.coerceIn(0f, 0.85f) * 210).toInt(), 0, 0, 0)),
                    floatArrayOf(0.45f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(rect, Paint().apply { shader = vShader })
                canvas.restore()
            }
        }

        if (strokePaint != null) {
            canvas.drawPath(clipPath, strokePaint)
        }
    }

    private fun drawCanvasTexture(
        canvas: Canvas,
        texture: CanvasTexture,
        width: Int,
        height: Int,
        pxFactor: Float
    ) {
        when (texture) {
            CanvasTexture.NONE -> Unit
            CanvasTexture.FILM_GRAIN -> {
                val rng = Random(42)
                val p = Paint().apply { strokeWidth = 1.5f * pxFactor }
                val count = ((width.toLong() * height.toLong()) / 900L).toInt().coerceIn(800, 5000)
                for (i in 0 until count) {
                    val x = rng.nextFloat() * width
                    val y = rng.nextFloat() * height
                    p.color = if (i % 2 == 0) Color.argb(26, 255, 255, 255) else Color.argb(28, 0, 0, 0)
                    canvas.drawPoint(x, y, p)
                }
            }
            CanvasTexture.PAPER -> {
                canvas.drawColor(Color.argb(22, 245, 232, 205))
                val linePaint = Paint().apply {
                    color = Color.argb(14, 255, 255, 255)
                    strokeWidth = 1f * pxFactor
                }
                var y = 0f
                val step = 12f * pxFactor
                while (y < height) {
                    canvas.drawLine(0f, y, width.toFloat(), y, linePaint)
                    y += step
                }
            }
            CanvasTexture.TORN_EDGE -> {
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#F3F4F6")
                    style = Paint.Style.STROKE
                    strokeWidth = 10f * pxFactor
                    pathEffect = DashPathEffect(floatArrayOf(18f * pxFactor, 8f * pxFactor), 0f)
                }
                val inset = 8f * pxFactor
                canvas.drawRect(inset, inset, width - inset, height - inset, p)
            }
        }
    }


    private fun drawCollageFrame(
        canvas: Canvas,
        frameStyle: CollageFrameStyle,
        width: Int,
        height: Int,
        pxFactor: Float
    ) {
        if (frameStyle == CollageFrameStyle.NONE) return
        val w = width.toFloat()
        val h = height.toFloat()
        when (frameStyle) {
            CollageFrameStyle.MINIMAL -> {
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    style = Paint.Style.STROKE
                    strokeWidth = 10f * pxFactor
                }
                canvas.drawRect(5f * pxFactor, 5f * pxFactor, w - 5f * pxFactor, h - 5f * pxFactor, p)
            }
            CollageFrameStyle.DOUBLE_LINE -> {
                val p1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    style = Paint.Style.STROKE
                    strokeWidth = 6f * pxFactor
                }
                val p2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(200, 255, 255, 255)
                    style = Paint.Style.STROKE
                    strokeWidth = 2f * pxFactor
                }
                canvas.drawRect(6f * pxFactor, 6f * pxFactor, w - 6f * pxFactor, h - 6f * pxFactor, p1)
                canvas.drawRect(15f * pxFactor, 15f * pxFactor, w - 15f * pxFactor, h - 15f * pxFactor, p2)
            }
            CollageFrameStyle.POLAROID -> {
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F7F5F0") }
                val side = 16f * pxFactor
                val bottom = 52f * pxFactor
                canvas.drawRect(0f, 0f, w, side, p)
                canvas.drawRect(0f, 0f, side, h, p)
                canvas.drawRect(w - side, 0f, w, h, p)
                canvas.drawRect(0f, h - bottom, w, h, p)
            }
            CollageFrameStyle.FILM -> {
                val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#111317") }
                val hole = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E5E7EB") }
                val barW = 20f * pxFactor
                canvas.drawRect(0f, 0f, barW, h, bar)
                canvas.drawRect(w - barW, 0f, w, h, bar)
                var y = 12f * pxFactor
                while (y < h - 12f * pxFactor) {
                    canvas.drawRoundRect(RectF(5f * pxFactor, y, 15f * pxFactor, y + 12f * pxFactor), 2f * pxFactor, 2f * pxFactor, hole)
                    canvas.drawRoundRect(RectF(w - 15f * pxFactor, y, w - 5f * pxFactor, y + 12f * pxFactor), 2f * pxFactor, 2f * pxFactor, hole)
                    y += 26f * pxFactor
                }
            }
            CollageFrameStyle.SHADOW_CARD -> {
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(110, 0, 0, 0)
                    style = Paint.Style.STROKE
                    strokeWidth = 14f * pxFactor
                }
                canvas.drawRect(7f * pxFactor, 7f * pxFactor, w - 7f * pxFactor, h - 7f * pxFactor, p)
            }
            CollageFrameStyle.TORN_PAPER -> {
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#F4EFE6")
                    style = Paint.Style.STROKE
                    strokeWidth = 12f * pxFactor
                    pathEffect = DashPathEffect(floatArrayOf(18f * pxFactor, 6f * pxFactor, 10f * pxFactor, 4f * pxFactor), 0f)
                }
                canvas.drawRect(6f * pxFactor, 6f * pxFactor, w - 6f * pxFactor, h - 6f * pxFactor, p)
            }
            CollageFrameStyle.WASHI_TAPE -> {
                val tape = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(190, 232, 213, 183) }
                val tw = 64f * pxFactor
                val th = 18f * pxFactor
                listOf(
                    Triple(26f * pxFactor, 26f * pxFactor, -35f),
                    Triple(w - 26f * pxFactor, 26f * pxFactor, 35f),
                    Triple(26f * pxFactor, h - 26f * pxFactor, 35f),
                    Triple(w - 26f * pxFactor, h - 26f * pxFactor, -35f)
                ).forEach { (cx, cy, deg) ->
                    canvas.save()
                    canvas.translate(cx, cy)
                    canvas.rotate(deg)
                    canvas.drawRect(-tw / 2f, -th / 2f, tw / 2f, th / 2f, tape)
                    canvas.restore()
                }
            }
            CollageFrameStyle.NONE -> Unit
        }
    }

    private fun drawDrawingStrokes(
        canvas: Canvas,
        strokes: List<DrawingStroke>,
        width: Int,
        height: Int,
        pxFactor: Float
    ) {
        strokes.forEach { stroke ->
            if (stroke.points.size < 2) return@forEach
            val path = Path()
            stroke.points.forEachIndexed { idx, pt ->
                val x = pt.x * width
                val y = pt.y * height
                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = stroke.colorHex.toInt()
                alpha = (stroke.opacity.coerceIn(0.05f, 1f) * 255).toInt()
                style = Paint.Style.STROKE
                strokeWidth = stroke.strokeWidthDp * pxFactor
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            canvas.drawPath(path, paint)
        }
    }

    private fun resolveOverlayTypeface(overlay: TextOverlay): Typeface {
        val customPath = overlay.customFontPath
        if (!customPath.isNullOrBlank()) {
            val f = File(customPath)
            if (f.exists()) {
                runCatching { return Typeface.createFromFile(f) }
            }
        }
        return when (overlay.fontStyle) {
            TextFontStyle.SANS_BOLD -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            TextFontStyle.SERIF -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
            TextFontStyle.MONO -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            TextFontStyle.ITALIC -> Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
        }
    }

    private fun drawOverlays(
        context: Context,
        canvas: Canvas,
        state: StudioState,
        totalWidth: Int,
        totalHeight: Int,
        pxFactor: Float
    ) {
        val sortedOverlays = state.overlays.filter { it.visible }.sortedBy { it.zIndex }
        sortedOverlays.forEach { overlay ->
            val cx = totalWidth * overlay.xFraction
            val cy = totalHeight * overlay.yFraction

            if (overlay.stickerUri != null) {
                val targetSize = (totalHeight * overlay.stickerSizeFraction).toInt().coerceAtLeast(64)
                decodeMediaBitmap(context, overlay.stickerUri, targetSize, targetSize)?.let { bmp ->
                    canvas.save()
                    canvas.translate(cx, cy)
                    canvas.rotate(overlay.rotationDeg)
                    val aspect = bmp.width.toFloat() / bmp.height.toFloat().coerceAtLeast(1f)
                    val dh = targetSize.toFloat()
                    val dw = dh * aspect
                    val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                        alpha = (overlay.opacity.coerceIn(0.1f, 1f) * 255).toInt()
                    }
                    canvas.drawBitmap(bmp, null, RectF(-dw / 2f, -dh / 2f, dw / 2f, dh / 2f), p)
                    canvas.restore()
                    bmp.recycle()
                }
                return@forEach
            }

            if (overlay.text.isBlank()) return@forEach

            val typeface = resolveOverlayTypeface(overlay)
            val layoutAlign = when (overlay.alignment) {
                TextAlignOption.LEFT -> Layout.Alignment.ALIGN_NORMAL
                TextAlignOption.CENTER -> Layout.Alignment.ALIGN_CENTER
                TextAlignOption.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
            }
            val alphaInt = (overlay.opacity.coerceIn(0.1f, 1f) * 255).toInt()
            val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = overlay.textColorHex.toInt()
                alpha = alphaInt
                textSize = overlay.fontSizeSp * pxFactor
                this.typeface = typeface
                // Preserve Malayalam ligatures/conjuncts by only setting letterSpacing when explicitly > 0
                if (overlay.letterSpacingEm > 0.001f) {
                    letterSpacing = overlay.letterSpacingEm
                }
                if (overlay.useShadow) {
                    setShadowLayer(
                        overlay.shadowRadiusDp * pxFactor,
                        0f,
                        2f * pxFactor,
                        overlay.shadowColorHex.toInt()
                    )
                }
            }

            val maxBoxW = (totalWidth * 0.86f).toInt().coerceAtLeast(120)
            val longestLine = overlay.text.split("\n").maxOfOrNull { textPaint.measureText(it) } ?: 100f
            val layoutW = (longestLine + 16f * pxFactor).toInt().coerceIn(64, maxBoxW)

            val staticLayout = StaticLayout.Builder
                .obtain(overlay.text, 0, overlay.text.length, textPaint, layoutW)
                .setAlignment(layoutAlign)
                .setLineSpacing(0f, overlay.lineHeightMultiplier.coerceIn(0.8f, 2.5f))
                .setIncludePad(true)
                .build()

            val boxH = staticLayout.height.toFloat()
            val padX = overlay.paddingDp.coerceIn(0, 48) * pxFactor
            val padY = (overlay.paddingDp.coerceIn(0, 48) * 0.65f) * pxFactor

            canvas.save()
            canvas.translate(cx, cy)
            canvas.rotate(overlay.rotationDeg)

            if (overlay.usePill) {
                val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = overlay.bgColorHex.toInt()
                    alpha = ((Color.alpha(overlay.bgColorHex.toInt()) / 255f) * alphaInt).toInt().coerceIn(20, 255)
                }
                val bgRect = RectF(
                    -layoutW / 2f - padX,
                    -boxH / 2f - padY,
                    layoutW / 2f + padX,
                    boxH / 2f + padY
                )
                canvas.drawRoundRect(bgRect, 12f * pxFactor, 12f * pxFactor, pillPaint)
            }

            canvas.translate(-layoutW / 2f, -boxH / 2f)

            if (overlay.useOutline) {
                val strokePaint = TextPaint(textPaint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = overlay.outlineWidthDp.coerceIn(0.5f, 10f) * pxFactor
                    strokeJoin = Paint.Join.ROUND
                    color = overlay.outlineColorHex.toInt()
                    alpha = alphaInt
                    shader = null
                    clearShadowLayer()
                }
                val strokeLayout = StaticLayout.Builder
                    .obtain(overlay.text, 0, overlay.text.length, strokePaint, layoutW)
                    .setAlignment(layoutAlign)
                    .setLineSpacing(0f, overlay.lineHeightMultiplier.coerceIn(0.8f, 2.5f))
                    .setIncludePad(true)
                    .build()
                strokeLayout.draw(canvas)
            }

            if (overlay.useGradient) {
                textPaint.shader = LinearGradient(
                    0f, 0f, layoutW.toFloat(), boxH,
                    overlay.textColorHex.toInt(),
                    overlay.gradientEndHex.toInt(),
                    Shader.TileMode.CLAMP
                )
            }
            staticLayout.draw(canvas)
            canvas.restore()
        }
    }

    fun shareUris(context: Context, uris: List<Uri>, mimeType: String) {
        if (uris.isEmpty()) return
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uris.first())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mimeType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        context.startActivity(Intent.createChooser(intent, "Share ScrollGrid Export"))
    }

    private fun decodeMediaBitmap(context: Context, uri: Uri, reqW: Int, reqH: Int): Bitmap? {
        return try {
            val mime = context.contentResolver.getType(uri).orEmpty()
            if (mime.startsWith("video/")) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: return null
                    val maxW = reqW.coerceAtLeast(1)
                    val maxH = reqH.coerceAtLeast(1)
                    val factor = minOf(1f, maxW.toFloat() / frame.width, maxH.toFloat() / frame.height)
                    if (factor < 1f) Bitmap.createScaledBitmap(frame, (frame.width * factor).toInt().coerceAtLeast(1), (frame.height * factor).toInt().coerceAtLeast(1), true).also { if (it !== frame) frame.recycle() } else frame
                } finally { retriever.release() }
            } else {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= reqW.coerceAtLeast(1) && bounds.outHeight / (sample * 2) >= reqH.coerceAtLeast(1)) sample *= 2
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredColorSpace = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ColorSpace.get(ColorSpace.Named.SRGB) else null
                }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            }
        } catch (_: Exception) { null }
    }

    private fun buildColorMatrix(item: MediaItem): ColorMatrix {
        val cm = ColorMatrix()
        val sat = ColorMatrix().apply { setSaturation(item.saturation) }
        cm.postConcat(sat)
        val c = item.contrast
        val b = item.brightness * 255f
        val rBoost = item.warmth * 35f
        val bBoost = -item.warmth * 35f
        val translate = (-0.5f * c + 0.5f) * 255f + b
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, translate + rBoost,
                0f, c, 0f, 0f, translate,
                0f, 0f, c, 0f, translate + bBoost,
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.postConcat(contrastMatrix)
        return cm
    }

    private fun compressFormatFor(format: ExportFormat): Bitmap.CompressFormat = when (format) {
        ExportFormat.JPG -> Bitmap.CompressFormat.JPEG
        ExportFormat.PNG -> Bitmap.CompressFormat.PNG
        ExportFormat.WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
    }

    private fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
        cfg: ExportConfig
    ): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, cfg.format.mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ScrollGridStudio")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val outUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            resolver.openOutputStream(outUri)?.use { os ->
                val q = if (cfg.format == ExportFormat.PNG) 100 else cfg.qualityPercent.coerceIn(80, 100)
                bitmap.compress(compressFormatFor(cfg.format), q, os)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(outUri, values, null, null)
            }
            outUri
        } catch (_: Exception) {
            null
        }
    }

    private fun saveZipToDownloads(
        context: Context,
        zipName: String,
        slices: List<Bitmap>,
        cfg: ExportConfig
    ): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, zipName)
            put(MediaStore.Downloads.MIME_TYPE, "application/zip")
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/ScrollGridStudio")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val outUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            resolver.openOutputStream(outUri)?.use { os ->
                ZipOutputStream(os).use { zos ->
                    val q = if (cfg.format == ExportFormat.PNG) 100 else cfg.qualityPercent.coerceIn(80, 100)
                    slices.forEachIndexed { idx, bmp ->
                        val entryName = "Slide_${String.format("%02d", idx + 1)}.${cfg.format.extension}"
                        val baos = ByteArrayOutputStream()
                        bmp.compress(compressFormatFor(cfg.format), q, baos)
                        zos.putNextEntry(ZipEntry(entryName))
                        zos.write(baos.toByteArray())
                        zos.closeEntry()
                    }
                }
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(outUri, values, null, null)
            outUri
        } catch (_: Exception) {
            null
        }
    }
}
