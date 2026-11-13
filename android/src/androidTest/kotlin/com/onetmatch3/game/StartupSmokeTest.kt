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

    private fun tapPoint(x: Float, y: Float) {
        var screenX = 0f; var screenY = 0f
        onGl {
            val point = game.screenManager.currentScreen!!.projectUi(Vector2(x, y))
            screenX = point.x; screenY = Gdx.graphics.height - point.y
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val now = android.os.SystemClock.uptimeMillis()
        val down = android.view.MotionEvent.obtain(now, now, android.view.MotionEvent.ACTION_DOWN, screenX, screenY, 0)
        instrumentation.sendPointerSync(down); down.recycle()
        val up = android.view.MotionEvent.obtain(now, now + 60, android.view.MotionEvent.ACTION_UP, screenX, screenY, 0)
        instrumentation.sendPointerSync(up); up.recycle()
    }

    private fun tap(id: String) {
        var point = Vector2()
        onGl {
            val bounds = game.screenManager.currentScreen!!.actionBounds(id)
                ?: throw AssertionError("Missing action $id")
            point = bounds.getCenter(Vector2())
        }
        tapPoint(point.x, point.y)
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
                if (android.graphics.Color.red(c) in 30..140 &&
                    android.graphics.Color.green(c) in 40..170 &&
                    android.graphics.Color.blue(c) in 30..140) buttonPixels++
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
            tap("play"); awaitScreen(ScreenType.MODE_SELECT)
            screenshot("02-mode-select")
            tap("onet"); awaitScreen(ScreenType.LEVEL_SELECT)
            tap("level_1"); awaitScreen(ScreenType.ONET_GAME)
            screenshot("03-onet")
            var original: OnetGameScreen? = null
            onGl { original = game.screenManager.currentScreen as OnetGameScreen }
            tap("pause"); awaitScreen(ScreenType.PAUSE)
            tap("settings"); awaitScreen(ScreenType.SETTINGS)
            screenshot("04-settings")
            tap("back"); awaitScreen(ScreenType.PAUSE)
            tap("resume"); awaitScreen(ScreenType.ONET_GAME)
            onGl {
                assertSame(original, game.screenManager.currentScreen)
                assertFalse(original!!.isDisposed)
                assertFalse(original!!.roundState.paused)
                game.screenManager.setScreen(ScreenType.MAIN_MENU)
            }
            awaitScreen(ScreenType.MAIN_MENU)
            tap("play"); awaitScreen(ScreenType.MODE_SELECT)
            tap("triple"); awaitScreen(ScreenType.LEVEL_SELECT)
            tap("level_1"); awaitScreen(ScreenType.TRIPLE_MATCH_GAME)
            screenshot("05-triple-match")
            var stackPoint = Vector2()
            onGl { stackPoint = (game.screenManager.currentScreen as TripleMatchGameScreen).puzzleLayout.stack(0).getCenter(Vector2()) }
            tapPoint(stackPoint.x, stackPoint.y)
            Thread.sleep(300)
            onGl { assertEquals(1, (game.screenManager.currentScreen as TripleMatchGameScreen).roundState.tray.size) }
            tap("pause"); awaitScreen(ScreenType.PAUSE)
            tap("restart"); awaitScreen(ScreenType.TRIPLE_MATCH_GAME)
            onGl { assertTrue((game.screenManager.currentScreen as TripleMatchGameScreen).roundState.tray.isEmpty()) }
            screenshot("06-triple-restart")
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
            awaitScreen(ScreenType.TRIPLE_MATCH_GAME)
            onGl { assertFalse((game.screenManager.currentScreen as TripleMatchGameScreen).roundState.paused) }
            screenshot("07-after-background")
            onGl { assertEquals(0, game.economyService.gemBalance); game.saveNow() }
            scenario.recreate()
            scenario.onActivity { activity = it; game = it.applicationListener as OnetMatch3Game }
            awaitScreen(ScreenType.MAIN_MENU)
            tap("resume_game"); awaitScreen(ScreenType.TRIPLE_MATCH_GAME)
            screenshot("08-restored-after-recreate")
            onGl { assertFalse((game.screenManager.currentScreen as TripleMatchGameScreen).roundState.paused) }
            tap("pause"); awaitScreen(ScreenType.PAUSE)
            tap("quit"); awaitScreen(ScreenType.MAIN_MENU)
            tap("missions"); awaitScreen(ScreenType.MISSION_CENTER); screenshot("09-missions")
            tap("achievements"); awaitScreen(ScreenType.ACHIEVEMENT_CENTER); screenshot("10-achievements")
            tap("back"); awaitScreen(ScreenType.MISSION_CENTER)
            tap("back"); awaitScreen(ScreenType.MAIN_MENU)
            tap("profile"); awaitScreen(ScreenType.PLAYER_PROFILE); screenshot("11-profile")
            tap("collection"); awaitScreen(ScreenType.COSMETIC_COLLECTION); screenshot("12-collection")
            tap("shop"); awaitScreen(ScreenType.COSMETIC_SHOP); screenshot("13-shop")
            tap("card_0"); awaitScreen(ScreenType.COSMETIC_PREVIEW); screenshot("14-cosmetic-preview")
        }
    }
}
