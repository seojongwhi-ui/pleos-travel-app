package com.contest.travel.preview

import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData
import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.constant.Mode
import ai.pleos.playground.stt.listener.OnInitializeListener
import ai.pleos.playground.stt.listener.ResultListener
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

internal data class GeneratedPoll(val title: String, val options: List<String>)

/** Calls the host-only OpenAI bridge. No API key is stored in the APK. */
internal object PollBridge {
    fun generate(transcript: String): Result<GeneratedPoll> = runCatching {
        val connection = URL("http://10.0.2.2:8765/poll").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 6_000
            connection.readTimeout = 20_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.outputStream.use { it.write(JSONObject().put("transcript", transcript).toString().toByteArray(Charsets.UTF_8)) }
            val body = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
                .bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = JSONObject(body)
            if (connection.responseCode !in 200..299) error(json.optString("error", "투표 생성 서버 오류 (${connection.responseCode})"))
            val title = json.getString("title").trim()
            val choices = json.getJSONArray("options")
            val options = (0 until choices.length()).map { choices.getString(it).trim() }.filter { it.isNotEmpty() }.distinct()
            check(title.isNotEmpty() && options.size in 2..4) { "AI가 유효한 투표 안건을 만들지 못했습니다." }
            GeneratedPoll(title, options)
        } finally { connection.disconnect() }
    }
}

internal class LinkCarVoiceRuntime(private val context: Context) {
    val notes = RoadVoiceNoteStore(context)
    val audio = LinkCarOriginalAudio(context)
    var status by mutableStateOf("마이크를 누르면 투표 제안이나 길 위 음성을 남길 수 있어요.")
        private set
    var isListening by mutableStateOf(false)
        private set
    var isRecording by mutableStateOf(false)
        private set
    var currentPoint by mutableStateOf<GeoPoint?>(null)
        private set
    var locationStatus by mutableStateOf("시연 경로 사용")
        private set
    private var stt: SpeechToText? = null
    private var sttListener: ResultListener? = null
    private var fused: FusedLocation? = null
    private var finalReceived = false

    fun updateFallbackPoint(point: GeoPoint?) {
        if (currentPoint == null && point?.isValid() == true) {
            currentPoint = point
            locationStatus = "NaviHelper 위치 사용"
        }
    }

    fun startLocation() {
        if (fused != null) return
        if (context.checkSelfPermission("pleos.car.permission.FUSED_LOCATION") != PackageManager.PERMISSION_GRANTED) {
            locationStatus = "FusedLocation 권한 없음 · NaviHelper/시연 경로 사용"
            return
        }
        runCatching {
            val service = FusedLocation(context)
            check(service.isSupported()) { "FusedLocation을 지원하지 않는 환경입니다." }
            service.initialize()
            locationStatus = "Pleos FusedLocation 연결됨 · 위치 대기"
            service.registerFusedLocationCallback(object : FusedLocationListener {
                override fun onChanged(data: LocationData?) {
                    data?.let { GeoPoint(it.latitude, it.longitude) }?.takeIf { it.isValid() }?.let {
                        currentPoint = it
                        locationStatus = "Pleos FusedLocation 위치 수신"
                    }
                }
                override fun onError(error: Exception) { locationStatus = "FusedLocation 조회 실패: ${error.message}" }
            })
            fused = service
        }.onFailure { locationStatus = "FusedLocation 연결 실패: ${it.message}" }
    }

    fun startPollSpeech(onTranscript: (String) -> Unit) {
        if (!hasMicrophonePermission()) { status = "마이크 권한이 필요합니다."; return }
        if (isRecording) { status = "녹음을 끝낸 뒤 투표 안건을 말해 주세요."; return }
        if (isListening) return
        stopListening()
        finalReceived = false
        runCatching {
            val service = SpeechToText(context, Mode.ON_DEVICE)
            val listener = object : ResultListener {
                override fun onReady() { status = "마이크 준비 완료. 투표할 내용을 말해 주세요." }
                override fun onStartedRecognition() { isListening = true; status = "듣고 있어요. 투표 안건을 말해 주세요." }
                override fun onUpdated(sttText: String, completed: Boolean) {
                    if (completed && !finalReceived && sttText.isNotBlank()) {
                        finalReceived = true
                        isListening = false
                        status = "인식한 말: $sttText"
                        onTranscript(sttText.trim())
                    }
                }
                override fun onUpdatedEpdData(start: Long, end: Long) = Unit
                override fun onUpdatedRms(rms: Double) = Unit
                override fun onEndedRecognition() {
                    isListening = false
                    if (!finalReceived) status = "음성을 인식하지 못했습니다. 다시 말해 주세요."
                }
                override fun onError(error: ErrorType) { isListening = false; status = sttErrorMessage(error) }
            }
            stt = service
            isListening = true
            status = "Pleos 음성 인식을 연결하고 있어요."
            service.initialize(object : OnInitializeListener {
                override fun onSuccess() {
                    runCatching {
                        service.addListener(listener)
                        sttListener = listener
                        service.setLocale(Locale.KOREAN)
                        service.request()
                    }
                        .onFailure { status = "Pleos 음성 인식 시작 실패: ${it.message}"; isListening = false }
                }
                override fun onFailure(error: ErrorType) { isListening = false; status = sttErrorMessage(error) }
            })
        }.onFailure { isListening = false; status = "Pleos STT 연결 실패: ${it.message}" }
    }

    fun stopListening() {
        runCatching { stt?.stop() }
        sttListener?.let { listener -> runCatching { stt?.removeListener(listener) } }
        sttListener = null
        runCatching { stt?.release() }
        stt = null
        isListening = false
    }

    fun startRecording() {
        if (!hasMicrophonePermission()) { status = "마이크 권한이 필요합니다."; return }
        if (isListening) stopListening()
        if (isRecording) return
        val file = notes.newFile()
        audio.start(file).onSuccess {
            isRecording = true
            status = "원본 음성을 녹음하고 있어요. 완료를 눌러 저장하세요."
        }.onFailure { status = "녹음 시작 실패: ${it.message}" }
    }

    fun finishRecording(progress: Float, fallbackPoint: GeoPoint?): RoadVoiceNote? {
        if (!isRecording) { status = "진행 중인 녹음이 없습니다."; return null }
        isRecording = false
        return audio.stopAndSave().mapCatching { file ->
            val note = notes.add(file, progress, currentPoint ?: fallbackPoint)
            status = "음성을 저장했어요. 로컬 시연의 뒤차가 이 지점을 지나면 원본이 재생됩니다."
            note
        }.onFailure { status = "녹음 저장 실패: ${it.message}" }.getOrNull()
    }

    fun cancelRecording() {
        audio.cancelRecording()
        isRecording = false
        status = "녹음을 취소했어요."
    }

    fun play(note: RoadVoiceNote, car: String? = null) {
        audio.play(notes.file(note)) {
            status = if (car == null) "원본 음성 재생 완료" else "$car 시연 차량의 통과 재생 완료"
        }.onSuccess {
            status = if (car == null) "저장된 원본 음성을 재생합니다." else "$car 시연 차량이 녹음 지점을 지나 원본 음성을 재생합니다."
            if (car != null) notes.markPlayed(note.id, car)
        }.onFailure { status = "음성 재생 실패: ${it.message}" }
    }

    private fun hasMicrophonePermission() =
        context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun sttErrorMessage(error: ErrorType): String = when (error) {
        ErrorType.ON_DEVICE_ENGINE_ERROR ->
            "Pleos STT 엔진 오류: 에뮬레이터의 마이크 입력과 Cold boot 설정을 확인해 주세요."
        ErrorType.PERMISSION_DENIED -> "Pleos STT 서비스 권한이 없습니다. 앱의 STT_SERVICE 접근 상태를 확인해 주세요."
        ErrorType.MIC_ERROR -> "Pleos STT 마이크 오류: 에뮬레이터의 마이크 입력을 확인해 주세요."
        else -> "Pleos 음성 인식 오류: $error"
    }

    fun release() {
        val wasRecording = isRecording
        stopListening()
        fused?.release()
        fused = null
        audio.release()
        isRecording = false
        if (wasRecording) status = "앱이 닫혀 녹음을 취소했습니다."
    }
}
