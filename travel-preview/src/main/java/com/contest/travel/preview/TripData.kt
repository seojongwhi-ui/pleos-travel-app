package com.contest.travel.preview

import java.util.Locale
import kotlin.math.*

internal enum class ReadStatus { LOADING, READY, UNSUPPORTED, EMPTY, ERROR, STOPPED }

internal data class Reading<T>(
    val status: ReadStatus = ReadStatus.LOADING,
    val value: T? = null,
    val message: String = "응답을 기다리고 있어요",
    val receivedAt: Long? = null
)

internal data class GeoPoint(val latitude: Double, val longitude: Double) {
    fun isValid(): Boolean = latitude.isFinite() && longitude.isFinite() &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
        !(latitude == 0.0 && longitude == 0.0)
}

internal data class LiveDestination(val name: String, val point: GeoPoint, val meters: Int, val seconds: Int)

internal data class LiveTripState(
    val vehicleStatus: String = "차량 서비스 연결 대기",
    val minutes: Reading<Int> = Reading(),
    val meters: Reading<Float> = Reading(),
    val battery: Reading<Int> = Reading(),
    val fuel: Reading<Int> = Reading(),
    val naviConnected: Boolean = false,
    val naviStatus: String = "내비게이션 연결 대기",
    val location: Reading<GeoPoint> = Reading(),
    val destination: Reading<LiveDestination> = Reading(),
    val waypoints: Reading<List<String>> = Reading(),
    val routePending: Boolean = false,
    val routeMessage: String? = null
)

internal data class TripActions(
    val refresh: () -> Unit = {},
    val requestDestination: () -> Unit = {},
    val addRestStop: () -> Unit = {}
)

// Approximate Gapyeong valley area for the local demonstration. A real route
// must come from the selected destination and the navigation/Directions SDK.
internal val demoDestination = GeoPoint(37.840, 127.525)
internal val demoRestStop = GeoPoint(37.760, 127.500)

// When real location is selected, a failed read must never become a simulated car.
internal fun convoyMapPositions(progress: Double, useRealCar: Boolean, location: Reading<GeoPoint>): List<GeoPoint?> =
    listOf(0.0, -.12, .14).mapIndexed { index, offset ->
        if (index == 0 && useRealCar) location.value?.takeIf { location.status == ReadStatus.READY && it.isValid() }
        else routePosition(progress + offset)
    }

// These are local demonstration points, not a navigation service's road geometry.
internal val demoRoute = listOf(
    GeoPoint(37.5665, 126.978), GeoPoint(37.600, 127.100),
    GeoPoint(37.650, 127.300), GeoPoint(37.760, 127.500),
    demoDestination
)

internal fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
    val lat = Math.toRadians(b.latitude - a.latitude)
    val lon = Math.toRadians(b.longitude - a.longitude)
    val h = sin(lat / 2).pow(2) + cos(Math.toRadians(a.latitude)) *
        cos(Math.toRadians(b.latitude)) * sin(lon / 2).pow(2)
    return 6_371_000.0 * 2 * asin(sqrt(h.coerceIn(0.0, 1.0)))
}

internal fun routePosition(progress: Double): GeoPoint {
    val lengths = demoRoute.zipWithNext().map { (a, b) -> distanceMeters(a, b) }
    var remaining = lengths.sum() * progress.coerceIn(0.0, 1.0)
    lengths.forEachIndexed { index, length ->
        if (remaining <= length) {
            val fraction = if (length > 0.0) remaining / length else 0.0
            val a = demoRoute[index]
            val b = demoRoute[index + 1]
            return GeoPoint(a.latitude + (b.latitude - a.latitude) * fraction,
                a.longitude + (b.longitude - a.longitude) * fraction)
        }
        remaining -= length
    }
    return demoRoute.last()
}

internal fun formatMinutes(minutes: Int): String = if (minutes < 60) "${minutes}분" else "${minutes / 60}시간 ${minutes % 60}분"
internal fun formatMeters(meters: Number): String = String.format(Locale.KOREA, "%.1f km", meters.toDouble() / 1000.0)
internal fun formatPoint(point: GeoPoint): String = String.format(Locale.KOREA, "%.4f, %.4f", point.latitude, point.longitude)
