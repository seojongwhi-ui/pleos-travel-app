package com.contest.travel.preview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val journeyMapPages = setOf("home", "schedule", "crew", "talk", "album", "rest", "changes", "invite")

@Composable
internal fun InAppJourneyScreen(page: String, wide: Boolean, enabled: Boolean, go: (String) -> Unit, modifier: Modifier) {
    var overview by rememberSaveable { mutableStateOf(true) }
    var cameraRevision by rememberSaveable { mutableIntStateOf(0) }
    val convoy = LocalConvoy.current
    // Every two-minute update opens the shared overview, including from a car-centered view.
    LaunchedEffect(convoy.revision) { overview = true }
    if (wide) {
        Row(modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            JourneyMapArea(overview, cameraRevision, enabled, { overview = true; cameraRevision++ },
                { overview = false; cameraRevision++ }, Modifier.weight(1f).fillMaxHeight())
            JourneyFeaturePanel(page, enabled, go, Modifier.width(340.dp).fillMaxHeight())
        }
    } else {
        Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            JourneyMapArea(overview, cameraRevision, enabled, { overview = true; cameraRevision++ },
                { overview = false; cameraRevision++ }, Modifier.weight(1f).fillMaxWidth())
            JourneyFeaturePanel(page, enabled, go, Modifier.fillMaxWidth().height(310.dp))
        }
    }
}

@Composable
private fun JourneyMapArea(overview: Boolean, cameraRevision: Int, enabled: Boolean,
    showAll: () -> Unit, showOurCar: () -> Unit, modifier: Modifier) {
    val convoy = LocalConvoy.current
    val live = LocalTripLive.current
    Surface(modifier, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("강릉, 바다 보러", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("세 가족이 같은 목적지를 향해", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AppBadge("일행 시연", warm = true)
            }
            RoadConvoyMap(overview, cameraRevision, enabled, Modifier.weight(1f))
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(overview, showAll, enabled = enabled,
                    label = { Text("일행 전체", fontSize = 22.sp) }, modifier = Modifier.heightIn(min = 76.dp))
                FilterChip(!overview, showOurCar, enabled = enabled,
                    label = { Text("우리 차 중심", fontSize = 22.sp) }, modifier = Modifier.heightIn(min = 76.dp))
            }
            val locationMissing = convoy.useRealCar && (live.location.status != ReadStatus.READY || live.location.value?.isValid() != true)
            Text(when {
                locationMissing -> "우리 차 위치 확인 불가 · ${live.location.message}"
                convoy.useRealCar -> "우리 차: SDK 위치 · 일행 2대: 시연"
                else -> "차량 3대: 로컬 시연 · 공유 서버 없음"
            }, Modifier.padding(horizontal = 18.dp), fontSize = 18.sp,
                color = if (locationMissing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            Text("점선은 시연 이동선 · 실제 길 안내 아님", Modifier.padding(start = 18.dp, end = 18.dp, bottom = 14.dp),
                fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun JourneyFeaturePanel(page: String, enabled: Boolean, go: (String) -> Unit, modifier: Modifier) {
    val live = LocalTripLive.current
    val convoy = LocalConvoy.current
    val actions = LocalTripActions.current
    Surface(modifier, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
        key(page) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (page) {
                    "home" -> {
                        SectionTitle("함께 달리는 중")
                        MutedText("지도 옆에서 일행과 여행을 확인해요.")
                        NavigationAction("함께 쉬어가기", enabled) { go("rest") }
                        NavigationAction("차량·내비 연동 보기", enabled) { go("connection") }
                        SectionTitle("우리 차 · 실제 SDK")
                        ReadingLine("배터리 충전율", live.battery) { "$it%" }
                        ReadingLine("남은 거리·시간", live.destination) {
                            "${formatMeters(it.meters)} · ${formatMinutes((it.seconds + 59) / 60)}"
                        }
                        MutedText("남은 거리·시간은 기본 내비의 조회값입니다. 지도 점선과 별개입니다.")
                        SectionTitle("일행 시연 설정")
                        FilterChip(convoy.automatic, convoy.toggleAutomatic, enabled = enabled,
                            label = { Text("2분 자동", fontSize = 22.sp) }, modifier = Modifier.heightIn(min = 76.dp))
                        Text(if (convoy.automatic) "다음 갱신 ${convoy.secondsUntilUpdate}초" else "수동 갱신", fontSize = 21.sp)
                        NavigationAction("지금 갱신", enabled) { convoy.advance(); actions.refresh() }
                        Text("갱신 ${convoy.revision}회", fontSize = 20.sp)
                        FilterChip(convoy.useRealCar, convoy.toggleRealCar,
                            enabled = enabled && (convoy.useRealCar || live.location.status == ReadStatus.READY),
                            label = { Text("우리 차 내비 위치", fontSize = 22.sp) }, modifier = Modifier.heightIn(min = 76.dp))
                        TextButton(convoy.reset, enabled = enabled, modifier = Modifier.heightIn(min = 76.dp)) {
                            Text("시연 처음부터", fontSize = 22.sp)
                        }
                    }
                    "crew" -> {
                        SectionTitle("함께 달리는 일행")
                        convoyNames.forEachIndexed { index, name ->
                            MemberRow((index + 1).toString(), name, when {
                                index != 0 || !convoy.useRealCar -> "시연 차량 · 통신 연결 전"
                                live.location.status == ReadStatus.READY && live.location.value?.isValid() == true -> "SDK 현재 위치"
                                else -> "위치 확인 불가 · ${live.location.message}"
                            })
                        }
                        NavigationAction("일행 초대 화면", enabled) { go("invite") }
                        NavigationAction("지금 갱신", enabled) { convoy.advance(); actions.refresh() }
                        Text("갱신 ${convoy.revision}회", fontSize = 20.sp)
                        MutedText("실제 일행의 위치 공유와 통신은 아직 연결하지 않았어요.")
                    }
                    "schedule" -> {
                        SectionTitle("오늘의 여행")
                        AppBadge("예시 일정")
                        MemberRow("1", "서울에서 출발", "세 가족이 함께 출발해요")
                        MemberRow("2", "중간에 쉬어가기", "일행과 같은 휴게소에서 만나요")
                        MemberRow("3", "강릉 해변", "바다를 보며 여행을 시작해요")
                        NavigationAction("일정 변경 화면", enabled) { go("changes") }
                        MutedText("경로 계산·일정 자동 조정은 연결 전입니다.")
                    }
                    "rest" -> {
                        SectionTitle("함께 쉬어가기")
                        CurrentDrivingTimeText()
                        AppBadge("휴식 화면 예시")
                        MutedText("일행이 같은 휴게소에 모여 잠깐 쉬어갈 수 있도록 준비하고 있어요.")
                        FeatureAction("일행에게 휴식 제안")
                        MutedText("휴게소 검색과 일행 제안은 아직 연결하지 않았어요.")
                    }
                    "talk" -> {
                        SectionTitle("일행과 대화")
                        MutedText("떨어져 달려도, 같은 차에 탄 것처럼")
                        MemberRow("나", "우리 차", "차량 3대 · 연결 준비 중")
                        FeatureAction("눌러서 말하기")
                        MutedText("음성 송수신은 아직 연결하지 않았어요. 지도는 계속 확인할 수 있어요.")
                    }
                    "album" -> {
                        SectionTitle("함께한 순간")
                        MutedText("이동부터 도착까지, 여행을 하나의 추억으로")
                        FeatureAction("여행 앨범 만들기")
                        MutedText("사진·음성 기록과 공유는 아직 연결하지 않았어요.")
                    }
                    "changes" -> {
                        SectionTitle("일정을 함께 바꾸기")
                        MutedText("한 차가 늦어져도, 같은 여행을 이어가요.")
                        FeatureAction("일행에게 변경 제안")
                        MutedText("실시간 일정 조정은 아직 연결하지 않았어요.")
                    }
                    "invite" -> {
                        SectionTitle("함께 갈 일행 초대")
                        FeatureAction("초대 코드 만들기")
                        MutedText("서버와 실제 초대 기능은 아직 연결하지 않았어요.")
                    }
                }
            }
        }
    }
}
