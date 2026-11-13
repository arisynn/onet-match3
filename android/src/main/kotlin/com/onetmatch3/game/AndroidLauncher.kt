package com.onetmatch3.game

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import game.OnetMatch3Game

/**
 * Android mobile entry point bootstrapping the libGDX runtime.
 */
class AndroidLauncher : AndroidApplication() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
            useWakelock = true
            useGL30 = false
            useCompass = false
            useAccelerometer = false
        }

        val device = AndroidGamePlatform(this)
        val appGame = OnetMatch3Game(platform = device)
        initialize(appGame, config)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(graphics.view) { _, insets ->
            val safe = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars() or
                androidx.core.view.WindowInsetsCompat.Type.displayCutout())
            device.safeInsets = game.platform.SafeInsets(safe.left, safe.top, safe.right, safe.bottom)
            postRunnable { appGame.resize(graphics.width, graphics.height) }
            insets
        }
        androidx.core.view.ViewCompat.requestApplyInsets(graphics.view)
    }
}
