package com.contest.travel.preview

import org.junit.Assert.*
import org.junit.Test

class DrivingFlowTest {
    @Test fun demoArrivalClosesActivePanelsAndCannotReviveAnEarlierKnock() {
        val s = WireSession().apply {
            startLocalTrip(true)
            open(WireScreen.MAP)
            sendKnock("문준 차")
            beginVoiceRecording("문준 차")
            showHistory = true
            showSettings = true
            lastNoteId = "saved-recording"
        }
        s.arriveForDemo()
        assertEquals(listOf(WireScreen.HOME, WireScreen.ARRIVED), s.stack.toList())
        assertFalse(s.showVoiceRecorder || s.showHistory || s.showSettings)
        assertEquals("saved-recording", s.lastNoteId)
        s.expirePendingKnock()
        assertFalse(s.acceptPendingDemoKnock())
        assertEquals(WireScreen.ARRIVED, s.screen)
        s.open(WireScreen.SUMMARY)
        s.open(WireScreen.SAVE)
        s.back()
        assertEquals(WireScreen.SUMMARY, s.screen)
        s.home()
        assertFalse(s.demoArrival)
    }

    @Test fun movingConsoleMayShowOnlyExplicitDemoEndingPagesNeverRealCarOrSetupPages() {
        val s = WireSession().apply { startLocalTrip(true) }
        assertFalse(canShowDemoEnding(s, WireScreen.ARRIVED, false))
        s.arriveForDemo()
        for (page in listOf(WireScreen.ARRIVED, WireScreen.SUMMARY, WireScreen.SAVE)) {
            assertTrue(canShowDemoEnding(s, page, false))
            assertFalse(canShowDemoEnding(s, page, true))
        }
        assertFalse(canShowDemoEnding(s, WireScreen.HOME, false))
        assertFalse(canShowDemoEnding(s, WireScreen.CREATE, false))
        s.startLocalTrip(true)
        assertFalse(s.demoArrival)
    }

    @Test fun closingAnyPreviewFeedReturnsToTheIdleHomeWithoutRevivingTheSongMessage() {
        for (node in listOf("443:21058", "443:22235", "443:24648", "443:23484")) {
            val s = WireSession().apply { applyDrivingDesignPreview(node) }
            s.returnToDrivingMain()
            assertEquals(DFeed.IDLE, drivingFeed(s, s.screen))
            assertNull(s.designPreviewNode)
            s.openHistory(false)
            s.returnToDrivingMain()
            assertEquals(DFeed.IDLE, drivingFeed(s, s.screen))
        }
    }

    @Test fun aKnockImmediatelyReplacesOtherPanelsAndANewerRecipientGetsAFreshCountdown() {
        val panels: List<(WireSession) -> Unit> = listOf(
            { it.openHistory(false) }, { it.openVoiceUi() }, { it.beginVoiceRecording("문준 차") },
            { it.speechFeedbackVisible = true; it.pollBusy = true },
            { it.roadVoiceShown = true }, { it.proposalVisible = true }
        )
        for (showPanel in panels) {
            val s = WireSession().apply { open(WireScreen.DRIVE) }
            showPanel(s)
            s.sendKnock("다연 차")
            assertEquals(DFeed.SENT, drivingFeed(s, s.screen))
            assertFalse(s.showHistory || s.showVoiceMenu || s.showVoiceRecorder || s.pollBusy)
            val request = s.knockRequestRevision
            s.seconds = 1
            s.sendKnock("문준 차")
            assertEquals("문준 차", s.knockTarget)
            assertEquals(10, s.seconds)
            assertTrue(s.knockRequestRevision > request)
            s.returnToDrivingMain()
            assertEquals(DFeed.IDLE, drivingFeed(s, s.screen))
        }
    }

    @Test fun passivePanelsCanExpireWithoutCollapsingTheMapOrInterruptingActiveWork() {
        val s = WireSession().apply { open(WireScreen.MAP); openHistory(false) }
        assertTrue(canReturnDrivingToIdle(s))
        s.returnToDrivingMain(keepExpandedPanel = true)
        assertEquals(WireScreen.MAP, s.screen)
        assertEquals(DFeed.IDLE, drivingFeed(s, s.screen))
        s.beginVoiceRecording()
        assertFalse(canReturnDrivingToIdle(s))
        s.sendKnock("문준 차")
        assertFalse(canReturnDrivingToIdle(s))
        s.startKnockCall()
        assertFalse(canReturnDrivingToIdle(s))
        s.returnToDrivingMain()
        s.proposalVisible = true
        assertFalse(canReturnDrivingToIdle(s))
    }

    @Test fun bundledPollSpeechStaysVisibleInTheNewDrivingFeedAndCanBeClosed() {
        val s = WireSession().apply {
            open(WireScreen.DRIVE); pollDemoVisible = true; pollDemoSpeaking = true
        }
        assertEquals(DFeed.LISTENING, drivingFeed(s, s.screen))
        s.returnToDrivingMain()
        assertFalse(s.pollDemoVisible)
        assertFalse(s.pollDemoSpeaking)
        assertEquals(DFeed.IDLE, drivingFeed(s, s.screen))
    }

    @Test fun aNewKnockAndCallStartWithTheirOwnDurationsAndRecipient() {
        val s = WireSession().apply { seconds = 1 }
        s.sendKnock("문준 차")
        assertEquals(10, s.seconds)
        assertEquals(WireScreen.KNOCK_SENT, s.screen)
        s.startKnockCall()
        assertEquals(30, s.seconds)
        assertEquals("문준 차", s.incomingCaller)
        assertEquals(WireScreen.CALL, s.screen)
    }

    @Test fun acceptingTheLocalWaypointProposalOpensTheMap() {
        val s = WireSession().apply { open(WireScreen.DRIVE); proposalVisible = true }
        s.respondToProposal(true)
        assertTrue(s.waypoint)
        assertFalse(s.proposalVisible)
        assertEquals(WireScreen.MAP, s.screen)
    }

    @Test fun decliningANewProposalKeepsAnExistingWaypoint() {
        val s = WireSession().apply {
            open(WireScreen.DRIVE); waypoint = true; proposalVisible = true
        }
        s.respondToProposal(false)
        assertTrue(s.waypoint)
        assertFalse(s.proposalVisible)
        assertEquals(WireScreen.DRIVE, s.screen)
    }

    @Test fun explicitPlaybackCanStartMusicAfterDepartingWithoutIt() {
        // demo's music controls were intentionally enabled by the team's music simulation change.
        val s = WireSession().apply { startLocalTrip(false) }
        assertFalse(s.playing)
        s.toggleMusicPlayback()
        assertTrue(s.music)
        assertTrue(s.playing)
        val song = s.song
        s.stepSong(1)
        assertEquals((song + 1) % linkCarSongs.size, s.song)
        s.toggleMusicPlayback()
        assertFalse(s.playing)
    }

    @Test fun previousAndNextWrapWithoutLeavingAStaleSeekPosition() {
        val s = WireSession().apply { song = 0; songSeek = .8f }
        s.stepSong(-1)
        assertEquals(linkCarSongs.lastIndex, s.song)
        assertEquals(0f, s.songSeek)
        s.stepSong(1)
        assertEquals(0, s.song)
    }
}
