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
import game.session.*
import game.platform.GamePlatform
import game.platform.NoopGamePlatform
import game.gameplay.onet.OnetRoundState
import game.gameplay.triplematch.TripleRoundState
import game.missions.MissionObjective
import game.cosmetics.CosmeticInventory
import game.achievements.AchievementCatalog
import java.util.UUID

/**
 * Main application bootstrap for Onet + Triple Match.
 * Orchestrates ScreenManager, AssetService, domain engines, and audio lifecycle across Android and Desktop.
 */
class OnetMatch3Game(
    val platform: GamePlatform = NoopGamePlatform,
    private var store: AtomicSaveStore? = null
) : Game() {

    lateinit var assetService: AssetService
        private set
    lateinit var soundService: SoundService
        private set
    lateinit var screenManager: ScreenManager
        private set

    // Domain Services
    var economyService: EconomyService = EconomyService()
    val progressionEngine: ProgressionEngine = ProgressionEngine()
    var playerProfile: game.progression.PlayerProfile = game.progression.PlayerProfile(playerId = "player_1")
    var missionService: MissionService = MissionService()
    var achievementService: AchievementService = AchievementService()
    var cosmeticService: CosmeticService = CosmeticService()
    var billingService: BillingService = BillingService(platform.billingProvider, cosmeticService)

    // Current session context
    var selectedLevel: Int = 1
    var selectedTheme: String = "candy"
    var activeGameType: ScreenType = ScreenType.ONET_GAME
    var lastScore: Int = 0
    var lastStars: Int = 3
    var lastResultReason: String = ""

    var settings = GameSettings()
        private set
    var lastRoundXp: Long = 0L
        private set
    var lastSaveError: String? = null
        private set
    private var records = mutableMapOf<String, LevelRecord>()
    private var onetUnlocked = 1
    private var tripleUnlocked = 1
    private var tutorialModes = mutableSetOf<String>()
    private var roundId = ""
    private var onetSnapshot: OnetRoundState? = null
    private var tripleSnapshot: TripleRoundState? = null
    private var savedHints = 0
    private var savedShuffles = 0
    private var completedRounds = mutableSetOf<String>()
    private var missionClock = 0L
    private val rewardTuning = RewardTuning()

    override fun create() {
        assetService = AssetService()
        soundService = SoundService(assetService, platform)
        screenManager = ScreenManager(this)

        if (store == null && Gdx.app != null && Gdx.app.type != com.badlogic.gdx.Application.ApplicationType.HeadlessDesktop) {
            store = AtomicSaveStore(Gdx.files.local("player-data").file())
        }
        store?.load()?.let { restore(it) }
        refreshMissions()
        applySettings()
        soundService.queueDefaultAudio()
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

    @Synchronized fun saveNow() {
        try {
            if (::soundService.isInitialized) {
                soundService.isSfxMuted = !settings.soundEnabled
                soundService.isMusicMuted = !settings.musicEnabled
                soundService.hapticsEnabled = settings.hapticsEnabled
                soundService.reducedEffects = settings.reducedEffects
                soundService.sfxVolume = settings.sfxVolume
                soundService.musicVolume = settings.musicVolume
            }
            store?.save(snapshot())
            lastSaveError = null
        } catch (e: Exception) { lastSaveError = e.message; throw e }
    }

    private fun applySettings() {
        if (!::soundService.isInitialized) return
        soundService.isSfxMuted = !settings.soundEnabled
        soundService.isMusicMuted = !settings.musicEnabled
        soundService.hapticsEnabled = settings.hapticsEnabled
        soundService.reducedEffects = settings.reducedEffects
        soundService.sfxVolume = settings.sfxVolume
        soundService.musicVolume = settings.musicVolume
    }

    private fun persistLifecycle() {
        try { saveNow() } catch (e: Exception) { Gdx.app?.error("Save", "Unable to save player session", e) }
    }

    fun unlockedLevel(mode: ScreenType): Int = if (mode == ScreenType.TRIPLE_MATCH_GAME) tripleUnlocked else onetUnlocked
    fun bestScore(mode: ScreenType, level: Int): Int = records["${mode.name}:$level"]?.bestScore ?: 0
    fun levelRecord(mode: ScreenType, level: Int): LevelRecord = records["${mode.name}:$level"] ?: LevelRecord()
    fun tutorialSeen(mode: ScreenType): Boolean = mode.name in tutorialModes
    fun markTutorialSeen(mode: ScreenType) { transaction { tutorialModes.add(mode.name) } }

    fun startNewRound(mode: ScreenType, level: Int) {
        require(mode == ScreenType.ONET_GAME || mode == ScreenType.TRIPLE_MATCH_GAME)
        require(level in 1..unlockedLevel(mode)) { "This level is locked" }
        transaction {
            activeGameType = mode; selectedLevel = level; roundId = UUID.randomUUID().toString()
            onetSnapshot = null; tripleSnapshot = null; savedHints = 0; savedShuffles = 0
        }
    }

    fun resumedOnet(): OnetRoundState? = onetSnapshot?.takeIf { activeGameType == ScreenType.ONET_GAME && it.phase == "playing" }?.copy(paused = false)
    fun resumedTriple(): TripleRoundState? = tripleSnapshot?.takeIf { activeGameType == ScreenType.TRIPLE_MATCH_GAME && it.phase == "playing" }?.copy(paused = false)
    fun captureOnet(state: OnetRoundState, hints: Int = 0, shuffles: Int = 0) {
        if (roundId.isBlank()) roundId = UUID.randomUUID().toString()
        onetSnapshot = state; tripleSnapshot = null; activeGameType = ScreenType.ONET_GAME
        savedHints = hints; savedShuffles = shuffles
        saveNow()
    }
    fun captureTriple(state: TripleRoundState) {
        if (roundId.isBlank()) roundId = UUID.randomUUID().toString()
        tripleSnapshot = state; onetSnapshot = null; activeGameType = ScreenType.TRIPLE_MATCH_GAME
        saveNow()
    }

    fun refreshMissions() {
        missionClock = maxOf(missionClock, System.currentTimeMillis())
        missionService.refreshMissions(missionClock)
    }

    fun completeOnet(state: OnetRoundState, hintsUsed: Int = 0, shufflesUsed: Int = 0) {
        require(state.phase == "won" || state.phase == "lost")
        finishRound(ScreenType.ONET_GAME, state.score,
            if (state.phase == "won") (if (hintsUsed == 0 && shufflesUsed == 0 && state.mistakes == 0) 3 else 2) else 0,
            state.phase == "won", if (state.phase == "won") "Board cleared!" else "Time is up") {
            playerProfile = playerProfile.copy(stats = playerProfile.stats.recordOnetRound(
                state.phase == "won", state.matches * 2, state.highestCombo, hintsUsed, shufflesUsed,
                state.time.toFloat() / state.duration, selectedTheme))
            missionService.recordProgress(MissionObjective.MATCH_TILES, state.matches * 2)
            if (state.phase == "won") {
                missionService.recordProgress(MissionObjective.WIN_ONET_ROUNDS)
                missionService.recordProgress(MissionObjective.COMBO_STREAK, state.highestCombo)
                if (hintsUsed == 0 && shufflesUsed == 0) missionService.recordProgress(MissionObjective.FLAWLESS_ROUNDS)
                if (state.time.toFloat() / state.duration >= 0.5f) missionService.recordProgress(MissionObjective.FAST_ROUNDS)
            }
        }
    }

    fun completeTriple(state: TripleRoundState) {
        require(state.phase == "won" || state.phase == "lost")
        finishRound(ScreenType.TRIPLE_MATCH_GAME, game.gameplay.triplematch.tripleScore(state),
            if (state.phase == "won") (if (state.undoUsed) 2 else 3) else 0,
            state.phase == "won", if (state.phase == "won") "All triples matched!" else if (state.tray.size >= 7) "Tray is full" else "Time is up") {
            playerProfile = playerProfile.copy(stats = playerProfile.stats.recordTripleMatchRound(
                state.phase == "won", state.cleared, if (state.undoUsed) 1 else 0,
                state.time.toFloat() / state.duration, selectedTheme))
            missionService.recordProgress(MissionObjective.MATCH_TILES, state.cleared * 3)
            if (state.phase == "won") {
                missionService.recordProgress(MissionObjective.WIN_TRIPLE_MATCH_ROUNDS)
                if (!state.undoUsed) missionService.recordProgress(MissionObjective.FLAWLESS_ROUNDS)
                if (state.time.toFloat() / state.duration >= 0.5f) missionService.recordProgress(MissionObjective.FAST_ROUNDS)
            }
        }
    }

    private fun finishRound(mode: ScreenType, score: Int, stars: Int, won: Boolean, reason: String, updateStats: () -> Unit) {
        if (roundId.isBlank()) roundId = UUID.randomUUID().toString()
        if (roundId in completedRounds) return
        transaction {
            refreshMissions()
            val key = "${mode.name}:$selectedLevel"
            val old = records[key] ?: LevelRecord()
            lastRoundXp = if (won) rewardTuning.award(selectedLevel, old.wins == 0, score > old.bestScore, stars, old.wins) else 0
            updateStats()
            missionService.recordProgress(MissionObjective.PLAY_THEME, theme = selectedTheme)
            if (won) {
                records[key] = LevelRecord(maxOf(old.bestScore, score), maxOf(old.bestStars, stars), old.wins + 1)
                if (mode == ScreenType.ONET_GAME) onetUnlocked = maxOf(onetUnlocked, selectedLevel + 1)
                else tripleUnlocked = maxOf(tripleUnlocked, selectedLevel + 1)
            }
            playerProfile = progressionEngine.applyXp(playerProfile, lastRoundXp).first
            achievementService.syncStats(playerProfile.stats, cosmeticService.inventory.getUnlockedCount())
            lastScore = score; lastStars = stars; lastResultReason = reason
            completedRounds.add(roundId)
            onetSnapshot = null; tripleSnapshot = null
        }
    }

    fun claimMission(id: String) = transaction { refreshMissions(); missionService.claimMission(id, economyService) }
    fun claimAchievement(id: String, tier: Int) = transaction { achievementService.claimTier(id, tier, economyService) }
    fun buyCosmetic(id: String) = transaction { cosmeticService.purchaseEarnable(id, economyService) }
    fun equipCosmetic(id: String): Boolean = transaction {
        val result = cosmeticService.inventory.equip(id)
        if (result) game.cosmetics.CosmeticCatalog.getItem(id)?.takeIf { it.type == game.cosmetics.CosmeticType.TILE_SET }?.let {
            selectedTheme = it.assetPath.substringAfterLast('/')
        }
        result
    }

    @Synchronized private fun <T> transaction(action: () -> T): T {
        val before = snapshot()
        try { val result = action(); saveNow(); return result }
        catch (e: Exception) { restore(before); throw e }
    }

    fun snapshot(): SaveState = SaveState(
        profile = playerProfile, transactions = economyService.ledger.getTransactions(),
        missions = missionService.getActiveMissions(), achievements = achievementService.getAchievements(),
        unlocked = cosmeticService.inventory.getUnlockedIds(), equipped = cosmeticService.inventory.getEquippedMap(),
        records = records.toMap(), unlockedOnet = onetUnlocked, unlockedTriple = tripleUnlocked,
        settings = settings.copy(), tutorialModes = tutorialModes.toSet(), roundId = roundId,
        roundMode = activeGameType.name, roundLevel = selectedLevel, roundTheme = selectedTheme,
        onet = onetSnapshot, triple = tripleSnapshot, roundHints = savedHints, roundShuffles = savedShuffles,
        completedRoundIds = completedRounds.toSet(), missionTimeHighWater = missionClock
    )

    private fun restore(s: SaveState) {
        SaveCodec.validate(s)
        playerProfile = s.profile.copy(level = progressionEngine.calculateLevelForXp(s.profile.currentXp))
        economyService = EconomyService(game.economy.TransactionLedger(s.transactions))
        missionService = MissionService(initialMissions = s.missions)
        achievementService = AchievementService(if (s.achievements.isEmpty()) AchievementCatalog.createInitialCatalog() else s.achievements)
        cosmeticService = CosmeticService(CosmeticInventory(s.unlocked, s.equipped))
        billingService = BillingService(platform.billingProvider, cosmeticService)
        records = s.records.toMutableMap(); onetUnlocked = s.unlockedOnet; tripleUnlocked = s.unlockedTriple
        settings = s.settings.copy(); tutorialModes = s.tutorialModes.toMutableSet()
        roundId = s.roundId; activeGameType = ScreenType.valueOf(s.roundMode); selectedLevel = s.roundLevel
        selectedTheme = s.roundTheme; onetSnapshot = s.onet; tripleSnapshot = s.triple
        savedHints = s.roundHints; savedShuffles = s.roundShuffles
        completedRounds = s.completedRoundIds.toMutableSet(); missionClock = s.missionTimeHighWater
        applySettings()
    }

    override fun render() {
        val delta = Gdx.graphics?.deltaTime ?: 0.016f
        soundService.update()
        screenManager.render(delta)
    }

    override fun resize(width: Int, height: Int) {
        screenManager.resize(width, height)
    }

    override fun pause() {
        screenManager.pause()
        soundService.pauseMusic()
        persistLifecycle()
    }

    override fun resume() {
        screenManager.resume()
        soundService.resumeMusic()
    }

    override fun dispose() {
        persistLifecycle()
        screenManager.dispose()
        soundService.stopMusic()
        assetService.dispose()
    }
}
