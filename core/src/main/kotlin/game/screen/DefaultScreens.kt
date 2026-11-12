package game.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import game.OnetMatch3Game
import game.assets.TileAssetResolver
import game.cosmetics.CosmeticCatalog
import game.cosmetics.CosmeticType
import game.gameplay.onet.*
import game.gameplay.shared.SeededRandom
import game.gameplay.triplematch.*
import kotlin.math.max
import kotlin.math.min

// =============================================================================
// 1. BOOT / SPLASH SCREEN
// =============================================================================
class BootScreen(game: OnetMatch3Game) : BaseScreen(game) {
    private var timer = 0f

    private var ui: StartupUi? = null
    override fun onScreenShown() {
        if (Gdx.gl == null) return
        ui = StartupUi(this).apply {
            text("Onet + Triple Match", 70f, 620f)
            text("Starting...", 170f, 450f)
        }
    }
    override fun onDispose() { ui?.dispose(); ui = null }

    init {
        backgroundColor = Color(0.08f, 0.08f, 0.11f, 1f)
    }

    override fun update(delta: Float) {
        timer += delta
        if (timer >= 0.5f) {
            game.screenManager.setScreen(ScreenType.LOADING, FadeTransition(0.3f))
        }
    }

    override fun draw(delta: Float) { ui?.draw(delta) }
}

// =============================================================================
// 2. LOADING SCREEN
// =============================================================================
class LoadingScreen(game: OnetMatch3Game) : BaseScreen(game) {
    private var simulatedProgress = 0f

    private var ui: StartupUi? = null
    override fun onScreenShown() {
        if (Gdx.gl == null) return
        ui = StartupUi(this).apply {
            text("Loading...", 160f, 570f)
        }
    }
    override fun onDispose() { ui?.dispose(); ui = null }

    init {
        backgroundColor = Color(0.10f, 0.12f, 0.16f, 1f)
    }

    override fun update(delta: Float) {
        game.assetService.update()
        simulatedProgress = game.assetService.progress
        if (game.assetService.isFinished) {
            game.screenManager.setScreen(ScreenType.MAIN_MENU, FadeTransition(0.4f))
        }
    }

    override fun draw(delta: Float) { ui?.draw(delta) }
}

// =============================================================================
// 3. MAIN MENU SCREEN
// =============================================================================
class MainMenuScreen(game: OnetMatch3Game) : BaseScreen(game) {
    private var ui: StartupUi? = null
    override fun onScreenShown() {
        if (Gdx.gl == null) return
        ui = StartupUi(this).apply {
            text("Onet + Triple Match", 70f, 760f)
            text("Play", 210f, 500f)
            text("Missions", 180f, 395f)
            text("Shop", 210f, 295f)
            text("Profile", 125f, 195f)
            text("Settings", 290f, 195f)
        }
    }
    override fun onDispose() { ui?.dispose(); ui = null }

    init {
        backgroundColor = Color(0.14f, 0.16f, 0.22f, 1f)
        addInputProcessor(object : InputAdapter() {
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val touch = viewport.unproject(com.badlogic.gdx.math.Vector2(screenX.toFloat(), screenY.toFloat()))
                val x = touch.x
                val y = touch.y

                // Play Button
                if (x in 120f..420f && y in 480f..560f) {
                    game.screenManager.setScreen(ScreenType.MODE_SELECT, FadeTransition(0.25f))
                    return true
                }
                // Missions & Achievements Button
                if (x in 120f..420f && y in 380f..450f) {
                    game.screenManager.setScreen(ScreenType.MISSION_CENTER, FadeTransition(0.25f))
                    return true
                }
                // Cosmetic Shop Button
                if (x in 120f..420f && y in 280f..350f) {
                    game.screenManager.setScreen(ScreenType.COSMETIC_SHOP, FadeTransition(0.25f))
                    return true
                }
                // Player Profile Button
                if (x in 120f..260f && y in 180f..250f) {
                    game.screenManager.setScreen(ScreenType.PLAYER_PROFILE, FadeTransition(0.25f))
                    return true
                }
                // Settings Button
                if (x in 280f..420f && y in 180f..250f) {
                    game.screenManager.setScreen(ScreenType.SETTINGS, FadeTransition(0.25f))
                    return true
                }
                return false
            }
        })
    }

    override fun update(delta: Float) {}
    override fun draw(delta: Float) { ui?.draw(delta) }
}

// =============================================================================
// 4. GAME MODE SELECTION SCREEN
// =============================================================================
class ModeSelectScreen(game: OnetMatch3Game) : BaseScreen(game) {
    private var ui: StartupUi? = null
    override fun onScreenShown() {
        if (Gdx.gl == null) return
        ui = StartupUi(this).apply {
            text("Choose a mode", 100f, 780f)
            text("Onet Connect", 140f, 570f)
            text("Triple Match", 140f, 370f)
            text("Back", 40f, 875f)
        }
    }
    override fun onDispose() { ui?.dispose(); ui = null }

    init {
        backgroundColor = Color(0.13f, 0.15f, 0.20f, 1f)
        addInputProcessor(object : InputAdapter() {
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val touch = viewport.unproject(com.badlogic.gdx.math.Vector2(screenX.toFloat(), screenY.toFloat()))
                val x = touch.x
                val y = touch.y

                // Select Onet Connect Mode
                if (x in 80f..460f && y in 520f..680f) {
                    game.screenManager.setScreen(ScreenType.LEVEL_SELECT, FadeTransition(0.25f))
                    return true
                }
                // Select Triple Match Tray Mode
                if (x in 80f..460f && y in 320f..480f) {
                    game.screenManager.setScreen(ScreenType.TRIPLE_MATCH_GAME, FadeTransition(0.25f))
                    return true
                }
                // Back Button
                if (x in 40f..140f && y in 860f..920f) {
                    game.screenManager.setScreen(ScreenType.MAIN_MENU, FadeTransition(0.25f))
                    return true
                }
                return false
            }
        })
    }

    override fun update(delta: Float) {}
    override fun draw(delta: Float) { ui?.draw(delta) }
}

// =============================================================================
// 7. LEVEL SELECTION SCREEN
// =============================================================================
class LevelSelectScreen(game: OnetMatch3Game) : BaseScreen(game) {
    init {
        backgroundColor = Color(0.12f, 0.15f, 0.20f, 1f)
        addInputProcessor(object : InputAdapter() {
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val touch = viewport.unproject(com.badlogic.gdx.math.Vector2(screenX.toFloat(), screenY.toFloat()))
                val x = touch.x
                val y = touch.y

                // Level grid click simulation (Level 1..10)
                if (y in 250f..750f && x in 50f..490f) {
                    val col = ((x - 50f) / 110f).toInt().coerceIn(0, 3)
                    val row = ((750f - y) / 100f).toInt().coerceIn(0, 4)
                    val level = row * 4 + col + 1
                    game.selectedLevel = level
                    game.screenManager.setScreen(ScreenType.ONET_GAME, FadeTransition(0.3f))
                    return true
                }
                // Back button
                if (x in 40f..140f && y in 860f..920f) {
                    game.screenManager.setScreen(ScreenType.MODE_SELECT, FadeTransition(0.25f))
                    return true
                }
                return false
            }
        })
    }

    override fun update(delta: Float) {}
    override fun draw(delta: Float) {}
}

// =============================================================================
// 5. ONET CONNECT GAMEPLAY SCREEN
// =============================================================================
class OnetGameScreen(game: OnetMatch3Game) : BaseScreen(game) {
    var roundState: OnetRoundState = createOnetRound(
        theme = game.selectedTheme,
        level = game.selectedLevel,
        rng = SeededRandom("onet_level_${game.selectedLevel}")
    )
    private var activePath: List<Point>? = null
    private var pathTimer = 0f
    private var timeAccumulator = 0f
    private var highlightedHint: Hint? = null

    init {
        backgroundColor = Color(0.15f, 0.18f, 0.23f, 1f)
        addInputProcessor(object : InputAdapter() {
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val touch = viewport.unproject(com.badlogic.gdx.math.Vector2(screenX.toFloat(), screenY.toFloat()))
                val x = touch.x
                val y = touch.y

                // Pause button
                if (x in 440f..510f && y in 880f..940f) {
                    roundState = pauseOnetRound(roundState, true)
                    game.screenManager.pushScreen(ScreenType.PAUSE, FadeTransition(0.2f))
                    return true
                }
                // Hint button
                if (x in 60f..180f && y in 80f..140f) {
                    highlightedHint = hintFor(roundState)
                    return true
                }
                // Shuffle button
                if (x in 360f..480f && y in 80f..140f) {
                    roundState = shuffleRound(roundState, SeededRandom(System.currentTimeMillis()))
                    highlightedHint = null
                    return true
                }
                // Board area clicks: mapping screen touch to board (rows 1..10, cols 1..6)
                if (x in 45f..495f && y in 180f..820f) {
                    val col = (((x - 45f) / 75f).toInt() + 1).coerceIn(1, 6)
                    val row = (((820f - y) / 64f).toInt() + 1).coerceIn(1, 10)
                    handleTileSelection(row, col)
                    return true
                }
                return false
            }
        })
    }

    private fun handleTileSelection(r: Int, c: Int) {
        val nextState = selectTile(roundState, r, c, System.currentTimeMillis())
        roundState = nextState
        highlightedHint = null

        val event = roundState.event
        if (event?.type == "match") {
            activePath = event.path
            pathTimer = ((event?.duration ?: 480L)).toFloat() / 1000f
            game.soundService.playSound("assets/audio/match.ogg")
        } else if (event?.type == "blocked") {
            game.soundService.playSound("assets/audio/invalid.ogg")
        }
    }

    override fun update(delta: Float) {
        if (roundState.paused) return

        // Path line display countdown and match resolution
        if (activePath != null) {
            pathTimer -= delta
            if (pathTimer <= 0f) {
                activePath = null
                roundState = resolveMatch(roundState)
            }
        }

        // Timer tick
        timeAccumulator += delta
        if (timeAccumulator >= 1f) {
            val seconds = timeAccumulator.toInt()
            timeAccumulator -= seconds.toFloat()
            roundState = tickOnetRound(roundState, seconds)
        }

        // Check Win/Loss conditions
        if (roundState.phase == "won") {
            game.lastScore = roundState.score
            game.lastStars = if (roundState.mistakes == 0) 3 else if (roundState.mistakes <= 2) 2 else 1
            game.lastResultReason = "Board Cleared!"
            game.playerProfile = game.progressionEngine.applyXp(game.playerProfile, 100L).first
            game.screenManager.setScreen(ScreenType.VICTORY, FadeTransition(0.4f))
        } else if (roundState.phase == "lost") {
            game.lastScore = roundState.score
            game.lastResultReason = "Time's Up!"
            game.screenManager.setScreen(ScreenType.DEFEAT, FadeTransition(0.4f))
        }
    }

    override fun draw(delta: Float) {}
}

// =============================================================================
// 6. TRIPLE MATCH GAMEPLAY SCREEN
// =============================================================================
class TripleMatchGameScreen(game: OnetMatch3Game) : BaseScreen(game) {
    private var timeAccumulator = 0f
    var roundState: TripleRoundState = startTripleRound(
        createTripleRound(
            TileAssetResolver.tileIdsFor(game.selectedTheme),
            rng = SeededRandom("triple_match_${game.selectedLevel}"),
            seconds = 120
        )
    )

    init {
        backgroundColor = Color(0.18f, 0.20f, 0.25f, 1f)
        addInputProcessor(object : InputAdapter() {
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val touch = viewport.unproject(com.badlogic.gdx.math.Vector2(screenX.toFloat(), screenY.toFloat()))
                val x = touch.x
                val y = touch.y

                // Pause button
                if (x in 440f..510f && y in 880f..940f) {
                    roundState = pauseTripleRound(roundState, true)
                    game.screenManager.pushScreen(ScreenType.PAUSE, FadeTransition(0.2f))
                    return true
                }
                // Undo button (single use)
                if (x in 60f..180f && y in 60f..120f) {
                    if (!roundState.undoUsed && roundState.undo != null) {
                        roundState = undoPick(roundState)
                        game.soundService.playSound("assets/audio/undo.ogg")
                    }
                    return true
                }
                // 12 Stack selection area (4 columns x 3 rows of stacks)
                if (x in 50f..490f && y in 320f..780f) {
                    val col = ((x - 50f) / 110f).toInt().coerceIn(0, 3)
                    val row = ((780f - y) / 150f).toInt().coerceIn(0, 2)
                    val stackIndex = row * 4 + col
                    if (stackIndex < roundState.stacks.size && roundState.stacks[stackIndex].isNotEmpty()) {
                        roundState = pickTile(roundState, stackIndex)
                        game.soundService.playSound("assets/audio/pick.ogg")
                    }
                    return true
                }
                return false
            }
        })
    }

    override fun update(delta: Float) {
        if (roundState.paused) return

        timeAccumulator += delta
        if (timeAccumulator >= 1f) {
            val seconds = timeAccumulator.toInt()
            timeAccumulator -= seconds.toFloat()
            roundState = tickTripleRound(roundState, seconds)
        }

        if (roundState.phase == "won") {
            game.lastScore = tripleScore(roundState)
            game.lastStars = if (!roundState.undoUsed) 3 else 2
            game.lastResultReason = "All Triples Matched!"
            game.playerProfile = game.progressionEngine.applyXp(game.playerProfile, 120L).first
            game.screenManager.setScreen(ScreenType.VICTORY, FadeTransition(0.4f))
        } else if (roundState.phase == "lost") {
            game.lastScore = tripleScore(roundState)
            game.lastResultReason = if (roundState.tray.size >= TRAY_CAPACITY) "Tray Full!" else "Time's Up!"
            game.screenManager.setScreen(ScreenType.DEFEAT, FadeTransition(0.4f))
        }
    }

    override fun draw(delta: Float) {}
}

// =============================================================================
// 8. VICTORY SCREEN
// =============================================================================
class VictoryScreen(game: OnetMatch3Game) : BaseScreen(game) {
    init {
        backgroundColor = Color(0.10f, 0.16f, 0.12f, 0.95f)
        addInputProcessor(object : InputAdapter() {
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val touch = viewport.unproject(com.badlogic.gdx.math.Vector2(screenX.toFloat(), screenY.toFloat()))
                val x = touch.x
                val y = touch.y

                // Next Level
                if (x in 120f..420f && y in 380f..450f) {
                    game.selectedLevel += 1
                    game.screenManager.setScreen(ScreenType.ONET_GAME, FadeTransition(0.3f))
                    return true
                }
                // Replay
                if (x in 120f..420f && y in 290f..360f) {
                    game.screenManager.setScreen(ScreenType.ONET_GAME, FadeTransition(0.3f))
                    return true
                }
                // Main Menu
                if (x in 120f..420f && y in 200f..270f) {
                    game.screenManager.setScreen(ScreenType.MAIN_MENU, FadeTransition(0.3f))
                    return true
                }
                return false
            }
        })
    }

    override fun update(delta: Float) {}
    override fun draw(delta: Float) {}
}

// =============================================================================
// 9. DEFEAT SCREEN
// =============================================================================
class DefeatScreen(game: OnetMatch3Game) : BaseScreen(game) {
    init {
        backgroundColor = Color(0.20f, 0.10f, 0.10f, 0.95f)
        addInputProcessor(object : InputAdapter() {
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val touch = viewport.unproject(com.badlogic.gdx.math.Vector2(screenX.toFloat(), screenY.toFloat()))
                val x = touch.x
                val y = touch.y

                // Retry
                if (x in 120f..420f && y in 340f..420f) {
                    game.screenManager.setScreen(ScreenType.ONET_GAME, FadeTransition(0.3f))
                    return true
                }
                // Main Menu
                if (x in 120f..420f && y in 240f..320f) {
                    game.screenManager.setScreen(ScreenType.MAIN_MENU, FadeTransition(0.3f))
                    return true
                }
                return false
            }
        })
    }

    override fun update(delta: Float) {}
    override fun draw(delta: Float) {}
}

// =============================================================================
// 10. PAUSE SCREEN
// =============================================================================
class PauseScreen(game: OnetMatch3Game) : BaseScreen(game) {
    init {
        backgroundColor = Color(0.08f, 0.10f, 0.14f, 0.90f)
        addInputProcessor(object : InputAdapter() {
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val touch = viewport.unproject(com.badlogic.gdx.math.Vector2(screenX.toFloat(), screenY.toFloat()))
                val x = touch.x
                val y = touch.y

                // Resume
                if (x in 120f..420f && y in 520f..590f) {
                    game.screenManager.popScreen(FadeTransition(0.2f))
                    return true
                }
                // Restart
                if (x in 120f..420f && y in 430f..500f) {
                    game.screenManager.popScreen(InstantTransition)
                    game.screenManager.setScreen(ScreenType.ONET_GAME, FadeTransition(0.25f))
                    return true
                }
                // Settings
                if (x in 120f..420f && y in 340f..410f) {
                    game.screenManager.pushScreen(ScreenType.SETTINGS, FadeTransition(0.2f))
                    return true
                }
                // Quit to Main Menu
                if (x in 120f..420f && y in 250f..320f) {
                    game.screenManager.popScreen(InstantTransition)
                    game.screenManager.setScreen(ScreenType.MAIN_MENU, FadeTransition(0.3f))
                    return true
                }
                return false
            }
        })
    }

    override fun update(delta: Float) {}
    override fun draw(delta: Float) {}
}

// =============================================================================
// 11. MISSION CENTER SCREEN
// =============================================================================
class MissionCenterScreen(game: OnetMatch3Game) : BaseScreen(game) {
    init {
        backgroundColor = Color(0.14f, 0.17f, 0.22f, 1f)
        addInputProcessor(object : InputAdapter() {
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val touch = viewport.unproject(com.badlogic.gdx.math.Vector2(screenX.toFloat(), screenY.toFloat()))
                val x = touch.x
                val y = touch.y

                // Claim Mission 1
                if (x in 360f..480f && y in 660f..710f) {
                    game.economyService.claimMissionReward("daily_easy_1", 20)
                    game.soundService.playSound("assets/audio/claim.ogg")
                    return true
                }
                // Claim Mission 2
                if (x in 360f..480f && y in 540f..590f) {
                    game.economyService.claimMissionReward("daily_med_1", 30)
                    game.soundService.playSound("assets/audio/claim.ogg")
                    return true
                }
                // Claim Mission 3
                if (x in 360f..480f && y in 420f..470f) {
                    game.economyService.claimMissionReward("daily_hard_1", 50)
                    game.soundService.playSound("assets/audio/claim.ogg")
                    return true
                }
                // Switch to Achievements
                if (x in 280f..480f && y in 820f..870f) {
                    game.screenManager.setScreen(ScreenType.ACHIEVEMENT_CENTER, FadeTransition(0.2f))
                    return true
                }
                // Back
                if (x in 40f..140f && y in 880f..930f) {
                    game.screenManager.setScreen(ScreenType.MAIN_MENU, FadeTransition(0.25f))
                    return true
                }
                return false
            }
        })
    }

    override fun update(delta: Float) {}
    override fun draw(delta: Float) {}
}

// =============================================================================
// 12. ACHIEVEMENT CENTER SCREEN
// =============================================================================
class AchievementCenterScreen(game: OnetMatch3Game) : BaseScreen(game) {
    init {
        backgroundColor = Color(0.14f, 0.17f, 0.22f, 1f)
        addInputProcessor(object : InputAdapter() {
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val touch = viewport.unproject(com.badlogic.gdx.math.Vector2(screenX.toFloat(), screenY.toFloat()))
                val x = touch.x
                val y = touch.y

                // Claim Achievement Reward
                if (x in 360f..480f && y in 600f..660f) {
                    game.economyService.claimAchievementReward("first_win", 0, 100)
                    game.soundService.playSound("assets/audio/claim.ogg")
                    return true
                }
                // Switch to Missions
                if (x in 60f..260f && y in 820f..870f) {
                    game.screenManager.setScreen(ScreenType.MISSION_CENTER, FadeTransition(0.2f))
                    return true
                }
                // Back
                if (x in 40f..140f && y in 880f..930f) {
                    game.screenManager.setScreen(ScreenType.MAIN_MENU, FadeTransition(0.25f))
                    return true
                }
                return false
            }
        })
    }

    override fun update(delta: Float) {}
    override fun draw(delta: Float) {}
}

// =============================================================================
// 13. PLAYER PROFILE SCREEN
// =============================================================================
class PlayerProfileScreen(game: OnetMatch3Game) : BaseScreen(game) {
    init {
        backgroundColor = Color(0.13f, 0.16f, 0.22f, 1f)
        addInputProcessor(object : InputAdapter() {
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val touch = viewport.unproject(com.badlogic.gdx.math.Vector2(screenX.toFloat(), screenY.toFloat()))
                val x = touch.x
                val y = touch.y

                // Open Collection
                if (x in 120f..420f && y in 220f..290f) {
                    game.screenManager.setScreen(ScreenType.COSMETIC_COLLECTION, FadeTransition(0.25f))
                    return true
                }
                // Back
                if (x in 40f..140f && y in 880f..930f) {
                    game.screenManager.setScreen(ScreenType.MAIN_MENU, FadeTransition(0.25f))
                    return true
                }
                return false
            }
        })
    }

    override fun update(delta: Float) {}
    override fun draw(delta: Float) {}
}

// =============================================================================
// 14. COSMETIC COLLECTION SCREEN
// =============================================================================
class CosmeticCollectionScreen(game: OnetMatch3Game) : BaseScreen(game) {
    init {
        backgroundColor = Color(0.15f, 0.18f, 0.24f, 1f)
        addInputProcessor(object : InputAdapter() {
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val touch = viewport.unproject(com.badlogic.gdx.math.Vector2(screenX.toFloat(), screenY.toFloat()))
                val x = touch.x
                val y = touch.y

                // Equip Theme Sakura
                if (x in 60f..240f && y in 560f..660f) {
                    game.selectedTheme = "sakura"
                    game.cosmeticService.inventory.equip("theme_sakura")
                    return true
                }
                // Preview Theme Sakura
                if (x in 260f..440f && y in 560f..660f) {
                    game.screenManager.pushScreen(ScreenType.COSMETIC_PREVIEW, FadeTransition(0.2f))
                    return true
                }
                // Back
                if (x in 40f..140f && y in 880f..930f) {
                    game.screenManager.setScreen(ScreenType.PLAYER_PROFILE, FadeTransition(0.25f))
                    return true
                }
                return false
            }
        })
    }

    override fun update(delta: Float) {}
    override fun draw(delta: Float) {}
}

// =============================================================================
// 15. COSMETIC SHOP SCREEN
// =============================================================================
class CosmeticShopScreen(game: OnetMatch3Game) : BaseScreen(game) {
    init {
        backgroundColor = Color(0.16f, 0.18f, 0.25f, 1f)
        addInputProcessor(object : InputAdapter() {
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val touch = viewport.unproject(com.badlogic.gdx.math.Vector2(screenX.toFloat(), screenY.toFloat()))
                val x = touch.x
                val y = touch.y

                // Buy Bakery Theme (400 Gems)
                if (x in 320f..460f && y in 620f..670f) {
                    if (game.economyService.wallet.hasSufficientGems(400)) {
                        game.economyService.purchaseCosmetic("theme_reward_bakery", 400)
                        game.cosmeticService.inventory.unlock("theme_reward_bakery")
                        game.soundService.playSound("assets/audio/buy.ogg")
                    }
                    return true
                }
                // Buy Supporter Pack (IAP Billing)
                if (x in 320f..460f && y in 480f..530f) {
                    game.billingService.purchaseEntitlement("bundle_sakura_supporter")
                    return true
                }
                // Back
                if (x in 40f..140f && y in 880f..930f) {
                    game.screenManager.setScreen(ScreenType.MAIN_MENU, FadeTransition(0.25f))
                    return true
                }
                return false
            }
        })
    }

    override fun update(delta: Float) {}
    override fun draw(delta: Float) {}
}

// =============================================================================
// 16. SETTINGS SCREEN
// =============================================================================
class SettingsScreen(game: OnetMatch3Game) : BaseScreen(game) {
    init {
        backgroundColor = Color(0.12f, 0.14f, 0.18f, 1f)
        addInputProcessor(object : InputAdapter() {
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val touch = viewport.unproject(com.badlogic.gdx.math.Vector2(screenX.toFloat(), screenY.toFloat()))
                val x = touch.x
                val y = touch.y

                // SFX Mute Toggle
                if (x in 360f..460f && y in 660f..710f) {
                    game.soundService.isSfxMuted = !game.soundService.isSfxMuted
                    return true
                }
                // Music Mute Toggle
                if (x in 360f..460f && y in 560f..610f) {
                    game.soundService.isMusicMuted = !game.soundService.isMusicMuted
                    return true
                }
                // Back
                if (x in 40f..140f && y in 880f..930f) {
                    if (!game.screenManager.popScreen(FadeTransition(0.2f))) {
                        game.screenManager.setScreen(ScreenType.MAIN_MENU, FadeTransition(0.25f))
                    }
                    return true
                }
                return false
            }
        })
    }

    override fun update(delta: Float) {}
    override fun draw(delta: Float) {}
}

// =============================================================================
// 17. COSMETIC PREVIEW SCREEN
// =============================================================================
class CosmeticPreviewScreen(game: OnetMatch3Game) : BaseScreen(game) {
    init {
        backgroundColor = Color(0.10f, 0.12f, 0.16f, 0.95f)
        addInputProcessor(object : InputAdapter() {
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val touch = viewport.unproject(com.badlogic.gdx.math.Vector2(screenX.toFloat(), screenY.toFloat()))
                val x = touch.x
                val y = touch.y

                // Equip Selected Style
                if (x in 120f..420f && y in 240f..310f) {
                    game.cosmeticService.inventory.equip("theme_sakura")
                    game.screenManager.popScreen(FadeTransition(0.2f))
                    return true
                }
                // Close Preview
                if (x in 440f..500f && y in 880f..940f) {
                    game.screenManager.popScreen(FadeTransition(0.2f))
                    return true
                }
                return false
            }
        })
    }

    override fun update(delta: Float) {}
    override fun draw(delta: Float) {}
}
