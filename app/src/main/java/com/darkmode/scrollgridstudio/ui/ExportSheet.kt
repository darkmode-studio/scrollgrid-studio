package com.darkmode.scrollgridstudio.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkmode.scrollgridstudio.export.ExportResult
import com.darkmode.scrollgridstudio.model.*
import com.darkmode.scrollgridstudio.ui.theme.LocalScrollGridPalette

@Composable
fun ExportSheetContent(
    state: StudioState,
    isExporting: Boolean,
    exportProgress: Float,
    exportStage: String,
    lastResult: ExportResult?,
    onConfigChange: (ExportConfig) -> Unit,
    onExportGallery: () -> Unit,
    onExportZip: () -> Unit,
    onShare: () -> Unit
) {
    val palette = LocalScrollGridPalette.current
    val cfg = state.exportConfig
    val supportsAlpha = cfg.format == ExportFormat.PNG || cfg.format == ExportFormat.WEBP

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Export Studio", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                Text(
                    "${cfg.outputWidthPx()} × ${cfg.outputHeightPx()}px · ${cfg.format.label}",
                    fontSize = 11.sp,
                    color = palette.textSecondary
                )
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = palette.elevated,
                border = BorderStroke(1.dp, palette.border)
            ) {
                Text(
                    cfg.aspectRatio.shortLabel,
                    color = palette.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Format: JPG, PNG, WebP
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Format", color = palette.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExportFormat.entries.forEach { fmt ->
                    val selected = cfg.format == fmt
                    Surface(
                        onClick = { onConfigChange(cfg.copy(format = fmt)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) palette.elevated else palette.cardPaper,
                        border = BorderStroke(1.dp, if (selected) palette.accent else palette.border)
                    ) {
                        Box(Modifier.padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                            Text(
                                fmt.label,
                                color = if (selected) palette.textPrimary else palette.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Transparent Background (PNG / WebP)
        if (supportsAlpha) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Transparent Background", color = palette.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text("Omit canvas fill for PNG/WebP alpha transparency", color = palette.textSecondary, fontSize = 10.sp)
                }
                Switch(
                    checked = cfg.transparentBackground,
                    onCheckedChange = { onConfigChange(cfg.copy(transparentBackground = it)) }
                )
            }
        }

        // Resolution
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Resolution", color = palette.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ResolutionPreset.entries.forEach { res ->
                    FilterChip(
                        selected = cfg.resolution == res,
                        onClick = { onConfigChange(cfg.copy(resolution = res)) },
                        label = { Text(res.label, fontSize = 11.sp) }
                    )
                }
            }
        }

        // Quality (JPG / WebP)
        if (cfg.format != ExportFormat.PNG) {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Quality", color = palette.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text("${cfg.qualityPercent}%", color = palette.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = cfg.qualityPercent.toFloat(),
                    onValueChange = { onConfigChange(cfg.copy(qualityPercent = it.toInt())) },
                    valueRange = 60f..100f
                )
            }
        }

        // Output Mode (Individual Slides / Merged Image / Selected Slide)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Output Mode", color = palette.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ExportMode.entries.forEach { mode ->
                    FilterChip(
                        selected = cfg.mode == mode,
                        onClick = { onConfigChange(cfg.copy(mode = mode)) },
                        label = { Text(mode.label, fontSize = 11.sp) }
                    )
                }
            }
        }

        if (state.mode == StudioMode.CAROUSEL && cfg.mode == ExportMode.AUTO_SLICE) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SlideOrder.entries.forEach { order ->
                    FilterChip(
                        selected = cfg.slideOrder == order,
                        onClick = { onConfigChange(cfg.copy(slideOrder = order)) },
                        label = { Text(order.label, fontSize = 11.sp) }
                    )
                }
            }
        }

        // Real-time Export Progress
        if (isExporting) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = palette.cardPaper,
                border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.5f))
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(exportStage.ifBlank { "Exporting…" }, color = palette.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text("${(exportProgress * 100).toInt()}%", color = palette.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = { exportProgress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = palette.accent
                    )
                }
            }
        }

        // Result / Error Feedback
        if (!isExporting && lastResult != null) {
            val isErr = !lastResult.errorMessage.isNullOrBlank()
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = if (isErr) palette.danger.copy(alpha = 0.14f) else palette.bluePale,
                border = BorderStroke(1.dp, if (isErr) palette.danger else palette.accent)
            ) {
                Row(
                    Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (isErr) Icons.Default.ErrorOutline else Icons.Default.CheckCircleOutline,
                        null,
                        tint = if (isErr) palette.danger else palette.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        lastResult.errorMessage ?: lastResult.summary,
                        color = palette.textPrimary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Actions: Save, ZIP, Share
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onExportGallery,
                enabled = !isExporting,
                modifier = Modifier
                    .weight(1.3f)
                    .height(46.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = Color.White)
            ) {
                Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onExportZip,
                enabled = !isExporting,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, palette.border)
            ) {
                Icon(Icons.Default.FolderZip, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("ZIP", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = onShare,
                enabled = !isExporting,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, palette.border)
            ) {
                Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Share", fontSize = 12.sp)
            }
        }
    }
}
