package game.screen

import game.OnetMatch3Game
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ScreenLifecycleTest {

    @Test
    @DisplayName("Verify all 17 screens instantiate, initialize viewport, and handle lifecycle")
    fun testAllSeventeenScreensInstantiate() {
        val game = OnetMatch3Game()
        // LoadingScreen now uses the real asset service; bootstrap its dependencies.
        game.create()
        try {

        for (type in ScreenType.values()) {
            val screen: BaseScreen = when (type) {
                ScreenType.BOOT -> BootScreen(game)
                ScreenType.LOADING -> LoadingScreen(game)
                ScreenType.MAIN_MENU -> MainMenuScreen(game)
                ScreenType.MODE_SELECT -> ModeSelectScreen(game)
                ScreenType.ONET_GAME -> OnetGameScreen(game)
                ScreenType.TRIPLE_MATCH_GAME -> TripleMatchGameScreen(game)
                ScreenType.LEVEL_SELECT -> LevelSelectScreen(game)
                ScreenType.VICTORY -> VictoryScreen(game)
                ScreenType.DEFEAT -> DefeatScreen(game)
                ScreenType.PAUSE -> PauseScreen(game)
                ScreenType.MISSION_CENTER -> MissionCenterScreen(game)
                ScreenType.ACHIEVEMENT_CENTER -> AchievementCenterScreen(game)
                ScreenType.PLAYER_PROFILE -> PlayerProfileScreen(game)
                ScreenType.COSMETIC_COLLECTION -> CosmeticCollectionScreen(game)
                ScreenType.COSMETIC_SHOP -> CosmeticShopScreen(game)
                ScreenType.SETTINGS -> SettingsScreen(game)
                ScreenType.COSMETIC_PREVIEW -> CosmeticPreviewScreen(game)
                ScreenType.ROUND_RESULT -> VictoryScreen(game)
                ScreenType.SHOP_COSMETICS -> CosmeticShopScreen(game)
                ScreenType.MISSIONS_ACHIEVEMENTS -> MissionCenterScreen(game)
            }

            assertNotNull(screen)
            assertEquals(540f, screen.virtualWidth)
            assertEquals(960f, screen.virtualHeight)

            // Test show & update cycles
            screen.show()
            screen.update(0.016f)
            screen.draw(0.016f)
            screen.pause()
            screen.resume()
            screen.hide()
            screen.dispose()

            assertTrue(screen.isDisposed, "Screen $type must be marked disposed after dispose()")
        }
        } finally { game.dispose() }
    }

    @Test
    @DisplayName("Verify ScreenManager stack push/pop and modal navigation")
    fun testScreenManagerStackNavigation() {
        val game = OnetMatch3Game()
        val screenManager = ScreenManager(game)

        screenManager.registerScreen(ScreenType.MAIN_MENU) { MainMenuScreen(game) }
        screenManager.registerScreen(ScreenType.ONET_GAME) { OnetGameScreen(game) }
        screenManager.registerScreen(ScreenType.PAUSE) { PauseScreen(game) }

        // Start at Main Menu
        screenManager.setScreen(ScreenType.MAIN_MENU)
        assertEquals(ScreenType.MAIN_MENU, screenManager.currentScreenType)

        // Launch Onet Match
        screenManager.setScreen(ScreenType.ONET_GAME)
        assertEquals(ScreenType.ONET_GAME, screenManager.currentScreenType)

        // Push Pause modal
        screenManager.pushScreen(ScreenType.PAUSE)
        assertEquals(ScreenType.PAUSE, screenManager.currentScreenType)

        // Pop Pause modal -> return to Onet Match
        val popped = screenManager.popScreen()
        assertTrue(popped)
        assertTrue(screenManager.currentScreen is OnetGameScreen)
    }
}
