package com.contest.travel.preview

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.*
import kotlin.math.min

/** App-only styles from the nineteen frames in Figma section 443:20589. */
internal object DrivingFigma {
    val Ink = Color(0xFF131417)
    val Title = Color(0xFF2E323A)
    val Muted = Color(0xFFACB1C0)
    val Hint = Color(0xFF8F919F)
    val Subtle = Color(0xFF686A72)
    val Input = Color(0xFFEEEFF4)
    val Blue = Color(0xFF5263E7)
    val ScoreBlue = Color(0xFF5668EF)
    val Pale = Color(0xFFE7EAFF)
    val Mid = Color(0xFFBEC6FF)
    val Wave = Color(0xFF96A2FF)
    val Green = Color(0xFF19BB62)
    val Red = Color(0xFFFF7777)
    val Black = Color(0xFF191A1B)
    val Disabled = Color(0xFF9EA4B1)
    val PenaltyInk = Color(0xFFFF6B6E)
    val PenaltySurface = Color(0xFFFFE5E6)
    val PenaltyBorder = Color(0xFFFFD1D1)
    val Card = Color.White.copy(alpha = .8f)
    val Lufga = FontFamily(Font(R.font.lufga_regular))
    @OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
    val Outfit = FontFamily(Font(R.font.outfit_variable, variationSettings = FontVariation.Settings(FontVariation.weight(400))))
}

internal enum class DType(val size: Int, val line: Float, val weight: FontWeight = FontWeight.SemiBold,
    val numeric: Int = 0, val tracking: Float = 0f) {
    Header(32, 42f, FontWeight.Bold, tracking = -.32f),
    Subtitle(20, 28f, FontWeight.Medium), Eyebrow(24, 29f, FontWeight.Bold, tracking = -.24f),
    Body(32, 38f, FontWeight.Bold), Song(32, 40f, FontWeight.Bold, tracking = -.32f),
    Meta(20, 24f, FontWeight.Bold), Small(18, 22f), Tiny(16, 19f),
    Description(28, 34f), Caption(22, 26f), Headline(60, 72f, FontWeight.Bold),
    CompactHeadline(40, 48f, FontWeight.Bold), ModalTitle(48, 58f, FontWeight.Bold),
    ModalSub(26, 36.4f, FontWeight.Medium), Choice(36, 43f, FontWeight.Bold),
    Place(26, 31f, FontWeight.Bold),
    Score(56, 73f, FontWeight.Normal, 1), Unit(24, 31f, FontWeight.Normal, 1),
    Battery(20, 26f, FontWeight.Normal, 1), Clock(110, 138f, FontWeight.Normal, 2),
    Seconds(32, 40f, FontWeight.Normal, 2), CompactScore(44, 55f, FontWeight.Normal, 1),
    CompactName(28, 34f, FontWeight.Bold), Micro(14, 17f),
    CompactUnit(18, 23f, FontWeight.Normal, 1), CallBadge(72, 90f, FontWeight.Normal, 2),
    Strong(56, 67f, FontWeight.Bold), Quote(48, 60f, FontWeight.Bold),
    SettingTitle(44, 53f, FontWeight.Bold), SettingDetail(22, 26f, FontWeight.Medium),
    InputHint(32, 38f), Descriptive(26, 31f), Label(20, 24f),
    Name(32, 38f, FontWeight.Bold, tracking = -.32f),
    Proposal(60, 64f, FontWeight.Bold, tracking = -1.2f),
    Thought(40, 64f, FontWeight.Bold, tracking = -.8f),
    ThoughtSmall(32, 64f, FontWeight.Bold, tracking = -.64f),
    SongTime(20, 25f, FontWeight.Normal, 2), SubLabel(24, 29f);

    fun style(color: Color = DrivingFigma.Ink, align: TextAlign = TextAlign.Start) = TextStyle(
        fontFamily = when (numeric) { 1 -> DrivingFigma.Lufga; 2 -> DrivingFigma.Outfit; else -> FFont },
        fontWeight = weight, fontSize = size.sp, lineHeight = line.sp,
        letterSpacing = tracking.sp, color = color, textAlign = align,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        shadow = if (this == Thought || this == ThoughtSmall)
            Shadow(Color.White.copy(alpha = .5f), androidx.compose.ui.geometry.Offset.Zero, 16f) else null)
}

internal fun Modifier.dBounds(x: Number, y: Number, w: Number, h: Number): Modifier =
    offset(x.toFloat().dp, y.toFloat().dp).wrapContentSize(Alignment.TopStart, unbounded = true)
        .requiredSize(w.toFloat().dp, h.toFloat().dp)

@Composable
internal fun DText(text: String, x: Number, y: Number, type: DType = DType.Body,
    color: Color = DrivingFigma.Ink, w: Number? = null, align: TextAlign = TextAlign.Start,
    lines: Int = 1) = DText(AnnotatedString(text), x, y, type, color, w, align, lines)

@Composable
internal fun DText(text: AnnotatedString, x: Number, y: Number, type: DType = DType.Body,
    color: Color = DrivingFigma.Ink, w: Number? = null, align: TextAlign = TextAlign.Start,
    lines: Int = 1) {
    Text(text, Modifier.offset(x.toFloat().dp, y.toFloat().dp)
        .then(if (w != null) Modifier.width(w.toFloat().dp) else Modifier),
        style = type.style(color, align), maxLines = lines, overflow = TextOverflow.Ellipsis)
}

@Composable
internal fun DImage(id: Int, x: Number, y: Number, w: Number, h: Number,
    modifier: Modifier = Modifier, description: String? = null, scale: ContentScale = ContentScale.FillBounds) {
    Image(painterResource(id), description, Modifier.dBounds(x, y, w, h).then(modifier), contentScale = scale)
}

@Composable
internal fun DPanel(x: Number, y: Number, w: Number, h: Number, radius: Number = 56,
    color: Color = DrivingFigma.Card, modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}) {
    Box(Modifier.dBounds(x, y, w, h).then(modifier).clip(RoundedCornerShape(radius.toFloat().dp))
        .background(color), content = content)
}

@Composable
internal fun DCrop(id: Int, x: Number, y: Number, w: Number, h: Number,
    imageX: Number = 0, imageY: Number = 0, imageW: Number = w, imageH: Number = h,
    radius: Number = 0, alpha: Float = 1f, modifier: Modifier = Modifier) {
    Box(Modifier.dBounds(x, y, w, h).then(modifier).clip(RoundedCornerShape(radius.toFloat().dp))
        .graphicsLayer { this.alpha = alpha }) {
        DImage(id, imageX, imageY, imageW, imageH)
    }
}

@Composable
internal fun DButton(text: String, x: Number, y: Number, w: Number, h: Number = 120,
    enabled: Boolean = true, color: Color = DrivingFigma.Black, ink: Color = Color.White,
    icon: Int? = null, iconSize: Number = 36, iconAfter: Boolean = false,
    type: DType = DType.Body,
    onClick: () -> Unit) {
    DPanel(x, y, w, h, 48, color, Modifier.linkCarPress(enabled, onClick = onClick)) {
        Row(Modifier.align(Alignment.Center).offset(x = if (iconAfter) 18.dp else 0.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (icon != null && !iconAfter) Image(painterResource(icon), null,
                Modifier.size(iconSize.toFloat().dp))
            Text(text, Modifier.widthIn(max = (w.toFloat() - 40 - if (icon != null) iconSize.toFloat() + 14 else 0f).dp),
                style = type.style(ink), maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (icon != null && iconAfter) Image(painterResource(icon), null,
                Modifier.size(iconSize.toFloat().dp))
        }
    }
}

@Composable
internal fun DChip(text: String, x: Number, y: Number, w: Number, h: Number = 37,
    color: Color = DrivingFigma.Input, ink: Color = DrivingFigma.Subtle,
    type: DType = DType.Small,
    onClick: (() -> Unit)? = null) {
    DPanel(x, y, w, h, 100, color,
        if (onClick != null) Modifier.linkCarPress(onClick = onClick) else Modifier) {
        Text(text, Modifier.align(Alignment.Center), style = type.style(ink))
    }
}

internal fun dAccent(prefix: String, accent: String, suffix: String) = buildAnnotatedString {
    append(prefix); withStyle(SpanStyle(color = DrivingFigma.Blue)) { append(accent) }; append(suffix)
}

@Composable
internal fun DWave(x: Number, y: Number, scale: Float = 1f, long: Boolean = false,
    color: Color = DrivingFigma.Wave, bars: List<Int>? = null) {
    val heights = bars ?: (listOf(10, 20, 32, 20, 14, 26, 16, 10) + if (long) listOf(22, 12, 28, 14) else emptyList())
    heights.forEachIndexed { i, h -> DPanel(x.toFloat() + i * 14 * scale,
        y.toFloat() + (32 - h) * scale / 2, 8 * scale, h * scale, 4 * scale, color) }
}

@Composable
internal fun DVoiceHeader(x: Number, y: Number, who: String = "다연 차 방금",
    avatar: Int = R.drawable.driving_55f82, tail: String = "읽어 주는 중", width: Number = 416) {
    DPanel(x, y, width, 68, 34, Color.White) {
        Row(Modifier.fillMaxSize().padding(start = 10.dp, end = 24.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(painterResource(avatar), null, Modifier.size(48.dp))
            Text(who, style = DType.Meta.style(DrivingFigma.Subtle), maxLines = 1)
            Box(Modifier.size(106.dp, 32.dp)) { DWave(0, 0) }
            Text(tail, style = DType.Label.style(DrivingFigma.Subtle), maxLines = 1)
        }
    }
}

internal enum class DFeed { IDLE, MESSAGE, MENU, LISTENING, PROPOSAL, SENT, INCOMING,
    CALL, UNAVAILABLE, RECEIVED, ROAD_NEAR, ROAD_DELIVER, HISTORY, RECORDING, CALL_DECLINED }
internal data class DCommunicationKey(val feed: DFeed, val source: String)

internal val LocalDrivingAssets = staticCompositionLocalOf { drivingFigmaAssets.getValue("443:21058") }
@Composable internal fun drivingAsset(key: String, fallback: Int): Int = LocalDrivingAssets.current[key] ?: fallback

/** Authored snapshots keep their source photos; interactive scenes bind portraits to people. */
@Composable
internal fun drivingCompanionImage(s: WireSession, person: String, key: String, fallback: Int): Int =
    if (s.designPreviewNode != null && !s.motionPreview) drivingAsset(key, fallback) else when (person) {
        "다연 차" -> R.drawable.driving_cd7ef
        "문준 차" -> R.drawable.driving_1491e
        "나" -> R.drawable.driving_07296
        else -> fallback
    }

internal fun drivingFigmaNode(s: WireSession, screen: WireScreen): String = when {
    // Modal snapshots use their authored background; live dialogs retain the current screen's assets.
    s.designPreviewNode in setOf("443:24791", "443:25023") -> s.designPreviewNode!!
    screen == WireScreen.MAP -> if (s.waypoint) "443:22700" else "443:22556"
    screen == WireScreen.DJ -> "443:22856"
    else -> when (drivingFeed(s, screen)) {
        DFeed.IDLE -> "443:21947"; DFeed.MESSAGE -> "443:21058"; DFeed.MENU -> "443:22235"
        DFeed.LISTENING -> "443:21359"; DFeed.PROPOSAL -> "443:23005"; DFeed.SENT -> "443:23212"
        DFeed.INCOMING -> "443:23801"; DFeed.CALL -> if (s.designPreviewNode == "443:23343") "443:23343" else "443:24027"
        DFeed.UNAVAILABLE -> "443:23484"; DFeed.RECEIVED -> "443:23612"
        DFeed.ROAD_NEAR -> "443:24166"; DFeed.ROAD_DELIVER -> "443:24491"
        DFeed.HISTORY -> "443:24648"; else -> "443:21058"
    }
}

internal fun drivingFeed(s: WireSession, screen: WireScreen): DFeed = when {
    s.showHistory -> DFeed.HISTORY
    s.showVoiceRecorder -> DFeed.RECORDING
    s.showVoiceMenu -> DFeed.MENU
    screen == WireScreen.KNOCK_SENT -> DFeed.SENT
    screen in setOf(WireScreen.KNOCK_RECEIVED, WireScreen.KNOCK_FULL, WireScreen.INCOMING_CALL) -> DFeed.INCOMING
    screen == WireScreen.CALL -> DFeed.CALL
    screen == WireScreen.CALL_DECLINED -> DFeed.CALL_DECLINED
    screen == WireScreen.VOICE_LEAVE -> DFeed.UNAVAILABLE
    screen == WireScreen.VOICE_RECEIVED -> DFeed.RECEIVED
    screen == WireScreen.VOICE_DELIVER || s.roadVoiceShown -> DFeed.ROAD_DELIVER
    screen == WireScreen.VOICE_APPROACH -> DFeed.ROAD_NEAR
    s.speechFeedbackVisible || s.pollBusy || s.pollDemoVisible -> DFeed.LISTENING
    s.proposalVisible || s.generatedPoll != null -> DFeed.PROPOSAL
    s.drivingIdle -> DFeed.IDLE
    else -> DFeed.MESSAGE
}

/** Keep one 1700 × 1184 composition alive as module sizes and M2 state change. */
@Composable
internal fun ColumnScope.FigmaDriving(s: WireSession, screen: WireScreen, enabled: Boolean,
    setupRestricted: Boolean, voice: LinkCarVoiceRuntime,
    onVoiceClick: () -> Unit, onResetDemo: () -> Unit, onPollSpeech: () -> Unit) {
    BoxWithConstraints(Modifier.weight(1f)) {
        val density = LocalDensity.current
        val fit = min(maxWidth.value / 1700f, maxHeight.value / 1184f)
        CompositionLocalProvider(LocalDensity provides Density(density.density * fit, density.fontScale)) {
            var mapDragging by remember { mutableStateOf(false) }
            var musicDragging by remember { mutableStateOf(false) }
            var mapTarget by remember { mutableFloatStateOf(if (screen == WireScreen.MAP) 560f else 165f) }
            var musicTarget by remember { mutableFloatStateOf(if (screen == WireScreen.DJ) 560f else 200f) }
            LaunchedEffect(screen) {
                mapTarget = if (screen == WireScreen.MAP) 560f else 165f
                musicTarget = if (screen == WireScreen.DJ) 560f else 200f
            }
            val mapHeight by animateFloatAsState(mapTarget,
                if (mapDragging) snap() else tween(300), label = "drivingMapHeight")
            val musicHeight by animateFloatAsState(musicTarget,
                if (musicDragging) snap() else tween(300), label = "drivingMusicHeight")
            val feed = drivingFeed(s, screen)
            val compact = mapHeight >= 280f || musicHeight >= 300f
            val bodyY = 130f + mapHeight
            val bodyH = 1184f - musicHeight - (32f - 8f * (musicHeight - 200f) / 360f) - bodyY
            CompositionLocalProvider(LocalDrivingAssets provides drivingFigmaAssets.getValue(drivingFigmaNode(s, screen))) {
            Box(Modifier.size(1700.dp, 1184.dp).align(Alignment.Center).clip(RoundedCornerShape(0.dp))
                .background(DesignedJourneyBackdrop)) {
                DHeader(s, enabled && !setupRestricted)
                DRoute(s, screen, mapHeight, enabled, voice, onResetDemo)
                DPanel(24, bodyY, 1022, bodyH) {
                    val feedSource = if (s.designPreviewNode == null) drivingFigmaNode(s, WireScreen.DRIVE)
                        else drivingFigmaNode(s, screen)
                    val communication = DCommunicationKey(feed, feedSource)
                    AnimatedContent(targetState = communication,
                        transitionSpec = { fadeIn(tween(220)).togetherWith(fadeOut(tween(140))) },
                        label = "drivingCommunication") { (target, source) ->
                        CompositionLocalProvider(LocalDrivingAssets provides drivingFigmaAssets.getValue(source)) {
                        Box(Modifier.requiredSize(1022.dp, bodyH.dp)) {
                            DFeedContent(s, target, compact, bodyH,
                                enabled && communication == DCommunicationKey(target, source),
                                setupRestricted, voice, onVoiceClick, onPollSpeech)
                        }
                        }
                    }
                }
                DCompanions(s, screen, feed, bodyY, bodyH, compact, enabled)
                DMusic(s, musicHeight, musicHeight >= 300f, enabled)
                DResizeTargets(screen, mapHeight, musicHeight, enabled, onMapDrag = { delta ->
                        mapDragging = true
                        mapTarget = (mapTarget + delta / density.density / fit).coerceIn(165f, 560f)
                    }, onMapRelease = {
                        mapDragging = false
                        if (mapTarget >= 280f) {
                            mapTarget = 560f
                            if (screen != WireScreen.MAP) s.open(WireScreen.MAP)
                        } else {
                            mapTarget = 165f
                            if (screen == WireScreen.MAP) s.drive()
                        }
                    }, onMusicDrag = { delta ->
                        musicDragging = true
                        musicTarget = (musicTarget - delta / density.density / fit).coerceIn(200f, 560f)
                    }, onMusicRelease = {
                        musicDragging = false
                        if (musicTarget >= 300f) {
                            musicTarget = 560f
                            if (screen != WireScreen.DJ) s.open(WireScreen.DJ)
                        } else {
                            musicTarget = 200f
                            if (screen == WireScreen.DJ) s.drive()
                        }
                    })
                if (s.message.isNotBlank() && s.designPreviewNode == null) {
                    DChip(s.message.take(85), 200, 1148, 1300, 32,
                        color = DrivingFigma.Input, ink = DrivingFigma.Subtle)
                }
                DDialogs(s, enabled && !setupRestricted)
                if (!compact && feed in setOf(DFeed.MESSAGE, DFeed.LISTENING) && !s.showSettings && !s.showEnd)
                    Box(Modifier.fillMaxSize().border(6.dp, Color(0xFFC9CDD4)))
            }
            }
        }
    }
}

@Composable
private fun DHeader(s: WireSession, enabled: Boolean) {
    val authoredModalSnapshot = s.designPreviewNode in setOf("443:24791", "443:25023")
    DText(if (s.destination.startsWith("가평")) "가평 계곡 여행" else "${s.destination} 여행", 56, 18,
        DType.Header, DrivingFigma.Title)
    DText("순위는 안전 운전 점수로 정해져요", 56, 62, DType.Subtitle, Color(0xFF9398A5))
    if (s.drivingSimulation) {
        val motion = LocalVehicleMotion.current
        DChip(if (motion.error != null) "운전 신호를 확인해 주세요" else "다연 차 운전 시연 · ${motion.gearLabel}",
            984, 30, 300, 42)
    }
    if (!authoredModalSnapshot) {
    DPanel(1309, 25, 223, 56, 12, Color(0xFFF0F2F6))
    val real = LocalConvoy.current.useRealCar && s.designPreviewNode == null
    val live = LocalTripLive.current
    val range = if (!real) "462km" else live.meters.value?.takeIf { live.meters.status == ReadStatus.READY }
        ?.let { "${(it / 1000).toInt()}km" } ?: "—"
    val battery = if (!real) 80 else live.battery.value?.takeIf { live.battery.status == ReadStatus.READY }
    DText(range, 1312, 40, DType.Battery, DrivingFigma.Hint, 82, TextAlign.Center)
    DText(battery?.let { "$it%" } ?: "—", 1395, 40, DType.Battery, DrivingFigma.Hint, 53, TextAlign.Center)
    DImage(R.drawable.driving_c5303, 1392, 45, 1, 16)
    repeat(8) { i -> DPanel(1450 + i * 9, 37, 7, 32, 2,
        if (battery != null && i < (battery * 8 / 100f).toInt()) Color(0xFF52E79C) else Color(0xFFDDDFE4)) }
    }
    DPanel(1540, 12, 68, 72, 0, Color.Transparent,
        Modifier.semantics { contentDescription = "설정" }.linkCarPress(enabled) {
            s.showSettings = true
        }) { DImage(R.drawable.driving_1ce48, 16, if (authoredModalSnapshot) 14 else 17, 48, 48) }
    DPanel(1602, 12, 68, 72, 0, Color.Transparent,
        Modifier.semantics { contentDescription = "동행 종료" }.linkCarPress(enabled) {
            s.showEnd = true
        }) { DImage(R.drawable.driving_81be0, 10, if (authoredModalSnapshot) 14 else 17, 48, 48) }
}

@Composable
private fun DResizeTargets(screen: WireScreen, mapHeight: Float, musicHeight: Float, enabled: Boolean,
    onMapDrag: (Float) -> Unit, onMapRelease: () -> Unit,
    onMusicDrag: (Float) -> Unit, onMusicRelease: () -> Unit) {
    val canResize = enabled && screen in setOf(WireScreen.DRIVE, WireScreen.MAP, WireScreen.DJ)
    DriveResizeGrip(GripAxis.VERTICAL, "지도를 아래로 끌어 펼치거나 위로 끌어 접기", canResize,
        Modifier.offset(802.dp, (106f + mapHeight - 24f).dp),
        onDrag = onMapDrag, onRelease = { onMapRelease() })
    DriveResizeGrip(GripAxis.VERTICAL, "음악을 위로 끌어 펼치거나 아래로 끌어 접기", canResize,
        Modifier.offset(802.dp, (1184f - musicHeight + 8f).dp),
        onDrag = onMusicDrag, onRelease = { onMusicRelease() })
}
