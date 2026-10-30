package com.contest.travel.preview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun TogetherHome(wide: Boolean, enabled: Boolean, go: (String) -> Unit) {
    PageTitle("따로 달려도, 같은 여행", "강릉, 바다 보러 · 세 가족이 함께하는 길")
    ConvoyJourneyPanel(wide, enabled)
    AppPanel {
        SectionTitle("우리 차에서 읽은 데이터")
        VehicleMetrics(wide)
        NavigationAction("차량·내비 연동 보기", enabled) { go("connection") }
    }
    TwoColumns(wide,
        first = {
            AppPanel {
                SectionTitle("함께 쉬어가기")
                CurrentDrivingTimeText()
                NavigationAction("휴식 장소 보기", enabled) { go("rest") }
            }
        },
        second = {
            AppPanel {
                SectionTitle("우리 차 내비게이션")
                NavigationSummary()
                RequestDestinationButton(enabled)
            }
        }
    )
}

@Composable
internal fun TogetherCrew(wide: Boolean, enabled: Boolean, go: (String) -> Unit) {
    PageTitle("함께 달리는 일행", "어디쯤 달리고 있는지, 한 화면에서 함께")
    ConvoyJourneyPanel(wide, enabled)
    NavigationAction("일행 초대 화면", enabled) { go("invite") }
}

@Composable
internal fun SdkConnectionScreen(wide: Boolean, enabled: Boolean) {
    val live = LocalTripLive.current
    val actions = LocalTripActions.current
    PageTitle("차량·내비 연동", "이 화면에는 SDK가 실제로 반환한 값만 표시합니다")
    AppPanel {
        SectionTitle("Vehicle SDK 2.3.2")
        MutedText(live.vehicleStatus)
        VehicleMetrics(wide)
    }
    AppPanel {
        SectionTitle("NaviHelper SDK 3.0.5")
        MutedText(live.naviStatus)
        ReadingLine("우리 차 현재 위치", live.location, ::formatPoint)
        ReadingLine("현재 목적지", live.destination) { it.name.ifBlank { "이름 없는 목적지" } }
        ReadingLine("내비 경유지", live.waypoints) { it.joinToString(" · ") }
        if (live.destination.status == ReadStatus.READY) {
            live.destination.value?.let { destination ->
                MutedText("남은 거리 ${formatMeters(destination.meters)} · 남은 시간 ${formatMinutes((destination.seconds + 59) / 60)}")
            }
        }
        RequestDestinationButton(enabled)
        NavigationAction("휴식 장소 경유지 요청", enabled && live.naviConnected && !live.routePending && live.destination.status == ReadStatus.READY) {
            actions.addRestStop()
        }
        MutedText("경로 요청은 이 에뮬레이터의 내비게이션에만 적용됩니다. 일행 차량으로 전송하지 않습니다.")
    }
    NavigationAction("SDK 다시 조회", enabled, filled = true) { actions.refresh() }
}

@Composable
internal fun VehicleMetrics(wide: Boolean) {
    val live = LocalTripLive.current
    if (wide) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ReadingLine("현재 트립 주행 시간", live.minutes, ::formatMinutes)
                ReadingLine("현재 트립 주행 거리", live.meters, ::formatMeters)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ReadingLine("배터리 충전율", live.battery) { "$it%" }
                ReadingLine("연료 잔량", live.fuel) { "$it%" }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ReadingLine("현재 트립 주행 시간", live.minutes, ::formatMinutes)
            ReadingLine("현재 트립 주행 거리", live.meters, ::formatMeters)
            ReadingLine("배터리 충전율", live.battery) { "$it%" }
            ReadingLine("연료 잔량", live.fuel) { "$it%" }
        }
    }
}

@Composable
internal fun <T> ReadingLine(label: String, reading: Reading<T>, format: (T) -> String) {
    Surface(color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val text = when (reading.status) {
                ReadStatus.READY -> reading.value?.let(format) ?: "값 없음"
                ReadStatus.LOADING -> "확인 중"
                ReadStatus.UNSUPPORTED -> "이 차량에서 미지원"
                ReadStatus.EMPTY -> "데이터 없음"
                ReadStatus.ERROR -> "조회 실패"
                ReadStatus.STOPPED -> "연결 종료"
            }
            Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                color = if (reading.status == ReadStatus.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            val stamp = reading.receivedAt?.let { SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(Date(it)) }
            Text(if (stamp != null) "SDK 수신 · $stamp" else reading.message, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun CurrentDrivingTimeText() {
    val reading = LocalTripLive.current.minutes
    val minutes = reading.value?.takeIf { reading.status == ReadStatus.READY }
    MutedText(when {
        minutes == null -> "주행 시간은 차량·내비 연동 화면에서 확인해 주세요."
        minutes >= 120 -> "현재 트립 주행 시간 ${formatMinutes(minutes)}. 잠깐 쉬어가는 건 어떨까요?"
        else -> "현재 트립 주행 시간 ${formatMinutes(minutes)}. 일행과 쉬어갈 곳을 살펴보세요."
    })
}

@Composable
private fun NavigationSummary() {
    val live = LocalTripLive.current
    val destination = live.destination.value?.takeIf { live.destination.status == ReadStatus.READY }
    if (destination != null) {
        MutedText(destination.name.ifBlank { "설정된 목적지" })
        Text("${formatMeters(destination.meters)} · ${formatMinutes((destination.seconds + 59) / 60)}",
            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    } else MutedText(live.destination.message)
}

@Composable
internal fun RequestDestinationButton(enabled: Boolean) {
    val live = LocalTripLive.current
    val actions = LocalTripActions.current
    NavigationAction(if (live.routePending) "내비 응답 기다리는 중" else "기본 내비로 강릉 경로 요청",
        enabled && live.naviConnected && !live.routePending, filled = true) { actions.requestDestination() }
    live.routeMessage?.let { MutedText(it) }
}

@Composable
internal fun RestWaypointAction(enabled: Boolean) {
    val live = LocalTripLive.current
    val actions = LocalTripActions.current
    NavigationAction("경유지로 추가", enabled && live.naviConnected && !live.routePending && live.destination.status == ReadStatus.READY) {
        actions.addRestStop()
    }
    live.routeMessage?.let { MutedText(it) }
}
