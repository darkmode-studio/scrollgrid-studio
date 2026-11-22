package com.darkmode.scrollgridstudio.ui

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.darkmode.scrollgridstudio.model.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Paper = Color(0xFFF2F0E9)
private val Ink = Color(0xFF171916)
private val MutedInk = Color(0xFF777A72)
private val Blue = Color(0xFF3157F6)
private val BluePale = Color(0xFFE0E6FF)
private val Coral = Color(0xFFE8795B)
private val CardPaper = Color(0xFFFFFEFA)
private val Hairline = Color(0xFFE0DED5)

@Composable
fun HomeHubScreen(
    drafts: List<SavedDraftMeta>,
    onStartPhotoGrid: () -> Unit,
    onStartCarousel: (Int) -> Unit,
    onStartFreeform: () -> Unit,
    onSelectScrlTemplate: (ScrlTemplate) -> Unit,
    onSelectGridTemplate: (LayoutStyle) -> Unit,
    onOpenDraft: (SavedDraftMeta) -> Unit,
    onDuplicateDraft: (SavedDraftMeta) -> Unit,
    onDeleteDraft: (SavedDraftMeta) -> Unit
) {
    var templateFilter by remember { mutableStateOf("All") }
    val dateFmt = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    val featured = remember {
        listOf(LayoutStyle.BENTO_4, LayoutStyle.POLAROID_2, LayoutStyle.SLANT_3, LayoutStyle.FILM_3,
            LayoutStyle.HEART_COLLAGE_3, LayoutStyle.HEX_HONEYCOMB_4, LayoutStyle.GRID_2X2, LayoutStyle.GRID_9)
    }
    val visibleTemplates = featured.filter { templateFilter == "All" || it.category.equals(templateFilter, true) }

    Column(Modifier.fillMaxSize().background(Paper).statusBarsPadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(top = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("SCROLLGRID", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-.8).sp)
                    Text("THE COLLAGE STUDIO", color = MutedInk, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.4.sp)
                }
                Box(Modifier.size(42.dp).clip(CircleShape).background(Ink), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Paper, modifier = Modifier.size(19.dp))
                }
            }

            Column(Modifier.padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(Coral))
                    Text("YOUR CREATIVE SPACE", color = MutedInk, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
                Text(
                    "Good stories\ndeserve a great\nlayout.",
                    color = Ink, fontSize = 43.sp, lineHeight = 43.sp,
                    fontWeight = FontWeight.Black, letterSpacing = (-2.2).sp
                )
                Text(
                    "Turn your photos into something worth stopping for.",
                    color = MutedInk, fontSize = 13.sp, lineHeight = 19.sp
                )
                HeroComposition(Modifier.fillMaxWidth().height(236.dp))
                Button(
                    onClick = onStartPhotoGrid,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(9.dp))
                    Text("Create a collage", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(19.dp))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                HomeSectionTitle("PICK YOUR FORMAT", "Start with a canvas")
                Row(Modifier.padding(horizontal = 22.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FormatCard("Photo grid", "Classic collage", Icons.Default.GridView, Blue, Modifier.weight(1f), onStartPhotoGrid)
                    FormatCard("Carousel", "Swipe post", Icons.Default.ViewCarousel, Coral, Modifier.weight(1f)) { onStartCarousel(3) }
                }
                Surface(
                    onClick = onStartFreeform,
                    modifier = Modifier.padding(horizontal = 22.dp).fillMaxWidth(),
                    color = Ink, shape = RoundedCornerShape(16.dp)
                ) {
                    Row(Modifier.padding(horizontal = 17.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Layers, null, tint = Color.White, modifier = Modifier.size(21.dp))
                        }
                        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Freeform canvas", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Arrange every layer your way", color = Color(0xFFB9BDB5), fontSize = 10.sp)
                        }
                        Icon(Icons.Default.ArrowOutward, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.padding(horizontal = 22.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                    HomeSectionTitle("PICK UP WHERE YOU LEFT OFF", "Recent projects")
                    Text(drafts.size.toString().padStart(2, '0'), color = Blue, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
                if (drafts.isEmpty()) {
                    Surface(
                        modifier = Modifier.padding(horizontal = 22.dp).fillMaxWidth(),
                        color = CardPaper, shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Hairline)
                    ) {
                        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(BluePale), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.FolderOpen, null, tint = Blue)
                            }
                            Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Your gallery starts here", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Create your first design and it will appear here.", color = MutedInk, fontSize = 10.sp, lineHeight = 15.sp)
                            }
                        }
                    }
                } else {
                    LazyRow(contentPadding = PaddingValues(horizontal = 22.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(drafts, key = { it.id }) { draft ->
                            DraftCard(draft, if (draft.updatedAtMillis > 0L) dateFmt.format(Date(draft.updatedAtMillis)) else "Saved",
                                { onOpenDraft(draft) }, { onDuplicateDraft(draft) }, { onDeleteDraft(draft) })
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(Modifier.padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HomeSectionTitle("DESIGNED TO GET YOU STARTED", "Find your layout")
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("All", "Classic", "Hero", "Shapes", "Masonry").forEach { category ->
                            val selected = templateFilter == category
                            Surface(
                                onClick = { templateFilter = category },
                                color = if (selected) Ink else CardPaper,
                                shape = CircleShape,
                                border = BorderStroke(1.dp, if (selected) Ink else Hairline)
                            ) {
                                Text(category, color = if (selected) Color.White else MutedInk, fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp))
                            }
                        }
                    }
                }
                Column(Modifier.padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    visibleTemplates.chunked(2).forEach { rowTemplates ->
                        Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                            rowTemplates.forEach { layout -> TemplateCard(layout, Modifier.weight(1f)) { onSelectGridTemplate(layout) } }
                            if (rowTemplates.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Column(Modifier.padding(horizontal = 22.dp)) {
                    HomeSectionTitle("MADE FOR THE SWIPE", "Carousel stories")
                }
                LazyRow(contentPadding = PaddingValues(horizontal = 22.dp), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    items(ScrlTemplate.entries.toList()) { template -> CarouselCard(template) { onSelectScrlTemplate(template) } }
                }
            }
        }
    }
}

@Composable
private fun HeroComposition(modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(22.dp)).background(Color(0xFFDDD8CA))) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color(0xFFB5C0A8), size.height * .63f, androidx.compose.ui.geometry.Offset(size.width * .88f, size.height * .12f))
            drawCircle(Color(0xFF3157F6).copy(alpha = .12f), size.height * .55f, androidx.compose.ui.geometry.Offset(size.width * .12f, size.height * .92f))
            drawLine(Color(0xFF777A72).copy(alpha = .35f), androidx.compose.ui.geometry.Offset(size.width * .5f, 0f), androidx.compose.ui.geometry.Offset(size.width * .5f, size.height), 1.dp.toPx())
        }
        Box(Modifier.align(Alignment.CenterStart).padding(start = 19.dp, top = 12.dp, bottom = 12.dp).width(112.dp).fillMaxHeight(.82f)
            .clip(RoundedCornerShape(9.dp)).background(Color(0xFF536B55))) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color(0xFFE8C89B), size.minDimension * .18f, androidx.compose.ui.geometry.Offset(size.width * .72f, size.height * .25f))
                val p = Path().apply {
                    moveTo(0f, size.height * .72f); lineTo(size.width * .42f, size.height * .36f)
                    lineTo(size.width * .72f, size.height * .62f); lineTo(size.width, size.height * .44f)
                    lineTo(size.width, size.height); lineTo(0f, size.height); close()
                }
                drawPath(p, Color(0xFF253C32))
            }
        }
        Box(Modifier.align(Alignment.Center).padding(start = 75.dp, top = 28.dp).width(100.dp).fillMaxHeight(.72f)
            .clip(RoundedCornerShape(9.dp)).background(Color(0xFFC17C62)).border(4.dp, Paper, RoundedCornerShape(9.dp))) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color(0xFFF0CBA0), size.minDimension * .17f, androidx.compose.ui.geometry.Offset(size.width * .5f, size.height * .3f))
                drawRoundRect(Color(0xFF34483B), androidx.compose.ui.geometry.Offset(size.width * .17f, size.height * .57f),
                    androidx.compose.ui.geometry.Size(size.width * .66f, size.height * .5f), androidx.compose.ui.geometry.CornerRadius(20f))
            }
        }
        Box(Modifier.align(Alignment.CenterEnd).padding(end = 18.dp, top = 14.dp).width(83.dp).fillMaxHeight(.86f)
            .clip(RoundedCornerShape(9.dp)).background(Color(0xFFE5B978)).border(4.dp, Paper, RoundedCornerShape(9.dp))) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color(0xFF9B604C), size.minDimension * .19f, androidx.compose.ui.geometry.Offset(size.width * .52f, size.height * .28f))
                drawRect(Color(0xFF41584A), androidx.compose.ui.geometry.Offset(0f, size.height * .58f), androidx.compose.ui.geometry.Size(size.width, size.height * .42f))
            }
        }
        Surface(Modifier.align(Alignment.TopStart).padding(13.dp), color = Ink, shape = CircleShape) {
            Text("LAYOUT NO. 01", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
        }
        Surface(Modifier.align(Alignment.BottomEnd).padding(13.dp), color = Blue, shape = CircleShape) {
            Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.padding(10.dp).size(16.dp))
        }
    }
}

@Composable
private fun HomeSectionTitle(eyebrow: String, title: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(eyebrow, color = Blue, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
        Text(title, color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Black, letterSpacing = (-.8).sp)
    }
}

@Composable
private fun FormatCard(title: String, detail: String, icon: ImageVector, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier, color = CardPaper, shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Hairline)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
            Box(Modifier.size(39.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(detail, color = MutedInk, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun DraftCard(draft: SavedDraftMeta, dateText: String, onOpen: () -> Unit, onDuplicate: () -> Unit, onDelete: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    Column(Modifier.width(194.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(Modifier.fillMaxWidth().height(145.dp).clip(RoundedCornerShape(15.dp)).background(Color(0xFFE4E1D8)).clickable(onClick = onOpen)) {
            if (draft.previewUris.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Collections, null, tint = Blue, modifier = Modifier.size(30.dp))
                }
            } else {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    draft.previewUris.take(3).forEachIndexed { index, uri ->
                        AsyncImage(model = Uri.parse(uri), contentDescription = if (index == 0) draft.name else null,
                            contentScale = ContentScale.Crop, modifier = Modifier.weight(if (index == 0) 1.2f else 1f).fillMaxHeight())
                    }
                }
            }
            Surface(Modifier.align(Alignment.TopEnd).padding(7.dp), color = Color.White.copy(alpha = .9f), shape = CircleShape) {
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.MoreHoriz, "Project options", tint = Ink, modifier = Modifier.size(17.dp))
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(text = { Text("Open") }, onClick = { menuExpanded = false; onOpen() })
                        DropdownMenuItem(text = { Text("Duplicate") }, onClick = { menuExpanded = false; onDuplicate() })
                        DropdownMenuItem(text = { Text("Delete", color = Color(0xFFB63D32)) }, onClick = { menuExpanded = false; onDelete() })
                    }
                }
            }
        }
        Column(Modifier.clickable(onClick = onOpen).padding(horizontal = 2.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(draft.name, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(dateText + "  ·  " + draft.itemCount + " items", color = MutedInk, fontSize = 10.sp)
        }
    }
}

@Composable
private fun TemplateCard(layout: LayoutStyle, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier, color = CardPaper, shape = RoundedCornerShape(17.dp),
        border = BorderStroke(1.dp, Hairline)) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(Modifier.fillMaxWidth().aspectRatio(1.12f).clip(RoundedCornerShape(11.dp)).background(Color(0xFFE8E5DC)).padding(10.dp)) {
                LayoutArtwork(layout, Modifier.fillMaxSize())
                Surface(Modifier.align(Alignment.TopEnd), color = Ink, shape = CircleShape) {
                    Text(layout.slotCount.toString() + "P", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(layout.label, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(layout.category.uppercase(), color = MutedInk, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
            }
        }
    }
}

@Composable
private fun LayoutArtwork(layout: LayoutStyle, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val gap = 3.dp.toPx()
        val palette = listOf(Color(0xFF3157F6), Color(0xFFE8795B), Color(0xFFB5C0A8), Color(0xFF343B36), Color(0xFFE2B978))
        layout.slotRects().forEachIndexed { index, rect ->
            val left = rect.left * size.width + gap
            val top = rect.top * size.height + gap
            val width = (rect.width * size.width - gap * 2).coerceAtLeast(2f)
            val height = (rect.height * size.height - gap * 2).coerceAtLeast(2f)
            drawRoundRect(palette[index % palette.size], androidx.compose.ui.geometry.Offset(left, top),
                androidx.compose.ui.geometry.Size(width, height), androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()))
            drawRoundRect(Color.White.copy(alpha = .3f), androidx.compose.ui.geometry.Offset(left, top),
                androidx.compose.ui.geometry.Size(width, height), androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()), style = Stroke(1.dp.toPx()))
        }
    }
}

@Composable
private fun CarouselCard(template: ScrlTemplate, onClick: () -> Unit) {
    Surface(onClick = onClick, color = CardPaper, shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Hairline), modifier = Modifier.width(220.dp)) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth().height(92.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(template.slides.coerceAtMost(5)) { index ->
                    Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(5.dp))
                        .background(listOf(Color(0xFF536B55), Color(0xFFE8795B), Color(0xFF3157F6), Color(0xFFE2B978), Color(0xFF465747))[index]))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(template.label, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(template.slides.toString() + " seamless slides", color = MutedInk, fontSize = 10.sp)
                }
                Icon(Icons.Default.ArrowOutward, null, tint = Blue, modifier = Modifier.size(17.dp))
            }
        }
    }
}
