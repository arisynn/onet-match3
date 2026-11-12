package game.security

import game.economy.*
import game.gameplay.onet.*
import game.gameplay.shared.SeededRandom
import game.gameplay.triplematch.*
import game.progression.PlayerProfile
import game.progression.PlayerStats
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Save/Restore State Regression Suite.
 * Validates that mid-match states, progression profiles, and transaction ledgers
 * can be serialized, persisted, and faithfully restored with zero data corruption
 * or gameplay desynchronization.
 */
class SaveRestoreRegressionTest {

    // =========================================================================
    // Onet Round State Save & Restore
    // =========================================================================

    @Test
    fun `onet round mid-game state serialization and restoration preserves exact board and gameplay continuity`() {
        // 1. Initialize deterministic round
        val rng = SeededRandom("onet_save_seed")
        var original = createOnetRound(theme = "candy", level = 1, rng = rng, seconds = 180)

        // Find and execute first match
        val hint1 = hintFor(original)
        assertNotNull(hint1, "Board should have an initial legal match")
        original = selectTile(original, hint1!!.p1.r, hint1.p1.c, now = 1000L)
        original = selectTile(original, hint1.p2.r, hint1.p2.c, now = 1200L)
        assertNotNull(original.pending)
        original = resolveMatch(original)
        original = tickOnetRound(original, seconds = 15)

        assertEquals(1, original.matches)
        assertEquals(100, original.score)
        assertEquals(165, original.time)

        // 2. Simulate Save State Serialization (deep copy / snapshot mapping)
        val savedBoard = original.board.map { row ->
            row.map { tile ->
                tile?.let {
                    Tile(
                        id = it.id,
                        themeKey = it.themeKey,
                        openSide = it.openSide,
                        isSlave = it.isSlave,
                        masterR = it.masterR,
                        masterC = it.masterC
                    )
                }
            }
        }

        val restoredState = OnetRoundState(
            theme = original.theme,
            level = original.level,
            board = savedBoard,
            phase = original.phase,
            paused = original.paused,
            selected = original.selected?.let { Point(it.r, it.c) },
            pending = original.pending?.let {
                PendingMatch(
                    a = Point(it.a.r, it.a.c),
                    b = Point(it.b.r, it.b.c),
                    action = it.action,
                    path = it.path.map { pt -> Point(pt.r, pt.c) },
                    points = it.points
                )
            },
            score = original.score,
            combo = original.combo,
            highestCombo = original.highestCombo,
            lastMatchAt = original.lastMatchAt,
            matches = original.matches,
            mistakes = original.mistakes,
            splits = original.splits,
            time = original.time,
            duration = original.duration,
            event = null
        )

        // 3. Verify structural fidelity
        assertEquals(original.board, restoredState.board, "Restored board must match original board")
        assertEquals(original.score, restoredState.score)
        assertEquals(original.time, restoredState.time)
        assertEquals(original.matches, restoredState.matches)
        assertEquals(original.combo, restoredState.combo)

        // 4. Continue gameplay on restored state
        val hint2 = hintFor(restoredState)
        assertNotNull(hint2, "Restored board must continue to yield valid hints")

        var resumed = selectTile(restoredState, hint2!!.p1.r, hint2.p1.c, now = 2000L)
        resumed = selectTile(resumed, hint2.p2.r, hint2.p2.c, now = 2100L)
        assertNotNull(resumed.pending)
        resumed = resolveMatch(resumed)

        assertEquals(2, resumed.matches)
        assertTrue(resumed.score >= 200)

        // Shuffle verification on restored state
        val shuffleRng = SeededRandom("shuffle_restored")
        val shuffled = shuffleRound(resumed, shuffleRng)
        assertNotNull(hintFor(shuffled), "Shuffling restored board must retain playable moves")
    }

    // =========================================================================
    // Triple Match State Save & Restore
    // =========================================================================

    @Test
    fun `triple match state serialization preserves tray, stacks, and single-use undo`() {
        val stacks = listOf(
            listOf("apple", "banana", "cherry"),
            listOf("apple", "grape", "orange"),
            listOf("apple", "cherry", "banana")
        )
        var original = createTripleRound(emptyList(), stacks = stacks, seconds = 120)
        original = startTripleRound(original)

        // Pick one tile into tray
        original = pickTile(original, stackIndex = 0)
        original = tickTripleRound(original, seconds = 10)
        assertEquals(listOf("cherry"), original.tray)
        assertEquals(110, original.time)
        assertNotNull(original.undo)
        assertFalse(original.undoUsed)

        // Simulate serialization & restoration
        val savedStacks = original.stacks.map { it.toList() }
        val savedTray = original.tray.toList()
        val savedUndo = original.undo?.let {
            UndoState(
                stacks = it.stacks.map { s -> s.toList() },
                tray = it.tray.toList(),
                cleared = it.cleared
            )
        }

        val restored = TripleRoundState(
            stacks = savedStacks,
            tray = savedTray,
            cleared = original.cleared,
            phase = original.phase,
            paused = original.paused,
            time = original.time,
            duration = original.duration,
            undo = savedUndo,
            undoUsed = original.undoUsed
        )

        assertEquals(original.tray, restored.tray)
        assertEquals(original.stacks, restored.stacks)
        assertEquals(original.time, restored.time)
        assertEquals(original.undoUsed, restored.undoUsed)

        // Perform undo on restored state
        val undone = undoPick(restored)
        assertTrue(undone.undoUsed)
        assertEquals(emptyList<String>(), undone.tray, "Tray should be empty after undo")
        assertEquals(listOf("apple", "banana", "cherry"), undone.stacks[0], "Stack 0 restored")
        assertEquals(110, undone.time, "Undo must NOT refund elapsed timer")

        // Second undo attempt on restored state must be blocked
        assertSame(undone, undoPick(undone), "Subsequent undo must be rejected")
    }

    // =========================================================================
    // Player Stats & Progression Save/Restore
    // =========================================================================

    @Test
    fun `player progression profile restores without loss of lifetime statistics`() {
        var stats = PlayerStats()
        stats = stats.recordOnetRound(
            won = true,
            tilesMatched = 60,
            maxCombo = 5,
            hintsUsedInRound = 0,
            shufflesUsedInRound = 0,
            remainingTimeRatio = 0.65f,
            theme = "candy"
        )
        stats = stats.recordTripleMatchRound(
            won = true,
            triosMatched = 12,
            undosUsedInRound = 0,
            remainingTimeRatio = 0.55f,
            theme = "ocean"
        )

        val profile = PlayerProfile(
            playerId = "player_saved_123",
            level = 4,
            currentXp = 1250L,
            stats = stats
        )

        // Simulate save and rehydrate
        val restoredProfile = PlayerProfile(
            playerId = profile.playerId,
            level = profile.level,
            currentXp = profile.currentXp,
            stats = profile.stats.copy()
        )

        assertEquals(profile.playerId, restoredProfile.playerId)
        assertEquals(profile.level, restoredProfile.level)
        assertEquals(profile.currentXp, restoredProfile.currentXp)
        assertEquals(2, restoredProfile.stats.totalRoundsWon)
        assertEquals(96L, restoredProfile.stats.totalTilesMatched) // 60 + 12*3
        assertEquals(2, restoredProfile.stats.flawlessRoundsCount)
        assertEquals(2, restoredProfile.stats.fastRoundsCount)
        assertEquals(1, restoredProfile.stats.themePlays["candy"])
        assertEquals(1, restoredProfile.stats.themePlays["ocean"])
    }

    // =========================================================================
    // Transaction Ledger Snapshot & Chaining Verification
    // =========================================================================

    @Test
    fun `restored ledger retains unbroken cryptographic chain and validates integrity`() {
        val originalLedger = TransactionLedger()

        originalLedger.record(
            type = TransactionType.CREDIT,
            currency = Currency.GEM,
            amount = 15,
            source = TransactionSource.MISSION_CLAIM,
            sourceId = "m_snap_1",
            idempotencyKey = "tx_snap_1"
        )
        originalLedger.record(
            type = TransactionType.CREDIT,
            currency = Currency.GEM,
            amount = 25,
            source = TransactionSource.ACHIEVEMENT_CLAIM,
            sourceId = "m_snap_2",
            idempotencyKey = "tx_snap_2"
        )
        originalLedger.record(
            type = TransactionType.DEBIT,
            currency = Currency.GEM,
            amount = 20,
            source = TransactionSource.COSMETIC_PURCHASE,
            sourceId = "skin_01",
            idempotencyKey = "tx_snap_3"
        )

        assertEquals(20, originalLedger.getBalance())
        assertTrue(originalLedger.verifyIntegrity())

        // Recreate ledger from saved transactions
        val savedTxList = originalLedger.getTransactions()
        val restoredLedger = TransactionLedger(savedTxList)

        assertEquals(20, restoredLedger.getBalance())
        assertEquals(3, restoredLedger.getTransactionCount())
        assertTrue(restoredLedger.verifyIntegrity(), "Restored ledger must have 100% cryptographic integrity")

        // Replaying already recorded transaction fails idempotently
        val duplicateAttempt = restoredLedger.record(
            type = TransactionType.CREDIT,
            currency = Currency.GEM,
            amount = 15,
            source = TransactionSource.MISSION_CLAIM,
            sourceId = "m_snap_1",
            idempotencyKey = "tx_snap_1"
        )
        assertTrue(duplicateAttempt is TransactionResult.AlreadyProcessed)
        assertEquals(20, restoredLedger.getBalance(), "Balance must remain exactly 20")
    }
}
