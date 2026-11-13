package com.contest.travel.preview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

// Figma node 349:15776 is a 1700 x 1184 frame. Scale both axes equally to fit
// the Pleos app viewport; the frame's geometry must not stretch with the viewport.
@Composable
internal fun ColumnScope.FigmaHome(s: WireSession, enabled: Boolean) {
    val convoy = LocalConvoy.current
    BoxWithConstraints(Modifier.weight(1f)) {
        val originalDensity = LocalDensity.current
        val fit = min(maxWidth.value / 1700f, maxHeight.value / 1184f)
        CompositionLocalProvider(LocalDensity provides Density(
            originalDensity.density * fit, originalDensity.fontScale)) {
            Box(Modifier.size(1700.dp, 1184.dp).align(Alignment.Center)) {
                HomeAvatar(R.drawable.flow_imgprofilephoto, 32, 39, 76)
                HomeText("안녕하세요, 승호님", 128, 36, 32, 44, FontWeight.Bold)
                HomeText("내 차 · 아이오닉 5", 128, 79, 26, 36, FontWeight.Normal, HomeMuted)

                Box(Modifier.offset(32.dp, 144.dp).size(1016.dp, 1008.dp)
                    .clip(RoundedCornerShape(56.dp)).background(Color.White.copy(alpha = .8f))) {
                    Box(Modifier.offset(6.dp, 6.dp).size(1004.dp, 710.dp)
                        .clip(RoundedCornerShape(44.dp))) {
                        Image(painterResource(R.drawable.figma_349_15776_img1), null,
                            Modifier.size(1004.dp, 710.dp), contentScale = ContentScale.Crop)
                        Box(Modifier.size(1004.dp, 710.dp).background(Brush.verticalGradient(
                            0f to Color.Transparent,
                            .57814f to Color.Transparent,
                            .7384f to Color.White.copy(alpha = .73f),
                            .83089f to Color.White,
                            1f to Color.White)))
                    }
                    HomeText("새 여행 만들기", 48, 699, 44, 58, FontWeight.Bold)
                    HomeText("목적지를 정하고 함께 갈 차를 초대해요", 48, 769, 28, 40,
                        FontWeight.Normal, HomeMuted)
                    Box(Modifier.offset(48.dp, 872.dp).size(572.dp, 88.dp)
                        .clip(RoundedCornerShape(35.dp)).background(Color(0xFF191A1B))
                        .clickable(enabled) { convoy.reset(); s.open(WireScreen.CREATE) }) {
                        HomeText("여행 만들기", 48, 24, 32, 40, FontWeight.SemiBold, Color.White)
                        HomeText("→", 504, 18, 36, 48, FontWeight.Normal, Color.White)
                    }
                }

                Box(Modifier.offset(1072.dp, 144.dp).size(596.dp, 1008.dp)
                    .clip(RoundedCornerShape(56.dp)).background(Color.White.copy(alpha = .8f))) {
                    HomeText("지난 여행 다시 가기", 32, 37, 36, 48, FontWeight.Bold)
                    HomeRecent(125, if (s.saved) "${s.destination} 가는 길" else "가평 계곡 가는 길",
                        "친구 여행 · 10월 12일", true, enabled) { s.open(WireScreen.WAITING) }
                    HomeRecent(347, "할머니 댁", "가족 · 9월 17일", false, enabled) {
                        s.destination = "할머니 댁"
                        s.purpose = "가족"
                        s.open(WireScreen.WAITING)
                    }
                }
            }
        }
    }
}

private val HomeInk = Color(0xFF131417)
private val HomeMuted = Color(0xFF8F919F)

@Composable
private fun HomeText(value: String, x: Int, y: Int, size: Int, line: Int,
    weight: FontWeight, color: Color = HomeInk) {
    Text(value, Modifier.offset(x.dp, y.dp), color = color, style = TextStyle(
        fontFamily = FFont,
        fontSize = size.sp, lineHeight = line.sp, fontWeight = weight))
}

@Composable
private fun HomeAvatar(image: Int, x: Int, y: Int, diameter: Int) {
    Image(painterResource(image), null, Modifier.offset(x.dp, y.dp)
        .size(diameter.dp).clip(CircleShape), contentScale = ContentScale.Crop)
}

@Composable
private fun HomeRecent(y: Int, title: String, detail: String, first: Boolean,
    enabled: Boolean, onClick: () -> Unit) {
    Box(Modifier.offset(20.dp, y.dp).size(556.dp, 201.dp)
        .clip(RoundedCornerShape(40.dp)).background(Color(0xFFF3F4F8))
        .clickable(enabled, onClick = onClick)) {
        HomeText(title, 36, 16, 32, 44, FontWeight.SemiBold)
        HomeText(detail, 36, 57, 26, 36, FontWeight.Normal, HomeMuted)
        HomeAvatar(if (first) R.drawable.flow_imgprofilephoto2 else R.drawable.flow_imgprofilephoto,
            36, 115, 66)
        if (first) HomeAvatar(R.drawable.flow_imgprofilephoto1, 105, 115, 66)
        HomeText("›", 496, 75, 36, 48, FontWeight.Normal)
    }
}
