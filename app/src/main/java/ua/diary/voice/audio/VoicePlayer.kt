package ua.diary.voice.audio

import android.media.MediaPlayer

/** Відтворює одну нотатку за раз. */
class VoicePlayer {
    private var player: MediaPlayer? = null
    var playingPath: String? = null
        private set

    fun play(path: String, onComplete: () -> Unit) {
        stop()
        val p = MediaPlayer()
        try {
            p.setDataSource(path)
            p.setOnCompletionListener {
                stop()
                onComplete()
            }
            p.prepare()
            p.start()
            player = p
            playingPath = path
        } catch (e: Exception) {
            p.release()
            onComplete()
        }
    }

    fun positionMs(): Long = player?.currentPosition?.toLong() ?: 0L

    fun stop() {
        player?.run {
            runCatching { stop() }
            release()
        }
        player = null
        playingPath = null
    }
}
