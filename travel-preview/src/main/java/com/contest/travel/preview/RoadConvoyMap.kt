package com.contest.travel.preview

import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay

internal val convoyColors = listOf(Pleos215.Positive, Pleos215.Gray400, Pleos215.Filled)
internal val convoyNames = listOf("내 차", "다연 차", "문준 차")

/** MapLibre map-kit style with the user's artwork and road-aligned demo routes. */
@Composable
internal fun RoadConvoyMap(overview: Boolean, cameraRevision: Int, enabled: Boolean,
    modifier: Modifier, voiceMarkerVisible: Boolean = false) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val convoy = LocalConvoy.current
    val live = LocalTripLive.current
    var status by remember { mutableStateOf("링카 지도 연결 중…") }
    var failed by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    val real = live.location.value?.takeIf { convoy.useRealCar && live.location.status == ReadStatus.READY }
    val cars = convoyMapPositions(convoy, live.location)
    val fractions = listOf(convoy.progress.toDouble(), convoy.dayeonRouteMeters / demoRouteLengthMeters,
        convoy.munjunRouteMeters / demoRouteLengthMeters).map { it.coerceIn(0.0, 1.0) }
    val ownMinutes = live.destination.value?.takeIf {
        live.destination.status == ReadStatus.READY
    }?.seconds?.let { (it + 59) / 60 }
    val labels = listOf(
        if (ownMinutes != null) "내 차 · 남은 ${ownMinutes}분" else "내 차 · 시연 40분",
        "다연 차 · 시연 32분", "문준 차 · 시연 25분")
    var routeSource by remember { mutableStateOf("도로 경로 연결 중") }

    val bridge = remember(context, retry) { MapLibreRoadMapBridge(context) }
    DisposableEffect(bridge, owner) {
        loaded = false
        failed = false
        status = "링카 지도 연결 중…"
        bridge.start(
            onReady = {
                if (!failed) {
                    loaded = true
                    status = "링카 지도 · 차량 위치와 경로는 시연 데이터"
                }
            },
            onError = { reason ->
                loaded = false
                failed = true
                status = reason
            }
        )
        val observer = LifecycleEventObserver { _, event -> bridge.lifecycle(event) }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            bridge.destroy()
        }
    }
    LaunchedEffect(bridge) {
        delay(25_000)
        if (!loaded && !failed) {
            failed = true
            status = "링카 지도 응답 시간이 초과됐어요. 네트워크를 확인하고 다시 시도해 주세요."
        }
    }
    LaunchedEffect(bridge) {
        val road = LinkCarRoadRouteRepository.load(context.applicationContext)
        routeSource = road.source
        bridge.setRoutes(road.route, road.friendRoutes)
    }
    LaunchedEffect(loaded, cars, fractions, labels, enabled, voiceMarkerVisible, convoy.distanceMeters) {
        if (loaded) bridge.update(cars, fractions, labels, convoy.useRealCar,
            enabled, voiceMarkerVisible, convoy.distanceMeters)
    }
    // Reframe for explicit camera requests and new trip data, not every position animation frame.
    LaunchedEffect(loaded, overview, cameraRevision, convoy.revision, convoy.useRealCar, real) {
        if (loaded) bridge.camera(overview, cars)
    }

    Box(modifier) {
        AndroidView(factory = { bridge.view }, modifier = Modifier.fillMaxSize(),
            update = {
                it.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                it.contentDescription = "링카 링카 지도. " +
                    cars.mapIndexed { i, p -> "${convoyNames[i]} ${p?.let(::formatPoint) ?: "위치 없음"}" }
                        .joinToString(". ")
                bridge.setGesturesEnabled(enabled)
            })
        // The map reset control is supplied by the parent screen at the top left.
        LinkCarMapLegend(Modifier.align(Alignment.TopStart)
            .padding(start = 156.dp, top = 20.dp))
        if (loaded && !failed) {
            Column(Modifier.align(Alignment.TopEnd).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("+" to 1.0, "−" to -1.0).forEach { (label, amount) ->
                    Surface(Modifier.size(48.dp)
                        .semantics { contentDescription = if (amount > 0) "지도 확대" else "지도 축소" }
                        .clickable(enabled) { bridge.zoomBy(amount) },
                        shape = RoundedCornerShape(12.dp), color = Color.White,
                        shadowElevation = 2.dp) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(label, style = MaterialTheme.typography.headlineMedium,
                                color = Color(LinkCarMapPalette.labelInk))
                        }
                    }
                }
            }
        }
        if (failed || !loaded || routeSource.contains("실패")) {
            Surface(Modifier.align(Alignment.TopEnd).padding(20.dp)
                .then(if (failed) Modifier.clickable(enabled) { retry++ } else Modifier),
                shape = RoundedCornerShape(12.dp), color = Color.White,
                shadowElevation = 2.dp) {
                Text(if (failed) "$status · 다시 시도" else if (!loaded) status else routeSource,
                    Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LinkCarMapLegend(modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(32.dp), color = Color.White,
        shadowElevation = 3.dp) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically) {
            listOf(Triple("내 차", LinkCarMapPalette.ownPassed, false),
                Triple("다연 차", LinkCarMapPalette.dayeonPassed, true),
                Triple("문준 차", LinkCarMapPalette.munjunPassed, true)).forEach { (name, argb, dashed) ->
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Canvas(Modifier.width(32.dp).height(12.dp)) {
                        val tint = Color(argb)
                        if (dashed) {
                            for (left in listOf(0f, 12f, 24f)) {
                                drawLine(tint, Offset(left.dp.toPx(), center.y),
                                    Offset((left + 7f).dp.toPx(), center.y),
                                    strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
                            }
                        } else drawLine(tint, Offset(0f, center.y),
                            Offset(size.width, center.y), strokeWidth = 4.dp.toPx(),
                            cap = StrokeCap.Round)
                    }
                    Text(name, style = MaterialTheme.typography.labelSmall,
                        color = Color(LinkCarMapPalette.labelInk))
                }
            }
        }
    }
}
