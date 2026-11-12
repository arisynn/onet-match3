package game.gameplay.onet

import game.gameplay.shared.COLS
import game.gameplay.shared.ROWS
import game.gameplay.shared.RandomGenerator

object OnetEngine {
    const val BOARD_ROWS = ROWS
    const val BOARD_COLS = COLS

    fun generateBoard(
        themeKey: String,
        level: Int = 1,
        rng: RandomGenerator = RandomGenerator { Math.random() }
    ): List<List<Tile?>> = game.gameplay.onet.generateBoard(themeKey, level, rng)

    fun getPath(
        board: List<List<Tile?>>,
        r1: Int,
        c1: Int,
        r2: Int,
        c2: Int,
        ignoredTiles: List<Point> = emptyList()
    ): PathResult? = game.gameplay.onet.getPath(board, r1, c1, r2, c2, ignoredTiles)

    fun findHint(board: List<List<Tile?>>): Hint? = game.gameplay.onet.findHint(board)

    fun countRemaining(board: List<List<Tile?>>): Int = game.gameplay.onet.countRemaining(board)

    fun isBoardSolvable(board: List<List<Tile?>>): Boolean = game.gameplay.onet.isBoardSolvable(board)

    fun getMatchAction(board: List<List<Tile?>>, a: Point, b: Point? = null): String =
        game.gameplay.onet.getMatchAction(board, a, b)

    fun clearMatchedGroups(
        board: List<List<Tile?>>,
        a: Point,
        b: Point,
        themeKey: String? = null
    ): List<List<Tile?>> = game.gameplay.onet.clearMatchedGroups(board, a, b, themeKey)

    fun guaranteedShuffle(
        board: List<List<Tile?>>,
        rng: RandomGenerator = RandomGenerator { Math.random() },
        level: Int = 1
    ): List<List<Tile?>> = game.gameplay.onet.guaranteedShuffle(board, rng, level)

    fun createRound(
        theme: String = "sweets",
        level: Int = 1,
        rng: RandomGenerator = RandomGenerator { Math.random() },
        board: List<List<Tile?>>? = null,
        seconds: Int? = null
    ): OnetRoundState = createOnetRound(theme, level, rng, board, seconds)

    fun selectTile(
        state: OnetRoundState,
        r: Int,
        c: Int,
        now: Long = System.currentTimeMillis()
    ): OnetRoundState = game.gameplay.onet.selectTile(state, r, c, now)

    fun resolveMatch(state: OnetRoundState): OnetRoundState = game.gameplay.onet.resolveMatch(state)

    fun tickRound(state: OnetRoundState, seconds: Int = 1): OnetRoundState =
        tickOnetRound(state, seconds)

    fun pauseRound(state: OnetRoundState, paused: Boolean = true): OnetRoundState =
        pauseOnetRound(state, paused)

    fun hintFor(state: OnetRoundState): Hint? = game.gameplay.onet.hintFor(state)

    fun shuffleRound(
        state: OnetRoundState,
        rng: RandomGenerator = RandomGenerator { Math.random() }
    ): OnetRoundState = game.gameplay.onet.shuffleRound(state, rng)

    fun needsShuffle(state: OnetRoundState): Boolean = game.gameplay.onet.needsShuffle(state)

    fun result(state: OnetRoundState): OnetResult = onetResult(state)
}
