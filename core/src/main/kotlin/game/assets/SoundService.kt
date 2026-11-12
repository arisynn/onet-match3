package game.assets

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import game.platform.GamePlatform
import game.platform.HapticEffect
import game.platform.NoopGamePlatform

enum class SoundCue(val file: String, val haptic: HapticEffect?) {
    SELECT("select", HapticEffect.SELECT), MATCH("match", HapticEffect.MATCH),
    INVALID("invalid", HapticEffect.INVALID), COMBO("combo", HapticEffect.MATCH),
    VICTORY("victory", HapticEffect.CELEBRATE), DEFEAT("defeat", null),
    MISSION_CLAIM("claim", HapticEffect.CELEBRATE), ACHIEVEMENT("achievement", HapticEffect.CELEBRATE),
    COSMETIC_PREVIEW("preview", null), LEVEL_UP("level-up", HapticEffect.CELEBRATE),
    UNDO("undo", HapticEffect.SELECT), SHUFFLE("shuffle", HapticEffect.SELECT)
}

/** Replaceable audio source, allowing lifecycle tests without a device audio driver. */
interface AudioAssets {
    val available: Boolean
    fun queueSound(path: String)
    fun queueMusic(path: String)
    fun update()
    fun loaded(path: String): Boolean
    fun sound(path: String): Sound
    fun music(path: String): Music
}

private class ManagedAudioAssets(private val service: AssetService) : AudioAssets {
    override val available get() = Gdx.audio != null && Gdx.files != null &&
        Gdx.app?.type != Application.ApplicationType.HeadlessDesktop
    override fun queueSound(path: String) = service.loadSound(path)
    override fun queueMusic(path: String) = service.loadMusic(path)
    override fun update() { service.update() }
    override fun loaded(path: String) = service.isLoaded(path)
    override fun sound(path: String) = service.getSound(path)
    override fun music(path: String) = service.getMusic(path)
}

/** Muting and lifecycle pausing are independent; backgrounded music never restarts on a toggle. */
class SoundService(
    private val assets: AudioAssets,
    private val platform: GamePlatform = NoopGamePlatform
) {
    constructor(service: AssetService, platform: GamePlatform = NoopGamePlatform) :
        this(ManagedAudioAssets(service), platform)

    var isSfxMuted = false
    var isMusicMuted = false
        set(value) { field = value; applyMusicState() }
    var hapticsEnabled = true
    var reducedEffects = false
    var sfxVolume = 0.7f
        set(value) { field = safeVolume(value) }
    var musicVolume = 0.22f
        set(value) { field = safeVolume(value); applyMusicState() }
    private var lifecyclePaused = false
    private var currentMusic: Music? = null
    private var musicWanted = false
    private var defaultAudioQueued = false
    private var defaultMusicStarted = false
    private var lastFeedbackAt = Long.MIN_VALUE

    fun queueDefaultAudio() {
        if (defaultAudioQueued || !assets.available) return
        defaultAudioQueued = true
        SoundCue.entries.forEach { assets.queueSound("audio/${it.file}.wav") }
        assets.queueMusic(MUSIC_PATH)
    }

    /** Nonblocking asset loading. BGM is streamed by Music, not decoded as a Sound. */
    fun update() {
        if (!defaultAudioQueued) return
        try {
            assets.update()
            if (!defaultMusicStarted && assets.loaded(MUSIC_PATH)) {
                defaultMusicStarted = true
                playMusic(MUSIC_PATH)
            }
        } catch (error: RuntimeException) {
            defaultAudioQueued = false
            Gdx.app?.error("Audio", "Audio loading failed; continuing without audio", error)
        }
    }

    fun feedback(cue: SoundCue) {
        val now = platform.monotonicTimeMillis()
        // Bound rapid tap spam without delaying gameplay or coalescing important results.
        if (cue == SoundCue.SELECT && lastFeedbackAt != Long.MIN_VALUE && now - lastFeedbackAt < 35) return
        lastFeedbackAt = now
        if (!(reducedEffects && cue == SoundCue.COMBO)) playSound("audio/${cue.file}.wav")
        if (hapticsEnabled && !(reducedEffects && cue == SoundCue.COMBO)) cue.haptic?.let(platform::haptic)
    }

    fun playSound(path: String, volumeScale: Float = 1f, pitch: Float = 1f): Long? {
        val internalPath = normalize(path)
        if (lifecyclePaused || isSfxMuted || !assets.loaded(internalPath)) return null
        return try {
            assets.sound(internalPath).play(safeVolume(sfxVolume * volumeScale),
                if (pitch.isFinite()) pitch.coerceIn(0.5f, 2f) else 1f, 0f)
        } catch (_: RuntimeException) { null }
    }

    fun playMusic(path: String, loop: Boolean = true) {
        val internalPath = normalize(path)
        if (!assets.loaded(internalPath)) return
        try {
            val music = assets.music(internalPath)
            if (currentMusic !== music) { currentMusic?.stop(); currentMusic = music }
            musicWanted = true
            music.isLooping = loop
            applyMusicState()
        } catch (_: RuntimeException) { currentMusic = null }
    }

    private fun applyMusicState() {
        val music = currentMusic ?: return
        try {
            music.volume = if (isMusicMuted) 0f else musicVolume
            if (musicWanted && !lifecyclePaused && !isMusicMuted) {
                if (!music.isPlaying) music.play()
            } else if (music.isPlaying) music.pause()
        } catch (_: RuntimeException) { /* Hardware audio loss must not terminate a round. */ }
    }

    fun pauseMusic() { lifecyclePaused = true; applyMusicState() }
    fun resumeMusic() { lifecyclePaused = false; applyMusicState() }
    fun stopMusic() { musicWanted = false; currentMusic?.stop(); currentMusic = null }

    private fun normalize(path: String): String {
        val canonical = path.removePrefix("assets/")
        if (!canonical.startsWith("audio/")) return canonical
        val stem = canonical.substringAfterLast('/').substringBeforeLast('.')
        val alias = when (stem) { "pick" -> "select"; "buy" -> "claim"; else -> stem }
        return "audio/$alias.wav"
    }

    private fun safeVolume(value: Float) = if (value.isFinite()) value.coerceIn(0f, 1f) else 0f
    companion object { const val MUSIC_PATH = "audio/garden-loop.wav" }
}
