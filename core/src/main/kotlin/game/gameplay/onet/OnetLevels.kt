package game.gameplay.onet

import game.gameplay.shared.BOARD_COLS
import game.gameplay.shared.BOARD_ROWS
import game.gameplay.shared.RandomGenerator
import game.gameplay.shared.shuffleList
import game.gameplay.shared.tileIdsFor

/**
 * Campaign entry point: first three levels teach simple pairs on a small central
 * footprint, then retain the original generator and its split/direction mechanics.
 * Board dimensions stay 12x8 including the open border, so original path rules and
 * renderer coordinates are unchanged. createOnetRound remains the JS parity API.
 */
fun createOnetLevelRound(
    theme: String,
    level: Int,
    rng: RandomGenerator = RandomGenerator { Math.random() }
): OnetRoundState {
    require(level >= 1) { "Level must be positive." }
    if (level > 3) return createOnetRound(theme = theme, level = level, rng = rng)
    val pairs = level * 4
    val rows = level * 2
    val startRow = (10 - rows) / 2 + 1
    val ids = shuffleList(tileIdsFor(theme), rng).take(2 + level * 2)
    val pairIds = shuffleList(List(pairs) { ids[it % ids.size] }, rng)
    val board = List(BOARD_ROWS) { MutableList<Tile?>(BOARD_COLS) { null } }
    pairIds.forEachIndexed { index, id ->
        val row = startRow + index / 2
        val col = 2 + (index % 2) * 2
        board[row][col] = Tile(id)
        board[row][col + 1] = Tile(id)
    }
    return createOnetRound(theme = theme, level = level, board = board, seconds = 150)
}
