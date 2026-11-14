package com.contest.travel.preview

import org.junit.Assert.*
import org.junit.Test

class CompanionFeedbackTest {
    @Test fun aPenaltyChangesOnlyItsRecipientAndSurvivesPanelChanges() {
        val scores = CompanionScores().apply { reset(100, 70) }
        val event = scores.deduct("다연 차", 3, "급가속")!!
        assertEquals(97, scores.score("다연 차"))
        assertEquals(70, scores.score("문준 차"))
        assertEquals(3, event.points)
        assertSame(event, scores.penalty("다연 차"))
        assertNull(scores.penalty("문준 차"))
    }

    @Test fun deductionsNeverGoBelow70OrInventPointsLostAtTheFloor() {
        val scores = CompanionScores().apply { reset(72, 64) }
        assertEquals(2, scores.deduct("다연 차", 3, "급가속")!!.points)
        assertEquals(70, scores.score("다연 차"))
        assertNull(scores.deduct("다연 차", 3, "급가속"))
        assertNull(scores.deduct("문준 차", -3, "급가속"))
        assertEquals(70, scores.score("문준 차"))
    }

    @Test fun endingAndSettingsKeepTheLiveVoiceMenuAssetContext() {
        val s = WireSession().apply { applyDrivingDesignPreview("443:22235") }
        s.designPreviewNode = null
        val before = drivingFigmaNode(s, s.screen)
        s.showEnd = true
        assertEquals(before, drivingFigmaNode(s, s.screen))
        s.showEnd = false
        s.showSettings = true
        assertEquals(before, drivingFigmaNode(s, s.screen))
        assertEquals("443:22235", before)
    }

    @Test fun aNewJourneyClearsOldPenaltyBadgesAndScores() {
        val s = WireSession()
        s.drivingScores.deduct("다연 차", 3, "급가속")
        s.startLocalTrip(true)
        assertEquals(97, s.drivingScores.score("다연 차"))
        assertNull(s.drivingScores.penalty("다연 차"))
    }

    @Test fun demoReplyUsesTheSelectedPersonAndCannotReviveACancelledKnock() {
        val s = WireSession()
        s.sendKnock("다연 차")
        assertTrue(s.acceptPendingDemoKnock())
        assertEquals("다연 차", s.incomingCaller)
        assertEquals(WireScreen.CALL, s.screen)
        assertEquals(30, s.seconds)
        assertFalse(s.acceptPendingDemoKnock())
        s.sendKnock("다연 차")
        s.drive()
        assertFalse(s.acceptPendingDemoKnock())
        assertEquals(WireScreen.DRIVE, s.screen)
    }

    @Test fun visibleReturnActionEscapesHistoryAndUnavailableScreensWithoutEndingTheTrip() {
        val s = WireSession().apply {
            open(WireScreen.DRIVE)
            open(WireScreen.VOICE_LEAVE)
            showHistory = true
            showVoiceRecorder = true
            speechFeedbackVisible = true
            roadVoiceShown = true
        }
        s.returnToDrivingMain()
        assertEquals(WireScreen.DRIVE, s.screen)
        assertFalse(s.showHistory)
        assertFalse(s.showVoiceRecorder)
        assertFalse(s.speechFeedbackVisible)
        assertFalse(s.roadVoiceShown)
        assertFalse(s.stack.contains(WireScreen.VOICE_LEAVE))
    }
}
