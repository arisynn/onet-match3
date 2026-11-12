package game.assets

import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import game.platform.GamePlatform
import game.platform.HapticEffect
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy

class SoundServiceTest {
    private class FakePlatform : GamePlatform {
        var now = 1000L
        val haptics = mutableListOf<HapticEffect>()
        override fun monotonicTimeMillis() = now
        override fun haptic(effect: HapticEffect) { haptics += effect }
    }
    private class FakeAudio : AudioAssets {
        override val available = true
        val queuedSounds = mutableSetOf<String>()
        val queuedMusic = mutableSetOf<String>()
        var playing = false
        var plays = 0
        var soundPlays = 0
        var volume = 1f
        var updateCount = 0
        private val music = Proxy.newProxyInstance(Music::class.java.classLoader, arrayOf(Music::class.java)) { _, method, args ->
            when (method.name) {
                "play" -> { playing = true; plays++; null }
                "pause", "stop" -> { playing = false; null }
                "isPlaying" -> playing
                "setVolume" -> { volume = args!![0] as Float; null }
                "getVolume" -> volume
                "isLooping" -> true
                else -> null
            }
        } as Music
        private val sound = Proxy.newProxyInstance(Sound::class.java.classLoader, arrayOf(Sound::class.java)) { _, method, _ ->
            if (method.name == "play") { soundPlays++; 1L } else null
        } as Sound
        override fun queueSound(path: String) { queuedSounds += path }
        override fun queueMusic(path: String) { queuedMusic += path }
        override fun update() { updateCount++ }
        override fun loaded(path: String) = path in queuedSounds || path in queuedMusic
        override fun sound(path: String) = sound
        override fun music(path: String) = music
    }

    @Test fun `music toggles respect background lifecycle and current volume`() {
        val assets = FakeAudio()
        val service = SoundService(assets)
        service.queueDefaultAudio(); service.queueDefaultAudio(); service.update()
        assertEquals(SoundCue.entries.size, assets.queuedSounds.size)
        assertEquals(setOf(SoundService.MUSIC_PATH), assets.queuedMusic)
        assertEquals(1, assets.plays)
        service.isMusicMuted = true
        assertFalse(assets.playing)
        assertEquals(0f, assets.volume)
        service.pauseMusic()
        service.isMusicMuted = false
        assertFalse(assets.playing, "unmuting in background must not restart playback")
        service.musicVolume = 0.4f
        service.resumeMusic()
        assertTrue(assets.playing)
        assertEquals(0.4f, assets.volume)
        service.stopMusic(); service.resumeMusic()
        assertFalse(assets.playing)
    }

    @Test fun `muted feedback still allows optional haptics and rapid taps are bounded`() {
        val assets = FakeAudio()
        val platform = FakePlatform()
        val service = SoundService(assets, platform)
        service.queueDefaultAudio()
        service.feedback(SoundCue.SELECT); service.feedback(SoundCue.SELECT)
        assertEquals(1, assets.soundPlays)
        assertEquals(1, platform.haptics.size)
        service.isSfxMuted = true
        platform.now += 100
        service.feedback(SoundCue.MATCH)
        assertEquals(1, assets.soundPlays)
        assertEquals(2, platform.haptics.size)
        service.hapticsEnabled = false
        service.feedback(SoundCue.VICTORY)
        assertEquals(2, platform.haptics.size)
    }

    @Test fun `legacy paths resolve actual WAV files and invalid volume is safe`() {
        val assets = FakeAudio()
        val service = SoundService(assets)
        service.queueDefaultAudio()
        assertNotNull(service.playSound("assets/audio/pick.ogg"))
        assertNotNull(service.playSound("assets/audio/match.ogg"))
        service.sfxVolume = Float.NaN
        service.musicVolume = Float.POSITIVE_INFINITY
        assertEquals(0f, service.sfxVolume)
        assertEquals(0f, service.musicVolume)
        service.pauseMusic()
        assertNull(service.playSound("audio/select.wav"))
    }
}
