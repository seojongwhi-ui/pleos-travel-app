package com.contest.travel.preview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** The 21 frames in Figma section 252:11453. */
internal enum class WireScreen {
    HOME, CREATE, INVITE, WAITING, MISSION, MUSIC_PICK, FIRST_SONG,
    DRIVE, MAP, DJ, KNOCK_SENT, KNOCK_RECEIVED, KNOCK_FULL, CALL,
    VOICE_LEAVE, VOICE_RECEIVED, VOICE_APPROACH, VOICE_DELIVER,
    ARRIVED, SUMMARY, SAVE;

    val isDriving get() = this in setOf(DRIVE, MAP, DJ, KNOCK_SENT, KNOCK_RECEIVED,
        KNOCK_FULL, CALL, VOICE_LEAVE, VOICE_RECEIVED, VOICE_APPROACH, VOICE_DELIVER)
}

internal class WireSession {
    val stack = mutableStateListOf(WireScreen.HOME)
    val screen get() = stack.last()
    var purpose by mutableStateOf("친구 여행")
    var destination by mutableStateOf("가평 용추계곡")
    var invitees by mutableStateOf(setOf("민지", "도윤"))
    var phone by mutableStateOf("")
    var mission by mutableStateOf(true)
    var music by mutableStateOf(true)
    var playing by mutableStateOf(true)
    var song by mutableIntStateOf(0)
    var progress by mutableFloatStateOf(.36f)
    var vote by mutableStateOf("")
    var waypoint by mutableStateOf(false)
    var knockTarget by mutableStateOf("B차")
    var seconds by mutableIntStateOf(10)
    var incomingShown by mutableStateOf(false)
    var roadVoiceShown by mutableStateOf(false)
    var saved by mutableStateOf(false)
    var showSettings by mutableStateOf(false)
    var showEnd by mutableStateOf(false)
    var showHistory by mutableStateOf(false)
    var message by mutableStateOf("")
    var wager by mutableStateOf(false)

    fun open(next: WireScreen) {
        if (screen != next) stack.add(next)
    }
    fun back() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }
    fun drive() {
        while (stack.size > 1 && stack.last().isDriving) stack.removeAt(stack.lastIndex)
        if (screen != WireScreen.DRIVE) stack.add(WireScreen.DRIVE)
    }
    fun home() {
        stack.clear()
        stack.add(WireScreen.HOME)
        progress = .36f
        incomingShown = false
        roadVoiceShown = false
        waypoint = false
        vote = ""
        message = ""
    }
}

/** Wireframe colors are shared here; no new visual direction is added. */
internal object Wire {
    val background = Color(0xFFF7F8FA)
    val surface = Color.White
    val muted = Color(0xFFEDEEF2)
    val mutedStrong = Color(0xFFC7C9CE)
    val ink = Color(0xFF313236)
    val secondary = Color(0xFFA4A8AE)
    val action = Color(0xFF44464E)
    val teal = Color(0xFF0064FF)
    val orange = Color(0xFFA4A8AE)
}

@Composable
internal fun LinkCarDemoScreen(wide: Boolean, enabled: Boolean, onExit: () -> Unit, modifier: Modifier = Modifier) {
    val s = remember { WireSession() }
    val convoy by rememberUpdatedState(LocalConvoy.current)
    val screen = s.screen
    BackHandler { if (screen == WireScreen.HOME) onExit() else s.back() }

    LaunchedEffect(screen) {
        when (screen) {
            WireScreen.KNOCK_SENT, WireScreen.KNOCK_RECEIVED, WireScreen.KNOCK_FULL -> {
                s.seconds = 10
                repeat(10) { delay(1_000); s.seconds-- }
                if (screen == WireScreen.KNOCK_SENT) s.open(WireScreen.VOICE_LEAVE) else s.drive()
            }
            WireScreen.CALL -> {
                s.seconds = 30
                repeat(30) { delay(1_000); s.seconds-- }
                s.drive()
            }
            WireScreen.VOICE_APPROACH -> {
                delay(8_000)
                s.open(WireScreen.VOICE_DELIVER)
            }
            WireScreen.DRIVE -> if (!s.incomingShown) {
                delay(14_000)
                s.incomingShown = true
                s.open(WireScreen.KNOCK_RECEIVED)
            }
            else -> Unit
        }
    }
    LaunchedEffect(screen.isDriving) {
        var ticks = 0
        if (screen.isDriving) while (true) {
            delay(1_000)
            s.progress = (s.progress + .002f).coerceAtMost(1f)
            ticks++
            if (ticks % 35 == 0) convoy.advance()
            if (s.progress >= .46f && !s.roadVoiceShown && s.screen == WireScreen.DRIVE) {
                s.roadVoiceShown = true
                s.open(WireScreen.VOICE_APPROACH)
            }
        }
    }

    Surface(modifier.fillMaxSize(), color = Wire.background) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val contentWide = wide && maxWidth >= 900.dp
            Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 12.dp)) {
                when (screen) {
                    WireScreen.HOME -> WireHome(s, contentWide, enabled)
                    WireScreen.CREATE -> WireCreate(s, contentWide, enabled)
                    WireScreen.INVITE -> WireInvite(s, enabled)
                    WireScreen.WAITING -> WireWaiting(s, contentWide, enabled)
                    WireScreen.MISSION -> WireMission(s, contentWide, enabled)
                    WireScreen.MUSIC_PICK -> WireMusicPick(s, contentWide, enabled)
                    WireScreen.FIRST_SONG -> WireFirstSong(s, contentWide, enabled)
                    WireScreen.ARRIVED, WireScreen.SUMMARY, WireScreen.SAVE -> WireEnding(s, screen, contentWide, enabled)
                    else -> WireDriving(s, screen, contentWide, enabled)
                }
            }
        }
    }
    if (s.showSettings) AlertDialog(onDismissRequest = { s.showSettings = false },
        title = { Text("설정") }, text = {
            Text("현재는 한 기기 시연입니다. 차량 간 연결·음성·음악은 API 연결 전까지 실제로 전송되지 않습니다.")
        }, confirmButton = { TextButton(onClick = { s.showSettings = false }) { Text("닫기") } })
    if (s.showEnd) AlertDialog(onDismissRequest = { s.showEnd = false },
        title = { Text("동행을 종료할까요?") },
        text = { Text("시연 여행을 마치고 도착 화면으로 이동합니다.") },
        dismissButton = { TextButton(onClick = { s.showEnd = false }) { Text("계속 가기") } },
        confirmButton = { TextButton(onClick = {
            s.showEnd = false
            s.open(WireScreen.ARRIVED)
        }) { Text("동행 종료") } })
    if (s.showHistory) AlertDialog(onDismissRequest = { s.showHistory = false },
        title = { Text("이전 대화") },
        text = { Text("B차 · 휴게소 들렀다 가자\nC차 · 이 노래 누가 신청했어? 좋다") },
        confirmButton = { TextButton(onClick = { s.showHistory = false }) { Text("닫기") } })
}

@Composable
internal fun WireHeader(title: String, subtitle: String, step: String? = null, back: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = 86.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        if (back != null) WireButton("‹", back, filled = false, width = 64.dp, height = 64.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = Wire.ink)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = Wire.secondary)
        }
        step?.let { WireChip(it) }
    }
}

@Composable
internal fun WirePanel(modifier: Modifier = Modifier, padding: Dp = 28.dp,
    content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, color = Wire.surface, shape = RoundedCornerShape(Pleos215.RadiusLarge)) {
        Column(Modifier.fillMaxSize().padding(padding), content = content)
    }
}

@Composable
internal fun WireButton(label: String, onClick: () -> Unit, filled: Boolean = true,
    enabled: Boolean = true, modifier: Modifier = Modifier, width: Dp? = null, height: Dp = Pleos215.ButtonLarge) {
    val sized = if (width != null) modifier.width(width) else modifier
    Button(onClick = onClick, enabled = enabled, modifier = sized.height(height),
        shape = RoundedCornerShape(Pleos215.RadiusLarge),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (filled) Wire.action else Wire.muted,
            contentColor = if (filled) Color.White else Wire.ink,
            disabledContainerColor = Wire.muted,
            disabledContentColor = Wire.secondary),
        contentPadding = PaddingValues(horizontal = 20.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun WireChip(label: String, modifier: Modifier = Modifier, selected: Boolean = false,
    onClick: (() -> Unit)? = null) {
    Surface(modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = if (selected) Wire.action else Wire.muted,
        contentColor = if (selected) Color.White else Wire.ink,
        shape = RoundedCornerShape(Pleos215.RadiusSmall)) {
        Text(label, Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
internal fun WireAvatar(name: String, size: Dp = 80.dp, modifier: Modifier = Modifier) {
    val ring = when (name) { "민지", "B차" -> Wire.teal; "도윤", "C차" -> Wire.orange; else -> Wire.ink }
    Canvas(modifier.size(size)) {
        drawCircle(Wire.surface)
        drawCircle(ring, style = Stroke(4.dp.toPx()))
        drawCircle(Wire.mutedStrong, radius = this.size.minDimension * .38f)
        drawCircle(Color(0xFFC7C9CE), radius = this.size.minDimension * .11f,
            center = Offset(this.size.width * .5f, this.size.height * .4f))
        drawArc(Color(0xFFC7C9CE), 180f, 180f, false,
            topLeft = Offset(this.size.width * .26f, this.size.height * .54f),
            size = androidx.compose.ui.geometry.Size(this.size.width * .48f, this.size.height * .32f),
            style = Stroke(this.size.width * .14f, cap = StrokeCap.Round))
    }
}

@Composable
internal fun WireInput(value: String, onValueChange: (String) -> Unit, placeholder: String,
    modifier: Modifier = Modifier, enabled: Boolean = true) {
    TextField(value = value, onValueChange = onValueChange, enabled = enabled,
        placeholder = { Text(placeholder, style = MaterialTheme.typography.labelMedium, color = Wire.secondary) },
        modifier = modifier.heightIn(min = 80.dp),
        shape = RoundedCornerShape(Pleos215.RadiusLarge),
        singleLine = true,
        colors = TextFieldDefaults.colors(focusedContainerColor = Wire.muted,
            unfocusedContainerColor = Wire.muted, disabledContainerColor = Wire.muted,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
}

@Composable
internal fun WireSub(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.labelMedium, color = Wire.secondary)
}

@Composable
internal fun WireTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.displaySmall, color = Wire.ink)
}

@Composable
internal fun WireRow(title: String, detail: String, modifier: Modifier = Modifier,
    selected: Boolean = false, onClick: (() -> Unit)? = null) {
    Surface(modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = Wire.muted, shape = RoundedCornerShape(Pleos215.RadiusMedium),
        border = if (selected) BorderStroke(2.dp, Wire.action) else null) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Wire.ink)
            WireSub(detail)
        }
    }
}

@Composable
internal fun WireHome(s: WireSession, wide: Boolean, enabled: Boolean) {
    Row(Modifier.fillMaxWidth().heightIn(min = 86.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        WireAvatar("나", 76.dp)
        Column {
            Text("안녕하세요, 승호님", style = MaterialTheme.typography.headlineMedium, color = Wire.ink)
            WireSub("내 차 · 아이오닉 5")
        }
    }
    Spacer(Modifier.height(16.dp))
    if (wide) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            WireHomeCreate(s, enabled, Modifier.weight(1.55f).fillMaxHeight())
            WireHomeRecent(s, enabled, Modifier.weight(1f).fillMaxHeight())
        }
    } else Column(Modifier.verticalScroll(rememberScrollState())) {
        WireHomeCreate(s, enabled, Modifier.fillMaxWidth().height(420.dp))
        Spacer(Modifier.height(16.dp))
        WireHomeRecent(s, enabled, Modifier.fillMaxWidth().height(420.dp))
    }
}

@Composable
private fun WireHomeCreate(s: WireSession, enabled: Boolean, modifier: Modifier) {
    val convoy = LocalConvoy.current
    WirePanel(modifier) {
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(180.dp).background(Wire.muted, CircleShape).align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center) {
            Text("+", style = MaterialTheme.typography.displayLarge, color = Wire.action)
        }
        Spacer(Modifier.height(24.dp))
        WireTitle("새 여행 만들기", Modifier.align(Alignment.CenterHorizontally))
        WireSub("목적과 목적지를 정하면 함께 갈 차를 초대할 수 있어요", Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(24.dp))
        WireButton("여행 만들기", { convoy.reset(); s.open(WireScreen.CREATE) }, enabled = enabled,
            modifier = Modifier.fillMaxWidth(.78f).align(Alignment.CenterHorizontally))
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun WireHomeRecent(s: WireSession, enabled: Boolean, modifier: Modifier) {
    WirePanel(modifier) {
        Text("지난 여행 다시 가기", style = MaterialTheme.typography.titleLarge, color = Wire.ink)
        Spacer(Modifier.height(16.dp))
        WireRecentCard(if (s.saved) "${s.destination} 가는 길" else "가평 계곡 가는 길",
            "친구 여행 · 10월 12일", listOf("민지", "도윤"), enabled) {
            s.open(WireScreen.WAITING)
        }
        Spacer(Modifier.height(12.dp))
        WireRecentCard("할머니 댁", "가족 · 9월 17일", listOf("나"), enabled) {
            s.destination = "할머니 댁"; s.purpose = "가족"; s.open(WireScreen.WAITING)
        }
        Spacer(Modifier.weight(1f))
        WireSub("같은 멤버면 초대 없이 바로 다시 모여요")
    }
}

@Composable
private fun WireRecentCard(title: String, detail: String, people: List<String>,
    enabled: Boolean, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clickable(enabled, onClick = onClick),
        color = Wire.muted, shape = RoundedCornerShape(Pleos215.RadiusMedium)) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Wire.ink)
            WireSub(detail)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                people.forEach { WireAvatar(it, 46.dp) }
            }
        }
    }
}

@Composable
internal fun WireCreate(s: WireSession, wide: Boolean, enabled: Boolean) {
    WireHeader("여행 만들기", "어떤 이동인지와 목적지를 정해 주세요", "1 / 2", s::back)
    Spacer(Modifier.height(16.dp))
    if (wide) Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        WirePurpose(s, enabled, Modifier.weight(1f).fillMaxHeight())
        WireDestination(s, enabled, Modifier.weight(1f).fillMaxHeight())
    } else Column(Modifier.verticalScroll(rememberScrollState())) {
        WirePurpose(s, enabled, Modifier.fillMaxWidth().height(520.dp))
        Spacer(Modifier.height(16.dp))
        WireDestination(s, enabled, Modifier.fillMaxWidth().height(620.dp))
    }
}

@Composable
private fun WirePurpose(s: WireSession, enabled: Boolean, modifier: Modifier) {
    WirePanel(modifier) {
        Text("이동 목적", style = MaterialTheme.typography.titleLarge, color = Wire.ink)
        WireSub("고른 목적에 맞춰 기본 설정이 정해져요")
        Spacer(Modifier.height(22.dp))
        listOf("친구 여행" to "함께 듣는 DJ · 순위 켜짐",
            "가족" to "안부 · 도착 알림 중심", "업무 · 단체" to "조용한 기본값").forEach { (title, desc) ->
            val selected = s.purpose == title
            Surface(Modifier.fillMaxWidth().height(142.dp).clickable(enabled) { s.purpose = title },
                color = if (selected) Wire.muted else Wire.surface,
                shape = RoundedCornerShape(Pleos215.RadiusMedium),
                border = BorderStroke(2.dp, if (selected) Wire.action else Wire.mutedStrong)) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Box(Modifier.size(64.dp).background(Wire.mutedStrong, CircleShape))
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleMedium, color = Wire.ink)
                        WireSub(desc)
                    }
                    WireChip(if (selected) "✓" else "○", selected = selected)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WireDestination(s: WireSession, enabled: Boolean, modifier: Modifier) {
    WirePanel(modifier) {
        Text("목적지", style = MaterialTheme.typography.titleLarge, color = Wire.ink)
        Spacer(Modifier.height(18.dp))
        WireInput(s.destination, { s.destination = it }, "어디로 가나요?",
            Modifier.fillMaxWidth(), enabled)
        Spacer(Modifier.height(18.dp))
        WireSub("최근 목적지")
        Spacer(Modifier.height(12.dp))
        listOf("가평 용추계곡" to "경기 가평군 · 82km",
            "양주 글램핑장" to "경기 양주시 · 45km",
            "할머니 댁" to "충남 공주시 · 140km").forEach { (title, desc) ->
            Surface(Modifier.fillMaxWidth().clickable(enabled) { s.destination = title },
                color = Wire.muted, shape = RoundedCornerShape(Pleos215.RadiusMedium),
                border = if (s.destination == title) BorderStroke(2.dp, Wire.action) else null) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Box(Modifier.size(48.dp).background(Wire.mutedStrong, CircleShape))
                    Column {
                        Text(title, style = MaterialTheme.typography.titleMedium, color = Wire.ink)
                        WireSub(desc)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.weight(1f))
        WireButton("여행 만들기", { s.open(WireScreen.INVITE) },
            enabled = enabled && s.destination.isNotBlank(), modifier = Modifier.fillMaxWidth())
    }
}
