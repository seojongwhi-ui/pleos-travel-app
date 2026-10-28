package com.contest.travel.preview

import android.graphics.Paint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

internal val convoyColors = listOf(Color(0xFF245BB0), Color(0xFFE57C32), Color(0xFF29866F))
internal val convoyNames = listOf("우리 차", "민수네 차", "지연네 차")
private val progressOffsets = listOf(0.0, -.12, .14)

@Composable
internal fun ConvoyJourneyPanel(wide: Boolean, enabled: Boolean) {
    val convoy = LocalConvoy.current
    val live = LocalTripLive.current
    val actions = LocalTripActions.current
    AppPanel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionTitle("일행 한눈에")
                MutedText("서울에서 강릉까지, 같은 목적지를 향해")
            }
            AppBadge("로컬 시뮬레이션", warm = true)
        }
        ConvoyOverviewMap(wide)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            convoyNames.forEachIndexed { index, name ->
                val progress = (convoy.progress + progressOffsets[index]).coerceIn(0.0, 1.0)
                Surface(color = convoyColors[index].copy(alpha = .10f), shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        val real = index == 0 && convoy.useRealCar && live.location.status == ReadStatus.READY
                        Text(if (real) "내비 위치" else "시연 이동 ${(progress * 100).roundToInt()}%",
                            fontSize = 21.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = convoy.automatic, onClick = convoy.toggleAutomatic, enabled = enabled,
                label = { Text("2분 자동", fontSize = 22.sp) }, modifier = Modifier.heightIn(min = 76.dp))
            OutlinedButton(onClick = { convoy.advance(); actions.refresh() }, enabled = enabled,
                modifier = Modifier.heightIn(min = 76.dp), shape = RoundedCornerShape(16.dp)) {
                Text("지금 갱신", fontSize = 23.sp)
            }
            Text(if (convoy.automatic) "다음 ${convoy.secondsUntilUpdate / 60}:${(convoy.secondsUntilUpdate % 60).toString().padStart(2, '0')}" else "수동 갱신",
                fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text("갱신 ${convoy.revision}회", fontSize = 21.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            FilterChip(selected = convoy.useRealCar, onClick = convoy.toggleRealCar,
                enabled = enabled && live.location.status == ReadStatus.READY,
                label = { Text("우리 차 내비 위치", fontSize = 22.sp) }, modifier = Modifier.heightIn(min = 72.dp))
            TextButton(onClick = convoy.reset, enabled = enabled, modifier = Modifier.heightIn(min = 72.dp)) {
                Text("시연 처음부터", fontSize = 22.sp)
            }
        }
        Text(if (convoy.useRealCar && live.location.status == ReadStatus.READY)
            "우리 차는 NaviHelper 위치 · 일행 2대는 시뮬레이션"
            else "차량 3대의 이동은 이 기기에서 만든 시뮬레이션입니다. 위치를 서버로 보내지 않습니다.",
            fontSize = 21.sp, lineHeight = 31.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun ConvoyOverviewMap(wide: Boolean) {
    val convoy = LocalConvoy.current
    val live = LocalTripLive.current
    val density = LocalDensity.current
    val dark = isSystemInDarkTheme()
    val ground = if (dark) Color(0xFF293D42) else Color(0xFFE7EEE8)
    val sea = if (dark) Color(0xFF264D66) else Color(0xFFBCDDEB)
    val road = if (dark) Color(0xFF64797E) else Color.White
    val ink = if (dark) Color(0xFFC0D6DB) else Color(0xFF637E80)
    val animated = progressOffsets.mapIndexed { index, offset ->
        val target = (convoy.progress + offset).coerceIn(0.0, 1.0).toFloat()
        animateFloatAsState(targetValue = target, animationSpec = tween(4_000), label = "car-$index").value
    }
    val realPoint = live.location.value?.takeIf { convoy.useRealCar && live.location.status == ReadStatus.READY }
    val cars = animated.mapIndexed { index, progress -> if (index == 0 && realPoint != null) realPoint else routePosition(progress.toDouble()) }
    Surface(color = ground, shape = RoundedCornerShape(22.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(if (wide) 430.dp else 400.dp)
            .semantics { contentDescription = "일행 전체 지도. 갱신 ${convoy.revision}회. " +
                cars.mapIndexed { index, point -> "${convoyNames[index]} ${formatPoint(point)}" }.joinToString(". ") }) {
            val width = with(density) { maxWidth.toPx() }
            val height = with(density) { maxHeight.toPx() }
            val padding = with(density) { (if (wide) 100.dp else 92.dp).toPx() }
            val viewport = MapViewport.fit(demoRoute + cars, width, height, padding)
            fun xy(point: GeoPoint): Offset = viewport.project(point).let { Offset(it.first, it.second) }
            val lifts = listOf(-90.dp, 30.dp, -90.dp)
            val desiredLabels = cars.mapIndexed { index, point ->
                val projected = xy(point)
                with(density) {
                    (projected.x - 75.dp.toPx()) to (projected.y + lifts[index].toPx())
                }
            }
            val labelPositions = with(density) {
                placeCarLabels(desiredLabels, width, height, 150.dp.toPx(), 84.dp.toPx(),
                    12.dp.toPx(), 64.dp.toPx(), 48.dp.toPx()).map { Offset(it.first, it.second) }
            }
            Canvas(Modifier.fillMaxSize()) {
                val coastline = listOf(GeoPoint(38.35, 128.55), GeoPoint(38.05, 128.68),
                    GeoPoint(37.88, 128.81), GeoPoint(37.76, 128.94), GeoPoint(37.50, 129.15), GeoPoint(37.0, 129.45))
                val coast = Path().apply {
                    val first = xy(coastline.first()); moveTo(first.x, first.y)
                    coastline.drop(1).forEach { point -> val p = xy(point); lineTo(p.x, p.y) }
                    lineTo(size.width + 100f, size.height + 500f); lineTo(size.width + 100f, -500f); close()
                }
                drawPath(coast, sea)
                listOf(GeoPoint(37.88, 127.90), GeoPoint(37.47, 128.10), GeoPoint(37.88, 128.50)).forEach { point ->
                    drawOval(Color(0xFF7CA88B).copy(alpha = if (dark) .14f else .13f), xy(point) - Offset(80.dp.toPx(), 30.dp.toPx()),
                        Size(180.dp.toPx(), 85.dp.toPx()))
                }
                val river = Path().apply {
                    val a = xy(GeoPoint(37.45, 126.8)); val b = xy(GeoPoint(37.62, 127.25)); val c = xy(GeoPoint(37.72, 127.62))
                    moveTo(a.x, a.y); quadraticBezierTo(b.x, b.y + 40.dp.toPx(), c.x, c.y)
                }
                drawPath(river, sea, style = Stroke(13.dp.toPx(), cap = StrokeCap.Round))
                val route = Path().apply {
                    demoRoute.forEachIndexed { index, point -> val p = xy(point)
                        if (index == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
                }
                drawPath(route, road, style = Stroke(17.dp.toPx(), cap = StrokeCap.Round))
                drawPath(route, Color(0xFFCE9E76).copy(alpha = .7f), style = Stroke(9.dp.toPx(), cap = StrokeCap.Round))
                drawPath(route, ConceptOrange, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink.toArgb(); textSize = 20.sp.toPx() }
                listOf("서울" to demoRoute.first(), "가평" to GeoPoint(37.76, 127.5),
                    "홍천" to GeoPoint(37.70, 127.95), "평창" to GeoPoint(37.63, 128.42)).forEach { (name, point) ->
                    val p = xy(point)
                    drawContext.canvas.nativeCanvas.drawText(name, p.x - 15.dp.toPx(), p.y + 40.dp.toPx(), paint)
                }
                val end = xy(demoDestination)
                drawCircle(Color.White, 14.dp.toPx(), end)
                drawCircle(ConceptOrange, 9.dp.toPx(), end)
                paint.isFakeBoldText = true
                drawContext.canvas.nativeCanvas.drawText("강릉 · 목적지", (end.x - 52.dp.toPx()).coerceAtLeast(12.dp.toPx()), end.y + 44.dp.toPx(), paint)
                cars.forEachIndexed { index, point ->
                    val anchor = xy(point)
                    val label = labelPositions[index] + Offset(75.dp.toPx(), if (index == 1) 0f else 72.dp.toPx())
                    drawLine(convoyColors[index].copy(alpha = .65f), anchor, label, 2.dp.toPx())
                    drawCircle(Color.White.copy(alpha = .65f), 22.dp.toPx(), anchor)
                    val behind = xy(routePosition((animated[index] - .005f).coerceAtLeast(0f).toDouble()))
                    val ahead = xy(routePosition((animated[index] + .005f).coerceAtMost(1f).toDouble()))
                    val heading = (atan2(ahead.y - behind.y, ahead.x - behind.x) * 180 / PI + 90).toFloat()
                    rotate(heading, anchor) {
                        drawRoundRect(convoyColors[index], anchor - Offset(10.dp.toPx(), 19.dp.toPx()), Size(20.dp.toPx(), 38.dp.toPx()), CornerRadius(6.dp.toPx()))
                        drawRoundRect(Color.White.copy(alpha = .9f), anchor - Offset(7.dp.toPx(), 10.dp.toPx()), Size(14.dp.toPx(), 9.dp.toPx()), CornerRadius(2.dp.toPx()))
                    }
                }
            }
            cars.forEachIndexed { index, _ ->
                val real = index == 0 && realPoint != null
                Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(2.dp, convoyColors[index]), shadowElevation = 4.dp,
                    modifier = Modifier.width(150.dp).offset { IntOffset(labelPositions[index].x.roundToInt(), labelPositions[index].y.roundToInt()) }) {
                    Column(Modifier.padding(horizontal = 15.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(convoyNames[index], fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        Text(if (real) "내비 위치" else "시뮬레이션", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Box(Modifier.align(Alignment.TopStart).padding(16.dp)) { AppBadge("일행 3대 · 전체 보기") }
            Text("개발용 개략 지도 · 실제 도로 안내 아님", Modifier.align(Alignment.BottomStart).padding(16.dp),
                fontSize = 20.sp, color = ink)
        }
    }
}
