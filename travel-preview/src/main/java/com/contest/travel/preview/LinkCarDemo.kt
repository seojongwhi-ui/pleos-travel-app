package com.contest.travel.preview

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.ConnectException

/** The 21 frames in Figma section 252:11453. */
internal enum class WireScreen {
    HOME, CREATE, INVITE, WAITING, MISSION, MUSIC_PICK, FIRST_SONG,
    DRIVE, MAP, DJ, KNOCK_SENT, KNOCK_RECEIVED, KNOCK_FULL, INCOMING_CALL, CALL, CALL_DECLINED,
    VOICE_LEAVE, VOICE_RECEIVED, VOICE_APPROACH, VOICE_DELIVER,
    ARRIVED, SUMMARY, SAVE;

    val isDriving get() = this in setOf(DRIVE, MAP, DJ, KNOCK_SENT, KNOCK_RECEIVED,
        KNOCK_FULL, INCOMING_CALL, CALL, CALL_DECLINED,
        VOICE_LEAVE, VOICE_RECEIVED, VOICE_APPROACH, VOICE_DELIVER)
}

internal class WireSession {
    val stack = mutableStateListOf(WireScreen.HOME)
    val screen get() = stack.last()
    var purpose by mutableStateOf("친구 여행")
    var destination by mutableStateOf("가평 용추계곡")
    var createPurposeSelection by mutableStateOf<String?>(null)
    var createRecentSelection by mutableStateOf<String?>(null)
    var destinationQuery by mutableStateOf("")
    var invitees by mutableStateOf(setOf("다연", "문준"))
    var phone by mutableStateOf("")
    var mission by mutableStateOf(true)
    var music by mutableStateOf(true)
    var playing by mutableStateOf(true)
    var song by mutableIntStateOf(0)
    var musicPositionSeconds by mutableIntStateOf(0)
    val musicProgress get() = musicPositionSeconds / 180f

    fun skipSong(direction: Int) {
        song = (song + direction + 3) % 3
        musicPositionSeconds = 0
    }

    fun advanceMusic() {
        if (!music || !playing) return
        musicPositionSeconds++
        if (musicPositionSeconds >= 180) skipSong(1)
    }
    var progress by mutableFloatStateOf(0f)
    var tripMotionStarted by mutableStateOf(false)
    var demoVoiceTriggered by mutableStateOf(false)
    var vote by mutableStateOf("")
    var waypoint by mutableStateOf(false)
    var knockTarget by mutableStateOf("다연 차")
    var seconds by mutableIntStateOf(10)
    var incomingCaller by mutableStateOf("다연 차")
    var roadVoiceShown by mutableStateOf(false)
    var saved by mutableStateOf(false)
    var showSettings by mutableStateOf(false)
    var settingsToggles by mutableStateOf(listOf(true, true, false, true, false))
    var showEnd by mutableStateOf(false)
    var showHistory by mutableStateOf(false)
    var message by mutableStateOf("")
    var wager by mutableStateOf(false)
    var generatedPoll by mutableStateOf<GeneratedPoll?>(null)
    var pollBusy by mutableStateOf(false)
    var showVoiceMenu by mutableStateOf(false)
    var speechFeedbackVisible by mutableStateOf(false)
    var pollDemoVisible by mutableStateOf(false)
    var pollDemoSpeaking by mutableStateOf(false)
    var companionsExpanded by mutableStateOf(false)
    var showVoiceRecorder by mutableStateOf(false)
    var showPollTextEntry by mutableStateOf(false)
    var pollDraft by mutableStateOf("")
    var lastNoteId by mutableStateOf<String?>(null)

    fun openVoiceUi() {
        showVoiceMenu = true
    }

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
    fun acceptIncomingCall() {
        if (screen != WireScreen.INCOMING_CALL || incomingCaller != "다연 차") return
        open(WireScreen.CALL)
    }
    fun simulateIncomingCall(caller: String) {
        incomingCaller = caller
        showSettings = false
        open(WireScreen.INCOMING_CALL)
    }
    fun startKnockCall() {
        incomingCaller = knockTarget
        open(WireScreen.CALL)
    }
    fun sendKnock(target: String) {
        knockTarget = target
        open(WireScreen.KNOCK_SENT)
    }
    fun declineIncomingCall() {
        if (screen != WireScreen.INCOMING_CALL) return
        open(WireScreen.CALL_DECLINED)
    }
    fun home() {
        stack.clear()
        stack.add(WireScreen.HOME)
        createPurposeSelection = null
        createRecentSelection = null
        destinationQuery = ""
        progress = 0f
        song = 0
        musicPositionSeconds = 0
        playing = true
        tripMotionStarted = false
        demoVoiceTriggered = false
        incomingCaller = "다연 차"
        roadVoiceShown = false
        companionsExpanded = false
        speechFeedbackVisible = false
        pollDemoVisible = false
        pollDemoSpeaking = false
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
internal fun LinkCarDemoScreen(wide: Boolean, enabled: Boolean, setupRestricted: Boolean,
    foreground: Boolean,
    onExit: () -> Unit, modifier: Modifier = Modifier) {
    val s = remember { WireSession() }
    val context = LocalContext.current
    val voice = remember { LinkCarVoiceRuntime(context) }
    val demoAudioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val imported = voice.importDemoMarker(uri)
            s.message = voice.status
            if (imported) {
                s.demoVoiceTriggered = false
                s.roadVoiceShown = false
                s.showVoiceRecorder = false
            }
        }
    }
    val live = LocalTripLive.current
    fun startPollSpeech() {
        s.speechFeedbackVisible = true
        voice.startPollSpeech { transcript ->
            s.speechFeedbackVisible = false
            s.message = "인식: $transcript"
            s.pollBusy = true
        }
    }
    fun playDemoPollSpeech() {
        voice.stopListening()
        s.speechFeedbackVisible = false
        s.pollDemoVisible = true
        s.pollDemoSpeaking = false
        s.message = ""
        s.pollDemoSpeaking = voice.playDemoPollSpeech {
            s.pollDemoSpeaking = false
        }
        if (!s.pollDemoSpeaking) s.message = voice.status
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startPollSpeech() else {
            s.message = "마이크 권한이 거부되어 음성 녹음은 사용할 수 없어요."
            s.speechFeedbackVisible = false
        }
    }
    DisposableEffect(voice) { onDispose { voice.release() } }
    val naviPoint = live.location.value?.takeIf { live.location.status == ReadStatus.READY && it.isValid() }
    LaunchedEffect(naviPoint) { voice.updateFallbackPoint(naviPoint) }
    LaunchedEffect(foreground) { if (foreground) voice.startLocation() else voice.release() }
    val convoy by rememberUpdatedState(LocalConvoy.current)
    val screen = s.screen
    BackHandler {
        if (s.showVoiceRecorder) s.showVoiceRecorder = false
        else if (s.showVoiceMenu) s.showVoiceMenu = false
        else if (s.pollDemoVisible) {
            voice.audio.stopPlayback()
            s.pollDemoSpeaking = false
            s.pollDemoVisible = false
        }
        else if (screen == WireScreen.HOME) onExit() else s.back()
    }

    LaunchedEffect(setupRestricted) {
        if (setupRestricted) {
            s.showSettings = false
            s.showEnd = false
            s.showHistory = false
            s.showPollTextEntry = false
        }
    }

    LaunchedEffect(s.music, s.playing, screen.isDriving, foreground) {
        while (s.music && s.playing && screen.isDriving && foreground) {
            delay(1_000)
            s.advanceMusic()
        }
    }

    LaunchedEffect(screen) {
        when (screen) {
            WireScreen.KNOCK_SENT -> {
                s.seconds = 10
                repeat(10) { delay(1_000); s.seconds-- }
            }
            WireScreen.KNOCK_RECEIVED, WireScreen.KNOCK_FULL -> {
                s.seconds = 10
                repeat(10) { delay(1_000); s.seconds-- }
                s.drive()
            }
            WireScreen.CALL -> {
                s.seconds = 30
                repeat(30) { delay(1_000); s.seconds-- }
                s.drive()
            }
            WireScreen.INCOMING_CALL -> {
                s.seconds = if (s.incomingCaller == "문준 차") 6 else 30
                repeat(s.seconds) { delay(1_000); s.seconds-- }
                if (s.incomingCaller == "문준 차") s.declineIncomingCall()
                else s.drive()
            }
            WireScreen.CALL_DECLINED -> {
                delay(3_000)
                s.drive()
            }
            else -> Unit
        }
    }
    LaunchedEffect(screen.isDriving, convoy.distanceMeters, foreground, voice.notes.hasDemoMarker) {
        if (!screen.isDriving) {
            s.tripMotionStarted = false
            return@LaunchedEffect
        }
        if (!s.tripMotionStarted) {
            s.tripMotionStarted = true
            voice.notes.rearmDemoMarkerForNewTrip()
            convoy.reset()
            s.progress = 0f
            return@LaunchedEffect
        }
        s.progress = demoVisualProgress(convoy.distanceMeters)
        if (foreground && !s.demoVoiceTriggered && voice.notes.hasDemoMarker &&
            convoy.distanceMeters >= 200.0 && !voice.audio.isPlaying) {
            if (voice.playDemoMarker { s.roadVoiceShown = true }) {
                s.demoVoiceTriggered = true
            }
        }
    }

    Surface(modifier.fillMaxSize(), color = Wire.background) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val contentWide = wide && maxWidth >= 900.dp
            val designWide = maxWidth >= 1500.dp
            Column(Modifier.fillMaxSize().then(if (designWide)
                Modifier.background(DesignedJourneyBackdrop) else Modifier)
                .padding(horizontal = if (designWide && screen in setOf(WireScreen.HOME,
                        WireScreen.CREATE, WireScreen.INVITE, WireScreen.WAITING,
                        WireScreen.MISSION, WireScreen.MUSIC_PICK, WireScreen.FIRST_SONG,
                        WireScreen.SUMMARY)) 0.dp else 12.dp,
                    vertical = if (designWide && screen in setOf(WireScreen.HOME,
                        WireScreen.CREATE, WireScreen.INVITE, WireScreen.WAITING,
                        WireScreen.MISSION, WireScreen.MUSIC_PICK, WireScreen.FIRST_SONG,
                        WireScreen.SUMMARY)) 0.dp else 12.dp)) {
                if (setupRestricted && !screen.isDriving) {
                    DrivingHoldingScreen()
                } else when (screen) {
                    WireScreen.HOME, WireScreen.CREATE, WireScreen.INVITE,
                    WireScreen.WAITING, WireScreen.MISSION, WireScreen.MUSIC_PICK,
                    WireScreen.FIRST_SONG, WireScreen.SUMMARY ->
                        if (designWide) DesignedJourney(s, screen, enabled) else when (screen) {
                            WireScreen.HOME -> WireHome(s, contentWide, enabled)
                            WireScreen.CREATE -> WireCreate(s, contentWide, enabled)
                            WireScreen.INVITE -> WireInvite(s, enabled)
                            WireScreen.WAITING -> WireWaiting(s, contentWide, enabled)
                            WireScreen.MISSION -> WireMission(s, contentWide, enabled)
                            WireScreen.MUSIC_PICK -> WireMusicPick(s, contentWide, enabled)
                            WireScreen.FIRST_SONG -> WireFirstSong(s, contentWide, enabled)
                            else -> WireEnding(s, screen, contentWide, enabled)
                        }
                    WireScreen.ARRIVED, WireScreen.SAVE -> WireEnding(s, screen, contentWide, enabled)
                    else -> WireDriving(s, screen, contentWide, enabled, voice,
                        designWide = designWide, setupRestricted = setupRestricted,
                        onResetDemo = {
                            voice.audio.stopPlayback()
                            s.showVoiceRecorder = false
                            s.showVoiceMenu = false
                            voice.notes.rearmDemoMarkerForNewTrip()
                            convoy.reset()
                            s.progress = 0f
                            s.demoVoiceTriggered = false
                            s.roadVoiceShown = false
                            s.pollDemoVisible = false
                            s.pollDemoSpeaking = false
                            s.message = "출발 지점으로 돌아갔어요."
                            s.drive()
                        }, onPollSpeech = {
                        if (designWide) playDemoPollSpeech()
                        else if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                            PackageManager.PERMISSION_GRANTED) startPollSpeech()
                        else if (setupRestricted) {
                            s.message = "마이크 권한이 필요해요. 정차한 뒤 허용해 주세요."
                            s.speechFeedbackVisible = false
                        } else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                    }, onVoiceClick = {
                        if (s.screen != WireScreen.DRIVE) s.drive()
                        s.openVoiceUi()
                    })
                }
            }
        }
    }
    if (s.showVoiceMenu && !wide) AlertDialog(onDismissRequest = { s.showVoiceMenu = false },
        title = { Text("모두에게 말하기") },
        text = { Text("말로 투표 안건을 만들거나 길에 원본 음성을 남길 수 있어요.") },
        dismissButton = { TextButton(onClick = { s.showVoiceMenu = false }) { Text("취소") } },
        confirmButton = {
            Column {
                TextButton(onClick = {
                    s.showVoiceMenu = false
                    if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED) startPollSpeech()
                    else if (setupRestricted) s.message = "마이크 권한이 필요해요. 정차한 뒤 허용해 주세요."
                    else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                }) { Text("투표 안건 말하기") }
                TextButton(onClick = { s.showVoiceMenu = false; s.showVoiceRecorder = true }) {
                    Text("길에 음성 남기기")
                }
            }
        })
    if (s.showPollTextEntry && !setupRestricted) AlertDialog(onDismissRequest = { s.showPollTextEntry = false },
        title = { Text("텍스트로 AI 투표 시험") },
        text = {
            TextField(value = s.pollDraft, onValueChange = { s.pollDraft = it.take(500) },
                label = { Text("투표할 내용") }, singleLine = true)
        },
        dismissButton = { TextButton(onClick = { s.showPollTextEntry = false }) { Text("취소") } },
        confirmButton = { TextButton(onClick = {
            if (s.pollDraft.isNotBlank()) {
                s.message = "인식: ${s.pollDraft.trim()}"
                s.pollBusy = true
                s.showPollTextEntry = false
            }
        }, enabled = s.pollDraft.isNotBlank()) { Text("AI로 만들기") } })
    LaunchedEffect(s.message, s.pollBusy) {
        if (s.pollBusy && s.message.startsWith("인식: ")) {
            val transcript = s.message.removePrefix("인식: ")
            voice.stopListening()
            val result = withContext(Dispatchers.IO) { PollBridge.generate(transcript) }
            s.pollBusy = false
            result.onSuccess { s.generatedPoll = it; s.vote = ""; s.message = "AI가 투표 안건을 만들었어요." }
                .onFailure {
                    s.message = if (it is ConnectException)
                        "투표 생성 실패: PC에서 OpenAI 브리지(tools/openai_poll_bridge.py)를 실행해 주세요."
                    else "투표 생성 실패: ${it.message ?: "OpenAI 브리지를 확인해 주세요."}"
                }
        }
    }
    if (s.showSettings && wide && !setupRestricted) DesignedSettingsDialog(s)
    else if (s.showSettings && !setupRestricted) AlertDialog(onDismissRequest = { s.showSettings = false },
        title = { Text("설정") }, text = {
            Column {
                Text("현재는 한 기기 시연입니다. 원본 음성 녹음·재생은 이 기기에서 작동하지만 차량 간 음성 연결·음악은 아직 전송되지 않습니다.")
                TextButton(onClick = { s.simulateIncomingCall("다연 차") }) { Text("다연 차 전화 수신 시연") }
                TextButton(onClick = { s.simulateIncomingCall("문준 차") }) { Text("문준 차 전화 수신 시연") }
            }
        }, confirmButton = { TextButton(onClick = { s.showSettings = false }) { Text("닫기") } })
    if (s.showEnd && wide && !setupRestricted) DesignedEndDialog(s)
    else if (s.showEnd && !setupRestricted) AlertDialog(onDismissRequest = { s.showEnd = false },
        title = { Text("동행을 종료할까요?") },
        text = { Text("시연 여행을 마치고 도착 화면으로 이동합니다.") },
        dismissButton = { TextButton(onClick = { s.showEnd = false }) { Text("계속 가기") } },
        confirmButton = { TextButton(onClick = {
            s.showEnd = false
            s.open(WireScreen.ARRIVED)
        }) { Text("동행 종료") } })
    if (s.showHistory && wide && !setupRestricted) DesignedHistoryDialog(s, voice)
    else if (s.showHistory && !setupRestricted) AlertDialog(onDismissRequest = { s.showHistory = false },
        title = { Text("이전 대화") },
        text = { Text("다연 차 · 휴게소 들렀다 가자\n문준 차 · 이 노래 누가 신청했어? 좋다") },
        dismissButton = {
            voice.notes.notes.lastOrNull { it.id == s.lastNoteId }?.let { note ->
                TextButton(onClick = { voice.play(note) }) { Text("남긴 음성 듣기") }
            }
        },
        confirmButton = { TextButton(onClick = { s.showHistory = false }) { Text("닫기") } })
}

@Composable
private fun DrivingHoldingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("주행 중이에요", style = MaterialTheme.typography.headlineMedium, color = Wire.ink)
            Spacer(Modifier.height(12.dp))
            Text("여행 만들기와 설정은 안전한 곳에 정차한 뒤 이용할 수 있어요.",
                style = MaterialTheme.typography.bodyLarge, color = Wire.secondary)
        }
    }
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
    val ring = when (name) { "다연", "B차" -> Wire.teal; "문준", "C차" -> Wire.orange; else -> Wire.ink }
    val portrait = when (name) {
        "나", "내 차", "승호" -> R.drawable.flow_imgprofilephoto
        "다연", "다연 차", "B차" -> R.drawable.flow_imgprofilephoto2
        "문준", "문준 차", "C차" -> R.drawable.flow_imgprofilephoto1
        "성윤" -> R.drawable.invite_seongyun
        "민석" -> R.drawable.invite_minseok
        "현진" -> R.drawable.invite_hyeonjin
        "준호" -> R.drawable.invite_junho
        else -> null
    }
    if (portrait != null) {
        Image(painterResource(portrait), name,
            modifier.size(size).border(4.dp, ring, CircleShape).padding(4.dp).clip(CircleShape),
            contentScale = ContentScale.Crop)
        return
    }
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
            "친구 여행 · 10월 12일", listOf("다연", "문준"), enabled) {
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
