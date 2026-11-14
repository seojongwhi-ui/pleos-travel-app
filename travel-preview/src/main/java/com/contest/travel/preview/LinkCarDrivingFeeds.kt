package com.contest.travel.preview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
internal fun DFeedContent(s: WireSession, feed: DFeed, compact: Boolean, height: Float,
    enabled: Boolean, setupRestricted: Boolean, voice: LinkCarVoiceRuntime,
    onVoiceClick: () -> Unit, onPollSpeech: () -> Unit) {
    if (feed == DFeed.HISTORY) { DHistory(s, enabled && !setupRestricted, voice); return }
    if (compact) {
        DCompactFeedContent(s, feed, height, enabled && !setupRestricted, voice,
            onVoiceClick, onPollSpeech)
        return
    }
    DText("음성 연결", 49, 34, DType.Eyebrow, DrivingFigma.Muted)
    if (s.pollDemoVisible || feed in setOf(DFeed.MENU, DFeed.UNAVAILABLE, DFeed.RECEIVED,
            DFeed.ROAD_NEAR, DFeed.ROAD_DELIVER, DFeed.RECORDING, DFeed.CALL_DECLINED))
        DDrivingBack(s, enabled)
    fun leavePreview() { s.designPreviewNode = null }
    val historyEnabled = enabled && !setupRestricted
    if (feed in setOf(DFeed.IDLE, DFeed.MESSAGE, DFeed.LISTENING, DFeed.MENU, DFeed.ROAD_NEAR))
        DChip("이전 대화", if (feed == DFeed.IDLE) 893 else 860, 30, 99,
            onClick = if (historyEnabled) ({ leavePreview(); s.showHistory = true }) else null)
    when (feed) {
        DFeed.IDLE -> {
            DImage(drivingAsset("imgImage49", R.drawable.driving_70aaa), 447, 201, 111, 116)
            DText("친구 차를 두드려보세요", 0, 327, DType.InputHint, DrivingFigma.Hint, 1022, TextAlign.Center)
            DSpeakButton(s, enabled, onVoiceClick)
        }
        DFeed.MESSAGE, DFeed.ROAD_NEAR -> {
            DImage(drivingAsset("imgFrame1707487204", R.drawable.driving_1652f), 1, 169, 1021, 265)
            DVoiceHeader(if (feed == DFeed.ROAD_NEAR) 289 else 303, 153,
                if (feed == DFeed.ROAD_NEAR) "문준 차 · 3분 전" else "다연 차 방금",
                drivingCompanionImage(s, if (feed == DFeed.ROAD_NEAR) "문준 차" else "다연 차",
                    "imgAvatar", R.drawable.driving_55f82), width = if (feed == DFeed.ROAD_NEAR) 446 else 416)
            DText(dAccent("이 ", "노래", " 누가 신청했어? 좋다"), 0, 253,
                DType.Headline, w = 1022, align = TextAlign.Center)
            if (feed == DFeed.MESSAGE) DText("제안이 오면 좋아 · 다음에 버튼이 함께 나타나요", 0, 345,
                DType.Caption, Color(0xFFCDCFD5), 1022, TextAlign.Center)
            DSpeakButton(s, enabled, onVoiceClick, pale = feed == DFeed.ROAD_NEAR)
        }
        DFeed.MENU -> {
            DImage(drivingAsset("imgFrame1707487204", R.drawable.driving_1652f), 1, 169, 1021, 265)
            DPanel(87, 105, 848, 442, color = Color.Transparent) {
                DText("모두에게 말하기", 16, 38, DType.ModalTitle, w = 816, align = TextAlign.Center)
                DText("어떤 방식으로 말할까요?", 16, 112, DType.ModalSub, Color(0xFF9398A5), 816, TextAlign.Center)
                listOf("투표 안건 말하기", "길에 음성 남기기").forEachIndexed { i, title ->
                    DPanel(if (i == 0) 16 else 432, 196, 400, 230, 48,
                        if (i == 0) Color(0xFF44464E) else Color(0xFF7583EE),
                        Modifier.linkCarPress(enabled) {
                            leavePreview(); s.showVoiceMenu = false
                            if (i == 0) onPollSpeech() else s.showVoiceRecorder = true
                        }) {
                        DImage(drivingAsset(if (i == 0) "imgGroup" else "imgGroup1707486540",
                            if (i == 0) R.drawable.driving_00832 else R.drawable.driving_38ba7), 168, 54, 64, 64)
                        DText(title, 0, 135, DType.Choice, Color.White, 400, TextAlign.Center)
                    }
                }
            }
        }
        DFeed.LISTENING -> {
            DImage(drivingAsset("imgFrame1707487204", R.drawable.driving_1652f), 1, 169, 1021, 265)
            DVoiceHeader(if (s.pollDemoVisible) 303 else 346, 153, "본인",
                drivingCompanionImage(s, "나", "imgAvatar", R.drawable.driving_60416),
                if (s.pollDemoVisible) if (s.pollDemoSpeaking) "말하는 중..." else "말하기 완료" else "듣는중...",
                if (s.pollDemoVisible) 416 else 332)
            val transcript = if (s.pollDemoVisible) "아 배고프다 휴게소 들리면 안되나 나 밥 먹고 가고 싶은데"
                else if (s.designPreviewNode != null) "아 배고프다 휴게소 들리면 안되나 밥 먹고 가고 싶은데"
                else if (s.pollBusy) "AI가 투표 안건을 만들고 있어요"
                else if (s.message.startsWith("인식: ")) s.message.removePrefix("인식: ") else voice.status
            DText(transcript, 127.5, 253, DType.Headline, DrivingFigma.ScoreBlue, 769, TextAlign.Center, 4)
            DSpeakButton(s, enabled, {
                voice.stopListening()
                if (s.pollDemoVisible) voice.audio.stopPlayback()
                s.pollDemoVisible = false; s.pollDemoSpeaking = false; s.speechFeedbackVisible = false
            }, pale = true)
            if (s.designPreviewNode == null && (voice.status.contains("오류") || voice.status.contains("실패"))) {
                DChip("텍스트로 입력", 720, 468, 245, onClick = if (enabled && !setupRestricted) ({ s.showPollTextEntry = true }) else null)
            }
        }
        DFeed.PROPOSAL -> {
            DImage(drivingAsset("imgFrame1707487204", R.drawable.driving_1652f), 1, 169, 1021, 265)
            DVoiceHeader(294, 153, avatar = drivingCompanionImage(s, "다연 차", "imgAvatar", R.drawable.driving_39ac5), width = 406)
            DText(s.generatedPoll?.let { androidx.compose.ui.text.AnnotatedString(it.title) }
                ?: dAccent("우리 ", "휴게소", " 들렀다 가자!"), 0, 253,
                DType.Proposal, Color(0xFF3B3B3B), w = 1022, align = TextAlign.Center, lines = 2)
            DText(if (s.generatedPoll != null) if (s.vote.isBlank()) "응답을 선택해 주세요" else "${s.vote} 선택했어요"
                else "화장실 좀 들렸다가 가게", 0, 329, DType.Thought, Color(0xFFCDCFD5), 1022, TextAlign.Center)
            if (s.generatedPoll == null) DText("죽전휴게소로 오실?", 0, 393, DType.ThoughtSmall, Color(0xFFE6E6E7), 1022, TextAlign.Center)
            val options = s.generatedPoll?.options
            if (options != null) {
                val w = (990f - (options.size - 1) * 16f) / options.size
                options.forEachIndexed { i, option ->
                    DButton(option, 16 + i * (w + 16), 521, w, enabled = enabled,
                        color = if (s.vote == option) DrivingFigma.Blue else DrivingFigma.Black) {
                        s.designPreviewNode = null; s.vote = option
                    }
                }
            } else {
                DButton("좋아!", 16, 521, 489, enabled = enabled) { s.respondToProposal(true) }
                DButton("다음에", 521, 521, 485, enabled = enabled,
                    color = DrivingFigma.Input, ink = DrivingFigma.Disabled) { s.respondToProposal(false) }
            }
        }
        DFeed.SENT -> {
            DImage(drivingCompanionImage(s, s.knockTarget, "imgAvatar", R.drawable.driving_fa064), 451, 110, 120, 120)
            DPanel(443, 102, 136, 136, 100, Color.Transparent,
                Modifier.border(4.dp, DrivingFigma.Mid, RoundedCornerShape(100.dp)))
            DImage(drivingAsset("imgEllipse13", R.drawable.driving_6dffb), 533, 92, 56, 56)
            DImage(drivingAsset("imgImage49", R.drawable.driving_c4443), 543, 106, 35, 28, scale = androidx.compose.ui.layout.ContentScale.Crop)
            DText(dAccent("", s.knockTarget, "를 노크했어요"), 0, 262, DType.Headline,
                w = 1022, align = TextAlign.Center)
            DText("${s.knockTarget}가 문을 열면 30초 동안 바로 대화할 수 있어요", 0, 350,
                DType.Description, DrivingFigma.Disabled, 1022, TextAlign.Center)
            DPanel(136, 440, 690, 14, 8, DrivingFigma.Pale)
            val p = if (s.designPreviewNode != null) 300f else 690f * s.seconds.coerceIn(0, 10) / 10
            DPanel(136, 440, p, 14, 8, DrivingFigma.Blue)
            DText("${s.seconds}초", 846, 428, DType.Seconds, DrivingFigma.Blue)
            DButton(if (s.seconds > 0) "노크 취소" else "닫기", 16, 521, 400, enabled = enabled,
                color = DrivingFigma.Input, ink = DrivingFigma.Disabled) { s.drive() }
            DButton(if (s.seconds > 0) "응답 시연 · 통화 연결" else "음성 메시지 남기기",
                432, 521, 574, enabled = enabled) {
                if (s.seconds > 0) s.acceptPendingDemoKnock()
                else s.open(WireScreen.VOICE_LEAVE)
            }
        }
        DFeed.INCOMING -> {
            DImage(drivingCompanionImage(s, s.incomingCaller, "imgAvatar", R.drawable.driving_bce4b), 451, 102, 120, 120)
            DPanel(443, 94, 136, 136, 100, Color.Transparent,
                Modifier.border(4.dp, DrivingFigma.Blue, RoundedCornerShape(100.dp)))
            DChip("주행 중", 470, 244, 82)
            DText(dAccent("", s.incomingCaller, "가 노크했어요"), 0, 300, DType.Headline,
                w = 1022, align = TextAlign.Center)
            DText("말로 \"열어\"라고 해도 돼요 · 10초가 지나면 자동으로 닫혀요", 0, 390,
                DType.SubLabel, Color(0xFFCDCFD5), 1022, TextAlign.Center)
            DButton("지금은 어려워요", 16, 521, 400, enabled = enabled,
                color = DrivingFigma.Input, ink = DrivingFigma.Disabled) { s.drive() }
            DButton("문 열고 대화하기", 432, 521, 574, enabled = enabled,
                icon = drivingAsset("imgIconDoor", R.drawable.driving_7d789)) {
                leavePreview(); s.knockTarget = s.incomingCaller; s.startKnockCall()
            }
        }
        DFeed.CALL -> {
            DImage(drivingAsset("imgTimerTrack", R.drawable.driving_38b81), 72, 150, 300, 300)
            if (s.designPreviewNode != null) DImage(drivingAsset("imgTimerProgress", R.drawable.driving_c8165), 72, 150, 300, 300)
            else Canvas(Modifier.dBounds(72, 150, 300, 300)) {
                drawArc(DrivingFigma.Blue, -90f, 360f * s.seconds.coerceIn(0, 30) / 30,
                    false, topLeft = Offset(9.dp.toPx(), 9.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(282.dp.toPx(), 282.dp.toPx()),
                    style = Stroke(18.dp.toPx(), cap = StrokeCap.Round))
            }
            DText(s.seconds.toString(), 72, 211, DType.Clock, DrivingFigma.Blue, 300, TextAlign.Center)
            DText("초 남음", 72, 340, DType.SubLabel, DrivingFigma.Hint, 300, TextAlign.Center)
            DImage(drivingCompanionImage(s, s.incomingCaller, "imgAvatar", R.drawable.driving_093c2), 430, 170, 56, 56)
            DChip("주행 중", 498, 179.5, 82)
            DText(dAccent("", s.incomingCaller, "와 대화 중"), 430, 250, DType.Headline)
            DText("30초가 지나면 자동으로 끝나요", 430, 336, DType.Description, DrivingFigma.Disabled)
            DWave(430, 400, 1.4f, long = true)
            DButton("대화 끝내기", 16, 521, 990, enabled = enabled,
                color = Color(0xFFEE5D5D), icon = drivingAsset("imgIconX", R.drawable.driving_361db)) { s.drive() }
        }
        DFeed.UNAVAILABLE -> {
            DImage(drivingCompanionImage(s, s.knockTarget, "imgAvatar", R.drawable.driving_1491e), 451, 96, 120, 120)
            DChip(if (s.designPreviewNode == null && s.knockTarget == "다연 차") "응답 없음" else "휴게소 정차 중", 444, 232, 134)
            DText(buildUnavailable(s.knockTarget), 0, 290, DType.Strong, w = 1022, align = TextAlign.Center)
            DText("음성 메시지를 남기면 ${s.knockTarget} 화면에 글자로 먼저 보여요", 0, 376,
                DType.Descriptive, DrivingFigma.Disabled, 1022, TextAlign.Center)
            DButton("음성 메시지 남기기", 16, 521, 990, enabled = enabled,
                icon = drivingAsset("imgIconMic", R.drawable.driving_88021)) {
                leavePreview(); s.showVoiceRecorder = true
            }
            DImage(drivingAsset("imgEllipse13", R.drawable.driving_29f9a), 521, 78, 56, 56)
            DImage(drivingAsset("imgIconX", R.drawable.driving_db67b), 531, 88, 36, 36)
        }
        DFeed.RECEIVED -> {
            DImage(drivingAsset("imgFrame1707487204", R.drawable.driving_1652f), 1, 169, 1021, 265)
            DPanel(353, 140, 316, 68, 34, Color.White) {
                DImage(drivingCompanionImage(s, "다연 차", "imgAvatar", R.drawable.driving_55f82), 10, 10, 48, 48)
                DText("다연 차 · 방금", 70, 22, DType.Meta, DrivingFigma.Subtle)
                DChip("음성 메시지", 190, 18, 102, 32, DrivingFigma.Blue, Color.White, DType.Tiny)
            }
            DText(dAccent("휴게소에서 ", "커피", " 사 갈게, 뭐 마실래?"), 0, 250,
                DType.Strong, w = 1022, align = TextAlign.Center)
            DText("글자는 AI가 짧게 정리했어요 · 목소리는 원본 그대로", 0, 340,
                DType.SubLabel, Color(0xFFCDCFD5), 1022, TextAlign.Center)
            DButton("목소리로 듣기", 16, 521, 990, enabled = enabled,
                color = DrivingFigma.Blue, icon = drivingAsset("imgIconPlay", R.drawable.driving_5c717)) { DPlayNote(s, voice) }
        }
        DFeed.ROAD_DELIVER -> {
            DVoiceHeader(218, 100, "다연 차가 길에 남긴 음성 · 12분 전",
                drivingCompanionImage(s, "다연 차", "imgAvatar", R.drawable.driving_55f82), width = 586)
            DText(dAccent("\"마지막에 오는 사람 여기 들러서\n", "음료수", " 좀 사오기~\""),
                0, 200, DType.Quote, w = 1022, align = TextAlign.Center, lines = 2)
            DPanel(307, 372, 396, 92, 28, Color(0xFFF3F4F8)) {
                DPanel(16, 14, 64, 64, 20, Color.White) {
                    DImage(drivingAsset("imgIcon", R.drawable.driving_37a0c), 16, 16, 32, 32)
                }
                DText("GU 편의점 청평점", 96, 17, DType.Place)
                DText("오른쪽 · 3km 앞 · 다연 차가 남긴 위치", 96, 52, DType.Small, DrivingFigma.Hint)
            }
            DButton("그냥 갈게요", 16, 521, 400, enabled = enabled,
                color = DrivingFigma.Input, ink = DrivingFigma.Disabled) { s.roadVoiceShown = false; s.drive() }
            DButton("들렀다 갈게요", 432, 521, 574, enabled = enabled) {
                leavePreview(); s.waypoint = true; s.roadVoiceShown = false; s.open(WireScreen.MAP)
            }
        }
        DFeed.RECORDING -> {
            Box(Modifier.dBounds(0, 80, 1022, 560)) {
                DesignedRoadVoiceRecorderCard(Modifier.fillMaxSize()) {
                    s.showVoiceRecorder = false
                    s.message = "녹음 시연을 마쳤어요. 음성 파일은 저장되지 않았어요."
                }
            }
        }
        DFeed.CALL_DECLINED -> {
            DText("${s.incomingCaller}의 전화를 거절했어요", 40, 260, DType.Strong, w = 942, align = TextAlign.Center)
            DText("잠시 후 주행 화면으로 돌아가요", 40, 350, DType.Description,
                DrivingFigma.Hint, 942, TextAlign.Center)
        }
        else -> Unit
    }
}

/** A resized panel keeps the active conversation; resizing never creates a proposal. */
@Composable
private fun DCompactFeedContent(s: WireSession, feed: DFeed, height: Float, enabled: Boolean,
    voice: LinkCarVoiceRuntime, onVoiceClick: () -> Unit, onPollSpeech: () -> Unit) {
    val actionY = height - 88f
    DText("음성 연결", 48, 20, DType.Eyebrow, DrivingFigma.Muted)
    when (feed) {
        DFeed.IDLE -> {
            DText("친구 차를 두드려보세요", 48, 107, DType.CompactHeadline,
                DrivingFigma.Hint, 580)
            DButton("모두에게 말하기", 650, actionY, 344, 72, enabled = enabled,
                type = DType.Meta, onClick = onVoiceClick)
        }
        DFeed.MESSAGE, DFeed.ROAD_NEAR -> {
            val person = if (feed == DFeed.ROAD_NEAR) "문준 차 · 3분 전" else "다연 차 방금"
            DText(person, 48, 76, DType.Meta, DrivingFigma.Subtle)
            DText(dAccent("이 ", "노래", " 누가 신청했어? 좋다"), 48, 111,
                DType.Description, w = 570, lines = 2)
            DButton("모두에게 말하기", 650, actionY, 344, 72, enabled = enabled,
                type = DType.Meta, onClick = onVoiceClick)
        }
        DFeed.PROPOSAL -> {
            DText("투표 제안", 48, 76, DType.Meta, DrivingFigma.Subtle)
            DText(s.generatedPoll?.title ?: "우리 휴게소 들렀다 가자!", 48, 111,
                DType.Description, w = 520, lines = 2)
            val options = s.generatedPoll?.options
            if (options.isNullOrEmpty()) {
                DButton("좋아!", 600, actionY, 185, 72, enabled = enabled,
                    type = DType.Meta) { s.respondToProposal(true) }
                DButton("다음에", 799, actionY, 195, 72, enabled = enabled,
                    color = DrivingFigma.Input, ink = DrivingFigma.Disabled,
                    type = DType.Meta) { s.respondToProposal(false) }
            } else {
                options.take(4).forEachIndexed { i, option ->
                    DButton(option, 560 + (i % 2) * 220,
                        actionY - if (options.size > 2 && i < 2) 68 else 0,
                        206, 60, enabled = enabled, type = DType.Meta,
                        color = if (s.vote == option) DrivingFigma.Blue else DrivingFigma.Black) {
                        s.vote = option
                    }
                }
            }
        }
        DFeed.ROAD_DELIVER -> {
            DText("다연 차가 길에 남긴 음성 · 12분 전", 48, 76,
                DType.Meta, DrivingFigma.Subtle)
            DText(dAccent("\"마지막에 오는 사람 여기 들러서 ", "음료수", " 좀 사오기~\""),
                48, 111, DType.Description, w = 570, lines = 2)
            DChip("GU 편의점 청평점", 650, 100, 344, 44)
            DButton("그냥 갈게요", 650, actionY, 160, 72, enabled = enabled,
                color = DrivingFigma.Input, ink = DrivingFigma.Disabled,
                type = DType.Meta) { s.roadVoiceShown = false; s.drive() }
            DButton("들렀다 갈게요", 824, actionY, 170, 72, enabled = enabled,
                type = DType.Meta) {
                s.waypoint = true; s.roadVoiceShown = false; s.open(WireScreen.MAP)
            }
        }
        DFeed.LISTENING -> {
            DText("듣는 중", 48, 76, DType.Meta, DrivingFigma.Subtle)
            val transcript = if (s.pollBusy) "AI가 투표 안건을 만들고 있어요"
                else if (s.message.startsWith("인식: ")) s.message.removePrefix("인식: ")
                else voice.status
            DText(transcript, 48, 111, DType.Description, w = 570, lines = 2)
            DButton("입력 종료", 650, actionY, 344, 72, enabled = enabled,
                type = DType.Meta) {
                voice.stopListening(); s.pollDemoVisible = false
                s.pollDemoSpeaking = false; s.speechFeedbackVisible = false
            }
        }
        DFeed.MENU -> {
            DText("어떤 방식으로 말할까요?", 48, 106, DType.Description, w = 520)
            DButton("투표 안건 말하기", 560, actionY, 210, 72, enabled = enabled,
                type = DType.Meta) { s.showVoiceMenu = false; onPollSpeech() }
            DButton("길에 음성 남기기", 784, actionY, 210, 72, enabled = enabled,
                type = DType.Meta) { s.showVoiceMenu = false; s.showVoiceRecorder = true }
        }
        DFeed.SENT -> {
            DText("${s.knockTarget}를 노크했어요 · ${s.seconds}초", 48, 111,
                DType.Description, w = 570, lines = 2)
            DButton("노크 취소", 650, actionY, 160, 72, enabled = enabled,
                color = DrivingFigma.Input, ink = DrivingFigma.Disabled,
                type = DType.Meta) { s.drive() }
            DButton("응답 시연", 824, actionY, 170, 72, enabled = enabled,
                type = DType.Meta) { s.acceptPendingDemoKnock() }
        }
        DFeed.INCOMING -> {
            DText("${s.incomingCaller}가 노크했어요", 48, 111,
                DType.Description, w = 570)
            DButton("지금은 어려워요", 650, actionY, 160, 72, enabled = enabled,
                color = DrivingFigma.Input, ink = DrivingFigma.Disabled,
                type = DType.Meta) { s.drive() }
            DButton("문 열고 대화하기", 824, actionY, 170, 72, enabled = enabled,
                type = DType.Meta) { s.knockTarget = s.incomingCaller; s.startKnockCall() }
        }
        DFeed.CALL -> {
            DText("${s.incomingCaller}와 대화 중 · ${s.seconds}초 남음", 48, 111,
                DType.Description, w = 570)
            DButton("대화 끝내기", 650, actionY, 344, 72, enabled = enabled,
                type = DType.Meta) { s.drive() }
        }
        DFeed.RECEIVED -> {
            DText("휴게소에서 커피 사 갈게, 뭐 마실래?", 48, 111,
                DType.Description, w = 570, lines = 2)
            DButton("목소리로 듣기", 650, actionY, 344, 72, enabled = enabled,
                type = DType.Meta) { DPlayNote(s, voice) }
        }
        DFeed.UNAVAILABLE -> {
            DText("${s.knockTarget}가 지금은 문을 못 열어요", 48, 111,
                DType.Description, w = 570, lines = 2)
            DButton("음성 메시지 남기기", 650, actionY, 344, 72, enabled = enabled,
                type = DType.Meta) { s.showVoiceRecorder = true }
        }
        DFeed.RECORDING -> {
            DText("길에 음성 남기는 중", 48, 111, DType.Description, w = 570)
            DButton("완료", 650, actionY, 344, 72, enabled = enabled,
                type = DType.Meta) { s.showVoiceRecorder = false }
        }
        DFeed.CALL_DECLINED -> DText("${s.incomingCaller}의 전화를 거절했어요", 48, 111,
            DType.Description, w = 900)
        DFeed.HISTORY -> Unit
    }
}

private fun buildUnavailable(name: String) = androidx.compose.ui.text.buildAnnotatedString {
    withStyle(androidx.compose.ui.text.SpanStyle(color = DrivingFigma.Red)) { append(name) }
    append("가 지금은 문을 못 열어요")
}

private fun DPlayNote(s: WireSession, voice: LinkCarVoiceRuntime) {
    voice.notes.notes.lastOrNull { it.id == s.lastNoteId }?.let { voice.play(it) }
        ?: run { if (!voice.playDemoMarker {}) s.message = "재생할 원본 음성이 없습니다." }
}

@Composable
private fun DSpeakButton(s: WireSession, enabled: Boolean, onClick: () -> Unit, pale: Boolean = false) {
    if (!pale) DButton("모두에게 말하기", 16, 521, 990, enabled = enabled,
        icon = drivingAsset("imgIcon", R.drawable.driving_1f7d7), iconSize = 40, iconAfter = true) {
        s.designPreviewNode = null; onClick()
    } else DPanel(16, 521, 990, 120, 48, DrivingFigma.Input,
        Modifier.linkCarPress(enabled) { s.designPreviewNode = null; onClick() }) {
        DText("모두에게 말하기", 48, 41, DType.Body, DrivingFigma.Disabled)
        DPanel(882, 12, 96, 96, 48, DrivingFigma.Black) {
            DImage(drivingAsset("imgIcon", R.drawable.driving_e96b3), 28, 28, 40, 40)
        }
    }
}

@Composable
private fun DHistory(s: WireSession, enabled: Boolean, voice: LinkCarVoiceRuntime) {
    DText("이전 대화", 49, 34, DType.Eyebrow, DrivingFigma.Muted)
    DDrivingBack(s, enabled)
    DChip("정차 중에만 볼 수 있어요", 785, 30, 219, color = Color(0xFFE5F7ED), ink = DrivingFigma.Green)
    val messages = listOf("우리 휴게소 들렀다 가자!", "이 노래 누가 신청했어? 좋다",
        "마지막에 오는 사람 음료수 좀 사오기~", "다들 출발했어?")
    val times = listOf("다연 차 · 3분 전", "문준 차 · 8분 전", "다연 차 · 12분 전", "나 · 20분 전")
    repeat(4) { i ->
        val y = i * 136
        DImage(drivingCompanionImage(s, when (i) { 0, 2 -> "다연 차"; 1 -> "문준 차"; else -> "나" },
            when (i) { 0, 2 -> "imgAvatar"; 1 -> "imgAvatar1"; else -> "imgAvatar2" },
            R.drawable.driving_cd7ef), 40, 116 + y, 72, 72)
        DText(times[i], 136, 112 + y, DType.Label, Color(0xFF9398A5))
        DText(messages[i], 136, 142 + y, DType.Body)
        if (i < 3) DPanel(40, 224 + y, 950, 2, 0, Color(0xFFEEEFF2))
        when (i) {
            0 -> DChip("제안 · 좋아 3/3", 848, 134, 142, color = DrivingFigma.Blue, ink = Color.White)
            1, 2 -> {
                DChip(if (i == 1) "음성" else "길에 남긴 음성", if (i == 1) 816 else 746,
                    134 + y, if (i == 1) 66 else 136, color = if (i == 1) DrivingFigma.Input else DrivingFigma.Pale,
                    ink = if (i == 1) DrivingFigma.Subtle else DrivingFigma.Blue)
                DPanel(902, 116 + y, 88, 88, 100, DrivingFigma.Black,
                    Modifier.linkCarPress(enabled) { DPlayNote(s, voice) }) {
                    DImage(drivingAsset("imgIcon", R.drawable.driving_5c717), 28, 28, 32, 32)
                }
            }
            else -> DChip("모두에게", 895, 542, 95)
        }
    }
}

@Composable
private fun DDrivingBack(s: WireSession, enabled: Boolean) {
    DButton("주행 화면으로", 220, 22, 230, 56, enabled = enabled,
        color = DrivingFigma.Input, ink = DrivingFigma.Subtle,
        icon = R.drawable.predrive_f92b5, iconSize = 24, type = DType.Label) {
        s.returnToDrivingMain()
    }
}

@Composable
internal fun DDialogs(s: WireSession, enabled: Boolean) {
    val open = s.showSettings || s.showEnd
    AnimatedVisibility(open, enter = fadeIn(tween(180)), exit = fadeOut(tween(140))) {
        DPanel(0, 0, 1700, 1184, 0, Color(0xFF0D0E12).copy(alpha = .45f), Modifier.pointerInput(enabled) {
            detectTapGestures { if (enabled) { s.showSettings = false; s.showEnd = false } }
        })
    }
    AnimatedVisibility(s.showSettings, Modifier.dBounds(350, 122, 1000, 940),
        enter = fadeIn(tween(200)) + scaleIn(tween(240), initialScale = .97f),
        exit = fadeOut(tween(140)) + scaleOut(tween(160), targetScale = .98f)) {
        DPanel(0, 0, 1000, 940, 56, Color.White,
            Modifier.pointerInput(Unit) { detectTapGestures {} }) {
            DText("설정", 64, 56, DType.SettingTitle)
            DPanel(872, 48, 72, 72, 36, DrivingFigma.Input,
                Modifier.semantics { contentDescription = "설정 닫기" }.linkCarPress(enabled) { s.showSettings = false }) {
                DImage(R.drawable.driving_f7977, 20, 20, 32, 32)
            }
            val titles = listOf("메시지 읽어 주기", "노크 받기", "길에 남긴 음성 알림", "음악 신청 받기", "내 점수 공개")
            val hints = listOf("다른 차의 말을 음성으로 읽어 줘요", "노크가 오면 10초 안에 문을 열 수 있어요",
                "300m 전에 미리 알려 줘요", "다른 차가 신청한 곡을 함께 들어요", "끄면 순위에 이름 대신 점수만 보여요")
            titles.forEachIndexed { i, title ->
                val y = 180 + i * 140
                DText(title, 64, y, DType.Body)
                DText(hints[i], 64, y + 46, DType.SettingDetail, DrivingFigma.Hint)
                if (i < 4) DPanel(64, y + 118, 872, 2, 0, Color(0xFFEEEFF2))
                val checked = s.settingsToggles[i]
                DPanel(840, y + 14, 96, 56, 100, Color.Transparent,
                    Modifier.semantics {
                        contentDescription = title
                        toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
                    }.linkCarPress(enabled, Role.Switch) {
                        s.settingsToggles = s.settingsToggles.toMutableList().also { it[i] = !checked }
                    }) {
                    Crossfade(checked, animationSpec = tween(160), label = "drivingPreference") { on ->
                        DImage(if (on) R.drawable.driving_47016 else R.drawable.driving_c816a, 0, 0, 96, 56)
                    }
                }
            }
            DText("바꾼 설정은 이번 동행에만 적용돼요", 0, 880, DType.Meta,
                DrivingFigma.Muted, 1000, TextAlign.Center)
        }
    }
    AnimatedVisibility(s.showEnd, Modifier.dBounds(426, 272, 848, 530),
        enter = fadeIn(tween(200)) + scaleIn(tween(240), initialScale = .97f),
        exit = fadeOut(tween(140)) + scaleOut(tween(160), targetScale = .98f)) {
        DPanel(0, 0, 848, 530, 56, Color.White,
            Modifier.pointerInput(Unit) { detectTapGestures {} }) {
            DPanel(364, 64, 120, 120, 60, Color(0xFFFF5F5F)) {
                DImage(R.drawable.driving_2c053, 30, 32, 56, 56)
            }
            DText("동행을 끝낼까요?", 16, 216, DType.ModalTitle, w = 816, align = TextAlign.Center)
            DText("다른 차들은 계속 함께 가요\n지금까지의 기록은 내 여행 기록에 남아요", 16, 290,
                DType.ModalSub, Color(0xFF9398A5), 816, TextAlign.Center, 2)
            DButton("계속 함께 가기", 16, 394, 400, enabled = enabled,
                color = DrivingFigma.Input, ink = DrivingFigma.Disabled) { s.showEnd = false }
            DButton("동행 종료", 432, 394, 400, enabled = enabled) {
                s.home()
            }
        }
    }
}
