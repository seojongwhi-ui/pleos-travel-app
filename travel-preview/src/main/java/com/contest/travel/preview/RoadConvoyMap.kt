package com.contest.travel.preview

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/** A road map renderer, not a route calculation or turn-by-turn navigation engine. */
@Composable
internal fun RoadConvoyMap(overview: Boolean, cameraRevision: Int, enabled: Boolean, modifier: Modifier) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val convoy = LocalConvoy.current
    val live = LocalTripLive.current
    var status by remember { mutableStateOf("도로 지도를 불러오는 중") }
    var failed by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var showCredits by remember { mutableStateOf(false) }
    var creditLinkError by remember { mutableStateOf<String?>(null) }
    val uriHandler = LocalUriHandler.current
    val bridge = remember(context) {
        MapLibre.getInstance(context)
        RoadMapBridge(MapView(context, MapLibreMapOptions.createFromAttributes(context)
            .textureMode(true).localIdeographFontFamilyEnabled(true).localIdeographFontFamily("sans-serif")))
    }
    val progress = animateFloatAsState(convoy.progress.toFloat(), tween(4_000), label = "road-car-progress").value
    val real = live.location.value?.takeIf { convoy.useRealCar && live.location.status == ReadStatus.READY }
    val cars = convoyMapPositions(progress.toDouble(), convoy.useRealCar, live.location)

    DisposableEffect(bridge, owner) {
        val observer = LifecycleEventObserver { _, event -> bridge.lifecycle(event) }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            bridge.destroy()
        }
    }
    LaunchedEffect(bridge, retry) {
        loaded = false; failed = false; status = "도로 지도를 불러오는 중"
        bridge.load(onReady = { loaded = true; failed = false; status = "도로 지도 연결됨" },
            onError = { failed = true; status = "지도 다운로드 실패 · 네트워크를 확인해 주세요" })
        delay(25_000)
        if (!loaded && !failed) {
            failed = true; status = "지도 응답 시간 초과 · 다시 불러와 주세요"
        }
    }
    LaunchedEffect(loaded, cars, enabled) {
        if (loaded) bridge.update(cars, enabled)
    }
    // Reframe on explicit controls or a convoy update, not on every animation frame.
    LaunchedEffect(loaded, overview, cameraRevision, convoy.revision, convoy.useRealCar, real) {
        if (loaded) bridge.camera(overview, cars)
    }
    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(status, fontSize = 18.sp,
                color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            if (failed) TextButton(onClick = { retry++ }, enabled = enabled) { Text("지도 다시 불러오기", fontSize = 18.sp) }
        }
        AndroidView(factory = { bridge.view }, modifier = Modifier.weight(1f).fillMaxWidth(),
            update = {
                it.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                it.contentDescription = "같이가 도로 지도. 갱신 ${convoy.revision}회. " +
                    cars.mapIndexed { i, p -> "${convoyNames[i]} ${p?.let(::formatPoint) ?: "위치 없음"}" }.joinToString(". ")
                bridge.map?.uiSettings?.setAllGesturesEnabled(enabled)
            })
        TextButton(onClick = { creditLinkError = null; showCredits = true }, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            contentPadding = PaddingValues(6.dp)) {
            Text("OpenFreeMap · © OpenMapTiles · © OpenStreetMap contributors", fontSize = 13.sp)
        }
    }
    if (showCredits) AlertDialog(onDismissRequest = { showCredits = false },
        title = { Text("지도 출처") },
        text = {
            Column {
                listOf("OpenFreeMap" to "https://openfreemap.org/", "© OpenMapTiles" to "https://openmaptiles.org/",
                    "© OpenStreetMap contributors" to "https://www.openstreetmap.org/copyright").forEach { (name, url) ->
                    TextButton(onClick = {
                        runCatching { uriHandler.openUri(url) }.onFailure {
                            creditLinkError = "웹 링크를 열 수 있는 앱이 없습니다.\n$url"
                        }
                    }, enabled = enabled, modifier = Modifier.heightIn(min = 76.dp)) {
                        Text(name, fontSize = 22.sp)
                    }
                }
                creditLinkError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 18.sp) }
            }
        }, confirmButton = { TextButton(onClick = { showCredits = false }) { Text("닫기", fontSize = 22.sp) } })
}

private class RoadMapBridge(val view: MapView) {
    var map: MapLibreMap? = null
        private set
    private var style: Style? = null
    private var started = false
    private var resumed = false
    private var destroyed = false
    private var loadGeneration = 0
    private var onFailure: (() -> Unit)? = null
    private var latestCamera: Pair<Boolean, List<GeoPoint?>>? = null

    init {
        view.onCreate(null)
        view.addOnDidFailLoadingMapListener { if (!destroyed) onFailure?.invoke() }
        view.addOnLayoutChangeListener { _, l, t, r, b, oldL, oldT, oldR, oldB ->
            if (r - l != oldR - oldL || b - t != oldB - oldT) latestCamera?.let { camera(it.first, it.second) }
        }
    }

    fun load(onReady: () -> Unit, onError: () -> Unit) {
        val generation = ++loadGeneration
        onFailure = onError
        view.getMapAsync { result ->
            if (destroyed || generation != loadGeneration) return@getMapAsync
            map = result
            result.uiSettings.apply {
                isCompassEnabled = true
                isLogoEnabled = false
                isAttributionEnabled = true
                isRotateGesturesEnabled = false
                isTiltGesturesEnabled = false
            }
            result.setStyle("https://tiles.openfreemap.org/styles/liberty") { loaded ->
                if (!destroyed && generation == loadGeneration) {
                    runCatching { installLayers(loaded) }.onSuccess {
                        style = loaded
                        onReady()
                    }.onFailure { onError() }
                }
            }
        }
    }

    private fun installLayers(style: Style) {
        style.addSource(GeoJsonSource("gatiga-demo-line", LineString.fromLngLats(demoRoute.map { it.point() })))
        style.addLayer(LineLayer("gatiga-demo-line", "gatiga-demo-line").withProperties(
            lineColor(0xFF245BB0.toInt()), lineWidth(4f), lineDasharray(arrayOf(2f, 2f))))
        style.addSource(GeoJsonSource("gatiga-destination", demoDestination.point()))
        style.addLayer(CircleLayer("gatiga-destination", "gatiga-destination").withProperties(
            circleColor(0xFF172F46.toInt()), circleRadius(10f), circleStrokeColor(-1), circleStrokeWidth(3f)))
        style.addImage("gatiga-destination-label", labelBitmap("강릉 · 목적지", 0xFF172F46.toInt()))
        style.addLayer(SymbolLayer("gatiga-destination-label", "gatiga-destination").withProperties(
            iconImage("gatiga-destination-label"), iconAllowOverlap(true), iconAnchor("top"), iconOffset(arrayOf(0f, 18f))))
        convoyNames.forEachIndexed { index, name ->
            val id = "gatiga-car-$index"
            style.addImage(id, labelBitmap(name, convoyColors[index].toArgb()))
            style.addSource(GeoJsonSource(id, routePosition(.38 + listOf(0.0, -.12, .14)[index]).point()))
            style.addLayer(CircleLayer("$id-dot", id).withProperties(circleColor(convoyColors[index].toArgb()),
                circleRadius(9f), circleStrokeColor(-1), circleStrokeWidth(3f)))
            // Stagger labels so all names remain visible when vehicles converge at the destination.
            val labelOffset = listOf(arrayOf(0f, -18f), arrayOf(-90f, 50f), arrayOf(90f, 50f))[index]
            style.addLayer(SymbolLayer("$id-label", id).withProperties(iconImage(id), iconAllowOverlap(true),
                iconIgnorePlacement(true), iconAnchor("bottom"), iconOffset(labelOffset)))
        }
    }

    fun update(cars: List<GeoPoint?>, enabled: Boolean) {
        if (destroyed) return
        cars.forEachIndexed { i, point ->
            val visibility = visibility(if (point == null) "none" else "visible")
            style?.getLayer("gatiga-car-$i-dot")?.setProperties(visibility)
            style?.getLayer("gatiga-car-$i-label")?.setProperties(visibility)
            if (point != null) style?.getSourceAs<GeoJsonSource>("gatiga-car-$i")?.setGeoJson(point.point())
        }
        map?.uiSettings?.setAllGesturesEnabled(enabled)
    }

    fun camera(overview: Boolean, cars: List<GeoPoint?>) {
        if (destroyed) return
        latestCamera = overview to cars
        if (view.width <= 0 || view.height <= 0) return
        val update = if (overview) {
            val bounds = LatLngBounds.Builder().includes((demoRoute + cars.filterNotNull()).map { it.latLng() }).build()
            CameraUpdateFactory.newLatLngBounds(bounds, 100, 100, 100, 100)
        } else CameraUpdateFactory.newLatLngZoom((cars.first() ?: return).latLng(), 10.5)
        map?.animateCamera(update, 700)
    }

    fun lifecycle(event: Lifecycle.Event) {
        if (destroyed) return
        when (event) {
            Lifecycle.Event.ON_START -> if (!started) { view.onStart(); started = true }
            Lifecycle.Event.ON_RESUME -> if (!resumed) { view.onResume(); resumed = true }
            Lifecycle.Event.ON_PAUSE -> if (resumed) { view.onPause(); resumed = false }
            Lifecycle.Event.ON_STOP -> if (started) { view.onStop(); started = false }
            else -> Unit
        }
    }

    fun destroy() {
        if (destroyed) return
        if (resumed) view.onPause()
        if (started) view.onStop()
        destroyed = true; loadGeneration++; onFailure = null; style = null; map = null
        view.onDestroy()
    }
}

private fun GeoPoint.point(): Point = Point.fromLngLat(longitude, latitude)
private fun GeoPoint.latLng(): LatLng = LatLng(latitude, longitude)

private fun labelBitmap(name: String, color: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(164, 54, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = color
    canvas.drawRoundRect(1f, 1f, 163f, 53f, 16f, 16f, paint)
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 24f; paint.textAlign = Paint.Align.CENTER
    paint.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
    canvas.drawText(name, 82f, 35f, paint)
    return bitmap
}
