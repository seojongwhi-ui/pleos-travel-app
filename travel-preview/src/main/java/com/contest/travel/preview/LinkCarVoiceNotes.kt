package com.contest.travel.preview

import android.content.Context
import android.net.Uri
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.security.MessageDigest

internal const val DayeonRoadVoiceAsset = "dayeon_road_message.m4a"
private const val RoadMarkerNarrationAsset = "road_marker_ai.mp3"
// Upgrade earlier bundled markers only. History keeps Dayeon's original recording;
// a marker imported/recorded by the user stays intact.
private val LegacyRoadVoiceSha256 = setOf(
    "3b1a05ebc223357eaee3aa677d16cbb3fbdd73024091cbbae9d2d15b484f3f93", // original demo WAV
    "ba564ca6cfdd275bc71557204cb4d1ab7e04dc8d460cfe30ba1724b922cb8e12" // Dayeon's recording
)

internal data class RoadVoiceNote(
    val id: String,
    val fileName: String,
    val anchorProgress: Float,
    val latitude: Double?,
    val longitude: Double?,
    val createdAt: Long,
    val playedBy: Set<String> = emptySet(),
    val kind: String = "road"
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
        runCatching {
            if (markerFile.isFile && markerFile.length() > 0L) {
                val digest = MessageDigest.getInstance("SHA-256")
                markerFile.inputStream().use { source ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = source.read(buffer)
                        if (count < 0) break
                        digest.update(buffer, 0, count)
                    }
                }
                if (digest.digest().joinToString("") { "%02x".format(it) } !in LegacyRoadVoiceSha256)
                    return
            }
            val temp = File(directory, "bundled_road_voice.tmp")
            try {
                app.assets.open(RoadMarkerNarrationAsset).use { source ->
                    temp.outputStream().use { target -> source.copyTo(target) }
                }
                check(temp.length() > 0L && temp.renameTo(markerFile))
                hasDemoMarker = !prefs.getBoolean("marker_consumed", false)
            } finally { temp.delete() }
        }
    }

    fun newFile(): File = File(directory, "${UUID.randomUUID()}.wav")

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
        val bundled = app.assets.list("")?.contains(RoadMarkerNarrationAsset) == true
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

    fun add(file: File, progress: Float, point: GeoPoint?, kind: String = "road"): RoadVoiceNote {
        require(file.parentFile?.canonicalFile == directory.canonicalFile && file.isFile)
        val note = RoadVoiceNote(file.nameWithoutExtension, file.name, progress.coerceIn(0f, 1f),
            point?.latitude, point?.longitude, System.currentTimeMillis(), kind = kind)
        notes.add(note)
        persist()
        return note
    }

    fun file(note: RoadVoiceNote): File = File(directory, note.fileName)

    /** B/C are local simulated follower cars; no cross-vehicle transport is implied. */
    fun due(leaderProgress: Float): Pair<RoadVoiceNote, String>? {
        for (note in notes.filter { it.kind == "road" }) {
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
                .put("createdAt", note.createdAt).put("kind", note.kind)
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
                    (0 until played.length()).map { played.getString(it) }.toSet(),
                    item.optString("kind", "road")))
            }
        }
    }
}

internal class LinkCarOriginalAudio(private val context: Context) {
    private val microphone = LinkCarMicrophone()
    private var player: MediaPlayer? = null
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var focusRequest: AudioFocusRequest? = null
    val isRecording get() = microphone.isRecording
    val amplitude get() = microphone.amplitude
    var playingKey by androidx.compose.runtime.mutableStateOf<String?>(null)
        private set
    val isPlaying get() = playingKey != null

    fun start(file: File): Result<Unit> {
        stopPlayback()
        return microphone.start(file)
    }
    fun stopAndSave(): Result<File> = microphone.stopAndSave()
    fun cancelRecording() = microphone.cancel()

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
            next.setOnErrorListener { _, _, _ -> stopPlayback(); onDone(); true }
            next.start()
            playingKey = file.name
        } catch (error: Exception) {
            next.release()
            abandonFocus()
            throw error
        }
    }

    fun stopPlayback() {
        player?.release()
        player = null
        playingKey = null
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
