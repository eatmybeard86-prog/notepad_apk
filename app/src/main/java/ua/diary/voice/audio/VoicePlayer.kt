package ua.diary.voice.audio

import android.media.MediaPlayer

/** Відтворює одну нотатку за раз: пауза, продовження, перемотка. */
class VoicePlayer {
    private var player: MediaPlayer? = null
    var playingPath: String? = null
        private set

    val isActive get() = player != null
    val isPlaying get() = runCatching { player?.isPlaying == true }.getOrDefault(false)

    fun play(path: String, onComplete: () -> Unit): Boolean {
        stop()
        val p = MediaPlayer()
        return try {
            p.setDataSource(path)
            p.setOnCompletionListener { onComplete() }
            p.prepare()
            p.start()
            player = p
            playingPath = path
            true
        } catch (e: Exception) {
            p.release()
            false
        }
    }

    fun pause() { runCatching { player?.pause() } }

    fun resume() { runCatching { player?.start() } }

    fun seekTo(ms: Long) {
        val p = player ?: return
        runCatching { p.seekTo(ms.coerceIn(0, p.duration.toLong()).toInt()) }
    }

    fun positionMs(): Long = runCatching { player?.currentPosition?.toLong() }.getOrNull() ?: 0L
    fun durationMs(): Long = runCatching { player?.duration?.toLong() }.getOrNull() ?: 0L

    fun stop() {
        player?.run {
            runCatching { stop() }
            release()
        }
        player = null
        playingPath = null
    }
}
