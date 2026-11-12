package com.onetmatch3.game

import android.os.Bundle
import android.widget.FrameLayout
import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import game.OnetMatch3Game

class AndroidLauncher : AndroidApplication() {
    private var console: DebugConsole? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this)
        setContentView(root)
        console = DebugConsole(this, root)
        console?.log("AndroidLauncher onCreate")
        val config = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true; useWakelock = true; useGL30 = false
            useCompass = false; useAccelerometer = false
        }
        try {
            val game = OnetMatch3Game { console?.log(it) }
            val listener = object : ApplicationListener {
                override fun create() { checked { game.create() } }
                override fun render() { checked { game.render() } }
                override fun resize(width: Int, height: Int) { console?.log("Surface resize ${width}x$height"); checked { game.resize(width, height) } }
                override fun pause() { checked { game.pause() } }
                override fun resume() { checked { game.resume() } }
                override fun dispose() { checked { game.dispose() } }
            }
            val surface = initializeForView(listener, config)
            root.addView(surface, 0, FrameLayout.LayoutParams(-1, -1))
            console?.log("libGDX surface attached; OpenGL ES 2 requested")
        } catch (error: Throwable) { console?.failure(error); throw error }
    }
    private fun checked(action: () -> Unit) {
        try { action() } catch (error: Throwable) { console?.failure(error); throw error }
    }
    override fun onResume() { super.onResume(); console?.log("AndroidLauncher onResume") }
    override fun onPause() { console?.log("AndroidLauncher onPause"); super.onPause() }
    override fun onDestroy() { console?.log("AndroidLauncher onDestroy"); super.onDestroy(); console?.close() }
}
