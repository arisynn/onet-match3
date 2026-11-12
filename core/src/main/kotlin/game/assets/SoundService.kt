package game.assets

import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound

/**
 * Manages game sound effects and background music playback, volume curves, and mute states.
 */
class SoundService(
    private val assetService: AssetService
) {
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

    fun playSound(path: String, volumeScale: Float = 1.0f, pitch: Float = 1.0f): Long? {
        if (isSfxMuted || !assetService.isLoaded(path)) return null
        return try {
            val sound = assetService.getSound(path)
            val effectiveVolume = (sfxVolume * volumeScale).coerceIn(0f, 1f)
            sound.play(effectiveVolume, pitch, 0f)
        } catch (_: Exception) {
            null
        }
    }

    fun playMusic(path: String, loop: Boolean = true) {
        if (!assetService.isLoaded(path)) return
        try {
            val music = assetService.getMusic(path)
            if (currentMusic != music) {
                currentMusic?.stop()
                currentMusic = music
            }
            music.isLooping = loop
            music.volume = if (isMusicMuted) 0f else musicVolume
            if (!music.isPlaying) {
                music.play()
            }
        } catch (_: Exception) {
            // Ignore audio initialization failure on unsupported devices
        }
    }

    fun pauseMusic() {
        currentMusic?.pause()
    }

    fun resumeMusic() {
        if (!isMusicMuted && currentMusic?.isPlaying == false) {
            currentMusic?.play()
        }
    }

    fun stopMusic() {
        currentMusic?.stop()
        currentMusic = null
    }
}
