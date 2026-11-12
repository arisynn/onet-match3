package game.gameplay

import game.gameplay.onet.*
import game.gameplay.shared.*
import game.gameplay.triplematch.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LogicTest {

    private fun blank(): List<List<Tile?>> =
        List(BOARD_ROWS) { List(BOARD_COLS) { null } }

    private fun mutableBlank(): Array<Array<Tile?>> =
        Array(BOARD_ROWS) { Array(BOARD_COLS) { null } }

    // =========================================================================
    // Original 9 Node.js Tests Ported to JUnit 5
    // =========================================================================

    @Test
    fun `test 1 Onet generation is deterministic, paired, and solvable at representative levels`() {
        for (level in listOf(1, 4, 8, 15)) {
            val b1 = generateBoard("candy", level, SeededRandom("level-$level"))
            val b2 = generateBoard("candy", level, SeededRandom("level-$level"))
            assertEquals(b1, b2, "Board generation must be deterministic for level $level")
            assertEquals(60, countRemaining(b1), "Board must have 60 tiles remaining at start")
            assertNotNull(findHint(b1), "Board must have a legal hint available")
            assertTrue(isBoardSolvable(b1), "Board must be solvable at level $level")
        }
    }

    @Test
    fun `test 2 Onet routes use at most two turns - wrong IDs and blocked endpoints fail`() {
        val b = mutableBlank()
        b[1][1] = Tile(id = "candy_0")
        b[1][3] = Tile(id = "candy_0")
        val boardList = b.map { it.toList() }

        val routeResult = getPath(boardList, 1, 1, 1, 3)
        assertNotNull(routeResult)
        val path = routeResult!!.path
        assertNotNull(path)
        assertTrue(path!!.size <= 4, "Path should have at most 4 points (<= 2 turns)")

        // Endpoint blocked by direction
        b[1][3] = Tile(id = "candy_0", openSide = "down")
        b[2][3] = Tile(id = "obstacle")
        val blockedResult = getPath(b.map { it.toList() }, 1, 1, 1, 3)
        assertNotNull(blockedResult)
        assertEquals("NO_PATH", blockedResult!!.error)

        // Mismatched IDs fail immediately
        b[1][3] = Tile(id = "candy_1")
        val mismatchResult = getPath(b.map { it.toList() }, 1, 1, 1, 3)
        assertNull(mismatchResult)
    }

    @Test
    fun `test 3 Onet split preserves footprints, sibling distinction, and counterpart pairing`() {
        val b = generateBoard("candy", 4, SeededRandom("split"))
        val a = Point(1, 1)
        val z = Point(1, 5)
        assertEquals("split_to_1x2", getMatchAction(b, a, z))

        val next = clearMatchedGroups(b, a, z, "candy")
        assertEquals(60, countRemaining(next))
        assertNotEquals(next[1][1]?.id, next[2][1]?.id, "Siblings must differ in ID")
        assertEquals(next[1][1]?.id, next[1][5]?.id, "Counterparts must share ID")
        assertEquals(next[2][1]?.id, next[2][5]?.id, "Counterparts must share ID")

        val size = groupSize(b[1][1])
        assertEquals(4, size.rows * size.cols, "Level 4 initial group footprint must be 4")
    }

    @Test
    fun `test 4 Onet matching locks during animation and resolves without mutating previous state`() {
        val board = mutableBlank()
        board[1][1] = Tile(id = "candy_0")
        board[1][2] = Tile(id = "candy_0")
        val boardList = board.map { it.toList() }

        var s = createOnetRound(board = boardList, theme = "candy")
        val beforeBoard = s.board

        s = selectTile(s, 1, 1, 0L)
        s = selectTile(s, 1, 2, 100L)
        assertNotNull(s.pending, "Match must set pending state")
        assertSame(s, selectTile(s, 1, 1), "Picks must be locked during pending animation")

        s = resolveMatch(s)
        assertEquals("won", s.phase)
        assertEquals(100, s.score)
        assertEquals(1, s.matches)
        assertEquals(beforeBoard, boardList, "Previous state must not be mutated")
    }

    @Test
    fun `test 5 Onet pause stops timer - timeout is terminal - shuffle retains a legal move`() {
        var s = createOnetRound(theme = "ocean", rng = SeededRandom("shuffle"))
        val paused = pauseOnetRound(s)
        assertSame(paused, tickOnetRound(paused), "Tick on paused state must return same state")

        val shuffled = shuffleRound(s, SeededRandom("shuffle-again"))
        assertNotNull(hintFor(shuffled), "Shuffled board must contain a legal hint")

        s = tickOnetRound(s, s.time)
        assertEquals("lost", s.phase)
        assertSame(s, selectTile(s, 1, 1), "Cannot select tiles when game is lost")
    }

    @Test
    fun `test 6 Triple generator preserves source 12x3 stack layout and multiples of three`() {
        val stacks = createStacks(tileIdsFor("candy"), SeededRandom("triple"))
        assertEquals(12, stacks.size)
        assertTrue(stacks.all { it.size == 3 })

        val counts = mutableMapOf<String, Int>()
        for (id in stacks.flatten()) {
            counts[id] = (counts[id] ?: 0) + 1
        }
        assertEquals(6, counts.size)
        assertTrue(counts.values.all { it % 3 == 0 })
        assertEquals(stacks, createStacks(tileIdsFor("candy"), SeededRandom("triple")))
    }

    @Test
    fun `test 7 Triple third tile clears immediately, scores 150, and wins with empty tray`() {
        var s = createTripleRound(emptyList(), stacks = listOf(listOf("a"), listOf("a"), listOf("a")))
        assertSame(s, pickTile(s, 0), "Picks before start must be ignored")

        s = startTripleRound(s)
        val beforeStacks = s.stacks
        for (i in 0..2) {
            s = pickTile(s, i)
        }

        assertEquals("won", s.phase)
        assertEquals(0, s.tray.size)
        assertEquals(150, tripleScore(s))
        assertEquals(listOf(listOf("a"), listOf("a"), listOf("a")), beforeStacks)
    }

    @Test
    fun `test 8 Triple seventh unmatched tile loses - a trio on seventh pick frees the tray first`() {
        // 7 unmatched tiles -> overflow failure
        var s1 = startTripleRound(
            createTripleRound(
                emptyList(),
                stacks = listOf("a", "b", "c", "d", "e", "f", "g").map { listOf(it) }
            )
        )
        for (i in 0 until 7) {
            s1 = pickTile(s1, i)
        }
        assertEquals("lost", s1.phase)

        // 7th pick completes a trio -> trio resolves before overflow check
        var s2 = startTripleRound(
            createTripleRound(
                emptyList(),
                stacks = listOf("a", "a", "b", "c", "d", "e", "a", "b").map { listOf(it) }
            )
        )
        for (i in 0 until 7) {
            s2 = pickTile(s2, i)
        }
        assertEquals("playing", s2.phase)
        assertEquals(4, s2.tray.size)
    }

    @Test
    fun `test 9 Triple undo is single-use and does not refund elapsed time - pause blocks picks`() {
        var s = startTripleRound(
            createTripleRound(
                emptyList(),
                stacks = listOf(listOf("a", "b"), listOf("c"))
            )
        )
        val before = s
        s = pickTile(s, 0)
        s = tickTripleRound(s, 5)
        s = undoPick(s)

        assertEquals(before.stacks, s.stacks)
        assertEquals(115, s.time)
        assertTrue(s.undoUsed)
        assertSame(s, undoPick(s), "Second undo must be ignored")

        val paused = pauseTripleRound(s)
        assertSame(paused, pickTile(paused, 0), "Pick on paused game must be ignored")
        assertSame(paused, tickTripleRound(paused), "Tick on paused game must be ignored")
        assertEquals("lost", tickTripleRound(s, 115).phase)
    }

    // =========================================================================
    // Edge-Case & Invariant Verification Tests
    // =========================================================================

    @Test
    fun `edge case SeededRandom bit-accurate SplitMix32 reproducibility`() {
        val rng1 = SeededRandom("level-1")
        val expected = doubleArrayOf(
            0.864232927095145,
            0.13979023671709,
            0.331050613662228,
            0.6919702005106956,
            0.22724962886422873
        )
        for (exp in expected) {
            val v = rng1.nextDouble()
            assertEquals(exp, v, 1e-12)
        }
    }

    @Test
    fun `edge case Onet combo timer window resets correctly`() {
        val board = mutableBlank()
        board[1][1] = Tile(id = "candy_0")
        board[1][2] = Tile(id = "candy_0")
        board[2][1] = Tile(id = "candy_1")
        board[2][2] = Tile(id = "candy_1")

        var s = createOnetRound(board = board.map { it.toList() }, theme = "candy")

        // First match at t=1000ms: combo = 1
        s = selectTile(s, 1, 1, 1000L)
        s = selectTile(s, 1, 2, 1000L)
        assertEquals(1, s.combo)
        assertEquals(100, s.pending?.points)
        s = resolveMatch(s)

        // Second match at t=3000ms (< COMBO_WINDOW_MS 4000ms): combo = 2
        s = selectTile(s, 2, 1, 3000L)
        s = selectTile(s, 2, 2, 3000L)
        assertEquals(2, s.combo)
        assertEquals(110, s.pending?.points) // 100 + (2 - 1) * 10
        s = resolveMatch(s)
        assertEquals(210, s.score)
        assertEquals(2, s.highestCombo)
        assertEquals("won", s.phase)
    }

    @Test
    fun `edge case Onet path around board perimeter in outer zero border`() {
        // Tiles on opposite sides of the board with obstacles in between
        val board = mutableBlank()
        board[1][1] = Tile(id = "candy_0")
        board[10][1] = Tile(id = "candy_0")
        // Fill row 2..9 column 1 with obstacles
        for (r in 2..9) {
            board[r][1] = Tile(id = "obstacle")
        }
        // Block column 2 so path cannot detour internally
        for (r in 1..10) {
            board[r][2] = Tile(id = "obstacle")
        }
        // Path should loop around column 0 (border)
        val route = getPath(board.map { it.toList() }, 1, 1, 10, 1)
        assertNotNull(route)
        assertNotNull(route?.path)
        assertTrue(route!!.path!!.any { it.c == 0 }, "Path must travel through perimeter border cell")
        assertTrue(route.path!!.size <= 4, "Must turn <= 2 times")
    }

    @Test
    fun `edge case Triple match cannot undo from won or lost state`() {
        var s = startTripleRound(createTripleRound(emptyList(), stacks = listOf(listOf("a"), listOf("a"), listOf("a"))))
        for (i in 0..2) s = pickTile(s, i)
        assertEquals("won", s.phase)
        assertSame(s, undoPick(s), "Cannot undo after game won")

        var sLost = startTripleRound(createTripleRound(emptyList(), stacks = listOf(listOf("a"))))
        sLost = tickTripleRound(sLost, 120)
        assertEquals("lost", sLost.phase)
        assertSame(sLost, undoPick(sLost), "Cannot undo after game lost")
    }

    @Test
    fun `edge case Onet result statistics calculation`() {
        val board = mutableBlank()
        board[1][1] = Tile(id = "candy_0")
        board[1][2] = Tile(id = "candy_0")
        var s = createOnetRound(board = board.map { it.toList() }, theme = "candy", seconds = 100)
        s = tickOnetRound(s, 20)
        s = selectTile(s, 1, 1, 0L)
        s = selectTile(s, 1, 2, 0L)
        s = resolveMatch(s)

        val res = onetResult(s)
        assertEquals("won", res.phase)
        assertEquals(100, res.score)
        assertEquals(1, res.matches)
        assertEquals(0, res.mistakes)
        assertTrue(res.isFlawless)
        assertEquals(80, res.remainingTime)
        assertEquals(20000L, res.timeElapsed)
    }

    @Test
    fun `edge case Triple result statistics calculation`() {
        var s = startTripleRound(createTripleRound(emptyList(), stacks = listOf(listOf("a"), listOf("a"), listOf("a")), seconds = 120))
        s = tickTripleRound(s, 30)
        for (i in 0..2) s = pickTile(s, i)

        val res = tripleResult(s)
        assertEquals("won", res.phase)
        assertEquals(150, res.score)
        assertEquals(1, res.matches)
        assertEquals(0, res.mistakes)
        assertEquals(30000L, res.timeElapsed)
    }
}
