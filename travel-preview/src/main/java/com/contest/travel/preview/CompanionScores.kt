package com.contest.travel.preview

import androidx.compose.runtime.mutableStateMapOf

/** Presentation state for received score changes; no vehicle-event detection is implied. */
internal class CompanionScores {
    private val values = mutableStateMapOf("다연 차" to 97, "문준 차" to 64)
    private val penalties = mutableStateMapOf<String, ScorePenalty>()
    private var revision = 0L

    fun score(name: String): Int = values.getValue(name)
    fun penalty(name: String): ScorePenalty? = penalties[name]

    /** Keep the existing convoy simulation as the source for normal trips. */
    fun showConvoyScores(first: Int, second: Int) {
        values["다연 차"] = first.coerceIn(0, 100)
        values["문준 차"] = second.coerceIn(0, 100)
        penalties.clear()
    }

    fun reset(first: Int = 97, second: Int = 64) {
        values["다연 차"] = first.coerceIn(0, 100)
        values["문준 차"] = second.coerceIn(0, 100)
        penalties.clear()
    }

    fun deduct(name: String, points: Int, reason: String): ScorePenalty? {
        val before = values[name] ?: return null
        val amount = points.coerceIn(0, before)
        if (amount == 0) return null
        val event = ScorePenalty(++revision, reason, amount, before - amount, feedbackNowMillis())
        values[name] = event.score
        penalties[name] = event
        return event
    }
}

internal data class ScorePenalty(val id: Long, val reason: String, val points: Int,
    val score: Int, val createdAtMillis: Long)

internal fun feedbackNowMillis(): Long = System.nanoTime() / 1_000_000
