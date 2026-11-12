package game.headless

import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import game.OnetMatch3Game
import game.assets.TileAssetResolver
import game.screen.ScreenType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class HeadlessGameBootstrapTest {

    @Test
    fun testTileAssetResolverIntegrity() {
        // Verify base themes exist
        val candyTiles = TileAssetResolver.tileIdsFor("candy")
        assertEquals(16, candyTiles.size)
        assertEquals("candy_0", candyTiles[0])
        assertEquals("assets/tiles/candy/0.png", TileAssetResolver.tileAssetFor("candy_0"))

        // Verify reward themes exist
        val rewardSakura = TileAssetResolver.tileIdsFor("reward_sakura")
        assertEquals(10, rewardSakura.size)
        assertEquals("reward_sakura_0", rewardSakura[0])
        assertEquals("assets/tiles/reward_sakura/0.webp", TileAssetResolver.tileAssetFor("reward_sakura_0"))

        // Verify beach tiles
        val sweetsTiles = TileAssetResolver.tileIdsFor("sweets")
        assertEquals(15, sweetsTiles.size)
        assertEquals("beach_ball", sweetsTiles[0])
        assertEquals("assets/tiles/beach/beach_ball.png", TileAssetResolver.tileAssetFor("beach_ball"))
    }

    @Test
    fun testGameBootstrapAndScreenLifecycle() {
        com.badlogic.gdx.utils.GdxNativesLoader.disableNativesLoading = true
        val game = OnetMatch3Game()
        val config = HeadlessApplicationConfiguration()
        val latch = CountDownLatch(1)

        val app = HeadlessApplication(game, config)
        try {
            // Post runnable to ensure libGDX context is ready
            app.postRunnable {
                try {
                    assertNotNull(game.screenManager)
                    assertNotNull(game.assetService)
                    assertNotNull(game.soundService)

                    // Initially boot screen
                    assertEquals(ScreenType.BOOT, game.screenManager.currentScreenType)

                    // Switch to MAIN_MENU
                    game.screenManager.setScreen(ScreenType.MAIN_MENU)
                    assertEquals(ScreenType.MAIN_MENU, game.screenManager.currentScreenType)

                    // Switch to ONET_GAME
                    game.screenManager.setScreen(ScreenType.ONET_GAME)
                    assertEquals(ScreenType.ONET_GAME, game.screenManager.currentScreenType)

                    // Push SETTINGS screen onto stack
                    game.screenManager.pushScreen(ScreenType.SETTINGS)
                    assertEquals(ScreenType.SETTINGS, game.screenManager.currentScreenType)

                    // Pop back to ONET_GAME
                    val popped = game.screenManager.popScreen()
                    assertTrue(popped)
                    assertEquals(ScreenType.ONET_GAME, game.screenManager.currentScreenType)
                } finally {
                    latch.countDown()
                }
            }

            val completed = latch.await(5, TimeUnit.SECONDS)
            assertTrue(completed, "Headless application test timed out")
        } finally {
            app.exit()
        }
    }
}
