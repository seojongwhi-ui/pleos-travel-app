package com.contest.travel.preview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun WireInvite(s: WireSession, enabled: Boolean) {
    WireHeader("${s.destination} 가는 길", "${s.purpose} · 그룹이 만들어졌어요", "2 / 2", s::back)
    Spacer(Modifier.height(16.dp))
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        WirePanel(Modifier.fillMaxWidth(.72f).fillMaxHeight(), padding = 40.dp) {
            Box(Modifier.size(88.dp).background(Wire.muted, CircleShape)
                .align(Alignment.CenterHorizontally), contentAlignment = Alignment.Center) {
                Text("↗", style = MaterialTheme.typography.displaySmall, color = Wire.action)
            }
            Spacer(Modifier.height(20.dp))
            WireTitle("함께 갈 차를 초대하세요", Modifier.align(Alignment.CenterHorizontally))
            WireSub("앱을 쓰는 사람만 초대할 수 있어요 · 내 차 포함 최대 4대",
                Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(28.dp))
            WireInput(s.phone, { s.phone = it }, "전화번호 입력", Modifier.fillMaxWidth(), enabled)
            Spacer(Modifier.height(24.dp))
            WireSub("최근 함께한 사람")
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("다연", "문준", "성윤", "민석", "현진", "준호").forEach { name ->
                    val chosen = name in s.invitees
                    Column(Modifier.clickable(enabled) {
                        s.invitees = if (chosen) s.invitees - name
                            else if (s.invitees.size < 3) s.invitees + name else s.invitees
                    }, horizontalAlignment = Alignment.CenterHorizontally) {
                        WireAvatar(name, 74.dp)
                        Text(name, style = MaterialTheme.typography.labelMedium, color = Wire.ink)
                        Text(if (chosen) "선택됨" else "앱 사용 중",
                            style = MaterialTheme.typography.labelSmall, color = Wire.secondary)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            WireSub("선택한 차   ${s.invitees.joinToString(", ")} · ${s.invitees.size}대")
            Spacer(Modifier.height(12.dp))
            WireButton("초대 보내기 (${s.invitees.size})", {
                if (s.phone.isNotBlank() && s.invitees.size < 3) s.invitees += s.phone
                s.open(WireScreen.WAITING)
            }, enabled = enabled && (s.invitees.isNotEmpty() || s.phone.isNotBlank()),
                modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun ColumnScope.WireWaiting(s: WireSession, wide: Boolean, enabled: Boolean) {
    WireHeader("${s.destination} 가는 길", "${s.purpose} · 도착 예정 15:40", back = s::back)
    Spacer(Modifier.height(16.dp))
    WirePanel(Modifier.fillMaxWidth().weight(.59f)) {
        Spacer(Modifier.weight(1f))
        Row(Modifier.align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy((-12).dp), verticalAlignment = Alignment.CenterVertically) {
            (listOf("나") + s.invitees.take(2)).forEach { WireAvatar(it, 104.dp) }
            Box(Modifier.size(76.dp).background(Wire.muted, CircleShape),
                contentAlignment = Alignment.Center) {
                Text("+", style = MaterialTheme.typography.headlineLarge, color = Wire.secondary)
            }
        }
        Spacer(Modifier.height(18.dp))
        WireTitle("${s.invitees.take(2).joinToString(", ")}이 함께해요!",
            Modifier.align(Alignment.CenterHorizontally))
        WireSub("1자리 남았어요 · 지금 바로 출발해도 돼요",
            Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(26.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            WireButton("바로 출발", { s.music = false; s.open(WireScreen.DRIVE) }, filled = false,
                enabled = enabled, modifier = Modifier.weight(1f))
            WireButton("출발 준비하기", { s.open(WireScreen.MISSION) }, enabled = enabled,
                modifier = Modifier.weight(1.5f))
        }
        Spacer(Modifier.height(12.dp))
        WireSub("미션 → 음악 → 출발 · 미션과 음악은 건너뛸 수 있어요",
            Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.weight(1f))
    }
    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth().weight(.41f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf("나", "다연", "문준", "초대하기").forEachIndexed { index, name ->
            Surface(Modifier.weight(1f).fillMaxHeight().then(
                if (index == 3) Modifier.clickable(enabled) { s.open(WireScreen.INVITE) } else Modifier),
                color = if (index == 3) Wire.muted else Wire.surface,
                shape = RoundedCornerShape(Pleos215.RadiusLarge),
                border = if (index == 1) BorderStroke(2.dp, Wire.teal) else null) {
                Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    if (index == 3) {
                        Text("+", style = MaterialTheme.typography.displayLarge, color = Wire.secondary)
                        Text("초대하기", style = MaterialTheme.typography.titleLarge, color = Wire.ink)
                        WireSub("1자리 남음")
                    } else {
                        WireAvatar(name, 78.dp)
                        Spacer(Modifier.height(8.dp))
                        Text(name, style = MaterialTheme.typography.titleLarge, color = Wire.ink)
                        WireSub(when (index) {
                            0 -> "내 차 · 아이오닉 5"
                            1 -> "다연 차 · 쏘렌토"
                            else -> "문준 차 · 아반떼"
                        })
                        Spacer(Modifier.height(6.dp))
                        WireChip(when (index) {
                            0 -> "동승자 있음"
                            1 -> "혼자 운전"
                            else -> "탑승 전"
                        })
                    }
                }
            }
        }
    }
}

@Composable
internal fun ColumnScope.WireMission(s: WireSession, wide: Boolean, enabled: Boolean) {
    WireHeader("출발 준비", "${s.destination} 가는 길 · ${s.invitees.size + 1}대",
        "① 미션  —  ② 음악 · 출발", s::back)
    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        WirePanel(Modifier.weight(1.4f).fillMaxHeight()) {
            WireSub("오늘의 미션")
            Spacer(Modifier.height(10.dp))
            WireTitle("안전하게 온 차가 1등!")
            WireSub("모든 차가 100점에서 시작해요. 도착할 때까지 아래 행동마다 점수가 깎여요")
            Spacer(Modifier.height(24.dp))
            listOf("급가속" to "-3점", "급제동" to "-3점", "과속" to "-5점").forEach { (name, score) ->
                Surface(Modifier.fillMaxWidth().padding(bottom = 12.dp), color = Wire.muted,
                    shape = RoundedCornerShape(Pleos215.RadiusMedium)) {
                    Row(Modifier.padding(22.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(name, style = MaterialTheme.typography.titleMedium, color = Wire.ink)
                        Text(score, style = MaterialTheme.typography.titleMedium, color = Wire.action)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            WireSub("점수는 차량 주행 데이터로 자동 계산돼요 · 현재 점수는 시연 데이터예요")
        }
        WirePanel(Modifier.weight(1f).fillMaxHeight()) {
            Text("참가하는 차", style = MaterialTheme.typography.titleLarge, color = Wire.ink)
            Spacer(Modifier.height(14.dp))
            (listOf("나") + s.invitees.take(2)).forEach { name ->
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).background(Wire.muted,
                    RoundedCornerShape(Pleos215.RadiusMedium)).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    WireAvatar(name, 62.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = Wire.ink)
                    Text("100점", style = MaterialTheme.typography.titleMedium, color = Wire.ink)
                }
            }
            Spacer(Modifier.weight(1f))
            WireSub("내기 걸기 (선택)")
            Spacer(Modifier.height(8.dp))
            WireChip(if (s.wager) "꼴찌가 커피 사기" else "\"꼴찌가 커피 사기\"처럼 말해 보세요",
                onClick = if (enabled) { { s.wager = !s.wager } } else null)
        }
    }
    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        WireButton("미션 없이 다음", { s.mission = false; s.open(WireScreen.MUSIC_PICK) },
            filled = false, enabled = enabled, modifier = Modifier.weight(1f))
        WireButton("이 미션으로 다음", { s.mission = true; s.open(WireScreen.MUSIC_PICK) },
            enabled = enabled, modifier = Modifier.weight(2.5f))
    }
}

@Composable
internal fun ColumnScope.WireMusicPick(s: WireSession, wide: Boolean, enabled: Boolean) {
    WireHeader("출발 준비", "${s.destination} 가는 길 · ${s.invitees.size + 1}대",
        "✓  —  ② 음악 · 출발", s::back)
    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        WirePanel(Modifier.weight(1.1f).fillMaxHeight()) {
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(260.dp).background(Wire.muted, CircleShape)
                .align(Alignment.CenterHorizontally), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    WireAvatar("문준", 86.dp)
                    Text("당첨!  AI 무작위", style = MaterialTheme.typography.labelMedium, color = Wire.ink)
                }
            }
            Spacer(Modifier.height(22.dp))
            WireTitle("문준님이 첫 곡을 골랐어요!", Modifier.align(Alignment.CenterHorizontally))
            WireSub("뽑힌 차가 말로 신청하면 모든 차에서 함께 재생돼요",
                Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.weight(1f))
        }
        WirePanel(Modifier.weight(.9f).fillMaxHeight()) {
            WireSub("첫 곡")
            Spacer(Modifier.height(12.dp))
            Box(Modifier.size(230.dp).background(Wire.mutedStrong,
                RoundedCornerShape(Pleos215.RadiusMedium)), contentAlignment = Alignment.Center) {
                WireSub("앨범 재킷")
            }
            Spacer(Modifier.height(16.dp))
            WireTitle("밤편지")
            WireSub("아이유 · 문준님이 골랐어요")
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(progress = { .18f }, modifier = Modifier.fillMaxWidth(),
                color = Wire.action, trackColor = Wire.muted)
            Spacer(Modifier.weight(1f))
            WireSub("함께 듣는 차   나 · 다연 · 문준   3대")
        }
    }
    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        WireButton("음악 없이 출발", { s.music = false; s.open(WireScreen.DRIVE) },
            filled = false, enabled = enabled, modifier = Modifier.weight(1f))
        WireButton("출발하기", { s.music = true; s.open(WireScreen.FIRST_SONG) },
            enabled = enabled, modifier = Modifier.weight(2.5f))
    }
}

@Composable
internal fun WireFirstSong(s: WireSession, wide: Boolean, enabled: Boolean) {
    WireHeader("음악과 함께 출발해요", "내비 안내를 시작하면 주행 화면으로 넘어가요", back = s::back)
    Spacer(Modifier.height(16.dp))
    WirePanel(Modifier.fillMaxSize()) {
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(54.dp)) {
            Box(Modifier.size(340.dp).background(Wire.mutedStrong,
                RoundedCornerShape(Pleos215.RadiusMedium)), contentAlignment = Alignment.Center) {
                WireSub("앨범 재킷")
            }
            Column(Modifier.weight(1f)) {
                WireSub("지금 함께 듣는 첫 곡")
                Spacer(Modifier.height(8.dp))
                WireTitle("밤편지")
                WireSub("아이유 · 문준님이 골랐어요")
                Spacer(Modifier.height(18.dp))
                LinearProgressIndicator(progress = { .1f }, modifier = Modifier.fillMaxWidth(),
                    color = Wire.action, trackColor = Wire.muted)
                WireSub("0:24                                                    4:13")
                Spacer(Modifier.height(22.dp))
                WireSub("함께 듣는 중   3대")
                Spacer(Modifier.height(20.dp))
                WireButton("출발하기", { s.open(WireScreen.DRIVE) },
                    enabled = enabled, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                WireSub("내 차가 경로 안내를 시작하면 자동으로 출발해요")
            }
        }
        Spacer(Modifier.weight(1f))
    }
}
