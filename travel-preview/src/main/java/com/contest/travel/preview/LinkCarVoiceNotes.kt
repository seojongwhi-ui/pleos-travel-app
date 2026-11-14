package com.contest.travel.preview

import android.content.Context
import android.net.Uri
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

internal data class RoadVoiceNote(
    val id: String,
    val fileName: String,
    val anchorProgress: Float,
    val latitude: Double?,
    val longitude: Double?,
    val createdAt: Long,
    val playedBy: Set<String> = emptySet()
)

/** Original microphone audio stays in this app's private storage on the demo device. */
internal class RoadVoiceNoteStore(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("linkcar_road_voice", Context.MODE_PRIVATE)
    private val directory = File(app.filesDir, "road_voice").apply { mkdirs() }
    val notes = mutableStateListOf<RoadVoiceNote>()
    private val markerFile = File(directory, "demo_road_voice.audio")
    var hasDemoMarker by androidx.compose.runtime.mutableStateOf(
        markerFile.isFile && !prefs.getBoolean("marker_consumed", false)
    )
        private set

    init {
        load()
        importBundledDemoMarker()
    }

    private fun importBundledDemoMarker() {
        if (hasDemoMarker || prefs.getBoolean("marker_consumed", false)) return
        val name = app.assets.list("")?.firstOrNull {
            it in setOf("road_voice_demo.m4a", "road_voice_demo.mp3", "road_voice_demo.wav", "road_voice_demo.ogg")
        } ?: return
        runCatching {
            app.assets.open(name).use { source ->
                markerFile.outputStream().use { target -> source.copyTo(target) }
            }
            if (markerFile.length() > 0L) hasDemoMarker = true else markerFile.delete()
        }
    }

    fun newFile(): File = File(directory, "${UUID.randomUUID()}.m4a")

    /** A local audio file is placed 200 m ahead of the trip's starting point. */
    fun importDemoMarker(uri: Uri): Result<Unit> = runCatching {
        val temp = File(directory, "demo_road_voice.tmp")
        try {
            val input = requireNotNull(app.contentResolver.openInputStream(uri)) { "음성 파일을 열 수 없어요." }
            input.use { source ->
                FileOutputStream(temp).use { target ->
                    val bytes = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val count = source.read(bytes)
                        if (count < 0) break
                        total += count
                        check(total <= 20L * 1024 * 1024) { "음성 파일은 20MB 이하로 선택해 주세요." }
                        target.write(bytes, 0, count)
                    }
                    check(total > 0L) { "선택한 음성 파일이 비어 있어요." }
                }
            }
            if (markerFile.exists()) check(markerFile.delete()) { "기존 음성 파일을 바꾸지 못했어요." }
            check(temp.renameTo(markerFile)) { "음성 파일을 저장하지 못했어요." }
            prefs.edit().putBoolean("marker_consumed", false).apply()
            hasDemoMarker = true
        } finally { temp.delete() }
    }

    fun demoMarkerFile(): File? = markerFile.takeIf { hasDemoMarker && it.isFile }

    /** A played marker stays hidden for the rest of this trip, then returns on a new trip. */
    fun rearmDemoMarkerForNewTrip(): Boolean {
        if (hasDemoMarker) return true
        prefs.edit().putBoolean("marker_consumed", false).apply()
        if (markerFile.isFile && markerFile.length() > 0L) {
            hasDemoMarker = true
        } else {
            importBundledDemoMarker()
        }
        return hasDemoMarker
    }

    fun resetBundledDemoMarker(): Boolean {
        val bundled = app.assets.list("")?.any {
            it in setOf("road_voice_demo.m4a", "road_voice_demo.mp3", "road_voice_demo.wav", "road_voice_demo.ogg")
        } == true
        if (!bundled) return false
        markerFile.delete()
        hasDemoMarker = false
        prefs.edit().putBoolean("marker_consumed", false).apply()
        importBundledDemoMarker()
        return hasDemoMarker
    }

    fun useAsDemoMarker(note: RoadVoiceNote) {
        file(note).copyTo(markerFile, overwrite = true)
        prefs.edit().putBoolean("marker_consumed", false).apply()
        hasDemoMarker = true
    }

    fun consumeDemoMarker() {
        prefs.edit().putBoolean("marker_consumed", true).apply()
        hasDemoMarker = false
    }

    fun add(file: File, progress: Float, point: GeoPoint?): RoadVoiceNote {
        require(file.parentFile?.canonicalFile == directory.canonicalFile && file.isFile)
        val note = RoadVoiceNote(file.nameWithoutExtension, file.name, progress.coerceIn(0f, 1f),
            point?.latitude, point?.longitude, System.currentTimeMillis())
        notes.add(note)
        persist()
        return note
    }

    fun file(note: RoadVoiceNote): File = File(directory, note.fileName)

    /** B/C are local simulated follower cars; no cross-vehicle transport is implied. */
    fun due(leaderProgress: Float): Pair<RoadVoiceNote, String>? {
        for (note in notes) {
            for ((car, gap) in listOf("B차" to .08f, "C차" to .15f)) {
                if (car !in note.playedBy && leaderProgress - gap >= note.anchorProgress && file(note).isFile)
                    return note to car
            }
        }
        return null
    }

    fun markPlayed(id: String, car: String) {
        val index = notes.indexOfFirst { it.id == id }
        if (index < 0) return
        notes[index] = notes[index].copy(playedBy = notes[index].playedBy + car)
        persist()
    }

    private fun persist() {
        val array = JSONArray()
        notes.forEach { note ->
            array.put(JSONObject().put("id", note.id).put("file", note.fileName)
                .put("progress", note.anchorProgress.toDouble())
                .put("latitude", note.latitude ?: JSONObject.NULL)
                .put("longitude", note.longitude ?: JSONObject.NULL)
                .put("createdAt", note.createdAt)
                .put("playedBy", JSONArray(note.playedBy.toList())))
        }
        prefs.edit().putString("notes", array.toString()).apply()
    }

    private fun load() {
        val raw = prefs.getString("notes", null) ?: return
        runCatching {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val name = item.getString("file")
                if (name != File(name).name || !File(directory, name).isFile) continue
                val played = item.optJSONArray("playedBy") ?: JSONArray()
                notes.add(RoadVoiceNote(item.getString("id"), name,
                    item.getDouble("progress").toFloat(),
                    item.optDouble("latitude").takeIf { it.isFinite() },
                    item.optDouble("longitude").takeIf { it.isFinite() },
                    item.getLong("createdAt"),
                    (0 until played.length()).map { played.getString(it) }.toSet()))
            }
        }
    }
}

internal class LinkCarOriginalAudio(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var player: MediaPlayer? = null
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var focusRequest: AudioFocusRequest? = null
    val isRecording get() = recorder != null
    val isPlaying get() = player != null

    @Suppress("DEPRECATION")
    fun start(file: File): Result<Unit> = runCatching {
        check(recorder == null) { "이미 녹음 중입니다" }
        file.parentFile?.mkdirs()
        val next = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
        try {
            next.setAudioSource(MediaRecorder.AudioSource.MIC)
            next.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            next.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            next.setAudioEncodingBitRate(64_000)
            next.setAudioSamplingRate(16_000)
            next.setOutputFile(file.absolutePath)
            next.prepare()
            next.start()
            recorder = next
            recordingFile = file
        } catch (error: Exception) {
            next.release()
            file.delete()
            throw error
        }
    }

    fun stopAndSave(): Result<File> = runCatching {
        val active = checkNotNull(recorder) { "녹음 중이 아닙니다" }
        recorder = null
        val file = checkNotNull(recordingFile)
        recordingFile = null
        try {
            active.stop()
            check(file.length() > 0L) { "녹음된 소리가 없습니다" }
            file
        } catch (error: Exception) {
            file.delete()
            throw error
        } finally {
            active.release()
        }
    }

    fun cancelRecording() {
        val active = recorder ?: return
        recorder = null
        runCatching { active.stop() }
        active.release()
        recordingFile?.delete()
        recordingFile = null
    }

    fun play(file: File, onDone: () -> Unit): Result<Unit> = runCatching {
        check(file.isFile) { "저장된 음성 파일이 없습니다" }
        check(player == null) { "이미 재생 중입니다" }
        val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attributes).build()
        check(audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            "오디오 재생 권한을 얻지 못했습니다"
        }
        focusRequest = request
        val next = MediaPlayer()
        try {
            next.setAudioAttributes(attributes)
            next.setDataSource(file.absolutePath)
            next.setOnCompletionListener {
                stopPlayback()
                onDone()
            }
            next.prepare()
            player = next
            next.start()
        } catch (error: Exception) {
            next.release()
            abandonFocus()
            throw error
        }
    }

    fun stopPlayback() {
        player?.release()
        player = null
        abandonFocus()
    }

    private fun abandonFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    fun release() {
        cancelRecording()
        stopPlayback()
    }
}
