package game.gameplay.triplematch

import game.gameplay.shared.RandomGenerator
import game.gameplay.shared.shuffleList
import kotlin.math.max

const val TRAY_CAPACITY: Int = 7
const val ROUND_SECONDS: Int = 120
const val TRIO_SCORE: Int = 150

data class UndoState(
    val stacks: List<List<String>>,
    val tray: List<String>,
    val cleared: Int
)

data class TripleRoundState(
    val stacks: List<List<String>>,
    val tray: List<String> = emptyList(),
    val cleared: Int = 0,
    val phase: String = "ready", // "ready", "playing", "won", "lost"
    val paused: Boolean = false,
    val time: Int = ROUND_SECONDS,
    val duration: Int = ROUND_SECONDS,
    val undo: UndoState? = null,
    val undoUsed: Boolean = false
)

data class TripleResult(
    val phase: String,
    val score: Int,
    val matches: Int,
    val mistakes: Int,
    val timeElapsed: Long
)

fun randomize(items: List<String>, rng: RandomGenerator): List<String> =
    shuffleList(items, rng)

fun createStacks(
    tileIds: List<String>,
    rng: RandomGenerator = RandomGenerator { Math.random() }
): List<List<String>> {
    val ids = randomize(tileIds, rng).take(6)
    require(ids.size == 6 && ids.toSet().size == 6) {
        "Triple Match needs at least six distinct tile IDs."
    }

    val layers = (0..2).map { n ->
        val layerIds = List(4) { i -> ids[(i + n) % 6] }.flatMap { id -> listOf(id, id, id) }
        randomize(layerIds, rng)
    }

    return List(12) { i ->
        layers.map { layer -> layer[i] }
    }
}

fun createTripleRound(
    tileIds: List<String>,
    rng: RandomGenerator = RandomGenerator { Math.random() },
    seconds: Int = ROUND_SECONDS,
    stacks: List<List<String>>? = null
): TripleRoundState {
    require(seconds > 0) { "seconds must be positive." }
    val initialStacks = stacks?.map { it.toList() } ?: createStacks(tileIds, rng)
    return TripleRoundState(
        stacks = initialStacks,
        tray = emptyList(),
        cleared = 0,
        phase = "ready",
        paused = false,
        time = seconds,
        duration = seconds,
        undo = null,
        undoUsed = false
    )
}

data class TrioResult(
    val tray: List<String>,
    val matched: Boolean,
    val matchedId: String? = null
)

fun resolveTrio(tray: List<String>): TrioResult {
    val counts = mutableMapOf<String, Int>()
    for (id in tray) {
        val count = (counts[id] ?: 0) + 1
        counts[id] = count
        if (count == 3) {
            return TrioResult(
                tray = tray.filter { it != id },
                matched = true,
                matchedId = id
            )
        }
    }
    return TrioResult(tray = tray, matched = false)
}

fun remainingTiles(state: TripleRoundState): Int =
    state.stacks.sumOf { it.size }

fun tripleScore(state: TripleRoundState): Int =
    state.cleared * TRIO_SCORE

private fun finish(state: TripleRoundState): TripleRoundState {
    val remaining = remainingTiles(state)
    if (remaining == 0 || state.tray.size >= TRAY_CAPACITY || state.time == 0) {
        val won = remaining == 0 && state.tray.isEmpty()
        return state.copy(phase = if (won) "won" else "lost")
    }
    return state
}

fun startTripleRound(state: TripleRoundState): TripleRoundState {
    return if (state.phase == "ready") {
        finish(state.copy(phase = "playing"))
    } else {
        state
    }
}

fun pauseTripleRound(state: TripleRoundState, paused: Boolean = true): TripleRoundState {
    return if (state.phase == "playing") state.copy(paused = paused) else state
}

fun pickTile(state: TripleRoundState, stackIndex: Int): TripleRoundState {
    if (state.phase != "playing" || state.paused || state.tray.size >= TRAY_CAPACITY) return state
    if (stackIndex < 0 || stackIndex >= state.stacks.size) return state
    val stack = state.stacks[stackIndex]
    if (stack.isEmpty()) return state

    val undo = UndoState(
        stacks = state.stacks.map { it.toList() },
        tray = state.tray.toList(),
        cleared = state.cleared
    )

    val mutableStacks = state.stacks.map { it.toMutableList() }.toMutableList()
    val id = mutableStacks[stackIndex].removeAt(mutableStacks[stackIndex].size - 1)

    val items = state.tray + id
    val matched = items.count { it == id } == 3
    val nextTray = if (matched) items.filter { it != id } else items
    val nextCleared = state.cleared + if (matched) 1 else 0

    val nextState = state.copy(
        stacks = mutableStacks.map { it.toList() },
        tray = nextTray,
        cleared = nextCleared,
        undo = undo
    )

    return finish(nextState)
}

fun undoPick(state: TripleRoundState): TripleRoundState {
    if (state.undo == null || state.undoUsed || state.phase != "playing" || state.paused) return state
    return state.copy(
        stacks = state.undo.stacks.map { it.toList() },
        tray = state.undo.tray.toList(),
        cleared = state.undo.cleared,
        undoUsed = true
    )
}

fun tickTripleRound(state: TripleRoundState, seconds: Int = 1): TripleRoundState {
    if (state.phase != "playing" || state.paused) return state
    require(seconds >= 0) { "seconds must be finite and nonnegative." }
    return finish(state.copy(time = max(0, state.time - seconds)))
}

fun tripleResult(state: TripleRoundState): TripleResult {
    return TripleResult(
        phase = state.phase,
        score = tripleScore(state),
        matches = state.cleared,
        mistakes = if (state.undoUsed) 1 else 0,
        timeElapsed = (state.duration - state.time).toLong() * 1000L
    )
}

object TripleMatchEngine {
    const val TRAY_CAPACITY = game.gameplay.triplematch.TRAY_CAPACITY
    const val ROUND_SECONDS = game.gameplay.triplematch.ROUND_SECONDS
    const val TRIO_SCORE = game.gameplay.triplematch.TRIO_SCORE

    fun createStacks(tileIds: List<String>, rng: RandomGenerator = RandomGenerator { Math.random() }) =
        game.gameplay.triplematch.createStacks(tileIds, rng)

    fun createRound(
        tileIds: List<String>,
        rng: RandomGenerator = RandomGenerator { Math.random() },
        seconds: Int = ROUND_SECONDS,
        stacks: List<List<String>>? = null
    ) = createTripleRound(tileIds, rng, seconds, stacks)

    fun difficulty(level: Int) = getTripleDifficulty(level)
    fun createLevelRound(tileIds: List<String>, level: Int, rng: RandomGenerator = RandomGenerator { Math.random() }) =
        createTripleLevelRound(tileIds, level, rng)

    fun startRound(state: TripleRoundState) = startTripleRound(state)
    fun pauseRound(state: TripleRoundState, paused: Boolean = true) = pauseTripleRound(state, paused)
    fun resolveTrio(tray: List<String>) = game.gameplay.triplematch.resolveTrio(tray)
    fun pickTile(state: TripleRoundState, stackIndex: Int) = game.gameplay.triplematch.pickTile(state, stackIndex)
    fun undoPick(state: TripleRoundState) = game.gameplay.triplematch.undoPick(state)
    fun tickRound(state: TripleRoundState, seconds: Int = 1) = tickTripleRound(state, seconds)
    fun remainingTiles(state: TripleRoundState) = game.gameplay.triplematch.remainingTiles(state)
    fun score(state: TripleRoundState) = tripleScore(state)
    fun result(state: TripleRoundState) = tripleResult(state)
}
