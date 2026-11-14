package com.contest.travel.preview

import androidx.compose.runtime.mutableStateMapOf

internal const val MIN_COMPANION_SCORE = 70

/** Presentation state for companion changes, including the reason and signed delta. */
internal class CompanionScores {
    private val values = mutableStateMapOf("다연 차" to 97, "문준 차" to MIN_COMPANION_SCORE)
    private val penalties = mutableStateMapOf<String, ScorePenalty>()
    private var revision = 0L
    private var convoyRevision: Int? = null
    private val receivedEvents = mutableMapOf<String, Long>()

    fun score(name: String): Int = values.getValue(name)
    fun penalty(name: String): ScorePenalty? = penalties[name]

    fun showConvoyScores(first: Int, second: Int, tripRevision: Int,
        firstEvent: CompanionScoreEvent?, secondEvent: CompanionScoreEvent?) {
        if (convoyRevision != tripRevision) {
            reset(first, second)
            convoyRevision = tripRevision
            listOf("다연 차" to firstEvent, "문준 차" to secondEvent).forEach { (name, event) ->
                if (event != null) receivedEvents[name] = event.id
            }
            return
        }
        listOf(Triple("다연 차", first, firstEvent), Triple("문준 차", second, secondEvent))
            .forEach { (name, score, event) ->
                values[name] = score.coerceIn(MIN_COMPANION_SCORE, 100)
                if (event != null && receivedEvents[name] != event.id) {
                    receivedEvents[name] = event.id
                    val delta = values.getValue(name) -
                        (score - event.delta).coerceIn(MIN_COMPANION_SCORE, 100)
                    if (delta != 0) penalties[name] = ScorePenalty(++revision, event.reason, -delta,
                        values.getValue(name), feedbackNowMillis())
                }
            }
    }

    fun reset(first: Int = 97, second: Int = MIN_COMPANION_SCORE) {
        values["다연 차"] = first.coerceIn(MIN_COMPANION_SCORE, 100)
        values["문준 차"] = second.coerceIn(MIN_COMPANION_SCORE, 100)
        penalties.clear()
        receivedEvents.clear()
        convoyRevision = null
    }

    fun deduct(name: String, points: Int, reason: String): ScorePenalty? {
        val before = values[name] ?: return null
        val amount = points.coerceIn(0, before - MIN_COMPANION_SCORE)
        if (amount == 0) return null
        val event = ScorePenalty(++revision, reason, amount, before - amount, feedbackNowMillis())
        values[name] = event.score
        penalties[name] = event
        return event
    }
}

/** Positive points are a loss; negative points are a gain, matching existing penalty callers. */
internal data class ScorePenalty(val id: Long, val reason: String, val points: Int,
    val score: Int, val createdAtMillis: Long) {
    val gained get() = points < 0
    val signedPoints get() = if (gained) "+${-points}" else "-$points"
}
internal fun isLowDrivingScore(score: Int) = score <= 79
internal fun feedbackNowMillis(): Long = System.nanoTime() / 1_000_000
