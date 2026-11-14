package com.contest.travel.preview

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** A single microphone owner saves PCM WAV; the same bytes can then be sent to Pleos STT. */
internal class LinkCarMicrophone {
    @Volatile private var capturing = false
    @Volatile private var failure: Exception? = null
    @Volatile var amplitude = 0f
        private set
    private var input: AudioRecord? = null
    private var worker: Thread? = null
    private var output: File? = null
    val isRecording get() = capturing

    @SuppressLint("MissingPermission") // Checked at the runtime entry point and requested by the UI.
    fun start(file: File): Result<Unit> = runCatching {
        check(input == null) { "이미 녹음 중입니다." }
        val minimum = AudioRecord.getMinBufferSize(16_000, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT)
        check(minimum > 0) { "마이크 입력을 사용할 수 없습니다." }
        val next = AudioRecord(MediaRecorder.AudioSource.MIC, 16_000,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minimum * 2, 6400))
        try {
            check(next.state == AudioRecord.STATE_INITIALIZED) { "마이크를 준비하지 못했습니다." }
            file.parentFile?.mkdirs()
            RandomAccessFile(file, "rw").use { it.setLength(0); it.write(ByteArray(44)) }
            next.startRecording()
            check(next.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "마이크 녹음을 시작하지 못했습니다." }
            input = next
            output = file
            failure = null
            capturing = true
            worker = Thread({
                try {
                    RandomAccessFile(file, "rw").use { stream ->
                        stream.seek(44)
                        val buffer = ByteArray(3200)
                        while (capturing) {
                            val count = next.read(buffer, 0, buffer.size)
                            if (count < 0) {
                                if (capturing) error("마이크 입력 오류 ($count)")
                                break
                            }
                            if (count == 0) continue
                            stream.write(buffer, 0, count)
                            var peak = 0
                            for (i in 0 until count - 1 step 2) {
                                val sample = ((buffer[i].toInt() and 255) or (buffer[i + 1].toInt() shl 8)).toShort()
                                peak = maxOf(peak, kotlin.math.abs(sample.toInt()))
                            }
                            amplitude = (peak / 32768f).coerceIn(0f, 1f)
                        }
                        val bytes = (stream.length() - 44).toInt()
                        stream.seek(0)
                        stream.write(wavHeader(bytes))
                    }
                } catch (error: Exception) { failure = error }
                finally { capturing = false }
            }, "LinkCar-microphone").also { it.start() }
        } catch (error: Exception) {
            next.release()
            file.delete()
            throw error
        }
    }

    fun stopAndSave(): Result<File> = runCatching {
        val active = checkNotNull(input) { "녹음 중이 아닙니다." }
        val file = checkNotNull(output)
        capturing = false
        runCatching { active.stop() }
        // stop() unblocks read(); join before releasing the recorder or inspecting the WAV header.
        worker?.join(2000)
        active.release()
        input = null
        output = null
        amplitude = 0f
        if (failure != null || file.length() <= 44L) file.delete()
        check(worker?.isAlive != true) { "마이크 녹음 종료를 확인하지 못했습니다." }
        worker = null
        failure?.let { throw it }
        check(file.length() > 44L) { "녹음된 음성이 없습니다." }
        file
    }.onFailure { output?.delete() }

    fun cancel() {
        val file = output
        if (input != null) stopAndSave()
        file?.delete()
    }
}

internal fun wavHeader(dataBytes: Int): ByteArray = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
    .put("RIFF".toByteArray()).putInt(36 + dataBytes).put("WAVEfmt ".toByteArray())
    .putInt(16).putShort(1).putShort(1).putInt(16_000).putInt(32_000)
    .putShort(2).putShort(16).put("data".toByteArray()).putInt(dataBytes).array()
