package com.darkmode.scrollgridstudio.ui

import android.graphics.Path as AndroidPath
import android.graphics.RectF
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.darkmode.scrollgridstudio.model.TileMaskShape
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object ShapePaths {

    fun composeShape(mask: TileMaskShape, cornerRadius: Dp): Shape = when (mask) {
        TileMaskShape.RECTANGLE -> RoundedCornerShape(cornerRadius)
        TileMaskShape.CIRCLE -> GenericShape { size, _ ->
            addOval(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
        }
        TileMaskShape.HEART -> GenericShape { size, _ ->
            buildComposeHeart(this, size.width, size.height)
        }
        TileMaskShape.STAR -> GenericShape { size, _ ->
            buildComposeStar(this, size.width, size.height)
        }
        TileMaskShape.HEXAGON -> GenericShape { size, _ ->
            buildComposeHexagon(this, size.width, size.height)
        }
        TileMaskShape.SLANT_LEFT -> GenericShape { size, _ ->
            moveTo(size.width * 0.14f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        TileMaskShape.SLANT_RIGHT -> GenericShape { size, _ ->
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width * 0.86f, size.height)
            lineTo(0f, size.height)
            close()
        }
    }

    fun androidPath(mask: TileMaskShape, rect: RectF, radius: Float): AndroidPath {
        val path = AndroidPath()
        val l = rect.left
        val t = rect.top
        val w = rect.width()
        val h = rect.height()
        when (mask) {
            TileMaskShape.RECTANGLE -> path.addRoundRect(rect, radius, radius, AndroidPath.Direction.CW)
            TileMaskShape.CIRCLE -> path.addOval(rect, AndroidPath.Direction.CW)
            TileMaskShape.HEART -> {
                path.moveTo(l + w * 0.5f, t + h * 0.92f)
                path.cubicTo(l + w * 0.05f, t + h * 0.62f, l, t + h * 0.25f, l + w * 0.26f, t + h * 0.12f)
                path.cubicTo(l + w * 0.42f, t + h * 0.04f, l + w * 0.5f, t + h * 0.2f, l + w * 0.5f, t + h * 0.28f)
                path.cubicTo(l + w * 0.5f, t + h * 0.2f, l + w * 0.58f, t + h * 0.04f, l + w * 0.74f, t + h * 0.12f)
                path.cubicTo(l + w, t + h * 0.25f, l + w * 0.95f, t + h * 0.62f, l + w * 0.5f, t + h * 0.92f)
                path.close()
            }
            TileMaskShape.STAR -> {
                val cx = l + w / 2f
                val cy = t + h / 2f
                val outerR = minOf(w, h) * 0.48f
                val innerR = outerR * 0.42f
                for (i in 0 until 10) {
                    val angle = -PI / 2.0 + i * PI / 5.0
                    val r = if (i % 2 == 0) outerR else innerR
                    val x = cx + (r * cos(angle)).toFloat()
                    val y = cy + (r * sin(angle)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
            }
            TileMaskShape.HEXAGON -> {
                val cx = l + w / 2f
                val cy = t + h / 2f
                val rad = minOf(w, h) * 0.48f
                for (i in 0 until 6) {
                    val angle = -PI / 2.0 + i * PI / 3.0
                    val x = cx + (rad * cos(angle)).toFloat()
                    val y = cy + (rad * sin(angle)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
            }
            TileMaskShape.SLANT_LEFT -> {
                path.moveTo(l + w * 0.14f, t)
                path.lineTo(l + w, t)
                path.lineTo(l + w, t + h)
                path.lineTo(l, t + h)
                path.close()
            }
            TileMaskShape.SLANT_RIGHT -> {
                path.moveTo(l, t)
                path.lineTo(l + w, t)
                path.lineTo(l + w * 0.86f, t + h)
                path.lineTo(l, t + h)
                path.close()
            }
        }
        return path
    }

    private fun buildComposeHeart(path: Path, w: Float, h: Float) {
        path.moveTo(w * 0.5f, h * 0.92f)
        path.cubicTo(w * 0.05f, h * 0.62f, 0f, h * 0.25f, w * 0.26f, h * 0.12f)
        path.cubicTo(w * 0.42f, h * 0.04f, w * 0.5f, h * 0.2f, w * 0.5f, h * 0.28f)
        path.cubicTo(w * 0.5f, h * 0.2f, w * 0.58f, h * 0.04f, w * 0.74f, h * 0.12f)
        path.cubicTo(w, h * 0.25f, w * 0.95f, h * 0.62f, w * 0.5f, h * 0.92f)
        path.close()
    }

    private fun buildComposeStar(path: Path, w: Float, h: Float) {
        val cx = w / 2f
        val cy = h / 2f
        val outerR = minOf(w, h) * 0.48f
        val innerR = outerR * 0.42f
        for (i in 0 until 10) {
            val angle = -PI / 2.0 + i * PI / 5.0
            val r = if (i % 2 == 0) outerR else innerR
            val x = cx + (r * cos(angle)).toFloat()
            val y = cy + (r * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
    }

    private fun buildComposeHexagon(path: Path, w: Float, h: Float) {
        val cx = w / 2f
        val cy = h / 2f
        val rad = minOf(w, h) * 0.48f
        for (i in 0 until 6) {
            val angle = -PI / 2.0 + i * PI / 3.0
            val x = cx + (rad * cos(angle)).toFloat()
            val y = cy + (rad * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
    }
}
