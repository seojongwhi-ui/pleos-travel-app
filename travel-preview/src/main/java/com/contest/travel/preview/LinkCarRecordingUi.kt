package com.contest.travel.preview

import android.os.SystemClock
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

internal fun finishVoiceRecording(s: WireSession, voice: LinkCarVoiceRuntime) {
    val target = s.recordingTarget
    val kind = if (target == null) "road" else if (target == "모두에게") "broadcast" else "message"
    val note = voice.finishRecording(s.progress, null, kind)
    if (note == null) { s.message = voice.status; return }
    s.lastNoteId = note.id
    s.returnToDrivingMain()
    s.message = voice.status
    if (target == "모두에게") {
        s.speechFeedbackVisible = true
        voice.recognizeRecording(note) { transcript ->
            s.message = "인식: $transcript"
            s.pollBusy = true
        }
    }
}

/** The recorder stays in the runtime across panel resizing; only its layout changes. */
@Composable
internal fun DRecordingContent(s: WireSession, voice: LinkCarVoiceRuntime, compact: Boolean,
    height: Float, enabled: Boolean) {
    var seconds by remember { mutableIntStateOf(0) }
    var level by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(voice.isRecording) {
        while (voice.isRecording) {
            seconds = ((SystemClock.elapsedRealtime() - voice.recordingStartedAt) / 1000).toInt()
            level = voice.audio.amplitude
            delay(80)
        }
        level = 0f
    }
    val title = when (val target = s.recordingTarget) {
        null -> "길에 음성 남기기"
        "모두에게" -> "모두에게 말하기"
        else -> "${target}에 음성 메시지 남기기"
    }
    val active = voice.isRecording
    val time = "%02d:%02d".format(seconds / 60, seconds % 60)
    if (compact) {
        DText(title, 48, 68, DType.Description, w = 560)
        Box(Modifier.dBounds(48, 108, 540, 80)) {
            DemoRecordingWaves(Modifier.fillMaxSize(), level, active)
        }
        DText(if (active) "$time · 녹음 중" else "마이크를 허용해 주세요", 48, height - 60,
            DType.Meta, DrivingFigma.Subtle)
        DButton(if (active) "녹음 완료" else "다시 시도", 650, height - 88, 344, 72,
            enabled = enabled, type = DType.Meta) {
            if (active) finishVoiceRecording(s, voice) else s.recordingAttempt++
        }
    } else {
        DText(title, 48, 100, DType.Name, w = 926)
        DText(if (active) "내 목소리를 녹음하고 있어요" else voice.status,
            48, 148, DType.Meta, DrivingFigma.Subtle, 926, lines = 2)
        Box(Modifier.dBounds(32, 198, 958, 196)) {
            DemoRecordingWaves(Modifier.fillMaxSize(), level, active)
        }
        DPanel(451, 234, 120, 120, 60, Color.White,
            Modifier.border(3.dp, DrivingFigma.Blue, CircleShape)) {
            Image(painterResource(R.drawable.drive_voice_map_mic), null, Modifier.dBounds(28, 28, 64, 64),
                colorFilter = ColorFilter.tint(DrivingFigma.Blue))
        }
        DText(time, 0, 404, DType.Strong, w = 1022, align = TextAlign.Center)
        DText(if (active) "완료를 누르면 원본 음성을 저장해요" else "권한을 허용한 뒤 다시 시도해 주세요",
            0, 480, DType.Meta, DrivingFigma.Subtle, 1022, TextAlign.Center)
        DButton(if (active) "녹음 완료" else "다시 시도", 16, 521, 990, enabled = enabled) {
            if (active) finishVoiceRecording(s, voice) else s.recordingAttempt++
        }
    }
}
