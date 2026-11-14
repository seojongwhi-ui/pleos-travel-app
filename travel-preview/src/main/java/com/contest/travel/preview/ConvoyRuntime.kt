package com.contest.travel.preview

import androidx.compose.runtime.*
import kotlin.random.Random

internal val LocalTripLive = staticCompositionLocalOf { LiveTripState() }
internal val LocalVehicleMotion = staticCompositionLocalOf { VehicleMotionState() }
internal val LocalTripActions = staticCompositionLocalOf { TripActions() }
internal val LocalConvoy = staticCompositionLocalOf<ConvoyController> { error("Convoy state is not provided") }

internal data class ConvoyController(
    val progress: Double,
    val distanceMeters: Double,
    val dayeonRouteMeters: Double,
    val munjunRouteMeters: Double,
    val dayeonScore: Int,
    val munjunScore: Int,
    val speedKph: Float,
    val motionError: String?,
    val revision: Int,
    val useRealCar: Boolean,
    val reset: () -> Unit
)

/** Local demo scores for the other cars; events are driven by distance, never by a timer. */
internal class CompanionScoreTracker(private val random: Random = Random.Default) {
    private val scores = intArrayOf(97, 64)
    private val nextEventMeters = DoubleArray(2) { 70.0 + random.nextInt(50) }
    private val changes = intArrayOf(-5, -3, -2, -1, 1, 2, 3)

    fun advance(distanceMeters: Double): Pair<Int, Int> {
        if (distanceMeters > 0.0) {
            for (index in scores.indices) {
                while (distanceMeters >= nextEventMeters[index]) {
                    scores[index] = (scores[index] + changes[random.nextInt(changes.size)])
                        .coerceIn(0, 100)
                    nextEventMeters[index] += 90.0 + random.nextInt(80)
                }
            }
        }
        return scores[0] to scores[1]
    }
}

/** The demo route advances only when the vehicle speed signal reports movement. */
@Composable
internal fun rememberConvoyController(motion: VehicleMotionState): ConvoyController {
    var startMeters by remember { mutableDoubleStateOf(motion.meters) }
    var revision by remember { mutableIntStateOf(0) }
    val distance = (motion.meters - startMeters).coerceAtLeast(0.0)
    val scoreTracker = remember(revision) { CompanionScoreTracker() }
    var companionScores by remember(revision) { mutableStateOf(97 to 64) }
    LaunchedEffect(distance, revision) {
        companionScores = scoreTracker.advance(distance)
    }
    // In the 100 km/h demonstration, the two simulated cars travel at 60/40 km/h.
    // Scaling their distance from the measured own-car distance keeps the spacing stable.
    return ConvoyController(
        progress = .36 + distance / demoRouteLengthMeters,
        distanceMeters = distance,
        dayeonRouteMeters = (demoRouteLengthMeters * .84 + distance * .60)
            .coerceAtMost(demoRouteLengthMeters),
        munjunRouteMeters = (demoRouteLengthMeters * .91 + distance * .40)
            .coerceAtMost(demoRouteLengthMeters),
        dayeonScore = companionScores.first,
        munjunScore = companionScores.second,
        speedKph = motion.speedKph,
        motionError = motion.error,
        revision = revision,
        useRealCar = false,
        reset = { startMeters = motion.meters; revision++ }
    )
}
