package game.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.utils.Disposable

/**
 * Manages game sound effects and background music playback, volume curves, and mute states.
 */
class SoundService(
    private val assetService: AssetService
) : Disposable {
    var isSfxMuted: Boolean = false
    var isMusicMuted: Boolean = false

    var sfxVolume: Float = 1.0f
        set(value) {
            field = value.coerceIn(0f, 1f)
        }

    var musicVolume: Float = 0.8f
        set(value) {
            field = value.coerceIn(0f, 1f)
            currentMusic?.volume = if (isMusicMuted) 0f else field
        }

    private var currentMusic: Music? = null
    private val soundCache = mutableMapOf<String, Sound>()
    private val musicCache = mutableMapOf<String, Music>()

    fun normalizeAudioPath(path: String): String {
        var p = if (path.startsWith("assets/")) path.removePrefix("assets/") else path
        if (Gdx.files != null) {
            if (!Gdx.files.internal(p).exists()) {
                if (p.endsWith(".ogg")) {
                    val wav = p.removeSuffix(".ogg") + ".wav"
                    if (Gdx.files.internal(wav).exists()) p = wav
                } else if (p.endsWith(".wav")) {
                    val ogg = p.removeSuffix(".wav") + ".ogg"
                    if (Gdx.files.internal(ogg).exists()) p = ogg
                }
            }
        }
        return p
    }

    fun playSound(path: String, volumeScale: Float = 1.0f, pitch: Float = 1.0f): Long? {
        if (isSfxMuted) return null
        val normalized = normalizeAudioPath(path)
        return try {
            val sound = soundCache.getOrPut(normalized) {
                if (assetService.isLoaded(normalized)) {
                    assetService.getSound(normalized)
                } else if (Gdx.audio != null && Gdx.files != null && Gdx.files.internal(normalized).exists()) {
                    Gdx.audio.newSound(Gdx.files.internal(normalized))
                } else {
                    return null
                }
            }
            val effectiveVolume = (sfxVolume * volumeScale).coerceIn(0f, 1f)
            sound.play(effectiveVolume, pitch, 0f)
        } catch (_: Exception) {
            null
        }
    }

    fun playMusic(path: String, loop: Boolean = true) {
        val normalized = normalizeAudioPath(path)
        try {
            val music = musicCache.getOrPut(normalized) {
                if (assetService.isLoaded(normalized)) {
                    assetService.getMusic(normalized)
                } else if (Gdx.audio != null && Gdx.files != null && Gdx.files.internal(normalized).exists()) {
                    Gdx.audio.newMusic(Gdx.files.internal(normalized))
                } else {
                    return
                }
            }
            if (currentMusic != music) {
                currentMusic?.stop()
                currentMusic = music
            }
            music.isLooping = loop
            music.volume = if (isMusicMuted) 0f else musicVolume
            if (!music.isPlaying && !isMusicMuted) {
                music.play()
            }
        } catch (_: Exception) {
            // Ignore audio initialization failure on unsupported devices or headless
        }
    }

    fun pauseMusic() {
        try {
            currentMusic?.pause()
        } catch (_: Exception) {}
    }

    fun resumeMusic() {
        try {
            if (!isMusicMuted && currentMusic?.isPlaying == false) {
                currentMusic?.play()
            }
        } catch (_: Exception) {}
    }

    fun stopMusic() {
        try {
            currentMusic?.stop()
            currentMusic = null
        } catch (_: Exception) {}
    }

    override fun dispose() {
        stopMusic()
        soundCache.values.forEach {
            try { it.dispose() } catch (_: Exception) {}
        }
        soundCache.clear()
        musicCache.values.forEach {
            try { it.dispose() } catch (_: Exception) {}
        }
        musicCache.clear()
    }
}
