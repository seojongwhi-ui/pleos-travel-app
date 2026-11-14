package com.contest.travel.preview

import org.junit.Assert.*
import org.junit.Test

class DrivingFlowTest {
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

    @Test fun mediaControlsCannotEnableMusicWhenDepartingWithoutIt() {
        val s = WireSession().apply { startLocalTrip(false) }
        val song = s.song
        s.toggleMusicPlayback()
        s.stepSong(1)
        assertFalse(s.playing)
        assertEquals(song, s.song)
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
