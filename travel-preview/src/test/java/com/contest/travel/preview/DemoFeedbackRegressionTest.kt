package com.contest.travel.preview

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DemoFeedbackRegressionTest {
    @Test fun colorThresholdAppliesToEitherCarAt79Not80() {
        assertTrue(isLowDrivingScore(79))
        assertTrue(isLowDrivingScore(0))
        assertFalse(isLowDrivingScore(80))
        assertFalse(isLowDrivingScore(100))
    }

    @Test fun convoyGainsAndLossesKeepTheirReasonAcrossResizingAndDuplicateUpdates() {
        val scores = CompanionScores()
        scores.showConvoyScores(97, 70, 0, null, null)
        val loss = CompanionScoreEvent(1, "차선 이탈", -2)
        scores.showConvoyScores(95, 70, 0, loss, null)
        val badge = scores.penalty("다연 차")!!
        assertEquals("차선 이탈", badge.reason)
        assertEquals("-2", badge.signedPoints)
        scores.showConvoyScores(95, 70, 0, loss, null)
        assertSame(badge, scores.penalty("다연 차"))
        val gain = CompanionScoreEvent(2, "안전 운전", 3)
        scores.showConvoyScores(95, 73, 0, loss, gain)
        assertSame(badge, scores.penalty("다연 차"))
        assertEquals("+3", scores.penalty("문준 차")!!.signedPoints)
        scores.showConvoyScores(97, 70, 1, null, null)
        assertNull(scores.penalty("다연 차"))
        assertNull(scores.penalty("문준 차"))
    }

    @Test fun randomPeerScenarioIncludesLaneDeparturesAndRecoveryWithoutStationaryDrain() {
        val tracker = CompanionScoreTracker(Random(42))
        val start = tracker.advance(0.0)
        repeat(1000) { assertEquals(start, tracker.advance(0.0)) }
        val events = mutableMapOf<Long, CompanionScoreEvent>()
        for (meters in 0..100_000 step 50) {
            val scores = tracker.advance(meters.toDouble())
            assertTrue(scores.first in 70..100 && scores.second in 70..100)
            repeat(2) { i -> tracker.event(i)?.let { events[it.id] = it } }
        }
        assertTrue(events.values.any { it.reason == "차선 이탈" && it.delta < 0 })
        assertTrue(events.values.any { it.delta > 0 })
        assertTrue(events.size < 400)
    }

    @Test fun convoyFeedbackShowsOnlyPointsActuallyLostAbove70() {
        val scores = CompanionScores()
        scores.showConvoyScores(72, 64, 0, null, null)
        assertEquals(70, scores.score("문준 차"))
        scores.showConvoyScores(69, 61, 0,
            CompanionScoreEvent(1, "급가속", -3), CompanionScoreEvent(2, "급가속", -3))
        assertEquals(70, scores.score("다연 차"))
        assertEquals("-2", scores.penalty("다연 차")!!.signedPoints)
        assertNull(scores.penalty("문준 차"))
    }

    @Test fun expiredKnockOpensMessageFallbackAndLateResponseCannotReopenCall() {
        val s = WireSession().apply { sendKnock("문준 차") }
        s.seconds = 0
        s.expirePendingKnock()
        assertTrue(s.knockTimedOut)
        assertEquals(WireScreen.VOICE_LEAVE, s.screen)
        assertEquals(DFeed.UNAVAILABLE, drivingFeed(s, s.screen))
        assertFalse(s.acceptPendingDemoKnock())
        s.beginVoiceRecording(s.knockTarget)
        assertEquals("문준 차", s.recordingTarget)
        assertEquals(DFeed.RECORDING, drivingFeed(s, s.screen))
        s.returnToDrivingMain()
        s.expirePendingKnock()
        assertEquals(WireScreen.DRIVE, s.screen)
    }

    @Test fun bothRoadResponsesWorkWhenTheMapIsAlreadyExpanded() {
        for (accepted in listOf(false, true)) {
            val s = WireSession().apply { open(WireScreen.MAP); roadVoiceShown = true }
            s.respondToRoadVoice(accepted)
            assertFalse(s.roadVoiceShown)
            assertEquals(accepted, s.waypoint)
            assertEquals(if (accepted) WireScreen.MAP else WireScreen.DRIVE, s.screen)
            assertNotEquals(DFeed.ROAD_DELIVER, drivingFeed(s, s.screen))
        }
    }

    @Test fun munjunStopsAndLocksWhileDayeonRemainsTheMovingPeer() {
        assertFalse(companionIsDriving("문준 차", 0f, false))
        assertTrue(companionIsDriving("문준 차", 30f, false))
        assertTrue(companionIsDriving("다연 차", 0f, false))
        assertFalse(companionIsDriving("문준 차", 30f, true))
    }

    @Test fun microphoneWavCanBePlayedAndRecognizedAs16kMonoPcm() {
        val header = wavHeader(32_000)
        assertEquals("RIFF", String(header, 0, 4))
        assertEquals("WAVE", String(header, 8, 4))
        val bytes = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(32_036, bytes.getInt(4))
        assertEquals(1, bytes.getShort(22).toInt())
        assertEquals(16_000, bytes.getInt(24))
        assertEquals(16, bytes.getShort(34).toInt())
        assertEquals(32_000, bytes.getInt(40))
    }
}
