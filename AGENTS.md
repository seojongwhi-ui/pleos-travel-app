# 같이가 개발 기준

- 기획서, 기획 대화 기록, 탐색 이미지의 Git 반영 위치는 `seojongwhi-ui/pleos-travel-app` 저장소의 기존 `기획서` 브랜치다. 해당 자료의 커밋·푸시 요청을 받으면 원격 저장소와 현재 브랜치를 먼저 확인한다.
- 사용자가 제공한 `Pleos_Connect_display_guide_215.fig`와 `Pleos_Connect_design_assets_215.fig`를 UI 기준으로 사용한다. 현재 사용자 지시가 항상 우선한다.
- UI를 수정하기 전에 `docs/design/pleos-215.md`와 `docs/design/pleos-215-tokens.json`을 읽는다. 원본은 `design/reference/`에 있으며 다운로드 원본도 유지한다.
- 공통 색상·서체·문자 크기·행간·버튼 규격은 `travel-preview/.../PleosDesign215.kt`와 `PreviewComponents.kt`에서 관리한다. 새 임의 글자 크기, 색상, 버튼 모양을 화면별로 추가하지 않는다.
- 현재 Foundation에 명시된 Asta Sans를 사용한다. 파일 내부에 남은 과거 42dot Sans 스타일을 현재 기준으로 혼용하지 않는다.
- 검증한 폼팩터는 Connect-S이다. 앱 창을 좁혔다고 Connect-L/W로 간주하지 않는다. 다른 디스플레이에 대응할 때는 원본의 해당 폼팩터 변수와 앱 영역을 확인해 적용한다.
- 시스템의 Driving View·Status bar·GNB 영역은 시스템에 맡긴다. 앱은 실제 Activity 영역에 맞춰 배치하고 시스템 UI를 복제하지 않는다.
- 제공된 아이콘이 있으면 우선 사용한다. 없는 아이콘을 표현할 때는 원본 Iconography의 48px/24 그리드, 3.6px 스트로크, round cap/join 원칙을 따르고 원본 아이콘 복사본으로 표시하지 않는다.
- 주행 UX 제한, 뒤로가기, 실제 SDK 값과 시연 데이터의 구분, 오류·미지원 표시는 디자인 변경에서도 유지한다.
- 앱 구현 범위는 별도 `:travel-preview` 모듈이다. 기존 공식 샘플 `:app`의 기능을 임의로 바꾸지 않는다.
- 화면 변경은 가능한 환경에서 넓은 창·좁은 창·라이트·다크로 확인한다. 검증하지 않은 기기나 동작을 통과했다고 적지 않는다.
- UI 변경 후 `node tools/check-pleos-design.cjs`로 원본 지문·색상·타입 스케일을 확인한다. 이 검사로 배치나 플랫폼 심사까지 통과한 것으로 판단하지 않는다.
- 로컬 `.fig`는 참조용이다. 원본 참조 파일은 `.gitignore` 대상이며, 소스·문서 변경을 GitHub에 푸시하는 것은 사용자가 요청했을 때만 한다.
