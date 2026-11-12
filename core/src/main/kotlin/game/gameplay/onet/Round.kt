package game.gameplay.onet

import game.gameplay.shared.RandomGenerator
import kotlin.math.max

const val MATCH_ANIMATION_MS: Long = 480L
const val COMBO_WINDOW_MS: Long = 4000L

data class PendingMatch(
    val a: Point,
    val b: Point,
    val action: String,
    val path: List<Point>,
    val points: Int
)

data class OnetEvent(
    val type: String,
    val tiles: List<Point>? = null,
    val a: Point? = null,
    val b: Point? = null,
    val action: String? = null,
    val path: List<Point>? = null,
    val points: Int? = null,
    val duration: Long? = null
)

data class OnetRoundState(
    val theme: String = "sweets",
    val level: Int = 1,
    val board: List<List<Tile?>>,
    val phase: String = "playing", // "playing", "won", "lost"
    val paused: Boolean = false,
    val selected: Point? = null,
    val pending: PendingMatch? = null,
    val score: Int = 0,
    val combo: Int = 0,
    val highestCombo: Int = 0,
    val lastMatchAt: Long? = null,
    val matches: Int = 0,
    val mistakes: Int = 0,
    val splits: Int = 0,
    val time: Int,
    val duration: Int,
    val event: OnetEvent? = null
)

data class OnetResult(
    val phase: String,
    val level: Int,
    val score: Int,
    val matches: Int,
    val splits: Int,
    val mistakes: Int,
    val isFlawless: Boolean,
    val highestCombo: Int,
    val remainingTime: Int,
    val timeElapsed: Long
)

fun createOnetRound(
    theme: String = "sweets",
    level: Int = 1,
    rng: RandomGenerator = RandomGenerator { Math.random() },
    board: List<List<Tile?>>? = null,
    seconds: Int? = null
): OnetRoundState {
    val lvl = max(1, level)
    val duration = seconds ?: getOnetDifficulty(lvl).time
    require(duration > 0) { "seconds must be positive." }

    val initialBoard = board ?: generateBoard(theme, lvl, rng)
    return OnetRoundState(
        theme = theme,
        level = lvl,
        board = initialBoard,
        phase = "playing",
        paused = false,
        selected = null,
        pending = null,
        score = 0,
        combo = 0,
        highestCombo = 0,
        lastMatchAt = null,
        matches = 0,
        mistakes = 0,
        splits = 0,
        time = duration,
        duration = duration,
        event = null
    )
}

private fun finish(state: OnetRoundState): OnetRoundState {
    if (state.pending != null) return state
    if (countRemaining(state.board) == 0) {
        return state.copy(phase = "won", selected = null)
    }
    if (state.time == 0) {
        return state.copy(phase = "lost", selected = null)
    }
    return state
}

fun pauseOnetRound(state: OnetRoundState, paused: Boolean = true): OnetRoundState {
    return if (state.phase == "playing") state.copy(paused = paused) else state
}

fun selectTile(state: OnetRoundState, r: Int, c: Int, now: Long = System.currentTimeMillis()): OnetRoundState {
    if (state.phase != "playing" || state.paused || state.pending != null) return state
    val cellTile = state.board.getOrNull(r)?.getOrNull(c) ?: return state

    var targetR = r
    var targetC = c
    var tile = cellTile
    if (tile.isSlave) {
        targetR = tile.masterR
        targetC = tile.masterC
        tile = state.board[targetR][targetC] ?: return state
    }

    val b = Point(targetR, targetC)
    val a = state.selected

    if (a == null) {
        return state.copy(selected = b, event = null)
    }
    if (a.r == targetR && a.c == targetC) {
        return state.copy(selected = null, event = null)
    }

    val tileA = state.board[a.r][a.c]
    if (!canMatch(tileA, tile)) {
        return state.copy(selected = b, event = null)
    }

    val route = getPath(state.board, a.r, a.c, targetR, targetC)
    if (route?.path == null) {
        return state.copy(
            selected = null,
            mistakes = state.mistakes + 1,
            event = OnetEvent(type = "blocked", tiles = listOf(a, b))
        )
    }

    val combo = if (state.lastMatchAt != null && (now - state.lastMatchAt) < COMBO_WINDOW_MS) {
        state.combo + 1
    } else {
        1
    }

    val action = getMatchAction(state.board, a, b)
    val baseScore = when (action) {
        "split_to_1x2" -> 200
        "split_to_1x1" -> 150
        else -> 100
    }
    val points = baseScore + (combo - 1) * 10
    val pending = PendingMatch(a, b, action, route.path, points)

    return state.copy(
        selected = null,
        pending = pending,
        combo = combo,
        highestCombo = max(state.highestCombo, combo),
        lastMatchAt = now,
        matches = state.matches + 1,
        splits = state.splits + if (action != "clear") 1 else 0,
        event = OnetEvent(
            type = "match",
            a = a,
            b = b,
            action = action,
            path = route.path,
            points = points,
            duration = MATCH_ANIMATION_MS
        )
    )
}

fun resolveMatch(state: OnetRoundState): OnetRoundState {
    if (state.pending == null || state.phase != "playing") return state
    val (a, b, _, _, points) = state.pending
    val newBoard = clearMatchedGroups(state.board, a, b, state.theme)
    val nextState = state.copy(
        board = newBoard,
        score = state.score + points,
        pending = null,
        event = null
    )
    return finish(nextState)
}

fun tickOnetRound(state: OnetRoundState, seconds: Int = 1): OnetRoundState {
    if (state.phase != "playing" || state.paused) return state
    require(seconds >= 0) { "seconds must be finite and nonnegative." }
    return finish(state.copy(time = max(0, state.time - seconds)))
}

fun hintFor(state: OnetRoundState): Hint? {
    if (state.phase != "playing" || state.paused || state.pending != null) return null
    return findHint(state.board)
}

fun shuffleRound(
    state: OnetRoundState,
    rng: RandomGenerator = RandomGenerator { Math.random() }
): OnetRoundState {
    if (state.phase != "playing" || state.paused || state.pending != null) return state
    val newBoard = guaranteedShuffle(state.board, rng, state.level)
    return state.copy(
        board = newBoard,
        selected = null,
        event = OnetEvent(type = "shuffle")
    )
}

fun needsShuffle(state: OnetRoundState): Boolean {
    return state.phase == "playing" &&
            state.pending == null &&
            countRemaining(state.board) > 0 &&
            findHint(state.board) == null
}

fun onetResult(state: OnetRoundState): OnetResult {
    return OnetResult(
        phase = state.phase,
        level = state.level,
        score = state.score,
        matches = state.matches,
        splits = state.splits,
        mistakes = state.mistakes,
        isFlawless = state.phase == "won" && state.mistakes == 0,
        highestCombo = state.highestCombo,
        remainingTime = state.time,
        timeElapsed = (state.duration - state.time).toLong() * 1000L
    )
}
