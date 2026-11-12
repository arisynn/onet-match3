package game.screen

import game.OnetMatch3Game
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ScreenNavigationRegressionTest {
    private class CountingScreen(game: OnetMatch3Game) : BaseScreen(game) {
        var updates = 0
        var resumes = 0
        override fun update(delta: Float) { updates++ }
        override fun draw(delta: Float) {}
        override fun resume() { resumes++ }
    }

    @Test fun `animated nested modals retain original instances and do not invoke factories on pop`() {
        val game = OnetMatch3Game()
        val manager = ScreenManager(game)
        var factoryCalls = 0
        manager.registerScreen(ScreenType.ONET_GAME) { factoryCalls++; CountingScreen(game) }
        manager.registerScreen(ScreenType.PAUSE) { factoryCalls++; CountingScreen(game) }
        manager.registerScreen(ScreenType.SETTINGS) { factoryCalls++; CountingScreen(game) }
        manager.setScreen(ScreenType.ONET_GAME)
        val board = manager.currentScreen as CountingScreen
        fun finishFade() { repeat(5) { manager.render(0.1f) } }
        manager.pushScreen(ScreenType.PAUSE, FadeTransition(0.3f))
        // Duplicate input while a transition is active must not instantiate another modal.
        manager.pushScreen(ScreenType.PAUSE, FadeTransition(0.3f))
        finishFade()
        val pause = manager.currentScreen
        assertFalse(board.isDisposed)
        assertEquals(2, factoryCalls)
        assertEquals(0, board.updates)
        manager.pushScreen(ScreenType.SETTINGS, FadeTransition(0.3f))
        finishFade()
        manager.popScreen(FadeTransition(0.3f)); finishFade()
        assertSame(pause, manager.currentScreen)
        assertFalse(pause!!.isDisposed)
        manager.popScreen(FadeTransition(0.3f)); finishFade()
        assertSame(board, manager.currentScreen)
        assertEquals(ScreenType.ONET_GAME, manager.currentScreenType)
        assertFalse(board.isDisposed)
        assertEquals(1, board.resumes)
        assertEquals(3, factoryCalls)
        manager.dispose()
        assertTrue(board.isDisposed)
    }

    @Test fun `leaving a modal flow disposes retained screens`() {
        val manager = ScreenManager(OnetMatch3Game())
        manager.registerScreen(ScreenType.ONET_GAME) { CountingScreen(manager.game) }
        manager.registerScreen(ScreenType.PAUSE) { CountingScreen(manager.game) }
        manager.registerScreen(ScreenType.MAIN_MENU) { CountingScreen(manager.game) }
        manager.setScreen(ScreenType.ONET_GAME)
        val board = manager.currentScreen!!
        manager.pushScreen(ScreenType.PAUSE)
        val pause = manager.currentScreen!!
        manager.setScreen(ScreenType.MAIN_MENU, FadeTransition(0.2f))
        repeat(3) { manager.render(0.1f) }
        assertTrue(board.isDisposed)
        assertTrue(pause.isDisposed)
        assertFalse(manager.popScreen())
        manager.dispose()
    }

    @Test fun `gameplay resumes after platform or modal pause`() {
        val game = OnetMatch3Game()
        val onet = OnetGameScreen(game)
        onet.pause(); assertTrue(onet.roundState.paused)
        onet.resume(); assertFalse(onet.roundState.paused)
        val triple = TripleMatchGameScreen(game)
        triple.pause(); assertTrue(triple.roundState.paused)
        triple.resume(); assertFalse(triple.roundState.paused)
    }
}
