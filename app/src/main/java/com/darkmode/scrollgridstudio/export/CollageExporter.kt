package com.darkmode.scrollgridstudio.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.provider.MediaStore
import com.darkmode.scrollgridstudio.model.BackgroundStyle
import com.darkmode.scrollgridstudio.model.MediaItem
import com.darkmode.scrollgridstudio.model.TextOverlay

object CollageExporter {

    fun exportPng(
        context: Context,
        slots: List<MediaItem>,
        overlays: List<TextOverlay>,
        backgroundStyle: BackgroundStyle,
        borderDp: Int,
        widthPx: Int = 2160,
        heightPx: Int = 2700
    ): String? {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgColor = when (backgroundStyle) {
            BackgroundStyle.OBSIDIAN -> Color.parseColor("#0C0F14")
            BackgroundStyle.FOREST -> Color.parseColor("#133326")
            BackgroundStyle.BAMBOO -> Color.parseColor("#1D3B2B")
            BackgroundStyle.NAVY -> Color.parseColor("#0F1E36")
            BackgroundStyle.BLUR_MATCH -> Color.parseColor("#1C232E")
        }
        canvas.drawColor(bgColor)

        val items = slots.ifEmpty { emptyList() }
        if (items.isNotEmpty()) {
            val cols = 2
            val rows = ((items.size + 1) / 2).coerceAtLeast(1)
            val pad = (borderDp * 3f).coerceAtLeast(12f)
            val cellW = (widthPx - pad * (cols + 1)) / cols
            val cellH = (heightPx - pad * (rows + 1)) / rows

            val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1C232E")
            }

            items.forEachIndexed { idx, item ->
                val r = idx / cols
                val c = idx % cols
                val left = pad + c * (cellW + pad)
                val top = pad + r * (cellH + pad)
                val rect = RectF(left, top, left + cellW, top + cellH)
                canvas.drawRoundRect(rect, 24f, 24f, framePaint)

                context.contentResolver.openInputStream(item.uri)?.use { stream ->
                    val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                    val decoded = BitmapFactory.decodeStream(stream, null, opts)
                    if (decoded != null) {
                        val scaled = Bitmap.createScaledBitmap(
                            decoded,
                            cellW.toInt().coerceAtLeast(1),
                            cellH.toInt().coerceAtLeast(1),
                            true
                        )
                        canvas.drawBitmap(scaled, left, top, null)
                        if (scaled !== decoded) scaled.recycle()
                        decoded.recycle()
                    }
                }
            }
        }

        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CC0C0F14")
        }
        overlays.forEach { overlay ->
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = overlay.fontSizeSp * 3.6f
                textAlign = Paint.Align.CENTER
            }
            val cx = widthPx * overlay.xFraction
            val cy = heightPx * overlay.yFraction
            val textWidth = textPaint.measureText(overlay.text)
            if (overlay.usePill && overlay.text.isNotBlank()) {
                val pillRect = RectF(
                    cx - textWidth / 2f - 36f,
                    cy - textPaint.textSize * 0.85f,
                    cx + textWidth / 2f + 36f,
                    cy + 24f
                )
                canvas.drawRoundRect(pillRect, 24f, 24f, pillPaint)
            }
            canvas.drawText(overlay.text, cx, cy, textPaint)
        }

        val fileName = "ScrollGrid_${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ScrollGridStudio")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val outUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            resolver.openOutputStream(outUri)?.use { os ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(outUri, values, null, null)
            }
            bitmap.recycle()
            fileName
        } catch (_: Exception) {
            null
        }
    }
}
