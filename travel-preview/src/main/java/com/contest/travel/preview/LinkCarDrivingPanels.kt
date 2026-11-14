package com.contest.travel.preview

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun DLine(id: Int, x: Number, y: Number, length: Number, thickness: Number = 2,
    vertical: Boolean = false) {
    if (!vertical) DImage(id, x, y, length, thickness)
    else {
        val delta = (length.toFloat() - thickness.toFloat()) / 2
        DImage(id, x.toFloat() - delta, y.toFloat() + delta, length, thickness,
            Modifier.graphicsLayer { rotationZ = 90f })
    }
}

@Composable
private fun DPin(x: Number, y: Number, portrait: Int, selected: Boolean = false,
    rotation: Float = 0f, frameKey: String = "imgFrame11") {
    if (selected) DImage(drivingAsset("imgFrame10", R.drawable.driving_c074e),
        x.toFloat() - 26, y.toFloat() - 23, 148, 172)
    else DImage(drivingAsset(frameKey, drivingAsset("imgFrame11", R.drawable.driving_96210)), x, y, 96, 120,
        Modifier.graphicsLayer { rotationZ = rotation })
    DImage(portrait, x.toFloat() + 9, y.toFloat() + 9, 78, 78)
}

@Composable
internal fun DRoute(s: WireSession, screen: WireScreen, height: Float, enabled: Boolean,
    voice: LinkCarVoiceRuntime, onResetDemo: () -> Unit) {
    val expanded = screen == WireScreen.MAP
    val preview = s.designPreviewNode != null
    val realCar = LocalConvoy.current.useRealCar && !preview
    val live = LocalTripLive.current
    val ownKnown = !realCar || (live.location.status == ReadStatus.READY && live.location.value?.isValid() == true)
    // The team's MapLibre bridge needs no NAVER key. Authored snapshots keep the Figma map.
    val interactiveMap = expanded && !preview
    val routeModifier = Modifier.semantics { contentDescription = if (expanded) "지도 접기" else "지도 펼치기" }
        .linkCarPress(enabled && screen in setOf(WireScreen.DRIVE, WireScreen.MAP, WireScreen.DJ)) {
            if (expanded) s.drive() else s.open(WireScreen.MAP)
        }
    DPanel(24, 106, 1652, height, 48, color = androidx.compose.ui.graphics.Color.White,
        modifier = if (interactiveMap) Modifier else routeModifier) {
        if (interactiveMap) {
            RoadConvoyMap(true, 0, enabled, Modifier.fillMaxSize(), voice.notes.hasDemoMarker)
            DChip("접기", 20, 20, 120, onClick = { s.drive() })
        } else {
            val opening = ((height - 165f) / 395f).coerceIn(0f, 1f)
            val artHeight = 1101.325f + (1101.333f - 1101.325f) * opening
            DImage(drivingAsset("imgCompanionVehicleCardB", R.drawable.driving_e3505), 0,
                -720.192f * (1 - opening) - 270.667f * opening, 1652, artHeight)
            Crossfade(targetState = expanded, animationSpec = tween(240), label = "mapDetail") { large ->
                if (large) {
                    if (s.waypoint) DWaypointRoutes()
                    else DImage(drivingAsset("imgRoutes", R.drawable.driving_98eaf), 0, 0, 1652, 560)
                    DPin(583, 84, drivingAsset("imgProfilePhoto", R.drawable.driving_07296), true)
                    DPin(1074, 394, drivingCompanionImage(s, "문준 차", if (s.waypoint) "imgProfilePhoto2" else "imgProfilePhoto1", R.drawable.driving_3471d))
                    DImage(drivingAsset("imgFrame11", R.drawable.driving_96210), 1541, 384, 96, 120,
                        Modifier.graphicsLayer { rotationZ = -90f })
                    DImage(drivingCompanionImage(s, "다연 차", if (s.waypoint) "imgProfilePhoto1" else "imgProfilePhoto2", R.drawable.driving_2e6a4), 1553, 405, 78, 78)
                    DChip("내 차 · 남은 40분", 553, 211, 166, color = DrivingFigma.Black, ink = Color.White)
                    DChip("문준 차 · 남은 25분", 1037, 517, 181, color = Color.White, ink = DrivingFigma.Title)
                    DChip("다연 차 · 남은 32분", 1349, 425, 181, color = Color.White, ink = DrivingFigma.Title)
                    Row(Modifier.offset(40.dp, 32.dp).clip(RoundedCornerShape(100.dp))
                        .background(Color.White).padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        val labels = listOf("내 차", "다연 차", "문준 차")
                        listOf("imgIcon1", "imgIcon2", "imgIcon3").forEachIndexed { i, key ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(drivingAsset(key, R.drawable.driving_e9eb3)),
                                    null, Modifier.size(36.dp, 12.dp))
                                androidx.compose.material3.Text(labels[i], style = DType.Small.style(DrivingFigma.Title))
                            }
                        }
                    }
                    if (!s.waypoint) DPanel(1434, 96, 72, 72, 36, DrivingFigma.Black) {
                        DImage(drivingAsset("imgIcon", R.drawable.driving_f4448), 19, 19, 34, 34)
                    }
                    if (!s.waypoint) DChip("목적지 · 가평 계곡", 1383.5, 180, 188, 40,
                        color = Color.White, ink = DrivingFigma.Title)
                } else {
                    val drift = if (preview) 0f else s.progress * 300
                    if (ownKnown) DPin(432 + drift, 19, drivingAsset("imgProfilePhoto", R.drawable.driving_07296), true)
                    else DChip("내 차 위치 확인 중", 360, 38, 240, color = Color.White)
                    DPin(754 + drift, 19, drivingCompanionImage(s, "문준 차", "imgProfilePhoto1", R.drawable.driving_3471d))
                    DPin(1076 + drift, 19, drivingCompanionImage(s, "다연 차", "imgProfilePhoto2", R.drawable.driving_af426), frameKey = "imgFrame12")
                    DPanel(52, 139, 1548, 14, 8, Color(0xFFADAEB6))
                    DPanel(52, 139, 428 + drift, 14, 0, DrivingFigma.Blue)
                    DPanel(480 + drift, 139, 322, 14, 0, DrivingFigma.Mid)
                    DPanel(802 + drift, 139, 322, 14, 0, DrivingFigma.Pale)
                    if (screen == WireScreen.VOICE_DELIVER) {
                        DImage(R.drawable.driving_b4989, 549.951, 82.995, 45.1, 56.001)
                        DImage(R.drawable.driving_c7c80, 561, 94, 24, 24)
                    } else if (screen == WireScreen.VOICE_APPROACH || (voice.notes.hasDemoMarker && !preview)) {
                        DPanel(1180, 56, 64, 64, 32, Color.White,
                            Modifier.border(3.dp, DrivingFigma.Blue, RoundedCornerShape(32.dp))) {
                            DImage(R.drawable.driving_f6c5c, 17, 17, 30, 30)
                        }
                        Row(Modifier.offset(1254.dp, 66.dp).clip(RoundedCornerShape(100.dp))
                            .background(Color.White).padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(R.drawable.driving_c9e24), null, Modifier.size(22.dp))
                            androidx.compose.material3.Text("편의점 · 다연 차가 남김 · 1.2km 앞", style = DType.Meta.style(DrivingFigma.Title))
                        }
                        Row(Modifier.offset(1432.dp, 14.dp).clip(RoundedCornerShape(100.dp)).background(DrivingFigma.Black)
                            .linkCarPress(enabled) { s.open(WireScreen.VOICE_DELIVER) }.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(R.drawable.driving_6927f), null, Modifier.size(20.dp))
                            androidx.compose.material3.Text("길에 남긴 음성 1개", style = DType.Small.style(Color.White))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DCompanions(s: WireSession, screen: WireScreen, feed: DFeed, y: Float, height: Float,
    compact: Boolean, enabled: Boolean) {
    DPanel(1062, y, 614, height) {
        if (compact) DCompactCars(s, screen, height, enabled)
        else {
            if (feed == DFeed.ROAD_DELIVER) DPanel(16, 84, 582, 258, 40, Color(0xFFEEF0FF),
                Modifier.border(2.dp, DrivingFigma.Mid, RoundedCornerShape(40.dp)))
            DText("연결된 차량", 48, 34, DType.Eyebrow, DrivingFigma.Muted)
            DText("(2)", 163, 34, DType.Eyebrow, DrivingFigma.Muted)
            DCrop(R.drawable.driving_8c1ee, 524, 21, 56, 56,
                -35, -14, 126, 84, alpha = .2f)
            DLine(drivingAsset("imgVector3", R.drawable.driving_b3144), 24, 358, 566)
            listOf("다연 차", "문준 차").forEachIndexed { index, name ->
                val motion = rememberCompanionButtonMotion(s, name)
                val offset = index * 292
                val target = when (feed) { DFeed.CALL -> s.incomingCaller; DFeed.INCOMING -> s.incomingCaller; else -> s.knockTarget }
                val active = name == target && feed in setOf(DFeed.SENT, DFeed.INCOMING, DFeed.CALL, DFeed.RECEIVED)
                val running = index == 0 || (feed == DFeed.SENT && name == target)
                val ring = if (active && feed == DFeed.SENT) DrivingFigma.Mid
                    else if (active) DrivingFigma.Mid else DrivingFigma.Input
                val photo = if (index == 0) drivingCompanionImage(s, name, "imgProfilePhotoCompanion1", R.drawable.driving_03d8c)
                    else drivingCompanionImage(s, name, "imgImage46", drivingAsset("imgImage45", R.drawable.driving_6cab5))
                DImage(photo, 40, 103 + offset, 90, 90,
                    Modifier.clip(RoundedCornerShape(1000.dp)), scale = ContentScale.Crop)
                DImage(drivingAsset(if (index == 0) "imgEllipse5" else "imgEllipse6",
                    if (running) R.drawable.driving_f426a else R.drawable.driving_b4198),
                    102, 155 + offset, 38, 38)
                DText(name, 149, 118 + offset, DType.Name)
                DText(if (feed == DFeed.ROAD_DELIVER && index == 0) "음성 남김 · 12분 전"
                    else if (running) "주행중" else "정차중", 149, 162 + offset, DType.Label,
                    if (feed == DFeed.ROAD_DELIVER && index == 0) DrivingFigma.Blue
                    else if (running) DrivingFigma.Green else DrivingFigma.Red)
                val distance = if (s.designPreviewNode != null) if (index == 0) "30" else "28"
                    else remainingRouteKilometers(if (index == 0) LocalConvoy.current.dayeonRouteMeters else LocalConvoy.current.munjunRouteMeters)
                DDrivingScore(s, name, index, 55, 213 + offset, enabled = enabled)
                DText(distance, 177, 213 + offset, DType.Score, Color.Black, 90, TextAlign.Center)
                DText("km", 259, 244 + offset, DType.Unit, Color.Black.copy(alpha = .4f))
                DText("주행 점수", 55, 285 + offset, DType.Tiny, DrivingFigma.Hint, 90, TextAlign.Center)
                DText("목적지까지", 177, 285 + offset, DType.Tiny, DrivingFigma.Hint, 90, TextAlign.Center)
                DLine(drivingAsset(if (index == 0) "imgLine5" else "imgLine6", R.drawable.driving_489dd),
                    161, 222 + offset, if (index == 0) 53 else 47, vertical = true)
                val buttonColor = if (active && feed != DFeed.SENT) DrivingFigma.Blue
                    else if (active) Color.White else if (running) DrivingFigma.Black else DrivingFigma.Input
                DPanel(340, 93 + offset, 240, 240, 132, Color.Transparent,
                    motion.buttonModifier.border(if (active && feed != DFeed.SENT) 6.dp else 3.dp, ring, RoundedCornerShape(132.dp))
                        .semantics { contentDescription = "$name ${if (running) "노크" else "정차 중"}" }
                        .then(if (!active) Modifier.linkCarPress(enabled && !motion.busy) {
                            motion.tap(enabled, running)
                        } else Modifier)) {
                    DPanel(10, 10, 220, 220, 132, buttonColor,
                        if (active && feed == DFeed.SENT) Modifier.border(6.dp, DrivingFigma.Blue, RoundedCornerShape(132.dp)) else Modifier)
                    when {
                        active && feed == DFeed.CALL -> {
                            DText(s.seconds.toString(), 0, 48, DType.CallBadge, Color.White, 240, TextAlign.Center)
                            DText("대화 중", 0, 135, DType.Eyebrow, Color.White, 240, TextAlign.Center)
                        }
                        active && feed == DFeed.SENT -> {
                            DWave(89, 64, color = DrivingFigma.Blue, bars = listOf(10, 20, 32, 20, 14))
                            DText("응답\n기다리는 중", 20, 116, DType.Eyebrow, DrivingFigma.Blue, 200, TextAlign.Center, 2)
                        }
                        active && feed == DFeed.RECEIVED -> {
                            DImage(drivingAsset("imgIcon", R.drawable.driving_88021), 94, 58, 52, 52)
                            DText("새 음성\n메시지", 20, 122, DType.Eyebrow, Color.White, 200, TextAlign.Center, 2)
                        }
                        active && feed == DFeed.INCOMING -> {
                            DImage(R.drawable.driving_9304f, 90, 62, 60, 62, scale = ContentScale.Crop)
                            DText("노크 왔어요", 20, 138, DType.Eyebrow, Color.White, 200, TextAlign.Center)
                        }
                        feed == DFeed.UNAVAILABLE && name == s.knockTarget -> {
                            DImage(drivingAsset("imgIcon", R.drawable.driving_affb4), 98, 56, 44, 44)
                            DText("1분 뒤\n다시 노크", 20, 118, DType.Eyebrow, DrivingFigma.Disabled, 200, TextAlign.Center, 2)
                        }
                        // Use the transparent originals; node exports bake the page backdrop into these fills.
                        running -> DCrop(R.drawable.driving_c4443, 79, 78, 81, 84,
                            -4.76, 13.22, 90.49, 63.05, modifier = motion.iconModifier)
                        else -> DCrop(R.drawable.driving_e3d50, 84, 83, 71, 74,
                            -19.887, -.032, 110.776, 74.001, alpha = .5f, modifier = motion.iconModifier)
                    }
                }
                DLockedHint(motion, name, 24, 202 + offset)
            }
        }
    }
}

/** S-11/2's authored route segments, including the selected convenience-store waypoint. */
@Composable
private fun DWaypointRoutes() {
    Box(Modifier.dBounds(0, 0, 1652, 560).clip(RoundedCornerShape(0.dp))) {
        DPercentPath(R.drawable.driving_8d6db, 29.02f, 4.63f, 15.45f, 4.9f, -1.29f, -.4f, -1.65f, -.27f)
        DPercentPath(R.drawable.driving_c8cd1, 35.8f, 61.77f, 15.45f, 4.9f, -1.47f, -.73f, -1.88f, -.73f)
        DPercentPath(R.drawable.driving_4f72e, 29.11f, 4.72f, .18f, 48f, -.76f, -.57f, -.76f, -.38f)
        DPercentPath(R.drawable.driving_5cb83, 80.18f, 32.63f, .18f, 48f, -3.47f, -.94f, -2.73f, -.94f)
        DPercentPath(R.drawable.driving_a0ac4, 52.32f, 13.32f, 37.68f, 83.96f)
        Row(Modifier.offset((1652 * .796f).dp, (560 * .4375f).dp).clip(RoundedCornerShape(100.dp))
            .background(DrivingFigma.Black).padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(R.drawable.driving_362ef), null, Modifier.size(22.dp))
            androidx.compose.material3.Text("편의점 · 300m", style = DType.Meta.style(Color.White))
        }
        DPercentPath(R.drawable.driving_11694, 29.11f, 4.72f, -.63f, 32.99f, -1f, -.28f, -1f, -.39f)
    }
}

@Composable
private fun DPercentPath(id: Int, t: Float, r: Float, b: Float, l: Float,
    innerT: Float = 0f, innerR: Float = 0f, innerB: Float = 0f, innerL: Float = 0f) {
    val w = 1652f * (100 - l - r) / 100
    val h = 560f * (100 - t - b) / 100
    DImage(id, 1652f * l / 100 + w * innerL / 100, 560f * t / 100 + h * innerT / 100,
        w * (100 - innerL - innerR) / 100, h * (100 - innerT - innerB) / 100)
}

@Composable
private fun DCompactCars(s: WireSession, screen: WireScreen, height: Float, enabled: Boolean) {
    val extra by animateFloatAsState(if (screen == WireScreen.DJ) 10.5f else 0f, tween(340), label = "compactCarInset")
    val step by animateFloatAsState(if (screen == WireScreen.DJ) 140f else 119f, tween(340), label = "compactCarSpacing")
    listOf("다연 차", "문준 차").forEachIndexed { i, name ->
        val motion = rememberCompanionButtonMotion(s, name)
        val y = 15.5f + i * step + extra
        DImage(drivingCompanionImage(s, name, if (i == 0) "imgAvatar1" else "imgAvatar2",
            if (i == 0) R.drawable.driving_52fa2 else R.drawable.driving_08ff1), 28, y + 18, 76, 76)
        DImage(if (i == 0) R.drawable.driving_c5a99 else R.drawable.driving_90922, 84, y + 70, 24, 24)
        DText(name, 120, y + 20, DType.CompactName)
        DText(if (i == 0) "주행중" else "정차중", 120, y + 60, DType.Small,
            if (i == 0) DrivingFigma.Green else DrivingFigma.Red)
        DDrivingScore(s, name, i, 250, y + 20, compact = true, enabled = enabled)
        DText("주행 점수", 250, y + 72, DType.Micro, DrivingFigma.Hint)
        DPanel(322, y + 28, 1, 44, 0, Color(0xFFD9DADF))
        DText(if (i == 0) "30" else "28", 338, y + 20, DType.CompactScore, Color.Black)
        DText("km", 392, y + 48, DType.CompactUnit, Color.Black)
        DText("목적지까지", 338, y + 72, DType.Micro, DrivingFigma.Hint)
        DPanel(478, y, 112, 112, 100, Color.Transparent,
            motion.buttonModifier.border(3.dp, Color(0xFFE1E1E1), RoundedCornerShape(100.dp))
                .linkCarPress(enabled && !motion.busy) { motion.tap(enabled, i == 0) }) {
            DPanel(6, 6, 100, 100, 100, if (i == 0) DrivingFigma.Black else DrivingFigma.Input)
            if (i == 0) DImage(drivingAsset("imgIconKnock", R.drawable.driving_9304f), 30, 30, 52, 54,
                motion.iconModifier, scale = ContentScale.Crop)
            else DImage(drivingAsset("imgIconLock", R.drawable.driving_8e0b4), 34, 34, 44, 44, motion.iconModifier)
        }
        DLockedHint(motion, name, 120, y + 2, compact = true)
        if (i == 0) DPanel(24, if (screen == WireScreen.DJ) 152.5 else 131, 566, 2, 0, Color(0xFFEBEBEB))
    }
}

@Composable
private fun DMusicGlow(expanded: Boolean) {
    val bitmap = ImageBitmap.imageResource(drivingAsset("imgVector2", R.drawable.driving_1f021)).asAndroidBitmap()
    val h = if (expanded) 347.033f else 286.061f
    Canvas(Modifier.dBounds(if (expanded) -124.3 else -125.26, if (expanded) 416.84 else 96,
        if (expanded) 1267.9 else 1177.339, h)) {
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            native.save(); native.translate(size.width / 2, size.height / 2)
            native.rotate(if (expanded) 13.71f else 10.61f); native.scale(1f, .53f)
            native.skew(kotlin.math.tan(Math.toRadians(-57.77)).toFloat(), 0f)
            val halfHeight = if (expanded) 318.397 else 268.663
            native.drawBitmap(bitmap, null, android.graphics.RectF((-539.383).dp.toPx(),
                (-halfHeight).dp.toPx(), 539.383.dp.toPx(), halfHeight.dp.toPx()),
                android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))
            native.restore()
        }
    }
    DImage(drivingAsset("imgEllipse4", R.drawable.driving_3389b), 725, if (expanded) 440 else 102, 1155, 259)
}

@Composable
private fun DRequest(text: String, x: Number, y: Number, width: Number = 149, type: DType = DType.Small) {
    DPanel(x, y, width, 33, 4, DrivingFigma.Card) {
        DImage(drivingAsset("imgImage41", R.drawable.driving_a9f3f), 4, 4, 44, 25, scale = ContentScale.Crop)
        DText(text, 54, if (type == DType.Tiny) 7 else 6, type, Color(0xFF9095A9))
    }
}

@Composable
internal fun DMusic(s: WireSession, height: Float, expanded: Boolean, enabled: Boolean) {
    val preview = s.designPreviewNode != null
    DPanel(0, 1184 - height, 1700, height, 0, modifier = Modifier.linkCarPress(enabled) {
        if (expanded) s.drive() else s.open(WireScreen.DJ)
    }) {
        DMusicGlow(expanded)
        DPanel(800, 8, 100, 6, 6, Color(0xFFE8E8E8))
        Crossfade(targetState = expanded, animationSpec = tween(230), label = "musicDetail") { large ->
            Box(Modifier.requiredSize(1700.dp, (if (large) 560 else 200).dp)) {
                if (large) CompositionLocalProvider(LocalDrivingAssets provides drivingFigmaAssets.getValue("443:22856")) {
                    DMusicExpanded(s, enabled)
                }
                else {
                    val art = Modifier.linkCarPress(enabled) { s.open(WireScreen.DJ) }
                    DImage(drivingAsset("imgRectangle346244413", R.drawable.driving_84cf2), 56, 20, 156, 156,
                        art.clip(RoundedCornerShape(24.dp)), scale = ContentScale.Crop)
                    DText(if (preview) "노래명 입력란" else if (s.music) linkCarSongs[s.song % linkCarSongs.size].first else "재생할 곡 없음",
                        252, 36, DType.Song, DrivingFigma.Title, 300)
                    DText(if (preview) "가수 명 입력란" else if (s.music) linkCarSongs[s.song % linkCarSongs.size].second else "음악 없이 출발했어요",
                        252, 84, DType.Label, DrivingFigma.Subtle)
                    if (s.music) DRequest("문준 차 신청", 252, 127)
                    DMusicControl(R.drawable.driving_91f78, "이전 곡", 556, 56, 85, enabled && s.music) { s.stepSong(-1) }
                    DMusicControl(if (s.playing) R.drawable.driving_4ff70 else R.drawable.driving_5c717,
                        if (s.playing) "일시정지" else "재생", 669, 56, 85, enabled && s.music,
                        background = if (!s.playing && s.music) DrivingFigma.Black else Color.Transparent) { s.toggleMusicPlayback() }
                    DMusicControl(R.drawable.driving_a89f1, "다음 곡", 782, 56, 85, enabled && s.music) { s.stepSong(1) }
                    DLine(drivingAsset("imgLine4", R.drawable.driving_5a46c), 1053, 40, 122, vertical = true)
                    DText(if (s.music) "다음 곡 3" else "다음 곡 0", 1093, 36, DType.Label, Color(0xFFC3C5CC))
                    DText(if (s.music) "노래명 입력란" else "신청한 곡 없음", 1093, 68, DType.Song, Color(0xFFA8ACB4))
                    if (s.music) DRequest("다연 차 신청", 1093, 127)
                    if (s.music) listOf("imgEllipse11", "imgEllipse10", "imgEllipse9").forEachIndexed { i, key ->
                        val x = 1496 - i * 85
                        DImage(drivingAsset(key, R.drawable.driving_e5fae), x, 34, 125, 125)
                        DImage(drivingAsset("imgEllipse12", R.drawable.driving_b6d1d), x + 44, 78, 37, 37)
                    }
                }
            }
        }
        DPanel(0, height - 4, 1700, 4, 0, Color(0xFF3A3F42).copy(alpha = .4f))
        DPanel(0, height - 4, 750, 4, 0, Color.White)
    }
}

@Composable
private fun DMusicControl(id: Int, label: String, x: Number, y: Number, size: Number,
    enabled: Boolean, background: Color = Color.Transparent, outlined: Boolean = false, onClick: () -> Unit) {
    DPanel(x, y, size, size, 100, background,
        Modifier.semantics { contentDescription = label }.graphicsLayer { alpha = if (enabled) 1f else .35f }
            .then(if (outlined) Modifier.border(2.dp, Color(0xFFD5D7DE), RoundedCornerShape(100.dp)) else Modifier)
            .linkCarPress(enabled, onClick = onClick)) {
        val (w, h) = drivingSvgSizes[id] ?: (28f to 28f)
        DImage(id, (size.toFloat() - w) / 2, (size.toFloat() - h) / 2, w, h)
    }
}

@Composable
private fun DMusicExpanded(s: WireSession, enabled: Boolean) {
    DImage(drivingAsset("imgAlbumArt", R.drawable.driving_84cf2), 56, 56, 448, 448,
        Modifier.clip(RoundedCornerShape(32.dp)), scale = ContentScale.Crop)
    DText("함께 듣는 DJ", 560, 64, DType.SubLabel, DrivingFigma.Muted)
    DText(if (s.designPreviewNode != null) "노래명 입력란" else if (s.music) linkCarSongs[s.song % linkCarSongs.size].first else "재생할 곡 없음",
        560, 106, DType.Strong, DrivingFigma.Title, 550)
    DText(if (s.designPreviewNode != null) "가수 명 입력란" else if (s.music) linkCarSongs[s.song % linkCarSongs.size].second else "음악 없이 출발했어요",
        560, 182, DType.Description, DrivingFigma.Subtle)
    if (s.music) DRequest("문준 차 신청", 560, 232)
    DSeek(s, enabled && s.music)
    DMusicControl(drivingAsset("imgIcon", R.drawable.driving_d73dc), "이전 곡", 660, 392, 96,
        enabled && s.music, outlined = true) { s.stepSong(-1) }
    DMusicControl(if (s.playing) drivingAsset("imgIcon1", R.drawable.driving_62414) else R.drawable.driving_5c717,
        "일시정지 또는 재생", 792, 392, 96, enabled && s.music, background = DrivingFigma.Black) { s.toggleMusicPlayback() }
    DMusicControl(drivingAsset("imgIcon2", R.drawable.driving_f3f01), "다음 곡", 924, 392, 96,
        enabled && s.music, outlined = true) { s.stepSong(1) }
    DPanel(1180, 48, 464, 464, 40, Color.White.copy(alpha = .75f)) {
        DText(if (s.music) "다음 곡 3" else "다음 곡 0", 32, 28, DType.Caption, DrivingFigma.Muted)
        if (s.music) listOf("imgCover", "imgCover1", "imgCover2").forEachIndexed { i, key ->
            val top = 72 + i * 114
            DImage(drivingAsset(key, R.drawable.driving_7eac3), 32, top, 96, 96)
            DImage(drivingAsset("imgEllipse", R.drawable.driving_be907), 67, top + 35, 26, 26)
            DText("노래명 입력란", 146, top + 9, DType.Place, if (i == 0) DrivingFigma.Title else Color(0xFFA8ACB4))
            DRequest(listOf("다연 차 신청", "문준 차 신청", "내 차 신청")[i], 146, top + 48, 139, DType.Tiny)
        }
        DText("신청은 '모두에게 말하기'로 해요", 32, 414, DType.Small, DrivingFigma.Muted)
    }
}

@Composable
private fun DSeek(s: WireSession, enabled: Boolean) {
    val density = LocalDensity.current.density
    val value = if (s.music) s.songSeek else 0f
    DPanel(560, 330, 560, 10, 6, Color(0xFFE4E6EC))
    DPanel(560, 330, 560 * value, 10, 6, DrivingFigma.Black)
    DImage(drivingAsset("imgSeekKnob", R.drawable.driving_0620f), 547 + 560 * value, 322, 26, 26)
    val seconds = if (s.designPreviewNode != null) 102 else (238 * value).toInt()
    DText("${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}", 560, 354, DType.SongTime, DrivingFigma.Hint)
    DText("3:58", 1081, 354, DType.SongTime, DrivingFigma.Hint)
    Box(Modifier.dBounds(548, 306, 584, 58).semantics {
        contentDescription = "음악 재생 위치"
        progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
        setProgress { v -> if (enabled) { s.designPreviewNode = null; s.songSeek = v.coerceIn(0f, 1f); true } else false }
    }.pointerInput(enabled) {
        detectTapGestures { p -> if (enabled) { s.designPreviewNode = null; s.songSeek = ((p.x / density - 12) / 560).coerceIn(0f, 1f) } }
    }.pointerInput(enabled) {
        detectDragGestures { change, delta -> if (enabled) { change.consume(); s.designPreviewNode = null
            s.songSeek = (s.songSeek + delta.x / density / 560).coerceIn(0f, 1f) } }
    })
}
