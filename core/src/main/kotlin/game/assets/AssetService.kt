package game.assets

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas

/**
 * Centralized asset management service wrapping libGDX AssetManager.
 * Manages lifecycle, loading progress, and retrieval of visual & audio assets.
 */
class AssetService(
    val manager: AssetManager = AssetManager(),
    val diagnostics: (String) -> Unit = {}
) {

    val progress: Float
        get() = manager.progress

    val isFinished: Boolean
        get() = manager.isFinished

    fun update(): Boolean {
        return try { manager.update() } catch (error: Throwable) {
            diagnostics("ERROR asset loading: ${error.stackTraceToString()}")
            throw error
        }
    }

    fun finishLoading() {
        manager.finishLoading()
    }

    // --- Loading Queues ---

    fun loadTexture(path: String) {
        if (!manager.isLoaded(path, Texture::class.java)) {
            diagnostics("Asset queued: $path")
            manager.load(path, Texture::class.java)
        }
    }

    fun loadAtlas(path: String) {
        if (!manager.isLoaded(path, TextureAtlas::class.java)) {
            diagnostics("Asset queued: $path")
            manager.load(path, TextureAtlas::class.java)
        }
    }

    fun loadSound(path: String) {
        if (!manager.isLoaded(path, Sound::class.java)) {
            diagnostics("Asset queued: $path")
            manager.load(path, Sound::class.java)
        }
    }

    fun loadMusic(path: String) {
        if (!manager.isLoaded(path, Music::class.java)) {
            diagnostics("Asset queued: $path")
            manager.load(path, Music::class.java)
        }
    }

    // --- Asset Getters ---

    fun getTexture(path: String): Texture {
        return manager.get(path, Texture::class.java)
    }

    fun getAtlas(path: String): TextureAtlas {
        return manager.get(path, TextureAtlas::class.java)
    }

    fun getSound(path: String): Sound {
        return manager.get(path, Sound::class.java)
    }

    fun getMusic(path: String): Music {
        return manager.get(path, Music::class.java)
    }

    fun isLoaded(path: String): Boolean {
        return manager.isLoaded(path)
    }

    fun unload(path: String) {
        if (manager.isLoaded(path)) {
            manager.unload(path)
        }
    }

    fun dispose() {
        manager.dispose()
    }
}
