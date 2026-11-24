package com.darkmode.scrollgridstudio.ui

import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkmode.scrollgridstudio.data.ImportedFont
import com.darkmode.scrollgridstudio.model.TextAlignOption
import com.darkmode.scrollgridstudio.model.TextFontStyle
import com.darkmode.scrollgridstudio.model.TextOverlay
import com.darkmode.scrollgridstudio.ui.theme.LocalScrollGridPalette
import java.io.File

data class BuiltInFontItem(val name: String, val style: TextFontStyle)

val StudioFontCatalog = listOf(
    BuiltInFontItem("Roboto", TextFontStyle.SANS_BOLD),
    BuiltInFontItem("Inter Sans", TextFontStyle.SANS_BOLD),
    BuiltInFontItem("Editorial Serif", TextFontStyle.SERIF),
    BuiltInFontItem("Archive Mono", TextFontStyle.MONO),
    BuiltInFontItem("Italic Script", TextFontStyle.ITALIC),
    BuiltInFontItem("Malayalam Sans", TextFontStyle.SANS_BOLD),
    BuiltInFontItem("Malayalam Serif", TextFontStyle.SERIF)
)

private val TextColorSwatches = listOf(
    0xFFF3F4F6L, 0xFF0D0F14L, 0xFF5B8DEFL, 0xFF38BDF8L, 0xFF34D399L,
    0xFFFBBF24L, 0xFFFB7185L, 0xFFC084FCL, 0xFFE07A5FL, 0xFFE5D4B3L
)

private val BgColorSwatches = listOf(
    0xCC0D0F14L, 0xCCF3F4F6L, 0xCC1E293BL, 0xCC1B253AL, 0xCC3B1D2AL, 0xCC143326L
)

fun resolveComposeFontFamily(overlay: TextOverlay): FontFamily {
    val path = overlay.customFontPath
    if (!path.isNullOrBlank()) {
        val f = File(path)
        if (f.exists()) {
            runCatching { return FontFamily(Typeface.createFromFile(f)) }
        }
    }
    return when (overlay.fontStyle) {
        TextFontStyle.SANS_BOLD -> FontFamily.SansSerif
        TextFontStyle.SERIF, TextFontStyle.ITALIC -> FontFamily.Serif
        TextFontStyle.MONO -> FontFamily.Monospace
    }
}

@Composable
fun TypographyPanelContent(
    selectedText: TextOverlay?,
    textInput: String,
    onTextInputChange: (String) -> Unit,
    importedFonts: List<ImportedFont>,
    onImportFontClick: () -> Unit,
    onAddOrUpdateText: () -> Unit,
    onUpdateSelectedText: ((TextOverlay) -> TextOverlay) -> Unit
) {
    val palette = LocalScrollGridPalette.current
    var subSection by remember { mutableStateOf("Font") }

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Input Row (supports English & Malayalam)
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = onTextInputChange,
                placeholder = { Text("Type English or മലയാളം text…", fontSize = 12.sp, color = palette.textSecondary) },
                modifier = Modifier.weight(1f),
                maxLines = 2,
                shape = RoundedCornerShape(8.dp)
            )
            Button(
                onClick = onAddOrUpdateText,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = Color.White)
            ) {
                Text(if (selectedText != null) "Apply" else "Add", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Compact Sub-tabs: Font, Color, Effects, Spacing, Box
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Font", "Color", "Effects", "Spacing", "Box").forEach { tab ->
                val active = subSection == tab
                Surface(
                    onClick = { subSection = tab },
                    shape = RoundedCornerShape(6.dp),
                    color = if (active) palette.elevated else Color.Transparent,
                    border = BorderStroke(1.dp, if (active) palette.accent else palette.border)
                ) {
                    Text(
                        tab,
                        color = if (active) palette.textPrimary else palette.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        if (selectedText == null) {
            Text(
                "Tap Add to place text, or tap existing text on the canvas to style it.",
                color = palette.textSecondary,
                fontSize = 11.sp
            )
            return
        }

        when (subSection) {
            "Font" -> {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Font Browser", color = palette.textSecondary, fontSize = 11.sp)
                    OutlinedButton(
                        onClick = onImportFontClick,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, palette.border)
                    ) {
                        Icon(Icons.Default.FileUpload, null, modifier = Modifier.size(14.dp), tint = palette.accent)
                        Spacer(Modifier.width(4.dp))
                        Text("Import .ttf/.otf", fontSize = 11.sp, color = palette.textPrimary)
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(importedFonts) { font ->
                        val active = selectedText.customFontPath == font.path
                        Surface(
                            onClick = {
                                onUpdateSelectedText { it.copy(fontName = font.name, customFontPath = font.path) }
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = if (active) palette.elevated else palette.cardPaper,
                            border = BorderStroke(1.dp, if (active) palette.accent else palette.border)
                        ) {
                            Text(
                                "${font.name} (Custom)",
                                color = palette.textPrimary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    items(StudioFontCatalog) { fontItem ->
                        val active = selectedText.customFontPath == null && selectedText.fontName == fontItem.name
                        Surface(
                            onClick = {
                                onUpdateSelectedText {
                                    it.copy(fontName = fontItem.name, fontStyle = fontItem.style, customFontPath = null)
                                }
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = if (active) palette.elevated else palette.cardPaper,
                            border = BorderStroke(1.dp, if (active) palette.accent else palette.border)
                        ) {
                            Text(
                                fontItem.name,
                                color = palette.textPrimary,
                                fontSize = 11.sp,
                                fontFamily = when (fontItem.style) {
                                    TextFontStyle.SERIF, TextFontStyle.ITALIC -> FontFamily.Serif
                                    TextFontStyle.MONO -> FontFamily.Monospace
                                    else -> FontFamily.SansSerif
                                },
                                fontStyle = if (fontItem.style == TextFontStyle.ITALIC) FontStyle.Italic else FontStyle.Normal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                // Alignment + Size
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TextAlignOption.entries.forEach { opt ->
                        val active = selectedText.alignment == opt
                        Surface(
                            onClick = { onUpdateSelectedText { it.copy(alignment = opt) } },
                            shape = RoundedCornerShape(6.dp),
                            color = if (active) palette.elevated else palette.cardPaper,
                            border = BorderStroke(1.dp, if (active) palette.accent else palette.border)
                        ) {
                            Icon(
                                when (opt) {
                                    TextAlignOption.LEFT -> Icons.Default.FormatAlignLeft
                                    TextAlignOption.CENTER -> Icons.Default.FormatAlignCenter
                                    TextAlignOption.RIGHT -> Icons.Default.FormatAlignRight
                                },
                                contentDescription = opt.label,
                                tint = if (active) palette.accent else palette.textSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp).size(16.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                    Text("Size ${selectedText.fontSizeSp.toInt()}sp", color = palette.textSecondary, fontSize = 11.sp)
                    Slider(
                        value = selectedText.fontSizeSp,
                        onValueChange = { v -> onUpdateSelectedText { it.copy(fontSizeSp = v) } },
                        valueRange = 12f..64f,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            "Color" -> {
                Text("Text Color", color = palette.textSecondary, fontSize = 11.sp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextColorSwatches.forEach { hex ->
                        val active = selectedText.textColorHex == hex
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(if (active) 2.dp else 1.dp, if (active) palette.accent else palette.border, CircleShape)
                                .clickable { onUpdateSelectedText { it.copy(textColorHex = hex) } }
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Gradient Text", color = palette.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = selectedText.useGradient,
                        onCheckedChange = { checked -> onUpdateSelectedText { it.copy(useGradient = checked) } }
                    )
                }
                if (selectedText.useGradient) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextColorSwatches.forEach { hex ->
                            val active = selectedText.gradientEndHex == hex
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(Color(selectedText.textColorHex), Color(hex))))
                                    .border(if (active) 2.dp else 1.dp, if (active) palette.accent else palette.border, CircleShape)
                                    .clickable { onUpdateSelectedText { it.copy(gradientEndHex = hex) } }
                            )
                        }
                    }
                }
            }

            "Effects" -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedText.useOutline,
                        onClick = { onUpdateSelectedText { it.copy(useOutline = !it.useOutline) } },
                        label = { Text("Outline", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedText.useShadow,
                        onClick = { onUpdateSelectedText { it.copy(useShadow = !it.useShadow) } },
                        label = { Text("Shadow", fontSize = 11.sp) }
                    )
                }
                if (selectedText.useOutline) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Outline ${String.format("%.1f", selectedText.outlineWidthDp)}dp", color = palette.textSecondary, fontSize = 11.sp)
                        Slider(
                            value = selectedText.outlineWidthDp,
                            onValueChange = { v -> onUpdateSelectedText { it.copy(outlineWidthDp = v) } },
                            valueRange = 0.5f..6f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (selectedText.useShadow) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Shadow ${selectedText.shadowRadiusDp.toInt()}dp", color = palette.textSecondary, fontSize = 11.sp)
                        Slider(
                            value = selectedText.shadowRadiusDp,
                            onValueChange = { v -> onUpdateSelectedText { it.copy(shadowRadiusDp = v) } },
                            valueRange = 1f..16f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Opacity ${(selectedText.opacity * 100).toInt()}%", color = palette.textSecondary, fontSize = 11.sp)
                    Slider(
                        value = selectedText.opacity,
                        onValueChange = { v -> onUpdateSelectedText { it.copy(opacity = v) } },
                        valueRange = 0.15f..1f,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            "Spacing" -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Letter ${String.format("%.2f", selectedText.letterSpacingEm)}", color = palette.textSecondary, fontSize = 11.sp)
                    Slider(
                        value = selectedText.letterSpacingEm,
                        onValueChange = { v -> onUpdateSelectedText { it.copy(letterSpacingEm = v) } },
                        valueRange = 0f..0.4f,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Line ${String.format("%.2f", selectedText.lineHeightMultiplier)}x", color = palette.textSecondary, fontSize = 11.sp)
                    Slider(
                        value = selectedText.lineHeightMultiplier,
                        onValueChange = { v -> onUpdateSelectedText { it.copy(lineHeightMultiplier = v) } },
                        valueRange = 0.9f..2.2f,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Rotate ${selectedText.rotationDeg.toInt()}°", color = palette.textSecondary, fontSize = 11.sp)
                    Slider(
                        value = selectedText.rotationDeg,
                        onValueChange = { v -> onUpdateSelectedText { it.copy(rotationDeg = v) } },
                        valueRange = -180f..180f,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { onUpdateSelectedText { it.copy(rotationDeg = 0f, xFraction = 0.5f, yFraction = 0.5f) } }) {
                        Text("Center", fontSize = 11.sp)
                    }
                }
            }

            "Box" -> {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Text Background", color = palette.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = selectedText.usePill,
                        onCheckedChange = { checked -> onUpdateSelectedText { it.copy(usePill = checked) } }
                    )
                }
                if (selectedText.usePill) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BgColorSwatches.forEach { hex ->
                            val active = selectedText.bgColorHex == hex
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(hex))
                                    .border(if (active) 2.dp else 1.dp, if (active) palette.accent else palette.border, RoundedCornerShape(6.dp))
                                    .clickable { onUpdateSelectedText { it.copy(bgColorHex = hex) } }
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Padding ${selectedText.paddingDp}dp", color = palette.textSecondary, fontSize = 11.sp)
                        Slider(
                            value = selectedText.paddingDp.toFloat(),
                            onValueChange = { v -> onUpdateSelectedText { it.copy(paddingDp = v.toInt()) } },
                            valueRange = 0f..28f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
