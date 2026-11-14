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
    val dayeonEvent: CompanionScoreEvent? = null,
    val munjunEvent: CompanionScoreEvent? = null,
    val motionError: String?,
    val revision: Int,
    val useRealCar: Boolean,
    val reset: () -> Unit
)

internal data class CompanionScoreEvent(val id: Long, val reason: String, val delta: Int)

/** Peer-car demo events. Real travelled distance advances the scenario, not a wall clock. */
internal class CompanionScoreTracker(private val random: Random = Random.Default) {
    private val scores = intArrayOf(97, MIN_COMPANION_SCORE)
    private val nextEventMeters = DoubleArray(2) { 180.0 + it * 100 + random.nextInt(100) }
    private val events = arrayOfNulls<CompanionScoreEvent>(2)
    private var eventId = 0L
    private val changes = listOf("급가속" to -3, "급제동" to -3, "차선 이탈" to -2,
        "과속" to -5, "안전 운전" to 2, "안전 운전" to 2, "안전 운전" to 3,
        "안전 운전" to 3, "안전 운전" to 2)

    fun event(index: Int) = events[index]
    fun advance(distanceMeters: Double): Pair<Int, Int> {
        if (!distanceMeters.isFinite() || distanceMeters <= 0.0) return scores[0] to scores[1]
        for (index in scores.indices) {
            while (distanceMeters >= nextEventMeters[index]) {
                val (reason, delta) = changes[random.nextInt(changes.size)]
                val before = scores[index]
                scores[index] = (before + delta).coerceIn(MIN_COMPANION_SCORE, 100)
                if (scores[index] != before)
                    events[index] = CompanionScoreEvent(++eventId, reason, scores[index] - before)
                nextEventMeters[index] += 450.0 + random.nextInt(350)
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
    var companionScores by remember(revision) { mutableStateOf(scoreTracker.advance(0.0)) }
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
        dayeonEvent = scoreTracker.event(0),
        munjunEvent = scoreTracker.event(1),
        motionError = motion.error,
        revision = revision,
        useRealCar = false,
        reset = { startMeters = motion.meters; revision++ }
    )
}

/** Munjun's local demo car stops when the driving console stops; Dayeon stays in motion. */
internal fun companionIsDriving(name: String, demoSpeedKph: Float, snapshot: Boolean,
    waitingForKnock: Boolean = false): Boolean = name == "다연 차" ||
    (if (snapshot) waitingForKnock else demoSpeedKph.isFinite() && demoSpeedKph > .5f)
