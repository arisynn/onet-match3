package game.desktop

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import game.OnetMatch3Game
import game.screen.*
import game.session.AtomicSaveStore
import game.gameplay.onet.createOnetRound
import game.gameplay.shared.SeededRandom
import java.io.File
import java.nio.file.Files

/** Native libGDX screenshots; fixtures and saves are isolated from player data. */
fun main(args: Array<String>) {
    val output = File(args.getOrElse(0) { "build/ui-preview" }).apply { mkdirs() }
    val width = args.getOrElse(1) { "540" }.toInt()
    val height = args.getOrElse(2) { "1200" }.toInt()
    val screens = ScreenType.entries.take(17) + ScreenType.MAIN_MENU
    val game = OnetMatch3Game(store = AtomicSaveStore(Files.createTempDirectory("onet-ui-review").toFile()))
    val adapter = object : ApplicationAdapter() {
        var index = 0
        var frames = 0
        override fun create() {
            game.create()
            game.settings.reducedMotion = true
            game.startNewRound(ScreenType.ONET_GAME, 1)
            game.completeOnet(createOnetRound("candy", 1, SeededRandom("preview-win")).copy(phase = "won", score = 460))
            next()
        }
        fun next() {
            val type = screens[index]
            if (type == ScreenType.ONET_GAME) {
                game.selectedTheme = "ocean"
                game.selectedLevel = 4
            } else if (type == ScreenType.TRIPLE_MATCH_GAME) {
                game.selectedTheme = "candy"
                game.selectedLevel = 1
            }
            if (type == ScreenType.VICTORY || type == ScreenType.DEFEAT) {
                game.activeGameType = ScreenType.ONET_GAME
                game.selectedLevel = 1
            }
            if (index == screens.lastIndex) {
                game.startNewRound(ScreenType.ONET_GAME, 1)
                game.captureOnet(createOnetRound("candy", 1, SeededRandom("saved-preview")))
            }
            game.screenManager.setScreen(type)
            if (type == ScreenType.ONET_GAME) {
                (game.screenManager.currentScreen as OnetGameScreen).roundState = createOnetRound("ocean", 4, SeededRandom("ui-ocean"))
            }
            frames = 0
        }
        override fun resize(width: Int, height: Int) { game.resize(width, height) }
        override fun render() {
            // Hold screens steady: this is a visual fixture, not a gameplay simulation.
            game.screenManager.currentScreen?.renderStatic()
            if (++frames < 4) return
            val pixmap = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.backBufferWidth, Gdx.graphics.backBufferHeight)
            val name = if (index == screens.lastIndex) "MAIN_MENU_SAVED" else screens[index].name
            val file = File(output, "$name.png")
            try { PixmapIO.writePNG(Gdx.files.absolute(file.absolutePath), pixmap, -1, true) }
            finally { pixmap.dispose() }
            println("Captured ${screens[index]} ${Gdx.graphics.width}x${Gdx.graphics.height}")
            index++
            if (index == screens.size) Gdx.app.exit() else next()
        }
        override fun dispose() { game.dispose() }
    }
    Lwjgl3Application(adapter, Lwjgl3ApplicationConfiguration().apply {
        setTitle("Native UI review")
        setWindowedMode(width, height)
        setForegroundFPS(30)
        disableAudio(true)
        useVsync(false)
    })
}
