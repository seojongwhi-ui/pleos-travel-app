# 여행 앱 공모전 프로젝트

## 링카(LinkCar) 현재 개발

현재 앱 이름은 **링카(LinkCar)**입니다. 이전에 사용한 **온길(ONGIL)**과 **같이가** 기획 자료는 별도 `기획서` 브랜치에 보관합니다.

## 개발 작업 안내

Pleos Vehicle SDK 공식 샘플을 바탕으로 차량 상태 데이터를 활용하는 여행 앱을 준비합니다.
기존 샘플의 상태 조회를 검증했고, 별도 `:travel-preview` 모듈의 실행 UI를 [Figma 와이어프레임 21개](https://www.figma.com/design/c3q0MskWTtRP5gE9WspRVo/%ED%98%84%EB%8C%80-%EA%B3%B5%EB%AA%A8%EC%A0%84?node-id=252-11453) 흐름으로 교체했습니다.
Pleos 에뮬레이터에서 링카 아이콘을 누르면 여행 만들기·초대·출발 준비·주행·도착 시연을 시작할 수 있습니다.
지도 확대는 MapLibre Native + OpenFreeMap과 로컬 일행 위치를 사용합니다. 실제 차량 간 연결, 음성 전송, 음원 재생, 경로 안내는 아직 API 연결 전입니다.
화면의 차량 점수와 도착 시간은 시연 값입니다. [시연 범위와 API 연결 지점](docs/linkcar-demo-api-slots.md), [디자인 적용 기준](docs/design/pleos-215.md)을 확인하세요.
GitHub 커밋·푸시는 사용자가 요청할 때만 진행합니다.

- 원본 저장소: https://github.com/Pleos-Playground/pleos-sample-vehicle-app
- 원본 기준 커밋: `959981d960ec71144accb27af5ae5d341e72a644`
- 앱 코드 브랜치: `main` · 기획 자료 브랜치: `기획서`
- [개발 환경 분석 및 상태 조회 구현 계획](docs/개발환경_분석_및_구현계획.md)
- 원본 사용 설명: [README.md](README.md)

## 현재 확인 결과

2026-10-27에 Vehicle SDK 의존성을 `2.3.1`에서 공식 배포 버전인 `2.3.2`로 수정했습니다.
Windows의 한글 프로젝트 경로에서 빌드할 수 있도록 `gradle.properties`에 `android.overridePathCheck=true`도 설정했습니다.
공식 샘플의 Kotlin 코드와 Manifest는 변경하지 않았습니다.

수정 후 확인한 결과입니다.

- `assembleDebug` 성공. 별도 `-P` 옵션 없이도 한글 경로 검사 단계를 통과했습니다.
- Pleos Connect 2.1.0 AVD에 debug APK 설치 및 샘플 앱 실행 성공.
- 앱 화면에 `SDK 2.3.2`, `Vehicle Service 2.3.22` 표시.
- Door Position의 `ROW_1_LEFT` 영역 지원 확인 성공: 읽기/쓰기 지원, 범위 `0..1`.
- `getCurrentDoorPosition`의 실제 조회 결과 `0`을 화면에서 확인.
- 검증 중 제어 요청이나 Listener 등록은 실행하지 않았습니다.

초기 빌드 실패의 원인은 샘플에 지정된 `2.3.1`이 공식 Maven에서 HTTP 404로 응답한 것이었습니다.
JDK 17, Android SDK API 34/35, Build Tools 34/35와 WHPX 가속을 사용해 위 결과를 확인했습니다.

![SDK 2.3.2의 실제 Door Position 조회 결과](docs/images/sdk-2.3.2-door-read.png)

## 다음 개발 순서

1. 문 열림 상태 하나를 읽기 전용 화면에 표시하고, 실패·미지원·값 없음 상태를 구분합니다.
2. 여행 앱에 필요한 주행 시간·거리 등으로 조회 범위를 확장합니다.

빌드 명령, 실제 API 이름과 권한, 오류 처리와 에뮬레이터 테스트 절차는 분석 문서에 정리했습니다.
테스트 APK는 `app/build/outputs/apk/debug/app-debug.apk`에 생성됩니다.
이 APK는 debug 빌드이며 공모전 제출용 release APK는 아직 만들지 않았습니다.
