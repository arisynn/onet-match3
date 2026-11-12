package game.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import game.OnetMatch3Game

/**
 * Desktop application bootstrap using LWJGL3 backend for rapid development and testing.
 */
fun main() {
    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle("Onet + Triple Match")
        setWindowedMode(540, 960)
        setForegroundFPS(60)
        useVsync(true)
    }

    Lwjgl3Application(OnetMatch3Game(), config)
}
