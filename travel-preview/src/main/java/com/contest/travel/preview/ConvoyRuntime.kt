package com.contest.travel.preview

import androidx.compose.runtime.*

internal val LocalTripLive = staticCompositionLocalOf { LiveTripState() }
internal val LocalTripActions = staticCompositionLocalOf { TripActions() }
internal val LocalConvoy = staticCompositionLocalOf<ConvoyController> { error("Convoy state is not provided") }

internal data class ConvoyController(
    val progress: Double,
    val distanceMeters: Double,
    val dayeonRouteMeters: Double,
    val munjunRouteMeters: Double,
    val speedKph: Float,
    val motionError: String?,
    val revision: Int,
    val useRealCar: Boolean,
    val reset: () -> Unit
)

/** The demo route advances only when the vehicle speed signal reports movement. */
@Composable
internal fun rememberConvoyController(motion: VehicleMotionState): ConvoyController {
    var startMeters by remember { mutableDoubleStateOf(motion.meters) }
    var revision by remember { mutableIntStateOf(0) }
    val distance = (motion.meters - startMeters).coerceAtLeast(0.0)
    // In the 100 km/h demonstration, the two simulated cars travel at 60/40 km/h.
    // Scaling their distance from the measured own-car distance keeps the spacing stable.
    return ConvoyController(
        progress = .36 + distance / demoRouteLengthMeters,
        distanceMeters = distance,
        dayeonRouteMeters = (demoRouteLengthMeters * .84 + distance * .60)
            .coerceAtMost(demoRouteLengthMeters),
        munjunRouteMeters = (demoRouteLengthMeters * .91 + distance * .40)
            .coerceAtMost(demoRouteLengthMeters),
        speedKph = motion.speedKph,
        motionError = motion.error,
        revision = revision,
        useRealCar = false,
        reset = { startMeters = motion.meters; revision++ }
    )
}
