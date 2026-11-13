package com.contest.travel.preview

import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Figma section 328:4624, screens S-01 through S-05 and S-51.
private object JourneyLook {
    val ink = Color(0xFF191A1B)
    val subtle = Color(0xFFA4A8AE)
    val blue = Color(0xFF5263E7)
    val pale = Color(0xFFEDEEF2)
    val card = Color.White.copy(alpha = .82f)
    val recent = Color(0xFFF3F4F8)
}

internal val DesignedJourneyBackdrop = Brush.linearGradient(listOf(
    Color(0xFFEBEDF2), Color(0xFFD5D7DD)))

@Composable
private fun JTitle(text: String, modifier: Modifier = Modifier) = Text(text, modifier,
    style = MaterialTheme.typography.headlineMedium, color = JourneyLook.ink)

@Composable
private fun JSub(text: String, modifier: Modifier = Modifier) = Text(text, modifier,
    style = MaterialTheme.typography.labelMedium, color = JourneyLook.subtle)

@Composable
private fun JCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, color = JourneyLook.card, shape = RoundedCornerShape(48.dp)) {
        Column(Modifier.fillMaxSize().padding(32.dp), content = content)
    }
}

@Composable
private fun JButton(text: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier,
    primary: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.height(88.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (primary) JourneyLook.ink else JourneyLook.pale,
            contentColor = if (primary) Color.White else JourneyLook.ink),
        contentPadding = PaddingValues(horizontal = 26.dp)) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun JPhoto(id: Int, modifier: Modifier, scale: ContentScale = ContentScale.Crop) {
    Image(painterResource(id), null, modifier.clip(RoundedCornerShape(32.dp)), contentScale = scale)
}

@Composable
private fun JRegionPhoto(id: Int, x: Int, y: Int, width: Int, height: Int, modifier: Modifier) {
    val bitmap = ImageBitmap.imageResource(id)
    Canvas(modifier.clip(RoundedCornerShape(32.dp))) {
        drawImage(bitmap, srcOffset = IntOffset(x, y), srcSize = IntSize(width, height),
            dstSize = IntSize(size.width.toInt(), size.height.toInt()))
    }
}

@Composable
private fun JAvatar(id: Int, name: String, size: Dp = 70.dp) {
    Image(painterResource(id), name, Modifier.size(size).clip(CircleShape), contentScale = ContentScale.Crop)
}

@Composable
private fun JHeader(title: String, detail: String, back: () -> Unit, step: String? = null) {
    Row(Modifier.fillMaxWidth().height(90.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("‹", Modifier.size(60.dp).clickable(onClick = back), textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineLarge, color = JourneyLook.ink)
        Column(Modifier.weight(1f)) { JTitle(title); JSub(detail) }
        if (step != null) JSub(step)
    }
}

@Composable
internal fun ColumnScope.DesignedJourney(s: WireSession, screen: WireScreen, enabled: Boolean) {
    when (screen) {
        WireScreen.HOME -> FigmaHome(s, enabled)
        WireScreen.CREATE, WireScreen.INVITE, WireScreen.WAITING,
        WireScreen.MISSION, WireScreen.MUSIC_PICK, WireScreen.FIRST_SONG,
        WireScreen.SUMMARY -> FigmaJourneyExact(s, screen, enabled)
        else -> Unit
    }
}

@Composable
private fun ColumnScope.DesignedCreate(s: WireSession, enabled: Boolean) {
    JHeader("여행 만들기", "이동 목적과 목적지를 정해 주세요", s::back, "●  ●")
    Spacer(Modifier.height(16.dp))
    JTitle("이동 목적", Modifier.padding(start = 8.dp))
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth().height(300.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf(Triple("친구 여행", "함께 듣는 DJ · 순위 켜짐", 37),
            Triple("가족", "안부 · 도착 알림 중심", 540),
            Triple("업무 · 단체", "알림 최소화", 1010)).forEach { (name, detail, photoX) ->
            Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(32.dp))
                .clickable(enabled) { s.purpose = name }) {
                JRegionPhoto(R.drawable.fig_9037_img, photoX, 195, 460, 250, Modifier.fillMaxSize())
                Column(Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.White)))
                    .padding(start = 20.dp, top = 80.dp, bottom = 16.dp)) {
                    JTitle(name); JSub(detail)
                }
                if (s.purpose == name) Text("✓", Modifier.align(Alignment.TopEnd).padding(18.dp)
                    .background(JourneyLook.blue, CircleShape).padding(horizontal = 12.dp, vertical = 5.dp),
                    color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    JCard(Modifier.fillMaxWidth().weight(1f)) {
        JTitle("목적지")
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                TextField(s.destination, { s.destination = it }, Modifier.fillMaxWidth(),
                    placeholder = { Text("어디로 가나요?") }, singleLine = true,
                    colors = TextFieldDefaults.colors(focusedContainerColor = JourneyLook.pale,
                        unfocusedContainerColor = JourneyLook.pale))
                Spacer(Modifier.weight(1f))
                JButton("초대하기   →", enabled && s.destination.isNotBlank(),
                    { s.open(WireScreen.INVITE) }, Modifier.width(330.dp))
            }
            Spacer(Modifier.width(36.dp))
            Column(Modifier.weight(1f)) {
                JSub("최근 기록")
                listOf("가평 용추계곡" to "경기 가평군 · 82km", "양주 글램핑장" to "경기 양주시 · 45km",
                    "할머니 댁" to "충남 공주시 · 140km").forEach { (name, detail) ->
                    Column(Modifier.fillMaxWidth().clickable(enabled) { s.destination = name }
                        .padding(vertical = 12.dp)) { JTitle(name); JSub(detail) }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.DesignedInvite(s: WireSession, enabled: Boolean) {
    JHeader("${s.destination} 가는 길", "${s.purpose} · 그룹이 만들어졌어요", s::back, "●  ●")
    JCard(Modifier.fillMaxWidth(.9f).weight(1f).align(Alignment.CenterHorizontally)) {
        JTitle("함께 갈 차를 초대하세요")
        JSub("앱을 쓰는 사람만 초대할 수 있어요 · 내 차 포함 최대 4대")
        Spacer(Modifier.height(30.dp))
        TextField(s.phone, { s.phone = it }, Modifier.fillMaxWidth(),
            placeholder = { Text("전화번호 입력") }, singleLine = true)
        Spacer(Modifier.height(44.dp))
        JSub("최근 함께한 사람")
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("다연", "문준", "성윤", "민석", "현진", "준호").forEachIndexed { i, name ->
                val image = listOf(R.drawable.invite_dayeon,R.drawable.invite_munjun,
                    R.drawable.invite_seongyun,R.drawable.invite_minseok,
                    R.drawable.invite_hyeonjin,R.drawable.invite_junho)[i]
                Column(Modifier.clickable(enabled) {
                    s.invitees = if (name in s.invitees) s.invitees - name
                    else if (s.invitees.size < 3) s.invitees + name else s.invitees
                }, horizontalAlignment = Alignment.CenterHorizontally) {
                    JAvatar(image, name, 92.dp)
                    Spacer(Modifier.height(8.dp))
                    JTitle(name)
                    JSub(if (name in s.invitees) "선택됨" else "앱 사용 중")
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            JButton("초대 보내기 (${s.invitees.size})   →", enabled && (s.invitees.isNotEmpty() || s.phone.isNotBlank()), {
                if (s.phone.isNotBlank() && s.invitees.size < 3) s.invitees += s.phone
                s.open(WireScreen.WAITING)
            })
            Spacer(Modifier.weight(1f))
            JSub("선택한 차  ${s.invitees.joinToString(", ")} · ${s.invitees.size}대")
        }
    }
}

@Composable
private fun ColumnScope.DesignedWaiting(s: WireSession, enabled: Boolean) {
    JHeader("${s.destination} 가는 길", "${s.purpose} · 도착 예정 15:40", s::back)
    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        JCard(Modifier.weight(1.5f).fillMaxHeight()) {
            JRegionPhoto(R.drawable.fig_9223_img, 30, 140, 812, 537,
                Modifier.fillMaxWidth().weight(1f))
            Spacer(Modifier.height(10.dp))
            JTitle("${s.invitees.take(2).joinToString(", ")}이 함께해요!")
            JSub("1자리 남았어요 · 지금 바로 출발해도 돼요")
            Spacer(Modifier.height(30.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                JButton("출발 준비하기  →", enabled, { s.open(WireScreen.MISSION) })
                JButton("바로 출발", enabled, { s.music = false; s.open(WireScreen.DRIVE) },
                    primary = false)
            }
        }
        JCard(Modifier.weight(1f).fillMaxHeight()) {
            Row { JTitle("함께 가는 사람", Modifier.weight(1f)); JSub("${s.invitees.size + 1} / 4") }
            Spacer(Modifier.height(24.dp))
            val people = listOf(Triple("나", "아이오닉 5", R.drawable.flow_imgprofilephoto),
                Triple("다연", "쏘렌토", R.drawable.flow_imgprofilephoto2),
                Triple("문준", "아반떼", R.drawable.flow_imgprofilephoto1))
            people.chunked(2).forEach { row ->
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { (name, car, image) ->
                        Surface(Modifier.weight(1f).fillMaxHeight(), color = JourneyLook.pale,
                            shape = RoundedCornerShape(32.dp)) {
                            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center) {
                                JAvatar(image, name, 86.dp); JTitle(name); JSub(car)
                            }
                        }
                    }
                    if (row.size == 1) Surface(Modifier.weight(1f).fillMaxHeight()
                        .clickable(enabled) { s.open(WireScreen.INVITE) }, color = JourneyLook.pale,
                        shape = RoundedCornerShape(32.dp)) {
                        Box(contentAlignment = Alignment.Center) { JTitle("＋ 초대하기") }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ColumnScope.DesignedMission(s: WireSession, enabled: Boolean) {
    JHeader("출발 준비", "${s.destination} 가는 길 · ${s.invitees.size + 1}대", s::back, "●  ●")
    Box(Modifier.fillMaxWidth().height(370.dp).clip(RoundedCornerShape(48.dp))) {
        JPhoto(R.drawable.fig_9335_img, Modifier.fillMaxSize())
        Column(Modifier.align(Alignment.CenterStart).padding(48.dp)) {
            JSub("오늘의 미션"); Spacer(Modifier.height(24.dp))
            JTitle("안전하게 온 차가 1등!"); JSub("모든 차가 100점에서 시작해요.")
        }
    }
    Spacer(Modifier.height(18.dp))
    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        JCard(Modifier.weight(1f).fillMaxHeight()) {
            JTitle("감점 기준")
            listOf("급가속" to "−3점", "급제동" to "−3점", "과속" to "−5점").forEach { (name, points) ->
                Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    JTitle(name, Modifier.weight(1f)); JTitle(points)
                }
            }
        }
        JCard(Modifier.weight(1.2f).fillMaxHeight()) {
            JTitle("참가하는 차")
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf(Triple("나", "아이오닉 5", R.drawable.flow_imgprofilephoto),
                    Triple("다연", "쏘렌토", R.drawable.flow_imgprofilephoto2),
                    Triple("문준", "아반떼", R.drawable.flow_imgprofilephoto1)).forEach { (name, car, image) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        JAvatar(image, name, 100.dp); JTitle(name); JSub(car)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().clickable(enabled) { s.wager = !s.wager }
                .background(JourneyLook.pale, RoundedCornerShape(24.dp)).padding(18.dp)) {
                JTitle("내기 걸기"); Spacer(Modifier.width(18.dp))
                JSub(if (s.wager) "꼴찌가 커피 사기" else "‘꼴찌가 커피 사기’처럼 말해 보세요")
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.weight(1f))
        JButton("미션 없이 다음", enabled, { s.mission = false; s.open(WireScreen.MUSIC_PICK) }, primary = false)
        JButton("이 미션으로 다음  →", enabled, { s.mission = true; s.open(WireScreen.MUSIC_PICK) })
    }
}

@Composable
private fun ColumnScope.DesignedMusicPick(s: WireSession, enabled: Boolean) {
    JHeader("출발 준비", "${s.destination} 가는 길 · ${s.invitees.size + 1}대", s::back, "●  ●")
    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        JCard(Modifier.weight(1f).fillMaxHeight()) {
            JSub("첫 곡 DJ")
            Spacer(Modifier.weight(1f))
            JAvatar(R.drawable.flow_imgprofilephoto1, "문준", 200.dp)
            Spacer(Modifier.weight(1f))
            JTitle("문준님이 첫 곡을 골랐어요")
            JSub("무작위로 뽑힌 차가 첫 곡을 신청해요")
        }
        JCard(Modifier.weight(1f).fillMaxHeight()) {
            JSub("첫 곡")
            Spacer(Modifier.height(16.dp))
            Box(Modifier.size(330.dp)) {
                JPhoto(R.drawable.fig_9457_imgimage50, Modifier.fillMaxSize())
                JPhoto(R.drawable.fig_9457_img9, Modifier.fillMaxSize(.8f).align(Alignment.Center))
            }
            Spacer(Modifier.height(24.dp))
            JTitle("밤편지"); JSub("아이유 · 문준 차 신청")
            Spacer(Modifier.weight(1f))
            JSub("함께 듣는 차     나 · 다연 · 문준    3대")
        }
    }
    Spacer(Modifier.height(14.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        JButton("음악 없이 출발", enabled, { s.music = false; s.open(WireScreen.DRIVE) }, primary = false)
        JButton("출발하기  →", enabled, { s.music = true; s.open(WireScreen.FIRST_SONG) })
    }
}

@Composable
private fun ColumnScope.DesignedFirstSong(s: WireSession, enabled: Boolean) {
    JHeader("음악과 함께 출발해요", "첫 곡을 함께 들으며 출발할 준비를 해요", s::back)
    JCard(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(50.dp)) {
            Box(Modifier.size(460.dp)) {
                JPhoto(R.drawable.fig_9457_imgimage50, Modifier.fillMaxSize())
                JPhoto(R.drawable.fig_9457_img9, Modifier.fillMaxSize(.8f).align(Alignment.Center))
            }
            Column(Modifier.weight(1f)) {
                JSub("첫 곡"); Spacer(Modifier.height(20.dp)); JTitle("밤편지")
                JSub("아이유 · 문준 차 신청"); Spacer(Modifier.height(48.dp))
                JSub("함께 듣는 차 · 3대"); Spacer(Modifier.height(40.dp))
                JButton("출발하기  →", enabled, { s.open(WireScreen.DRIVE) })
            }
        }
    }
}

@Composable
private fun ColumnScope.DesignedSummary(s: WireSession, enabled: Boolean) {
    JHeader("여행 기록", "${s.destination} 가는 길 · 11월 5일 · 3대", s::back, "●  ●")
    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        JCard(Modifier.weight(1f).fillMaxHeight()) {
            JTitle("✦  AI 여행 요약")
            Spacer(Modifier.height(24.dp))
            JSub("세 대가 함께 달린 가평 여행, 만장일치로 청평휴게소에 들러 호두과자를 먹고, 첫 곡은 아이유의 밤편지로 시작했어요.")
            Spacer(Modifier.height(80.dp))
            JTitle("길 위의 한마디")
            JSub("“음료수 좀 사 오기” · 다연가 길에 남긴 음성")
            Spacer(Modifier.weight(1f))
            JTitle("24       6       1       18")
            JSub("대화       노크       남긴 음성       함께 들은 곡")
        }
        JCard(Modifier.weight(1.2f).fillMaxHeight()) {
            JTitle("안전 운전 순위")
            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf(Triple("나", "85", R.drawable.flow_imgprofilephoto),
                    Triple("다연", "88", R.drawable.flow_imgprofilephoto2),
                    Triple("문준", "66", R.drawable.flow_imgprofilephoto1)).forEach { (name, score, image) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        JAvatar(image, name, 110.dp); JTitle(name); JTitle(score); JSub("시연 점수")
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            JSub(if (s.wager) "내기 결과 · 커피는 문준님이 사요" else "내기 결과 · 선택한 내기 없음")
        }
    }
    Spacer(Modifier.height(14.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        JButton("저장하기", enabled, { s.open(WireScreen.SAVE) }, Modifier.weight(1f))
        JButton("저장 안 함", enabled, { s.saved = false; s.home() }, Modifier.weight(1f), false)
    }
}
