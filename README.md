# 링카(LinkCar) — Pleos 여행 앱 개발 미리보기

여러 대의 차가 같은 목적지를 향해 함께 여행하는 경험을 위한 Android Automotive 앱입니다.
별도 `:travel-preview` 모듈에 피그마 와이어프레임을 따른 21개 화면, 앱 안 도로 지도와 로컬 일행 이동 시연을 구현했습니다.
현재 UI는 사용자가 제공한 Pleos Connect 2.1.5 디자인 원본을 기준으로 합니다. [디자인 기준과 출처](docs/design/pleos-215.md)를 참고하세요.

- 현재 실행 화면은 Figma 와이어프레임의 홈·여행 만들기·초대·출발 준비·주행·도착 흐름으로 교체했습니다. 뒤로가기와 화면 간 선택은 동작합니다.
- 주행 화면의 지도 확대에는 도로 지도와 로컬 일행 위치를 표시합니다. 차량 간 위치 공유·실시간 통화·음원 재생·실제 경로 안내는 API 연결 전 시연 상태입니다.
- Pleos SpeechToText 3.3.1로 투표 제안을 받아쓰고, 로컬 OpenAI 브리지로 투표 안건을 생성합니다. 원본 음성 녹음은 앱 내부 저장소에 보관되며 시연 차량 B·C가 녹음 지점을 지날 때 재생됩니다. 이는 실제 차량 간 전송이 아닙니다.
- Pleos FusedLocation 1.1.3이 허용되면 녹음 위치 메타데이터에 사용합니다. 권한이나 서비스가 없으면 NaviHelper 위치 또는 로컬 시연 경로를 사용하며 화면에 상태를 표시합니다.
- Vehicle SDK 2.3.2와 NaviHelper 3.0.5의 기존 연동 코드는 프로젝트에 남아 있지만, 새 와이어프레임 화면의 점수·도착 시간에는 아직 연결하지 않았습니다.

## 실행 방법과 테스트 환경

JDK 17과 Android SDK 35로 `./gradlew :travel-preview:assembleDebug`를 실행합니다.
Windows에서는 `./gradlew.bat :travel-preview:assembleDebug`를 사용할 수 있습니다.
생성된 APK를 Pleos 에뮬레이터에 설치한 뒤 **링카 앱 아이콘**을 누르면 시연 홈이 바로 열립니다.

- 확인 환경: Pleos Connect 2.1.0 / Android 14 / Vehicle Service 2.3.22.
- 도로 지도 다운로드에는 인터넷 연결이 필요합니다. 현재는 주차 상태의 개발 시연입니다.
- 개발용 APK 생성 위치: `travel-preview/build/outputs/apk/debug/travel-preview-debug.apk`.
  APK는 빌드 산출물이므로 Git에 포함하지 않으며, 공모전 제출용 서명된 release APK는 아직 준비 전입니다.
- 디버그 빌드·에뮬레이터 설치·앱 실행과 Pleos 디자인 검사를 확인했습니다. 원본 음성 녹음·재생, B차 시연 차량의 녹음 지점 통과 자동 재생, Pleos FusedLocation 위치 수신, OpenAI 브리지의 키 누락 오류 표시도 에뮬레이터에서 확인했습니다. Pleos STT는 현재 에뮬레이터가 `ON_DEVICE_ENGINE_ERROR`를 반환해 음성 인식 완료까지는 확인되지 않았습니다.

## 음성 투표 시연 설정

1. 호스트 PC의 PowerShell에서 API 키를 **환경 변수** `OPENAI_API_KEY`로 설정합니다. 키를 코드·APK·Git에 넣지 마세요.
2. 같은 PowerShell에서 `python tools/openai_poll_bridge.py`를 실행해 둡니다. 브리지는 PC의 `127.0.0.1:8765`에서만 듣고, 에뮬레이터 앱은 `10.0.2.2:8765`로 접속합니다.
3. 링카 주행 화면의 **모두에게 말하기 → 투표 안건 말하기**를 누른 뒤 마이크·위치 권한을 허용합니다. Pleos STT가 받아쓴 문장을 OpenAI Responses API에 보내 투표 질문과 선택지를 M2에 표시합니다. 모델은 `OPENAI_POLL_MODEL`로 바꿀 수 있으며 기본값은 `gpt-4o-mini`입니다.
4. **모두에게 말하기 → 길에 음성 남기기**에서 녹음 시작·완료를 누르면 원본 `.m4a` 파일이 앱 비공개 저장소에 저장됩니다. 로컬 시연 차량이 해당 진행 지점을 지나면 재생합니다.

브리지가 실행되지 않거나 키·Pleos 서비스 권한이 없으면 앱에 실패 이유가 표시됩니다. 운전자 주행 UX 제한이 적용될 때는 앱의 음성 조작도 제한될 수 있습니다.
STT 엔진 오류가 나는 에뮬레이터에서는 **텍스트로 AI 투표 시험** 버튼으로 OpenAI 연결만 따로 검증할 수 있습니다. Pleos 공식 안내대로 에뮬레이터 Extended Controls의 Microphone 입력과 AVD Cold boot 설정을 확인하세요.

[프로젝트 안내](START_HERE.md) · [링카 시연과 API 연결 지점](docs/linkcar-demo-api-slots.md)

아래는 기반으로 사용한 공식 SDK 샘플의 설명입니다.

---

# Pleos Vehicle SDK Android Sample

An Android sample for integrating the Pleos Vehicle SDK `2.3.2`.
The Door Position flow is the official, typed SDK reference in this repository.
The in-app API catalog is a companion screen for discovering public method names,
parameters, and operation order before writing application code.

## What is included

- A typed Door Position reference flow covering capability, read, write, register, and
  unregister patterns.
- A searchable feature catalog grouping 442 non-deprecated public methods across 19 domains:
  Brake, Car info, Display, Door, Driving mode, EV battery, Fuel, HVAC, Light,
  Odometer, Safety, Seat, Side mirror, Steering wheel, Tire, Toll, Turn signal,
  Window, and Wiper.
- Domain counts show grouped **features**, not API methods. For example, Door shows
  **3** features (Door Position, Door Lock, Glove Box Lock), rather than 15 methods.
- The domain sidebar stays visible while a feature is open. The upper-left Back
  action returns to that domain's feature list.
- A compact common app bar keeps the app name, SDK/Service versions and connection
  state visible on both screens. Entering a feature does not move the header or
  domain list; wide layouts use a 64dp bar instead of the previous introduction.
- Area dropdowns, ON/OFF switches, named choices, and numeric editors with +/- buttons.
  Sliders appear only when the capability response supplies a real numeric range.
- A small, fixed **API activity** dock below the scrolling controls. Expand it for
  full method names and responses; additional results never increase its height.
- Explicit **Apply change** for control calls. Editing a value does not send it.

Private, hidden, and deprecated APIs are intentionally excluded. Gear is not part
of this sample's catalog.

The Door Position feature is the reference to copy into an application. Its methods
call the typed Vehicle SDK directly. The catalog is useful for finding the public API
name and its parameter shape, but production code should use the SDK's typed API
directly as shown below.

## Screenshots

Captured on `Pleos_Connect_v2.1` (Android 14), using Vehicle SDK `2.3.2` and
Vehicle Service `2.3.22`. Images show the actual app region cropped from the
2560 x 1440 AVD display, not the HTML prototype.

### Domain counts and feature browsing

Door offers three grouped features. Searching narrows the cards without changing
the domain's total. The same compact app bar remains visible in the dashboard.

![Feature catalog with Door showing three features](docs/images/ux-feature-catalog.png)

### Persistent domains, left Back and discrete controls

The app bar and domain list keep their positions in the dashboard. Door Position's reported
`0..1` range becomes Closed/Open choices. The bottom Activity dock is fixed at
128dp on this wide layout while the controls above it scroll independently.

![Door Position with retained domains and compact fixed Activity](docs/images/ux-door-position.png)

### Expanded API results

Expand raises the dock to 256dp on this display, without covering the controls.
Each API shows its latest result; the expanded content has its own scroll.
Invoking an API brings its result into view within the dock without moving the
control pane or taking input focus.

![Expanded API Activity with complete method names and responses](docs/images/ux-activity-expanded.png)

### Boolean input and visible failures

The switch prepares an ON/true draft; it does not send a control request.
This AVD rejected the Door Lock read for `ROW_1_LEFT`, so the actual error and
Failed activity status are retained in the screenshot. No Door Lock write is
claimed or shown as successful.

![Door Lock switch and the actual unsupported-area read error](docs/images/ux-boolean-control.png)

### Real capability range, slider and exact input

Window Position reports `0..10` for `ROW_1_LEFT` on this AVD. The screenshot shows
a draft of 5 selected with the slider; the last read remains 0. The +/- buttons
and direct numeric entry share the same bounds. Areas without reported bounds
show numeric entry without a guessed slider range.

![Window Position with actual 0 to 10 bounds and a draft value of five](docs/images/ux-range-control.png)

During verification, Door Position SET=1 completed, but an explicit follow-up
read still returned 0. This confirms request completion and separate read behavior,
not a persistent vehicle-state transition. A restore SET=0 completed and the final
read was 0; no test subscriptions were left running.

## Requirements

- Android Studio with Android SDK Platform 35
- JDK 17
- Android 6.0 (API 23) or later
- A Pleos device or emulator containing a compatible Vehicle Service
- Vehicle SDK `2.3.2`

The app cannot exercise vehicle properties on a standard Android phone image because
Pleos Vehicle Service is not available there.

## Dependency

The repository already includes the Pleos Maven repository and dependency:

```kotlin
repositories {
    maven("https://nexus-playground.pleos.ai/repository/maven-releases/")
}

dependencies {
    implementation("ai.pleos.playground:Vehicle:2.3.2")
}
```

## Permissions

The manifest declares the public `CAR_*` read permissions and matching
`CONTROL_CAR_*` permissions required by the 19 supported domains. It does not declare
any `PRIVATE_*` permission.

These Pleos Vehicle Service permissions have `protectionLevel="normal"`, so they do
not use an Android dangerous-permission runtime dialog. Actual property availability
still depends on the connected vehicle or emulator and should be checked through each
capability API.

## Architecture

```text
MainActivity
    └── MainViewModel
            ├── VehicleClient
            │       └── DoorPositionRequests (typed Door integration)
            └── VehicleApiExplorer (public catalog and reflection calls)
                    └── Vehicle SDK 2.3.2 / Vehicle Service
```

- `MainActivity` hosts Compose and forwards UI actions.
- `MainViewModel` owns navigation, grouped feature counts, per-area capability bounds,
  presentation state, and API results.
- `VehicleClient` owns the SDK lifecycle and typed Door calls.
- `SampleApp` contains the domain catalog and feature dashboard.
- `FeatureControlPane` uses shared parameter editors. Read, write and subscription
  actions use the selected area; unknown numeric bounds are never replaced with
  an assumed percentage range.
- `ApiActivityPanel` is a fixed bottom dock with independent expanded scrolling.

## How to use the typed Door API

The SDK calls are asynchronous. Initialize one `Vehicle` session, obtain the typed
`Door` API, check capability first, and then perform read, control, or listener
operations. Always release the session when the owning component is destroyed.

### 1. Create and initialize the session

```kotlin
import android.content.Context
import ai.pleos.playground.vehicle.Vehicle

class DoorPositionClient(context: Context) {
    private val vehicle = Vehicle(context.applicationContext)

    fun initialize() {
        vehicle.initialize()
    }

    fun release() {
        vehicle.release()
    }
}
```

`Vehicle.initialize()` must complete before calling a vehicle property API. If the
service is unavailable or initialization fails, surface the error and do not continue
with property calls.

### 2. Select the area and check capability

The sample uses the driver's first-row area. A capability response tells you whether the
property is available, which access mode is supported, and the valid value range for
each area.

```kotlin
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.constant.area.DoorValue

private val area = DoorArea(DoorValue.ROW_1_LEFT)

fun checkDoorPosition() {
    val door = vehicle.getDoor()
    door.checkDoorPositionCapabilities(
        onComplete = { config, areaBounds ->
            val bounds = areaBounds[area]
            println("access=${config.accessType}")
            println("range=${bounds?.minValue}..${bounds?.maxValue}")
        },
        onFailed = { exception ->
            reportError(exception)
        }
    )
}
```

Use the returned range instead of assuming a universal value range. A vehicle may
support the API but expose different bounds per area, or report that the property is
unsupported.

### 3. Read the current value

Call the read API only after a successful capability check. The result can be `null`
when the service cannot provide a current value.

```kotlin
fun readDoorPosition() {
    val door = vehicle.getDoor()
    door.getCurrentDoorPosition(
        area = area,
        onComplete = { position ->
            println("current position=$position")
        },
        onFailed = { exception ->
            reportError(exception)
        }
    )
}
```

### 4. Set a value

Setting a value changes vehicle state. Validate the value against the capability range,
make sure the capability grants control access, and require an explicit user action
before calling the control API.

```kotlin
fun setDoorPosition(position: Int) {
    val door = vehicle.getDoor()
    door.setDoorPosition(
        area = area,
        value = position,
        onComplete = { _, acceptedValue ->
            println("accepted position=$acceptedValue")
        },
        onFailed = { exception ->
            reportError(exception)
        }
    )
}
```

The sample UI sends this call only when **Apply change** is pressed. Numeric input
must parse as the SDK type and stay inside the selected area's reported range, if
available. Door Position with a `0..1` range uses **Closed / Open**, not a percentage
slider. Other ranges use a slider plus exact numeric entry.

A successful SET acknowledgement appears in API activity. It does not trigger an
automatic GET or replace the displayed measured value. Press **Refresh** to read
the current value; an active listener can also update it.

### 5. Register and unregister updates

Keep the same listener instance for both operations. Unregister it before releasing the
Vehicle session or when the observing screen/component stops.

```kotlin
import ai.pleos.playground.vehicle.listener.DoorPositionListener

private val doorPositionListener = object : DoorPositionListener {
    override fun onPositionUpdated(pos: Int?) {
        println("door position updated=$pos")
    }

    override fun onFailed(exception: Exception) {
        reportError(exception)
    }
}

fun registerDoorUpdates() {
    vehicle.getDoor().registerDoorPosition(area, doorPositionListener)
}

fun unregisterDoorUpdates() {
    vehicle.getDoor().unregisterDoorPosition(area, doorPositionListener)
}
```

A complete owner follows this order:

```kotlin
initialize()
checkDoorPosition()
readDoorPosition()
setDoorPosition(position = 0) // only after capability and user confirmation
registerDoorUpdates()
unregisterDoorUpdates()
release()
```

The exact sequence can be adapted to the screen or service lifecycle, but a listener
must not outlive the `Vehicle` session that registered it. Because these APIs are
asynchronous, start each next step from the previous call's successful callback (or
an equivalent coroutine/state machine) rather than running the sequence synchronously.

### Complete lifecycle shape

For reference, the sample's client follows this lifecycle:

```kotlin
val vehicle = Vehicle(context)
vehicle.initialize()

val door = vehicle.getDoor()
// checkDoorPositionCapabilities / getCurrentDoorPosition /
// setDoorPosition / registerDoorPosition / unregisterDoorPosition

vehicle.release()
```

## Run

1. Start a compatible Pleos emulator or connect a safely parked test vehicle.
2. Build and install the debug APK:

   ```bash
   JAVA_HOME=/path/to/jdk-17 ./gradlew installDebug
   ```

3. Open **Vehicle SDK Sample**.
4. The app opens with **Door** selected at the top of the domain list. Open
   **Door Position** to follow the typed integration reference.
5. Opening a feature checks its capability and performs an initial read after a
   successful check. **Check support** can repeat the capability request.
6. Choose an area, edit the value with its type-specific control, then press
   **Apply change**. Use **Refresh** to read the result. These diagnostic actions
   remain independent; they are not a mandatory step-by-step wizard.
7. Watch **API activity** at the bottom while scrolling the controls. The compact
   dock shows two recent method results on wide layouts, or one on narrower layouts.
   **Expand / Collapse** changes the dock size explicitly.
8. Use **Start updates / Stop updates** to test the listener APIs. Stop a typed
   Door subscription using its registered area. Switching the typed subscription
   to another area unregisters the old area with the same listener first.
9. Use the upper-left Back arrow for another feature or choose a different domain
   directly. On narrow layouts, **Browse domains** remains available in the dashboard.

Missing permissions, unsupported capabilities, incompatible services, and callback
failures are displayed in the result area.

## Target SDK versioning

Sample releases encode the target Android SDK in both Android version fields:

```text
versionName = <major>.<minor>.<patch>-sdk<targetSdk>
versionCode = targetSdk * 1,000,000 + major * 10,000 + minor * 100 + patch
```

For this SDK 35 release:

```text
minSdk = 23
targetSdk = 35
versionName = 1.0.0-sdk35
versionCode = 35010000
```

## Verify

Run all local checks with JDK 17:

```bash
JAVA_HOME=/path/to/jdk-17 ./gradlew lintDebug testDebugUnitTest assembleDebug
```

Run Compose UI tests on a connected compatible emulator:

```bash
JAVA_HOME=/path/to/jdk-17 ./gradlew connectedDebugAndroidTest
```

Tests cover retained navigation, fixed dock geometry, feature counts, parameter
editors, numeric validation, actual capability-bound extraction, selected Door
areas, listener cleanup, and separate SET/read behavior.

Known existing test mismatch: `PublicApiPolicyTest` still pins the SDK `2.2.10`
catalog to 441 methods, while the current `2.3.2` dependency exposes 442. That
assertion is not skipped or relaxed; it remains a failure in the full unit suite.
