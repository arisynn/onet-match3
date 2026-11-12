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
import game.missions.MissionStatus
import game.missions.MissionObjective
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

    init {
        backgroundColor = Color(0.08f, 0.08f, 0.11f, 1f)
    }

    override fun update(delta: Float) {
        timer += delta
        if (timer >= 0.5f) {
            game.screenManager.setScreen(ScreenType.LOADING, FadeTransition(0.3f))
        }
    }

    override fun draw(delta: Float) { ui {
        title("ONET + TRIPLE", "A little pause. A perfect match.")
        tile("candy_0", 156f, 490f, 100f, 110f)
        tile("candy_0", 282f, 490f, 100f, 110f)
        text("Starting...", 20f, 340f, width = 500f)
    } }
}

// =============================================================================
// 2. LOADING SCREEN
// =============================================================================
class LoadingScreen(game: OnetMatch3Game) : BaseScreen(game) {
    private var simulatedProgress = 0f

    init {
        backgroundColor = Color(0.10f, 0.12f, 0.16f, 1f)
    }

    override fun update(delta: Float) {
        simulatedProgress = min(1f, simulatedProgress + delta * 2.5f)
        if (simulatedProgress >= 1f) {
            game.screenManager.setScreen(ScreenType.MAIN_MENU, FadeTransition(0.4f))
        }
    }

    override fun draw(delta: Float) { ui {
        title("Getting ready", "Preparing your next puzzle")
        box(80f, 480f, 380f, 18f, Color.DARK_GRAY)
        box(80f, 480f, 380f * simulatedProgress, 18f, Color.valueOf("A7DCC2"))
    } }
}

// =============================================================================
// 3. MAIN MENU SCREEN
// =============================================================================
class MainMenuScreen(game: OnetMatch3Game) : BaseScreen(game) {
    init {
        backgroundColor = Color(0.14f, 0.16f, 0.22f, 1f)
        addInputProcessor(object : InputAdapter() {
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val touch = inputPoint(screenX, screenY)
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
    override fun draw(delta: Float) { ui {
        title("ONET + TRIPLE", "Find your moment of calm")
        tile("candy_2", 160f, 595f, 95f, 100f)
        tile("candy_4", 285f, 595f, 95f, 100f)
        button("Play", 120f, 480f, 300f, 80f)
        button("Missions", 120f, 380f, 300f, 70f)
        button("Collection shop", 120f, 280f, 300f, 70f)
        button("Profile", 120f, 180f, 140f, 70f)
        button("Settings", 280f, 180f, 140f, 70f)
        text("Onet + Triple Match 1.0.2", 20f, 85f, 1.1f, width = 500f)
    } }
}

// =============================================================================
// 4. GAME MODE SELECTION SCREEN
// =============================================================================
class ModeSelectScreen(game: OnetMatch3Game) : BaseScreen(game) {
    init {
        backgroundColor = Color(0.13f, 0.15f, 0.20f, 1f)
        addInputProcessor(object : InputAdapter() {
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val touch = inputPoint(screenX, screenY)
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
    override fun draw(delta: Float) { ui {
        back()
        title("Choose your puzzle", "Two ways to make a match")
        button("Onet Connect", 80f, 520f, 380f, 160f)
        text("Connect pairs with up to two turns", 20f, 505f, 1.1f, width = 500f)
        button("Triple Match", 80f, 320f, 380f, 160f)
        text("Collect triples. Keep your tray clear.", 20f, 300f, 1.1f, width = 500f)
    } }
}

// =============================================================================
// 7. LEVEL SELECTION SCREEN
// =============================================================================
class LevelSelectScreen(game: OnetMatch3Game) : BaseScreen(game) {
    init {
        backgroundColor = Color(0.12f, 0.15f, 0.20f, 1f)
        addInputProcessor(object : InputAdapter() {
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val touch = inputPoint(screenX, screenY)
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
    override fun draw(delta: Float) { ui {
        back()
        text("Choose a level", 20f, 835f, 2.2f, width = 500f)
        for (i in 0 until 20) {
            button("${i + 1}", 54f + (i % 4) * 110f, 654f - (i / 4) * 100f, 102f, 88f)
        }
        text("Match every pair before time runs out", 20f, 190f, 1.1f, width = 500f)
    } }
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
    private var hintsUsed = 0
    private var shufflesUsed = 0

    init {
        backgroundColor = Color(0.15f, 0.18f, 0.23f, 1f)
        addInputProcessor(object : InputAdapter() {
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val touch = inputPoint(screenX, screenY)
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
                    if (highlightedHint != null) hintsUsed++
                    return true
                }
                // Shuffle button
                if (x in 360f..480f && y in 80f..140f) {
                    roundState = shuffleRound(roundState, SeededRandom(System.currentTimeMillis()))
                    shufflesUsed++
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

    override fun onScreenShown() { game.activeGameType = ScreenType.ONET_GAME }
    override fun pause() { roundState = pauseOnetRound(roundState, true) }
    override fun resume() { roundState = pauseOnetRound(roundState, false) }

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
            game.playerProfile = game.playerProfile.copy(stats = game.playerProfile.stats.recordOnetRound(
                true, roundState.matches * 2, roundState.highestCombo, hintsUsed, shufflesUsed,
                roundState.time.toFloat() / roundState.duration, game.selectedTheme))
            game.recordWin(MissionObjective.WIN_ONET_ROUNDS, roundState.matches * 2)
            game.playerProfile = game.progressionEngine.applyXp(game.playerProfile, 100L).first
            game.screenManager.setScreen(ScreenType.VICTORY, FadeTransition(0.4f))
        } else if (roundState.phase == "lost") {
            game.lastScore = roundState.score
            game.lastResultReason = "Time's Up!"
            game.screenManager.setScreen(ScreenType.DEFEAT, FadeTransition(0.4f))
        }
    }

    override fun draw(delta: Float) { ui {
        text("ONET  /  LEVEL ${game.selectedLevel}", 40f, 930f, 1.5f)
        button("Pause", 440f, 880f, 70f, 60f)
        text("Score ${roundState.score}    Time ${roundState.time}s", 45f, 860f, 1.4f)
        for (r in 1..10) for (c in 1..6) {
            val cell = roundState.board[r][c] ?: continue
            if (cell.isSlave) continue
            val size = groupSize(cell)
            val selected = roundState.selected == Point(r, c) ||
                highlightedHint?.let { it.p1 == Point(r, c) || it.p2 == Point(r, c) } == true
            val x = 45f + (c - 1) * 75f
            val y = 820f - (r - 1 + size.rows) * 64f
            tile(cell.id, x + 3f, y + 3f, size.cols * 75f - 6f, size.rows * 64f - 6f, selected)
            cell.openSide?.let { direction ->
                val arrow = when (direction) { "up" -> "^"; "down" -> "v"; "left" -> "<"; else -> ">" }
                text(arrow, x + 7f, y + 20f, 1.1f, Color.valueOf("263B41"))
            }
        }
        activePath?.zipWithNext()?.forEach { (a, b) ->
            line(45f + (a.c - 0.5f) * 75f, 820f - (a.r - 0.5f) * 64f,
                 45f + (b.c - 0.5f) * 75f, 820f - (b.r - 0.5f) * 64f)
        }
        button("Hint", 60f, 80f, 120f, 60f)
        button("Shuffle", 360f, 80f, 120f, 60f)
        text("Tap two matching tiles", 20f, 45f, 1.1f, width = 500f)
    } }
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
                val touch = inputPoint(screenX, screenY)
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

    override fun onScreenShown() { game.activeGameType = ScreenType.TRIPLE_MATCH_GAME }
    override fun pause() { roundState = pauseTripleRound(roundState, true) }
    override fun resume() { roundState = pauseTripleRound(roundState, false) }

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
            game.playerProfile = game.playerProfile.copy(stats = game.playerProfile.stats.recordTripleMatchRound(
                true, roundState.cleared, if (roundState.undoUsed) 1 else 0,
                roundState.time.toFloat() / roundState.duration, game.selectedTheme))
            game.recordWin(MissionObjective.WIN_TRIPLE_MATCH_ROUNDS, roundState.cleared * 3)
            game.playerProfile = game.progressionEngine.applyXp(game.playerProfile, 120L).first
            game.screenManager.setScreen(ScreenType.VICTORY, FadeTransition(0.4f))
        } else if (roundState.phase == "lost") {
            game.lastScore = tripleScore(roundState)
            game.lastResultReason = if (roundState.tray.size >= TRAY_CAPACITY) "Tray Full!" else "Time's Up!"
            game.screenManager.setScreen(ScreenType.DEFEAT, FadeTransition(0.4f))
        }
    }

    override fun draw(delta: Float) { ui {
        text("TRIPLE MATCH", 40f, 930f, 1.7f)
        button("Pause", 440f, 880f, 70f, 60f)
        text("Score ${tripleScore(roundState)}    Time ${roundState.time}s", 50f, 850f, 1.4f)
        roundState.stacks.forEachIndexed { i, stack ->
            if (stack.isNotEmpty()) {
                val x = 55f + (i % 4) * 110f
                val y = 650f - (i / 4) * 150f
                tile(stack.last(), x, y, 96f, 115f)
                text("${stack.size} left", x, y - 10f, 1.1f)
            }
        }
        text("TRAY  ${roundState.tray.size} / 7", 50f, 285f, 1.4f)
        for (i in 0 until 7) {
            val x = 40f + i * 66f
            val id = roundState.tray.getOrNull(i)
            if (id != null) tile(id, x, 185f, 60f, 72f) else box(x, 185f, 60f, 72f, Color.valueOf("324B53"))
        }
        button(if (roundState.undoUsed) "Used" else "Undo", 60f, 60f, 120f, 60f, !roundState.undoUsed && roundState.undo != null)
        text("Match three. Avoid a full tray.", 200f, 95f, 1.05f)
    } }
}

// =============================================================================
// 8. VICTORY SCREEN
// =============================================================================
class VictoryScreen(game: OnetMatch3Game) : BaseScreen(game) {
    init {
        backgroundColor = Color(0.10f, 0.16f, 0.12f, 0.95f)
        addInputProcessor(object : InputAdapter() {
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val touch = inputPoint(screenX, screenY)
                val x = touch.x
                val y = touch.y

                // Next Level
                if (x in 120f..420f && y in 380f..450f) {
                    game.selectedLevel += 1
                    game.screenManager.setScreen(game.activeGameType, FadeTransition(0.3f))
                    return true
                }
                // Replay
                if (x in 120f..420f && y in 290f..360f) {
                    game.screenManager.setScreen(game.activeGameType, FadeTransition(0.3f))
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
    override fun draw(delta: Float) { ui {
        title("Beautifully matched!", game.lastResultReason)
        text("Score ${game.lastScore}", 20f, 620f, 2.2f, width = 500f)
        text("${game.lastStars} / 3 stars", 20f, 550f, 1.7f, width = 500f)
        button("Next puzzle", 120f, 380f, 300f, 70f)
        button("Play again", 120f, 290f, 300f, 70f)
        button("Main menu", 120f, 200f, 300f, 70f)
    } }
}

// =============================================================================
// 9. DEFEAT SCREEN
// =============================================================================
class DefeatScreen(game: OnetMatch3Game) : BaseScreen(game) {
    init {
        backgroundColor = Color(0.20f, 0.10f, 0.10f, 0.95f)
        addInputProcessor(object : InputAdapter() {
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val touch = inputPoint(screenX, screenY)
                val x = touch.x
                val y = touch.y

                // Retry
                if (x in 120f..420f && y in 340f..420f) {
                    game.screenManager.setScreen(game.activeGameType, FadeTransition(0.3f))
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
    override fun draw(delta: Float) { ui {
        title("Try a fresh start", game.lastResultReason)
        text("Score ${game.lastScore}", 20f, 600f, 2.2f, width = 500f)
        button("Try again", 120f, 340f, 300f, 80f)
        button("Main menu", 120f, 240f, 300f, 80f)
    } }
}

// =============================================================================
// 10. PAUSE SCREEN
// =============================================================================
class PauseScreen(game: OnetMatch3Game) : BaseScreen(game) {
    init {
        backgroundColor = Color(0.08f, 0.10f, 0.14f, 0.90f)
        addInputProcessor(object : InputAdapter() {
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val touch = inputPoint(screenX, screenY)
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
                    game.screenManager.setScreen(game.activeGameType, FadeTransition(0.25f))
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
    override fun draw(delta: Float) { ui {
        title("Take a breather", "Your puzzle is waiting")
        button("Resume", 120f, 520f, 300f, 70f)
        button("Restart", 120f, 430f, 300f, 70f)
        button("Settings", 120f, 340f, 300f, 70f)
        button("Main menu", 120f, 250f, 300f, 70f)
    } }
}

// =============================================================================
// 11. MISSION CENTER SCREEN
// =============================================================================
class MissionCenterScreen(game: OnetMatch3Game) : BaseScreen(game) {
    init {
        backgroundColor = Color(0.14f, 0.17f, 0.22f, 1f)
        addInputProcessor(object : InputAdapter() {
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val touch = inputPoint(screenX, screenY)
                val x = touch.x
                val y = touch.y

                // Claim Mission 1
                if (x in 360f..480f && y in 660f..710f) {
                    game.missionService.getActiveMissions().getOrNull(0)?.takeIf { it.isClaimable }?.let {
                        game.missionService.claimMission(it.id, game.economyService)
                    }
                    game.soundService.playSound("assets/audio/claim.ogg")
                    return true
                }
                // Claim Mission 2
                if (x in 360f..480f && y in 540f..590f) {
                    game.missionService.getActiveMissions().getOrNull(1)?.takeIf { it.isClaimable }?.let {
                        game.missionService.claimMission(it.id, game.economyService)
                    }
                    game.soundService.playSound("assets/audio/claim.ogg")
                    return true
                }
                // Claim Mission 3
                if (x in 360f..480f && y in 420f..470f) {
                    game.missionService.getActiveMissions().getOrNull(2)?.takeIf { it.isClaimable }?.let {
                        game.missionService.claimMission(it.id, game.economyService)
                    }
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

    override fun onScreenShown() { game.missionService.refreshMissions(System.currentTimeMillis()) }

    override fun update(delta: Float) {}
    override fun draw(delta: Float) { ui {
        back()
        text("Missions", 180f, 920f, 2f)
        button("Achievements", 280f, 820f, 200f, 50f)
        game.missionService.getActiveMissions().take(3).forEachIndexed { i, mission ->
            val y = 710f - i * 120f
            text(mission.title, 50f, y + 35f, 1.25f)
            text("${mission.currentCount} / ${mission.targetCount}    ${mission.gemReward} gems", 50f, y - 2f, 1.1f)
            button(if (mission.status == MissionStatus.CLAIMED) "Claimed" else "Claim", 360f, y - 50f, 120f, 50f, mission.isClaimable)
        }
        text("Complete goals to claim gems", 20f, 230f, 1.25f, width = 500f)
    } }
}

// =============================================================================
// 12. ACHIEVEMENT CENTER SCREEN
// =============================================================================
class AchievementCenterScreen(game: OnetMatch3Game) : BaseScreen(game) {
    init {
        backgroundColor = Color(0.14f, 0.17f, 0.22f, 1f)
        addInputProcessor(object : InputAdapter() {
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val touch = inputPoint(screenX, screenY)
                val x = touch.x
                val y = touch.y

                // Claim Achievement Reward
                if (x in 360f..480f && y in 600f..660f) {
                    game.achievementService.getAchievement("onet_master")?.let { achievement ->
                        achievement.tiers.firstOrNull { achievement.isTierClaimable(it.tierIndex) }?.let { tier ->
                            game.achievementService.claimTier(achievement.id, tier.tierIndex, game.economyService)
                        }
                    }
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
    override fun draw(delta: Float) { ui {
        back()
        text("Achievements", 180f, 920f, 1.8f)
        button("Missions", 60f, 820f, 200f, 50f)
        text("Milestones", 50f, 730f, 1.8f)
        text("Keep matching to build your collection.", 50f, 670f, 1.1f)
        game.achievementService.getAchievement("onet_master")?.let { achievement ->
            text("Onet wins: ${achievement.currentValue}", 50f, 610f, 1.4f)
            val claimable = achievement.tiers.any { achievement.isTierClaimable(it.tierIndex) }
            button("Claim", 360f, 600f, 120f, 60f, claimable)
        }
    } }
}

// =============================================================================
// 13. PLAYER PROFILE SCREEN
// =============================================================================
class PlayerProfileScreen(game: OnetMatch3Game) : BaseScreen(game) {
    init {
        backgroundColor = Color(0.13f, 0.16f, 0.22f, 1f)
        addInputProcessor(object : InputAdapter() {
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val touch = inputPoint(screenX, screenY)
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
    override fun draw(delta: Float) { ui {
        back()
        title("Your journey", "One puzzle at a time")
        text("Level ${game.playerProfile.level}", 20f, 650f, 2f, width = 500f)
        text("XP ${game.playerProfile.currentXp}", 20f, 580f, 1.7f, width = 500f)
        text("Gems ${game.economyService.wallet.gemBalance}", 20f, 510f, 1.7f, width = 500f)
        button("Tile collection", 120f, 220f, 300f, 70f)
    } }
}

// =============================================================================
// 14. COSMETIC COLLECTION SCREEN
// =============================================================================
class CosmeticCollectionScreen(game: OnetMatch3Game) : BaseScreen(game) {
    init {
        backgroundColor = Color(0.15f, 0.18f, 0.24f, 1f)
        addInputProcessor(object : InputAdapter() {
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val touch = inputPoint(screenX, screenY)
                val x = touch.x
                val y = touch.y

                // Equip Theme Sakura
                if (x in 60f..240f && y in 560f..660f) {
                    game.selectedTheme = "candy"
                    game.cosmeticService.inventory.equip("theme_candy")
                    return true
                }
                // Preview Theme Sakura
                if (x in 260f..440f && y in 560f..660f) {
                    game.selectedTheme = "ocean"
                    game.cosmeticService.inventory.equip("theme_ocean")
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
    override fun draw(delta: Float) { ui {
        back()
        title("Tile collection", "Choose your favorite free theme")
        button("Candy", 60f, 560f, 180f, 100f)
        button("Ocean", 260f, 560f, 180f, 100f)
        text("Selected: ${game.selectedTheme}", 20f, 470f, 1.5f, width = 500f)
    } }
}

// =============================================================================
// 15. COSMETIC SHOP SCREEN
// =============================================================================
class CosmeticShopScreen(game: OnetMatch3Game) : BaseScreen(game) {
    init {
        backgroundColor = Color(0.16f, 0.18f, 0.25f, 1f)
        addInputProcessor(object : InputAdapter() {
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val touch = inputPoint(screenX, screenY)
                val x = touch.x
                val y = touch.y

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
    override fun draw(delta: Float) { ui {
        back()
        title("Collection shop", "More styles are on the way")
        tile("candy_1", 155f, 540f, 100f, 110f)
        tile("ocean_1", 285f, 540f, 100f, 110f)
        text("Purchases are not available in this build.", 20f, 430f, 1.15f, width = 500f)
        text("Enjoy Candy and Ocean in your collection.", 20f, 385f, 1.1f, width = 500f)
    } }
}

// =============================================================================
// 16. SETTINGS SCREEN
// =============================================================================
class SettingsScreen(game: OnetMatch3Game) : BaseScreen(game) {
    init {
        backgroundColor = Color(0.12f, 0.14f, 0.18f, 1f)
        addInputProcessor(object : InputAdapter() {
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val touch = inputPoint(screenX, screenY)
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
    override fun draw(delta: Float) { ui {
        back()
        title("Settings", "Make yourself comfortable")
        text("Sound effects", 60f, 700f, 1.5f)
        button(if (game.soundService.isSfxMuted) "Off" else "On", 360f, 660f, 100f, 50f)
        text("Music", 60f, 600f, 1.5f)
        button(if (game.soundService.isMusicMuted) "Off" else "On", 360f, 560f, 100f, 50f)
        text("Audio tracks are not bundled yet.", 60f, 450f, 1.15f)
    } }
}

// =============================================================================
// 17. COSMETIC PREVIEW SCREEN
// =============================================================================
class CosmeticPreviewScreen(game: OnetMatch3Game) : BaseScreen(game) {
    init {
        backgroundColor = Color(0.10f, 0.12f, 0.16f, 0.95f)
        addInputProcessor(object : InputAdapter() {
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val touch = inputPoint(screenX, screenY)
                val x = touch.x
                val y = touch.y

                // Equip Selected Style
                if (x in 120f..420f && y in 240f..310f) {
                    game.cosmeticService.inventory.equip("theme_candy")
                    game.selectedTheme = "candy"
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
    override fun draw(delta: Float) { ui {
        button("Close", 440f, 880f, 60f, 60f)
        title("Candy tiles", "A sweet little collection")
        tile("candy_0", 145f, 500f, 110f, 120f)
        tile("candy_3", 285f, 500f, 110f, 120f)
        button("Use Candy", 120f, 240f, 300f, 70f)
    } }
}
