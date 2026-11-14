package com.contest.travel.preview

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import android.os.SystemClock
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlinx.coroutines.delay

/** S-10 / Figma 328:5199, adapted to the Connect-S app activity region. */
private object DriveDesign {
    val Base = Color(0xFFF4F5F7)
    val GradientStart = Color(0xFFEBEDF2)
    val GradientEnd = Color(0xFFD5D7DD)
    val Card = Color.White.copy(alpha = .80f)
    val Ink = Color(0xFF131417)
    val Muted = Color(0xFFACB1C0)
    val Subtle = Color(0xFF686A72)
    val Input = Color(0xFFEEEFF4)
    val Blue = Color(0xFF5263E7)
    val BlueMid = Color(0xFFBEC6FF)
    val BluePale = Color(0xFFE7EAFF)
    val Track = Color(0xFFADAEB6)
    val Green = Color(0xFF19BB62)
    val Red = Color(0xFFFF7777)
    val Black = Color(0xFF191A1B)
    val CallRed = Color(0xFFEE5D5D)
    val CallTrack = Color(0xFFE7EAFF)
    val CallWave = Color(0xFF96A2FF)
    val CallMuted = Color(0xFF8F919F)
}

private val CallTimerFont = FontFamily(Font(R.font.outfit_regular, FontWeight.Normal))

@Composable
internal fun DesignedDriveScreen(s: WireSession, screen: WireScreen, enabled: Boolean,
    setupRestricted: Boolean,
    voice: LinkCarVoiceRuntime, onVoiceClick: () -> Unit, onResetDemo: () -> Unit,
    onPollSpeech: () -> Unit) {
    val density = LocalDensity.current.density
    val canResize = enabled && !s.showVoiceRecorder &&
        screen in setOf(WireScreen.DRIVE, WireScreen.MAP, WireScreen.DJ)
    var mapDragging by remember { mutableStateOf(false) }
    var musicDragging by remember { mutableStateOf(false) }
    var companionsDragging by remember { mutableStateOf(false) }
    var mapTarget by remember { mutableFloatStateOf(if (screen == WireScreen.MAP) 560f else 165f) }
    var musicTarget by remember { mutableFloatStateOf(if (screen == WireScreen.DJ) 500f else 200f) }
    var companionsTarget by remember { mutableFloatStateOf(if (s.companionsExpanded) 900f else 614f) }
    LaunchedEffect(screen) {
        mapTarget = if (screen == WireScreen.MAP) 560f else 165f
        musicTarget = if (screen == WireScreen.DJ) 500f else 200f
    }
    LaunchedEffect(s.companionsExpanded) {
        companionsTarget = if (s.companionsExpanded) 900f else 614f
    }
    val mapHeight by animateFloatAsState(mapTarget,
        animationSpec = if (mapDragging) snap() else tween(300), label = "지도 크기")
    val musicHeight by animateFloatAsState(musicTarget,
        animationSpec = if (musicDragging) snap() else tween(300), label = "음악 크기")
    val companionsWeight by animateFloatAsState(companionsTarget,
        animationSpec = if (companionsDragging) snap() else tween(300), label = "차량 목록 너비")
    val mapOpen = mapHeight >= 280f
    val musicOpen = musicHeight >= 300f
    LaunchedEffect(s.speechFeedbackVisible, voice.status) {
        if (s.speechFeedbackVisible &&
            (voice.status.contains("오류") || voice.status.contains("실패") ||
                voice.status.contains("인식하지 못"))) {
            delay(10_000)
            s.speechFeedbackVisible = false
        }
    }

    Column(Modifier.fillMaxSize().background(Brush.linearGradient(
        listOf(DriveDesign.GradientStart, DriveDesign.GradientEnd)))) {
        DesignedDriveHeader(s, enabled && !setupRestricted)
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth().height(mapHeight.dp)) {
            if (mapOpen) {
                Surface(Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    color = Color.White,
                    shape = RoundedCornerShape(48.dp)) {
                    RoadConvoyMap(overview = true, cameraRevision = 0, enabled = enabled,
                        modifier = Modifier.fillMaxSize(), voiceMarkerVisible = voice.notes.hasDemoMarker)
                }
                MapResetButton(enabled, onResetDemo,
                    Modifier.align(Alignment.TopStart).padding(start = 28.dp, top = 12.dp))
            } else {
                DesignedRouteStrip(s, voice.notes.hasDemoMarker, mapHeight.dp,
                    enabled, onResetDemo)
            }
            DriveResizeGrip(GripAxis.VERTICAL, "지도를 아래로 끌어 펼치거나 위로 끌어 접기",
                canResize && screen != WireScreen.DJ,
                Modifier.align(Alignment.BottomCenter), barOffsetY = 8.dp,
                onDrag = { delta ->
                    mapDragging = true
                    mapTarget = (mapTarget + delta / density).coerceIn(165f, 560f)
                }, onRelease = { _ ->
                    mapDragging = false
                    // Resizing the map is a layout change, not a navigation event.
                    mapTarget = if (mapTarget >= 280f) 560f else 165f
                })
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp)) {
            val voiceModifier = Modifier.weight(1636f - companionsWeight).fillMaxHeight()
            if (s.showVoiceRecorder) {
                DesignedRoadVoiceRecorderCard(voiceModifier) {
                    s.showVoiceRecorder = false
                    s.message = "녹음 화면 시연을 마쳤어요. 음성 파일은 저장되지 않았어요."
                }
            } else if (mapOpen) {
                DesignedMapVoice(s, enabled, voiceModifier)
            } else if (musicOpen) {
                DesignedCompactVoice(s, enabled, voice, onVoiceClick, voiceModifier)
            } else {
                DesignedVoiceCard(s, screen, enabled, setupRestricted, voice, onVoiceClick,
                    onPollSpeech, voiceModifier)
            }
            Spacer(Modifier.width(32.dp))
            Box(Modifier.weight(companionsWeight).fillMaxHeight()) {
                if (mapOpen) DesignedMapCompanions(s, enabled, Modifier.fillMaxSize())
                else if (musicOpen) DesignedCompactCompanions(s, enabled, Modifier.fillMaxSize())
                else DesignedCompanions(s, screen, enabled, Modifier.fillMaxSize())
                DriveResizeGrip(GripAxis.HORIZONTAL,
                    "차량 목록을 왼쪽으로 끌어 펼치거나 오른쪽으로 끌어 접기",
                    canResize, Modifier.align(Alignment.CenterStart).padding(start = 4.dp),
                    onDrag = { delta ->
                        companionsDragging = true
                        companionsTarget = (companionsTarget - delta / density).coerceIn(614f, 900f)
                    }, onRelease = { total ->
                        companionsDragging = false
                        s.companionsExpanded = if (s.companionsExpanded) total <= 56f * density
                            else total < -56f * density
                        companionsTarget = if (s.companionsExpanded) 900f else 614f
                    })
            }
        }
        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth().height(musicHeight.dp)) {
            if (musicOpen) DesignedMusicExpanded(s, enabled, musicHeight.dp)
            else DesignedMusicBar(s, enabled, motionAllowed = !setupRestricted,
                height = musicHeight.dp)
            DriveResizeGrip(GripAxis.VERTICAL, "음악을 위로 끌어 펼치거나 아래로 끌어 접기",
                canResize && screen != WireScreen.MAP,
                Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                onDrag = { delta ->
                    musicDragging = true
                    musicTarget = (musicTarget - delta / density).coerceIn(200f, 500f)
                }, onRelease = { total ->
                    musicDragging = false
                    val expanded = if (screen == WireScreen.DJ) total <= 56f * density
                        else total < -56f * density
                    if (expanded && screen != WireScreen.DJ) {
                        s.drive()
                        s.open(WireScreen.DJ)
                    } else if (!expanded && screen == WireScreen.DJ) s.drive()
                    musicTarget = if (expanded) 500f else 200f
                })
        }
    }
}

/** S-11 map cards. Keep the live NAVER map while matching the Figma card layout. */
@Composable
private fun DesignedMapVoice(s: WireSession, enabled: Boolean, modifier: Modifier) {
    Surface(modifier, color = DriveDesign.Card, shape = RoundedCornerShape(48.dp)) {
        Box(Modifier.fillMaxSize().padding(26.dp)) {
            Text("음성 연결", Modifier.align(Alignment.TopStart),
                style = TextStyle(fontFamily = FFont, fontSize = 23.sp,
                    color = DriveDesign.Muted))
            Column(Modifier.align(Alignment.CenterStart)) {
                val poll = s.generatedPoll
                Text(when {
                    poll != null -> poll.title
                    s.demoVoiceTriggered && !s.roadVoiceShown -> "다연 차가 길에 남긴 음성"
                    else -> "아직 보낸 음성이 없어요"
                }, style = TextStyle(fontFamily = FFont, fontSize = 37.sp,
                    fontWeight = FontWeight.Bold, color = DriveDesign.Ink))
                if (poll == null) Text(
                    if (s.demoVoiceTriggered && !s.roadVoiceShown) "길에 남긴 음성을 확인해 보세요"
                    else "다른 친구의 차를 두드려보세요",
                    style = TextStyle(fontFamily = FFont, fontSize = 17.sp,
                        color = DriveDesign.Subtle))
            }
            s.generatedPoll?.let { poll ->
                Row(Modifier.align(Alignment.CenterEnd),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    poll.options.forEachIndexed { index, option ->
                        MapVoteButton(option, index == 0, enabled) { s.vote = option }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapVoteButton(label: String, primary: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(if (primary) 140.dp else 124.dp, 72.dp)
        .clip(RoundedCornerShape(32.dp))
        .background(if (primary) DriveDesign.Black else DriveDesign.Input)
        .clickable(enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, style = TextStyle(fontFamily = FFont, fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (primary) Color.White else DriveDesign.Muted))
    }
}

@Composable
private fun DesignedMapCompanions(s: WireSession, enabled: Boolean, modifier: Modifier) {
    val convoy = LocalConvoy.current
    Surface(modifier, color = DriveDesign.Card, shape = RoundedCornerShape(48.dp)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Text("연결된 차량 (2)", Modifier.padding(top = 18.dp, bottom = 2.dp),
                style = TextStyle(fontFamily = FFont, fontSize = 23.sp,
                    color = DriveDesign.Muted))
            MapCompanionRow("다연 차", convoy.dayeonScore,
                remainingRouteKilometers(convoy.dayeonRouteMeters),
                R.drawable.flow_imgprofilephoto2, true, enabled,
                Modifier.weight(1f)) { s.sendKnock("다연 차") }
            Box(Modifier.fillMaxWidth().height(1.dp).background(DriveDesign.Track.copy(alpha = .2f)))
            MapCompanionRow("문준 차", convoy.munjunScore,
                remainingRouteKilometers(convoy.munjunRouteMeters),
                R.drawable.flow_imgprofilephoto1, true, enabled,
                Modifier.weight(1f)) { s.sendKnock("문준 차") }
        }
    }
}

@Composable
private fun MapCompanionRow(name: String, score: Int, distance: String, portrait: Int,
    canKnock: Boolean, enabled: Boolean, modifier: Modifier, onKnock: () -> Unit) {
    val moving = LocalConvoy.current.speedKph > 0f
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(60.dp)) {
            Image(painterResource(portrait), name, Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop)
            Box(Modifier.align(Alignment.BottomEnd).size(18.dp).clip(CircleShape)
                .background(if (canKnock) DriveDesign.Green else DriveDesign.Red))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.width(118.dp)) {
            Text(name, style = TextStyle(fontFamily = FFont, fontSize = 25.sp,
                fontWeight = FontWeight.SemiBold, color = DriveDesign.Ink))
            Text(if (moving) "주행중" else "정차중",
                style = TextStyle(fontFamily = FFont, fontSize = 16.sp,
                    color = if (canKnock) DriveDesign.Green else DriveDesign.Red))
        }
        Text("$score", style = TextStyle(fontFamily = FFont, fontSize = 38.sp,
            color = if (canKnock) DriveDesign.Blue else DriveDesign.Red))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.width(1.dp).height(39.dp).background(DriveDesign.Track.copy(alpha = .45f)))
        Spacer(Modifier.width(8.dp))
        Text(distance, style = TextStyle(fontFamily = FFont,
            fontSize = 36.sp, color = DriveDesign.Ink))
        Text("km", style = TextStyle(fontFamily = FFont, fontSize = 16.sp,
            color = DriveDesign.Subtle))
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(92.dp).clip(CircleShape)
            .background(if (canKnock) DriveDesign.Black else DriveDesign.Input)
            .then(if (canKnock) Modifier.clickable(enabled, onClick = onKnock)
                .semantics { contentDescription = "$name 두드리기" } else Modifier),
            contentAlignment = Alignment.Center) {
            Image(painterResource(if (canKnock) R.drawable.drive_knock_hand else R.drawable.drive_locked),
                if (canKnock) "$name 두드리기" else "정차 중 · 두드리기 불가",
                Modifier.size(if (canKnock) 76.dp else 37.dp),
                contentScale = ContentScale.Fit)
        }
    }
}

@Composable
private fun DesignedCompactVoice(s: WireSession, enabled: Boolean,
    voice: LinkCarVoiceRuntime, onVoiceClick: () -> Unit, modifier: Modifier) {
    Surface(modifier, color = DriveDesign.Card, shape = RoundedCornerShape(48.dp)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("음성 연결", style = MaterialTheme.typography.labelMedium,
                    color = DriveDesign.Muted)
                Spacer(Modifier.weight(1f))
                Surface(Modifier.clickable(enabled) { s.showHistory = true },
                    color = DriveDesign.Input, shape = CircleShape) {
                    Text("이전 대화", Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = DriveDesign.Subtle)
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.align(Alignment.CenterStart)) {
                    val poll = s.generatedPoll
                    when {
                        s.demoVoiceTriggered && !s.roadVoiceShown -> {
                            Text("다연 차가 길에 남긴 음성",
                                style = MaterialTheme.typography.headlineLarge,
                                color = DriveDesign.Ink)
                            Text(voice.status, style = MaterialTheme.typography.labelSmall,
                                color = DriveDesign.Subtle, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                        }
                        poll != null -> {
                            Text(poll.title, style = MaterialTheme.typography.headlineLarge,
                                color = DriveDesign.Ink, maxLines = 2,
                                overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                poll.options.forEach { option ->
                                    WireButton(option, { s.vote = option },
                                        filled = s.vote == option, enabled = enabled,
                                        modifier = Modifier.weight(1f), height = Pleos215.ButtonSmall)
                                }
                            }
                            if (s.vote.isNotEmpty()) Text("선택: ${s.vote} · 로컬 시연 투표",
                                style = MaterialTheme.typography.labelSmall,
                                color = DriveDesign.Subtle)
                        }
                        else -> {
                            Text("아직 보낸 음성이 없어요", style = MaterialTheme.typography.headlineLarge,
                                color = DriveDesign.Ink, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                            Text("다른 친구의 차를 두드려보세요",
                                style = MaterialTheme.typography.labelSmall,
                                color = DriveDesign.Muted, maxLines = 1)
                        }
                    }
                    if (s.pollBusy) Text("AI가 투표 안건을 만들고 있어요",
                        style = MaterialTheme.typography.labelSmall,
                        color = DriveDesign.Subtle)
                    if (s.message.isNotBlank()) Text(s.message,
                        style = MaterialTheme.typography.labelSmall,
                        color = DriveDesign.Subtle, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    if (s.speechFeedbackVisible) Text(voice.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = DriveDesign.Subtle, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
            }
            WireButton("모두에게 말하기", onVoiceClick, filled = false,
                enabled = enabled, modifier = Modifier.fillMaxWidth(),
                height = Pleos215.ButtonSmall)
        }
    }
}

@Composable
private fun DesignedCompactCompanions(s: WireSession, enabled: Boolean, modifier: Modifier) {
    val convoy = LocalConvoy.current
    Surface(modifier, color = DriveDesign.Card, shape = RoundedCornerShape(48.dp)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Text("연결된 차량 (2)", Modifier.padding(top = 14.dp),
                style = MaterialTheme.typography.labelMedium, color = DriveDesign.Muted)
            val motionLabel = if (convoy.speedKph > 0f) "주행중" else "정차중"
            listOf(Triple("다연 차", "${convoy.dayeonScore}점 · $motionLabel", R.drawable.flow_imgprofilephoto2),
                Triple("문준 차", "${convoy.munjunScore}점 · $motionLabel", R.drawable.flow_imgprofilephoto1)).forEachIndexed { _, (name, detail, portrait) ->
                Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(portrait), name, Modifier.size(64.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleMedium, color = DriveDesign.Ink)
                        Text(detail, style = MaterialTheme.typography.labelSmall, color = DriveDesign.Subtle)
                    }
                    WireButton("두드리기", { s.sendKnock(name) },
                        enabled = enabled, height = Pleos215.ButtonSmall)
                }
            }
        }
    }
}

@Composable
private fun DesignedDriveHeader(s: WireSession, enabled: Boolean) {
    Row(Modifier.fillMaxWidth().height(90.dp).padding(start = 44.dp, end = 24.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("가평 계곡 여행", style = MaterialTheme.typography.titleLarge, color = DriveDesign.Ink)
            Text("순위는 안전 운전 점수로 정해져요", style = MaterialTheme.typography.labelSmall,
                color = DriveDesign.Subtle)
        }
        Image(painterResource(R.drawable.drive_settings), "설정",
            Modifier.size(Pleos215.Icon).clickable(enabled) { s.showSettings = true })
        Spacer(Modifier.width(8.dp))
        Image(painterResource(R.drawable.drive_close), "동행 종료",
            Modifier.size(Pleos215.Icon).clickable(enabled) { s.showEnd = true })
    }
}

@Composable
internal fun DesignedRouteStrip(s: WireSession, voiceMarkerVisible: Boolean, height: Dp,
    enabled: Boolean, onResetDemo: () -> Unit) {
    val motion = LocalConvoy.current
    BoxWithConstraints(Modifier.fillMaxWidth().height(height).padding(horizontal = 12.dp)
        .clip(RoundedCornerShape(48.dp)).background(Color.White)) {
        Image(painterResource(R.drawable.drive_map_strip), null,
            Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
            alignment = BiasAlignment(0f, .54f))
        MapResetButton(enabled, onResetDemo,
            Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 10.dp))
        val progress = s.progress.coerceIn(0f, 1f)
        val own = (.10f + progress * .49f).coerceIn(.10f, .59f)
        val second = (.72f + ((motion.dayeonRouteMeters / demoRouteLengthMeters) - .84).toFloat() * .5f)
            .coerceIn(.72f, .82f)
        val third = (.90f + ((motion.munjunRouteMeters / demoRouteLengthMeters) - .91).toFloat() * .5f)
            .coerceIn(.90f, .95f)
        val trackStart = 52.dp
        val trackWidth = maxWidth - trackStart * 2
        Box(Modifier.fillMaxWidth().padding(horizontal = 52.dp).height(14.dp)
            .align(Alignment.BottomCenter).offset(y = (-12).dp)
            .clip(CircleShape).background(DriveDesign.Track)) {
            Box(Modifier.fillMaxWidth(third).fillMaxHeight().background(DriveDesign.BluePale))
            Box(Modifier.fillMaxWidth(second).fillMaxHeight().background(DriveDesign.BlueMid))
            Box(Modifier.fillMaxWidth(own).fillMaxHeight().background(DriveDesign.Blue))
        }
        listOf(own, second, third).forEachIndexed { index, place ->
            val portrait = when (index) {
                0 -> R.drawable.flow_imgprofilephoto
                1 -> R.drawable.flow_imgprofilephoto2
                else -> R.drawable.flow_imgprofilephoto1
            }
            Box(Modifier.offset(x = trackStart + trackWidth * place - 48.dp,
                y = 18.dp + (height - 165.dp).coerceAtLeast(0.dp))
                .size(96.dp, 120.dp)) {
                if (index == 0) {
                    val pin = painterResource(R.drawable.drive_pin_self)
                    // Figma's 148x172 asset includes a 26px horizontal / 23px top
                    // shadow inset around the 96x120 pin. Draw in the same coordinate
                    // space as the portrait, without requiredSize's overflow centering.
                    Canvas(Modifier.fillMaxSize()) {
                        translate(left = -26.dp.toPx(), top = -23.dp.toPx()) {
                            with(pin) { draw(Size(148.dp.toPx(), 172.dp.toPx())) }
                        }
                    }
                } else {
                    Image(painterResource(R.drawable.drive_pin_other), null, Modifier.fillMaxSize())
                }
                Image(painterResource(portrait), if (index == 0) "내 차" else "시연 차량 ${if (index == 1) "B" else "C"}",
                    Modifier.offset(x = 9.dp, y = 9.dp).size(78.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop)
            }
        }
        if (voiceMarkerVisible) {
            val markerPlace = .10f + .2f * .49f
            Box(
                Modifier.offset(x = trackStart + trackWidth * markerPlace - 22.dp,
                    y = 83.dp + (height - 165.dp).coerceAtLeast(0.dp))
                    .size(45.dp, 56.dp).semantics {
                        contentDescription = "출발 지점 200m 앞에 남긴 음성"
                    }) {
                Image(painterResource(R.drawable.drive_voice_map_pin), null, Modifier.fillMaxSize())
                Image(painterResource(R.drawable.drive_voice_map_mic), null,
                    Modifier.offset(x = 11.dp, y = 11.dp).size(24.dp))
            }
        }
        Surface(Modifier.align(Alignment.TopEnd).padding(end = 20.dp, top = 10.dp),
            color = Color.White.copy(alpha = .94f), shape = RoundedCornerShape(20.dp)) {
            Text(motion.motionError ?: if (voiceMarkerVisible) {
                "내 차 ${motion.speedKph.toInt()} km/h · 음성 지점까지 ${(200.0 - motion.distanceMeters).coerceAtLeast(0.0).toInt()}m"
            } else {
                "내 차 ${motion.speedKph.toInt()} km/h · ${motion.distanceMeters.toInt()}m 이동"
            }, Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                style = MaterialTheme.typography.labelSmall,
                color = if (motion.motionError == null) DriveDesign.Ink else DriveDesign.Red)
        }
    }
}

@Composable
private fun MapResetButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.size(150.dp, 56.dp).clickable(enabled, onClick = onClick)
        .semantics { contentDescription = "주행 시연 처음으로" },
        color = Color.White.copy(alpha = .96f),
        shape = RoundedCornerShape(16.dp), shadowElevation = 2.dp) {
        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(28.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                // The provided bitmap has generous transparent padding around its glyph.
                Image(painterResource(R.drawable.drive_refresh), null, Modifier.requiredSize(88.dp))
            }
            Text("처음으로", style = MaterialTheme.typography.labelSmall,
                color = DriveDesign.Ink, maxLines = 1)
        }
    }
}

/** Visual-only recorder state for the last car; the existing audio runtime is kept intact. */
@Composable
internal fun DesignedRoadVoiceRecorderCard(modifier: Modifier = Modifier, onDone: () -> Unit) {
    val started = remember { SystemClock.elapsedRealtime() }
    var elapsedSeconds by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            elapsedSeconds = ((SystemClock.elapsedRealtime() - started) / 1_000L).toInt()
            delay(250)
        }
    }
    val time = "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60)
    Surface(modifier, color = DriveDesign.Card, shape = RoundedCornerShape(56.dp)) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val compact = maxHeight < 440.dp
            Column(Modifier.fillMaxSize().padding(horizontal = 32.dp,
                vertical = if (compact) 16.dp else 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("길에 음성 남기기", Modifier.align(Alignment.Start),
                    style = MaterialTheme.typography.headlineSmall, color = DriveDesign.Ink)
                Spacer(Modifier.height(4.dp))
                Text("마지막 차의 녹음 화면을 미리 보고 있어요",
                    Modifier.align(Alignment.Start),
                    style = MaterialTheme.typography.labelSmall, color = DriveDesign.Subtle)
                Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
                Surface(Modifier.align(Alignment.Start), color = DriveDesign.BluePale,
                    shape = CircleShape) {
                    Text("화면 시연 · 음성 저장 안 함",
                        Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall, color = DriveDesign.Blue)
                }
                Spacer(Modifier.weight(1f))
                Box(Modifier.fillMaxWidth().height(if (compact) 110.dp else 190.dp),
                    contentAlignment = Alignment.Center) {
                    DemoRecordingWaves(Modifier.fillMaxSize())
                    Box(Modifier.size(if (compact) 88.dp else 122.dp)
                        .border(3.dp, DriveDesign.Blue, CircleShape)
                        .background(Color.White.copy(alpha = .97f), CircleShape),
                        contentAlignment = Alignment.Center) {
                        Image(painterResource(R.drawable.drive_voice_map_mic), null,
                            Modifier.size(if (compact) 48.dp else 64.dp),
                            colorFilter = ColorFilter.tint(DriveDesign.Blue))
                    }
                }
                Spacer(Modifier.height(if (compact) 2.dp else 12.dp))
                Text(time, style = MaterialTheme.typography.displayLarge,
                    color = DriveDesign.Ink)
                Text("파형이 일렁이는 녹음 시연 중",
                    style = MaterialTheme.typography.labelSmall, color = DriveDesign.Subtle)
                Spacer(Modifier.weight(1f))
                Box(Modifier.fillMaxWidth().height(if (compact) Pleos215.ButtonSmall
                    else Pleos215.ButtonMedium)
                    .clip(RoundedCornerShape(Pleos215.RadiusMedium))
                    .background(DriveDesign.Black).clickable(onClick = onDone),
                    contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(16.dp).clip(RoundedCornerShape(3.dp))
                            .background(Color.White))
                        Text("완료", style = MaterialTheme.typography.titleLarge,
                            color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoRecordingWaves(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "녹음 시연 파형")
    val phase by transition.animateFloat(0f, (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1_700, easing = LinearEasing)),
        label = "일렁이는 파형")
    Canvas(modifier) {
        val mid = size.height / 2f
        repeat(3) { layer ->
            val path = Path()
            for (point in 0..100) {
                val fraction = point / 100f
                val x = size.width * fraction
                val envelope = (1f - abs(fraction - .5f) * 1.8f).coerceAtLeast(0f)
                val wave = sin(fraction * PI * (2.4 + layer * .45) + phase + layer * .9)
                val y = mid + wave.toFloat() * size.height * (.08f + layer * .035f) * envelope
                if (point == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, DriveDesign.Blue.copy(alpha = .12f - layer * .025f),
                style = Stroke(width = (12 - layer * 3).dp.toPx(), cap = StrokeCap.Round))
        }
        val gap = size.width / 38f
        for (index in 3..35) {
            val x = gap * index
            if (abs(x - size.width / 2f) < 88.dp.toPx()) continue
            val height = (7f + 26f * abs(sin(index * .71 + phase))).dp.toPx()
            drawLine(DriveDesign.BlueMid.copy(alpha = .55f),
                Offset(x, mid - height / 2f), Offset(x, mid + height / 2f),
                strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun DesignedVoiceCard(s: WireSession, screen: WireScreen, enabled: Boolean,
    setupRestricted: Boolean, voice: LinkCarVoiceRuntime,
    onVoiceClick: () -> Unit, onPollSpeech: () -> Unit, modifier: Modifier) {
    if (screen == WireScreen.CALL) {
        DesignedActiveCallCard(s, enabled, modifier)
        return
    }
    Surface(modifier, color = DriveDesign.Card, shape = RoundedCornerShape(56.dp)) {
        Box(Modifier.fillMaxSize()) {
            Image(painterResource(R.drawable.drive_voice_wave), null,
                Modifier.fillMaxWidth().height(265.dp).align(Alignment.Center).offset(y = (-24).dp),
                contentScale = ContentScale.FillBounds)
            Text(if (screen in setOf(WireScreen.INCOMING_CALL, WireScreen.CALL,
                WireScreen.CALL_DECLINED)) "전화 시연" else "음성 연결",
                Modifier.align(Alignment.TopStart).padding(start = 48.dp, top = 30.dp),
                style = MaterialTheme.typography.labelMedium, color = DriveDesign.Muted)
            Surface(Modifier.align(Alignment.TopEnd).padding(end = 38.dp, top = 30.dp)
                .clickable(enabled && !setupRestricted) { s.showHistory = true },
                color = DriveDesign.Input, shape = CircleShape) {
                Text("이전 대화", Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelSmall, color = DriveDesign.Subtle)
            }
            if (screen in setOf(WireScreen.INCOMING_CALL, WireScreen.CALL_DECLINED,
                    WireScreen.KNOCK_SENT, WireScreen.KNOCK_RECEIVED, WireScreen.KNOCK_FULL,
                    WireScreen.VOICE_LEAVE, WireScreen.VOICE_RECEIVED,
                    WireScreen.VOICE_APPROACH, WireScreen.VOICE_DELIVER) ||
                (s.demoVoiceTriggered && !s.roadVoiceShown) || s.pollDemoVisible) {
            val wavePulse by rememberInfiniteTransition(label = "투표 시연 음성 파형")
                .animateFloat(initialValue = .72f, targetValue = 1.18f,
                    animationSpec = infiniteRepeatable(tween(360), RepeatMode.Reverse),
                    label = "음성 파형 높이")
            Surface(Modifier.align(Alignment.TopCenter).offset(y = 153.dp),
                color = Color.White, shape = CircleShape) {
                Row(Modifier.padding(start = 10.dp, end = 22.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Image(painterResource(when {
                        s.pollDemoVisible -> R.drawable.flow_imgprofilephoto
                        screen in setOf(WireScreen.INCOMING_CALL, WireScreen.CALL_DECLINED) &&
                            s.incomingCaller == "문준 차" -> R.drawable.flow_imgprofilephoto1
                        screen == WireScreen.KNOCK_SENT && s.knockTarget == "문준 차" ->
                            R.drawable.flow_imgprofilephoto1
                        screen in setOf(WireScreen.INCOMING_CALL, WireScreen.CALL_DECLINED,
                            WireScreen.KNOCK_SENT, WireScreen.KNOCK_RECEIVED,
                            WireScreen.KNOCK_FULL) -> R.drawable.flow_imgprofilephoto2
                        else -> R.drawable.drive_avatar
                    }), null,
                        Modifier.size(48.dp).clip(CircleShape))
                    Text(if (s.pollDemoVisible) "본인" else "${if (screen in setOf(WireScreen.INCOMING_CALL, WireScreen.CALL,
                        WireScreen.CALL_DECLINED)) s.incomingCaller else if (screen == WireScreen.KNOCK_SENT)
                        s.knockTarget else "다연 차"} 방금",
                        style = MaterialTheme.typography.labelSmall,
                        color = DriveDesign.Subtle)
                    listOf(10, 20, 32, 20, 14, 26, 16, 10).forEach { height ->
                        Box(Modifier.width(7.dp)
                            .height((height * if (s.pollDemoSpeaking) wavePulse else 1f).dp)
                            .clip(CircleShape)
                            .background(DriveDesign.BlueMid))
                    }
                    Text(when (screen) {
                        WireScreen.DRIVE -> if (s.pollDemoVisible) {
                            if (s.pollDemoSpeaking) "말하는 중..." else "말하기 완료"
                        } else "읽어 주는 중"
                        WireScreen.INCOMING_CALL -> "전화 수신 중"
                        WireScreen.KNOCK_SENT -> if (s.seconds > 0) "응답 기다리는 중" else "응답 없음"
                        WireScreen.CALL -> "대화 화면 · 시연"
                        WireScreen.CALL_DECLINED -> "전화 거절됨"
                        else -> "읽어 주는 중"
                    }, style = MaterialTheme.typography.labelSmall,
                        color = DriveDesign.Subtle)
                }
            }
            }
            Column(Modifier.align(Alignment.TopCenter).fillMaxWidth()
                .padding(horizontal = 32.dp).offset(y = 252.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                val poll = s.generatedPoll
                if (s.pollDemoVisible && screen == WireScreen.DRIVE) {
                    Text("아 배고프다 휴게소 들리면 안되나\n나 밥 먹고 가고 싶은데",
                        style = TextStyle(fontFamily = FFont, fontSize = 54.sp,
                            lineHeight = 66.sp, fontWeight = FontWeight.Bold,
                            color = DriveDesign.Blue),
                        textAlign = TextAlign.Center)
                } else if (screen == WireScreen.INCOMING_CALL) {
                    Text("${s.incomingCaller}에서 전화가 왔어요",
                        style = MaterialTheme.typography.displayLarge,
                        color = DriveDesign.Ink, textAlign = TextAlign.Center)
                    Text(if (s.incomingCaller == "다연 차")
                        "전화 받기를 누르면 30초간 대화 화면이 표시돼요 · ${s.seconds}초"
                        else "${s.seconds}초 후 자동으로 거절돼요",
                        style = MaterialTheme.typography.labelMedium,
                        color = DriveDesign.Subtle, textAlign = TextAlign.Center)
                } else if (screen == WireScreen.CALL_DECLINED) {
                    Text("문준 차의 전화를 거절했어요",
                        style = MaterialTheme.typography.displayLarge,
                        color = DriveDesign.Ink, textAlign = TextAlign.Center)
                    Text("잠시 후 주행 화면으로 돌아가요 · 시연",
                        style = MaterialTheme.typography.labelMedium,
                        color = DriveDesign.Subtle)
                } else if (screen == WireScreen.KNOCK_RECEIVED) {
                    Text("다연 차가 두드렸어요", style = MaterialTheme.typography.displayLarge,
                        color = DriveDesign.Ink, textAlign = TextAlign.Center)
                    Text("문을 열면 30초 동안 바로 대화할 수 있어요 · ${s.seconds}초",
                        style = MaterialTheme.typography.labelSmall, color = DriveDesign.Subtle,
                        textAlign = TextAlign.Center)
                } else if (screen == WireScreen.KNOCK_SENT) {
                    Text("${s.knockTarget}에 두드렸어요", style = MaterialTheme.typography.displayLarge,
                        color = DriveDesign.Ink, textAlign = TextAlign.Center)
                    Text(if (s.seconds > 0) "응답을 기다리는 중이에요 · ${s.seconds}초" else "응답이 없어요",
                        style = MaterialTheme.typography.labelSmall, color = DriveDesign.Subtle,
                        textAlign = TextAlign.Center)
                } else if (screen == WireScreen.CALL) {
                    Text("${s.incomingCaller}와 대화 중 · 시연", style = MaterialTheme.typography.displayLarge,
                        color = DriveDesign.Ink, textAlign = TextAlign.Center)
                    Text("${s.seconds}초 뒤 자동으로 끝나요", style = MaterialTheme.typography.labelMedium,
                        color = DriveDesign.Subtle)
                } else if (screen in setOf(WireScreen.VOICE_LEAVE, WireScreen.VOICE_RECEIVED,
                        WireScreen.VOICE_APPROACH, WireScreen.VOICE_DELIVER)) {
                    Text(when (screen) {
                        WireScreen.VOICE_LEAVE -> "지금은 문을 열 수 없어요"
                        WireScreen.VOICE_RECEIVED -> "길에 목소리를 남겼어요"
                        WireScreen.VOICE_APPROACH -> "앞차의 목소리가 가까워져요"
                        else -> "남긴 목소리를 듣고 있어요"
                    }, style = MaterialTheme.typography.displayLarge, color = DriveDesign.Ink,
                        textAlign = TextAlign.Center)
                    Text("위치와 음성은 현재 로컬 시연 데이터예요",
                        style = MaterialTheme.typography.labelMedium, color = DriveDesign.Subtle)
                } else if (s.demoVoiceTriggered && !s.roadVoiceShown && screen == WireScreen.DRIVE) {
                    Text("다연 차가 길에 남긴 음성", style = MaterialTheme.typography.displayLarge,
                        color = DriveDesign.Ink, textAlign = TextAlign.Center)
                    Text(voice.status,
                        style = MaterialTheme.typography.labelMedium, color = DriveDesign.Subtle,
                        textAlign = TextAlign.Center)
                } else if (poll == null) {
                    Text("아직 보낸 음성이 없어요",
                        style = MaterialTheme.typography.displayLarge, color = DriveDesign.Ink,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                    Text("다른 친구의 차를 두드려보세요",
                        style = MaterialTheme.typography.labelSmall, color = DriveDesign.Muted,
                        textAlign = TextAlign.Center)
                } else {
                    Text(poll.title, style = MaterialTheme.typography.headlineLarge,
                        color = DriveDesign.Ink, textAlign = TextAlign.Center,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        poll.options.forEach { option ->
                            WireButton(option, { s.vote = option }, filled = s.vote == option,
                                enabled = enabled, modifier = Modifier.weight(1f), height = Pleos215.ButtonSmall)
                        }
                    }
                    if (s.vote.isNotEmpty()) Text("선택: ${s.vote} · 로컬 시연 투표",
                        style = MaterialTheme.typography.labelSmall, color = DriveDesign.Subtle)
                }
                if (s.pollBusy) Text("AI가 투표 안건을 만들고 있어요", style = MaterialTheme.typography.labelSmall,
                    color = DriveDesign.Subtle)
                if (s.message.isNotBlank()) Text(s.message, style = MaterialTheme.typography.labelSmall,
                    color = DriveDesign.Subtle, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center)
                if (s.speechFeedbackVisible) {
                    Text(voice.status, style = MaterialTheme.typography.labelSmall,
                        color = DriveDesign.Subtle, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center)
                    if (voice.status.contains("오류") || voice.status.contains("실패")) {
                        WireButton("텍스트로 AI 투표 시험", { s.showPollTextEntry = true }, filled = false,
                            enabled = enabled && !setupRestricted, height = Pleos215.ButtonSmall)
                    }
                }
            }
            if (screen == WireScreen.INCOMING_CALL) {
                WireButton(if (s.incomingCaller == "다연 차") "전화 받기" else "전화 거절",
                    if (s.incomingCaller == "다연 차") s::acceptIncomingCall
                    else s::declineIncomingCall, enabled = enabled,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth())
            } else if (screen == WireScreen.CALL_DECLINED) {
                // The timed rejection state provides visible feedback before returning to driving.
            } else if (screen == WireScreen.CALL) {
                WireButton("대화 끝내기", { s.drive() }, enabled = enabled,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth())
            } else if (screen == WireScreen.VOICE_LEAVE) {
                WireButton("음성 메시지 남기기", onVoiceClick, enabled = enabled,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth())
            } else if (screen == WireScreen.VOICE_RECEIVED || screen == WireScreen.VOICE_DELIVER) {
                Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    WireButton("닫기", { s.drive() }, filled = false,
                        enabled = enabled, modifier = Modifier.weight(1f))
                    WireButton("다시 듣기", {
                        voice.notes.notes.lastOrNull { it.id == s.lastNoteId }?.let { voice.play(it) }
                            ?: run { s.message = "재생할 원본 음성이 없습니다." }
                    }, enabled = enabled, modifier = Modifier.weight(1f))
                }
            } else if (screen == WireScreen.KNOCK_RECEIVED || screen == WireScreen.KNOCK_FULL) {
                Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    WireButton("지금은 어려워요", { s.drive() }, filled = false,
                        enabled = enabled, modifier = Modifier.weight(1f), height = Pleos215.ButtonLarge)
                    WireButton("문 열고 대화하기", s::startKnockCall,
                        enabled = enabled, modifier = Modifier.weight(1.6f), height = Pleos215.ButtonLarge)
                }
            } else if (screen == WireScreen.KNOCK_SENT) {
                Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    WireButton(if (s.seconds > 0) "취소" else "닫기", { s.drive() },
                        filled = false, enabled = enabled, modifier = Modifier.weight(1f))
                    WireButton(if (s.seconds > 0) "응답 시연" else "음성 메시지 남기기",
                        { if (s.seconds > 0) s.startKnockCall() else s.open(WireScreen.VOICE_LEAVE) },
                        enabled = enabled, modifier = Modifier.weight(1.4f))
                }
            } else {
                Surface(Modifier.align(Alignment.BottomCenter).padding(16.dp)
                    .fillMaxWidth().height(120.dp).clickable(enabled, onClick = onVoiceClick),
                    color = DriveDesign.Input, shape = RoundedCornerShape(48.dp)) {
                    Row(Modifier.padding(start = 32.dp, end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("모두에게 말하기", Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge, color = DriveDesign.Muted)
                        Box(Modifier.size(96.dp).clip(RoundedCornerShape(48.dp))
                            .background(DriveDesign.Black), contentAlignment = Alignment.Center) {
                            Image(painterResource(R.drawable.drive_mic), null, Modifier.size(40.dp))
                        }
                    }
                }
            }
            if (s.showVoiceMenu) {
                Column(Modifier.align(Alignment.TopCenter).padding(top = 67.dp)
                    .width(848.dp).height(442.dp)
                    .clip(RoundedCornerShape(56.dp)).background(Color.White.copy(alpha = .96f)),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("모두에게 말하기", Modifier.padding(top = 38.dp),
                        style = TextStyle(fontFamily = FFont, fontSize = 48.sp,
                            fontWeight = FontWeight.Bold, color = DriveDesign.Ink))
                    Text("어떤 방식으로 말할까요?", Modifier.padding(top = 18.dp),
                        style = TextStyle(fontFamily = FFont, fontSize = 26.sp,
                            color = Color(0xFF9398A5)))
                    Spacer(Modifier.height(52.dp))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        VoiceChoiceTile("투표 안건 말하기", R.drawable.drive_vote_option,
                            Color(0xFF44464E), enabled, Modifier.weight(1f)) {
                            s.showVoiceMenu = false
                            onPollSpeech()
                        }
                        VoiceChoiceTile("길에 음성 남기기", R.drawable.drive_road_voice_option,
                            Color(0xFF7583EE), enabled, Modifier.weight(1f)) {
                            s.showVoiceMenu = false
                            s.showVoiceRecorder = true
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceChoiceTile(label: String, icon: Int, color: Color, enabled: Boolean,
    modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.height(230.dp).clip(RoundedCornerShape(48.dp))
        .background(color).clickable(enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Image(painterResource(icon), null, Modifier.size(70.dp))
        Spacer(Modifier.height(24.dp))
        Text(label, style = TextStyle(fontFamily = FFont, fontSize = 36.sp,
            fontWeight = FontWeight.Bold, color = Color.White), maxLines = 1)
    }
}

/** Figma 328:6560, S-22: the accepted 30-second conversation state. */
@Composable
private fun DesignedActiveCallCard(s: WireSession, enabled: Boolean, modifier: Modifier) {
    Surface(modifier, color = DriveDesign.Card, shape = RoundedCornerShape(56.dp)) {
        Box(Modifier.fillMaxSize()) {
            Text("음성 연결", Modifier.offset(49.dp, 34.dp),
                style = MaterialTheme.typography.labelMedium, color = DriveDesign.Muted)
            Canvas(Modifier.offset(72.dp, 150.dp).size(300.dp)) {
                val stroke = 18.dp.toPx()
                val inset = stroke / 2f
                drawCircle(DriveDesign.CallTrack, radius = size.minDimension / 2f - inset,
                    style = Stroke(stroke))
                drawArc(DriveDesign.Blue, -90f, 360f * (s.seconds.coerceIn(0, 30) / 30f),
                    useCenter = false, topLeft = Offset(inset, inset),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke, cap = StrokeCap.Butt))
            }
            Text("${s.seconds}", Modifier.offset(72.dp, 193.dp).width(300.dp),
                style = TextStyle(fontFamily = CallTimerFont,
                    fontSize = 110.sp, fontWeight = FontWeight.Normal),
                color = DriveDesign.Blue, textAlign = TextAlign.Center)
            Text("초 남음", Modifier.offset(72.dp, 340.dp).width(300.dp),
                style = TextStyle(fontFamily = FFont, fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold),
                color = DriveDesign.CallMuted, textAlign = TextAlign.Center)
            Row(Modifier.offset(430.dp, 170.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(painterResource(if (s.incomingCaller == "문준 차")
                    R.drawable.flow_imgprofilephoto1 else R.drawable.flow_imgprofilephoto2),
                    s.incomingCaller,
                    Modifier.size(56.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                Surface(color = DriveDesign.Input, shape = CircleShape) {
                    Text("주행 중", Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelSmall, color = DriveDesign.Subtle)
                }
            }
            Text(buildAnnotatedString {
                withStyle(SpanStyle(color = DriveDesign.Blue)) { append(s.incomingCaller) }
                append("와 대화 중")
            }, Modifier.offset(430.dp, 250.dp),
                style = MaterialTheme.typography.displayLarge, color = DriveDesign.Ink,
                maxLines = 1)
            Text("30초가 지나면 자동으로 끝나요", Modifier.offset(430.dp, 336.dp),
                style = MaterialTheme.typography.titleSmall, color = DriveDesign.CallMuted)
            Row(Modifier.offset(430.dp, 400.dp).height(50.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(14, 28, 45, 28, 20, 36, 22, 14, 31, 17, 39, 20).forEach { height ->
                    Box(Modifier.width(11.dp).height(height.dp)
                        .clip(RoundedCornerShape(6.dp)).background(DriveDesign.CallWave))
                }
            }
            Surface(Modifier.align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .fillMaxWidth().height(120.dp).clickable(enabled) { s.drive() },
                color = DriveDesign.CallRed, shape = RoundedCornerShape(48.dp)) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center) {
                    Text("×", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Spacer(Modifier.width(14.dp))
                    Text("대화 끝내기", style = MaterialTheme.typography.titleLarge, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun DesignedCompanions(s: WireSession, screen: WireScreen, enabled: Boolean, modifier: Modifier) {
    val convoy = LocalConvoy.current
    val motionLabel = if (convoy.speedKph > 0f) "시연 주행" else "시연 정차"
    Surface(modifier, color = DriveDesign.Card, shape = RoundedCornerShape(56.dp)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Row(Modifier.fillMaxWidth().height(90.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("연결된 차량 (2)", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium,
                    color = DriveDesign.Muted)
                Box(Modifier.size(56.dp).alpha(.2f), contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.drive_refresh), null,
                        Modifier.size(56.dp), contentScale = ContentScale.Crop)
                }
            }
            DesignedCompanionRow("다연 차", "$motionLabel · ${(convoy.speedKph * .60f).toInt()} km/h", convoy.dayeonScore,
                remainingRouteKilometers(convoy.dayeonRouteMeters),
                R.drawable.flow_imgprofilephoto2, R.drawable.drive_status_green,
                enabled, true, screen == WireScreen.CALL && s.incomingCaller == "다연 차",
                s.seconds, Modifier.weight(1f)) {
                s.sendKnock("다연 차")
            }
            Image(painterResource(R.drawable.drive_card_divider), null,
                Modifier.fillMaxWidth().height(2.dp), contentScale = ContentScale.FillBounds)
            DesignedCompanionRow("문준 차", "$motionLabel · ${(convoy.speedKph * .40f).toInt()} km/h", convoy.munjunScore,
                remainingRouteKilometers(convoy.munjunRouteMeters),
                R.drawable.flow_imgprofilephoto1, R.drawable.drive_status_green,
                enabled, true, screen == WireScreen.CALL && s.incomingCaller == "문준 차",
                s.seconds, Modifier.weight(1f)) { s.sendKnock("문준 차") }
        }
    }
}

@Composable
private fun DesignedCompanionRow(name: String, state: String, score: Int, distance: String,
    portrait: Int, statusDot: Int, enabled: Boolean, canKnock: Boolean,
    inCall: Boolean, callSeconds: Int, modifier: Modifier, onKnock: () -> Unit) {
    Row(modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(90.dp)) {
                    Image(painterResource(portrait), null, Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop)
                    Image(painterResource(statusDot), null,
                        Modifier.align(Alignment.BottomEnd).size(30.dp))
                }
                Spacer(Modifier.width(18.dp))
                Column {
                    Text(name, style = MaterialTheme.typography.titleLarge,
                        color = DriveDesign.Ink)
                    Text(state, style = MaterialTheme.typography.labelSmall,
                        color = if (canKnock) DriveDesign.Green else DriveDesign.Red)
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$score", style = MaterialTheme.typography.displayMedium,
                        color = if (canKnock) DriveDesign.Blue else DriveDesign.Red)
                    Text("주행 점수", style = MaterialTheme.typography.labelSmall,
                        color = DriveDesign.Subtle)
                }
                Image(painterResource(R.drawable.drive_link_line_a), null,
                    Modifier.width(2.dp).height(53.dp), contentScale = ContentScale.FillBounds)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(distance, style = MaterialTheme.typography.displayMedium,
                            color = DriveDesign.Ink)
                        Text("km", style = MaterialTheme.typography.labelSmall,
                            color = DriveDesign.Subtle)
                    }
                    Text("목적지까지", style = MaterialTheme.typography.labelSmall,
                        color = DriveDesign.Subtle, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(if (inCall) 240.dp else 220.dp)
            .then(if (inCall) Modifier.border(6.dp, DriveDesign.BlueMid, CircleShape)
                else Modifier.clip(CircleShape)
                    .background(if (canKnock) DriveDesign.Black else DriveDesign.Input))
            .then(if (canKnock && !inCall) Modifier.clickable(enabled, onClick = onKnock)
                .semantics { contentDescription = "$name 두드리기" } else Modifier),
            contentAlignment = Alignment.Center) {
            if (inCall) {
                Box(Modifier.size(220.dp).clip(CircleShape).background(DriveDesign.Blue),
                    contentAlignment = Alignment.Center) {
                    Column(Modifier.offset(y = (-5).dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$callSeconds", style = TextStyle(fontFamily = CallTimerFont,
                            fontSize = 64.sp, fontWeight = FontWeight.Normal),
                            color = Color.White)
                        Spacer(Modifier.height(4.dp))
                        Text("대화 중", style = TextStyle(fontFamily = FFont,
                            fontSize = 24.sp, fontWeight = FontWeight.Bold),
                            color = Color.White)
                    }
                }
            } else if (canKnock) {
                Image(painterResource(R.drawable.drive_knock_hand), null,
                    Modifier.size(220.dp), contentScale = ContentScale.Fit)
            } else Image(painterResource(R.drawable.drive_locked), "정차 중 · 두드리기 불가",
                Modifier.size(74.dp), contentScale = ContentScale.Fit)
        }
    }
}

@Composable
private fun DesignedMusicBar(s: WireSession, enabled: Boolean, motionAllowed: Boolean, height: Dp) {
    val beat = if (motionAllowed && s.music && s.playing) {
        val beatTransition = rememberInfiniteTransition(label = "music glow")
        val animatedBeat by beatTransition.animateFloat(0f, 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 520, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse), label = "beat")
        animatedBeat
    } else 0f
    val blueGlow = if (!s.music) 0f else if (s.playing) .16f + .11f * beat else .12f
    val purpleGlow = if (!s.music) 0f else if (s.playing) .13f + .08f * beat else .10f
    val rise = if (motionAllowed && s.music && s.playing) beat * 5f else 0f
    Surface(Modifier.fillMaxWidth().height(height), color = DriveDesign.Card) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(0.dp))) {
            Image(painterResource(R.drawable.drive_music_wave_blue), null,
                Modifier.offset(x = (-125).dp, y = (80f - rise).dp)
                    .size(1079.dp, 537.dp),
                contentScale = ContentScale.FillBounds, alpha = blueGlow)
            Image(painterResource(R.drawable.drive_music_wave_purple), null,
                Modifier.align(Alignment.BottomEnd)
                    .offset(x = 95.dp, y = (140f - rise * .6f).dp)
                    .size(1155.dp, 259.dp), contentScale = ContentScale.FillBounds,
                alpha = purpleGlow)
            Row(Modifier.fillMaxSize().padding(start = 44.dp, end = 24.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.drive_album_art), "앨범 이미지",
                    Modifier.size(156.dp).clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop)
                Spacer(Modifier.width(40.dp))
                Column(Modifier.width(300.dp)) {
                    Text(if (s.music) listOf("밤편지", "여행", "좋은 밤")[s.song] else "노래명 입력란",
                        style = MaterialTheme.typography.titleLarge, color = DriveDesign.Ink,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (s.music) "아이유" else "가수 명 입력란",
                        style = MaterialTheme.typography.labelSmall, color = DriveDesign.Subtle)
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.drive_music_car), null,
                            Modifier.size(44.dp, 25.dp), contentScale = ContentScale.Crop)
                        Text("문준 차 신청", style = MaterialTheme.typography.labelSmall,
                            color = DriveDesign.Subtle)
                    }
                }
                Spacer(Modifier.width(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    MusicIcon(R.drawable.drive_media_previous, "이전 곡", enabled && s.music) {
                        s.skipSong(-1)
                    }
                    MusicIcon(if (s.playing) R.drawable.drive_media_pause
                        else R.drawable.drive_media_play, if (s.playing) "일시정지" else "재생",
                        enabled && s.music) { s.playing = !s.playing }
                    MusicIcon(R.drawable.drive_media_next, "다음 곡", enabled && s.music) {
                        s.skipSong(1)
                    }
                }
                Spacer(Modifier.width(156.dp))
                Image(painterResource(R.drawable.drive_music_divider), null,
                    Modifier.width(2.dp).height(122.dp), contentScale = ContentScale.FillBounds)
                Spacer(Modifier.width(38.dp))
                Column(Modifier.width(220.dp)) {
                    Text("다음 곡 3", style = MaterialTheme.typography.labelSmall,
                        color = DriveDesign.Muted)
                    Text("노래명 입력란", style = MaterialTheme.typography.titleLarge,
                        color = DriveDesign.Muted, maxLines = 1)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.drive_music_car), null,
                            Modifier.size(44.dp, 25.dp), contentScale = ContentScale.Crop)
                        Text("다연 차 신청", style = MaterialTheme.typography.labelSmall,
                            color = DriveDesign.Subtle)
                    }
                }
                Spacer(Modifier.weight(1f))
                Box(Modifier.width(290.dp).height(125.dp)) {
                    listOf(R.drawable.drive_record_white, R.drawable.drive_record_black,
                        R.drawable.drive_record_blue).forEachIndexed { index, disc ->
                        Box(Modifier.offset(x = (index * 82).dp).size(125.dp)) {
                            Image(painterResource(disc), null, Modifier.fillMaxSize())
                            Image(painterResource(R.drawable.drive_record_center), null,
                                Modifier.align(Alignment.Center).size(33.dp))
                        }
                    }
                }
            }
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp)
                .background(DriveDesign.Track.copy(alpha = .4f))) {
                Box(Modifier.fillMaxWidth(s.musicProgress).fillMaxHeight().background(Color.White))
            }
        }
    }
}

@Composable
private fun DesignedMusicExpanded(s: WireSession, enabled: Boolean, height: Dp) {
    Surface(Modifier.fillMaxWidth().height(height), color = DriveDesign.Card) {
        Row(Modifier.fillMaxSize().padding(30.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            Image(painterResource(R.drawable.drive_album_art), "앨범 이미지",
                Modifier.size(400.dp).clip(RoundedCornerShape(32.dp)), contentScale = ContentScale.Crop)
            Column(Modifier.weight(1f)) {
                Text("함께 듣는 DJ", style = MaterialTheme.typography.labelMedium,
                    color = DriveDesign.Subtle)
                Spacer(Modifier.height(16.dp))
                Text(if (s.music) listOf("밤편지", "여행", "좋은 밤")[s.song] else "재생할 곡 없음",
                    style = MaterialTheme.typography.displayLarge, color = DriveDesign.Ink)
                Text(if (s.music) "아이유 · 문준 차 신청" else "음악 없이 출발했어요",
                    style = MaterialTheme.typography.labelMedium, color = DriveDesign.Subtle)
                Spacer(Modifier.height(40.dp))
                LinearProgressIndicator(progress = { s.musicProgress }, modifier = Modifier.fillMaxWidth(),
                    color = DriveDesign.Black, trackColor = DriveDesign.Input)
                Spacer(Modifier.height(30.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    MusicIcon(R.drawable.drive_media_previous, "이전 곡", enabled && s.music) {
                        s.skipSong(-1)
                    }
                    MusicIcon(if (s.playing) R.drawable.drive_media_pause
                        else R.drawable.drive_media_play, if (s.playing) "일시정지" else "재생",
                        enabled && s.music) { s.playing = !s.playing }
                    MusicIcon(R.drawable.drive_media_next, "다음 곡", enabled && s.music) {
                        s.skipSong(1)
                    }
                }
            }
            Surface(Modifier.width(300.dp).fillMaxHeight(), color = Color.White,
                shape = RoundedCornerShape(32.dp)) {
                Column(Modifier.padding(24.dp)) {
                    Text("다음 곡 3", style = MaterialTheme.typography.labelMedium,
                        color = DriveDesign.Subtle)
                    repeat(3) { index ->
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(listOf(R.drawable.drive_record_blue,
                                R.drawable.drive_record_white,R.drawable.drive_record_black)[index]),
                                null, Modifier.size(76.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("노래명 입력란", style = MaterialTheme.typography.labelSmall,
                                color = DriveDesign.Ink)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicIcon(icon: Int, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(85.dp).clip(CircleShape).clickable(enabled, onClick = onClick)
        .semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Image(painterResource(icon), null, Modifier.size(42.dp))
    }
}
