package com.contest.travel.preview

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.view.View
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.NaverMapSdk
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.overlay.PathOverlay
import com.naver.maps.map.util.MarkerIcons
import kotlinx.coroutines.delay

internal val convoyColors = listOf(Pleos215.Positive, Pleos215.Gray400, Pleos215.Filled)
internal val convoyNames = listOf("내 차", "다연 차", "문준 차")

/** NAVER basemap with local demo overlays; this does not request a Directions route. */
@Composable
internal fun RoadConvoyMap(overview: Boolean, cameraRevision: Int, enabled: Boolean,
    modifier: Modifier, voiceMarkerVisible: Boolean = false) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val convoy = LocalConvoy.current
    val live = LocalTripLive.current
    val hasClientId = remember(context) {
        runCatching {
            context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
                .metaData?.getString("com.naver.maps.map.NCP_KEY_ID")?.isNotBlank() == true
        }.getOrDefault(false)
    }
    var status by remember { mutableStateOf("네이버 지도 연결 중…") }
    var failed by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    val real = live.location.value?.takeIf { convoy.useRealCar && live.location.status == ReadStatus.READY }
    val cars = convoyMapPositions(convoy, live.location)

    if (!hasClientId) {
        Column(modifier.padding(20.dp)) {
            Text("네이버 지도 Client ID가 없습니다.", color = MaterialTheme.colorScheme.error)
            Text("프로젝트의 local.properties에 NAVER_MAP_CLIENT_ID를 입력하고 앱을 다시 빌드해 주세요.",
                style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val bridge = remember(context, retry) { NaverRoadMapBridge(context) }
    DisposableEffect(bridge, owner) {
        loaded = false
        failed = false
        status = "네이버 지도 연결 중…"
        bridge.start(
            onReady = {
                if (!failed) {
                    loaded = true
                    status = "네이버 지도 · 차량 위치와 경로는 시연 데이터"
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
            status = "네이버 지도 응답 시간이 초과됐어요. 네트워크를 확인하고 다시 시도해 주세요."
        }
    }
    LaunchedEffect(loaded, cars, enabled, voiceMarkerVisible) {
        if (loaded) bridge.update(cars, enabled, voiceMarkerVisible)
    }
    // Reframe for explicit camera requests and new trip data, not every position animation frame.
    LaunchedEffect(loaded, overview, cameraRevision, convoy.revision, convoy.useRealCar, real) {
        if (loaded) bridge.camera(overview, cars)
    }

    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(status, style = MaterialTheme.typography.labelSmall,
                color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            if (failed) {
                Text("다시 시도", Modifier.padding(start = 12.dp).then(
                    Modifier.clickable(enabled) { retry++ }),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
        AndroidView(factory = { bridge.view }, modifier = Modifier.weight(1f).fillMaxWidth(),
            update = {
                it.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                it.contentDescription = "링카 네이버 지도. " +
                    cars.mapIndexed { i, p -> "${convoyNames[i]} ${p?.let(::formatPoint) ?: "위치 없음"}" }
                        .joinToString(". ")
                bridge.setGesturesEnabled(enabled)
            })
    }
}

private class NaverRoadMapBridge(context: android.content.Context) {
    private val appContext = context
    val view = MapView(context)
    private val sdk = NaverMapSdk.getInstance(context)
    private var authListener: NaverMapSdk.OnAuthFailedListener? = null
    private var map: NaverMap? = null
    private var path: PathOverlay? = null
    private var destination: Marker? = null
    private var voiceMarker: Marker? = null
    private val carMarkers = mutableListOf<Marker>()
    private var started = false
    private var resumed = false
    private var destroyed = false
    private var latestCamera: Pair<Boolean, List<GeoPoint?>>? = null

    init {
        view.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
                latestCamera?.let { camera(it.first, it.second) }
            }
        }
    }

    fun start(onReady: () -> Unit, onError: (String) -> Unit) {
        authListener = NaverMapSdk.OnAuthFailedListener { exception ->
            if (!destroyed) {
                val detail = when (exception.errorCode) {
                    "401" -> "Client ID와 네이버 클라우드에 등록한 패키지명을 확인해 주세요."
                    "429" -> "Dynamic Map 사용 설정 또는 이용 한도를 확인해 주세요."
                    "800" -> "Client ID가 앱에 전달되지 않았어요."
                    else -> "인증 설정을 확인해 주세요."
                }
                onError("네이버 지도 인증 실패 (${exception.errorCode}) · $detail")
            }
        }
        sdk.onAuthFailedListener = authListener
        runCatching {
            view.onCreate(null)
            view.getMapAsync { result ->
                if (destroyed) return@getMapAsync
                runCatching {
                    map = result
                    result.mapType = NaverMap.MapType.Basic
                    result.uiSettings.apply {
                        isCompassEnabled = true
                        isRotateGesturesEnabled = false
                        isTiltGesturesEnabled = false
                    }
                    installOverlays(result)
                    result.addOnLoadListener { if (!destroyed) onReady() }
                }.onFailure { onError("네이버 지도 표시 실패 · ${it.javaClass.simpleName}") }
            }
        }.onFailure { onError("네이버 지도 시작 실패 · ${it.javaClass.simpleName}") }
    }

    private fun installOverlays(naverMap: NaverMap) {
        path = PathOverlay(demoRoute.map(GeoPoint::latLng)).apply {
            color = Pleos215.Positive.toArgb()
            width = 7
            outlineWidth = 0
            map = naverMap
        }
        destination = Marker().apply {
            position = demoDestination.latLng()
            icon = MarkerIcons.BLACK
            iconTintColor = Pleos215.Neutral.toArgb()
            captionText = "가평 계곡 · 목적지"
            map = naverMap
        }
        voiceMarker = Marker().apply {
            position = routePositionAtMeters(demoRouteLengthMeters * .36 + 200.0).latLng()
            icon = voiceMarkerImage(appContext)
            width = 45
            height = 56
            captionText = "200m 앞 · 길에 남긴 음성"
            captionColor = Pleos215.Neutral.toArgb()
            captionHaloColor = android.graphics.Color.WHITE
            isVisible = false
            map = naverMap
        }
        convoyNames.forEachIndexed { index, name ->
            carMarkers += Marker().apply {
                position = routePositionAtMeters(demoRouteLengthMeters *
                    listOf(.36, .84, .91)[index]).latLng()
                icon = OverlayImage.fromResource(when (index) {
                    0 -> R.drawable.flow_imgprofilephoto
                    1 -> R.drawable.flow_imgprofilephoto2
                    else -> R.drawable.flow_imgprofilephoto1
                })
                width = 78
                height = 78
                captionText = name
                captionColor = Pleos215.Neutral.toArgb()
                captionHaloColor = android.graphics.Color.WHITE
                isForceShowIcon = true
                map = naverMap
            }
        }
    }

    fun update(cars: List<GeoPoint?>, enabled: Boolean, voiceMarkerVisible: Boolean) {
        if (destroyed) return
        voiceMarker?.isVisible = voiceMarkerVisible
        cars.forEachIndexed { i, point ->
            carMarkers.getOrNull(i)?.apply {
                isVisible = point != null
                if (point != null) position = point.latLng()
            }
        }
        setGesturesEnabled(enabled)
    }

    fun setGesturesEnabled(enabled: Boolean) {
        map?.uiSettings?.setAllGesturesEnabled(enabled)
    }

    fun camera(overview: Boolean, cars: List<GeoPoint?>) {
        if (destroyed) return
        latestCamera = overview to cars
        val naverMap = map ?: return
        if (view.width <= 0 || view.height <= 0) return
        val update = if (overview) {
            val bounds = LatLngBounds.from((demoRoute + cars.filterNotNull()).map(GeoPoint::latLng))
            CameraUpdate.fitBounds(bounds, 90)
        } else {
            val position = cars.firstOrNull() ?: return
            CameraUpdate.toCameraPosition(CameraPosition(position.latLng(), 10.5))
        }
        naverMap.moveCamera(update)
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
        path?.map = null
        destination?.map = null
        voiceMarker?.map = null
        carMarkers.forEach { it.map = null }
        if (sdk.onAuthFailedListener === authListener) sdk.onAuthFailedListener = null
        view.onDestroy()
    }
}

private fun GeoPoint.latLng(): LatLng = LatLng(latitude, longitude)

private fun voiceMarkerImage(context: android.content.Context): OverlayImage {
    val bitmap = Bitmap.createBitmap(45, 56, Bitmap.Config.ARGB_8888)
    val pin = requireNotNull(context.getDrawable(R.drawable.drive_voice_map_pin))
    pin.setBounds(0, 0, 45, 56)
    pin.draw(AndroidCanvas(bitmap))
    val mic = requireNotNull(context.getDrawable(R.drawable.drive_voice_map_mic))
    mic.setBounds(11, 11, 35, 35)
    mic.draw(AndroidCanvas(bitmap))
    return OverlayImage.fromBitmap(bitmap)
}
