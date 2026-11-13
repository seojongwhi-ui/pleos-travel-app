# 링카 네이버 지도 키 입력

## 지도 화면용 Client ID

프로젝트 최상위의 `local.properties` 파일을 열고 아래 등호 뒤에 발급받은 **Client ID**를 입력한다.

```properties
NAVER_MAP_CLIENT_ID=발급받은_Client_ID
```

이 파일은 Git 업로드 대상에서 제외되어 있다. 빌드할 때 `:travel-preview` 앱의
`com.naver.maps.map.NCP_KEY_ID` 매니페스트 설정으로 전달된다. 네이버 클라우드에서
**Dynamic Map**을 활성화하고 Android 패키지명 `com.contest.travel.preview`를 등록해야 한다.

## Directions용 Client Secret

**Client Secret은`local.properties`나 앱 코드에 입력 하지 않는다.** 추후 경로 요청을
실행할 PC의 PowerShell 창에서 다음과 같이 환경 변수로 설정한다.

```powershell
$env:NAVER_DIRECTIONS_CLIENT_SECRET = "발급받은_Client_Secret"
```

Directions 연결 서버는 아직 구현되지 않았다. 따라서 Secret을 설정해도 지금 화면의
경로가 바뀌지는 않는다. 추후 서버에서 같은 Client ID와 이 Secret으로 Directions를
호출하도록 연결한다. Secret은 APK나 Git에 포함하지 않는다.

지도 확대 화면은 네이버 지도 SDK로 표시된다. 지도 위 차량 마커와 경로선은 현재
로컬 시연 데이터이며, 실제 Directions 경로나 다른 차량의 실시간 위치가 아니다.
