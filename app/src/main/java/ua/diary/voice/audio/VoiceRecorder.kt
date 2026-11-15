package ua.diary.voice.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import java.io.File

/** Записує голос у файл .m4a (AAC) у внутрішній пам'яті додатка. */
class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var startedAt = 0L
    private var pausedAt = 0L
    private var pausedTotal = 0L

    val isRecording get() = recorder != null
    var isPaused = false
        private set

    fun pause() {
        val r = recorder ?: return
        if (isPaused) return
        runCatching { r.pause() }.onSuccess {
            isPaused = true
            pausedAt = SystemClock.elapsedRealtime()
        }
    }

    fun resume() {
        val r = recorder ?: return
        if (!isPaused) return
        runCatching { r.resume() }.onSuccess {
            pausedTotal += SystemClock.elapsedRealtime() - pausedAt
            isPaused = false
        }
    }

    fun start(): File {
        stopAndDiscard()
        val dir = File(context.filesDir, "voice").apply { mkdirs() }
        val out = File(dir, "voice_${System.currentTimeMillis()}.m4a")
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
        else @Suppress("DEPRECATION") MediaRecorder()
        try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(96_000)
            r.setAudioSamplingRate(44_100)
            r.setOutputFile(out.absolutePath)
            r.prepare()
            r.start()
        } catch (e: Exception) {
            r.release()
            out.delete()
            throw e
        }
        recorder = r
        file = out
        startedAt = SystemClock.elapsedRealtime()
        pausedTotal = 0L
        isPaused = false
        return out
    }

    /** Тривалість запису без пауз. */
    fun elapsedMs(): Long {
        if (!isRecording) return 0L
        val now = if (isPaused) pausedAt else SystemClock.elapsedRealtime()
        return now - startedAt - pausedTotal
    }

    /** Зупиняє запис і повертає файл з тривалістю, або null, якщо запис не вдався (наприклад, надто короткий). */
    fun stop(): Pair<File, Long>? {
        val r = recorder ?: return null
        val out = file
        val duration = elapsedMs()
        recorder = null
        file = null
        isPaused = false
        return try {
            r.stop()
            r.release()
            out?.let { it to duration }
        } catch (e: RuntimeException) {
            r.release()
            out?.delete()
            null
        }
    }

    fun stopAndDiscard() {
        stop()?.first?.delete()
    }
}
