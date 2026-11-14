package com.contest.travel.preview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun ColumnScope.WireDriving(s: WireSession, screen: WireScreen, wide: Boolean, enabled: Boolean,
    voice: LinkCarVoiceRuntime, designWide: Boolean, setupRestricted: Boolean,
    onVoiceClick: () -> Unit, onResetDemo: () -> Unit, onPollSpeech: () -> Unit) {
    if (screen.isDriving && designWide) {
        DesignedDriveScreen(s, screen, enabled, setupRestricted, voice, onVoiceClick,
            onResetDemo, onPollSpeech)
        return
    }
    if (screen == WireScreen.KNOCK_FULL) {
        WireKnockFull(s, enabled)
        return
    }
    Row(Modifier.fillMaxWidth().height(76.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(Modifier.weight(1f)) {
            Text("목적지까지 함께해요", style = MaterialTheme.typography.titleLarge, color = Wire.ink)
            WireSub("순위는 안전 운전 점수로 정해져요")
        }
        WireChip("⚑  도착 15:40 · ${(82 - (LocalConvoy.current.distanceMeters / 1_000).toInt()).coerceAtLeast(0)}km")
        Spacer(Modifier.weight(.5f))
        WireButton("설정", { s.showSettings = true }, filled = false,
            enabled = enabled && !setupRestricted, width = 120.dp, height = 60.dp)
        WireButton("동행 종료", { s.showEnd = true }, enabled = enabled && !setupRestricted,
            width = 170.dp, height = 60.dp)
    }
    Spacer(Modifier.height(12.dp))
    WireTrack(s, screen == WireScreen.MAP, enabled, onResetDemo)
    Spacer(Modifier.height(16.dp))
    if (wide) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (s.showVoiceRecorder) {
                DesignedRoadVoiceRecorderCard(Modifier.weight(1.9f).fillMaxHeight()) {
                    s.showVoiceRecorder = false
                    s.message = "녹음 화면 시연을 마쳤어요. 음성 파일은 저장되지 않았어요."
                }
            } else WireFeed(s, screen, enabled, setupRestricted,
                Modifier.weight(1.9f).fillMaxHeight(), voice, onVoiceClick)
            WireCars(s, screen, enabled, Modifier.weight(1f).fillMaxHeight())
        }
    } else {
        Column(Modifier.weight(1f)) {
            if (s.showVoiceRecorder) {
                DesignedRoadVoiceRecorderCard(Modifier.weight(1f).fillMaxWidth()) {
                    s.showVoiceRecorder = false
                    s.message = "녹음 화면 시연을 마쳤어요. 음성 파일은 저장되지 않았어요."
                }
            } else WireFeed(s, screen, enabled, setupRestricted,
                Modifier.weight(1f).fillMaxWidth(), voice, onVoiceClick)
            WireCars(s, screen, enabled, Modifier.height(180.dp).fillMaxWidth())
        }
    }
    Spacer(Modifier.height(12.dp))
    WireDj(s, screen == WireScreen.DJ, enabled)
}

@Composable
private fun WireTrack(s: WireSession, expanded: Boolean, enabled: Boolean,
    onResetDemo: () -> Unit) {
    Surface(Modifier.fillMaxWidth().height(if (expanded) 550.dp else 188.dp),
        color = Wire.muted, shape = RoundedCornerShape(Pleos215.RadiusLarge)) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                WireButton("처음으로", onResetDemo, filled = false, enabled = enabled,
                    width = 150.dp, height = Pleos215.ButtonSmall)
                Box(Modifier.weight(1f).clickable(enabled) {
                    if (expanded) s.drive() else s.open(WireScreen.MAP)
                }.padding(horizontal = 12.dp)) {
                    WireSub(if (expanded) "동행 지도 접기" else "동행 지도 펼치기")
                }
                WireChip(if (s.roadVoiceShown) "길에 남긴 음성 1개"
                    else if (s.waypoint) "경유지 · 투표 완료" else "경유지 · 투표 현황")
            }
            Spacer(Modifier.height(12.dp))
            if (expanded) {
                Surface(Modifier.fillMaxWidth().weight(1f), color = Wire.surface,
                    shape = RoundedCornerShape(Pleos215.RadiusMedium)) {
                    RoadConvoyMap(overview = true, cameraRevision = 0, enabled = enabled,
                        modifier = Modifier.fillMaxSize())
                }
            } else {
                BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxWidth().height(54.dp)) {
                        val y = size.height / 2f
                        drawLine(Wire.mutedStrong, Offset(10.dp.toPx(), y),
                            Offset(size.width - 10.dp.toPx(), y), 42.dp.toPx(), StrokeCap.Round)
                        drawLine(Wire.action.copy(alpha = .35f), Offset(10.dp.toPx(), y),
                            Offset(10.dp.toPx() + (size.width - 20.dp.toPx()) * s.progress, y),
                            5.dp.toPx(), StrokeCap.Round)
                    }
                    val people = listOf("내 차", "B", "C")
                    people.forEachIndexed { index, name ->
                        val p = (s.progress + when(index) { 0 -> 0f; 1 -> .25f; else -> .45f }).coerceIn(0f, .92f)
                        val x = (maxWidth - 120.dp) * p
                        Column(Modifier.offset(x = x - maxWidth / 2f + 60.dp).width(120.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            WireChip(name, selected = true)
                            Text("${index + 1}위 · ${100 - index * 4}점",
                                style = MaterialTheme.typography.labelSmall, color = Wire.secondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WireFeed(s: WireSession, screen: WireScreen, enabled: Boolean,
    setupRestricted: Boolean, modifier: Modifier,
    voice: LinkCarVoiceRuntime, onVoiceClick: () -> Unit) {
    val compact = screen == WireScreen.MAP
    Column(modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            WireSub("M2 소통 · 결정")
            if (!compact && screen in listOf(WireScreen.DRIVE, WireScreen.DJ, WireScreen.VOICE_APPROACH)) {
                WireChip("이전 대화", onClick = if (enabled && !setupRestricted) {
                    { s.showHistory = true }
                } else null)
            }
        }
        Spacer(Modifier.height(if (compact) 12.dp else 26.dp))
        when (screen) {
            WireScreen.INCOMING_CALL -> {
                WireSub("${s.incomingCaller} · 전화 수신 시연")
                Spacer(Modifier.height(18.dp))
                WireTitle("${s.incomingCaller}에서 전화가 왔어요")
                WireSub(if (s.incomingCaller == "문준 차")
                    "${s.seconds}초 후 자동으로 거절돼요" else "${s.seconds}초 안에 전화 받기를 눌러 주세요")
                Spacer(Modifier.height(22.dp))
                WireButton(if (s.incomingCaller == "다연 차") "전화 받기" else "전화 거절",
                    if (s.incomingCaller == "다연 차") s::acceptIncomingCall
                    else s::declineIncomingCall, enabled = enabled,
                    modifier = Modifier.fillMaxWidth())
            }
            WireScreen.CALL_DECLINED -> {
                WireTitle("문준 차의 전화를 거절했어요")
                WireSub("잠시 후 주행 화면으로 돌아가요 · 시연")
            }
            WireScreen.KNOCK_SENT -> {
                WireSub("${s.knockTarget}  ·  노크 보내는 중")
                Spacer(Modifier.height(18.dp))
                WireTitle("${s.knockTarget}를 노크했어요")
                WireSub("${s.knockTarget}가 문을 열면 30초 동안 바로 대화할 수 있어요")
                Spacer(Modifier.height(22.dp))
                LinearProgressIndicator(progress = { (10 - s.seconds) / 10f },
                    modifier = Modifier.fillMaxWidth(), color = Wire.action, trackColor = Wire.mutedStrong)
                WireSub(if (s.seconds > 0) "${s.seconds}초" else "응답이 없어요")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    WireButton(if (s.seconds > 0) "취소" else "닫기", { s.drive() },
                        filled = false, enabled = enabled, modifier = Modifier.weight(1f))
                    WireButton(if (s.seconds > 0) "응답 시연" else "음성 메시지 남기기",
                        { if (s.seconds > 0) s.startKnockCall() else s.open(WireScreen.VOICE_LEAVE) },
                        enabled = enabled, modifier = Modifier.weight(1.4f))
                }
            }
            WireScreen.KNOCK_RECEIVED -> {
                WireSub("다연 차  ·  주행 중")
                Spacer(Modifier.height(18.dp))
                WireTitle("다연 차가 노크했어요", Modifier.clickable(enabled) { s.open(WireScreen.KNOCK_FULL) })
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    WireButton("지금은 어려워요", { s.drive() }, filled = false,
                        enabled = enabled, modifier = Modifier.weight(1f))
                    WireButton("문 열고 대화하기", s::startKnockCall,
                        enabled = enabled, modifier = Modifier.weight(1.6f))
                }
                Spacer(Modifier.height(12.dp))
                WireSub("말로 \"열어\"라고 해도 돼요 · ${s.seconds}초가 지나면 자동으로 닫혀요")
            }
            WireScreen.CALL -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${s.seconds}", style = MaterialTheme.typography.displayLarge, color = Wire.action)
                    Spacer(Modifier.width(22.dp))
                    Column {
                        WireTitle("${s.incomingCaller}와 대화 중 · 시연")
                        WireSub("30초가 지나면 자동으로 끝나요")
                    }
                }
                Spacer(Modifier.height(25.dp))
                WireButton("대화 끝내기", { s.drive() }, enabled = enabled,
                    modifier = Modifier.fillMaxWidth())
            }
            WireScreen.VOICE_LEAVE -> {
                WireSub("${s.knockTarget}  ·  휴게소 정차 중")
                Spacer(Modifier.height(18.dp))
                WireTitle("${s.knockTarget}가 지금은 문을 못 열어요")
                WireSub("원본 목소리를 녹음해 두면 시연 차량이 해당 지점을 지날 때 들려요")
                Spacer(Modifier.height(22.dp))
                WireButton("음성 메시지 남기기", {
                    onVoiceClick()
                }, enabled = enabled, modifier = Modifier.fillMaxWidth())
            }
            WireScreen.VOICE_RECEIVED -> {
                WireSub("내 차 · 방금 · 로컬 음성 메시지")
                Spacer(Modifier.height(18.dp))
                WireTitle("길에 원본 목소리를 남겼어요")
                Spacer(Modifier.height(20.dp))
                WireButton("목소리로 듣기", {
                    voice.notes.notes.lastOrNull { it.id == s.lastNoteId }?.let { voice.play(it) }
                        ?: run { s.message = "재생할 원본 음성이 없습니다." }
                }, filled = false, enabled = enabled, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                WireSub("이 메시지는 로컬 시연 차량에서만 재생됩니다")
            }
            WireScreen.VOICE_APPROACH -> {
                WireSub("문준 차 · 3분 전  ·  읽어 주는 중")
                Spacer(Modifier.height(18.dp))
                WireTitle("이 노래 누가 신청했어? 좋다")
                WireSub("다연 차가 길에 남긴 음성 · 1.2km 앞")
            }
            WireScreen.VOICE_DELIVER -> {
                WireSub("로컬 시연 차량 · 녹음 지점 통과")
                Spacer(Modifier.height(12.dp))
                WireTitle("녹음된 원본 목소리가 재생되고 있어요")
                Spacer(Modifier.height(12.dp))
                WireChip("저장된 지점 통과 · 로컬 시연")
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    WireButton("닫기", { s.drive() }, filled = false,
                        enabled = enabled, modifier = Modifier.weight(1f))
                    WireButton("다시 듣기", {
                        voice.notes.notes.lastOrNull { it.id == s.lastNoteId }?.let { voice.play(it) }
                    },
                        enabled = enabled, modifier = Modifier.weight(1.4f))
                }
            }
            else -> {
                WireSub(if (s.generatedPoll != null) "내 차 · AI가 만든 투표 · 로컬 시연"
                    else "다연 차 · 방금 · 읽어 주는 중")
                Spacer(Modifier.height(if (compact) 6.dp else 18.dp))
                val poll = s.generatedPoll
                if (poll != null) {
                    WireTitle(poll.title)
                    Spacer(Modifier.height(if (compact) 8.dp else 20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        poll.options.forEach { option ->
                            WireButton(option, { s.vote = option }, filled = s.vote == option,
                                enabled = enabled, modifier = Modifier.weight(1f), height = 88.dp)
                        }
                    }
                    if (s.vote.isNotEmpty()) WireSub("선택: ${s.vote} · 로컬 시연 투표")
                } else if (s.demoVoiceTriggered && !s.roadVoiceShown && screen == WireScreen.DRIVE) {
                    WireTitle("200m 음성 지점에 도착했어요")
                    Spacer(Modifier.height(16.dp))
                    WireSub("버튼을 눌러 이곳에 원본 목소리를 남겨보세요 · 한 기기 시연")
                    Spacer(Modifier.height(20.dp))
                    WireButton("이곳에 음성 남기기", onVoiceClick, enabled = enabled,
                        modifier = Modifier.fillMaxWidth())
                } else {
                    WireTitle(if (s.vote == "accepted") "좋아요, 휴게소에서 만나요"
                        else if (s.vote == "rejected") "다음에 들를게요"
                        else "휴게소 들렀다 가자")
                    Spacer(Modifier.height(if (compact) 8.dp else 20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        WireButton("다음에", { s.vote = "rejected" }, filled = false,
                            enabled = enabled, modifier = Modifier.weight(1f), height = 88.dp)
                        WireButton("좋아", { s.vote = "accepted"; s.waypoint = true },
                            enabled = enabled, modifier = Modifier.weight(1.4f), height = 88.dp)
                    }
                }
                if (!compact) {
                    Spacer(Modifier.height(10.dp))
                    WireSub(if (s.generatedPoll != null) "선택 결과는 이 기기에서만 표시됩니다"
                        else "제안일 때만 버튼이 함께 나타남")
                }
            }
        }
        if (s.message.isNotBlank()) WireSub(s.message)
        if (voice.status != "마이크를 누르면 투표 제안이나 길 위 음성을 남길 수 있어요.") {
            Spacer(Modifier.height(8.dp))
            WireSub(voice.status)
        }
        if (voice.status.contains("STT") || voice.status.contains("음성 인식 오류")) {
            Spacer(Modifier.height(8.dp))
            WireButton("텍스트로 AI 투표 시험", { s.showPollTextEntry = true }, filled = false,
                enabled = enabled && !setupRestricted,
                modifier = Modifier.fillMaxWidth(), height = 64.dp)
        }
        Spacer(Modifier.weight(1f))
        if (!compact && screen != WireScreen.CALL) {
            Surface(Modifier.fillMaxWidth().height(92.dp).clickable(enabled) {
                onVoiceClick()
            }, color = Wire.muted, shape = RoundedCornerShape(Pleos215.RadiusLarge)) {
                Row(Modifier.padding(horizontal = 26.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("모두에게 말하기", style = MaterialTheme.typography.titleMedium, color = Wire.secondary)
                    Box(Modifier.size(64.dp).background(Wire.action, CircleShape),
                        contentAlignment = Alignment.Center) {
                        WireMic(Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun WireMic(modifier: Modifier) {
    Canvas(modifier) {
        val stroke = Stroke(size.width * 3.6f / 48f, cap = StrokeCap.Round)
        fun point(x: Float, y: Float) = Offset(size.width * x, size.height * y)
        drawRoundRect(Color.White, point(.35f, .08f),
            Size(size.width * .3f, size.height * .5f), CornerRadius(size.width * .15f), style = stroke)
        drawArc(Color.White, 0f, 180f, false, point(.2f, .25f),
            Size(size.width * .6f, size.height * .5f), style = stroke)
        drawLine(Color.White, point(.5f, .75f), point(.5f, .93f), stroke.width, StrokeCap.Round)
        drawLine(Color.White, point(.32f, .94f), point(.68f, .94f), stroke.width, StrokeCap.Round)
    }
}

@Composable
private fun WireCars(s: WireSession, screen: WireScreen, enabled: Boolean, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf("다연 차", "문준 차").forEachIndexed { index, name ->
            Surface(Modifier.fillMaxWidth().weight(1f), color = Wire.muted,
                shape = RoundedCornerShape(Pleos215.RadiusLarge),
                border = if (screen == WireScreen.VOICE_DELIVER && index == 0)
                    BorderStroke(2.dp, Wire.action) else null) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(54.dp, 76.dp).background(Wire.mutedStrong,
                        RoundedCornerShape(Pleos215.RadiusSmall)))
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleLarge, color = Wire.ink)
                        WireSub(if (screen == WireScreen.VOICE_DELIVER && index == 0)
                            "음성 남김 · 12분 전" else "순위 · 점수")
                    }
                    WireButton(when {
                        screen == WireScreen.KNOCK_SENT && name == s.knockTarget -> "응답 기다리는 중"
                        screen == WireScreen.CALL && name == "다연 차" -> "대화 중"
                        screen == WireScreen.VOICE_LEAVE && name == s.knockTarget -> "1분 뒤 다시 노크"
                        else -> "두드리기"
                    }, {
                        s.sendKnock(name)
                    }, enabled = enabled && screen !in setOf(WireScreen.KNOCK_SENT, WireScreen.CALL),
                        width = 176.dp, height = 84.dp)
                }
            }
        }
        Surface(Modifier.fillMaxWidth().weight(1f), color = Wire.muted,
            shape = RoundedCornerShape(Pleos215.RadiusLarge)) {
            Box(contentAlignment = Alignment.Center) { WireSub("빈 슬롯 · 최대 3대") }
        }
    }
}

@Composable
private fun WireDj(s: WireSession, expanded: Boolean, enabled: Boolean) {
    Surface(Modifier.fillMaxWidth().height(if (expanded) 360.dp else 164.dp),
        color = Wire.muted, shape = RoundedCornerShape(Pleos215.RadiusLarge)) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            Box(Modifier.size(if (expanded) 260.dp else 112.dp)
                .background(Wire.mutedStrong, RoundedCornerShape(Pleos215.RadiusMedium))
                .clickable(enabled) { if (expanded) s.drive() else s.open(WireScreen.DJ) },
                contentAlignment = Alignment.Center) { WireSub("앨범 재킷") }
            Column(Modifier.weight(1f).clickable(enabled) {
                if (expanded) s.drive() else s.open(WireScreen.DJ)
            }) {
                WireSub(if (expanded) "M3 함께 듣는 DJ · 다시 탭하면 원래 크기" else "M3 함께 듣는 DJ")
                Text(if (s.music) listOf("밤편지", "여행", "좋은 밤")[s.song] else "재생할 곡 없음",
                    style = MaterialTheme.typography.titleLarge, color = Wire.ink)
                WireSub(if (s.music) "아이유 · 문준님이 골랐어요" else "음악 없이 출발했어요")
            }
            LinearProgressIndicator(progress = { s.musicProgress }, modifier = Modifier.width(200.dp),
                color = Wire.action, trackColor = Wire.mutedStrong)
            WireButton("◀", { s.skipSong(-1) }, filled = false,
                enabled = enabled && s.music, width = 70.dp, height = 70.dp)
            WireButton(if (s.playing) "Ⅱ" else "▶", { s.playing = !s.playing },
                enabled = enabled && s.music, width = 82.dp, height = 82.dp)
            WireButton("▶", { s.skipSong(1) }, filled = false,
                enabled = enabled && s.music, width = 70.dp, height = 70.dp)
            WireChip("다음 곡 · 신청한 차")
        }
    }
}

@Composable
private fun WireKnockFull(s: WireSession, enabled: Boolean) {
    Row(Modifier.fillMaxWidth().height(76.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("목적지까지 함께해요", Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge, color = Wire.ink)
        WireChip("도착 15:40 · 32km")
    }
    Spacer(Modifier.height(16.dp))
    WirePanel(Modifier.fillMaxSize()) {
        Spacer(Modifier.weight(1f))
        Text("${s.seconds}초", Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.displayLarge, color = Wire.action)
        Spacer(Modifier.height(20.dp))
        WireAvatar("다연", 160.dp, Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(16.dp))
        WireSub("다연  ·  다연 차 · 쏘렌토  ·  혼자 운전  ·  주행 중",
            Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(18.dp))
        WireTitle("노크 왔어요", Modifier.align(Alignment.CenterHorizontally))
        WireSub("다연님이 노크했어요 · 문을 열면 30초 동안 바로 이야기할 수 있어요",
            Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(25.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            WireButton("문 열고 대화하기", s::startKnockCall,
                enabled = enabled, modifier = Modifier.weight(1.7f))
            WireButton("지금은 어려워요", { s.drive() }, filled = false,
                enabled = enabled, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        WireSub("말로 \"열어\" 또는 \"나중에\"라고 해도 돼요 · 10초 지나면 자동으로 닫혀요",
            Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.weight(1f))
    }
}
