package com.contest.travel.preview

import android.content.Context
import android.graphics.Point as ScreenPoint
import android.view.MotionEvent
import androidx.lifecycle.Lifecycle
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/** Uses the supplied MapLibre style rather than a recolored NAVER basemap. */
internal class MapLibreRoadMapBridge(private val context: Context) {
    val view: MapView = MapView(context.also { MapLibre.getInstance(it) })
    private var map: MapLibreMap? = null
    private var style: Style? = null
    private var route = LinkCarMapRoute(demoRoute)
    private var friendRoutes = listOf(LinkCarMapRoute(demoRoute), LinkCarMapRoute(demoRoute))
    private val lineSources = mutableMapOf<String, GeoJsonSource>()
    private val pointSources = mutableMapOf<String, GeoJsonSource>()
    private var lastCars: List<GeoPoint?> = listOf(null, null, null)
    private var lastRawFractions: List<Double> = listOf(.36, .84, .91)
    private var fractions: List<Double> = listOf(.36, .84, .91)
    private var labels = listOf("내 차 · 시연 40분", "다연 차 · 시연 32분", "문준 차 · 시연 25분")
    private var realMode = false
    private var voiceVisible = false
    private var distanceMeters = 0.0
    private var voiceLabelMeters = -1
    private var started = false
    private var resumed = false
    private var destroyed = false
    private var latestCamera: Pair<Boolean, List<GeoPoint?>>? = null
    private var gesturesEnabled = false

    init {
        view.setOnGenericMotionListener { _, event ->
            if (gesturesEnabled && event.actionMasked == MotionEvent.ACTION_SCROLL) {
                val ticks = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                if (ticks != 0f) {
                    map?.moveCamera(CameraUpdateFactory.zoomBy(
                        ticks.toDouble(), ScreenPoint(event.x.toInt(), event.y.toInt())))
                    true
                } else false
            } else false
        }
        view.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop)
                latestCamera?.let { camera(it.first, it.second) }
        }
    }

    fun start(onReady: () -> Unit, onError: (String) -> Unit) {
        runCatching {
            view.onCreate(null)
            view.getMapAsync { readyMap ->
                if (destroyed) return@getMapAsync
                runCatching {
                    map = readyMap
                    readyMap.uiSettings.apply {
                        isCompassEnabled = true
                        isRotateGesturesEnabled = false
                        isTiltGesturesEnabled = false
                    }
                    val json = context.assets.open("lincar-map-style.json")
                        .bufferedReader().use { it.readText() }
                    readyMap.setStyle(Style.Builder().fromJson(json)) { loadedStyle ->
                        if (destroyed) return@setStyle
                        runCatching {
                            style = loadedStyle
                            installLayers(loadedStyle)
                            updateLines()
                            updateMarkers()
                            latestCamera?.let { camera(it.first, it.second) }
                            onReady()
                        }.onFailure { onError("지도 요소 표시 실패 · ${it.javaClass.simpleName}: ${it.message}") }
                    }
                }.onFailure { onError("지도 스타일 불러오기 실패 · ${it.javaClass.simpleName}: ${it.message}") }
            }
        }.onFailure { onError("지도 시작 실패 · ${it.javaClass.simpleName}: ${it.message}") }
    }

    private fun installLayers(loadedStyle: Style) {
        // Remaining routes go underneath passed routes; the own route remains most prominent.
        for ((index, prefix) in listOf("dayeon", "munjun", "own").withIndex()) {
            val tint = when (index) {
                0 -> LinkCarMapPalette.dayeonPassed
                1 -> LinkCarMapPalette.munjunPassed
                else -> LinkCarMapPalette.ownPassed
            }
            val pale = when (index) {
                0 -> LinkCarMapPalette.dayeonRemaining
                1 -> LinkCarMapPalette.munjunRemaining
                else -> LinkCarMapPalette.ownRemaining
            }
            for (part in listOf("remaining", "passed")) {
                val id = "$prefix-$part"
                val source = GeoJsonSource(id, line(route.points))
                loadedStyle.addSource(source)
                lineSources[id] = source
                val color = if (part == "passed") tint else pale
                val width = if (index == 2) 6f else if (index == 0) 5f else 4f
                val layer = LineLayer("$id-line", id).withProperties(
                    PropertyFactory.lineColor(color),
                    PropertyFactory.lineWidth(width),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                    *if (index < 2) arrayOf(PropertyFactory.lineDasharray(arrayOf(2f, 2f)))
                        else emptyArray()
                )
                loadedStyle.addLayer(layer)
            }
        }
        val initial = route.points.first()
        // At overview scale, the voice point is only a few pixels ahead.
        // Keep the car portrait readable; the voice pin separates when zoomed.
        val markers = listOf("destination", "voice", "car-0", "car-1", "car-2")
        markers.forEach { id ->
            val source = GeoJsonSource("$id-point", point(initial))
            loadedStyle.addSource(source)
            pointSources[id] = source
            val art = artwork(id)
            loadedStyle.addImage("$id-art", art.bitmap)
            loadedStyle.addLayer(SymbolLayer("$id-symbol", "$id-point").withProperties(
                PropertyFactory.iconImage("$id-art"),
                PropertyFactory.iconSize(0.5f),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER),
                PropertyFactory.iconOffset(iconOffset(art)),
                PropertyFactory.visibility(if (id == "voice") Property.NONE else Property.VISIBLE)
            ))
        }
    }

    private fun artwork(id: String): LinkCarMapImage = when (id) {
        "destination" -> LinkCarMapArtwork.destination(context, "목적지 · 가평 계곡")
        "voice" -> LinkCarMapArtwork.voice(context, voiceDistance())
        else -> {
            val index = id.last().digitToInt()
            val portraits = listOf(R.drawable.flow_imgprofilephoto,
                R.drawable.flow_imgprofilephoto2, R.drawable.flow_imgprofilephoto1)
            LinkCarMapArtwork.car(context, index == 0, portraits[index], labels[index])
        }
    }

    fun setRoutes(newRoute: LinkCarMapRoute, newFriendRoutes: List<LinkCarMapRoute>) {
        if (destroyed) return
        require(newFriendRoutes.size == 2)
        route = newRoute
        friendRoutes = newFriendRoutes
        fractions = mapFractions(lastRawFractions)
        updateLines()
        updateMarkers()
        latestCamera?.let { camera(it.first, it.second) }
    }

    fun update(cars: List<GeoPoint?>, rawFractions: List<Double>, newLabels: List<String>,
        useRealCar: Boolean, enabled: Boolean, showVoice: Boolean, distance: Double) {
        if (destroyed) return
        lastCars = cars
        lastRawFractions = rawFractions
        realMode = useRealCar
        voiceVisible = showVoice
        distanceMeters = distance
        fractions = mapFractions(rawFractions)
        if (labels != newLabels) {
            labels = newLabels
            listOf("car-0", "car-1", "car-2").forEach { refreshArt(it) }
        }
        updateLines()
        updateMarkers()
        setGesturesEnabled(enabled)
    }

    private fun refreshArt(id: String) {
        val art = artwork(id)
        style?.addImage("$id-art", art.bitmap)
        style?.getLayer("$id-symbol")?.setProperties(PropertyFactory.iconOffset(iconOffset(art)))
    }

    private fun iconOffset(art: LinkCarMapImage): Array<Float> = arrayOf(
        (0.5f - art.anchor.x) * art.width * 2f,
        (0.5f - art.anchor.y) * art.height * 2f
    )

    private fun updateLines() {
        if (style == null) return
        for ((index, prefix) in listOf("dayeon", "munjun", "own").withIndex()) {
            val activeRoute = if (index == 2) route else friendRoutes[index]
            val fraction = fractions[if (index == 2) 0 else index + 1]
            val (past, future) = activeRoute.splitAtFraction(fraction)
            lineSources["$prefix-passed"]?.setGeoJson(line(past))
            lineSources["$prefix-remaining"]?.setGeoJson(line(future))
        }
    }

    private fun mapFractions(raw: List<Double>): List<Double> {
        val starts = listOf(.36, .84, .91)
        return raw.mapIndexed { index, value ->
            if (index == 0 && realMode && lastCars[0] != null) {
                route.fractionAt(lastCars[0]!!)
            } else if (index > 0) {
                val initialRemaining = if (index == 1) 9_700.0 else 5_400.0
                val relativeSpeed = if (index == 1) .60 else .40
                (1.0 - (initialRemaining - distanceMeters * relativeSpeed)
                    .coerceAtLeast(0.0) / friendRoutes[index - 1].lengthMeters)
                    .coerceIn(0.0, 1.0)
            } else {
                (starts[index] + (value - starts[index]) *
                    demoRouteLengthMeters / route.lengthMeters).coerceIn(0.0, 1.0)
            }
        }
    }

    private fun mapCar(index: Int): GeoPoint? {
        val original = lastCars.getOrNull(index) ?: return null
        val activeRoute = if (index == 0) route else friendRoutes[index - 1]
        return if (index == 0 && realMode)
            activeRoute.pointAtFraction(activeRoute.fractionAt(original))
        else activeRoute.pointAtFraction(fractions[index])
    }

    private fun updateMarkers() {
        if (style == null) return
        pointSources["destination"]?.setGeoJson(point(route.points.last()))
        lastCars.indices.forEach { index ->
            val id = "car-$index"
            val vehicle = mapCar(index)
            style?.getLayer("$id-symbol")?.setProperties(
                PropertyFactory.visibility(if (vehicle == null) Property.NONE else Property.VISIBLE))
            if (vehicle != null) pointSources[id]?.setGeoJson(point(vehicle))
        }
        style?.getLayer("voice-symbol")?.setProperties(
            PropertyFactory.visibility(if (voiceVisible) Property.VISIBLE else Property.NONE))
        if (voiceVisible) {
            pointSources["voice"]?.setGeoJson(point(
                route.pointAtMeters(route.lengthMeters * .36 + 200.0)))
            val meters = voiceDistance()
            if (meters != voiceLabelMeters) {
                voiceLabelMeters = meters
                refreshArt("voice")
            }
        }
    }

    private fun voiceDistance() =
        ((200.0 - distanceMeters).coerceAtLeast(0.0) / 10.0).toInt() * 10

    fun setGesturesEnabled(enabled: Boolean) {
        gesturesEnabled = enabled
        map?.uiSettings?.apply {
            setAllGesturesEnabled(enabled)
            isZoomGesturesEnabled = enabled
            isDoubleTapGesturesEnabled = enabled
            isQuickZoomGesturesEnabled = enabled
        }
    }

    fun zoomBy(steps: Double) {
        if (destroyed || !gesturesEnabled) return
        map?.moveCamera(CameraUpdateFactory.zoomBy(steps))
    }

    fun camera(overview: Boolean, cars: List<GeoPoint?>) {
        if (destroyed) return
        latestCamera = overview to cars
        val activeMap = map ?: return
        if (view.width <= 0 || view.height <= 0) return
        val update = if (overview) {
            val points = route.points + friendRoutes.flatMap(LinkCarMapRoute::points) +
                lastCars.indices.mapNotNull(::mapCar)
            val bounds = LatLngBounds.from(points.maxOf(GeoPoint::latitude),
                points.maxOf(GeoPoint::longitude), points.minOf(GeoPoint::latitude),
                points.minOf(GeoPoint::longitude))
            CameraUpdateFactory.newLatLngBounds(bounds, 90, 170, 90, 70)
        } else {
            val vehicle = mapCar(0) ?: cars.firstOrNull() ?: return
            CameraUpdateFactory.newLatLngZoom(vehicle.latLng(), 10.5)
        }
        activeMap.moveCamera(update)
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
        destroyed = true
        view.onDestroy()
    }
}

private fun GeoPoint.latLng() = LatLng(latitude, longitude)
private fun point(point: GeoPoint) = Point.fromLngLat(point.longitude, point.latitude)
private fun line(points: List<GeoPoint>) =
    LineString.fromLngLats(points.map { point(it) })
