package com.contest.travel.preview

import org.junit.Assert.*
import org.junit.Test

class PreDriveFlowTest {
    @Test fun invitationNeverExceedsFourCarsIncludingTheHost() {
        val session = WireSession()
        session.toggleInvitee("성윤")
        session.toggleInvitee("민석")
        assertEquals(setOf("다연", "문준", "성윤"), session.invitees)
        session.toggleInvitee("다연")
        session.toggleInvitee("민석")
        assertEquals(setOf("문준", "성윤", "민석"), session.invitees)
    }

    @Test fun reselectingAPersonKeepsTheRosterOrderStable() {
        val session = WireSession()
        session.toggleInvitee("다연")
        session.toggleInvitee("다연")
        assertEquals(listOf("다연", "문준"), session.orderedInvitees)
    }

    @Test fun emptyInvitationCannotAdvanceButAValidPhoneCan() {
        val session = WireSession().apply { invitees = emptySet(); phone = "123" }
        session.sendInvites()
        assertEquals(WireScreen.HOME, session.screen)
        session.phone = "010-1234-5678"
        session.sendInvites()
        assertEquals(WireScreen.WAITING, session.screen)
        assertEquals(setOf("010-1234-5678"), session.invitees)
        session.sendInvites()
        assertEquals(1, session.invitees.size)
    }

    @Test fun goingBackPreservesTheTripAndSelection() {
        val session = WireSession().apply {
            open(WireScreen.CREATE)
            createPurposeSelection = "가족"
            purpose = "가족"
            destination = "할머니 댁"
            createRecentSelection = "할머니 댁"
            open(WireScreen.INVITE)
        }
        session.back()
        assertEquals(WireScreen.CREATE, session.screen)
        assertEquals("가족", session.createPurposeSelection)
        assertEquals("할머니 댁", session.createRecentSelection)
        assertEquals(-1, session.navigationDirection)
    }

    @Test fun sharingScreenAndExistingSettingsUseTheSamePreferences() {
        val session = WireSession()
        session.sharingOptions = listOf(false, true, true)
        assertFalse(session.settingsToggles[1])
        assertTrue(session.settingsToggles[2])
        assertTrue(session.settingsToggles[3])
        session.settingsToggles = session.settingsToggles.toMutableList().also { it[2] = false }
        assertEquals(listOf(false, false, true), session.sharingOptions)
    }

    @Test fun departingWithoutMusicCannotLeavePlaybackEnabled() {
        val session = WireSession().apply { open(WireScreen.MUSIC_PICK); playing = true }
        session.startLocalTrip(false)
        assertEquals(WireScreen.DRIVE, session.screen)
        assertFalse(session.music)
        assertFalse(session.playing)
        assertFalse(session.tripMotionStarted)
    }
}
