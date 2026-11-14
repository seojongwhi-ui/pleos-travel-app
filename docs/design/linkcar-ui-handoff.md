# 링카 UI 인수인계 · 2026-11-14

작업 모듈은 `:travel-preview`다. 기존 SDK 샘플인 `:app`은 변경하지 않는다. Figma 원본과 Pleos 시스템 영역을 구분하며, 앱은 시스템에서 받은 Activity 영역에 배치한다.

## 원본과 구현 범위

- Figma 파일: `c3q0MskWTtRP5gE9WspRVo`
- [주행 전 흐름 443:14524](https://www.figma.com/design/c3q0MskWTtRP5gE9WspRVo/?node-id=443-14524): 홈부터 초대·대기·미션·음악·참여자 화면까지 11개 시안.
- [주행 중 흐름 443:20589](https://www.figma.com/design/c3q0MskWTtRP5gE9WspRVo/?node-id=443-20589): 지도·음악·노크·통화·음성·이전 대화·설정·동행 종료 등 19개 시안.
- 원본 노드·색상·모션 명세: `linkcar-journey-figma.json`, `linkcar-driving-figma.json`.
- 이미지·아이콘 원본 및 지문: `linkcar-predrive-assets.json`, `linkcar-driving-assets.json`. SVG는 assets에 원본을 보존하고 Android 리소스용 PNG를 함께 둔다.
- 주행 상세 구현 및 검증 기록: [linkcar-driving-implementation.md](linkcar-driving-implementation.md).

주요 반영 사항은 화면 간 이동·페이드, 버튼 눌림, 음악 준비 화면의 프로필 원형 부유와 링 크기 변화, 노크 두 번 두드림, 잠금 흔들림·문맥 안내, 점수 카운트다운·감점 배지다. 배지는 사용자 첨부 SVG 기준으로 20% 확대했다. 지도·음악 확대의 점수·거리 숫자도 Lufga를 사용한다.

노크는 로컬 시연에서 약 3초 뒤 상대가 수락한다. 다연·문준 프로필을 대기·통화·응답 불가 화면에 일관되게 연결했다. 이전 대화와 안내에는 ‘주행 화면으로’ 복귀 버튼이 있다. 동행 종료 확인은 현재 배경을 유지하고, 종료 후 최신 홈으로 돌아간다.

## 빌드와 검사

JDK 17, Android SDK 35와 Pleos Connect 에뮬레이터가 필요하다. SDK 경로는 각자의 `local.properties` 또는 `ANDROID_HOME`에 설정한다. 로컬 설정·키·APK·에뮬레이터 이미지는 저장소에 포함하지 않는다.

```sh
./gradlew :travel-preview:assembleDebug :travel-preview:testDebugUnitTest :travel-preview:lintDebug
node tools/check-pleos-design.cjs
adb install -r travel-preview/build/outputs/apk/debug/travel-preview-debug.apk
```

Windows는 `gradlew.bat`를 사용한다. 여러 에뮬레이터가 연결되어 있으면 아래 adb 명령에 `-s <기기 일련번호>`를 붙인다.

확인 환경은 Pleos Connect 2.1.0 / Android 14 / Connect-S다. 넓은 앱 창과 실제 시스템 분할, 다크 모드, 애니메이션 배율 0을 확인했다. 다른 폼팩터의 검증 완료를 뜻하지 않는다. 단위 검사 24개가 있으며 연결 오류·주차/후진·재연결 급변·중복 감점 방지와 개발팀 음성 시연의 새 화면 연결도 포함한다.

## 개발용 미리보기

디버그 빌드에서만 `DESIGN_PREVIEW` 인텐트를 읽는다. 프레임 목록은 각 Figma 명세 JSON의 `sourceNodes`를 참고한다. Activity가 이미 실행 중이면 먼저 종료하여 새 인텐트로 시작한다.

```sh
adb shell am force-stop com.contest.travel.preview
adb shell am start -W -a com.contest.travel.preview.DESIGN_PREVIEW -n com.contest.travel.preview/.MainActivity --es figmaNode 443:21947
```

노크·잠금·숫자 클릭 모션만 확인하려면 같은 명령 끝에 `--ez companionMotion true`를 붙인다. 이 모드는 시작 후 100→97 예시를 한 번 재생한다. 미응답 화면을 확인하려면 `--ez knockAutoAnswer false`를 붙여 10초 미응답 흐름을 사용한다.

## D단 운전과 감점 확인

```sh
adb forward tcp:8088 tcp:8088
adb shell am force-stop com.contest.travel.preview
adb shell am start -W -a com.contest.travel.preview.DESIGN_PREVIEW -n com.contest.travel.preview/.MainActivity --es figmaNode 443:21947 --ez drivingSimulation true
```

브라우저에서 `http://127.0.0.1:8088/`를 열고 D를 선택한다. ACCEL을 길게 누르면 가속하고 BRAKE를 길게 누르면 제동한다. 멈춘 뒤 P로 전환한다. [공식 Virtual Console 안내](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/use-vehicle-emulator)를 참고한다.

이 전용 모드는 에뮬레이터 속도·기어를 **다연 차의 시연 입력**으로 매핑한다. 실제 상대 차량에서 수신한 데이터가 아니다. 주행 화면에는 내 차 점수를 표시하지 않으며, 상단에 시연 대상과 기어를 명시한다. 일반 Figma 스냅샷과 숫자 클릭 모드에는 이 매핑을 적용하지 않는다.

임시 시험 기준은 급가속 2.5m/s²·급제동 3m/s²가 0.4초 지속되면 각 -3, 80km/h 초과가 2초 지속되면 과속 -5다. 공식 채점 기준이나 도로별 제한 속도가 아니다. `DrivingSimulationRules`에서 조정한다.

차량 간 동기화·실제 통화·음원 스트리밍은 이번 UI 작업에서 추가하지 않았다. 음성·지도 API의 기존 연결 코드와 오류 표시는 유지한다. 필요한 지도 ID·API 키는 각자의 로컬 설정으로 제공한다.

## 개발팀 최신 변경과 통합

`jooseok-ui`의 `67d163f`를 기반으로 디자인을 반영했다. MapLibre 지도·도로 경로·차량 위치 동기화와 투표 시연 음성 파일을 유지한다. 실제 조작으로 지도를 펼치면 개발팀 MapLibre 지도가 열리며, 디자인 원본 확인용 스냅샷은 Figma 지도를 유지한다. NAVER 키가 없다는 이유로 MapLibre 지도를 가리지 않으며, 지도 프로필은 새 주행 화면의 다연·문준 사진과 일치시켰다. 지도 접기와 확대/축소 버튼은 겹치지 않게 배치한다.

개발팀의 투표 음성 시연 상태도 새 M2 화면에 연결했다. 말하는 중/완료 상태와 원본 예시 문장을 표시하고, 돌아가기·뒤로가기·종료 시 시연 음성을 정리한다.
