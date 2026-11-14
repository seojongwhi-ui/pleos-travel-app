package com.contest.travel.preview

import org.junit.Assert.*
import org.junit.Test

class DrivingPenaltyDetectorTest {
    @Test fun sustainedAccelerationDeductsOncePerManeuver() {
        val detector = DrivingPenaltyDetector()
        val events = (0..10).flatMap { i -> detector.sample(1000L + i * 200L, i * .8, true, true) }
        assertEquals(listOf(DetectedDrivingPenalty("급가속", 3)), events)
    }

    @Test fun hardBrakingHasItsOwnEventAndNormalBrakingDoesNot() {
        val hard = DrivingPenaltyDetector()
        val events = (0..8).flatMap { i -> hard.sample(1000L + i * 200L, 20 - i * .8, true, true) }
        assertEquals(listOf(DetectedDrivingPenalty("급제동", 3)), events)
        val normal = DrivingPenaltyDetector()
        assertTrue((0..8).flatMap { i -> normal.sample(1000L + i * 200L, 20 - i * .1, true, true) }.isEmpty())
    }

    @Test fun parkAndReverseCannotProducePenaltyEvents() {
        val detector = DrivingPenaltyDetector()
        assertTrue((0..20).flatMap { i -> detector.sample(1000L + i * 200L, i * 2.0, false, true) }.isEmpty())
        assertTrue(detector.sample(6000, -10.0, true, true).isEmpty())
    }

    @Test fun missingSamplesAndReconnectJumpsDoNotCountAsAcceleration() {
        val detector = DrivingPenaltyDetector()
        detector.sample(1000, 0.0, true, true)
        assertTrue(detector.sample(5000, 30.0, true, true).isEmpty())
        assertTrue(detector.sample(5200, 31.0, true, false).isEmpty())
        assertTrue(detector.sample(5400, 40.0, true, true).isEmpty())
    }

    @Test fun aSingleStepSpikeDoesNotCountAsSustainedAcceleration() {
        val detector = DrivingPenaltyDetector()
        detector.sample(1000, 0.0, true, true)
        assertTrue(detector.sample(1200, 10.0, true, true).isEmpty())
        assertTrue(detector.sample(1400, 10.0, true, true).isEmpty())
        assertTrue(detector.sample(1600, 10.0, true, true).isEmpty())
    }

    @Test fun speedingDeductsOnceUntilSpeedDropsBelowTheRearmPoint() {
        val detector = DrivingPenaltyDetector()
        val first = (0..30).flatMap { i -> detector.sample(1000L + i * 200L, 25.0, true, true) }
        assertEquals(listOf(DetectedDrivingPenalty("과속", 5)), first)
        detector.sample(7200, 20.0, true, true)
        val second = (0..15).flatMap { i -> detector.sample(7400L + i * 200L, 25.0, true, true) }
        assertEquals(1, second.count { it.reason == "과속" })
    }
}
