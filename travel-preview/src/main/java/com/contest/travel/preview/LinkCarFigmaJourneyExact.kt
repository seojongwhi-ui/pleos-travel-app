package com.contest.travel.preview

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign

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
                    WireScreen.HOME -> PHome(s, enabled)
                    WireScreen.CREATE -> FCreate(s, enabled)
                    WireScreen.INVITE -> FInvite(s, enabled)
                    WireScreen.WAITING -> FWaiting(s, enabled)
                    WireScreen.WAITING_DECLINED -> FWaiting(s, enabled, declined = true)
                    WireScreen.INVITE_CONFIRM -> PInviteConfirmation(s, enabled)
                    WireScreen.SHARE_SCOPE -> PShareScope(s, enabled)
                    WireScreen.GUEST_WAITING -> PGuestWaiting(s, enabled)
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

// Source: Figma 443:14524 and the eleven frames in linkcar-predrive-figma.json.
// All positions are authored coordinates inside the existing uniformly fitted frame.
// Static images are individual Figma assets, never a rendered screen.
internal object PreDriveLook {
    val ink = Color(0xFF131417)
    val action = Color(0xFF191A1B)
    val muted = Color(0xFF8F919F)
    val hint = Color(0xFF959DAF)
    val blue = Color(0xFF5263E7)
    val pale = Color(0xFFF3F4F8)
    val secondary = Color(0xFFEEEFF4)
    val disabled = Color(0xFFCBCFD3)
    val disabledText = Color(0xFF9EA4B1)
    val selected = Color(0xFFEEF0FF)
    val chip = Color(0xFFE7EAFF)
    val ghost = Color(0xFFACB1C0)
    val border = Color(0xFFCDD0D8)
    val photoFade = Color(0xFFFBFBFD)
    val photoCover = Color(0xFFFAFBFD)
    val danger = Color(0xFFFF5F5F)
    val dangerPale = Color(0xFFFFECEC)
    val dangerScore = Color(0xFFFF6B6E)
    val divider = Color(0xFFE1E4EF)
    val card = Color.White.copy(alpha = .8f)
}

private enum class PType(val size: Int, val line: Int, val weight: FontWeight,
    val tracking: Float = -.01f) {
    Header(32, 44, FontWeight.Bold), Sub(26, 36, FontWeight.Normal),
    H1(44, 58, FontWeight.Bold, -.02f), H2(36, 48, FontWeight.Bold, -.02f),
    Display(56, 72, FontWeight.Bold, -.02f), Body(28, 40, FontWeight.Normal),
    BodyMedium(28, 40, FontWeight.Medium), Title(32, 44, FontWeight.SemiBold),
    TitleSmall(28, 40, FontWeight.SemiBold), Button(32, 40, FontWeight.SemiBold),
    ButtonBold(32, 40, FontWeight.Bold), Eyebrow(26, 36, FontWeight.SemiBold),
    Caption(24, 34, FontWeight.Normal), Small(22, 28, FontWeight.SemiBold),
    Chip(24, 32, FontWeight.SemiBold), Detail(20, 36, FontWeight.SemiBold),
    Score(44, 52, FontWeight.Bold, -.02f), GuestHero(48, 58, FontWeight.Bold),
    GuestTitle(44, 54, FontWeight.Bold), GuestSection(40, 48, FontWeight.Bold),
    GuestName(32, 38, FontWeight.Bold), GuestMeta(24, 29, FontWeight.Medium),
    GuestSub(26, 32, FontWeight.Medium), GuestHint(22, 27, FontWeight.Medium),
    GuestRow(30, 36, FontWeight.Bold), GuestDetail(22, 27, FontWeight.Medium),
    GuestChip(20, 24, FontWeight.SemiBold), TinyChip(18, 22, FontWeight.SemiBold),
    Footnote(20, 24, FontWeight.Medium)
}

private fun PType.style(color: Color = PreDriveLook.ink) = TextStyle(
    fontFamily = FFont, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight,
    letterSpacing = (size * tracking).sp, color = color,
    platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
        androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
        androidx.compose.ui.text.style.LineHeightStyle.Trim.None))

private fun Modifier.pBounds(x: Number, y: Number, w: Number, h: Number) =
    offset(x.toFloat().dp, y.toFloat().dp)
        .wrapContentSize(Alignment.TopStart, unbounded = true)
        .requiredSize(w.toFloat().dp, h.toFloat().dp)

@Composable
private fun PText(text: String, x: Number, y: Number, type: PType = PType.Title,
    color: Color = PreDriveLook.ink, width: Number? = null, center: Boolean = false,
    end: Boolean = false) {
    Text(text, Modifier.offset(x.toFloat().dp, y.toFloat().dp)
        .then(if (width != null) Modifier.width(width.toFloat().dp) else Modifier),
        style = type.style(color), maxLines = 1, overflow = TextOverflow.Ellipsis,
        textAlign = if (end) TextAlign.End else if (center) TextAlign.Center else TextAlign.Start)
}

@Composable
private fun PAccent(prefix: String, rest: String, x: Number, y: Number,
    type: PType = PType.GuestTitle) {
    Text(buildAnnotatedString {
        withStyle(SpanStyle(color = PreDriveLook.blue)) { append(prefix) }
        append(rest)
    }, Modifier.offset(x.toFloat().dp, y.toFloat().dp), style = type.style(), maxLines = 1)
}

@Composable
private fun PImage(id: Int, x: Number, y: Number, w: Number, h: Number,
    modifier: Modifier = Modifier, description: String? = null,
    scale: ContentScale = ContentScale.FillBounds) {
    Image(painterResource(id), description, Modifier.pBounds(x, y, w, h).then(modifier),
        contentScale = scale)
}

@Composable
private fun PPanel(x: Number, y: Number, w: Number, h: Number, radius: Number = 56,
    color: Color = PreDriveLook.card, modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}) {
    Box(Modifier.pBounds(x, y, w, h).then(modifier)
        .clip(RoundedCornerShape(radius.toFloat().dp)).background(color), content = content)
}

@Composable
private fun PChip(text: String, x: Number, y: Number, w: Number, h: Number = 40,
    color: Color = PreDriveLook.chip, ink: Color = PreDriveLook.blue,
    type: PType = PType.GuestChip) {
    PPanel(x, y, w, h, 100, color) {
        Text(text, Modifier.align(Alignment.Center), style = type.style(ink), maxLines = 1)
    }
}

private enum class PButtonTone { Primary, Secondary, Muted }

@Composable
private fun PButton(text: String, x: Number, y: Number, w: Number, h: Number = 88,
    enabled: Boolean = true, tone: PButtonTone = PButtonTone.Primary,
    arrow: Boolean = false, centered: Boolean = false,
    arrowId: Int = R.drawable.predrive_5a700, arrowSize: Number = 38,
    onClick: () -> Unit) {
    val primary = tone == PButtonTone.Primary
    val background = if (primary) {
        if (enabled) PreDriveLook.action else PreDriveLook.disabled
    } else PreDriveLook.secondary
    val ink = if (primary) Color.White else if (tone == PButtonTone.Muted)
        PreDriveLook.disabledText else PreDriveLook.ink
    val radius = if (h.toFloat() >= 104) 48 else 35
    PPanel(x, y, w, h, radius, background,
        Modifier.linkCarPress(enabled, onClick = onClick).semantics(mergeDescendants = true) {}) {
        if (centered) {
            Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(text, style = PType.ButtonBold.style(ink))
                if (arrow) Image(painterResource(R.drawable.predrive_e61c2), null, Modifier.size(36.dp))
            }
        } else {
            Text(text, Modifier.align(if (arrow) Alignment.CenterStart else Alignment.Center)
                .then(if (arrow) Modifier.padding(start = 48.dp, end = 78.dp) else Modifier),
                style = PType.Button.style(ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (arrow) Image(painterResource(arrowId), null,
                Modifier.align(Alignment.CenterEnd).padding(end = 32.dp).size(arrowSize.toFloat().dp))
        }
    }
}

@Composable
private fun PHeader(title: String, subtitle: String, s: WireSession, enabled: Boolean,
    step: Int? = null) {
    // Expand the transparent hit area without moving the authored icon.
    PPanel(15, 28, 100, 100, 50, Color.Transparent,
        Modifier.linkCarPress(enabled, onClick = s::back).semantics { contentDescription = "뒤로가기" }) {
        PImage(R.drawable.predrive_f92b5, 33.59, 33.59, 33.184, 33.184)
    }
    PText(title, 128, 38, PType.Header, width = 1200)
    PText(subtitle, 128, 82, PType.Sub, PreDriveLook.muted, 1200)
    if (step != null) {
        PPanel(1582, 68.32, 36.502, 9.955, 5,
            if (step == 1) Color(0xFF252923) else Color(0xFFADB5C5))
        PPanel(1628.46, 68.32, 36.502, 9.955, 5,
            if (step == 2) Color(0xFF252923) else Color(0xFFADB5C5))
    }
}

@Composable
private fun PMicrophone(x: Number, y: Number, size: Number = 54,
    icon: Int = R.drawable.predrive_b2263) {
    PPanel(x, y, size, size, size, Color(0xFF252927)) {
        PImage(icon, size.toFloat() / 4, size.toFloat() / 4,
            size.toFloat() / 2, size.toFloat() / 2, description = "음성 입력 (연결 전)")
    }
}

@Composable
private fun PInput(value: String, onChange: (String) -> Unit, placeholder: String,
    x: Number, y: Number, w: Number, h: Number, enabled: Boolean,
    search: Boolean = false, voiceInset: Int = 62) {
    val keyboard = LocalSoftwareKeyboardController.current
    PPanel(x, y, w, h, h.toFloat() / 2,
        if (search) Color(0xFFE9EBF2) else PreDriveLook.secondary) {
        if (search) PImage(R.drawable.predrive_8cd38, 23, 22, 27, 27)
        val inset = if (search) 70 else 43
        BasicTextField(value, onChange, Modifier.offset(inset.dp, 0.dp)
            .size((w.toFloat() - inset - 100).dp, h.toFloat().dp),
            enabled = enabled, singleLine = true, textStyle = PType.BodyMedium.style(),
            cursorBrush = SolidColor(PreDriveLook.blue),
            keyboardOptions = KeyboardOptions(keyboardType = if (search) KeyboardType.Text else KeyboardType.Phone,
                imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(placeholder, style = PType.BodyMedium.style(PreDriveLook.hint))
                    inner()
                }
            })
        if (search) PMicrophone(w.toFloat() - voiceInset, 8)
        else PMicrophone(w.toFloat() - 81, 14, 66, R.drawable.predrive_b8c48)
    }
}

@Composable
private fun PHome(s: WireSession, enabled: Boolean) {
    val convoy = LocalConvoy.current
    PImage(R.drawable.predrive_e4340, 32, 39, 76, 76)
    PText("안녕하세요, 주석님", 128, 36, PType.Header)
    PText("내 차 · 아이오닉 5", 128, 79, PType.Sub, PreDriveLook.muted)
    PPanel(32, 144, 1016, 1008) {
        PImage(R.drawable.predrive_0ce7a, 0, 0, 1016, 825, scale = ContentScale.Crop)
        Box(Modifier.pBounds(0, 0, 1016, 825).background(Brush.verticalGradient(
            0f to Color.Transparent, .5641f to Color.Transparent,
            .82967f to Color(0xFFF9F9FB), 1f to Color(0xFFF9F9FB))))
        PText("새 여행 만들기", 48, 690, PType.H1)
        PText("목적지를 정하고 함께 갈 차를 초대해요", 48, 760, PType.Body, PreDriveLook.muted)
        PPanel(48, 840, 916, 120, 48, PreDriveLook.action,
            Modifier.linkCarPress(enabled) { convoy.reset(); s.open(WireScreen.CREATE) }
                .semantics(mergeDescendants = true) {}) {
            PText("여행 만들기", 48, 40, PType.Button, Color.White)
            PImage(R.drawable.predrive_fcc52, 848.65, 46.65, 31.7, 26.7)
        }
    }
    PPanel(1072, 144, 596, 1008) {
        PText("지난 여행 다시 가기", 32, 37, PType.H2)
        listOf(125 to true, 347 to false).forEach { (y, first) ->
            PPanel(20, y, 556, 201, 40, PreDriveLook.pale,
                Modifier.linkCarPress(enabled) {
                    s.isGuest = false
                    s.destination = if (first) "가평 용추계곡" else "할머니 댁"
                    s.purpose = if (first) "친구 여행" else "가족"
                    s.open(WireScreen.WAITING)
                }.semantics(mergeDescendants = true) {}) {
                PText(if (first) "가평 계곡 가는 길" else "할머니 댁", 36, 16)
                PText(if (first) "친구 여행 · 10월 12일" else "가족 · 9월 17일",
                    36, 57, PType.Sub, PreDriveLook.muted)
                PImage(if (first) R.drawable.predrive_ae7bc else R.drawable.predrive_284cc, 36, 115, 66, 66)
                if (first) PImage(R.drawable.predrive_59733, 105, 115, 66, 66)
                PImage(R.drawable.predrive_013b1, 501.2, 84, 20.8, 31.2)
            }
        }
    }
}

@Composable
private fun FCreate(s: WireSession, enabled: Boolean) {
    PHeader("여행 만들기", "이동 목적과 목적지를 정해 주세요", s, enabled, 1)
    PText("이동 목적", 32, 147, PType.H2)
    val purposes = listOf(
        Triple("친구 여행", "함께 듣는 DJ · 순위 켜짐", R.drawable.predrive_ef85f),
        Triple("가족", "안부 · 도착 알림 중심", R.drawable.predrive_f5bf5),
        Triple("업무 · 단체", "알림 최소화", R.drawable.predrive_2b158))
    purposes.forEachIndexed { index, (name, detail, image) ->
        val dim by animateFloatAsState(if (s.createPurposeSelection != null && s.createPurposeSelection != name)
            .2f else 1f, tween(180), label = "purposeSelection")
        val tint = when (index) { 0 -> Color(0xFFD4BA63); 1 -> Color(0xFFFFB258); else -> Color(0xFF96A2FF) }
        val width = if (index == 2) 529 else 530
        PPanel(32 + index * 554, 206, width, 380, 36, Color.White,
            Modifier.graphicsLayer { alpha = dim }
                .then(if (index < 2) Modifier.border(3.dp, Color(0xFFE5E7EC), RoundedCornerShape(36.dp)) else Modifier)
                .linkCarPress(enabled, Role.RadioButton) { s.purpose = name; s.createPurposeSelection = name }
                .semantics(mergeDescendants = true) { selected = s.createPurposeSelection == name }) {
            PImage(image, if (index == 2) -65 else -14, if (index == 2) 0 else 3,
                if (index == 2) 687 else 671, if (index == 2) 387 else 378, scale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                0f to Color.Transparent, .54f to Color.Transparent,
                .78105f to tint.copy(alpha = .9f), 1f to tint.copy(alpha = .9f))))
            PText(name, 36, 262, PType.Header, Color.White)
            PText(detail, 36, 309, PType.Detail, Color.White)
            androidx.compose.animation.AnimatedVisibility(s.createPurposeSelection == name,
                Modifier.offset(452.dp, 303.dp), enter = fadeIn(tween(150)) + scaleIn(initialScale = .8f),
                exit = fadeOut(tween(100))) {
                Image(painterResource(R.drawable.predrive_5b547), null, Modifier.size(46.dp))
            }
        }
    }
    PPanel(32, 610, 1636, 542) {
        PText("목적지", 36, 32, PType.H2)
        PInput(s.destinationQuery, {
            s.destinationQuery = it; s.destination = it; s.createRecentSelection = null
        }, "어디로 가나요?", 36, 110, 709, 70, enabled, search = true,
            voiceInset = if (s.createRecentSelection != null) 83 else 62)
        PPanel(781, 55, 2, 432, 0, Color(0xFFEBEBEB))
        PText("최근 기록", 819, 42, PType.Eyebrow, PreDriveLook.muted)
        listOf("가평 용추계곡" to "경기 가평군 · 82km", "양주 글램핑장" to "경기 양주시 · 45km",
            "할머니 댁" to "충남 공주시 · 140km").forEachIndexed { i, (name, detail) ->
            val chosen = s.createRecentSelection == name
            val selectedLayout = s.createRecentSelection != null
            val y = if (selectedLayout) listOf(108, 216, 315)[i] else 100 + i * 99
            val titleY = if (selectedLayout) { if (i == 0) 6 else 2 } else if (i == 0) 14 else 11
            val detailY = if (selectedLayout) { if (i == 0) 47 else 43 } else if (i == 0) 55 else 52
            PPanel(819, y, 785, 88, 24, if (chosen) Color(0xFFE5E8FF) else Color.Transparent,
                Modifier.linkCarPress(enabled, Role.RadioButton) {
                    s.destination = name; s.destinationQuery = ""; s.createRecentSelection = name
                }.semantics(mergeDescendants = true) { selected = chosen }) {
                PText(name, 36, titleY, PType.TitleSmall)
                PText(detail, 36, detailY, PType.Sub, PreDriveLook.muted)
                if (chosen) PImage(R.drawable.predrive_5b547, 718, 21, 46, 46)
            }
        }
        val ready = s.createPurposeSelection != null &&
            (s.createRecentSelection != null || s.destinationQuery.isNotBlank())
        PButton("초대하기", 36, 418, 709, enabled = enabled && ready, arrow = true) {
            s.isGuest = false; s.open(WireScreen.INVITE)
        }
        if (ready) PText("${s.purpose} · ${s.destination}", 819, 442,
            PType.TitleSmall, PreDriveLook.blue, 785, end = true)
    }
}

private data class PPerson(val name: String, val car: String, val image: Int)
private val PPeople = listOf(
    PPerson("다연", "쏘렌토", R.drawable.predrive_05649),
    PPerson("문준", "아반떼", R.drawable.predrive_42075),
    PPerson("성윤", "차량 정보 없음", R.drawable.predrive_fb62c),
    PPerson("민석", "차량 정보 없음", R.drawable.predrive_73a18),
    PPerson("현진", "차량 정보 없음", R.drawable.predrive_049b1),
    PPerson("준호", "차량 정보 없음", R.drawable.predrive_5e95f))

private fun participant(name: String) = PPeople.firstOrNull { it.name == name }
    ?: PPerson(name, "차량 정보 없음", R.drawable.predrive_5e95f)

@Composable
private fun FInvite(s: WireSession, enabled: Boolean) {
    PHeader("${s.destination} 가는 길", "${s.purpose} · 그룹이 만들어졌어요", s, enabled, 2)
    PPanel(109, 144, 1482, 978) {
        PText("함께 갈 차를 초대하세요", 89, 60, PType.H2)
        PText("앱을 쓰는 사람만 초대할 수 있어요 · 내 차 포함 최대 4대", 89, 114, PType.Sub, PreDriveLook.muted)
        PInput(s.phone, { s.phone = it.filter { c -> c.isDigit() || c in "+- " }.take(20) },
            "전화번호 입력", 89, 188, 1304, 94, enabled)
        PText("최근 함께한 사람", 89, 364, PType.Eyebrow, PreDriveLook.muted)
        PPeople.forEachIndexed { i, person ->
            PPanel(89 + i * 227, 434, 170, 240, 30, Color.Transparent,
                Modifier.linkCarPress(enabled, Role.Checkbox) { s.toggleInvitee(person.name) }
                    .semantics(mergeDescendants = true) {
                        toggleableState = if (person.name in s.invitees) ToggleableState.On else ToggleableState.Off
                    }) {
                PImage(person.image, 13, 0, 143, 143)
                PText(person.name, 0, 151, PType.Title, width = 170, center = true)
                PText(if (person.name in s.invitees) "선택됨" else "앱 사용 중",
                    0, 198, PType.Sub, PreDriveLook.muted, 170, center = true)
                if (person.name in s.invitees) PImage(R.drawable.predrive_3cd58, 111, 97, 44, 44)
            }
        }
        PButton("초대 보내기 (${s.invitees.size})", 86, 842, 377,
            enabled = enabled && s.canSendInvites, arrow = true,
            arrowId = R.drawable.predrive_4fe44, arrowSize = 46) {
            s.sendInvites()
        }
        val people = s.orderedInvitees.take(3)
        if (people.isNotEmpty()) {
            val start = 927 - (people.size - 2).coerceAtLeast(0) * 74
            PText("선택한 차", start, 868, PType.Sub, PreDriveLook.muted)
            people.forEachIndexed { i, name ->
                val id = when (name) { "다연" -> R.drawable.predrive_a29f8
                    "문준" -> R.drawable.predrive_8525d; else -> participant(name).image }
                PImage(id, start + 130 + i * 74, 859, 54, 54)
            }
            PText("${people.joinToString(", ")} · ${people.size}대", 1205, 866,
                PType.TitleSmall, width = 260)
        }
    }
}

@Composable
private fun PHero(width: Int, fadeY: Int, coverY: Int? = null, coverH: Int = 0,
    photoH: Int = 665, content: @Composable BoxScope.() -> Unit) {
    PPanel(32, 144, width, 1008) {
        PImage(R.drawable.predrive_c29e3, 0, 0, width, photoH, scale = ContentScale.Crop)
        Box(Modifier.pBounds(0, fadeY, width, 210).background(Brush.verticalGradient(
            0f to Color.Transparent, .67857f to PreDriveLook.photoFade,
            1f to PreDriveLook.photoFade)))
        if (coverY != null) PPanel(0, coverY, width, coverH, 0, PreDriveLook.photoCover)
        content()
    }
}

@Composable
private fun PDashedOutline(width: Int, height: Int, radius: Int, color: Color,
    thickness: Int = 3) {
    Canvas(Modifier.size(width.dp, height.dp)) {
        val stroke = thickness.dp.toPx()
        drawRoundRect(color, Offset(stroke / 2, stroke / 2),
            Size(size.width - stroke, size.height - stroke),
            CornerRadius(radius.dp.toPx()), style = Stroke(stroke,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 8.dp.toPx()))))
    }
}

private fun waitingPerson(name: String) = when (name) {
    "다연" -> PPerson(name, "쏘렌토", R.drawable.predrive_fcb24)
    "문준" -> PPerson(name, "아반떼", R.drawable.predrive_04b1f)
    else -> participant(name)
}

@Composable
private fun PPersonCard(person: PPerson, x: Int, y: Int,
    compact: Boolean, guest: Boolean, declined: Boolean, hasPassenger: Boolean) {
    val me = guest && person.name == "다연"
    val rejected = declined && person.name == "다연"
    val shape = RoundedCornerShape(40.dp)
    PPanel(x, y, 318, 424, 40,
        if (me) PreDriveLook.selected else PreDriveLook.pale,
        if (me) Modifier.border(4.dp, PreDriveLook.blue, shape) else Modifier) {
        val photo = if (rejected) R.drawable.predrive_1acdc else person.image
        PImage(photo, 104, if (compact) 56 else 61, 110, 110,
            modifier = Modifier.graphicsLayer { alpha = if (rejected) .35f else 1f })
        val name = if (me) "나 (다연)" else person.name
        PText(name, 0, if (compact) 190 else 207,
            if (compact) PType.GuestName else PType.Title,
            if (rejected) PreDriveLook.ghost else PreDriveLook.ink, 318, center = true)
        PText(person.car, 0, if (compact) 238 else 286,
            if (compact) PType.GuestMeta else PType.Sub, PreDriveLook.muted, 318, center = true)
        val chip = when {
            rejected -> "이번엔 어려워요"
            me -> if (hasPassenger) "동승자 있음" else "혼자 운전"
            person.name == "문준" || person.name !in setOf("나", "주석", "다연") -> if (compact) "탑승 전" else "탑승전"
            else -> "동승자 있음"
        }
        val chipWidth = if (!compact) 144 else when (chip) {
            "이번엔 어려워요" -> 158; "동승자 있음" -> 124; "혼자 운전" -> 106; else -> 89 }
        val chipColor = when {
            rejected -> PreDriveLook.dangerPale
            me -> PreDriveLook.blue
            !compact -> Color.White
            chip == "탑승 전" -> PreDriveLook.secondary
            else -> PreDriveLook.chip
        }
        val chipInk = when {
            rejected -> PreDriveLook.danger
            me -> Color.White
            chip == "탑승전" -> PreDriveLook.dangerScore
            chip == "탑승 전" -> Color(0xFF686A72)
            else -> PreDriveLook.blue
        }
        PChip(chip, (318 - chipWidth) / 2, if (compact) 290 else 338,
            chipWidth, if (compact) 40 else 44, chipColor, chipInk,
            if (compact) PType.GuestChip else PType.Chip)
    }
}

@Composable
private fun PWaitingGrid(s: WireSession, enabled: Boolean, guest: Boolean = false,
    declined: Boolean = false) {
    val compact = guest || declined
    val people = listOf(PPerson(if (guest) "주석" else "나",
        if (guest) "아이오닉 5 · 방장" else "아이오닉 5", R.drawable.predrive_33c8a)) +
        s.orderedInvitees.take(3).map(::waitingPerson)
    val count = people.count { !declined || it.name !in s.declinedInvitees }
    PPanel(976, 144, 692, 1008) {
        PText("함께 가는 사람", 32, if (compact) 40 else 34, PType.H2)
        PText("$count / 4", if (compact) 592 else 602, if (compact) 44 else 39,
            if (compact) PType.GuestSub else PType.Body, PreDriveLook.muted)
        (0 until 4).forEach { i ->
            val x = 20 + i % 2 * 334
            val y = 116 + i / 2 * 444
            val person = people.getOrNull(i)
            if (person != null) PPersonCard(person, x, y, compact, guest,
                declined && person.name in s.declinedInvitees, s.guestHasPassenger)
            else PPanel(x, y, 318, 424, if (compact) 40 else 30, Color.Transparent,
                if (!guest) Modifier.linkCarPress(enabled) { s.open(WireScreen.INVITE) }
                    .semantics(mergeDescendants = true) {} else Modifier) {
                PDashedOutline(318, 424, if (compact) 40 else 30,
                    if (compact) PreDriveLook.border else Color(0xFFC4CCE4), if (compact) 3 else 2)
                if (compact) PPanel(117, 110, 84, 84, 42, PreDriveLook.chip) {
                    PImage(R.drawable.predrive_79f49, 24, 24, 36, 36)
                } else {
                    PImage(R.drawable.predrive_80842, 114, 114, 84, 84)
                    PImage(R.drawable.predrive_7bd95, 142.33, 142.33, 27.333, 27.333)
                }
                PText(if (guest) "빈 자리" else "초대하기", 0, if (compact) 220 else 217,
                    if (compact) PType.GuestName else PType.Title, width = 318, center = true)
                PText(if (guest) "방장이 초대할 수 있어요" else "${4 - count}자리 남음",
                    0, if (compact) 268 else 272,
                    if (compact) PType.GuestMeta else PType.Sub, PreDriveLook.muted, 318, center = true)
            }
        }
    }
}

@Composable
private fun FWaiting(s: WireSession, enabled: Boolean, declined: Boolean = false) {
    PHeader("${s.destination} 가는 길", "${s.purpose} · 도착 예정 15:40", s, enabled)
    PHero(920, if (declined) 441 else 513, if (declined) 599 else null, 67) {
        if (declined) {
            PAccent("다연님", "은 이번엔 함께하지 못해요", 48, 640)
            PText("자리는 비워 둘게요 · 다시 초대하거나 바로 출발해도 돼요", 48, 708,
                PType.GuestSub, Color(0xFF9398A5), 824)
            PButton("다시 초대", 24, 864, 520, 120, enabled, arrow = true, centered = true) {
                s.open(WireScreen.INVITE)
            }
            PButton("바로 출발", 560, 864, 336, 120, enabled, PButtonTone.Muted,
                centered = true) { s.startLocalTrip(false) }
        } else {
            val names = s.orderedInvitees.filterNot { it in s.declinedInvitees }
            PText(if (names.isEmpty()) "함께 떠날 준비가 됐어요!" else "${names.take(2).joinToString(", ")}이 함께해요!",
                48, 717, PType.H1, width = 824)
            PText("${3 - names.size}자리 남았어요 · 지금 바로 출발해도 돼요",
                48, 784, PType.Body, PreDriveLook.muted, 824)
            PButton("출발 준비하기", 48, 872, 335, enabled = enabled, arrow = true) {
                s.open(WireScreen.MISSION)
            }
            PButton("바로 출발", 398, 872, 239, 89, enabled, PButtonTone.Secondary) {
                s.startLocalTrip(false)
            }
        }
    }
    PWaitingGrid(s, enabled, declined = declined)
}

@Composable
private fun PInviteConfirmation(s: WireSession, enabled: Boolean) {
    PHeader("초대가 왔어요", "주석님이 보낸 여행 초대", s, enabled)
    PHero(1016, 390, 599, 136, photoH = 734) {
        PAccent("주석님", "이 함께 가자고 해요", 56, 640, PType.GuestHero)
        PText("${s.destination} 가는 길 · ${s.purpose} · 도착 예정 15:40", 56, 712,
            PType.GuestSub, Color(0xFF9398A5), 920)
        PImage(R.drawable.predrive_91144, 56, 770, 26, 26)
        PText("말로 \"같이 갈래\"라고 해도 돼요", 90, 770, PType.Small, PreDriveLook.ghost)
        PButton("다음에", 24, 864, 340, 120, enabled, PButtonTone.Muted, centered = true) {
            s.home()
        }
        PButton("같이 갈래요", 380, 864, 612, 120, enabled, arrow = true, centered = true) {
            s.isGuest = true; s.open(WireScreen.SHARE_SCOPE)
        }
    }
    PPanel(1072, 144, 596, 1008) {
        PText("함께 가는 사람", 40, 40, PType.H2)
        PText("2 / 4", 480, 44, PType.GuestSub, PreDriveLook.muted)
        val rows = listOf(Triple("주석", "아이오닉 5 · 방장", R.drawable.predrive_29a07),
            Triple("문준", "아반떼 · 참여 확정", R.drawable.predrive_1cfb5),
            Triple("나", "쏘렌토 · 응답 대기", R.drawable.predrive_7f98d))
        rows.forEachIndexed { i, (name, detail, image) ->
            val y = 120 + i * 190
            PPanel(20, y, 556, 170, 40,
                if (i == 2) PreDriveLook.selected else PreDriveLook.pale,
                if (i == 2) Modifier.border(3.dp, PreDriveLook.blue, RoundedCornerShape(40.dp)) else Modifier)
            PImage(image, 48, y + 35, 100, 100)
            PText(name, 172, y + 44, PType.GuestName)
            PText(detail, 172, y + 90, PType.GuestDetail, PreDriveLook.muted)
            PChip(listOf("방장", "참여", "대기 중")[i], if (i == 2) 473 else 492,
                y + 64, if (i == 2) 83 else 64, 37,
                listOf(PreDriveLook.blue, PreDriveLook.chip, PreDriveLook.secondary)[i],
                listOf(Color.White, PreDriveLook.blue, Color(0xFF686A72))[i], PType.TinyChip)
        }
        PPanel(20, 700, 556, 170, 40, Color.Transparent) {
            PDashedOutline(556, 170, 40, PreDriveLook.border)
            PText("빈 자리 1", 0, 66, PType.TitleSmall, PreDriveLook.ghost, 556, center = true)
        }
    }
}

@Composable
private fun PGuestWaiting(s: WireSession, enabled: Boolean) {
    PHeader("${s.destination} 가는 길", "주석님의 여행 · 도착 예정 15:40", s, enabled)
    PHero(920, 310, 519, 147) {
        PAccent("대기실", "에 들어왔어요", 48, 548, PType.GuestHero)
        PText("주석님이 출발하면 함께 출발해요 · 미션과 음악은 함께 정해요", 48, 616,
            PType.GuestSub, Color(0xFF9398A5), 824)
        PText("오늘 나는", 48, 700, PType.Chip, PreDriveLook.ghost)
        PPanel(24, 748, 872, 120, 48, PreDriveLook.secondary) {
            val x by animateFloatAsState(if (s.guestHasPassenger) 440f else 8f,
                tween(220, easing = FastOutSlowInEasing), label = "passengerSelection")
            PPanel(x, 8, 424, 104, 42, PreDriveLook.action)
            listOf("혼자 운전", "동승자 있음").forEachIndexed { i, label ->
                PPanel(8 + i * 432, 8, 424, 104, 42, Color.Transparent,
                    Modifier.linkCarPress(enabled, Role.RadioButton) { s.guestHasPassenger = i == 1 }
                        .semantics(mergeDescendants = true) { selected = s.guestHasPassenger == (i == 1) }) {
                    Text(label, Modifier.align(Alignment.Center), style = PType.ButtonBold.style(
                        if (s.guestHasPassenger == (i == 1)) Color.White else PreDriveLook.disabledText))
                }
            }
        }
        PText(if (s.guestHasPassenger) "동승자와 함께 편안하게 대화할 수 있어요" else
            "혼자 운전이면 노크는 짧은 말로만 주고받아요", 48, 900, PType.Small, PreDriveLook.ghost)
    }
    PWaitingGrid(s, enabled, guest = true)
}

@Composable
private fun PSharingSwitch(checked: Boolean, x: Int, y: Int, enabled: Boolean,
    label: String, onChange: () -> Unit) {
    val onAlpha by animateFloatAsState(if (checked) 1f else 0f,
        tween(160), label = "sharingSwitch")
    PPanel(x, y, 96, 56, 28, Color.Transparent,
        Modifier.linkCarPress(enabled, Role.Switch, onChange).semantics {
            contentDescription = label
            toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
        }) {
        PImage(R.drawable.predrive_c816a, 0, 0, 96, 56,
            modifier = Modifier.graphicsLayer { alpha = 1f - onAlpha })
        PImage(R.drawable.predrive_47016, 0, 0, 96, 56,
            modifier = Modifier.graphicsLayer { alpha = onAlpha })
    }
}

@Composable
private fun PShareScope(s: WireSession, enabled: Boolean) {
    PHeader("함께 가기 전에 확인해 주세요", "동행하는 동안 다른 차에 보이는 정보예요", s, enabled)
    PPanel(32, 144, 1636, 880) {
        PText("이런 정보가 함께 공유돼요", 56, 48, PType.GuestSection)
        PText("동행이 끝나면 공유도 함께 끝나요", 56, 104, PType.GuestMeta, Color(0xFF9398A5))
        val rows = listOf(
            Triple("내 위치 · 남은 거리", "지도와 진행 바에 표시돼요", R.drawable.predrive_67484),
            Triple("안전 운전 점수", "급가속 · 급제동 · 과속만 반영돼요", R.drawable.predrive_18afc),
            Triple("노크 대화", "내가 문을 열 때만 30초 연결돼요", R.drawable.predrive_ca5d9),
            Triple("길에 남긴 음성", "경로 위 다른 차에게 음성을 남겨요", R.drawable.predrive_2a7db),
            Triple("함께 듣는 DJ", "내가 신청한 곡이 모두에게 재생돼요", R.drawable.predrive_b7e91))
        rows.forEachIndexed { i, (title, detail, icon) ->
            val y = 168 + 136 * i
            PPanel(56, y, 88, 88, 28, PreDriveLook.pale) { PImage(icon, 24, 24, 40, 40) }
            PText(title, 172, y + 8, PType.GuestRow)
            PText(detail, 172, y + 52, PType.GuestDetail, PreDriveLook.muted)
            if (i < 2) PChip("필수", 836, y + 26, 64, 37, type = PType.TinyChip)
            else PSharingSwitch(s.sharingOptions[i - 2], 804, y + 16, enabled, title) {
                s.sharingOptions = s.sharingOptions.mapIndexed { j, v -> if (j == i - 2) !v else v }
            }
            if (i < 4) PPanel(56, y + 112, 848, 2, 0, Color(0xFFEEEFF2))
        }
        PPanel(984, 48, 600, 784, 40, PreDriveLook.pale) {
            PText("공유되지 않는 것", 48, 48, PType.GuestName)
            listOf("대화 내용 녹음 · 저장", "정확한 출발지 · 집 주소", "동행이 끝난 뒤의 위치",
                "차 안 카메라 · 동승자 정보").forEachIndexed { i, text ->
                val y = 114 + i * 72
                PPanel(48, y, 44, 44, 22, PreDriveLook.danger) {
                    PImage(R.drawable.predrive_94766, 10, 10, 24, 24)
                }
                PText(text, 108, y + 6.5, PType.Eyebrow, Color(0xFF2E323A), 450)
            }
            PText("설정은 주행 중에도 상단 설정에서 바꿀 수 있어요", 48, 402,
                PType.Footnote, PreDriveLook.muted, 504)
        }
    }
    PButton("설정 바꾸기", 32, 1040, 600, 112, enabled, PButtonTone.Muted, centered = true) {
        s.showSettings = true
    }
    PButton("이대로 시작", 648, 1040, 1020, 112, enabled, arrow = true, centered = true) {
        s.sharingConfirmed = true; s.music = s.sharingOptions[2]
        s.open(WireScreen.GUEST_WAITING)
    }
}

@Composable
private fun FMission(s: WireSession, enabled: Boolean) {
    PHeader("출발 준비", "${s.destination} 가는 길 · ${s.invitees.size + 1}대", s, enabled, 1)
    PPanel(32, 144, 1636, 375, 49.805, Color(0xFFF1F4FF)) {
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(
            0f to Color(0xFFF1F4FF), .1527f to Color(0xFFF6F9FF), .38967f to Color(0xFFE0E7FA))))
        PImage(R.drawable.predrive_23d4a, 431, 0, 1205, 375)
        PText("오늘의 미션", 48, 58, PType.Eyebrow, PreDriveLook.muted)
        PText("안전하게 온 차가 1등!", 48, 151, PType.Display)
        Text(buildAnnotatedString {
            append("모든 차가 ")
            withStyle(SpanStyle(color = PreDriveLook.blue, fontWeight = FontWeight.Bold)) { append("100점") }
            append("에서 시작해요.")
        }, Modifier.offset(48.dp, 238.dp), style = PType.Button.style(PreDriveLook.muted)
            .copy(fontWeight = FontWeight.Normal, lineHeight = 46.sp))
    }
    PPanel(32, 543, 690, 495) {
        PText("감점 기준", 32, 29, PType.H2)
        val icons = listOf(R.drawable.predrive_87248, R.drawable.predrive_4ed4b, R.drawable.predrive_116df)
        listOf("급가속" to "−3점", "급제동" to "−3점", "과속" to "−5점").forEachIndexed { i, (label, score) ->
            val y = 130 + i * 120
            // SVG bounds are the exported inner group, including its stroke overflow.
            when (i) {
                0 -> PImage(icons[i], 44.1, y + 12.1, 36.8, 36.8)
                1 -> PImage(icons[i], 40.1, y + 8.1, 44.8, 44.8)
                else -> PImage(icons[i], 36.1, y + 22.1, 51.8, 26.8)
            }
            PText(label, 116, y + 9, PType.Title)
            PText(score, 565, y + 4, PType.Score, PreDriveLook.dangerScore)
            if (i < 2) PPanel(32, y + 90, 626, 2, 0, PreDriveLook.divider)
        }
    }
    PPanel(746, 543, 922, 495) {
        PText("참가하는 차", 32, 29, PType.H2)
        Text(buildAnnotatedString {
            append("시작 점수 ")
            withStyle(SpanStyle(color = PreDriveLook.blue, fontWeight = FontWeight.Bold, fontSize = 36.sp)) { append("100") }
            append(" 점")
        }, Modifier.offset(696.dp, 39.dp), style = PType.Sub.style(PreDriveLook.muted).copy(lineHeight = 44.sp))
        val people = listOf(PPerson("나", "아이오닉 5", R.drawable.predrive_f3941)) +
            s.orderedInvitees.take(3).map { name -> when (name) {
                "다연" -> PPerson(name, "쏘렌토", R.drawable.predrive_25e44)
                "문준" -> PPerson(name, "아반떼", R.drawable.predrive_5375b)
                else -> participant(name)
            } }
        val columns = people.size.coerceAtLeast(3)
        people.forEachIndexed { i, person ->
            val columnWidth = if (columns == 3) 289f else 866f / columns
            val left = if (columns == 3) 21f + columnWidth * i else 24f + columnWidth * i
            PImage(person.image, left + (columnWidth - 113) / 2, 127, 113, 113)
            PText(person.name, left, 250, PType.Title, width = columnWidth, center = true)
            PText(person.car, left, 293, PType.Sub, PreDriveLook.muted, columnWidth, center = true)
            if (i < people.lastIndex) PPanel(if (columns == 3) 305f + 289f * i else left + columnWidth - 8,
                143, 2, 166, 0, PreDriveLook.divider)
        }
        PPanel(30, 385, 862, 86, 26.563, PreDriveLook.secondary,
            Modifier.linkCarPress(enabled) { s.wager = !s.wager }
                .semantics(mergeDescendants = true) { selected = s.wager }) {
            PText("내기 걸기", 28, 23, PType.TitleSmall)
            PChip(if (s.wager) "선택됨" else "선택", 169, 23, 80, 40, Color(0xFFDDE1ED),
                PreDriveLook.muted, PType.Small)
            PPanel(279, 20, 2, 46, 0, Color(0xFFD7DBE7))
            PText(if (s.wager) "꼴찌가 커피 사기" else "'꼴찌가 커피 사기'처럼 말해 보세요",
                305, 26, PType.Caption, PreDriveLook.hint, 450)
            PMicrophone(772, 10, 65, R.drawable.predrive_5ca27)
        }
    }
    PButton("미션 없이 다음", 32, 1064, 339, enabled = enabled, tone = PButtonTone.Secondary) {
        s.mission = false; s.open(WireScreen.MUSIC_PICK)
    }
    PButton("이 미션으로 다음", 387, 1064, 428, enabled = enabled, arrow = true,
        arrowId = R.drawable.predrive_ab9ab, arrowSize = 46.457) {
        s.mission = true; s.open(WireScreen.MUSIC_PICK)
    }
    PText("점수는 주행 데이터로 계산해요. 주행 중에는 순위만 보여요.", 1120, 1074,
        PType.Caption, PreDriveLook.hint, 548)
    PText("마지막 차가 도착하면 순위가 확정돼요.", 1120, 1108,
        PType.Caption, PreDriveLook.hint, 548)
}

@Composable
private fun PMusicGlow() {
    val bitmap = ImageBitmap.imageResource(R.drawable.predrive_881a3).asAndroidBitmap()
    Canvas(Modifier.pBounds(-462.93, 786, 1177.057, 288.826)) {
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            native.save()
            native.translate(size.width / 2, size.height / 2)
            native.rotate(10.9f)
            native.scale(1f, .53f)
            native.skew(kotlin.math.tan(Math.toRadians(-57.77)).toFloat(), 0f)
            native.drawBitmap(bitmap, null,
                android.graphics.RectF((-539.5).dp.toPx(), (-268.5).dp.toPx(),
                    539.5.dp.toPx(), 268.5.dp.toPx()), android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))
            native.restore()
        }
    }
    PImage(R.drawable.predrive_3389b, 320, 799, 1155, 259)
}

@Composable
private fun FMusic(s: WireSession, enabled: Boolean) {
    val orbit = rememberLinkCarMusicOrbit()
    val members = listOf("나") + s.orderedInvitees.take(3)
    val dj = if ("문준" in members) "문준" else members.last()
    val companions = members.filterNot { it == dj }.take(2)
    fun largePortrait(name: String) = when (name) {
        "나" -> R.drawable.predrive_e8286
        "다연" -> R.drawable.predrive_fada2
        "문준" -> R.drawable.predrive_ff1b9
        else -> participant(name).image
    }
    PHeader("출발 준비", "${s.destination} 가는 길 · ${s.invitees.size + 1}대", s, enabled, 2)
    PPanel(32, 144, 795, 894) {
        PText("첫 곡 DJ", 48, 36, PType.Eyebrow, PreDriveLook.muted)
        PImage(R.drawable.predrive_32f6a, 159, 126, 491, 485, modifier = Modifier.musicRing(orbit, .045f))
        PImage(R.drawable.predrive_a2802, 202, 176, 405, 385, modifier = Modifier.musicRing(orbit, .035f))
        PImage(R.drawable.predrive_65620, 250, 214, 309, 309, modifier = Modifier.musicRing(orbit, -.025f))
        companions.forEachIndexed { i, name ->
            val x = if (i == 0) 169 else 518
            PImage(largePortrait(name), x, 372, 120, 120,
                modifier = Modifier.musicCompanionOrbit(orbit, x + 60f, 432f))
        }
        Box(Modifier.pBounds(305, 232, 199, 263).musicDjFloat(orbit)) {
            PImage(largePortrait(dj), 0, 44, 199, 199)
            PChip("DJ", 60, 223, 83, 40, Color(0xFFDDD5FF), PreDriveLook.blue, PType.Small)
            PImage(R.drawable.predrive_37c25, 82, 0, 38, 38)
        }
        PText("${dj}님이 첫 곡을 골랐어요", 167, 647, PType.H1)
        PText("무작위로 뽑힌 차가 첫 곡을 신청해요", 215, 715, PType.Sub, PreDriveLook.muted)
    }
    PPanel(851, 144, 817, 894) {
        PImage(R.drawable.predrive_bb8db, 395.0195, 113.2195, 296, 296,
            modifier = Modifier.graphicsLayer { rotationZ = 74.47f }, scale = ContentScale.Crop)
        PText("첫 곡", 48, 36, PType.Eyebrow, PreDriveLook.muted)
        PText("밤편지", 48, 492, PType.H1)
        PText("아이유", 48, 571, PType.Body, PreDriveLook.muted)
        PPanel(48, 647, 721, 12, 6.641, PreDriveLook.secondary)
        PText("함께 듣는 차", 48, 746, PType.Sub, PreDriveLook.muted)
        members.forEachIndexed { i, name ->
            val image = when (name) { "나" -> R.drawable.predrive_5c930
                "다연" -> R.drawable.predrive_e9fc7; "문준" -> R.drawable.predrive_0128b
                else -> participant(name).image }
            PImage(image, 210 + i * 61, 727, 80, 80)
        }
        PText("${members.size}대", 445 + (members.size - 3).coerceAtLeast(0) * 61, 743, PType.TitleSmall)
        PPanel(161, 574, 2, 33, 0, Color(0xFFA6ABBD))
        PMusicGlow()
        PImage(R.drawable.predrive_489ee, 184, 82, 359, 359,
            modifier = Modifier.shadow(10.dp, RoundedCornerShape(26.563.dp))
                .clip(RoundedCornerShape(26.563.dp)), scale = ContentScale.Crop)
        PPanel(201, 574, 167, 37, 4, Color(0xFFE8E9EF)) {
            PImage(R.drawable.predrive_a9f3f, 4, 6, 44, 25, scale = ContentScale.Crop)
            PText("$dj 차 신청", 54, 4.5, PType.Small, PreDriveLook.muted)
        }
    }
    PButton("음악 없이 출발", 32, 1064, 339, enabled = enabled, tone = PButtonTone.Secondary) {
        s.startLocalTrip(false)
    }
    PButton("출발하기", 387, 1064, 428, enabled = enabled, arrow = true,
        arrowId = R.drawable.predrive_4fe44, arrowSize = 40) { s.startLocalTrip(true) }
    PText("출발하면 첫 곡을 함께 들어요", 1398, 1091, PType.Caption, PreDriveLook.hint)
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
