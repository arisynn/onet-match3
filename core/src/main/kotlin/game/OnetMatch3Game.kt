package game

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import game.assets.AssetService
import game.assets.SoundService
import game.economy.EconomyService
import game.progression.ProgressionEngine
import game.missions.MissionService
import game.achievements.AchievementService
import game.cosmetics.CosmeticService
import game.billing.BillingService
import game.screen.*

/**
 * Main application bootstrap for Onet + Triple Match.
 * Orchestrates ScreenManager, AssetService, domain engines, and audio lifecycle across Android and Desktop.
 */
class OnetMatch3Game : Game() {

    lateinit var assetService: AssetService
        private set
    lateinit var soundService: SoundService
        private set
    lateinit var screenManager: ScreenManager
        private set

    // Domain Services
    val economyService: EconomyService = EconomyService()
    val progressionEngine: ProgressionEngine = ProgressionEngine()
    var playerProfile: game.progression.PlayerProfile = game.progression.PlayerProfile(playerId = "player_1")
    val missionService: MissionService = MissionService()
    val achievementService: AchievementService = AchievementService()
    val cosmeticService: CosmeticService = CosmeticService()
    val billingService: BillingService = BillingService(game.billing.FakeBillingProvider(), cosmeticService)

    // Current session context
    var selectedLevel: Int = 1
    var selectedTheme: String = "candy"
    var activeGameType: ScreenType = ScreenType.ONET_GAME
    var lastScore: Int = 0
    var lastStars: Int = 3
    var lastResultReason: String = ""

    override fun create() {
        assetService = AssetService()
        soundService = SoundService(assetService)
        screenManager = ScreenManager(this)

        missionService.refreshMissions(System.currentTimeMillis())
        registerAllScreens()

        // Kick off with boot screen
        screenManager.setScreen(ScreenType.BOOT)
    }

    private fun registerAllScreens() {
        // 1. Splash screen
        screenManager.registerScreen(ScreenType.BOOT) { BootScreen(this) }
        // 2. Loading screen
        screenManager.registerScreen(ScreenType.LOADING) { LoadingScreen(this) }
        // 3. Main menu
        screenManager.registerScreen(ScreenType.MAIN_MENU) { MainMenuScreen(this) }
        // 4. Game mode selection
        screenManager.registerScreen(ScreenType.MODE_SELECT) { ModeSelectScreen(this) }
        // 5. Onet gameplay
        screenManager.registerScreen(ScreenType.ONET_GAME) { OnetGameScreen(this) }
        // 6. Triple Match gameplay
        screenManager.registerScreen(ScreenType.TRIPLE_MATCH_GAME) { TripleMatchGameScreen(this) }
        // 7. Level selection
        screenManager.registerScreen(ScreenType.LEVEL_SELECT) { LevelSelectScreen(this) }
        // 8. Victory screen
        screenManager.registerScreen(ScreenType.VICTORY) { VictoryScreen(this) }
        // 9. Defeat screen
        screenManager.registerScreen(ScreenType.DEFEAT) { DefeatScreen(this) }
        // 10. Pause menu
        screenManager.registerScreen(ScreenType.PAUSE) { PauseScreen(this) }
        // 11. Mission center
        screenManager.registerScreen(ScreenType.MISSION_CENTER) { MissionCenterScreen(this) }
        // 12. Achievement center
        screenManager.registerScreen(ScreenType.ACHIEVEMENT_CENTER) { AchievementCenterScreen(this) }
        // 13. Player profile
        screenManager.registerScreen(ScreenType.PLAYER_PROFILE) { PlayerProfileScreen(this) }
        // 14. Cosmetic collection
        screenManager.registerScreen(ScreenType.COSMETIC_COLLECTION) { CosmeticCollectionScreen(this) }
        // 15. Cosmetic shop
        screenManager.registerScreen(ScreenType.COSMETIC_SHOP) { CosmeticShopScreen(this) }
        // 16. Settings
        screenManager.registerScreen(ScreenType.SETTINGS) { SettingsScreen(this) }
        // 17. Cosmetic preview and equip interface
        screenManager.registerScreen(ScreenType.COSMETIC_PREVIEW) { CosmeticPreviewScreen(this) }

        // Compatibility Aliases
        screenManager.registerScreen(ScreenType.ROUND_RESULT) { VictoryScreen(this) }
        screenManager.registerScreen(ScreenType.SHOP_COSMETICS) { CosmeticShopScreen(this) }
        screenManager.registerScreen(ScreenType.MISSIONS_ACHIEVEMENTS) { MissionCenterScreen(this) }
    }

    fun recordWin(objective: game.missions.MissionObjective, matchedTiles: Int) {
        missionService.recordProgress(objective)
        missionService.recordProgress(game.missions.MissionObjective.MATCH_TILES, matchedTiles)
        missionService.recordProgress(game.missions.MissionObjective.PLAY_THEME, theme = selectedTheme)
        achievementService.syncStats(playerProfile.stats, cosmeticService.inventory.getUnlockedCount())
    }

    override fun render() {
        val delta = Gdx.graphics?.deltaTime ?: 0.016f
        screenManager.render(delta)
    }

    override fun resize(width: Int, height: Int) {
        screenManager.resize(width, height)
    }

    override fun pause() {
        screenManager.pause()
        soundService.pauseMusic()
    }

    override fun resume() {
        screenManager.resume()
        soundService.resumeMusic()
    }

    override fun dispose() {
        screenManager.dispose()
        soundService.stopMusic()
        assetService.dispose()
    }
}
