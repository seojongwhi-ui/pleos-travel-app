package com.contest.travel.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun ColumnScope.WireEnding(s: WireSession, screen: WireScreen, wide: Boolean, enabled: Boolean) {
    when (screen) {
        WireScreen.ARRIVED -> WireArrived(s, enabled)
        WireScreen.SUMMARY -> WireSummary(s, wide, enabled)
        WireScreen.SAVE -> WireSave(s, enabled)
        else -> Unit
    }
}

@Composable
private fun ColumnScope.WireArrived(s: WireSession, enabled: Boolean) {
    WireHeader("${s.destination} 도착!", "15:52 도착 · 내 차가 마지막으로 도착했어요")
    Spacer(Modifier.height(16.dp))
    WirePanel(Modifier.fillMaxWidth().weight(1f)) {
        Spacer(Modifier.weight(1f))
        Row(Modifier.align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy((-12).dp)) {
            listOf("민지", "도윤", "나").forEach { WireAvatar(it, 140.dp) }
        }
        Spacer(Modifier.height(26.dp))
        WireTitle("모두 도착했어요!", Modifier.align(Alignment.CenterHorizontally))
        WireSub("마지막 차가 도착해서 여행 기록이 완성됐어요",
            Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(25.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(Triple("1", "민지", "15:31 도착"),
                Triple("2", "도윤", "15:44 도착"), Triple("3", "나", "15:52 · 방금")).forEach { (rank, name, time) ->
                Surface(Modifier.weight(1f), color = Wire.muted,
                    shape = RoundedCornerShape(Pleos215.RadiusMedium)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        WireChip(rank)
                        WireAvatar(name, 56.dp)
                        Column {
                            Text(name, style = MaterialTheme.typography.titleMedium, color = Wire.ink)
                            WireSub(time)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        WireSub("도착 순서예요 · 순위는 안전 운전 점수로 정해져요",
            Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.weight(1f))
    }
    Spacer(Modifier.height(16.dp))
    WireButton("여행 요약 보기", { s.open(WireScreen.SUMMARY) },
        enabled = enabled, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun ColumnScope.WireSummary(s: WireSession, wide: Boolean, enabled: Boolean) {
    WireHeader("함께 온 길", "${s.destination} 가는 길 · 11월 5일 · ${s.invitees.size + 1}대",
        "① 요약  —  ② 저장", s::back)
    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        WirePanel(Modifier.weight(1.6f).fillMaxHeight()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("안전 운전 순위", style = MaterialTheme.typography.titleLarge, color = Wire.ink)
                WireSub("100점에서 감점")
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth().height(350.dp), horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Bottom) {
                listOf(Triple("2", "민지", "92점"), Triple("1", "도윤", "96점"),
                    Triple("3", "나", "85점")).forEach { (rank, name, score) ->
                    val height = when (rank) { "1" -> 270.dp; "2" -> 220.dp; else -> 190.dp }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        WireAvatar(name, 80.dp)
                        Text("$name · $score", style = MaterialTheme.typography.titleMedium, color = Wire.ink)
                        Surface(Modifier.fillMaxWidth().height(height), color = Wire.muted,
                            shape = RoundedCornerShape(Pleos215.RadiusMedium)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(rank, style = MaterialTheme.typography.displayLarge, color = Wire.action)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            WireSub("차                 주행 거리               걸린 시간                 감점")
            Spacer(Modifier.height(10.dp))
            listOf("도윤  ·  82.4km  ·  1시간 28분  ·  급제동 1 · 급가속 1",
                "민지  ·  81.9km  ·  1시간 15분  ·  급제동 1 · 과속 1",
                "나  ·  83.1km  ·  1시간 36분  ·  급제동 2 · 과속 2").forEach {
                WireRow(it, "", Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }
        }
        WirePanel(Modifier.weight(1f).fillMaxHeight()) {
            Text("AI 하이라이트", style = MaterialTheme.typography.titleLarge, color = Wire.ink)
            Spacer(Modifier.height(16.dp))
            listOf("\"호두과자 먹자\" 3:0 만장일치로 청평휴게소 들름",
                "민지님이 길에 남긴 음성 \"음료수 좀 사오기~\"",
                "가장 많이 함께 부른 곡 · 밤편지").forEach {
                WireRow(it, "", Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WireChip("대화 24"); WireChip("노크 6"); WireChip("남긴 음성 1")
            }
            Spacer(Modifier.height(8.dp))
            WireChip("함께 들은 곡 18")
            Spacer(Modifier.weight(1f))
            WireRow("내기 결과", if (s.wager) "꼴찌가 커피 사기 → 승호님" else "선택한 내기 없음",
                Modifier.fillMaxWidth())
        }
    }
    Spacer(Modifier.height(16.dp))
    WireButton("다음 · 저장하기", { s.open(WireScreen.SAVE) },
        enabled = enabled, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun ColumnScope.WireSave(s: WireSession, enabled: Boolean) {
    WireHeader("이번 여행을 저장할까요?", "각자 정해요 · 다른 사람의 선택과 상관없어요",
        "✓ 요약  —  ② 저장", s::back)
    Spacer(Modifier.height(16.dp))
    WirePanel(Modifier.fillMaxWidth().weight(1f)) {
        Spacer(Modifier.weight(1f))
        Row(Modifier.align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WireChip("저장하면")
            Text("지난 여행에 이렇게 남아요", style = MaterialTheme.typography.titleMedium, color = Wire.ink)
        }
        Spacer(Modifier.height(24.dp))
        Surface(Modifier.fillMaxWidth(.72f).align(Alignment.CenterHorizontally),
            color = Wire.muted, shape = RoundedCornerShape(Pleos215.RadiusLarge)) {
            Column(Modifier.padding(40.dp)) {
                WireTitle("${s.destination} 가는 길")
                WireSub("${s.purpose} · 11월 5일 · 83.1km · 1시간 36분")
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    listOf("민지", "도윤", "나").forEach { WireAvatar(it, 56.dp) }
                    Spacer(Modifier.width(16.dp))
                    Text("민지 · 도윤 · 나", style = MaterialTheme.typography.labelMedium, color = Wire.ink)
                    Spacer(Modifier.width(16.dp))
                    WireChip("내 순위 3위 · 85점")
                }
                Spacer(Modifier.height(18.dp))
                WireRow("AI 하이라이트 · \"호두과자 3:0 만장일치\" 외 2개", "",
                    Modifier.fillMaxWidth())
            }
        }
        Spacer(Modifier.height(22.dp))
        WireSub("다시 가기 · 홈에서 같은 멤버로 초대 없이 바로 다시 모여요",
            Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.weight(1f))
    }
    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        WireButton("저장 안 함", { s.saved = false; s.home() }, filled = false,
            enabled = enabled, modifier = Modifier.weight(1f))
        WireButton("여행 저장하기", { s.saved = true; s.home() },
            enabled = enabled, modifier = Modifier.weight(2.8f))
    }
}
