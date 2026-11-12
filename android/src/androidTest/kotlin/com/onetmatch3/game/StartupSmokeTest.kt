package com.onetmatch3.game

import android.graphics.Bitmap
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector2
import game.OnetMatch3Game
import game.screen.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Runs with real Android assets, input, EGL and rendering; blank screens fail this test. */
@RunWith(AndroidJUnit4::class)
class StartupSmokeTest {
    private lateinit var activity: AndroidLauncher
    private lateinit var game: OnetMatch3Game

    private fun onGl(action: () -> Unit) {
        val done = CountDownLatch(1)
        val failure = AtomicReference<Throwable?>()
        activity.postRunnable {
            try { action() } catch (t: Throwable) { failure.set(t) } finally { done.countDown() }
        }
        assertTrue("GL thread did not respond", done.await(15, TimeUnit.SECONDS))
        failure.get()?.let { throw AssertionError("GL assertion failed", it) }
    }

    private fun awaitScreen(type: ScreenType) {
        val deadline = System.currentTimeMillis() + 15000
        var ready = false
        while (!ready && System.currentTimeMillis() < deadline) {
            onGl { ready = game.screenManager.currentScreenType == type && !game.screenManager.isTransitioning }
            if (!ready) Thread.sleep(50)
        }
        assertTrue("Did not reach $type", ready)
    }

    private fun tap(x: Float, y: Float) {
        onGl {
            val screen = game.screenManager.currentScreen!!
            val point = screen.viewport.project(Vector2(x, y))
            screen.inputMultiplexer.touchUp(point.x.toInt(), Gdx.graphics.height - point.y.toInt(), 0, 0)
        }
    }

    private fun screenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val deadline = System.currentTimeMillis() + 20000
        var colors = 0
        var lightPixels = 0
        var buttonPixels = 0
        // A resumed Activity may precede the first SurfaceView buffer reaching the
        // compositor. Wait for actual visible content, with a bounded failure time.
        do {
            val bitmap = instrumentation.uiAutomation.takeScreenshot()
            assertNotNull("Cannot capture Android surface", bitmap)
            val distinct = HashSet<Int>()
            lightPixels = 0
            buttonPixels = 0
            for (y in 0 until bitmap.height step 3) for (x in 0 until bitmap.width step 3) {
                val c = bitmap.getPixel(x, y)
                distinct.add(c)
                if (android.graphics.Color.red(c) in 155..180 &&
                    android.graphics.Color.green(c) in 205..235 &&
                    android.graphics.Color.blue(c) in 180..210) buttonPixels++
                if (android.graphics.Color.red(c) > 145 && android.graphics.Color.green(c) > 145) lightPixels++
            }
            colors = distinct.size
            val output = File(instrumentation.targetContext.getExternalFilesDir("smoke"), "$name.png")
            output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            if (colors > 32 && lightPixels > 500 && buttonPixels > 200) return
            Thread.sleep(200)
        } while (System.currentTimeMillis() < deadline)
        assertTrue("$name has no app buttons ($buttonPixels pixels)", buttonPixels > 200)
        assertTrue("$name has no rendered content ($colors colors)", colors > 32)
        assertTrue("$name has no visible UI ($lightPixels light pixels)", lightPixels > 500)
    }

    @Test fun startupAndBothGamesRenderAndResume() {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("settings put secure immersive_mode_confirmations confirmed").close()
        ActivityScenario.launch(AndroidLauncher::class.java).use { scenario ->
            scenario.onActivity { activity = it; game = it.applicationListener as OnetMatch3Game }
            awaitScreen(ScreenType.MAIN_MENU)
            onGl { assertTrue(Gdx.files.internal("tiles/candy/0.png").exists()) }
            screenshot("01-main-menu")
            tap(270f, 520f); awaitScreen(ScreenType.MODE_SELECT)
            screenshot("02-mode-select")
            tap(270f, 600f); awaitScreen(ScreenType.LEVEL_SELECT)
            tap(100f, 700f); awaitScreen(ScreenType.ONET_GAME)
            screenshot("03-onet")
            var original: OnetGameScreen? = null
            onGl { original = game.screenManager.currentScreen as OnetGameScreen }
            tap(475f, 910f); awaitScreen(ScreenType.PAUSE)
            tap(270f, 375f); awaitScreen(ScreenType.SETTINGS)
            screenshot("04-settings")
            tap(90f, 905f); awaitScreen(ScreenType.PAUSE)
            tap(270f, 555f); awaitScreen(ScreenType.ONET_GAME)
            onGl {
                assertSame(original, game.screenManager.currentScreen)
                assertFalse(original!!.isDisposed)
                assertFalse(original!!.roundState.paused)
                game.screenManager.setScreen(ScreenType.TRIPLE_MATCH_GAME)
            }
            awaitScreen(ScreenType.TRIPLE_MATCH_GAME)
            screenshot("05-triple-match")
            tap(100f, 700f)
            onGl { assertEquals(1, (game.screenManager.currentScreen as TripleMatchGameScreen).roundState.tray.size) }
            tap(475f, 910f); awaitScreen(ScreenType.PAUSE)
            tap(270f, 465f); awaitScreen(ScreenType.TRIPLE_MATCH_GAME)
            onGl { assertTrue((game.screenManager.currentScreen as TripleMatchGameScreen).roundState.tray.isEmpty()) }
            screenshot("06-triple-restart")
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
            awaitScreen(ScreenType.TRIPLE_MATCH_GAME)
            onGl { assertFalse((game.screenManager.currentScreen as TripleMatchGameScreen).roundState.paused) }
            screenshot("07-after-background")
        }
    }
}
