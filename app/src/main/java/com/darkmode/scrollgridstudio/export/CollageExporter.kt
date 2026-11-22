package com.darkmode.scrollgridstudio.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.darkmode.scrollgridstudio.model.*
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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

        val slideCount = when {
            state.mode != StudioMode.CAROUSEL -> 1
            cfg.mode == ExportMode.SELECTED_SLIDE -> 1
            else -> state.carouselSlides.coerceIn(1, 20)
        }

        val totalWidth = if (state.mode == StudioMode.CAROUSEL && cfg.mode != ExportMode.SELECTED_SLIDE) {
            slideW * slideCount
        } else {
            slideW
        }

        val master = Bitmap.createBitmap(totalWidth, slideH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(master)
        canvas.drawColor(state.backgroundStyle.hex.toInt())

        val items = state.slots
        if (items.isNotEmpty()) {
            val cols = if (state.mode == StudioMode.CAROUSEL && cfg.mode != ExportMode.SELECTED_SLIDE) {
                (slideCount * 2).coerceAtLeast(1)
            } else {
                2
            }
            val rows = if (state.mode == StudioMode.CAROUSEL) 1 else ((items.size + 1) / 2).coerceAtLeast(1)
            val pad = (state.borderDp * (slideW / 360f)).coerceAtLeast(0f)
            val radius = state.cornerRadiusDp * (slideW / 360f)
            val cellW = (totalWidth - pad * (cols + 1)) / cols
            val cellH = (slideH - pad * (rows + 1)) / rows

            val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1C232E")
            }

            items.forEachIndexed { idx, item ->
                val r = idx / cols
                val c = idx % cols
                if (r >= rows) return@forEachIndexed
                val left = pad + c * (cellW + pad)
                val top = pad + r * (cellH + pad)
                val rect = RectF(left, top, left + cellW, top + cellH)
                canvas.drawRoundRect(rect, radius, radius, framePaint)

                decodeMediaBitmap(context, item.uri, cellW.toInt(), cellH.toInt())?.let { decoded ->
                    canvas.save()
                    val clipPath = Path().apply {
                        addRoundRect(rect, radius, radius, Path.Direction.CW)
                    }
                    canvas.clipPath(clipPath)

                    val cx = rect.centerX() + item.panX * (slideW / 360f)
                    val cy = rect.centerY() + item.panY * (slideW / 360f)
                    canvas.translate(cx, cy)
                    canvas.rotate(item.rotationDeg)
                    val sx = (if (item.flipHorizontal) -1f else 1f) * item.scale
                    val sy = item.scale
                    canvas.scale(sx, sy)

                    val drawScale = maxOf(cellW / decoded.width.toFloat(), cellH / decoded.height.toFloat())
                    val dw = decoded.width * drawScale
                    val dh = decoded.height * drawScale

                    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                        colorFilter = ColorMatrixColorFilter(buildColorMatrix(item))
                    }
                    canvas.drawBitmap(
                        decoded,
                        null,
                        RectF(-dw / 2f, -dh / 2f, dw / 2f, dh / 2f),
                        paint
                    )
                    canvas.restore()
                    decoded.recycle()
                }
            }
        }

        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CC0B0D10")
        }
        state.overlays.forEach { overlay ->
            if (overlay.text.isBlank()) return@forEach
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = overlay.textColorHex.toInt()
                textSize = overlay.fontSizeSp * (slideW / 360f)
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val cx = totalWidth * overlay.xFraction
            val cy = slideH * overlay.yFraction
            val textWidth = textPaint.measureText(overlay.text)
            if (overlay.usePill) {
                val padX = 24f * (slideW / 360f)
                val padY = 14f * (slideW / 360f)
                val pillRect = RectF(
                    cx - textWidth / 2f - padX,
                    cy - textPaint.textSize * 0.85f,
                    cx + textWidth / 2f + padX,
                    cy + padY
                )
                canvas.drawRoundRect(pillRect, 16f * (slideW / 360f), 16f * (slideW / 360f), pillPaint)
            }
            canvas.drawText(overlay.text, cx, cy, textPaint)
        }

        if (cfg.includeSignature && cfg.signatureText.isNotBlank()) {
            val sigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(180, 243, 244, 246)
                textSize = 13f * (slideW / 360f)
                textAlign = Paint.Align.RIGHT
            }
            val margin = 20f * (slideW / 360f)
            for (s in 0 until slideCount) {
                val rightEdge = (s + 1) * slideW - margin
                canvas.drawText(cfg.signatureText, rightEdge, slideH - margin, sigPaint)
            }
        }

        val slices: List<Bitmap> = if (state.mode == StudioMode.CAROUSEL && cfg.mode == ExportMode.AUTO_SLICE && slideCount > 1) {
            val orderedIndices = if (cfg.slideOrder == SlideOrder.REVERSE) {
                (slideCount - 1 downTo 0).toList()
            } else {
                (0 until slideCount).toList()
            }
            orderedIndices.map { idx ->
                Bitmap.createBitmap(master, idx * slideW, 0, slideW, slideH)
            }.also { master.recycle() }
        } else {
            listOf(master)
        }

        val timestamp = System.currentTimeMillis()
        if (asZip) {
            val zipName = "ScrollGrid_${timestamp}.zip"
            val zipUri = saveZipToDownloads(context, zipName, slices, cfg)
            slices.forEach { if (!it.isRecycled) it.recycle() }
            return zipUri?.let {
                ExportResult(listOf(it), listOf(zipName), "Saved $zipName (${slices.size} slides)")
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
            ExportResult(
                uris = savedUris,
                fileNames = savedNames,
                summary = "Saved ${savedUris.size} ${cfg.format.label} file(s) • ${slideW}x${slideH}px"
            )
        } else {
            null
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
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= reqW.coerceAtLeast(1) &&
                bounds.outHeight / (sample * 2) >= reqH.coerceAtLeast(1)
            ) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredColorSpace = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ColorSpace.get(ColorSpace.Named.SRGB)
                } else null
            }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun buildColorMatrix(item: MediaItem): ColorMatrix {
        val cm = ColorMatrix()
        val sat = ColorMatrix().apply { setSaturation(item.saturation) }
        cm.postConcat(sat)
        val c = item.contrast
        val b = item.brightness * 255f
        val translate = (-0.5f * c + 0.5f) * 255f + b
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, translate,
                0f, c, 0f, 0f, translate,
                0f, 0f, c, 0f, translate,
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
