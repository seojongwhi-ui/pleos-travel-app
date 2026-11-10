# 링카(LinkCar) 단일 기기 시연과 API 연결 지점

## 현재 동작

Pleos 에뮬레이터에서 **링카 앱 아이콘**을 누르면 시연 홈이 바로 열리고 다음 흐름을 실행한다.

화면 구성과 문구는 [Figma 와이어프레임 21개](https://www.figma.com/design/c3q0MskWTtRP5gE9WspRVo/%ED%98%84%EB%8C%80-%EA%B3%B5%EB%AA%A8%EC%A0%84?node-id=252-11453)를 기준으로 다시 만들었다.

`S-01 홈 → S-02 여행 만들기 → S-03 초대 → S-04 대기실 → S-09 미션 → S-05 음악 → S-06 첫 곡 → S-10 주행 → S-50 모두 도착 → S-51 요약 → S-52 저장`

주행 화면에서는 M1 지도 확대(S-11), M3 음악 확대(S-12), 노크·30초 대화(S-20~23), 음성 메시지(S-24), 길에 남긴 음성(S-25~26)을 한 기기에서 상태 전환으로 시연한다.

- 친구 차의 위치·점수·탑승 상태는 이 기기의 시연 값이다. 실시간 차량 연결이나 서버 업로드는 없다.
- 모두에게 말하기와 음성 메시지는 상태 전환 시연이다. 소리가 녹음되거나 전송되지 않으며, 재생 버튼은 API 미연결 상태를 표시한다.
- 노크 10초 대기와 30초 통화 화면은 상태 전환 시연이다. 음성 채널은 열리지 않는다.
- 투표가 가결되면 M1의 경유지 표시만 바뀐다. 실제 내비게이션 경유지는 추가하지 않는다.
- 지도 확대 화면은 현재 OpenFreeMap 도로 지도와 로컬 일행 위치를 보여준다. 네이버 지도 SDK·Directions 연결은 아직 없다.
- 목적지와 음악은 시연 선택지다. 실제 경로 안내·음원 재생은 시작하지 않는다.
- 저장은 실행 중 시연 상태만 표시한다. 영구 여행 기록이나 음성 파일은 없다.
- 이 와이어프레임 시연의 차량 점수·도착 시간은 시연 값이다. 실제 SDK 조회 결과로 표시하지 않는다.

## 이후 구현할 어댑터

`LinkCarApiSlots.kt`에 각 연결 지점의 인터페이스가 있다. 현재 값은 모두 비어 있다. API를 사용할 때는 구현체를 만들고 시연 데이터 소스와 명확히 전환한다.

| 슬롯 | 예정 출처 | 연결 시 확인할 점 |
|---|---|---|
| `vehicle` | Pleos Vehicle 2.3.2의 Seat·Odometer, 필요 시 AAOS Car API | 좌석 권한, 속도 권한, 에뮬레이터 지원. 점수 계산에 필요한 가감속 데이터는 별도 검증 |
| `navigation` | Pleos NaviHelper 3.0.5 | 경로 탐색 시작과 실제 출발을 구분. 경유지는 결과 콜백으로 성공 확인 |
| `mapRoute` | 네이버 지도 Android SDK + Directions | 지도 클라이언트 ID, Directions 서버 인증 정보. Directions 비밀키를 APK에 넣지 않음 |
| `proximity` | Pleos FusedLocation 1.1.3 | 길에 남긴 음성의 근접 알림, 위치 권한과 실제 지원 여부 |
| `speech` | Pleos SpeechToText·TextToSpeech 3.3.1 | 프로젝트 접근 권한, 마이크·음성 출력, 주행 중 사용 가능 범위 |
| `voiceMessage` | 추후 녹음·재생 및 보관 수단 | 음성 파일 공유 동의, 보관·삭제 기준 |
| `group` | 추후 그룹 동기화 수단 | 초대·노크·투표·다른 차 위치. 현재는 서버 미사용 |
| `notification` | 추후 초대 알림 수단 | FCM 등 차량 앱에서 가능한 푸시 방식 확인 |
| `proposal` | 추후 분류 서비스 또는 로컬 규칙 | 일반 메시지와 장소 제안 구분. 제안의 장소 좌표 확인 필요 |
| `call` | 추후 실시간 음성 채널 | 30초 노크콜. STT/TTS와 별도 |
| `music` | 추후 허가된 음원 서비스 | 차량별 재생권·오디오 포커스·동기화 |

지도와 Directions는 서로 다른 데이터다. 네이버 경로선과 Pleos 내비의 실제 선택 경로가 다를 수 있으므로, 실서비스에서는 어느 경로를 기준으로 진행률을 표시할지 정해야 한다.

## 기준 자료

- `C:\Users\82102\Documents\카카오톡 받은 파일\lincar-handoff\ongil-handoff\docs\02_flows.md`
- `C:\Users\82102\Documents\카카오톡 받은 파일\lincar-handoff\ongil-handoff\docs\03_screens.md`
- `C:\Users\82102\Documents\카카오톡 받은 파일\lincar-handoff\ongil-handoff\docs\04_modules.md`
- `C:\Users\82102\Documents\카카오톡 받은 파일\lincar-handoff\ongil-handoff\docs\09_mvp_scope.md`
