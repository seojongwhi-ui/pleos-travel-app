package com.contest.travel.preview

import kotlin.math.abs

/** Configurable test rules, not a certified driving assessment or a road speed-limit service. */
internal data class DrivingSimulationRules(
    val accelerationMps2: Double = 2.5,
    val brakingMps2: Double = 3.0,
    val maneuverHoldMillis: Long = 400,
    val quietRearmMillis: Long = 600,
    val repeatCooldownMillis: Long = 3000,
    val speedLimitKph: Double = 80.0,
    val speedingHoldMillis: Long = 2000,
    val maxSampleGapMillis: Long = 1500
)

internal data class DetectedDrivingPenalty(val reason: String, val points: Int)

internal class DrivingPenaltyDetector(private val rules: DrivingSimulationRules = DrivingSimulationRules()) {
    private var previous: Pair<Long, Double>? = null
    private var candidate: String? = null
    private var candidateSince = 0L
    private var latched: String? = null
    private var quietSince: Long? = null
    private val emittedAt = mutableMapOf<String, Long>()
    private var speedingSince: Long? = null
    private var speedingLatched = false

    fun reset() {
        previous = null; candidate = null; latched = null; quietSince = null
        emittedAt.clear(); speedingSince = null; speedingLatched = false
    }

    fun sample(timeMillis: Long, speedMps: Double, inDrive: Boolean, valid: Boolean): List<DetectedDrivingPenalty> {
        if (!inDrive) { reset(); return emptyList() }
        if (!valid || !speedMps.isFinite() || speedMps < 0 || speedMps > 80) {
            previous = null; candidate = null; quietSince = null; speedingSince = null
            return emptyList()
        }
        val before = previous
        previous = timeMillis to speedMps
        val elapsed = before?.let { timeMillis - it.first }
        if (elapsed != null && (elapsed <= 0 || elapsed > rules.maxSampleGapMillis)) {
            candidate = null; quietSince = null; speedingSince = null
            return emptyList()
        }
        val found = mutableListOf<DetectedDrivingPenalty>()
        if (before != null && elapsed != null) {
            val acceleration = (speedMps - before.second) / (elapsed / 1000.0)
            val kind = when {
                acceleration >= rules.accelerationMps2 -> "급가속"
                acceleration <= -rules.brakingMps2 -> "급제동"
                else -> null
            }
            if (kind != candidate) { candidate = kind; candidateSince = timeMillis }
            if (kind == null) {
                if (abs(acceleration) <= 1.0) {
                    val quiet = quietSince ?: timeMillis.also { quietSince = it }
                    if (timeMillis - quiet >= rules.quietRearmMillis) latched = null
                } else quietSince = null
            } else {
                quietSince = null
                val last = emittedAt[kind]
                if (kind != latched && timeMillis - candidateSince >= rules.maneuverHoldMillis &&
                    (last == null || timeMillis - last >= rules.repeatCooldownMillis)) {
                    found += DetectedDrivingPenalty(kind, 3)
                    emittedAt[kind] = timeMillis; latched = kind
                }
            }
        }
        val speedKph = speedMps * 3.6
        if (speedKph <= rules.speedLimitKph - 2) {
            speedingSince = null; speedingLatched = false
        } else if (speedKph > rules.speedLimitKph && !speedingLatched) {
            val since = speedingSince ?: timeMillis.also { speedingSince = it }
            if (timeMillis - since >= rules.speedingHoldMillis) {
                found += DetectedDrivingPenalty("과속", 5); speedingLatched = true
            }
        } else if (speedKph <= rules.speedLimitKph) speedingSince = null
        return found
    }
}
