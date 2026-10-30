package com.contest.travel.preview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun AppBadge(text: String, warm: Boolean = false) {
    Surface(
        color = if (warm) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (warm) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(Pleos215.RadiusSmall)
    ) {
        Text(text, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
internal fun AppPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(Pleos215.RadiusLarge), modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}

@Composable
internal fun PageTitle(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        subtitle?.let { MutedText(it) }
    }
}

@Composable
internal fun MutedText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge)
}

@Composable
internal fun NavigationAction(label: String, enabled: Boolean, filled: Boolean = false, onClick: () -> Unit) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val ink = MaterialTheme.colorScheme.onSurface
    Button(onClick = onClick, enabled = enabled, interactionSource = interactions,
        shape = RoundedCornerShape(if (filled) Pleos215.RadiusLarge else Pleos215.RadiusMedium),
        modifier = Modifier.fillMaxWidth().heightIn(min = if (filled) Pleos215.ButtonLarge else Pleos215.ButtonMedium),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (filled) {
                if (pressed) {
                    if (ink == Color.White) Color(0xFFC7C9CE) else Color(0xFF313236)
                } else MaterialTheme.colorScheme.primary
            } else ink.copy(alpha = if (pressed) .10f else .05f),
            contentColor = if (filled) MaterialTheme.colorScheme.onPrimary else ink,
            disabledContainerColor = ink.copy(alpha = .05f), disabledContentColor = ink.copy(alpha = .32f)),
        contentPadding = PaddingValues(horizontal = if (filled) 40.dp else 32.dp, vertical = 16.dp)) {
        Text(label, style = if (filled) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelLarge)
    }
}

@Composable
internal fun FeatureAction(label: String) {
    NavigationAction(label, enabled = false, onClick = {})
}

@Composable
internal fun PleosTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
    content: @Composable RowScope.() -> Unit) {
    TextButton(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = Pleos215.ButtonMedium),
        shape = RoundedCornerShape(Pleos215.RadiusMedium), contentPadding = contentPadding,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .32f)), content = content)
}

@Composable
internal fun PleosToggle(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, label: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    FilterChip(selected, onClick, enabled = enabled, label = label,
        modifier = modifier.heightIn(min = Pleos215.ButtonMedium), shape = RoundedCornerShape(Pleos215.RadiusMedium),
        border = null, colors = FilterChipDefaults.filterChipColors(
            containerColor = colors.onSurface.copy(alpha = .05f), labelColor = colors.onSurface,
            selectedContainerColor = colors.primary, selectedLabelColor = colors.onPrimary,
            disabledContainerColor = colors.onSurface.copy(alpha = .05f),
            disabledLabelColor = colors.onSurface.copy(alpha = .32f)))
}

@Composable
internal fun FormField(label: String, placeholder: String, symbol: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Surface(color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
            Row(Modifier.fillMaxWidth().heightIn(min = Pleos215.ButtonMedium).padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(placeholder, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                symbol?.let { AppSymbol(it, Modifier.size(Pleos215.IconMedium)) }
            }
        }
    }
}

@Composable
internal fun MemberRow(letter: String, name: String, detail: String, trailing: String? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = Pleos215.ButtonMedium), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp)) {
            Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                Text(letter, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing?.let { Text(it, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Normal) }
    }
}

@Composable
internal fun TwoColumns(wide: Boolean, first: @Composable () -> Unit, second: @Composable () -> Unit) {
    if (wide) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            Box(Modifier.weight(1.3f)) { first() }
            Box(Modifier.weight(1f)) { second() }
        }
    } else {
        first()
        second()
    }
}

@Composable
internal fun RouteMark(modifier: Modifier = Modifier) {
    val tint = MaterialTheme.colorScheme.onSurface
    Canvas(modifier) {
        val route = Path().apply {
            moveTo(size.width * .2f, size.height * .8f)
            cubicTo(size.width * .15f, size.height * .35f, size.width * .85f, size.height * .65f, size.width * .8f, size.height * .2f)
        }
        drawPath(route, tint, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(tint, 6.dp.toPx(), Offset(size.width * .2f, size.height * .8f))
        drawCircle(tint, 6.dp.toPx(), Offset(size.width * .8f, size.height * .2f))
    }
}

@Composable
internal fun AppSymbol(kind: String, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.onSurface) {
    Canvas(modifier) {
        val stroke = Stroke(size.width * (3.6f / 48f), cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun point(x: Float, y: Float) = Offset(size.width * x, size.height * y)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(tint, point(x1, y1), point(x2, y2), stroke.width, StrokeCap.Round)
        when (kind) {
            "back" -> {
                line(.82f, .5f, .18f, .5f)
                line(.18f, .5f, .5f, .18f)
                line(.18f, .5f, .5f, .82f)
            }
            "home" -> {
                val outline = Path().apply {
                    moveTo(size.width * .12f, size.height * .45f)
                    lineTo(size.width * .5f, size.height * .13f)
                    lineTo(size.width * .88f, size.height * .45f)
                    moveTo(size.width * .24f, size.height * .4f)
                    lineTo(size.width * .24f, size.height * .86f)
                    lineTo(size.width * .76f, size.height * .86f)
                    lineTo(size.width * .76f, size.height * .4f)
                }
                drawPath(outline, tint, style = stroke)
            }
            "people" -> {
                drawCircle(tint, size.width * .13f, point(.5f, .26f), style = stroke)
                drawCircle(tint, size.width * .1f, point(.15f, .38f), style = stroke)
                drawCircle(tint, size.width * .1f, point(.85f, .38f), style = stroke)
                drawArc(tint, 180f, 180f, false, point(.23f, .48f), Size(size.width * .54f, size.height * .62f), style = stroke)
                line(.05f, .65f, .05f, .86f); line(.95f, .65f, .95f, .86f)
            }
            "mic" -> {
                drawRoundRect(tint, point(.35f, .08f), Size(size.width * .3f, size.height * .5f),
                    CornerRadius(size.width * .15f), style = stroke)
                drawArc(tint, 0f, 180f, false, point(.2f, .25f), Size(size.width * .6f, size.height * .5f), style = stroke)
                line(.5f, .75f, .5f, .93f); line(.32f, .94f, .68f, .94f)
            }
            "chat" -> {
                drawRoundRect(tint, point(.1f, .14f), Size(size.width * .8f, size.height * .6f),
                    CornerRadius(size.width * .12f), style = stroke)
                line(.25f, .74f, .25f, .91f); line(.25f, .91f, .45f, .74f)
                line(.28f, .36f, .72f, .36f); line(.28f, .52f, .57f, .52f)
            }
            else -> {
                drawRoundRect(tint, point(.12f, .18f), Size(size.width * .76f, size.height * .68f),
                    CornerRadius(size.width * .08f), style = stroke)
                if (kind == "calendar") {
                    line(.12f, .39f, .88f, .39f); line(.3f, .08f, .3f, .28f); line(.7f, .08f, .7f, .28f)
                } else {
                    drawCircle(tint, size.width * .08f, point(.68f, .36f))
                    line(.2f, .75f, .43f, .48f); line(.43f, .48f, .67f, .76f)
                }
            }
        }
    }
}

@Composable
internal fun TripMap(height: Dp = 300.dp) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val street = MaterialTheme.colorScheme.surface
    val route = MaterialTheme.colorScheme.primary
    BoxWithConstraints(Modifier.fillMaxWidth().height(height)) {
        Surface(color = base, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize()) {
                val river = Path().apply {
                    moveTo(size.width * .7f, -40f)
                    cubicTo(size.width * .35f, size.height * .25f, size.width * .95f, size.height * .7f, size.width * .6f, size.height + 40f)
                }
                drawPath(river, route.copy(alpha = .10f), style = Stroke(42.dp.toPx()))
                listOf(.2f, .48f, .76f).forEach { x ->
                    drawLine(street, Offset(size.width * x, 0f), Offset(size.width * (x - .18f), size.height), 18.dp.toPx())
                }
                listOf(.25f, .65f).forEach { y ->
                    drawLine(street, Offset(0f, size.height * y), Offset(size.width, size.height * (y + .1f)), 18.dp.toPx())
                }
                val path = Path().apply {
                    moveTo(size.width * .12f, size.height * .75f)
                    cubicTo(size.width * .2f, size.height * .1f, size.width * .6f, size.height * .95f, size.width * .85f, size.height * .2f)
                }
                drawPath(path, route.copy(alpha = .2f), style = Stroke(18.dp.toPx(), cap = StrokeCap.Round))
                drawPath(path, Pleos215.Positive, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
            }
        }
        listOf(Triple("A", .15f, .57f), Triple("B", .48f, .43f), Triple("C", .75f, .23f)).forEach { (label, x, y) ->
            Surface(color = route, contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp), modifier = Modifier.offset(x = maxWidth * x, y = maxHeight * y)) {
                Text(label, Modifier.padding(horizontal = 16.dp, vertical = 10.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
        Box(Modifier.align(Alignment.TopStart).padding(16.dp)) { AppBadge("일행 3대") }
        Box(Modifier.align(Alignment.BottomEnd).padding(16.dp)) { AppBadge("강릉 방향") }
    }
}

@Composable
internal fun PhotoTile(modifier: Modifier = Modifier) {
    val fill = MaterialTheme.colorScheme.primary.copy(alpha = .22f)
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Canvas(Modifier.fillMaxWidth().height(142.dp).padding(24.dp)) {
            drawCircle(fill, 15.dp.toPx(), Offset(size.width * .76f, size.height * .23f))
            val mountain = Path().apply {
                moveTo(0f, size.height); lineTo(size.width * .32f, size.height * .2f)
                lineTo(size.width * .58f, size.height * .75f); lineTo(size.width * .77f, size.height * .47f)
                lineTo(size.width, size.height); close()
            }
            drawPath(mountain, fill)
        }
    }
}
