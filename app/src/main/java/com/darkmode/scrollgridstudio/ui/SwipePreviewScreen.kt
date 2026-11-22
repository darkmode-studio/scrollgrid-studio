package com.darkmode.scrollgridstudio.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.darkmode.scrollgridstudio.model.*

private val PrevBg = Color(0xFF07090C)
private val PrevPanel = Color(0xFF14171D)
private val PrevBorder = Color(0xFF242933)
private val PrevAccent = Color(0xFF22C55E)
private val PrevText = Color(0xFFF3F4F6)
private val PrevSubtext = Color(0xFF9CA3AF)

@Composable
fun SwipePreviewScreen(
    state: StudioState,
    onBackToEdit: () -> Unit,
    onOpenExport: () -> Unit
) {
    val pageCount = if (state.mode == StudioMode.CAROUSEL) state.carouselSlides.coerceIn(1, 20) else 1
    val pagerState = rememberPagerState(pageCount = { pageCount })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PrevBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Minimal Preview Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(PrevPanel)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackToEdit) {
                    Icon(Icons.Default.ArrowBack, "Back to Editor", tint = PrevText)
                }
                Column {
                    Text(
                        text = if (pageCount > 1) "Swipe Preview (${pagerState.currentPage + 1} / $pageCount)" else "Final Preview",
                        color = PrevText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${state.exportConfig.aspectRatio.label} • ${state.exportConfig.outputWidthPx()}×${state.exportConfig.outputHeightPx()}px",
                        color = PrevSubtext,
                        fontSize = 11.sp
                    )
                }
            }

            Button(
                onClick = onOpenExport,
                colors = ButtonDefaults.buttonColors(containerColor = PrevAccent, contentColor = Color(0xFF05130A)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.Default.FileUpload, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Export", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        HorizontalDivider(thickness = 1.dp, color = PrevBorder)

        // Instagram-style Swipeable Card Pager
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val density = LocalDensity.current
            val ratioW = state.exportConfig.outputWidthPx().toFloat()
            val ratioH = state.exportConfig.outputHeightPx().toFloat()
            val aspect = (ratioW / ratioH).coerceIn(0.45f, 1.95f)

            val maxCardH = (maxHeight - 16.dp).coerceAtLeast(220.dp)
            val cardW = (maxCardH * aspect).coerceAtMost(maxWidth)
            val cardH = cardW / aspect
            val fullRibbonW = cardW * pageCount

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.size(width = cardW, height = cardH)
            ) { page ->
                val bg = state.backgroundStyle
                val bgMod = when {
                    state.customBgHex != null -> Modifier.background(Color(state.customBgHex!!))
                    bg.startHex != bg.endHex && !bg.isBlurPhoto -> Modifier.background(Brush.linearGradient(listOf(Color(bg.startHex), Color(bg.endHex))))
                    else -> Modifier.background(Color(bg.startHex))
                }

                Box(
                    modifier = Modifier
                        .size(width = cardW, height = cardH)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, PrevBorder, RoundedCornerShape(12.dp))
                        .clipToBounds()
                ) {
                    // Full ribbon shifted left by page index for exact seam-by-seam preview
                    Box(
                        modifier = Modifier
                            .requiredSize(width = fullRibbonW, height = cardH)
                            .offset(x = -(cardW * page))
                            .then(bgMod)
                    ) {
                        if (state.customBgHex == null && bg.isBlurPhoto && state.slots.isNotEmpty()) {
                            AsyncImage(
                                model = state.slots.first().uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().blur(18.dp)
                            )
                            Box(Modifier.fillMaxSize().background(Color(0x660B0D10)))
                        }

                        if (state.mode == StudioMode.GRID) {
                            BoxWithConstraints(
                                modifier = Modifier.fillMaxSize().padding(state.outerMarginDp.dp)
                            ) {
                                val cw = maxWidth
                                val ch = maxHeight
                                val gap = state.borderDp.dp
                                state.layout.slotRects().forEachIndexed { idx, slotRect ->
                                    val item = state.slots.getOrNull(idx) ?: return@forEachIndexed
                                    val cellW = (cw * slotRect.width - gap).coerceAtLeast(10.dp)
                                    val cellH = (ch * slotRect.height - gap).coerceAtLeast(10.dp)
                                    val offX = cw * slotRect.left + gap / 2
                                    val offY = ch * slotRect.top + gap / 2
                                    val mask = if (item.maskShape != TileMaskShape.RECTANGLE) item.maskShape else slotRect.maskShape
                                    val shape = ShapePaths.composeShape(mask, state.cornerRadiusDp.dp)
                                    Box(
                                        modifier = Modifier
                                            .offset(x = offX, y = offY)
                                            .size(width = cellW, height = cellH)
                                            .clip(shape)
                                            .background(if (slotRect.polaroidBottomPad > 0f) Color(0xFFF3F4F6) else Color(0xFF1C232E))
                                            .border(
                                                width = if (state.borderColorHex != null) 2.dp else 0.dp,
                                                color = state.borderColorHex?.let { Color(it) } ?: Color.Transparent,
                                                shape = shape
                                            )
                                    ) {
                                        val padMod = if (slotRect.polaroidBottomPad > 0f) {
                                            Modifier.padding(start = 6.dp, top = 6.dp, end = 6.dp, bottom = (cellH * slotRect.polaroidBottomPad))
                                        } else Modifier
                                        PreviewMediaTile(item = item, modifier = Modifier.fillMaxSize().then(padMod))
                                    }
                                }
                            }
                        } else {
                            val sorted = state.slots.sortedBy { it.zIndex }
                            sorted.forEach { item ->
                                val itemW = fullRibbonW * item.freeW
                                val itemH = cardH * item.freeH
                                val leftDp = fullRibbonW * item.freeX - itemW / 2
                                val topDp = cardH * item.freeY - itemH / 2
                                val shape = ShapePaths.composeShape(item.maskShape, state.cornerRadiusDp.dp)
                                Box(
                                    modifier = Modifier
                                        .offset(x = leftDp, y = topDp)
                                        .size(width = itemW, height = itemH)
                                        .graphicsLayer {
                                            rotationZ = item.rotationDeg
                                            alpha = item.opacity
                                        }
                                        .then(if (item.shadowDp > 0) Modifier.shadow(item.shadowDp.dp, shape) else Modifier)
                                        .clip(shape)
                                        .background(Color(0xFF1C232E))
                                ) {
                                    PreviewMediaTile(item = item, modifier = Modifier.fillMaxSize())
                                }
                            }
                        }

                        // Overlays
                        val ribbonWpx = with(density) { fullRibbonW.toPx() }.coerceAtLeast(1f)
                        val ribbonHpx = with(density) { cardH.toPx() }.coerceAtLeast(1f)
                        state.overlays.forEach { overlay ->
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .graphicsLayer {
                                        translationX = (overlay.xFraction * ribbonWpx) - 50f
                                        translationY = (overlay.yFraction * ribbonHpx) - 24f
                                        rotationZ = overlay.rotationDeg
                                        alpha = overlay.opacity
                                    }
                            ) {
                                if (overlay.stickerUri != null) {
                                    AsyncImage(
                                        model = overlay.stickerUri,
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.size((cardH * overlay.stickerSizeFraction).coerceIn(48.dp, 180.dp))
                                    )
                                } else {
                                    val ff = when (overlay.fontStyle) {
                                        TextFontStyle.SANS_BOLD -> FontFamily.SansSerif
                                        TextFontStyle.SERIF, TextFontStyle.ITALIC -> FontFamily.Serif
                                        TextFontStyle.MONO -> FontFamily.Monospace
                                    }
                                    val fs = if (overlay.fontStyle == TextFontStyle.ITALIC) FontStyle.Italic else FontStyle.Normal
                                    val ta = when (overlay.alignment) {
                                        TextAlignOption.LEFT -> TextAlign.Start
                                        TextAlignOption.CENTER -> TextAlign.Center
                                        TextAlignOption.RIGHT -> TextAlign.End
                                    }
                                    Surface(
                                        color = if (overlay.usePill) Color(0xCC0B0D10) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = overlay.text,
                                            color = Color(overlay.textColorHex),
                                            fontSize = overlay.fontSizeSp.sp,
                                            fontFamily = ff,
                                            fontStyle = fs,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = ta,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Slide Indicator Dots & Hint
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PrevPanel)
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (pageCount > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(pageCount) { idx ->
                        val active = idx == pagerState.currentPage
                        Box(
                            modifier = Modifier
                                .size(if (active) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (active) PrevAccent else PrevBorder)
                        )
                    }
                }
                Text("Swipe left or right to inspect seamless slide transitions", color = PrevSubtext, fontSize = 11.sp)
            } else {
                Text("Inspect framing and text legibility before exporting", color = PrevSubtext, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun PreviewMediaTile(item: MediaItem, modifier: Modifier) {
    val cm = ColorMatrix().apply {
        val sat = ColorMatrix().apply { setToSaturation(item.saturation) }
        timesAssign(sat)
        val c = item.contrast
        val b = item.brightness * 255f
        val rBoost = item.warmth * 35f
        val bBoost = -item.warmth * 35f
        val t = (-0.5f * c + 0.5f) * 255f + b
        timesAssign(
            ColorMatrix(
                floatArrayOf(
                    c, 0f, 0f, 0f, t + rBoost,
                    0f, c, 0f, 0f, t,
                    0f, 0f, c, 0f, t + bBoost,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    }
    Box(modifier = modifier) {
        AsyncImage(
            model = item.uri,
            contentDescription = null,
            contentScale = if (item.fitInside) ContentScale.Fit else ContentScale.Crop,
            colorFilter = ColorFilter.colorMatrix(cm),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = (if (item.flipHorizontal) -1f else 1f) * item.scale
                    scaleY = item.scale
                    translationX = item.panX
                    translationY = item.panY
                    rotationZ = item.rotationDeg
                }
        )
        if (item.vignette > 0.02f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = (item.vignette * 0.75f).coerceIn(0f, 0.75f)))
                        )
                    )
            )
        }
    }
}
