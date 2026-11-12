package game.screen

import game.OnetMatch3Game
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StartupTransitionTest {
    @Test
    fun `repeated navigation from update cannot restart an active transition`() {
        val manager = ScreenManager(OnetMatch3Game())
        var loadingCreated = 0
        var bootDisposed = false
        manager.registerScreen(ScreenType.BOOT) {
            object : BaseScreen(manager.game) {
                override fun update(delta: Float) { manager.setScreen(ScreenType.LOADING, FadeTransition(.3f)) }
                override fun draw(delta: Float) {}
                override fun onDispose() { bootDisposed = true }
            }
        }
        manager.registerScreen(ScreenType.LOADING) {
            loadingCreated++
            object : BaseScreen(manager.game) {
                override fun update(delta: Float) {}
                override fun draw(delta: Float) {}
            }
        }
        manager.setScreen(ScreenType.BOOT)
        repeat(60) { manager.render(1f / 60f) }
        assertEquals(ScreenType.LOADING, manager.currentScreenType)
        assertEquals(1, loadingCreated)
        assertTrue(bootDisposed)
        manager.dispose()
    }
}
