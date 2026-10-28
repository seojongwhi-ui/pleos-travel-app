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

// Core trip pages use SDK readings and a separate, explicitly labelled local convoy simulation.
@Composable
internal fun AppPage(page: String, wide: Boolean, canNavigate: Boolean, go: (String) -> Unit) {
    when (page) {
        "lobby" -> TravelLobby(wide, canNavigate, go)
        "create" -> CreateTrip(wide)
        "join" -> JoinTrip()
        "home" -> TripHome(wide, canNavigate, go)
        "schedule" -> TripSchedule(wide, canNavigate, go)
        "crew" -> TripCrew(wide, canNavigate, go)
        "talk" -> TripTalk(wide)
        "rest" -> TripRest(wide, canNavigate, go)
        "changes" -> TripChanges(canNavigate, go)
        "invite" -> TripInvite(canNavigate, go)
        "album" -> TripAlbum(wide)
        "connection" -> SdkConnectionScreen(wide, canNavigate)
    }
}

@Composable
private fun TravelLobby(wide: Boolean, enabled: Boolean, go: (String) -> Unit) {
    PageTitle("어디로 함께 떠날까요?", "여행을 만들거나 일행의 초대로 참여하세요.")
    TwoColumns(wide,
        first = {
            AppPanel {
                AppSymbol("calendar", Modifier.size(38.dp))
                SectionTitle("새로운 여행")
                MutedText("목적지를 정하고, 함께 갈 일행을 초대하세요.")
                NavigationAction("여행 만들기", enabled, filled = true) { go("create") }
            }
        },
        second = {
            AppPanel {
                AppSymbol("people", Modifier.size(38.dp))
                SectionTitle("일행과 합류하기")
                MutedText("전달받은 초대 코드로 같은 여행에 참여하세요.")
                NavigationAction("초대 코드로 참여", enabled) { go("join") }
            }
        }
    )
    SectionTitle("나의 여행")
    AppPanel {
        TwoColumns(wide,
            first = {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    AppBadge("예시 여행", warm = true)
                    Text("강릉, 바다 보러", fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    MutedText("10월 31일 · 1박 2일")
                    MemberRow("3", "세 가족이 함께", "우리 가족 · 민수네 · 지연네")
                    NavigationAction("예시 여행 열기", enabled, filled = true) { go("home") }
                }
            },
            second = { TripMap(270.dp) }
        )
    }
}

@Composable
private fun CreateTrip(wide: Boolean) {
    PageTitle("새 여행 만들기", "함께할 여행의 시작을 정해 주세요.")
    AppPanel {
        TwoColumns(wide,
            first = {
                Column(verticalArrangement = Arrangement.spacedBy(26.dp)) {
                    FormField("여행 이름", "어떤 여행을 떠나나요?")
                    FormField("목적지", "가고 싶은 곳을 찾아보세요", "home")
                }
            },
            second = {
                Column(verticalArrangement = Arrangement.spacedBy(26.dp)) {
                    FormField("출발일", "날짜 선택", "calendar")
                    FormField("출발지", "출발 장소 선택", "home")
                }
            }
        )
        MutedText("입력과 여행 생성은 화면 검토 후 연결할 예정입니다.")
        FeatureAction("여행 만들기")
    }
}

@Composable
private fun JoinTrip() {
    PageTitle("초대 코드로 참여", "일행에게 받은 여섯 자리 코드를 입력해 주세요.")
    AppPanel {
        SectionTitle("초대 코드")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(6) {
                Surface(color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.weight(1f)) {
                    Box(Modifier.height(100.dp), contentAlignment = Alignment.Center) {
                        Text("—", fontSize = 32.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        MutedText("코드 입력과 참여는 아직 연결되지 않았습니다.")
        FeatureAction("참여하기")
    }
}

@Composable
private fun TripHome(wide: Boolean, enabled: Boolean, go: (String) -> Unit) {
    TogetherHome(wide, enabled, go)
}

@Composable
private fun TripSchedule(wide: Boolean, enabled: Boolean, go: (String) -> Unit) {
    PageTitle("우리의 일정", "첫째 날 · 서울에서 강릉까지")
    TwoColumns(wide,
        first = {
            AppPanel {
                AppBadge("10월 31일 · 토요일")
                ScheduleStop("09:00", "서울 출발", "각자의 출발지에서 만나요")
                ScheduleStop("10:30", "가평휴게소", "일행과 함께 잠깐 쉬어가기")
                ScheduleStop("12:40", "강릉 해변", "오늘의 첫 목적지")
                ScheduleStop("13:30", "바닷가에서 점심", "민수네가 고른 횟집")
                ScheduleStop("15:00", "바다 전망 카페", "지연네가 찾은 곳")
                RequestDestinationButton(enabled)
            }
        },
        second = {
            Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
                AppPanel {
                    SectionTitle("이번 여행에 하고 싶은 것")
                    WishBubble("우리 가족", "바다 보고 싶어")
                    WishBubble("민수네 가족", "맛있는 회 먹자")
                    WishBubble("지연네 가족", "바다 보이는 카페도 가자")
                    FeatureAction("의견 추가하기")
                    FeatureAction("의견으로 일정 만들기")
                }
                AppPanel {
                    SectionTitle("일정 변경안이 있어요")
                    MutedText("민수네 차 도착 지연 · 30분")
                    NavigationAction("변경안 살펴보기", enabled) { go("changes") }
                }
            }
        }
    )
}

@Composable
private fun TripCrew(wide: Boolean, enabled: Boolean, go: (String) -> Unit) {
    TogetherCrew(wide, enabled, go)
}

@Composable
private fun TripTalk(wide: Boolean) {
    PageTitle("에어보이스", "따로 달려도 같은 차 안에 있는 것처럼")
    TwoColumns(wide,
        first = {
            AppPanel {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    AppSymbol("people", Modifier.size(30.dp))
                    SectionTitle("우리 여행 채널")
                    AppBadge("차량 3대")
                }
                WishBubble("민수네 · 10:42", "삼촌 차 보여?")
                WishBubble("우리 차 · 10:43", "응, 다음 휴게소에서 같이 쉬자.")
                MutedText("대화 내용은 화면 구성을 위한 예시입니다.")
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(22.dp)) {
                    Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        AppSymbol("mic", Modifier.size(50.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionTitle("목소리로 함께하기")
                            MutedText("운전 중에는 음성으로 간단하게")
                        }
                    }
                }
                FeatureAction("눌러서 말하기")
            }
        },
        second = {
            AppPanel {
                SectionTitle("일행에게 남기기")
                MutedText("잠시 멈췄을 때 여행의 순간을 나눠요.")
                PhotoTile(Modifier.fillMaxWidth())
                FeatureAction("사진 보내기")
                FeatureAction("짧은 영상 보내기")
                MutedText("사진·영상 기능은 정차 중 사용하는 흐름입니다.")
            }
        }
    )
}

@Composable
private fun TripRest(wide: Boolean, enabled: Boolean, go: (String) -> Unit) {
    PageTitle("어디서 함께 쉴까요?", "일행이 비슷한 시각에 만날 수 있는 곳")
    TwoColumns(wide,
        first = {
            AppPanel {
                AppBadge("휴식 장소 예시", warm = true)
                SectionTitle("가평휴게소")
                TripMap(280.dp)
                MutedText("화장실 · 카페 · 식사")
                FeatureAction("일행에게 휴식 제안")
                RestWaypointAction(enabled)
            }
        },
        second = {
            AppPanel {
                SectionTitle("잠깐 쉬어가도 좋아요")
                CurrentDrivingTimeText()
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                SectionTitle("휴게소까지")
                MemberRow("A", "우리 차", "도착까지", "15분")
                MemberRow("B", "민수네 차", "도착까지", "20분")
                MemberRow("C", "지연네 차", "도착까지", "18분")
                NavigationAction("여행 홈으로", enabled) { go("home") }
            }
        }
    )
}

@Composable
private fun TripChanges(enabled: Boolean, go: (String) -> Unit) {
    PageTitle("일정 변경안", "모두의 도착 시각에 맞춰 여유 있게")
    AppPanel {
        AppBadge("민수네 차 · 30분 지연", warm = true)
        SectionTitle("점심과 카페 시간을 늦출까요?")
        MutedText("먼저 도착한 일행은 해변에서 조금 더 머무를 수 있어요.")
        ChangeRow("강릉 해변", "12:40", "12:40")
        ChangeRow("점심 식사", "13:30", "14:00")
        ChangeRow("바다 전망 카페", "15:00", "15:30")
        FeatureAction("변경안을 일행에게 적용")
        NavigationAction("일정으로 돌아가기", enabled) { go("schedule") }
    }
}

@Composable
private fun TripInvite(enabled: Boolean, go: (String) -> Unit) {
    PageTitle("일행 초대", "함께 떠날 가족과 친구를 초대하세요.")
    AppPanel {
        SectionTitle("강릉, 바다 보러")
        MutedText("초대 코드")
        Text("— — — — — —", fontSize = 36.sp, fontWeight = FontWeight.Bold)
        MutedText("코드 발급과 초대 공유는 아직 연결되지 않았습니다.")
        FeatureAction("초대 코드 발급")
        FeatureAction("초대 공유하기")
        NavigationAction("일행으로 돌아가기", enabled) { go("crew") }
    }
}

@Composable
private fun TripAlbum(wide: Boolean) {
    PageTitle("우리의 추억", "이동한 길 위에 남길 여행의 순간들")
    TwoColumns(wide,
        first = {
            AppPanel {
                SectionTitle("강릉, 바다 보러")
                TripMap(260.dp)
                MutedText("서울 → 가평 → 강릉")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    PhotoTile(Modifier.weight(1f))
                    PhotoTile(Modifier.weight(1f))
                }
                MutedText("사진이 놓일 자리")
                FeatureAction("여행 앨범 만들기")
            }
        },
        second = {
            AppPanel {
                SectionTitle("함께 남길 이야기")
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(22.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        AppSymbol("mic", Modifier.size(36.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("목소리로 남길 순간", fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
                            MutedText("음성 기록이 놓일 자리")
                        }
                    }
                }
                FeatureAction("음성 기록 재생")
                FeatureAction("앨범 공유하기")
                MutedText("아직 저장된 사진이나 녹음은 없습니다.")
            }
        }
    )
}

@Composable
private fun WishBubble(name: String, words: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(name, fontSize = 21.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(18.dp)) {
            Text(words, Modifier.fillMaxWidth().padding(20.dp), fontSize = 25.sp, lineHeight = 36.sp)
        }
    }
}

@Composable
private fun ScheduleStop(time: String, title: String, detail: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
        Text(time, Modifier.width(86.dp), fontSize = 24.sp, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            MutedText(detail)
        }
    }
}

@Composable
private fun ChangeRow(place: String, before: String, after: String) {
    Surface(color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(place, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            Text("$before  →  $after", fontSize = 25.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
