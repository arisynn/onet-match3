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

        initialize(OnetMatch3Game(), config)
    }
}
