package com.contest.travel.preview

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

// The current Figma journey frames are 1700 × 1184. The Pleos activity is
// slightly taller: center this frame and scale both axes by the same amount.
private val FInk = Color(0xFF131417)
private val FMuted = Color(0xFF8F919F)
private val FPale = Color(0xFFF3F4F8)
private val FBlue = Color(0xFF5263E7)
private val FRed = Color(0xFFFF7777)
private val FCard = Color.White.copy(alpha = .82f)
@OptIn(ExperimentalTextApi::class)
internal val FFont = FontFamily(
    Font(R.font.pretendard_variable, FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.pretendard_variable, FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.pretendard_variable, FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.pretendard_variable, FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)))
)

@Composable
internal fun ColumnScope.FigmaJourneyExact(s: WireSession, screen: WireScreen, enabled: Boolean) {
    BoxWithConstraints(Modifier.weight(1f)) {
        val density = LocalDensity.current
        val fit = min(maxWidth.value / 1700f, maxHeight.value / 1184f)
        CompositionLocalProvider(LocalDensity provides Density(density.density * fit, density.fontScale)) {
            Box(Modifier.size(1700.dp, 1184.dp).align(Alignment.Center)) {
                when (screen) {
                    WireScreen.CREATE -> FCreate(s, enabled)
                    WireScreen.INVITE -> FInvite(s, enabled)
                    WireScreen.WAITING -> FWaiting(s, enabled)
                    WireScreen.MISSION -> FMission(s, enabled)
                    WireScreen.MUSIC_PICK, WireScreen.FIRST_SONG -> FMusic(s, enabled)
                    WireScreen.SUMMARY -> FSummary(s, enabled)
                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun FRect(x: Int, y: Int, w: Int, h: Int, color: Color,
    radius: Int = 0, border: Color? = null, onClick: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(radius.dp)
    Box(Modifier.offset(x.dp, y.dp).size(w.dp, h.dp).clip(shape)
        .background(color).then(if (border != null) Modifier.border(2.dp, border, shape) else Modifier)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier))
}

@Composable
private fun FDashedBorder(x: Int, y: Int, w: Int, h: Int, radius: Int, color: Color) {
    Canvas(Modifier.offset(x.dp, y.dp).size(w.dp, h.dp)) {
        val stroke = 2.dp.toPx()
        drawRoundRect(color, topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(size.width - stroke, size.height - stroke),
            cornerRadius = CornerRadius(radius.dp.toPx()),
            style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(8.dp.toPx(), 8.dp.toPx()))))
    }
}

@Composable
private fun FLabel(value: String, x: Int, y: Int, size: Int, line: Int = size + 7,
    color: Color = FInk, weight: FontWeight = FontWeight.Normal, width: Int? = null,
    maxLines: Int = 1) {
    Text(value, Modifier.offset(x.dp, y.dp).then(if (width != null) Modifier.size(width.dp, (line * maxLines + 6).dp) else Modifier),
        style = TextStyle(fontFamily = FFont, fontSize = size.sp, lineHeight = line.sp,
            fontWeight = weight, color = color), maxLines = maxLines,
        overflow = TextOverflow.Ellipsis)
}

@Composable
private fun FImage(id: Int, x: Int, y: Int, w: Int, h: Int, radius: Int = 0,
    scale: ContentScale = ContentScale.Crop, onClick: (() -> Unit)? = null) {
    Image(painterResource(id), null, Modifier.offset(x.dp, y.dp).size(w.dp, h.dp)
        .clip(RoundedCornerShape(radius.dp))
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentScale = scale)
}

@Composable
private fun FAvatar(id: Int, x: Int, y: Int, size: Int, name: String,
    onClick: (() -> Unit)? = null) {
    Image(painterResource(id), name, Modifier.offset(x.dp, y.dp).size(size.dp)
        .clip(CircleShape).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentScale = ContentScale.Crop)
}

@Composable
private fun FSelectedCheck(x: Int, y: Int) {
    Canvas(Modifier.offset(x.dp, y.dp).size(46.dp)) {
        val unit = size.width / 46f
        drawCircle(FBlue, radius = 23f * unit)
        drawLine(Color.White, Offset(13f * unit, 23f * unit),
            Offset(20f * unit, 30f * unit), strokeWidth = 2.6f * unit,
            cap = StrokeCap.Round)
        drawLine(Color.White, Offset(20f * unit, 30f * unit),
            Offset(33f * unit, 15f * unit), strokeWidth = 2.6f * unit,
            cap = StrokeCap.Round)
    }
}

@Composable
private fun FButton(label: String, x: Int, y: Int, w: Int, h: Int, enabled: Boolean,
    primary: Boolean = true, arrow: Boolean = false, arrowEndInset: Int = 32,
    onClick: () -> Unit) {
    val color = if (primary) Color(0xFF191A1B) else Color(0xFFEDEEF2)
    val text = if (primary) Color.White else FInk
    Box(Modifier.offset(x.dp, y.dp).size(w.dp, h.dp)
        .clip(RoundedCornerShape(35.dp)).background(color).clickable(enabled, onClick = onClick)) {
        if (arrow) {
            Text(label, Modifier.align(Alignment.CenterStart)
                .padding(start = 48.dp, end = (arrowEndInset + 54).dp),
                style = TextStyle(fontFamily = FFont, fontSize = 32.sp,
                    lineHeight = 40.sp, letterSpacing = (-.32).sp,
                    fontWeight = FontWeight.SemiBold, color = text), maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Image(painterResource(R.drawable.figma_icon_arrow), null,
                Modifier.align(Alignment.CenterEnd).padding(end = arrowEndInset.dp)
                    .size(38.dp))
        } else Text(label, Modifier.align(Alignment.Center), style = TextStyle(fontFamily = FFont,
            fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold, color = text))
    }
}

@Composable
private fun FHeader(title: String, subtitle: String, back: () -> Unit, step: Int? = null) {
    FImage(R.drawable.figma_icon_back, 32, 45, 66, 66, onClick = back)
    FLabel(title, 128, 38, 32, 44, weight = FontWeight.Bold)
    FLabel(subtitle, 128, 82, 26, 36, FMuted)
    if (step != null) {
        FRect(1582, 66, 37, 10, if (step == 1) FInk else Color(0xFFADB5C5), 5)
        FRect(1628, 66, 37, 10, if (step == 2) FInk else Color(0xFFADB5C5), 5)
    }
}

@Composable
private fun FInput(value: String, onValueChange: (String) -> Unit,
    placeholder: String, x: Int, y: Int, w: Int, h: Int, search: Boolean = false) {
    FRect(x, y, w, h, Color(0xFFE9EBF2), 35)
    if (search) FImage(R.drawable.figma_icon_search, x + 24, y + (h - 27) / 2, 27, 27)
    BasicTextField(value, onValueChange,
        Modifier.offset((x + if (search) 70 else 38).dp, y.dp)
            .size((w - if (search) 165 else 120).dp, h.dp), singleLine = true,
        textStyle = TextStyle(fontFamily = FFont, fontSize = 27.sp, lineHeight = 36.sp, color = FInk),
        decorationBox = { inner ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(placeholder, style = TextStyle(fontFamily = FFont,
                    fontSize = 27.sp, color = FMuted))
                inner()
            }
        })
    FImage(R.drawable.figma_icon_mic, x + w - 70, y + (h - 54) / 2, 54, 54)
}

@Composable
private fun FCreate(s: WireSession, enabled: Boolean) {
    FHeader("여행 만들기", "이동 목적과 목적지를 정해 주세요", s::back, 1)
    FLabel("이동 목적", 32, 149, 36, 48, weight = FontWeight.Bold)
    val cards = listOf(
        Triple("친구 여행", "함께 듣는 DJ · 순위 켜짐", R.drawable.figma_328_9037_imgimage57),
        Triple("가족", "안부 · 도착 알림 중심", R.drawable.figma_328_9037_imgimage58),
        Triple("업무 · 단체", "알림 최소화", R.drawable.figma_328_9037_imgimage59)
    )
    cards.forEachIndexed { i, (title, detail, photo) ->
        val x = 32 + i * 554
        val selectPurpose = { if (enabled) {
            s.purpose = title
            s.createPurposeSelection = title
        } }
        FImage(photo, x, 206, 530, 380, 36, onClick = selectPurpose)
        Box(Modifier.offset(x.dp, 206.dp).size(530.dp, 380.dp).clip(RoundedCornerShape(36.dp))
            .background(Brush.verticalGradient(0f to Color.Transparent, .54f to Color.Transparent,
                .78f to when (i) { 0 -> Color(0xFFD4BA63); 1 -> Color(0xFFFFB258); else -> Color(0xFF96A2FF) }.copy(alpha = .9f),
                1f to when (i) { 0 -> Color(0xFFD4BA63); 1 -> Color(0xFFFFB258); else -> Color(0xFF96A2FF) }.copy(alpha = .9f))))
        FLabel(title, x + 33, 472, 32, 44, Color.White, FontWeight.Bold)
        FLabel(detail, x + 33, 524, 20, 30, Color.White, FontWeight.SemiBold)
        if (s.createPurposeSelection != null && s.createPurposeSelection != title) {
            FRect(x, 206, 530, 380, Color.White.copy(alpha = .8f), 36,
                onClick = selectPurpose)
        }
        if (s.createPurposeSelection == title) {
            FSelectedCheck(x + 449, 506)
        }
    }
    FRect(32, 613, 1636, 539, FCard, 56)
    FLabel("목적지", 68, 654, 36, 48, weight = FontWeight.Bold)
    FInput(s.destinationQuery, {
        s.destinationQuery = it
        s.destination = it
        s.createRecentSelection = null
    }, "어디로 가나요?", 68, 723, 709, 70, search = true)
    FRect(816, 668, 2, 408, Color(0xFFE0E1E6))
    FLabel("최근 기록", 854, 660, 26, 34, FMuted)
    listOf(Triple("가평 용추계곡", "경기 가평군 · 82km", 718),
        Triple("양주 글램핑장", "경기 양주시 · 45km", 826),
        Triple("할머니 댁", "충남 공주시 · 140km", 925)).forEach { (name, detail, y) ->
        FRect(851, y, 785, 88,
            if (s.createRecentSelection == name) Color(0xFFE5E8FF) else Color.Transparent,
            24, onClick = { if (enabled) {
                s.destination = name
                s.destinationQuery = ""
                s.createRecentSelection = name
            } })
        FLabel(name, 887, y + 6, 30, 38, weight = FontWeight.SemiBold)
        FLabel(detail, 887, y + 46, 24, 32, FMuted)
        if (s.createRecentSelection == name) {
            FSelectedCheck(1569, y + 21)
        }
    }
    FButton("초대하기", 68, 1028, 337, 88,
        enabled && s.createPurposeSelection != null && s.destination.isNotBlank() &&
            (s.createRecentSelection != null || s.destinationQuery.isNotBlank()), arrow = true) {
        s.createPurposeSelection?.let { s.purpose = it }
        s.open(WireScreen.INVITE)
    }
    if (s.createPurposeSelection != null && s.destination.isNotBlank() &&
        (s.createRecentSelection != null || s.destinationQuery.isNotBlank())) {
        FLabel("${s.createPurposeSelection} · ${s.destination}", 1360, 1052, 28, 40,
            FBlue, FontWeight.SemiBold, 306)
    }
}

@Composable
private fun FInvite(s: WireSession, enabled: Boolean) {
    FHeader("${s.destination} 가는 길", "${s.purpose} · 그룹이 만들어졌어요", s::back, 2)
    FRect(110, 144, 1480, 976, FCard, 56)
    FLabel("함께 갈 차를 초대하세요", 200, 211, 36, 48, weight = FontWeight.Bold)
    FLabel("앱을 쓰는 사람만 초대할 수 있어요 · 내 차 포함 최대 4대", 200, 264, 26, 36, FMuted)
    FInput(s.phone, { s.phone = it }, "전화번호 입력", 200, 332, 1300, 94)
    FLabel("최근 함께한 사람", 200, 513, 26, 36, FMuted)
    val people = listOf(
        Triple("다연", R.drawable.invite_dayeon, 0), Triple("문준", R.drawable.invite_munjun, 1),
        Triple("성윤", R.drawable.invite_seongyun, 2), Triple("민석", R.drawable.invite_minseok, 3),
        Triple("현진", R.drawable.invite_hyeonjin, 4), Triple("준호", R.drawable.invite_junho, 5)
    )
    people.forEach { (name, image, index) ->
        val x = 210 + index * 225
        FAvatar(image, x, 576, 140, name) {
            if (enabled) s.invitees = if (name in s.invitees) s.invitees - name
                else if (s.invitees.size < 3) s.invitees + name else s.invitees
        }
        if (name in s.invitees) {
            FRect(x + 104, 672, 42, 42, FBlue, 21)
            FLabel("✓", x + 111, 674, 30, 36, Color.White, FontWeight.Bold)
        }
        FLabel(name, x + 32, 734, 30, 38, weight = FontWeight.Bold)
        FLabel(if (name in s.invitees) "선택됨" else "앱 사용 중",
            x + if (name in s.invitees) 18 else 4, 784, 25, 34, FMuted)
    }
    FButton("초대 보내기 (${s.invitees.size})", 195, 986, 377, 88,
        enabled && (s.invitees.isNotEmpty() || s.phone.isNotBlank()), arrow = true,
        arrowEndInset = 36) {
        if (s.phone.isNotBlank() && s.invitees.size < 3) s.invitees += s.phone
        s.open(WireScreen.WAITING)
    }
    val selectedCars = s.invitees.take(3)
    val extraCarOffset = ((selectedCars.size - 2).coerceAtLeast(0)) * 74
    FLabel("선택한 차", 1036 - extraCarOffset, 1014, 26, 36, FMuted)
    selectedCars.forEachIndexed { i, name ->
        val image = when (name) { "다연" -> R.drawable.invite_dayeon
            "문준" -> R.drawable.invite_munjun; "성윤" -> R.drawable.invite_seongyun
            "민석" -> R.drawable.invite_minseok; "현진" -> R.drawable.invite_hyeonjin
            else -> R.drawable.invite_junho }
        FAvatar(image, 1166 - extraCarOffset + i * 74, 1002, 54, name)
    }
    FLabel("${selectedCars.joinToString(", ")} · ${selectedCars.size}대", 1314, 1012,
        28, 40, FInk, FontWeight.SemiBold, 276)
}

@Composable
private fun FWaiting(s: WireSession, enabled: Boolean) {
    FHeader("${s.destination} 가는 길", "${s.purpose} · 도착 예정 15:40", s::back)
    FRect(32, 144, 920, 1008, FCard, 56)
    FImage(R.drawable.figma_328_9223_img, 32, 144, 920, 665, 56)
    Box(Modifier.offset(32.dp, 600.dp).size(920.dp, 210.dp)
        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.White))))
    FLabel("${s.invitees.take(2).joinToString(", ")}이 함께해요!", 80, 868,
        36, 48, weight = FontWeight.Bold)
    FLabel("1자리 남았어요 · 지금 바로 출발해도 돼요", 80, 929, 26, 36, FMuted)
    FButton("출발 준비하기", 80, 1015, 340, 88, enabled, arrow = true) {
        s.open(WireScreen.MISSION)
    }
    FButton("바로 출발", 438, 1015, 240, 88, enabled, primary = false) {
        s.music = false; s.open(WireScreen.DRIVE)
    }
    FRect(976, 144, 692, 1008, FCard, 56)
    FLabel("함께 가는 사람", 1008, 185, 36, 48, weight = FontWeight.Bold)
    FLabel("${s.invitees.size + 1} / 4", 1566, 189, 28, 36, FMuted)
    val cards = listOf(
        Triple("나", "아이오닉 5", R.drawable.flow_imgprofilephoto),
        Triple("다연", "쏘렌토", R.drawable.invite_dayeon),
        Triple("문준", "아반떼", R.drawable.invite_munjun)
    )
    cards.forEachIndexed { i, (name, car, image) ->
        val x = 996 + (i % 2) * 334
        val y = 260 + (i / 2) * 444
        FRect(x, y, 318, 424, FPale, 40)
        FAvatar(image, x + 104, y + 61, 110, name)
        FLabel(name, x + 130 - name.length * 9, y + 203, 32, 40, weight = FontWeight.Bold)
        FLabel(car, x + 76, y + 272, 25, 34, FMuted)
        FRect(x + 80, y + 338, 160, 42, Color.White, 21)
        FLabel(if (i == 2) "탑승 전" else "동승자 있음", x + 94, y + 341,
            22, 34, if (i == 2) FRed else FBlue, FontWeight.SemiBold)
    }
    FRect(1330, 704, 318, 424, Color.White.copy(alpha = .30f), 32,
        onClick = { if (enabled) s.open(WireScreen.INVITE) })
    FDashedBorder(1330, 704, 318, 424, 32, Color(0xFFC4CCE4))
    FRect(1442, 818, 84, 84, Color(0xFFE7EAF2), 42)
    FLabel("+", 1468, 822, 52, 60, FMuted)
    FLabel("초대하기", 1400, 933, 32, 40, weight = FontWeight.Bold)
    FLabel("1자리 남음", 1410, 993, 26, 34, FMuted)
}

@Composable
private fun FMission(s: WireSession, enabled: Boolean) {
    FHeader("출발 준비", "${s.destination} 가는 길 · ${s.invitees.size + 1}대", s::back, 1)
    FRect(32, 144, 1636, 375, Color(0xFFF1F4FF), 50)
    Image(painterResource(R.drawable.figma_328_9335_img), null,
        Modifier.offset(460.dp, 144.dp).size(1208.dp, 375.dp)
            .clip(RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp)),
        contentScale = ContentScale.FillBounds)
    FLabel("오늘의 미션", 80, 201, 26, 34, FMuted)
    FLabel("안전하게 온 차가 1등!", 80, 299, 48, 60, FInk, FontWeight.Bold)
    Text(buildAnnotatedString {
        append("모든 차가 ")
        withStyle(SpanStyle(color = FBlue, fontWeight = FontWeight.Bold)) { append("100점") }
        append("에서 시작해요.")
    }, Modifier.offset(80.dp, 383.dp), style = TextStyle(fontFamily = FFont,
        fontSize = 30.sp, lineHeight = 40.sp, color = FMuted))
    FRect(32, 542, 690, 495, FCard, 56)
    FLabel("감점 기준", 64, 576, 36, 46, weight = FontWeight.Bold)
    listOf(Triple("급가속", "−3점", 677), Triple("급제동", "−3점", 800),
        Triple("과속", "−5점", 923)).forEachIndexed { i, (name, points, y) ->
        FImage(listOf(R.drawable.figma_mission_accel, R.drawable.figma_mission_brake,
            R.drawable.figma_mission_speed)[i], 64, y - 5, 60, 60)
        FLabel(name, 145, y, 34, 44, weight = FontWeight.SemiBold)
        FLabel(points, 587, y, 42, 50, FRed, FontWeight.Bold)
        if (y < 900) FRect(64, y + 90, 626, 2, Color(0xFFE2E4EB))
    }
    FRect(746, 542, 922, 495, FCard, 56)
    FLabel("참가하는 차", 779, 576, 36, 46, weight = FontWeight.Bold)
    FLabel("시작 점수", 1432, 581, 25, 34, FMuted)
    FLabel("100점", 1540, 572, 34, 45, FBlue, FontWeight.Bold)
    FRect(1051, 670, 2, 184, Color(0xFFE0E1E6))
    FRect(1340, 670, 2, 184, Color(0xFFE0E1E6))
    listOf(Triple("나", "아이오닉 5", R.drawable.flow_imgprofilephoto),
        Triple("다연", "쏘렌토", R.drawable.invite_dayeon),
        Triple("문준", "아반떼", R.drawable.invite_munjun)).forEachIndexed { i, (name, car, image) ->
        val x = 795 + i * 289
        FAvatar(image, x + 72, 669, 113, name)
        FLabel(name, x + 107, 798, 32, 40, weight = FontWeight.Bold)
        FLabel(car, x + 68, 841, 25, 34, FMuted)
    }
    FRect(779, 928, 858, 81, Color(0xFFEDEEF2), 35, onClick = { if (enabled) s.wager = !s.wager })
    FLabel("내기 걸기", 805, 949, 27, 36, weight = FontWeight.SemiBold)
    FRect(952, 945, 74, 44, if (s.wager) Color(0xFFCDD6FF) else Color(0xFFE2E4EB), 22)
    FLabel(if (s.wager) "선택됨" else "선택", 962, 953, 19, 27, FMuted)
    FLabel(if (s.wager) "꼴찌가 커피 사기" else "‘꼴찌가 커피 사기’처럼 말해 보세요",
        1058, 951, 24, 32, FMuted)
    FImage(R.drawable.figma_icon_mic, 1576, 938, 58, 58)
    FLabel("점수는 주행 데이터로 계산해요. 주행 중에는 순위만 보여요.",
        32, 1082, 22, 30, FMuted)
    FLabel("마지막 차가 도착하면 순위가 확정돼요.",
        32, 1116, 22, 30, FMuted)
    FButton("미션 없이 다음", 885, 1060, 340, 88, enabled, primary = false) {
        s.mission = false; s.open(WireScreen.MUSIC_PICK)
    }
    FButton("이 미션으로 다음", 1240, 1060, 428, 88, enabled, arrow = true) {
        s.mission = true; s.open(WireScreen.MUSIC_PICK)
    }
}

@Composable
private fun FMusicOrbits() {
    // These are Figma's three transparent ellipse layers. The former PNG exports
    // included an opaque off-white canvas and left visible rectangular seams.
    Canvas(Modifier.offset(191.dp, 270.dp).size(491.dp, 485.dp)) {
        val sx = size.width / 491f
        val sy = size.height / 485f
        drawOval(Color(0xFFEBEAFB).copy(alpha = .72f),
            topLeft = Offset(2f * sx, 2f * sy),
            size = Size(487f * sx, 481f * sy), style = Stroke(2f * sx))
        drawOval(brush = Brush.radialGradient(
            0f to Color(0xFFD3D1FF).copy(alpha = .9f),
            .5f to Color(0xFFEEEDFF).copy(alpha = .3f),
            1f to Color(0xFFDED9FF).copy(alpha = .6f),
            center = Offset(245.5f * sx, 242.5f * sy), radius = 203f * sx),
            topLeft = Offset(53f * sx, 60f * sy),
            size = Size(385f * sx, 365f * sy), style = Stroke(20f * sx))
        drawOval(Color(0xFFEEEDFB).copy(alpha = .8f),
            topLeft = Offset(91.83f * sx, 88.83f * sy),
            size = Size(307.34f * sx, 307.34f * sy), style = Stroke(1.66f * sx))
    }
}

@Composable
private fun FMusic(s: WireSession, enabled: Boolean) {
    FHeader("출발 준비", "${s.destination} 가는 길 · ${s.invitees.size + 1}대", s::back, 2)
    FRect(32, 144, 795, 894, FCard, 56)
    FRect(851, 144, 817, 894, FCard, 56)
    Canvas(Modifier.offset(851.dp, 938.dp).size(817.dp, 100.dp)
        .clip(RoundedCornerShape(bottomStart = 56.dp, bottomEnd = 56.dp))) {
        drawRect(Brush.verticalGradient(
            listOf(Color.Transparent, Color(0xFFB5EFF0).copy(alpha = .16f))))
        drawRect(Brush.radialGradient(
            listOf(Color(0xFFF0D4F8).copy(alpha = .17f), Color.Transparent),
            center = Offset(size.width, size.height), radius = size.width * .6f))
    }
    FLabel("첫 곡 DJ", 80, 180, 26, 36, FMuted, FontWeight.SemiBold)
    FLabel("첫 곡", 899, 180, 26, 36, FMuted, FontWeight.SemiBold)
    FMusicOrbits()
    FImage(R.drawable.figma_music_crown_vector, 419, 376, 38, 38)
    FAvatar(R.drawable.invite_munjun, 340, 420, 199, "문준")
    FAvatar(R.drawable.flow_imgprofilephoto, 201, 516, 120, "나")
    FAvatar(R.drawable.invite_dayeon, 550, 516, 120, "다연")
    FRect(410, 620, 80, 40, Color(0xFFE5E5FF), 20)
    FLabel("DJ", 431, 623, 24, 32, FBlue, FontWeight.Bold)
    FLabel("문준님이 첫 곡을 골랐어요", 199, 791, 44, 58, weight = FontWeight.Bold)
    FLabel("무작위로 뽑힌 차가 첫 곡을 신청해요", 247, 859, 26, 40, FMuted)
    FImage(R.drawable.figma_328_9457_imgimage50, 1212, 223, 364, 364, 182)
    FImage(R.drawable.figma_328_9457_img9, 1035, 226, 359, 359, 26)
    FLabel("밤편지", 899, 636, 44, 58, weight = FontWeight.Bold)
    FLabel("아이유", 899, 715, 28, 40, FMuted)
    FRect(1012, 718, 2, 33, Color(0xFFACB1C0))
    FRect(1052, 718, 167, 37, Color(0xFFE8E9EF), 4)
    FImage(R.drawable.drive_music_car, 1056, 724, 44, 25)
    FLabel("문준 차 신청", 1106, 721, 20, 28, FMuted)
    FRect(899, 791, 721, 12, Color(0xFFEDEEF2), 6)
    FLabel("함께 듣는 차", 899, 890, 26, 36, FMuted)
    FAvatar(R.drawable.flow_imgprofilephoto, 1061, 871, 80, "나")
    FAvatar(R.drawable.invite_dayeon, 1121, 871, 80, "다연")
    FAvatar(R.drawable.invite_munjun, 1184, 871, 80, "문준")
    FLabel("3대", 1296, 887, 28, 40, FInk, FontWeight.SemiBold)
    FButton("음악 없이 출발", 32, 1064, 339, 88, enabled, primary = false) {
        s.music = false; s.open(WireScreen.DRIVE)
    }
    FButton("출발하기", 387, 1064, 428, 88, enabled, arrow = true) {
        s.music = true; s.open(WireScreen.DRIVE)
    }
    FLabel("출발하면 첫 곡을 함께 들어요", 1398, 1090, 23, 32, FMuted)
}

@Composable
private fun FScoreRing(x: Int, y: Int, score: Int, color: Color) {
    Canvas(Modifier.offset(x.dp, y.dp).size(150.dp)) {
        val stroke = 14.dp.toPx()
        val inset = stroke / 2
        val origin = Offset(inset, inset)
        val ringSize = Size(size.width - stroke, size.height - stroke)
        drawArc(Color(0xFFEBEDF6), 0f, 360f, false,
            topLeft = origin, size = ringSize, style = Stroke(stroke))
        drawArc(color, -90f, score.coerceIn(0, 100) * 3.6f, false,
            topLeft = origin, size = ringSize,
            style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

@Composable
private fun FSummary(s: WireSession, enabled: Boolean) {
    FLabel("여행 기록", 32, 36, 36, 46, weight = FontWeight.Bold)
    FLabel("${s.destination} 가는 길 · 11월 5일 · ${s.invitees.size + 1}대", 32, 82, 26, 34, FMuted)
    FRect(32, 144, 880, 890, FCard, 56)
    FRect(936, 144, 732, 890, FCard, 56)
    FLabel("✦", 80, 195, 48, 60, FBlue, FontWeight.Bold)
    FLabel("AI 여행 요약", 154, 207, 36, 46, weight = FontWeight.Bold)
    FLabel("세 대가 함께 달린 가평 여행. 만장일치로", 80, 294, 32, 48)
    Text(buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("청평휴게소") }
        append("에 들러 호두과자를 먹고,")
    }, Modifier.offset(80.dp, 342.dp), style = TextStyle(fontFamily = FFont,
        fontSize = 32.sp, lineHeight = 48.sp, color = FInk))
    Text(buildAnnotatedString {
        append("첫 곡은 아이유의 ")
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("밤편지") }
        append("로 시작했어요.")
    }, Modifier.offset(80.dp, 390.dp), style = TextStyle(fontFamily = FFont,
        fontSize = 32.sp, lineHeight = 48.sp, color = FInk))
    FLabel("길 위의 한마디", 80, 546, 32, 42, weight = FontWeight.Bold)
    FLabel("“음료수 좀 사 오기”", 80, 603, 28, 38)
    FLabel("다연이 길에 남긴 음성", 80, 646, 24, 32, FMuted)
    listOf(Triple("24", "대화", 122), Triple("6", "노크", 345),
        Triple("1", "남긴 음성", 568), Triple("18", "함께 들은 곡", 743)).forEach { (value, title, x) ->
        FLabel(value, x, 866, 74, 84, FInk, FontWeight.SemiBold)
        FLabel(title, x, 958, 24, 32, FMuted)
    }
    FLabel("안전 운전 순위", 968, 185, 36, 46, weight = FontWeight.Bold)
    listOf(Triple("나", "85", R.drawable.flow_imgprofilephoto),
        Triple("다연", "88", R.drawable.invite_dayeon),
        Triple("문준", "66", R.drawable.invite_munjun)).forEachIndexed { i, (name, score, image) ->
        val x = 1006 + i * 214
        FAvatar(image, x + 25, 260, 125, name)
        FLabel(name, x + 64 - name.length * 7, 401, 30, 38, weight = FontWeight.Bold)
        FScoreRing(x + 14, 493, score.toInt(), if (i == 2) FRed else FBlue)
        FLabel(score, x + 49, 536, 39, 48, if (i == 2) FRed else FBlue, FontWeight.Bold)
        FLabel(if (i == 1) "1시간 15분" else if (i == 0) "1시간 36분" else "1시간 28분",
            x + 10, 687, 23, 32, FMuted)
        FRect(x, 736, 193, 115, if (i == 1) Color(0xFFCDD6FF) else FPale, 25)
        FLabel((if (i == 0) 2 else if (i == 1) 1 else 3).toString(),
            x + 85, 766, 34, 44, FMuted, FontWeight.Bold)
    }
    FRect(968, 869, 668, 137, FPale, 40)
    FRect(1006, 896, 78, 78, Color.White, 39)
    FImage(R.drawable.figma_summary_cup, 1021, 907, 48, 52,
        scale = ContentScale.Fit)
    FLabel("내기 결과", 1090, 892, 25, 34, FMuted)
    FLabel(if (s.wager) "커피는 문준님이 사요" else "선택한 내기 없음",
        1090, 932, 30, 38, FInk, FontWeight.SemiBold)
    FButton("저장하기", 32, 1060, 880, 88, enabled) { s.open(WireScreen.SAVE) }
    FButton("저장 안 함", 936, 1060, 732, 88, enabled, primary = false) {
        s.saved = false; s.home()
    }
}
