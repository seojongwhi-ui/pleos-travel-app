package com.contest.travel.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay

/** Only passive panels expire; recording, calls, pending knocks and unanswered votes stay open. */
internal fun canReturnDrivingToIdle(s: WireSession): Boolean = s.screen.isDriving &&
    !s.showSettings && !s.showEnd && !s.showHistoryUnavailable && !s.showPollTextEntry &&
    !s.showVoiceRecorder && !s.pollBusy && drivingFeed(s, s.screen) in setOf(
        DFeed.MESSAGE, DFeed.MENU, DFeed.HISTORY, DFeed.LISTENING,
        DFeed.UNAVAILABLE, DFeed.RECEIVED, DFeed.ROAD_NEAR, DFeed.ROAD_DELIVER
    )

@Composable
internal fun DrivingIdleReturn(s: WireSession, voice: LinkCarVoiceRuntime, foreground: Boolean) {
    LaunchedEffect(s, s.screen, drivingFeed(s, s.screen), s.drivingInteraction, s.drivingTouchActive,
        foreground, voice.isRecording, voice.isListening, voice.audio.isPlaying,
        s.pollBusy, s.showSettings, s.showEnd, s.showHistoryUnavailable, s.showPollTextEntry) {
        if (!foreground || s.drivingTouchActive || voice.isRecording || voice.isListening ||
            voice.audio.isPlaying || !canReturnDrivingToIdle(s)) return@LaunchedEffect
        delay(10_000)
        s.returnToDrivingMain(keepExpandedPanel = true)
    }
}

/** Observe touches without consuming them, including an interaction that is held or dragged. */
internal fun Modifier.trackDrivingInteraction(s: WireSession): Modifier = pointerInput(s) {
    try {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val pressed = event.changes.any { it.pressed }
                if (pressed != s.drivingTouchActive || event.changes.any { it.pressed != it.previousPressed }) {
                    s.drivingTouchActive = pressed
                    s.drivingInteraction++
                }
            }
        }
    } finally { s.drivingTouchActive = false }
}

/** Interrupt audio before clearing UI state, so an in-progress recording is saved, not discarded. */
internal fun sendPriorityKnock(s: WireSession, voice: LinkCarVoiceRuntime, target: String) {
    voice.audio.stopPlayback()
    voice.stopListening()
    if (voice.isRecording) {
        val kind = when (s.recordingTarget) { null -> "road"; "모두에게" -> "broadcast"; else -> "message" }
        voice.finishRecording(s.progress, null, kind)?.let { s.lastNoteId = it.id }
    }
    s.sendKnock(target)
}
