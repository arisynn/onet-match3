package game.screen

import game.OnetMatch3Game
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SlowFrameTimerTest {
    @Test fun `slow frames preserve elapsed gameplay time instead of extending the timer`() {
        val screen = OnetGameScreen(OnetMatch3Game())
        val initial = screen.roundState.time
        screen.render(0.75f)
        screen.render(0.75f)
        assertEquals(initial - 1, screen.roundState.time)
        screen.pause()
        screen.render(2f)
        assertEquals(initial - 1, screen.roundState.time)
    }
}
