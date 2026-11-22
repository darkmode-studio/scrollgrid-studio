package com.darkmode.scrollgridstudio.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.darkmode.scrollgridstudio.model.*
import com.darkmode.scrollgridstudio.ui.ShapePaths
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.random.Random

data class ExportResult(
    val uris: List<Uri>,
    val fileNames: List<String>,
    val summary: String
)

object CollageExporter {

    fun exportStudio(
        context: Context,
        state: StudioState,
        asZip: Boolean = false
    ): ExportResult? {
        val cfg = state.exportConfig
        val slideW = cfg.outputWidthPx()
        val slideH = cfg.outputHeightPx()

        val totalSlides = if (state.mode == StudioMode.CAROUSEL) {
            state.carouselSlides.coerceIn(1, 20)
        } else {
            1
        }
        val totalWidthLong = slideW.toLong() * totalSlides.toLong()
        val totalPixels = totalWidthLong * slideH.toLong()
        // Refuse allocations that can exceed practical Android heap limits instead of crashing the app.
        if (totalWidthLong > 32768L || totalPixels > 48_000_000L) return null
        val totalWidth = totalWidthLong.toInt()
        val master = try { Bitmap.createBitmap(totalWidth, slideH, Bitmap.Config.ARGB_8888) }
        catch (_: OutOfMemoryError) { return null }
        val canvas = Canvas(master)

        drawBackground(context, canvas, state, totalWidth, slideH)

        val pxFactor = slideW / 360f
        if (state.slots.isNotEmpty()) {
            when (state.mode) {
                StudioMode.GRID -> drawGridSlots(context, canvas, state, totalWidth, slideH, pxFactor)
                StudioMode.CAROUSEL, StudioMode.FREESTYLE -> drawFreeformLayers(context, canvas, state, totalWidth, slideH, pxFactor)
            }
        }

        drawCanvasTexture(canvas, state.canvasTexture, totalWidth, slideH, pxFactor)
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
            val zipName = "ScrollGrid_${timestamp}.zip"
            val zipUri = saveZipToDownloads(context, zipName, slices, cfg)
            slices.forEach { if (!it.isRecycled) it.recycle() }
            return zipUri?.let {
                ExportResult(listOf(it), listOf(zipName), "Saved $zipName (${slices.size} slide(s))")
            }
        }

        val savedUris = mutableListOf<Uri>()
        val savedNames = mutableListOf<String>()
        slices.forEachIndexed { idx, bmp ->
            val suffix = if (slices.size > 1) "_Slide_${String.format("%02d", idx + 1)}" else ""
            val fileName = "ScrollGrid_${timestamp}${suffix}.${cfg.format.extension}"
            val uri = saveBitmapToGallery(context, bmp, fileName, cfg)
            if (uri != null) {
                savedUris.add(uri)
                savedNames.add(fileName)
            }
            if (!bmp.isRecycled) bmp.recycle()
        }

        return if (savedUris.isNotEmpty()) {
            val outW = if (cfg.mode == ExportMode.SINGLE_MERGED) totalWidth else slideW
            ExportResult(
                uris = savedUris,
                fileNames = savedNames,
                summary = "Saved ${savedUris.size} ${cfg.format.label} file(s) • ${outW}x${slideH}px"
            )
        } else {
            null
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
            val tiny = decodeMediaBitmap(context, state.slots.first().uri, 48, 48)
            if (tiny != null) {
                val blurPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(tiny, null, RectF(0f, 0f, width.toFloat(), height.toFloat()), blurPaint)
                tiny.recycle()
                canvas.drawColor(Color.argb(110, 11, 13, 16))
                return
            }
        }
        if (bg.startHex != bg.endHex) {
            val shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                bg.startHex.toInt(), bg.endHex.toInt(),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply { this.shader = shader })
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
        val sorted = state.slots.sortedBy { it.zIndex }
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

    private fun drawOverlays(
        context: Context,
        canvas: Canvas,
        state: StudioState,
        totalWidth: Int,
        totalHeight: Int,
        pxFactor: Float
    ) {
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CC0B0D10")
        }
        state.overlays.forEach { overlay ->
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

            val typeface = when (overlay.fontStyle) {
                TextFontStyle.SANS_BOLD -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                TextFontStyle.SERIF -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
                TextFontStyle.MONO -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                TextFontStyle.ITALIC -> Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
            }
            val align = when (overlay.alignment) {
                TextAlignOption.LEFT -> Paint.Align.LEFT
                TextAlignOption.CENTER -> Paint.Align.CENTER
                TextAlignOption.RIGHT -> Paint.Align.RIGHT
            }
            val alphaInt = (overlay.opacity.coerceIn(0.1f, 1f) * 255).toInt()
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = overlay.textColorHex.toInt()
                alpha = alphaInt
                textSize = overlay.fontSizeSp * pxFactor
                textAlign = align
                this.typeface = typeface
                if (overlay.useOutline) {
                    setShadowLayer(4f * pxFactor, 0f, 2f * pxFactor, Color.BLACK)
                }
            }

            val textWidth = textPaint.measureText(overlay.text)
            canvas.save()
            canvas.translate(cx, cy)
            canvas.rotate(overlay.rotationDeg)

            if (overlay.usePill) {
                pillPaint.alpha = (alphaInt * 0.8f).toInt()
                val padX = 20f * pxFactor
                val padY = 12f * pxFactor
                val rectLeft = when (overlay.alignment) {
                    TextAlignOption.LEFT -> -padX
                    TextAlignOption.CENTER -> -textWidth / 2f - padX
                    TextAlignOption.RIGHT -> -textWidth - padX
                }
                val pillRect = RectF(
                    rectLeft,
                    -textPaint.textSize * 0.85f,
                    rectLeft + textWidth + padX * 2f,
                    padY
                )
                canvas.drawRoundRect(pillRect, 14f * pxFactor, 14f * pxFactor, pillPaint)
            }
            canvas.drawText(overlay.text, 0f, 0f, textPaint)
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
